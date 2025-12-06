package workout.api

import workout.domain._
import workout.service.WorkoutService
import zio._
import zio.http._
import zio.json._
import java.util.UUID
import scala.util.Try

object WorkoutRoutes {

  private def parseUUID(str: String): Either[String, UUID] =
    try {
      Right(UUID.fromString(str))
    } catch {
      case _: IllegalArgumentException => Left(s"Invalid UUID format: $str")
    }

  private def getIntParam(req: Request, name: String, default: Int): Int =
    req.url.queryParams.get(name)
      .flatMap(_.headOption)
      .flatMap(s => Try(s.toInt).toOption)
      .getOrElse(default)

  val routes: Routes[WorkoutService, Nothing] =
    Routes(
      // Health check
      Method.GET / "health" -> handler {
        Response.json("""{"status":"healthy","service":"workout-service"}""")
      },

      // Get all workouts
      Method.GET / "api" / "workouts" -> handler { (req: Request) =>
        val limit = getIntParam(req, "limit", 100)
        val offset = getIntParam(req, "offset", 0)

        WorkoutService.findAll(limit, offset)
          .map(workouts => Response.json(WorkoutListResponse.success(workouts).toJson))
          .catchAll(error =>
            ZIO.succeed(Response.json(WorkoutListResponse.error(error.getMessage).toJson).status(Status.InternalServerError))
          )
      },

      // Get workout by ID
      Method.GET / "api" / "workouts" / string("id") -> handler { (id: String, req: Request) =>
        (for {
          uuid <- ZIO.fromEither(parseUUID(id)).mapError(new IllegalArgumentException(_))
          workout <- WorkoutService.findById(uuid)
        } yield workout match {
          case Some(w) => Response.json(WorkoutResponse.success(w).toJson)
          case None => Response.json(WorkoutResponse.error(s"Workout not found: $id").toJson).status(Status.NotFound)
        }).catchAll {
          case e: IllegalArgumentException =>
            ZIO.succeed(Response.json(WorkoutResponse.error(e.getMessage).toJson).status(Status.BadRequest))
          case e: Throwable =>
            ZIO.succeed(Response.json(WorkoutResponse.error(e.getMessage).toJson).status(Status.InternalServerError))
        }
      },

      // Create workout
      Method.POST / "api" / "workouts" -> handler { (req: Request) =>
        (for {
          body <- req.body.asString
          createReq <- ZIO.fromEither(body.fromJson[CreateWorkoutRequest])
            .mapError(e => new IllegalArgumentException(s"Invalid request body: $e"))
          workout <- WorkoutService.create(createReq)
        } yield Response.json(WorkoutResponse.success(workout).toJson).status(Status.Created)
        ).catchAll {
          case e: IllegalArgumentException =>
            ZIO.succeed(Response.json(WorkoutResponse.error(e.getMessage).toJson).status(Status.BadRequest))
          case e: Throwable =>
            ZIO.succeed(Response.json(WorkoutResponse.error(e.getMessage).toJson).status(Status.InternalServerError))
        }
      },

      // Update workout
      Method.PUT / "api" / "workouts" / string("id") -> handler { (id: String, req: Request) =>
        (for {
          uuid <- ZIO.fromEither(parseUUID(id)).mapError(new IllegalArgumentException(_))
          body <- req.body.asString
          updateReq <- ZIO.fromEither(body.fromJson[UpdateWorkoutRequest])
            .mapError(e => new IllegalArgumentException(s"Invalid request body: $e"))
          workout <- WorkoutService.update(uuid, updateReq)
        } yield workout match {
          case Some(w) => Response.json(WorkoutResponse.success(w).toJson)
          case None => Response.json(WorkoutResponse.error(s"Workout not found: $id").toJson).status(Status.NotFound)
        }).catchAll {
          case e: IllegalArgumentException =>
            ZIO.succeed(Response.json(WorkoutResponse.error(e.getMessage).toJson).status(Status.BadRequest))
          case e: Throwable =>
            ZIO.succeed(Response.json(WorkoutResponse.error(e.getMessage).toJson).status(Status.InternalServerError))
        }
      },

      // Delete workout
      Method.DELETE / "api" / "workouts" / string("id") -> handler { (id: String, req: Request) =>
        (for {
          uuid <- ZIO.fromEither(parseUUID(id)).mapError(new IllegalArgumentException(_))
          deleted <- WorkoutService.delete(uuid)
        } yield if (deleted) {
          Response.json(DeleteResponse(true, s"Workout $id deleted successfully").toJson)
        } else {
          Response.json(DeleteResponse(false, s"Workout not found: $id").toJson).status(Status.NotFound)
        }).catchAll {
          case e: IllegalArgumentException =>
            ZIO.succeed(Response.json(WorkoutResponse.error(e.getMessage).toJson).status(Status.BadRequest))
          case e: Throwable =>
            ZIO.succeed(Response.json(WorkoutResponse.error(e.getMessage).toJson).status(Status.InternalServerError))
        }
      },

      // Get workouts by type
      Method.GET / "api" / "workouts" / "type" / string("workoutType") -> handler { (workoutType: String, req: Request) =>
        (for {
          wType <- ZIO.fromEither(WorkoutType.fromString(workoutType))
            .mapError(e => new IllegalArgumentException(e))
          workouts <- WorkoutService.findByType(wType)
        } yield Response.json(WorkoutListResponse.success(workouts).toJson)
        ).catchAll {
          case e: IllegalArgumentException =>
            ZIO.succeed(Response.json(WorkoutListResponse.error(e.getMessage).toJson).status(Status.BadRequest))
          case e: Throwable =>
            ZIO.succeed(Response.json(WorkoutListResponse.error(e.getMessage).toJson).status(Status.InternalServerError))
        }
      },

      // Get workouts by difficulty
      Method.GET / "api" / "workouts" / "difficulty" / string("difficulty") -> handler { (difficulty: String, req: Request) =>
        (for {
          diff <- ZIO.fromEither(Difficulty.fromString(difficulty))
            .mapError(e => new IllegalArgumentException(e))
          workouts <- WorkoutService.findByDifficulty(diff)
        } yield Response.json(WorkoutListResponse.success(workouts).toJson)
        ).catchAll {
          case e: IllegalArgumentException =>
            ZIO.succeed(Response.json(WorkoutListResponse.error(e.getMessage).toJson).status(Status.BadRequest))
          case e: Throwable =>
            ZIO.succeed(Response.json(WorkoutListResponse.error(e.getMessage).toJson).status(Status.InternalServerError))
        }
      }
    )

  val middleware: Middleware[Any] = Middleware.requestLogging()
}
