package workoutcats.repository

import cats.effect.kernel.Sync
import cats.syntax.all._
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.implicits.javatime._
import doobie.implicits.javasql._
import scala.language.postfixOps
import workoutcats.domain._
import java.time.Instant
import java.util.UUID
import java.sql.Timestamp
import java.sql.Timestamp

final class WorkoutRepositoryDoobie[F[_]: Sync](xa: Transactor[F]) extends WorkoutRepository[F] {

  private def workoutTypeToString(wt: WorkoutType): String = wt.toString
  private def difficultyToString(d: Difficulty): String = d.toString

  private def rowToDomain(
      id: UUID,
      name: String,
      description: Option[String],
      workoutType: String,
      duration: Int,
      calories: Option[Int],
      difficulty: String,
      created: Instant,
      updated: Instant
  ): Either[String, Workout] = for {
    wt <- WorkoutType.fromString(workoutType)
    df <- Difficulty.fromString(difficulty)
  } yield Workout(id, name, description, wt, duration, calories, df, created, updated)

  override def create(req: CreateWorkoutRequest): F[Workout] = {
    val id = UUID.randomUUID()
    val now = Instant.now()
    val nowTs = Timestamp.from(now)
    sql"""
      INSERT INTO workouts(id, name, description, workout_type, duration_minutes, calories_burned, difficulty, created_at, updated_at)
      VALUES ($id, ${req.name}, ${req.description}, ${workoutTypeToString(req.workoutType)}, ${req.durationMinutes},
              ${req.caloriesBurned}, ${difficultyToString(req.difficulty)}, $nowTs, $nowTs)
    """.update.run.transact(xa) *> Sync[F].fromEither(rowToDomain(
      id,
      req.name,
      req.description,
      workoutTypeToString(req.workoutType),
      req.durationMinutes,
      req.caloriesBurned,
      difficultyToString(req.difficulty),
      now,
      now
    ).leftMap(new Exception(_)))
  }

  override def findById(id: UUID): F[Option[Workout]] =
    sql"""
      SELECT id, name, description, workout_type, duration_minutes, calories_burned, difficulty, created_at, updated_at
      FROM workouts WHERE id = $id
    """.query[(UUID, String, Option[String], String, Int, Option[Int], String, Timestamp, Timestamp)]
      .to[List]
      .map(_.collect { case (a,b,c,d,e,f,g,h,i) => rowToDomain(a,b,c,d,e,f,g,h.toInstant,i.toInstant) }.collect { case Right(w) => w }.headOption)
      .transact(xa)

  override def findAll(limit: Int, offset: Int): F[List[Workout]] =
    sql"""
      SELECT id, name, description, workout_type, duration_minutes, calories_burned, difficulty, created_at, updated_at
      FROM workouts
      ORDER BY created_at DESC
      OFFSET $offset LIMIT $limit
    """.query[(UUID, String, Option[String], String, Int, Option[Int], String, Timestamp, Timestamp)]
      .to[List]
      .map(_.collect { case (a,b,c,d,e,f,g,h,i) => rowToDomain(a,b,c,d,e,f,g,h.toInstant,i.toInstant) }.collect { case Right(w) => w })
      .transact(xa)

  override def update(id: UUID, req: UpdateWorkoutRequest): F[Option[Workout]] =
    findById(id).flatMap {
      case Some(w) =>
        val merged = w.copy(
          name = req.name.getOrElse(w.name),
          description = req.description.orElse(w.description),
          workoutType = req.workoutType.getOrElse(w.workoutType),
          durationMinutes = req.durationMinutes.getOrElse(w.durationMinutes),
          caloriesBurned = req.caloriesBurned.orElse(w.caloriesBurned),
          difficulty = req.difficulty.getOrElse(w.difficulty),
          updatedAt = Instant.now()
        )
        val updTs = Timestamp.from(merged.updatedAt)
        sql"""
          UPDATE workouts SET
            name = ${merged.name},
            description = ${merged.description},
            workout_type = ${workoutTypeToString(merged.workoutType)},
            duration_minutes = ${merged.durationMinutes},
            calories_burned = ${merged.caloriesBurned},
            difficulty = ${difficultyToString(merged.difficulty)},
            updated_at = $updTs
          WHERE id = $id
        """.update.run.transact(xa).map(_ => Some(merged))
      case None => Sync[F].pure(None)
    }

  override def delete(id: UUID): F[Boolean] =
    sql"DELETE FROM workouts WHERE id = $id".update.run.transact(xa).map(_ > 0)

  override def findByType(workoutType: WorkoutType): F[List[Workout]] =
    sql"""
      SELECT id, name, description, workout_type, duration_minutes, calories_burned, difficulty, created_at, updated_at
      FROM workouts WHERE workout_type = ${workoutTypeToString(workoutType)}
      ORDER BY created_at DESC
    """.query[(UUID, String, Option[String], String, Int, Option[Int], String, Timestamp, Timestamp)]
      .to[List]
      .map(_.collect { case (a,b,c,d,e,f,g,h,i) => rowToDomain(a,b,c,d,e,f,g,h.toInstant,i.toInstant) }.collect { case Right(w) => w })
      .transact(xa)

  override def findByDifficulty(difficulty: Difficulty): F[List[Workout]] =
    sql"""
      SELECT id, name, description, workout_type, duration_minutes, calories_burned, difficulty, created_at, updated_at
      FROM workouts WHERE difficulty = ${difficultyToString(difficulty)}
      ORDER BY created_at DESC
    """.query[(UUID, String, Option[String], String, Int, Option[Int], String, Timestamp, Timestamp)]
      .to[List]
      .map(_.collect { case (a,b,c,d,e,f,g,h,i) => rowToDomain(a,b,c,d,e,f,g,h.toInstant,i.toInstant) }.collect { case Right(w) => w })
      .transact(xa)
}

