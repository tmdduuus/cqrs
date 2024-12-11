// 4. EventProcessingException.java - 이벤트 처리 관련 예외
package com.telecom.cqrs.query.exception;

public class EventProcessingException extends RuntimeException {
    public EventProcessingException(String message) {
        super(message);
    }

    public EventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}