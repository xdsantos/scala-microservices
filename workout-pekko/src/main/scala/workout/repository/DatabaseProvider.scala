package workout.repository

import slick.jdbc.PostgresProfile.api._
import workout.config.DatabaseConfig
import com.typesafe.scalalogging.LazyLogging

object DatabaseProvider extends LazyLogging {

  def createDatabase(config: DatabaseConfig): Database = {
    logger.info(s"Creating database connection pool for ${config.url}")
    
    Database.forURL(
      url = config.url,
      user = config.user,
      password = config.password,
      driver = config.driver,
      executor = AsyncExecutor(
        name = "workout-db-executor",
        minThreads = config.numThreads,
        maxThreads = config.numThreads,
        queueSize = 1000,
        maxConnections = config.numThreads
      )
    )
  }
}

