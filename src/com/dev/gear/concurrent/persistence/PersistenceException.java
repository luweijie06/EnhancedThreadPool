package com.dev.gear.concurrent.persistence;

/**
 * 持久化异常类
 */
class PersistenceException extends Exception {
    public PersistenceException(String message) {
        super(message);
    }

    public PersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}