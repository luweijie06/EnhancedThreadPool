package com.dev.gear.concurrent;


import com.dev.gear.concurrent.config.AlertConfig;
import com.dev.gear.concurrent.config.MonitoringConfig;
import com.dev.gear.concurrent.config.ThreadPoolConfig;
import com.dev.gear.concurrent.event.AlertEvent;
import com.dev.gear.concurrent.event.AlertEventListener;
import com.dev.gear.concurrent.event.AlertEventPublisher;
import com.dev.gear.concurrent.event.AlertMessageType;
import com.dev.gear.concurrent.persistence.NoOpPersistenceStrategy;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class EnhancedThreadPoolExample {
    private static final Logger logger = Logger.getLogger(ThreadPoolExample.class.getName());

    public static void main(String[] args) {
        // 1. 创建告警监听器
        setupAlertListeners();

        // 2. 配置线程池
        EnhancedThreadPool threadPool = createThreadPool();

        // 3. 提交任务示例
        submitTasks(threadPool);
/*
        // 4. 等待任务完成并关闭线程池
        shutdownThreadPool(threadPool);*/
    }

    private static void setupAlertListeners() {
        // 创建日志告警监听器
        AlertEventListener logListener = event -> {
            logger.warning(String.format("[%s] %s - Metadata: %s",
                    event.getLevel(),
                    event.getMessage(),
                    event.getMetadata()));
        };

        // 创建邮件告警监听器（示例）
        AlertEventListener emailListener = event -> {
            if (event.getLevel() == AlertConfig.AlertLevel.CRITICAL) {
                sendEmailAlert(event); // 实现邮件发送逻辑
            }
        };

        // 创建钉钉告警监听器（示例）
        AlertEventListener dingTalkListener = event -> {
            if (event.getLevel().compareTo(AlertConfig.AlertLevel.WARNING) >= 0) {
                sendDingTalkMessage(event); // 实现钉钉消息发送逻辑
            }
        };

        // 注册监听器
        AlertEventPublisher publisher = AlertEventPublisher.getInstance();
        publisher.subscribe(AlertConfig.AlertLevel.CRITICAL, AlertMessageType.MONITORING, emailListener,"ExamplePool"); // 只订阅严重告警
        publisher.subscribe(AlertConfig.AlertLevel.WARNING,AlertMessageType.MONITORING, dingTalkListener,"ExamplePool"); // 订阅警告及以上级别
    }

    private static EnhancedThreadPool createThreadPool() {
        // 创建告警配置
        AlertConfig alertConfig = new AlertConfig.Builder()
                .setQueueSizeWarningThreshold(100)         // 队列达到100时告警
                .setTaskTimeoutMs(3000)                    // 任务执行超过5秒告警
                .setThreadPoolUsageThreshold(75)           // 线程池使用率达到75%告警
                .setMinimumAlertLevel(AlertConfig.AlertLevel.WARNING)  // 最小告警级别为WARNING
                .build();

        // 创建监控配置
        MonitoringConfig monitorConfig = new MonitoringConfig.Builder()
                .setMonitoringPeriodMs(1000)              // 每秒监控一次
                .setEnableDetailedMetrics(true)          // 启用详细指标
                .setEnableLatencyMetrics(true)            // 启用线程指标
                .setEnableQueueMetrics(true)             // 启用队列指标
                .setEnableTaskMetrics(true)              // 启用任务指标
                .build();
        // 创建持久化策略
        NoOpPersistenceStrategy persistenceStrategy = new NoOpPersistenceStrategy();

        // 创建线程池配置
        ThreadPoolConfig poolConfig = new ThreadPoolConfig.Builder()
                .alertConfig(alertConfig)
                .monitoringConfig(monitorConfig)
                .persistenceStrategy(persistenceStrategy)
                .build();

        // 创建线程池
        return new EnhancedThreadPool.Builder()
                .corePoolSize(4)
                .maxPoolSize(8)
                .keepAliveTime(60, TimeUnit.SECONDS)
                .queueCapacity(200)
                .poolName("ExamplePool")
                .configuration(poolConfig)
                .build();
    }

    private static void submitTasks(EnhancedThreadPool threadPool) {
        // 提交正常任务
        for (int i = 0; i < 50; i++) {
            final int taskId = i;
            threadPool.submit(() -> {
                try {
                    // 模拟任务执行
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, 5); // 优先级为5
        }

        // 提交一个超时任务来触发告警
        threadPool.submit(() -> {
            try {
                Thread.sleep(4000); // 超过配置的超时时间
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 8); // 高优先级任务
    }


    // 模拟发送邮件告警
    private static void sendEmailAlert(AlertEvent event) {
        logger.info(String.format("Sending email alert: %s", event.getMessage()));
        // 实现邮件发送逻辑
    }

    // 模拟发送钉钉消息
    private static void sendDingTalkMessage(AlertEvent event) {
        logger.info(String.format("Sending DingTalk message: %s", event.getMessage()));
        // 实现钉钉消息发送逻辑
    }
}