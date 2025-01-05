package com.dev.gear.concurrent.event;

/**
 * 告警事件监听器接口
 */
public interface AlertEventListener {
    void onAlert(AlertEvent event);
}