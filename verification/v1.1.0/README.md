# 犟牛地牢 v1.1.0 验证记录

本记录对应恢复源码后重新编译的最终 APK，基线为已发布 v1.0.0 的提交 `bf33b4688cfe7381a32e833141f218a2783660c7`。本轮在本地构建与测试，不运行 GitHub Actions。重建前安装包的运行结果不计入本次最终文件。

## 当前交付与发布状态

当前交付完整 `Jiangniu-v1.1.0.apk` 与源码 ZIP。**GitHub 完整 APK 上传及源码分支发布尚未完成**；本记录不提供未发布的 v1.1.0 GitHub 下载链接。

本地构建、57 项核心测试、资源与中文、Lint、APK 签名和包体检查已完成。**最终重建 APK 的真实 v1.0.0 覆盖升级及旧档再次保存重载、新版 Android 实际交互均已通过**，分别对应下文两个独立 instrumentation 用例。

## 安装包

| 项目 | 结果 |
| --- | --- |
| 文件名 | `Jiangniu-v1.1.0.apk` |
| 应用包名 | `com.jarvistowne.jiangniu` |
| 应用版本 | `4.0.2-1.1.0`，versionCode `1100` |
| 最低 Android 版本 | Android 5.0 / API 21 |
| 文件长度 | **35,455,851 字节** |
| APK SHA-256 | `72b9f880c54ffd94c86a8d664ffb1668e660a4e9efb30196d1630f939b9d2f4e` |
| 签名证书 SHA-256 | `5c09895ed947c7f965480468dcc026af0d3a4686fcbc910d0ec5eead57a15a00` |
| 签名检查 | v1、v2 通过，证书与 v1.0.0 相同 |
| 原生库 | `armeabi-v7a`、`arm64-v8a`、`x86`、`x86_64` 四种 ABI |
| APK 对齐 | `zipalign -c -P 16 4` 检查通过 |

检查输出见 [signature.txt](signature.txt)、[apk-metadata.txt](apk-metadata.txt)、[apk-contents.txt](apk-contents.txt)；文件校验清单见 [SHA256SUMS.txt](../../downloads/SHA256SUMS.txt)。固定公开开发签名的用途与侧载范围见 [签名说明](../../tools/signing/README.md)。

## 核心逻辑测试

本次重新执行 **57 项核心测试，全部通过，0 失败、0 错误、0 跳过**。

| 测试组 | 数量 | 最终 JUnit XML |
| --- | --- | --- |
| 原有犟牛机制 | 16 | [CowMechanicsTest](TEST-com.shatteredpixel.shatteredpixeldungeon.CowMechanicsTest.xml) |
| 原有集成与存档 | 8 | [CowIntegrationTest](TEST-com.shatteredpixel.shatteredpixeldungeon.CowIntegrationTest.xml) |
| 成长与专属装备 | 10 | [CowGearEvolutionTest](TEST-com.shatteredpixel.shatteredpixeldungeon.CowGearEvolutionTest.xml) |
| 牛铃进化与配合 | 10 | [CowBellEvolutionTest](TEST-com.shatteredpixel.shatteredpixeldungeon.CowBellEvolutionTest.xml) |
| 技能熟练与彩蛋 | 13 | [CowCraftJournalTest](TEST-com.shatteredpixel.shatteredpixeldungeon.CowCraftJournalTest.xml) |

新增覆盖包括阶段的历练与等级双门槛、旧命中计数只迁移一次、实际损失与无效命中判定、有限黑锅与护盾冷却、牛铃资源消耗与交替窗口、跨层清理、视线及敌友限制、首领虚弱时间、三项能力独立熟练、空放不成长、18 项彩蛋事件入口与一次性奖励，以及压缩存档保存恢复。

## 构建、Lint 与素材

| 项目 | 本轮结果 |
| --- | --- |
| `:core:test` | 成功 |
| `:android:assembleDebug` | 成功 |
| `:android:assembleDebugAndroidTest` | 成功 |
| `:android:lintDebug` | 成功 |
| Android Lint | **0 错误、5 个已有 manifest/API 警告** |
| 图像与动画资源 | 7 张纹理、168 个动画格检查通过 |
| 中文机制文本 | 222 个中文键检查通过 |
| 原有专属天赋 | 26 项检查通过 |

