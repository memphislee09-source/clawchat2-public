# ClawChat2 Status

## English

Last updated: 2026-03-31

### Public Summary

- Baseline version: `0.2.6`
- Android compatibility baseline: `minSdk 30` (Android 11+)
- Stage: early public pre-release
- Upstream origin: `openclaw/openclaw -> apps/android`
- Repository role: independent Android fork focused on direct agent chat UX
- Chat backend baseline: `openclaw-webchat` for contacts and conversations
- Public repository: `memphislee09-source/clawchat2-public`
- Public pre-release: `v0.2.6`

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
- `openclaw-webchat`-backed contacts, sessions, and chat history
- stricter direct-session handling for fork-owned chats
- enhanced agent-sent image/audio/video rendering
- gateway-relative media reference support in the Android client
- practical LAN and Tailscale-oriented connection support
- selectable app theme mode with light/dark/system options
- refined contacts list styling with synced agent avatars, a continuous list layout, and lighter separators
- refined chat message presentation with spacing-based grouping, no extra bubble borders, and synced user display names from `openclaw-webchat`
- optional chat reply readout with a composer-row speaker toggle and Android system-TTS fallback
- lightweight composer quick controls for send/stop, `/new`, model switching, and thinking switching
- denser left-aligned chat bubbles with smaller symmetric side padding, screenshot-aligned light/dark surfaces, and stronger color separation between user, agent, and system messages
- solid green user reply bubbles now use dark text in both light and dark mode for better readability against the brighter reply fill
- large chat images now render edge-to-edge inside the bubble body, while smaller images keep their natural size
- fullscreen chat images now support tap-to-open and tap-to-dismiss in addition to pinch zoom
- borderless icon-only composer actions keep attachment, new chat, model, thinking, readout, and send/stop visible in one row
- generic user attachment upload for images, audio, video, and regular files
- stabilized fullscreen image behavior
- streamed fullscreen video playback on Android via Media3/ExoPlayer

### Current Confirmed Baseline

