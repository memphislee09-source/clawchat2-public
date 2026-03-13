# ClawChat2

ClawChat2 is a standalone Android app derived from the official OpenClaw Android client.
This repository is the active development mainline for the ClawChat2 variant and is currently in internal testing.

## Current Status

- Baseline version: `0.2.1`
- App package / namespace: `ai.openclaw.app`
- Android compatibility baseline: `minSdk 30` (Android 11+)
- UI direction: contacts-first, chat-centered shell
- Stage: internal testing, not release-hardened yet

Development history and accepted changes are tracked in `status.md`.
UI conventions and visual rules are tracked in `style.md`.

## What Changed From Upstream

This repo started from `openclaw/openclaw -> apps/android`, then continued as an independent Android project.

Current product direction includes:

- `ClawChat2` branding in the app UI
- contacts synced from OpenClaw agents
- strict per-agent direct chat session keys: `agent:<agentId>:clawchat2`
- contacts-first navigation flow
- chat as the default interaction surface
- voice opened as an in-chat modal instead of a dedicated full-screen tab

Some internal-testing shortcuts are intentionally still present for faster validation.
Do not treat the current build as release-ready.

## Agent Media Contract

ClawChat2 now renders agent-sent `image`, `audio`, and `video` attachments when they reach the Android client as structured chat content.

Preferred local contract for agents:

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

Rules:

- target session key: `agent:<agentId>:clawchat2`
- prefer gateway-relative media references:
  - `mediaPath` + `mediaPort` are the stable fields for current ClawChat2 builds
  - `mediaUrl` is kept as a compatibility hint for older builds
- preferred agent behavior: call `scripts/send-clawchat-media.mjs` instead of hand-writing media payloads
- do not send URL-only media
- do not send markdown image syntax
- do not send `data:` URIs
- for local testing, prefer structured media references from the sender script instead of inline base64
- `mimeType` must be the real file MIME type

Recommended first-pass formats:

- image: `image/jpeg`, `image/png`, `image/webp`
- audio: `audio/mpeg`, `audio/mp4`, `audio/wav`
- video: `video/mp4`

Notes:

- This repo ships `scripts/send-clawchat-media.mjs`, which:
  - stores the file in a local media store
  - auto-starts a tiny HTTP server
  - writes a structured assistant media message into the target transcript
  - includes `mediaPath`/`mediaPort` so the Android client can resolve media from the current gateway host
- Emulator default `mediaUrl` host is `10.0.2.2` as a legacy fallback; updated ClawChat2 builds no longer require `--public-host` when the media server runs on the same host as the gateway.
- For agent instructions, prefer `AGENT_MEDIA_SEND.md` as the operational source of truth.
- For media-server start/recovery instructions, prefer `AGENT_MEDIA_SERVER.md` as the operational source of truth.
- Current validation status:
  - emulator image/audio/video receive: passed
  - Android 11 real-device install + launch: passed
  - user-confirmed real-device image/audio/video behavior: passed
  - user-confirmed Android 11 real-device fullscreen image/video behavior: passed on the stable in-app dialog path
  - current accepted fullscreen video sizing is aspect-fit and non-cropping
- Media server lifecycle:
  - the sender script now installs and refreshes a macOS `launchd` agent for the local media HTTP server
  - after the first media send on macOS, the media server should auto-recover if the prior server process exits or the local OpenClaw host environment is restarted
  - if media does not recover after a restart, use `AGENT_MEDIA_SERVER.md` to health-check and restart the LaunchAgent directly
- Current known UI limitation:
  - on the stable `VideoView` fullscreen path, letterbox space is accepted for aspect-fit playback, but that empty area is not yet guaranteed to render as pure black on every real-device path
- The Android chat composer in this repo still only exposes image picking for user-originated sends; audio/video support added here is focused on agent -> ClawChat2 receive and render.
- Usage guide: `AGENT_MEDIA_SEND.md`
- Media-server guide: `AGENT_MEDIA_SERVER.md`

## Open In Android Studio

Open this repository root directly:

