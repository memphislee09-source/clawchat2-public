# Why This Fork

ClawChat2 exists to explore a simpler Android experience for OpenClaw users whose main goal is straightforward:

- open the app
- connect to their OpenClaw gateway
- chat directly with their agents
- receive and view media reliably

This fork is aimed at reducing friction for users who do not want a more layered Android surface or a more complicated setup path than necessary for everyday agent chat.

## Core Idea

The core idea behind this fork is:

> talking to an OpenClaw agent on Android should feel simple, direct, and chat-first.

That means favoring a UX where the user can get to an agent conversation quickly, without unnecessary navigation, extra shell surfaces, or setup/registration complexity beyond what is actually needed to connect to their own OpenClaw gateway.

## Main Product Priorities

ClawChat2 currently emphasizes:

- direct agent chat as the primary interaction model
- chat-first UI instead of a broader control-shell feel
- stronger support for image, audio, and video messages sent by agents
- stable fullscreen media viewing and playback behavior
- practical support for gateway connection scenarios including local network and Tailscale-based access

## Why It May Matter Beyond The Fork

Even though this repository is an independent fork, some of the work done here may be useful to the official OpenClaw Android project later, especially where it improves:

- Android media rendering correctness
- playback stability
- direct-chat usability
- connection reliability in real user environments

## What This Fork Is Not Claiming

This document is not claiming that the full fork should replace the official OpenClaw Android app.

It is only stating the user problem this fork is trying to solve:

- simpler direct agent chat
- less friction in daily use
- better media behavior on Android

Any future upstream proposal should be limited to the generally useful technical improvements, not the full fork product direction as a package.
