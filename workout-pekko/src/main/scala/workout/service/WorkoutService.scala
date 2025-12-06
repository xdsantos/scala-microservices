package workout.service

import workout.domain._
import scala.concurrent.Future
import java.util.UUID

trait WorkoutService {
  /** Async create - publishes to Kafka and returns correlationId */
  def createAsync(request: CreateWorkoutRequest): Future[UUID]
  /** Direct create - saves to DB (used by consumer) */
  def create(request: CreateWorkoutRequest): Future[Workout]
  def findById(id: UUID): Future[Option[Workout]]
  def findAll(limit: Int, offset: Int): Future[List[Workout]]
  def update(id: UUID, request: UpdateWorkoutRequest): Future[Option[Workout]]
  def delete(id: UUID): Future[Boolean]
  def findByType(workoutType: WorkoutType): Future[List[Workout]]
  def findByDifficulty(difficulty: Difficulty): Future[List[Workout]]
}

