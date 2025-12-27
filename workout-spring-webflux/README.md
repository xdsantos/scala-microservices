# Workout Spring WebFlux Microservice

Spring Boot 3 + WebFlux + Kafka + R2DBC Postgres + Flyway + OpenTelemetry. Async POST publishes a command to Kafka and returns 202 with a correlationId; a consumer persists the workout to Postgres.

## Prereqs
- JDK 17
- Docker & Docker Compose
- Gradle 8.5+ (wrapper recommended)

## Run with Docker Compose
```bash
cd workout-spring-webflux
./gradlew wrapper --gradle-version 8.5
docker-compose up -d postgres kafka zookeeper jaeger adminer
docker-compose up -d workout-webflux-service
```

Services/ports:
- API: http://localhost:8083
- Postgres: 5436 (container 5432)
- Kafka: 9095 (container 29092 internal)
- Jaeger UI: http://localhost:18689 (OTLP gRPC 4323)
- Adminer: http://localhost:8084 (connect to host `postgres`, user/pass `postgres`, db `workout_db`)

## Run locally (no Docker)
Start infra separately (Docker or local). Then:
```bash
cd workout-spring-webflux
./gradlew clean bootRun
```
Env overrides (matching application.yml defaults):
```
SERVER_PORT=8083
DB_HOST=localhost
DB_PORT=5436
DB_USER=postgres
DB_PASSWORD=postgres
KAFKA_BOOTSTRAP_SERVERS=localhost:9095
KAFKA_TOPIC=workout-events
KAFKA_COMMAND_TOPIC=workout-commands
KAFKA_GROUP_ID=workout-webflux-service
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4323
OTEL_ENABLED=true
```

## API
- `GET /health`
- `POST /api/workouts` → 202 Accepted + `correlationId` (command sent to Kafka)
- `GET /api/workouts?limit&offset`
- `GET /api/workouts/{id}`
- `PUT /api/workouts/{id}`
- `DELETE /api/workouts/{id}`
- `GET /api/workouts/type/{type}`
- `GET /api/workouts/difficulty/{difficulty}`

Async flow:
```
POST /api/workouts -> Kafka (workout-commands) -> consumer -> DB
         202 Accepted (correlationId)
```

## Building the image
```bash
cd workout-spring-webflux
docker build --no-cache -t workout-webflux-service .
```

## Notes
- Flyway migration: `src/main/resources/db/migration/V1__create_workouts.sql`
- Tracing: OTLP exporter to Jaeger (see ports above)
- Kafka topics auto-created: `workout-commands`, `workout-events`

