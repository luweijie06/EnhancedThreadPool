package com.dev.gear.concurrent.config;

import com.dev.gear.concurrent.persistence.PersistenceStrategy;

/**
 * 线程池配置基类
 */
public class ThreadPoolConfig {
    private final MonitoringConfig monitoringConfig;
    private final AlertConfig alertConfig;
    private final ScalingConfig scalingConfig;
    private final boolean persistenceEnabled;
    private final PersistenceStrategy persistenceStrategy;

    private ThreadPoolConfig(Builder builder) {
        this.monitoringConfig = builder.monitoringConfig;
        this.alertConfig = builder.alertConfig;
        this.scalingConfig = builder.scalingConfig;
        this.persistenceEnabled = builder.persistenceEnabled;
        this.persistenceStrategy = builder.persistenceStrategy;
    }

    public MonitoringConfig getMonitoringConfig() {
        return monitoringConfig;
    }

    public AlertConfig getAlertConfig() {
        return alertConfig;
    }

    public ScalingConfig getScalingConfig() {
        return scalingConfig;
    }

    public boolean isPersistenceEnabled() {
        return persistenceEnabled;
    }

    public PersistenceStrategy getPersistenceStrategy() {
        return persistenceStrategy;
    }

    public static class Builder {
        private MonitoringConfig monitoringConfig = new MonitoringConfig.Builder().build();
        private AlertConfig alertConfig = new AlertConfig.Builder().build();
        private ScalingConfig scalingConfig = new ScalingConfig.Builder().build();
        private boolean persistenceEnabled = false;  // Default to false
        private PersistenceStrategy persistenceStrategy;

        public Builder monitoringConfig(MonitoringConfig config) {
            this.monitoringConfig = config;
            return this;
        }

        public Builder alertConfig(AlertConfig config) {
            this.alertConfig = config;
            return this;
        }

        public Builder scalingConfig(ScalingConfig config) {
            this.scalingConfig = config;
            return this;
        }

        public Builder enablePersistence(boolean enable) {
            this.persistenceEnabled = enable;
            return this;
        }

        public Builder persistenceStrategy(PersistenceStrategy strategy) {
            this.persistenceStrategy = strategy;
            return this;
        }

        public ThreadPoolConfig build() {
            if (persistenceEnabled && persistenceStrategy == null) {
                throw new IllegalStateException("Persistence strategy must be provided when persistence is enabled");
            }
            return new ThreadPoolConfig(this);
        }
    }
}

