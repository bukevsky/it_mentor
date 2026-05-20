import type {
  ChatMessageResponse,
  ChatResponse,
  PagedResponse,
  SendMessageRequest
} from "@/shared/api/contracts";
import { request } from "@/shared/api/http";
import { buildQuery } from "@/shared/api/query";

export const chatApi = {
  getChats(params: { page?: number; size?: number }) {
    return request<PagedResponse<ChatResponse>>(`/chats${buildQuery(params)}`);
  },
  getById(chatId: number) {
    return request<ChatResponse>(`/chats/${chatId}`);
  },
  getByRequestId(requestId: number) {
    return request<ChatResponse>(`/chats/by-request/${requestId}`);
  },
  getMessages(chatId: number, params: { page?: number; size?: number }) {
    return request<PagedResponse<ChatMessageResponse>>(
      `/chats/${chatId}/messages${buildQuery(params)}`
    );
  },
  getMessagesCursor(chatId: number, params: { beforeMessageId?: number; limit?: number } = {}) {
    return request<ChatMessageResponse[]>(`/chats/${chatId}/messages/cursor${buildQuery(params)}`);
  },
  sendMessage(chatId: number, payload: SendMessageRequest) {
    return request<ChatMessageResponse>(`/chats/${chatId}/messages`, {
      method: "POST",
      body: payload
    });
  },
  markAsRead(chatId: number) {
    return request<void>(`/chats/${chatId}/read`, {
      method: "POST"
    });
  },
  sendTyping(chatId: number, typing: boolean) {
    return request<void>(`/chats/${chatId}/typing`, {
      method: "POST",
      body: { typing }
    });
  }
};
