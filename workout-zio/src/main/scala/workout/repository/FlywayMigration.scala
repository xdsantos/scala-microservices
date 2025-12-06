package workout.repository

import workout.config.DatabaseConfig
import zio._

trait FlywayMigration {
  def migrate: Task[Int]
  def clean: Task[Unit]
  def validate: Task[Unit]
}

object FlywayMigration {
  def migrate: ZIO[FlywayMigration, Throwable, Int] =
    ZIO.serviceWithZIO[FlywayMigration](_.migrate)

  def clean: ZIO[FlywayMigration, Throwable, Unit] =
    ZIO.serviceWithZIO[FlywayMigration](_.clean)

  def validate: ZIO[FlywayMigration, Throwable, Unit] =
    ZIO.serviceWithZIO[FlywayMigration](_.validate)
}
