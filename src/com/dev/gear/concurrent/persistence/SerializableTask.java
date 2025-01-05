package com.dev.gear.concurrent.persistence;

import com.dev.gear.concurrent.TrackedTask;
import com.dev.gear.concurrent.monitor.TaskStats;

import java.io.*;

/**
 * 可序列化的任务类
 * 该类用于将任务对象转换为可以持久化存储的格式
 */
class SerializableTask implements Serializable {
    // 序列化版本号，用于版本控制
    private static final long serialVersionUID = 1L;
    
    // 任务的唯一标识符
    private final String taskId;
    
    // 任务提交时间戳
    private final long submitTime;
    
    // 任务优先级
    private final int priority;
    
    // 序列化后的任务字节数组
    private final byte[] serializedTask;

    /**
     * 构造方法 - 直接使用序列化数据创建任务
     * @param taskId 任务ID
     * @param submitTime 提交时间
     * @param priority 优先级
     * @param serializedTask 已序列化的任务数据
     */
    public SerializableTask(String taskId, long submitTime, int priority, byte[] serializedTask) {
        this.taskId = taskId;
        this.submitTime = submitTime;
        this.priority = priority;
        this.serializedTask = serializedTask;
    }

    /**
     * 构造方法 - 将TrackedTask对象转换为可序列化格式
     * @param task 要序列化的TrackedTask对象
     * @throws IOException 序列化过程中可能发生的IO异常
     */
    public SerializableTask(TrackedTask task) throws IOException {
        this.taskId = task.getTaskId();
        this.submitTime = task.getSubmitTime();
        this.priority = task.getPriority();

        // 将任务对象序列化为字节数组
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(task.getTask());
        this.serializedTask = bos.toByteArray();
    }

    // 获取任务ID
    public String getTaskId() { return taskId; }
    
    // 获取提交时间
    public long getSubmitTime() { return submitTime; }
    
    // 获取优先级
    public int getPriority() { return priority; }
    
    // 获取序列化后的任务字节数组
    public byte[] getSerializedTaskBytes() { return serializedTask; }

    /**
     * 将序列化的任务转换回TrackedTask对象
     * @param stats 任务统计信息
     * @return 反序列化后的TrackedTask对象
     * @throws IOException 反序列化过程中可能发生的IO异常
     * @throws ClassNotFoundException 如果找不到序列化对象的类定义
     */
    public TrackedTask toTrackedTask(TaskStats stats) throws IOException, ClassNotFoundException {
        // 从字节数组中反序列化任务对象
        ByteArrayInputStream bis = new ByteArrayInputStream(serializedTask);
        ObjectInputStream in = new ObjectInputStream(bis);
        Runnable originalTask = (Runnable) in.readObject();

        // 创建新的TrackedTask对象
        return new TrackedTask(originalTask, stats, priority, taskId, submitTime);
    }
}