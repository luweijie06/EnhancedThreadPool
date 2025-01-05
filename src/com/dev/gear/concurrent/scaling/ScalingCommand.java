package com.dev.gear.concurrent.scaling;

/**
 * 线程池扩缩容命令
 * 支持调整多个线程池参数
 */
public class ScalingCommand {
    private final int threadDelta;          // 线程数调整量
    private final int coreSizeDelta;        // 核心线程数调整量
    private final int maxSizeDelta;         // 最大线程数调整量
    private final int queueCapacityDelta;   // 队列容量调整量
    private final long keepAliveTimeDelta;  // 空闲线程存活时间调整量(毫秒)
    private final String reason;            // 调整原因

    private ScalingCommand(Builder builder) {
        this.threadDelta = builder.threadDelta;
        this.coreSizeDelta = builder.coreSizeDelta;
        this.maxSizeDelta = builder.maxSizeDelta;
        this.queueCapacityDelta = builder.queueCapacityDelta;
        this.keepAliveTimeDelta = builder.keepAliveTimeDelta;
        this.reason = builder.reason;
    }

    public int getThreadDelta() {
        return threadDelta;
    }

    public int getCoreSizeDelta() {
        return coreSizeDelta;
    }

    public int getMaxSizeDelta() {
        return maxSizeDelta;
    }

    public int getQueueCapacityDelta() {
        return queueCapacityDelta;
    }

    public long getKeepAliveTimeDelta() {
        return keepAliveTimeDelta;
    }

    public String getReason() {
        return reason;
    }

    /**
     * 检查是否有任何参数需要调整
     */
    public boolean hasAdjustments() {
        return threadDelta != 0 
            || coreSizeDelta != 0 
            || maxSizeDelta != 0 
            || queueCapacityDelta != 0 
            || keepAliveTimeDelta != 0;
    }

    public static class Builder {
        private int threadDelta = 0;
        private int coreSizeDelta = 0;
        private int maxSizeDelta = 0;
        private int queueCapacityDelta = 0;
        private long keepAliveTimeDelta = 0;
        private String reason = "";

        public Builder threadDelta(int delta) {
            this.threadDelta = delta;
            return this;
        }

        public Builder coreSizeDelta(int delta) {
            this.coreSizeDelta = delta;
            return this;
        }

        public Builder maxSizeDelta(int delta) {
            this.maxSizeDelta = delta;
            return this;
        }

        public Builder queueCapacityDelta(int delta) {
            this.queueCapacityDelta = delta;
            return this;
        }

        public Builder keepAliveTimeDelta(long delta) {
            this.keepAliveTimeDelta = delta;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public ScalingCommand build() {
            return new ScalingCommand(this);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ScalingCommand{");
        if (threadDelta != 0) {
            sb.append("threadDelta=").append(threadDelta).append(", ");
        }
        if (coreSizeDelta != 0) {
            sb.append("coreSizeDelta=").append(coreSizeDelta).append(", ");
        }
        if (maxSizeDelta != 0) {
            sb.append("maxSizeDelta=").append(maxSizeDelta).append(", ");
        }
        if (queueCapacityDelta != 0) {
            sb.append("queueCapacityDelta=").append(queueCapacityDelta).append(", ");
        }
        if (keepAliveTimeDelta != 0) {
            sb.append("keepAliveTimeDelta=").append(keepAliveTimeDelta).append(", ");
        }
        if (!reason.isEmpty()) {
            sb.append("reason='").append(reason).append("'");
        }
        return sb.append("}").toString();
    }
}