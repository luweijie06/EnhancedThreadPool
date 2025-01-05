package com.dev.gear.concurrent.persistence;

import java.io.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * 文件系统持久化实现
 */
public class FileSystemPersistenceStrategy implements PersistenceStrategy {
    private final String filePath;
    private static final Logger logger = Logger.getLogger(FileSystemPersistenceStrategy.class.getName());

    public FileSystemPersistenceStrategy(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void save(Queue<SerializableTask> tasks) throws PersistenceException {
        try {
            File file = new File(filePath);
            
            // Create parent directories if they don't exist
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (!created) {
                    throw new PersistenceException("Failed to create parent directories for: " + filePath);
                }
            }

            try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
                out.writeObject(tasks);
            }
            logger.info("Successfully persisted " + tasks.size() + " tasks to file");
        } catch (IOException e) {
            throw new PersistenceException("Failed to persist tasks to file: " + e.getMessage(), e);
        }
    }


    @Override
    public Queue<SerializableTask> load() throws PersistenceException {
        File file = new File(filePath);
        if (!file.exists()) {
            return new LinkedList<>();
        }

        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            @SuppressWarnings("unchecked")
            Queue<SerializableTask> tasks = (Queue<SerializableTask>) in.readObject();
            logger.info("Successfully loaded " + tasks.size() + " tasks from file");
            return tasks;
        } catch (IOException | ClassNotFoundException e) {
            throw new PersistenceException("Failed to load tasks from file: " + e.getMessage(), e);
        }
    }

    @Override
    public void cleanup() throws PersistenceException {
        File file = new File(filePath);
        if (file.exists() && !file.delete()) {
            throw new PersistenceException("Failed to delete persistence file: " + filePath);
        }
    }
}