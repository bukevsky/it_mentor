# Demo File Transport Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a Supabase-backed demo file transport that supports avatars, file management, and cross-device chat attachments while preserving the real backend transport.

**Architecture:** Keep `filesApi` as the application facade and select a focused real or demo transport from environment configuration. Demo chat attachments are encoded into the existing STOMP message body and normalized into a local attachment view model on both live and historical messages.

**Tech Stack:** Vue 3, TypeScript, Pinia, Vitest, STOMP, `@supabase/supabase-js`, Supabase Storage/Postgres

---

### Task 1: Transport Configuration And Interface

**Files:**
- Modify: `frontend/src/shared/config/env.ts`
- Modify: `frontend/.env.example`
- Create: `frontend/src/features/files/api/files-transport.ts`
- Create: `frontend/src/features/files/api/files-transport.test.ts`

- [ ] **Step 1: Write failing configuration tests**

Test real mode defaults, demo mode accepts complete configuration, and incomplete demo configuration throws `DemoFilesConfigurationError`.

```ts
expect(resolveFilesTransportConfig({})).toEqual({ mode: "real" });
expect(resolveFilesTransportConfig({ VITE_FILE_TRANSPORT: "demo" })).toThrowError(
  "Demo file storage is not configured"
);
```

- [ ] **Step 2: Run the focused test and verify failure**

Run: `npm run test:unit -- src/features/files/api/files-transport.test.ts`

Expected: FAIL because the transport resolver does not exist.

- [ ] **Step 3: Define the common interface and resolver**

```ts
export interface DownloadedFileBlob {
  blob: Blob;
  filename: string;
}

export interface FilesTransport {
  upload(type: FileType, file: File, ownerUserId: number): Promise<FileUploadResponse>;
  list(params: FilesListParams, ownerUserId: number): Promise<PagedResponse<FileResponse>>;
  replace(fileId: number, file: File, ownerUserId: number): Promise<FileResponse>;
  remove(fileId: number, ownerUserId: number): Promise<void>;
  fetchBlob(fileId: number, fallbackFilename: string): Promise<DownloadedFileBlob>;
}
```

Extend `env` with `fileTransport`, `supabaseUrl`, `supabaseAnonKey`, and `supabaseFilesBucket`. Document the four variables in `.env.example`.

- [ ] **Step 4: Run tests and typecheck**

Run: `npm run test:unit -- src/features/files/api/files-transport.test.ts`

Expected: PASS.

Run: `npm run typecheck`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/shared/config/env.ts frontend/.env.example frontend/src/features/files/api/files-transport.ts frontend/src/features/files/api/files-transport.test.ts
git commit -m "feat: define selectable file transport"
```

### Task 2: Preserve The Real Backend Transport

**Files:**
- Create: `frontend/src/features/files/api/real-files-transport.ts`
- Create: `frontend/src/features/files/api/real-files-transport.test.ts`
- Modify: `frontend/src/features/files/api/files-api.ts`

- [ ] **Step 1: Write failing real transport tests**

Mock `request`, `uploadFile`, and `XMLHttpRequest`. Assert that upload, list, replace, remove, and blob download retain the exact current paths and bearer behavior.

```ts
expect(uploadFile).toHaveBeenCalledWith("/files/avatar", avatarFile);
expect(request).toHaveBeenCalledWith("/files/42", { method: "DELETE" });
```

- [ ] **Step 2: Run tests and verify failure**

Run: `npm run test:unit -- src/features/files/api/real-files-transport.test.ts`

Expected: FAIL because the real transport module does not exist.

- [ ] **Step 3: Move current HTTP behavior without changing semantics**

Move upload/list/replace/remove and XHR blob download into `realFilesTransport`. Keep browser save behavior in the facade. `filesApi.ts` selects the transport once and supplies `useAuthStore().user.id` through a small `currentOwnerId()` helper at call time.

- [ ] **Step 4: Run focused and existing tests**

Run: `npm run test:unit -- src/features/files/api/real-files-transport.test.ts`

Expected: PASS.

Run: `npm run test:unit`

Expected: all existing tests PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/features/files/api/files-api.ts frontend/src/features/files/api/real-files-transport.ts frontend/src/features/files/api/real-files-transport.test.ts
git commit -m "refactor: isolate real file transport"
```

