import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

let client: Client | null = null;

export const connectWebSocket = (
  onConnect?: () => void
) => {
  client = new Client({
    webSocketFactory: () =>
      new SockJS("http://localhost:8080/ws"),

    reconnectDelay: 5000,

    onConnect: () => {
      console.log("WebSocket connected");
      onConnect?.();
    },

    onDisconnect: () => {
      console.log("WebSocket disconnected");
    },

    onStompError: (frame) => {
      console.error(
        "WebSocket error:",
        frame.headers["message"]
      );
    },
  });

  client.activate();

  return client;
};

export const subscribeToIssue = (
  issueId: string,
  callback: (message: any) => void
) => {
  if (!client || !client.connected) {
    console.warn("WebSocket is not connected");
    return;
  }

  return client.subscribe(
    `/topic/issue/${issueId}`,
    (message) => {
      callback(JSON.parse(message.body));
    }
  );
};

export const disconnectWebSocket = () => {
  if (client) {
    client.deactivate();
    client = null;
  }
};