package workout.api

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.model.StatusCodes
import workout.api.CirceSupport._
import workout.domain._
import workout.service.WorkoutService
import workout.tracing.Tracing
import io.opentelemetry.api.trace.SpanKind
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}
import java.util.UUID

case class HealthResponse(status: String, service: String)

class WorkoutRoutes(service: WorkoutService)(implicit ec: ExecutionContext) extends LazyLogging {

  private def parseUUID(str: String): Either[String, UUID] =
    Try(UUID.fromString(str)).toEither.left.map(_ => s"Invalid UUID format: $str")

  val routes: Route = concat(
    // Health check
    path("health") {
      get {
        complete(StatusCodes.OK -> HealthResponse("healthy", "workout-pekko-service"))
      }
    },

    // Workout API routes
    pathPrefix("api" / "workouts") {
      concat(
        // GET /api/workouts - List all workouts
        pathEnd {
          get {
            parameters("limit".as[Int].withDefault(100), "offset".as[Int].withDefault(0)) { (limit, offset) =>
              val tracedFuture = Tracing.withSpanAsync("HTTP GET /api/workouts", SpanKind.SERVER) { span =>
                span.setAttribute("http.method", "GET")
                span.setAttribute("http.route", "/api/workouts")
                span.setAttribute("query.limit", limit.toLong)
                span.setAttribute("query.offset", offset.toLong)
                service.findAll(limit, offset)
              }
              onComplete(tracedFuture) {
                case Success(workouts) =>
                  complete(StatusCodes.OK -> WorkoutListResponse.success(workouts))
                case Failure(ex) =>
                  logger.error("Failed to list workouts", ex)
                  complete(StatusCodes.InternalServerError -> WorkoutListResponse.error(ex.getMessage))
              }
            }
          }
        },

        // POST /api/workouts - Create workout (async)
        pathEnd {
          post {
            entity(as[CreateWorkoutRequest]) { request =>
              val tracedFuture = Tracing.withSpanAsync("HTTP POST /api/workouts", SpanKind.SERVER) { span =>
                span.setAttribute("http.method", "POST")
                span.setAttribute("http.route", "/api/workouts")
                span.setAttribute("workout.name", request.name)
                service.createAsync(request)
              }
              onComplete(tracedFuture) {
                case Success(correlationId) =>
                  complete(StatusCodes.Accepted -> AcceptedResponse(correlationId))
                case Failure(ex) =>
                  logger.error("Failed to submit workout creation request", ex)
                  complete(StatusCodes.InternalServerError -> WorkoutResponse.error(ex.getMessage))
              }
            }
          }
        },

        // GET /api/workouts/type/:type - Get by type
        path("type" / Segment) { workoutType =>
          get {
            WorkoutType.fromString(workoutType) match {
              case Right(wType) =>
                onComplete(service.findByType(wType)) {
                  case Success(workouts) =>
                    complete(StatusCodes.OK -> WorkoutListResponse.success(workouts))
                  case Failure(ex) =>
                    logger.error(s"Failed to find workouts by type: $workoutType", ex)
                    complete(StatusCodes.InternalServerError -> WorkoutListResponse.error(ex.getMessage))
                }
              case Left(error) =>
                complete(StatusCodes.BadRequest -> WorkoutListResponse.error(error))
            }
          }
        },

        // GET /api/workouts/difficulty/:difficulty - Get by difficulty
        path("difficulty" / Segment) { difficulty =>
          get {
            Difficulty.fromString(difficulty) match {
              case Right(diff) =>
                onComplete(service.findByDifficulty(diff)) {
                  case Success(workouts) =>
                    complete(StatusCodes.OK -> WorkoutListResponse.success(workouts))
                  case Failure(ex) =>
                    logger.error(s"Failed to find workouts by difficulty: $difficulty", ex)
                    complete(StatusCodes.InternalServerError -> WorkoutListResponse.error(ex.getMessage))
                }
              case Left(error) =>
                complete(StatusCodes.BadRequest -> WorkoutListResponse.error(error))
            }
          }
        },

        // GET /api/workouts/:id - Get by ID
        path(Segment) { id =>
          get {
            parseUUID(id) match {
              case Right(uuid) =>
                val tracedFuture = Tracing.withSpanAsync("HTTP GET /api/workouts/:id", SpanKind.SERVER) { span =>
                  span.setAttribute("http.method", "GET")
                  span.setAttribute("http.route", "/api/workouts/:id")
                  span.setAttribute("workout.id", uuid.toString)
                  service.findById(uuid)
                }
                onComplete(tracedFuture) {
                  case Success(Some(workout)) =>
                    complete(StatusCodes.OK -> WorkoutResponse.success(workout))
                  case Success(None) =>
                    complete(StatusCodes.NotFound -> WorkoutResponse.error(s"Workout not found: $id"))
                  case Failure(ex) =>
                    logger.error(s"Failed to find workout: $id", ex)
                    complete(StatusCodes.InternalServerError -> WorkoutResponse.error(ex.getMessage))
                }
              case Left(error) =>
                complete(StatusCodes.BadRequest -> WorkoutResponse.error(error))
            }
          }
        },

        // PUT /api/workouts/:id - Update workout
        path(Segment) { id =>
          put {
            entity(as[UpdateWorkoutRequest]) { request =>
              parseUUID(id) match {
                case Right(uuid) =>
                  val tracedFuture = Tracing.withSpanAsync("HTTP PUT /api/workouts/:id", SpanKind.SERVER) { span =>
                    span.setAttribute("http.method", "PUT")
                    span.setAttribute("http.route", "/api/workouts/:id")
                    span.setAttribute("workout.id", uuid.toString)
                    service.update(uuid, request)
                  }
                  onComplete(tracedFuture) {
                    case Success(Some(workout)) =>
                      complete(StatusCodes.OK -> WorkoutResponse.success(workout))
                    case Success(None) =>
                      complete(StatusCodes.NotFound -> WorkoutResponse.error(s"Workout not found: $id"))
                    case Failure(ex) =>
                      logger.error(s"Failed to update workout: $id", ex)
                      complete(StatusCodes.InternalServerError -> WorkoutResponse.error(ex.getMessage))
                  }
                case Left(error) =>
                  complete(StatusCodes.BadRequest -> WorkoutResponse.error(error))
              }
            }
          }
        },

        // DELETE /api/workouts/:id - Delete workout
        path(Segment) { id =>
          delete {
            parseUUID(id) match {
              case Right(uuid) =>
                val tracedFuture = Tracing.withSpanAsync("HTTP DELETE /api/workouts/:id", SpanKind.SERVER) { span =>
                  span.setAttribute("http.method", "DELETE")
                  span.setAttribute("http.route", "/api/workouts/:id")
                  span.setAttribute("workout.id", uuid.toString)
                  service.delete(uuid)
                }
                onComplete(tracedFuture) {
                  case Success(true) =>
                    complete(StatusCodes.OK -> DeleteResponse(success = true, s"Workout $id deleted successfully"))
                  case Success(false) =>
                    complete(StatusCodes.NotFound -> DeleteResponse(success = false, s"Workout not found: $id"))
                  case Failure(ex) =>
                    logger.error(s"Failed to delete workout: $id", ex)
                    complete(StatusCodes.InternalServerError -> DeleteResponse(success = false, ex.getMessage))
                }
              case Left(error) =>
                complete(StatusCodes.BadRequest -> DeleteResponse(success = false, error))
            }
          }
        }
      )
    }
  )
}

