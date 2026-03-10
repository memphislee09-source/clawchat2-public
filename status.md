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

## Feature Update (2026-03-10, pass-3)
- Chinese localization pass expanded further for shell/connect/settings key labels.
- Tailscale routing revised for compatibility with previous ClawChat behavior:
  - Tailscale default host: `100.103.47.113`
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

## Next Actions
- Brand rename pass (app name / package id / icon / default gateway config) when requested.
- Establish standard upstream sync checklist script if needed.
