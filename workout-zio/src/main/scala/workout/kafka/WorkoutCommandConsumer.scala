package workout.kafka

import workout.config.KafkaConfig
import workout.service.WorkoutService
import workout.telemetry.Tracing
import zio._
import zio.json._
import zio.kafka.consumer._
import zio.kafka.serde._
import io.opentelemetry.api.trace.SpanKind

trait WorkoutCommandConsumer {
  def start: Task[Unit]
}

object WorkoutCommandConsumer {
  def start: ZIO[WorkoutCommandConsumer, Throwable, Unit] =
    ZIO.serviceWithZIO[WorkoutCommandConsumer](_.start)
}

final case class WorkoutCommandConsumerLive(
    consumer: Consumer,
    config: KafkaConfig,
    workoutService: WorkoutService,
    tracing: Tracing
) extends WorkoutCommandConsumer {

  override def start: Task[Unit] = {
    ZIO.logInfo(s"Starting workout command consumer on topic: ${config.commandTopic}") *>
      consumer
        .plainStream(Subscription.topics(config.commandTopic), Serde.string, Serde.string)
        .mapZIO { record =>
          val value = record.value
          ZIO.logDebug(s"Received message: $value") *>
            (value.fromJson[CreateWorkoutCommand] match {
              case Right(command) =>
                processCommand(command).as(record.offset)
              case Left(error) =>
                ZIO.logError(s"Failed to decode message: $error").as(record.offset)
            })
        }
        .aggregateAsync(Consumer.offsetBatches)
        .mapZIO(_.commit)
        .runDrain
  }

  private def processCommand(command: CreateWorkoutCommand): Task[Unit] = {
    val processEffect = (command.traceId, command.spanId) match {
      case (Some(traceId), Some(spanId)) if traceId.nonEmpty && spanId.nonEmpty =>
        ZIO.logDebug(s"Found propagated trace context: traceId=$traceId, spanId=$spanId") *>
          tracing.spanZIOWithParent("KafkaConsumer.processMessage", traceId, spanId) {
            doProcess(command)
          }
      case _ =>
        tracing.spanZIOWithKind("KafkaConsumer.processMessage", SpanKind.CONSUMER) {
          doProcess(command)
        }
    }
    processEffect
  }

  private def doProcess(command: CreateWorkoutCommand): Task[Unit] =
    for {
      _ <- tracing.addAttribute("messaging.system", "kafka")
      _ <- tracing.addAttribute("messaging.destination", config.commandTopic)
      _ <- tracing.addAttribute("messaging.operation", "process")
      _ <- tracing.addAttribute("correlation.id", command.correlationId.toString)
      _ <- tracing.addAttribute("workout.name", command.request.name)
      _ <- ZIO.logInfo(s"Processing CreateWorkoutCommand with correlationId: ${command.correlationId}")
      workout <- workoutService.create(command.request)
      _ <- tracing.addAttribute("workout.id", workout.id.toString)
      _ <- ZIO.logInfo(s"Successfully created workout ${workout.id} for correlationId: ${command.correlationId}")
    } yield ()
}

object WorkoutCommandConsumerLive {
  val layer: ZLayer[Consumer with KafkaConfig with WorkoutService with Tracing, Nothing, WorkoutCommandConsumer] =
    ZLayer.fromFunction(WorkoutCommandConsumerLive(_, _, _, _))
}

