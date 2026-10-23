# 🐮 犟牛地牢 · Jiangniu Pixel Dungeon

> **桌面上那个总在皱眉的「牛哞」，真的闯进地牢了！** 这是基于 [Shattered Pixel Dungeon 4.0.2](https://github.com/00-Evan/shattered-pixel-dungeon) 的中文同人扩展：在保留原版六名英雄的基础上，增加可在开局直接选择的第七英雄 **「犟牛」**，配备自己的武器、职业道具、技能、天赋、转职与隐藏彩蛋。

[**⬇ 下载 Android 安装包（v1.0.0）**](https://raw.githubusercontent.com/Jarvis-Towne/shattered-pixel-dungeon/master/downloads/Jiangniu-v1.0.0.apk) · [APK SHA-256 校验](downloads/SHA256SUMS.txt) · [详细玩法与构建说明](docs/jiangniu-guide.md) · [版本更新说明](docs/jiangniu-release-notes.md)

<p align="center"><img src="docs/art/cow-pixel-preview.png" width="520" alt="犟牛像素化游戏角色、装备和技能示意" /></p>

## 🌟 从 Codex 桌宠「牛哞」到地牢英雄

角色原型是 **Jarvis 创作的 Codex 桌宠「牛哞」**：黑白花纹、短角、粉色鼻头、标志性的粗眉毛，以及「我就是不服」的表情。游戏将它重绘为符合 SPD 画风的小尺寸像素人物，并实现行走、攻击、受伤、死亡、操作等动作和不同装备外观。

**[▶ 在 Codex Pets 查看、下载原始「牛哞」桌宠](https://codex-pets.net/#/pets/niumou)**（[可分享的作品介绍页](https://codex-pets.net/share/niumou)）

<details>
<summary>展开查看原始桌宠素材（用户提供的精灵图集）</summary>

<img src="docs/art/niumou-original-spritesheet.webp" width="740" alt="Codex 桌宠牛哞的原始动态动作精灵图集" />

[查看原图 / 保存原始精灵图](docs/art/niumou-original-spritesheet.webp)

</details>

<p align="center"><img src="docs/art/jiangniu-game-preview.png" width="620" alt="犟牛地牢游戏预览" /></p>

## ⚔️ 英雄特色

**核心资源「倔劲」：** 普通攻击造成有效伤害或承受敌人直接伤害时获得倔劲；初始 3 点，上限 6 点。消耗倔劲发动职业技能。不能通过原地等待白拿资源。

| 专属内容 | 功能 |
| --- | --- |
| **犟角** | 可升级、可附魔的专属角锤，伤害与等级、倔劲、天赋联动。 |
| **不服铃** | 不占饰品栏的职业道具，消耗 3 点倔劲发动 **顶撞** 或 **踏地**。 |
| **顶撞** | 沿直线向敌人突进，距离越远威力越高；遇到墙壁、陷阱或新出现的敌人会受到真实地形规则限制。 |
| **踏地** | 攻击相邻敌人、施加残废并获得短时护盾。 |
| **拼斑围兜** | 可升级、可刻印的起始护甲；倔劲充足时提高防护。 |
| **温热牛乳** | 有限的补给食物，缓解饥饿并逐回合恢复生命。 |
| **星牧战铠** | 击败矮人王后获得的专属英雄护甲，搭载一种终局能力。 |

### 🌿 双转职与终局能力

| 转职 | 风格 |
| --- | --- |
| **撼地蛮牛** | 进攻型。强化顶撞和后续攻击，让冲锋形成连击。 |
| **守夜牧卫** | 防守型。通过牛铃产生护盾、保护友方，并获得有限恢复。 |

后期可从 **「万蹄奔雷」**（线性冲击）、**「方寸牧场」**（区域守护）和 **「夜半开工」**（临时强化）中选择一种英雄护甲能力。完整体系还包含 **26 项专属天赋、6 种新增状态**，以及仅在特定地点或特定时机出现的彩蛋。

> **操作提示：** 点击快捷栏牛铃瞄准敌人发动「顶撞」；要使用「踏地」，从背包打开「不服铃」的道具菜单。想体验彩蛋，可以多留意草丛、镜子和小牛的坚持。

## 📱 下载与安装

1. [直接下载 **Jiangniu-v1.0.0.apk**](https://raw.githubusercontent.com/Jarvis-Towne/shattered-pixel-dungeon/master/downloads/Jiangniu-v1.0.0.apk)（约 34 MB），无需运行任何构建任务。
2. 在 Android 5.0 或更新系统上安装，必要时允许当前浏览器/文件管理器安装应用。
3. 打开 **「犟牛地牢」**，在开局选人界面选择第七位英雄「犟牛」。

- **独立包名：** `com.jarvistowne.jiangniu`；可与原版共存，存档互不覆盖。
- **语言：** 首次运行默认简体中文，原创游戏内容全部提供中文说明。
- **签名：** 使用固定公开开发签名的体验版，适合侧载测试，不属于商店正式发行版本。
- **完整性校验：** `10510d86e256e190c34be008bb1243a1595c1f323f114f068362fa4d10aaa21a`（SHA-256）。

## 🧪 验证与源码

当前版本完成 **24 项核心逻辑测试**、**Android 9 模拟器安装与交互验证**，包括开局选人、战斗技能、转职、护甲技能与存档恢复；Lint 检查为 0 错误。本次 APK 由已验证的本地构建直接上传，**未通过 GitHub Actions 重新构建或重新运行测试**。完整真机和长线平衡测试仍有待进一步验证。

- **游戏源码：** 直接浏览本仓库 `core/`、`android/`、`docs/`、`tools/` 等目录，或使用 GitHub 的「Code → Download ZIP」下载。
- **使用说明：** [docs/jiangniu-guide.md](docs/jiangniu-guide.md)
- **原始桌宠与像素美术：** [docs/art/](docs/art/)；[美术制作说明](docs/art/犟牛美术说明.md)
- **本地构建：** JDK 17、Android SDK Platform 36、Build Tools 36.0.0，执行 `gradlew.bat :android:assembleDebug`（Windows）或 `./gradlew :android:assembleDebug`（Linux）。

## 📄 开源与致谢

本项目基于 **[Shattered Pixel Dungeon](https://github.com/00-Evan/shattered-pixel-dungeon)**（Evan Debenham）和 **[Pixel Dungeon](https://github.com/00-Evan/pixel-dungeon-gradle)**（Watabou）的源代码，遵守仓库保留的 **[GPL-3.0 许可证](LICENSE.txt)**。这是个人改作项目，**并非原版 SPD 官方发布**。桌宠形象来自作者 Jarvis 提供的「牛哞」原始素材，桌宠下载页面见上文。
