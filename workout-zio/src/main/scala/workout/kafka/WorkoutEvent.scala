package workout.kafka

import workout.domain.Workout
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
