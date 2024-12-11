package com.telecom.cqrs.command.config;

import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.telecom.cqrs.common.constant.EventHubConstants;
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

    @Bean(name = "usageEventProducer")
    public EventHubProducerClient usageEventProducer() {
        log.info("Creating Usage Event producer for hub: {}", EventHubConstants.USAGE_HUB_NAME);
        return new EventHubClientBuilder()
                .connectionString(connectionString, EventHubConstants.USAGE_HUB_NAME)
                .buildProducerClient();
    }

    @Bean(name = "planEventProducer")
    public EventHubProducerClient planEventProducer() {
        log.info("Creating Plan Event producer for hub: {}", EventHubConstants.PLAN_HUB_NAME);
        return new EventHubClientBuilder()
                .connectionString(connectionString, EventHubConstants.PLAN_HUB_NAME)
                .buildProducerClient();
    }
}