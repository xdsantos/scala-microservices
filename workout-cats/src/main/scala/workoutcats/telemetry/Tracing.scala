package workoutcats.telemetry

import cats.effect.kernel.{Resource, Sync}
import cats.effect.kernel.MonadCancelThrow
import cats.syntax.all._
import io.opentelemetry.api.trace.{Span, SpanKind, StatusCode, Tracer, SpanContext, TraceFlags, TraceState}
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.{Resource => OtelResource}
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.SimpleSpanProcessor
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.api.common.AttributeKey
import workoutcats.config.TelemetryConfig
import java.time.Duration

trait Tracing[F[_]] {
  def span[A](name: String, kind: SpanKind = SpanKind.INTERNAL)(fa: F[A]): F[A]
  def rootSpan[A](name: String, kind: SpanKind = SpanKind.INTERNAL)(fa: F[A]): F[A]
  def spanWithParent[A](name: String, traceId: String, spanId: String, kind: SpanKind = SpanKind.CONSUMER)(fa: F[A]): F[A]
  def addAttribute(key: String, value: String): F[Unit]
  def addAttribute(key: String, value: Long): F[Unit]
  def addAttribute(key: String, value: Boolean): F[Unit]
  def currentTraceId: F[Option[String]]
  def currentSpanId: F[Option[String]]
  def logWarn(msg: String): F[Unit]
  def unit: F[Unit]
}

object Tracing {
  def noop[F[_]: Sync]: Tracing[F] = new Tracing[F] {
    private val F = Sync[F]
    override def span[A](name: String, kind: SpanKind)(fa: F[A]): F[A] = fa
    override def rootSpan[A](name: String, kind: SpanKind)(fa: F[A]): F[A] = fa
    override def spanWithParent[A](name: String, traceId: String, spanId: String, kind: SpanKind)(fa: F[A]): F[A] = fa
    override def addAttribute(key: String, value: String): F[Unit] = F.unit
    override def addAttribute(key: String, value: Long): F[Unit] = F.unit
    override def addAttribute(key: String, value: Boolean): F[Unit] = F.unit
    override def currentTraceId: F[Option[String]] = F.pure(None)
    override def currentSpanId: F[Option[String]] = F.pure(None)
    override def logWarn(msg: String): F[Unit] = F.unit
    override def unit: F[Unit] = F.unit
  }

  def live[F[_]: Sync](config: TelemetryConfig): Resource[F, Tracing[F]] = {
    if (!config.enabled) Resource.pure(noop[F])
    else {
      val build = Sync[F].delay {
        val resource = OtelResource.getDefault.toBuilder
          .put(AttributeKey.stringKey("service.name"), config.serviceName)
          .build()

        val exporter = OtlpGrpcSpanExporter.builder()
          .setEndpoint(config.endpoint)
          .setTimeout(Duration.ofSeconds(10))
          .build()

        val tracerProvider = SdkTracerProvider.builder()
          .addSpanProcessor(SimpleSpanProcessor.create(exporter))
          .setResource(resource)
          .build()

        val otel = OpenTelemetrySdk.builder()
          .setTracerProvider(tracerProvider)
          .build()

        otel.getTracer(config.serviceName)
      }

      Resource.make(build)(_ => Sync[F].unit).map { tracer =>
        new Tracing[F] {
          private val F = Sync[F]

          private def inSpan[A](span: Span)(fa: F[A]): F[A] =
            F.bracket(F.delay(span.makeCurrent()))(_ => fa)(
              scope => F.delay { scope.close(); span.end() }
            )

          override def span[A](name: String, kind: SpanKind)(fa: F[A]): F[A] =
            F.defer {
              val span = tracer.spanBuilder(name).setSpanKind(kind).startSpan()
              inSpan(span) {
                fa.attempt.flatTap {
                  case Left(e) =>
                    F.delay {
                      span.setStatus(StatusCode.ERROR, e.getMessage)
                      span.recordException(e)
                    }
                  case Right(_) => F.delay(span.setStatus(StatusCode.OK))
                }.rethrow
              }
            }

          override def rootSpan[A](name: String, kind: SpanKind)(fa: F[A]): F[A] =
            F.defer {
              val span = tracer.spanBuilder(name).setNoParent().setSpanKind(kind).startSpan()
              inSpan(span) {
                fa.attempt.flatTap {
                  case Left(e) =>
                    F.delay {
                      span.setStatus(StatusCode.ERROR, e.getMessage)
                      span.recordException(e)
                    }
                  case Right(_) => F.delay(span.setStatus(StatusCode.OK))
                }.rethrow
              }
            }

          override def spanWithParent[A](name: String, traceId: String, spanId: String, kind: SpanKind)(fa: F[A]): F[A] =
            F.defer {
              val spanContext = SpanContext.createFromRemoteParent(
                traceId,
                spanId,
                TraceFlags.getSampled,
                TraceState.getDefault
              )
              val parentCtx = Context.root().`with`(Span.wrap(spanContext))
              val span = tracer.spanBuilder(name).setParent(parentCtx).setSpanKind(kind).startSpan()
              inSpan(span) {
                fa.attempt.flatTap {
                  case Left(e) =>
                    F.delay {
                      span.setStatus(StatusCode.ERROR, e.getMessage)
                      span.recordException(e)
                    }
                  case Right(_) => F.delay(span.setStatus(StatusCode.OK))
                }.rethrow
              }
            }

          override def addAttribute(key: String, value: String): F[Unit] =
            F.delay(Span.current().setAttribute(key, value))
          override def addAttribute(key: String, value: Long): F[Unit] =
            F.delay(Span.current().setAttribute(key, value))
          override def addAttribute(key: String, value: Boolean): F[Unit] =
            F.delay(Span.current().setAttribute(key, value))

          override def currentTraceId: F[Option[String]] =
            F.delay {
              val ctx = Span.current().getSpanContext
              if (ctx.isValid) Some(ctx.getTraceId) else None
            }
          override def currentSpanId: F[Option[String]] =
            F.delay {
              val ctx = Span.current().getSpanContext
              if (ctx.isValid) Some(ctx.getSpanId) else None
            }

          override def logWarn(msg: String): F[Unit] = F.delay(println(s"[WARN] $msg"))
          override def unit: F[Unit] = F.unit
        }
      }
    }
  }
}

