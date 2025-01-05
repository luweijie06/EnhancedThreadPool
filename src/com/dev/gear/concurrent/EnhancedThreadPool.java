package com.dev.gear.concurrent;

import com.dev.gear.concurrent.config.MonitoringConfig;
import com.dev.gear.concurrent.config.ThreadPoolConfig;
import com.dev.gear.concurrent.expand.ThreadPoolMonitor;
import com.dev.gear.concurrent.expand.ThreadPoolScalier;
import com.dev.gear.concurrent.monitor.TaskStats;
import com.dev.gear.concurrent.monitor.ThreadPoolStats;
import com.dev.gear.concurrent.persistence.PersistentTaskQueue;

import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * 增强型线程池执行器
 */
public class EnhancedThreadPool extends ThreadPoolExecutor {
    private static final Logger logger = Logger.getLogger(EnhancedThreadPool.class.getName());

    // 使用volatile保证任务统计信息的可见性
    private volatile TaskStats stats;

    // 线程池关闭状态标志
    private volatile boolean isShutdown = false;

    // 线程池动态扩缩容管理器
    private final ThreadPoolScalier scaler;

    // 线程池配置信息（使用final保证不可变性）
    private final ThreadPoolConfig config;

    // 持久化任务队列
    private final PersistentTaskQueue taskQueue;

    // 监控配置信息
    private final MonitoringConfig monitorConfig;

    // 线程池监控器
    private final ThreadPoolMonitor monitor;

    // 线程池名称
    private final String poolName;

    // 使用ThreadLocal存储任务上下文，避免线程间同步开销
    private final ThreadLocal<TaskContext> taskContext = ThreadLocal.withInitial(() -> null);

    /**
     * 私有构造函数，通过Builder模式创建实例
     * 初始化线程池的核心组件：
     * - 任务队列
     * - 线程工厂
     * - 拒绝策略处理器
     * - 动态扩缩容管理器
     * - 监控器
     */
    private EnhancedThreadPool(Builder builder) {
        super(builder.corePoolSize,
                builder.maxPoolSize,
                builder.keepAliveTime,
                builder.timeUnit,
                new PersistentTaskQueue(
                        builder.queueCapacity,
                        builder.config.getPersistenceStrategy(),
                        createTaskStats(builder.config.getMonitoringConfig())
                ),
                new CustomThreadFactory(builder.poolName),
                new TaskRejectHandler());

        this.scaler = new ThreadPoolScalier(
                this,
                builder.config.getScalingConfig(),
                builder.poolName
        );

        this.stats = ((PersistentTaskQueue)getQueue()).getStats();
        this.config = builder.config;
        this.monitorConfig = builder.config.getMonitoringConfig();
        this.taskQueue = (PersistentTaskQueue)getQueue();
        this.poolName = builder.poolName;

        // 初始化线程池监控器
        this.monitor = new ThreadPoolMonitor(
                this,
                builder.config.getMonitoringConfig(),
                builder.config.getAlertConfig(),
                builder.config.getScalingConfig(),
                scaler,
                stats,
                builder.poolName
        );

        // 注册JVM关闭钩子，确保线程池优雅关闭
        Runtime.getRuntime().addShutdownHook(new Thread(this::gracefulShutdown));
    }
    private static BlockingQueue<Runnable> createDefaultQueue(Builder builder) {
        if (builder.config.isPersistenceEnabled()) {
            return new PersistentTaskQueue(
                    builder.queueCapacity,
                    builder.config.getPersistenceStrategy(),
                    createTaskStats(builder.config.getMonitoringConfig())
            );
        } else {
            return new PriorityBlockingQueue<>(builder.queueCapacity);
        }
    }
    /**
     * 获取线程池名称
     * @return 线程池名称
     */
    public String getPoolName() {
        return poolName;
    }

