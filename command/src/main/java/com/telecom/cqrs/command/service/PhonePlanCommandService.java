// command/src/main/java/com/telecom/cqrs/command/service/PhonePlanCommandService.java
package com.telecom.cqrs.command.service;

import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventDataBatch;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telecom.cqrs.command.domain.PhonePlan;
import com.telecom.cqrs.command.dto.UsageUpdateRequest;
import com.telecom.cqrs.command.dto.UsageUpdateResponse;
import com.telecom.cqrs.command.exception.UsageUpdateException;
import com.telecom.cqrs.command.repository.PhonePlanRepository;
import com.telecom.cqrs.common.event.PhonePlanEvent;
import com.telecom.cqrs.common.event.UsageUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 요금제 및 사용량 관련 Command를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PhonePlanCommandService {
    private final PhonePlanRepository phonePlanRepository;
    private final EventHubProducerClient eventHubProducerClient;
    private final ObjectMapper objectMapper;

    /**
     * 요금제를 변경하고 이벤트를 발행합니다.
     */
    @Transactional
    public PhonePlan changePhonePlan(PhonePlan phonePlan) {
        PhonePlan savedPlan = null;
        try {
            // 기본 상태값 설정
            if (phonePlan.getStatus() == null) {
                phonePlan.setStatus("ACTIVE");
            }

            // DB 저장
            savedPlan = phonePlanRepository.save(phonePlan);
            log.info("요금제 정보가 저장되었습니다. userId={}", phonePlan.getUserId());

            // 이벤트 발행
            publishPhonePlanEvent(savedPlan);

            return savedPlan;

        } catch (Exception e) {
            log.error("요금제 변경 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("요금제 변경 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 사용량 업데이트 이벤트를 발행합니다.
     */
    public UsageUpdateResponse updateUsage(UsageUpdateRequest request) {
        try {
            log.info("Publishing usage update event for user: {}", request.getUserId());

            // 사용자 존재 여부 확인
            PhonePlan existingPlan = phonePlanRepository.findByUserId(request.getUserId());
            if (existingPlan == null) {
                throw new UsageUpdateException("존재하지 않는 사용자입니다: " + request.getUserId());
            }

            // 이벤트 발행
            publishUsageEvent(request);

            return UsageUpdateResponse.builder()
                    .success(true)
                    .message("사용량 업데이트 이벤트가 발행되었습니다.")
                    .userId(request.getUserId())
                    .build();

        } catch (Exception e) {
            log.error("Error publishing usage update event for user {}: {}",
                    request.getUserId(), e.getMessage());
            throw new UsageUpdateException("사용량 업데이트 이벤트 발행 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 요금제 변경 이벤트를 발행합니다.
     */
    private void publishPhonePlanEvent(PhonePlan plan) {
        try {
            PhonePlanEvent event = createPhonePlanEvent(plan);
            EventDataBatch batch = eventHubProducerClient.createBatch();

            if (!batch.tryAdd(new EventData(objectMapper.writeValueAsString(event)))) {
                throw new RuntimeException("이벤트 크기가 너무 큽니다");
            }
            eventHubProducerClient.send(batch);
            log.info("요금제 변경 이벤트가 발행되었습니다. userId={}", plan.getUserId());
        } catch (Exception e) {
            log.error("이벤트 발행 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("이벤트 발행 중 오류가 발생했습니다", e);
        }
    }

    /**
     * 사용량 업데이트 이벤트를 발행합니다.
     */
    private void publishUsageEvent(UsageUpdateRequest request) {
        try {
            UsageUpdatedEvent event = createUsageEvent(request);
            EventDataBatch batch = eventHubProducerClient.createBatch();

            if (!batch.tryAdd(new EventData(objectMapper.writeValueAsString(event)))) {
                throw new UsageUpdateException("이벤트 크기가 너무 큽니다");
            }

            eventHubProducerClient.send(batch);
            log.info("사용량 업데이트 이벤트가 발행되었습니다. userId={}", request.getUserId());

        } catch (Exception e) {
            log.error("사용량 이벤트 발행 중 오류 발생: {}", e.getMessage(), e);
            throw new UsageUpdateException("사용량 이벤트 발행 중 오류가 발생했습니다", e);
        }
    }

    private PhonePlanEvent createPhonePlanEvent(PhonePlan plan) {
        PhonePlanEvent event = new PhonePlanEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("PLAN_CHANGED");
        event.setUserId(plan.getUserId());
        event.setPlanName(plan.getPlanName());
        event.setDataAllowance(plan.getDataAllowance());
        event.setCallMinutes(plan.getCallMinutes());
        event.setMessageCount(plan.getMessageCount());
        event.setMonthlyFee(plan.getMonthlyFee());
        event.setStatus(plan.getStatus());
        event.setTimestamp(LocalDateTime.now());
        return event;
    }

    private UsageUpdatedEvent createUsageEvent(UsageUpdateRequest request) {
        UsageUpdatedEvent event = new UsageUpdatedEvent();
        event.setEventId(UUID.randomUUID().toString());
        event.setEventType("USAGE_UPDATED");
        event.setUserId(request.getUserId());
        event.setDataUsage(request.getDataUsage());
        event.setCallUsage(request.getCallUsage());
        event.setMessageUsage(request.getMessageUsage());
        event.setTimestamp(LocalDateTime.now());
        return event;
    }
}