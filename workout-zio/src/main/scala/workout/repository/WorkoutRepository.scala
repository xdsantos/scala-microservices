package workout.repository

import workout.domain._
import zio._
import java.util.UUID

trait WorkoutRepository {
  def create(request: CreateWorkoutRequest): Task[Workout]
  def findById(id: UUID): Task[Option[Workout]]
  def findAll(limit: Int, offset: Int): Task[List[Workout]]
  def update(id: UUID, request: UpdateWorkoutRequest): Task[Option[Workout]]
  def delete(id: UUID): Task[Boolean]
  def findByType(workoutType: WorkoutType): Task[List[Workout]]
  def findByDifficulty(difficulty: Difficulty): Task[List[Workout]]
}

object WorkoutRepository {
  def create(request: CreateWorkoutRequest): ZIO[WorkoutRepository, Throwable, Workout] =
    ZIO.serviceWithZIO[WorkoutRepository](_.create(request))

  def findById(id: UUID): ZIO[WorkoutRepository, Throwable, Option[Workout]] =
    ZIO.serviceWithZIO[WorkoutRepository](_.findById(id))

  def findAll(limit: Int = 100, offset: Int = 0): ZIO[WorkoutRepository, Throwable, List[Workout]] =
    ZIO.serviceWithZIO[WorkoutRepository](_.findAll(limit, offset))

  def update(id: UUID, request: UpdateWorkoutRequest): ZIO[WorkoutRepository, Throwable, Option[Workout]] =
    ZIO.serviceWithZIO[WorkoutRepository](_.update(id, request))

  def delete(id: UUID): ZIO[WorkoutRepository, Throwable, Boolean] =
    ZIO.serviceWithZIO[WorkoutRepository](_.delete(id))

  def findByType(workoutType: WorkoutType): ZIO[WorkoutRepository, Throwable, List[Workout]] =
    ZIO.serviceWithZIO[WorkoutRepository](_.findByType(workoutType))

  def findByDifficulty(difficulty: Difficulty): ZIO[WorkoutRepository, Throwable, List[Workout]] =
    ZIO.serviceWithZIO[WorkoutRepository](_.findByDifficulty(difficulty))
}
