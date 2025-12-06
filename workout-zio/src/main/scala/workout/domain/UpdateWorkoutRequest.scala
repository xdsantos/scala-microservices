package workout.domain

import zio.json._

final case class UpdateWorkoutRequest(
    name: Option[String],
    description: Option[String],
    workoutType: Option[WorkoutType],
    durationMinutes: Option[Int],
    caloriesBurned: Option[Int],
    difficulty: Option[Difficulty]
)

object UpdateWorkoutRequest {
  implicit val encoder: JsonEncoder[UpdateWorkoutRequest] = DeriveJsonEncoder.gen[UpdateWorkoutRequest]
  implicit val decoder: JsonDecoder[UpdateWorkoutRequest] = DeriveJsonDecoder.gen[UpdateWorkoutRequest]
}
