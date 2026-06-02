import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { useAuthStore } from '../stores/auth'

// 创建 Axios 实例
// API 基础地址：开发环境通过 Vite proxy 转发，生产环境通过 Nginx 反向代理或直接配置后端地址
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
  withCredentials: true, // 跨域携带 Session Cookie
  headers: {
    'Content-Type': 'application/json'
  }
})

// 响应拦截器：401 时跳转登录页
request.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.clearAuth()
      ElMessage.warning('登录已过期，请重新登录')
      router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
    }
    return Promise.reject(error)
  }
)

export default request
