import request from '../utils/request'

/** 查询变更记录 */
export function getChangeRecords(params: { eventTypeCode?: string; status?: string; page?: number; size?: number }) {
  return request.get('/api/v1/admin/change-records', { params })
}

/** 确认变更记录 */
export function confirmChangeRecord(id: number) {
  return request.put(`/api/v1/admin/change-records/${id}/confirm`)
}

/** 驳回变更记录 */
export function dismissChangeRecord(id: number) {
  return request.put(`/api/v1/admin/change-records/${id}/dismiss`)
}
