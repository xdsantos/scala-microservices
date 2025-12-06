package workout.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class CreateWorkoutRequest(
    name: String,
    description: Option[String],
    workoutType: WorkoutType,
    durationMinutes: Int,
    caloriesBurned: Option[Int],
    difficulty: Difficulty
)

object CreateWorkoutRequest {
  implicit val encoder: Encoder[CreateWorkoutRequest] = deriveEncoder[CreateWorkoutRequest]
  implicit val decoder: Decoder[CreateWorkoutRequest] = deriveDecoder[CreateWorkoutRequest]
}

