package workoutcats.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

final case class CreateWorkoutRequest(
    name: String,
    description: Option[String],
    workoutType: WorkoutType,
    durationMinutes: Int,
    caloriesBurned: Option[Int],
    difficulty: Difficulty
)

object CreateWorkoutRequest {
  implicit val enc: Encoder[CreateWorkoutRequest] = deriveEncoder
  implicit val dec: Decoder[CreateWorkoutRequest] = deriveDecoder
}

