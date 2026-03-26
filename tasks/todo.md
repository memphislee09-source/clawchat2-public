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
- [ ] Merge `codex/upstream-bridge-pass` into `main` as the new development baseline.
- [ ] Verify local `main` points at the tested onboarding/Tailscale/pairing fix set.
- [ ] Push updated `main` to GitHub and keep future work based on it.

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
