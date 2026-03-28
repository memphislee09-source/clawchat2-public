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
- Separate follow-up decision: if the project later resumes true Android voice conversation work, then the Android client should migrate that voice path onto the new WebChat `/events` + `/turns` + `/abort` interfaces and retire the remaining gateway-direct voice plumbing.

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
