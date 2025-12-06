package workout

import workout.api.WorkoutRoutes
import workout.config._
import workout.kafka._
import workout.repository._
import workout.service._
import workout.telemetry._
import zio._
import zio.http._
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault {

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private val serverProgram: ZIO[ServerConfig with WorkoutService, Throwable, Unit] = for {
    config <- ZIO.service[ServerConfig]
    _ <- ZIO.logInfo(s"Starting Workout Service on ${config.host}:${config.port}")
    app = (WorkoutRoutes.routes @@ WorkoutRoutes.middleware).toHttpApp
    _ <- Server
      .serve(app)
      .provideSome[WorkoutService](Server.defaultWith(_.binding(config.host, config.port)))
  } yield ()

  private val program: ZIO[AppConfig, Throwable, Unit] = for {
    config <- ZIO.service[AppConfig]

    // Log startup info
    _ <- ZIO.logInfo("=" * 60)
    _ <- ZIO.logInfo("   Workout Microservice - Starting Up")
    _ <- ZIO.logInfo("=" * 60)
    _ <- ZIO.logInfo(s"Database URL: ${config.database.url}")
    _ <- ZIO.logInfo(s"Kafka Bootstrap: ${config.kafka.bootstrapServers}")
    _ <- ZIO.logInfo(s"Telemetry Enabled: ${config.telemetry.enabled}")

    // Run Flyway migrations
    _ <- ZIO.logInfo("Running database migrations...")
    migrationCount <- FlywayMigration.migrate
      .provide(
        FlywayMigrationLive.layer,
        ZLayer.succeed(config.database)
      )
    _ <- ZIO.logInfo(s"Applied $migrationCount migration(s)")

    // Start server with all dependencies
    _ <- ZIO.logInfo(s"Starting HTTP server on ${config.server.host}:${config.server.port}...")
    _ <- serverProgram.provide(
      // Server config
      ZLayer.succeed(config.server),

      // Database layers
      ZLayer.succeed(config.database),
      DataSourceLive.layer,
      QuillLive.layer,
      WorkoutRepositoryLive.layer,

      // Kafka - use noop if not available
      WorkoutEventProducerNoop.layer,

      // Telemetry
      TracingNoop.layer,

      // Service layer
      WorkoutServiceLive.layer
    )
  } yield ()

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] =
    program
      .provide(AppConfig.layer)
      .tapError(e => ZIO.logError(s"Application failed: ${e.getMessage}"))
      .exitCode
}
