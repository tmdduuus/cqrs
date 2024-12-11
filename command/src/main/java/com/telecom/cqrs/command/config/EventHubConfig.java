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
    @Value("${EVENT_HUB_PLAN_CONNECTION_STRING}")
    private String planConnectionString;

    @Value("${EVENT_HUB_USAGE_CONNECTION_STRING}")
    private String usageConnectionString;

    @Value("${EVENT_HUB_PLAN_NAME}")
    private String planEventHubName;

    @Value("${EVENT_HUB_USAGE_NAME}")
    private String usageEventHubName;

    @Bean(name = "usageEventProducer")
    public EventHubProducerClient usageEventProducer() {
        log.info("Creating Usage Event producer for hub: {}", usageEventHubName);
        return new EventHubClientBuilder()
                .connectionString(usageConnectionString, usageEventHubName)
                .buildProducerClient();
    }

    @Bean(name = "planEventProducer")
    public EventHubProducerClient planEventProducer() {
        log.info("Creating Plan Event producer for hub: {}", planEventHubName);
        return new EventHubClientBuilder()
                .connectionString(planConnectionString, planEventHubName)
                .buildProducerClient();
    }
}