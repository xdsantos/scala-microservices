package com.workout.app.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workout.app.api.dto.CreateWorkoutRequest;
import com.workout.app.service.WorkoutService;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.observation.KafkaReceiverObservation;
import reactor.kafka.receiver.observation.KafkaRecordReceiverContext;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkoutCommandConsumer {

    private final WorkoutService workoutService;
    private final ObjectMapper objectMapper;
    private final KafkaReceiver<String, String> kafkaReceiver;
    private final ObservationRegistry observationRegistry;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @PostConstruct
    public void start() {
        log.info("Starting Kafka consumer...");
        
        kafkaReceiver.receive()
                .flatMap(event -> {
                    Observation receiverObservation = KafkaReceiverObservation.RECEIVER_OBSERVATION.start(
                            null,
                            KafkaReceiverObservation.DefaultKafkaReceiverObservationConvention.INSTANCE,
                            () -> new KafkaRecordReceiverContext(event, "workout.receiver", bootstrapServers),
                            observationRegistry);

                    return processMessage(event.value())
                            .doOnTerminate(receiverObservation::stop)
                            .doOnError(receiverObservation::error)
                            .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, receiverObservation))
                            .doOnSuccess(v -> {
                                event.receiverOffset().acknowledge();
                                log.info("Successfully processed and acknowledged record");
                            })
                            .onErrorResume(e -> {
                                log.error("Error processing Kafka message", e);
                                return Mono.empty();
                            });
                })
                .subscribe();
    }

    private Mono<Void> processMessage(String message) {
        return Mono.defer(() -> {
            try {
                Map<String, Object> map = objectMapper.readValue(message, new TypeReference<>() {});
                String correlationId = (String) map.get("correlationId");
                CreateWorkoutRequest request = objectMapper.convertValue(map.get("request"), CreateWorkoutRequest.class);
                
                log.info("Processing workout creation for correlationId: {}", correlationId);

                return workoutService.create(request).then();
            } catch (Exception e) {
                return Mono.error(e);
            }
        });
    }
}
