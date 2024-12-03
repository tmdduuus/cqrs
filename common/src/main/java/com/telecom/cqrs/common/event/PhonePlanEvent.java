package com.telecom.cqrs.common.event;

import lombok.Data;
import java.time.LocalDateTime;
/**
 * 요금제 변경 이벤트를 표현하는 클래스입니다.
 * Event Bus(Kafka)를 통해 전달됩니다.
 */
@Data
public class PhonePlanEvent {
    private String eventId;
    private String eventType;
    private String userId;
    private String planName;
    private int dataAllowance;
    private int callMinutes;
    private int messageCount;
    private double monthlyFee;
    private String status;
    private LocalDateTime timestamp;
}
