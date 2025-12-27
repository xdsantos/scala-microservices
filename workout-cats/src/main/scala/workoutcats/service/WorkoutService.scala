package workoutcats.service

import workoutcats.domain._
import cats.effect.kernel.MonadCancelThrow
import cats.syntax.all._
import java.util.UUID

trait WorkoutService[F[_]] {
  def createAsync(req: CreateWorkoutRequest): F[UUID]
  def create(req: CreateWorkoutRequest): F[Workout]
  def findById(id: UUID): F[Option[Workout]]
  def findAll(limit: Int, offset: Int): F[List[Workout]]
  def update(id: UUID, req: UpdateWorkoutRequest): F[Option[Workout]]
  def delete(id: UUID): F[Boolean]
  def findByType(wt: WorkoutType): F[List[Workout]]
  def findByDifficulty(diff: Difficulty): F[List[Workout]]
}

