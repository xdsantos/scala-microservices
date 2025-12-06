package workout.kafka

import workout.domain.Workout
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.Future
import java.util.UUID

class NoopEventProducer extends WorkoutEventProducer with LazyLogging {

  override def publishCreateCommand(command: CreateWorkoutCommand): Future[UUID] = {
    logger.info(s"[NOOP] Would publish create command with correlationId ${command.correlationId}")
    Future.successful(command.correlationId)
  }

  override def publishCreated(workout: Workout): Future[Unit] = {
    logger.info(s"[NOOP] Would publish created event for workout ${workout.id}")
    Future.successful(())
  }

  override def publishUpdated(workout: Workout): Future[Unit] = {
    logger.info(s"[NOOP] Would publish updated event for workout ${workout.id}")
    Future.successful(())
  }

  override def publishDeleted(workoutId: UUID): Future[Unit] = {
    logger.info(s"[NOOP] Would publish deleted event for workout $workoutId")
    Future.successful(())
  }
}

