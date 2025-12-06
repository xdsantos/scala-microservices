package workout.repository

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import workout.config.DatabaseConfig
import zio._
import javax.sql.DataSource

object DataSourceLive {
  val layer: ZLayer[DatabaseConfig, Throwable, DataSource] =
    ZLayer.scoped {
      ZIO.serviceWithZIO[DatabaseConfig] { config =>
        ZIO.acquireRelease(
          ZIO.attemptBlocking {
            val hikariConfig = new HikariConfig()
            hikariConfig.setJdbcUrl(config.url)
            hikariConfig.setUsername(config.user)
            hikariConfig.setPassword(config.password)
            hikariConfig.setDriverClassName(config.driver)
            hikariConfig.setMaximumPoolSize(config.poolSize)
            hikariConfig.setMinimumIdle(2)
            hikariConfig.setConnectionTimeout(30000)
            hikariConfig.setIdleTimeout(600000)
            hikariConfig.setMaxLifetime(1800000)
            hikariConfig.setPoolName("workout-pool")
            new HikariDataSource(hikariConfig)
          }
        )(ds => ZIO.attemptBlocking(ds.close()).orDie)
      }
    }
}
