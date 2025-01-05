package com.dev.gear.concurrent.scaling;


import com.dev.gear.concurrent.monitor.ThreadPoolStats;

public interface ScalingStrategy {
    /**
     * 根据线程池当前状态决定是否需要调整线程池大小
     * @param stats 线程池统计信息
     * @return 建议的线程池大小调整，正数表示扩容，负数表示缩容，0表示维持不变
     */
    ScalingCommand calculateScaling(ThreadPoolStats stats);
}