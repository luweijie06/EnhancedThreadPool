package com.dev.gear.concurrent.scaling;

import com.dev.gear.concurrent.monitor.ThreadPoolStats;

import java.util.List;
import java.util.ArrayList;

public class CompositeScalingStrategy implements ScalingStrategy {
    private final List<ScalingStrategy> strategies;
    
    public CompositeScalingStrategy(List<ScalingStrategy> strategies) {
        this.strategies = strategies;
    }
    
    @Override
    public ScalingCommand calculateScaling(ThreadPoolStats stats) {
        List<ScalingCommand> commands = new ArrayList<>();
        StringBuilder reasons = new StringBuilder();
        
        // 收集所有策略的扩缩容命令
        for (ScalingStrategy strategy : strategies) {
            ScalingCommand command = strategy.calculateScaling(stats);
            if (command != null) {
                commands.add(command);
                if (reasons.length() > 0) {
                    reasons.append(" + ");
                }
                reasons.append(command.getReason());
            }
        }
        
        if (commands.isEmpty()) {
            return null;
        }
        
        // 合并所有命令
        return mergeCommands(commands, reasons.toString());
    }
    
    private ScalingCommand mergeCommands(List<ScalingCommand> commands, String combinedReason) {
        ScalingCommand.Builder builder = new ScalingCommand.Builder();
        
        // 累加所有调整量
        int threadDelta = 0;
        int coreSizeDelta = 0;
        int maxSizeDelta = 0;
        int queueCapacityDelta = 0;
        long keepAliveTimeDelta = 0;
        
        for (ScalingCommand cmd : commands) {
            threadDelta += cmd.getThreadDelta();
            coreSizeDelta += cmd.getCoreSizeDelta();
            maxSizeDelta += cmd.getMaxSizeDelta();
            queueCapacityDelta += cmd.getQueueCapacityDelta();
            keepAliveTimeDelta += cmd.getKeepAliveTimeDelta();
        }
        
        // 构建合并后的命令
        return builder
            .threadDelta(threadDelta)
            .coreSizeDelta(coreSizeDelta)
            .maxSizeDelta(maxSizeDelta)
            .queueCapacityDelta(queueCapacityDelta)
            .keepAliveTimeDelta(keepAliveTimeDelta)
            .reason("Combined: " + combinedReason)
            .build();
    }
}