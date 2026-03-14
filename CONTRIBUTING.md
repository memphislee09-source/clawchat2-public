# Contributing to ClawChat2

ClawChat2 is an early-stage, unofficial Android fork derived from the OpenClaw Android client.

Before contributing, read:

- [README.md](README.md)
- [FORK_NOTES.md](FORK_NOTES.md)
- [UPSTREAM_BOUNDARY.md](UPSTREAM_BOUNDARY.md)
- [LICENSE](LICENSE)

## Project Expectations

This repository is not an official OpenClaw repository.

Contributions should support the current fork goals:

- simple, direct, chat-first Android UX
- practical Android media receive/render improvements
- stable real-device behavior

## Rules For Contributions

- Do not commit personal endpoints, private tokens, local machine paths, or test-only credentials.
- Do not present this repository as an official OpenClaw build.
- Do not remove upstream attribution or license notices.
- Keep changes focused. One problem per PR is preferred.
- Document user-visible behavior changes clearly.
- Keep fork-only changes easy to identify.

## Fork vs Upstream

Some changes belong only in this fork. Some may become upstream candidates later.

Before starting a larger change, decide which category it belongs to:

- fork-only
- maybe-upstream
- upstream-targeted

Use [UPSTREAM_BOUNDARY.md](UPSTREAM_BOUNDARY.md) as the decision guide.

## What Not To Submit

These are not acceptable in this repository:

- hardcoded real gateway hosts or tailscale hosts
- real tokens, secrets, or credentials
- machine-specific absolute paths
- screenshots or docs that imply official OpenClaw endorsement
- unrelated cleanup mixed into feature changes

## Recommended Verification

For Android changes, verify as many of these as apply:

- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:assembleDebug`
- targeted unit tests
- emulator install + launch
- real-device verification when the change affects media, playback, permissions, or lifecycle behavior

## Pull Requests

Include:

- what problem is being solved
- why the change belongs in this fork
- what did not change
- how you verified it
- screenshots for UI changes when relevant

If a change might be proposed upstream later, say so explicitly and keep the scope narrow.

## Copyright And Attribution

- This fork remains subject to the MIT license preserved in [LICENSE](LICENSE).
- Upstream authors retain attribution for upstream-originated code.
- New contributions should not remove or obscure original project attribution.
