# ClawChat2 v0.2.4 Release Notes

## English

This release focuses on making direct agent chat feel faster and more natural on Android.

ClawChat2 remains an unofficial OpenClaw Android fork centered on a simple, direct, chat-first experience for talking with OpenClaw agents.

Highlights in this build:

- contact-to-chat entry now opens the selected conversation more reliably without stale-session jumps
- known conversations can render recent local history immediately instead of waiting through a blank load
- entering a conversation from Contacts now lands at the real conversation bottom, including chats where the last message is long
- fullscreen images now support pinch zoom and drag
- the chat composer now starts at a single-line height and grows with message length
- chat replies can now be read aloud through a lightweight speaker toggle in the composer action row, with Android system TTS verified on a real Huawei Mate60
- the chat attachment button can now upload images, audio, video, and regular files, with generic file cards rendered directly in the transcript
- the composer action row now also exposes direct send/stop, `/new`, model, and thinking controls so common WebChat chat actions do not require manual slash-command entry
- the chat transcript now uses a denser left-aligned bubble layout with clearer color separation, including white agent bubbles for stronger visual distinction
- large chat images now render flush inside the bubble body when space allows, smaller images keep their natural size, and fullscreen viewing now supports tap-to-open plus tap-to-dismiss
- the composer quick controls are now borderless icon-only actions so attachment, new chat, model, thinking, readout, and send/stop all stay visible in one row

Important notes:

- this is still an early `Pre-release`
- this is not an official OpenClaw build
- `openclaw-webchat` remains the source of truth for contacts and chat history

Install:

- download the attached APK
- allow installation from this source if Android prompts for it
- launch `ClawChat2`

## 中文

这个版本主要聚焦于让 Android 上的直接 agent 聊天体验更快、更自然。

ClawChat2 仍然是一个非官方 OpenClaw Android 分叉，目标是在 Android 上以更简单、更直接、聊天优先的方式与 OpenClaw agent 对话。

本版本重点包括：

- 从联系人进入会话时，当前选中的对话现在能更稳定地打开，不再容易被旧会话抢回
- 已知会话现在可以优先显示本地最近历史，不再先经历一段空白加载
- 从联系人进入会话时，现在会真正落到对话底部，即使最后一条消息很长也一样
- 全屏图片现在支持双指缩放和拖动查看
- 聊天输入框默认收敛为单行，并会随着输入内容自动增高
- 聊天回复现在可以通过输入框下方操作按钮行里的喇叭开关直接朗读，并且已在真实 Huawei Mate60 上验证 Android 系统 TTS 可正常工作
- 聊天附件按钮现在已经支持上传图片、音频、视频和普通文件，普通文件也会直接在聊天记录里显示为文件卡片
- 输入框下方的操作按钮行现在也直接提供 send/stop、`/new`、模型和 thinking 控制，常用 WebChat 聊天操作不再需要手动输入 slash 命令
- 聊天气泡现在进一步收敛为左对齐的高密度布局，并通过更明确的底色区分用户、agent 与 system 消息，其中 agent 气泡改为白色底
- 大图现在会尽量贴合气泡内容区边缘显示，小图保持自然尺寸不过度放大；图片同时支持单击进入全屏、全屏内再单击直接返回
- 输入框下方的快捷操作现已改为无外框的纯图标按钮，附件、新会话、模型、thinking、朗读和发送/停止都能在一行内完整显示

重要说明：

- 当前版本仍属于早期 `Pre-release`
- 这不是官方 OpenClaw 构建版本
- 联系人与聊天历史的服务端真源仍然是 `openclaw-webchat`

安装方式：

- 下载附带的 APK
- 如果 Android 提示，请允许从当前来源安装
- 启动 `ClawChat2`
