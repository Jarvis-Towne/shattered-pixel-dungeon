# 犟牛地牢体验版签名

`jiangniu-debug.keystore` 是本项目公开的开发测试密钥，用于保持不同体验版本之间的签名一致。

| 项目 | 值 |
| --- | --- |
| 包名 | `com.jarvistowne.jiangniu` |
| 密钥别名 | `androiddebugkey` |
| 密钥库和密钥密码 | `android`，公开 Android 调试约定 |
| 固定证书 SHA-256 | `5c09895ed947c7f965480468dcc026af0d3a4686fcbc910d0ec5eead57a15a00` |
| v1.1.0 的 versionCode | `1100`，高于 v1.0.0 的 `1001` |

最终重建的 v1.1.0 APK 已通过 v1、v2 签名校验，证书 SHA-256 与上表及 v1.0.0 一致，包名保持相同。**最终重建 APK 的真实旧版覆盖安装与旧档加载测试已通过**（`OK (1 test)`，110.538 秒）：原 v1.0.0 APK 实际生成的 `1001` 版存档可正常继续游戏，角色装备和倔劲等状态保留；新版再次保存、重载成功，旧计数只迁移一次。检查输出、升级日志与两张截图见 [本轮验证记录](../../verification/v1.1.0/README.md)。

本版 APK 在本地编译与测试，不通过 GitHub Actions 构建。当前交付完整 APK 和源码 ZIP；GitHub 完整 APK 上传及源码分支发布仍待完成。构建配置从本目录读取固定签名文件，无需重新生成密钥。

## 验证方法

在 Android SDK Build Tools 所在目录使用 `apksigner`：

```bash
apksigner verify --verbose --print-certs Jiangniu-v1.1.0.apk
```

对比输出中的证书 SHA-256 与上表。APK 文件本身的 SHA-256 是另一项校验：本次最终文件为 **35,455,851 字节**，SHA-256 为 `72b9f880c54ffd94c86a8d664ffb1668e660a4e9efb30196d1630f939b9d2f4e`，已记录于 [SHA256SUMS.txt](../../downloads/SHA256SUMS.txt)。此数值只对应本次重建文件。

此公开密钥只用于体验版，不适合应用商店或需要可信签名身份的正式发行。将来若转为私有发行签名，应在单独的发行流程中管理密钥，并明确新旧签名应用的迁移方式；不要直接替换现有体验版签名后声称可无损覆盖升级。
