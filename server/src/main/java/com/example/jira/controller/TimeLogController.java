package com.example.jira.controller;

import com.example.jira.model.TimeLog;
import com.example.jira.service.TimeLogService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/time-logs")
public class TimeLogController {

    private final TimeLogService timeLogService;

    public TimeLogController(
            TimeLogService timeLogService) {

        this.timeLogService = timeLogService;
    }

    // =========================================================
    // CREATE
    // =========================================================

    @PostMapping
    public TimeLog createTimeLog(
            @RequestBody TimeLog timeLog,
            @RequestParam String actorUserId) {

        return timeLogService.createTimeLog(
                timeLog,
                actorUserId
        );
    }

    // =========================================================
    // GET LOGS FOR TASK
    // =========================================================

    @GetMapping("/task/{taskId}")
    public List<TimeLog> getTimeLogsForTask(
            @PathVariable String taskId) {

        return timeLogService
                .getTimeLogsForTask(taskId);
    }

    // =========================================================
    // TOTAL FOR TASK
    // =========================================================

    @GetMapping("/task/{taskId}/total")
    public int getTaskTotal(
            @PathVariable String taskId) {

        return timeLogService
                .getTotalMinutesForTask(taskId);
    }

    // =========================================================
    // TOTAL FOR SPRINT
    // =========================================================

    @GetMapping("/sprint/{sprintId}/total")
    public int getSprintTotal(
            @PathVariable String sprintId) {

        return timeLogService
                .getTotalMinutesForSprint(sprintId);
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @PutMapping("/{id}")
    public TimeLog updateTimeLog(
            @PathVariable String id,
            @RequestBody TimeLog updated,
            @RequestParam String actorUserId) {

        return timeLogService.updateTimeLog(
                id,
                updated,
                actorUserId
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    @DeleteMapping("/{id}")
    public void deleteTimeLog(
            @PathVariable String id,
            @RequestParam String actorUserId) {

        timeLogService.deleteTimeLog(
                id,
                actorUserId
        );
    }
}