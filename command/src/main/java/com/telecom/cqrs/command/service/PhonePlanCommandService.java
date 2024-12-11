package com.telecom.cqrs.command.service;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.azure.messaging.eventhubs.models.CreateBatchOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.cqrs.command.domain.PhonePlan;
import com.telecom.cqrs.common.constant.EventHubConstants;
import com.telecom.cqrs.common.dto.UsageUpdateRequest;
import com.telecom.cqrs.common.dto.UsageUpdateResponse;
import com.telecom.cqrs.common.event.PhonePlanEvent;
import com.telecom.cqrs.common.event.UsageUpdatedEvent;
import com.telecom.cqrs.common.exception.EventHubException;
import com.telecom.cqrs.common.exception.PhonePlanChangeException;
import com.telecom.cqrs.common.exception.UsageUpdateException;
import com.telecom.cqrs.command.repository.PhonePlanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class PhonePlanCommandService {
    private final PhonePlanRepository phonePlanRepository;
    private final EventHubProducerClient planEventProducer;
    private final EventHubProducerClient usageEventProducer;
    private final ObjectMapper objectMapper;

    public PhonePlanCommandService(
            PhonePlanRepository phonePlanRepository,
            @Qualifier("planEventProducer") EventHubProducerClient planEventProducer,
            @Qualifier("usageEventProducer") EventHubProducerClient usageEventProducer,
            ObjectMapper objectMapper) {
        this.phonePlanRepository = phonePlanRepository;
        this.planEventProducer = planEventProducer;
        this.usageEventProducer = usageEventProducer;
        this.objectMapper = objectMapper;
    }

    public PhonePlan changePhonePlan(PhonePlan phonePlan) {
        try {
            PhonePlan savedPlan = savePlan(phonePlan);
            PhonePlanEvent event = createPlanEvent(savedPlan);
            publishEvent(event, savedPlan.getUserId(), planEventProducer);
            return savedPlan;
        } catch (Exception e) {
            log.warn("요금제 변경 실패: userId={}, error={}", maskUserId(phonePlan.getUserId()), e.getMessage());
            throw new PhonePlanChangeException("요금제 변경 중 오류가 발생했습니다", e);
        }
    }

    private <T> void publishEvent(T event, String partitionKey, EventHubProducerClient producer) {
        try {
            CreateBatchOptions options = new CreateBatchOptions()
                    .setPartitionKey(partitionKey);

            EventDataBatch batch = producer.createBatch(options);
            String eventJson = objectMapper.writeValueAsString(event);
            EventData eventData = new EventData(eventJson);

            String eventType = getEventType(event);
            eventData.getProperties().put("type", eventType);

            if (!batch.tryAdd(eventData)) {
                throw new EventHubException("이벤트 크기가 너무 큽니다");
            }

            producer.send(batch);
            log.info("이벤트 발행 완료: type={}, userId={}", eventType, maskUserId(partitionKey));

        } catch (Exception e) {
            log.warn("이벤트 발행 실패: type={}, userId={}, error={}",
                    event.getClass().getSimpleName(), maskUserId(partitionKey), e.getMessage());
            throw new EventHubException("이벤트 발행 중 오류가 발생했습니다", e);
        }
    }

    public UsageUpdateResponse updateUsage(UsageUpdateRequest request) {
        try {
            phonePlanRepository.findByUserId(request.getUserId())
                    .orElseThrow(() -> new UsageUpdateException("존재하지 않는 사용자입니다: " + maskUserId(request.getUserId())));

            UsageUpdatedEvent event = createUsageEvent(request);
            publishEvent(event, request.getUserId(), usageEventProducer);

            return UsageUpdateResponse.builder()
                    .success(true)
                    .message("사용량 업데이트가 완료되었습니다")
                    .userId(maskUserId(request.getUserId()))
                    .build();
        } catch (Exception e) {
            log.warn("사용량 업데이트 실패: userId={}, error={}", maskUserId(request.getUserId()), e.getMessage());
            throw new UsageUpdateException("사용량 업데이트 중 오류가 발생했습니다", e);
        }
    }

    private void update(PhonePlan existingPlan, PhonePlan newPlan) {
        existingPlan.setPlanName(newPlan.getPlanName());
        existingPlan.setDataAllowance(newPlan.getDataAllowance());
        existingPlan.setCallMinutes(newPlan.getCallMinutes());
        existingPlan.setMessageCount(newPlan.getMessageCount());
        existingPlan.setMonthlyFee(newPlan.getMonthlyFee());
        existingPlan.setStatus(newPlan.getStatus() == null ? existingPlan.getStatus() : newPlan.getStatus());
    }

    private PhonePlanEvent createPlanEvent(PhonePlan plan) {
        return PhonePlanEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("PLAN_CHANGED")
                .userId(plan.getUserId())
                .planName(plan.getPlanName())
                .dataAllowance(plan.getDataAllowance())
                .callMinutes(plan.getCallMinutes())
                .messageCount(plan.getMessageCount())
                .monthlyFee(plan.getMonthlyFee())
                .status(plan.getStatus())
                .timestamp(LocalDateTime.now())
                .build();
    }

    private UsageUpdatedEvent createUsageEvent(UsageUpdateRequest request) {
        return UsageUpdatedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("USAGE_UPDATED")
                .userId(request.getUserId())
                .dataUsage(request.getDataUsage())
                .callUsage(request.getCallUsage())
                .messageUsage(request.getMessageUsage())
                .timestamp(LocalDateTime.now())
                .build();
    }

    private String getEventType(Object event) {
        if (event instanceof PhonePlanEvent) {
            return EventHubConstants.EVENT_TYPE_PLAN;
        } else if (event instanceof UsageUpdatedEvent) {
            return EventHubConstants.EVENT_TYPE_USAGE;
        }
        throw new IllegalArgumentException("지원되지 않는 이벤트 타입입니다: " + event.getClass().getSimpleName());
    }

    @Transactional
    protected PhonePlan savePlan(PhonePlan phonePlan) {
        return phonePlanRepository.findByUserId(phonePlan.getUserId())
                .map(existingPlan -> {
                    update(existingPlan, phonePlan);
                    return phonePlanRepository.save(existingPlan);
                })
                .orElseGet(() -> phonePlanRepository.save(phonePlan));
    }

    private String maskUserId(String userId) {
        if (userId == null || userId.isEmpty()) {
            return userId;
        }
        return userId.replaceAll("(\\w{2})(\\w+)(\\w{2})", "$1****$3");
    }
}