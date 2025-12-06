package workout.service

import workout.domain._
import workout.repository.WorkoutRepository
import workout.kafka.{CreateWorkoutCommand, WorkoutEventProducer}
import workout.tracing.Tracing
import io.opentelemetry.api.trace.SpanKind
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID

class WorkoutServiceImpl(
    repository: WorkoutRepository,
    eventProducer: WorkoutEventProducer
)(implicit ec: ExecutionContext)
    extends WorkoutService
    with LazyLogging {

  override def createAsync(request: CreateWorkoutRequest): Future[UUID] = {
    Tracing.withSpanAsync("WorkoutService.createAsync", SpanKind.PRODUCER) { span =>
      span.setAttribute("workout.name", request.name)
      span.setAttribute("workout.type", request.workoutType.value)
      span.setAttribute("messaging.system", "kafka")
      span.setAttribute("messaging.operation", "publish")

      logger.debug(s"Creating async workout request: ${request.name}")
      
      // Capture trace context to propagate through Kafka
      val traceId = Tracing.getCurrentTraceId.getOrElse("")
      val spanId = Tracing.getCurrentSpanId.getOrElse("")
      
      val command = if (traceId.nonEmpty && spanId.nonEmpty) {
        CreateWorkoutCommand(request, traceId, spanId)
      } else {
        CreateWorkoutCommand(request)
      }
      
      eventProducer.publishCreateCommand(command).map { correlationId =>
        span.setAttribute("correlation.id", correlationId.toString)
        span.setAttribute("trace.propagated", traceId.nonEmpty)
        logger.info(s"Published create command with correlationId: $correlationId, traceId: $traceId")
        correlationId
      }
    }
  }

  override def create(request: CreateWorkoutRequest): Future[Workout] = {
    Tracing.withSpanAsync("WorkoutService.create", SpanKind.INTERNAL) { span =>
      span.setAttribute("workout.name", request.name)
      span.setAttribute("workout.type", request.workoutType.value)

      logger.debug(s"Creating workout: ${request.name}")
      for {
        workout <- repository.create(request)
        _ <- eventProducer.publishCreated(workout).recover { case ex =>
          logger.warn(s"Failed to publish created event: ${ex.getMessage}")
        }
      } yield {
        span.setAttribute("workout.id", workout.id.toString)
        logger.info(s"Created workout: ${workout.id}")
        workout
      }
    }
  }

  override def findById(id: UUID): Future[Option[Workout]] = {
    Tracing.withSpanAsync("WorkoutService.findById", SpanKind.INTERNAL) { span =>
      span.setAttribute("workout.id", id.toString)
      logger.debug(s"Finding workout by id: $id")
      repository.findById(id)
    }
  }

  override def findAll(limit: Int, offset: Int): Future[List[Workout]] = {
    Tracing.withSpanAsync("WorkoutService.findAll", SpanKind.INTERNAL) { span =>
      span.setAttribute("query.limit", limit.toLong)
      span.setAttribute("query.offset", offset.toLong)
      logger.debug(s"Finding all workouts with limit=$limit, offset=$offset")
      repository.findAll(limit, offset).map { workouts =>
        span.setAttribute("result.count", workouts.size.toLong)
        workouts
      }
    }
  }

  override def update(id: UUID, request: UpdateWorkoutRequest): Future[Option[Workout]] = {
    Tracing.withSpanAsync("WorkoutService.update", SpanKind.INTERNAL) { span =>
      span.setAttribute("workout.id", id.toString)
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
        span.setAttribute("workout.found", result.isDefined)
        result.foreach(w => logger.info(s"Updated workout: ${w.id}"))
        result
      }
    }
  }

  override def delete(id: UUID): Future[Boolean] = {
    Tracing.withSpanAsync("WorkoutService.delete", SpanKind.INTERNAL) { span =>
      span.setAttribute("workout.id", id.toString)
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
        span.setAttribute("workout.deleted", deleted)
        if (deleted) logger.info(s"Deleted workout: $id")
        deleted
      }
    }
  }

  override def findByType(workoutType: WorkoutType): Future[List[Workout]] = {
    Tracing.withSpanAsync("WorkoutService.findByType", SpanKind.INTERNAL) { span =>
      span.setAttribute("workout.type", workoutType.value)
      logger.debug(s"Finding workouts by type: ${workoutType.value}")
      repository.findByType(workoutType).map { workouts =>
        span.setAttribute("result.count", workouts.size.toLong)
        workouts
      }
    }
  }

  override def findByDifficulty(difficulty: Difficulty): Future[List[Workout]] = {
    Tracing.withSpanAsync("WorkoutService.findByDifficulty", SpanKind.INTERNAL) { span =>
      span.setAttribute("workout.difficulty", difficulty.value)
      logger.debug(s"Finding workouts by difficulty: ${difficulty.value}")
      repository.findByDifficulty(difficulty).map { workouts =>
        span.setAttribute("result.count", workouts.size.toLong)
        workouts
      }
    }
  }
}

