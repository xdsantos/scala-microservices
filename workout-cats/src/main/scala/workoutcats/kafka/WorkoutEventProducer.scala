package workoutcats.kafka

import workoutcats.domain.Workout
import cats.effect.kernel.MonadCancelThrow
import java.util.UUID

trait WorkoutEventProducer[F[_]] {
  def publishCreateCommand(cmd: CreateWorkoutCommand): F[UUID]
  def publishCreated(workout: Workout): F[Unit]
  def publishUpdated(workout: Workout): F[Unit]
  def publishDeleted(id: UUID): F[Unit]
}

