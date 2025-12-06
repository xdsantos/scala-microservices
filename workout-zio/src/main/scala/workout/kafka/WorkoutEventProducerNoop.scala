package workout.kafka

import workout.domain.Workout
import zio._
import java.util.UUID

final case class WorkoutEventProducerNoop() extends WorkoutEventProducer {
  override def publishCreated(workout: Workout): Task[Unit] =
    ZIO.logInfo(s"[NOOP] Would publish created event for workout ${workout.id}")

  override def publishUpdated(workout: Workout): Task[Unit] =
    ZIO.logInfo(s"[NOOP] Would publish updated event for workout ${workout.id}")

  override def publishDeleted(workoutId: UUID): Task[Unit] =
    ZIO.logInfo(s"[NOOP] Would publish deleted event for workout $workoutId")
}

object WorkoutEventProducerNoop {
  val layer: ZLayer[Any, Nothing, WorkoutEventProducer] =
    ZLayer.succeed(WorkoutEventProducerNoop())
}
