# OpenClaw WebChat Voice Session API

This document defines the server-side contract needed for ClawChat2 to implement continuous bidirectional voice conversation without introducing a second chat source of truth.

The product rule is strict:

- voice must reuse the same `openclaw-webchat` session model as normal chat
- voice must write into the same chat history as text chat
- user raw audio clips must be preserved in history together with the transcript text
- the Android app should not need a separate gateway-direct chat path for voice

This is the required contract for the `openclaw-webchat` side.

## Primary Goals

- Keep `openclaw-webchat` as the source of truth for contacts, sessions, history, and pending run state.
- Let Android submit voice turns asynchronously and receive live assistant output.
- Preserve each user voice turn as both:
  - transcript text used for reasoning
  - original uploaded audio clip kept in the same chat history
- Support barge-in by aborting the active assistant run when the user starts a new turn.

## Non-Goals For V1

- No WebRTC, SIP, RTP, or raw duplex audio transport.
- No server-generated assistant audio persistence requirement.
- No separate voice-only session namespace.
- No requirement that the server perform speech recognition in v1.

## Existing Endpoints That Stay Valid

These existing APIs should keep working:

- `GET /api/openclaw-webchat/agents`
- `GET /api/openclaw-webchat/agents/{agentId}/history?limit=200`
- `POST /api/openclaw-webchat/agents/{agentId}/open`
- `POST /api/openclaw-webchat/sessions/{sessionKey}/send`
- `POST /api/openclaw-webchat/uploads`

Voice mode will add new APIs instead of breaking the current synchronous text path.

## Required New APIs

### 1. Session Event Stream

`GET /api/openclaw-webchat/sessions/{sessionKey}/events`

Purpose:

- push live assistant output to Android during voice mode
- push run lifecycle changes
- let Android reconnect without polling history every second

Transport requirement:

- use Server-Sent Events
- content type must be `text/event-stream`
- send heartbeat comments or ping events at least every 15 seconds

Query parameters:

- `cursor` optional
- `mode` optional, may be `voice` or `chat`

Headers:

- support `Last-Event-ID` for reconnect if feasible

Required event types:

- `ready`
- `run.accepted`
- `run.state`
- `assistant.delta`
- `assistant.final`
- `assistant.error`

Event payload rules:

- every payload must include `sessionKey`
- every run-related payload must include `runId`
- ordering must be stable per session
- `assistant.final` must contain the same message shape that later appears in history

Example stream:

```text
event: ready
data: {"sessionKey":"openclaw-webchat:main","streamVersion":1}

event: run.accepted
data: {"sessionKey":"openclaw-webchat:main","clientTurnId":"turn_123","runId":"run_123","userMessageId":"msg_user_1"}

event: run.state
data: {"sessionKey":"openclaw-webchat:main","runId":"run_123","state":"running"}

event: assistant.delta
data: {"sessionKey":"openclaw-webchat:main","runId":"run_123","sequence":1,"textDelta":"Hello","text":"Hello"}

event: assistant.delta
data: {"sessionKey":"openclaw-webchat:main","runId":"run_123","sequence":2,"textDelta":" there.","text":"Hello there."}

event: assistant.final
data: {"sessionKey":"openclaw-webchat:main","runId":"run_123","message":{"id":"msg_asst_1","role":"assistant","createdAt":"2026-03-26T12:00:00.000Z","blocks":[{"type":"text","text":"Hello there."}]}}
```

### 2. Asynchronous Turn Submission

`POST /api/openclaw-webchat/sessions/{sessionKey}/turns`

Purpose:

- submit a turn and get a `runId` immediately
- support both text and voice turn submission under one async contract
- avoid blocking the request until the assistant finishes

Required request body:

```json
{
  "clientTurnId": "2b091745-9db3-4d90-9d8e-a0fe01c8f0f9",
  "mode": "voice",
  "text": "帮我总结一下今天的日程。",
  "blocks": [
    {
      "type": "text",
      "text": "帮我总结一下今天的日程。"
    },
    {
      "type": "audio",
      "source": "upload_abc123",
      "name": "voice-turn-20260326-120000.m4a",
      "mimeType": "audio/mp4",
      "sizeBytes": 48231,
      "durationMs": 2140
    }
  ],
  "transcript": {
    "text": "帮我总结一下今天的日程。",
    "source": "android-speech-recognizer",
    "locale": "zh-CN",
    "isFinal": true
  },
  "response": {
    "stream": true,
    "thinking": "low"
  },
  "interrupt": {
    "policy": "abort_previous_if_running"
  },
  "client": {
    "platform": "android",
    "app": "clawchat2",
    "appVersion": "0.2.3"
  }
}
```

Hard requirements:

- `clientTurnId` is required and must be idempotent within the session
- `mode` must accept at least `text` and `voice`
- `text` is the transcript text used for agent reasoning in v1
- `blocks` must be persisted into chat history as the user message content
- for voice turns, `blocks` must contain both the transcript text block and the uploaded audio block
- `response.stream=true` means the server must emit assistant output over the SSE session stream
- `interrupt.policy=abort_previous_if_running` means the server should abort the current active run in this session before starting the new run

Required success response:

```json
{
  "ok": true,
  "accepted": true,
  "sessionKey": "openclaw-webchat:main",
  "clientTurnId": "2b091745-9db3-4d90-9d8e-a0fe01c8f0f9",
  "userMessageId": "msg_user_1",
  "runId": "run_123",
  "status": "queued"
}
```

Required behavior:

