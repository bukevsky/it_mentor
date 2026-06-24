import type { Page } from "@playwright/test";

export const registerStompMock = async (page: Page) => {
  await page.addInitScript(() => {
    class MockWebSocket {
      static readonly CONNECTING = 0;
      static readonly OPEN = 1;
      static readonly CLOSING = 2;
      static readonly CLOSED = 3;

      readonly url: string;
      readonly protocol = "";
      readonly extensions = "";
      bufferedAmount = 0;
      binaryType: BinaryType = "blob";
      readyState = MockWebSocket.CONNECTING;
      onopen: ((event: Event) => void) | null = null;
      onmessage: ((event: MessageEvent) => void) | null = null;
      onclose: ((event: CloseEvent) => void) | null = null;
      onerror: ((event: Event) => void) | null = null;
      private readonly listeners = new Map<string, Set<EventListenerOrEventListenerObject>>();
      private subscriptionId = "sub-0";
      private messageId = 503;

      constructor(url: string | URL) {
        this.url = String(url);
        setTimeout(() => {
          this.readyState = MockWebSocket.OPEN;
          const event = new Event("open");
          this.onopen?.(event);
          this.dispatch("open", event);
        }, 0);
      }

      addEventListener(type: string, listener: EventListenerOrEventListenerObject) {
        const listeners = this.listeners.get(type) ?? new Set();
        listeners.add(listener);
        this.listeners.set(type, listeners);
      }

      removeEventListener(type: string, listener: EventListenerOrEventListenerObject) {
        this.listeners.get(type)?.delete(listener);
      }

      dispatchEvent(event: Event) {
        this.dispatch(event.type, event);
        return true;
      }

      send(data: string | ArrayBufferLike | Blob | ArrayBufferView) {
        if (typeof data !== "string") return;

        data
          .split("\0")
          .map((frame) => frame.trim())
          .filter(Boolean)
          .forEach((frame) => this.handleFrame(frame));
      }

      close(code = 1000, reason = "") {
        this.readyState = MockWebSocket.CLOSED;
        const event = new CloseEvent("close", { code, reason, wasClean: true });
        this.onclose?.(event);
        this.dispatch("close", event);
      }

      private dispatch(type: string, event: Event) {
        this.listeners.get(type)?.forEach((listener) => {
          if (typeof listener === "function") listener.call(this, event);
          else listener.handleEvent(event);
        });
      }

      private emitFrame(frame: string) {
        setTimeout(() => {
          const event = new MessageEvent("message", { data: `${frame}\0` });
          this.onmessage?.(event);
          this.dispatch("message", event);
        }, 0);
      }

      private handleFrame(frame: string) {
        const separator = frame.indexOf("\n\n");
        const head = separator >= 0 ? frame.slice(0, separator) : frame;
        const body = separator >= 0 ? frame.slice(separator + 2) : "";
        const lines = head.split("\n");
        const command = lines.shift();
        const headers = Object.fromEntries(
          lines.map((line) => {
            const index = line.indexOf(":");
            return [line.slice(0, index), line.slice(index + 1)];
          })
        );

        if (command === "CONNECT" || command === "STOMP") {
          this.emitFrame("CONNECTED\nversion:1.2\nheart-beat:0,0\n\n");
          return;
        }

        if (command === "SUBSCRIBE") {
          this.subscriptionId = headers.id ?? "sub-0";
          return;
        }

        if (command !== "SEND" || !headers.destination?.endsWith("/messages/send")) {
          return;
        }

        const payload = JSON.parse(body) as {
          requestId: string;
          body: string | null;
          attachmentFileId: number | null;
        };
        const occurredAt = "2026-04-07T10:10:00.000Z";
        const ack = {
          type: "chat.command.ack",
          requestId: payload.requestId,
          chatId: 401,
          occurredAt,
          payload: { command: "send", resourceId: this.messageId, duplicate: false }
        };
        const created = {
          type: "chat.message.created",
          requestId: payload.requestId,
          chatId: 401,
          occurredAt,
          payload: {
            id: this.messageId++,
            chatId: 401,
            senderUserId: 17,
            clientMessageId: payload.requestId,
            body: payload.body,
            attachment: null,
            deliveryStatus: "SENT",
            deliveredAt: null,
            readAt: null,
            createdAt: occurredAt
          }
        };

        [ack, created].forEach((event, index) => {
          const eventBody = JSON.stringify(event);
          const contentLength = new TextEncoder().encode(eventBody).length;
          this.emitFrame(
            `MESSAGE\nsubscription:${this.subscriptionId}\nmessage-id:mock-${index}\ndestination:/user/queue/chat-events\ncontent-type:application/json\ncontent-length:${contentLength}\n\n${eventBody}`
          );
        });
      }
    }

    window.WebSocket = MockWebSocket as unknown as typeof WebSocket;
  });
};
