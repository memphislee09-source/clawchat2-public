# Agent Media Send

If you are sending image, audio, or video into ClawChat2, use the local sender script.
Do not hand-build `mediaUrl` unless you have no other option.

Script:

```bash
./scripts/send-clawchat-media.mjs
```

Default usage:

```bash
./scripts/send-clawchat-media.mjs \
  --agent main \
  --file /absolute/path/to/file \
  --message "Optional caption"
```

What the script does:

- writes an `assistant` media message directly into session `agent:<agentId>:clawchat2`
- stores the real file in the local media store
- writes stable gateway-relative fields:
  - `mediaPath`
  - `mediaPort`
- also writes legacy compatibility field:
  - `mediaUrl`

Current rule:

- current ClawChat2 builds resolve media primarily from `mediaPath + mediaPort + current gateway host`
- `mediaUrl` is only a fallback for older builds or older messages
- in normal use, do not worry about LAN vs Tailscale switching if the media server runs on the same host as the gateway
- only use `--public-host <LAN-or-tailnet-host>` when you explicitly need a legacy absolute `mediaUrl` to point somewhere else

Hard requirements:

- use absolute file paths
- target session key must be `agent:<agentId>:clawchat2`
- supported media kinds are `image`, `audio`, `video`
- `mimeType` must be the real MIME type
- do not send markdown image syntax
- do not send URL-only media
- do not send `data:` URIs

Examples:

```bash
./scripts/send-clawchat-media.mjs \
  --agent main \
  --file /tmp/photo.jpg \
  --message "Please see the image."
```

```bash
./scripts/send-clawchat-media.mjs \
  --agent main \
  --file /tmp/voice.mp3 \
  --message "Please play this audio."
```

```bash
./scripts/send-clawchat-media.mjs \
  --agent main \
  --file /tmp/clip.mp4 \
  --message "Please see this video."
```

If you cannot call the script and must write structured content directly, use this shape:

```json
[
  { "type": "text", "text": "Optional caption" },
  {
    "type": "image|audio|video",
    "mimeType": "real MIME type",
    "fileName": "original file name",
    "mediaPath": "/media/<token>",
    "mediaPort": 39393,
    "mediaUrl": "http://10.0.2.2:39393/media/<token>",
    "mediaSha256": "<sha256>",
    "sizeBytes": 123456
  }
]
```

Direct-payload rules:

- `mediaPath` + `mediaPort` are the important fields
- keep `mediaUrl` only as a compatibility hint
- `mediaUrl` may point to `10.0.2.2` for emulator fallback, but current ClawChat2 builds should not depend on it

Optional script overrides:

- `--session-key agent:<agentId>:clawchat2`
- `--type image|audio|video`
- `--mime <mimeType>`
- `--name <fileName>`
- `--sender <label>`
- `--public-host <host>`
- `--port <port>`
- `--store-dir <path>`

If ClawChat2 is already open on the target chat, pull down to refresh once after sending.
