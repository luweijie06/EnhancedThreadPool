package com.dev.gear.concurrent.event;

public enum AlertMessageType {
    SCALING("扩缩容告警"),
    MONITORING("监控告警");

    private final String description;

    AlertMessageType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}