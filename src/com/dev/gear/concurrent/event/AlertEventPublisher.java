package com.dev.gear.concurrent.event;

import com.dev.gear.concurrent.config.AlertConfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class AlertEventPublisher {
    private static final AlertEventPublisher INSTANCE = new AlertEventPublisher();
    private final Map<AlertConfig.AlertLevel, Map<AlertMessageType, List<AlertEventListener>>> listenersByType;

    private AlertEventPublisher() {
        listenersByType = new ConcurrentHashMap<>();
        for (AlertConfig.AlertLevel level : AlertConfig.AlertLevel.values()) {
            Map<AlertMessageType, List<AlertEventListener>> typeMap = new ConcurrentHashMap<>();
            for (AlertMessageType type : AlertMessageType.values()) {
                typeMap.put(type, new CopyOnWriteArrayList<>());
            }
            listenersByType.put(level, typeMap);
        }
    }

    public static AlertEventPublisher getInstance() {
        return INSTANCE;
    }

    /**
     * 订阅特定级别和消息类型的告警事件
     */
    public void subscribe(AlertConfig.AlertLevel level, AlertMessageType messageType, AlertEventListener listener) {
        if (messageType == null) {
            throw new IllegalArgumentException("Message type cannot be null");
        }
        listenersByType.get(level).get(messageType).add(listener);
    }

    /**
     * 订阅特定线程池的特定级别和消息类型告警事件
     */
    public void subscribe(AlertConfig.AlertLevel level, AlertMessageType messageType, AlertEventListener listener, String poolName) {
        subscribe(level, messageType, new FilteredAlertEventListener(listener, poolName));
    }

    /**
     * 订阅所有消息类型的特定级别告警事件
     */
    public void subscribeAllTypes(AlertConfig.AlertLevel level, AlertEventListener listener) {
        Map<AlertMessageType, List<AlertEventListener>> typeListeners = listenersByType.get(level);
        for (List<AlertEventListener> listeners : typeListeners.values()) {
            listeners.add(listener);
        }
    }

    /**
     * 订阅所有级别和类型的告警事件
     */
    public void subscribeAll(AlertEventListener listener) {
        for (Map<AlertMessageType, List<AlertEventListener>> typeListeners : listenersByType.values()) {
            for (List<AlertEventListener> listeners : typeListeners.values()) {
                listeners.add(listener);
            }
        }
    }

    /**
     * 取消订阅特定级别和消息类型的告警事件
     */
    public void unsubscribe(AlertConfig.AlertLevel level, AlertMessageType messageType, AlertEventListener listener) {
        if (messageType == null) {
            return;
        }
        List<AlertEventListener> listeners = listenersByType.get(level).get(messageType);
        listeners.removeIf(l -> {
            if (l instanceof FilteredAlertEventListener) {
                return ((FilteredAlertEventListener) l).getDelegate().equals(listener);
            }
            return l.equals(listener);
        });
    }

    /**
     * 取消订阅特定线程池的特定级别和消息类型告警事件
     */
    public void unsubscribe(AlertConfig.AlertLevel level, AlertMessageType messageType, AlertEventListener listener, String poolName) {
        if (messageType == null) {
            return;
        }
        List<AlertEventListener> listeners = listenersByType.get(level).get(messageType);
        FilteredAlertEventListener filteredListener = new FilteredAlertEventListener(listener, poolName);
        listeners.remove(filteredListener);
    }

    /**
     * 取消订阅所有消息类型的特定级别告警事件
     */
    public void unsubscribeAllTypes(AlertConfig.AlertLevel level, AlertEventListener listener) {
        Map<AlertMessageType, List<AlertEventListener>> typeListeners = listenersByType.get(level);
        for (List<AlertEventListener> listeners : typeListeners.values()) {
            listeners.removeIf(l -> {
                if (l instanceof FilteredAlertEventListener) {
                    return ((FilteredAlertEventListener) l).getDelegate().equals(listener);
                }
                return l.equals(listener);
            });
        }
    }

    /**
     * 取消订阅所有级别和类型的告警事件
     */
    public void unsubscribeAll(AlertEventListener listener) {
        for (Map<AlertMessageType, List<AlertEventListener>> typeListeners : listenersByType.values()) {
            for (List<AlertEventListener> listeners : typeListeners.values()) {
                listeners.removeIf(l -> {
                    if (l instanceof FilteredAlertEventListener) {
                        return ((FilteredAlertEventListener) l).getDelegate().equals(listener);
                    }
                    return l.equals(listener);
                });
            }
        }
    }

    /**
     * 发布告警事件
     */
    public void publishAlert(String message, AlertConfig.AlertLevel level, AlertMessageType messageType, Map<String, Object> metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("Metadata cannot be null");
        }
        if (messageType == null) {
            throw new IllegalArgumentException("Message type cannot be null");
        }

        AlertEvent event = new AlertEvent(message, level, messageType, metadata);
        List<AlertEventListener> listeners = listenersByType.get(level).get(messageType);
        
        for (AlertEventListener listener : listeners) {
            try {
                listener.onAlert(event);
            } catch (Exception e) {
                System.err.println("Error notifying listener: " + e.getMessage());
            }
        }
    }
}