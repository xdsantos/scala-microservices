package com.workout.app.mapper;

import com.workout.app.api.dto.CreateWorkoutRequest;
import com.workout.app.api.dto.UpdateWorkoutRequest;
import com.workout.app.domain.Workout;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.Instant;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface WorkoutMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "workoutType", expression = "java(request.workoutType().name())")
    @Mapping(target = "difficulty", expression = "java(request.difficulty().name())")
    @Mapping(target = "createdAt", expression = "java(java.time.Instant.now())")
    @Mapping(target = "updatedAt", expression = "java(java.time.Instant.now())")
    Workout toWorkout(CreateWorkoutRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "workoutType", ignore = true)
    @Mapping(target = "difficulty", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateWorkoutFromRequest(UpdateWorkoutRequest request, @MappingTarget Workout existing);

    @AfterMapping
    default void updateWorkoutEnumsAndTimestamp(UpdateWorkoutRequest request, @MappingTarget Workout existing) {
        if (request.workoutType() != null) {
            existing.setWorkoutType(request.workoutType().name());
        }
        if (request.difficulty() != null) {
            existing.setDifficulty(request.difficulty().name());
        }
        existing.setUpdatedAt(Instant.now());
    }
}
