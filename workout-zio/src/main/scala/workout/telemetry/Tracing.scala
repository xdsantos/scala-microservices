package workout.telemetry

import io.opentelemetry.api.trace.{Span, SpanContext, SpanKind, StatusCode, TraceFlags, TraceState, Tracer}
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.SimpleSpanProcessor
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.context.Context
import workout.config.TelemetryConfig
import zio._

import java.time.Duration

trait Tracing {
  def spanZIO[R, E, A](name: String)(effect: ZIO[R, E, A]): ZIO[R, E, A]
  def spanZIOWithKind[R, E, A](name: String, kind: SpanKind)(effect: ZIO[R, E, A]): ZIO[R, E, A]
  def spanZIORoot[R, E, A](name: String, kind: SpanKind = SpanKind.INTERNAL)(effect: ZIO[R, E, A]): ZIO[R, E, A]
  def spanZIOWithParent[R, E, A](name: String, traceId: String, spanId: String)(effect: ZIO[R, E, A]): ZIO[R, E, A]
  def addAttribute(key: String, value: String): UIO[Unit]
  def addAttributeLong(key: String, value: Long): UIO[Unit]
  def addAttributeBool(key: String, value: Boolean): UIO[Unit]
  def recordException(throwable: Throwable): UIO[Unit]
  def getCurrentTraceId: UIO[Option[String]]
  def getCurrentSpanId: UIO[Option[String]]
}

object Tracing {
  def spanZIO[R, E, A](name: String)(effect: ZIO[R, E, A]): ZIO[R with Tracing, E, A] =
    ZIO.serviceWithZIO[Tracing](_.spanZIO(name)(effect))

  def spanZIOWithKind[R, E, A](name: String, kind: SpanKind)(effect: ZIO[R, E, A]): ZIO[R with Tracing, E, A] =
    ZIO.serviceWithZIO[Tracing](_.spanZIOWithKind(name, kind)(effect))

  def spanZIORoot[R, E, A](name: String, kind: SpanKind = SpanKind.INTERNAL)(effect: ZIO[R, E, A]): ZIO[R with Tracing, E, A] =
    ZIO.serviceWithZIO[Tracing](_.spanZIORoot(name, kind)(effect))

  def spanZIOWithParent[R, E, A](name: String, traceId: String, spanId: String)(effect: ZIO[R, E, A]): ZIO[R with Tracing, E, A] =
    ZIO.serviceWithZIO[Tracing](_.spanZIOWithParent(name, traceId, spanId)(effect))

  def addAttribute(key: String, value: String): ZIO[Tracing, Nothing, Unit] =
    ZIO.serviceWithZIO[Tracing](_.addAttribute(key, value))

  def addAttributeLong(key: String, value: Long): ZIO[Tracing, Nothing, Unit] =
    ZIO.serviceWithZIO[Tracing](_.addAttributeLong(key, value))

  def addAttributeBool(key: String, value: Boolean): ZIO[Tracing, Nothing, Unit] =
    ZIO.serviceWithZIO[Tracing](_.addAttributeBool(key, value))

  def recordException(throwable: Throwable): ZIO[Tracing, Nothing, Unit] =
    ZIO.serviceWithZIO[Tracing](_.recordException(throwable))

  def getCurrentTraceId: ZIO[Tracing, Nothing, Option[String]] =
    ZIO.serviceWithZIO[Tracing](_.getCurrentTraceId)

  def getCurrentSpanId: ZIO[Tracing, Nothing, Option[String]] =
    ZIO.serviceWithZIO[Tracing](_.getCurrentSpanId)
}

