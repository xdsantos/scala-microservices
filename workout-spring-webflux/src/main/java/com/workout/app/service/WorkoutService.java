package com.workout.app.service;

import com.workout.app.api.dto.CreateWorkoutRequest;
import com.workout.app.api.dto.UpdateWorkoutRequest;
import com.workout.app.domain.Workout;
import com.workout.app.kafka.WorkoutCommandProducer;
import com.workout.app.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkoutService {

    private final WorkoutRepository repository;
    private final WorkoutCommandProducer producer;

    public Mono<UUID> createAsync(CreateWorkoutRequest request) {
        return producer.publishCreateCommand(request);
    }

    public Mono<Workout> create(CreateWorkoutRequest request) {
        Workout workout = new Workout();
        workout.setName(request.name());
        workout.setDescription(request.description());
        workout.setWorkoutType(request.workoutType().name());
        workout.setDurationMinutes(request.durationMinutes());
        workout.setCaloriesBurned(request.caloriesBurned());
        workout.setDifficulty(request.difficulty().name());
        workout.setCreatedAt(Instant.now());
        workout.setUpdatedAt(Instant.now());

        return repository.save(workout)
                .doOnSuccess(w -> log.info("Saved workout to database with id: {}", w.getId()));
    }

    public Mono<Workout> findById(UUID id) {
        return repository.findById(id);
    }

    public Flux<Workout> findAll(int limit, int offset) {
        return repository.findAllPaged(limit, offset);
    }

    public Mono<Workout> update(UUID id, UpdateWorkoutRequest request) {
        return repository.findById(id)
                .flatMap(existing -> {
                    if (request.name() != null) existing.setName(request.name());
                    if (request.description() != null) existing.setDescription(request.description());
                    if (request.workoutType() != null) existing.setWorkoutType(request.workoutType().name());
                    if (request.durationMinutes() != null) existing.setDurationMinutes(request.durationMinutes());
                    if (request.caloriesBurned() != null) existing.setCaloriesBurned(request.caloriesBurned());
                    if (request.difficulty() != null) existing.setDifficulty(request.difficulty().name());
                    existing.setUpdatedAt(Instant.now());
                    return repository.save(existing);
                });
    }

    public Mono<Boolean> delete(UUID id) {
        return repository.findById(id)
                .flatMap(w -> repository.delete(w).thenReturn(true))
                .defaultIfEmpty(false);
    }

    public Flux<Workout> findByType(String type) {
        return repository.findByType(type);
    }

    public Flux<Workout> findByDifficulty(String difficulty) {
        return repository.findByDifficulty(difficulty);
    }
}

