package com.example.jira.service;

import com.example.jira.model.Issue;
import com.example.jira.model.Sprint;
import com.example.jira.repository.IssueRepository;
import com.example.jira.repository.SprintRepository;
import com.example.jira.service.RealtimeService;

import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Service
public class IssueService {

    private final IssueRepository issueRepository;
    private final SprintRepository sprintRepository;
    private final NotificationService notificationService;
    private final RealtimeService realtimeService;

    public IssueService(
        IssueRepository issueRepository,
        SprintRepository sprintRepository,
        NotificationService notificationService,
        RealtimeService realtimeService) {

    this.issueRepository = issueRepository;
    this.sprintRepository = sprintRepository;
    this.notificationService = notificationService;
    this.realtimeService = realtimeService;
}

    // =========================================================
    // CREATE NORMAL ISSUE
    // =========================================================

    public Issue createIssue(Issue issue) {

        /*
         * A subtask MUST be created through the parent-specific
         * endpoint. This prevents orphan subtasks.
         */
        if (issue.getParentIssueId() != null
                || "SUBTASK".equalsIgnoreCase(issue.getType())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Subtasks must be created using a parent task."
            );
        }

        validateSprintBelongsToProject(
                issue.getSprintId(),
                issue.getProjectId()
        );

        issue.setParentIssueId(null);
        issue.setDependencyIds(new ArrayList<>());

        if (issue.getStatus() == null) {
            issue.setStatus("TODO");
        }

        if (issue.getComments() == null) {
            issue.setComments(new ArrayList<>());
        }

