package com.dev.gear.concurrent.monitor;

public class ThreadPoolStats {
    private final TaskStats taskStats;
    private final int activeThreads;
    private final int queueSize;
    private final int poolSize;
    private final int maxPoolSize;
    private final long completedTasks;
    private final long timestamp;
    private final int queueCapacity;

    public ThreadPoolStats(
            TaskStats taskStats,
            int activeThreads,
            int queueSize,
            int poolSize,
            int maxPoolSize,
            long completedTasks,
            int queueCapacity) {
        this.taskStats = taskStats;
        this.activeThreads = activeThreads;
        this.queueSize = queueSize;
        this.poolSize = poolSize;
        this.maxPoolSize = maxPoolSize;
        this.completedTasks = completedTasks;
        this.timestamp = System.currentTimeMillis();
        this.queueCapacity = queueCapacity;
    }
    public static class Builder {
        private TaskStats taskStats;
        private int activeThreads;
        private int queueSize;
        private int poolSize;
        private int maxPoolSize;
        private long completedTasks;
        private int queueCapacity;

        public Builder taskStats(TaskStats taskStats) {
            this.taskStats = taskStats;
            return this;
        }

        public Builder activeThreads(int activeThreads) {
            this.activeThreads = activeThreads;
            return this;
        }

        public Builder queueSize(int queueSize) {
            this.queueSize = queueSize;
            return this;
        }

        public Builder poolSize(int poolSize) {
            this.poolSize = poolSize;
            return this;
        }

        public Builder maxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        public Builder completedTasks(long completedTasks) {
            this.completedTasks = completedTasks;
            return this;
        }

        public Builder queueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
            return this;
        }

        public ThreadPoolStats build() {
            return new ThreadPoolStats(
                    taskStats,
                    activeThreads,
                    queueSize,
                    poolSize,
                    maxPoolSize,
                    completedTasks,
                    queueCapacity
            );
        }
    }
    // 基础指标
    public TaskStats getTaskStats() { return taskStats; }
    public int getActiveThreads() { return activeThreads; }
    public int getQueueSize() { return queueSize; }
    public int getPoolSize() { return poolSize; }
    public int getMaxPoolSize() { return maxPoolSize; }
    public long getCompletedTasks() { return completedTasks; }
    public long getTimestamp() { return timestamp; }

    // 线程相关指标
    public double getThreadUtilization() {
        return poolSize > 0 ? (double) activeThreads / poolSize : 0.0;
    }

    public double getMaxThreadUtilization() {
        return maxPoolSize > 0 ? (double) activeThreads / maxPoolSize : 0.0;
    }

    public int getIdleThreads() {
        return poolSize - activeThreads;
    }

    // 队列相关指标
    public double getQueueUtilization() {
        return queueCapacity > 0 ? (double) queueSize / queueCapacity : 0.0;
    }

    public int getRemainingQueueCapacity() {
        return queueCapacity - queueSize;
    }

    // 任务处理指标
    public double getTaskSuccessRate() {
        return taskStats.getTaskSuccessRate();
    }

    public double getTaskRejectionRate() {
        long total = taskStats.getTotalTasks();
        return total > 0 ? (double) taskStats.getRejectedTasks() / total : 0.0;
    }

    public double getTaskThroughput() {
        long uptime = System.currentTimeMillis() - taskStats.getStartTime().toEpochMilli();
        return uptime > 0 ? (double) completedTasks / (uptime / 1000.0) : 0.0;
    }

    // 延迟指标
    public long getAverageWaitTime() {
        return taskStats.getAverageWaitTime();
    }

    public long getAverageExecutionTime() {
        return taskStats.getAverageExecutionTime();
    }

    public long getLatencyPercentile(int percentile) {
        return taskStats.getLatencyPercentile(percentile);
    }

    @Override
    public String toString() {
        return toJson();
    }

    public String toJson() {
        return String.format(
            "{" +
            "\"timestamp\":%d," +
            "\"activeThreads\":%d," +
            "\"poolSize\":%d," +
            "\"maxPoolSize\":%d," +
            "\"queueSize\":%d," +
            "\"queueCapacity\":%d," +
            "\"queueUtilization\":%.4f," +
            "\"threadUtilization\":%.4f," +
            "\"maxThreadUtilization\":%.4f," +
            "\"completedTasks\":%d," +
            "\"taskSuccessRate\":%.4f," +
            "\"taskRejectionRate\":%.4f," +
            "\"taskThroughput\":%.2f," +
            "\"averageWaitTime\":%d," +
            "\"averageExecutionTime\":%d," +
            "\"p50Latency\":%d," +
            "\"p95Latency\":%d," +
            "\"p99Latency\":%d" +
            "}",
            timestamp,
            activeThreads,
            poolSize,
            maxPoolSize,
            queueSize,
            queueCapacity,
            getQueueUtilization(),
            getThreadUtilization(),
            getMaxThreadUtilization(),
            completedTasks,
            getTaskSuccessRate(),
            getTaskRejectionRate(),
            getTaskThroughput(),
            getAverageWaitTime(),
            getAverageExecutionTime(),
            getLatencyPercentile(50),
            getLatencyPercentile(95),
            getLatencyPercentile(99)
        );
    }
}