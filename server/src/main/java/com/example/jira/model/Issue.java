package com.example.jira.model;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "issues")
public class Issue {

    @Id
    private ObjectId id;

    private String key;
    private String title;
    private String description;
    private String type;
    private String status;
    private String priority;

    private String projectId;
    private String sprintId;

    private String reporterId;
    private String assigneeId;

    /*
     * If this is a subtask, this contains the parent's issue ID.
     * Normal issues have null parentIssueId.
     */
    private String parentIssueId;

    /*
     * Issues that must be completed before THIS issue can start.
     *
     * Example:
     * Task B dependencyIds = [Task A ID]
     *
     * This means A blocks B.
     */
    private List<String> dependencyIds = new ArrayList<>();

    private int order;

    private List<String> comments;

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

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getSprintId() {
        return sprintId;
    }

    public void setSprintId(String sprintId) {
        this.sprintId = sprintId;
    }

    public String getReporterId() {
        return reporterId;
    }

    public void setReporterId(String reporterId) {
        this.reporterId = reporterId;
    }

    public String getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(String assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getParentIssueId() {
        return parentIssueId;
    }

    public void setParentIssueId(String parentIssueId) {
        this.parentIssueId = parentIssueId;
    }

    public List<String> getDependencyIds() {
        if (dependencyIds == null) {
            dependencyIds = new ArrayList<>();
        }
        return dependencyIds;
    }

    public void setDependencyIds(List<String> dependencyIds) {
        this.dependencyIds =
                dependencyIds != null
                        ? dependencyIds
                        : new ArrayList<>();
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public List<String> getComments() {
        return comments;
    }

    public void setComments(List<String> comments) {
        this.comments = comments;
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