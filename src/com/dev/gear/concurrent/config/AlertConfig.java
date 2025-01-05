package com.dev.gear.concurrent.config;

/**
 * 告警配置类
 */
public class AlertConfig {
    private final int queueSizeWarningThreshold;    // 队列大小告警阈值
    private final long taskTimeoutMs;               // 任务超时告警阈值
    private final int threadPoolUsageThreshold;     // 线程池使用率告警阈值
    private final AlertLevel minimumAlertLevel;     // 最小告警级别

    private AlertConfig(Builder builder) {
        this.queueSizeWarningThreshold = builder.queueSizeWarningThreshold;
        this.taskTimeoutMs = builder.taskTimeoutMs;
        this.threadPoolUsageThreshold = builder.threadPoolUsageThreshold;
        this.minimumAlertLevel = builder.minimumAlertLevel;
    }

    public enum AlertLevel {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }

    public int getQueueSizeWarningThreshold() {
        return queueSizeWarningThreshold;
    }

    public long getTaskTimeoutMs() {
        return taskTimeoutMs;
    }

    public int getThreadPoolUsageThreshold() {
        return threadPoolUsageThreshold;
    }

    public AlertLevel getMinimumAlertLevel() {
        return minimumAlertLevel;
    }

    public static class Builder {
        private int queueSizeWarningThreshold = 1000;
        private long taskTimeoutMs = 60000L;
        private int threadPoolUsageThreshold = 80;  // 默认80%
        private AlertLevel minimumAlertLevel = AlertLevel.WARNING;

        public Builder setQueueSizeWarningThreshold(int threshold) {
            this.queueSizeWarningThreshold = threshold;
            return this;
        }

        public Builder setTaskTimeoutMs(long timeoutMs) {
            this.taskTimeoutMs = timeoutMs;
            return this;
        }

        public Builder setThreadPoolUsageThreshold(int threshold) {
            this.threadPoolUsageThreshold = threshold;
            return this;
        }

        public Builder setMinimumAlertLevel(AlertLevel level) {
            this.minimumAlertLevel = level;
            return this;
        }

        public AlertConfig build() {
            validate();
            return new AlertConfig(this);
        }

        private void validate() {
            if (queueSizeWarningThreshold <= 0) {
                throw new IllegalArgumentException("Queue size warning threshold must be positive");
            }
            if (taskTimeoutMs <= 0) {
                throw new IllegalArgumentException("Task timeout must be positive");
            }
            if (threadPoolUsageThreshold <= 0 || threadPoolUsageThreshold > 100) {
                throw new IllegalArgumentException("Thread pool usage threshold must be between 1 and 100");
            }
        }
    }
}