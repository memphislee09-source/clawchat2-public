# Agent Media Server

If ClawChat2 media stops loading after the local OpenClaw host restarts, recover the local media HTTP server before debugging Android.

Use this document as the operational source of truth for starting the server on macOS.

## Target Service

- launchd label: `ai.openclaw.clawchat-media-server`
- health URL: `http://127.0.0.1:39393/health`
- plist path: `~/Library/LaunchAgents/ai.openclaw.clawchat-media-server.plist`
- log path: `~/.openclaw/clawchat-media-store/server.log`
- server script: `./scripts/clawchat-media-server.mjs`

## Fast Path

1. Check health:

```bash
curl -sf http://127.0.0.1:39393/health
```

2. If that returns JSON with `"ok":true`, stop there. The media server is already running.

3. If the health check fails, restart the launchd service:

```bash
launchctl kickstart -k gui/$(id -u)/ai.openclaw.clawchat-media-server
curl -sf http://127.0.0.1:39393/health
```

## Full Recovery

If `kickstart` fails or health is still down, refresh the LaunchAgent and try again:

```bash
launchctl bootout gui/$(id -u) ~/Library/LaunchAgents/ai.openclaw.clawchat-media-server.plist 2>/dev/null || true
launchctl bootstrap gui/$(id -u) ~/Library/LaunchAgents/ai.openclaw.clawchat-media-server.plist
launchctl kickstart -k gui/$(id -u)/ai.openclaw.clawchat-media-server
curl -sf http://127.0.0.1:39393/health
```

Expected healthy response shape:

```json
{"ok":true,"host":"0.0.0.0","port":39393,"storeDir":"<media-store-dir>"}
```

## If The Plist Is Missing

The LaunchAgent is normally created and refreshed by the media sender script:

```bash
./scripts/send-clawchat-media.mjs \
  --agent main \
  --file /absolute/path/to/file
```

That script rewrites `~/Library/LaunchAgents/ai.openclaw.clawchat-media-server.plist`, bootstraps it, and waits for the server to become healthy.

If you only need to restore server startup behavior, do not hand-edit the plist unless you also update `scripts/send-clawchat-media.mjs`.

## Verification

Use these checks after recovery:

```bash
launchctl list | rg 'ai\.openclaw\.clawchat-media-server'
curl -sf http://127.0.0.1:39393/health
tail -n 80 ~/.openclaw/clawchat-media-store/server.log
```

Success means:

- `launchctl list` shows `ai.openclaw.clawchat-media-server`
- health returns JSON with `"ok":true`
- the log does not show repeated startup crashes

## Agent Rule

When media is not loading in ClawChat2 on this machine:

1. Check `http://127.0.0.1:39393/health`.
2. If unhealthy, run `launchctl kickstart -k gui/$(id -u)/ai.openclaw.clawchat-media-server`.
3. If still unhealthy, run the full `bootout -> bootstrap -> kickstart` sequence above.
4. Only after the server is healthy should you debug the Android app or the gateway payload.
