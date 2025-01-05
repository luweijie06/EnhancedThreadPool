package com.dev.gear.concurrent.persistence;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * 数据库持久化实现
 */
public class DatabasePersistenceStrategy implements PersistenceStrategy {
    private final DataSource dataSource;
    private static final Logger logger = Logger.getLogger(DatabasePersistenceStrategy.class.getName());

    public DatabasePersistenceStrategy(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(Queue<SerializableTask> tasks) throws PersistenceException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 清除现有任务
                try (PreparedStatement clearStmt = conn.prepareStatement(
                        "DELETE FROM persistent_tasks")) {
                    clearStmt.executeUpdate();
                }

                // 插入新任务
                try (PreparedStatement insertStmt = conn.prepareStatement(
                        "INSERT INTO persistent_tasks (task_id, submit_time, priority, serialized_task) VALUES (?, ?, ?, ?)")) {
                    for (SerializableTask task : tasks) {
                        insertStmt.setString(1, task.getTaskId());
                        insertStmt.setLong(2, task.getSubmitTime());
                        insertStmt.setInt(3, task.getPriority());
                        insertStmt.setBytes(4, task.getSerializedTaskBytes());
                        insertStmt.addBatch();
                    }
                    insertStmt.executeBatch();
                }
                conn.commit();
                logger.info("Successfully persisted " + tasks.size() + " tasks to database");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenceException("Failed to persist tasks to database: " + e.getMessage(), e);
        }
    }



    @Override
    public Queue<SerializableTask> load() throws PersistenceException {
        Queue<SerializableTask> tasks = new LinkedList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT task_id, submit_time, priority, serialized_task FROM persistent_tasks ORDER BY priority, submit_time");
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String taskId = rs.getString("task_id");
                long submitTime = rs.getLong("submit_time");
                int priority = rs.getInt("priority");
                byte[] serializedTask = rs.getBytes("serialized_task");

                tasks.offer(new SerializableTask(taskId, submitTime, priority, serializedTask));
            }
            logger.info("Successfully loaded " + tasks.size() + " tasks from database");
            return tasks;
        } catch (SQLException e) {
            throw new PersistenceException("Failed to load tasks from database: " + e.getMessage(), e);
        }
    }

    @Override
    public void cleanup() throws PersistenceException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM persistent_tasks")) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("Failed to cleanup persisted tasks: " + e.getMessage(), e);
        }
    }
}