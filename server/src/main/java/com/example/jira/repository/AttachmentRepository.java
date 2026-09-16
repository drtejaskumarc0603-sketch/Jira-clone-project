package com.example.jira.repository;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.jira.model.Attachment;

public interface AttachmentRepository
        extends MongoRepository<Attachment, ObjectId> {

    List<Attachment> findByIssueIdOrderByUploadedAtDesc(
            String issueId);
}