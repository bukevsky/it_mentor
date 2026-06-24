# Mobile Demo Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the authentication, catalog, requests, chat, files, and profile demonstration flows efficient and reliable on 360-430 px screens.

**Architecture:** Introduce focused mobile shell components and one final mobile stylesheet imported after desktop styles. Keep route components and desktop behavior intact while standardizing mobile list/detail transitions, touch targets, safe areas, and viewport sizing.

**Tech Stack:** Vue 3, TypeScript, Pinia, Vue Router, Conductor UI, Sass, Playwright

---

### Task 1: Mobile Test Harness And Overflow Guard

**Files:**
- Modify: `frontend/playwright.config.ts`
- Create: `frontend/tests/e2e/mobile.spec.ts`
- Create: `frontend/tests/e2e/support/mobile-assertions.ts`

- [ ] **Step 1: Add two mobile Playwright projects**

```ts
{
  name: "mobile-390",
  use: { ...devices["iPhone 13"], viewport: { width: 390, height: 844 } }
},
{
  name: "mobile-360",
  use: { ...devices["Galaxy S9+"] , viewport: { width: 360, height: 800 } }
}
```

- [ ] **Step 2: Add failing shell and overflow tests**

Test that authenticated mobile pages expose exactly five primary navigation items, open a More control, and satisfy:

```ts
expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(
  await page.evaluate(() => document.documentElement.clientWidth)
);
```

- [ ] **Step 3: Run the mobile test and verify failure**

Run: `npx playwright test tests/e2e/mobile.spec.ts --project=mobile-390`

Expected: FAIL because the current bottom bar renders every permitted route and has no More sheet.

- [ ] **Step 4: Commit the failing harness**

```bash
git add frontend/playwright.config.ts frontend/tests/e2e/mobile.spec.ts frontend/tests/e2e/support/mobile-assertions.ts
git commit -m "test: add mobile demo harness"
```

### Task 2: Mobile App Bar, Bottom Navigation, And More Sheet

**Files:**
- Create: `frontend/src/app/layouts/mobile/MobileAppBar.vue`
- Create: `frontend/src/app/layouts/mobile/MobileBottomNav.vue`
- Create: `frontend/src/app/layouts/mobile/MobileMoreSheet.vue`
- Modify: `frontend/src/app/layouts/MainLayout.vue`
- Create: `frontend/src/app/layouts/mobile/mobile-navigation.ts`
- Create: `frontend/src/app/layouts/mobile/mobile-navigation.test.ts`

- [ ] **Step 1: Write failing role-specific navigation tests**

```ts
expect(buildPrimaryMobileNav(["STUDENT"]).map((item) => item.name)).toEqual([
  "home", "mentors", "requests", "chat", "more"
]);
expect(buildMoreMobileNav(["STUDENT"]).map((item) => item.name)).toEqual(["files", "profile"]);
```

- [ ] **Step 2: Run the focused test and verify failure**

Run: `npm run test:unit -- src/app/layouts/mobile/mobile-navigation.test.ts`

Expected: FAIL because the navigation builder does not exist.

- [ ] **Step 3: Implement shell components**

Use `BaseIcon` for all icons. `MobileMoreSheet` uses a Teleport backdrop, `role="dialog"`, `aria-modal="true"`, Escape handling, focus restoration, Files/Profile routes, theme toggle, and logout command.

`MobileAppBar` derives the current title from route metadata and exposes notifications as a 44 px icon button. `MainLayout` renders these components only when `isMobile`.

- [ ] **Step 4: Run unit and mobile shell tests**

Run: `npm run test:unit -- src/app/layouts/mobile/mobile-navigation.test.ts`

Expected: PASS.

