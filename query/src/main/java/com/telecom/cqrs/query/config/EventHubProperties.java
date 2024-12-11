// 1. EventHubProperties.java - Event Hub 설정을 위한 Properties 클래스
package com.telecom.cqrs.query.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "eventhub")
@Getter @Setter
public class EventHubProperties {
    private String planHubName;
    private String usageHubName;
    private String planConsumerGroup;
    private String usageConsumerGroup;
    private Integer batchSize = 100;
    private Long checkpointInterval = 5000L; // 5초
}
