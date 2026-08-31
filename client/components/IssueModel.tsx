"use client";

import React, { useEffect, useState } from "react";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "./ui/dialog";
import { Button } from "./ui/button";
import { MoreHorizontal, Trash2, ExternalLink } from "lucide-react";
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
};

const IssueModel = ({ issue, isOpen, onClose }: any) => {
  const { user } = useAuth();

  const [assignee, setAssignee] = useState<any>(null);
  const [commentText, setCommentText] = useState("");
  const [loading, setLoading] = useState(false);
  const [localIssue, setLocalIssue] = useState<any>(null);
  const [subtasks, setSubtasks] = useState<any[]>([]);
const [dependencies, setDependencies] = useState<any[]>([]);
const [showSubtaskForm, setShowSubtaskForm] = useState(false);
const [selectedDependency, setSelectedDependency] = useState("");
const [projectIssues, setProjectIssues] = useState<any[]>([]);


  useEffect(() => {
    if (!isOpen || !issue?.id) return;

    const fetchIssue = async () => {
      try {
        setLoading(true);
        setLocalIssue(issue);
      } catch (err) {
        console.error("Failed to load issue", err);
      } finally {
        setLoading(false);
      }
    };

    fetchIssue();
  }, [isOpen, issue?.id]);

  useEffect(() => {
    if (!localIssue?.assigneeId) {
      setAssignee(null);
      return;
    }

    const fetchAssignee = async () => {
      try {
        const res = await axiosInstance.get(
          `/api/users/${localIssue.assigneeId}`,
        );
        setAssignee(res.data);
      } catch (err) {
        console.error("Failed to load assignee", err);
      }
    };

    fetchAssignee();
  }, [localIssue?.assigneeId]);

  const saveComment = async () => {
    if (!commentText.trim() || !user || !localIssue) return;

    try {
      setLoading(true);

      const updatedComments = [
        ...(localIssue.comments || []),
        commentText, // ONLY STRING
      ];

      await axiosInstance.put(`/api/issues/${localIssue.id}`, {
        title: localIssue.title,
        description: localIssue.description,
        type: localIssue.type,
        priority: localIssue.priority,
        status: localIssue.status,
        projectId: localIssue.projectId,
        reporterId: localIssue.reporterId,
        assigneeId: localIssue.assigneeId,
        sprintId: localIssue.sprintId ?? null,
        order: localIssue.order ?? 0,
        comments: updatedComments,
        updatedAt: new Date().toISOString(),
      });

      setLocalIssue((prev: any) => ({
        ...prev,
        comments: updatedComments,
      }));

      setCommentText("");
    } catch (err) {
      console.error("Failed to save comment", err);
    } finally {
      setLoading(false);
    }

    const loadTaskRelationships = async () => {
  if (!issue?.id) return;

  try {
    const [subtasksResponse, dependenciesResponse] =
      await Promise.all([
        axiosInstance.get(
          `/api/issues/${issue.id}/subtasks`
        ),
        axiosInstance.get(
          `/api/issues/${issue.id}/dependencies`
        ),
      ]);

    setSubtasks(subtasksResponse.data);
    setDependencies(dependenciesResponse.data);

  } catch (error) {
    console.error(
      "Failed to load task relationships:",
      error
    );
  }
};

loadTaskRelationships();

const createSubtask = async () => {
  if (!issue?.id) return;

  const title = window.prompt(
    "Enter the subtask title:"
  );

  if (!title?.trim()) return;

  try {
    await axiosInstance.post(
      `/api/issues/${issue.id}/subtasks`,
      {
        title: title.trim(),
        description: "",
        priority: "MEDIUM",
        status: "TODO",
        reporterId: user?.id,
        assigneeId: null,
        order: 0,
      }
    );

    await loadTaskRelationships();

  } catch (error: any) {
    console.error(
      "Failed to create subtask:",
      error
    );

    alert(
      error?.response?.data?.message ||
      "Failed to create subtask."
    );
  }
};

const addDependency = async () => {
  if (
    !issue?.id ||
    !selectedDependency
  ) {
    return;
  }

  try {
    await axiosInstance.post(
      `/api/issues/${issue.id}/dependencies/${selectedDependency}`
    );

    setSelectedDependency("");

    await loadTaskRelationships();

  } catch (error: any) {
    console.error(
      "Failed to add dependency:",
      error
    );

    alert(
      error?.response?.data?.message ||
      "Failed to add dependency."
    );
  }
};
const removeDependency = async (
  dependencyId: string
) => {
  try {
    await axiosInstance.delete(
      `/api/issues/${issue.id}/dependencies/${dependencyId}`
    );

    await loadTaskRelationships();

  } catch (error: any) {
    console.error(
      "Failed to remove dependency:",
      error
    );

    alert(
      error?.response?.data?.message ||
      "Failed to remove dependency."
    );
  }
};

const loadProjectIssues = async () => {
  if (!issue?.projectId) return;

  try {
    const response =
      await axiosInstance.get(
        `/api/issues/project/${issue.projectId}`
      );

    setProjectIssues(response.data);

  } catch (error) {
    console.error(
      "Failed to load project issues:",
      error
    );
  }
};

loadProjectIssues();

  };

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto p-0 gap-0 border-none shadow-2xl">
        <DialogHeader className="p-4 border-b">
          <DialogTitle className="text-sm font-semibold text-[#5E6C84]">
            {localIssue?.title ?? "Loading issue…"}
          </DialogTitle>
        </DialogHeader>

        {!localIssue || loading ? (
          <div className="flex h-64 items-center justify-center text-sm text-[#6B778C]">
            Loading issue…
          </div>
        ) : (
          <div className="flex flex-col md:flex-row">
            {/* Main */}
            <div className="flex-1 p-6">
              <h2 className="text-2xl font-semibold mb-4">
                {localIssue.title}
              </h2>

              <h3 className="text-sm font-semibold mb-2">Description</h3>
              <p className="text-sm text-[#42526E] mb-8">
                {localIssue.description || "No description"}
              </p>

              <h3 className="text-sm font-semibold mb-4">
                Comments ({localIssue.comments?.length || 0})
              </h3>

              <div className="space-y-4 mb-6">
                {localIssue.comments?.length > 0 ? (
                  <div className="space-y-3">
                    {localIssue.comments.map(
                      (comment: string, index: number) => (
                        <div
                          key={index}
                          className="rounded-md border border-[#DFE1E6] bg-[#F4F5F7] p-3"
                        >
                          <p className="text-sm text-[#172B4D] whitespace-pre-wrap">
                            {comment}
                          </p>
                        </div>
                      ),
                    )}
                  </div>
                ) : (
                  <p className="text-sm text-[#6B778C] italic">
                    No comments yet
                  </p>
                )}
              </div>
              {/*substack*/}
              <div className="mt-6">

  <div className="mb-3 flex items-center justify-between">

    <h3 className="text-sm font-semibold">
      Subtasks
    </h3>

    {!issue?.parentIssueId && (
      <button
        type="button"
        onClick={createSubtask}
        className="rounded bg-[#0052CC] px-3 py-1.5 text-xs font-medium text-white hover:bg-[#0747A6]"
      >
        + Add Subtask
      </button>
    )}

  </div>

  {subtasks.length === 0 ? (

    <p className="text-sm text-gray-500">
      No subtasks.
    </p>

  ) : (

    <div className="space-y-2">

      {subtasks.map((subtask) => (

        <div
          key={subtask.id}
          className="flex items-center justify-between rounded border p-3"
        >

          <div>

            <div className="text-xs text-gray-500">
              {subtask.key}
            </div>

            <div className="text-sm font-medium">
              {subtask.title}
            </div>

          </div>

          <span className="rounded bg-gray-100 px-2 py-1 text-xs">
            {subtask.status}
          </span>

        </div>

      ))}

    </div>

  )}

</div>
{/*dependencies*/}

<div className="mt-6">

  <h3 className="mb-3 text-sm font-semibold">
    Dependencies
  </h3>

  {dependencies.length > 0 && (

    <div className="mb-3 space-y-2">

      {dependencies.map((dependency) => (

        <div
          key={dependency.id}
          className="flex items-center justify-between rounded border bg-yellow-50 p-3"
        >

          <div>

            <div className="text-xs text-gray-500">
              Blocks this task
            </div>

            <div className="text-sm font-medium">
              {dependency.key}
            </div>

            <div className="text-xs">
              Status: {dependency.status}
            </div>

          </div>

          <button
            type="button"
            onClick={() =>
              removeDependency(
                dependency.id
              )
            }
            className="text-xs text-red-600"
          >
            Remove
          </button>

        </div>

      ))}

    </div>

  )}

  <div className="flex gap-2">

    <select
      value={selectedDependency}
      onChange={(event) =>
        setSelectedDependency(
          event.target.value
        )
      }
      className="h-9 flex-1 rounded border px-2 text-sm"
    >

      <option value="">
        Select a blocking task
      </option>

      {projectIssues
        .filter(
          (item) =>
            item.id !== issue?.id &&
            !dependencies.some(
              (dependency) =>
                dependency.id === item.id
            )
        )
        .map((item) => (

          <option
            key={item.id}
            value={item.id}
          >
            {item.key} - {item.title}
          </option>

        ))}

    </select>

    <button
      type="button"
      onClick={addDependency}
      disabled={!selectedDependency}
      className="rounded bg-[#0052CC] px-3 text-xs font-medium text-white disabled:opacity-50"
    >
      Add
    </button>

  </div>

</div>

              {/* Add Comment */}
              <div className="flex gap-3">
                <Avatar className="h-8 w-8">
                  <AvatarImage src={user?.avatar} />
                  <AvatarFallback>ME</AvatarFallback>
                </Avatar>
                <div className="flex-1">
                  <Textarea
                    placeholder="Add a comment..."
                    value={commentText}
                    onChange={(e) => setCommentText(e.target.value)}
                  />
                  <div className="flex justify-end mt-2">
                    <Button
                      size="sm"
                      className="bg-[#0052CC] text-white"
                      disabled={!commentText || loading}
                      onClick={saveComment}
                    >
                      Save
                    </Button>
                  </div>
                </div>
              </div>
            </div>

            {/* Sidebar */}
            <div className="w-full md:w-[280px] p-6 border-l">
              <div className="space-y-5">
                <div>
                  <h3 className="text-xs font-bold uppercase mb-1">Status</h3>
                  <Badge>{localIssue.status}</Badge>
                </div>

                <div>
                  <h3 className="text-xs font-bold uppercase mb-1">Type</h3>
                  <span>
                    {typeIcons[localIssue.type]} {localIssue.type}
                  </span>
                </div>

                <div>
                  <h3 className="text-xs font-bold uppercase mb-1">Priority</h3>
                  <Badge>{priorityLabels[localIssue.priority]}</Badge>
                </div>

                <div>
                  <h3 className="text-xs font-bold uppercase mb-1">Assignee</h3>
                  {assignee ? (
                    <div className="flex items-center gap-2">
                      <Avatar className="h-6 w-6">
                        <AvatarImage src={assignee.avatar} />
                        <AvatarFallback>{assignee.name[0]}</AvatarFallback>
                      </Avatar>
                      <span className="text-sm">{assignee.name}</span>
                    </div>
                  ) : (
                    <span className="text-sm italic">Unassigned</span>
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
