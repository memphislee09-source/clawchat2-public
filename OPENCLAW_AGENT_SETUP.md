# OpenClaw Agent Setup For ClawChat2

## English

This document is written for an OpenClaw-side agent or operator.

Goal:

- prepare an existing OpenClaw installation so ClawChat2 can connect
- keep setup simple for the user
- optionally enable stronger media support used by ClawChat2

## Short Answer

ClawChat2 does not require a custom OpenClaw fork just to connect.

What it does require on the OpenClaw side is:

- a working OpenClaw gateway
- a gateway host and port reachable from Android over LAN or Tailscale
- either a setup code, or manual host/port/token details

If the user chooses manual or Tailscale setup in ClawChat2, they should fill in the gateway token.
Without a token, the device may connect, but it will not reliably appear in `openclaw devices list` for approval.

## Required OpenClaw-Side Preparation

### 1. Make sure OpenClaw is installed and working

On the OpenClaw host, verify the CLI works and the gateway is already set up.

Use the normal OpenClaw installation path for your environment.

Minimum outcome:

- `openclaw` CLI is available
- the OpenClaw gateway is running
- the gateway port is reachable from the Android device

### 2. Decide how ClawChat2 will connect

ClawChat2 supports three practical connection modes:

- setup code
- manual host/port
- Tailscale host/port

Recommended order:

1. setup code
2. Tailscale
3. manual host/port

### 3. If using setup code, generate it on the OpenClaw host

Run one of:

```bash
openclaw qr --setup-code-only
```

or:

```bash
openclaw qr --json
```

Then give the resulting setup code or QR payload to the user so they can paste or scan it in ClawChat2.

Important for Tailscale Serve:

- if OpenClaw is using `gateway.tailscale.mode=serve`, `openclaw qr --json` will usually advertise `wss://<magicdns>` on port `443`
- that is the correct route for ClawChat2; do not replace it with a LAN IP manually
- after scanning in ClawChat2, the user still needs to tap `Connect`

### 4. If using manual setup, provide the user with host, port, and token

The user needs:

- host
- port
- whether TLS is enabled
- token

Important:

- if the user chooses manual setup in ClawChat2, they should fill in the token
- if the user chooses Tailscale setup in ClawChat2, they should also fill in the token
- this is the safest way to ensure the device appears in `openclaw devices list`

### 5. If using Tailscale, make the gateway reachable on the tailnet

Recommended OpenClaw-side preparation:

- install Tailscale on the OpenClaw host
- join the same tailnet as the Android device
- confirm the host has a stable tailnet DNS name such as `openclaw.example.ts.net`
- provide that host and port to the user
- if using Tailscale Serve, prefer the MagicDNS hostname and port `443`
- do not tell the user to enter a private LAN IP when the intended route is tailnet access

Important Android-side reality:

- the Android device must have the Tailscale app actively connected
- "installed" or "logged in" is not sufficient if the VPN/tunnel is currently off
- if Tailscale is off, ClawChat2 can fail early with errors such as `can't read TLS fingerprint`

### 6. Approve the device if pairing is requested

If ClawChat2 shows a pairing or approval state, run:

```bash
openclaw devices list
openclaw devices approve <requestId>
```

Then ask the user to tap Connect again.

## Optional ClawChat2 Media Enhancements

These are optional.
They are only needed if you want the OpenClaw-side agent to send image, audio, or video into ClawChat2 with the stronger media path used by this fork.

Recommended extras:

- Node.js on the OpenClaw-side host or operator machine
- this repository's media helper scripts

Relevant files:

- `./scripts/send-clawchat-media.mjs`
- `./scripts/clawchat-media-server.mjs`
- [AGENT_MEDIA_SEND.md](AGENT_MEDIA_SEND.md)
- [AGENT_MEDIA_SERVER.md](AGENT_MEDIA_SERVER.md)

For ClawChat2 media messages, prefer payloads that include:

- `mediaPath`
- `mediaPort`

Keep `mediaUrl` only as a fallback.

## What To Give The User

Give the user one of these:

### Preferred

- a setup code from `openclaw qr --setup-code-only`

### Or manual details

- gateway host
- gateway port
- TLS on/off
- gateway token

### Or Tailscale details

- tailnet host, for example `openclaw.example.ts.net`
- gateway port
- gateway token

## Completion Checklist

The OpenClaw-side setup is complete when:

- OpenClaw gateway is running
- Android can reach the gateway over LAN or Tailscale
- the user has either a setup code or manual connection details
- manual or Tailscale users also have a token
- `openclaw devices list` can see the pending device when approval is needed

## 中文

这份文档写给 OpenClaw 侧的 agent 或操作者。

目标：

- 把现有 OpenClaw 安装准备成可被 ClawChat2 连接
- 尽量降低用户侧操作复杂度
- 在需要时启用 ClawChat2 的增强媒体能力

## 简短结论

如果只是让 ClawChat2 连上 OpenClaw，通常不需要修改 OpenClaw 核心代码。

OpenClaw 侧真正需要准备的是：

