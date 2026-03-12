# clawchat2 status

Last updated: 2026-03-12 (Asia/Shanghai)

## Project Status
- Version baseline: **0.2.1** (`versionName=0.2.1`, `versionCode=3`)
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
- Current working baseline promoted to `0.2.1`.
- After this update, GitHub `main` should be treated as the development mainline going forward.

## Chat Attachment Capability Note (2026-03-12)
- Scope checked:
  - local ClawChat2 Android client
  - local upstream docs snapshot under `builds/openclaw-main-android`
- Current conclusion:
  - image attachments: supported
  - audio file attachments: not supported
  - video file attachments: not supported
- Reasoning:
  - local Android chat composer currently only picks `image/*`
  - local Android chat send path currently emits attachment `type = "image"` from the chat UI
  - local Android chat render path currently treats non-text attachments as image/base64 render attempts
  - upstream docs snapshot currently describes Gateway chat attachment handling as image-oriented
  - upstream changelog note states Gateway/Control UI `chat.send` sniffs image attachments and drops non-images
- Current agent send contract for direct ClawChat2 image delivery:
  - send through `chat.send`
  - target session key pattern: `agent:<agentId>:clawchat2`
  - attachment payload shape:
    - `type`: `image`
    - `mimeType`: real image MIME type such as `image/jpeg` or `image/png`
    - `fileName`: source file name
    - `content`: raw base64 bytes without `data:` URI prefix
- Implementation note:
  - do not mark audio/video chat attachments as supported in this repo until Gateway/agent + Android client are expanded together end-to-end

## Upstream Sync Check (2026-03-12, v2026.3.11 review)
- Scope checked:
  - official source: `openclaw/openclaw`
  - compare range: `v2026.3.8..v2026.3.11`
  - Android subtree: `apps/android`
- Result:
  - no Android feature/code changes need to be merged into `clawchat2`
  - upstream Android diff in this range only changed `apps/android/app/build.gradle.kts`
  - upstream change was version-only:
    - `versionCode`: `202603081` -> `202603110`
    - `versionName`: `2026.3.8` -> `2026.3.11`
- Local decision:
  - keep `clawchat2` on its own product baseline versioning for now
  - continue using local version baseline `0.2.1` (`versionName=0.2.1`, `versionCode=3`)
  - do not apply the upstream `v2026.3.11` version bump into this repo unless we later choose to align release numbering with official OpenClaw

## Development Rules
- Before each development session: read this file first.
- All future development is based on latest official Android code.
- Keep regular upstream sync before feature work.
- Do not use `MEMORY.md` to track project progress/status; update this file instead.

## Android Environment (2026-03-12)
- Project root for Android work on this machine: `/Users/memphis/.openclaw/workspace-mira/clawchat2`
- Main compile check:
  - `./gradlew :app:compileDebugKotlin`
- Targeted unit test example:
  - `./gradlew :app:testDebugUnitTest --tests ai.openclaw.app.chat.AgentContactsTest`
- Install debug build:
  - `./gradlew :app:installDebug`
- Launch app on device/emulator:
  - `adb shell am start -n ai.openclaw.app/.MainActivity`
- Check foreground app:
  - `adb shell dumpsys window | rg 'mCurrentFocus|mFocusedApp'`

### Android Tools Paths
- `adb` on PATH: `/opt/homebrew/bin/adb`
- `adb` real binary: `/opt/homebrew/Caskroom/android-platform-tools/36.0.2/platform-tools/adb`
- `sdkmanager`: `/opt/homebrew/bin/sdkmanager`
- `avdmanager`: `/opt/homebrew/bin/avdmanager`
- Emulator binary is **not** on PATH; use:
  - `/opt/homebrew/share/android-commandlinetools/emulator/emulator`

### Emulator / AVD
- Current AVD name: `clawchat2_api35`
- AVD path: `/Users/memphis/.android/avd/clawchat2_api35.avd`
- AVD target:
  - Android 15 / API 35
  - `google_apis/arm64-v8a`
- List AVDs:
  - `avdmanager list avd`
- Start this AVD directly:
  - `/opt/homebrew/share/android-commandlinetools/emulator/emulator -avd clawchat2_api35 -no-snapshot-save`
- Check device readiness:
  - `adb devices -l`
  - `adb shell getprop sys.boot_completed`

### Installed SDK Components
- Confirmed installed via `sdkmanager --list_installed`:
  - `cmdline-tools;latest`
  - `emulator`
  - `platform-tools`
  - `platforms;android-35`
  - `platforms;android-36`
  - `system-images;android-35;google_apis;arm64-v8a`

### Notes
- On this machine, `emulator` is installed but not exported to PATH.
- If Gradle fails inside sandbox with `FileLockContentionHandler` / socket permission errors, rerun build commands outside sandbox.

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
  - User follow-up acceptance for modal voice flow: passed

