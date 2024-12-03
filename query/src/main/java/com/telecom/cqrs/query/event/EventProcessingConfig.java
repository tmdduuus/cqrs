// File: cqrs/query/src/main/java/com/telecom/cqrs/query/event/EventProcessingConfig.java
package com.telecom.cqrs.query.event;

import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.messaging.eventhubs.models.ErrorContext;
import com.azure.messaging.eventhubs.models.EventContext;
import com.telecom.cqrs.query.config.BlobStorageConfig;
import com.telecom.cqrs.query.config.BlobStorageContainers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class EventProcessingConfig {
    private final BlobStorageConfig blobStorageConfig;
    private final UsageEventHandler usageEventHandler;
    private final PhonePlanEventHandler phonePlanEventHandler;

    private static final String DEFAULT_CONSUMER_GROUP_NAME = "$Default";

    @Value("${EVENT_HUB_CONNECTION_STRING}")
    private String eventHubConnectionString;

    @Bean(name = "usageEventProcessor")
    public EventProcessorClient usageEventProcessor() {
        return createEventProcessor(
                "usage-events",
                BlobStorageContainers.USAGE_CONTAINER,
                usageEventHandler::processEvent,
                usageEventHandler::processError
        );
    }

    @Bean(name = "planEventProcessor")
    public EventProcessorClient planEventProcessor() {
        return createEventProcessor(
                "phone-plan-events",
                BlobStorageContainers.PLAN_CONTAINER,
                phonePlanEventHandler::processEvent,
                phonePlanEventHandler::processError
        );
    }

    private EventProcessorClient createEventProcessor(
            String eventHubName,
            String containerName,
            java.util.function.Consumer<EventContext> processEvent,
            java.util.function.Consumer<ErrorContext> processError) {
        try {
            var blobClient = blobStorageConfig.getBlobContainerAsyncClient(containerName);

            return new EventProcessorClientBuilder()
                    .connectionString(eventHubConnectionString, eventHubName)
                    .consumerGroup(DEFAULT_CONSUMER_GROUP_NAME)
                    .checkpointStore(new BlobCheckpointStore(blobClient))
                    .processEvent(processEvent)
                    .processError(processError)
                    .buildEventProcessorClient();

        } catch (Exception e) {
            log.error("Failed to create event processor for {}: {}",
                    eventHubName, e.getMessage(), e);
            throw new RuntimeException(
                    "Failed to create event processor for " + eventHubName, e);
        }
    }
}