- agent-sent image, audio, and video rendering is working
- user-originated attachment upload now supports image, audio, video, and generic file blocks through `openclaw-webchat`
- contacts and conversations now read from `openclaw-webchat`, not the old gateway chat assembly path
- media resolution prefers `mediaPath + mediaPort`, with `mediaUrl` kept only as fallback
- fullscreen video now uses streamed playback and no longer requires a full remote download before play
- image/video memory regressions in long chats were reduced by downsampled image decode and streamed video playback
- onboarding first-run welcome screen now links directly to the `claw-webchat` server install guide at `https://github.com/memphislee09-source/claw-webchat/blob/main/docs/AGENT_INSTALL_NETWORK.md`
- the welcome flow explicitly tells users to first give that `claw-webchat` install guide to the OpenClaw-side agent or operator before continuing app setup
- manual and Tailscale setup now explicitly remind users to fill in the gateway token if they want the device to appear in `openclaw devices list`
- setup-code pairing is now compatible with official OpenClaw `v2026.3.12` flows again
- the pairing regression was fixed by restoring support for setup-code `bootstrapToken`
- setup-code / Tailscale-Serve handling now correctly preserves `wss://...:443` instead of incorrectly forcing `:18789`
- explicit setup-code/manual/Tailscale endpoints now win over LAN auto-discovery during connect
- pairing diagnostics on a real phone confirmed one practical requirement: the Android Tailscale app must be actively connected, not merely installed and logged in
- Tailscale Serve routing was validated on a real Huawei Mate60 after restoring the missing published `.ts.net:443` route on the gateway host
- manual TLS fingerprint entry is now available for setup-code/manual/Tailscale flows when a device cannot complete first-time certificate probing automatically
- Android pairing no longer requests `operator.talk.secrets` by default, which removes a practical approval deadlock on gateways where the available local approver only has pairing/read/write scopes
- the practical pairing guidance is now: verify `tailscale serve status`, reconnect from Android, then approve with `openclaw devices approve --latest` if pending request IDs are rotating
- QR / setup-code pairing now pauses Android-side automatic reconnect churn while approval is pending, so the currently listed device request no longer gets invalidated by repeated background retries
- app theme selection is now available in Settings with `System Default`, `Light`, and `Dark`
- the onboarding welcome screen now starts with an explicit app-language choice for `Follow system`, `English`, and `Chinese`
- settings and in-app About surfaces now follow dark mode correctly
- contacts now use a cleaner WeChat-style list treatment instead of boxed rows, with larger avatars and subtler separators
- chat now follows the same lighter interaction direction, and the local user label now syncs from `openclaw-webchat` settings
- chat reply readout is now available as a lightweight speaker toggle in the composer action row instead of the old chat-side microphone shortcut
- chat reply readout no longer depends on gateway-direct voice mode; it reuses the local Android TTS path and now declares Android 11+ TTS package visibility so system engines can actually bind on devices like the Huawei Mate60
- the composer action row now also exposes direct send/stop, `/new`, model, and thinking controls so common WebChat chat commands no longer require typing slash commands manually
- generic files now render as first-class file cards in the chat transcript instead of falling through to an unsupported-attachment state
- chat now also includes a localized Chinese processing indicator with animated pulse dots, and the current `main` workspace was compiled, installed, and launched successfully on the local Android 15 emulator baseline on 2026-03-24
- the current `codex/upstream-bridge-pass` workspace was rebuilt as `playDebug` on 2026-03-26 and the Mate60 (`BRA-AL00`, Android 12 / SDK 31) completed Tailscale setup-code pairing successfully after the route, TLS fallback, and pairing-scope fixes
- the current `main` workspace was rebuilt as `playDebug` on 2026-03-28 and chat reply readout was then verified successfully on the Mate60 after the Android TTS package-visibility fix
- the current `main` workspace was also verified on 2026-03-28 on the Mate60 by uploading a Markdown file through the chat attachment button, confirming the generic file-upload path works on device
- the current `main` workspace was also rebuilt as `playDebug` on 2026-03-29 and the Redmi K20 (`c2f22adf`) verified the new composer `N` control successfully, confirming it dispatches `/new` from the chat action row
- the current `main` workspace was rebuilt again as `playDebug` on 2026-03-29 and the Redmi K20 (`c2f22adf`) completed install-and-launch verification for the release-facing chat visual polish pass, including left-aligned dense bubbles, screenshot-aligned light/dark chat palette, dark-text user reply bubbles, edge-to-edge large images, and the borderless icon-only composer row

### Current Public Distribution State

- public repository is live
- public release APK has been rebuilt and refreshed from the current `main` baseline
- current public release page: `https://github.com/memphislee09-source/clawchat2-public/releases/tag/v0.2.6`
- current public release asset: `openclaw-0.2.6-play-release.apk`
- current public release SHA256: `b300097f352e75993ccf65fe569266a0e02d9cd0a69b5bcae1dbb6d84d158af9`
- current development build version is now `0.2.6`
- the current public homepage now links to the Bilibili intro video and uses the refreshed single-image preview instead of the older screenshot pair
- the pairing retry suppression fix and first-launch language picker are now part of the promoted release baseline for both private and public repositories

### Current Development Baseline

- private repository `main` is now the continuing development baseline
- public repository `main` has been synchronized to the same product baseline
- future Android work should continue from this fixed setup-code-pairing-compatible version
- do not regress the stable media and fullscreen playback path while iterating on new features

## 中文

最后更新：2026-03-31

### 公开摘要

