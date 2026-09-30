"""P3-B — 仓库内静态素材一次性瘦身（构建期工具，非运行时）

用法：
    python .workbuddy/_shrink_assets.py            # 预演（默认，不写任何文件）
    python .workbuddy/_shrink_assets.py --apply    # 实际写入

范围：
    1. frontend/public/**   —— 真正随构建产物分发的静态资源
    2. generated-images/    —— 仓库内的大图素材（未被任何代码引用）

规则：
    · PNG → JPEG（quality 82, progressive, optimize），按显示需要降采样
    · 目标长边 1600px（超过才缩放）
    · 已经是 JPEG/WebP 且小于阈值（400 KB）的文件跳过
    · 默认预演：只打印 before/after，不改磁盘

⚠️ 风险提示：generated-images/ 当前**未被 git 跟踪**，写入后无法回滚。
   因此该目录只在显式传 --apply 时才会被处理。
"""
import argparse
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

TARGETS = [
    ('frontend/public', True),   # 随构建分发 —— 安全
    ('generated-images', False),  # 未纳入版本控制 —— 需显式放行
]

MAX_EDGE = 1600
JPEG_QUALITY = 82
SKIP_BELOW = 400 * 1024
SKIP_EXT = {'.jpg', '.jpeg', '.webp', '.gif', '.svg', '.ico'}


def human(n):
    return f'{n / 1024:.0f} KB' if n < 1024 * 1024 else f'{n / 1024 / 1024:.2f} MB'


def shrink(path, apply_write):
    before = os.path.getsize(path)
    if before <= SKIP_BELOW:
        return before, before, 'skip:already-small'

    try:
        im = Image.open(path)
        im.load()
    except Exception as e:  # noqa: BLE001
        return before, before, f'skip:unreadable({e.__class__.__name__})'

    has_alpha = im.mode in ('RGBA', 'LA') or (im.mode == 'P' and 'transparency' in im.info)
    if im.width > MAX_EDGE:
        h = round(im.height * MAX_EDGE / im.width)
        im = im.resize((MAX_EDGE, h), Image.LANCZOS)

    dst = os.path.splitext(path)[0] + ('.png' if has_alpha else '.jpg')
    if not apply_write:
        # 预演：编码到内存拿真实字节数，不落盘
        import io
        buf = io.BytesIO()
        if has_alpha:
            im.convert('RGBA').save(buf, 'PNG', optimize=True)
        else:
            im.convert('RGB').save(buf, 'JPEG', quality=JPEG_QUALITY, optimize=True, progressive=True)
        after = buf.tell()
    else:
        if has_alpha:
            im.convert('RGBA').save(dst, 'PNG', optimize=True)
        else:
            im.convert('RGB').save(dst, 'JPEG', quality=JPEG_QUALITY, optimize=True, progressive=True)
        after = os.path.getsize(dst)
        if dst != path:
            os.remove(path)

    return before, after, 'png-lossless' if has_alpha else 'jpeg'


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--apply', action='store_true', help='实际写入（默认只预演）')
    args = ap.parse_args()

    total_before = total_after = 0
    for rel, safe in TARGETS:
        base = os.path.join(ROOT, rel)
        if not os.path.isdir(base):
            print(f'  SKIP {rel} (missing)')
            continue
        print(f'\n── {rel} ──')
        for dirpath, _dirs, files in os.walk(base):
            for name in sorted(files):
                ext = os.path.splitext(name)[1].lower()
                if ext in SKIP_EXT:
                    continue
                path = os.path.join(dirpath, name)
                if not safe and not args.apply:
                    before = os.path.getsize(path)
                    if before > SKIP_BELOW:
                        print(f'  {os.path.relpath(path, ROOT):<64} {human(before):>9}'
                              f'  → 需 --apply 才会处理（该目录未被 git 跟踪）')
                    total_before += before
                    total_after += before
                    continue
                before, after, how = shrink(path, args.apply)
                total_before += before
                total_after += after
                flag = '   ' if after < before else ' = '
                print(f'  {os.path.relpath(path, ROOT):<64} {human(before):>9}{flag}{human(after):>9}  {how}')

    saved = total_before - total_after
    pct = (saved / total_before * 100) if total_before else 0
    mode = 'APPLIED' if args.apply else 'DRY-RUN (未写入任何文件)'
    print(f'\n  [{mode}] TOTAL {human(total_before)} → {human(total_after)}  '
          f'(-{human(saved)}, -{pct:.1f}%)')
    if not args.apply:
        print('  加 --apply 才会真正写入。')
    return 0


if __name__ == '__main__':
    sys.exit(main())
