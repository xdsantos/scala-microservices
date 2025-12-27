package com.workout.app.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("workouts")
public class Workout {
    @Id
    private UUID id;
    private String name;
    private String description;
    private String workoutType;
    private int durationMinutes;
    private Integer caloriesBurned;
    private String difficulty;
    private Instant createdAt;
    private Instant updatedAt;
}