### Task 3: Supabase Demo Storage

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`
- Create: `frontend/src/features/files/api/demo-files-transport.ts`
- Create: `frontend/src/features/files/api/demo-files-transport.test.ts`
- Create: `docs/demo-files-supabase.sql`

- [ ] **Step 1: Install the supported Supabase client**

Run: `npm install @supabase/supabase-js`

Expected: dependency added to package files.

- [ ] **Step 2: Write failing demo CRUD tests**

Use a mocked Supabase client and verify:

```ts
await transport.upload("AVATAR", avatarFile, 17);
expect(storage.upload).toHaveBeenCalledWith(expect.stringMatching(/^17\//), avatarFile);
expect(table.insert).toHaveBeenCalledWith(expect.objectContaining({ owner_user_id: 17 }));
```

Also cover type-filtered list, owner-scoped replacement, soft deletion, public URL resolution, upload rollback after metadata failure, and configuration errors.

- [ ] **Step 3: Run focused tests and verify failure**

Run: `npm run test:unit -- src/features/files/api/demo-files-transport.test.ts`

Expected: FAIL because the demo transport does not exist.

- [ ] **Step 4: Implement demo CRUD**

Create one Supabase client and map rows through a pure `toFileResponse` function. Use UUID object paths, validate `owner_user_id` on reads and mutations, soft-delete metadata before best-effort storage removal, and return existing response contracts.

```ts
const objectPath = `${ownerUserId}/${crypto.randomUUID()}-${sanitizeFilename(file.name)}`;
const { data, error } = await client.storage.from(bucket).upload(objectPath, file, {
  contentType: file.type || "application/octet-stream",
  upsert: false
});
```

- [ ] **Step 5: Add reproducible Supabase setup SQL**

The SQL creates `demo_files`, the public `demo-files` bucket, indexes on owner/type/created time, and explicit demo-only anonymous CRUD policies. Begin the file with a warning that the policies must never be used for sensitive production files.

- [ ] **Step 6: Run tests and typecheck**

Run: `npm run test:unit -- src/features/files/api/demo-files-transport.test.ts`

Expected: PASS.

Run: `npm run typecheck`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/package.json frontend/package-lock.json frontend/src/features/files/api/demo-files-transport.ts frontend/src/features/files/api/demo-files-transport.test.ts docs/demo-files-supabase.sql
git commit -m "feat: add supabase demo file transport"
```

### Task 4: Wire The Facade, File Manager, Avatar, And Cache

**Files:**
- Modify: `frontend/src/features/files/api/files-api.ts`
- Modify: `frontend/src/features/files/model/image-cache.ts`
- Modify: `frontend/src/features/files/model/files-store.ts`
- Modify: `frontend/src/features/profile/model/profiles-store.ts`
- Create: `frontend/src/features/files/model/files-store.test.ts`

- [ ] **Step 1: Write failing store tests**

Cover demo upload followed by list refresh, replacement, delete, blob download, and latest avatar reload.

```ts
await store.upload("resume", resumeFile);
expect(filesApi.list).toHaveBeenCalled();
expect(store.files[0]).toMatchObject({ name: "resume.pdf", status: "uploaded" });
```

- [ ] **Step 2: Run tests and verify failure**

Run: `npm run test:unit -- src/features/files/model/files-store.test.ts`

Expected: FAIL on missing transport-aware behavior.

- [ ] **Step 3: Select and use the transport**

Make all facade methods delegate to `realFilesTransport` or `demoFilesTransport`. Keep the existing `filesApi` method signatures so stores and components remain stable. Route `fetchFileBlob` through the selected transport so `imageCache` works in both modes.

- [ ] **Step 4: Run store, profile, and full unit tests**

Run: `npm run test:unit -- src/features/files/model/files-store.test.ts`

Expected: PASS.

Run: `npm run test:unit`

Expected: all tests PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/features/files/api/files-api.ts frontend/src/features/files/model/image-cache.ts frontend/src/features/files/model/files-store.ts frontend/src/features/profile/model/profiles-store.ts frontend/src/features/files/model/files-store.test.ts
git commit -m "feat: use demo files across profile and manager"
```

### Task 5: Chat Attachment Envelope

**Files:**
- Create: `frontend/src/features/chat/model/demo-attachment.ts`
- Create: `frontend/src/features/chat/model/demo-attachment.test.ts`
- Modify: `frontend/src/features/chat/model/chat-store.ts`
- Modify: `frontend/src/features/chat/model/chat-store.test.ts`
- Modify: `frontend/src/features/chat/ui/ChatMessageBubble.vue`

- [ ] **Step 1: Write failing codec tests**

```ts
const encoded = encodeDemoAttachment({ id: 12, url, filename: "photo.jpg", contentType: "image/jpeg", size: 1024 });
expect(parseDemoAttachment(`Подпись\n${encoded}`)).toEqual({
  body: "Подпись",
  attachment: expect.objectContaining({ demoFileId: 12, filename: "photo.jpg" })
});
```

Cover attachment-only captions, malformed markers, non-HTTPS URLs, and hosts outside configured Supabase.

- [ ] **Step 2: Run codec tests and verify failure**

Run: `npm run test:unit -- src/features/chat/model/demo-attachment.test.ts`

Expected: FAIL because the codec does not exist.

- [ ] **Step 3: Implement a versioned safe codec**

Define `DemoChatAttachment`, `encodeDemoAttachment`, and `parseDemoAttachment`. Use UTF-8-safe base64url encoding and exact marker boundaries. Validate version, numeric ID/size, filename length, content type length, HTTPS, and configured host.

- [ ] **Step 4: Write failing chat-store tests**

Assert that demo mode sends the marker in `body`, sends `attachmentFileId: null`, preserves the optimistic preview, parses live confirmations, and parses historical messages. Assert real mode keeps the numeric backend attachment command.

- [ ] **Step 5: Implement local message normalization**

Extend `ChatViewMessage` with `displayBody` and `demoAttachment`. Normalize server responses at every store ingress: history load, sync merge, live event, last message, and optimistic confirmation. Render/download `demoAttachment.url` directly; keep backend attachments on `filesApi`.

- [ ] **Step 6: Run chat tests**

Run: `npm run test:unit -- src/features/chat/model/demo-attachment.test.ts src/features/chat/model/chat-store.test.ts src/features/chat/model/use-chat-socket.test.ts`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/features/chat/model/demo-attachment.ts frontend/src/features/chat/model/demo-attachment.test.ts frontend/src/features/chat/model/chat-store.ts frontend/src/features/chat/model/chat-store.test.ts frontend/src/features/chat/ui/ChatMessageBubble.vue
git commit -m "feat: exchange demo attachments through chat"
```

### Task 6: Deterministic E2E And Deployment Documentation

**Files:**
- Modify: `frontend/tests/e2e/support/mock-api.ts`
- Modify: `frontend/tests/e2e/support/mock-stomp.ts`
- Modify: `frontend/tests/e2e/app.spec.ts`
- Modify: `frontend/.env.example`
- Create: `docs/demo-file-transport.md`

- [ ] **Step 1: Extend E2E mocks with demo file URLs and attachment markers**

Keep live Supabase out of automated tests. The mock storage returns deterministic metadata and an in-memory Blob URL; the STOMP mock echoes a marker-bearing message to sender and recipient contexts.

- [ ] **Step 2: Add an attachment exchange E2E assertion**

Upload a small PNG fixture, send it, and assert the message contains a visible image with the original filename while the marker text is absent.

- [ ] **Step 3: Document deployment configuration**

Document Supabase SQL execution, bucket verification, environment variables, non-sensitive demo-only policy, switching back to real mode, and a two-device smoke-test checklist.

- [ ] **Step 4: Run complete frontend verification**

Run: `npm run test:unit`

Expected: all tests PASS.

Run: `npm run typecheck`

Expected: PASS.

Run: `npm run build`

Expected: PASS.

Run: `npm run test:e2e`

Expected: all E2E tests PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/tests/e2e frontend/.env.example docs/demo-file-transport.md
git commit -m "test: cover demo file exchange"
```
