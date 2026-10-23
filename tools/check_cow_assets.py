#!/usr/bin/env python3
"""校验犟牛资源、图集边界及中文文案；仅依赖 Pillow，不启动游戏。"""

from pathlib import Path
import re
import sys

from PIL import Image, ImageChops

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "core/src/main/assets"
JAVA = ROOT / "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon"
HANZI = re.compile(r"[\u3400-\u9fff]")

DIMENSIONS = {
    "sprites/cow.png": (256, 128),
    "sprites/cow_avatar.png": (24, 32),
    "sprites/cow_items.png": (96, 16),
    "sprites/items_cow.png": (256, 512),
    "interfaces/cow_icons.png": (96, 16),
    "interfaces/cow_portrait.png": (32, 32),
    "splashes/cow.png": (800, 450),
}

REQUIRED_KEYS = {
    "actors.hero.heroclass.cow",
    "actors.hero.heroclass.cow_desc",
    "actors.hero.heroclass.cow_desc_short",
    "actors.hero.heroclass.cow_unlock",
    "actors.hero.herosubclass.bulldozer",
    "actors.hero.herosubclass.herdguard",
    "items.weapon.melee.oxhorn.name",
    "items.weapon.melee.oxhorn.desc",
    "items.artifacts.cowbell.name",
    "items.artifacts.cowbell.desc",
    "items.artifacts.cowbell.ac_charge",
    "items.artifacts.cowbell.ac_stomp",
    "items.armor.cowhidearmor.name",
    "items.armor.cowarmor.name",
    "items.food.warmmilk.name",
    "items.food.meadowcake.name",
    "actors.buffs.stubbornness.name",
    "actors.buffs.cowguard.name",
    "actors.buffs.cowrush.name",
    "actors.buffs.rumination.name",
    "actors.buffs.nightshift.name",
    "actors.buffs.pasturefield.name",
    "actors.hero.abilities.cow.stampede.name",
    "actors.hero.abilities.cow.pastureward.name",
    "actors.hero.abilities.cow.midnightsun.name",
}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def read_png(relative):
    path = ASSETS / relative
    require(path.is_file(), f"缺少图像：{relative}")
    with Image.open(path) as source:
        require(source.format == "PNG", f"图像格式错误：{relative}")
        require(source.size == DIMENSIONS[relative], f"图像尺寸错误：{relative}，实际{source.size}")
        source.load()
        return source.convert("RGBA")


def check_art():
    images = {name: read_png(name) for name in DIMENSIONS}
    sprite = images["sprites/cow.png"]
    alpha = sprite.getchannel("A")
    require(alpha.getextrema() == (0, 255), "主精灵须同时有透明背景和不透明像素")
    require(set(alpha.tobytes()) <= {0, 255}, "主精灵出现半透明像素，低分辨率边缘可能发虚")
    for tier in range(8):
        for frame in range(21):
            box = (frame * 12, tier * 15, (frame + 1) * 12, (tier + 1) * 15)
            frame_alpha = alpha.crop(box)
            require(frame_alpha.getbbox() is not None, f"空动画格：护甲阶段{tier}，帧{frame}")
            require(0 in frame_alpha.tobytes(), f"动画格没有透明底：阶段{tier}，帧{frame}")
    require(alpha.crop((252, 0, 256, 128)).getbbox() is None, "主精灵右侧填充区出现意外像素")
    require(alpha.crop((0, 120, 256, 128)).getbbox() is None, "主精灵底部填充区出现意外像素")

    for name in ("sprites/cow_items.png", "interfaces/cow_icons.png"):
        sheet = images[name]
        require(set(sheet.getchannel("A").tobytes()) <= {0, 255}, f"图标含半透明像素：{name}")
        for index in range(6):
            require(sheet.crop((index * 16, 0, (index + 1) * 16, 16)).getchannel("A").getbbox(),
                    f"缺少第{index}个图标：{name}")

    original_path = ASSETS / "sprites/items.png"
    require(original_path.is_file(), "缺少原版物品图集")
    with Image.open(original_path) as source:
        original = source.convert("RGBA")
    merged = images["sprites/items_cow.png"]
    region = (0, 192, 96, 208)
    require(original.size == merged.size, "合成图集尺寸与原版不同")
    require(original.crop(region).getchannel("A").getbbox() is None, "犟牛图标覆盖了原版非空物品格")
    require(merged.crop(region).tobytes() == images["sprites/cow_items.png"].tobytes(),
            "合成图集与独立犟牛物品图标不一致")
    restored = merged.copy()
    restored.paste(original.crop(region), (0, 192))
    require(restored.tobytes() == original.tobytes(), "合成图集修改了第13行六格以外的原版像素")
    require(ImageChops.difference(original, merged).getbbox() is not None, "合成图集未包含新增物品")

    assets_java = (JAVA / "Assets.java").read_text(encoding="utf-8")
    for relative in DIMENSIONS:
        if relative == "sprites/cow_items.png":
            continue  # 独立导入源，实际物品通过合成图集使用。
        require('"' + relative + '"' in assets_java, f"Assets.java 未声明资源：{relative}")
    print("通过：7张游戏图像、168格动画、12格图标、图集变更边界与资源路径。")


