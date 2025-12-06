package workout.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class UpdateWorkoutRequest(
    name: Option[String],
    description: Option[String],
    workoutType: Option[WorkoutType],
    durationMinutes: Option[Int],
    caloriesBurned: Option[Int],
    difficulty: Option[Difficulty]
)

object UpdateWorkoutRequest {
  implicit val encoder: Encoder[UpdateWorkoutRequest] = deriveEncoder[UpdateWorkoutRequest]
  implicit val decoder: Decoder[UpdateWorkoutRequest] = deriveDecoder[UpdateWorkoutRequest]
}

