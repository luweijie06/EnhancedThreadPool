package com.dev.gear.concurrent.event;

import com.dev.gear.concurrent.config.AlertConfig;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AlertEvent {
    private final String message;
    private final AlertConfig.AlertLevel level;
    private final AlertMessageType messageType;
    private final long timestamp;
    private final Map<String, Object> metadata;

    public AlertEvent(String message, AlertConfig.AlertLevel level, AlertMessageType messageType, Map<String, Object> metadata) {
        if (messageType == null) {
            throw new IllegalArgumentException("Message type cannot be null");
        }
        this.message = message;
        this.level = level;
        this.messageType = messageType;
        this.timestamp = System.currentTimeMillis();
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    // Getters
    public String getMessage() { return message; }
    public AlertConfig.AlertLevel getLevel() { return level; }
    public AlertMessageType getMessageType() { return messageType; }
    public long getTimestamp() { return timestamp; }
    public Map<String, Object> getMetadata() { return Collections.unmodifiableMap(metadata); }
}