## UI Update (2026-03-11, contacts-nav pass)
- Goal: make chat navigation feel conversational-first instead of page-first.
- Changes:
  - Added a dedicated contacts screen.
  - Contacts screen top bar shows the app name centered.
  - Contacts list is derived from known chat sessions; tapping a contact opens that conversation directly.
  - Chat screen now also shows a left-side back arrow in the top bar.
  - Chat screen back behavior was corrected:
    - left arrow / system back / left-edge swipe from chat now enters contacts instead of exiting app directly.
    - from contacts, left arrow / system back / left-edge swipe exits the app.
  - Voice modal dismiss behavior tightened:
    - tapping outside the voice popup returns directly to chat.
  - Full-screen pages keep centered titles with a left-side back indicator to match the new gesture model.
- Implementation files:
  - `app/src/main/java/ai/openclaw/app/ui/PostOnboardingTabs.kt`
  - `app/src/main/java/ai/openclaw/app/ui/ContactsScreen.kt`
  - `app/src/main/java/ai/openclaw/app/ui/VoiceTabScreen.kt`
- Validation:
  - Kotlin compile passed: `./gradlew :app:compileDebugKotlin`
  - Emulator install passed: `./gradlew :app:installDebug`
  - App launch confirmed in emulator foreground: `adb shell am start -n ai.openclaw.app/.MainActivity`
  - User acceptance for this step: passed

## UI Update (2026-03-11, contacts-state pass)
- Goal: make the contacts-first shell stable across launches and easier to scan visually.
- Changes:
  - Contacts screen now shows unread state with a compact green dot in the top-right corner of the contact row.
  - Removed secondary helper text under each contact row to keep the list visually cleaner.
  - App exit behavior updated so a fresh launch defaults back to the contacts screen.
  - App background/foreground restoration updated so minimizing and returning preserves the pre-minimize shell state:
    - current page
    - current chat session
    - voice dialog open/closed state
  - Added persisted per-session read timestamps so contact rows can derive unread state from session update time.
- Implementation files:
  - `app/src/main/java/ai/openclaw/app/ui/ContactsScreen.kt`
  - `app/src/main/java/ai/openclaw/app/ui/PostOnboardingTabs.kt`
  - `app/src/main/java/ai/openclaw/app/ui/chat/ChatSheetContent.kt`
  - `app/src/main/java/ai/openclaw/app/MainActivity.kt`
  - `app/src/main/java/ai/openclaw/app/MainViewModel.kt`
  - `app/src/main/java/ai/openclaw/app/NodeRuntime.kt`
  - `app/src/main/java/ai/openclaw/app/SecurePrefs.kt`
  - `app/src/main/java/ai/openclaw/app/chat/ChatController.kt`
- Validation:
  - Kotlin compile passed: `./gradlew :app:compileDebugKotlin`
  - Emulator install passed: `./gradlew :app:installDebug`
  - App launch confirmed in emulator foreground: `adb shell am start -n ai.openclaw.app/.MainActivity`
  - User acceptance for this step: passed

## UI Update (2026-03-11, branding + chat-bubble pass)
- Goal: tighten the chat reading layout and align visible app branding with ClawChat.
- Changes:
  - Chat bubbles widened so they occupy nearly the full row width with only narrow side gutters.
  - User message role label moved to the right side of the bubble header.
  - App display name updated to `ClawChat2` for both English and Chinese resources.
  - Launcher icon updated to match the icon used in the GitHub `clawchat` project:
    - source confirmed from `memphislee09-source/clawchat`
    - synced asset: `app/src/main/res/drawable/clawchat.jpg`
  - Android manifest launcher icon now points directly to the synced `clawchat` drawable asset.
