package com.workout.app.repository;

import com.workout.app.domain.Workout;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface WorkoutRepository extends ReactiveCrudRepository<Workout, UUID> {
    
    @Query("SELECT * FROM workouts ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    Flux<Workout> findAllPaged(int limit, int offset);

    @Query("SELECT * FROM workouts WHERE workout_type = :type ORDER BY created_at DESC")
    Flux<Workout> findByType(String type);

    @Query("SELECT * FROM workouts WHERE difficulty = :difficulty ORDER BY created_at DESC")
    Flux<Workout> findByDifficulty(String difficulty);
}

