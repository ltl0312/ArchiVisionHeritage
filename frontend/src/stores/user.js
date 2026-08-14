import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'

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

  /** 从 JWT payload 解析 userId 和 role */
  function parseToken() {
    try {
      const payload = JSON.parse(atob(token.value.split('.')[1]))
      userId.value = payload.sub ? Number(payload.sub) : null
      username.value = payload.username || ''
      role.value = payload.role || 'USER'
    } catch { /* ignore */ }
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
