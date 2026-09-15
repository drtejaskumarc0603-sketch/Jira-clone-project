package com.example.jira.model;

import java.time.Instant;
import java.time.LocalDate;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "time_logs")
public class TimeLog {

    @Id
    private ObjectId id;

    /*
     * The Jira issue/task this work was performed on.
     */
    private String taskId;

    /*
     * The user who created this work log.
     */
    private String userId;

    /*
     * Date on which the work was performed.
     */
    private LocalDate date;

    /*
     * Duration stored as minutes.
     *
     * Example:
     * 30  = 30 minutes
     * 60  = 1 hour
     * 90  = 1 hour 30 minutes
     */
    private int durationMinutes;

    /*
     * Description of the work performed.
     */
    private String description;

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    public String getId() {
        return id != null ? id.toHexString() : null;
    }

    public ObjectId getObjectId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}