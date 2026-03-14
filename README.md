# ClawChat2

ClawChat2 is an unofficial Android fork derived from the official OpenClaw Android client in `openclaw/openclaw -> apps/android`.

This fork is focused on one product goal: a simple, direct, chat-first way to talk with OpenClaw agents from Android, with stronger media receive/render support for image, audio, and video messages.

The short rationale for the fork is documented in [WHY_THIS_FORK.md](WHY_THIS_FORK.md).

## Project Status

- Baseline version: `0.2.1`
- Android compatibility baseline: `minSdk 30` (Android 11+)
- Stage: early, experimental, not release-hardened
- Scope: independent community fork, not an official OpenClaw distribution

## Fork Positioning

Compared with the upstream Android client, this fork currently emphasizes:

- chat as the primary surface
- direct agent conversations
- simplified chat-first navigation
- enhanced agent-to-client media handling
- practical Android playback stability improvements for fullscreen image/video viewing
- practical gateway access patterns including Tailscale-friendly usage

This repository exists to explore a narrower Android UX for users who mainly want to open the app and chat with their OpenClaw agents directly, without extra shell complexity.

It is specifically aimed at reducing friction for users who want simple direct agent chat without unnecessary setup, registration, or navigation overhead beyond what is required to connect to their own OpenClaw gateway.

## Important Notice

- This repository is based on code from the official OpenClaw project.
- It is not published by, affiliated with, or endorsed by the OpenClaw maintainers.
- Upstream project references are included only to document code origin and compatibility targets.
- Original upstream license and attribution are preserved in [LICENSE](LICENSE), [FORK_NOTES.md](FORK_NOTES.md), and [THIRD_PARTY_LICENSES](THIRD_PARTY_LICENSES).
- Contribution guidance for this fork lives in [CONTRIBUTING.md](CONTRIBUTING.md).
- Fork-only vs potential upstream change boundaries are documented in [UPSTREAM_BOUNDARY.md](UPSTREAM_BOUNDARY.md).
- Public issue templates and the minimum CI workflow live under [.github](.github).
- Public release prep is tracked in [PUBLIC_RELEASE_CHECKLIST.md](PUBLIC_RELEASE_CHECKLIST.md).
- A draft for upstream communication lives in [UPSTREAM_OUTREACH_DRAFT.md](UPSTREAM_OUTREACH_DRAFT.md).
- The fork rationale for maintainers and reviewers lives in [WHY_THIS_FORK.md](WHY_THIS_FORK.md).

## Media Support

ClawChat2 currently supports structured agent-sent media attachments:

- image receive and fullscreen viewing
- audio receive and playback
- video receive, preview, and fullscreen playback

Current local media contract in this fork prefers gateway-relative fields:

```json
[
  { "type": "text", "text": "Optional caption" },
  {
    "type": "image|audio|video",
    "mimeType": "real MIME type",
    "fileName": "original file name",
    "mediaPath": "/media/<token>",
    "mediaPort": 39393,
    "mediaUrl": "http://10.0.2.2:39393/media/<token>",
    "mediaSha256": "<sha256>",
    "sizeBytes": 123456
  }
]
```

Notes:

- `mediaPath` + `mediaPort` are the preferred fields in this fork
- `mediaUrl` is retained as a compatibility fallback
- current user-originated picker flow is still image-only; audio/video support in this fork is focused on agent-to-client delivery

Operational guides:

- [AGENT_MEDIA_SEND.md](AGENT_MEDIA_SEND.md)
- [AGENT_MEDIA_SERVER.md](AGENT_MEDIA_SERVER.md)

## Build

From the repository root:

```bash
./gradlew :app:assembleDebug
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
```

Install and launch:

```bash
./gradlew :app:installDebug
adb shell am start -n ai.openclaw.app/.MainActivity
```

## Development Notes

- Treat this repository as an early-stage fork, not a drop-in upstream replacement.
- Keep fork-specific behavior clearly separated from changes that could plausibly go upstream later.
- Do not add personal endpoints, private tokens, machine-specific paths, or test-only defaults to committed code.
- Use [status.md](status.md) as the public project status summary for this fork.
