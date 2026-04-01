<script setup lang="ts">
import { reactive, ref } from "vue";
import type {
  ChatMessageResponse,
  ChatResponse,
  ErrorResponse,
  PagedResponse,
  SendMessageRequest
} from "@/shared/api/contracts";
import { normalizeErrorResponse } from "@/shared/lib/api-errors";
import { formatJson, parseJsonInput, parseOptionalNumber } from "@/shared/lib/json";
import JsonPreview from "@/shared/ui/JsonPreview.vue";
import SectionCard from "@/shared/ui/SectionCard.vue";
import { chatApi } from "../api/chat-api";

const chatId = ref("");
const requestId = ref("");
const listParams = reactive({
  page: "0",
  size: "20"
});
const messageDraft = ref(
  formatJson<SendMessageRequest>({
    body: "Привет! Как дела с домашним заданием?",
    attachmentFileId: null
  })
);

const chatsResponse = ref<PagedResponse<ChatResponse>>();
const chatResponse = ref<ChatResponse>();
const messagesResponse = ref<PagedResponse<ChatMessageResponse>>();
const messageResponse = ref<ChatMessageResponse>();
const error = ref<ErrorResponse | null>(null);
const isLoading = ref(false);

const execute = async <T>(action: () => Promise<T>, path: string, target: { value: T | undefined }) => {
  isLoading.value = true;
  error.value = null;

  try {
    target.value = await action();
  } catch (rawError) {
    error.value = normalizeErrorResponse(rawError, path);
  } finally {
    isLoading.value = false;
  }
};

const requireNumber = (value: string, path: string, label: string) => {
  const parsed = parseOptionalNumber(value);

  if (parsed === undefined) {
    error.value = {
      timestamp: new Date().toISOString(),
      status: 0,
      error: "FORM_ERROR",
      message: `Укажите корректный ${label}`,
      path
    };
    return null;
  }

  return parsed;
};

const loadChats = async () => {
  await execute(
    () =>
      chatApi.getChats({
        page: parseOptionalNumber(listParams.page) ?? 0,
        size: parseOptionalNumber(listParams.size) ?? 20
      }),
    "/chats",
    chatsResponse
  );
};

const loadChat = async () => {
  const id = requireNumber(chatId.value, "/chats/{chatId}", "ID чата");
  if (id === null) return;
  await execute(() => chatApi.getById(id), `/chats/${id}`, chatResponse);
};

const loadChatByRequest = async () => {
  const id = requireNumber(requestId.value, "/chats/by-request/{requestId}", "ID заявки");
  if (id === null) return;
  await execute(() => chatApi.getByRequestId(id), `/chats/by-request/${id}`, chatResponse);
};

const loadMessages = async () => {
  const id = requireNumber(chatId.value, "/chats/{chatId}/messages", "ID чата");
  if (id === null) return;
  await execute(
    () =>
      chatApi.getMessages(id, {
        page: parseOptionalNumber(listParams.page) ?? 0,
        size: parseOptionalNumber(listParams.size) ?? 20
      }),
    `/chats/${id}/messages`,
    messagesResponse
  );
};

const sendMessage = async () => {
  const id = requireNumber(chatId.value, "/chats/{chatId}/messages", "ID чата");
  if (id === null) return;
  await execute(
    () => chatApi.sendMessage(id, parseJsonInput<SendMessageRequest>(messageDraft.value)),
    `/chats/${id}/messages`,
    messageResponse
  );
};
</script>

<template>
  <SectionCard
    description="Чат покрывает список, чтение по chatId/requestId, пагинацию сообщений и отправку message body с optional attachmentFileId."
    kicker="Chat"
    title="Чаты и сообщения"
  >
    <div class="section-grid section-grid--wide">
      <div class="stack">
        <div class="form-grid form-grid--columns">
          <label class="field">
            <span>page</span>
            <input v-model.trim="listParams.page" type="text" />
          </label>
          <label class="field">
            <span>size</span>
            <input v-model.trim="listParams.size" type="text" />
          </label>
          <label class="field">
            <span>chatId</span>
            <input v-model.trim="chatId" placeholder="1" type="text" />
          </label>
          <label class="field">
            <span>requestId</span>
            <input v-model.trim="requestId" placeholder="5" type="text" />
          </label>
        </div>

        <div class="actions">
          <button class="primary-button" :disabled="isLoading" type="button" @click="loadChats">
            GET /chats
          </button>
          <button class="ghost-button" :disabled="isLoading" type="button" @click="loadChat">
            GET /chats/{chatId}
          </button>
          <button class="ghost-button" :disabled="isLoading" type="button" @click="loadChatByRequest">
            GET /chats/by-request/{requestId}
          </button>
          <button class="ghost-button" :disabled="isLoading" type="button" @click="loadMessages">
            GET /messages
          </button>
        </div>

        <label class="field">
          <span>SendMessageRequest</span>
          <textarea v-model="messageDraft" rows="8" />
        </label>
        <button class="ghost-button" :disabled="isLoading" type="button" @click="sendMessage">
          POST /chats/{chatId}/messages
        </button>

        <div v-if="error" class="error-box">
          <strong>{{ error.error }}</strong>
          <p>{{ error.message }}</p>
        </div>
      </div>

      <div class="stack">
        <JsonPreview :value="chatResponse" title="ChatResponse" />
        <JsonPreview :value="messageResponse" title="ChatMessageResponse" />
      </div>
    </div>

    <div class="section-grid">
      <JsonPreview :value="chatsResponse" title="PagedResponse<ChatResponse>" />
      <JsonPreview :value="messagesResponse" title="PagedResponse<ChatMessageResponse>" />
    </div>
  </SectionCard>
</template>