```text
clawchat2/
```

This repo is already the Android project root. Do not open `apps/android`; that path only existed in the upstream monorepo.

## Project Layout

- `app/`: Android application module
- `benchmark/`: macrobenchmark module
- `scripts/`: local performance helper scripts
- `status.md`: project baseline, environment notes, accepted changes
- `style.md`: Compose UI style guide for this repo

## Build

From the repository root:

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
```

Targeted test example:

```bash
./gradlew :app:testDebugUnitTest --tests ai.openclaw.app.chat.AgentContactsTest
```

`gradlew` auto-detects the Android SDK at `~/Library/Android/sdk` on macOS if `ANDROID_SDK_ROOT` / `ANDROID_HOME` are unset.

## Lint And Format

Run directly with Gradle:

```bash
./gradlew :app:ktlintCheck :benchmark:ktlintCheck
./gradlew :app:ktlintFormat :benchmark:ktlintFormat
./gradlew :app:lintDebug
```

## Run On Device Or Emulator

Install and launch:

```bash
./gradlew :app:installDebug
adb shell am start -n ai.openclaw.app/.MainActivity
```

Quick device checks:

```bash
adb devices -l
adb shell getprop sys.boot_completed
adb shell dumpsys window | rg 'mCurrentFocus|mFocusedApp'
```

## Pair With OpenClaw Gateway

1. Start an OpenClaw gateway on your main machine with your existing OpenClaw CLI or upstream workspace.

2. In the Android app, open the connection flow and connect with either:

- `Setup Code`
- `Manual`

3. Approve the pairing request on the gateway machine:

```bash
openclaw devices list
openclaw devices approve <requestId>
```

### USB-Only Local Testing

If the phone is connected by USB, you can tunnel the gateway through `adb reverse`:

Terminal A:

Start the OpenClaw gateway from your existing OpenClaw environment.

Terminal B:

```bash
adb reverse tcp:18789 tcp:18789
```

Then connect in the app with:

- Host: `127.0.0.1`
- Port: `18789`
- TLS: off

## Performance

Macrobenchmark:

```bash
./gradlew :benchmark:connectedDebugAndroidTest
```

Reports are written under:

```text
benchmark/build/reports/androidTests/connected/
```

Helper scripts:

```bash
./scripts/perf-startup-benchmark.sh
./scripts/perf-startup-hotspots.sh
```

The benchmark helper writes snapshots to:

```text
benchmark/results/
```

## Integration Capability Test

This suite assumes manual setup is already complete. It does not install, run, or pair automatically.

Preconditions:

1. Gateway is reachable from the Android app.
2. The app is already paired and connected.
3. The app stays unlocked and foregrounded during the run.
4. Keep the `Screen` page active for canvas/A2UI checks.
5. Grant runtime permissions needed by the capabilities under test.

This repo does not currently include the operator-side integration test runner.
Run that suite from the external OpenClaw workspace that owns your gateway/operator tooling.

Optional overrides:

- `OPENCLAW_ANDROID_GATEWAY_URL=ws://...`
- `OPENCLAW_ANDROID_GATEWAY_TOKEN=...`
- `OPENCLAW_ANDROID_GATEWAY_PASSWORD=...`
- `OPENCLAW_ANDROID_NODE_ID=...`
- `OPENCLAW_ANDROID_NODE_NAME=...`

## Permissions

- Discovery:
  - Android 13+ (`API 33+`): `NEARBY_WIFI_DEVICES`
  - Android 12 and below: `ACCESS_FINE_LOCATION`
- Foreground service notifications (Android 13+): `POST_NOTIFICATIONS`
- Camera:
  - `CAMERA` for `camera.snap` and `camera.clip`
  - `RECORD_AUDIO` for `camera.clip` when `includeAudio=true`

## Development Notes

- Read `status.md` before starting a new development session.
- Keep `origin` for this repo and `upstream` for official OpenClaw Android sync.
- Before release/public distribution, remove or replace all test-only defaults, preset endpoints, and convenience credentials.
