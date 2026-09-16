"use client";

import { useEffect, useRef, useState } from "react";
import axiosInstance from "@/lib/Axiosinstance";
import { useAuth } from "@/lib/AuthContext";

interface Attachment {
  id: string;
  issueId: string;
  originalFilename: string;
  contentType: string;
  size: number;
  uploadedBy: string;
  uploadedAt: string;
}

interface AttachmentsProps {
  issueId: string;
}

const Attachments = ({ issueId }: AttachmentsProps) => {
  const { user } = useAuth();

  const [attachments, setAttachments] = useState<Attachment[]>([]);
  const [uploading, setUploading] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const loadAttachments = async () => {
    try {
      const response = await axiosInstance.get(
        `/api/attachments/issue/${issueId}`
      );

      setAttachments(response.data || []);
    } catch (error) {
      console.error("Failed to load attachments:", error);
    }
  };

  useEffect(() => {
    if (issueId) {
      loadAttachments();
    }
  }, [issueId]);

  const uploadFile = async (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    const file = event.target.files?.[0];

    if (!file || !user?.id) return;

    if (file.size > 10 * 1024 * 1024) {
      alert("File size cannot exceed 10 MB");
      return;
    }

    const allowedTypes = [
      "image/jpeg",
      "image/png",
      "image/webp",
      "application/pdf",
      "text/plain",
      "application/zip",
    ];

    if (!allowedTypes.includes(file.type)) {
      alert("Unsupported file type");
      return;
    }

    const formData = new FormData();

    formData.append("issueId", issueId);
    formData.append("userId", user.id);
    formData.append("file", file);

    try {
      setUploading(true);

      const response = await axiosInstance.post(
        "/api/attachments/upload",
        formData
      );

      setAttachments((prev) => [
        response.data,
        ...prev,
      ]);
    } catch (error) {
      console.error("Upload failed:", error);
      alert("Failed to upload file");
    } finally {
      setUploading(false);

      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
    }
  };

 const downloadFile = async (attachment: Attachment) => {
    
  try {
    const baseURL =
      process.env.API_BASE_URL || "http://localhost:8080";

   const url =
  `${baseURL}/api/attachments/file/${attachment.fileId}/download`;






    const response = await axiosInstance.get(url, {
      responseType: "blob",
    });

    const blob = new Blob([response.data], {
      type: attachment.contentType,
    });

    const downloadUrl = window.URL.createObjectURL(blob);

    const link = document.createElement("a");
    link.href = downloadUrl;
    link.download = attachment.originalFilename;
    document.body.appendChild(link);
    link.click();

    link.remove();
    window.URL.revokeObjectURL(downloadUrl);
  } catch (error) {
    console.error("Failed to download attachment:", error);
    alert("Failed to download attachment");
  }
};

     

     

  const deleteFile = async (id: string) => {
    if (!confirm("Delete this attachment?")) {
      return;
    }

    try {
      await axiosInstance.delete(
        `/api/attachments/${id}`
      );

      setAttachments((prev) =>
        prev.filter((attachment) => attachment.id !== id)
      );
    } catch (error) {
      console.error("Delete failed:", error);
      alert("Failed to delete attachment");
    }
  };

  const formatSize = (size: number) => {
    if (size < 1024) {
      return `${size} B`;
    }

    if (size < 1024 * 1024) {
      return `${(size / 1024).toFixed(1)} KB`;
    }

    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="mt-6 rounded-lg border p-4">
      <div className="mb-4 flex items-center justify-between">
        <h3 className="text-lg font-semibold">
          Attachments
        </h3>

        <button
          type="button"
          onClick={() => fileInputRef.current?.click()}
          disabled={uploading}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {uploading ? "Uploading..." : "Attach File"}
        </button>

        <input
          ref={fileInputRef}
          type="file"
          className="hidden"
          accept=".jpg,.jpeg,.png,.webp,.pdf,.txt,.zip"
          onChange={uploadFile}
        />
      </div>

      {attachments.length === 0 ? (
        <p className="text-sm text-gray-500">
          No attachments
        </p>
      ) : (
        <div className="space-y-2">
          {attachments.map((attachment) => (
            <div
              key={attachment.id}
              className="flex items-center justify-between rounded-md border p-3"
            >
              <div>
                <p className="font-medium">
                  {attachment.originalFilename}
                </p>

                <p className="text-xs text-gray-500">
                  {formatSize(attachment.size)}
                </p>
              </div>

              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() =>
                    downloadFile(attachment)
                  }
                  className="rounded-md border px-3 py-1 text-sm hover:bg-gray-100"
                >
                  Download
                </button>

                <button
                  type="button"
                  onClick={() =>
                    deleteFile(attachment.id)
                  }
                  className="rounded-md border px-3 py-1 text-sm text-red-600 hover:bg-red-50"
                >
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default Attachments;