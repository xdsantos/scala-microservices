package workout.tracing

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.{Span, SpanContext, SpanKind, StatusCode, TraceFlags, TraceState, Tracer}
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.{BatchSpanProcessor, SimpleSpanProcessor}
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.resources.Resource
import com.typesafe.scalalogging.LazyLogging
import workout.config.TracingConfig

import java.time.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object Tracing extends LazyLogging {

  private var openTelemetry: OpenTelemetry = OpenTelemetry.noop()
  private var tracer: Tracer = openTelemetry.getTracer("workout-pekko-service")
  private var tracerProvider: SdkTracerProvider = _

  def init(config: TracingConfig): Unit = {
    if (config.enabled) {
      logger.info(s"Initializing OpenTelemetry with endpoint: ${config.endpoint}")

      try {
        val resource = Resource.builder()
          .put(AttributeKey.stringKey("service.name"), config.serviceName)
          .build()

        val spanExporter = OtlpGrpcSpanExporter.builder()
          .setEndpoint(config.endpoint)
          .setTimeout(Duration.ofSeconds(10))
          .build()

        // Use SimpleSpanProcessor for immediate export (better for debugging)
        // For production, use BatchSpanProcessor with shorter schedule delay
        tracerProvider = SdkTracerProvider.builder()
          .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
          .setResource(resource)
          .build()

        openTelemetry = OpenTelemetrySdk.builder()
          .setTracerProvider(tracerProvider)
          .build()

        tracer = openTelemetry.getTracer(config.serviceName)

        logger.info(s"OpenTelemetry initialized successfully - sending traces to ${config.endpoint}")
      } catch {
        case ex: Exception =>
          logger.error(s"Failed to initialize OpenTelemetry: ${ex.getMessage}", ex)
      }
    } else {
      logger.info("Tracing is disabled")
    }
  }

  def getTracer: Tracer = tracer

  /** Get current trace ID if a span is active */
  def getCurrentTraceId: Option[String] = {
    val span = Span.current()
    val ctx = span.getSpanContext
    if (ctx.isValid) Some(ctx.getTraceId) else None
  }

  /** Get current span ID if a span is active */
  def getCurrentSpanId: Option[String] = {
    val span = Span.current()
    val ctx = span.getSpanContext
    if (ctx.isValid) Some(ctx.getSpanId) else None
  }

  /** Create a SpanContext from trace and span IDs for linking */
  def createRemoteContext(traceId: String, spanId: String): Context = {
    val spanContext = SpanContext.createFromRemoteParent(
      traceId,
      spanId,
      TraceFlags.getSampled,
      TraceState.getDefault
    )
    Context.current().`with`(Span.wrap(spanContext))
  }

  def shutdown(): Unit = {
    if (tracerProvider != null) {
      logger.info("Flushing and shutting down OpenTelemetry")
      tracerProvider.forceFlush()
      tracerProvider.close()
    }
    openTelemetry match {
      case sdk: OpenTelemetrySdk =>
        sdk.close()
      case _ => // noop, nothing to close
    }
  }

  /** Create a span for an operation and execute the block within it */
  def withSpan[T](operationName: String, kind: SpanKind = SpanKind.INTERNAL)(block: Span => T): T = {
    val span = tracer.spanBuilder(operationName)
      .setSpanKind(kind)
      .startSpan()

    val scope = span.makeCurrent()
    try {
      val result = block(span)
      span.setStatus(StatusCode.OK)
      result
    } catch {
      case ex: Throwable =>
        span.setStatus(StatusCode.ERROR, ex.getMessage)
        span.recordException(ex)
        throw ex
    } finally {
      scope.close()
      span.end()
    }
  }

  /** Create a span for an async operation */
  def withSpanAsync[T](operationName: String, kind: SpanKind = SpanKind.INTERNAL)(
      block: Span => Future[T]
  )(implicit ec: ExecutionContext): Future[T] = {
    val span = tracer.spanBuilder(operationName)
      .setSpanKind(kind)
      .startSpan()

    val scope = span.makeCurrent()
    try {
      block(span).transform {
        case Success(value) =>
          span.setStatus(StatusCode.OK)
          span.end()
          Success(value)
        case Failure(ex) =>
          span.setStatus(StatusCode.ERROR, ex.getMessage)
          span.recordException(ex)
          span.end()
          Failure(ex)
      }
    } finally {
      scope.close()
    }
  }

  /** Create a child span from a parent context */
  def withChildSpan[T](operationName: String, parentContext: Context, kind: SpanKind = SpanKind.INTERNAL)(
      block: Span => T
  ): T = {
    val span = tracer.spanBuilder(operationName)
      .setParent(parentContext)
      .setSpanKind(kind)
      .startSpan()

    val scope = span.makeCurrent()
    try {
      val result = block(span)
      span.setStatus(StatusCode.OK)
      result
    } catch {
      case ex: Throwable =>
        span.setStatus(StatusCode.ERROR, ex.getMessage)
        span.recordException(ex)
        throw ex
    } finally {
      scope.close()
      span.end()
    }
  }

  /** Create a child span for async operations */
  def withChildSpanAsync[T](operationName: String, parentContext: Context, kind: SpanKind = SpanKind.INTERNAL)(
      block: Span => Future[T]
  )(implicit ec: ExecutionContext): Future[T] = {
    val span = tracer.spanBuilder(operationName)
      .setParent(parentContext)
      .setSpanKind(kind)
      .startSpan()

    val scope = span.makeCurrent()
    try {
      block(span).transform {
        case Success(value) =>
          span.setStatus(StatusCode.OK)
          span.end()
          Success(value)
        case Failure(ex) =>
          span.setStatus(StatusCode.ERROR, ex.getMessage)
          span.recordException(ex)
          span.end()
          Failure(ex)
      }
    } finally {
      scope.close()
    }
  }
}

