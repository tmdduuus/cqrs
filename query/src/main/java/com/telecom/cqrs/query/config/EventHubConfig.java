// File: cqrs/query/src/main/java/com/telecom/cqrs/query/config/EventHubConfig.java
package com.telecom.cqrs.query.config;

import com.azure.messaging.eventhubs.EventProcessorClient;
import com.azure.messaging.eventhubs.EventProcessorClientBuilder;
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore;
import com.telecom.cqrs.common.constant.Constants;
import com.telecom.cqrs.query.event.PhonePlanEventHandler;
import com.telecom.cqrs.query.event.UsageEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class EventHubConfig {
    @Value("${EVENT_HUB_CONNECTION_STRING}")
    private String connectionString;

    private final BlobStorageConfig blobStorageConfig;
    private final UsageEventHandler usageEventHandler;
    private final PhonePlanEventHandler planEventHandler;

    public EventHubConfig(
            BlobStorageConfig blobStorageConfig,
            UsageEventHandler usageEventHandler,
            PhonePlanEventHandler planEventHandler) {
        this.blobStorageConfig = blobStorageConfig;
        this.usageEventHandler = usageEventHandler;
        this.planEventHandler = planEventHandler;
    }

    @Bean
    public EventProcessorClient usageEventProcessor() {
        /*
        사용량 업데이트 이벤트 처리기
        - Event Hub 연결 문자열
        - consumer group
        - 체크 포인트로 사용할 저장소 객체: 처리한 메시지를 마킹하여 중복 처리를 방지
        - 메시지 처리 메소드:
        - 에러 처리 메소드
        */
        var blobClient = blobStorageConfig
                .getBlobContainerAsyncClient(BlobStorageContainers.USAGE_CONTAINER);

        return new EventProcessorClientBuilder()
                .connectionString(connectionString, Constants.EVENT_HUB_NAME)
                .consumerGroup("$Default")
                .checkpointStore(new BlobCheckpointStore(blobClient))
                .processEvent(usageEventHandler::processEvent)
                .processError(usageEventHandler::processError)
                .buildEventProcessorClient();
    }

    @Bean
    public EventProcessorClient planEventProcessor() {
        var blobClient = blobStorageConfig
                .getBlobContainerAsyncClient(BlobStorageContainers.PLAN_CONTAINER);

        return new EventProcessorClientBuilder()
                .connectionString(connectionString, Constants.EVENT_HUB_NAME)
                .consumerGroup("$Default")
                .checkpointStore(new BlobCheckpointStore(blobClient))
                .processEvent(planEventHandler::processEvent)
                .processError(planEventHandler::processError)
                .buildEventProcessorClient();
    }
}
