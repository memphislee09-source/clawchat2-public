# Contributing to ClawChat2

## English

ClawChat2 is an early-stage, unofficial Android fork derived from the OpenClaw Android client.

Before contributing, read:

- [README.md](README.md)
- [FORK_NOTES.md](FORK_NOTES.md)
- [LICENSE](LICENSE)

### Project Expectations

- This repository is not an official OpenClaw repository.
- Contributions should support the fork goals: simple direct chat, practical media improvements, and stable Android behavior.

### Rules

- Do not commit personal endpoints, private tokens, local machine paths, or test-only credentials.
- Do not present this repository as an official OpenClaw build.
- Do not remove upstream attribution or license notices.
- Keep changes focused.
- Document user-visible changes clearly.

### Fork vs Upstream

Some changes belong only in this fork. Some may become upstream candidates later.

Before starting a larger change, decide whether it is:

- fork-only
- maybe-upstream
- upstream-targeted

Use this repository's public positioning as the decision guide:

- keep chat-first product behavior in this fork
- keep private environment assumptions out of committed code
- separate any future upstream candidates into smaller, reusable changes

### Recommended Verification

For Android changes, verify as many of these as apply:

- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:assembleDebug`
- targeted unit tests
- emulator install + launch
- real-device verification for media, playback, permissions, or lifecycle changes

### Pull Requests

Include:

- the problem being solved
- why the change belongs in this fork
- what did not change
- how you verified it
- screenshots for UI changes when relevant

### Copyright And Attribution

- This fork remains under the MIT license preserved in [LICENSE](LICENSE).
- Upstream authors retain attribution for upstream-originated code.
- New contributions must not remove or obscure original project attribution.

## 中文

ClawChat2 是一个早期阶段的非官方 Android 分叉项目，源自 OpenClaw Android 客户端。

贡献前请先阅读：

- [README.md](README.md)
- [FORK_NOTES.md](FORK_NOTES.md)
- [LICENSE](LICENSE)

### 项目要求

- 本仓库不是官方 OpenClaw 仓库。
- 贡献应服务于本分叉当前目标：更直接的聊天体验、实用的媒体增强，以及稳定的 Android 行为。

### 规则

- 不要提交个人 endpoint、私有 token、本机路径或测试专用凭据。
- 不要把本仓库表述成官方 OpenClaw 发布版本。
- 不要删除上游归属说明或许可证信息。
- 保持改动聚焦。
- 清楚描述用户可见行为变化。

### 分叉与上游

有些改动只属于本分叉，有些未来可能拆分后提交上游。

开始较大改动前，请先判断它属于：

- fork-only
- maybe-upstream
- upstream-targeted

判断标准以本仓库当前公开定位为准：

- 聊天优先的产品行为保留在本分叉中
- 不要把私有环境假设写入已提交代码
- 如果未来考虑 upstream，应先拆成更小、更可复用的改动

### 建议验证项

对 Android 改动，尽量验证以下项目：

- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:assembleDebug`
- 有针对性的单元测试
- 模拟器安装与启动
- 对媒体、播放、权限、生命周期相关改动做真机验证

### Pull Request 要包含

- 解决了什么问题
- 为什么这个改动应该留在本分叉
- 哪些内容没有改
- 你是如何验证的
- 如果涉及 UI，请附截图

### 版权与归属

- 本分叉继续遵循 [LICENSE](LICENSE) 中保留的 MIT 许可证。
- 上游原始代码部分的归属仍属于上游作者。
- 新贡献不得删除或弱化原始项目的归属信息。
