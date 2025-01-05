package com.dev.gear.concurrent.persistence;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * 空实现的持久化策略
 * 这个实现不会实际执行任何持久化操作，适用于不需要持久化的场景
 */
public class NoOpPersistenceStrategy implements PersistenceStrategy {
    private static final Logger logger = Logger.getLogger(NoOpPersistenceStrategy.class.getName());

    @Override
    public void save(Queue<SerializableTask> tasks) {
        logger.fine("NoOp persistence strategy: Skipping save operation for " + tasks.size() + " tasks");
        // 不执行任何操作
    }

    @Override
    public Queue<SerializableTask> load() {
        logger.fine("NoOp persistence strategy: Returning empty queue");
        // 始终返回空队列
        return new LinkedList<>();
    }

    @Override
    public void cleanup() {
        logger.fine("NoOp persistence strategy: Skipping cleanup operation");
        // 不执行任何操作
    }
}