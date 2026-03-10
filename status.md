# clawchat2 status

Last updated: 2026-03-10 (Asia/Shanghai)

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

## Feature Update (2026-03-10, pass-2)
- Chinese localization pass expanded: major shell/connect/settings labels switched to Chinese when language is Chinese.
- Language option persists and recreates activity for immediate UI switch.
- Tailscale connectivity integrated with persistence:
  - Stored host + port (`gateway.tailscale.host` / `gateway.tailscale.port`)
  - Default host: `100.103.47.113`
  - Connect priority: LAN discovery first, fallback to Tailscale on LAN failure, otherwise Offline.
- Tailscale host/port now saved when connecting.

## Next Actions
- Brand rename pass (app name / package id / icon / default gateway config) when requested.
- Establish standard upstream sync checklist script if needed.
