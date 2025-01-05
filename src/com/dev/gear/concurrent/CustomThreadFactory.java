package com.dev.gear.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;


/**
 * 自定义线程工厂
 */
public  class CustomThreadFactory implements ThreadFactory {
    private static final Logger logger = Logger.getLogger(CustomThreadFactory.class.getName());

    private final ThreadGroup group;
    private final AtomicLong threadNumber = new AtomicLong(1);
    private final String namePrefix;

    CustomThreadFactory(String poolName) {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        namePrefix = poolName + "-thread-";
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
        if (t.isDaemon()) {
            t.setDaemon(false);
        }
        if (t.getPriority() != Thread.NORM_PRIORITY) {
            t.setPriority(Thread.NORM_PRIORITY);
        }
        t.setUncaughtExceptionHandler((thread, e) ->
                logger.severe("Uncaught exception in thread " + thread.getName() + ": " + e.getMessage())
        );
        return t;
    }
}
