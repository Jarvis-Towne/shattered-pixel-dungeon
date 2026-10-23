# 体验版签名

`jiangniu-debug.keystore` 是本项目专门生成并公开的**开发测试密钥**，不是生产签名秘密。

- 别名：`androiddebugkey`。
- 密码：`android`（公开 Android 调试约定）。
- 用途：让每次 GitHub Actions 构建的体验版使用同一签名，允许覆盖安装更新并保留存档。
- 包名：`com.jarvistowne.jiangniu`，与原版分开安装、分开存档。
- 适用范围：个人安装、测试；不得将此公开密钥用于应用商店或可信发行。

正式发行时应创建私有发行签名，通过 GitHub Actions secrets 注入。公开签名版迁移到私有签名版通常需要卸载或使用新的包名，请先导出存档。

