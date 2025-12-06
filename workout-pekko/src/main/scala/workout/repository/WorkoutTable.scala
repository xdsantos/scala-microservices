package workout.repository

import slick.jdbc.PostgresProfile.api._
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

// Custom type mappers for Slick
trait SlickTypeMappers {
  implicit val instantColumnType: BaseColumnType[Instant] =
    MappedColumnType.base[Instant, Timestamp](
      instant => Timestamp.from(instant),
      timestamp => timestamp.toInstant
    )
  
  // Note: PostgresProfile already has native UUID support, no custom mapper needed
}

object SlickTypeMappers extends SlickTypeMappers

case class WorkoutRow(
    id: UUID,
    name: String,
    description: Option[String],
    workoutType: String,
    durationMinutes: Int,
    caloriesBurned: Option[Int],
    difficulty: String,
    createdAt: Instant,
    updatedAt: Instant
)

class WorkoutTable(tag: Tag) extends Table[WorkoutRow](tag, "workouts") with SlickTypeMappers {
  def id = column[UUID]("id", O.PrimaryKey)
  def name = column[String]("name")
  def description = column[Option[String]]("description")
  def workoutType = column[String]("workout_type")
  def durationMinutes = column[Int]("duration_minutes")
  def caloriesBurned = column[Option[Int]]("calories_burned")
  def difficulty = column[String]("difficulty")
  def createdAt = column[Instant]("created_at")
  def updatedAt = column[Instant]("updated_at")

  def * = (id, name, description, workoutType, durationMinutes, caloriesBurned, difficulty, createdAt, updatedAt)
    .mapTo[WorkoutRow]
}

object WorkoutTable extends SlickTypeMappers {
  val workouts = TableQuery[WorkoutTable]
}

