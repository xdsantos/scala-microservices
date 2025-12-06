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
    topic: String
)

case class AppConfig(
    server: ServerConfig,
    database: DatabaseConfig,
    kafka: KafkaConfig
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
        topic = config.getString("app.kafka.topic")
      )
    )
  }
}

