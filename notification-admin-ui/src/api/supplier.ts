import request from '../utils/request'

/** 供应商分页查询 */
export function getSupplierList(params: { keyword?: string; status?: number; page?: number; size?: number }) {
  return request.get('/api/v1/admin/suppliers', { params })
}

/** 获取单个供应商详情 */
export function getSupplier(id: number) {
  return request.get(`/api/v1/admin/suppliers/${id}`)
}

/** 按供应商编码查询配置 */
export function getSupplierByCode(code: string) {
  return request.get(`/api/v1/admin/suppliers/by-code/${code}`)
}

/** 新增供应商 */
export function createSupplier(data: Record<string, unknown>) {
  return request.post('/api/v1/admin/suppliers', data)
}

/** 更新供应商 */
export function updateSupplier(id: number, data: Record<string, unknown>) {
  return request.put(`/api/v1/admin/suppliers/${id}`, data)
}

/** 切换供应商启用/禁用状态 */
export function toggleSupplierStatus(id: number, status: number) {
  return request.patch(`/api/v1/admin/suppliers/${id}/status`, null, { params: { status } })
}
