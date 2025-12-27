package workoutcats.repository

import cats.effect.kernel.Sync
import cats.effect.kernel.MonadCancelThrow
import workoutcats.domain._
import java.time.Instant
import java.util.UUID

trait WorkoutRepository[F[_]] {
  def create(req: CreateWorkoutRequest): F[Workout]
  def findById(id: UUID): F[Option[Workout]]
  def findAll(limit: Int, offset: Int): F[List[Workout]]
  def update(id: UUID, req: UpdateWorkoutRequest): F[Option[Workout]]
  def delete(id: UUID): F[Boolean]
  def findByType(workoutType: WorkoutType): F[List[Workout]]
  def findByDifficulty(difficulty: Difficulty): F[List[Workout]]
}

