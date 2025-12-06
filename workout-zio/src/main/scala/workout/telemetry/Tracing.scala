package workout.telemetry

import io.opentelemetry.api.trace.{Span, SpanKind, StatusCode, Tracer}
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.BatchSpanProcessor
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.api.common.AttributeKey
import workout.config.TelemetryConfig
import zio._

trait Tracing {
  def spanZIO[R, E, A](name: String)(effect: ZIO[R, E, A]): ZIO[R, E, A]
  def addAttribute(key: String, value: String): UIO[Unit]
  def recordException(throwable: Throwable): UIO[Unit]
}

object Tracing {
  def spanZIO[R, E, A](name: String)(effect: ZIO[R, E, A]): ZIO[R with Tracing, E, A] =
    ZIO.serviceWithZIO[Tracing](_.spanZIO(name)(effect))

  def addAttribute(key: String, value: String): ZIO[Tracing, Nothing, Unit] =
    ZIO.serviceWithZIO[Tracing](_.addAttribute(key, value))

  def recordException(throwable: Throwable): ZIO[Tracing, Nothing, Unit] =
    ZIO.serviceWithZIO[Tracing](_.recordException(throwable))
}

final case class TracingLive(tracer: Tracer) extends Tracing {
  override def spanZIO[R, E, A](name: String)(effect: ZIO[R, E, A]): ZIO[R, E, A] =
    ZIO.acquireReleaseWith(
      ZIO.succeed(tracer.spanBuilder(name).setSpanKind(SpanKind.INTERNAL).startSpan())
    ) { span =>
      ZIO.succeed(span.end())
    } { span =>
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

  override def recordException(throwable: Throwable): UIO[Unit] =
    ZIO.succeed {
      Span.current().recordException(throwable)
      Span.current().setStatus(StatusCode.ERROR, throwable.getMessage)
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
                .build()

              val tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                .setResource(resource)
                .build()

              val openTelemetry = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal()

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
  override def addAttribute(key: String, value: String): UIO[Unit] = ZIO.unit
  override def recordException(throwable: Throwable): UIO[Unit] = ZIO.unit
}

object TracingNoop {
  val layer: ZLayer[Any, Nothing, Tracing] =
    ZLayer.succeed(TracingNoop())
}
