package workout.kafka

import workout.domain.Workout
import zio._
import java.util.UUID

trait WorkoutEventProducer {
  def publishCreated(workout: Workout): Task[Unit]
  def publishUpdated(workout: Workout): Task[Unit]
  def publishDeleted(workoutId: UUID): Task[Unit]
}

object WorkoutEventProducer {
  def publishCreated(workout: Workout): ZIO[WorkoutEventProducer, Throwable, Unit] =
    ZIO.serviceWithZIO[WorkoutEventProducer](_.publishCreated(workout))

  def publishUpdated(workout: Workout): ZIO[WorkoutEventProducer, Throwable, Unit] =
    ZIO.serviceWithZIO[WorkoutEventProducer](_.publishUpdated(workout))

  def publishDeleted(workoutId: UUID): ZIO[WorkoutEventProducer, Throwable, Unit] =
    ZIO.serviceWithZIO[WorkoutEventProducer](_.publishDeleted(workoutId))
}
