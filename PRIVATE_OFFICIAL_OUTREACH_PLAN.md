# Private Official Outreach Plan

## English

This document is private planning material for contacting the OpenClaw maintainers.
It should stay in the private repository unless we explicitly decide to publish it later.

## Official Process To Respect

According to the current OpenClaw contributing guide:

- bugs and small fixes can go directly to PR
- new features or architecture changes should start as a GitHub Discussion or a Discord conversation first

Source:

- [OpenClaw CONTRIBUTING.md](https://github.com/openclaw/openclaw/blob/main/CONTRIBUTING.md)

Important current guidance from the official repo:

- "New features / architecture → Start a GitHub Discussion or ask in Discord first"
- keep PRs focused
- include screenshots for UI changes

## What We Should Ask For

Primary question:

- would the maintainers be open to a chat-first Android client direction inside OpenClaw, or as a lighter official Android mode?

Secondary question:

- if a fully separate chat-first client is not the right direction, would they be open to selectively adopting a few ideas from ClawChat2?

Those ideas should be framed as:

- chat-first navigation
- simpler gateway connection flow
- stronger Android media rendering and playback behavior
- Tailscale-friendly connection UX

## What We Should Not Ask For

Do not ask:

- "please adopt our whole fork"
- "please replace the official Android client with ClawChat2"
- "please merge all of our branding and product direction"

That framing is too large and too easy to reject.

## Best Outreach Sequence

1. Start with a GitHub Discussion or Discord message, not a PR.
2. Frame it as a product/use-case conversation, not a demand for merge.
3. Ask whether a chat-first Android surface is interesting to the project at all.
4. If the answer is positive, follow up with one or two narrow proposals.
5. Only after that, prepare small PRs or a design note.

## Recommended Framing

The strongest framing is not:

- "we built a fork, please take it"

The stronger framing is:

- "we found a recurring user need: some users want a much more direct, chat-first Android client with less setup friction"
- "we validated a few concrete improvements in a fork"
- "we want to know whether this direction is interesting upstream, either as a mode, a slimmer client surface, or a small set of Android UX changes"

## Evidence We Should Mention

We should mention only evidence that is concrete and easy to evaluate:

- Android 11 real-device validation on Redmi K20
- stable fullscreen image/video behavior
- stronger media receive/render support
- successful support for official setup-code pairing after restoring `bootstrapToken` compatibility
- practical LAN and Tailscale connection support

## Ask Structure

The actual ask should have three parts:

1. problem
2. validated improvements
3. question for maintainers

Example structure:

- some users primarily want to open Android and directly chat with their OpenClaw agents
- the current Android surface may be broader than what those users need
- we explored a chat-first fork and found concrete wins in onboarding, media UX, and connection flow
- would maintainers be open to discussing either:
  - a chat-first mode in the official Android client
  - or selective upstreaming of a few validated changes?

## If They Say No

If maintainers are not interested in a chat-first official direction:

- keep ClawChat2 as an independent public fork
- still upstream narrow bug fixes and compatibility fixes where they are clearly general improvements
- do not argue product direction once maintainers give a clear answer

## 中文

这份文档是面向 OpenClaw 官方沟通的私有规划材料。
除非后续明确决定公开，否则它应保留在私有仓库中。

## 需要遵守的官方流程

根据 OpenClaw 当前的贡献说明：

- bug 和小修复可以直接发 PR
- 新功能或架构改动，应先在 GitHub Discussion 或 Discord 中讨论

来源：

- [OpenClaw CONTRIBUTING.md](https://github.com/openclaw/openclaw/blob/main/CONTRIBUTING.md)

当前官方仓库的重要规则包括：

- “New features / architecture → Start a GitHub Discussion or ask in Discord first”
- PR 要保持聚焦
- UI 改动应附截图

## 我们应该问什么

核心问题：

- 官方是否愿意讨论一种“聊天优先”的 Android 客户端方向，或者在官方 Android 客户端里提供更轻量的聊天优先模式？

次要问题：

- 如果完全独立的聊天优先客户端不合适，官方是否愿意选择性吸收 ClawChat2 里的一些思路？

这些思路应被表述为：

- 聊天优先导航
- 更简单的网关连接流程
- 更稳定的 Android 媒体渲染与播放行为
- 对 Tailscale 更友好的连接体验

## 我们不该怎么问

不要这样提：

- “请把我们的整个 fork 收进去”
- “请用 ClawChat2 替换官方 Android 客户端”
- “请把我们的品牌和产品方向整体并入官方”

这种提法太大，也最容易被直接拒绝。

## 最合适的沟通顺序

1. 先发 GitHub Discussion 或 Discord，不要直接发 PR。
2. 以产品方向 / 用户场景讨论切入，不要以“请求合并 fork”切入。
3. 先问官方是否对“聊天优先 Android 体验”这个方向本身感兴趣。
4. 如果态度积极，再继续跟进一两个更小的具体提案。
5. 只有在这之后，再准备小 PR 或设计说明。

## 推荐的表达方式

最弱的表述不是：

- “我们做了个 fork，请收下”

更强的表述应该是：

- “我们发现一类反复出现的用户需求：一部分用户希望 Android 客户端更直接、更聊天优先、设置阻力更低”
- “我们在 fork 中验证了一些具体改进”
- “想确认官方是否对这个方向感兴趣，无论是作为模式、精简客户端表层，还是少量 Android UX 改动”

## 应该提哪些证据

只提具体且容易评估的证据：

- Android 11 真机 Redmi K20 验证通过
- 图片 / 视频全屏行为稳定
- 更强的媒体接收与渲染支持
- 恢复 `bootstrapToken` 兼容后，已经支持官方 setup-code 配对
- 更贴近实际使用的 LAN / Tailscale 连接支持

## 正式提问结构

实际沟通内容应由三部分组成：

1. 问题
2. 已验证改进
3. 对 maintainer 的问题

示例结构：

- 有一类用户的核心诉求是：打开 Android 直接和自己的 OpenClaw agent 聊天
- 当前 Android 表层对这些用户来说可能比所需更宽
- 我们在一个聊天优先 fork 中验证了 onboarding、媒体体验、连接流程上的一些收益
- 想问官方是否愿意讨论以下任一方向：
  - 官方 Android 客户端增加聊天优先模式
  - 或选择性 upstream 一些已经验证过的改进

## 如果官方不感兴趣

如果 maintainer 明确表示不考虑聊天优先官方方向：

- 就继续把 ClawChat2 作为独立公开 fork 维护
- 仍然可以把那些明显具备普适价值的 bugfix / 兼容性修复单独 upstream
- 在官方给出明确产品方向后，不要继续争论整体路线
