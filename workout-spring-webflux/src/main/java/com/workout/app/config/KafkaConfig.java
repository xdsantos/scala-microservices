package com.workout.app.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${kafka.topics.commands}")
    private String commandTopic;

    @Value("${kafka.topics.events}")
    private String eventsTopic;

    @Bean
    public NewTopic commandTopic() {
        return new NewTopic(commandTopic, 1, (short) 1);
    }

    @Bean
    public NewTopic eventsTopic() {
        return new NewTopic(eventsTopic, 1, (short) 1);
    }
}
