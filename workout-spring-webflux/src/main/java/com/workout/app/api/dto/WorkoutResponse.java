package com.workout.app.api.dto;

import com.workout.app.domain.Workout;

public record WorkoutResponse(boolean success, Workout data, String message) {
    public static WorkoutResponse success(Workout w) {
        return new WorkoutResponse(true, w, null);
    }
    public static WorkoutResponse error(String msg) {
        return new WorkoutResponse(false, null, msg);
    }
}

