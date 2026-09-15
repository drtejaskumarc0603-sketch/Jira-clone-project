"use client";

import { useEffect, useState } from "react";
import axios from "@/lib/Axiosinstance";

interface User {
  id: string;
  name: string;
  email: string;
  role: string;
  group?: string;
  avatar?: string;
  active: boolean;
  emailVerified: boolean;
}

export default function ProfilePage() {
  const [user, setUser] = useState<User | null>(null);

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [group, setGroup] = useState("");

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");

  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const storedUser = localStorage.getItem("user");

    if (!storedUser) return;

    const parsedUser: User = JSON.parse(storedUser);

    setUser(parsedUser);
    setName(parsedUser.name || "");
    setEmail(parsedUser.email || "");
    setGroup(parsedUser.group || "");

    loadProfile(parsedUser.id);
  }, []);

  const loadProfile = async (userId: string) => {
    try {
      const response = await axios.get(`/api/users/${userId}`);

      setUser(response.data);
      setName(response.data.name || "");
      setEmail(response.data.email || "");
      setGroup(response.data.group || "");
    } catch {
      setError("Failed to load profile");
    }
  };

  const updateProfile = async () => {
    if (!user) return;

    setLoading(true);
    setMessage("");
    setError("");

    try {
      const response = await axios.put(`/api/users/${user.id}`, {
        name,
        email,
        group,
      });

      setUser(response.data);

      localStorage.setItem(
        "user",
        JSON.stringify(response.data)
      );

      if (email !== user.email) {
        setMessage(
          "Profile updated. Check the server/email for the verification link."
        );
      } else {
        setMessage("Profile updated successfully.");
      }
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          err?.response?.data ||
          "Failed to update profile"
      );
    } finally {
      setLoading(false);
    }
  };

  const changePassword = async () => {
    if (!user) return;

    setMessage("");
    setError("");

    try {
      await axios.put(`/api/users/${user.id}/password`, {
        currentPassword,
        newPassword,
      });

      setCurrentPassword("");
      setNewPassword("");

      setMessage("Password updated successfully.");
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          err?.response?.data ||
          "Failed to change password"
      );
    }
  };

  const uploadAvatar = async (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    if (!user || !event.target.files?.[0]) return;

    const file = event.target.files[0];

    const formData = new FormData();
    formData.append("file", file);

    setMessage("");
    setError("");

    try {
      const response = await axios.post(
  `/api/users/${user.id}/avatar`,
  formData
);

      setUser(response.data);

      localStorage.setItem(
        "user",
        JSON.stringify(response.data)
      );

      setMessage("Profile picture updated successfully.");
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          err?.response?.data ||
          "Failed to upload image"
      );
    }
  };

  const deactivateAccount = async () => {
    if (!user) return;

    const confirmed = window.confirm(
      "Are you sure you want to deactivate your account?"
    );

    if (!confirmed) return;

    try {
      await axios.put(
        `/api/users/${user.id}/deactivate`
      );

      setMessage(
        "Account deactivated. You will no longer be able to log in."
      );

      const updatedUser = {
        ...user,
        active: false,
      };

      setUser(updatedUser);

      localStorage.setItem(
        "user",
        JSON.stringify(updatedUser)
      );
    } catch (err: any) {
      setError(
        err?.response?.data?.message ||
          err?.response?.data ||
          "Failed to deactivate account"
      );
    }
  };

  if (!user) {
    return (
      <div className="p-8">
        <p>Loading profile...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="mx-auto max-w-3xl space-y-6">

        <h1 className="text-3xl font-bold">
          Profile & Security
        </h1>

        {message && (
          <div className="rounded-md bg-green-100 p-3 text-green-700">
            {message}
          </div>
        )}

        {error && (
          <div className="rounded-md bg-red-100 p-3 text-red-700">
            {error}
          </div>
        )}

        {/* Profile */}
        <div className="rounded-lg bg-white p-6 shadow">
          <h2 className="mb-4 text-xl font-semibold">
            Profile Information
          </h2>

          <div className="mb-5 flex items-center gap-5">
            {user.avatar ? (
              <img
                src={user.avatar}
                alt="Profile"
                className="h-24 w-24 rounded-full object-cover"
              />
            ) : (
              <div className="flex h-24 w-24 items-center justify-center rounded-full bg-gray-200 text-2xl">
                {user.name?.charAt(0)?.toUpperCase()}
              </div>
            )}

            <div>
              <label className="block text-sm font-medium">
                Profile picture
              </label>

              <input
                type="file"
                accept="image/jpeg,image/png,image/webp"
                onChange={uploadAvatar}
                className="mt-2"
              />

              <p className="mt-1 text-xs text-gray-500">
                JPG, PNG or WEBP. Maximum 5MB.
              </p>
            </div>
          </div>

          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium">
                Name
              </label>

              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="mt-1 w-full rounded-md border p-2"
              />
            </div>

            <div>
              <label className="text-sm font-medium">
                Email
              </label>

              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="mt-1 w-full rounded-md border p-2"
              />

              <p className="mt-1 text-xs text-gray-500">
                Changing email requires verification.
              </p>
            </div>

            <div>
              <label className="text-sm font-medium">
                Group
              </label>

              <input
                value={group}
                onChange={(e) => setGroup(e.target.value)}
                className="mt-1 w-full rounded-md border p-2"
              />
            </div>

            <div className="text-sm">
              <strong>Role:</strong> {user.role}
            </div>

            <div className="text-sm">
              <strong>Email status:</strong>{" "}
              {user.emailVerified
                ? "Verified"
                : "Not verified"}
            </div>

            <button
              onClick={updateProfile}
              disabled={loading}
              className="rounded-md bg-blue-600 px-5 py-2 text-white hover:bg-blue-700 disabled:opacity-50"
            >
              {loading ? "Saving..." : "Save Changes"}
            </button>
          </div>
        </div>

        {/* Password */}
        <div className="rounded-lg bg-white p-6 shadow">
          <h2 className="mb-4 text-xl font-semibold">
            Change Password
          </h2>

          <div className="space-y-4">
            <div>
              <label className="text-sm font-medium">
                Current Password
              </label>

              <input
                type="password"
                value={currentPassword}
                onChange={(e) =>
                  setCurrentPassword(e.target.value)
                }
                className="mt-1 w-full rounded-md border p-2"
              />
            </div>

            <div>
              <label className="text-sm font-medium">
                New Password
              </label>

              <input
                type="password"
                value={newPassword}
                onChange={(e) =>
                  setNewPassword(e.target.value)
                }
                className="mt-1 w-full rounded-md border p-2"
              />

              <p className="mt-1 text-xs text-gray-500">
                8–64 characters, uppercase, lowercase, number
                and special character.
              </p>
            </div>

            <button
              onClick={changePassword}
              className="rounded-md bg-blue-600 px-5 py-2 text-white hover:bg-blue-700"
            >
              Change Password
            </button>
          </div>
        </div>

        {/* Account */}
        <div className="rounded-lg border border-red-200 bg-white p-6 shadow">
          <h2 className="mb-2 text-xl font-semibold text-red-600">
            Account
          </h2>

          <p className="mb-4 text-sm text-gray-600">
            Deactivating your account prevents login access
            while preserving your historical data.
          </p>

          {user.active && (
            <button
              onClick={deactivateAccount}
              className="rounded-md bg-red-600 px-5 py-2 text-white hover:bg-red-700"
            >
              Deactivate Account
            </button>
          )}
        </div>

      </div>
    </div>
  );
}