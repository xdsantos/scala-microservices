package workout

import workout.api.WorkoutRoutes
import workout.config._
import workout.kafka._
import workout.repository._
import workout.service._
import workout.telemetry._
import zio._
import zio.http._
import zio.kafka.consumer._
import zio.kafka.producer._
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault {

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private def producerLayer(bootstrapServers: String): ZLayer[Any, Throwable, Producer] =
    ZLayer.scoped(Producer.make(ProducerSettings(List(bootstrapServers))))

  private def consumerLayer(bootstrapServers: String, groupId: String): ZLayer[Any, Throwable, Consumer] =
    ZLayer.scoped(Consumer.make(
      ConsumerSettings(List(bootstrapServers))
        .withGroupId(groupId)
        .withOffsetRetrieval(Consumer.OffsetRetrieval.Auto(Consumer.AutoOffsetStrategy.Earliest))
    ))

  private val program: ZIO[AppConfig with Scope, Throwable, Unit] = for {
    config <- ZIO.service[AppConfig]

    // Log startup info
    _ <- ZIO.logInfo("=" * 60)
    _ <- ZIO.logInfo("   Workout ZIO Microservice - Starting Up")
    _ <- ZIO.logInfo("=" * 60)
    _ <- ZIO.logInfo(s"Database URL: ${config.database.url}")
    _ <- ZIO.logInfo(s"Kafka Bootstrap: ${config.kafka.bootstrapServers}")
    _ <- ZIO.logInfo(s"Kafka Command Topic: ${config.kafka.commandTopic}")
    _ <- ZIO.logInfo(s"Telemetry Enabled: ${config.telemetry.enabled}")

    // Run Flyway migrations
    _ <- ZIO.logInfo("Running database migrations...")
    migrationCount <- FlywayMigration.migrate
      .provide(
        FlywayMigrationLive.layer,
        ZLayer.succeed(config.database)
      )
    _ <- ZIO.logInfo(s"Applied $migrationCount migration(s)")

    // Build layers
    dbLayer = ZLayer.succeed(config.database) >>> DataSourceLive.layer >>> QuillLive.layer >>> WorkoutRepositoryLive.layer
    kafkaProducerLayer = producerLayer(config.kafka.bootstrapServers) ++ ZLayer.succeed(config.kafka) >>> WorkoutEventProducerLive.layer
    tracingLayer = ZLayer.succeed(config.telemetry) >>> TracingLive.layer
    
    serviceLayer = (dbLayer ++ kafkaProducerLayer ++ tracingLayer) >>> WorkoutServiceLive.layer

    kafkaConsumerLayer = consumerLayer(config.kafka.bootstrapServers, config.kafka.groupId)
    consumerAppLayer = (kafkaConsumerLayer ++ ZLayer.succeed(config.kafka) ++ serviceLayer ++ tracingLayer) >>> WorkoutCommandConsumerLive.layer

    // Start consumer in the background
    _ <- ZIO.logInfo("Starting Kafka command consumer...")
    _ <- WorkoutCommandConsumer.start
      .provide(consumerAppLayer)
      .forkScoped

    // Start HTTP server
    _ <- ZIO.logInfo(s"Starting HTTP server on ${config.server.host}:${config.server.port}...")
    app = (WorkoutRoutes.routes @@ WorkoutRoutes.middleware).toHttpApp
    _ <- Server
      .serve(app)
      .provide(
        Server.defaultWith(_.binding(config.server.host, config.server.port)),
        serviceLayer
      )
  } yield ()

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] =
    program
      .provideSome[Scope](AppConfig.layer)
      .tapError(e => ZIO.logError(s"Application failed: ${e.getMessage}"))
      .exitCode
}
