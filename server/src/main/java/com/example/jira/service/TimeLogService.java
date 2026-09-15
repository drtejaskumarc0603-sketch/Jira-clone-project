package com.example.jira.service;

import com.example.jira.model.AuditLog;
import com.example.jira.model.Issue;
import com.example.jira.model.Project;
import com.example.jira.model.TimeLog;
import com.example.jira.model.User;
import com.example.jira.repository.AuditLogRepository;
import com.example.jira.repository.IssueRepository;
import com.example.jira.repository.TimeLogRepository;
import com.example.jira.repository.UserRepository;

import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class TimeLogService {

    private final TimeLogRepository timeLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
   
    public TimeLogService(
            TimeLogRepository timeLogRepository,
            AuditLogRepository auditLogRepository,
            IssueRepository issueRepository,
            UserRepository userRepository
            ) {

        this.timeLogRepository = timeLogRepository;
        this.auditLogRepository = auditLogRepository;
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
      
    }

    // =========================================================
    // CREATE TIME LOG
    // =========================================================

    public TimeLog createTimeLog(
            TimeLog timeLog,
            String actorUserId) {

        Issue issue = findIssue(timeLog.getTaskId());

        findUser(actorUserId);

        validateTimeLog(timeLog);

        /*
         * A new log is created by the user who submitted it.
         */
        timeLog.setUserId(actorUserId);

        timeLog.setUpdatedAt(Instant.now());

        TimeLog saved =
                timeLogRepository.save(timeLog);

        /*
         * Creation is also recorded in the audit history.
         */
        createAuditLog(
                "CREATE",
                saved,
                actorUserId,
                "Created work log of "
                        + saved.getDurationMinutes()
                        + " minutes."
        );

        return saved;
    }

    // =========================================================
    // GET LOGS FOR TASK
    // =========================================================

    public List<TimeLog> getTimeLogsForTask(
            String taskId) {

        findIssue(taskId);

        return timeLogRepository
                .findByTaskIdOrderByDateDesc(taskId);
    }

    // =========================================================
    // GET TOTAL FOR TASK
    // =========================================================

    public int getTotalMinutesForTask(
            String taskId) {

        findIssue(taskId);

        return timeLogRepository
                .findByTaskId(taskId)
                .stream()
                .mapToInt(TimeLog::getDurationMinutes)
                .sum();
    }

    // =========================================================
    // UPDATE TIME LOG
    // =========================================================

    public TimeLog updateTimeLog(
            String id,
            TimeLog updated,
            String actorUserId) {

        TimeLog existing =
                findTimeLog(id);

        Issue issue =
                findIssue(existing.getTaskId());

        checkModificationPermission(
                issue,
                actorUserId
        );

        validateTimeLog(updated);

        int oldDuration =
                existing.getDurationMinutes();

        LocalDate oldDate =
                existing.getDate();

        String oldDescription =
                existing.getDescription();

        /*
         * We intentionally do not allow the task
         * to be changed during an edit.
         *
         * A work log belongs to its original task.
         */
        existing.setDate(updated.getDate());
        existing.setDurationMinutes(
                updated.getDurationMinutes()
        );
        existing.setDescription(
                updated.getDescription()
        );

        existing.setUpdatedAt(Instant.now());

        TimeLog saved =
                timeLogRepository.save(existing);

        createAuditLog(
                "UPDATE",
                saved,
                actorUserId,
                "Updated work log. "
                        + "Old date=" + oldDate
                        + ", new date=" + saved.getDate()
                        + "; old duration="
                        + oldDuration
                        + " minutes, new duration="
                        + saved.getDurationMinutes()
                        + " minutes."
                        + " Old description="
                        + oldDescription
                        + ", new description="
                        + saved.getDescription()
        );

        return saved;
    }

    // =========================================================
    // DELETE TIME LOG
    // =========================================================

    public void deleteTimeLog(
            String id,
            String actorUserId) {

        TimeLog existing =
                findTimeLog(id);

        Issue issue =
                findIssue(existing.getTaskId());

        checkModificationPermission(
                issue,
                actorUserId
        );

        /*
         * Create the audit entry BEFORE deleting the
         * actual work log so the historical information
         * is preserved.
         */
        createAuditLog(
                "DELETE",
                existing,
                actorUserId,
                "Deleted work log. "
                        + "Date=" + existing.getDate()
                        + ", duration="
                        + existing.getDurationMinutes()
                        + " minutes."
                        + " Description="
                        + existing.getDescription()
        );

        timeLogRepository.deleteById(
                existing.getObjectId()
        );
    }

    // =========================================================
    // TOTAL FOR SPRINT
    // =========================================================

    public int getTotalMinutesForSprint(
            String sprintId) {

        List<Issue> issues =
                issueRepository.findBySprintId(sprintId);

        int total = 0;

        for (Issue issue : issues) {

            total += timeLogRepository
                    .findByTaskId(issue.getId())
                    .stream()
                    .mapToInt(
                            TimeLog::getDurationMinutes
                    )
                    .sum();
        }

        return total;
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private void validateTimeLog(
            TimeLog timeLog) {

        if (timeLog.getDate() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Work date is required."
            );
        }

        /*
         * Future dates are not allowed.
         */
        if (timeLog.getDate()
                .isAfter(LocalDate.now())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Work date cannot be in the future."
            );
        }

        /*
         * Negative and zero durations are not useful
         * work logs.
         */
        if (timeLog.getDurationMinutes() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Duration must be greater than zero."
            );
        }

        if (timeLog.getDescription() == null
                || timeLog.getDescription().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Work description is required."
            );
        }
    }

    // =========================================================
    // PERMISSION
    // =========================================================

    private void checkModificationPermission(
            Issue issue,
            String actorUserId) {

        User actor =
                findUser(actorUserId);

        /*
         * Task assignee is allowed.
         */
        if (actorUserId.equals(
                issue.getAssigneeId())) {
            return;
        }

        /*
         * Project Manager is allowed.
         */
        if ("PROJECT_MANAGER".equalsIgnoreCase(
                actor.getRole())) {
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the task assignee or Project Manager "
                        + "can modify work logs."
        );
    }

    // =========================================================
    // AUDIT LOG
    // =========================================================

    private void createAuditLog(
            String action,
            TimeLog timeLog,
            String actorUserId,
            String details) {

        AuditLog audit =
                new AuditLog();

        audit.setEntityType("TIME_LOG");
        audit.setEntityId(timeLog.getId());
        audit.setAction(action);
        audit.setActorUserId(actorUserId);
        audit.setTaskId(timeLog.getTaskId());
        audit.setDetails(details);

        auditLogRepository.save(audit);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private TimeLog findTimeLog(
            String id) {

        if (!ObjectId.isValid(id)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid time log ID."
            );
        }

        return timeLogRepository
                .findById(new ObjectId(id))
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Time log not found."
                        )
                );
    }

    private Issue findIssue(
            String id) {

        if (id == null
                || !ObjectId.isValid(id)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid task ID."
            );
        }

        return issueRepository
                .findById(new ObjectId(id))
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Task not found."
                        )
                );
    }

    private User findUser(
            String id) {

        if (id == null
                || !ObjectId.isValid(id)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid user ID."
            );
        }

        return userRepository
                .findById(new ObjectId(id))
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found."
                        )
                );
    }
}