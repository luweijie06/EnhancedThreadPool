package com.dev.gear.concurrent.expand;

import com.dev.gear.concurrent.config.AlertConfig;
import com.dev.gear.concurrent.config.MonitoringConfig;
import com.dev.gear.concurrent.config.ScalingConfig;
import com.dev.gear.concurrent.event.AlertEventPublisher;
import com.dev.gear.concurrent.event.AlertMessageType;
import com.dev.gear.concurrent.monitor.TaskStats;
import com.dev.gear.concurrent.monitor.ThreadPoolStats;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * 线程池监控器
 * 主要功能：
 * 1. 实时监控线程池的运行状态
 * 2. 收集线程池性能指标
 * 3. 处理告警和自动扩缩容
 * 4. 提供监控数据统计和日志记录
 */
public class ThreadPoolMonitor {
    // 日志记录器
    private static final Logger logger = Logger.getLogger(ThreadPoolMonitor.class.getName());
    
    // 被监控的线程池实例
    private final ThreadPoolExecutor threadPool;
    // 监控配置，包含监控周期、启用的指标等
    private final MonitoringConfig monitorConfig;
    // 告警配置，包含告警阈值和级别等
    private final AlertConfig alertConfig;
    // 扩缩容配置，定义了自动扩缩容的策略
    private final ScalingConfig scalingConfig;
    // 线程池扩缩容器，负责执行扩缩容操作
    private final ThreadPoolScalier scaler;
    // 线程池名称，用于标识
    private final String poolName;
    // 任务统计信息，记录任务执行的相关指标
    private final TaskStats stats;
    
    // 专门用于执行监控任务的调度线程池
    private final ScheduledExecutorService monitorService;
    
