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
import com.telecom.cqrs.common.event.PhonePlanEvent;
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
public class PhonePlanEventHandler {
    private final PhonePlanViewRepository phonePlanViewRepository;
    private final ObjectMapper objectMapper;

    @Value("${EVENT_HUB_CONNECTION_STRING}")
    private String eventHubConnectionString;

    @Value("${STORAGE_CONNECTION_STRING}")
    private String storageConnectionString;

    private EventProcessorClient eventProcessorClient;

    @PostConstruct
    public void startEventProcessing() {
        log.info("Starting Event Processor...");
        BlobContainerAsyncClient blobContainerClient = new BlobContainerClientBuilder()
                .connectionString(storageConnectionString)
                .containerName("eventhub-checkpoints")
                .buildAsyncClient();

        eventProcessorClient = new EventProcessorClientBuilder()
                .connectionString(eventHubConnectionString, "phone-plan-events")
                .consumerGroup(EventHubClientBuilder.DEFAULT_CONSUMER_GROUP_NAME)
                .checkpointStore(new BlobCheckpointStore(blobContainerClient))
                .processEvent(this::processEvent)
                .processError(this::processError)
                .buildEventProcessorClient();

        eventProcessorClient.start();
        log.info("Event Processor started successfully");
    }

    private void processEvent(EventContext eventContext) {
        try {
            String eventData = eventContext.getEventData().getBodyAsString();
            log.info("Received event data: {}", eventData);

            PhonePlanEvent phonePlanEvent = objectMapper.readValue(eventData, PhonePlanEvent.class);
            log.info("Parsed PhonePlanEvent: userId={}, planName={}",
                    phonePlanEvent.getUserId(), phonePlanEvent.getPlanName());

            handlePhonePlanEvent(phonePlanEvent);
            eventContext.updateCheckpoint();
            log.info("Event processed and checkpoint updated for userId={}", phonePlanEvent.getUserId());

        } catch (Exception e) {
            log.error("Error processing event: {}", e.getMessage(), e);
        }
    }

    private void processError(ErrorContext errorContext) {
        log.error("Error in partition processor: {}", errorContext.getThrowable().getMessage(),
                errorContext.getThrowable());
    }

    private void handlePhonePlanEvent(PhonePlanEvent event) {
        try {
            PhonePlanView view = phonePlanViewRepository.findByUserId(event.getUserId());
            if (view == null) {
                log.info("Creating new PhonePlanView for userId={}", event.getUserId());
                view = new PhonePlanView();
                view.setUserId(event.getUserId());
            } else {
                log.info("Updating existing PhonePlanView for userId={}", event.getUserId());
            }

            view.setPlanName(event.getPlanName());
            view.setDataAllowance(event.getDataAllowance());
            view.setCallMinutes(event.getCallMinutes());
            view.setMessageCount(event.getMessageCount());
            view.setMonthlyFee(event.getMonthlyFee());
            view.setStatus(event.getStatus());

            phonePlanViewRepository.save(view);
            log.info("PhonePlanView saved successfully for userId={}", event.getUserId());

        } catch (Exception e) {
            log.error("Error handling event for userId={}: {}",
                    event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    @PreDestroy
    public void cleanup() {
        if (eventProcessorClient != null) {
            log.info("Stopping Event Processor...");
            eventProcessorClient.stop();
            log.info("Event Processor stopped");
        }
    }
}