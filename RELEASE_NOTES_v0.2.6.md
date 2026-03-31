# ClawChat2 v0.2.6 Release Notes

## English

This release refreshes the public package around the current `main` baseline, adds a first-screen language choice for new installs, and removes the pairing-request churn that could race against CLI approval during QR/setup-code onboarding.

ClawChat2 remains an unofficial OpenClaw Android fork centered on a simple, direct, chat-first experience for talking with OpenClaw agents.

Highlights in this build:

- the first onboarding screen now starts with an explicit app-language choice, so users can pick `Follow system`, `English`, or `Chinese` before the rest of setup continues
- Android now stops automatic reconnect churn while pairing approval is pending, so `openclaw devices approve --latest` no longer loses the request to repeated retries during setup-code onboarding
- the setup guidance now consistently recommends `openclaw devices approve --latest` when pending pairing request IDs are rotating
- the repository homepage now links to the new intro video and uses the refreshed `0.2.6` preview image instead of the older screenshot set

Important notes:

- this is still an early `Pre-release`
- this is not an official OpenClaw build
- `openclaw-webchat` remains the source of truth for contacts and chat history
- before setup, users should ask their agent/operator to follow `https://github.com/memphislee09-source/claw-webchat/blob/main/docs/AGENT_INSTALL_NETWORK.md`

Install:

- download the attached APK
- allow installation from this source if Android prompts for it
- launch `ClawChat2`

## 中文

这个版本以当前 `main` 基线重新整理公开发布包，补上了新安装首屏语言选择，并修复了二维码 / setup-code 配对时因 Android 自动重连导致 CLI 审批请求不断轮换的问题。

ClawChat2 仍然是一个非官方 OpenClaw Android 分叉，目标是在 Android 上以更简单、更直接、聊天优先的方式与 OpenClaw agent 对话。

本版本重点包括：

- onboarding 首屏现在先提供应用语言选择，用户可以在继续设置前选择 `跟随系统`、`English` 或 `中文`
- 当配对审批仍在等待中时，Android 端现在会暂停自动重连，避免 setup-code onboarding 过程中不断刷新 pending request，导致 `openclaw devices approve --latest` 之前的请求很快失效
- 配对说明文档现已统一建议：如果 pending request ID 一直轮换，优先使用 `openclaw devices approve --latest`
- 仓库首页现已加入新的介绍视频链接，并用新的 `0.2.6` 预览图替换了旧版截图组

重要说明：

- 当前版本仍属于早期 `Pre-release`
- 这不是官方 OpenClaw 构建版本
- 联系人与聊天历史的服务端真源仍然是 `openclaw-webchat`
- 开始设置前，应先让 agent/operator 按照这个文档完成 `claw-webchat` 服务端安装：
  `https://github.com/memphislee09-source/claw-webchat/blob/main/docs/AGENT_INSTALL_NETWORK.md`

安装方式：

- 下载附带的 APK
- 如果 Android 提示，请允许从当前来源安装
- 启动 `ClawChat2`
