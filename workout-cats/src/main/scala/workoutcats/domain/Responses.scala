package workoutcats.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import java.util.UUID

final case class WorkoutResponse(success: Boolean, data: Option[Workout], message: Option[String])
object WorkoutResponse {
  implicit val enc: Encoder[WorkoutResponse] = deriveEncoder
  implicit val dec: Decoder[WorkoutResponse] = deriveDecoder

  def success(workout: Workout): WorkoutResponse = WorkoutResponse(true, Some(workout), None)
  def error(msg: String): WorkoutResponse = WorkoutResponse(false, None, Some(msg))
}

final case class WorkoutListResponse(success: Boolean, data: List[Workout], total: Int, message: Option[String])
object WorkoutListResponse {
  implicit val enc: Encoder[WorkoutListResponse] = deriveEncoder
  implicit val dec: Decoder[WorkoutListResponse] = deriveDecoder

  def success(workouts: List[Workout]): WorkoutListResponse =
    WorkoutListResponse(true, workouts, workouts.size, None)

  def error(msg: String): WorkoutListResponse =
    WorkoutListResponse(false, Nil, 0, Some(msg))
}

final case class DeleteResponse(success: Boolean, message: String)
object DeleteResponse {
  implicit val enc: Encoder[DeleteResponse] = deriveEncoder
  implicit val dec: Decoder[DeleteResponse] = deriveDecoder
}

final case class AcceptedResponse(success: Boolean, correlationId: UUID, message: String)
object AcceptedResponse {
  implicit val enc: Encoder[AcceptedResponse] = deriveEncoder
  implicit val dec: Decoder[AcceptedResponse] = deriveDecoder

  def apply(correlationId: UUID): AcceptedResponse =
    AcceptedResponse(success = true, correlationId = correlationId, message = "Request accepted for processing")
}

