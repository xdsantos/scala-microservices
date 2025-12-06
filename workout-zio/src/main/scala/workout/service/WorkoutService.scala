package workout.service

import workout.domain._
import zio._
import java.util.UUID

trait WorkoutService {
  def create(request: CreateWorkoutRequest): Task[Workout]
  def findById(id: UUID): Task[Option[Workout]]
  def findAll(limit: Int, offset: Int): Task[List[Workout]]
  def update(id: UUID, request: UpdateWorkoutRequest): Task[Option[Workout]]
  def delete(id: UUID): Task[Boolean]
  def findByType(workoutType: WorkoutType): Task[List[Workout]]
  def findByDifficulty(difficulty: Difficulty): Task[List[Workout]]
}

object WorkoutService {
  def create(request: CreateWorkoutRequest): ZIO[WorkoutService, Throwable, Workout] =
    ZIO.serviceWithZIO[WorkoutService](_.create(request))

  def findById(id: UUID): ZIO[WorkoutService, Throwable, Option[Workout]] =
    ZIO.serviceWithZIO[WorkoutService](_.findById(id))

  def findAll(limit: Int = 100, offset: Int = 0): ZIO[WorkoutService, Throwable, List[Workout]] =
    ZIO.serviceWithZIO[WorkoutService](_.findAll(limit, offset))

  def update(id: UUID, request: UpdateWorkoutRequest): ZIO[WorkoutService, Throwable, Option[Workout]] =
    ZIO.serviceWithZIO[WorkoutService](_.update(id, request))

  def delete(id: UUID): ZIO[WorkoutService, Throwable, Boolean] =
    ZIO.serviceWithZIO[WorkoutService](_.delete(id))

  def findByType(workoutType: WorkoutType): ZIO[WorkoutService, Throwable, List[Workout]] =
    ZIO.serviceWithZIO[WorkoutService](_.findByType(workoutType))

  def findByDifficulty(difficulty: Difficulty): ZIO[WorkoutService, Throwable, List[Workout]] =
    ZIO.serviceWithZIO[WorkoutService](_.findByDifficulty(difficulty))
}
