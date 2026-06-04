import request from '../utils/request'

/** 订阅关系分页查询 */
export function getSubscriptionList(params: { subscriberCode?: string; eventTypeCode?: string; status?: string; page?: number; size?: number }) {
  return request.get('/api/v1/admin/subscriptions', { params })
}

/** 获取单个订阅关系 */
export function getSubscription(id: number) {
  return request.get(`/api/v1/admin/subscriptions/${id}`)
}

/** 新增订阅关系 */
export function createSubscription(data: Record<string, unknown>) {
  return request.post('/api/v1/admin/subscriptions', data)
}

/** 更新订阅关系 */
export function updateSubscription(id: number, data: Record<string, unknown>) {
  return request.put(`/api/v1/admin/subscriptions/${id}`, data)
}

/** 删除订阅关系 */
export function deleteSubscription(id: number) {
  return request.delete(`/api/v1/admin/subscriptions/${id}`)
}
