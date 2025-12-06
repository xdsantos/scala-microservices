package workout.domain

import zio.json._

final case class WorkoutListResponse(
    success: Boolean,
    data: List[Workout],
    total: Int,
    message: Option[String]
)

object WorkoutListResponse {
  implicit val encoder: JsonEncoder[WorkoutListResponse] = DeriveJsonEncoder.gen[WorkoutListResponse]
  implicit val decoder: JsonDecoder[WorkoutListResponse] = DeriveJsonDecoder.gen[WorkoutListResponse]

  def success(workouts: List[Workout]): WorkoutListResponse =
    WorkoutListResponse(success = true, data = workouts, total = workouts.size, message = None)

  def error(msg: String): WorkoutListResponse =
    WorkoutListResponse(success = false, data = List.empty, total = 0, message = Some(msg))
}
