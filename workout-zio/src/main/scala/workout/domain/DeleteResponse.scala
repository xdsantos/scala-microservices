package workout.domain

import zio.json._

final case class DeleteResponse(
    success: Boolean,
    message: String
)

object DeleteResponse {
  implicit val encoder: JsonEncoder[DeleteResponse] = DeriveJsonEncoder.gen[DeleteResponse]
  implicit val decoder: JsonDecoder[DeleteResponse] = DeriveJsonDecoder.gen[DeleteResponse]
}
