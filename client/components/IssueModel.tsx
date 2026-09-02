"use client";

import React, { useEffect, useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "./ui/dialog";
import { Button } from "./ui/button";
import { Avatar, AvatarFallback, AvatarImage } from "./ui/avatar";
import { Textarea } from "./ui/textarea";
import { Badge } from "./ui/badge";

import axiosInstance from "@/lib/Axiosinstance";
import { useAuth } from "@/lib/AuthContext";

const priorityLabels: Record<string, string> = {
  HIGH: "High",
  MEDIUM: "Medium",
  LOW: "Low",
};

const typeIcons: Record<string, string> = {
  BUG: "🐛",
  TASK: "✓",
  STORY: "📖",
  SUBTASK: "↳",
};

const IssueModel = ({ issue, isOpen, onClose }: any) => {
  const { user } = useAuth();

  const [assignee, setAssignee] = useState<any>(null);
  const [commentText, setCommentText] = useState("");
  const [loading, setLoading] = useState(false);
  const [localIssue, setLocalIssue] = useState<any>(null);

  // Task 1: Subtasks
  const [subtasks, setSubtasks] = useState<any[]>([]);

  // Task 1: Dependencies
  const [dependencies, setDependencies] = useState<any[]>([]);
  const [projectIssues, setProjectIssues] = useState<any[]>([]);
  const [selectedDependency, setSelectedDependency] = useState("");

  /*
   * Load the selected issue into local state.
   */
  useEffect(() => {
    if (!isOpen || !issue?.id) return;

    setLocalIssue(issue);
  }, [isOpen, issue]);

  /*
   * Load assignee information.
   */
  useEffect(() => {
    if (!localIssue?.assigneeId) {
      setAssignee(null);
      return;
    }

    const fetchAssignee = async () => {
      try {
        const res = await axiosInstance.get(
          `/api/users/${localIssue.assigneeId}`
        );

        setAssignee(res.data);
      } catch (err) {
        console.error("Failed to load assignee", err);
        setAssignee(null);
      }
    };

    fetchAssignee();
  }, [localIssue?.assigneeId]);

  /*
   * Load subtasks and dependencies for the current issue.
   */
  const loadTaskRelationships = async () => {
    if (!localIssue?.id) return;

    try {
      const [subtasksResponse, dependenciesResponse] =
        await Promise.all([
          axiosInstance.get(
            `/api/issues/${localIssue.id}/subtasks`
          ),
          axiosInstance.get(
            `/api/issues/${localIssue.id}/dependencies`
          ),
        ]);

      setSubtasks(
        Array.isArray(subtasksResponse.data)
          ? subtasksResponse.data
          : []
      );

      setDependencies(
        Array.isArray(dependenciesResponse.data)
          ? dependenciesResponse.data
          : []
      );
    } catch (error) {
      console.error(
        "Failed to load task relationships:",
        error
      );

      setSubtasks([]);
      setDependencies([]);
    }
  };

  /*
   * Load all issues in the current project.
   *
   * These are used in the dependency dropdown.
   */
  const loadProjectIssues = async () => {
    if (!localIssue?.projectId) return;

    try {
      const response = await axiosInstance.get(
        `/api/issues/project/${localIssue.projectId}`
      );

      setProjectIssues(
        Array.isArray(response.data)
          ? response.data
          : []
      );
    } catch (error) {
      console.error(
        "Failed to load project issues:",
        error
      );

      setProjectIssues([]);
    }
  };

  /*
   * When the issue changes, load its relationships
   * and project issues.
   */
  useEffect(() => {
    if (!isOpen || !localIssue?.id) return;

    loadTaskRelationships();
    loadProjectIssues();
  }, [isOpen, localIssue?.id, localIssue?.projectId]);

  /*
   * Create a subtask.
   *
   * IMPORTANT:
   * We only send the fields that the user is allowed
   * to control here.
   *
   * The backend automatically inherits:
   * - projectId
   * - sprintId
   * - parentIssueId
   */
  const createSubtask = async () => {
    if (!localIssue?.id) return;

    const title = window.prompt(
      "Enter the subtask title:"
    );

    if (!title?.trim()) return;

    try {
      setLoading(true);

      await axiosInstance.post(
        `/api/issues/${localIssue.id}/subtasks`,
        {
          title: title.trim(),
          description: "",
          type: "SUBTASK",
          priority: "MEDIUM",
          status: "TODO",
          reporterId: user?.id ?? null,
          assigneeId: null,
          order: 0,
        }
      );

      await loadTaskRelationships();

      alert("Subtask created successfully.");
    } catch (error: any) {
      console.error(
        "Failed to create subtask:",
        error
      );

      const message =
        error?.response?.data?.message ||
        error?.response?.data ||
        "Failed to create subtask.";

      alert(message);
    } finally {
      setLoading(false);
    }
  };

  /*
   * Add a dependency.
   *
   * Example:
   *
   * Task B depends on Task A
   *
   * We call:
   * POST /api/issues/B/dependencies/A
   */
  const addDependency = async () => {
    if (
      !localIssue?.id ||
      !selectedDependency
    ) {
      return;
    }

    try {
      setLoading(true);

      await axiosInstance.post(
        `/api/issues/${localIssue.id}/dependencies/${selectedDependency}`
      );

      setSelectedDependency("");

      await loadTaskRelationships();

      alert("Dependency added successfully.");
    } catch (error: any) {
      console.error(
        "Failed to add dependency:",
        error
      );

      const message =
        error?.response?.data?.message ||
        error?.response?.data ||
        "Failed to add dependency.";

      alert(message);
    } finally {
      setLoading(false);
    }
  };

  /*
   * Remove an existing dependency.
   */
  const removeDependency = async (
    dependencyId: string
  ) => {
    if (!localIssue?.id) return;

    try {
      setLoading(true);

      await axiosInstance.delete(
        `/api/issues/${localIssue.id}/dependencies/${dependencyId}`
      );

      await loadTaskRelationships();

      alert("Dependency removed.");
    } catch (error: any) {
      console.error(
        "Failed to remove dependency:",
        error
      );

      const message =
        error?.response?.data?.message ||
        error?.response?.data ||
        "Failed to remove dependency.";

      alert(message);
    } finally {
      setLoading(false);
    }
  };

  /*
   * Save a comment.
   */
  const saveComment = async () => {
    if (
      !commentText.trim() ||
      !user ||
      !localIssue
    ) {
      return;
    }

    try {
      setLoading(true);

      const updatedComments = [
        ...(localIssue.comments || []),
        commentText.trim(),
      ];

      await axiosInstance.put(
        `/api/issues/${localIssue.id}`,
        {
          title: localIssue.title,
          description: localIssue.description,
          type: localIssue.type,
          priority: localIssue.priority,
          status: localIssue.status,
          projectId: localIssue.projectId,
          reporterId: localIssue.reporterId,
          assigneeId: localIssue.assigneeId,
          sprintId: localIssue.sprintId ?? null,
          parentIssueId:
            localIssue.parentIssueId ?? null,
          dependencyIds:
            localIssue.dependencyIds ?? [],
          order: localIssue.order ?? 0,
          comments: updatedComments,
          updatedAt: new Date().toISOString(),
        }
      );

      setLocalIssue((prev: any) => ({
        ...prev,
        comments: updatedComments,
      }));

      setCommentText("");
    } catch (err: any) {
      console.error(
        "Failed to save comment",
        err
      );

      alert(
        err?.response?.data?.message ||
          "Failed to save comment."
      );
    } finally {
      setLoading(false);
    }
  };

  /*
   * Don't allow a subtask to create another subtask.
   *
   * Parent tasks can have subtasks.
   * Subtasks cannot have subtasks.
   */
  const isSubtask = Boolean(
    localIssue?.parentIssueId
  );

  /*
   * Issues that can be selected as dependencies.
   *
   * We exclude:
   * - the current issue
   * - existing dependencies
   */
  const availableDependencies =
    projectIssues.filter((item) => {
      if (!item?.id) return false;

      if (item.id === localIssue?.id) {
        return false;
      }

      if (
        dependencies.some(
          (dependency) =>
            dependency?.id === item.id
        )
      ) {
        return false;
      }

      return true;
    });

  if (!isOpen) {
    return null;
  }

  return (
    <Dialog
      open={isOpen}
      onOpenChange={onClose}
    >
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto p-0 gap-0 border-none shadow-2xl">
        <DialogHeader className="p-4 border-b">
          <DialogTitle className="text-sm font-semibold text-[#5E6C84]">
            {localIssue?.title ??
              "Loading issue…"}
          </DialogTitle>
        </DialogHeader>

        {!localIssue ? (
          <div className="flex h-64 items-center justify-center text-sm text-[#6B778C]">
            Loading issue…
          </div>
        ) : (
          <div className="flex flex-col md:flex-row">

            {/* ========================= */}
            {/* MAIN CONTENT */}
            {/* ========================= */}

            <div className="flex-1 p-6">

              <h2 className="text-2xl font-semibold mb-4">
                {localIssue.title}
              </h2>

              <h3 className="text-sm font-semibold mb-2">
                Description
              </h3>

              <p className="text-sm text-[#42526E] mb-8">
                {localIssue.description ||
                  "No description"}
              </p>

              {/* ========================= */}
              {/* SUBTASKS */}
              {/* ========================= */}

              <div className="mt-6 mb-8">

                <div className="flex items-center justify-between mb-4">

                  <h3 className="text-sm font-semibold">
                    Subtasks
                  </h3>

                  {!isSubtask && (
                    <Button
                      type="button"
                      size="sm"
                      className="bg-[#0052CC] text-white"
                      onClick={createSubtask}
                      disabled={loading}
                    >
                      + Add Subtask
                    </Button>
                  )}

                </div>

                {subtasks.length === 0 ? (

                  <div className="rounded-md border border-dashed border-[#DFE1E6] p-4">
                    <p className="text-sm text-[#6B778C]">
                      No subtasks yet.
                    </p>
                  </div>

                ) : (

                  <div className="space-y-2">

                    {subtasks.map(
                      (subtask: any) => (

                        <div
                          key={subtask.id}
                          className="flex items-center justify-between rounded-md border border-[#DFE1E6] bg-white p-3"
                        >

                          <div className="min-w-0">

                            <div className="text-xs text-[#6B778C]">
                              {subtask.key ||
                                "SUBTASK"}
                            </div>

                            <div className="text-sm font-medium text-[#172B4D] truncate">
                              {subtask.title}
                            </div>

                          </div>

                          <Badge>
                            {subtask.status ||
                              "TODO"}
                          </Badge>

                        </div>
                      )
                    )}

                  </div>
                )}

              </div>

              {/* ========================= */}
              {/* DEPENDENCIES */}
              {/* ========================= */}

              <div className="mt-6 mb-8">

                <h3 className="text-sm font-semibold mb-4">
                  Dependencies
                </h3>

                {dependencies.length === 0 ? (

                  <p className="text-sm text-[#6B778C] mb-4">
                    This task has no blocking
                    dependencies.
                  </p>

                ) : (

                  <div className="space-y-2 mb-4">

                    {dependencies.map(
                      (dependency: any) => (

                        <div
                          key={dependency.id}
                          className="flex items-center justify-between rounded-md border border-[#DFE1E6] bg-[#FFF7E6] p-3"
                        >

                          <div className="min-w-0">

                            <div className="text-xs text-[#6B778C]">
                              BLOCKED BY
                            </div>

                            <div className="text-sm font-medium text-[#172B4D]">
                              {dependency.key ||
                                dependency.title ||
                                "Task"}
                            </div>

                            <div className="text-xs text-[#6B778C]">
                              Status:{" "}
                              {dependency.status ||
                                "UNKNOWN"}
                            </div>

                          </div>

                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() =>
                              removeDependency(
                                dependency.id
                              )
                            }
                            disabled={loading}
                          >
                            Remove
                          </Button>

                        </div>
                      )
                    )}

                  </div>
                )}

                <div className="flex gap-2">

                  <select
                    value={
                      selectedDependency
                    }
                    onChange={(event) =>
                      setSelectedDependency(
                        event.target.value
                      )
                    }
                    className="h-9 flex-1 rounded-md border border-[#DFE1E6] bg-white px-3 text-sm"
                    disabled={loading}
                  >

                    <option value="">
                      Select a blocking task
                    </option>

                    {availableDependencies.map(
                      (item: any) => (

                        <option
                          key={item.id}
                          value={item.id}
                        >
                          {item.key
                            ? `${item.key} - `
                            : ""}
                          {item.title}
                        </option>

                      )
                    )}

                  </select>

                  <Button
                    type="button"
                    size="sm"
                    className="bg-[#0052CC] text-white"
                    onClick={addDependency}
                    disabled={
                      loading ||
                      !selectedDependency
                    }
                  >
                    Add
                  </Button>

                </div>

              </div>

              {/* ========================= */}
              {/* COMMENTS */}
              {/* ========================= */}

              <h3 className="text-sm font-semibold mb-4">
                Comments (
                {localIssue.comments?.length ||
                  0}
                )
              </h3>

              <div className="space-y-4 mb-6">

                {localIssue.comments?.length >
                0 ? (

                  <div className="space-y-3">

                    {localIssue.comments.map(
                      (
                        comment: string,
                        index: number
                      ) => (

                        <div
                          key={index}
                          className="rounded-md border border-[#DFE1E6] bg-[#F4F5F7] p-3"
                        >

                          <p className="text-sm text-[#172B4D] whitespace-pre-wrap">
                            {comment}
                          </p>

                        </div>

                      )
                    )}

                  </div>

                ) : (

                  <p className="text-sm text-[#6B778C] italic">
                    No comments yet
                  </p>

                )}

              </div>

              {/* Add Comment */}

              <div className="flex gap-3">

                <Avatar className="h-8 w-8">

                  <AvatarImage
                    src={user?.avatar}
                  />

                  <AvatarFallback>
                    ME
                  </AvatarFallback>

                </Avatar>

                <div className="flex-1">

                  <Textarea
                    placeholder="Add a comment..."
                    value={commentText}
                    onChange={(e) =>
                      setCommentText(
                        e.target.value
                      )
                    }
                  />

                  <div className="flex justify-end mt-2">

                    <Button
                      size="sm"
                      className="bg-[#0052CC] text-white"
                      disabled={
                        !commentText.trim() ||
                        loading
                      }
                      onClick={saveComment}
                    >
                      Save
                    </Button>

                  </div>

                </div>

              </div>

            </div>

            {/* ========================= */}
            {/* SIDEBAR */}
            {/* ========================= */}

            <div className="w-full md:w-[280px] p-6 border-l">

              <div className="space-y-5">

                {/* Parent */}

                {isSubtask && (
                  <div>

                    <h3 className="text-xs font-bold uppercase mb-1">
                      Parent Task
                    </h3>

                    <span className="text-sm text-[#0052CC]">
                      {localIssue.parentIssueId}
                    </span>

                  </div>
                )}

                {/* Status */}

                <div>

                  <h3 className="text-xs font-bold uppercase mb-1">
                    Status
                  </h3>

                  <Badge>
                    {localIssue.status}
                  </Badge>

                </div>

                {/* Type */}

                <div>

                  <h3 className="text-xs font-bold uppercase mb-1">
                    Type
                  </h3>

                  <span>
                    {typeIcons[
                      localIssue.type
                    ] || "✓"}{" "}
                    {localIssue.type}
                  </span>

                </div>

                {/* Priority */}

                <div>

                  <h3 className="text-xs font-bold uppercase mb-1">
                    Priority
                  </h3>

                  <Badge>
                    {priorityLabels[
                      localIssue.priority
                    ] ||
                      localIssue.priority}
                  </Badge>

                </div>

                {/* Sprint */}

                <div>

                  <h3 className="text-xs font-bold uppercase mb-1">
                    Sprint
                  </h3>

                  <span className="text-sm">
                    {localIssue.sprintId ||
                      "No sprint"}
                  </span>

                </div>

                {/* Assignee */}

                <div>

                  <h3 className="text-xs font-bold uppercase mb-1">
                    Assignee
                  </h3>

                  {assignee ? (

                    <div className="flex items-center gap-2">

                      <Avatar className="h-6 w-6">

                        <AvatarImage
                          src={assignee.avatar}
                        />

                        <AvatarFallback>
                          {assignee.name?.[0] ||
                            "U"}
                        </AvatarFallback>

                      </Avatar>

                      <span className="text-sm">
                        {assignee.name}
                      </span>

                    </div>

                  ) : (

                    <span className="text-sm italic">
                      Unassigned
                    </span>

                  )}

                </div>

              </div>

            </div>

          </div>
        )}
      </DialogContent>
    </Dialog>
  );
};

export default IssueModel;