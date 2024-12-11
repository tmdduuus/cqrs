package com.telecom.cqrs.query.event;

import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.models.ErrorContext;
import com.azure.messaging.eventhubs.models.EventContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.cqrs.common.event.UsageUpdatedEvent;
import com.telecom.cqrs.query.domain.PhonePlanView;
import com.telecom.cqrs.query.exception.EventProcessingException;
import com.telecom.cqrs.query.repository.PhonePlanViewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Slf4j
@Service
public class UsageEventHandler implements Consumer<EventContext> {
    private final PhonePlanViewRepository phonePlanViewRepository;
    private final ObjectMapper objectMapper;
    private final RetryTemplate retryTemplate;
    private final AtomicLong eventsProcessed = new AtomicLong(0);
    private final AtomicLong eventErrors = new AtomicLong(0);
    private EventProcessorClient eventProcessorClient;

    public UsageEventHandler(
            PhonePlanViewRepository phonePlanViewRepository,
            ObjectMapper objectMapper,
            RetryTemplate retryTemplate) {
        this.phonePlanViewRepository = phonePlanViewRepository;
        this.objectMapper = objectMapper;
        this.retryTemplate = retryTemplate;
    }

    public void setEventProcessorClient(EventProcessorClient client) {
        log.info("Setting Usage Event Processor Client");
        this.eventProcessorClient = client;
        startEventProcessing();
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
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
        } else {
            log.warn("Usage Event Processor Client is not set");
        }
    }

    @Override
    @Transactional
    public void accept(EventContext eventContext) {
        String eventData = eventContext.getEventData().getBodyAsString();
        String partitionId = eventContext.getPartitionContext().getPartitionId();

        try {
            log.debug("Processing usage event: partition={}, offset={}, data={}",
                    partitionId,
                    eventContext.getEventData().getSequenceNumber(),
                    eventData);

            UsageUpdatedEvent event = parseEvent(eventData);
            if (event != null) {
                processUserEvent(event);
                eventsProcessed.incrementAndGet();
                eventContext.updateCheckpoint();
                log.debug("Usage event processed successfully: userId={}", event.getUserId());
            }
        } catch (Exception e) {
            log.error("Failed to process usage event: partition={}, data={}, error={}",
                    partitionId, eventData, e.getMessage(), e);
            eventErrors.incrementAndGet();
        }
    }

    private UsageUpdatedEvent parseEvent(String eventData) {
        try {
            return objectMapper.readValue(eventData, UsageUpdatedEvent.class);
        } catch (Exception e) {
            log.error("Error parsing usage event: {}", e.getMessage(), e);
            return null;
        }
    }

    private void processUserEvent(UsageUpdatedEvent event) {
        try {
            retryTemplate.execute(context -> {
                PhonePlanView view = getPhonePlanView(event.getUserId());
                if (view != null) {
                    updateViewFromEvent(view, event);
                    phonePlanViewRepository.save(view);
                    log.debug("Successfully processed usage event for userId={}", event.getUserId());
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Error processing usage event for userId={}: {}",
                    event.getUserId(), e.getMessage(), e);
            throw new EventProcessingException("Failed to process usage event", e);
        }
    }

    private PhonePlanView getPhonePlanView(String userId) {
        PhonePlanView view = phonePlanViewRepository.findByUserId(userId);
        if (view == null) {
            log.warn("No PhonePlanView found for userId={}, skipping usage update", userId);
        }
        return view;
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

    public void processError(ErrorContext errorContext) {
        log.error("Error in usage event processor: {}, {}",
                errorContext.getThrowable().getMessage(),
                errorContext.getPartitionContext().getPartitionId(),
                errorContext.getThrowable());
        eventErrors.incrementAndGet();
    }

    public long getProcessedEventCount() {
        return eventsProcessed.get();
    }

    public long getErrorCount() {
        return eventErrors.get();
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