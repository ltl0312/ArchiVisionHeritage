import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'

/** 从 JWT payload 解码（纯函数，router 守卫复用）。解码失败返回 null */
export function decodeTokenPayload(token) {
  if (!token) return null
  try {
    return JSON.parse(atob(token.split('.')[1]))
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref('')
  const userId = ref(null)
  const role = ref('USER')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')

  function setToken(t) {
    token.value = t
    localStorage.setItem('token', t)
  }

  /** 从 JWT payload 解析 userId 和 role（解码失败时保留原值） */
  function parseToken() {
    const payload = decodeTokenPayload(token.value)
    if (!payload) return
    userId.value = payload.sub ? Number(payload.sub) : null
    username.value = payload.username || ''
    role.value = payload.role || 'USER'
  }

  async function login(credentials) {
    const res = await authApi.login(credentials)
    setToken(res.data.token)
    parseToken()
    return res
  }

  async function register(data) {
    return await authApi.register(data)
  }

  function logout() {
    token.value = ''
    username.value = ''
    userId.value = null
    role.value = 'USER'
    localStorage.removeItem('token')
    window.location.href = '/login'
  }

  return { token, username, userId, role, isLoggedIn, isAdmin,
           setToken, parseToken, login, register, logout }
})
