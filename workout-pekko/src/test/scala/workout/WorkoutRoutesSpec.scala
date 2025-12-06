package workout

import org.apache.pekko.http.scaladsl.model.{ContentTypes, StatusCodes}
import org.apache.pekko.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import workout.api.CirceSupport._
import io.circe.syntax._
import io.circe.generic.auto._
import workout.api.WorkoutRoutes
import workout.domain._
import workout.service.WorkoutService

import scala.concurrent.Future
import java.time.Instant
import java.util.UUID

class WorkoutRoutesSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {

  val sampleWorkout: Workout = Workout(
    id = UUID.randomUUID(),
    name = "Test Workout",
    description = Some("A test workout"),
    workoutType = WorkoutType.Running,
    durationMinutes = 30,
    caloriesBurned = Some(300),
    difficulty = Difficulty.Beginner,
    createdAt = Instant.now(),
    updatedAt = Instant.now()
  )

  // Mock service
  class MockWorkoutService extends WorkoutService {
    override def create(request: CreateWorkoutRequest): Future[Workout] =
      Future.successful(sampleWorkout)

    override def findById(id: UUID): Future[Option[Workout]] =
      if (id == sampleWorkout.id) Future.successful(Some(sampleWorkout))
      else Future.successful(None)

    override def findAll(limit: Int, offset: Int): Future[List[Workout]] =
      Future.successful(List(sampleWorkout))

    override def update(id: UUID, request: UpdateWorkoutRequest): Future[Option[Workout]] =
      if (id == sampleWorkout.id) Future.successful(Some(sampleWorkout))
      else Future.successful(None)

    override def delete(id: UUID): Future[Boolean] =
      Future.successful(id == sampleWorkout.id)

    override def findByType(workoutType: WorkoutType): Future[List[Workout]] =
      Future.successful(List(sampleWorkout).filter(_.workoutType == workoutType))

    override def findByDifficulty(difficulty: Difficulty): Future[List[Workout]] =
      Future.successful(List(sampleWorkout).filter(_.difficulty == difficulty))
  }

  val mockService = new MockWorkoutService()
  val routes = new WorkoutRoutes(mockService)

  "WorkoutRoutes" should {

    "return healthy on GET /health" in {
      Get("/health") ~> routes.routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "list workouts on GET /api/workouts" in {
      Get("/api/workouts") ~> routes.routes ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[WorkoutListResponse]
        response.success shouldBe true
        response.data.nonEmpty shouldBe true
      }
    }

    "create workout on POST /api/workouts" in {
      val request = CreateWorkoutRequest(
        name = "New Workout",
        description = Some("Description"),
        workoutType = WorkoutType.Running,
        durationMinutes = 30,
        caloriesBurned = Some(300),
        difficulty = Difficulty.Beginner
      )

      Post("/api/workouts")
        .withEntity(ContentTypes.`application/json`, request.asJson.noSpaces) ~> routes.routes ~> check {
        status shouldBe StatusCodes.Created
        val response = responseAs[WorkoutResponse]
        response.success shouldBe true
        response.data shouldBe defined
      }
    }

    "get workout by ID on GET /api/workouts/:id" in {
      Get(s"/api/workouts/${sampleWorkout.id}") ~> routes.routes ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[WorkoutResponse]
        response.success shouldBe true
        response.data shouldBe defined
      }
    }

    "return 404 for non-existent workout" in {
      Get(s"/api/workouts/${UUID.randomUUID()}") ~> routes.routes ~> check {
        status shouldBe StatusCodes.NotFound
      }
    }

    "return 400 for invalid UUID" in {
      Get("/api/workouts/invalid-uuid") ~> routes.routes ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "update workout on PUT /api/workouts/:id" in {
      val request = UpdateWorkoutRequest(
        name = Some("Updated"),
        description = None,
        workoutType = None,
        durationMinutes = None,
        caloriesBurned = None,
        difficulty = None
      )

      Put(s"/api/workouts/${sampleWorkout.id}")
        .withEntity(ContentTypes.`application/json`, request.asJson.noSpaces) ~> routes.routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "delete workout on DELETE /api/workouts/:id" in {
      Delete(s"/api/workouts/${sampleWorkout.id}") ~> routes.routes ~> check {
        status shouldBe StatusCodes.OK
        val response = responseAs[DeleteResponse]
        response.success shouldBe true
      }
    }

    "filter by type on GET /api/workouts/type/:type" in {
      Get("/api/workouts/type/Running") ~> routes.routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }

    "return 400 for invalid workout type" in {
      Get("/api/workouts/type/Invalid") ~> routes.routes ~> check {
        status shouldBe StatusCodes.BadRequest
      }
    }

    "filter by difficulty on GET /api/workouts/difficulty/:difficulty" in {
      Get("/api/workouts/difficulty/Beginner") ~> routes.routes ~> check {
        status shouldBe StatusCodes.OK
      }
    }
  }
}

