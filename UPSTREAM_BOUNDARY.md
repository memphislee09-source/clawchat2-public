# Upstream Boundary

This document explains which changes are fork-specific and which changes may be suitable for proposal to the official OpenClaw project later.

## Default Rule

Do not assume a change should go upstream just because it works well in this fork.

Upstream candidates should be:

- small
- generally useful
- not tied to ClawChat2 branding or product direction
- free of private infrastructure assumptions

## Fork-Only Changes

These changes should normally stay in ClawChat2:

- ClawChat2 branding, naming, and identity
- fork-owned session naming such as `agent:<agentId>:clawchat2`
- chat-first product positioning specific to this fork
- local operational scripts and machine-specific media-server workflows
- docs written only for this repository's testing setup

## Likely Upstream Candidates

These changes may be worth upstreaming if they are isolated cleanly:

- Android media rendering correctness fixes
- fullscreen image or video playback stability fixes
- Android lifecycle or playback bug fixes
- gateway connection compatibility fixes
- UI bug fixes that improve correctness without imposing fork-specific product direction

## Poor Upstream Candidates

These usually should not be proposed upstream in their fork form:

- product rebranding
- opinionated navigation rewrites without broad maintainer support
- protocol changes that only this fork understands
- changes that depend on local scripts or local media daemons

## How To Prepare An Upstream Candidate

If a change might go upstream later:

1. Keep the implementation independent from ClawChat2 branding.
2. Avoid fork-only session keys, copy, and assumptions.
3. Remove local test shortcuts and private defaults.
4. Add focused verification and screenshots.
5. Split it into the smallest reviewable PR shape possible.

## Review Questions

Before merging a change here, ask:

- Would this still make sense without the ClawChat2 name?
- Would this still make sense without the local media tooling?
- Does this improve Android correctness, or only this fork's product direction?
- Could it be explained to upstream maintainers in one short problem statement?

If the answer is mostly no, keep it classified as fork-only.
