package com.telecom.cqrs.query.service;

import com.azure.messaging.eventhubs.EventProcessorClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;

@Slf4j
@Service
public class EventProcessorService {
    private final EventProcessorClient usageEventProcessor;
    private final EventProcessorClient planEventProcessor;

    public EventProcessorService(
            EventProcessorClient usageEventProcessor,
            EventProcessorClient planEventProcessor) {
        this.usageEventProcessor = usageEventProcessor;
        this.planEventProcessor = planEventProcessor;
    }

    @PostConstruct
    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    public void startProcessors() {
        try {
            log.info("Starting event processors...");

            usageEventProcessor.start();
            log.info("Usage Event Processor started successfully");

            planEventProcessor.start();
            log.info("Plan Event Processor started successfully");

            log.info("All Event processors started successfully");
        } catch (Exception e) {
            log.error("Failed to start event processors: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to start event processors", e);
        }
    }

    @PreDestroy
    public void stopProcessors() {
        try {
            log.info("Stopping event processors...");

            if (usageEventProcessor != null) {
                usageEventProcessor.stop();
                log.info("Usage Event Processor stopped successfully");
            }

            if (planEventProcessor != null) {
                planEventProcessor.stop();
                log.info("Plan Event Processor stopped successfully");
            }

            log.info("All Event processors stopped successfully");
        } catch (Exception e) {
            log.error("Error stopping event processors: {}", e.getMessage(), e);
        }
    }
}
