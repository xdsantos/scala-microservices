package com.workout.app.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workout.app.api.dto.CreateWorkoutRequest;
import com.workout.app.service.WorkoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkoutCommandConsumer {

    private final WorkoutService workoutService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topics.commands}", groupId = "${kafka.group-id}")
    public void consume(String message) {
        try {
            Map<String, Object> map = objectMapper.readValue(message, Map.class);
            String correlationId = (String) map.get("correlationId");
            
            CreateWorkoutRequest request = objectMapper.convertValue(map.get("request"), CreateWorkoutRequest.class);
            
            log.info("Received create command for correlationId: {}", correlationId);

            workoutService.create(request).subscribe();

        } catch (Exception e) {
            log.error("Error processing Kafka message", e);
        }
    }
}

