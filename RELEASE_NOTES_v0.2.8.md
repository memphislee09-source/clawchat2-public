# ClawChat2 v0.2.8 Release Notes

## English

This release promotes the recent contacts/chat visual polish pass into the main public Android baseline and makes that refreshed UI the new foundation for future ClawChat2 development.

ClawChat2 remains an unofficial OpenClaw Android fork centered on a direct, chat-first experience for talking with OpenClaw agents.

Highlights in this build:

- refreshed Contacts styling in light mode with stronger row contrast, clearer active-state emphasis, and better avatar/list layering
- refreshed Chat styling in light mode with cleaner surface separation between transcript, bubbles, and composer
- speaker name and timestamp now sit outside each message bubble at the top-left instead of consuming the first line inside the bubble
- single-line plain-text message bubbles now shrink to content width, while multi-line messages and attachment-bearing messages keep the wider layout
- dark-mode composer surfaces now follow the active theme correctly instead of reusing light-mode container colors

Important notes:

- this is now the current public release
- this is not an official OpenClaw build
- `openclaw-webchat` remains the source of truth for contacts and chat history
- before setup, users should ask their agent/operator to follow `https://github.com/memphislee09-source/claw-webchat/blob/main/docs/AGENT_INSTALL_NETWORK.md`

Install:

- download the attached APK
- allow installation from this source if Android prompts for it
- launch `ClawChat2`

## 中文

这个版本把最近这一轮联系人页与聊天页的视觉收口正式提升为新的公开 Android 基线，并把这套界面作为后续 ClawChat2 继续开发的起点。

ClawChat2 仍然是一个非官方 OpenClaw Android 分叉，目标是在 Android 上以更简单、更直接、聊天优先的方式与 OpenClaw agent 对话。

本版本重点包括：

- 浅色模式下的联系人页继续增强了层次，对比更明确，当前联系人强调更清楚，头像与列表层次也更干净
- 浅色模式下的聊天页进一步收口，聊天记录、气泡和输入区之间的表层关系更清楚
- 每条消息的发言人与时间现在移到气泡外左上角，不再占用气泡内第一行
- 只有单行纯文本消息会按内容宽度收缩；多行文本消息和带附件的消息继续保持更宽的布局
- 深色模式下的输入区整体表层现在会正确跟随主题，不再沿用浅色模式底色

重要说明：

- 当前版本现已作为当前公开发行版
- 这不是官方 OpenClaw 构建版本
- 联系人与聊天历史的服务端真源仍然是 `openclaw-webchat`
- 开始设置前，应先让 agent/operator 按照这个文档完成 `claw-webchat` 服务端安装：
  `https://github.com/memphislee09-source/claw-webchat/blob/main/docs/AGENT_INSTALL_NETWORK.md`

安装方式：

- 下载附带的 APK
- 如果 Android 提示，请允许从当前来源安装
- 启动 `ClawChat2`
