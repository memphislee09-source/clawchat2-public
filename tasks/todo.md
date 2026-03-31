# Task Plan

- [x] Create an experimental branch for upstream absorption work.
- [x] Re-scope the branch to selective upstream absorption only.
- [x] Add gateway diagnostics helpers and wire them into connection surfaces.
- [x] Split Android builds into `play` and `thirdParty` flavors with SMS gating.
- [x] Expose Voice as a first-class shell destination without replacing contacts/chat-first flow.
- [x] Self-test the branch with local Android builds.
- [x] Install the tested build onto the emulator and notify for manual QA.
- [x] Fix first-time TLS fingerprint probing for Tailscale / HTTPS gateway endpoints.
- [ ] Expand copied gateway diagnostics with stronger version/build identity and structured endpoint details.
- [x] Expand copied gateway diagnostics with stronger version/build identity and structured endpoint details.
- [ ] Verify the manual TLS fingerprint fallback is reachable from setup-code and manual/Tailscale flows.
- [ ] Add focused regression coverage for fingerprint normalization / persistence paths if gaps remain.
- [ ] Rebuild `playDebug` serially after clearing Kotlin incremental cache conflicts.
- [ ] Document the Mate60/Harmony manual TLS fingerprint workaround in the review notes.
- [x] Verify the manual TLS fingerprint fallback works for setup-code and manual/Tailscale connection flows.
- [x] Rebuild a fresh `playDebug` APK serially after the TLS fingerprint fallback changes.
- [x] Update review notes with the verified Mate60/Harmony workaround.
- [x] Reduce default Android operator pairing scopes so `pairing required` approvals do not need `operator.talk.secrets`.
- [x] Verify basic pairing still works with the reduced operator scope set.
- [x] Rebuild a fresh `playDebug` APK for the Mate60 pairing retest.
- [x] Merge `codex/upstream-bridge-pass` into `main` as the new development baseline.
- [x] Verify local `main` points at the tested onboarding/Tailscale/pairing fix set.
- [x] Push updated `main` to GitHub and keep future work based on it.

# Review

- Current work is happening on branch `codex/upstream-bridge-pass`.
- ClawChat2 already had the foundations for streaming UI, notifications access, Voice, and Screen, so this branch focuses on upstream-style hardening and exposure rather than replacing the app shell.
- The implementation target is selective absorption: diagnostics, flavor split, and compatible UX/runtime improvements that preserve `openclaw-webchat`, contacts-first navigation, and the current media path.
- Streaming chat remains on the follow-up list because the active production path is `WebChatController`, not the upstream `ChatController`.
- Added `GatewayDiagnostics.kt`, connected diagnostics copy actions to Connect and onboarding failure states, and kept the existing TLS trust prompt flow intact.
- Added `play` and `thirdParty` flavors with `OPENCLAW_ENABLE_SMS`; `play` removes `SEND_SMS` from the manifest while onboarding/settings/runtime/device capability reporting now hide or disable SMS accordingly.
- Added a first-class `Voice` destination to the shell overflow menu while preserving the existing chat voice dialog and contacts/chat-first default navigation.
- Fixed the first-time TLS fingerprint probe path for HTTPS / Tailscale endpoints by probing with OkHttp first, then falling back to a hostname-aware raw TLS socket path. This keeps the existing trust confirmation UX but makes certificate capture closer to the real `wss://` connection stack.
- Tightened the TLS probe again after Huawei/Mate60 feedback by raising the first-time probe timeout to 10 seconds and adding a direct host-based TLS socket fallback before the resolved-address fallback.
- Expanded copied diagnostics so reports now include app build identity (`versionName`, `versionCode`, flavor, build type), package name, device build identity, route guess, parsed gateway host/port/TLS fields, and a likely failure classification.
- Verification completed on 2026-03-24 with:
- `./gradlew :app:compilePlayDebugKotlin :app:compileThirdPartyDebugKotlin`
- `./gradlew :app:assemblePlayDebug :app:assembleThirdPartyDebug`
- `./gradlew :app:testPlayDebugUnitTest --tests ai.openclaw.app.gateway.GatewayTlsTest --tests ai.openclaw.app.node.ConnectionManagerTest`
- Installed `app/build/outputs/apk/play/debug/openclaw-0.2.3-play-debug.apk` to emulator `emulator-5554` after removing the newer official build that blocked downgrade install.
- Verified `ai.openclaw.app/.MainActivity` is resumed on the emulator and the app process is alive (`pid 11998` at validation time).
- Rebuilt and reinstalled the updated `play` APK on the connected Android phone (`c2f22adf`) and verified `ai.openclaw.app/.MainActivity` resumed after launch.
- Latest fixed artifact for Mate60/Tailscale retest: `app/build/outputs/apk/play/debug/openclaw-0.2.3-play-debug.apk` built at `2026-03-24 21:33:37 +0800`.
- Diagnostics-enhanced artifact rebuilt after the report-format change with `./gradlew :app:assemblePlayDebug`.
- Added an explicit manual TLS fingerprint input to both onboarding advanced setup and the Connect tab advanced controls so Huawei/Harmony devices can bypass automatic first-time certificate probing.
- Fixed fingerprint normalization so the app accepts the exact output produced by `openssl x509 -noout -fingerprint -sha256` (`sha256 Fingerprint=AA:BB:...`) instead of rejecting valid pasted values.
- Fresh verification for the manual TLS workaround completed on 2026-03-26 with:
- `./gradlew --stop`
- `./gradlew :app:testPlayDebugUnitTest --tests ai.openclaw.app.gateway.GatewayTlsTest --tests ai.openclaw.app.ui.GatewayDiagnosticsTest --tests ai.openclaw.app.node.ConnectionManagerTest`
- `./gradlew :app:assemblePlayDebug`
- Fresh Mate60 retest artifact: `app/build/outputs/apk/play/debug/openclaw-0.2.3-play-debug.apk` built at `2026-03-26 14:52:23 +0800`.
- Diagnosed a second Mate60 pairing blocker on 2026-03-26: the Android app requested `operator.talk.secrets` during default operator pairing, while the available local CLI approval path only had `operator.read`, `operator.write`, `operator.approvals`, and `operator.pairing`, so `openclaw devices approve --latest` failed with `missing scope: operator.talk.secrets`.
- Reduced the default Android operator pairing scopes to `operator.read` + `operator.write` so standard local approval can succeed without a higher-scope paired operator device.
- Added a regression test for the reduced default operator scopes and re-verified with:
- `./gradlew --stop`
- `./gradlew :app:testPlayDebugUnitTest --tests ai.openclaw.app.node.ConnectionManagerTest --tests ai.openclaw.app.gateway.GatewayTlsTest --tests ai.openclaw.app.ui.GatewayDiagnosticsTest`
- `./gradlew :app:assemblePlayDebug`
- Fresh pairing-retest artifact: `app/build/outputs/apk/play/debug/openclaw-0.2.3-play-debug.apk` built at `2026-03-26 15:20:38 +0800`.
- The verified branch `codex/upstream-bridge-pass` has now been merged into local `main` and pushed to `origin/main` as merge commit `accdac1dc9`.
- Future development should continue from `main` at or after `accdac1dc9`; the branch work is no longer just an experiment and is now the project baseline.

## TLS Probe Inspection Plan

- [x] Trace `GatewayTls.kt` probe flow and all callers.
- [x] Compare first-time fingerprint probing against the real OkHttp websocket path.
- [x] Identify the safest fix that preserves the existing trust prompt UX.

## TLS Probe Inspection Review

- `NodeRuntime.connect()` uses `probeGatewayTlsFingerprint()` only for first-time TLS trust acquisition before any websocket session is started.
- The probe path uses a raw trust-all `SSLSocket`, manual DNS resolution, manual IPv4-first ordering, manual SNI setup, and a short fixed timeout.
- The real gateway connection uses OkHttp WebSocket with a custom `X509TrustManager` and optional hostname bypass when a fingerprint is pinned.
- The probe and the real session therefore do not share the same DNS, route selection, connect fallback, timeout behavior, or platform TLS integration, so the probe can fail on devices where the later OkHttp connection would succeed.
- Safest fix: replace the raw-socket probe with an OkHttp-based preflight that captures the peer certificate fingerprint through the same client/TLS stack used by `GatewaySession`, but still gates pin persistence behind the existing user trust prompt.

