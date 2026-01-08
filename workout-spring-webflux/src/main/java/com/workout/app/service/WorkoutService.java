package com.workout.app.service;

import com.workout.app.api.dto.CreateWorkoutRequest;
import com.workout.app.api.dto.UpdateWorkoutRequest;
import com.workout.app.domain.Workout;
import com.workout.app.kafka.WorkoutCommandProducer;
import com.workout.app.mapper.WorkoutMapper;
import com.workout.app.repository.WorkoutRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkoutService {

    private final WorkoutRepository repository;
    private final WorkoutCommandProducer producer;
    private final WorkoutMapper workoutMapper;

    public Mono<UUID> createAsync(CreateWorkoutRequest request) {
        return producer.publishCreateCommand(request);
    }

    public Mono<Workout> create(CreateWorkoutRequest request) {
        Workout workout = workoutMapper.toWorkout(request);
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
                    workoutMapper.updateWorkoutFromRequest(request, existing);
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

