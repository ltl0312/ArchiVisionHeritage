"""预览页资源瘦身：PNG → JPEG（保留分辨率，按显示尺寸适度降采样）。"""
from PIL import Image
import os

SRC = os.path.join('frontend', 'preview', 'assets')

# name: (目标宽度 or None, quality)
PLAN = {
    'hero':       (1400, 80),   # 首屏横幅，显示宽约 1100–1408
    'pointcloud': (1536, 82),   # 三栏视口，竖向取满，需保留高度
    'model3d':    (1400, 82),   # 幻筑预览 + 详情查看器
    'datang':     (1536, 80),   # 档案头图，显示宽 1440
    'eave':       (724,  78),   # 卡片封面，显示仅 362 宽 → 2x 足矣
}

total_before = total_after = 0
for name, (target_w, q) in PLAN.items():
    src = os.path.join(SRC, name + '.png')
    if not os.path.exists(src):
        print(f'  SKIP {name} (missing)')
        continue
    before = os.path.getsize(src)
    total_before += before

    im = Image.open(src)
    mode = 'RGBA' if im.mode in ('RGBA', 'LA', 'P') else 'RGB'
    im = im.convert('RGB')
    if target_w and im.width > target_w:
        h = round(im.height * target_w / im.width)
        im = im.resize((target_w, h), Image.LANCZOS)

    dst = os.path.join(SRC, name + '.jpg')
    im.save(dst, 'JPEG', quality=q, optimize=True, progressive=True)
    after = os.path.getsize(dst)
    total_after += after
    print(f'  {name:11s} {im.width:>4}x{im.height:<4} {before/1024:8.0f} KB -> {after/1024:7.0f} KB'
          f'  ({after/before*100:5.1f}%)')

print(f'\n  TOTAL {total_before/1024/1024:.2f} MB -> {total_after/1024/1024:.2f} MB'
      f'  (-{(1-total_after/total_before)*100:.1f}%)')