## Voice Conversation Plan

- [x] Confirm the target product shape is continuous bidirectional voice conversation, not raw WebRTC phone-style transport.
- [x] Keep `openclaw-webchat` as the single source of truth for sessions, history, and voice-originated turns.
- [x] Draft the required `openclaw-webchat` API contract for streaming voice turns, run abort, and raw audio clip persistence.
- [ ] Implement the `openclaw-webchat` turn-streaming and abort endpoints.
- [ ] Update Android voice mode to submit turns through `openclaw-webchat` instead of the gateway-direct voice path.
- [ ] Add Android capture support for preserving raw user audio clips alongside the transcript text.
- [ ] Verify on device that voice turns stream back live, support barge-in, and persist transcript plus raw audio in the same chat history.

## Voice Conversation Review

- The agreed direction is to make voice mode feel like a live two-way conversation while still using chat turns under the hood.
- The main architectural rule is that voice must not create a second session truth beside `openclaw-webchat`.
- Server-side interface requirements have been written to `OPENCLAW_WEBCHAT_VOICE_SESSION_API.md` so the paired project agent can implement against a stable contract before Android-side integration starts.

## Contact Navigation Investigation Plan

- [x] Trace the contacts click path into session-switch and chat-load state.
- [x] Compare contact click navigation with chat screen auto-load and saved-session restore behavior.
- [x] Check whether a connected device or emulator is available for targeted repro.
- [x] Confirm the root cause and decide whether the next step should be a narrow code fix or a repro-only report.
- [x] Fix the contact-to-chat session race so the selected contact wins.
- [x] Make chat entry anchor to the latest message on new-session entry without forcing later jumpy auto-scroll.
- [x] Rebuild and verify on the connected Android phone.

## Contact Navigation Investigation Review

- The current working hypothesis is a session-selection race between contact click navigation, chat screen startup auto-load, and saved-session restoration.
- Root cause confirmed on the connected Redmi K20 (`c2f22adf`): tapping `bai` from Contacts could open `yuyan-mini` because chat navigation, chat-screen startup `loadChat(chatSessionKey)`, and saved-session restoration were all allowed to race without a "latest navigation wins" guard.
- `WebChatController` now applies an optimistic target session immediately, tracks in-flight navigation, and ignores stale open/history responses from superseded session requests.
- `ChatMessageListCard` now anchors a newly entered session directly to the latest message with a non-animated jump, then only auto-scrolls later updates when the user is already following the bottom.
- Fresh verification completed on 2026-03-26 with:
- `./gradlew :app:compilePlayDebugKotlin`
- `./gradlew :app:assemblePlayDebug`
- `adb -s c2f22adf install -r app/build/outputs/apk/play/debug/openclaw-0.2.3-play-debug.apk`
- Real-device repro before fix: tapping `bai` opened `yuyan-mini`.
- Real-device verification after fix: tapping `bai` opened `bai`, and tapping `王语嫣` opened `王语嫣`.

## Instant Chat Entry Plan

- [x] Confirm why contact-to-chat still shows a long blank loading period after the session race fix.
- [x] Add a local webchat history cache so entering a known session can render recent messages immediately.
- [x] Keep the cached view consistent with background refresh and outbound messages.
- [x] Rebuild and verify on device that contact entry shows cached history instantly without reintroducing session mix-ups.

## Instant Chat Entry Review

- The blank delay after entering from Contacts was caused by Chat waiting for a fresh `/agents/{id}/open` or `/history` response before rendering anything, even when the session had already been seen locally.
- `WebChatController` now persists recent per-session chat history to app-local storage and restores it optimistically on session navigation, so known sessions can render immediately before network refresh completes.
- Cached histories are sanitized before persistence so large inline attachment payloads are not written back into the cache file.
- Contact navigation now prefers the contact's real direct session key when choosing optimistic state, instead of only using the fallback `openclaw-webchat:` key. This was required for the cache to hit existing `agent:<id>:clawchat2` sessions.
- Background refresh, outbound sends, slash-command results, and agent-open results now all refresh the local history cache so the instant view stays aligned with the latest transcript.
- Contacts refresh now also warms recent direct-session histories in the background for up to 8 contacts, which makes "not opened in this process yet" sessions behave much closer to WeChat after the contacts page has been visible briefly.
- Fresh verification completed on 2026-03-26 with:
- `./gradlew :app:compilePlayDebugKotlin`
- `./gradlew :app:assemblePlayDebug`
- `adb -s c2f22adf install -r app/build/outputs/apk/play/debug/openclaw-0.2.3-play-debug.apk`
- Real-device verification on Redmi K20 (`c2f22adf`):
- Cold-start reopen of `bai` restored message history immediately from local cache.
- Returning to Contacts and tapping `bai`, then dumping UI after `0.2s`, showed cached chat content immediately instead of the previous blank loading state.
- Returning to Contacts, waiting `3s` for background prefetch, and tapping `王语嫣`, then dumping UI after `0.2s`, showed the correct `王语嫣` conversation content immediately.
- The previous wrong-session regression did not return during this verification: tapping `王语嫣` still opened `王语嫣`, not another agent.

## Local SQLite Evaluation Plan

- [x] Compare the current lightweight history cache against a formal local SQLite session/message store.
- [x] Estimate the work for a minimal SQLite MVP versus a more complete local-first chat system.
- [x] Record the evaluation in project docs and defer implementation for now.

## Local SQLite Evaluation Review

- SQLite is a viable next step if ClawChat2 later wants stronger WeChat-like behavior than the current lightweight cache can provide.
- The work has been evaluated and documented in `docs/webchat-local-sqlite-evaluation.md`.
- Current recommendation is to keep the lightweight cache for now and only start SQLite work later if the project wants a true local-first session/message read path.
- The preferred future entry point is a narrow MVP first: `sessions` + `messages`, local-first open, then background refresh layered on top.

## Chat UX Polish Plan

- [x] Trace the current chat image viewer and composer input implementations.
- [x] Add touch pinch-to-zoom behavior for chat image viewing.
- [x] Change the chat composer to default to one line and grow with input content.
- [x] Rebuild and verify the updated chat interactions.

## Chat UX Polish Review

- Fullscreen chat images now support two-finger pinch zoom and pan inside the viewer, so image inspection no longer depends on the fixed fit-to-screen scale.
- The chat composer now starts at a single-line height and grows with message length up to a bounded maximum, instead of reserving a two-line tall input by default.
- Fresh verification completed on 2026-03-26 with:
- `./gradlew :app:compilePlayDebugKotlin`
- `./gradlew :app:assemblePlayDebug`
- This round was build-verified only; no fresh device-side manual interaction pass was run after the UI polish change.

## Chat Bottom Anchor Plan

- [x] Reproduce and trace why entering a contact chat does not land on the real conversation bottom.
- [x] Replace the current last-item jump with a true scroll-to-bottom behavior.
- [x] Rebuild, install, and verify on device that contact entry lands at the latest message.

## Chat Bottom Anchor Review

- Root cause: `ChatMessageListCard` treated `scrollToItem(lastIndex)` as a bottom-anchor primitive, but that only jumps to the start of the last item. For long final messages, the viewport still landed above the true conversation bottom.
- `ChatMessageListCard` now uses explicit `scrollToConversationBottom` / `animateToConversationBottom` helpers that first jump to the last item and then continue scrolling until the list can no longer scroll forward.
- Fresh verification completed on 2026-03-26 with:
- `./gradlew :app:compilePlayDebugKotlin`
- `./gradlew :app:assemblePlayDebug`
- `adb -s c2f22adf install -r app/build/outputs/apk/play/debug/openclaw-0.2.3-play-debug.apk`
- Real-device verification on Redmi K20 (`c2f22adf`):
- Re-entered the `王语嫣` contact conversation after install and dumped the UI tree.
- Visible content now starts inside the tail end of the latest long assistant message instead of the beginning of that message, which confirms the viewport is landing at the true bottom of the conversation rather than the top of the last item.

## Release 0.2.4 Plan

