package workout.kafka

import workout.config.KafkaConfig
import zio._
import zio.kafka.producer._

object KafkaProducerLive {
  val layer: ZLayer[KafkaConfig, Throwable, Producer] =
    ZLayer.scoped {
      ZIO.serviceWithZIO[KafkaConfig] { config =>
        val producerSettings = ProducerSettings(List(config.bootstrapServers))
          .withClientId("workout-service-producer")
        Producer.make(producerSettings)
      }
    }
}
