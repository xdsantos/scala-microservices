package workout.kafka

import workout.domain.Workout
import scala.concurrent.Future
import java.util.UUID

trait WorkoutEventProducer {
  def publishCreated(workout: Workout): Future[Unit]
  def publishUpdated(workout: Workout): Future[Unit]
  def publishDeleted(workoutId: UUID): Future[Unit]
}

