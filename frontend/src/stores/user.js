import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const username = ref('')
  const userId = ref(null)

  const isLoggedIn = computed(() => !!token.value)

  function setToken(t) {
    token.value = t
    localStorage.setItem('token', t)
  }

  function setUserInfo(id, name) {
    userId.value = id
    username.value = name
  }

  async function login(credentials) {
    const res = await authApi.login(credentials)
    setToken(res.data.token)
    return res
  }

  async function register(data) {
    return await authApi.register(data)
  }

  function logout() {
    token.value = ''
    username.value = ''
    userId.value = null
    localStorage.removeItem('token')
    window.location.href = '/login'
  }

  return { token, username, userId, isLoggedIn, setToken, setUserInfo, login, register, logout }
})
