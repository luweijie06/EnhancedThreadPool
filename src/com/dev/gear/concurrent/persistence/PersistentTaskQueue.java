package com.dev.gear.concurrent.persistence;

import com.dev.gear.concurrent.TrackedTask;
import com.dev.gear.concurrent.monitor.TaskStats;

import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

/**
 * 持久化任务队列
 */
public class PersistentTaskQueue extends PriorityBlockingQueue<Runnable> {
    private static final Logger logger = Logger.getLogger(PersistentTaskQueue.class.getName());
    private final PersistenceStrategy persistenceStrategy;
    private final TaskStats stats;
    private final ScheduledExecutorService persistenceScheduler;
    private volatile int capacity;  // 使用 volatile 确保线程可见性
    private final ReentrantLock resizeLock = new ReentrantLock();  // 用于调整大小的锁

    public PersistentTaskQueue(int capacity, PersistenceStrategy persistenceStrategy, TaskStats stats) {
        super(Math.max(capacity, 16));  // 确保初始容量合理
        this.capacity = capacity;
        this.persistenceStrategy = persistenceStrategy;
        this.stats = stats;
        this.persistenceScheduler = Executors.newSingleThreadScheduledExecutor(
            r -> {
                Thread t = new Thread(r, "PersistentQueue-Scheduler");
                t.setDaemon(true);  // 设置为守护线程
                return t;
            }
        );
        persistenceScheduler.scheduleAtFixedRate(
                this::persistQueue,
                1, 1, TimeUnit.MINUTES
        );
    }

    public TaskStats getStats() {
        return stats;
    }

    @Override
    public boolean offer(Runnable task) {
        if (size() >= capacity) {
            return false;  // 当超过容量时拒绝新任务
        }
        boolean result = super.offer(task);
        if (result && size() % 100 == 0) {
            persistQueue();
        }
        return result;
    }



    private void persistQueue() {

        try {
            Queue<SerializableTask> serializableTasks = new LinkedList<>();
            for (Runnable runnable : this) {
                if (runnable instanceof TrackedTask) {
                    serializableTasks.offer(new SerializableTask((TrackedTask) runnable));
                }
            }
            persistenceStrategy.save(serializableTasks);
        } catch (PersistenceException | IOException e) {
            logger.severe("Failed to persist task queue: " + e.getMessage());
        }
    }


    public int getCapacity() {
        return capacity;
    }

    public void shutdown() {
        persistQueue();
        persistenceScheduler.shutdown();
        try {
            if (!persistenceScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                persistenceScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            persistenceScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}