/**
 * MyBatis-Plus 分页响应兜底：`data.records || data`，兼容列表直返与空值。
 * 注意：不适用于 store 拷贝场景（如通知列表）。
 * @param {*} resData 接口返回的 data 字段
 * @param {Array} [fallback] 兜底默认值
 */
export function recordsFallback(resData, fallback = []) {
  return resData?.records || resData || fallback
}
