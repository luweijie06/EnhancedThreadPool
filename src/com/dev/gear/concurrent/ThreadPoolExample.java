package com.dev.gear.concurrent;



import com.dev.gear.concurrent.persistence.FileSystemPersistenceStrategy;

import java.util.logging.Logger;




/**
 * 使用示例
 */
public class ThreadPoolExample {
    private static final Logger logger = Logger.getLogger(ThreadPoolExample.class.getName());
   /**
    * 监控配置
    *   提供线程池各个指标
    * 告警配置
    *   设置对应 阈值到达进行提醒
    * 持久化配置
    *   持久化数据
    * 重发
    *   任务重新执行
    * */
    public static void main(String[] args) {
        // 1. 创建持久化策略
        FileSystemPersistenceStrategy persistenceStrategy = new FileSystemPersistenceStrategy("tasks.dat");

    }


}