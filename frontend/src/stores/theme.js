import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 主题状态（由 App.vue 原主题逻辑收敛而来，行为逐行等价）：
 * - 优先级：localStorage('theme') > 系统 prefers-color-scheme
 * - 通过 documentElement[data-theme] 驱动全局 CSS 变量
 */
export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(false)

  function applyTheme(dark) {
    isDark.value = dark
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light')
    localStorage.setItem('theme', dark ? 'dark' : 'light')
  }

  function toggleTheme() {
    applyTheme(!isDark.value)
  }

  function initTheme() {
    const saved = localStorage.getItem('theme')
    if (saved === 'dark') {
      applyTheme(true)
    } else if (saved === 'light') {
      applyTheme(false)
    } else {
      applyTheme(window.matchMedia('(prefers-color-scheme: dark)').matches)
    }
  }

  return { isDark, toggleTheme, initTheme }
})