- [x] Review the current workspace status and decide the next version bump target.
- [x] Bump Android app version to `0.2.4` / `versionCode 6`.
- [x] Update release-facing docs to reflect the new baseline and this round's chat/contact UX fixes.
- [x] Build and verify the new `playDebug` APK.
- [ ] Commit the release prep changes on `main` and push to `origin/main`.

## Release 0.2.4 Review

- Android app version has been bumped to `0.2.4` with `versionCode 6`.
- Release-facing docs now point at the `0.2.4` baseline, and a new `RELEASE_NOTES_v0.2.4.md` has been added for this round's contact/chat UX work.
- This release includes the current verified chat UX fixes: instant local history restore for known sessions, contact-entry bottom anchoring, fullscreen image pinch zoom, and the single-line auto-growing composer.
- Fresh verification completed on 2026-03-26 with:
- `./gradlew :app:compilePlayDebugKotlin`
- `./gradlew :app:assemblePlayDebug`
- `adb -s c2f22adf install -r app/build/outputs/apk/play/debug/openclaw-0.2.4-play-debug.apk`
- Verified artifact for this release-prep pass: `app/build/outputs/apk/play/debug/openclaw-0.2.4-play-debug.apk` built at `2026-03-26 19:23 +0800`.

## Project Re-entry Plan

- [x] Read the main project status docs and recent task history.
- [x] Inspect local git branch state and uncommitted workspace contents.
- [x] Fetch remote state from GitHub remotes and compare ahead/behind status.
- [x] Summarize the current baseline, sync status, and the safest next starting point.

## Project Re-entry Review

- Read `README.md`, `status.md`, and `tasks/todo.md` to re-establish the active product baseline before continuing work.
- Local development baseline is `main` at `c31cd620fa` (`release: cut 0.2.4 baseline`), and `git rev-list --left-right --count main...origin/main` returned `0 0`, so the private GitHub repo is fully in sync with the local checked-out branch.
- The local workspace is not clean: it contains many untracked emulator screenshots, UI XML dumps, and temporary log files in the repo root, plus this `tasks/todo.md` update.
- `public/main` is not a branch with shared ancestry to this private repo; it is a separate public-history line currently at `aa4ec3cd83` (`ci: fix android checks workflow`), so ahead/behind counts are not a safe measure of product sync there.
- Content comparison confirms the public repo is still on the older `0.2.1` baseline (`versionCode 3`, `versionName 0.2.1` in `app/build.gradle.kts`), while local/private `main` is on `0.2.4` (`versionCode 6`, `versionName 0.2.4`) and includes the later chat UX, diagnostics, flavor split, TLS fallback, pairing-scope, and webchat-cache work.
- `upstream/main` has also diverged with unrelated history and a very different product scope, so it should be treated only as an upstream reference source, not as a branch that can be compared to local `main` by simple ahead/behind sync status.
- Safest restart point: continue all new ClawChat2 development from local/private `main` tracked against `origin/main`, and treat `public/main` as a later release/public-sync target rather than the active development baseline.

## Chat Reply Readout Interface Review Plan

- [x] Confirm the requested feature is agent-reply readout, not full duplex voice conversation.
- [x] Read the `claw-webchat` Android voice integration doc and identify the newly available server interfaces.
- [x] Compare those interfaces against the current ClawChat2 chat + local TTS architecture.
- [x] Decide whether agent-reply readout should call webchat voice APIs or stay local-only.

## Chat Reply Readout Interface Review

- `claw-webchat/docs/ANDROID_VOICE_AGENT_INTEGRATION.md` is specifically an implementation guide for replacing the old gateway-direct Android voice flow with WebChat-backed async turns, session SSE, upload-backed raw audio persistence, and session-scoped abort.
- The newly available server interfaces are `GET /api/openclaw-webchat/sessions/{sessionKey}/events`, `POST /api/openclaw-webchat/sessions/{sessionKey}/turns`, and `POST /api/openclaw-webchat/sessions/{sessionKey}/runs/{runId}/abort`.
- Those interfaces are aimed at full voice conversation behavior: async voice/text turns, live assistant streaming, raw user audio upload persistence, and barge-in abort. They are not required for a feature whose only goal is to read an already-rendered agent reply aloud on Android.
- Current ClawChat2 chat still uses the older HTTP-only WebChat path in `WebChatController`: `POST /sessions/{sessionKey}/send`, polling-based refresh, and no SSE consumption yet. The controller still contains a stale comment saying WebChat has no abort API.
- ClawChat2 already has a reusable local reply-TTS layer in `NodeRuntime` + `TalkModeManager`, including persisted speaker enablement, system-TTS fallback, and immediate stop/mute behavior.
- Recommended decision for the current feature: do not call the new WebChat voice endpoints just to add chat reply readout. Implement reply readout locally off the existing chat message flow, using current WebChat message/history state as the trigger and the existing Android TTS layer as the output path.

## Public Release UI Polish Plan

- [x] Rework the chat composer controls so send/stop remains primary while model, thinking, new-chat, attachment, and readout controls are more self-explanatory and accessible.
- [x] Refine chat bubble layout and metadata so user/assistant messages read more like a public-release messaging UI without changing system/tool event treatment.
- [x] Improve contacts list scanning with stronger active-state treatment and lightweight recency context.
- [x] Replace remaining chat-facing mixed-language/error/empty-state copy with consistent release-ready wording and retry affordances.
- [x] Rebuild, test, package, and install the updated Android build onto device `c2f22adf`.

## Public Release UI Polish Review

- The chat composer now keeps send/stop as the dominant primary action while moving attachment, new chat, model, thinking, and reply readout into clearer labeled chips that remain below the input box as lightweight quick controls.
- The composer quick controls now expose their current state more directly, and the primary send/stop button has an explicit accessibility label instead of relying on a silent custom icon branch.
- Chat bubbles now separate assistant/user alignment more clearly, use bounded bubble widths, and split sender/timestamp metadata into a cleaner header row so the transcript reads more like a public-release messaging UI.
- Contacts now show a stronger current-session treatment with a leading accent rail, an `Active` / `当前` pill, and a lightweight recency label to make scanning the list easier once the device has many agents.
- Chat-facing empty/error states now use release-ready wording and include a retry affordance instead of only engineering-style status copy.
- Remaining chat attachment surfaces that were still English-only now use bilingual UI copy for loading, unavailable, preview, and playback states.
- Fresh verification completed on 2026-03-29 with:
- `./gradlew --stop`
- `./gradlew :app:compilePlayDebugKotlin`
- `./gradlew :app:testPlayDebugUnitTest`
- `./gradlew :app:assemblePlayDebug`
- `adb -s c2f22adf install -r app/build/outputs/apk/play/debug/openclaw-0.2.4-play-debug.apk`
- `adb -s c2f22adf shell am start -n ai.openclaw.app/.MainActivity`
- `adb -s c2f22adf shell dumpsys window windows | rg "ai.openclaw.app|mCurrentFocus|mFocusedApp"`
- `adb -s c2f22adf shell pidof ai.openclaw.app`
- Updated artifact installed for device QA: `app/build/outputs/apk/play/debug/openclaw-0.2.4-play-debug.apk` built on `2026-03-29 08:13 +0800`.
- Real-device manual visual QA is still pending from the user after install; this pass verified build/test/install/launch readiness, not a full human interaction review.

## Session Density Polish Plan

- [x] Make chat bubbles left-aligned with tighter, symmetrical horizontal margins to maximize transcript area.
- [x] Distinguish user, agent, and system messages primarily through color while keeping the transcript visually compact.
- [x] Replace the bottom quick controls with compact icon-only actions that all fit without horizontal scrolling.
- [x] Rebuild, package, install to device `c2f22adf`, and update review notes with the verification result.

## Session Density Polish Review

