package com.dev.gear.concurrent.config;

import com.dev.gear.concurrent.scaling.ScalingStrategy;

public class ScalingConfig {
    private final ScalingStrategy scalingStrategy;
    private final long scalingCheckPeriodMs;
    private final int minThreads;
    private final int maxThreads;
    
    private ScalingConfig(Builder builder) {
        this.scalingStrategy = builder.scalingStrategy;
        this.scalingCheckPeriodMs = builder.scalingCheckPeriodMs;
        this.minThreads = builder.minThreads;
        this.maxThreads = builder.maxThreads;
    }
    
    public static class Builder {
        private ScalingStrategy scalingStrategy;
        private long scalingCheckPeriodMs = 30000; // 默认30秒
        private int minThreads = 1;
        private int maxThreads = Runtime.getRuntime().availableProcessors() * 2;
        
        public Builder scalingStrategy(ScalingStrategy strategy) {
            this.scalingStrategy = strategy;
            return this;
        }
        
        public Builder scalingCheckPeriod(long periodMs) {
            this.scalingCheckPeriodMs = periodMs;
            return this;
        }
        
        public Builder minThreads(int min) {
            this.minThreads = min;
            return this;
        }
        
        public Builder maxThreads(int max) {
            this.maxThreads = max;
            return this;
        }
        
        public ScalingConfig build() {
            return new ScalingConfig(this);
        }
    }
    
    // Getters
    public ScalingStrategy getScalingStrategy() { return scalingStrategy; }
    public long getScalingCheckPeriodMs() { return scalingCheckPeriodMs; }
    public int getMinThreads() { return minThreads; }
    public int getMaxThreads() { return maxThreads; }
}
