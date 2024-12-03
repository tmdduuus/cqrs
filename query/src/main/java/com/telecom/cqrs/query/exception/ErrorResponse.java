package com.telecom.cqrs.query.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * API 오류 응답을 위한 DTO 클래스입니다.
 */
@Data
@AllArgsConstructor
public class ErrorResponse {
    private String code;
    private String message;
}
