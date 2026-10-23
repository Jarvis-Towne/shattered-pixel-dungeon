#!/usr/bin/env python3
"""将 imagegen 原稿导入 SPD 的固定像素图集；不会修改原版图集。

用法：python tools/cow_assets/import_pixel_art.py 精灵原稿.png 图标原稿.png 选人原稿.png
依赖：Pillow、numpy。只做裁切、缩放、有限调色板和游戏图集排版。
"""
from pathlib import Path
import argparse
import hashlib
import json

import numpy as np
from PIL import Image, ImageOps

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "core/src/main/assets"
PALETTE = [
    (31, 29, 32), (61, 55, 53), (96, 83, 74), (151, 135, 110),
    (205, 190, 160), (248, 239, 215), (255, 248, 231),
    (176, 103, 81), (232, 161, 130), (255, 202, 162),
    (47, 116, 120), (79, 161, 162), (136, 196, 186),
    (122, 76, 39), (188, 126, 52), (241, 190, 91),
    (76, 116, 49), (143, 167, 65), (215, 224, 147),
    (72, 107, 142), (127, 174, 197), (205, 227, 234),
]


def quantize(sprite, palette=PALETTE):
    arr = np.asarray(sprite.convert("RGBA")).copy()
    colors = np.array(palette, dtype=np.int32)
    rgb = arr[:, :, :3].astype(np.int32)
    distances = ((rgb[:, :, None, :] - colors[None, None, :, :]) ** 2).sum(axis=-1)
    arr[:, :, :3] = colors[distances.argmin(axis=-1)]
    arr[:, :, 3] = np.where(arr[:, :, 3] >= 128, 255, 0)
    arr[arr[:, :, 3] == 0] = 0
    return Image.fromarray(arr, "RGBA")


def crop_alpha(image):
    alpha = image.getchannel("A").point(lambda p: 255 if p >= 128 else 0)
    return image.crop(alpha.getbbox())


def fit_sprite(image, width, height, max_scale=None, bottom=True):
    image = crop_alpha(image)
    scale = min(width / image.width, height / image.height)
    if max_scale is not None:
        scale = min(scale, max_scale)
    size = (max(1, round(image.width * scale)), max(1, round(image.height * scale)))
    # BOX aggregates a connected source cluster before hard palette/alpha snapping.
    return quantize(image.resize(size, Image.Resampling.BOX))


def extract_grid(path, columns, y_bounds):
    source = Image.open(path).convert("RGBA")
    out = []
    for y0, y1 in zip(y_bounds[:-1], y_bounds[1:]):
        for column in range(columns):
            x0 = round(column * source.width / columns)
            x1 = round((column + 1) * source.width / columns)
            out.append(crop_alpha(source.crop((x0, y0, x1, y1))))
    return out


