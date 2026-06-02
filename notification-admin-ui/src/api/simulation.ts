import request from '../utils/request'

/** 单表达式 JSONata 转换 */
export function transform(jsonataExpression: string, mockInputContext: unknown) {
  return request.post('/api/v1/admin/simulation/transform', {
    jsonataExpression,
    mockInputContext
  })
}

/** 完整请求预览 */
export function fullPreview(data: {
  pathTemplate?: string
  queryTemplate?: string
  headerTemplate?: string
  bodyTemplate?: string
  baseUrl: string
  httpMethod: string
  contentTypeBehavior: string
  mockInputContext: unknown
}) {
  return request.post('/api/v1/admin/simulation/full-preview', data)
}
