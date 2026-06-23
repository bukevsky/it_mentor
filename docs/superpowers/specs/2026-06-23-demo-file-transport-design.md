# Demo File Transport Design

## Goal

Provide a demonstrable file workflow while the production backend file integration is unavailable. A user must be able to upload an avatar, manage files, and send images or documents in chat from one device so that another device can render or download them.

The existing backend file implementation remains available and is selected by configuration.

## Scope

- Resume, portfolio, avatar, and chat attachment upload.
- File listing, replacement, deletion, preview, and download.
- Avatar persistence across devices for the same application user.
- Chat attachment exchange across devices through the existing STOMP chat.
- Runtime selection between real and demo transports at build time.

Production-grade private storage and migration of demo files into the backend are out of scope.

## Transport Selection

`files-api.ts` remains the public facade used by stores and components. It delegates to one of two implementations of a common `FilesTransport` interface:

- `real-files-transport.ts` contains the current HTTP behavior.
- `demo-files-transport.ts` uses Supabase Storage and its metadata table.

Selection is controlled by `VITE_FILE_TRANSPORT=real|demo`. The default is `real`. Demo mode also requires:

- `VITE_SUPABASE_URL`
- `VITE_SUPABASE_ANON_KEY`
- `VITE_SUPABASE_FILES_BUCKET`, defaulting to `demo-files`

If demo mode is explicitly selected without valid Supabase configuration, file operations fail with a configuration-specific user message and do not silently call the real backend.

## Supabase Model

The public demonstration bucket stores objects under an unguessable path:

```text
<app-user-id>/<uuid>-<sanitized-filename>
```

The `demo_files` table contains:

- `id bigint generated always as identity primary key`
- `owner_user_id bigint not null`
- `file_type text not null`
- `original_filename text not null`
- `content_type text not null`
- `size bigint not null`
- `storage_path text not null unique`
- `created_at timestamptz not null default now()`
- `deleted_at timestamptz null`

The numeric identity lets the demo transport return the existing `FileResponse` and `FileUploadResponse` shapes without leaking a second identifier type into the application.

The repository includes a setup SQL document for the table, bucket, and demo-only anonymous policies. The bucket is public because chat recipients use different application sessions. The setup must state clearly that this mode is for non-sensitive demonstration files only.

## Identity And Ownership

The current authenticated application user ID is passed to the demo transport as the owner identifier. Lists, replacements, and deletes are scoped to that ID. This is a convenience boundary for the demo UI, not a security boundary: Supabase does not validate the application's JWT.

Files receive UUID-based object paths, so another user cannot discover an object by guessing a sequential file ID alone.

## Chat Attachment Envelope

The backend cannot validate a Supabase file ID. In demo mode, the frontend therefore sends no `attachmentFileId` in the STOMP command. Instead, it appends a versioned plain-text marker to the message body:

```text
[[ITM_DEMO_FILE_V1:<base64url-json>]]
```

The JSON contains only:

- metadata version;
- demo file ID;
- public object URL;
- original filename;
- content type;
- byte size.

The visible caption precedes the marker. For an attachment-only message, the transport supplies a non-empty internal caption so the backend accepts the message. The view-model parser removes both the marker and internal caption from visible text.

Incoming and historical messages pass through a pure parser that returns visible text plus an optional demo attachment. UI components render backend attachments and demo attachments through the same local attachment view model. URLs are accepted only when they use HTTPS and match the configured Supabase host.

Real mode continues to send `attachmentFileId` and never emits demo markers. The shared safe parser remains enabled in both modes, so historical demo markers stay readable after switching back to the backend.

## Avatar And File Manager Flow

Uploads use the existing optimistic progress UI. The demo transport uploads the Blob, inserts metadata, and returns the existing response contract. `imageCache` can then prime and load images from the demo transport exactly as it does for backend files.

Avatar loading requests the latest active `AVATAR` file for the current user. Replacement uploads a new object, updates the metadata row, and removes the previous object after the metadata update succeeds. Delete is soft-delete-first in metadata and best-effort object removal.

## Errors And Recovery

- Upload failure removes an incomplete storage object when possible.
- Metadata insertion failure rolls back the uploaded object when possible.
- Download failure leaves the file metadata visible and offers retry.
- Invalid chat markers render as ordinary text instead of throwing.
- Missing or invalid demo configuration produces a dedicated configuration error.
- Real transport errors preserve the current normalization behavior.

## Testing

- Unit tests for transport selection and configuration validation.
- Unit tests for marker encoding, parsing, host validation, caption handling, and malformed data.
- Unit tests for Supabase response mapping using a mocked client.
- Store tests for upload, replacement, deletion, avatar loading, and chat reconciliation.
- E2E tests use a deterministic in-browser demo transport rather than the live Supabase project.
- A manual smoke test uses two real devices against the deployed demo configuration.

## Rollout

Local development and production keep `VITE_FILE_TRANSPORT=real` unless explicitly changed. The demonstration deployment sets `demo` and supplies its public Supabase configuration. Returning to the backend requires only rebuilding with `real`; no application code is removed.
