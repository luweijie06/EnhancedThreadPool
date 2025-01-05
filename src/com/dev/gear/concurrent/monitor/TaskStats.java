package com.dev.gear.concurrent.monitor;

import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 任务统计信息类
 * 用于跟踪和统计任务执行的各项指标，包括：
 * - 任务数量统计（总数、完成数、失败数、拒绝数）
 * - 执行时间统计（等待时间、执行时间）
 * - 延迟分布统计（各个百分位的延迟时间）
 * - 队列统计（队列大小、队列等待时间）
 */
public class TaskStats {
    // ===== 基础任务计数器 =====
    // 总任务数计数器
    private final LongAdder totalTasks = new LongAdder();
    // 失败任务数计数器
    private final LongAdder failedTasks = new LongAdder();
    // 被拒绝任务数计数器
    private final LongAdder rejectedTasks = new LongAdder();
    // 已完成任务数计数器
    private final LongAdder completedTasks = new LongAdder();
    
    // ===== 任务执行时间统计 =====
    // 总等待时间计数器
    private final LongAdder totalWaitTime = new LongAdder();
    // 总执行时间计数器
    private final LongAdder totalExecutionTime = new LongAdder();
    
    // ===== 延迟分布统计 =====
    // 需要统计的延迟百分位数数组
    private final int[] latencyPercentiles;
    // 延迟分布统计桶，用于记录不同延迟范围的任务数量
    private final AtomicLong[] latencyBuckets;
    // 统计桶的数量，将延迟时间划分为100个区间
    private static final int BUCKET_COUNT = 100;
    // 最大统计延迟时间
    private final long maxLatency;

    // ===== 任务队列统计 =====
    // 最大队列大小计数器
    private final LongAdder maxQueueSize = new LongAdder();
    // 总队列等待时间计数器
    private final LongAdder totalQueueTime = new LongAdder();
    
    // 统计开始时间
    private final Instant startTime;

    /**
     * 构造函数
     * @param percentiles 需要统计的延迟百分位数数组
     * @param maxLatencyMs 最大延迟时间（毫秒）
     */
    public TaskStats(int[] percentiles, long maxLatencyMs) {
        this.startTime = Instant.now();
        this.latencyPercentiles = percentiles != null ? percentiles : new int[]{50, 75, 90, 95, 99};
        this.maxLatency = maxLatencyMs;
        this.latencyBuckets = new AtomicLong[BUCKET_COUNT];
        for (int i = 0; i < BUCKET_COUNT; i++) {
            latencyBuckets[i] = new AtomicLong(0);
        }
    }

    /**
     * 默认构造函数
     * 使用默认的百分位数组{50, 75, 90, 95, 99}和10秒的最大延迟时间
     */
    public TaskStats() {
        this(new int[]{50, 75, 90, 95, 99}, 10000);
    }

    // ===== 基础任务记录方法 =====
    /**
     * 记录任务提交
     */
    public void recordSubmission() {
        totalTasks.increment();
    }

    /**
     * 记录任务完成
     */
    public void recordCompletion() {
        completedTasks.increment();
    }

    /**
     * 记录任务失败
     */
    public void recordFailure() {
        failedTasks.increment();
    }

    /**
     * 记录任务被拒绝
     */
    public void recordRejection() {
        rejectedTasks.increment();
    }

    // ===== 延迟统计方法 =====
    /**
     * 记录任务等待时间
     * @param waitTime 等待时间（毫秒）
     */
    public void recordWaitTime(long waitTime) {
        if (waitTime >= 0) {
            totalWaitTime.add(waitTime);
            recordLatency(waitTime);
        }
    }

    /**
     * 记录任务执行时间
     * @param executionTime 执行时间（毫秒）
     */
    public void recordExecutionTime(long executionTime) {
        if (executionTime >= 0) {
            totalExecutionTime.add(executionTime);
            recordLatency(executionTime);
        }
    }

    // ===== 队列统计方法 =====
    /**
     * 记录队列大小
     * @param size 当前队列大小
     */
    public void recordQueueSize(int size) {
        maxQueueSize.add(Math.max(0, size));
    }

    /**
     * 记录队列等待时间
     * @param queueTime 队列等待时间（毫秒）
     */
    public void recordQueueTime(long queueTime) {
        if (queueTime >= 0) {
            totalQueueTime.add(queueTime);
        }
    }

