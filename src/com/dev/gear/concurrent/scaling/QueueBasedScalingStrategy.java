package com.dev.gear.concurrent.scaling;

import com.dev.gear.concurrent.monitor.ThreadPoolStats;

public class QueueBasedScalingStrategy implements ScalingStrategy {
    private final int queueSizeThreshold;   // 队列大小阈值
    private final int scaleUpSize;          // 每次扩容的线程数
    private final double queueCapacityIncreaseRatio; // 队列容量增加比例
    
    public QueueBasedScalingStrategy(
            int queueSizeThreshold, 
            int scaleUpSize,
            double queueCapacityIncreaseRatio) {
        this.queueSizeThreshold = queueSizeThreshold;
        this.scaleUpSize = scaleUpSize;
        this.queueCapacityIncreaseRatio = queueCapacityIncreaseRatio;
    }
    
    @Override
    public ScalingCommand calculateScaling(ThreadPoolStats stats) {
        if (stats.getQueueSize() > queueSizeThreshold && 
            stats.getPoolSize() < stats.getMaxPoolSize()) {
            
            // 计算队列容量增加量
            int queueCapacityDelta = (int)(stats.getQueueSize() * queueCapacityIncreaseRatio);
            
            return new ScalingCommand.Builder()
                .threadDelta(scaleUpSize)
                .coreSizeDelta(scaleUpSize)
                .maxSizeDelta(scaleUpSize * 2)
                .queueCapacityDelta(queueCapacityDelta)
                .reason(String.format(
                    "Queue size (%d) exceeded threshold (%d)",
                    stats.getQueueSize(),
                    queueSizeThreshold
                ))
                .build();
        }
        return null;  // 不需要调整时返回null
    }
}