    /**
     * 构造函数
     * @param threadPool 要监控的线程池
     * @param monitorConfig 监控配置
     * @param alertConfig 告警配置
     * @param scalingConfig 扩缩容配置
     * @param scaler 扩缩容执行器
     * @param stats 任务统计对象
     * @param poolName 线程池名称
     */
    public ThreadPoolMonitor(
            ThreadPoolExecutor threadPool,
            MonitoringConfig monitorConfig,
            AlertConfig alertConfig,
            ScalingConfig scalingConfig,
            ThreadPoolScalier scaler,
            TaskStats stats,
            String poolName) {
        this.threadPool = threadPool;
        this.monitorConfig = monitorConfig;
        this.alertConfig = alertConfig;
        this.scalingConfig = scalingConfig;
        this.scaler = scaler;
        this.stats = stats;
        this.poolName = poolName;
        
        // 创建单线程的调度线程池，用于执行监控任务
        this.monitorService = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, poolName + "-monitor"));
                
        initializeMonitoring();
    }
    
    /**
     * 初始化监控
     * 设置定时任务，定期收集线程池状态并进行分析
     */
    private void initializeMonitoring() {
        monitorService.scheduleAtFixedRate(() -> {
            try {
                // 如果未启用详细指标收集，则直接返回
                if (!monitorConfig.isDetailedMetricsEnabled()) {
                    return;
                }

                // 收集线程池当前状态
                ThreadPoolStats poolStats = collectThreadPoolStats();

                // 监控线程使用率（如果启用）
                if (monitorConfig.isThreadMetricsEnabled()) {
                    monitorThreadUsage(poolStats);
                }

                // 监控队列状态（如果启用）
                if (monitorConfig.isQueueMetricsEnabled()) {
                    monitorQueueStatus(poolStats);
                }

                // 执行自动扩缩容（如果配置了扩缩容策略）
                if (scalingConfig != null && scalingConfig.getScalingStrategy() != null) {
                    handleAutoScaling(poolStats);
                }

                // 记录统计信息
                logWithLevel("Thread pool stats: " + poolStats.toJson(), AlertConfig.AlertLevel.INFO);

            } catch (Exception e) {
                logWithLevel("Monitoring failed: " + e.getMessage(), AlertConfig.AlertLevel.ERROR);
            }
        }, 0, monitorConfig.getMonitoringPeriodMs(), TimeUnit.MILLISECONDS);
    }
    
    /**
     * 收集线程池统计信息
     * @return 包含当前线程池各项指标的统计对象
     */
    private ThreadPoolStats collectThreadPoolStats() {
        return new ThreadPoolStats(
                stats.snapshot(),            // 任务执行统计快照
                threadPool.getActiveCount(), // 活跃线程数
                threadPool.getQueue().size(),// 当前队列大小
                threadPool.getPoolSize(),    // 当前线程池大小
                threadPool.getMaximumPoolSize(), // 最大线程池大小
                threadPool.getCompletedTaskCount(), // 已完成任务数
                ((BlockingQueue)threadPool.getQueue()).remainingCapacity() 
                        + threadPool.getQueue().size() // 队列总容量
        );
    }

    /**
     * 监控线程使用情况
     * 当线程使用率超过阈值时发出警告
     * @param stats 线程池统计信息
     */
    private void monitorThreadUsage(ThreadPoolStats stats) {
        // 计算线程使用率百分比
        int threadUsagePercent = (int)((float)stats.getActiveThreads() / stats.getPoolSize() * 100);
        if (threadUsagePercent > alertConfig.getThreadPoolUsageThreshold()) {
            logWithLevel(
                    String.format(
                            "Thread pool usage (%d%%) exceeded threshold (%d%%)",
                            threadUsagePercent,
                            alertConfig.getThreadPoolUsageThreshold()
                    ),
                    AlertConfig.AlertLevel.WARNING
            );
        }
    }

    /**
     * 监控队列状态
     * 当队列大小超过警告阈值时发出警告
     * @param stats 线程池统计信息
     */
    private void monitorQueueStatus(ThreadPoolStats stats) {
        if (stats.getQueueSize() > alertConfig.getQueueSizeWarningThreshold()) {
            logWithLevel(
                    String.format(
                            "Queue size (%d) exceeded threshold (%d)",
                            stats.getQueueSize(),
                            alertConfig.getQueueSizeWarningThreshold()
                    ),
                    AlertConfig.AlertLevel.WARNING
            );
        }
    }

    /**
     * 处理自动扩缩容
     * 根据当前状态和配置的策略进行线程池大小的调整
     * @param stats 线程池统计信息
     */
    private void handleAutoScaling(ThreadPoolStats stats) {
        if (scalingConfig != null) {
            scaler.attemptScaling(stats);
        }
    }

    /**
     * 根据级别记录日志和发送告警
     * @param message 日志消息
     * @param level 告警级别
     */
    private void logWithLevel(String message, AlertConfig.AlertLevel level) {
        // 如果告警级别低于配置的最小告警级别，则忽略
        if (level.compareTo(alertConfig.getMinimumAlertLevel()) < 0) {
            return;
        }

        // 添加线程池名称到消息前缀
        String logMessage = String.format("[ThreadPool: %s] %s", poolName, message);

        // 构建告警元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("poolName", poolName);
        metadata.put("activeThreads", threadPool.getActiveCount());
        metadata.put("poolSize", threadPool.getPoolSize());
        metadata.put("queueSize", threadPool.getQueue().size());

        // 发布告警事件
        AlertEventPublisher.getInstance().publishAlert(logMessage, level, AlertMessageType.MONITORING, metadata);

        // 根据不同级别记录日志
        switch (level) {
            case INFO:
                logger.info(logMessage);
                break;
            case WARNING:
                logger.warning(logMessage);
                break;
            case ERROR:
            case CRITICAL:
                logger.severe(logMessage);
                break;
        }
    }

    /**
     * 关闭监控器
     * 优雅地关闭监控服务，确保资源正确释放
     */
    public void shutdown() {
        try {
            monitorService.shutdown();
            // 等待最多5秒让监控任务完成
            if (!monitorService.awaitTermination(5, TimeUnit.SECONDS)) {
                monitorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Monitor shutdown interrupted");
        }
    }
}