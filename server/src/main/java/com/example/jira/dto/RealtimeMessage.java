package com.example.jira.dto;

public class RealtimeMessage {

    private String type;
    private String entityId;
    private String userId;
    private String message;

    public RealtimeMessage() {
    }

    public RealtimeMessage(
            String type,
            String entityId,
            String userId,
            String message) {

        this.type = type;
        this.entityId = entityId;
        this.userId = userId;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}