# clawchat2 status

Last updated: 2026-03-11 (Asia/Shanghai)

## Project Status
- Version baseline: **0.1** (`versionName=0.1`, `versionCode=1`)
- Stage: internal testing

## Project Baseline
- Upstream source: `openclaw/openclaw` -> `apps/android`
- Working repo: `https://github.com/memphislee09-source/clawchat2`
- Local path: `/Users/memphis/.openclaw/workspace-mira/clawchat2`
- Sync model: keep `upstream` (official) + `origin` (clawchat2)

## Current State
- Project initialized from official Android codebase.
- Initial bootstrap commit pushed to `main`.
- `upstream` remote configured and fetched.
- v1 feature pass in progress: language selector + Tailscale connection mode.

## Development Rules
- Before each development session: read this file first.
- All future development is based on latest official Android code.
- Keep regular upstream sync before feature work.
- Do not use `MEMORY.md` to track project progress/status; update this file instead.

## Test & Build (2026-03-10)
- Targeted test passed: `:app:testDebugUnitTest --tests ai.openclaw.app.ui.GatewayConfigResolverTest*`
- APK build passed: `:app:assembleDebug`
- APK output: `app/build/outputs/apk/debug/openclaw-2026.3.9-debug.apk`

## Feature Update (2026-03-10, pass-3)
- Chinese localization pass expanded further for shell/connect/settings key labels.
- Tailscale routing revised for compatibility with previous ClawChat behavior:
  - Tailscale default host: `100.116.69.82`
  - Tailscale default port: `18789`
  - Tailscale path uses non-TLS websocket mode by default.
- Connect policy implemented:
  - First try LAN endpoint (discovery)
  - If LAN disconnects/fails, auto-fallback to saved Tailscale endpoint
  - If both fail, status resolves to `Offline`
- Preset dev gateway credentials added (for this environment) to reduce repeated QR pairing during testing:
  - Gateway host: `192.168.0.247`
  - Gateway port: `18789`
  - Token preloaded in secure prefs when missing.

## Testing Policy Note (2026-03-11)
- Current hardcoded/default connection settings (including preset gateway/tailscale and related convenience behavior) are **intentional for test efficiency**.
- These test-oriented shortcuts are accepted in the current internal testing stage.
- Before any release/public build, these items must be removed or replaced with secure release configuration.

## UI Update (2026-03-11, overflow-menu pass)
- Post-onboarding navigation was adjusted for cleaner bottom-tab layout:
  - Added a top-right vertical three-dot overflow button.
  - Moved `Connect` and `Settings` out of the bottom tab bar into the overflow menu.
  - Bottom tab bar now keeps only `Chat`, `Voice`, and `Screen`.
- Active overflow destinations keep the top-right menu button highlighted for clearer orientation.
- Implementation file: `app/src/main/java/ai/openclaw/app/ui/PostOnboardingTabs.kt`
- Validation:
  - Kotlin compile passed: `./gradlew :app:compileDebugKotlin`
  - Emulator install + launch passed: `./gradlew :app:installDebug`
  - Running emulator confirmed app foreground activity: `ai.openclaw.app/.MainActivity`

## UI Update (2026-03-11, chat-density pass)
- Goal: maximize visible message area on the `Chat` screen for internal testing.
- Changes:
  - Removed the in-page `session` selector section entirely.
  - Replaced the top-left `OpenClaw` title on the chat screen with the current conversation/session title, using a smaller text style.
  - Simplified the composer action area:
    - `Send` changed from text button to centered icon button.
    - `Attach` changed to a compact icon button placed to the right of send.
    - `Thinking: low` dropdown trigger changed to a compact `T` button with the same dropdown menu.
  - Reduced chat page outer padding slightly to return more vertical space to the message list.
- Implementation files:
  - `app/src/main/java/ai/openclaw/app/ui/PostOnboardingTabs.kt`
  - `app/src/main/java/ai/openclaw/app/ui/chat/ChatSheetContent.kt`
  - `app/src/main/java/ai/openclaw/app/ui/chat/ChatComposer.kt`
