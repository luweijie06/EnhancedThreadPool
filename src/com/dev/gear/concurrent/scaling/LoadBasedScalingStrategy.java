package com.dev.gear.concurrent.scaling;

import com.dev.gear.concurrent.monitor.ThreadPoolStats;

public class LoadBasedScalingStrategy implements ScalingStrategy {
    private final double highLoadThreshold;  // 高负载阈值，例如0.8表示80%
    private final double lowLoadThreshold;   // 低负载阈值，例如0.2表示20%
    private final int scaleUpSize;          // 每次扩容的线程数
    private final int scaleDownSize;        // 每次缩容的线程数
    private final long keepAliveAdjustment; // 空闲线程存活时间调整量
    
    public LoadBasedScalingStrategy(
            double highLoadThreshold,
            double lowLoadThreshold,
            int scaleUpSize,
            int scaleDownSize,
            long keepAliveAdjustment) {
        this.highLoadThreshold = highLoadThreshold;
        this.lowLoadThreshold = lowLoadThreshold;
        this.scaleUpSize = scaleUpSize;
        this.scaleDownSize = scaleDownSize;
        this.keepAliveAdjustment = keepAliveAdjustment;
    }
    
    @Override
    public ScalingCommand calculateScaling(ThreadPoolStats stats) {
        double currentLoad = (double) stats.getActiveThreads() / stats.getPoolSize();
        ScalingCommand.Builder builder = new ScalingCommand.Builder();
        
        if (currentLoad > highLoadThreshold && stats.getPoolSize() < stats.getMaxPoolSize()) {
            // 高负载情况：扩容并减少空闲线程存活时间
            return builder
                .threadDelta(scaleUpSize)
                .coreSizeDelta(scaleUpSize)
                .maxSizeDelta(scaleUpSize * 2)
                .keepAliveTimeDelta(-keepAliveAdjustment)
                .reason(String.format(
                    "High load detected: %.2f%% (threshold: %.2f%%)",
                    currentLoad * 100,
                    highLoadThreshold * 100
                ))
                .build();
                
        } else if (currentLoad < lowLoadThreshold && stats.getMaxPoolSize() > stats.getPoolSize()) {
            // 低负载情况：缩容并增加空闲线程存活时间
            return builder
                .threadDelta(-scaleDownSize)
                .coreSizeDelta(-scaleDownSize)
                .keepAliveTimeDelta(keepAliveAdjustment)
                .reason(String.format(
                    "Low load detected: %.2f%% (threshold: %.2f%%)",
                    currentLoad * 100,
                    lowLoadThreshold * 100
                ))
                .build();
        }
        
        return null;  // 不需要调整时返回null
    }
}