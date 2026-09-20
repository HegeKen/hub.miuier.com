#!/usr/bin/env python3
"""从 logo 生成 Android 启动图标（各密度 + 自适应图标）。

用法：
    python3 scripts/make_icons.py [源图.png]

默认源图是脚本同目录的 `favion.png`。需要 Pillow（pip install pillow）。

产物写入 `composeApp/src/androidMain/res/`：

    mipmap-anydpi-v26/ic_launcher.xml         自适应图标（API 26+ 走这条）
    mipmap-anydpi-v26/ic_launcher_round.xml
    drawable-<dpi>/ic_launcher_foreground.png 自适应前景：108dp 画布，图形居中缩到 72dp 安全区
    mipmap-<dpi>/ic_launcher.png              旧版图标（API < 26），图形压白底
    mipmap-<dpi>/ic_launcher_round.png        同上，圆形裁切

**这里只做缩放，不动源图内容**：图标就是 logo 本身按各密度尺寸等比重采样（LANCZOS），
不改颜色、不抠字标。曾经有一版会先把 `MiROMS HUB` 字标抹掉（理由是同尺寸下认不出来），
那样虽然干净，但等于把 logo 的细节改掉了 —— 不做。

源图保持**原始分辨率**。曾经把它降到 1024 再生成：后果其实很小
（192px 下平均差 0.10/255），但 432px 前景有约 0.9% 的像素在边缘处会软化，
既然原图就在手边，没必要多降一次。
"""

import argparse
import os
import sys

from PIL import Image, ImageDraw

# dp -> px 的倍率
DENSITIES = {
    'mdpi': 1.0,
    'hdpi': 1.5,
    'xhdpi': 2.0,
    'xxhdpi': 3.0,
    'xxxhdpi': 4.0,
}
LEGACY_DP = 48          # 旧版图标尺寸
FOREGROUND_DP = 108     # 自适应图标前景画布
SAFE_ZONE_DP = 72       # 自适应图标安全区（108 画布居中 72，超出部分可能被启动器裁掉）
LEGACY_CONTENT = 0.88   # 旧版图标里图形占的比例，留一圈白边

WHITE = (255, 255, 255, 255)


def centered(img, size, content_ratio):
    """把 img 等比缩放到 size*content_ratio，居中放到 size 的透明画布上"""
    canvas = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    inner = max(1, round(size * content_ratio))
    resized = img.resize((inner, inner), Image.LANCZOS)
    off = (size - inner) // 2
    canvas.alpha_composite(resized, (off, off))
    return canvas


def on_white(img, size, content_ratio):
    canvas = Image.new('RGBA', (size, size), WHITE)
    canvas.alpha_composite(centered(img, size, content_ratio))
    return canvas.convert('RGB')


def rounded(img, size, content_ratio):
    """圆形裁切版本：先生成白底方形图标，再套一个圆形 alpha 蒙版"""
    square = on_white(img, size, content_ratio).convert('RGBA')
    mask = Image.new('L', (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    square.putalpha(mask)
    return square


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    default_source = os.path.join(here, 'favion.png')
    res_dir = os.path.join(here, '..', 'composeApp', 'src', 'androidMain', 'res')

    parser = argparse.ArgumentParser()
    parser.add_argument('source', nargs='?', default=default_source,
                        help='方形 logo 源图（默认 scripts/favion.png）')
    parser.add_argument('--res', default=res_dir, help='Android res 目录')
    args = parser.parse_args()

    source = os.path.abspath(args.source)
    if not os.path.exists(source):
        sys.exit(f'找不到源图：{source}')
    res = os.path.abspath(args.res)

    logo = Image.open(source).convert('RGBA')
    if logo.width != logo.height:
        sys.exit('源图需要是正方形')
    print(f'源图 {source}  {logo.width}×{logo.height}')

    written = []

    def save(img, rel):
        path = os.path.join(res, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        img.save(path)
        written.append((rel, os.path.getsize(path)))

    for dpi, scale in DENSITIES.items():
        # 自适应图标前景：108dp 画布，图形缩到 72dp 安全区，任何蒙版都不会裁到
        fg = round(FOREGROUND_DP * scale)
        save(centered(logo, fg, SAFE_ZONE_DP / FOREGROUND_DP),
             f'drawable-{dpi}/ic_launcher_foreground.png')

        # 旧版图标（API < 26）：没有蒙版，直接给白底方形 + 圆形两个版本
        legacy = round(LEGACY_DP * scale)
        save(on_white(logo, legacy, LEGACY_CONTENT), f'mipmap-{dpi}/ic_launcher.png')
        save(rounded(logo, legacy, LEGACY_CONTENT), f'mipmap-{dpi}/ic_launcher_round.png')

    for rel, size in written:
        print(f'  {rel}: {size} bytes')
    print(f'共 {len(written)} 个文件 -> {res}')


if __name__ == '__main__':
    main()
