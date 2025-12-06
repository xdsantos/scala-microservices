package workout.config

import zio._
import com.typesafe.config.ConfigFactory

final case class ServerConfig(
    host: String,
    port: Int
)

final case class DatabaseConfig(
    driver: String,
    url: String,
    user: String,
    password: String,
    poolSize: Int
)

final case class KafkaConfig(
    bootstrapServers: String,
    topic: String,
    groupId: String
)

final case class TelemetryConfig(
    serviceName: String,
    endpoint: String,
    enabled: Boolean
)

final case class AppConfig(
    server: ServerConfig,
    database: DatabaseConfig,
    kafka: KafkaConfig,
    telemetry: TelemetryConfig
)

object AppConfig {
  
  val layer: ZLayer[Any, Throwable, AppConfig] =
    ZLayer {
      ZIO.attempt {
        val config = ConfigFactory.load()
        AppConfig(
          server = ServerConfig(
            host = config.getString("app.server.host"),
            port = config.getInt("app.server.port")
          ),
          database = DatabaseConfig(
            driver = config.getString("app.database.driver"),
            url = config.getString("app.database.url"),
            user = config.getString("app.database.user"),
            password = config.getString("app.database.password"),
            poolSize = config.getInt("app.database.pool-size")
          ),
          kafka = KafkaConfig(
            bootstrapServers = config.getString("app.kafka.bootstrap-servers"),
            topic = config.getString("app.kafka.topic"),
            groupId = config.getString("app.kafka.group-id")
          ),
          telemetry = TelemetryConfig(
            serviceName = config.getString("app.telemetry.service-name"),
            endpoint = config.getString("app.telemetry.endpoint"),
            enabled = config.getBoolean("app.telemetry.enabled")
          )
        )
      }
    }

  val serverLayer: ZLayer[AppConfig, Nothing, ServerConfig] =
    ZLayer.fromFunction((config: AppConfig) => config.server)

  val databaseLayer: ZLayer[AppConfig, Nothing, DatabaseConfig] =
    ZLayer.fromFunction((config: AppConfig) => config.database)

  val kafkaLayer: ZLayer[AppConfig, Nothing, KafkaConfig] =
    ZLayer.fromFunction((config: AppConfig) => config.kafka)

  val telemetryLayer: ZLayer[AppConfig, Nothing, TelemetryConfig] =
    ZLayer.fromFunction((config: AppConfig) => config.telemetry)
}
