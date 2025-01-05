package com.dev.gear.concurrent.expand;

import com.dev.gear.concurrent.config.AlertConfig;
import com.dev.gear.concurrent.config.ScalingConfig;
import com.dev.gear.concurrent.event.AlertEventPublisher;
import com.dev.gear.concurrent.event.AlertMessageType;
import com.dev.gear.concurrent.monitor.ThreadPoolStats;
import com.dev.gear.concurrent.scaling.ScalingCommand;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 线程池扩缩容
 */
public class ThreadPoolScalier {

    // 需要管理的线程池实例
    private final ThreadPoolExecutor threadPool;
    // 扩缩容配置，包含扩缩容策略和相关参数
    private final ScalingConfig scalingConfig;
    // 线程池名称，用于标识和告警
    private final String poolName;
    // 用于确保扩缩容操作的互斥执行
    private final ReentrantLock scalingLock = new ReentrantLock();
    // 上次扩缩容操作的时间戳，用于控制扩缩容频率
    private volatile long lastScalingTime = 0;

    /**
     * 构造函数
     * @param threadPool 要管理的线程池实例
     * @param scalingConfig 扩缩容配置
     * @param poolName 线程池名称
     */
    public ThreadPoolScalier(ThreadPoolExecutor threadPool, ScalingConfig scalingConfig, String poolName) {
        this.threadPool = threadPool;
        this.scalingConfig = scalingConfig;
        this.poolName = poolName;
    }

    /**
     * 尝试执行扩缩容操作
     * 该方法会根据当前线程池统计信息判断是否需要进行扩缩容
     * 
     * @param stats 线程池当前的统计信息
     */
    public void attemptScaling(ThreadPoolStats stats) {
        // 执行基础检查，确认是否满足扩缩容条件
        if (!shouldAttemptScaling(stats)) {
            return;
        }

        // 获取锁失败说明有其他扩缩容操作在进行，直接返回
        if (!scalingLock.tryLock()) {
            return;
        }

        try {
            // 根据策略计算需要进行的扩缩容调整
            ScalingCommand command = scalingConfig.getScalingStrategy().calculateScaling(stats);
            // 如果有需要调整的参数，执行扩缩容命令
            if (command != null && command.hasAdjustments()) {
                executeScalingCommand(command, stats);
            }
        } finally {
            scalingLock.unlock();
        }
    }

    /**
     * 判断是否应该尝试扩缩容操作
     * 
     * @param stats 线程池统计信息
     * @return 是否可以进行扩缩容操作
     */
    private boolean shouldAttemptScaling(ThreadPoolStats stats) {
        // 检查是否在冷却期内（距离上次扩缩容操作的时间是否足够）
        if (System.currentTimeMillis() - lastScalingTime < scalingConfig.getScalingCheckPeriodMs()) {
            return false;
        }

        // 检查线程池是否处于正常运行状态
        if (threadPool.isShutdown() || threadPool.isTerminating()) {
            return false;
        }

        // 检查当前线程池大小是否有效
        int currentSize = stats.getPoolSize();
        if (currentSize <= 0) {
            return false;
        }

        // 通过基础检查，具体的扩缩容决策交给策略处理
        return true;
    }

    /**
     * 执行扩缩容命令
     * 该方法负责具体的参数调整操作，并发送相应的告警通知
     * 
     * @param command 扩缩容命令，包含具体的调整参数
     * @param stats 当前线程池统计信息
     */
    private void executeScalingCommand(ScalingCommand command, ThreadPoolStats stats) {
        // 记录调整前的参数，用于告警和回滚
        int oldCoreSize = threadPool.getCorePoolSize();
        int oldMaxSize = threadPool.getMaximumPoolSize();
        long oldKeepAlive = threadPool.getKeepAliveTime(TimeUnit.MILLISECONDS);

        try {
            // 扩容时先调整最大值再调整核心值，缩容时反之
            // 这样可以避免因参数约束导致的调整失败
            if (command.getMaxSizeDelta() > 0) {
                adjustMaxSize(command.getMaxSizeDelta());
                adjustCoreSize(command.getCoreSizeDelta());
            } else {
                adjustCoreSize(command.getCoreSizeDelta());
                adjustMaxSize(command.getMaxSizeDelta());
            }

            // 如果需要，调整线程存活时间
            if (command.getKeepAliveTimeDelta() != 0) {
                adjustKeepAliveTime(command.getKeepAliveTimeDelta());
            }

            // 发送扩缩容成功的告警通知
            sendScalingAlert(command, oldCoreSize, oldMaxSize, oldKeepAlive, stats, null);
            lastScalingTime = System.currentTimeMillis();

        } catch (IllegalArgumentException e) {
            // 扩缩容失败时发送错误告警
            sendScalingAlert(command, oldCoreSize, oldMaxSize, oldKeepAlive, stats, e);
        }
    }

