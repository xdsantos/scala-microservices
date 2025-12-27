package workoutcats.kafka

import workoutcats.domain.CreateWorkoutRequest
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import java.util.UUID

final case class CreateWorkoutCommand(
    correlationId: UUID,
    request: CreateWorkoutRequest,
    timestamp: Long,
    traceId: Option[String],
    spanId: Option[String]
)

object CreateWorkoutCommand {
  implicit val enc: Encoder[CreateWorkoutCommand] = deriveEncoder
  implicit val dec: Decoder[CreateWorkoutCommand] = deriveDecoder

  def apply(req: CreateWorkoutRequest): CreateWorkoutCommand =
    CreateWorkoutCommand(UUID.randomUUID(), req, System.currentTimeMillis(), None, None)

  def apply(req: CreateWorkoutRequest, traceId: String, spanId: String): CreateWorkoutCommand =
    CreateWorkoutCommand(UUID.randomUUID(), req, System.currentTimeMillis(), Some(traceId), Some(spanId))
}

