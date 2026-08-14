import { ref } from 'vue'

/**
 * 统一 loading + try/catch 异步动作模板。
 * 默认吞掉错误（axios 拦截器已统一弹 toast），与原视图的静默 catch 语义一致；
 * 如需感知错误传入 options.onError。
 * @param {Function} fn 异步函数
 * @param {{ initialLoading?: boolean, onError?: (e: Error) => void }} [options]
 * @returns {{ loading: import('vue').Ref<boolean>, run: (...args: any[]) => Promise<any> }}
 */
export function useAsyncAction(fn, options = {}) {
  const loading = ref(options.initialLoading ?? false)

  async function run(...args) {
    loading.value = true
    try {
      return await fn(...args)
    } catch (e) {
      if (options.onError) options.onError(e)
      // 默认吞掉：拦截器已统一提示，视图层保持静默
    } finally {
      loading.value = false
    }
  }

  return { loading, run }
}
