import request from '../utils/request'

/** 发布方分页查询 */
export function getPublisherList(params: { keyword?: string; status?: number; page?: number; size?: number }) {
  return request.get('/api/v1/admin/publishers', { params })
}

/** 获取单个发布方 */
export function getPublisher(id: number) {
  return request.get(`/api/v1/admin/publishers/${id}`)
}

/** 新增发布方 */
export function createPublisher(data: Record<string, unknown>) {
  return request.post('/api/v1/admin/publishers', data)
}

/** 更新发布方 */
export function updatePublisher(id: number, data: Record<string, unknown>) {
  return request.put(`/api/v1/admin/publishers/${id}`, data)
}

/** 轮换 API Key */
export function rotateApiKey(id: number) {
  return request.post(`/api/v1/admin/publishers/${id}/rotate-key`)
}