def armor_variant(tile, tier):
    # The scarf and lower bib change with armor; face, horns, eyebrows and muzzle stay intact.
    if tier == 0:
        return tile.copy()
    tiers = [None,
        [(97, 76, 58), (164, 131, 87), (222, 192, 135)],
        [(79, 69, 53), (128, 104, 72), (185, 158, 113)],
        [(64, 77, 89), (110, 132, 145), (171, 190, 194)],
        [(55, 66, 87), (98, 121, 145), (155, 180, 198)],
        [(116, 83, 42), (186, 143, 67), (240, 202, 114)],
        [(54, 95, 101), (80, 159, 163), (172, 224, 210)],
        [(78, 65, 109), (138, 112, 173), (206, 184, 224)],
    ]
    arr = np.asarray(tile).copy()
    for index, color in enumerate(PALETTE[10:13]):
        mask = np.all(arr[:, :, :3] == color, axis=-1) & (arr[:, :, 3] > 0)
        arr[mask, :3] = tiers[tier][index]
    return Image.fromarray(arr, "RGBA")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("sprites", type=Path)
    parser.add_argument("icons", type=Path)
    parser.add_argument("splash", type=Path)
    args = parser.parse_args()
    sprites = extract_grid(args.sprites, 7, [0, 313, 570, 821])
    assert len(sprites) == 21
    atlas = Image.new("RGBA", (256, 128))
    baseline = 14
    for index, source in enumerate(sprites):
        sprite = fit_sprite(source, 11, 14, max_scale=14 / 236)
        tile = Image.new("RGBA", (12, 15))
        y = baseline - sprite.height
        if index == 18:  # flight keeps its feet above the floor
            y = max(0, y - 1)
        tile.alpha_composite(sprite, ((13 - sprite.width) // 2, y))
        for tier in range(8):
            atlas.alpha_composite(armor_variant(tile, tier), (index * 12, tier * 15))
    atlas.save(ASSETS / "sprites/cow.png")

    raw_icons = extract_grid(args.icons, 6, [0, 360, 724])
    assert len(raw_icons) == 12
    icon_tiles = []
    for source in raw_icons:
        icon = fit_sprite(source, 15, 15)
        tile = Image.new("RGBA", (16, 16))
        tile.alpha_composite(icon, ((16 - icon.width) // 2, (16 - icon.height) // 2))
        icon_tiles.append(tile)
    for name, start in [("sprites/cow_items.png", 0), ("interfaces/cow_icons.png", 6)]:
        sheet = Image.new("RGBA", (96, 16))
        for index, tile in enumerate(icon_tiles[start:start + 6]):
            sheet.alpha_composite(tile, (index * 16, 0))
        sheet.save(ASSETS / name)
    merged = Image.open(ASSETS / "sprites/items.png").convert("RGBA")
    assert merged.crop((0, 192, 96, 208)).getchannel("A").getbbox() is None
    for index, tile in enumerate(icon_tiles[:6]):
        merged.alpha_composite(tile, (index * 16, 192))
    merged.save(ASSETS / "sprites/items_cow.png")

    portrait = fit_sprite(raw_icons[6], 31, 31)
    portrait_tile = Image.new("RGBA", (32, 32))
    portrait_tile.alpha_composite(portrait, ((32 - portrait.width) // 2, (32 - portrait.height) // 2))
    portrait_tile.save(ASSETS / "interfaces/cow_portrait.png")
    victory = fit_sprite(sprites[20], 23, 30)
    avatar = Image.new("RGBA", (24, 32))
    avatar.alpha_composite(victory, ((24 - victory.width) // 2, 31 - victory.height))
    avatar.save(ASSETS / "sprites/cow_avatar.png")
    splash = Image.open(args.splash).convert("RGB")
    splash = ImageOps.fit(splash, (400, 225), method=Image.Resampling.BOX)
    splash.resize((800, 450), Image.Resampling.NEAREST).save(ASSETS / "splashes/cow.png")

    preview = Image.new("RGBA", (672, 400), (28, 31, 38, 255))
    preview.alpha_composite(atlas.crop((0, 0, 252, 120)).resize((504, 240), Image.Resampling.NEAREST), (16, 16))
    for index, tile in enumerate(icon_tiles):
        preview.alpha_composite(tile.resize((48, 48), Image.Resampling.NEAREST), (16 + 52 * index, 280))
    preview.alpha_composite(avatar.resize((48, 64), Image.Resampling.NEAREST), (560, 64))
    preview.alpha_composite(portrait_tile.resize((64, 64), Image.Resampling.NEAREST), (552, 160))
    preview.convert("RGB").save(ROOT / "docs/art/cow-pixel-preview.png")
    metadata_path = ROOT / "docs/art/cow-art-sources.json"
    metadata = json.loads(metadata_path.read_text()) if metadata_path.exists() else {}
    metadata.update({key: {"file": value.name, "sha256": hashlib.sha256(value.read_bytes()).hexdigest()}
                     for key, value in vars(args).items()})
    metadata_path.write_text(json.dumps(metadata, ensure_ascii=False, indent=2) + "\n")


if __name__ == "__main__":
    main()