Run: `npx playwright test tests/e2e/mobile.spec.ts --project=mobile-390 --grep "navigation"`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/app/layouts/MainLayout.vue frontend/src/app/layouts/mobile frontend/tests/e2e/mobile.spec.ts
git commit -m "feat: rebuild mobile application shell"
```

### Task 3: Consolidate Mobile Styles

**Files:**
- Create: `frontend/src/styles/mobile.scss`
- Modify: `frontend/src/main.ts`
- Modify: `frontend/src/style.scss`

- [ ] **Step 1: Import a final mobile layer**

Import `@/styles/mobile.scss` after `@/style.scss`. Define shared variables for app bar height, bottom navigation height, safe-area inset, mobile page padding, and touch size.

```scss
:root {
  --mobile-app-bar-height: 52px;
  --mobile-nav-height: 60px;
  --mobile-touch-size: 44px;
}
```

- [ ] **Step 2: Move and normalize shell overrides**

Move the existing `max-width: 999px` shell rules from `style.scss` into `mobile.scss`. Remove duplicate chat, bottom-nav, app-section, and request-detail declarations from the source stylesheet. Use `100dvh`, safe-area padding, and one breakpoint set: 999 px for mobile and 430 px for narrow mobile.

- [ ] **Step 3: Run typecheck and mobile overflow tests**

Run: `npm run typecheck`

Expected: PASS.

Run: `npx playwright test tests/e2e/mobile.spec.ts --project=mobile-390 --grep "overflow"`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add frontend/src/main.ts frontend/src/style.scss frontend/src/styles/mobile.scss
git commit -m "refactor: consolidate mobile layout styles"
```

### Task 4: Catalog And Request Workflow

**Files:**
- Modify: `frontend/src/pages/MentorsPage.vue`
- Modify: `frontend/src/pages/StudentsPage.vue`
- Modify: `frontend/src/pages/RequestsPage.vue`
- Create: `frontend/src/shared/ui/MobileBottomSheet.vue`
- Modify: `frontend/tests/e2e/mobile.spec.ts`

- [ ] **Step 1: Add failing mobile workflow assertions**

Test that search is initially visible, filters open through one button, result cards are one column, contact is visible, selecting a request opens a detail layer, Back closes it, and actions remain visible at 360 px.

- [ ] **Step 2: Run the focused mobile test and verify failure**

Run: `npx playwright test tests/e2e/mobile.spec.ts --project=mobile-360 --grep "catalog and request"`

Expected: FAIL on current filter and detail geometry.

- [ ] **Step 3: Implement catalog bottom-sheet filters**

Create a reusable `MobileBottomSheet` with title, close action, backdrop, safe-area footer slot, Escape handling, and focus restoration. Desktop filters remain inline; mobile renders the same controls in the sheet. Keep search and a compact result count above cards.

- [ ] **Step 4: Implement request list/detail transition**

Make the selected request a full-screen mobile layer with a sticky action footer and explicit back button. Keep the desktop split-panel markup active outside mobile.

- [ ] **Step 5: Run both mobile projects**

Run: `npx playwright test tests/e2e/mobile.spec.ts --project=mobile-390 --project=mobile-360 --grep "catalog and request"`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/pages/MentorsPage.vue frontend/src/pages/StudentsPage.vue frontend/src/pages/RequestsPage.vue frontend/src/shared/ui/MobileBottomSheet.vue frontend/tests/e2e/mobile.spec.ts
git commit -m "feat: streamline mobile mentoring workflow"
```

### Task 5: Chat And Image Preview

**Files:**
- Modify: `frontend/src/pages/ChatPage.vue`
- Modify: `frontend/src/features/chat/ui/ChatHeader.vue`
- Modify: `frontend/src/features/chat/ui/ChatInput.vue`
- Modify: `frontend/src/features/chat/ui/ChatMessageBubble.vue`
- Modify: `frontend/src/styles/mobile.scss`
- Modify: `frontend/tests/e2e/mobile.spec.ts`

- [ ] **Step 1: Add failing mobile chat tests**

Test list-to-thread navigation, back navigation, composer visibility, image attachment rendering, full-screen preview, and no overlap between composer, keyboard-safe content, and bottom navigation.

- [ ] **Step 2: Run the focused test and verify failure**

Run: `npx playwright test tests/e2e/mobile.spec.ts --project=mobile-390 --grep "chat image"`

Expected: FAIL on current viewport/composer geometry.

- [ ] **Step 3: Implement stable mobile chat states**

Use one route-local `isMobileChatOpen` state, a 48 px header row, independently scrolling message feed, and a composer fixed within the chat flex layout. Use `min-height: 0`, `100dvh`, and safe-area padding rather than page-level fixed positioning.

- [ ] **Step 4: Polish image preview**

Constrain inline images to `max-width: min(78vw, 320px)` and `max-height: 42dvh`. On mobile, preview uses the whole viewport with Close and Download icon buttons; desktop zoom controls remain unchanged.

- [ ] **Step 5: Run chat tests on both viewports**

Run: `npx playwright test tests/e2e/mobile.spec.ts --project=mobile-390 --project=mobile-360 --grep "chat image"`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/pages/ChatPage.vue frontend/src/features/chat/ui/ChatHeader.vue frontend/src/features/chat/ui/ChatInput.vue frontend/src/features/chat/ui/ChatMessageBubble.vue frontend/src/styles/mobile.scss frontend/tests/e2e/mobile.spec.ts
git commit -m "feat: polish mobile chat and image preview"
```

