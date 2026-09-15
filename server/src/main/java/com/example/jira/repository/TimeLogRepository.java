package com.example.jira.repository;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.jira.model.TimeLog;

public interface TimeLogRepository
        extends MongoRepository<TimeLog, ObjectId> {

    List<TimeLog> findByTaskId(String taskId);

    List<TimeLog> findByUserId(String userId);

    List<TimeLog> findByTaskIdOrderByDateDesc(String taskId);
}