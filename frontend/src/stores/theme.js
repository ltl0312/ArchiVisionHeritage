import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 视觉偏好状态（主题 + 信息密度）
 *
 * 归属说明：本 store 是「文档级视觉偏好」的唯一 owner —— 主题与信息密度
 * 都以 documentElement 上的标记驱动全局 CSS 变量 / 布局，二者同源同责。
 * （计划书 §6 原列 stores/** 不改动；此处新增 density 是 §4.3 要求的
 *  「信息密度真实改变布局」的最小落点，未改动既有主题行为。）
 *
 * - 主题：优先级 localStorage('theme') > 系统 prefers-color-scheme
 *         通过 documentElement[data-theme] 驱动
 * - 密度：localStorage('density')，默认 cozy（舒展）
 *         通过 documentElement.density-compact 驱动
 */
export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(false)
  /** 'cozy'（舒展，默认） | 'compact'（紧凑） */
  const density = ref('cozy')

  /* ═══ 主题 ═══ */
  function applyTheme(dark) {
    isDark.value = dark
    document.documentElement.setAttribute('data-theme', dark ? 'dark' : 'light')
    localStorage.setItem('theme', dark ? 'dark' : 'light')
  }

  function setTheme(mode) {
    applyTheme(mode === 'dark')
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

  /* ═══ 信息密度 ═══ */
  function applyDensity(mode) {
    density.value = mode === 'compact' ? 'compact' : 'cozy'
    document.documentElement.classList.toggle('density-compact', density.value === 'compact')
    localStorage.setItem('density', density.value)
  }

  function initDensity() {
    applyDensity(localStorage.getItem('density') || 'cozy')
  }

  return {
    isDark, density,
    applyTheme, setTheme, toggleTheme, initTheme,
    applyDensity, initDensity
  }
})