- Chat bubbles are now left-aligned for all roles, and the timeline uses tighter symmetrical horizontal padding so more transcript width is given to the conversation itself.
- User, agent, and system messages are now separated primarily by bubble color: user stays on the accent tint, agent moves to the stronger neutral surface tint, and system keeps the warning tint.
- The bottom quick controls are back to compact icon-only actions so attachment, new chat, model, thinking, readout, and send/stop all fit in one visible row without horizontal scrolling.
- Model and thinking still keep their existing popup menus, but their triggers are now icon buttons instead of letter chips.
- Fresh verification completed on 2026-03-29 with:
- `./gradlew :app:compilePlayDebugKotlin`
- `./gradlew :app:testPlayDebugUnitTest`
- `./gradlew :app:assemblePlayDebug`
- `adb -s c2f22adf install -r app/build/outputs/apk/play/debug/openclaw-0.2.4-play-debug.apk`
- `adb -s c2f22adf shell am start -n ai.openclaw.app/.MainActivity`
- `adb -s c2f22adf shell dumpsys activity activities | rg "mResumedActivity|topResumedActivity|ai.openclaw.app|MainActivity"`
- `adb -s c2f22adf shell dumpsys window | rg "mCurrentFocus|mFocusedApp|ai.openclaw.app|MainActivity"`
- `adb -s c2f22adf shell pidof ai.openclaw.app`
- Real-device install and launch verification passed; manual visual QA on the updated session density/layout is now ready for the user.

## Session Bubble Visual Pass Plan

- [x] Make image attachments flush to the left, right, and bottom edges of the chat bubble when large, while keeping smaller images at natural size without forced upscaling.
- [x] Set agent bubbles to a white background and move timestamp formatting/placement to follow the sender label with same-day vs historical formatting.
- [x] Simplify the bottom action row further by removing button chrome, rotating the attachment icon, and swapping the model icon to a cleaner visual.
- [x] Update fullscreen image interaction so a single tap opens and a single tap in fullscreen dismisses.
- [x] Rebuild, package, install to device `c2f22adf`, and update review notes with verification.

## Session Bubble Visual Pass Review

- Large image attachments now render edge-to-edge inside the bubble body without extra left, right, or bottom padding, while smaller images keep their natural pixel width and sit centered instead of being forcibly stretched.
- Assistant bubbles now use a white background, and sender/time metadata sits on one left-aligned line with same-day messages shown as `HH:mm` and historical messages shown as `yyyy.M.d HH:mm`.
- The bottom quick-control row keeps every action visible in one line but drops the button chrome entirely; the attachment glyph is rotated for a lighter paperclip feel, and the model trigger now uses a cleaner sparkle-style icon instead of the robot glyph.
- Fullscreen image viewing still opens on tap from the chat bubble, and a single tap on the fullscreen image now dismisses the viewer directly without reaching for the close button.
- Fresh verification completed on 2026-03-29 with:
- `./gradlew :app:compilePlayDebugKotlin`
- `./gradlew :app:testPlayDebugUnitTest`
- `./gradlew :app:assemblePlayDebug`
- `adb -s c2f22adf install -r app/build/outputs/apk/play/debug/openclaw-0.2.4-play-debug.apk`
- `adb -s c2f22adf shell am start -n ai.openclaw.app/.MainActivity`
- `adb -s c2f22adf shell pidof ai.openclaw.app`
- `adb -s c2f22adf shell dumpsys activity activities | rg "mResumedActivity|topResumedActivity|ai.openclaw.app|MainActivity"`
- `adb -s c2f22adf shell dumpsys window | rg "mCurrentFocus|mFocusedApp|ai.openclaw.app|MainActivity"`
- Real-device install and launch verification passed on Redmi K20 (`c2f22adf`); the updated session visual pass is ready for manual QA.
- Separate follow-up decision: if the project later resumes true Android voice conversation work, then the Android client should migrate that voice path onto the new WebChat `/events` + `/turns` + `/abort` interfaces and retire the remaining gateway-direct voice plumbing.

## Release Docs Sync Plan

- [x] Review release-facing docs that should mention the latest chat visual polish.
- [x] Update `README.md`, `status.md`, and `RELEASE_NOTES_v0.2.4.md` to reflect the verified session UI baseline.
- [x] Stage only the intended tracked changes, commit them on `main`, and push to `origin/main`.

## Release Docs Sync Review

- `README.md` now reflects the denser chat transcript layout, edge-to-edge large image treatment, natural-size small image rendering, fullscreen image tap behavior, and the new borderless icon-only composer row in both English and Chinese.
- `status.md` now records the same release-facing chat UI baseline and adds the 2026-03-29 Redmi K20 install-and-launch verification for the public-release visual polish pass.
- `RELEASE_NOTES_v0.2.4.md` now includes the final chat-UI release highlights: white agent bubbles, denser left-aligned transcript styling, refined image presentation, and the simplified one-row icon composer controls.
- This sync pass keeps the repo-root screenshots, XML dumps, and temporary logs untracked and excluded from Git history, while the verified tracked chat UI and doc changes are prepared for commit on `main` and push to `origin/main`.

## Chat Reply Readout Implementation Plan

- [x] Wire chat reply readout onto the existing local Android TTS path instead of the new WebChat voice endpoints.
- [x] Remove the chat composer microphone action.
- [x] Add a readout toggle below the chat input and bind it to persisted speaker state.
- [x] Trigger readout only for newly arrived assistant replies in the active chat session.
- [x] Rebuild the Android app to verify the change compiles and packages cleanly.

## Chat Reply Readout Implementation Review

- The chat composer microphone icon has been removed, and the input area now shows a dedicated `朗读 Agent 回复` switch directly below the text field.
- The new chat toggle reuses the existing persisted speaker state, so the preference survives app restarts and stays aligned with the existing TTS plumbing.
- `NodeRuntime` now watches the active WebChat session, arms reply readout only while a run is pending, and reads aloud only the next newly arrived assistant message with speakable text. This avoids replaying old history on chat open or session switch.
- Switching chats or sending the app to the background now stops any in-progress reply readout.
- Fresh verification completed with:
- `./gradlew :app:compilePlayDebugKotlin`
- `./gradlew :app:assemblePlayDebug`

## Chat Reply Readout Regression Debug Plan

- [x] Reproduce the missing-readout issue on the connected Redmi K20 instead of handing testing back to the user.
- [x] Capture runtime evidence to distinguish missing callback vs. TTS playback failure.
- [x] Apply the narrowest fix for the proven root cause.
- [x] Rebuild, reinstall, and re-test on the phone until the TTS path completes successfully.

## Chat Reply Readout Regression Debug Review

- Root cause was not the chat callback anymore; the device reached `TalkMode` and tried to fall back to system TTS, but Android package visibility blocked access to the installed Xiaomi TTS engine (`AppsFilter ... ai.openclaw.app -> com.xiaomi.mibrain.speech BLOCKED`), which made `TextToSpeech` initialization fail.
- Fixed by adding an Android manifest `<queries>` entry for `android.intent.action.TTS_SERVICE`, allowing the app to discover and bind system TTS engines on Android 11+.
- Also added explicit `TalkMode` debug logs for system TTS initialization, speak start, and utterance completion so device-side verification is straightforward.
- During the same pass, the chat readout control was tightened from a full-width row into the composer action strip as the rightmost speaker icon, matching the requested lighter interaction footprint.
- Fresh device verification on Redmi K20 (`c2f22adf`) after reinstall showed:
- `TextToSpeech: Sucessfully bound to com.xiaomi.mibrain.speech`
- `TalkMode: system TTS initialized`
- `TalkMode: system TTS speak start chars=67`
- `TalkMode: system TTS utterance done`

## Documentation Sync Plan

- [x] Update public-facing docs to mention chat reply readout and the verified Mate60 result.
- [x] Verify final workspace diff and keep local-only debug artifacts out of the commit.
- [x] Commit the readout + documentation changes on `main`.
- [x] Push updated `main` to `origin/main`.

## Documentation Sync Review

- Updated `README.md`, `status.md`, and `RELEASE_NOTES_v0.2.4.md` so the repo now documents both the chat reply readout feature and the successful Mate60 verification.
- Verified the final staged diff excluded the repository-root screenshots, UI dumps, and temporary logs, which remain local-only debug artifacts.
- Re-ran `./gradlew :app:compilePlayDebugKotlin` before publishing.
- Pushed the final `main` update to `origin/main` as commit `6c299e7405` (`feat: add chat reply readout`).

## General Attachment Upload Plan

- [x] Confirm the current Android composer is still image-only.
- [x] Compare Android-side upload assumptions with the current `claw-webchat` generic attachment contract.
- [x] Expand the composer picker and pending-upload model beyond image-only behavior.
- [x] Send generic file blocks through the existing WebChat upload path so agents can process them.
- [x] Rebuild and verify the updated Android attachment flow.

