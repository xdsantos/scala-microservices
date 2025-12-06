package workout.domain

import zio.json._
import java.time.Instant
import java.util.UUID

final case class Workout(
    id: UUID,
    name: String,
    description: Option[String],
    workoutType: WorkoutType,
    durationMinutes: Int,
    caloriesBurned: Option[Int],
    difficulty: Difficulty,
    createdAt: Instant,
    updatedAt: Instant
)

object Workout {
  implicit val encoder: JsonEncoder[Workout] = DeriveJsonEncoder.gen[Workout]
  implicit val decoder: JsonDecoder[Workout] = DeriveJsonDecoder.gen[Workout]
}
