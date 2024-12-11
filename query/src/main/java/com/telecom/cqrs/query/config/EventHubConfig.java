package com.telecom.cqrs.query.config;

import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.telecom.cqrs.query.event.PhonePlanEventHandler;
import com.telecom.cqrs.query.event.UsageEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
public class EventHubConfig {
    @Value("${EVENT_HUB_CONNECTION_STRING}")
    private String connectionString;

    @Value("${EVENT_HUB_PLAN_GROUP:plan-consumer}")
    private String planConsumerGroup;

    @Value("${EVENT_HUB_USAGE_GROUP:usage-consumer}")
    private String usageConsumerGroup;

    @Value("${EVENT_HUB_PLAN_NAME}")
    private String planHubName;

    @Value("${EVENT_HUB_USAGE_NAME}")
    private String usageHubName;

    private final BlobStorageConfig blobStorageConfig;
    private final UsageEventHandler usageEventHandler;
    private final PhonePlanEventHandler planEventHandler;

    public EventHubConfig(
            BlobStorageConfig blobStorageConfig,
            UsageEventHandler usageEventHandler,
            PhonePlanEventHandler planEventHandler) {
        this.blobStorageConfig = blobStorageConfig;
        this.usageEventHandler = usageEventHandler;
        this.planEventHandler = planEventHandler;
    }

    @PostConstruct
    public void validateConfig() {
        log.info("Validating Event Hub configuration...");
        validateNotEmpty("connectionString", connectionString);
        validateNotEmpty("planHubName", planHubName);
        validateNotEmpty("usageHubName", usageHubName);
        validateNotEmpty("planConsumerGroup", planConsumerGroup);
        validateNotEmpty("usageConsumerGroup", usageConsumerGroup);
        log.info("Event Hub configuration validated successfully");
    }

    private void validateNotEmpty(String name, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                    String.format("Event Hub configuration error: %s is not configured", name));
        }
    }

    @Bean
    public EventProcessorClient usageEventProcessor() {
        log.info("Creating usage event processor with hub: {}, consumer group: {}",
                usageHubName, usageConsumerGroup);

        var blobClient = blobStorageConfig
                .getBlobContainerAsyncClient(BlobStorageContainers.USAGE_CONTAINER);

        return new EventProcessorClientBuilder()
                .connectionString(connectionString, usageHubName)
                .consumerGroup(usageConsumerGroup)
                .checkpointStore(new BlobCheckpointStore(blobClient))
                .processEvent(usageEventHandler)    // 핸들러 직접 전달
                .processError(usageEventHandler::processError)
                .buildEventProcessorClient();
    }

    @Bean
    public EventProcessorClient planEventProcessor() {
        log.info("Creating plan event processor with hub: {}, consumer group: {}",
                planHubName, planConsumerGroup);

        var blobClient = blobStorageConfig
                .getBlobContainerAsyncClient(BlobStorageContainers.PLAN_CONTAINER);

        return new EventProcessorClientBuilder()
                .connectionString(connectionString, planHubName)
                .consumerGroup(planConsumerGroup)
                .checkpointStore(new BlobCheckpointStore(blobClient))
                .processEvent(planEventHandler)    // 핸들러 직접 전달
                .processError(planEventHandler::processError)
                .buildEventProcessorClient();
    }
}