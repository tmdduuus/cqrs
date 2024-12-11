package com.telecom.cqrs.common.constant;

public class EventHubConstants {
    public static final String PLAN_HUB_NAME = "phone-plan-events";
    public static final String USAGE_HUB_NAME = "phone-usage-events";
    public static final String EVENT_TYPE_PLAN = "PLAN_CHANGED";
    public static final String EVENT_TYPE_USAGE = "USAGE_UPDATED";

    private EventHubConstants() {}
}