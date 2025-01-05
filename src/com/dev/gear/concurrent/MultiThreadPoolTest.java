package com.dev.gear.concurrent;

import com.dev.gear.concurrent.config.AlertConfig;
import com.dev.gear.concurrent.config.MonitoringConfig;
import com.dev.gear.concurrent.config.ThreadPoolConfig;
import domin.config.*;
import com.dev.gear.concurrent.event.AlertEventListener;
import com.dev.gear.concurrent.event.AlertEventPublisher;
import com.dev.gear.concurrent.event.AlertMessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class MultiThreadPoolTest {
    private static final Logger logger = Logger.getLogger(MultiThreadPoolTest.class.getName());
    private static final List<EnhancedThreadPool> threadPools = new ArrayList<>();

    public static void main(String[] args) {
        // 1. 设置全局告警监听器
        setupGlobalAlertListeners();

        // 2. 创建多个不同配置的线程池
        createThreadPools();

        // 3. 提交不同类型的任务到各个线程池
        submitDifferentTasks();

        // 4. 等待一段时间观察监控结果
        waitAndObserve();

        // 5. 关闭所有线程池
        shutdownAllPools();
    }

    private static void setupGlobalAlertListeners() {
        AlertEventPublisher publisher = AlertEventPublisher.getInstance();

        // 日志监听器 - 监听所有级别
        AlertEventListener logListener = event -> {
            logger.warning(String.format("[%s][%s] %s - Pool: %s, Metadata: %s",
                    event.getLevel(),
                    event.getMessageType(),
                    event.getMessage(),
                    event.getMetadata()));
        };

        // 严重告警监听器
        AlertEventListener criticalListener = event -> {
            if (event.getLevel() == AlertConfig.AlertLevel.CRITICAL) {
                logger.severe(String.format("CRITICAL ALERT in pool %s: %s",
                    event.getMessage()));
            }
        };

        // 注册全局监听器
        publisher.subscribe(AlertConfig.AlertLevel.INFO, AlertMessageType.MONITORING, logListener, "ALL");
        publisher.subscribe(AlertConfig.AlertLevel.CRITICAL, AlertMessageType.MONITORING, criticalListener, "ALL");
    }

    private static void createThreadPools() {
        // 1. 高负载线程池 - 较小的队列和较严格的告警阈值
        threadPools.add(createHighLoadThreadPool());

        // 2. 低延迟线程池 - 较多的核心线程和较短的超时时间
        threadPools.add(createLowLatencyThreadPool());

        // 3. 批处理线程池 - 大队列和较宽松的告警阈值
        threadPools.add(createBatchProcessingThreadPool());
    }

    private static EnhancedThreadPool createHighLoadThreadPool() {
        AlertConfig alertConfig = new AlertConfig.Builder()
                .setQueueSizeWarningThreshold(50)          // 较小的队列告警阈值
                .setTaskTimeoutMs(3000)                    // 较短的任务超时时间
                .setThreadPoolUsageThreshold(70)           // 较低的使用率告警阈值
                .setMinimumAlertLevel(AlertConfig.AlertLevel.WARNING)
                .build();

        MonitoringConfig monitorConfig = new MonitoringConfig.Builder()
                .setMonitoringPeriodMs(500)// 更频繁的监控
                .setSamplingIntervalMs(300)
                .setEnableDetailedMetrics(true)
                .setEnableLatencyMetrics(true)
                .setEnableQueueMetrics(true)
                .setEnableTaskMetrics(true)
                .build();

        ThreadPoolConfig poolConfig = new ThreadPoolConfig.Builder()
                .alertConfig(alertConfig)
                .monitoringConfig(monitorConfig)
                .build();

        return new EnhancedThreadPool.Builder()
                .corePoolSize(4)
                .maxPoolSize(8)
                .keepAliveTime(30, TimeUnit.SECONDS)
                .queueCapacity(100)
                .poolName("HighLoadPool")
                .configuration(poolConfig)
                .build();
    }

    private static EnhancedThreadPool createLowLatencyThreadPool() {
        AlertConfig alertConfig = new AlertConfig.Builder()
                .setQueueSizeWarningThreshold(20)          // 很小的队列告警阈值
                .setTaskTimeoutMs(1000)                    // 非常短的任务超时时间
                .setThreadPoolUsageThreshold(80)
                .setMinimumAlertLevel(AlertConfig.AlertLevel.WARNING)
                .build();

        MonitoringConfig monitorConfig = new MonitoringConfig.Builder()
                .setMonitoringPeriodMs(500)// 更频繁的监控
                .setSamplingIntervalMs(300)          // 非常频繁的监控
                .setEnableDetailedMetrics(true)
                .setEnableLatencyMetrics(true)
                .setEnableQueueMetrics(true)
                .setEnableTaskMetrics(true)
                .build();

        ThreadPoolConfig poolConfig = new ThreadPoolConfig.Builder()
                .alertConfig(alertConfig)
                .monitoringConfig(monitorConfig)
                .build();

        return new EnhancedThreadPool.Builder()
                .corePoolSize(8)
                .maxPoolSize(16)
                .keepAliveTime(10, TimeUnit.SECONDS)
                .queueCapacity(50)
                .poolName("LowLatencyPool")
                .configuration(poolConfig)
                .build();
    }

    private static EnhancedThreadPool createBatchProcessingThreadPool() {
        AlertConfig alertConfig = new AlertConfig.Builder()
                .setQueueSizeWarningThreshold(200)         // 大队列告警阈值
                .setTaskTimeoutMs(10000)                   // 较长的任务超时时间
                .setThreadPoolUsageThreshold(90)           // 较高的使用率告警阈值
                .setMinimumAlertLevel(AlertConfig.AlertLevel.WARNING)
                .build();

        MonitoringConfig monitorConfig = new MonitoringConfig.Builder()
                .setMonitoringPeriodMs(500)// 更频繁的监控
                .setSamplingIntervalMs(300)                 // 正常监控频率
                .setEnableDetailedMetrics(true)
                .setEnableLatencyMetrics(true)
                .setEnableQueueMetrics(true)
                .setEnableTaskMetrics(true)
                .build();

        ThreadPoolConfig poolConfig = new ThreadPoolConfig.Builder()
                .alertConfig(alertConfig)
                .monitoringConfig(monitorConfig)
                .build();

        return new EnhancedThreadPool.Builder()
                .corePoolSize(2)
                .maxPoolSize(4)
                .keepAliveTime(120, TimeUnit.SECONDS)
                .queueCapacity(400)
                .poolName("BatchProcessingPool")
                .configuration(poolConfig)
                .build();
    }

    private static void submitDifferentTasks() {
        // 1. 提交任务到高负载线程池 - 大量短任务
        EnhancedThreadPool highLoadPool = threadPools.get(0);
        for (int i = 0; i < 100; i++) {
            final int taskId = i;
            highLoadPool.submit(() -> {
                try {
                    Thread.sleep(100);
                    if (taskId % 50 == 0) {
                        Thread.sleep(2000); // 偶尔添加超时任务
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, 5);
        }

        // 2. 提交任务到低延迟线程池 - 快速任务
        EnhancedThreadPool lowLatencyPool = threadPools.get(1);
        for (int i = 0; i < 20; i++) {
            final int taskId = i;
            lowLatencyPool.submit(() -> {
                try {
                    Thread.sleep(50);
                    if (taskId % 20 == 0) {
                        Thread.sleep(2000); // 偶尔添加较慢任务
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, 8);
        }

        // 3. 提交任务到批处理线程池 - 少量长任务
        EnhancedThreadPool batchPool = threadPools.get(2);
        for (int i = 0; i < 20; i++) {
            final int taskId = i;
            batchPool.submit(() -> {
                try {
                    Thread.sleep(1000);
                    if (taskId % 5 == 0) {
                        Thread.sleep(14000); // 偶尔添加超长任务
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, 3);
        }
    }

    private static void waitAndObserve() {
        try {
            logger.info("Waiting for 30 seconds to observe monitoring metrics...");
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Observation period interrupted");
        }
    }

    private static void shutdownAllPools() {
        for (EnhancedThreadPool pool : threadPools) {
            try {
                pool.shutdown();
            } catch (Exception e) {
                logger.severe("Error while shutting down pool " + pool.getPoolName() + ": " + e.getMessage());
            }
        }
    }
}