## General Attachment Upload Review

- The Android composer attachment picker was still locked to `image/*` and always converted pending uploads into `OutgoingAttachment(type = "image", ...)`, even though `openclaw-webchat` now accepts `image`, `audio`, `video`, and generic `file` uploads through the same `/api/openclaw-webchat/uploads` endpoint.
- `ChatSheetContent` now uses `ActivityResultContracts.OpenMultipleDocuments()` with `*/*`, resolves a display name from `OpenableColumns.DISPLAY_NAME`, classifies each selected URI as `image` / `audio` / `video` / `file`, and keeps the existing max-8 pending attachment cap.
- The composer-side pending model is now generic instead of image-only, and the pending attachment strip shows a small type icon so users can tell images, audio, video, and regular files apart before sending.
- The existing WebChat send path did not need protocol changes: `OutgoingAttachment` was already generic enough, so the Android side now simply passes the real attachment kind through to `WebChatController.uploadAttachment()`.
- Chat attachment rendering now treats `type = "file"` as a first-class generic file card instead of surfacing it as `Unsupported attachment`, which keeps sent and received file messages readable inside the thread.
- Fresh verification completed on 2026-03-28 with:
- `./gradlew :app:testPlayDebugUnitTest --tests ai.openclaw.app.ui.chat.ChatAttachmentSupportTest`
- `./gradlew :app:compilePlayDebugKotlin`
- `./gradlew :app:assemblePlayDebug`
- User-side manual verification also passed on 2026-03-28: a real Huawei Mate60 successfully uploaded a Markdown file through the chat attachment button, confirming the generic `file` path works end to end on device.
- One verification caveat showed up during the first parallel Gradle run: Kotlin incremental compilation briefly hit the known multi-daemon backup/classpath issue, but the serial rerun completed successfully and no code-level compile errors remained.

## Chat Send/Stop Button Plan

- [x] Trace the current Android chat send button and stop path against the existing WebChat behavior.
- [x] Replace the separate Android send/stop split with a single send button that switches to stop-state while a reply is running.
- [x] Wire the stop action to the existing WebChat session stop endpoint so tapping the send button again aborts the active reply.
- [x] Add focused regression coverage for the WebChat send/stop controller behavior.
- [x] Rebuild and verify the updated Android chat composer flow.

## Chat Send/Stop Button Review

- Android chat composer send behavior now matches the current `openclaw-webchat` input more closely: once a chat run is pending, the existing send button switches from the paper-plane icon to a stop icon instead of leaving a separate stop control in the button row.
- Tapping the send button again while a reply is pending now calls `WebChatController.abort()`, which is no longer a stub. The controller now posts to `POST /api/openclaw-webchat/sessions/{sessionKey}/stop`, tracks a transient `stopInFlight` state, and clears that state when the pending run settles or the user switches chats.
- `ChatComposer` no longer shows the old dedicated stop button. The button row stays compact, and the send button now owns all send/stop semantics: send when idle, stop while pending, and spinner while the stop request itself is in flight.
- Added a focused Robolectric + MockWebServer regression test in `WebChatControllerTest` to verify that aborting during an in-flight send issues the expected session stop request to WebChat.
- Fresh verification completed on 2026-03-28 with:
- `./gradlew :app:testPlayDebugUnitTest --tests ai.openclaw.app.chat.WebChatControllerTest`
- `./gradlew :app:compilePlayDebugKotlin`
- `./gradlew :app:assemblePlayDebug`
- Installed the fresh APK onto the connected real device `c2f22adf` with:
- `adb -s c2f22adf install -r app/build/outputs/apk/play/debug/openclaw-0.2.4-play-debug.apk`
- Brought `ai.openclaw.app/.MainActivity` to the foreground on that device so the user can test the send-button stop interaction immediately.

## Chat Stop Color + Thinking Picker Plan

- [x] Compare the current Android `T` button against the live WebChat thinking picker endpoints and interaction model.
- [x] Change the send button stop-state styling to use a red abort affordance instead of the normal accent color.
- [x] Replace the local fixed `T` menu with a session-backed thinking picker that loads options from WebChat and patches the current session on selection.
- [x] Add focused regression coverage for WebChat thinking option fetch/switch behavior.
- [x] Rebuild, install to the connected real device, and update review notes.

## Chat Stop Color + Thinking Picker Review

- The composer send button now uses a red stop-state affordance while a reply is pending, so the same button clearly reads as an abort action instead of looking like the normal blue send action.
- The Android `T` control no longer uses a fixed local `off/low/medium/high` list. It now opens a session-backed picker that requests `GET /api/openclaw-webchat/sessions/{sessionKey}/thinking-options`, shows the current thinking level plus the current model label, and renders the real option list returned by WebChat.
- Selecting a thinking option now patches the active WebChat session immediately through `PATCH /api/openclaw-webchat/sessions/{sessionKey}/thinking`, updates the local current-thinking state from the response, and supports provider-specific values such as binary `on/off` models instead of forcing everything through the old four-level mapping.
- Added focused coverage in `WebChatControllerTest` for both stop-request routing and the thinking picker fetch/switch flow, using MockWebServer to verify the Android controller hits the expected WebChat endpoints and updates local state from their responses.
- Fresh verification completed on 2026-03-28 with:
- `./gradlew :app:testPlayDebugUnitTest --tests ai.openclaw.app.chat.WebChatControllerTest`
- `./gradlew :app:compilePlayDebugKotlin`
- `./gradlew :app:assemblePlayDebug`
- Installed the fresh APK onto the connected real device `c2f22adf` with:
- `adb -s c2f22adf install -r app/build/outputs/apk/play/debug/openclaw-0.2.4-play-debug.apk`
- Brought `ai.openclaw.app/.MainActivity` to the foreground after install so the user can test the red stop-state button and the new `T` thinking picker immediately.

## Chat Model Picker Plan

- [x] Compare the WebChat model picker endpoints and interaction against the current Android composer controls.
- [x] Remove the leftmost refresh button from the Android composer strip and insert an `M` model button to the left of `T`.
- [x] Add a session-backed model picker that loads the current model plus available models from WebChat and switches the active session model on selection.
- [x] Add focused regression coverage for WebChat model option fetch/switch behavior.
- [x] Rebuild, install to the connected real device, and update review notes.

## Chat Model Picker Review

- The Android composer control strip no longer shows the leftmost refresh button. The strip now uses attachment, send/stop, `M`, `T`, and readout controls, which keeps the lightweight chat controls focused on in-thread actions instead of a dedicated refresh icon.
- Added an `M` button immediately to the left of `T`. Tapping it opens an upward model picker that shows the agent's current model on the first line and the currently available model list below, matching the role that WebChat exposes through its `/model` command and model picker.
- The Android model picker is now session-backed instead of local-only. `WebChatController` loads `GET /api/openclaw-webchat/sessions/{sessionKey}/model-options`, stores the current model plus available models, and switches the active session through `PATCH /api/openclaw-webchat/sessions/{sessionKey}/model` with the selected `provider` and `model`.
- After a model switch succeeds, the Android client clears the cached thinking picker state so the next `T` open reloads the new model's valid thinking levels instead of reusing stale options from the previous model.
- Added focused MockWebServer coverage in `WebChatControllerTest` for the model picker fetch/switch flow alongside the existing send/stop and thinking-picker tests.
- Fresh verification completed on 2026-03-28 with:
- `./gradlew :app:testPlayDebugUnitTest --tests ai.openclaw.app.chat.WebChatControllerTest`
- `./gradlew :app:compilePlayDebugKotlin`
- `./gradlew :app:assemblePlayDebug`
- Installed the fresh APK onto the connected real device `c2f22adf` with:
- `adb -s c2f22adf install -r app/build/outputs/apk/play/debug/openclaw-0.2.4-play-debug.apk`
- Brought `ai.openclaw.app/.MainActivity` to the foreground after install so the user can test the new `M` model picker immediately.

## Chat Model Picker Follow-up Plan