### Task 6: Files, Profile, And Authentication

**Files:**
- Modify: `frontend/src/pages/FilesPage.vue`
- Modify: `frontend/src/features/files/ui/FileUploader.vue`
- Modify: `frontend/src/features/files/ui/FileItem.vue`
- Create: `frontend/src/features/files/ui/FileActionsMenu.vue`
- Modify: `frontend/src/pages/ProfilePage.vue`
- Modify: `frontend/src/features/profile/ui/ProfileAvatarUploader.vue`
- Modify: `frontend/src/pages/AuthPage.vue`
- Modify: `frontend/src/styles/mobile.scss`
- Modify: `frontend/tests/e2e/mobile.spec.ts`

- [ ] **Step 1: Add failing mobile files/profile/auth tests**

Cover segmented file categories, upload visibility, per-file actions menu, avatar preview, one active profile section, and 16 px auth inputs without horizontal overflow.

- [ ] **Step 2: Run the focused tests and verify failure**

Run: `npx playwright test tests/e2e/mobile.spec.ts --project=mobile-360 --grep "files profile auth"`

Expected: FAIL on current category and profile layouts.

- [ ] **Step 3: Implement compact file management**

Render categories as a horizontally scrolling segmented row. Keep Upload full-width. File rows expose Download directly and group Replace/Delete in `FileActionsMenu` with 44 px items and an accessible trigger.

- [ ] **Step 4: Implement mobile profile and auth behavior**

Make profile section tabs horizontally scrollable with exactly one active section. Keep avatar and progress at the top. Show the save footer only while dirty. Set mobile form input font size to 16 px and preserve desktop two-column forms.

- [ ] **Step 5: Run focused mobile tests**

Run: `npx playwright test tests/e2e/mobile.spec.ts --project=mobile-390 --project=mobile-360 --grep "files profile auth"`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/pages/FilesPage.vue frontend/src/features/files/ui frontend/src/pages/ProfilePage.vue frontend/src/features/profile/ui/ProfileAvatarUploader.vue frontend/src/pages/AuthPage.vue frontend/src/styles/mobile.scss frontend/tests/e2e/mobile.spec.ts
git commit -m "feat: polish mobile files profile and auth"
```

### Task 7: Final Mobile Verification

**Files:**
- Modify: `frontend/tests/e2e/mobile.spec.ts`
- Modify: `frontend/tests/e2e/app.spec.ts-snapshots/*` only when desktop rendering intentionally changed
- Create: `frontend/tests/e2e/mobile.spec.ts-snapshots/*`

- [ ] **Step 1: Capture focused mobile visual baselines**

Capture only navigation/More, mentor catalog, request detail, chat image, files, and avatar profile at 390 px. Capture navigation and chat at 360 px to guard the narrow viewport.

- [ ] **Step 2: Run all automated verification**

Run: `npm run test:unit`

Expected: all tests PASS.

Run: `npm run typecheck`

Expected: PASS.

Run: `npm run build`

Expected: PASS.

Run: `npm run test:e2e`

Expected: desktop and both mobile projects PASS.

- [ ] **Step 3: Inspect mobile screenshots**

Verify no overlap, clipping, blank media, inaccessible actions, or hidden bottom content at both viewport sizes. Verify chat image aspect ratio and safe-area spacing.

- [ ] **Step 4: Commit**

```bash
git add frontend/tests/e2e/mobile.spec.ts frontend/tests/e2e/mobile.spec.ts-snapshots frontend/tests/e2e/app.spec.ts-snapshots
git commit -m "test: verify mobile demo journey"
```
