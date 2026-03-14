# Upstream Outreach Draft

This draft is for proposing selected ClawChat2 improvements to the official OpenClaw project without implying that the full fork should be merged as-is.

## Recommended Position

Do not ask maintainers to adopt the entire fork.

Ask instead whether they are open to reviewing a small set of isolated Android improvements that were developed while building a chat-focused fork intended to make direct agent conversation feel simpler and less procedurally heavy on Android.

## Short Intro Draft

Hi OpenClaw team,

I have been developing an unofficial Android fork based on `openclaw/openclaw -> apps/android` to explore a simpler, chat-first Android experience for users who mostly want direct conversations with OpenClaw agents, without extra setup, registration, or shell complexity beyond what is needed to connect to their own gateway.

While doing that work, I ended up with a few Android-specific fixes and improvements that may have value outside the fork as well. I am not proposing that the full fork be merged as-is. Instead, I would like to split out a small number of focused Android changes and submit them individually if they match your direction.

The main areas I think may be relevant are:

- simpler direct-chat-oriented Android UX
- Android media rendering and playback hardening
- fullscreen image/video stability improvements
- selected gateway connection compatibility fixes, including practical Tailscale-friendly usage

If that sounds aligned, I can prepare narrow PRs with screenshots, validation notes, and clear scope boundaries.

## What To Avoid Saying

Avoid language like:

- "Please adopt this fork"
- "Please replace the current Android app with this version"
- "This is the better OpenClaw Android app"

That framing makes review much harder and creates unnecessary product tension.

## Better Framing

Prefer language like:

- "This fork helped validate a few Android-specific fixes"
- "I can split these into smaller upstream candidates"
- "I am only proposing the generally useful parts"

## Suggested First Upstream Candidates

Start with the smallest, least controversial Android fixes:

1. fullscreen video playback stability and backdrop correctness
2. media rendering correctness or fallback handling
3. gateway connection compatibility fixes that align with upstream protocol changes

Do not lead with:

- branding
- navigation/product direction rewrites
- fork-specific session naming
- local media-server operational tooling

## Evidence To Prepare Before Contact

Before opening a Discussion or PR, prepare:

- before/after screenshots
- exact device and Android version used for validation
- minimal reproduction steps
- scope boundary stating what did not change
- note explaining why the change is not tied to ClawChat2 branding

## Best Contact Path

Use the official contribution guidance in the upstream project:

- GitHub Discussions for larger features or architecture questions
- focused PRs for isolated fixes
- Discord for early signal if you want to test interest before preparing a PR

## One-Paragraph Public Description

ClawChat2 is an unofficial Android fork of the OpenClaw Android client focused on direct, chat-first interaction with OpenClaw agents. It explores a narrower Android UX and stronger media receive/render behavior, while keeping potential upstream fixes separable from fork-specific branding and product choices.