        return issueRepository.save(issue);
    }

    // =========================================================
    // CREATE SUBTASK
    // =========================================================

    public Issue createSubtask(
            String parentId,
            Issue subtask) {

        ObjectId parentObjectId =
                toObjectId(parentId, "Parent issue");

        Issue parent = issueRepository.findById(parentObjectId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Parent issue not found"
                        )
                );

        /*
         * Do not allow a subtask to become a parent of another
         * subtask. This keeps the hierarchy simple:
         *
         * Parent
         *   ├── Subtask
         *   ├── Subtask
         *   └── Subtask
         */
        if (parent.getParentIssueId() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A subtask cannot have its own subtasks."
            );
        }

        /*
         * A DONE parent cannot receive a new unfinished child.
         */
        if ("DONE".equalsIgnoreCase(parent.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A completed parent cannot receive a new subtask."
            );
        }

        /*
         * The server controls these fields.
         * The client cannot choose another project/sprint.
         */
        subtask.setType("SUBTASK");
        subtask.setParentIssueId(parent.getId());

        // INHERIT PROJECT
        subtask.setProjectId(parent.getProjectId());

        // INHERIT SPRINT
        subtask.setSprintId(parent.getSprintId());

        if (subtask.getStatus() == null) {
            subtask.setStatus("TODO");
        }

        subtask.setDependencyIds(new ArrayList<>());

        if (subtask.getComments() == null) {
            subtask.setComments(new ArrayList<>());
        }

        return issueRepository.save(subtask);
    }

    // =========================================================
    // UPDATE ISSUE
    // =========================================================

    public Issue updateIssue(
            String id,
            Issue updated) {

        Issue issue = findIssue(id);
        String oldAssigneeId = issue.getAssigneeId();

        String oldStatus = issue.getStatus();

int oldCommentCount =
        issue.getComments() != null
                ? issue.getComments().size()
                : 0;
       
      
        String newStatus =
                updated.getStatus() != null
                        ? updated.getStatus()
                        : issue.getStatus();

        /*
         * Check rules BEFORE changing the status.
         */
        validateStatusChange(issue, newStatus);

        issue.setTitle(updated.getTitle());
        issue.setDescription(updated.getDescription());
        issue.setStatus(newStatus);
        issue.setPriority(updated.getPriority());
        issue.setAssigneeId(updated.getAssigneeId());
        issue.setOrder(updated.getOrder());

        if (updated.getComments() != null) {
            issue.setComments(updated.getComments());
        }

        /*
         * IMPORTANT:
         *
         * We intentionally DO NOT copy:
         * projectId
         * sprintId
         * parentIssueId
         * dependencyIds
         *
         * from the request.
         *
         * This prevents the frontend from accidentally moving
         * a subtask to another project/sprint or changing its
         * parent through the normal update endpoint.
         */

        issue.setUpdatedAt(Instant.now());

       Issue savedIssue = issueRepository.save(issue);

       // Notify assignee when the issue is assigned
if (issue.getAssigneeId() != null
        && !issue.getAssigneeId().equals(oldAssigneeId)) {

    notificationService.notifyIssueAssigned(
            issue.getAssigneeId(),
            savedIssue
    );
}

// Notify assignee when status changes
if (!oldStatus.equalsIgnoreCase(newStatus)) {

    notificationService.notifyStatusChanged(
            savedIssue.getAssigneeId(),
            savedIssue,
            oldStatus,
            newStatus
    );
}

realtimeService.notifyIssueUpdated(
        savedIssue.getId(),
        savedIssue.getAssigneeId(),
        "Issue updated"
);



        /*
         * Notify users only when the issue actually changes
         * into DONE.
         */
        if (!"DONE".equalsIgnoreCase(oldStatus)
                && "DONE".equalsIgnoreCase(newStatus)) {

            notifyBlockedIssues(savedIssue);
        }

       return savedIssue;
    }
    

    // =========================================================
    // STATUS VALIDATION
    // =========================================================

    private void validateStatusChange(
            Issue issue,
            String newStatus) {

        if (newStatus == null) {
            return;
        }

        /*
         * Rule 1:
         *
         * A parent cannot become DONE until every subtask
         * is DONE.
         */
        if ("DONE".equalsIgnoreCase(newStatus)) {

            List<Issue> subtasks =
                    issueRepository.findByParentIssueId(issue.getId());

            List<Issue> incompleteSubtasks =
                    subtasks.stream()
                            .filter(subtask ->
                                    !"DONE".equalsIgnoreCase(
                                            subtask.getStatus()
                                    )
                            )
                            .toList();

            if (!incompleteSubtasks.isEmpty()) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Cannot complete parent task. "
                                + incompleteSubtasks.size()
                                + " subtask(s) are not completed."
                );
            }
        }

        /*
         * Rule 2:
         *
         * A task cannot START or become DONE while one of
         * its dependencies is incomplete.
         */
        boolean attemptingToStart =
                "IN_PROGRESS".equalsIgnoreCase(newStatus)
                        || "DONE".equalsIgnoreCase(newStatus);

        if (attemptingToStart) {

            List<Issue> dependencies =
                    loadDependencies(issue);

            List<Issue> blockingDependencies =
                    dependencies.stream()
                            .filter(dependency ->
                                    !"DONE".equalsIgnoreCase(
                                            dependency.getStatus()
                                    )
                            )
                            .toList();

            if (!blockingDependencies.isEmpty()) {

                String blockingKeys =
                        blockingDependencies.stream()
                                .map(Issue::getKey)
                                .reduce(
                                        (a, b) -> a + ", " + b
                                )
                                .orElse("another task");

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Task is blocked by: "
                                + blockingKeys
                );
            }
        }
    }

    // =========================================================
    // ADD DEPENDENCY
    // =========================================================

    public Issue addDependency(
            String issueId,
            String dependencyId) {

        Issue issue = findIssue(issueId);
        Issue dependency = findIssue(dependencyId);

        /*
         * A task cannot depend on itself.
         */
        if (issue.getId().equals(dependency.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A task cannot depend on itself."
            );
        }

        /*
         * Keep dependencies inside the same project.
         */
        if (issue.getProjectId() == null
                || !issue.getProjectId().equals(
                        dependency.getProjectId()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Dependencies must belong to the same project."
            );
        }

        /*
         * Don't create duplicates.
         */
        if (issue.getDependencyIds()
                .contains(dependency.getId())) {

            return issue;
        }

        /*
         * Check for circular dependency.
         *
         * Example:
         *
         * A depends on B
         * B depends on C
         *
         * Trying to make C depend on A
         * would create:
         *
         * A → B → C → A
         */
        if (wouldCreateCycle(
                issue.getId(),
                dependency.getId()
        )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot add dependency because it would create a circular dependency."
            );
        }

        issue.getDependencyIds()
                .add(dependency.getId());

        issue.setUpdatedAt(Instant.now());

        return issueRepository.save(issue);
    }

    // =========================================================
    // REMOVE DEPENDENCY
    // =========================================================

    public Issue removeDependency(
            String issueId,
            String dependencyId) {

        Issue issue = findIssue(issueId);

        issue.getDependencyIds()
                .remove(dependencyId);

        issue.setUpdatedAt(Instant.now());

        return issueRepository.save(issue);
    }

    // =========================================================
    // GET SUBTASKS
    // =========================================================

    public List<Issue> getSubtasks(String parentId) {

        findIssue(parentId);

        return issueRepository.findByParentIssueId(parentId);
    }

    // =========================================================
    // GET DEPENDENCIES
    // =========================================================

    public List<Issue> getDependencies(String issueId) {

        Issue issue = findIssue(issueId);

        return loadDependencies(issue);
    }

    // =========================================================
    // GET BLOCKED ISSUES
    // =========================================================

    public List<Issue> getBlockedIssues(String issueId) {

        findIssue(issueId);

        return issueRepository
                .findByDependencyIdsContaining(issueId);
    }

    // =========================================================
    // DELETE ISSUE
    // =========================================================

    public void deleteIssue(String id) {

        Issue issue = findIssue(id);

        /*
         * Don't allow deleting a parent while children exist.
         * Otherwise the subtasks would become orphaned.
         */
        List<Issue> subtasks =
                issueRepository.findByParentIssueId(id);

        if (!subtasks.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete a parent task while it has subtasks."
            );
        }

        /*
         * Remove this issue from other tasks' dependency lists.
         */
        List<Issue> dependents =
                issueRepository.findByDependencyIdsContaining(id);

        for (Issue dependent : dependents) {

            dependent.getDependencyIds()
                    .remove(id);

            dependent.setUpdatedAt(Instant.now());

            issueRepository.save(dependent);
        }

        issueRepository.deleteById(
                new ObjectId(id)
        );
    }

    // =========================================================
    // NOTIFY BLOCKED TASKS
    // =========================================================

    private void notifyBlockedIssues(
            Issue completedDependency) {

        List<Issue> blockedIssues =
                issueRepository.findByDependencyIdsContaining(
                        completedDependency.getId()
                );

        for (Issue blockedIssue : blockedIssues) {

            /*
             * Notify the assignee.
             *
             * If there is no assignee, notify the reporter.
             */
            String recipient =
                    blockedIssue.getAssigneeId();

            if (recipient == null
                    || recipient.isBlank()) {

                recipient =
                        blockedIssue.getReporterId();
            }

            try {

                notificationService
                        .notifyDependencyCompleted(
                                recipient,
                                blockedIssue,
                                completedDependency
                        );

            } catch (Exception exception) {

                /*
                 * Notification failure should NOT undo
                 * the successful task completion.
                 */
                System.err.println(
                        "Failed to create dependency notification: "
                                + exception.getMessage()
                );
            }
        }
    }

    // =========================================================
    // LOAD DEPENDENCIES
    // =========================================================

    private List<Issue> loadDependencies(
            Issue issue) {

        List<Issue> result = new ArrayList<>();

        for (String dependencyId :
                issue.getDependencyIds()) {

            if (!ObjectId.isValid(dependencyId)) {
                continue;
            }

            issueRepository
                    .findById(new ObjectId(dependencyId))
                    .ifPresent(result::add);
        }

        return result;
    }

    // =========================================================
    // CIRCULAR DEPENDENCY DETECTION
    // =========================================================

    private boolean wouldCreateCycle(
            String issueId,
            String dependencyId) {

        Set<String> visited = new HashSet<>();

        return reachesIssue(
                dependencyId,
                issueId,
                visited
        );
    }

    private boolean reachesIssue(
            String currentId,
            String targetId,
            Set<String> visited) {

        if (currentId.equals(targetId)) {
            return true;
        }

        if (!visited.add(currentId)) {
            return false;
        }

        if (!ObjectId.isValid(currentId)) {
            return false;
        }

        Issue current =
                issueRepository
                        .findById(new ObjectId(currentId))
                        .orElse(null);

        if (current == null) {
            return false;
        }

        for (String dependency :
                current.getDependencyIds()) {

            if (reachesIssue(
                    dependency,
                    targetId,
                    visited
            )) {
                return true;
            }
        }

        return false;
    }

    // =========================================================
    // HELPERS
    // =========================================================

    public Issue findIssue(String id) {

        ObjectId objectId =
                toObjectId(id, "Issue");

        return issueRepository
                .findById(objectId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Issue not found"
                        )
                );
    }

    private ObjectId toObjectId(
            String id,
            String label) {

        if (!ObjectId.isValid(id)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    label + " ID is invalid."
            );
        }

        return new ObjectId(id);
    }

    private void validateSprintBelongsToProject(
            String sprintId,
            String projectId) {

        if (sprintId == null || sprintId.isBlank()) {
            return;
        }

        if (!ObjectId.isValid(sprintId)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sprint ID is invalid."
            );
        }

        Sprint sprint =
                sprintRepository
                        .findById(new ObjectId(sprintId))
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Sprint not found."
                                )
                        );

        if (projectId != null
                && !projectId.equals(
                        sprint.getProjectId()
                )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Sprint does not belong to the issue's project."
            );
        }
    }
}