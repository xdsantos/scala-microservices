# Workout ZIO Microservice

A modern Scala microservice for managing workouts, built with ZIO 2 and a cutting-edge functional tech stack.

## 🏋️ Features

- **CRUD Operations** for workouts (Create, Read, Update, Delete)
- **RESTful API** with ZIO HTTP
- **PostgreSQL** database with Quill ORM
- **Kafka** event streaming for workout events
- **OpenTelemetry** distributed tracing
- **Flyway** database migrations
- **Docker** support with sbt-native-packager
- **Testcontainers** for integration testing

## 🛠️ Tech Stack

| Component | Technology |
|-----------|------------|
| Effect System | ZIO 2 |
| HTTP Server | ZIO HTTP 3.0 |
| Database | PostgreSQL + Quill |
| Event Streaming | ZIO Kafka |
| Configuration | ZIO Config |
| Migrations | Flyway |
| Tracing | OpenTelemetry |
| Packaging | sbt-native-packager |
| Testing | ZIO Test + Testcontainers |
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

# Optional: Start Jaeger for tracing
docker-compose up -d jaeger
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
docker build -t workout-service .
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

### List by Type

```bash
curl http://localhost:8080/api/workouts/type/Running
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
| `DATABASE_POOL_SIZE` | Connection pool size | `10` |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers | `localhost:9092` |
| `KAFKA_TOPIC` | Event topic name | `workout-events` |
| `OTEL_ENABLED` | Enable tracing | `true` |
| `OTEL_SERVICE_NAME` | Service name for tracing | `workout-service` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OTLP collector endpoint | `http://localhost:4317` |

## 🧪 Testing

```bash
# Run all tests (requires Docker for Testcontainers)
sbt test

# Run specific test suite
sbt "testOnly workout.WorkoutServiceSpec"
```

## 📊 Observability

### Distributed Tracing

The service exports traces via OpenTelemetry to any OTLP-compatible collector. By default, traces are sent to `http://localhost:4317`.

To view traces with Jaeger:
1. Start Jaeger: `docker-compose up -d jaeger`
2. Open http://localhost:16686

### Logging

Structured logging is configured with Logback. Logs include:
- Request/response logging
- Database query logging
- Kafka event logging
- Trace correlation IDs

## 📁 Project Structure

```
workout-zio/
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
│   │       ├── api/                # HTTP routes
│   │       ├── config/             # Configuration models
│   │       ├── domain/             # Domain models
│   │       ├── kafka/              # Kafka producer
│   │       ├── repository/         # Database layer
│   │       ├── service/            # Business logic
│   │       └── telemetry/          # OpenTelemetry
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
WorkoutEventProducerNoop.layer
```
with:
```scala
KafkaProducerLive.layer ++ WorkoutEventProducerLive.layer
```

### Enable Real Tracing

In `Main.scala`, replace:
```scala
TracingNoop.layer
```
with:
```scala
TracingLive.layer
```

## 📜 License

MIT License

