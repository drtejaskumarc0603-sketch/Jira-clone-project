package com.example.jira.controller;

import com.example.jira.model.Notification;
import com.example.jira.repository.NotificationRepository;

import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(
            NotificationRepository notificationRepository) {

        this.notificationRepository =
                notificationRepository;
    }

    @GetMapping("/user/{userId}")
    public List<Notification> getUserNotifications(
            @PathVariable String userId) {

        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(
                        userId
                );
    }

    @GetMapping("/user/{userId}/unread")
    public List<Notification> getUnreadNotifications(
            @PathVariable String userId) {

        return notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(
                        userId
                );
    }

    @PutMapping("/{id}/read")
    public Notification markAsRead(
            @PathVariable String id) {

        if (!ObjectId.isValid(id)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Notification ID is invalid."
            );
        }

        Notification notification =
                notificationRepository
                        .findById(new ObjectId(id))
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Notification not found."
                                )
                        );

        notification.setRead(true);

        return notificationRepository.save(
                notification
        );
    }
}