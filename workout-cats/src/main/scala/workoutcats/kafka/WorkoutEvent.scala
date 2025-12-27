package workoutcats.kafka

import workoutcats.domain.Workout
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import java.util.UUID

final case class WorkoutEvent(
    eventType: WorkoutEventType,
    workoutId: UUID,
    workout: Option[Workout],
    timestamp: Long
)

object WorkoutEvent {
  implicit val enc: Encoder[WorkoutEvent] = deriveEncoder
  implicit val dec: Decoder[WorkoutEvent] = deriveDecoder

  def created(workout: Workout): WorkoutEvent =
    WorkoutEvent(WorkoutEventType.Created, workout.id, Some(workout), System.currentTimeMillis())
  def updated(workout: Workout): WorkoutEvent =
    WorkoutEvent(WorkoutEventType.Updated, workout.id, Some(workout), System.currentTimeMillis())
  def deleted(id: UUID): WorkoutEvent =
    WorkoutEvent(WorkoutEventType.Deleted, id, None, System.currentTimeMillis())
}

