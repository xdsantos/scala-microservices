package workout.service

import workout.domain._
import workout.repository.WorkoutRepository
import workout.kafka.WorkoutEventProducer
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID

class WorkoutServiceImpl(
    repository: WorkoutRepository,
    eventProducer: WorkoutEventProducer
)(implicit ec: ExecutionContext)
    extends WorkoutService
    with LazyLogging {

  override def create(request: CreateWorkoutRequest): Future[Workout] = {
    logger.debug(s"Creating workout: ${request.name}")
    for {
      workout <- repository.create(request)
      _ <- eventProducer.publishCreated(workout).recover { case ex =>
        logger.warn(s"Failed to publish created event: ${ex.getMessage}")
      }
    } yield {
      logger.info(s"Created workout: ${workout.id}")
      workout
    }
  }

  override def findById(id: UUID): Future[Option[Workout]] = {
    logger.debug(s"Finding workout by id: $id")
    repository.findById(id)
  }

  override def findAll(limit: Int, offset: Int): Future[List[Workout]] = {
    logger.debug(s"Finding all workouts with limit=$limit, offset=$offset")
    repository.findAll(limit, offset)
  }

  override def update(id: UUID, request: UpdateWorkoutRequest): Future[Option[Workout]] = {
    logger.debug(s"Updating workout: $id")
    for {
      result <- repository.update(id, request)
      _ <- result match {
        case Some(workout) =>
          eventProducer.publishUpdated(workout).recover { case ex =>
            logger.warn(s"Failed to publish updated event: ${ex.getMessage}")
          }
        case None =>
          Future.successful(())
      }
    } yield {
      result.foreach(w => logger.info(s"Updated workout: ${w.id}"))
      result
    }
  }

  override def delete(id: UUID): Future[Boolean] = {
    logger.debug(s"Deleting workout: $id")
    for {
      deleted <- repository.delete(id)
      _ <- if (deleted) {
        eventProducer.publishDeleted(id).recover { case ex =>
          logger.warn(s"Failed to publish deleted event: ${ex.getMessage}")
        }
      } else {
        Future.successful(())
      }
    } yield {
      if (deleted) logger.info(s"Deleted workout: $id")
      deleted
    }
  }

  override def findByType(workoutType: WorkoutType): Future[List[Workout]] = {
    logger.debug(s"Finding workouts by type: ${workoutType.value}")
    repository.findByType(workoutType)
  }

  override def findByDifficulty(difficulty: Difficulty): Future[List[Workout]] = {
    logger.debug(s"Finding workouts by difficulty: ${difficulty.value}")
    repository.findByDifficulty(difficulty)
  }
}