    /**
     * 记录延迟分布
     * 将延迟时间映射到对应的延迟桶中
     * @param latency 延迟时间（毫秒）
     */
    private void recordLatency(long latency) {
        if (latency < 0 || latency > maxLatency) return;
        int bucket = (int) ((latency * BUCKET_COUNT) / maxLatency);
        bucket = Math.min(bucket, BUCKET_COUNT - 1);
        latencyBuckets[bucket].incrementAndGet();
    }

    /**
     * 获取指定百分位的延迟时间
     * @param percentile 百分位数（0-100）
     * @return 该百分位对应的延迟时间（毫秒）
     */
    public long getLatencyPercentile(int percentile) {
        long total = Arrays.stream(latencyBuckets)
            .mapToLong(AtomicLong::get)
            .sum();
        if (total == 0) return 0;

        long count = 0;
        long targetCount = (total * percentile) / 100;
        
        for (int i = 0; i < BUCKET_COUNT; i++) {
            count += latencyBuckets[i].get();
            if (count >= targetCount) {
                return (i * maxLatency) / BUCKET_COUNT;
            }
        }
        return maxLatency;
    }

    /**
     * 获取所有配置的延迟百分位数值
     * @return 延迟百分位数值数组
     */
    public long[] getAllLatencyPercentiles() {
        return Arrays.stream(latencyPercentiles)
            .mapToLong(this::getLatencyPercentile)
            .toArray();
    }

    // ===== 统计计算方法 =====
    /**
     * 计算任务成功率
     * @return 成功率（0.0-1.0）
     */
    public double getTaskSuccessRate() {
        long total = totalTasks.sum();
        return total > 0 ? (double)(total - failedTasks.sum()) / total : 0.0;
    }

    /**
     * 计算平均等待时间
     * @return 平均等待时间（毫秒）
     */
    public long getAverageWaitTime() {
        long total = totalTasks.sum();
        return total > 0 ? totalWaitTime.sum() / total : 0;
    }

    /**
     * 计算平均执行时间
     * @return 平均执行时间（毫秒）
     */
    public long getAverageExecutionTime() {
        long total = completedTasks.sum();
        return total > 0 ? totalExecutionTime.sum() / total : 0;
    }

    /**
     * 计算平均队列等待时间
     * @return 平均队列等待时间（毫秒）
     */
    public long getAverageQueueTime() {
        long total = totalTasks.sum();
        return total > 0 ? totalQueueTime.sum() / total : 0;
    }

    // ===== Getter方法 =====
    public long getTotalTasks() { return totalTasks.sum(); }
    public long getFailedTasks() { return failedTasks.sum(); }
    public long getRejectedTasks() { return rejectedTasks.sum(); }
    public long getCompletedTasks() { return completedTasks.sum(); }
    public long getMaxQueueSize() { return maxQueueSize.sum(); }
    public Instant getStartTime() { return startTime; }

    /**
     * 创建当前状态的快照
     * 复制所有计数器和统计数据到新的实例中
     * @return 新的TaskStats实例，包含当前所有统计数据
     */
    public TaskStats snapshot() {
        TaskStats snapshot = new TaskStats(latencyPercentiles, maxLatency);
        snapshot.totalTasks.add(this.totalTasks.sum());
        snapshot.failedTasks.add(this.failedTasks.sum());
        snapshot.rejectedTasks.add(this.rejectedTasks.sum());
        snapshot.completedTasks.add(this.completedTasks.sum());
        snapshot.totalWaitTime.add(this.totalWaitTime.sum());
        snapshot.totalExecutionTime.add(this.totalExecutionTime.sum());
        snapshot.maxQueueSize.add(this.maxQueueSize.sum());
        snapshot.totalQueueTime.add(this.totalQueueTime.sum());
        
        // 复制延迟分布数据
        for (int i = 0; i < BUCKET_COUNT; i++) {
            snapshot.latencyBuckets[i].set(this.latencyBuckets[i].get());
        }
        
        return snapshot;
    }

    /**
     * 生成统计信息的字符串表示
     * @return 包含主要统计指标的字符串
     */
    @Override
    public String toString() {
        return String.format(
            "TaskStats[total=%d, completed=%d, failed=%d, rejected=%d, " +
            "avgWait=%dms, avgExec=%dms, successRate=%.2f%%, " +
            "p50=%dms, p95=%dms, p99=%dms]",
            getTotalTasks(),
            getCompletedTasks(),
            getFailedTasks(),
            getRejectedTasks(),
            getAverageWaitTime(),
            getAverageExecutionTime(),
            getTaskSuccessRate() * 100,
            getLatencyPercentile(50),
            getLatencyPercentile(95),
            getLatencyPercentile(99)
        );
    }
}