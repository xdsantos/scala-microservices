# Workout Pekko Microservice

A Scala microservice for managing workouts, built with Apache Pekko and a traditional actor-based stack.

## 🏋️ Features

- **CRUD Operations** for workouts (Create, Read, Update, Delete)
- **RESTful API** with Pekko HTTP
- **PostgreSQL** database with Slick ORM
- **Kafka** event streaming with Alpakka/Pekko Connectors
- **Flyway** database migrations
- **Docker** support with sbt-native-packager
- **Testcontainers** for integration testing

## 🛠️ Tech Stack

| Component | Technology |
|-----------|------------|
| HTTP Server | Pekko HTTP 1.0 |
| Actor System | Pekko 1.0 |
| Database | PostgreSQL + Slick 3.5 |
| Event Streaming | Pekko Connectors Kafka |
| JSON | Circe |
| Migrations | Flyway |
| Logging | Logback + scala-logging |
| Packaging | sbt-native-packager |
| Testing | ScalaTest + Testcontainers |
| Scala Version | 2.13.14 |

## 📋 Prerequisites

- JDK 17+
- sbt 1.9+
- Docker & Docker Compose
- PostgreSQL 15+ (or use Docker)
- Apache Kafka (or use Docker)

## 🚀 Quick Start

### 1. Start Infrastructure

```bash
# Start PostgreSQL and Kafka
docker-compose up -d postgres kafka zookeeper
```

### 2. Run the Application

```bash
# Run with sbt
sbt run
```

The service will be available at `http://localhost:8080`

### 3. Build Docker Image

```bash
# Build with sbt-native-packager
sbt docker:publishLocal

# Or build with Dockerfile
docker build -t workout-pekko-service .
```

### 4. Run with Docker Compose

```bash
# Run all services including the workout-service
docker-compose --profile full up
```

## 📡 API Endpoints

### Health Check
```bash
GET /health
```

### Workouts

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/workouts` | List all workouts |
| GET | `/api/workouts/:id` | Get workout by ID |
| POST | `/api/workouts` | Create new workout |
| PUT | `/api/workouts/:id` | Update workout |
| DELETE | `/api/workouts/:id` | Delete workout |
| GET | `/api/workouts/type/:type` | Filter by workout type |
| GET | `/api/workouts/difficulty/:difficulty` | Filter by difficulty |

### Query Parameters

- `limit` - Number of results (default: 100)
- `offset` - Pagination offset (default: 0)

## 📝 Request/Response Examples

### Create Workout

```bash
curl -X POST http://localhost:8080/api/workouts \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Morning Run",
    "description": "Easy 5K morning jog",
    "workoutType": "Running",
    "durationMinutes": 30,
    "caloriesBurned": 300,
    "difficulty": "Beginner"
  }'
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Morning Run",
    "description": "Easy 5K morning jog",
    "workoutType": "Running",
    "durationMinutes": 30,
    "caloriesBurned": 300,
    "difficulty": "Beginner",
    "createdAt": "2024-01-15T08:00:00Z",
    "updatedAt": "2024-01-15T08:00:00Z"
  },
  "message": null
}
```

### Update Workout

```bash
curl -X PUT http://localhost:8080/api/workouts/550e8400-e29b-41d4-a716-446655440000 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Evening Run",
    "durationMinutes": 45
  }'
```

### Delete Workout

```bash
curl -X DELETE http://localhost:8080/api/workouts/550e8400-e29b-41d4-a716-446655440000
```

## 🏃 Workout Types

- `Cardio`
- `Strength`
- `Flexibility`
- `HIIT`
- `Yoga`
- `CrossFit`
- `Swimming`
- `Running`
- `Cycling`
- `Other`

## 💪 Difficulty Levels

- `Beginner`
- `Intermediate`
- `Advanced`
- `Expert`

## ⚙️ Configuration

Configuration is managed via `application.conf` and can be overridden with environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_HOST` | Server bind host | `0.0.0.0` |
| `SERVER_PORT` | Server port | `8080` |
| `DATABASE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/workout_db` |
| `DATABASE_USER` | Database username | `postgres` |
| `DATABASE_PASSWORD` | Database password | `postgres` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers | `localhost:9092` |
| `KAFKA_TOPIC` | Event topic name | `workout-events` |

## 🧪 Testing

```bash
# Run all tests (requires Docker for Testcontainers)
sbt test

# Run specific test
sbt "testOnly workout.WorkoutServiceSpec"
sbt "testOnly workout.WorkoutRoutesSpec"
```

## 📁 Project Structure

```
workout-pekko/
├── build.sbt                 # SBT build configuration
├── project/
│   ├── build.properties      # SBT version
│   └── plugins.sbt           # SBT plugins
├── src/
│   ├── main/
│   │   ├── resources/
│   │   │   ├── application.conf    # App configuration
│   │   │   ├── logback.xml         # Logging config
│   │   │   └── db/migration/       # Flyway migrations
│   │   └── scala/workout/
│   │       ├── Main.scala          # App entry point
│   │       ├── api/                # Pekko HTTP routes
│   │       ├── config/             # Configuration models
│   │       ├── domain/             # Domain models
│   │       ├── kafka/              # Alpakka Kafka producer
│   │       ├── repository/         # Slick database layer
│   │       └── service/            # Business logic
│   └── test/
│       └── scala/workout/          # Tests
├── docker-compose.yml        # Docker Compose setup
├── Dockerfile               # Multi-stage Dockerfile
└── README.md                # This file
```

## 🔧 Development

### Enable Real Kafka Events

In `Main.scala`, replace:
```scala
val eventProducer = new NoopEventProducer()
```
with:
```scala
val eventProducer = new PekkoKafkaProducer(config.kafka)
```

## 🔄 Comparison with ZIO Version

| Aspect | ZIO Version | Pekko Version |
|--------|-------------|---------------|
| Effect System | ZIO (functional) | Futures (imperative) |
| Error Handling | Typed errors | Exceptions |
| Concurrency | Fibers | Actors/Futures |
| Testability | ZIO Test | ScalaTest |
| Learning Curve | Steeper | More familiar |
| Type Safety | Higher | Standard |

## 📜 License

MIT License

