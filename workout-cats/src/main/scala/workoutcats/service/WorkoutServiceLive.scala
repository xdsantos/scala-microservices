package workoutcats.service

import workoutcats.domain._
import workoutcats.repository.WorkoutRepository
import workoutcats.kafka.{CreateWorkoutCommand, WorkoutEventProducer}
import workoutcats.telemetry.Tracing
import cats.effect.kernel.MonadCancelThrow
import cats.syntax.all._
import java.util.UUID
import io.opentelemetry.api.trace.SpanKind

final class WorkoutServiceLive[F[_]: MonadCancelThrow](
    repo: WorkoutRepository[F],
    producer: WorkoutEventProducer[F],
    tracing: Tracing[F]
) extends WorkoutService[F] {

  override def createAsync(req: CreateWorkoutRequest): F[UUID] =
    tracing.span("WorkoutService.createAsync", SpanKind.PRODUCER) {
      for {
        _ <- tracing.addAttribute("workout.name", req.name)
        _ <- tracing.addAttribute("workout.type", req.workoutType.toString)
        traceId <- tracing.currentTraceId
        spanId  <- tracing.currentSpanId
        command = (traceId, spanId) match {
          case (Some(t), Some(s)) => CreateWorkoutCommand(req, t, s)
          case _                  => CreateWorkoutCommand(req)
        }
        correlationId <- producer.publishCreateCommand(command)
        _ <- tracing.addAttribute("correlation.id", correlationId.toString)
      } yield correlationId
    }

  override def create(req: CreateWorkoutRequest): F[Workout] =
    tracing.span("WorkoutService.create") {
      for {
        _ <- tracing.addAttribute("workout.name", req.name)
        w <- repo.create(req)
        _ <- tracing.addAttribute("workout.id", w.id.toString)
        _ <- producer.publishCreated(w).handleErrorWith(e => tracing.logWarn(s"Kafka publish failed: ${e.getMessage}"))
      } yield w
    }

  override def findById(id: UUID): F[Option[Workout]] =
    tracing.span("WorkoutService.findById") {
      tracing.addAttribute("workout.id", id.toString) *> repo.findById(id)
    }

  override def findAll(limit: Int, offset: Int): F[List[Workout]] =
    tracing.rootSpan("WorkoutService.findAll", SpanKind.SERVER) {
      for {
        _ <- tracing.addAttribute("query.limit", limit)
        _ <- tracing.addAttribute("query.offset", offset)
        list <- repo.findAll(limit, offset)
        _ <- tracing.addAttribute("result.count", list.size)
      } yield list
    }

  override def update(id: UUID, req: UpdateWorkoutRequest): F[Option[Workout]] =
    tracing.span("WorkoutService.update") {
      for {
        _ <- tracing.addAttribute("workout.id", id.toString)
        res <- repo.update(id, req)
        _ <- res match {
          case Some(w) => producer.publishUpdated(w).handleErrorWith(e => tracing.logWarn(s"Kafka publish failed: ${e.getMessage}"))
          case None    => tracing.unit
        }
      } yield res
    }

  override def delete(id: UUID): F[Boolean] =
    tracing.span("WorkoutService.delete") {
      for {
        _ <- tracing.addAttribute("workout.id", id.toString)
        deleted <- repo.delete(id)
        _ <- tracing.addAttribute("workout.deleted", deleted)
        _ <- if (deleted) producer.publishDeleted(id).handleErrorWith(e => tracing.logWarn(s"Kafka publish failed: ${e.getMessage}")) else tracing.unit
      } yield deleted
    }

  override def findByType(wt: WorkoutType): F[List[Workout]] =
    tracing.span("WorkoutService.findByType") {
      for {
        _ <- tracing.addAttribute("workout.type", wt.toString)
        list <- repo.findByType(wt)
        _ <- tracing.addAttribute("result.count", list.size)
      } yield list
    }

  override def findByDifficulty(diff: Difficulty): F[List[Workout]] =
    tracing.span("WorkoutService.findByDifficulty") {
      for {
        _ <- tracing.addAttribute("workout.difficulty", diff.toString)
        list <- repo.findByDifficulty(diff)
        _ <- tracing.addAttribute("result.count", list.size)
      } yield list
    }
}

