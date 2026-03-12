# ClawChat2

ClawChat2 is a standalone Android app derived from the official OpenClaw Android client.
This repository is the active development mainline for the ClawChat2 variant and is currently in internal testing.

## Current Status

- Baseline version: `0.2.1`
- App package / namespace: `ai.openclaw.app`
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