- [x] Fix the Android model picker so WebChat model options are actually selectable when `available` is omitted from the payload.
- [x] Remove the model picker header copy and current-model line so the popup stays as a compact list-first chooser.
- [x] Rebuild, install to the connected real device, and update review notes.

## Chat Model Picker Follow-up Review

- Root cause of the unselectable `M` picker was Android-side option parsing, not the menu click handler: `WebChatController` treated a missing `available` field as `false`, which disabled every WebChat model option even though WebChat itself treats omitted `available` as selectable-by-default.
- Model option parsing now defaults missing `available` to `true`, so the menu can actually switch models when WebChat returns its normal payload shape without an explicit `available` flag on each item.
- The `M` popup has been simplified by removing the top current-model summary and explanatory copy. It now behaves as a compact list-first chooser, with only a short transient status line shown when loading, switching, or reporting an error.
- Added a tighter assertion to `WebChatControllerTest` so the model picker regression coverage now explicitly verifies that fetched model options remain selectable when `available` is omitted from the payload.
- Fresh verification completed on 2026-03-28 with:
- `./gradlew :app:testPlayDebugUnitTest --tests ai.openclaw.app.chat.WebChatControllerTest`
- `./gradlew :app:compilePlayDebugKotlin`
- `./gradlew :app:assemblePlayDebug`
- Installed the fresh APK onto the connected real device `c2f22adf` with:
- `adb -s c2f22adf install -r app/build/outputs/apk/play/debug/openclaw-0.2.4-play-debug.apk`
- Brought `ai.openclaw.app/.MainActivity` to the foreground after install so the user can immediately retest model selection on device.

## Chat Picker Stay-Open Plan

- [x] Change the `M` and `T` picker interactions so selecting an item does not auto-dismiss the popup.
- [x] Keep popup dismissal user-driven by outside tap only, while preserving the in-menu success feedback through the updated active-state highlight.
- [x] Rebuild, install to the connected real device, and update review notes.

## Chat Picker Stay-Open Review

- The `M` and `T` picker rows no longer auto-dismiss after a selection. Choosing a model or thinking level now leaves the popup open so the user can confirm the new active highlight before deciding to tap outside and close it.
- Popup dismissal is now fully user-driven by outside-tap dismissal. The in-menu success confirmation comes from the active option styling updating in place once the switch succeeds, instead of the menu disappearing immediately after the tap.
- Fresh verification completed on 2026-03-28 with:
- `./gradlew :app:testPlayDebugUnitTest --tests ai.openclaw.app.chat.WebChatControllerTest`
- `./gradlew :app:compilePlayDebugKotlin`
- `./gradlew :app:assemblePlayDebug`
- Installed the fresh APK onto the connected real device `c2f22adf` with:
- `adb -s c2f22adf install -r app/build/outputs/apk/play/debug/openclaw-0.2.4-play-debug.apk`
- Brought `ai.openclaw.app/.MainActivity` to the foreground after install so the user can immediately retest the stay-open picker behavior on device.

## Public Release Review Plan

- [x] Inspect the current git/worktree state and identify the exact code under review for this public release candidate.
- [x] Review the recent chat composer, send/stop, thinking picker, and model picker changes for correctness, regressions, and WebChat contract alignment.
- [x] Check whether automated verification meaningfully covers the new release-critical behavior and note any gaps.
- [x] Report review findings ordered by severity, with file references and residual release risks.

## Public Release Fix Plan

- [x] Gate the Android stop, model, and thinking composer controls on WebChat server capability instead of enabling them unconditionally.
- [x] Replace the stale `Pull to refresh` composer error copy with a recovery instruction that matches the actual Android UI.
- [x] Add focused regression coverage for the capability gating path so older WebChat deployments keep the new controls disabled cleanly.
- [x] Rebuild, reinstall on the connected real device, and capture the release-fix review notes.

## Public Release Fix Review

- The new chat composer controls no longer assume every WebChat server is new enough to support them. Android now reads `projectInfo.version` from `GET /api/openclaw-webchat/settings` and only enables the stop, model, and thinking controls when the connected server is at least `v0.1.6`, which is the first published WebChat line that contains all three endpoints.
- The capability gate is enforced both in the UI and in `WebChatController` itself. That means older or partially upgraded WebChat deployments now fail closed: the buttons stay disabled and the controller will not fire unsupported `stop`, `model`, or `thinking` requests behind the UI.
- The stale composer helper text has been corrected. When chat service health is down, the Android input area now tells the user to reconnect the gateway or reopen the chat instead of instructing them to use a non-existent refresh affordance.
- Added focused regression coverage in `WebChatControllerTest` for the new version gate, and updated the existing stop/thinking/model controller tests so they explicitly opt into the supported-capability state they are validating.
- Fresh verification completed on 2026-03-28 with:
- `./gradlew :app:testPlayDebugUnitTest --tests ai.openclaw.app.chat.WebChatControllerTest`
- `./gradlew :app:compilePlayDebugKotlin`
- `./gradlew :app:assemblePlayDebug`
- Installed the fresh APK onto the connected real device `c2f22adf` with:
- `adb -s c2f22adf install -r /Users/memphis/.openclaw/workspace-mira/clawchat2/app/build/outputs/apk/play/debug/openclaw-0.2.4-play-debug.apk`
- Brought `ai.openclaw.app/.MainActivity` to the foreground after install so the public-release candidate can be retested immediately on device.

## Chat New Button Plan

- [x] Add a compact `N` control immediately to the right of the send button in the chat composer button row.
- [x] Wire the new button to execute the existing WebChat `/new` slash-command path without disturbing the rest of the composer controls.
- [x] Rebuild, install to the connected real device, and capture review notes for the new-button behavior.

## Chat New Button Review

- The chat composer button row now includes a compact `N` control immediately to the right of the send button, keeping the lightweight controls aligned in a single strip without adding a separate row.
- Tapping `N` now sends the existing `/new` slash command through the normal WebChat send path, so the feature reuses the server-side conversation-reset behavior instead of introducing a second Android-only reset implementation.
- The `N` button is only enabled when chat is healthy and idle, matching the other lightweight composer controls. It also closes any open `M` or `T` popup before dispatching `/new`, which keeps the button row interaction tidy.
- This implementation intentionally leaves any unsent text draft or pending attachment chips in place, so using `/new` resets the conversation context without silently discarding the user's current composer draft.
- Fresh verification completed on 2026-03-29 with:
- `./gradlew :app:testPlayDebugUnitTest --tests ai.openclaw.app.chat.WebChatControllerTest`
- `./gradlew :app:compilePlayDebugKotlin`
- `./gradlew :app:assemblePlayDebug`
- Installed the fresh APK onto the connected real device `c2f22adf` with:
- `adb -s c2f22adf install -r /Users/memphis/.openclaw/workspace-mira/clawchat2/app/build/outputs/apk/play/debug/openclaw-0.2.4-play-debug.apk`
- Brought `ai.openclaw.app/.MainActivity` to the foreground after install so the user can test the new `N` control immediately.

## Palette Alignment Pass Plan

- [x] Map the provided Stitch palette and `DESIGN.md` surface hierarchy onto the existing Android theme tokens, keeping this pass focused on colors and surface layering rather than behavior changes.
- [x] Update the shared mobile palette and Material color scheme so the app shifts from the current green-gray theme to the new deep navy, bright green, and cyan accent system.
- [x] Apply any minimal screen-level visual adjustments needed for chat and contacts so the new palette reads correctly in containers, bubbles, composer chrome, and selection states.
- [x] Rebuild, run the relevant verification commands, install the updated APK onto the connected real device `c2f22adf`, and capture review notes.

## Palette Alignment Pass Review

