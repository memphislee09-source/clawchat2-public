# Public Release Checklist

## English

Use this checklist before making the ClawChat2 fork repository public or tagging a public build.

### Identity And Attribution

- [ ] README clearly states this is an unofficial fork
- [ ] upstream origin is documented
- [ ] [LICENSE](LICENSE) is present
- [ ] [FORK_NOTES.md](FORK_NOTES.md) is present
- [ ] no text implies official OpenClaw endorsement

### Privacy And Secrets

- [ ] no real tokens are committed
- [ ] no private gateway hosts are committed
- [ ] no personal machine paths are committed
- [ ] no local-only plist or launch agent paths are presented as required public setup
- [ ] screenshots and logs are checked for private information

### Repository Hygiene

- [ ] issue templates are present under `.github/ISSUE_TEMPLATE`
- [ ] PR template is present under `.github/pull_request_template.md`
- [ ] minimum CI workflow is present under `.github/workflows`
- [ ] public docs describe the fork scope accurately
- [ ] internal-only notes are not used as the main public entrypoint

### Technical Validation

- [ ] `./gradlew :app:compileDebugKotlin`
- [ ] `./gradlew :app:assembleDebug`
- [ ] `./gradlew :app:testDebugUnitTest`
- [ ] `./gradlew :app:assembleRelease`
- [ ] emulator install + launch
- [ ] real-device install + launch when relevant

### Public APK Release

- [ ] a signed `release` APK is produced
- [ ] SHA256 is recorded for the uploaded APK
- [ ] GitHub Release notes are prepared in English and Chinese
- [ ] the release is marked `Pre-release` while the fork is still early
- [ ] the release notes clearly state this is an unofficial OpenClaw fork

## 中文

在将 ClawChat2 仓库设为公开或打公开版本标签之前，请使用这份清单。

### 身份与归属

- [ ] README 已明确说明这是非官方分叉
- [ ] 已明确记录上游来源
- [ ] 已包含 [LICENSE](LICENSE)
- [ ] 已包含 [FORK_NOTES.md](FORK_NOTES.md)
- [ ] 没有任何文字暗示官方背书

### 隐私与敏感信息

- [ ] 没有提交真实 token
- [ ] 没有提交私有 gateway host
- [ ] 没有提交个人机器路径
- [ ] 没有把仅适用于本地机器的 plist 或 launch agent 路径写成公开必需流程
- [ ] 截图和日志已检查过是否包含隐私信息

### 仓库卫生

- [ ] `.github/ISSUE_TEMPLATE` 下已有 issue 模板
- [ ] `.github/pull_request_template.md` 已存在
- [ ] `.github/workflows` 下已有最小 CI
- [ ] 公开文档准确描述了本分叉范围
- [ ] 内部笔记没有被当作公开主入口

### 技术验证

- [ ] `./gradlew :app:compileDebugKotlin`
- [ ] `./gradlew :app:assembleDebug`
- [ ] `./gradlew :app:testDebugUnitTest`
- [ ] `./gradlew :app:assembleRelease`
- [ ] 模拟器安装和启动验证
- [ ] 相关场景下完成真机安装和启动验证

### 公开 APK 发布

- [ ] 已产出已签名的 `release` APK
- [ ] 已记录上传 APK 的 SHA256
- [ ] GitHub Release 说明已准备好中英文版本
- [ ] 项目仍处于早期时，发布条目已标记为 `Pre-release`
- [ ] Release 说明已明确写明这是非官方 OpenClaw 分叉
