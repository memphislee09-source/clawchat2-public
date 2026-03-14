# Private Official Discussion Draft

## English Draft

Title:

`Would OpenClaw be open to discussing a chat-first Android client or mode?`

Body:

Hi OpenClaw team,

I have been working on an independent Android fork derived from `openclaw/openclaw -> apps/android`, mainly to explore a simpler, more direct, chat-first mobile experience for users whose primary goal is just to open the app and talk to their OpenClaw agents.

I am not asking whether the project should adopt the whole fork. The question is narrower:

Would OpenClaw be open to discussing either:

1. a chat-first Android client direction inside the official project
2. or a lighter chat-first mode / surface within the existing Android app
3. or, if that is too large, selective upstreaming of a few validated Android UX improvements?

The reason I am asking is that I keep seeing a specific use case:

- some users want Android primarily as a direct agent chat surface
- they want less setup friction and less navigation overhead
- they still need practical gateway connectivity, including LAN and Tailscale scenarios
- they also care a lot about reliable media handling on Android

In the fork, a few concrete improvements have been useful:

- chat-first navigation and conversation emphasis
- stronger image/audio/video receive and render handling
- more stable fullscreen image/video behavior on Android
- practical Tailscale-friendly connection UX
- restored compatibility with official setup-code pairing flows by supporting `bootstrapToken`

This has all been validated on real Android hardware as well as emulator testing.

The main thing I want to understand is whether this overall direction is interesting upstream at all.

If the answer is yes, I would be happy to reduce the conversation to one or two smaller proposals instead of discussing the fork as a whole.

If the answer is no, that is also useful and clear.

Either way, I would appreciate guidance on whether:

- this should remain an external fork only
- a chat-first mode is worth discussing
- or a few smaller Android improvements would be welcome as focused PRs

Thanks.

## 中文说明

建议发给官方时以英文版本为主。

这份草稿的核心意图是：

- 先讨论方向，不先谈合并整个 fork
- 不要求官方接受我们的全部品牌和产品路线
- 先判断官方是否愿意讨论“聊天优先 Android 客户端 / 模式”
- 如果官方愿意，再把话题缩成几个小提案

### 发送建议

优先顺序：

1. GitHub Discussion
2. Discord

如果先发 Discord，建议先发一个短版：

`I have been exploring an independent chat-first Android fork of the OpenClaw Android client. Not asking whether the project should adopt the whole fork, but would maintainers be open to discussing either a chat-first Android mode or a few selective Android UX improvements validated in that fork? If yes, I can open a focused GitHub Discussion with screenshots and a short summary.`
