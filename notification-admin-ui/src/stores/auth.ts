import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  // 认证状态（基于 HttpSession cookie，此处仅维护前端标志）
  const isAuthenticated = ref(false)
  const username = ref('')

  function setAuthenticated(user: string) {
    isAuthenticated.value = true
    username.value = user
  }

  function clearAuth() {
    isAuthenticated.value = false
    username.value = ''
  }

  return {
    isAuthenticated,
    username,
    setAuthenticated,
    clearAuth
  }
})
