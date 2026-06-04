import request from '../utils/request'

/** 查询字段指纹 */
export function getFieldFingerprints(params: { eventTypeCode: string }) {
  return request.get('/api/v1/admin/field-fingerprints', { params })
}
