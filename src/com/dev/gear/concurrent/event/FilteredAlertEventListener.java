package com.dev.gear.concurrent.event;

/**
 * 支持按线程池名称过滤的告警事件监听器包装类
 */
public class FilteredAlertEventListener implements AlertEventListener {
    private final AlertEventListener delegate;
    private final String poolName;

    public FilteredAlertEventListener(AlertEventListener delegate, String poolName) {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate listener cannot be null");
        }
        if (poolName == null || poolName.trim().isEmpty()) {
            throw new IllegalArgumentException("Pool name cannot be null or empty");
        }
        this.delegate = delegate;
        this.poolName = poolName.trim();
    }

    @Override
    public void onAlert(AlertEvent event) {
        // 从元数据中获取线程池名称
        Object poolNameObj = event.getMetadata().get("poolName");
        if (poolNameObj != null && poolName.equals(poolNameObj.toString())) {
            // 只有当线程池名称匹配时才转发事件
            delegate.onAlert(event);
        }
    }

    public String getPoolName() {
        return poolName;
    }

    public AlertEventListener getDelegate() {
        return delegate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FilteredAlertEventListener)) return false;
        FilteredAlertEventListener that = (FilteredAlertEventListener) o;
        return delegate.equals(that.delegate) && poolName.equals(that.poolName);
    }

    @Override
    public int hashCode() {
        return 31 * delegate.hashCode() + poolName.hashCode();
    }
}