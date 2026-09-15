package com.example.jira.service;

import com.example.jira.dto.RealtimeMessage;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RealtimeService {

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeService(
            SimpMessagingTemplate messagingTemplate) {

        this.messagingTemplate = messagingTemplate;
    }

    public void notifyUser(
        String userId,
        String type,
        String message) {

    RealtimeMessage realtimeMessage =
            new RealtimeMessage(
                    type,
                    null,
                    userId,
                    message
            );

    messagingTemplate.convertAndSend(
            "/topic/user/" + userId,
            realtimeMessage
    );
}

    public void notifyIssueUpdated(
            String issueId,
            String userId,
            String message) {

        RealtimeMessage realtimeMessage =
                new RealtimeMessage(
                        "ISSUE_UPDATED",
                        issueId,
                        userId,
                        message
                );

        messagingTemplate.convertAndSend(
                "/topic/issue/" + issueId,
                realtimeMessage
        );
    }

    public void notifyCommentAdded(
            String issueId,
            String userId,
            String message) {

        RealtimeMessage realtimeMessage =
                new RealtimeMessage(
                        "COMMENT_ADDED",
                        issueId,
                        userId,
                        message
                );

        messagingTemplate.convertAndSend(
                "/topic/issue/" + issueId,
                realtimeMessage
        );
    }

    public void notifyStatusChanged(
            String issueId,
            String userId,
            String status) {

        RealtimeMessage realtimeMessage =
                new RealtimeMessage(
                        "STATUS_CHANGED",
                        issueId,
                        userId,
                        status
                );

        messagingTemplate.convertAndSend(
                "/topic/issue/" + issueId,
                realtimeMessage
        );
    }
}