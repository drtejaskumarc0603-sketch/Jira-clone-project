package com.example.jira.controller;

import com.example.jira.model.Issue;
import com.example.jira.repository.IssueRepository;
import com.example.jira.service.IssueService;

import org.bson.types.ObjectId;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/issues")
public class IssueController {

    private final IssueRepository issueRepository;
    private final IssueService issueService;

    public IssueController(
            IssueRepository issueRepository,
            IssueService issueService) {

        this.issueRepository = issueRepository;
        this.issueService = issueService;
    }

    // =========================================================
    // CREATE NORMAL ISSUE
    // =========================================================

    @PostMapping
    public Issue createIssue(
            @RequestBody Issue issue) {

        return issueService.createIssue(issue);
    }

    // =========================================================
    // CREATE SUBTASK
    // =========================================================

    @PostMapping("/{parentId}/subtasks")
    public Issue createSubtask(
            @PathVariable String parentId,
            @RequestBody Issue subtask) {

        return issueService.createSubtask(
                parentId,
                subtask
        );
    }

    // =========================================================
    // GET BY PROJECT
    // =========================================================

    @GetMapping("/project/{projectId}")
    public List<Issue> getIssuesByProject(
            @PathVariable String projectId) {

        return issueRepository.findByProjectId(
                projectId
        );
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    @GetMapping("/{id}")
    public Issue getIssueById(
            @PathVariable String id) {

        return issueService.findIssue(id);
    }

    // =========================================================
    // GET SUBTASKS
    // =========================================================

    @GetMapping("/{id}/subtasks")
    public List<Issue> getSubtasks(
            @PathVariable String id) {

        return issueService.getSubtasks(id);
    }

    // =========================================================
    // GET DEPENDENCIES
    // =========================================================

    @GetMapping("/{id}/dependencies")
    public List<Issue> getDependencies(
            @PathVariable String id) {

        return issueService.getDependencies(id);
    }

    // =========================================================
    // GET TASKS BLOCKED BY THIS ISSUE
    // =========================================================

    @GetMapping("/{id}/blocked-issues")
    public List<Issue> getBlockedIssues(
            @PathVariable String id) {

        return issueService.getBlockedIssues(id);
    }

    // =========================================================
    // ADD DEPENDENCY
    // =========================================================

    @PostMapping("/{issueId}/dependencies/{dependencyId}")
    public Issue addDependency(
            @PathVariable String issueId,
            @PathVariable String dependencyId) {

        return issueService.addDependency(
                issueId,
                dependencyId
        );
    }

    // =========================================================
    // REMOVE DEPENDENCY
    // =========================================================

    @DeleteMapping("/{issueId}/dependencies/{dependencyId}")
    public Issue removeDependency(
            @PathVariable String issueId,
            @PathVariable String dependencyId) {

        return issueService.removeDependency(
                issueId,
                dependencyId
        );
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @PutMapping("/{id}")
    public Issue updateIssue(
            @PathVariable String id,
            @RequestBody Issue updated) {

        return issueService.updateIssue(
                id,
                updated
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    @DeleteMapping("/{id}")
    public void deleteIssue(
            @PathVariable String id) {

        issueService.deleteIssue(id);
    }
}