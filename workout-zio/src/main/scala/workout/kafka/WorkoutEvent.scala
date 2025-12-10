package workout.kafka

import workout.domain.{CreateWorkoutRequest, Workout}
import zio.json._
import java.util.UUID

final case class WorkoutEvent(
    eventType: WorkoutEventType,
    workoutId: UUID,
    workout: Option[Workout],
    timestamp: Long
)

object WorkoutEvent {
  implicit val encoder: JsonEncoder[WorkoutEvent] = DeriveJsonEncoder.gen[WorkoutEvent]
  implicit val decoder: JsonDecoder[WorkoutEvent] = DeriveJsonDecoder.gen[WorkoutEvent]

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
final case class CreateWorkoutCommand(
    correlationId: UUID,
    request: CreateWorkoutRequest,
    timestamp: Long,
    traceId: Option[String],
    spanId: Option[String]
)

object CreateWorkoutCommand {
  implicit val encoder: JsonEncoder[CreateWorkoutCommand] = DeriveJsonEncoder.gen[CreateWorkoutCommand]
  implicit val decoder: JsonDecoder[CreateWorkoutCommand] = DeriveJsonDecoder.gen[CreateWorkoutCommand]

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
