import request from '../utils/request'

/** 管理员登录 */
export function login(username: string, password: string) {
  return request.post('/api/v1/admin/login', { username, password })
}

/** 管理员登出 */
export function logout() {
  return request.post('/api/v1/admin/logout')
}
