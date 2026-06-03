import { http, HttpResponse } from 'msw'

export const handlers = [
  // Supplier API
  http.get('/api/v1/admin/suppliers/:id', () => {
    return HttpResponse.json({
      supplierCode: 'ALI_YUN',
      supplierName: 'Aliyun SMS',
      description: 'Test supplier',
      baseUrl: 'https://api.example.com',
      httpMethod: 'POST',
      contentTypeBehavior: 'APPLICATION_JSON',
      connectTimeoutMs: 3000,
      readTimeoutMs: 5000,
      maxRetryCount: 3,
      retryBackoffInitialMs: 1000,
      retryBackoffMultiplier: 2.0,
      retryBackoffMaxMs: 30000,
      successHttpCodes: '200',
      successBodyPattern: '',
      successBodyMatchMode: 'EQUALS',
      successCaseSensitive: 1,
      workerConcurrency: 10,
      pathTemplate: '',
      queryTemplate: '',
      headerTemplate: '',
      bodyTemplate: '{"mobile": mobile}',
      credentialKeys: ['accessKeyId', 'accessKeySecret']
    })
  }),

  http.post('/api/v1/admin/suppliers', () => {
    return HttpResponse.json({ id: 1 })
  }),

  http.put('/api/v1/admin/suppliers/:id', () => {
    return HttpResponse.json({ id: 1 })
  }),

  // DLQ API
  http.get('/api/v1/admin/dlq', () => {
    return HttpResponse.json({
      records: [
        {
          id: 1,
          bizSign: 'BIZ001',
          traceId: 'TRACE001',
          supplierCode: 'ALI_YUN',
          errorMsg: 'Connection timeout',
          retryCount: 3,
          dlqStatus: 0,
          createTime: '2026-01-01 00:00:00'
        },
        {
          id: 2,
          bizSign: 'BIZ002',
          traceId: 'TRACE002',
          supplierCode: 'TENCENT',
          errorMsg: 'Server error',
          retryCount: 1,
          dlqStatus: 1,
          createTime: '2026-01-02 00:00:00'
        }
      ],
      total: 2
    })
  }),

  http.post('/api/v1/admin/dlq/:id/retry', () => {
    return HttpResponse.json({ success: true })
  }),

  http.post('/api/v1/admin/dlq/batch-retry', () => {
    return HttpResponse.json({ successCount: 2, failureCount: 0 })
  }),

  // Simulation API
  http.post('/api/v1/admin/simulation/transform', () => {
    return HttpResponse.json({ result: '{"transformed": true}' })
  }),

  http.post('/api/v1/admin/simulation/full-preview', () => {
    return HttpResponse.json({
      url: 'https://api.example.com/send',
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: '{"mobile":"13800138000"}'
    })
  })
]