    /**
     * 调整核心线程数
     * 确保调整后的值在配置的最小和最大线程数范围内
     * 
     * @param delta 核心线程数的调整量
     */
    private void adjustCoreSize(int delta) {
        if (delta != 0) {
            int newSize = threadPool.getCorePoolSize() + delta;
            // 确保新的核心线程数在配置的范围内
            newSize = Math.max(scalingConfig.getMinThreads(),
                    Math.min(scalingConfig.getMaxThreads(), newSize));
            threadPool.setCorePoolSize(newSize);
        }
    }

    /**
     * 调整最大线程数
     * 确保调整后的值不小于核心线程数且不超过配置的最大线程数
     * 
     * @param delta 最大线程数的调整量
     */
    private void adjustMaxSize(int delta) {
        if (delta != 0) {
            int newSize = threadPool.getMaximumPoolSize() + delta;
            // 确保新的最大线程数不小于核心线程数且不超过配置的最大值
            newSize = Math.max(threadPool.getCorePoolSize(),
                    Math.min(scalingConfig.getMaxThreads(), newSize));
            threadPool.setMaximumPoolSize(newSize);
        }
    }

    /**
     * 调整线程存活时间
     * 确保调整后的值不小于0
     * 
     * @param delta 存活时间的调整量（毫秒）
     */
    private void adjustKeepAliveTime(long delta) {
        if (delta != 0) {
            long newTime = Math.max(0,
                    threadPool.getKeepAliveTime(TimeUnit.MILLISECONDS) + delta);
            threadPool.setKeepAliveTime(newTime, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 发送扩缩容相关的告警通知
     * 包含调整前后的参数信息和调整原因
     * 
     * @param command 扩缩容命令
     * @param oldCoreSize 原核心线程数
     * @param oldMaxSize 原最大线程数
     * @param oldKeepAlive 原线程存活时间
     * @param stats 线程池统计信息
     * @param error 错误信息（如果有）
     */
    private void sendScalingAlert(
            ScalingCommand command,
            int oldCoreSize,
            int oldMaxSize,
            long oldKeepAlive,
            ThreadPoolStats stats,
            Exception error) {
            
        // 构建告警元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("poolName", poolName);
        metadata.put("oldCoreSize", oldCoreSize);
        metadata.put("newCoreSize", threadPool.getCorePoolSize());
        metadata.put("oldMaxSize", oldMaxSize);
        metadata.put("newMaxSize", threadPool.getMaximumPoolSize());
        metadata.put("oldKeepAlive", oldKeepAlive);
        metadata.put("newKeepAlive", threadPool.getKeepAliveTime(TimeUnit.MILLISECONDS));
        metadata.put("activeThreads", stats.getActiveThreads());
        metadata.put("queueSize", stats.getQueueSize());
        metadata.put("reason", command.getReason());

        // 根据是否发生错误发送不同级别的告警
        if (error != null) {
            metadata.put("error", error.getMessage());
            AlertEventPublisher.getInstance().publishAlert(
                "Thread pool scaling failed: " + command.getReason(),
                AlertConfig.AlertLevel.ERROR,
                AlertMessageType.MONITORING,
                metadata
            );
        } else {
            AlertEventPublisher.getInstance().publishAlert(
                "Thread pool scaled: " + command.getReason(),
                AlertConfig.AlertLevel.INFO,
                AlertMessageType.MONITORING,
                metadata
            );
        }
    }
}