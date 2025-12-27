package workoutcats

import cats.effect.{IO, IOApp, Resource, ExitCode}
import cats.syntax.all._
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.implicits._
import doobie._
import doobie.hikari.HikariTransactor
import workoutcats.config._
import workoutcats.repository._
import workoutcats.service._
import workoutcats.kafka._
import workoutcats.telemetry._
import workoutcats.api.WorkoutRoutes
import org.flywaydb.core.Flyway
import fs2.kafka._
import scala.concurrent.ExecutionContext
import com.comcast.ip4s._

object Main extends IOApp {

  private def transactor(cfg: DatabaseConfig): Resource[IO, HikariTransactor[IO]] =
    for {
      ce <- ExecutionContexts.fixedThreadPool[IO](cfg.poolSize)
      xa <- HikariTransactor.newHikariTransactor[IO](
        cfg.driver,
        cfg.url,
        cfg.user,
        cfg.password,
        ce
      )
    } yield xa

  private def flywayMigrate(cfg: DatabaseConfig): IO[Unit] =
    IO.blocking {
      Flyway.configure().dataSource(cfg.url, cfg.user, cfg.password).load().migrate()
    }.void

  private def producerResource(cfg: KafkaConfig): Resource[IO, KafkaProducer[IO, String, String]] =
    KafkaProducer.resource(ProducerSettings[IO, String, String].withBootstrapServers(cfg.bootstrapServers))

  private def consumerResource(cfg: KafkaConfig): Resource[IO, KafkaConsumer[IO, String, String]] =
    KafkaConsumer.resource(
      ConsumerSettings[IO, String, String]
        .withBootstrapServers(cfg.bootstrapServers)
        .withGroupId(cfg.groupId)
        .withEnableAutoCommit(false)
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
    )

  override def run(args: List[String]): IO[ExitCode] = {
    val cfg = AppConfig.load()

    val app = for {
      _ <- Resource.eval(IO.println("============================================================"))
      _ <- Resource.eval(IO.println("   Workout Cats Effect Microservice - Starting Up"))
      _ <- Resource.eval(IO.println("============================================================"))
      _ <- Resource.eval(IO.println(s"Database URL: ${cfg.database.url}"))
      _ <- Resource.eval(IO.println(s"Kafka Bootstrap: ${cfg.kafka.bootstrapServers}"))
      _ <- Resource.eval(IO.println(s"Kafka Command Topic: ${cfg.kafka.commandTopic}"))
      _ <- Resource.eval(IO.println(s"Telemetry Enabled: ${cfg.telemetry.enabled}"))

      _ <- Resource.eval(flywayMigrate(cfg.database))

      xa <- transactor(cfg.database)
      tracing <- Tracing.live[IO](cfg.telemetry)
      producer <- producerResource(cfg.kafka)
      consumer <- consumerResource(cfg.kafka)

      repo = new WorkoutRepositoryDoobie[IO](xa)
      eventProducer = new WorkoutEventProducerFs2[IO](producer, cfg.kafka, tracing)
      service = new WorkoutServiceLive[IO](repo, eventProducer, tracing)
      consumerLogic = new WorkoutCommandConsumer[IO](consumer, service, tracing, cfg.kafka)

      routes = new WorkoutRoutes[IO](service).routes

      server <- EmberServerBuilder
        .default[IO]
        .withHost(Host.fromString(cfg.server.host).getOrElse(ipv4"0.0.0.0"))
        .withPort(Port.fromInt(cfg.server.port).getOrElse(port"8082"))
        .withHttpApp(Router("/" -> routes).orNotFound)
        .build

      _ <- Resource.make(
        consumerLogic.stream.compile.drain.start
      )(_.cancel)

    } yield server

    app.useForever.as(ExitCode.Success)
  }
}

