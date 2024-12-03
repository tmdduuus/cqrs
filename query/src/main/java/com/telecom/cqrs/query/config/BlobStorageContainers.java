package com.telecom.cqrs.query.config;

/**
 * Blob Storage 컨테이너 이름을 관리하는 상수 클래스입니다.
 */
public class BlobStorageContainers {
    public static final String USAGE_CONTAINER = "usage-checkpoints";
    public static final String PLAN_CONTAINER = "plan-checkpoints";

    private BlobStorageContainers() {} // 인스턴스화 방지
}