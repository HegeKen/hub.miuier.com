#!/usr/bin/env python3
"""从方形 logo 源图生成站点 favicon / 应用图标。

用法：
    python3 scripts/make-favicon.py <源图.png>

需要 Pillow（pip install pillow）。产物写入 public/：

    favicon.ico          16 + 32 + 48，去字标的图形版（标签页/书签栏）
    favicon-16x16.png    同上，供现代浏览器按尺寸选择
    favicon-32x32.png
    apple-touch-icon.png 180，完整 logo 压白底（iOS 不支持透明应用图标）
    icon-192.png         完整 logo，供 Android / 安装到桌面
    icon-512.png

源图的中间横带是纯色（默认 #FF6802），字标为白色，因此小尺寸版本直接把
字标及其抗锯齿边缘覆盖为带色，得到干净的图形版——字标在 16/32px 下无法辨认。
"""

import argparse
from PIL import Image

BAND = (255, 104, 2)
# 字标（白色文字）在 1905×1905 源图中的包围盒，换源图时按需调整
TEXT_BBOX = (104, 823, 1800, 1091)
PAD = 10


def make_mark(img):
    out = img.copy()
    px = out.load()
    x0, y0, x1, y1 = TEXT_BBOX
    for y in range(max(0, y0 - PAD), min(out.height, y1 + PAD + 1)):
        for x in range(max(0, x0 - PAD), min(out.width, x1 + PAD + 1)):
            r, g, b, a = px[x, y]
            # 字标及抗锯齿边缘都偏向白色，横带本身是纯色，其余图形比带色更深
            if a and (r, g, b) != BAND and r > 240 and g > 108 and b > 6:
                px[x, y] = (*BAND, a)
    return out


def resized(img, size):
    return img.resize((size, size), Image.LANCZOS)


def on_white(img, size):
    canvas = Image.new('RGBA', (size, size), (255, 255, 255, 255))
    canvas.alpha_composite(resized(img, size))
    return canvas.convert('RGB')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('source', help='方形 logo 源图（PNG，建议 ≥512×512）')
    parser.add_argument('--out', default='public', help='输出目录，默认 public/')
    args = parser.parse_args()

    logo = Image.open(args.source).convert('RGBA')
    if logo.width != logo.height:
        raise SystemExit('源图需要是正方形')
    mark = make_mark(logo)

    import os
    os.makedirs(args.out, exist_ok=True)
    join = lambda name: os.path.join(args.out, name)  # noqa: E731

    mark.save(join('favicon.ico'), sizes=[(16, 16), (32, 32), (48, 48)])
    resized(mark, 16).save(join('favicon-16x16.png'))
    resized(mark, 32).save(join('favicon-32x32.png'))
    on_white(logo, 180).save(join('apple-touch-icon.png'))
    resized(logo, 192).save(join('icon-192.png'))
    resized(logo, 512).save(join('icon-512.png'))

    for name in ('favicon.ico', 'favicon-16x16.png', 'favicon-32x32.png',
                 'apple-touch-icon.png', 'icon-192.png', 'icon-512.png'):
        print(f'{name}: {os.path.getsize(join(name))} bytes')


if __name__ == '__main__':
    main()
