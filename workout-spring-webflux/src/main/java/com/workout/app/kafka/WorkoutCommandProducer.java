package com.workout.app.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workout.app.api.dto.CreateWorkoutRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkoutCommandProducer {

    private final KafkaSender<String, String> kafkaSender;
    private final ObjectMapper objectMapper;
    
    @Value("${kafka.topics.commands}")
    private String commandTopic;

    public Mono<UUID> publishCreateCommand(CreateWorkoutRequest request) {
        UUID correlationId = UUID.randomUUID();
        
        return Mono.defer(() -> {
            try {
                Map<String, Object> message = Map.of(
                    "correlationId", correlationId.toString(),
                    "request", request
                );

                String json = objectMapper.writeValueAsString(message);
                ProducerRecord<String, String> event = new ProducerRecord<>(commandTopic, correlationId.toString(), json);
                SenderRecord<String, String, UUID> senderRecord = SenderRecord.create(event, correlationId);

                return kafkaSender.send(Mono.just(senderRecord))
                        .next()
                        .doOnSuccess(result -> log.info("Published create command for correlationId: {}", correlationId))
                        .thenReturn(correlationId);

            } catch (JsonProcessingException e) {
                return Mono.error(new RuntimeException(e));
            }
        });
    }
}