final case class TracingLive(tracer: Tracer) extends Tracing {
  override def spanZIO[R, E, A](name: String)(effect: ZIO[R, E, A]): ZIO[R, E, A] =
    spanZIOWithKind(name, SpanKind.INTERNAL)(effect)

  override def spanZIOWithKind[R, E, A](name: String, kind: SpanKind)(effect: ZIO[R, E, A]): ZIO[R, E, A] =
    ZIO.acquireReleaseWith(
      ZIO.succeed {
        val span = tracer.spanBuilder(name).setSpanKind(kind).startSpan()
        val scope = span.makeCurrent()
        (span, scope)
      }
    ) { case (span, scope) =>
      ZIO.succeed {
        scope.close()
        span.end()
      }
    } { case (span, _) =>
      effect
        .tapError { e =>
          ZIO.succeed {
            span.setStatus(StatusCode.ERROR, e.toString)
            e match {
              case t: Throwable => span.recordException(t)
              case _ => ()
            }
          }
        }
        .tap(_ => ZIO.succeed(span.setStatus(StatusCode.OK)))
    }

  override def spanZIORoot[R, E, A](name: String, kind: SpanKind = SpanKind.INTERNAL)(effect: ZIO[R, E, A]): ZIO[R, E, A] =
    ZIO.acquireReleaseWith(
      ZIO.succeed {
        val span = tracer.spanBuilder(name).setNoParent().setSpanKind(kind).startSpan()
        val scope = span.makeCurrent()
        (span, scope)
      }
    ) { case (span, scope) =>
      ZIO.succeed {
        scope.close()
        span.end()
      }
    } { case (span, _) =>
      effect
        .tapError { e =>
          ZIO.succeed {
            span.setStatus(StatusCode.ERROR, e.toString)
            e match {
              case t: Throwable => span.recordException(t)
              case _ => ()
            }
          }
        }
        .tap(_ => ZIO.succeed(span.setStatus(StatusCode.OK)))
    }

  override def spanZIOWithParent[R, E, A](name: String, traceId: String, spanId: String)(effect: ZIO[R, E, A]): ZIO[R, E, A] =
    ZIO.acquireReleaseWith(
      ZIO.succeed {
        val parentContext = SpanContext.createFromRemoteParent(
          traceId,
          spanId,
          TraceFlags.getSampled,
          TraceState.getDefault
        )
        val parent = Context.current().`with`(Span.wrap(parentContext))
        val span = tracer.spanBuilder(name)
          .setParent(parent)
          .setSpanKind(SpanKind.CONSUMER)
          .startSpan()
        val scope = span.makeCurrent()
        (span, scope)
      }
    ) { case (span, scope) =>
      ZIO.succeed {
        scope.close()
        span.end()
      }
    } { case (span, _) =>
      effect
        .tapError { e =>
          ZIO.succeed {
            span.setStatus(StatusCode.ERROR, e.toString)
            e match {
              case t: Throwable => span.recordException(t)
              case _ => ()
            }
          }
        }
        .tap(_ => ZIO.succeed(span.setStatus(StatusCode.OK)))
    }

  override def addAttribute(key: String, value: String): UIO[Unit] =
    ZIO.succeed {
      Span.current().setAttribute(key, value)
    }

  override def addAttributeLong(key: String, value: Long): UIO[Unit] =
    ZIO.succeed {
      Span.current().setAttribute(key, value)
    }

  override def addAttributeBool(key: String, value: Boolean): UIO[Unit] =
    ZIO.succeed {
      Span.current().setAttribute(key, value)
    }

  override def recordException(throwable: Throwable): UIO[Unit] =
    ZIO.succeed {
      Span.current().recordException(throwable)
      Span.current().setStatus(StatusCode.ERROR, throwable.getMessage)
    }

  override def getCurrentTraceId: UIO[Option[String]] =
    ZIO.succeed {
      val ctx = Span.current().getSpanContext
      if (ctx.isValid) Some(ctx.getTraceId) else None
    }

  override def getCurrentSpanId: UIO[Option[String]] =
    ZIO.succeed {
      val ctx = Span.current().getSpanContext
      if (ctx.isValid) Some(ctx.getSpanId) else None
    }
}

object TracingLive {
  def layer: ZLayer[TelemetryConfig, Throwable, Tracing] =
    ZLayer.scoped {
      ZIO.serviceWithZIO[TelemetryConfig] { config =>
        if (config.enabled) {
          ZIO.acquireRelease(
            ZIO.attemptBlocking {
              val resource = Resource.getDefault.toBuilder
                .put(AttributeKey.stringKey("service.name"), config.serviceName)
                .build()

              val spanExporter = OtlpGrpcSpanExporter.builder()
                .setEndpoint(config.endpoint)
                .setTimeout(Duration.ofSeconds(10))
                .build()

              val tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .setResource(resource)
                .build()

              val openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .build()

              TracingLive(openTelemetry.getTracer(config.serviceName))
            }
          )(_ => ZIO.succeed(()))
        } else {
          ZIO.succeed(TracingNoop())
        }
      }
    }
}

// Noop implementation when tracing is disabled
final case class TracingNoop() extends Tracing {
  override def spanZIO[R, E, A](name: String)(effect: ZIO[R, E, A]): ZIO[R, E, A] = effect
  override def spanZIOWithKind[R, E, A](name: String, kind: SpanKind)(effect: ZIO[R, E, A]): ZIO[R, E, A] = effect
  override def spanZIORoot[R, E, A](name: String, kind: SpanKind)(effect: ZIO[R, E, A]): ZIO[R, E, A] = effect
  override def spanZIOWithParent[R, E, A](name: String, traceId: String, spanId: String)(effect: ZIO[R, E, A]): ZIO[R, E, A] = effect
  override def addAttribute(key: String, value: String): UIO[Unit] = ZIO.unit
  override def addAttributeLong(key: String, value: Long): UIO[Unit] = ZIO.unit
  override def addAttributeBool(key: String, value: Boolean): UIO[Unit] = ZIO.unit
  override def recordException(throwable: Throwable): UIO[Unit] = ZIO.unit
  override def getCurrentTraceId: UIO[Option[String]] = ZIO.none
  override def getCurrentSpanId: UIO[Option[String]] = ZIO.none
}

object TracingNoop {
  val layer: ZLayer[Any, Nothing, Tracing] =
    ZLayer.succeed(TracingNoop())
}
