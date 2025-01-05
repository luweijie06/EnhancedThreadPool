package com.dev.gear.concurrent.config;

/**
 * 线程池监控配置类
 */
public class MonitoringConfig {
    private final long monitoringPeriodMs;      // 监控周期
    private final int samplingIntervalMs;       // 采样间隔
    private final boolean enableDetailedMetrics; // 是否启用详细指标收集
    
    // 线程池核心指标
    private final boolean enableQueueMetrics;    // 队列相关指标
    private final boolean enableTaskMetrics;     // 任务执行相关指标
    private final boolean enableThreadMetrics;   // 线程相关指标
    private final boolean enableLatencyMetrics;  // 延迟相关指标
    private final int latencyPercentiles[];     // 延迟百分位数配置
    private final boolean enableRejectionMetrics; // 拒绝策略相关指标
    
    private MonitoringConfig(Builder builder) {
        this.monitoringPeriodMs = builder.monitoringPeriodMs;
        this.samplingIntervalMs = builder.samplingIntervalMs;
        this.enableDetailedMetrics = builder.enableDetailedMetrics;
        this.enableQueueMetrics = builder.enableQueueMetrics;
        this.enableTaskMetrics = builder.enableTaskMetrics;
        this.enableThreadMetrics = builder.enableThreadMetrics;
        this.enableLatencyMetrics = builder.enableLatencyMetrics;
        this.latencyPercentiles = builder.latencyPercentiles;
        this.enableRejectionMetrics = builder.enableRejectionMetrics;
    }

    // 原有的getter方法
    public long getMonitoringPeriodMs() {
        return monitoringPeriodMs;
    }

    public int getSamplingIntervalMs() {
        return samplingIntervalMs;
    }

    public boolean isDetailedMetricsEnabled() {
        return enableDetailedMetrics;
    }

    // 新增的getter方法
    public boolean isQueueMetricsEnabled() {
        return enableQueueMetrics;
    }

    public boolean isTaskMetricsEnabled() {
        return enableTaskMetrics;
    }

    public boolean isThreadMetricsEnabled() {
        return enableThreadMetrics;
    }

    public boolean isLatencyMetricsEnabled() {
        return enableLatencyMetrics;
    }

    public int[] getLatencyPercentiles() {
        return latencyPercentiles;
    }

    public boolean isRejectionMetricsEnabled() {
        return enableRejectionMetrics;
    }

    public static class Builder {
        private long monitoringPeriodMs = 5000L;    // 默认5秒
        private int samplingIntervalMs = 1000;      // 默认1秒
        private boolean enableDetailedMetrics = false;
        
        // 线程池监控相关的默认配置
        private boolean enableQueueMetrics = true;   // 默认启用队列指标
        private boolean enableTaskMetrics = true;    // 默认启用任务指标
        private boolean enableThreadMetrics = true;  // 默认启用线程指标
        private boolean enableLatencyMetrics = true; // 默认启用延迟指标
        private int[] latencyPercentiles = new int[]{50, 75, 90, 95, 99}; // 默认的延迟百分位
        private boolean enableRejectionMetrics = true; // 默认启用拒绝指标

        // 原有的builder方法
        public Builder setMonitoringPeriodMs(long periodMs) {
            this.monitoringPeriodMs = periodMs;
            return this;
        }

        public Builder setSamplingIntervalMs(int intervalMs) {
            this.samplingIntervalMs = intervalMs;
            return this;
        }

        public Builder setEnableDetailedMetrics(boolean enable) {
            this.enableDetailedMetrics = enable;
            return this;
        }

        // 新增的builder方法
        public Builder setEnableQueueMetrics(boolean enable) {
            this.enableQueueMetrics = enable;
            return this;
        }

        public Builder setEnableTaskMetrics(boolean enable) {
            this.enableTaskMetrics = enable;
            return this;
        }

        public Builder setEnableThreadMetrics(boolean enable) {
            this.enableThreadMetrics = enable;
            return this;
        }

        public Builder setEnableLatencyMetrics(boolean enable) {
            this.enableLatencyMetrics = enable;
            return this;
        }

        public Builder setLatencyPercentiles(int[] percentiles) {
            this.latencyPercentiles = percentiles;
            return this;
        }

        public Builder setEnableRejectionMetrics(boolean enable) {
            this.enableRejectionMetrics = enable;
            return this;
        }

        public MonitoringConfig build() {
            validate();
            return new MonitoringConfig(this);
        }

        private void validate() {
            if (monitoringPeriodMs <= 0) {
                throw new IllegalArgumentException("Monitoring period must be positive");
            }
            if (samplingIntervalMs <= 0) {
                throw new IllegalArgumentException("Sampling interval must be positive");
            }
            if (samplingIntervalMs > monitoringPeriodMs) {
                throw new IllegalArgumentException("Sampling interval cannot be larger than monitoring period");
            }
            if (enableLatencyMetrics && (latencyPercentiles == null || latencyPercentiles.length == 0)) {
                throw new IllegalArgumentException("Latency percentiles must be specified when latency metrics is enabled");
            }
            if (latencyPercentiles != null) {
                for (int percentile : latencyPercentiles) {
                    if (percentile < 0 || percentile > 100) {
                        throw new IllegalArgumentException("Latency percentile must be between 0 and 100");
                    }
                }
            }
        }
    }
}