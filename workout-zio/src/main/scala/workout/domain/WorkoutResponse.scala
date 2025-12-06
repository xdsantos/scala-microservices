package workout.domain

import zio.json._

final case class WorkoutResponse(
    success: Boolean,
    data: Option[Workout],
    message: Option[String]
)

object WorkoutResponse {
  implicit val encoder: JsonEncoder[WorkoutResponse] = DeriveJsonEncoder.gen[WorkoutResponse]
  implicit val decoder: JsonDecoder[WorkoutResponse] = DeriveJsonDecoder.gen[WorkoutResponse]

  def success(workout: Workout): WorkoutResponse =
    WorkoutResponse(success = true, data = Some(workout), message = None)

  def error(msg: String): WorkoutResponse =
    WorkoutResponse(success = false, data = None, message = Some(msg))
}
