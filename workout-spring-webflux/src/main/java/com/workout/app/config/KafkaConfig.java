package com.workout.app.config;

import io.micrometer.observation.ObservationRegistry;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.Collections;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${kafka.topics.commands}")
    private String commandTopic;

    @Value("${kafka.topics.events}")
    private String eventsTopic;

    @Value("${kafka.group-id}")
    private String groupId;

    @Bean
    public NewTopic commandTopic() {
        return new NewTopic(commandTopic, 1, (short) 1);
    }

    @Bean
    public NewTopic eventsTopic() {
        return new NewTopic(eventsTopic, 1, (short) 1);
    }

    @Bean
    public KafkaSender<String, String> kafkaSender(KafkaProperties properties, ObservationRegistry observationRegistry) {
        Map<String, Object> props = properties.buildProducerProperties(null);
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, String.join(",", properties.getBootstrapServers()));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        SenderOptions<String, String> senderOptions = SenderOptions.create(props);
        return KafkaSender.create(senderOptions.withObservation(observationRegistry));
    }

    @Bean
    public KafkaReceiver<String, String> kafkaReceiver(KafkaProperties properties, ObservationRegistry observationRegistry) {
        Map<String, Object> props = properties.buildConsumerProperties(null);
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, String.join(",", properties.getBootstrapServers()));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        ReceiverOptions<String, String> receiverOptions = ReceiverOptions.<String, String>create(props)
                .subscription(Collections.singleton(commandTopic))
                .withObservation(observationRegistry);
        return KafkaReceiver.create(receiverOptions);
    }
}