- Rebased the shared Android palette on the provided Stitch colors and `DESIGN.md` hierarchy: the app now uses a deep navy background/surface stack, brighter green primary accents, cyan tertiary accents, cooler slate text, and a subtle background gradient instead of the previous green-gray theme wash.
- Updated the shared Material color scheme so primary/secondary/tertiary roles map cleanly onto the new palette without changing behavior. This keeps buttons, toggles, focus states, error surfaces, and overlay chrome visually aligned with the new editorial direction.
- Tightened the most visible screen-level surfaces to match the new system: chat composer menus and input focus states now sit on darker layered containers, assistant/voice bubbles no longer fall back to white, empty/error cards read as tonal surfaces, and contacts now use card-like layered rows instead of divider-led separation.
- Removed the most obvious old-theme hardcoded colors from `ConnectTabScreen`, so the connection method chips and command blocks no longer flash the previous blue/green palette inside the new navy theme.
- Fresh verification completed on 2026-03-29 with:
- `./gradlew :app:testPlayDebugUnitTest`
- `./gradlew :app:compilePlayDebugKotlin`
- `./gradlew :app:assemblePlayDebug`
- Installed the fresh APK onto the connected real device `c2f22adf` with:
- `adb -s c2f22adf install -r /Users/memphis/.openclaw/workspace-mira/clawchat2/app/build/outputs/apk/play/debug/openclaw-0.2.4-play-debug.apk`
- Relaunched `ai.openclaw.app/.MainActivity` and confirmed the app process/activity came up on device with:
- `adb -s c2f22adf shell am start -n ai.openclaw.app/.MainActivity`
- `adb -s c2f22adf shell pidof ai.openclaw.app`
- `adb -s c2f22adf shell dumpsys activity activities | rg "mResumedActivity|topResumedActivity|ai.openclaw.app|MainActivity"`

## Chat Screenshot Palette Alignment Plan

- [x] Compare the newly provided light and dark chat screenshots against the current Android result and identify the remaining mismatches in bubble/background/composer color relationships.
- [x] Refine the shared light and dark palette values so the light theme reads as near-white with soft gray assistant surfaces, and the dark theme reads as near-black with ink-blue assistant surfaces.
- [x] Adjust chat bubble and composer colors so user messages feel like solid green reply blocks while assistant/system surfaces stay quieter and closer to the screenshots.
- [x] Rebuild, reinstall on `c2f22adf`, and capture review notes for this screenshot-based palette pass.

## Chat Screenshot Palette Alignment Review

- Tightened the light theme toward the supplied mock: the chat background now reads as near-white instead of cool tinted gray, assistant surfaces moved to a softer neutral-gray container, and metadata/secondary text shifted to calmer blue-gray values.
- Tightened the dark theme toward the supplied mock: the base background is now closer to near-black navy, assistant containers sit on a deeper ink-blue layer, and the supporting border/text values are less bright so the green reply bubbles remain the visual focus.
- Chat reply bubbles now match the references more closely: user messages use a solid green fill with dark text/metadata instead of the previous pale-green card treatment, while assistant/system bubbles keep quieter tonal surfaces with much softer borders.
- The composer input now sits on the same stronger container layer in both focused and unfocused states, which makes the bottom composer read more like the dark reference screenshot instead of a brighter standalone text field.
- Synced the same user-bubble color treatment into `VoiceTabScreen` so chat-adjacent conversation UI does not fall back to the previous pale-green style after this screenshot pass.
- Fresh verification completed on 2026-03-29 with:
- `./gradlew :app:testPlayDebugUnitTest`
- `./gradlew :app:compilePlayDebugKotlin`
- `./gradlew :app:assemblePlayDebug`
- Installed the fresh APK onto the connected real device `c2f22adf` with:
- `adb -s c2f22adf install -r /Users/memphis/.openclaw/workspace-mira/clawchat2/app/build/outputs/apk/play/debug/openclaw-0.2.4-play-debug.apk`
- Relaunched and verified the app on device with:
- `adb -s c2f22adf shell am start -n ai.openclaw.app/.MainActivity`
- `adb -s c2f22adf shell monkey -p ai.openclaw.app -c android.intent.category.LAUNCHER 1`
- `adb -s c2f22adf shell pidof ai.openclaw.app`
- `adb -s c2f22adf shell dumpsys activity activities | rg "mResumedActivity|topResumedActivity|ai.openclaw.app|MainActivity"`
- Follow-up tweak on 2026-03-29: user-bubble text was switched from white to dark text in both light and dark mode, and the same dark-text treatment was synced to `VoiceTabScreen`. Rebuilt `playDebug`, reinstalled on `c2f22adf`, and reverified foreground launch after the tweak.

## Release 0.2.5 Promotion Plan

- [x] Merge `codex/ui-palette-pass` into `main` so the screenshot-aligned chat palette becomes the new long-term baseline.
- [x] Bump Android app version to `0.2.5` / `versionCode 7`.
- [x] Update README, release notes, status docs, and onboarding home copy so users are told to first give the `claw-webchat` install guide to their agent: `https://github.com/memphislee09-source/claw-webchat/blob/main/docs/AGENT_INSTALL_NETWORK.md`
- [x] Run merged-baseline verification, build the public APK artifact, and install the verification build on device `c2f22adf`.
- [x] Push the new `main` baseline to the private GitHub repo, sync the same baseline to `clawchat2-public`, and publish the GitHub Release with APK + checksum.

## Release 0.2.5 Promotion Review

- Promoted `codex/ui-palette-pass` into `main` inside a clean release worktree so the screenshot-aligned palette pass becomes the new baseline without disturbing the user's other in-progress branch work.
- Bumped the Android app to `versionName 0.2.5` / `versionCode 7`, added `RELEASE_NOTES_v0.2.5.md`, and updated `README.md`, `status.md`, `RELEASING.md`, and `OnboardingFlow.kt` so both the repository homepage and the first-run in-app home screen tell users to first hand the `claw-webchat` install guide to their agent/operator.
- Sequential release verification completed successfully with `./gradlew :app:compilePlayDebugKotlin`, `./gradlew :app:testPlayDebugUnitTest`, `./gradlew :app:assemblePlayDebug`, and `./gradlew :app:assemblePlayRelease`.
- Installed `app/build/outputs/apk/play/debug/openclaw-0.2.5-play-debug.apk` onto the required Redmi K20 (`c2f22adf`), relaunched `ai.openclaw.app/.MainActivity`, and confirmed the app process was resumed in the foreground.
- Built public release artifact `app/build/outputs/apk/play/release/openclaw-0.2.5-play-release.apk` with SHA-256 `69258095f6651462158a199c5c375ae81d957e6590ff3250c8956036a76e5cea`, pushed `main` to both `origin` and `public`, and published the GitHub pre-release at `https://github.com/memphislee09-source/clawchat2-public/releases/tag/v0.2.5`.
- Because `clawchat2-public` had an older independent history, its previous `main` was preserved first as `archive/public-main-before-v0.2.5` before `public/main` was force-updated to the new shared baseline.

## K20 QR Pairing Failure Plan

- [x] Reconstruct the exact failure path for K20 QR pairing and collect the relevant existing pairing guidance from docs and task history.
- [x] Trace the Android setup-code / QR connection flow and identify why the approval `requestId` becomes invalid before `openclaw devices approve` succeeds.
- [x] Implement the narrowest fix or user-path correction based on evidence, without disturbing unrelated onboarding/chat behavior.
- [x] Re-run the most relevant verification for the failing pairing path and record the result here.

## K20 QR Pairing Failure Review

