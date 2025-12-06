package workout.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class WorkoutResponse(
    success: Boolean,
    data: Option[Workout],
    message: Option[String]
)

object WorkoutResponse {
  implicit val encoder: Encoder[WorkoutResponse] = deriveEncoder[WorkoutResponse]
  implicit val decoder: Decoder[WorkoutResponse] = deriveDecoder[WorkoutResponse]

  def success(workout: Workout): WorkoutResponse =
    WorkoutResponse(success = true, data = Some(workout), message = None)

  def error(msg: String): WorkoutResponse =
    WorkoutResponse(success = false, data = None, message = Some(msg))
}

case class WorkoutListResponse(
    success: Boolean,
    data: List[Workout],
    total: Int,
    message: Option[String]
)

object WorkoutListResponse {
  implicit val encoder: Encoder[WorkoutListResponse] = deriveEncoder[WorkoutListResponse]
  implicit val decoder: Decoder[WorkoutListResponse] = deriveDecoder[WorkoutListResponse]

  def success(workouts: List[Workout]): WorkoutListResponse =
    WorkoutListResponse(success = true, data = workouts, total = workouts.size, message = None)

  def error(msg: String): WorkoutListResponse =
    WorkoutListResponse(success = false, data = List.empty, total = 0, message = Some(msg))
}

case class DeleteResponse(
    success: Boolean,
    message: String
)

object DeleteResponse {
  implicit val encoder: Encoder[DeleteResponse] = deriveEncoder[DeleteResponse]
  implicit val decoder: Decoder[DeleteResponse] = deriveDecoder[DeleteResponse]
}

