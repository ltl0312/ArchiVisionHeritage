/**
 * 上传前图像压缩（P3-A）
 *
 * 技术前提：浏览器**无法只解码图片的一部分**。`<img width/height>` 只是布局提示，
 * 不会降低解码分辨率；`decoding="async"` 只把解码挪到其他线程。所以「在展示端省内存」
 * 这条路走不通 —— 唯一有效的纯前端手段是在图片进入系统之前把它压小。
 *
 * 策略：
 *   · 长边 ≤ 1600px（等比缩放，不放大）
 *   · JPEG 质量 0.8
 *   · 按 EXIF 方向解码（createImageBitmap 的 imageOrientation: 'from-image'）
 *   · PNG 且检出透明通道 → 走 PNG 无损分支，避免透明底变黑
 *   · GIF 动图不处理（重编码会丢帧）
 *   · 已经小于 skipBelow 的图不动；压完反而更大的也不换
 *
 * 预期效果：新上传封面 2–4 MB → 约 300 KB。
 */

export const MAX_EDGE = 1600
export const JPEG_QUALITY = 0.8

/** 小于该体积的图片视为「已经够小」，不做重编码（避免无谓的有损损失） */
const SKIP_BELOW_BYTES = 300 * 1024

/** 透明通道探测的采样边长 */
const ALPHA_PROBE = 96

/** 人类可读字节数（用于 UI 提示「已优化 2.4 MB → 312 KB」） */
export function formatBytes(bytes) {
  if (!Number.isFinite(bytes) || bytes < 0) return '—'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}

/**
 * 压缩一张图片文件。
 * @param {File} file
 * @param {{ maxEdge?: number, quality?: number, skipBelow?: number }} [options]
 * @returns {Promise<File>} 压缩后的 File；任何一步不划算或失败都原样返回入参
 */
export async function compressImage(file, options = {}) {
  const maxEdge = options.maxEdge ?? MAX_EDGE
  const quality = options.quality ?? JPEG_QUALITY
  const skipBelow = options.skipBelow ?? SKIP_BELOW_BYTES

  if (!file || typeof file.type !== 'string' || !file.type.startsWith('image/')) return file
  if (file.type === 'image/gif') return file
  if (file.size <= skipBelow) return file

  const source = await loadSource(file)
  if (!source) return file

  try {
    const scale = Math.min(1, maxEdge / Math.max(source.width, source.height))
    const width = Math.max(1, Math.round(source.width * scale))
    const height = Math.max(1, Math.round(source.height * scale))

    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height

    const ctx = canvas.getContext('2d')
    if (!ctx) return file
    ctx.drawImage(source.source, 0, 0, width, height)

    const keepAlpha = file.type === 'image/png' && hasAlpha(source)
    const mime = keepAlpha ? 'image/png' : 'image/jpeg'

    const blob = await toBlob(canvas, mime, keepAlpha ? undefined : quality)
    // 压不小就不换：宁可保留原图，也不要"优化"出一个更大的文件
    if (!blob || blob.size >= file.size) return file

    const ext = keepAlpha ? 'png' : 'jpg'
    return new File([blob], renameExt(file.name, ext), {
      type: mime,
      lastModified: file.lastModified
    })
  } finally {
    source.release()
  }
}

/* ══════════════════════ 内部实现 ══════════════════════ */

/** 解码为可 drawImage 的源，并给出释放句柄 */
async function loadSource(file) {
  // 首选 createImageBitmap：可按 EXIF 方向解码，且不占用 DOM
  if (typeof createImageBitmap === 'function') {
    try {
      const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' })
      return {
        source: bitmap,
        width: bitmap.width,
        height: bitmap.height,
        release: () => { if (typeof bitmap.close === 'function') bitmap.close() }
      }
    } catch {
      // 某些浏览器 / 格式不支持该选项，退化为 <img> 解码
    }
  }

  const url = URL.createObjectURL(file)
  try {
    const img = await new Promise((resolve, reject) => {
      const el = new Image()
      el.onload = () => resolve(el)
      el.onerror = () => reject(new Error('image decode failed'))
      el.src = url
    })
    return {
      source: img,
      width: img.naturalWidth,
      height: img.naturalHeight,
      release: () => URL.revokeObjectURL(url)
    }
  } catch {
    URL.revokeObjectURL(url)
    return null
  }
}

/**
 * 透明通道探测：把源缩到 ALPHA_PROBE 见方再扫 alpha。
 * 用采样而非全图扫描，代价从 O(像素) 降到常数级；
 * 代价是极小的透明区域可能漏检 —— 对封面场景可接受。
 */
function hasAlpha(source) {
  const size = ALPHA_PROBE
  const canvas = document.createElement('canvas')
  canvas.width = size
  canvas.height = size
  const ctx = canvas.getContext('2d')
  if (!ctx) return false
  ctx.drawImage(source.source, 0, 0, size, size)

  let data
  try {
    data = ctx.getImageData(0, 0, size, size).data
  } catch {
    // 理论上不会发生（canvas 未被跨域污染），保守起见按不透明处理
    return false
  }

  for (let i = 3; i < data.length; i += 4) {
    if (data[i] < 255) return true
  }
  return false
}

function toBlob(canvas, mime, quality) {
  return new Promise((resolve) => {
    canvas.toBlob((blob) => resolve(blob), mime, quality)
  })
}

function renameExt(name, ext) {
  const base = (name || 'image').replace(/\.[^./\\]+$/, '')
  return `${base}.${ext}`
}