- Implementation files:
  - `app/src/main/java/ai/openclaw/app/ui/chat/ChatMessageViews.kt`
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-zh/strings.xml`
  - `app/src/main/res/drawable/clawchat.jpg`
- Validation:
  - Kotlin compile passed: `./gradlew :app:compileDebugKotlin`
  - Emulator install passed: `./gradlew :app:installDebug`
  - App launch confirmed in emulator foreground: `adb shell am start -n ai.openclaw.app/.MainActivity`
  - User acceptance for this step: passed

## Feature Update (2026-03-12, agent-contacts + clawchat2-session pass)
- Goal: make contacts sync from OpenClaw agents instead of raw sessions, and ensure app-created chats use a strict `ClawChat2` session key per agent.
- Changes:
  - Contacts screen now syncs from OpenClaw `agents.list`, using each agent's identity name and emoji.
  - Contacts manual refresh now uses Material 3 pull-to-refresh.
  - Agent contacts auto-refresh after operator connection succeeds.
  - Each agent's app-owned direct session key is now fixed to `agent:<agentId>:clawchat2`.
  - Contact tap behavior now only checks for and opens that strict `ClawChat2` key.
  - WhatsApp / Slack / other channel sessions no longer count as the agent's app chat.
  - If a strict `ClawChat2` session does not yet exist, opening the contact now enters a fresh empty chat state for that key instead of treating missing history as an error.
  - Chat top bar now prefers the current agent identity (`emoji + name`) when the active session is a `ClawChat2` agent chat.
- Implementation files:
  - `app/src/main/java/ai/openclaw/app/chat/AgentContacts.kt`
  - `app/src/main/java/ai/openclaw/app/chat/ChatController.kt`
  - `app/src/main/java/ai/openclaw/app/MainViewModel.kt`
  - `app/src/main/java/ai/openclaw/app/NodeRuntime.kt`
  - `app/src/main/java/ai/openclaw/app/ui/ContactsScreen.kt`
  - `app/src/main/java/ai/openclaw/app/ui/PostOnboardingTabs.kt`
  - `app/src/test/java/ai/openclaw/app/chat/AgentContactsTest.kt`
- Validation:
  - Kotlin compile passed: `./gradlew :app:compileDebugKotlin`
  - Targeted unit test passed: `./gradlew :app:testDebugUnitTest --tests ai.openclaw.app.chat.AgentContactsTest`
  - Emulator install passed: `./gradlew :app:installDebug`
  - App launch confirmed in emulator foreground: `adb shell am start -n ai.openclaw.app/.MainActivity`

## UI Update (2026-03-12, agent-label + contact-preview + icon-source pass)
- Goal: align visible chat identity with the selected agent, simplify the contacts page, and standardize app icons from the provided `ClawChat.jpg` source file.
- Changes:
  - Assistant-side chat bubble headers now show the current agent label instead of the generic `assistant`.
  - Live / typing / tool-running assistant bubbles now use the same agent label.
  - Contacts page in-content `Contacts` header block removed for a cleaner layout.
  - Top bar `ClawChat2` title on the contacts page now uses the larger title style previously used by the in-page `Contacts` heading.
  - Contact subtitle now shows the latest message from that contact's strict `agent:<agentId>:clawchat2` session:
    - single-line only
    - ellipsized when too long
    - falls back to `No messages yet` when no direct app conversation exists
  - Contact preview text is derived from latest non-system chat history content for that strict `ClawChat2` session.
  - New icon source path for this repo:
    - `app/src/ClawChat.jpg`
  - App icon resources were resynced from that image into:
    - `app/src/main/res/drawable/clawchat.jpg`
    - launcher mipmap assets (`ic_launcher*.png`)
  - Android manifest launcher icon config now points to standard mipmap launcher resources again:
    - `@mipmap/ic_launcher`
    - `@mipmap/ic_launcher_round`
- Implementation files:
  - `app/src/main/java/ai/openclaw/app/chat/AgentContacts.kt`
  - `app/src/main/java/ai/openclaw/app/MainViewModel.kt`
  - `app/src/main/java/ai/openclaw/app/NodeRuntime.kt`
  - `app/src/main/java/ai/openclaw/app/ui/ContactsScreen.kt`
  - `app/src/main/java/ai/openclaw/app/ui/PostOnboardingTabs.kt`
  - `app/src/main/java/ai/openclaw/app/ui/chat/ChatMessageListCard.kt`
  - `app/src/main/java/ai/openclaw/app/ui/chat/ChatMessageViews.kt`
  - `app/src/main/java/ai/openclaw/app/ui/chat/ChatSheetContent.kt`
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/res/drawable/clawchat.jpg`
  - `app/src/main/res/mipmap-*/ic_launcher*.png`
  - `app/src/test/java/ai/openclaw/app/chat/AgentContactsTest.kt`
- Validation:
  - Kotlin compile passed: `./gradlew :app:compileDebugKotlin`
  - Targeted unit test passed: `./gradlew :app:testDebugUnitTest --tests ai.openclaw.app.chat.AgentContactsTest`
  - Emulator install passed: `./gradlew :app:installDebug`
  - App launch confirmed in emulator foreground: `adb shell am start -n ai.openclaw.app/.MainActivity`

## UI Update (2026-03-12, reference-ui polish pass)
- Goal: align contacts/chat presentation with locally provided reference screens and tighten the in-chat interaction surface for device testing.
- Changes:
  - Reworked top bars toward a flatter reference style:
    - removed button chrome around the back and overflow affordances
    - contacts page keeps no visible back arrow
    - overflow menu now contains only `Connect`, `Screen`, `Settings`, and `About`
    - overflow menu icons removed; menu palette normalized to app surfaces
  - Contacts screen visual structure simplified:
    - flatter list rows with smaller corner radii
    - lighter robot-style leading icon treatment
    - tighter spacing to match the provided contact-list reference
  - Chat composer simplified further:
    - input box remains a single main surface with flatter corners
    - bottom action row now keeps 6 actions evenly distributed
    - `T` is fixed as the visible thinking control label
    - `T` popup widened so `Medium` fits on one line
    - `T` popup is floating and outside-click dismissible
    - opening the `T` popup no longer shifts the other bottom-row actions
  - Chat bubbles refined for closer reference alignment:
    - smaller corner radii across bubbles and supporting panels
    - wider bubble width with only narrow edge gutters
    - timestamp appended after the speaker label in the bubble header
  - Voice modal reduced in size for lower visual dominance during in-chat use.