- Validation:
  - Kotlin compile passed: `./gradlew :app:compileDebugKotlin`
  - Emulator install passed: `./gradlew :app:installDebug`
  - App launch confirmed in emulator foreground: `adb shell am start -n ai.openclaw.app/.MainActivity`
  - User manual acceptance for this step: passed

## UI Update (2026-03-11, chat-default + voice-dialog pass)
- Goal: make chat the default shell and reduce navigation/context switching.
- Changes:
  - Removed the bottom tab bar entirely; default app landing screen is now the chat interface.
  - Moved `Screen` into the top-right overflow menu.
  - Added `Chat` to the overflow menu so full-screen utility pages can always return to the default conversation view.
  - Moved the voice entry into the chat composer area, positioned between the thinking control and attachment button.
  - Voice interaction now opens as an in-chat modal dialog instead of navigating to a separate full-screen page.
  - Voice requests are now prepared against the active chat session so voice remains bound to the current conversation.
  - Removed the in-voice `Open settings` button; `App info` is now exposed from the top-right overflow menu instead.
  - Back/edge-swipe behavior aligned with the new structure:
    - Close voice dialog first.
    - Return full-screen pages (`Connect`, `Screen`, `Settings`) to default chat.
    - From default chat, back exits the app.
  - Top bar vertical padding reduced again so the chat viewport starts slightly higher.
- Implementation files:
  - `app/src/main/java/ai/openclaw/app/ui/PostOnboardingTabs.kt`
  - `app/src/main/java/ai/openclaw/app/ui/VoiceTabScreen.kt`
  - `app/src/main/java/ai/openclaw/app/ui/ChatSheet.kt`
  - `app/src/main/java/ai/openclaw/app/ui/chat/ChatSheetContent.kt`
  - `app/src/main/java/ai/openclaw/app/ui/chat/ChatComposer.kt`
  - `app/src/main/java/ai/openclaw/app/MainViewModel.kt`
  - `app/src/main/java/ai/openclaw/app/NodeRuntime.kt`
- Validation:
  - Kotlin compile passed: `./gradlew :app:compileDebugKotlin`
  - Emulator install passed: `./gradlew :app:installDebug`
  - App launch confirmed in emulator foreground: `adb shell am start -n ai.openclaw.app/.MainActivity`
  - User-directed follow-up acceptance requested for this step

## Session Summary (2026-03-11)
- Codebase was explicitly rolled back to commit `66bc66212` (local + GitHub `main`).
- Codex review completed on current baseline:
  - Verdict: not ideal as formal release baseline, but acceptable for current internal testing when test shortcuts are intentional.
  - Key noted risks (kept intentionally for current phase): hardcoded/preset connection defaults, relaxed transport/security choices for test convenience.
- Android emulator environment is now working on this machine (API 35 AVD created + app install/launch + screenshot loop verified).
- Simulated app control loop verified:
  - Can remotely click tabs/screens and return screenshots on demand.
  - Connect page confirmed with endpoint `192.168.0.247:18789`, state observed as `Connected` during session.
- Chat density iteration verified:
  - Latest branch build installed into emulator successfully.
  - User confirmed this round of chat-layout changes is acceptable and can proceed.
- Chat-default / voice-dialog iteration prepared:
  - Latest branch build installed into emulator successfully.
  - App is ready for user validation of modal voice flow and edge-swipe/back behavior.
- Process rule added from this session:
  - After UI click, wait 2 seconds before screenshot, and verify target page text before sending screenshot.

## Handoff / Next Actions
- Continue interactive simulator-driven UI testing in next chat (user directs taps; assistant executes and screenshots).
- Keep test-efficient defaults during internal validation.
- Before release: remove hardcoded secrets/endpoints and tighten transport/security config.
- Brand rename pass (app name / package id / icon / default gateway config) when requested.
- Establish standard upstream sync checklist script if needed.
