package workout.kafka

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.kafka.ProducerSettings
import org.apache.pekko.kafka.scaladsl.Producer
import org.apache.pekko.stream.scaladsl.Source
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import workout.config.KafkaConfig
import workout.domain.Workout
import io.circe.syntax._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID

class PekkoKafkaProducer(config: KafkaConfig)(implicit system: ActorSystem[_], ec: ExecutionContext)
    extends WorkoutEventProducer
    with LazyLogging {

  private val producerSettings = ProducerSettings(system, new StringSerializer, new StringSerializer)
    .withBootstrapServers(config.bootstrapServers)

  private def publish(event: WorkoutEvent): Future[Unit] = {
    val key = event.workoutId.toString
    val value = event.asJson.noSpaces
    val record = new ProducerRecord[String, String](config.topic, key, value)

    Source
      .single(record)
      .runWith(Producer.plainSink(producerSettings))
      .map { _ =>
        logger.debug(s"Published event: ${event.eventType.value} for workout ${event.workoutId}")
      }
      .recover { case ex =>
        logger.error(s"Failed to publish event: ${ex.getMessage}", ex)
      }
  }

  override def publishCreated(workout: Workout): Future[Unit] =
    publish(WorkoutEvent.created(workout))

  override def publishUpdated(workout: Workout): Future[Unit] =
    publish(WorkoutEvent.updated(workout))

  override def publishDeleted(workoutId: UUID): Future[Unit] =
    publish(WorkoutEvent.deleted(workoutId))
}

