package com.telecom.cqrs.query.metrics;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class EventProcessingMetrics {
    @Getter
    private final AtomicLong planEventsProcessed = new AtomicLong(0);
    @Getter
    private final AtomicLong usageEventsProcessed = new AtomicLong(0);
    @Getter
    private final AtomicLong planEventErrors = new AtomicLong(0);
    @Getter
    private final AtomicLong usageEventErrors = new AtomicLong(0);

    public void incrementPlanEventsProcessed() {
        planEventsProcessed.incrementAndGet();
    }

    public void incrementUsageEventsProcessed() {
        usageEventsProcessed.incrementAndGet();
    }

    public void incrementPlanEventErrors() {
        planEventErrors.incrementAndGet();
    }

    public void incrementUsageEventErrors() {
        usageEventErrors.incrementAndGet();
    }

    public String getMetricsSummary() {
        return String.format(
                "Metrics - Plan Events (Processed: %d, Errors: %d), Usage Events (Processed: %d, Errors: %d)",
                planEventsProcessed.get(),
                planEventErrors.get(),
                usageEventsProcessed.get(),
                usageEventErrors.get()
        );
    }
}