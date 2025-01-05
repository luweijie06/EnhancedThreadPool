package com.dev.gear.concurrent.persistence;

import java.io.IOException;
import java.util.Queue;

/**
 * 任务持久化策略接口
 */
public interface PersistenceStrategy {
    /**
     * 保存任务队列
     */
    void save(Queue<SerializableTask> tasks) throws PersistenceException, IOException;


    /**
     * 加载持久化的任务
     */
    Queue<SerializableTask> load() throws PersistenceException, IOException;

    /**
     * 清理持久化存储
     */
    void cleanup() throws PersistenceException, IOException;
}