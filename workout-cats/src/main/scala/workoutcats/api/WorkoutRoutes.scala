package workoutcats.api

import cats.effect.Async
import cats.syntax.all._
import io.circe.parser.decode
import io.circe.syntax._
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._
import workoutcats.domain._
import workoutcats.service.WorkoutService
import java.util.UUID

final class WorkoutRoutes[F[_]: Async](service: WorkoutService[F]) extends Http4sDsl[F] {

  implicit val workoutDecoder: EntityDecoder[F, CreateWorkoutRequest] = jsonOf
  implicit val updateDecoder: EntityDecoder[F, UpdateWorkoutRequest] = jsonOf

  private def parseUuid(id: String): Either[String, UUID] =
    Either.catchOnly[IllegalArgumentException](UUID.fromString(id)).leftMap(_ => s"Invalid UUID: $id")

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "health" =>
      Ok("""{"status":"healthy","service":"workout-cats-service"}""")

    case req @ POST -> Root / "api" / "workouts" =>
      for {
        body <- req.as[CreateWorkoutRequest]
        corr <- service.createAsync(body)
        resp <- Accepted(AcceptedResponse(corr).asJson)
      } yield resp

    case GET -> Root / "api" / "workouts" :? LimitMatcher(limit) +& OffsetMatcher(offset) =>
      service.findAll(limit.getOrElse(100), offset.getOrElse(0))
        .flatMap(ws => Ok(WorkoutListResponse.success(ws).asJson))
        .handleErrorWith(e => InternalServerError(WorkoutListResponse.error(e.getMessage).asJson))

    case GET -> Root / "api" / "workouts" / id =>
      parseUuid(id) match {
        case Left(err) => BadRequest(WorkoutResponse.error(err).asJson)
        case Right(uuid) =>
          service.findById(uuid).flatMap {
            case Some(w) => Ok(WorkoutResponse.success(w).asJson)
            case None    => NotFound(WorkoutResponse.error(s"Workout not found: $id").asJson)
          }.handleErrorWith(e => InternalServerError(WorkoutResponse.error(e.getMessage).asJson))
      }

    case req @ PUT -> Root / "api" / "workouts" / id =>
      parseUuid(id) match {
        case Left(err) => BadRequest(WorkoutResponse.error(err).asJson)
        case Right(uuid) =>
          req.as[UpdateWorkoutRequest].flatMap { upd =>
            service.update(uuid, upd).flatMap {
              case Some(w) => Ok(WorkoutResponse.success(w).asJson)
              case None    => NotFound(WorkoutResponse.error(s"Workout not found: $id").asJson)
            }.handleErrorWith(e => InternalServerError(WorkoutResponse.error(e.getMessage).asJson))
          }
      }

    case DELETE -> Root / "api" / "workouts" / id =>
      parseUuid(id) match {
        case Left(err) => BadRequest(WorkoutResponse.error(err).asJson)
        case Right(uuid) =>
          service.delete(uuid).flatMap {
            case true  => Ok(DeleteResponse(success = true, s"Workout $id deleted").asJson)
            case false => NotFound(DeleteResponse(success = false, s"Workout not found: $id").asJson)
          }.handleErrorWith(e => InternalServerError(DeleteResponse(success = false, e.getMessage).asJson))
      }

    case GET -> Root / "api" / "workouts" / "type" / wt =>
      WorkoutType.fromString(wt) match {
        case Left(err) => BadRequest(WorkoutListResponse.error(err).asJson)
        case Right(wtype) =>
          service.findByType(wtype)
            .flatMap(ws => Ok(WorkoutListResponse.success(ws).asJson))
            .handleErrorWith(e => InternalServerError(WorkoutListResponse.error(e.getMessage).asJson))
      }

    case GET -> Root / "api" / "workouts" / "difficulty" / diff =>
      Difficulty.fromString(diff) match {
        case Left(err) => BadRequest(WorkoutListResponse.error(err).asJson)
        case Right(d) =>
          service.findByDifficulty(d)
            .flatMap(ws => Ok(WorkoutListResponse.success(ws).asJson))
            .handleErrorWith(e => InternalServerError(WorkoutListResponse.error(e.getMessage).asJson))
      }
  }

  private object LimitMatcher extends OptionalQueryParamDecoderMatcher[Int]("limit")
  private object OffsetMatcher extends OptionalQueryParamDecoderMatcher[Int]("offset")
}

