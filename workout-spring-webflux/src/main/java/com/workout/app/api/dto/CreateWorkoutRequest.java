package com.workout.app.api.dto;

import com.workout.app.domain.Difficulty;
import com.workout.app.domain.WorkoutType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateWorkoutRequest(
        @NotBlank String name,
        String description,
        @NotNull WorkoutType workoutType,
        @Min(1) int durationMinutes,
        Integer caloriesBurned,
        @NotNull Difficulty difficulty
) {}

