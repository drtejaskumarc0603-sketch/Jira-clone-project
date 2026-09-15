package com.example.jira.repository;

import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.jira.model.EmailVerificationToken;

public interface EmailVerificationTokenRepository
        extends MongoRepository<EmailVerificationToken, ObjectId> {

    Optional<EmailVerificationToken> findByToken(String token);

    void deleteByUserId(String userId);
}