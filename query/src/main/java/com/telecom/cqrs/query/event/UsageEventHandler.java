// query/src/main/java/com/telecom/cqrs/query/event/UsageEventHandler.java
package com.telecom.cqrs.query.event;

import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.azure.messaging.eventhubs.models.ErrorContext;
import com.azure.messaging.eventhubs.models.EventContext;
import com.azure.storage.blob.BlobContainerAsyncClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.cqrs.common.event.UsageUpdatedEvent;
import com.telecom.cqrs.query.domain.PhonePlanView;
import com.telecom.cqrs.query.repository.PhonePlanViewRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageEventHandler {
    private final PhonePlanViewRepository phonePlanViewRepository;
    private final ObjectMapper objectMapper;

    @Value("${EVENT_HUB_CONNECTION_STRING}")
    private String eventHubConnectionString;

    @Value("${STORAGE_CONNECTION_STRING}")
    private String storageConnectionString;

    private EventProcessorClient eventProcessorClient;

    @PostConstruct
    public void startEventProcessing() {
        log.info("Starting Usage Event Processor...");
        BlobContainerAsyncClient blobContainerClient = new BlobContainerClientBuilder()
                .connectionString(storageConnectionString)
                .containerName("usage-event-checkpoints")
                .buildAsyncClient();

        eventProcessorClient = new EventProcessorClientBuilder()
                .connectionString(eventHubConnectionString, "usage-events")
                .consumerGroup(EventHubClientBuilder.DEFAULT_CONSUMER_GROUP_NAME)
                .checkpointStore(new BlobCheckpointStore(blobContainerClient))
                .processEvent(this::processEvent)
                .processError(this::processError)
                .buildEventProcessorClient();

        eventProcessorClient.start();
        log.info("Usage Event Processor started successfully");
    }

    private void processEvent(EventContext eventContext) {
        try {
            String eventData = eventContext.getEventData().getBodyAsString();
            log.info("Received usage event data: {}", eventData);

            UsageUpdatedEvent usageEvent = objectMapper.readValue(eventData, UsageUpdatedEvent.class);
            log.info("Parsed UsageUpdatedEvent: userId={}, dataUsage={}, callUsage={}, messageUsage={}",
                    usageEvent.getUserId(), usageEvent.getDataUsage(),
                    usageEvent.getCallUsage(), usageEvent.getMessageUsage());

            handleUsageEvent(usageEvent);
            eventContext.updateCheckpoint();
            log.info("Usage event processed and checkpoint updated for userId={}", usageEvent.getUserId());

        } catch (Exception e) {
            log.error("Error processing usage event: {}", e.getMessage(), e);
        }
    }

    private void processError(ErrorContext errorContext) {
        log.error("Error in usage event processor: {}", errorContext.getThrowable().getMessage(),
                errorContext.getThrowable());
    }

    private void handleUsageEvent(UsageUpdatedEvent event) {
        try {
            PhonePlanView view = phonePlanViewRepository.findByUserId(event.getUserId());
            if (view == null) {
                log.warn("No PhonePlanView found for userId={}, skipping usage update", event.getUserId());
                return;
            }

            log.info("Updating usage for PhonePlanView userId={}", event.getUserId());

            // 사용량 업데이트
            if (event.getDataUsage() != null) {
                view.setDataUsage(event.getDataUsage());
            }
            if (event.getCallUsage() != null) {
                view.setCallUsage(event.getCallUsage());
            }
            if (event.getMessageUsage() != null) {
                view.setMessageUsage(event.getMessageUsage());
            }

            phonePlanViewRepository.save(view);
            log.info("Usage updated successfully for userId={}", event.getUserId());

        } catch (Exception e) {
            log.error("Error handling usage event for userId={}: {}",
                    event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    @PreDestroy
    public void cleanup() {
        if (eventProcessorClient != null) {
            log.info("Stopping Usage Event Processor...");
            eventProcessorClient.stop();
            log.info("Usage Event Processor stopped");
        }
    }
}