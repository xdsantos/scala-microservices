package workout.repository

import io.getquill._
import io.getquill.jdbczio.Quill
import workout.domain._
import zio._
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

final case class WorkoutRepositoryLive(quill: Quill.Postgres[SnakeCase]) extends WorkoutRepository {
  import quill._

  // Schema definition
  private val workoutsQuery = quote {
    querySchema[WorkoutRow]("workouts")
  }

  override def create(request: CreateWorkoutRequest): Task[Workout] = {
    val now = Timestamp.from(Instant.now())
    val id = UUID.randomUUID()
    val name = request.name
    val description = request.description
    val workoutType = request.workoutType.toString
    val durationMinutes = request.durationMinutes
    val caloriesBurned = request.caloriesBurned
    val difficulty = request.difficulty.toString
    
    for {
      _ <- run(
        workoutsQuery.insert(
          _.id -> lift(id),
          _.name -> lift(name),
          _.description -> lift(description),
          _.workoutType -> lift(workoutType),
          _.durationMinutes -> lift(durationMinutes),
          _.caloriesBurned -> lift(caloriesBurned),
          _.difficulty -> lift(difficulty),
          _.createdAt -> lift(now),
          _.updatedAt -> lift(now)
        )
      )
      row = WorkoutRow(id, name, description, workoutType, durationMinutes, caloriesBurned, difficulty, now, now)
      workout <- ZIO.fromEither(WorkoutRow.toDomain(row))
        .mapError(e => new RuntimeException(e))
    } yield workout
  }

  override def findById(id: UUID): Task[Option[Workout]] =
    for {
      rows <- run(workoutsQuery.filter(_.id == lift(id)))
      result <- rows.headOption match {
        case Some(row) =>
          ZIO.fromEither(WorkoutRow.toDomain(row))
            .mapError(e => new RuntimeException(e))
            .map(Some(_))
        case None => ZIO.succeed(None)
      }
    } yield result

  override def findAll(limit: Int, offset: Int): Task[List[Workout]] =
    for {
      rows <- run(
        workoutsQuery
          .sortBy(_.createdAt)(Ord.desc)
          .drop(lift(offset))
          .take(lift(limit))
      )
      workoutList <- ZIO.foreach(rows)(row =>
        ZIO.fromEither(WorkoutRow.toDomain(row))
          .mapError(e => new RuntimeException(e))
      )
    } yield workoutList

  override def update(id: UUID, request: UpdateWorkoutRequest): Task[Option[Workout]] =
    for {
      existing <- findById(id)
      result <- existing match {
        case Some(workout) =>
          val updated = workout.copy(
            name = request.name.getOrElse(workout.name),
            description = request.description.orElse(workout.description),
            workoutType = request.workoutType.getOrElse(workout.workoutType),
            durationMinutes = request.durationMinutes.getOrElse(workout.durationMinutes),
            caloriesBurned = request.caloriesBurned.orElse(workout.caloriesBurned),
            difficulty = request.difficulty.getOrElse(workout.difficulty),
            updatedAt = Instant.now()
          )
          val row = WorkoutRow.fromDomain(updated)
          for {
            _ <- run(
              workoutsQuery
                .filter(_.id == lift(id))
                .update(
                  _.name -> lift(row.name),
                  _.description -> lift(row.description),
                  _.workoutType -> lift(row.workoutType),
                  _.durationMinutes -> lift(row.durationMinutes),
                  _.caloriesBurned -> lift(row.caloriesBurned),
                  _.difficulty -> lift(row.difficulty),
                  _.updatedAt -> lift(row.updatedAt)
                )
            )
          } yield Some(updated)
        case None => ZIO.succeed(None)
      }
    } yield result

  override def delete(id: UUID): Task[Boolean] =
    for {
      count <- run(workoutsQuery.filter(_.id == lift(id)).delete)
    } yield count > 0

  override def findByType(workoutType: WorkoutType): Task[List[Workout]] = {
    val typeStr = workoutType.toString
    for {
      rows <- run(
        workoutsQuery.filter(_.workoutType == lift(typeStr))
          .sortBy(_.createdAt)(Ord.desc)
      )
      workoutList <- ZIO.foreach(rows)(row =>
        ZIO.fromEither(WorkoutRow.toDomain(row))
          .mapError(e => new RuntimeException(e))
      )
    } yield workoutList
  }

  override def findByDifficulty(difficulty: Difficulty): Task[List[Workout]] = {
    val diffStr = difficulty.toString
    for {
      rows <- run(
        workoutsQuery.filter(_.difficulty == lift(diffStr))
          .sortBy(_.createdAt)(Ord.desc)
      )
      workoutList <- ZIO.foreach(rows)(row =>
        ZIO.fromEither(WorkoutRow.toDomain(row))
          .mapError(e => new RuntimeException(e))
      )
    } yield workoutList
  }
}

object WorkoutRepositoryLive {
  val layer: ZLayer[Quill.Postgres[SnakeCase], Nothing, WorkoutRepository] =
    ZLayer.fromFunction(WorkoutRepositoryLive(_))
}
