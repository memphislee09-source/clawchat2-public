# ClawChat2 Status

## English

Last updated: 2026-03-14

### Public Summary

- Baseline version: `0.2.1`
- Android compatibility baseline: `minSdk 30` (Android 11+)
- Stage: early public pre-release
- Upstream origin: `openclaw/openclaw -> apps/android`
- Repository role: independent Android fork focused on direct agent chat UX
- Public repository: `memphislee09-source/clawchat2-public`
- Public pre-release: `v0.2.1`

### What This Fork Is Trying To Do

ClawChat2 is exploring a simpler Android experience for users who mainly want to:

- connect to an OpenClaw gateway
- open a direct conversation with an agent
- exchange media-rich messages
- use a stable fullscreen media viewing path on Android
- reach the gateway in practical local-network or Tailscale setups without extra product complexity

The broader intent is to make everyday Android agent chat feel more direct and less procedurally heavy for users whose main need is chatting with their agents, not managing a larger multi-surface shell.

### Current Fork-Specific Enhancements

- chat-first shell and direct agent conversation flow
- stricter direct-session handling for fork-owned chats
- enhanced agent-sent image/audio/video rendering
- gateway-relative media reference support in the Android client
- practical LAN and Tailscale-oriented connection support
- stabilized fullscreen image and video behavior on the accepted in-app dialog path

### Current Confirmed Baseline

- agent-sent image, audio, and video rendering is working
- media resolution prefers `mediaPath + mediaPort`, with `mediaUrl` kept only as fallback
- fullscreen video remains on the stable `Dialog + VideoView` path
- accepted fullscreen video policy remains: no cropping, preserve aspect ratio
- onboarding first-run welcome screen now links directly to `OPENCLAW_AGENT_SETUP.md`
- the welcome flow explicitly tells users to pass that guide to the OpenClaw-side agent or operator
- manual and Tailscale setup now explicitly remind users to fill in the gateway token if they want the device to appear in `openclaw devices list`
- setup-code pairing is now compatible with official OpenClaw `v2026.3.12` flows again
- the pairing regression was fixed by restoring support for setup-code `bootstrapToken`

### Current Public Distribution State

- public repository is live
- public release APK has been rebuilt and refreshed from the current `main` baseline
- current public release asset: `openclaw-0.2.1-release.apk`
- current public release SHA256: `144b5534fd0603be0088ce707aa754e5870aac3651fa64242a11b08bb1fb43b1`

### Current Development Baseline

- private repository `main` is now the continuing development baseline
- public repository `main` has been synchronized to the same product baseline
- future Android work should continue from this fixed setup-code-pairing-compatible version
- do not regress the stable media and fullscreen playback path while iterating on new features

## 中文

最后更新：2026-03-14

### 公开摘要

- 当前基线版本：`0.2.1`
- Android 兼容基线：`minSdk 30`（Android 11+）
- 当前阶段：早期公开预发布
- 上游来源：`openclaw/openclaw -> apps/android`
- 仓库定位：面向直接 agent 聊天体验的独立 Android 分叉
- 公开仓库：`memphislee09-source/clawchat2-public`
- 当前公开预发布：`v0.2.1`

### 这个分叉想解决什么问题

ClawChat2 正在探索一种更简单的 Android 体验，面向那些主要需求是：

- 连接 OpenClaw gateway
- 直接进入与 agent 的对话
- 收发媒体丰富的消息
- 在 Android 上稳定地全屏查看媒体
- 在本地网络或 Tailscale 场景下以更直接的方式接入 gateway

更广义的目标，是让 Android 上的日常 agent 聊天更直接、流程更少，而不是让用户先面对一个更复杂的多界面控制壳层。

### 当前分叉增强点

- 聊天优先的主界面与直接会话流程
- 更严格的分叉专属 direct session 处理
- 更强的 agent 图片/音频/视频渲染能力
- Android 客户端中的网关相对媒体引用支持
- 面向 LAN 与 Tailscale 的更实用连接支持
- 已接受的 in-app dialog 路径上的图片/视频全屏稳定性增强

### 当前确认基线

- agent 发送的图片、音频、视频渲染已经正常工作
- 媒体解析优先使用 `mediaPath + mediaPort`，`mediaUrl` 仅保留为 fallback
- 视频全屏继续保持在稳定的 `Dialog + VideoView` 路径
- 当前接受的视频全屏策略仍然是：不裁切、保持完整比例显示
- onboarding 首次运行欢迎页已直接链接到 `OPENCLAW_AGENT_SETUP.md`
- 首次运行流程会明确提示用户把该文档交给 OpenClaw 侧 agent 或 operator 阅读
- 手动设置和 Tailscale 设置已明确提醒：如果希望设备出现在 `openclaw devices list` 中，应填写 gateway token
- setup-code 配对现已重新兼容官方 OpenClaw `v2026.3.12` 流程
- 这次配对回归的修复点，是补回了 setup-code `bootstrapToken` 支持

### 当前公开发布状态

- 公开仓库已经上线
- 公开 release APK 已按当前 `main` 基线重新构建并刷新
- 当前公开 release 资产：`openclaw-0.2.1-release.apk`
- 当前公开 release SHA256：`144b5534fd0603be0088ce707aa754e5870aac3651fa64242a11b08bb1fb43b1`

### 当前开发基线

- 私有仓库 `main` 现在就是后续持续开发基线
- 公开仓库 `main` 已同步到相同产品基线
- 后续 Android 功能迭代应从这个已修复 setup-code 配对兼容性的版本继续
- 继续迭代时不要破坏当前稳定的媒体链路和全屏播放路径
