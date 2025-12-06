# Multi-stage build for workout-service
FROM eclipse-temurin:17-jdk-alpine AS builder

# Install sbt
RUN apk add --no-cache bash curl && \
    curl -fL "https://github.com/sbt/sbt/releases/download/v1.9.7/sbt-1.9.7.tgz" | tar xz -C /usr/local && \
    ln -s /usr/local/sbt/bin/sbt /usr/local/bin/sbt

WORKDIR /app

# Copy build files first for better caching
COPY build.sbt .
COPY project/build.properties project/
COPY project/plugins.sbt project/

# Download dependencies
RUN sbt update

# Copy source code
COPY src/ src/

# Build the application
RUN sbt stage

# Production image
FROM eclipse-temurin:17-jre-alpine

# Install bash (required by sbt-native-packager scripts)
RUN apk add --no-cache bash

WORKDIR /app

# Copy the staged application
COPY --from=builder /app/target/universal/stage/ ./

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup && \
    chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["./bin/workout-service"]

