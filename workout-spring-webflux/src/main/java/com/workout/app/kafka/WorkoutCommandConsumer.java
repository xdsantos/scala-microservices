package com.workout.app.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workout.app.api.dto.CreateWorkoutRequest;
import com.workout.app.service.WorkoutService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkoutCommandConsumer {

    private final WorkoutService workoutService;
    private final ObjectMapper objectMapper;
    private final KafkaReceiver<String, String> kafkaReceiver;
    private final KafkaTracingHelper tracingHelper;

    @PostConstruct
    public void start() {
        log.info("Starting Kafka consumer...");

        kafkaReceiver.receive()
                .flatMap(event -> tracingHelper.traceConsumer(
                        event,
                        "workout.command.consume",
                        processMessage(event.value())
                                .doOnSuccess(v -> {
                                    event.receiverOffset().acknowledge();
                                    log.info("Successfully processed and acknowledged record");
                                })
                                .onErrorResume(e -> {
                                    log.error("Error processing Kafka message", e);
                                    return Mono.empty();
                                })))
                .subscribe();
    }

    private Mono<Void> processMessage(String message) {
        return Mono.defer(() -> {
            try {
                Map<String, Object> map = objectMapper.readValue(message, new TypeReference<>() {
                });
                String correlationId = (String) map.get("correlationId");
                CreateWorkoutRequest request = objectMapper.convertValue(map.get("request"),
                        CreateWorkoutRequest.class);

                log.info("Processing workout creation for correlationId: {}", correlationId);

                return workoutService.create(request).then();
            } catch (Exception e) {
                return Mono.error(e);
            }
        });
    }
}
