package com.telecom.cqrs.command.service;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.azure.messaging.eventhubs.models.CreateBatchOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.cqrs.command.domain.PhonePlan;
import com.telecom.cqrs.common.dto.UsageUpdateRequest;
import com.telecom.cqrs.common.dto.UsageUpdateResponse;
import com.telecom.cqrs.common.event.PhonePlanEvent;
import com.telecom.cqrs.common.event.UsageUpdatedEvent;
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

    @Transactional
    public PhonePlan changePhonePlan(PhonePlan phonePlan) {
        try {
            phonePlan.setStatus(phonePlan.getStatus() == null ? "ACTIVE" : phonePlan.getStatus());
            PhonePlan savedPlan = phonePlanRepository.save(phonePlan);
            publishEvent(createPhonePlanEvent(savedPlan), savedPlan.getUserId(), planEventProducer);
            return savedPlan;
        } catch (Exception e) {
            log.error("요금제 변경 실패: userId={}, error={}", phonePlan.getUserId(), e.getMessage(), e);
            throw new RuntimeException("요금제 변경 중 오류가 발생했습니다", e);
        }
    }

    public UsageUpdateResponse updateUsage(UsageUpdateRequest request) {
        try {
            // 사용자 존재 여부 확인
            phonePlanRepository.findByUserId(request.getUserId())
                    .orElseThrow(() -> new UsageUpdateException("존재하지 않는 사용자입니다: " + request.getUserId()));

            // 이벤트 발행
            publishEvent(createUsageEvent(request), request.getUserId(), usageEventProducer);

            return UsageUpdateResponse.builder()
                    .success(true)
                    .message("사용량 업데이트가 완료되었습니다")
                    .userId(request.getUserId())
                    .build();
        } catch (Exception e) {
            log.error("사용량 업데이트 실패: userId={}, error={}", request.getUserId(), e.getMessage(), e);
            throw new UsageUpdateException("사용량 업데이트 중 오류가 발생했습니다", e);
        }
    }

    private void publishEvent(Object event, String partitionKey, EventHubProducerClient producer) {
        try {
            //순서보장을 위해 user id로 partition을 나눔
            CreateBatchOptions options = new CreateBatchOptions()
                    .setPartitionKey(partitionKey);

            EventDataBatch batch = producer.createBatch(options);

            String eventJson = objectMapper.writeValueAsString(event);
            EventData eventData = new EventData(eventJson);

            if (!batch.tryAdd(eventData)) {
                throw new RuntimeException("이벤트 크기가 너무 큽니다");
            }

            producer.send(batch);
            log.info("이벤트 발행 완료: type={}, userId={}", event.getClass().getSimpleName(), partitionKey);

        } catch (Exception e) {
            log.error("이벤트 발행 실패: type={}, userId={}, error={}",
                    event.getClass().getSimpleName(), partitionKey, e.getMessage(), e);
            throw new RuntimeException("이벤트 발행 중 오류가 발생했습니다", e);
        }
    }

    private PhonePlanEvent createPhonePlanEvent(PhonePlan plan) {
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
}
