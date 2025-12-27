package com.workout.app.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workout.app.api.dto.CreateWorkoutRequest;
import com.workout.app.service.WorkoutService;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkoutCommandConsumer {

    private final WorkoutService workoutService;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;
    private final Propagator propagator;

    @KafkaListener(topics = "${kafka.topics.commands}", groupId = "${kafka.group-id}")
    public void consume(ConsumerRecord<String, String> record) {
        try {
            String message = record.value();
            Map<String, Object> map = objectMapper.readValue(message, Map.class);
            String correlationId = (String) map.get("correlationId");
            
            CreateWorkoutRequest request = objectMapper.convertValue(map.get("request"), CreateWorkoutRequest.class);
            log.info("Received create command for correlationId: {}", correlationId);

            // Extract trace context from headers
            Span.Builder spanBuilder = propagator.extract(record.headers(), (headers, key) -> {
                if (headers.lastHeader(key) != null) {
                    return new String(headers.lastHeader(key).value(), StandardCharsets.UTF_8);
                }
                return null;
            });

            Span span = spanBuilder.name("workout.command.consume").start();
            
            try (Tracer.SpanInScope scope = tracer.withSpan(span)) {
                log.info("Consumer processing within linked trace from headers: traceId={}", span.context().traceId());
                workoutService.create(request).block();
            } finally {
                span.end();
            }

        } catch (Exception e) {
            log.error("Error processing Kafka message", e);
            throw new RuntimeException(e);
        }
    }
}

