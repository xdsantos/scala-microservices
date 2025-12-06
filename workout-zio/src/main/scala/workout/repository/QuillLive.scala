package workout.repository

import io.getquill._
import io.getquill.jdbczio.Quill
import zio._
import javax.sql.DataSource

object QuillLive {
  val layer: ZLayer[DataSource, Nothing, Quill.Postgres[SnakeCase]] =
    Quill.Postgres.fromNamingStrategy(SnakeCase)
}
