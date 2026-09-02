package com.example.jira.repository;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.jira.model.AuditLog;

public interface AuditLogRepository
        extends MongoRepository<AuditLog, ObjectId> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType,
            String entityId
    );

    List<AuditLog> findByTaskIdOrderByCreatedAtDesc(
            String taskId
    );
}