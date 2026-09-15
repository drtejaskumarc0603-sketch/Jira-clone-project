package com.example.jira.service;

import com.example.jira.model.Issue;
import com.example.jira.model.Notification;
import com.example.jira.model.User;
import com.example.jira.repository.NotificationRepository;
import com.example.jira.repository.UserRepository;

import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
   private final UserRepository userRepository;
    private final EmailService emailService;
    private final RealtimeService realtimeService;

    public NotificationService(
        NotificationRepository notificationRepository,
        UserRepository userRepository,
        EmailService emailService,
        RealtimeService realtimeService) {

    this.notificationRepository = notificationRepository;
    this.userRepository = userRepository;
    this.emailService = emailService;
    this.realtimeService = realtimeService;
}

    public void notifyDependencyCompleted(
            String userId,
            Issue blockedIssue,
            Issue completedDependency) {

        if (userId == null || userId.isBlank()) {
            return;
        }

        String eventKey =
                "DEPENDENCY_COMPLETED:"
                        + completedDependency.getId()
                        + ":"
                        + blockedIssue.getId();

        createNotification(
                userId,
                blockedIssue.getId(),
                "DEPENDENCY_COMPLETED",
                "Task " + completedDependency.getKey()
                        + " has been completed. It was blocking "
                        + blockedIssue.getKey() + ".",
                eventKey
        );
    }

    public void notifyIssueAssigned(
            String userId,
            Issue issue) {

        if (userId == null || userId.isBlank()) {
            return;
        }

        createNotification(
                userId,
                issue.getId(),
                "ISSUE_ASSIGNED",
                "You have been assigned to issue " + issue.getKey() + ".",
                "ISSUE_ASSIGNED:" + issue.getId() + ":" + userId
        );
    }

    public void notifyStatusChanged(
            String userId,
            Issue issue,
            String oldStatus,
            String newStatus) {

        if (userId == null || userId.isBlank()) {
            return;
        }

        createNotification(
                userId,
                issue.getId(),
                "STATUS_CHANGED",
                "Issue " + issue.getKey()
                        + " status changed from "
                        + oldStatus + " to " + newStatus + ".",
                "STATUS_CHANGED:"
                        + issue.getId()
                        + ":"
                        + oldStatus
                        + ":"
                        + newStatus
        );
    }

    public void notifyCommentAdded(
            String userId,
            Issue issue) {

        if (userId == null || userId.isBlank()) {
            return;
        }

        createNotification(
                userId,
                issue.getId(),
                "COMMENT_ADDED",
                "A new comment was added to issue "
                        + issue.getKey() + ".",
                "COMMENT_ADDED:"
                        + issue.getId()
                        + ":"
                        + System.currentTimeMillis()
        );
    }

    private void createNotification(
            String userId,
            String issueId,
            String type,
            String message,
            String eventKey) {

        if (notificationRepository.existsByEventKey(eventKey)) {
            return;
        }

        Notification notification = new Notification();

        notification.setUserId(userId);
        notification.setIssueId(issueId);
        notification.setType(type);
        notification.setMessage(message);
        notification.setEventKey(eventKey);

        notificationRepository.save(notification);

        realtimeService.notifyUser(
        userId,
        type,
        message
);

        sendEmail(userId, type, message);
    }

    private void sendEmail(
            String userId,
            String type,
            String message) {

        try {
            User user = userRepository.findById(
                    new org.bson.types.ObjectId(userId)
            ).orElse(null);

            if (user == null || user.getEmail() == null) {
                return;
            }

            emailService.sendNotificationEmail(
                    user.getEmail(),
                    "Jira Notification - " + type,
                    message
            );

        } catch (Exception e) {
            System.err.println(
                    "Failed to send notification email: "
                            + e.getMessage()
            );
        }
    }
}