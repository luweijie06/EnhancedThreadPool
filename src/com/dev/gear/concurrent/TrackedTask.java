package com.dev.gear.concurrent;

import com.dev.gear.concurrent.monitor.TaskStats;

import java.util.UUID;

public class TrackedTask implements Runnable, Comparable<TrackedTask> {
    private final Runnable task;
    private final String taskId;
    private final long submitTime;
    private final TaskStats stats;
    private final int priority;

    public TrackedTask(Runnable task, TaskStats stats, int priority) {
        this(task, stats, priority, UUID.randomUUID().toString(), System.currentTimeMillis());
    }

    public TrackedTask(Runnable task, TaskStats stats, int priority, String taskId, long submitTime) {
        this.task = task;
        this.taskId = taskId;
        this.submitTime = submitTime;
        this.stats = stats;
        this.priority = priority;
    }

    public Runnable getTask() { return task; }
    public String getTaskId() { return taskId; }
    public long getSubmitTime() { return submitTime; }
    public int getPriority() { return priority; }

    @Override
    public void run() {
        try {
            long waitTime = System.currentTimeMillis() - submitTime;
            stats.recordWaitTime(waitTime);

            long startTime = System.currentTimeMillis();
            task.run();

            long executionTime = System.currentTimeMillis() - startTime;
            stats.recordExecutionTime(executionTime);

        } catch (Exception e) {
            stats.recordFailure();
            throw e;
        }
    }

    @Override
    public int compareTo(TrackedTask other) {
        int priorityCompare = Integer.compare(this.priority, other.priority);
        if (priorityCompare != 0) {
            return priorityCompare;
        }
        return Long.compare(this.submitTime, other.submitTime);
    }
}

