import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  // 从 sessionStorage 恢复认证状态（浏览器标签页级别持久化）
  const isAuthenticated = ref(sessionStorage.getItem('authenticated') === 'true')
  const username = ref(sessionStorage.getItem('username') || '')

  function setAuthenticated(user: string) {
    isAuthenticated.value = true
    username.value = user
    sessionStorage.setItem('authenticated', 'true')
    sessionStorage.setItem('username', user)
  }

  function clearAuth() {
    isAuthenticated.value = false
    username.value = ''
    sessionStorage.removeItem('authenticated')
    sessionStorage.removeItem('username')
  }

  return {
    isAuthenticated,
    username,
    setAuthenticated,
    clearAuth
  }
})
