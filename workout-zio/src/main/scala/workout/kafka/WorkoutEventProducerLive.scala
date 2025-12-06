package workout.kafka

import workout.config.KafkaConfig
import workout.domain.Workout
import zio._
import zio.json._
import zio.kafka.producer._
import zio.kafka.serde._
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.UUID

final case class WorkoutEventProducerLive(
    producer: Producer,
    config: KafkaConfig
) extends WorkoutEventProducer {

  private def publish(event: WorkoutEvent): Task[Unit] = {
    val key = event.workoutId.toString
    val value = event.toJson
    val record = new ProducerRecord[String, String](config.topic, key, value)
    producer.produce(record, Serde.string, Serde.string).unit
  }

  override def publishCreated(workout: Workout): Task[Unit] =
    publish(WorkoutEvent.created(workout))

  override def publishUpdated(workout: Workout): Task[Unit] =
    publish(WorkoutEvent.updated(workout))

  override def publishDeleted(workoutId: UUID): Task[Unit] =
    publish(WorkoutEvent.deleted(workoutId))
}

object WorkoutEventProducerLive {
  val layer: ZLayer[Producer with KafkaConfig, Nothing, WorkoutEventProducer] =
    ZLayer.fromFunction(WorkoutEventProducerLive(_, _))
}
