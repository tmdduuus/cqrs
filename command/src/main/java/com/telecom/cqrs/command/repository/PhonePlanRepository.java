package com.telecom.cqrs.command.repository;

import com.telecom.cqrs.command.domain.PhonePlan;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 요금제 정보를 PostgreSQL에 저장하는 레포지토리입니다.
 */
public interface PhonePlanRepository extends JpaRepository<PhonePlan, Long> {
    PhonePlan findByUserId(String userId);
}