- Implementation files:
  - `app/src/main/java/ai/openclaw/app/ui/ContactsScreen.kt`
  - `app/src/main/java/ai/openclaw/app/ui/PostOnboardingTabs.kt`
  - `app/src/main/java/ai/openclaw/app/ui/VoiceTabScreen.kt`
  - `app/src/main/java/ai/openclaw/app/ui/chat/ChatComposer.kt`
  - `app/src/main/java/ai/openclaw/app/ui/chat/ChatMessageListCard.kt`
  - `app/src/main/java/ai/openclaw/app/ui/chat/ChatMessageViews.kt`
  - `app/src/main/java/ai/openclaw/app/ui/chat/ChatSheetContent.kt`
- Validation:
  - Kotlin compile passed: `./gradlew :app:compileDebugKotlin`
  - Emulator install passed: `./gradlew :app:installDebug`
  - App relaunch passed: `adb shell am start -n ai.openclaw.app/.MainActivity`

## UI Update (2026-03-12, chat-topbar + selection + keyboard-dismiss pass)
- Goal: reduce chat top bar height, enable partial text copy from chat bubbles, and make chat drag feel more natural when the keyboard is open.
- Changes:
  - Chat top bar height reduced by tightening top/bottom padding and shrinking back / overflow action hit areas slightly.
  - Chat bubble body now supports long-press text selection, so any segment of bubble text can be selected and copied with the system text handles/menu.
  - Selection support is applied at the message-body layer so normal markdown text and code content use the same selection flow.
  - Chat message list now dismisses the software keyboard on downward user drag.
  - Pull-down keyboard dismissal clears text focus first, then hides the IME explicitly for more reliable behavior.
- Implementation files:
  - `app/src/main/java/ai/openclaw/app/ui/PostOnboardingTabs.kt`
  - `app/src/main/java/ai/openclaw/app/ui/chat/ChatMarkdown.kt`
  - `app/src/main/java/ai/openclaw/app/ui/chat/ChatMessageListCard.kt`
  - `app/src/main/java/ai/openclaw/app/ui/chat/ChatMessageViews.kt`
  - `app/src/main/java/ai/openclaw/app/ui/chat/ChatSheetContent.kt`
- Validation:
  - Kotlin compile passed: `./gradlew :app:compileDebugKotlin`
  - Emulator install passed: `./gradlew :app:installDebug`
  - App relaunch passed: `adb shell am start -n ai.openclaw.app/.MainActivity`
  - User manual test on emulator: passed

## Release Update (2026-03-12, version 0.2.1 test baseline)
- Goal: roll the current accepted UI/interaction iteration into a new device-test baseline and sync it to GitHub main.
- Changes:
  - Android app version updated to:
    - `versionName=0.2.1`
    - `versionCode=3`
  - Project status updated to mark `0.2.1` as the current baseline.
  - Current README baseline version updated accordingly.
  - Latest reference-driven UI refinement pass recorded in this file.
- Git intent:
  - Commit accepted `0.2.1` baseline.
  - Sync commit to GitHub.
  - Promote GitHub `main` to this same `0.2.1` baseline.

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
  - User later accepted the modal voice flow after contacts/back navigation follow-up adjustments.
- Contacts / navigation iteration verified:
  - Latest branch build installed into emulator successfully.
  - User confirmed the final chat -> contacts -> exit navigation chain is acceptable.
- Contacts state iteration verified:
  - Latest branch build installed into emulator successfully.
  - User confirmed the green-dot unread style and simplified contact rows are acceptable.
- Branding / bubble iteration verified:
  - Latest branch build installed into emulator successfully.
  - User confirmed the expanded chat bubbles, `ClawChat2` naming, and icon sync are acceptable.
- Process rule added from this session:
  - After UI click, wait 2 seconds before screenshot, and verify target page text before sending screenshot.

## Handoff / Next Actions
- Continue interactive simulator-driven UI testing in next chat (user directs taps; assistant executes and screenshots).
- Keep test-efficient defaults during internal validation.
- Before release: remove hardcoded secrets/endpoints and tighten transport/security config.
- Brand rename pass (app name / package id / icon / default gateway config) when requested.
- Establish standard upstream sync checklist script if needed.
