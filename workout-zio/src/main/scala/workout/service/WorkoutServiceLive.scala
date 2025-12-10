package workout.service

import workout.domain._
import workout.kafka.{CreateWorkoutCommand, WorkoutEventProducer}
import workout.repository.WorkoutRepository
import workout.telemetry.Tracing
import io.opentelemetry.api.trace.SpanKind
import zio._
import java.util.UUID

final case class WorkoutServiceLive(
    repository: WorkoutRepository,
    eventProducer: WorkoutEventProducer,
    tracing: Tracing
) extends WorkoutService {

  override def createAsync(request: CreateWorkoutRequest): Task[UUID] =
    tracing.spanZIOWithKind("WorkoutService.createAsync", SpanKind.PRODUCER) {
      for {
        _ <- tracing.addAttribute("workout.name", request.name)
        _ <- tracing.addAttribute("workout.type", request.workoutType.toString)
        _ <- tracing.addAttribute("messaging.system", "kafka")
        traceId <- tracing.getCurrentTraceId
        spanId <- tracing.getCurrentSpanId
        command = (traceId, spanId) match {
          case (Some(tid), Some(sid)) => CreateWorkoutCommand(request, tid, sid)
          case _ => CreateWorkoutCommand(request)
        }
        correlationId <- eventProducer.publishCreateCommand(command)
        _ <- tracing.addAttribute("correlation.id", correlationId.toString)
        _ <- ZIO.logInfo(s"Published create command with correlationId: $correlationId")
      } yield correlationId
    }

  override def create(request: CreateWorkoutRequest): Task[Workout] =
    tracing.spanZIO("WorkoutService.create") {
      for {
        _ <- tracing.addAttribute("workout.name", request.name)
        workout <- repository.create(request)
        _ <- tracing.addAttribute("workout.id", workout.id.toString)
        _ <- eventProducer.publishCreated(workout).catchAll(e =>
          ZIO.logWarning(s"Failed to publish Kafka event: ${e.getMessage}")
        )
      } yield workout
    }

  override def findById(id: UUID): Task[Option[Workout]] =
    tracing.spanZIO("WorkoutService.findById") {
      for {
        _ <- tracing.addAttribute("workout.id", id.toString)
        result <- repository.findById(id)
      } yield result
    }

  override def findAll(limit: Int, offset: Int): Task[List[Workout]] =
    tracing.spanZIORoot("WorkoutService.findAll", SpanKind.SERVER) {
      for {
        _ <- tracing.addAttributeLong("query.limit", limit.toLong)
        _ <- tracing.addAttributeLong("query.offset", offset.toLong)
        workouts <- repository.findAll(limit, offset)
        _ <- tracing.addAttributeLong("result.count", workouts.size.toLong)
      } yield workouts
    }

  override def update(id: UUID, request: UpdateWorkoutRequest): Task[Option[Workout]] =
    tracing.spanZIO("WorkoutService.update") {
      for {
        _ <- tracing.addAttribute("workout.id", id.toString)
        result <- repository.update(id, request)
        _ <- tracing.addAttributeBool("workout.found", result.isDefined)
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
    tracing.spanZIO("WorkoutService.delete") {
      for {
        _ <- tracing.addAttribute("workout.id", id.toString)
        deleted <- repository.delete(id)
        _ <- tracing.addAttributeBool("workout.deleted", deleted)
        _ <- eventProducer.publishDeleted(id).catchAll(e =>
          ZIO.logWarning(s"Failed to publish Kafka event: ${e.getMessage}")
        ).when(deleted)
      } yield deleted
    }

  override def findByType(workoutType: WorkoutType): Task[List[Workout]] =
    tracing.spanZIO("WorkoutService.findByType") {
      for {
        _ <- tracing.addAttribute("workout.type", workoutType.toString)
        workouts <- repository.findByType(workoutType)
        _ <- tracing.addAttributeLong("result.count", workouts.size.toLong)
      } yield workouts
    }

  override def findByDifficulty(difficulty: Difficulty): Task[List[Workout]] =
    tracing.spanZIO("WorkoutService.findByDifficulty") {
      for {
        _ <- tracing.addAttribute("workout.difficulty", difficulty.toString)
        workouts <- repository.findByDifficulty(difficulty)
        _ <- tracing.addAttributeLong("result.count", workouts.size.toLong)
      } yield workouts
    }
}

object WorkoutServiceLive {
  val layer: ZLayer[WorkoutRepository with WorkoutEventProducer with Tracing, Nothing, WorkoutService] =
    ZLayer.fromFunction(WorkoutServiceLive(_, _, _))
}
