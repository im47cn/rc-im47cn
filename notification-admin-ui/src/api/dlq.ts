import request from '../utils/request'

/** 死信队列分页查询 */
export function getDlqList(params: { supplierCode?: string; dlqStatus?: number; page?: number; size?: number }) {
  return request.get('/api/v1/admin/dlq', { params })
}

/** 单条重试 */
export function retryDlq(id: number, operator: string) {
  return request.post(`/api/v1/admin/dlq/${id}/retry`, null, {
    headers: { 'X-Operator': operator }
  })
}

/** 批量重试 */
export function batchRetryDlq(ids: number[], operator: string) {
  return request.post('/api/v1/admin/dlq/batch-retry', { ids }, {
    headers: { 'X-Operator': operator }
  })
}

/** 标记忽略 */
export function ignoreDlq(id: number, operator: string) {
  return request.post(`/api/v1/admin/dlq/${id}/ignore`, null, {
    headers: { 'X-Operator': operator }
  })
}
