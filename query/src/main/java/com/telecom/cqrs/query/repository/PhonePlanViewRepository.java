package com.telecom.cqrs.query.repository;

import com.telecom.cqrs.query.domain.PhonePlanView;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * 요금제 조회를 위한 MongoDB 레포지토리입니다.
 */
public interface PhonePlanViewRepository extends MongoRepository<PhonePlanView, String> {
    /**
     * 사용자 ID로 요금제 정보를 조회합니다.
     * @param userId 조회할 사용자 ID
     * @return 요금제 정보. 없으면 null 반환
     */
    PhonePlanView findByUserId(String userId);
}