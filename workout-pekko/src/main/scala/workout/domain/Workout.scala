package workout.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import java.time.Instant
import java.util.UUID

case class Workout(
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
  implicit val encoder: Encoder[Workout] = deriveEncoder[Workout]
  implicit val decoder: Decoder[Workout] = deriveDecoder[Workout]
}

