# ClawChat2 Status

Last updated: 2026-03-14

## Public Summary

- Baseline version: `0.2.1`
- Android compatibility baseline: `minSdk 30` (Android 11+)
- Stage: early internal validation
- Upstream origin: `openclaw/openclaw -> apps/android`
- Repository role: independent Android fork focused on direct agent chat UX

## What This Fork Is Trying To Do

ClawChat2 is exploring a simpler Android experience for users who mainly want to:

- connect to an OpenClaw gateway
- open a direct conversation with an agent
- exchange media-rich messages
- use a stable fullscreen media viewing path on Android
- reach the gateway in practical local-network or Tailscale setups without extra product complexity

The broader intent is to make everyday Android agent chat feel more direct and less procedurally heavy for users whose main need is chatting with their agents, not managing a larger multi-surface shell.

## Current Fork-Specific Enhancements

- chat-first shell and direct agent conversation flow
- stricter direct-session handling for fork-owned chats
- enhanced agent-sent image/audio/video rendering
- gateway-relative media reference support in the Android client
- practical LAN and Tailscale-oriented connection support
- stabilized fullscreen image and video behavior on the accepted in-app dialog path

## Public Caveats

- This repository is not an official OpenClaw build.
- It is early and still contains fork-specific product choices that may never go upstream.
- Public releases should preserve upstream license and attribution.
- Public releases should avoid private infrastructure assumptions, local machine paths, and test-only credentials.

## Upstream Strategy

If parts of this fork are proposed upstream, they should be split into small, reviewable changes such as:

- Android media rendering hardening
- fullscreen playback stability fixes
- gateway connection compatibility fixes

Fork-specific branding, session naming, and local operational tooling should not be treated as automatic upstream candidates.