- 当前基线版本：`0.2.6`
- Android 兼容基线：`minSdk 30`（Android 11+）
- 当前阶段：早期公开预发布
- 上游来源：`openclaw/openclaw -> apps/android`
- 仓库定位：面向直接 agent 聊天体验的独立 Android 分叉
- 聊天后端基线：联系人与对话以 `openclaw-webchat` 为准
- 公开仓库：`memphislee09-source/clawchat2-public`
- 当前公开预发布：`v0.2.6`

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
- 基于 `openclaw-webchat` 的联系人、会话与聊天历史
- 更严格的分叉专属 direct session 处理
- 更强的 agent 图片/音频/视频渲染能力
- Android 客户端中的网关相对媒体引用支持
- 面向 LAN 与 Tailscale 的更实用连接支持
- 支持可选的应用主题模式，可在浅色、深色、跟随系统之间切换
- 联系人列表样式已继续细化，包含同步 agent 头像、连续列表布局和更浅的分隔线
- 聊天消息展示也已调整为更轻的留白分组样式，去掉额外气泡边框，并同步 `openclaw-webchat` 中“我”的名称
- 聊天页现已支持可选的回复朗读能力，并在输入框下方的操作按钮行里提供轻量的喇叭切换按钮
- 输入框下方的操作按钮行现在也提供 send/stop、`/new`、模型切换和 thinking 切换等轻量快捷控制
- 聊天气泡现在进一步收敛为左对齐、左右留白更小且对称的高密度排布，并按参考稿收紧浅色/深色表层关系，通过底色更明确地区分用户、agent 与 system 消息
- 用户回复气泡现在在浅色和深色模式下都统一使用实心绿色底配深色文字，提升亮色气泡上的可读性
- 大图现在会贴合气泡内容区边缘显示，小图则保持自然尺寸，不会被强行放大
- 聊天图片除了双指缩放外，也支持单击打开全屏、全屏内再单击直接返回
- 输入框下方的快捷操作现已收敛为无外框的纯图标按钮，一行内同时展示附件、新会话、模型、thinking、朗读与发送/停止
- 聊天页现已支持用户侧上传图片、音频、视频和普通文件给 agent
- 图片全屏稳定性增强
- 基于 Media3/ExoPlayer 的视频流式全屏播放

### 当前确认基线

