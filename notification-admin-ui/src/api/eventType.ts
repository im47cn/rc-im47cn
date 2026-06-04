import request from '../utils/request'

/** 事件类型分页查询 */
export function getEventTypeList(params: { keyword?: string; publisherCode?: string; status?: string; page?: number; size?: number }) {
  return request.get('/api/v1/admin/event-types', { params })
}

/** 获取单个事件类型 */
export function getEventType(id: number) {
  return request.get(`/api/v1/admin/event-types/${id}`)
}

/** 新增事件类型 */
export function createEventType(data: Record<string, unknown>) {
  return request.post('/api/v1/admin/event-types', data)
}

/** 更新事件类型 */
export function updateEventType(id: number, data: Record<string, unknown>) {
  return request.put(`/api/v1/admin/event-types/${id}`, data)
}
