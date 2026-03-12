# Agent Media Send

Preferred method: call the local script directly.

Script:

```bash
/Users/memphis/.openclaw/workspace-mira/clawchat2/scripts/send-clawchat-media.mjs
```

Use it like this:

```bash
/Users/memphis/.openclaw/workspace-mira/clawchat2/scripts/send-clawchat-media.mjs \
  --agent main \
  --file /absolute/path/to/file \
  --message "Optional caption"
```

Rules:

- default session is `agent:<agentId>:clawchat2`
- script writes an `assistant` media message directly into the ClawChat2 session transcript
- script stores the real file in a local media store and puts a `mediaUrl` reference into the message
- use absolute file paths
- supported media kinds: `image`, `audio`, `video`
- type and MIME are auto-detected from the file name; override only if needed
- emulator default host is `10.0.2.2`
- for a real device, override `--public-host <LAN-or-tailnet-host>`

Image example:

```bash
/Users/memphis/.openclaw/workspace-mira/clawchat2/scripts/send-clawchat-media.mjs \
  --agent main \
  --file /tmp/photo.jpg \
  --message "Please see the image."
```

Audio example:

```bash
/Users/memphis/.openclaw/workspace-mira/clawchat2/scripts/send-clawchat-media.mjs \
  --agent main \
  --file /tmp/voice.mp3 \
  --message "Please play this audio."
```

Video example:

```bash
/Users/memphis/.openclaw/workspace-mira/clawchat2/scripts/send-clawchat-media.mjs \
  --agent main \
  --file /tmp/clip.mp4 \
  --message "Please see this video."
```

Optional overrides:

- `--session-key agent:<agentId>:clawchat2`
- `--type image|audio|video`
- `--mime <mimeType>`
- `--name <fileName>`
- `--sender <label>`
- `--public-host <host>`
- `--port <port>`
- `--store-dir <path>`

If ClawChat2 is already open on the target chat, pull down to refresh once after sending.