- 一个正常工作的 OpenClaw gateway
- Android 设备可通过局域网或 Tailscale 访问 gateway 的主机与端口
- 提供 setup code，或提供手动连接所需的 host/port/token

如果用户在 ClawChat2 中选择手动设置或 Tailscale 设置，应该填写 gateway token。
没有 token 时，设备即使能连接，也不一定能稳定出现在 `openclaw devices list` 中等待批准。

## OpenClaw 侧必须准备的内容

### 1. 确认 OpenClaw 已安装并能正常工作

在 OpenClaw 主机上，先确认 CLI 可用，且 gateway 已正确配置。

请使用你所在环境中正常的 OpenClaw 安装方式。

最低要求是：

- 可以使用 `openclaw` CLI
- OpenClaw gateway 正在运行
- Android 设备能访问 gateway 端口

### 2. 选择 ClawChat2 的连接方式

ClawChat2 当前支持三种实际可用的连接方式：

- setup code
- 手动 host/port
- Tailscale host/port

推荐优先级：

1. setup code
2. Tailscale
3. 手动 host/port

### 3. 如果使用 setup code，在 OpenClaw 主机上生成

执行以下任一命令：

```bash
openclaw qr --setup-code-only
```

或者：

```bash
openclaw qr --json
```

然后把生成的 setup code 或 QR 载荷交给用户，让其在 ClawChat2 中粘贴或扫码。

针对 Tailscale Serve 的重要说明：

- 如果 OpenClaw 使用的是 `gateway.tailscale.mode=serve`，那么 `openclaw qr --json` 通常会给出 `wss://<magicdns>`，端口通常是 `443`
- 这就是 ClawChat2 应该使用的正确入口，不要再手动改成局域网 IP
- 用户在 ClawChat2 中扫码后，仍然需要手动点击一次 `Connect`

### 4. 如果使用手动设置，把 host、port、token 提供给用户

用户需要知道：

- host
- port
- 是否启用 TLS
- token

重要说明：

- 如果用户在 ClawChat2 中选择手动设置，应填写 token
- 如果用户在 ClawChat2 中选择 Tailscale 设置，也应填写 token
- 这是确保设备能出现在 `openclaw devices list` 中等待批准的最稳妥方式

### 5. 如果使用 Tailscale，确保 gateway 能通过 tailnet 访问

推荐的 OpenClaw 侧准备方式：

- 在 OpenClaw 主机安装 Tailscale
- 让主机加入与 Android 设备相同的 tailnet
- 确认主机拥有稳定的 tailnet DNS 名，例如 `openclaw.example.ts.net`
- 把该主机名和端口提供给用户
- 如果使用的是 Tailscale Serve，优先提供 MagicDNS 主机名和端口 `443`
- 如果目标是 tailnet 接入，不要让用户手动填局域网私网地址

Android 侧一个非常实际的前提：

- Android 设备上的 Tailscale app 必须处于真实“已连接”状态
- 仅仅“安装了并登录了账号”并不够，如果 VPN / tunnel 当前是关闭的，ClawChat2 仍然无法通过 tailnet 连接
- 在这种情况下，ClawChat2 可能会先报出 `can't read TLS fingerprint` 这一类早期连接错误

### 6. 如果出现配对请求，批准设备

如果 ClawChat2 显示需要配对或批准，请执行：

```bash
openclaw devices list
openclaw devices approve <requestId>
```

然后让用户再次点击 Connect。

## ClawChat2 增强媒体支持的可选项

以下内容是可选的。
只有当你希望 OpenClaw 侧 agent 向 ClawChat2 发送图片、音频、视频时，才需要额外准备。

推荐额外准备：

- 在 OpenClaw 侧主机或操作者机器上安装 Node.js
- 使用本仓库提供的媒体辅助脚本

相关文件：

- `./scripts/send-clawchat-media.mjs`
- `./scripts/clawchat-media-server.mjs`
- [AGENT_MEDIA_SEND.md](AGENT_MEDIA_SEND.md)
- [AGENT_MEDIA_SERVER.md](AGENT_MEDIA_SERVER.md)

对于 ClawChat2 的媒体消息，优先使用以下字段：

- `mediaPath`
- `mediaPort`

`mediaUrl` 只作为 fallback 保留。

## 应该交给用户什么信息

向用户提供以下任一种：

### 推荐方式

- 通过 `openclaw qr --setup-code-only` 生成的 setup code

### 或手动连接信息

- gateway host
- gateway port
- TLS 开关
- gateway token

### 或 Tailscale 连接信息

- tailnet 主机名，例如 `openclaw.example.ts.net`
- gateway port
- gateway token

## 完成检查

当以下条件满足时，说明 OpenClaw 侧准备完成：

- OpenClaw gateway 正在运行
- Android 设备可通过 LAN 或 Tailscale 访问 gateway
- 用户已经拿到 setup code 或手动连接参数
- 手动或 Tailscale 用户也拿到了 token
- 需要批准时，可通过 `openclaw devices list` 看到待批准设备
