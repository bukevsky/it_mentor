import type {
  ChatMessageResponse,
  ChatResponse,
  PagedResponse
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
  getMessagesCursor(chatId: number, params: { beforeMessageId: number; limit?: number }) {
    return request<ChatMessageResponse[]>(`/chats/${chatId}/messages/cursor${buildQuery(params)}`);
  },
  syncMessages(chatId: number, afterMessageId: number, limit = 100) {
    return request<ChatMessageResponse[]>(
      `/chats/${chatId}/messages/sync${buildQuery({ afterMessageId, limit })}`
    );
  },
};
