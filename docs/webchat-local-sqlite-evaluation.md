# WebChat Local SQLite Evaluation

Date: 2026-03-26
Status: evaluated, deferred

## Why Consider SQLite

The current instant-entry improvement uses a lightweight local history cache to avoid blank chat screens when entering from Contacts. That solves the immediate UX issue for known sessions, but it is still an optimization layer, not a full local-first chat data model.

Moving to a formal local SQLite store would make chat/session reads come from a persistent database first, with network sync updating local state in the background. That is the right direction if ClawChat2 later wants WeChat-like behavior across cold start, unread state, session ordering, paging, search, and stronger offline continuity.

## Current Baseline

- Active production chat path is `WebChatController`, not upstream `ChatController`.
- The app currently has no Room/SQLite infrastructure in the Android module.
- Recent work added a lightweight file-backed history cache for instant contact-to-chat entry.
- `openclaw-webchat` remains the source of truth on the server side; local persistence would be a read-optimized client mirror, not a second authority.

## Cache Layer vs SQLite

Lightweight cache:

- Goal is fast entry, not full local chat state.
- Stores only a limited recent window of messages.
- Simple file-backed persistence.
- Good for optimistic render before refresh.
- Not suitable as the long-term basis for unread counts, paging, search, drafts, or reliable session restoration.

Formal SQLite store:

- Local `sessions`, `messages`, and related metadata become the default read path.
- Contacts and chat screens can render from disk immediately.
- Network calls become sync/update work instead of blocking the first render.
- Enables unread state, paging, search, drafts, offline continuity, and better recovery after process death.

## Recommended Scope Split

### Option A: SQLite MVP

Estimated effort: 3 to 5 working days

Target:

- Add Room/SQLite infrastructure.
- Add minimal tables for `sessions` and `messages`.
- Persist chat history and session metadata after `open` / `history` / `send`.
- Read chat timeline from local DB before network refresh.
- Read contact preview and ordering from local session/message data where feasible.

Expected outcome:

- Known conversations open immediately even after cold start.
- Contacts/chat behavior becomes much closer to WeChat.
- Keeps the implementation focused on the current pain point instead of building a full offline system.

### Option B: Product-Grade Local-First Chat

Estimated effort: about 1 to 1.5 weeks

Adds on top of MVP:

- `attachments`
- `read_state`
- `drafts`
- stronger dedupe rules
- better session ordering
- paged history loading
- more complete sync/update paths

Expected outcome:

- Local chat experience feels substantially like a real messaging app.
- Opens the door to unread counts and more stable state restoration.

### Option C: Long-Term Durable Foundation

Estimated effort: about 2 to 3 weeks

Adds on top of product-grade local-first:

- schema migration strategy
- attachment lifecycle and cleanup
- search/indexing
- repair paths for inconsistent local/server state
- broader automated coverage

Expected outcome:

- Strong long-term foundation for chat-heavy iteration.
- Significantly higher implementation and maintenance cost.

## Main Risks

- Sync semantics: local DB must mirror `openclaw-webchat` without creating split-brain behavior.
- Session identity: direct-session keys and webchat fallback keys must be normalized correctly.
- Dedupe: `open`, `history`, optimistic sends, and background refresh can all produce overlapping message data.
- Schema stability: once local DB ships, migrations become part of ongoing maintenance.
- Attachments: media metadata, local rendering state, and cleanup rules need clear ownership.

## Recommended Direction

Do not build the full SQLite system immediately.

If instant chat entry remains the only active pain point, the lightweight cache is the correct short-term tradeoff.

If the project later wants stronger WeChat-like behavior, implement Option A first:

- local DB for `sessions` + `messages`
- local-first chat open
- network refresh layered on top

That is the best balance between user-visible impact and implementation risk. It also avoids over-investing before unread/search/drafts are truly needed.

## Deferred Follow-Up

When this work is resumed later, start with:

1. schema proposal for `sessions` and `messages`
2. repository boundary between `WebChatController` and local persistence
3. dedupe rules for remote history vs optimistic messages
4. migration plan from the current lightweight cache
