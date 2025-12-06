package workout.repository

import slick.jdbc.PostgresProfile.api._
import workout.domain._
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import java.time.Instant
import java.util.UUID

class SlickWorkoutRepository(db: Database)(implicit ec: ExecutionContext)
    extends WorkoutRepository
    with LazyLogging
    with SlickTypeMappers {

  import WorkoutTable._

  private def rowToDomain(row: WorkoutRow): Either[String, Workout] =
    for {
      workoutType <- WorkoutType.fromString(row.workoutType)
      difficulty <- Difficulty.fromString(row.difficulty)
    } yield Workout(
      id = row.id,
      name = row.name,
      description = row.description,
      workoutType = workoutType,
      durationMinutes = row.durationMinutes,
      caloriesBurned = row.caloriesBurned,
      difficulty = difficulty,
      createdAt = row.createdAt,
      updatedAt = row.updatedAt
    )

  private def domainToRow(workout: Workout): WorkoutRow =
    WorkoutRow(
      id = workout.id,
      name = workout.name,
      description = workout.description,
      workoutType = workout.workoutType.value,
      durationMinutes = workout.durationMinutes,
      caloriesBurned = workout.caloriesBurned,
      difficulty = workout.difficulty.value,
      createdAt = workout.createdAt,
      updatedAt = workout.updatedAt
    )

  override def create(request: CreateWorkoutRequest): Future[Workout] = {
    val now = Instant.now()
    val id = UUID.randomUUID()
    val row = WorkoutRow(
      id = id,
      name = request.name,
      description = request.description,
      workoutType = request.workoutType.value,
      durationMinutes = request.durationMinutes,
      caloriesBurned = request.caloriesBurned,
      difficulty = request.difficulty.value,
      createdAt = now,
      updatedAt = now
    )

    val action = workouts += row
    db.run(action).flatMap { _ =>
      rowToDomain(row) match {
        case Right(workout) => Future.successful(workout)
        case Left(error) => Future.failed(new RuntimeException(error))
      }
    }
  }

  override def findById(id: UUID): Future[Option[Workout]] = {
    val query = workouts.filter(_.id === id)
    db.run(query.result.headOption).flatMap {
      case Some(row) =>
        rowToDomain(row) match {
          case Right(workout) => Future.successful(Some(workout))
          case Left(error) => Future.failed(new RuntimeException(error))
        }
      case None => Future.successful(None)
    }
  }

  override def findAll(limit: Int, offset: Int): Future[List[Workout]] = {
    val query = workouts.sortBy(_.createdAt.desc).drop(offset).take(limit)
    db.run(query.result).flatMap { rows =>
      val results = rows.map(rowToDomain).toList
      val errors = results.collect { case Left(e) => e }
      if (errors.nonEmpty) {
        Future.failed(new RuntimeException(errors.mkString(", ")))
      } else {
        Future.successful(results.collect { case Right(w) => w })
      }
    }
  }

  override def update(id: UUID, request: UpdateWorkoutRequest): Future[Option[Workout]] = {
    findById(id).flatMap {
      case Some(existing) =>
        val updated = existing.copy(
          name = request.name.getOrElse(existing.name),
          description = request.description.orElse(existing.description),
          workoutType = request.workoutType.getOrElse(existing.workoutType),
          durationMinutes = request.durationMinutes.getOrElse(existing.durationMinutes),
          caloriesBurned = request.caloriesBurned.orElse(existing.caloriesBurned),
          difficulty = request.difficulty.getOrElse(existing.difficulty),
          updatedAt = Instant.now()
        )
        val row = domainToRow(updated)
        val query = workouts.filter(_.id === id).update(row)
        db.run(query).map(_ => Some(updated))
      case None =>
        Future.successful(None)
    }
  }

  override def delete(id: UUID): Future[Boolean] = {
    val query = workouts.filter(_.id === id).delete
    db.run(query).map(_ > 0)
  }

  override def findByType(workoutType: WorkoutType): Future[List[Workout]] = {
    val typeStr = workoutType.value
    val query = workouts.filter(_.workoutType === typeStr).sortBy(_.createdAt.desc)
    db.run(query.result).flatMap { rows =>
      val results = rows.map(rowToDomain).toList
      val errors = results.collect { case Left(e) => e }
      if (errors.nonEmpty) {
        Future.failed(new RuntimeException(errors.mkString(", ")))
      } else {
        Future.successful(results.collect { case Right(w) => w })
      }
    }
  }

  override def findByDifficulty(difficulty: Difficulty): Future[List[Workout]] = {
    val diffStr = difficulty.value
    val query = workouts.filter(_.difficulty === diffStr).sortBy(_.createdAt.desc)
    db.run(query.result).flatMap { rows =>
      val results = rows.map(rowToDomain).toList
      val errors = results.collect { case Left(e) => e }
      if (errors.nonEmpty) {
        Future.failed(new RuntimeException(errors.mkString(", ")))
      } else {
        Future.successful(results.collect { case Right(w) => w })
      }
    }
  }
}

