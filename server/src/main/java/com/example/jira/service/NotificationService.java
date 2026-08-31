package com.example.jira.service;

import com.example.jira.model.Issue;
import com.example.jira.model.Notification;
import com.example.jira.repository.NotificationRepository;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(
            NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
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

        // Prevent duplicate notifications
        if (notificationRepository.existsByEventKey(eventKey)) {
            return;
        }

        Notification notification = new Notification();

        notification.setUserId(userId);
        notification.setIssueId(blockedIssue.getId());
        notification.setType("DEPENDENCY_COMPLETED");

        notification.setMessage(
                "Task "
                        + completedDependency.getKey()
                        + " has been completed. "
                        + "It was blocking "
                        + blockedIssue.getKey()
                        + "."
        );

        notification.setEventKey(eventKey);

        notificationRepository.save(notification);
    }
}