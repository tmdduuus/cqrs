package com.telecom.cqrs.query.service;

import com.azure.messaging.eventhubs.EventProcessorClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    public void startProcessors() {
        try {
            log.info("Starting event processors...");
            usageEventProcessor.start();
            planEventProcessor.start();
            log.info("Event processors started successfully");
        } catch (Exception e) {
            log.error("Failed to start event processors: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to start event processors", e);
        }
    }

    @PreDestroy
    public void stopProcessors() {
        try {
            log.info("Stopping event processors...");
            usageEventProcessor.stop();
            planEventProcessor.stop();
            log.info("Event processors stopped successfully");
        } catch (Exception e) {
            log.error("Error stopping event processors: {}", e.getMessage(), e);
        }
    }
}