    /**
     * Builder模式构建器类
     * 用于配置和创建EnhancedThreadPool实例
     * 提供流式API和参数验证
     */
    public static class Builder {
        // 默认配置值
        private int corePoolSize = Runtime.getRuntime().availableProcessors(); // 核心线程数默认为CPU核心数
        private int maxPoolSize = corePoolSize * 2;  // 最大线程数默认为核心线程数的2倍
        private long keepAliveTime = 60L;  // 线程存活时间
        private TimeUnit timeUnit = TimeUnit.SECONDS;  // 时间单位
        private int queueCapacity = 1000;  // 队列容量
        private String poolName = "EnhancedThreadPool";  // 线程池名称
        private ThreadPoolConfig config = new ThreadPoolConfig.Builder().build();  // 线程池配置

        public Builder() {
            // 默认构造函数
        }

        // 设置核心线程数
        public Builder corePoolSize(int size) {
            this.corePoolSize = size;
            return this;
        }

        // 设置最大线程数
        public Builder maxPoolSize(int size) {
            this.maxPoolSize = size;
            return this;
        }

        // 设置线程存活时间
        public Builder keepAliveTime(long time, TimeUnit unit) {
            this.keepAliveTime = time;
            this.timeUnit = unit;
            return this;
        }

        // 设置队列容量
        public Builder queueCapacity(int capacity) {
            this.queueCapacity = capacity;
            return this;
        }

        // 设置线程池名称
        public Builder poolName(String name) {
            this.poolName = name;
            return this;
        }

        // 设置线程池配置
        public Builder configuration(ThreadPoolConfig config) {
            this.config = config;
            return this;
        }

        // 构建线程池实例
        public EnhancedThreadPool build() {
            validate();  // 验证配置参数
            return new EnhancedThreadPool(this);
        }

        /**
         * 验证配置参数的合法性
         * 检查项包括：
         * - 线程数配置
         * - 存活时间
         * - 队列容量
         * - 线程池名称
         * - 配置对象
         */
        private void validate() {
            if (corePoolSize < 0 || maxPoolSize <= 0 || maxPoolSize < corePoolSize) {
                throw new IllegalArgumentException(
                        String.format("Invalid pool size configuration: core=%d, max=%d",
                                corePoolSize, maxPoolSize));
            }
            if (keepAliveTime < 0) {
                throw new IllegalArgumentException("Invalid keepAliveTime: " + keepAliveTime);
            }
            if (queueCapacity <= 0) {
                throw new IllegalArgumentException("Invalid queue capacity: " + queueCapacity);
            }
            if (poolName == null || poolName.trim().isEmpty()) {
                throw new IllegalArgumentException("Pool name cannot be null or empty");
            }
            if (config == null) {
                throw new IllegalArgumentException("ThreadPoolConfig cannot be null");
            }
        }
    }

