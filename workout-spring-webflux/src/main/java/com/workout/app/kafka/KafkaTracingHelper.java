package com.workout.app.kafka;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.kafka.receiver.observation.KafkaReceiverObservation;
import reactor.kafka.receiver.observation.KafkaRecordReceiverContext;

@Component
@RequiredArgsConstructor
public class KafkaTracingHelper {

    private final ObservationRegistry observationRegistry;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Wraps a Kafka record processing Mono with Micrometer Observation for
     * distributed tracing.
     * This follows the official Reactor Kafka recommendation for consumer tracing.
     */
    public <K, V, T> Mono<T> traceConsumer(ReceiverRecord<K, V> event, String observationName,
            Mono<T> processingMono) {
        Observation receiverObservation = KafkaReceiverObservation.RECEIVER_OBSERVATION.start(
                null,
                KafkaReceiverObservation.DefaultKafkaReceiverObservationConvention.INSTANCE,
                () -> new KafkaRecordReceiverContext(event, observationName, bootstrapServers),
                observationRegistry);

        return processingMono
                .doOnTerminate(receiverObservation::stop)
                .doOnError(receiverObservation::error)
                .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, receiverObservation));
    }
}
