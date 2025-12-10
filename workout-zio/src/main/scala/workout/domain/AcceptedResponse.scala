package workout.domain

import zio.json._
import java.util.UUID

final case class AcceptedResponse(
    success: Boolean,
    correlationId: UUID,
    message: String
)

object AcceptedResponse {
  implicit val encoder: JsonEncoder[AcceptedResponse] = DeriveJsonEncoder.gen[AcceptedResponse]
  implicit val decoder: JsonDecoder[AcceptedResponse] = DeriveJsonDecoder.gen[AcceptedResponse]

  def apply(correlationId: UUID): AcceptedResponse =
    AcceptedResponse(
      success = true,
      correlationId = correlationId,
      message = "Request accepted for processing"
    )
}