    /**
     * 任务拒绝处理器
     * 当线程池无法接受新任务时的处理策略
     */
    private static class TaskRejectHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (r instanceof TrackedTask) {
                TrackedTask task = (TrackedTask) r;
                logger.warning("Task rejected: " + task.getTaskId());
                if (executor instanceof EnhancedThreadPool) {
                    ((EnhancedThreadPool) executor).stats.recordRejection();
                }
            }
            throw new RejectedExecutionException("Task " + r.toString() + " rejected");
        }
    }

    /**
     * 根据监控配置创建任务统计对象
     * @param config 监控配置
     * @return 任务统计对象
     */
    private static TaskStats createTaskStats(MonitoringConfig config) {
        if (config.isLatencyMetricsEnabled()) {
            return new TaskStats(config.getLatencyPercentiles(), 10000);
        } else {
            return new TaskStats(null, 10000);
        }
    }

    /**
     * 提交带优先级的任务
     * @param task 任务
     * @param priority 优先级
     * @return Future对象
     */
    public Future<?> submit(Runnable task, int priority) {
        if (task == null) throw new NullPointerException();
        TrackedTask trackedTask = new TrackedTask(task, stats, priority);
        return submit(trackedTask);
    }

    /**
     * 执行任务（使用默认优先级）
     */
    @Override
    public void execute(Runnable command) {
        execute(command, 5); // 默认优先级为5
    }

    /**
     * 执行带优先级的任务
     * @param command 任务
     * @param priority 优先级
     */
    public void execute(Runnable command, int priority) {
        if (command == null) throw new NullPointerException();
        if (monitorConfig.isTaskMetricsEnabled()) {
            stats.recordSubmission();
        }
        super.execute(new TrackedTask(command, stats, priority));
    }

    /**
     * 任务执行前的处理
     * - 创建任务上下文
     * - 记录队列相关指标
     */
    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        if (r instanceof TrackedTask) {
            TrackedTask task = (TrackedTask) r;
            TaskContext context = new TaskContext(task);
            taskContext.set(context);

            // 根据配置决定是否记录队列指标
            if (monitorConfig.isQueueMetricsEnabled()) {
                stats.recordQueueTime(System.currentTimeMillis() - task.getSubmitTime());
                stats.recordQueueSize(getQueue().size());
            }
        }
        super.beforeExecute(t, r);
    }

    /**
     * 任务执行后的处理
     * - 记录任务执行时间
     * - 更新任务统计信息
     * - 清理任务上下文
     */
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        try {
            TaskContext context = taskContext.get();
            if (context != null) {
                long duration = System.currentTimeMillis() - context.startTime;

                // 根据配置决定是否记录任务执行指标
                if (monitorConfig.isTaskMetricsEnabled()) {
                    stats.recordExecutionTime(duration);
                    if (t != null) {
                        stats.recordFailure();
                    } else {
                        stats.recordCompletion();
                    }
                }

                // 根据配置决定是否记录延迟指标
                if (monitorConfig.isLatencyMetricsEnabled()) {
                    stats.recordWaitTime(duration);
                }
            }
        } finally {
            taskContext.remove();
            super.afterExecute(r, t);
        }
    }

    /**
     * 获取线程池统计信息
     * 根据监控配置返回不同维度的统计数据
     * @return 线程池统计信息对象
     */
    public ThreadPoolStats getStats() {
        TaskStats taskStatsSnapshot = null;

        // 根据监控配置决定是否获取任务统计信息
        if (monitorConfig.isTaskMetricsEnabled() ||
                monitorConfig.isLatencyMetricsEnabled() ||
                monitorConfig.isQueueMetricsEnabled()) {
            taskStatsSnapshot = stats.snapshot();
        } else {
            // 如果所有相关指标都未启用，创建一个空的TaskStats
            taskStatsSnapshot = new TaskStats();
        }

        // 创建基础的ThreadPoolStats实例
        ThreadPoolStats.Builder statsBuilder = new ThreadPoolStats.Builder()
                .taskStats(taskStatsSnapshot);

        // 添加线程相关指标
        if (monitorConfig.isThreadMetricsEnabled()) {
            statsBuilder
                    .activeThreads(getActiveCount())
                    .poolSize(getPoolSize())
                    .maxPoolSize(getMaximumPoolSize());
        }

        // 添加队列相关指标
        if (monitorConfig.isQueueMetricsEnabled()) {
            statsBuilder
                    .queueSize(getQueue().size())
                    .queueCapacity(taskQueue.getCapacity());
        }

        // 添加任务完成指标
        if (monitorConfig.isTaskMetricsEnabled()) {
            statsBuilder.completedTasks(getCompletedTaskCount());
        }

        return statsBuilder.build();
    }

    /**
     * 关闭线程池
     * 停止接收新任务，等待现有任务完成
     */
    @Override
    public void shutdown() {
        logger.info("Initiating shutdown...");
        monitor.shutdown(); // 关闭监控器
        taskQueue.shutdown();
        super.shutdown();
    }

    /**
     * 优雅关闭处理
     * - 停止接收新任务
     * - 关闭监控器
     * - 等待现有任务完成
     * - 释放资源
     */
    private void gracefulShutdown() {
        if (isShutdown) {
            return;
        }

        isShutdown = true;

        try {
            // 停止接收新任务并关闭监控
            monitor.shutdown();
            shutdown();

            // 等待现有任务完成（最多等待30秒）
            if (!awaitTermination(30, TimeUnit.SECONDS)) {
                // 超时强制关闭
                shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Shutdown interrupted");
        } finally {
            // 确保资源释放
            taskQueue.shutdown();
        }
    }
}