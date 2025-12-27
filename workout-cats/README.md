# Workout Cats Effect Microservice

Cats Effect + http4s + Doobie + fs2-kafka implementation of the Workout service (mirrors the Pekko/ZIO versions):

- Async POST: publishes a create command to Kafka, returns 202 with correlationId
- Kafka consumer processes commands and saves workouts to Postgres
- Tracing via OpenTelemetry (OTLP to Jaeger)
- Flyway migrations

## Prerequisites
- JDK 17
- sbt 1.9+
- Docker & Docker Compose

## Run locally (no Docker)
```bash
cd workout-cats
export KAFKA_BOOTSTRAP_SERVERS=localhost:9094
export DATABASE_URL=jdbc:postgresql://localhost:5434/workout_db
export DATABASE_USER=postgres
export DATABASE_PASSWORD=postgres
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4321
sbt run
```

## Run with Docker Compose
```bash
cd workout-cats
docker-compose up -d postgres kafka zookeeper jaeger
docker-compose up -d workout-cats-service
```

## Endpoints
- `GET /health`
- `GET /api/workouts?limit&offset`
- `GET /api/workouts/{id}`
- `POST /api/workouts` (async, returns 202 with correlationId)
- `PUT /api/workouts/{id}`
- `DELETE /api/workouts/{id}`
- `GET /api/workouts/type/{type}`
- `GET /api/workouts/difficulty/{difficulty}`

### Async POST flow
```
POST /api/workouts -> Kafka (workout-commands) -> Consumer -> DB
          202 Accepted (correlationId)
```

## Ports (cats project)
- Service HTTP: 8082
- Postgres: 5434 (mapped to 5432 in container)
- Kafka: 9094 (mapped to 9092 in container)
- Jaeger UI: 18688
- OTLP gRPC: 4321

## Tracing
Jaeger UI: http://localhost:18688  
Select service: `workout-cats-service`

## Kafka topics
- Commands: `workout-commands`
- Events: `workout-events`

