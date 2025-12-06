package workout.repository

import org.flywaydb.core.Flyway
import workout.config.DatabaseConfig
import zio._

final case class FlywayMigrationLive(config: DatabaseConfig) extends FlywayMigration {
  private lazy val flyway: Flyway = Flyway
    .configure()
    .dataSource(config.url, config.user, config.password)
    .locations("classpath:db/migration")
    .load()

  override def migrate: Task[Int] =
    ZIO.attemptBlocking {
      flyway.migrate().migrationsExecuted
    }

  override def clean: Task[Unit] =
    ZIO.attemptBlocking {
      flyway.clean()
    }

  override def validate: Task[Unit] =
    ZIO.attemptBlocking {
      flyway.validate()
    }
}

object FlywayMigrationLive {
  val layer: ZLayer[DatabaseConfig, Nothing, FlywayMigration] =
    ZLayer.fromFunction(FlywayMigrationLive(_))
}
