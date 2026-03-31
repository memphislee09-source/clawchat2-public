# ClawChat2 v0.2.7 Release Notes

## English

This release promotes verified attachment downloads into the main public Android build, so agent-sent images, audio, video, and regular files can now be saved out to the phone instead of staying trapped inside app-private cache.

ClawChat2 remains an unofficial OpenClaw Android fork centered on a simple, direct, chat-first experience for talking with OpenClaw agents.

Highlights in this build:

- agent-sent image, audio, video, and file attachments can now be downloaded to Android public storage directly from the chat UI
- saved attachments are routed into the matching Android media collections: `Pictures/ClawChat2`, `Movies/ClawChat2`, `Music/ClawChat2`, and `Download/ClawChat2`
- downloaded attachments expose an immediate open action so users can jump into the system viewer or file app after saving
- the existing chat media pipeline is preserved: previewing, fullscreen image viewing, audio playback, and fullscreen video playback continue to use the current attachment-resolution flow

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

这个版本把已经验证通过的附件下载能力正式提升进主线公开 Android 构建，agent 回复中的图片、音频、视频和普通文件现在都可以直接保存到手机，而不再只停留在 app 私有缓存里。

ClawChat2 仍然是一个非官方 OpenClaw Android 分叉，目标是在 Android 上以更简单、更直接、聊天优先的方式与 OpenClaw agent 对话。

本版本重点包括：

- agent 回复中的图片、音频、视频和普通文件现在都可以直接从聊天界面下载到 Android 公共存储
- 已保存的附件会按类型进入对应系统目录：`Pictures/ClawChat2`、`Movies/ClawChat2`、`Music/ClawChat2` 和 `Download/ClawChat2`
- 附件下载完成后会直接提供打开入口，用户可以立即跳转到系统查看器或文件管理器
- 现有聊天媒体链路保持不变：图片预览、图片全屏、音频播放和视频全屏播放仍然沿用当前附件解析路径

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