Lint 的 5 个警告属于既有 manifest/API 用法：3 项 `UnusedAttribute`、1 项 `UseRequiresApi`、1 项 `ObsoleteSdkInt`。本轮使用离线检查，未查询依赖的新版本。资源输出见 [assets.txt](assets.txt)，结构化结果见 [validation.json](validation.json)。

构建环境为 JDK 17、Android SDK Platform 36、Build Tools 36.0.0，Gradle 使用仓库 wrapper。全部人物、装备、动作和插画沿用已有素材。

## Android 运行验证

本轮使用隔离 Android 9 / API 28、x86、480×800 软件模拟器。测试槽为 slot99；以下两个测试均针对本页所列 SHA-256 的最终 APK 执行并通过。

### v1.0.0 覆盖升级与旧档加载

**通过：`OK (1 test)`，耗时 110.538 秒。** 本次结果针对本页所列最终重建 APK。

本轮重新安装已发布的原 v1.0.0 APK，使用未修改的旧版测试源码生成真实 slot99。升级测试先确认旧存档版本为 `1001`、不含新增 `CowLegend`，再覆盖安装最终重建 APK，通过正常继续游戏流程加载。

已确认英雄、转职、位置、楼层、等级、生命上限、金币、装备强化、护甲能力和倔劲均保留。随后由新版保存并重新读取成功，旧命中计数只迁移一次，不会因读档重复发放历练。

- [本轮升级测试原始日志](android-upgrade/instrumentation.txt)
- [旧版存档覆盖升级截图](android-upgrade/cow-smoke/00-旧版存档覆盖升级.png)
- [升级后再次保存恢复截图](android-upgrade/cow-smoke/01-升级后再次保存恢复.png)

### 新版界面、技能与保存恢复

**通过：`OK (1 test)`，耗时 175.453 秒；留存 13 张流程截图。**

本轮完成实际选人、中文说明、开局移动、进化装备说明与牛铃动作菜单检查；通过真实触控点击打开成长手册和牛言牛语，并对完整相册执行触控滚动，确认末尾内容可以到达。查看手册与相册没有增加历练，也没有消耗倔劲或游戏回合。

通过真实点击据理力争施放，验证敌人确实损失生命、获得虚弱，英雄消耗 4 点倔劲。保存并重新读取后，阶段、历练、倔劲与全部 18 项发现标记恢复成功。原有转职与英雄护甲能力也在同一隔离流程中检查。

- [本轮新版交互测试原始日志](android-smoke/instrumentation.txt)
- [进化牛铃与动作菜单](android-smoke/cow-smoke/07-进化牛铃与动作菜单.png)
- [成长手册](android-smoke/cow-smoke/08-成长手册.png)
- [牛言牛语首屏](android-smoke/cow-smoke/09-牛言牛语首屏.png) · [触控滚动后的末屏](android-smoke/cow-smoke/10-牛言牛语末屏.png)
- [据理力争技能施放](android-smoke/cow-smoke/11-据理力争技能施放.png)
- [成长与彩蛋存档恢复](android-smoke/cow-smoke/12-成长与彩蛋存档恢复.png)

**测试范围：**英雄等级 14、90 历练、相册 18 项满记录，以及部分装备与测试敌人由测试注入，用于检查功能边界、UI 与序列化。这些截图及用例不代表自然游玩解锁或完整通关。核心逻辑测试另行验证成长、技能熟练与彩蛋的实际事件条件。长期强度与更多 Android 机型仍需实际游玩反馈。

## 本地复现

```bash
python3 tools/check_cow_assets.py
bash gradlew --no-daemon :core:test :android:assembleDebug :android:assembleDebugAndroidTest :android:lintDebug
python3 tools/check_apk.py android/build/outputs/apk/debug/android-debug.apk
bash tools/android_smoke.sh
```

Android 运行脚本允许指定游戏 APK、测试 APK、报告目录与完整测试类名，见 [tools/android_smoke.sh](../../tools/android_smoke.sh)。覆盖升级测试必须先准备真实旧版存档，不能用新版生成的数据代替。