- return success as soon as the run is accepted, not when the assistant finishes
- persist the user message before or atomically with accepting the run
- emit `run.accepted` and later `run.state` plus assistant events over SSE

### 3. Run Abort

`POST /api/openclaw-webchat/sessions/{sessionKey}/runs/{runId}/abort`

Purpose:

- let Android stop the current assistant reply when the user barges in
- let the user explicitly stop a long answer

Required success response:

```json
{
  "ok": true,
  "sessionKey": "openclaw-webchat:main",
  "runId": "run_123",
  "state": "aborted"
}
```

Required behavior:

- must be idempotent
- if the run is already terminal, return success with its terminal state
- after a successful abort, emit `run.state` with `state=aborted`
- do not persist partial assistant output as a normal assistant history message by default

## Upload API Requirements For Voice

The existing `POST /api/openclaw-webchat/uploads` endpoint should be treated as the only upload entry point for user voice clips.

For voice-mode compatibility it must accept:

- `kind=audio`
- `mimeType=audio/mp4` at minimum

Recommended additional mime support:

- `audio/mpeg`
- `audio/wav`
- `audio/webm`
- `audio/ogg`

Required upload response fields for audio:

```json
{
  "upload": {
    "source": "upload_abc123",
    "name": "voice-turn-20260326-120000.m4a",
    "mimeType": "audio/mp4",
    "size": 48231,
    "durationMs": 2140,
    "transcriptStatus": "not_requested"
  }
}
```

Rules:

- `source` must be stable enough to be referenced by `/turns`
- `durationMs` is required for audio uploads
- asynchronous server-side transcription is optional in v1
- if transcription exists, returning `transcriptText` is allowed but not required

## History Persistence Rules

This is the most important product requirement.

For a voice-originated user turn, history must show one user message with:

- a text block containing the transcript
- an audio block containing the original uploaded clip

Example persisted user message:

```json
{
  "id": "msg_user_1",
  "role": "user",
  "createdAt": "2026-03-26T12:00:00.000Z",
  "blocks": [
    {
      "type": "text",
      "text": "帮我总结一下今天的日程。"
    },
    {
      "type": "audio",
      "name": "voice-turn-20260326-120000.m4a",
      "mimeType": "audio/mp4",
      "url": "/api/openclaw-webchat/media?token=...",
      "sizeBytes": 48231,
      "durationMs": 2140
    }
  ]
}
```

Required history guarantees:

- the voice turn must appear in the normal agent history returned by:
  - `POST /api/openclaw-webchat/agents/{agentId}/open`
  - `GET /api/openclaw-webchat/agents/{agentId}/history?limit=200`
- no separate voice history endpoint should be introduced for v1
- assistant final text must also land in normal history

## Assistant Streaming Rules

`assistant.delta` payloads are additive and should reflect the current assistant text for the active run.

Required delta payload shape:

```json
{
  "sessionKey": "openclaw-webchat:main",
  "runId": "run_123",
  "sequence": 3,
  "textDelta": "，今天有三个安排",
  "text": "好的，今天有三个安排"
}
```

Rules:

- `sequence` must increase monotonically within a run
- `text` should contain the full accumulated text so Android can recover if one delta is dropped
- `assistant.final.message.blocks` should be the canonical final assistant message

## Presence And Pending State

The existing contacts response uses presence to hint whether an agent is running.

For voice-mode compatibility:

- set presence to `running` while a submitted run is active
- return to `idle` when the run reaches `final`, `error`, or `aborted`

This keeps current Android pending indicators meaningful until the app switches fully to the stream model.

## Error Contract

For `/turns`, `/abort`, and SSE terminal errors, use machine-readable codes.

Required error code set:

- `session_not_found`
- `invalid_upload_source`
- `unsupported_audio_mime`
- `duplicate_client_turn`
- `run_conflict`
- `run_not_found`
- `internal_error`

Example error response:

```json
{
  "ok": false,
  "error": {
    "code": "invalid_upload_source",
    "message": "Upload source upload_abc123 was not found for this user.",
    "retryable": false
  }
}
```

Example SSE error event:

```text
event: assistant.error
data: {"sessionKey":"openclaw-webchat:main","runId":"run_123","error":{"code":"internal_error","message":"Model execution failed","retryable":true}}
```

## Ordering And Idempotency Requirements

- `clientTurnId` deduplicates retries from Android
- duplicate `clientTurnId` submissions must not create duplicate user messages
- if the original submission was accepted, the server should return the original `runId` and `userMessageId`
- event ordering must be consistent for a single session

## Compatibility Constraints For ClawChat2

These constraints come from the current Android fork and should not be violated:

- the app is chat-first and should not gain a second chat source of truth
- the app already renders audio attachments from normal history blocks
- the app currently uses `openclaw-webchat` as the source of truth for contacts and chat history
- the old gateway-direct voice path is considered transitional and should be removable after this API lands

## Recommended V1 Implementation Order

1. Add the SSE session event stream.
2. Add async `/turns`.
3. Add `/runs/{runId}/abort`.
4. Ensure voice-originated turns persist transcript plus raw audio in normal history.
5. Ensure `agents/open` and `agents/history` surface those persisted messages unchanged.

## Summary For The Implementing Agent

Implement these server capabilities:

- SSE event stream for a single webchat session
- async turn submission with immediate `runId`
- run abort
- persistence of both transcript text and raw audio clip in the same user history message
- reuse of the normal `openclaw-webchat` session and history model

Do not implement:

- WebRTC
- a separate voice-only session model
- a second non-webchat truth for history
