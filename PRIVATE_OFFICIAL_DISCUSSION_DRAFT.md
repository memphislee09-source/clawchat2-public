# Private Official Discussion Draft

## Final GitHub Discussion Draft

### Suggested Title

`Would OpenClaw be open to discussing a chat-first Android mode or client surface?`

### Post-Ready Body

Hi OpenClaw team,

I have been exploring an independent Android fork based on `openclaw/openclaw -> apps/android`, mainly to test a narrower use case: users who want Android primarily as a direct, chat-first surface for talking to their OpenClaw agents.

I am not asking whether the project should adopt the whole fork. The question is narrower:

Would OpenClaw be open to discussing either:

1. a chat-first mode or client surface inside the official Android app
2. or, if that is too large, selective upstreaming of a few validated Android UX improvements?

The reason I am asking is that I keep seeing the same user need:

- open the app and get to agent chat quickly
- less setup friction and less navigation overhead
- practical gateway connectivity, including LAN and Tailscale scenarios
- reliable image/audio/video handling on Android

In the fork, a few concrete changes have worked well:

- chat-first navigation and conversation emphasis
- stronger media receive/render support
- more stable fullscreen image/video behavior
- Tailscale-friendly connection UX
- restored compatibility with official setup-code pairing flows by supporting `bootstrapToken`

These changes have been validated on emulator and real Android hardware.

The main thing I want to understand is whether this direction is interesting upstream at all. If yes, I would be happy to reduce the conversation to one or two smaller proposals instead of discussing the fork as a whole.

If the answer is no, that is also useful and clear. In that case I would still be interested in whether smaller Android bug fixes or compatibility fixes would be welcome as focused PRs.

Thanks.

## Shorter Version

Hi OpenClaw team,

I have been exploring an independent chat-first Android fork of the OpenClaw Android client. I am not asking whether the project should adopt the whole fork, but I would like to ask whether maintainers would be open to discussing either:

1. a chat-first mode / surface inside the official Android app
2. or selective upstreaming of a few validated Android UX improvements

The fork has mainly been useful for:

- simpler direct agent chat
- stronger Android media handling
- Tailscale-friendly connection UX
- restored setup-code pairing compatibility via `bootstrapToken`

If this direction is interesting, I can follow up with a much smaller proposal and screenshots.

## 中文说明

建议对官方发帖时优先使用上面的英文 `Post-Ready Body`。

这版已经是压缩后的最终稿，核心原则是：

- 先讨论方向，不谈合并整个 fork
- 先问官方是否对 chat-first Android 模式感兴趣
- 如果有兴趣，再继续缩小到 1 到 2 个具体提案

## 发送建议

优先顺序：

1. GitHub Discussion
2. Discord

如果先发 Discord，建议使用 `Shorter Version` 再附一句：

`If this sounds relevant, I can open a focused GitHub Discussion with screenshots and a short summary.`
