package com.workout.app.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workout.app.api.dto.CreateWorkoutRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class WorkoutCommandProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String commandTopic;

    public WorkoutCommandProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${kafka.topics.commands}") String commandTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.commandTopic = commandTopic;
    }

    public Mono<UUID> publishCreateCommand(CreateWorkoutRequest request) {
        UUID correlationId = UUID.randomUUID();
        
        return Mono.fromCallable(() -> {
            try {
                Map<String, Object> message = Map.of(
                    "correlationId", correlationId.toString(),
                    "request", request
                );

                String json = objectMapper.writeValueAsString(message);
                kafkaTemplate.send(commandTopic, correlationId.toString(), json);
                log.info("Published create command for correlationId: {}", correlationId);
                return correlationId;
            } catch (JsonProcessingException e) {
                log.error("Error serializing create command", e);
                throw new RuntimeException("Error publishing to Kafka", e);
            }
        });
    }
}

