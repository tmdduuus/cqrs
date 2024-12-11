// File: cqrs/query/src/main/java/com/telecom/cqrs/query/event/PhonePlanEventHandler.java
package com.telecom.cqrs.query.event;

import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.models.ErrorContext;
import com.azure.messaging.eventhubs.models.EventContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.cqrs.common.event.PhonePlanEvent;
import com.telecom.cqrs.query.domain.PhonePlanView;
import com.telecom.cqrs.query.repository.PhonePlanViewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PhonePlanEventHandler {
    private final PhonePlanViewRepository phonePlanViewRepository;
    private final ObjectMapper objectMapper;
    private EventProcessorClient eventProcessorClient;

    public PhonePlanEventHandler(
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
            log.info("******[PHONEPLAN] Starting PhonePlan Event Processor...");
            try {
                eventProcessorClient.start();
                log.info("PhonePlan Event Processor started successfully");
            } catch (Exception e) {
                log.error("Failed to start PhonePlan Event Processor: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to start event processor", e);
            }
        }
    }

    public void processEvent(EventContext eventContext) {
        String eventData = eventContext.getEventData().getBodyAsString();

        try {
            // 이벤트 정보 로깅
            log.info("Processing plan event: partition={}, offset={}",
                    eventContext.getPartitionContext().getPartitionId(),
                    eventContext.getEventData().getSequenceNumber());

            PhonePlanEvent event = objectMapper.readValue(eventData, PhonePlanEvent.class);
            handlePhonePlanEvent(event);

            // 체크포인트 갱신
            eventContext.updateCheckpoint();

            log.info("Successfully processed plan event for userId={}", event.getUserId());

        } catch (Exception e) {
            log.error("Failed to process plan event: {}", eventData, e);
        }
    }

    public void processError(ErrorContext errorContext) {
        log.error("Error in plan event processor: {}",
                errorContext.getThrowable().getMessage(),
                errorContext.getThrowable());
    }

    private void handlePhonePlanEvent(PhonePlanEvent event) {
        try {
            PhonePlanView view = phonePlanViewRepository.findByUserId(event.getUserId());
            if (view == null) {
                view = new PhonePlanView();
                view.setUserId(event.getUserId());
            }

            updateViewFromEvent(view, event);
            phonePlanViewRepository.save(view);
            log.info("PhonePlanView saved successfully for userId={}", event.getUserId());

        } catch (Exception e) {
            log.error("Error handling plan event for userId={}: {}",
                    event.getUserId(), e.getMessage(), e);
            throw e;
        }
    }

    private void updateViewFromEvent(PhonePlanView view, PhonePlanEvent event) {
        view.setPlanName(event.getPlanName());
        view.setDataAllowance(event.getDataAllowance());
        view.setCallMinutes(event.getCallMinutes());
        view.setMessageCount(event.getMessageCount());
        view.setMonthlyFee(event.getMonthlyFee());
        view.setStatus(event.getStatus());
    }

    public void cleanup() {
        if (eventProcessorClient != null) {
            try {
                log.info("Stopping PhonePlan Event Processor...");
                eventProcessorClient.stop();
                log.info("PhonePlan Event Processor stopped");
            } catch (Exception e) {
                log.error("Error stopping PhonePlan Event Processor: {}", e.getMessage(), e);
            }
        }
    }
}