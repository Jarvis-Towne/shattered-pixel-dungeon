#!/usr/bin/env python3
"""验证真正的 Android 安装包包含四种架构和新增运行资源。"""
import sys
import zipfile

with zipfile.ZipFile(sys.argv[1]) as apk:
    names = set(apk.namelist())
    for required in [
        'AndroidManifest.xml', 'classes.dex',
        'assets/sprites/cow.png', 'assets/sprites/items_cow.png',
        'assets/splashes/cow.png', 'assets/messages/cow/hero.properties',
        'assets/messages/cow/mechanics.properties',
    ]:
        assert required in names, f'安装包缺少资源：{required}'
    for abi in ['armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64']:
        assert f'lib/{abi}/libgdx.so' in names, f'缺少原生库：{abi}'
        assert f'lib/{abi}/libgdx-freetype.so' in names, f'缺少字体库：{abi}'
print('安装包内容检查通过：游戏代码、奶牛资源、四种架构及字体原生库完整。')

