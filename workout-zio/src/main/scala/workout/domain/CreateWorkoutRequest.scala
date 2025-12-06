package workout.domain

import zio.json._

final case class CreateWorkoutRequest(
    name: String,
    description: Option[String],
    workoutType: WorkoutType,
    durationMinutes: Int,
    caloriesBurned: Option[Int],
    difficulty: Difficulty
)

object CreateWorkoutRequest {
  implicit val encoder: JsonEncoder[CreateWorkoutRequest] = DeriveJsonEncoder.gen[CreateWorkoutRequest]
  implicit val decoder: JsonDecoder[CreateWorkoutRequest] = DeriveJsonDecoder.gen[CreateWorkoutRequest]
}
