# Releasing ClawChat2

## English

This repository should publish Android APKs through GitHub Releases.

For now, public binaries should follow these rules:

- use a maintainer-controlled Android release keystore
- ship a signed `release` APK, not a debug APK
- mark early builds as `Pre-release`
- include SHA256 for every uploaded APK
- state clearly that ClawChat2 is an unofficial fork of the OpenClaw Android client

### Release Steps

1. Verify the public repository state.
   - Review [PUBLIC_RELEASE_CHECKLIST.md](PUBLIC_RELEASE_CHECKLIST.md).
   - Confirm no private endpoints, tokens, or personal paths are present.
2. Run the validation steps.
   - `./gradlew :app:compileDebugKotlin`
   - `./gradlew :app:testDebugUnitTest`
   - `./gradlew :app:assembleRelease`
3. Collect the output APK.
   - Expected path: `app/build/outputs/apk/release/openclaw-<version>-release.apk`
4. Generate checksum material.
   - `sha256sum app/build/outputs/apk/release/openclaw-<version>-release.apk`
5. Create a GitHub Release.
   - Title example: `ClawChat2 v0.2.3`
   - Mark as `Pre-release` while the fork is still early.
   - Attach the APK.
   - Paste bilingual notes from [RELEASE_NOTES_v0.2.3.md](RELEASE_NOTES_v0.2.3.md).
6. Smoke test the final signed APK.
   - install on emulator
   - install on at least one real Android 11+ device when practical

### Signing Notes

- The release keystore must stay out of the repository.
- Gradle expects these properties in `~/.gradle/gradle.properties`:
  - `OPENCLAW_ANDROID_STORE_FILE`
  - `OPENCLAW_ANDROID_STORE_PASSWORD`
  - `OPENCLAW_ANDROID_KEY_ALIAS`
  - `OPENCLAW_ANDROID_KEY_PASSWORD`
- Back up the release keystore before publishing public builds. Losing it will block seamless updates for existing users.

## 中文

本仓库应通过 GitHub Releases 分发 Android APK。

目前公开二进制发布建议遵循以下规则：

- 使用维护者自己控制的 Android release keystore
- 发布已签名的 `release` APK，而不是 debug APK
- 早期版本标记为 `Pre-release`
- 每个上传的 APK 都附带 SHA256
- 明确说明 ClawChat2 是基于 OpenClaw Android 客户端的非官方分叉

### 发布步骤

1. 检查公开仓库状态。
   - 阅读 [PUBLIC_RELEASE_CHECKLIST.md](PUBLIC_RELEASE_CHECKLIST.md)。
   - 确认没有私有 endpoint、token 或个人路径。
2. 执行验证。
   - `./gradlew :app:compileDebugKotlin`
   - `./gradlew :app:testDebugUnitTest`
   - `./gradlew :app:assembleRelease`
3. 收集输出 APK。
   - 预期路径：`app/build/outputs/apk/release/openclaw-<version>-release.apk`
4. 生成校验信息。
   - `sha256sum app/build/outputs/apk/release/openclaw-<version>-release.apk`
5. 创建 GitHub Release。
   - 标题示例：`ClawChat2 v0.2.3`
   - 项目仍处于早期时请勾选 `Pre-release`
   - 上传 APK
   - 使用 [RELEASE_NOTES_v0.2.3.md](RELEASE_NOTES_v0.2.3.md) 中的双语说明
6. 对最终签名 APK 做冒烟验证。
   - 在模拟器安装
   - 条件允许时，在至少一台 Android 11+ 真机安装

### 签名说明

- release keystore 不能进入仓库。
- Gradle 通过 `~/.gradle/gradle.properties` 读取以下属性：
  - `OPENCLAW_ANDROID_STORE_FILE`
  - `OPENCLAW_ANDROID_STORE_PASSWORD`
  - `OPENCLAW_ANDROID_KEY_ALIAS`
  - `OPENCLAW_ANDROID_KEY_PASSWORD`
- 对外发布前必须备份 release keystore。丢失该 keystore 会导致后续无法为已有用户平滑升级。
