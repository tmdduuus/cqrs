package com.telecom.cqrs.query.config;

import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.telecom.cqrs.query.event.PhonePlanEventHandler;
import com.telecom.cqrs.query.event.UsageEventHandler;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class EventHubConfig {
    private final BlobStorageConfig blobStorageConfig;
    private final UsageEventHandler usageEventHandler;
    private final PhonePlanEventHandler planEventHandler;

    @Value("${EVENT_HUB_CONNECTION_STRING}")
    private String connectionString;

    public EventHubConfig(
            BlobStorageConfig blobStorageConfig,
            UsageEventHandler usageEventHandler,
            PhonePlanEventHandler planEventHandler) {
        this.blobStorageConfig = blobStorageConfig;
        this.usageEventHandler = usageEventHandler;
        this.planEventHandler = planEventHandler;
    }

    @Bean(name = "usageEventProcessor")
    public EventProcessorClient usageEventProcessor() {
        var blobClient = blobStorageConfig
                .getBlobContainerAsyncClient(BlobStorageContainers.USAGE_CONTAINER);

        return new EventProcessorClientBuilder()
                .connectionString(connectionString, "usage-events")
                .consumerGroup("$Default")
                .checkpointStore(new BlobCheckpointStore(blobClient))
                .processEvent(usageEventHandler::processEvent)
                .processError(usageEventHandler::processError)
                .buildEventProcessorClient();
    }

    @Bean(name = "planEventProcessor")
    public EventProcessorClient planEventProcessor() {
        var blobClient = blobStorageConfig
                .getBlobContainerAsyncClient(BlobStorageContainers.PLAN_CONTAINER);

        return new EventProcessorClientBuilder()
                .connectionString(connectionString, "phone-plan-events")
                .consumerGroup("$Default")
                .checkpointStore(new BlobCheckpointStore(blobClient))
                .processEvent(planEventHandler::processEvent)
                .processError(planEventHandler::processError)
                .buildEventProcessorClient();
    }
}
