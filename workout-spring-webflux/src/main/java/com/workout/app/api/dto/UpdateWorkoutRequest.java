package com.workout.app.api.dto;

import com.workout.app.domain.Difficulty;
import com.workout.app.domain.WorkoutType;

public record UpdateWorkoutRequest(
        String name,
        String description,
        WorkoutType workoutType,
        Integer durationMinutes,
        Integer caloriesBurned,
        Difficulty difficulty
) {}

