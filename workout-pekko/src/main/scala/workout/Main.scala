package workout

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.http.scaladsl.Http
import workout.api.WorkoutRoutes
import workout.config.AppConfig
import workout.kafka.NoopEventProducer
import workout.repository.{DatabaseProvider, FlywayMigration, SlickWorkoutRepository}
import workout.service.WorkoutServiceImpl
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}

object Main extends App with LazyLogging {

  // Create actor system
  implicit val system: ActorSystem[Nothing] = ActorSystem(Behaviors.empty, "workout-system")
  implicit val ec: ExecutionContextExecutor = system.executionContext

  // Load configuration
  val config = AppConfig.load()

  logger.info("=" * 60)
  logger.info("   Workout Pekko Microservice - Starting Up")
  logger.info("=" * 60)
  logger.info(s"Database URL: ${config.database.url}")
  logger.info(s"Kafka Bootstrap: ${config.kafka.bootstrapServers}")

  // Run database migrations
  logger.info("Running database migrations...")
  val migrationsApplied = FlywayMigration.migrate(config.database)
  logger.info(s"Applied $migrationsApplied migration(s)")

  // Create database and repository
  val database = DatabaseProvider.createDatabase(config.database)
  val repository = new SlickWorkoutRepository(database)

  // Create Kafka producer (using noop for now)
  val eventProducer = new NoopEventProducer()
  // For real Kafka: val eventProducer = new PekkoKafkaProducer(config.kafka)

  // Create service and routes
  val service = new WorkoutServiceImpl(repository, eventProducer)
  val routes = new WorkoutRoutes(service)

  // Start HTTP server
  val bindingFuture = Http()
    .newServerAt(config.server.host, config.server.port)
    .bind(routes.routes)

  bindingFuture.onComplete {
    case Success(binding) =>
      val address = binding.localAddress
      logger.info(s"Server started at http://${address.getHostString}:${address.getPort}/")
      logger.info("Press ENTER to stop...")
    case Failure(ex) =>
      logger.error(s"Failed to bind HTTP server: ${ex.getMessage}", ex)
      system.terminate()
  }

  // Shutdown hook
  sys.addShutdownHook {
    logger.info("Shutting down...")
    bindingFuture
      .flatMap(_.unbind())
      .onComplete { _ =>
        database.close()
        system.terminate()
        logger.info("Shutdown complete")
      }
  }
}

