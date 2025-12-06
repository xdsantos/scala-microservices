package workout.config

import com.typesafe.config.{Config, ConfigFactory}

case class ServerConfig(
    host: String,
    port: Int
)

case class DatabaseConfig(
    driver: String,
    url: String,
    user: String,
    password: String,
    numThreads: Int
)

case class KafkaConfig(
    bootstrapServers: String,
    topic: String,
    commandTopic: String,
    consumerGroup: String
)

case class TracingConfig(
    enabled: Boolean,
    serviceName: String,
    endpoint: String
)

case class AppConfig(
    server: ServerConfig,
    database: DatabaseConfig,
    kafka: KafkaConfig,
    tracing: TracingConfig
)

object AppConfig {
  def load(): AppConfig = {
    val config = ConfigFactory.load()
    fromConfig(config)
  }

  def fromConfig(config: Config): AppConfig = {
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
        numThreads = config.getInt("app.database.numThreads")
      ),
      kafka = KafkaConfig(
        bootstrapServers = config.getString("app.kafka.bootstrap-servers"),
        topic = config.getString("app.kafka.topic"),
        commandTopic = config.getString("app.kafka.command-topic"),
        consumerGroup = config.getString("app.kafka.consumer-group")
      ),
      tracing = TracingConfig(
        enabled = config.getBoolean("app.tracing.enabled"),
        serviceName = config.getString("app.tracing.service-name"),
        endpoint = config.getString("app.tracing.endpoint")
      )
    )
  }
}

