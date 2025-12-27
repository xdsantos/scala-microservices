package com.workout.app.api.dto;

import com.workout.app.domain.Workout;
import java.util.List;

public record WorkoutListResponse(boolean success, List<Workout> data, int total, String message) {
    public static WorkoutListResponse success(List<Workout> workouts) {
        return new WorkoutListResponse(true, workouts, workouts.size(), null);
    }
    public static WorkoutListResponse error(String msg) {
        return new WorkoutListResponse(false, List.of(), 0, msg);
    }
}

