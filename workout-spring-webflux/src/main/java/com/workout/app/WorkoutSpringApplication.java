package com.workout.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Hooks;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
public class WorkoutSpringApplication {
    public static void main(String[] args) {
        SpringApplication.run(WorkoutSpringApplication.class, args);
    }

    @PostConstruct
    public void init() {
        // Bridges Reactor context with Micrometer/ThreadLocal
        Hooks.enableAutomaticContextPropagation();
    }
}
