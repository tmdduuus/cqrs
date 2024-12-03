package com.telecom.cqrs.query.service;

import com.telecom.cqrs.query.domain.PhonePlanView;
import com.telecom.cqrs.query.dto.PhonePlanQueryResponse;
import com.telecom.cqrs.query.mapper.PhonePlanMapper;
import com.telecom.cqrs.query.repository.PhonePlanViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 요금제 조회 기능을 제공하는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhonePlanQueryService {
    private final PhonePlanViewRepository phonePlanViewRepository;
    private final PhonePlanMapper phonePlanMapper;

    /**
     * 사용자의 요금제 정보를 조회합니다.
     * @param userId 조회할 사용자 ID
     * @return 요금제 정보. 없으면 null을 반환
     */
    public PhonePlanQueryResponse getPhonePlan(String userId) {
        log.debug("Querying phone plan for user: {}", userId);
        PhonePlanView view = phonePlanViewRepository.findByUserId(userId);
        return phonePlanMapper.toDto(view);
    }
}