def unescape(text, location):
    """实现 Java properties 的转义规则，保留中文原字符。"""
    out = []
    index = 0
    translations = {"t": "\t", "r": "\r", "n": "\n", "f": "\f"}
    while index < len(text):
        char = text[index]
        index += 1
        if char != "\\":
            out.append(char)
            continue
        require(index < len(text), f"结尾转义未闭合：{location}")
        char = text[index]
        index += 1
        if char == "u":
            digits = text[index:index + 4]
            require(len(digits) == 4 and re.fullmatch(r"[0-9a-fA-F]{4}", digits),
                    f"无效的Unicode转义：{location}")
            out.append(chr(int(digits, 16)))
            index += 4
        else:
            out.append(translations.get(char, char))
    return "".join(out)


def properties(path):
    raw = path.read_text(encoding="utf-8-sig")
    require("\x00" not in raw and "\ufffd" not in raw, f"文案出现无效字符：{path.name}")
    pending = ""
    first_line = 0
    for number, line in enumerate(raw.splitlines(), 1):
        if pending:
            line = pending + line.lstrip(" \t\f")
        else:
            first_line = number
            line = line.lstrip(" \t\f")
            if not line or line[0] in "#!":
                continue
        slashes = len(line) - len(line.rstrip("\\"))
        if slashes % 2:
            pending = line[:-1]
            continue
        pending = ""
        location = f"{path.name}:{first_line}"
        separator = len(line)
        escaped = False
        for index, char in enumerate(line):
            if not escaped and char in "=: \t\f":
                separator = index
                break
            escaped = not escaped if char == "\\" else False
        key = unescape(line[:separator], location)
        rest = line[separator:].lstrip(" \t\f")
        if rest.startswith(("=", ":")):
            rest = rest[1:].lstrip(" \t\f")
        value = unescape(rest, location)
        require(key and key == key.lower(), f"空键或非小写键：{location}")
        yield key, value, location
    require(not pending, f"文件末尾有未完成的续行：{path.name}")


def check_text():
    entries = {}
    locations = {}
    for filename in ("hero.properties", "mechanics.properties"):
        path = ASSETS / "messages/cow" / filename
        require(path.is_file(), f"缺少中文文案：{filename}")
        for key, value, location in properties(path):
            require(key not in entries, f"重复文案键：{key}，位于{locations.get(key)}与{location}")
            require(not value or HANZI.search(value), f"新增非空文案没有汉字：{location}，键{key}")
            require("!!!NO TEXT FOUND!!!" not in value, f"文案包含缺失占位符：{location}")
            entries[key] = value
            locations[key] = location
    for key in sorted(REQUIRED_KEYS):
        require(entries.get(key), f"缺少必需中文文案：{key}")
    talent_java = (JAVA / "actors/hero/Talent.java").read_text(encoding="utf-8")
    talent_names = set(re.findall(r"\b(COW_[A-Z_]+)\s*\(\s*\d+", talent_java))
    require(len(talent_names) >= 20, "未找到完整的犟牛天赋定义")
    for talent in talent_names:
        for suffix in ("title", "desc"):
            key = "actors.hero.talent." + talent.lower() + "." + suffix
            require(entries.get(key), f"天赋缺少中文：{key}")
    messages_java = (JAVA / "messages/Messages.java").read_text(encoding="utf-8")
    for bundle in ("hero", "mechanics"):
        require(f'"messages/cow/{bundle}"' in messages_java, f"消息系统未加载中文扩展包：{bundle}")
    print(f"通过：{len(entries)}条中文文案可解析、无重复键，{len(talent_names)}项天赋文案完整。")


def main():
    try:
        check_art()
        check_text()
    except (ValueError, OSError) as error:
        print(f"资源校验失败：{error}", file=sys.stderr)
        return 1
    print("犟牛资源与文案校验全部通过。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
