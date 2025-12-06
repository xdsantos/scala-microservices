# Workout Microservices

This repository contains two implementations of the same Workout CRUD microservice using different Scala tech stacks.

## 📁 Projects

### [workout-zio](./workout-zio/) - Modern Functional Stack

Built with cutting-edge ZIO 2 ecosystem:

| Component | Technology |
|-----------|------------|
| Effect System | ZIO 2 |
| HTTP Server | ZIO HTTP 3.0 |
| Database | Quill + PostgreSQL |
| Event Streaming | ZIO Kafka |
| Configuration | ZIO Config |
| Tracing | OpenTelemetry |
| Testing | ZIO Test + Testcontainers |
| Scala Version | **Scala 2.13** |

### [workout-pekko](./workout-pekko/) - Traditional Actor-Based Stack

Built with Apache Pekko (Akka fork):

| Component | Technology |
|-----------|------------|
| HTTP Server | Pekko HTTP 1.0 |
| Actor System | Pekko 1.0 |
| Database | Slick + PostgreSQL |
| Event Streaming | Alpakka Kafka |
| Configuration | Typesafe Config |
| JSON | Circe |
| Testing | ScalaTest + Testcontainers |
| Scala Version | **Scala 2.13** |

## 🔄 Stack Comparison

| Aspect | ZIO Version | Pekko Version |
|--------|-------------|---------------|
| **Paradigm** | Functional Effects | Actor Model / Futures |
| **Error Handling** | Typed errors (ZIO) | Exceptions |
| **Concurrency** | Fibers | Actors / Futures |
| **Type Safety** | Higher | Standard |
| **Learning Curve** | Steeper | More familiar |
| **Maturity** | Newer | Battle-tested |

## 🚀 Quick Start

Both projects share the same API and functionality. Choose based on your preferred tech stack:

```bash
# ZIO Version
cd workout-zio
docker-compose up -d postgres kafka zookeeper
sbt run

# Pekko Version
cd workout-pekko
docker-compose up -d postgres kafka zookeeper
sbt run
```

## 📡 Common API

Both services expose identical REST endpoints:

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health check |
| GET | `/api/workouts` | List all workouts |
| GET | `/api/workouts/:id` | Get workout by ID |
| POST | `/api/workouts` | Create new workout |
| PUT | `/api/workouts/:id` | Update workout |
| DELETE | `/api/workouts/:id` | Delete workout |
| GET | `/api/workouts/type/:type` | Filter by workout type |
| GET | `/api/workouts/difficulty/:difficulty` | Filter by difficulty |

## 📝 Example Request

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

## 📋 Prerequisites

- JDK 17+
- sbt 1.9+
- Docker & Docker Compose

## 📜 License

MIT License

