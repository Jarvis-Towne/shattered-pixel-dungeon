# 使用 GitHub Actions 构建犟牛地牢 APK

## 已验证的 v1.1.0 构建

- 源码分支：[`jiangniu-v1.1.0`](https://github.com/Jarvis-Towne/shattered-pixel-dungeon/tree/jiangniu-v1.1.0)。
- 自动构建工作流：[build-jiangniu-apk.yml](../.github/workflows/build-jiangniu-apk.yml)。
- 通过的完整 CI 运行：[GitHub Actions #40058044477](https://github.com/Jarvis-Towne/shattered-pixel-dungeon/actions/runs/40058044477)（构建、核心测试、Lint、签名校验、Release 发布均成功）。
- **直接下载 APK：** [Jiangniu-v1.1.0.apk](https://github.com/Jarvis-Towne/shattered-pixel-dungeon/releases/download/jiangniu-v1.1.0-apk/Jiangniu-v1.1.0.apk)。
- [GitHub Release 详情与签名、校验附件](https://github.com/Jarvis-Towne/shattered-pixel-dungeon/releases/tag/jiangniu-v1.1.0-apk)。

Release 中的 `Jiangniu-v1.1.0.apk` 可以直接侧载安装；不需要安装 Android Studio，也不需要下载源码。使用 Android 系统文件管理器打开 APK，按提示允许该来源安装应用即可。已经安装同包名、同证书的 v1.0.0 时，可以直接覆盖升级；**不应先卸载**，否则应用数据可能丢失。

## 如何再次构建

GitHub Actions 会在向 **`jiangniu-v1.1.0` 分支推送新提交**时自动运行，无需手动上传 APK。源代码或 Gradle 配置变更以后提交并推送即可；构建成功后，工作流会生成 Actions Artifact 并将通过验证的 APK 更新到 GitHub Release。

也可以在仓库 Actions 页面选中最新的成功构建，使用 GitHub 提供的 `Re-run all jobs` 复用同一提交重新构建。工作流含有 `workflow_dispatch`，但 GitHub 通常要求工作流同时存在于默认分支，才会在网页上显示手动的 `Run workflow` 按钮。本项目目前在专用分支维护构建文件，因此**以分支推送自动构建为主**，不依赖手动运行按钮。

## 自动化步骤

1. 使用 Ubuntu 24.04、Temurin JDK 17、Android SDK Platform 36、Build Tools 36.0.0 和仓库自带 Gradle wrapper。
2. 用 Pillow 执行 `python tools/check_cow_assets.py`，检查像素图集及中文文本。
3. 执行 `bash ./gradlew --no-daemon :core:test :android:assembleDebug :android:assembleDebugAndroidTest :android:lintDebug`。这会执行核心单元测试、编译可安装的 Debug APK 及 Android 测试 APK，并执行静态 Lint 检查。
4. 使用 `tools/check_apk.py` 检查游戏资源及 ARMv7、ARM64、x86、x86_64 四种原生库，执行 `zipalign -c -P 16 4` 与 `apksigner verify --verbose --print-certs`。
5. 检查签名证书 SHA-256 一致后，上传 APK、`SHA256SUMS.txt`、`signature.txt` 到 Actions Artifact；随后下载 Artifact 并通过 GitHub CLI 发布为 GitHub Release 的三个附件。

注意：GitHub Actions 在此流程中**构建 Android 测试 APK，但不会启动模拟器运行 instrumentation 测试**。真实交互和旧档升级验证已在此前本地构建中完成，记录见 [v1.1.0 验证报告](../verification/v1.1.0/README.md)。两种构建不应混为一项完全相同的实机测试结论。

## 校验安装包

当前 GitHub Release 上传的 APK：

| 项目 | 值 |
| --- | --- |
| 文件 | `Jiangniu-v1.1.0.apk` |
| 长度 | 35,455,915 字节 |
| APK SHA-256 | `4f35eef634c3a3c74faebb0ef748c9569431ee4649c26d0b0974c43b68571458` |
| 包名 | `com.jarvistowne.jiangniu` |
| `versionCode` | `1100` |
| 固定签名证书 SHA-256 | `5c09895ed947c7f965480468dcc026af0d3a4686fcbc910d0ec5eead57a15a00` |
| 签名检查 | APK Signature Scheme v1、v2 均通过 |

从 [Release 附件](https://github.com/Jarvis-Towne/shattered-pixel-dungeon/releases/tag/jiangniu-v1.1.0-apk) 同时下载 `SHA256SUMS.txt` 和 `signature.txt`，可以离线核验 SHA-256 和签名。

**区分构建文件：**先前本地交付的 APK 长度为 35,455,851 字节，SHA-256 为 `72b9f880c54ffd94c86a8d664ffb1668e660a4e9efb30196d1630f939b9d2f4e`。这是另一份构建产物，不应用它的校验值判断从 GitHub Releases 下载的 APK 是否正确。

## 签名与分发注意事项

`:android:assembleDebug` 使用仓库 `tools/signing/jiangniu-debug.keystore` 中的**公开固定体验版密钥**。这种签名可以保证与采用同一密钥的 v1.0.0 侧载版本覆盖安装，但不是适用于商店发布、可信身份认证或高安全性分发的生产密钥。若迁移到私有正式密钥，必须另外规划已有安装的签名与数据迁移。签名背景详见 [签名说明](../tools/signing/README.md)。

Actions 页面提供的 APK Artifact 下载是 ZIP，需先解压；GitHub Releases 提供真正的 `.apk` 文件直链。Artifact 保留 90 天，Release 安装包独立保留，直到发布者删除或更新。
