# Mobile Demo Polish Design

## Goal

Make the existing application fast and reliable to demonstrate on 360-430 px mobile screens. The work improves the current product UI; it does not create a separate mobile application or replace desktop layouts.

The critical demonstration route is authentication, catalog, mentoring request, chat with an image, file manager, and profile/avatar.

## Current Problems

- Mobile rules are spread across several generations of overrides in `style.scss`.
- The bottom bar renders every permitted route, producing cramped or unlabeled controls.
- Several navigation icon names no longer match their SVG branches.
- Pages use inconsistent list/detail behavior and vertical sizing.
- Chat height and composer placement compete with browser chrome, the software keyboard, and the bottom bar.
- Dense desktop controls are stacked without consistently prioritizing the primary action.

## Mobile Application Shell

The shell has two stable regions:

1. A compact 52 px app bar with the current page title, notifications, and one contextual action when needed.
2. A 60 px bottom navigation bar with safe-area padding.

Authenticated student navigation contains Dashboard, Mentors, Requests, Chat, and More. Authenticated mentor navigation substitutes Students for Mentors. Guest navigation contains Dashboard and Account without empty placeholders.

More opens a bottom sheet containing Files, Profile, theme control, and logout. Admin or review routes appear in the sheet when permitted. The sheet traps focus, closes on Escape or backdrop click, and restores focus to its trigger.

All icons use `BaseIcon`; the hand-written conditional SVG chain is removed. Notification counts and unread chat counts remain visible without changing item dimensions.

## Shared Mobile Rules

- Touch targets are at least 44 by 44 px.
- Layout accounts for `env(safe-area-inset-top)` and `env(safe-area-inset-bottom)`.
- Page content has no horizontal overflow at 360 px.
- Headings inside panels use compact type sizes; desktop hero sizing is not carried into mobile tools.
- Primary actions remain visible without covering content.
- Form controls use at least 16 px input text to avoid automatic iOS zoom.
- Dialogs become bottom sheets or full-screen detail views according to task length.
- Loading, empty, error, and success states remain reachable and do not change the navigation geometry.

Existing mobile overrides are moved into `src/styles/mobile.scss`, imported after the desktop stylesheet. Obsolete and contradictory blocks are removed from `style.scss`. Page-specific mobile rules stay grouped by route inside the mobile stylesheet.

## Catalog

- Hero copy becomes a compact title and result count.
- Search remains visible at the top.
- Filters are collapsed by default and open in a bottom sheet.
- Mentor/student cards use one column with identity, key skills, availability, and the primary contact action visible without expansion.
- Secondary biography and review details can expand in place.

## Requests

- The default screen is the request list with compact counters and filters.
- Selecting a request opens a full-screen detail layer above the bottom bar.
- The back control is always visible.
- The action bar is sticky at the bottom of the detail layer and respects safe-area padding.
- Destructive and secondary actions remain visually distinct and require no horizontal scrolling.

## Chat

- Chat list and conversation are separate mobile states.
- Opening a conversation fills the available `100dvh` space above the bottom navigation.
- The header provides a back control, peer identity, and presence state in one row.
- Messages scroll independently.
- The composer remains above the keyboard and bottom navigation, supports a caption and attachment preview, and uses fixed-size attachment/send icon buttons.
- Image attachments fit the viewport, preserve aspect ratio, and open a simple full-screen preview.

## Files

- File categories use a horizontally scrollable segmented row instead of a large two-column block.
- Upload is a full-width primary action close to the category selector.
- Files render as compact rows with preview, name, size, and status.
- Secondary actions move into a per-file menu; download remains directly accessible.

## Profile And Authentication

- Authentication forms are single-column and keep the current tabs.
- Profile sections use a compact horizontal section selector and one active section at a time.
- Avatar upload is visible at the top of the profile and gives immediate progress and preview feedback.
- Save actions are sticky only while the section is dirty.

## Accessibility And Interaction

- Every icon-only button has an accessible name and tooltip where hover exists.
- Focus indicators remain visible.
- Bottom sheet and full-screen detail states are keyboard operable.
- Motion is limited to short transforms and respects reduced-motion preferences.

## Testing

Playwright adds mobile projects for 390x844 and 360x800 viewports. Tests cover:

- guest authentication navigation;
- role-specific bottom navigation and More sheet;
- catalog search and request creation;
- request list/detail/action transition;
- chat list/conversation/back flow;
- image attachment send and receive rendering;
- file upload and avatar preview;
- absence of horizontal page overflow.

Desktop visual tests remain in place. Mobile screenshots are limited to the critical demonstration screens to keep maintenance focused.

## Delivery Order

1. Build the shared shell and mobile stylesheet boundary.
2. Polish catalog and request workflows.
3. Polish chat and integrate demo attachments.
4. Polish files, profile, and authentication.
5. Add mobile E2E coverage and verify both target viewport sizes.
