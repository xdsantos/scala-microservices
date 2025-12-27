package workoutcats.kafka

import cats.effect.kernel.MonadCancelThrow
import cats.syntax.all._
import fs2.kafka._
import workoutcats.config.KafkaConfig
import workoutcats.telemetry.Tracing
import io.circe.syntax._
import workoutcats.domain.Workout
import java.util.UUID

final class WorkoutEventProducerFs2[F[_]: MonadCancelThrow](
    producer: KafkaProducer[F, String, String],
    config: KafkaConfig,
    tracing: Tracing[F]
) extends WorkoutEventProducer[F] {

  private def send(topic: String, key: String, value: String): F[Unit] =
    producer.produce(ProducerRecords.one(ProducerRecord(topic, key, value))).void

  override def publishCreateCommand(cmd: CreateWorkoutCommand): F[UUID] =
    tracing.span("KafkaProducer.publishCreateCommand") {
      send(config.commandTopic, cmd.correlationId.toString, cmd.asJson.noSpaces).as(cmd.correlationId)
    }

  override def publishCreated(workout: Workout): F[Unit] =
    tracing.span("KafkaProducer.publishCreated") {
      send(config.topic, workout.id.toString, WorkoutEvent.created(workout).asJson.noSpaces)
    }

  override def publishUpdated(workout: Workout): F[Unit] =
    tracing.span("KafkaProducer.publishUpdated") {
      send(config.topic, workout.id.toString, WorkoutEvent.updated(workout).asJson.noSpaces)
    }

  override def publishDeleted(id: UUID): F[Unit] =
    tracing.span("KafkaProducer.publishDeleted") {
      send(config.topic, id.toString, WorkoutEvent.deleted(id).asJson.noSpaces)
    }
}

