package workout.kafka

import workout.domain.{CreateWorkoutRequest, Workout}
import scala.concurrent.Future
import java.util.UUID

trait WorkoutEventProducer {
  def publishCreateCommand(command: CreateWorkoutCommand): Future[UUID]
  def publishCreated(workout: Workout): Future[Unit]
  def publishUpdated(workout: Workout): Future[Unit]
  def publishDeleted(workoutId: UUID): Future[Unit]
}

