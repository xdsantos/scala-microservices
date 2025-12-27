package workoutcats.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

final case class UpdateWorkoutRequest(
    name: Option[String],
    description: Option[String],
    workoutType: Option[WorkoutType],
    durationMinutes: Option[Int],
    caloriesBurned: Option[Int],
    difficulty: Option[Difficulty]
)

object UpdateWorkoutRequest {
  implicit val enc: Encoder[UpdateWorkoutRequest] = deriveEncoder
  implicit val dec: Decoder[UpdateWorkoutRequest] = deriveDecoder
}