- agent 发送的图片、音频、视频渲染已经正常工作
- 用户主动发送的附件现在也已支持图片、音频、视频和通用文件，并通过 `openclaw-webchat` 统一上传
- 联系人与对话现在直接读取 `openclaw-webchat`，不再走旧的 gateway chat 拼装路径
- 媒体解析优先使用 `mediaPath + mediaPort`，`mediaUrl` 仅保留为 fallback
- 视频全屏现在支持流式播放，不再要求先完整下载远端视频
- 长聊天中的图片/视频内存回归已通过图片采样解码和视频流式播放做了压降
- onboarding 首次运行欢迎页现已直接链接到 `claw-webchat` 的服务端安装文档：`https://github.com/memphislee09-source/claw-webchat/blob/main/docs/AGENT_INSTALL_NETWORK.md`
- 首次运行流程会明确提示用户：继续 app 设置前，应先把该 `claw-webchat` 安装文档交给 OpenClaw 侧 agent 或 operator
- 手动设置和 Tailscale 设置已明确提醒：如果希望设备出现在 `openclaw devices list` 中，应填写 gateway token
- setup-code 配对现已重新兼容官方 OpenClaw `v2026.3.12` 流程
- 这次配对回归的修复点，是补回了 setup-code `bootstrapToken` 支持
- setup-code / Tailscale Serve 路径现在能正确保留 `wss://...:443`，不再错误回落到 `:18789`
- 显式输入的 setup-code / 手动 / Tailscale 地址在连接时不再被 LAN 自动发现覆盖
- 真机排查已确认一个实际使用前提：Android 上的 Tailscale 必须处于真正已连接状态，而不是仅安装并登录
- 在真实 Huawei Mate60 上的排查又确认了另一条前提：如果网关主机配置了 `gateway.tailscale.mode=serve`，就必须先确认 `tailscale serve status` 确实已经发布 `.ts.net:443` 路由
- 现在已为 setup-code / 手动 / Tailscale 流程补上手工 TLS 指纹输入入口，设备无法自动读取首次证书指纹时可以直接粘贴 SHA-256 指纹继续连接
- Android 默认 operator 配对 scope 已从 `operator.read + operator.write + operator.talk.secrets` 收窄为 `operator.read + operator.write`，避免在本地审批身份不具备 `operator.talk.secrets` 时陷入配对死锁
- 当前更稳妥的配对操作建议是：先确认 `tailscale serve status`，手机重新连接后，若 pending request 一直轮换，则使用 `openclaw devices approve --latest`
- 二维码 / setup-code 配对在等待审批时现在会暂停 Android 侧自动重连，避免后台反复重试把当前待批准请求很快轮换掉
- 设置页现已支持 `跟随系统`、`浅色`、`深色` 主题切换
- onboarding 欢迎页现在会先提供应用语言选择，用户可在继续设置前选择 `跟随系统`、`English` 或 `中文`
- 设置页与应用内关于弹窗现在都能正确跟随深色模式
- 联系人列表现已改为更接近微信通讯录的连续列表样式，头像更大、分隔线更轻，不再使用外框卡片
- 聊天页现在也延续相同的轻量交互方向，并会把用户发言标题与 `openclaw-webchat` 里的“我”名称同步
- 聊天回复朗读现在使用本地 Android TTS 路径，不再依赖旧的 gateway-direct voice 流程；同时已补上 Android 11+ 所需的 TTS service package visibility，像 Huawei Mate60 这类设备也能真正绑定系统朗读引擎
- 聊天页操作按钮行现在也直接暴露 send/stop、`/new`、模型和 thinking 控制，常用 WebChat 聊天命令不再需要用户手动输入 slash 命令
- 通用文件现在会在聊天记录中显示为文件卡片，不再落到“不支持的附件”状态
- 聊天页当前还加入了中文“处理中”提示与动态圆点效果，并且当前 `main` 工作区已于 2026-03-24 在本地 Android 15 模拟器基线上成功编译、安装并启动
- 当前 `codex/upstream-bridge-pass` 工作区已于 2026-03-26 重新构建为 `playDebug`，并在 Huawei Mate60（`BRA-AL00`，Android 12 / SDK 31）上完成了 Tailscale setup-code 配对验证，验证通过依赖于 Tailscale Serve 路由恢复、手工 TLS fallback 入口以及默认配对 scope 收窄这三项修复
- 当前 `main` 工作区又已于 2026-03-28 重新构建为 `playDebug`，并在 Huawei Mate60（`BRA-AL00`，Android 12 / SDK 31）上完成聊天回复朗读验证，验证通过依赖于 Android TTS package visibility 补全与聊天页喇叭切换入口调整
- 当前 `main` 工作区也已于 2026-03-28 在 Huawei Mate60（`BRA-AL00`，Android 12 / SDK 31）上完成通用附件上传验证，测试文件为 Markdown，验证结果通过
- 当前 `main` 工作区也已于 2026-03-29 重新构建为 `playDebug`，并在 Redmi K20（设备 `c2f22adf`）上完成聊天页 `N` 按钮验证，确认该按钮会从操作按钮行直接执行 `/new`
- 当前 `main` 工作区又已于 2026-03-29 重新构建为 `playDebug`，并在 Redmi K20（设备 `c2f22adf`）上完成公开版前聊天视觉收口版本的安装与启动验证，覆盖左对齐高密度气泡、按参考稿对齐的浅色/深色聊天配色、用户绿色气泡深色文字、大图贴边显示以及无外框纯图标操作行

### 当前公开发布状态

- 公开仓库已经上线
- 公开 release APK 已按当前 `main` 基线重新构建并刷新
- 当前公开 release 页面：`https://github.com/memphislee09-source/clawchat2-public/releases/tag/v0.2.6`
- 当前公开 release 资产：`openclaw-0.2.6-play-release.apk`
- 当前公开 release SHA256：`b300097f352e75993ccf65fe569266a0e02d9cd0a69b5bcae1dbb6d84d158af9`
- 当前开发构建版本已提升到 `0.2.6`
- 当前公开仓首页已加入 Bilibili 介绍视频链接，并用新的单张合成预览图替换旧截图组
- 当前这条发布基线已同时包含配对重试抑制修复与首次安装语言选择入口

### 当前开发基线

- 私有仓库 `main` 现在就是后续持续开发基线
- 公开仓库 `main` 已同步到相同产品基线
- 后续 Android 功能迭代应从这个已修复 setup-code 配对兼容性的版本继续
- 继续迭代时不要破坏当前稳定的媒体链路和全屏播放路径
