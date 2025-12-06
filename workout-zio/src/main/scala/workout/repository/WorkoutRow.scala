package workout.repository

import workout.domain._
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

final case class WorkoutRow(
    id: UUID,
    name: String,
    description: Option[String],
    workoutType: String,
    durationMinutes: Int,
    caloriesBurned: Option[Int],
    difficulty: String,
    createdAt: Timestamp,
    updatedAt: Timestamp
)

object WorkoutRow {
  def fromDomain(workout: Workout): WorkoutRow =
    WorkoutRow(
      id = workout.id,
      name = workout.name,
      description = workout.description,
      workoutType = workout.workoutType.toString,
      durationMinutes = workout.durationMinutes,
      caloriesBurned = workout.caloriesBurned,
      difficulty = workout.difficulty.toString,
      createdAt = Timestamp.from(workout.createdAt),
      updatedAt = Timestamp.from(workout.updatedAt)
    )

  def toDomain(row: WorkoutRow): Either[String, Workout] =
    for {
      workoutType <- WorkoutType.fromString(row.workoutType)
      difficulty <- Difficulty.fromString(row.difficulty)
    } yield Workout(
      id = row.id,
      name = row.name,
      description = row.description,
      workoutType = workoutType,
      durationMinutes = row.durationMinutes,
      caloriesBurned = row.caloriesBurned,
      difficulty = difficulty,
      createdAt = row.createdAt.toInstant,
      updatedAt = row.updatedAt.toInstant
    )
}
