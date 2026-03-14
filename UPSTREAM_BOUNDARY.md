# Upstream Boundary

## English

This document explains which changes are fork-specific and which changes may be suitable for proposal to the official OpenClaw project later.

### Default Rule

Do not assume a change should go upstream just because it works well in this fork.

Upstream candidates should be:

- small
- generally useful
- not tied to ClawChat2 branding or product direction
- free of private infrastructure assumptions

### Fork-Only Changes

These changes should normally stay in ClawChat2:

- ClawChat2 branding, naming, and identity
- fork-owned session naming such as `agent:<agentId>:clawchat2`
- chat-first product positioning specific to this fork
- local operational scripts and machine-specific media-server workflows
- docs written only for this repository's testing setup

### Likely Upstream Candidates

These changes may be worth upstreaming if isolated cleanly:

- Android media rendering correctness fixes
- fullscreen image or video playback stability fixes
- Android lifecycle or playback bug fixes
- gateway connection compatibility fixes
- UI bug fixes that improve correctness without imposing fork-specific product direction

## 中文

这份文档用于说明：哪些改动属于本分叉专属，哪些改动未来有机会整理后提交给官方 OpenClaw 项目。

### 默认原则

不要因为某个改动在本分叉里效果不错，就默认它应该 upstream。

适合 upstream 的候选改动通常应满足：

- 范围小
- 普遍有价值
- 不绑定 ClawChat2 品牌或产品方向
- 不依赖私有基础设施假设

### 只属于分叉的改动

以下内容通常应保留在 ClawChat2：

- ClawChat2 的品牌、命名与身份
- 本分叉专属 session 命名，例如 `agent:<agentId>:clawchat2`
- 本分叉特有的聊天优先产品定位
- 本地运维脚本与机器专属媒体服务流程
- 只适用于本仓库测试环境的文档

### 更可能适合 upstream 的改动

如果能干净拆分，这些方向可能值得 upstream：

- Android 媒体渲染正确性修复
- 图片/视频全屏播放稳定性修复
- Android 生命周期或播放 bug 修复
- gateway 连接兼容性修复
- 提升正确性的 UI bugfix，但不强行带入本分叉产品方向
