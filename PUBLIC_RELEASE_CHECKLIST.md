# Public Release Checklist

Use this checklist before making the ClawChat2 fork repository public or tagging a public build.

## Identity And Attribution

- [ ] README clearly states this is an unofficial fork
- [ ] upstream origin is documented
- [ ] [LICENSE](LICENSE) is present
- [ ] [FORK_NOTES.md](FORK_NOTES.md) is present
- [ ] no text implies official OpenClaw endorsement

## Privacy And Secrets

- [ ] no real tokens are committed
- [ ] no private gateway hosts are committed
- [ ] no personal machine paths are committed
- [ ] no local-only plist or launch agent paths are presented as required public setup
- [ ] screenshots and logs are checked for private information

## Repository Hygiene

- [ ] issue templates are present under `.github/ISSUE_TEMPLATE`
- [ ] PR template is present under `.github/pull_request_template.md`
- [ ] minimum CI workflow is present under `.github/workflows`
- [ ] public docs describe the fork scope accurately
- [ ] internal-only notes are not used as the main public entrypoint

## Product Scope

- [ ] fork-only behavior is documented as fork-only
- [ ] candidate upstream fixes are separated conceptually from fork branding
- [ ] public messaging focuses on the user problem this fork solves
- [ ] early-stage status is stated clearly

## Technical Validation

- [ ] `./gradlew :app:compileDebugKotlin`
- [ ] `./gradlew :app:assembleDebug`
- [ ] `./gradlew :app:testDebugUnitTest`
- [ ] emulator install + launch
- [ ] real-device install + launch when relevant
- [ ] media receive/playback validation when media code changed

## Before Public Announcement

- [ ] decide whether to keep `ClawChat2` branding long-term
- [ ] decide whether to publish binaries or source only
- [ ] decide whether to accept outside contributions immediately or later
- [ ] prepare a short project description for GitHub repository settings
- [ ] prepare a short disclaimer for social posts and community posts

## Recommended First Public Positioning

Suggested positioning:

- unofficial Android fork
- chat-first OpenClaw agent experience
- stronger media receive/render support
- early and experimental
- not a replacement for the official OpenClaw Android client
