package com.workout.app.api;

import com.workout.app.api.dto.*;
import com.workout.app.service.WorkoutService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/workouts")
public record WorkoutController(WorkoutService workoutService) {

    @PostMapping
    public Mono<ResponseEntity<AcceptedResponse>> createWorkout(@Valid @RequestBody CreateWorkoutRequest request) {
        return workoutService.createAsync(request)
                .map(correlationId -> ResponseEntity.status(HttpStatus.ACCEPTED).body(AcceptedResponse.ok(correlationId)));
    }

    @GetMapping
    public Mono<WorkoutListResponse> getAllWorkouts(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        return workoutService.findAll(limit, offset)
                .collectList()
                .map(WorkoutListResponse::success);
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<WorkoutResponse>> getWorkoutById(@PathVariable UUID id) {
        return workoutService.findById(id)
                .map(w -> ResponseEntity.ok(WorkoutResponse.success(w)))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).body(WorkoutResponse.error("Workout not found")));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<WorkoutResponse>> updateWorkout(
            @PathVariable UUID id,
            @RequestBody UpdateWorkoutRequest request) {
        return workoutService.update(id, request)
                .map(w -> ResponseEntity.ok(WorkoutResponse.success(w)))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).body(WorkoutResponse.error("Workout not found")));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteWorkout(@PathVariable UUID id) {
        return workoutService.delete(id)
                .map(deleted -> Boolean.TRUE.equals(deleted) ? ResponseEntity.noContent().<Void>build() : ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{type}")
    public Mono<WorkoutListResponse> getWorkoutsByType(@PathVariable String type) {
        return workoutService.findByType(type)
                .collectList()
                .map(WorkoutListResponse::success);
    }

    @GetMapping("/difficulty/{difficulty}")
    public Mono<WorkoutListResponse> getWorkoutsByDifficulty(@PathVariable String difficulty) {
        return workoutService.findByDifficulty(difficulty)
                .collectList()
                .map(WorkoutListResponse::success);
    }
}

