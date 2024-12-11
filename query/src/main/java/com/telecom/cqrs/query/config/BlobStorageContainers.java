package com.telecom.cqrs.query.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BlobStorageContainers {
    @Value("${BLOB_CONTAINER}")
    private String containerName;

    public String getUsageContainer() {
        return containerName;
    }

    public String getPlanContainer() {
        return containerName;
    }
}