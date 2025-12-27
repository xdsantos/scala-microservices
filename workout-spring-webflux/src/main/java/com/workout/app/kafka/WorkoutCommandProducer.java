package com.workout.app.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workout.app.api.dto.CreateWorkoutRequest;
import io.micrometer.context.ContextSnapshot;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class WorkoutCommandProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;
    private final Propagator propagator;
    private final String commandTopic;

    public WorkoutCommandProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            Tracer tracer,
            Propagator propagator,
            @Value("${kafka.topics.commands}") String commandTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.tracer = tracer;
        this.propagator = propagator;
        this.commandTopic = commandTopic;
    }

    public Mono<UUID> publishCreateCommand(CreateWorkoutRequest request) {
        UUID correlationId = UUID.randomUUID();
        
        return Mono.deferContextual(ctx -> {
            try (ContextSnapshot.Scope scope = ContextSnapshot.setAllThreadLocalsFrom(ctx)) {
                Span currentSpan = tracer.currentSpan();
                
                Map<String, Object> message = new HashMap<>();
                message.put("correlationId", correlationId.toString());
                message.put("request", request);

                try {
                    String json = objectMapper.writeValueAsString(message);
                    ProducerRecord<String, String> record = new ProducerRecord<>(commandTopic, correlationId.toString(), json);
                    
                    if (currentSpan != null) {
                        log.info("Injecting trace headers for correlationId: {} with traceId: {}", correlationId, currentSpan.context().traceId());
                        propagator.inject(currentSpan.context(), record.headers(), (headers, key, value) -> {
                            headers.remove(key);
                            headers.add(key, value.getBytes(StandardCharsets.UTF_8));
                        });
                    }

                    return Mono.fromFuture(kafkaTemplate.send(record))
                            .doOnSuccess(result -> log.info("Published create command for correlationId: {}", correlationId))
                            .thenReturn(correlationId);
                } catch (JsonProcessingException e) {
                    log.error("Error serializing create command", e);
                    return Mono.error(new RuntimeException("Error publishing to Kafka", e));
                }
            }
        });
    }
}

