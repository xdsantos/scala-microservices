package workoutcats.config

import com.typesafe.config.ConfigFactory

final case class ServerConfig(host: String, port: Int)
final case class DatabaseConfig(url: String, user: String, password: String, driver: String, poolSize: Int)
final case class KafkaConfig(bootstrapServers: String, topic: String, commandTopic: String, groupId: String)
final case class TelemetryConfig(enabled: Boolean, serviceName: String, endpoint: String)
final case class AppConfig(
    server: ServerConfig,
    database: DatabaseConfig,
    kafka: KafkaConfig,
    telemetry: TelemetryConfig
)

object AppConfig {
  def load(): AppConfig = {
    val c = ConfigFactory.load().getConfig("app")
    AppConfig(
      server = ServerConfig(
        host = c.getConfig("server").getString("host"),
        port = c.getConfig("server").getInt("port")
      ),
      database = DatabaseConfig(
        url = c.getConfig("database").getString("url"),
        user = c.getConfig("database").getString("user"),
        password = c.getConfig("database").getString("password"),
        driver = c.getConfig("database").getString("driver"),
        poolSize = c.getConfig("database").getInt("poolSize")
      ),
      kafka = KafkaConfig(
        bootstrapServers = c.getConfig("kafka").getString("bootstrapServers"),
        topic = c.getConfig("kafka").getString("topic"),
        commandTopic = c.getConfig("kafka").getString("commandTopic"),
        groupId = c.getConfig("kafka").getString("groupId")
      ),
      telemetry = TelemetryConfig(
        enabled = c.getConfig("telemetry").getBoolean("enabled"),
        serviceName = c.getConfig("telemetry").getString("serviceName"),
        endpoint = c.getConfig("telemetry").getString("endpoint")
      )
    )
  }
}

