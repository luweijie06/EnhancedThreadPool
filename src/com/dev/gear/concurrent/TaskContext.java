package com.dev.gear.concurrent;

public  class TaskContext {
        final long startTime;
        final TrackedTask task;
        final long submitTime;

        TaskContext(TrackedTask task) {
            this.startTime = System.currentTimeMillis();
            this.submitTime = System.currentTimeMillis();
            this.task = task;
        }
    }