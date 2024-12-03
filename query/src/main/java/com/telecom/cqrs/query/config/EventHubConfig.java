package com.telecom.cqrs.query.config;

import com.azure.messaging.eventhubs.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class EventHubConfig {

    @Value("${EVENT_HUB_CONNECTION_STRING}")
    private String connectionString;

    @PostConstruct
    public void validateConfig() {
        if (connectionString == null || connectionString.trim().isEmpty()) {
            throw new IllegalStateException("Event Hub connection string not configured");
        }
        log.info("Event Hub connection string validated");
    }

    @Bean
    public EventHubProducerClient eventHubProducerClient() {
        return new EventHubClientBuilder()
                .connectionString(connectionString, "phone-plan-events")
                .buildProducerClient();
    }

    @Bean
    public EventHubConsumerClient eventHubConsumerClient() {
        return new EventHubClientBuilder()
                .connectionString(connectionString, "phone-plan-events")
                .consumerGroup(EventHubClientBuilder.DEFAULT_CONSUMER_GROUP_NAME)
                .buildConsumerClient();
    }
}