- Root-cause hypothesis was confirmed by code-path inspection and focused regression coverage: `GatewaySession.runLoop()` kept auto-retrying bootstrap-token connects after pairing-required failures, which could rotate pending pairing requests before the operator finished `openclaw devices approve <requestId>`.
- The old onboarding UI also amplified that failure mode by always telling the host-side operator to copy a literal `requestId`, even though the project docs already noted that rotating requests should be approved with `openclaw devices approve --latest`.
- `GatewaySession` now treats pairing-required connect failures as a pause point instead of a normal transient transport error. When the gateway says the device is awaiting approval, Android stops spinning new connect attempts until the user explicitly reconnects, which prevents the app from continuously invalidating the just-listed pending request.
- Added `GatewaySessionPairingTest` to prove the new behavior: a mocked `PAIRING_REQUIRED` connect response now yields exactly one connect attempt until `reconnect()` is called, after which the next connect attempt is allowed through.
- Updated onboarding and `OPENCLAW_AGENT_SETUP.md` to recommend `openclaw devices approve --latest` for the host-side recovery path, which matches the known request-rotation failure mode.
- Fresh verification completed on 2026-03-31 with:
- `./gradlew --stop`
- `./gradlew :app:testPlayDebugUnitTest --tests ai.openclaw.app.gateway.GatewaySessionPairingTest --tests ai.openclaw.app.node.ConnectionManagerTest`
- `./gradlew :app:compilePlayDebugKotlin`
- `./gradlew :app:assemblePlayDebug`
- `./gradlew :app:assemblePlayRelease`
- `adb -s c2f22adf install -r /Users/memphis/.openclaw/workspace-mira/clawchat2/app/build/outputs/apk/play/release/openclaw-0.2.5-play-release.apk`
- `adb -s c2f22adf shell am start -n ai.openclaw.app/.MainActivity`
- `adb -s c2f22adf shell monkey -p ai.openclaw.app -c android.intent.category.LAUNCHER 1`
- `adb -s c2f22adf shell dumpsys activity activities | rg "mResumedActivity|topResumedActivity|ai.openclaw.app|MainActivity"`
- `adb -s c2f22adf shell dumpsys window | rg "mCurrentFocus|mFocusedApp|ai.openclaw.app|MainActivity"`
- Device-install note: `playDebug` could not overwrite the existing K20 install because the phone was already running a release-signed package, so the verified on-device retest artifact for this pass is the freshly rebuilt release-signed APK `app/build/outputs/apk/play/release/openclaw-0.2.5-play-release.apk`.
- Manual end-to-end QR pairing on the real gateway is still pending from the user after install; this pass verified the retry suppression behavior in unit tests plus release build/install/launch readiness on device.

## First-Launch Language Picker Plan

- [x] Inspect the current app-wide translation mechanism and onboarding entry path.
- [x] Add a first-page language choice on fresh install and persist the selected app language for later screens.
- [x] Verify the updated onboarding/language path with focused build checks and record the result here.

## First-Launch Language Picker Review

- ClawChat2 already had persistent app-language support through `AppLanguageManager` and `SecurePrefs`, but the entry point was buried in Settings. The first-run path now surfaces language choice immediately on the welcome page.
- The first onboarding page now opens with a dedicated language section offering `Follow system`, `English`, and `Chinese`. Choosing an option saves `app.language` and immediately recreates the activity so later onboarding pages and the rest of the app switch to the selected language without waiting for a manual restart.
- To avoid a half-finished experience where the user picks a language and the next onboarding page stays English, the main onboarding chrome and core step copy were also moved onto `tr(...)` bilingual rendering for the welcome, gateway, permissions, and final-check path.
- Fresh verification completed on 2026-03-31 with:
- `./gradlew :app:compilePlayDebugKotlin`
- `./gradlew :app:assemblePlayRelease`
- `adb -s c2f22adf install -r /Users/memphis/.openclaw/workspace-mira/clawchat2/app/build/outputs/apk/play/release/openclaw-0.2.5-play-release.apk`
- `adb -s c2f22adf shell am start -n ai.openclaw.app/.MainActivity`
- `adb -s c2f22adf shell monkey -p ai.openclaw.app -c android.intent.category.LAUNCHER 1`
- `adb -s c2f22adf shell dumpsys window | rg "mCurrentFocus|mFocusedApp|ai.openclaw.app|MainActivity"`
- `adb -s c2f22adf shell pidof ai.openclaw.app`
- Installed verification artifact for the Redmi K20 (`c2f22adf`): `app/build/outputs/apk/play/release/openclaw-0.2.5-play-release.apk` built at `2026-03-31 08:52:25 +0800`.

## Release 0.2.6 Plan

- [x] Bump the Android app and release-facing docs from `0.2.5` to `0.2.6`.
- [x] Update the private/public repo homepage content with the provided Bilibili intro video link and replace the old screenshots with the provided single composite screenshot.
- [x] Document the pairing retry suppression and first-launch language picker in the new release notes and project status docs.
- [x] Build and verify fresh `playDebug` / `playRelease` artifacts for `0.2.6`, including a real-device install of the release-signed APK.
- [x] Commit the release changes, push `main` to the private repo, sync the same baseline to the public repo, and publish/update the public GitHub Release with the new APK and checksum.

## Release 0.2.6 Review

- Bumped the Android app to `versionName 0.2.6` / `versionCode 8`, added `RELEASE_NOTES_v0.2.6.md`, refreshed `README.md`, `status.md`, `RELEASING.md`, and synced the promo-copy files so the release-facing docs all point at the same `0.2.6` baseline.
- The private/public homepage now includes the requested Bilibili intro video link, and the old two-screenshot homepage set was replaced by the provided single composite preview image at `docs/images/home-preview-v0.2.6.png`.
- The new public release line explicitly documents both release-facing functional changes from this pass: pairing-request retry suppression while approval is pending, and the first-launch app-language choice on the onboarding welcome screen.
- Fresh verification completed on 2026-03-31 with:
- `./gradlew --stop`
- `./gradlew :app:testPlayDebugUnitTest --tests ai.openclaw.app.gateway.GatewaySessionPairingTest --tests ai.openclaw.app.node.ConnectionManagerTest`
- `./gradlew :app:compilePlayDebugKotlin :app:assemblePlayRelease`
- `./gradlew :app:assemblePlayDebug`
- `shasum -a 256 /Users/memphis/.openclaw/workspace-mira/clawchat2/app/build/outputs/apk/play/release/openclaw-0.2.6-play-release.apk`
- `adb -s c2f22adf install -r /Users/memphis/.openclaw/workspace-mira/clawchat2/app/build/outputs/apk/play/release/openclaw-0.2.6-play-release.apk`
- `adb -s c2f22adf shell am start -W -n ai.openclaw.app/.MainActivity`
- `adb -s c2f22adf shell monkey -p ai.openclaw.app -c android.intent.category.LAUNCHER 1`
- `adb -s c2f22adf shell dumpsys activity activities | rg "mResumedActivity|topResumedActivity|ai.openclaw.app|MainActivity"`
- `adb -s c2f22adf shell pidof ai.openclaw.app`
- Verified public release artifact: `app/build/outputs/apk/play/release/openclaw-0.2.6-play-release.apk`, built at `2026-03-31 09:13:22 +0800`, SHA-256 `b300097f352e75993ccf65fe569266a0e02d9cd0a69b5bcae1dbb6d84d158af9`.
- Pushed the same `main` baseline to both the private and public GitHub repositories, then published the public release at `https://github.com/memphislee09-source/clawchat2-public/releases/tag/v0.2.6`.

## Chat Video First-Frame Preview Plan

- [x] Inspect the current inline video attachment preview path and confirm why streamed chat videos fall back to a black placeholder.
- [x] Add first-frame preview support for the same streamed video source that already works in fullscreen playback.
- [x] Rebuild and verify the updated chat attachment rendering path, then record the result here.

## Chat Video First-Frame Preview Review

- Root cause was in `ChatVideoAttachment`: inline preview extraction only ran when the video had already been materialized as a local fallback file. For the normal streaming path, `streamUrl` skipped `rememberResolvedMediaFileState`, which left `previewState` null and forced the card into the black placeholder branch even though fullscreen playback could already open the same video URL.
- The fix keeps fullscreen playback unchanged and only widens the preview source. `previewState` now derives from `streamUrl ?: fallbackFile?.absolutePath`, and `rememberMediaPreviewState` now accepts a generic source string instead of only a local `File`, so the existing `MediaMetadataRetriever` preview path can decode the first frame from the streamed video source as well.
- Fresh verification completed on 2026-03-31 with:
- `./gradlew :app:compilePlayDebugKotlin :app:assemblePlayDebug`
- `./gradlew :app:assemblePlayRelease`
- `adb -s c2f22adf install -r /Users/memphis/.openclaw/workspace-mira/clawchat2/app/build/outputs/apk/play/release/openclaw-0.2.6-play-release.apk`
- `adb -s c2f22adf shell dumpsys activity activities | rg "mResumedActivity|topResumedActivity|ai.openclaw.app|MainActivity"`
- `adb -s c2f22adf shell pidof ai.openclaw.app`
- Verified install artifact for the Redmi K20 (`c2f22adf`): `app/build/outputs/apk/play/release/openclaw-0.2.6-play-release.apk` built at `2026-03-31 11:32:46 +0800`.
- User confirmed on 2026-03-31 that the chat video card now shows the expected first frame instead of the previous black placeholder.
