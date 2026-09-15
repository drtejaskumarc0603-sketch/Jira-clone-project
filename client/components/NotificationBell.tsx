"use client";

import { useEffect, useState } from "react";
import axiosInstance from "@/lib/Axiosinstance";
import { useAuth } from "@/lib/AuthContext";
import {
  connectWebSocket,
  disconnectWebSocket,
} from "@/lib/websocket";

const NotificationBell = () => {
  const { user } = useAuth();

  const [notifications, setNotifications] = useState<any[]>([]);
  const [open, setOpen] = useState(false);

  const loadNotifications = async () => {
    if (!user?.id) return;

    try {
      const response = await axiosInstance.get(
        `/api/notifications/user/${user.id}`
      );

      setNotifications(response.data || []);
    } catch (error) {
      console.error("Failed to load notifications:", error);
    }
  };

  const markAsRead = async (id: string) => {
    try {
      await axiosInstance.put(
        `/api/notifications/${id}/read`
      );

      setNotifications((prev) =>
        prev.map((notification) =>
          notification.id === id
            ? { ...notification, read: true }
            : notification
        )
      );
    } catch (error) {
      console.error("Failed to mark notification as read:", error);
    }
  };

  useEffect(() => {
    if (!user?.id) return;

    loadNotifications();

    const client = connectWebSocket(() => {
      client.subscribe(
        `/topic/user/${user.id}`,
        (message) => {
          try {
            const notification = JSON.parse(message.body);

            setNotifications((prev) => [
              {
                id: Date.now().toString(),
                type: notification.type,
                message: notification.message,
                read: false,
              },
              ...prev,
            ]);
          } catch (error) {
            console.error(
              "Failed to process notification:",
              error
            );
          }
        }
      );
    });

    return () => {
      disconnectWebSocket();
    };
  }, [user?.id]);

  const unreadCount = notifications.filter(
    (notification) => !notification.read
  ).length;

  return (
    <div className="relative">
      <button
        onClick={() => setOpen(!open)}
        className="relative rounded-full p-2 hover:bg-gray-100"
      >
        🔔

        {unreadCount > 0 && (
          <span className="absolute -right-1 -top-1 rounded-full bg-red-500 px-1.5 text-xs text-white">
            {unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 z-50 mt-2 w-80 rounded-lg border bg-white shadow-lg">
          <div className="border-b p-3 font-semibold">
            Notifications
          </div>

          {notifications.length === 0 ? (
            <div className="p-4 text-sm text-gray-500">
              No notifications
            </div>
          ) : (
            <div className="max-h-96 overflow-y-auto">
              {notifications.map((notification) => (
                <button
                  key={notification.id}
                  onClick={() => markAsRead(notification.id)}
                  className={`w-full border-b p-3 text-left hover:bg-gray-50 ${
                    !notification.read ? "bg-blue-50" : ""
                  }`}
                >
                  <p className="text-sm font-medium">
                    {notification.type}
                  </p>

                  <p className="text-sm text-gray-600">
                    {notification.message}
                  </p>

                  {!notification.read && (
                    <span className="text-xs text-blue-600">
                      Unread
                    </span>
                  )}
                </button>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default NotificationBell;