# Upstream Outreach Draft

## English

This draft is for proposing selected ClawChat2 improvements to the official OpenClaw project without implying that the full fork should be merged as-is.

### Recommended Position

Do not ask maintainers to adopt the entire fork.

Ask instead whether they are open to reviewing a small set of isolated Android improvements that were developed while building a chat-focused fork intended to make direct agent conversation feel simpler and less procedurally heavy on Android.

### Short Intro Draft

Hi OpenClaw team,

I have been developing an unofficial Android fork based on `openclaw/openclaw -> apps/android` to explore a simpler, chat-first Android experience for users who mostly want direct conversations with OpenClaw agents, without extra setup, registration, or shell complexity beyond what is needed to connect to their own gateway.

While doing that work, I ended up with a few Android-specific fixes and improvements that may have value outside the fork as well. I am not proposing that the full fork be merged as-is. Instead, I would like to split out a small number of focused Android changes and submit them individually if they match your direction.

The main areas I think may be relevant are:

- simpler direct-chat-oriented Android UX
- Android media rendering and playback hardening
- fullscreen image/video stability improvements
- selected gateway connection compatibility fixes, including practical Tailscale-friendly usage

## 中文

这份草稿用于和 OpenClaw 官方沟通时，说明 ClawChat2 中哪些改进值得单独讨论，而不是把整个分叉作为一个整体去请求官方接收。

### 建议立场

不要用“请接收整个 fork”的方式沟通。

更合适的方式是询问维护者：是否愿意评估一小组可以独立拆分的 Android 改进。这些改进是在构建一个聊天优先分叉时验证出来的，目标是让 Android 上与 agent 的直接对话更简单、更少流程负担。

### 简短沟通草稿

Hi OpenClaw team,

I have been developing an unofficial Android fork based on `openclaw/openclaw -> apps/android` to explore a simpler, chat-first Android experience for users who mostly want direct conversations with OpenClaw agents, without extra setup, registration, or shell complexity beyond what is needed to connect to their own gateway.

While doing that work, I ended up with a few Android-specific fixes and improvements that may have value outside the fork as well. I am not proposing that the full fork be merged as-is. Instead, I would like to split out a small number of focused Android changes and submit them individually if they match your direction.

The main areas I think may be relevant are:

- simpler direct-chat-oriented Android UX
- Android media rendering and playback hardening
- fullscreen image/video stability improvements
- selected gateway connection compatibility fixes, including practical Tailscale-friendly usage
