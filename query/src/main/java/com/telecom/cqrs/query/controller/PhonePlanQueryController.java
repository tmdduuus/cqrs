package com.telecom.cqrs.query.controller;

import com.telecom.cqrs.query.dto.PhonePlanQueryResponse;
import com.telecom.cqrs.query.service.PhonePlanQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 요금제 조회 API를 제공하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/plans/query")
@RequiredArgsConstructor
@Tag(name = "사용 현황 조회 API", description = "사용 현황을 제공합니다.")
public class PhonePlanQueryController {
    private final PhonePlanQueryService phonePlanQueryService;

    /**
     * 사용자의 요금제 정보를 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return 요금제 정보 응답
     */
    @Operation(summary = "사용 현황 조회", description = "사용자의 사용 현황을 조회합니다.")
    @GetMapping("/{userId}")
    public ResponseEntity<PhonePlanQueryResponse> getPhonePlan(
            @Parameter(description = "사용자 ID", example = "user123")
            @PathVariable String userId
    ) {
        PhonePlanQueryResponse response = phonePlanQueryService.getPhonePlan(userId);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.notFound().build();
    }
}