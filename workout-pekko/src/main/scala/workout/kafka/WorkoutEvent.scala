package workout.kafka

import workout.domain.{CreateWorkoutRequest, Workout}
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import java.util.UUID

sealed trait WorkoutEventType {
  def value: String
}

object WorkoutEventType {
  case object CreateRequested extends WorkoutEventType { val value = "CreateRequested" }
  case object Created extends WorkoutEventType { val value = "Created" }
  case object Updated extends WorkoutEventType { val value = "Updated" }
  case object Deleted extends WorkoutEventType { val value = "Deleted" }

  implicit val encoder: Encoder[WorkoutEventType] = Encoder.encodeString.contramap(_.value)
  implicit val decoder: Decoder[WorkoutEventType] = Decoder.decodeString.emap {
    case "CreateRequested" => Right(CreateRequested)
    case "Created" => Right(Created)
    case "Updated" => Right(Updated)
    case "Deleted" => Right(Deleted)
    case other => Left(s"Invalid event type: $other")
  }
}

case class WorkoutEvent(
    eventType: WorkoutEventType,
    workoutId: UUID,
    workout: Option[Workout],
    timestamp: Long
)

object WorkoutEvent {
  implicit val encoder: Encoder[WorkoutEvent] = deriveEncoder[WorkoutEvent]
  implicit val decoder: Decoder[WorkoutEvent] = deriveDecoder[WorkoutEvent]

  def created(workout: Workout): WorkoutEvent =
    WorkoutEvent(
      eventType = WorkoutEventType.Created,
      workoutId = workout.id,
      workout = Some(workout),
      timestamp = System.currentTimeMillis()
    )

  def updated(workout: Workout): WorkoutEvent =
    WorkoutEvent(
      eventType = WorkoutEventType.Updated,
      workoutId = workout.id,
      workout = Some(workout),
      timestamp = System.currentTimeMillis()
    )

  def deleted(workoutId: UUID): WorkoutEvent =
    WorkoutEvent(
      eventType = WorkoutEventType.Deleted,
      workoutId = workoutId,
      workout = None,
      timestamp = System.currentTimeMillis()
    )
}

// Command to create a workout (sent to Kafka for async processing)
case class CreateWorkoutCommand(
    correlationId: UUID,
    request: CreateWorkoutRequest,
    timestamp: Long,
    traceId: Option[String] = None,
    spanId: Option[String] = None
)

object CreateWorkoutCommand {
  implicit val encoder: Encoder[CreateWorkoutCommand] = deriveEncoder[CreateWorkoutCommand]
  implicit val decoder: Decoder[CreateWorkoutCommand] = deriveDecoder[CreateWorkoutCommand]

  def apply(request: CreateWorkoutRequest): CreateWorkoutCommand =
    CreateWorkoutCommand(
      correlationId = UUID.randomUUID(),
      request = request,
      timestamp = System.currentTimeMillis(),
      traceId = None,
      spanId = None
    )

  def apply(request: CreateWorkoutRequest, traceId: String, spanId: String): CreateWorkoutCommand =
    CreateWorkoutCommand(
      correlationId = UUID.randomUUID(),
      request = request,
      timestamp = System.currentTimeMillis(),
      traceId = Some(traceId),
      spanId = Some(spanId)
    )
}

