package workout.repository

import org.flywaydb.core.Flyway
import workout.config.DatabaseConfig
import com.typesafe.scalalogging.LazyLogging

object FlywayMigration extends LazyLogging {

  def migrate(config: DatabaseConfig): Int = {
    logger.info(s"Running Flyway migrations against ${config.url}")
    
    val flyway = Flyway
      .configure()
      .dataSource(config.url, config.user, config.password)
      .locations("classpath:db/migration")
      .load()

    val result = flyway.migrate()
    logger.info(s"Applied ${result.migrationsExecuted} migration(s)")
    result.migrationsExecuted
  }

  def clean(config: DatabaseConfig): Unit = {
    logger.warn("Cleaning database - this will delete all data!")
    val flyway = Flyway
      .configure()
      .dataSource(config.url, config.user, config.password)
      .cleanDisabled(false)
      .load()

    flyway.clean()
  }

  def validate(config: DatabaseConfig): Unit = {
    val flyway = Flyway
      .configure()
      .dataSource(config.url, config.user, config.password)
      .locations("classpath:db/migration")
      .load()

    flyway.validate()
  }
}

