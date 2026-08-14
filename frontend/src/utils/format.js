/**
 * 将后端标签字段解析为数组（兼容逗号分隔字符串 / 已数组 / 空值）。
 */
export function parseTags(tags) {
  if (!tags) return []
  if (Array.isArray(tags)) return tags
  return tags.split(',').map(t => t.trim()).filter(Boolean)
}
