package workout.service

import workout.domain._
import workout.kafka.WorkoutEventProducer
import workout.repository.WorkoutRepository
import workout.telemetry.Tracing
import zio._
import java.util.UUID

final case class WorkoutServiceLive(
    repository: WorkoutRepository,
    eventProducer: WorkoutEventProducer,
    tracing: Tracing
) extends WorkoutService {

  override def create(request: CreateWorkoutRequest): Task[Workout] =
    tracing.spanZIO("create-workout") {
      for {
        workout <- repository.create(request)
        _ <- eventProducer.publishCreated(workout).catchAll(e =>
          ZIO.logWarning(s"Failed to publish Kafka event: ${e.getMessage}")
        )
      } yield workout
    }

  override def findById(id: UUID): Task[Option[Workout]] =
    tracing.spanZIO(s"find-workout-by-id-$id") {
      repository.findById(id)
    }

  override def findAll(limit: Int, offset: Int): Task[List[Workout]] =
    tracing.spanZIO("find-all-workouts") {
      repository.findAll(limit, offset)
    }

  override def update(id: UUID, request: UpdateWorkoutRequest): Task[Option[Workout]] =
    tracing.spanZIO(s"update-workout-$id") {
      for {
        result <- repository.update(id, request)
        _ <- result match {
          case Some(workout) =>
            eventProducer.publishUpdated(workout).catchAll(e =>
              ZIO.logWarning(s"Failed to publish Kafka event: ${e.getMessage}")
            )
          case None => ZIO.unit
        }
      } yield result
    }

  override def delete(id: UUID): Task[Boolean] =
    tracing.spanZIO(s"delete-workout-$id") {
      for {
        deleted <- repository.delete(id)
        _ <- eventProducer.publishDeleted(id).catchAll(e =>
          ZIO.logWarning(s"Failed to publish Kafka event: ${e.getMessage}")
        ).when(deleted)
      } yield deleted
    }

  override def findByType(workoutType: WorkoutType): Task[List[Workout]] =
    tracing.spanZIO(s"find-workouts-by-type-$workoutType") {
      repository.findByType(workoutType)
    }

  override def findByDifficulty(difficulty: Difficulty): Task[List[Workout]] =
    tracing.spanZIO(s"find-workouts-by-difficulty-$difficulty") {
      repository.findByDifficulty(difficulty)
    }
}

object WorkoutServiceLive {
  val layer: ZLayer[WorkoutRepository with WorkoutEventProducer with Tracing, Nothing, WorkoutService] =
    ZLayer.fromFunction(WorkoutServiceLive(_, _, _))
}
