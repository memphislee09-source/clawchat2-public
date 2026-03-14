# ClawChat2 Status

## English

Last updated: 2026-03-14

### Public Summary

- Baseline version: `0.2.1`
- Android compatibility baseline: `minSdk 30` (Android 11+)
- Stage: early internal validation
- Upstream origin: `openclaw/openclaw -> apps/android`
- Repository role: independent Android fork focused on direct agent chat UX

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

## 中文

最后更新：2026-03-14

### 公开摘要

- 当前基线版本：`0.2.1`
- Android 兼容基线：`minSdk 30`（Android 11+）
- 当前阶段：早期内部验证
- 上游来源：`openclaw/openclaw -> apps/android`
- 仓库定位：面向直接 agent 聊天体验的独立 Android 分叉

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
