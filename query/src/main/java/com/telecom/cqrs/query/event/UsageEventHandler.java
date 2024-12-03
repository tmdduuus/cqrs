// File: cqrs/query/src/main/java/com/telecom/cqrs/query/event/UsageEventHandler.java
package com.telecom.cqrs.query.event;

import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.models.ErrorContext;
import com.azure.messaging.eventhubs.models.EventContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.cqrs.common.event.UsageUpdatedEvent;
import com.telecom.cqrs.query.domain.PhonePlanView;
import com.telecom.cqrs.query.repository.PhonePlanViewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UsageEventHandler {
    private final PhonePlanViewRepository phonePlanViewRepository;
    private final ObjectMapper objectMapper;
    private EventProcessorClient eventProcessorClient;

    public UsageEventHandler(
            PhonePlanViewRepository phonePlanViewRepository,
            ObjectMapper objectMapper) {
        this.phonePlanViewRepository = phonePlanViewRepository;
        this.objectMapper = objectMapper;
    }

    public void setEventProcessorClient(EventProcessorClient client) {
        this.eventProcessorClient = client;
        startEventProcessing();
    }

    public void startEventProcessing() {
        if (eventProcessorClient != null) {
            log.info("Starting Usage Event Processor...");
            try {
                eventProcessorClient.start();
                log.info("Usage Event Processor started successfully");
            } catch (Exception e) {
                log.error("Failed to start Usage Event Processor: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to start event processor", e);
            }
        }
    }

    public void processEvent(EventContext eventContext) {
        String eventData = eventContext.getEventData().getBodyAsString();

        try {
            // 이벤트 정보 로깅
            log.info("Processing usage event: partition={}, offset={}",
                    eventContext.getPartitionContext().getPartitionId(),
                    eventContext.getEventData().getSequenceNumber());

            UsageUpdatedEvent event = objectMapper.readValue(eventData, UsageUpdatedEvent.class);
            handleUsageEvent(event);

            // 체크포인트 갱신
            eventContext.updateCheckpoint();

            log.info("Successfully processed usage event for userId={}", event.getUserId());

        } catch (Exception e) {
            log.error("Failed to process usage event: {}", eventData, e);
        }
    }

    public void processError(ErrorContext errorContext) {
        log.error("Error in usage event processor: {}",
                errorContext.getThrowable().getMessage(),
                errorContext.getThrowable());
    }

    private void handleUsageEvent(UsageUpdatedEvent event) {
        try {
            PhonePlanView view = phonePlanViewRepository.findByUserId(event.getUserId());
            if (view == null) {
                log.warn("No PhonePlanView found for userId={}, skipping usage update",
                        event.getUserId());
                return;
            }

            updateViewFromEvent(view, event);
            phonePlanViewRepository.save(view);
            log.info("Usage updated successfully for userId={}", event.getUserId());

        } catch (Exception e) {
            log.error("Error handling usage event for userId={}: {}",
                    event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    private void updateViewFromEvent(PhonePlanView view, UsageUpdatedEvent event) {
        if (event.getDataUsage() != null) {
            view.setDataUsage(event.getDataUsage());
        }
        if (event.getCallUsage() != null) {
            view.setCallUsage(event.getCallUsage());
        }
        if (event.getMessageUsage() != null) {
            view.setMessageUsage(event.getMessageUsage());
        }
    }

    public void cleanup() {
        if (eventProcessorClient != null) {
            try {
                log.info("Stopping Usage Event Processor...");
                eventProcessorClient.stop();
                log.info("Usage Event Processor stopped");
            } catch (Exception e) {
                log.error("Error stopping Usage Event Processor: {}", e.getMessage(), e);
            }
        }
    }
}