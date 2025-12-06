package workout.kafka

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.kafka.{CommitterSettings, ConsumerSettings, Subscriptions}
import org.apache.pekko.kafka.scaladsl.{Committer, Consumer}
import org.apache.pekko.stream.scaladsl.Keep
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import workout.config.KafkaConfig
import workout.service.WorkoutService
import workout.tracing.Tracing
import io.opentelemetry.api.trace.SpanKind
import io.circe.parser._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext

class WorkoutCommandConsumer(
    config: KafkaConfig,
    workoutService: WorkoutService
)(implicit system: ActorSystem[_], ec: ExecutionContext)
    extends LazyLogging {

  import io.opentelemetry.api.trace.Span
  import workout.domain.Workout
  import scala.concurrent.Future

  private val consumerSettings = ConsumerSettings(system, new StringDeserializer, new StringDeserializer)
    .withBootstrapServers(config.bootstrapServers)
    .withGroupId(config.consumerGroup)
    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false")

  private val committerSettings = CommitterSettings(system)

  private def processCommand(span: Span, command: CreateWorkoutCommand): Future[Workout] = {
    span.setAttribute("messaging.system", "kafka")
    span.setAttribute("messaging.destination", config.commandTopic)
    span.setAttribute("messaging.operation", "process")
    span.setAttribute("correlation.id", command.correlationId.toString)
    span.setAttribute("workout.name", command.request.name)
    
    logger.info(s"Processing CreateWorkoutCommand with correlationId: ${command.correlationId}")

    workoutService.create(command.request).map { workout =>
      span.setAttribute("workout.id", workout.id.toString)
      logger.info(s"Successfully created workout ${workout.id} for correlationId: ${command.correlationId}")
      workout
    }.recover { case ex =>
      logger.error(s"Failed to create workout for correlationId ${command.correlationId}: ${ex.getMessage}", ex)
      throw ex
    }
  }

  def start(): Consumer.DrainingControl[_] = {
    logger.info(s"Starting workout command consumer on topic: ${config.commandTopic}")

    val control = Consumer
      .committableSource(consumerSettings, Subscriptions.topics(config.commandTopic))
      .mapAsync(1) { msg =>
        val value = msg.record.value()
        logger.debug(s"Received message: $value")

        decode[CreateWorkoutCommand](value) match {
          case Right(command) =>
            // Create span linked to the original trace if trace context was propagated
            val processFuture = (command.traceId, command.spanId) match {
              case (Some(traceId), Some(spanId)) if traceId.nonEmpty && spanId.nonEmpty =>
                logger.debug(s"Found propagated trace context: traceId=$traceId, spanId=$spanId")
                val parentContext = Tracing.createRemoteContext(traceId, spanId)
                Tracing.withChildSpanAsync("KafkaConsumer.processMessage", parentContext, SpanKind.CONSUMER) { span =>
                  processCommand(span, command)
                }
              case _ =>
                // No trace context, create a new trace
                Tracing.withSpanAsync("KafkaConsumer.processMessage", SpanKind.CONSUMER) { span =>
                  processCommand(span, command)
                }
            }
            // After successful processing, return the committable offset
            processFuture.map(_ => msg.committableOffset)
          case Left(error) =>
            logger.error(s"Failed to decode message: ${error.getMessage}")
            Future.failed(error)
        }
      }
      .toMat(Committer.sink(committerSettings))(Consumer.DrainingControl.apply)
      .run()

    logger.info("Workout command consumer started successfully")
    control
  }
}

