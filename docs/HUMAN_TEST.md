方案一：用 httpbin.org 作为模拟供应商（推荐）

  httpbin.org 会原样返回你发送的请求内容，适合验证完整链路。

  第一步：创建供应商（指向 httpbin）

  通过管理后台或 curl 创建：

  # 登录
  curl -c cookies.txt -X POST http://localhost:8080/api/v1/admin/login \
    -H 'Content-Type: application/json' \
    -d '{"username":"admin","password":"admin"}'

  # 创建供应商，baseUrl 指向 httpbin
  curl -b cookies.txt -X POST http://localhost:8080/api/v1/admin/suppliers \
    -H 'Content-Type: application/json' \
    -d '{
      "supplierCode": "HTTPBIN_TEST",
      "supplierName": "httpbin测试",
      "baseUrl": "https://httpbin.org",
      "httpMethod": "POST",
      "contentTypeBehavior": "APPLICATION_JSON",
      "pathTemplate": "\"/post\"",
      "bodyTemplate": "{\"mobile\": payload.mobile, \"content\": payload.content, \"eventType\": eventType}",
      "successHttpCodes": "200",
      "successBodyPattern": "",
      "maxRetryCount": 3,
      "retryBackoffInitialMs": 1000,
      "retryBackoffMultiplier": 2.00,
      "retryBackoffMaxMs": 30000,
      "workerConcurrency": 1,
      "status": 1
    }'

  第二步：模拟业务方发送通知

  调用 ingest 接口（不需要登录，这是对外 API）：

  curl -X POST http://localhost:8080/api/v1/notifications/ingest \
    -H 'Content-Type: application/json' \
    -d '{
      "eventId": "order-001",
      "supplierCode": "HTTPBIN_TEST",
      "eventType": "ORDER_CREATED",
      "payload": {
        "mobile": "13800138000",
        "content": "您的订单已创建",
        "orderId": "ORD-2026-001"
      }
    }'

  期望响应：
  {"eventId":"order-001","status":"ACCEPTED"}

  第三步：观察后端日志

  日志中应该出现完整投递链路：

  # 1. 入队
  事件入队成功: eventId=order-001, supplierCode=HTTPBIN_TEST

  # 2. Worker 取出消息并投递
  投递成功: bizSign=order-001, httpCode=200, elapsed=XXXms

  第四步：验证幂等

  重复发送相同 eventId：

  curl -X POST http://localhost:8080/api/v1/notifications/ingest \
    -H 'Content-Type: application/json' \
    -d '{
      "eventId": "order-001",
      "supplierCode": "HTTPBIN_TEST",
      "eventType": "ORDER_CREATED",
      "payload": {"mobile": "13800138000", "content": "您的订单已创建"}
    }'

  期望响应（HTTP 202）：
  {"eventId":"order-001","status":"IDEMPOTENT_HIT"}

  方案二：本地起一个模拟服务器

  如果不想依赖外网：

  # 用 python 启动一个接收所有请求并返回 200 的服务
  python3 -c "
  from http.server import HTTPServer, BaseHTTPRequestHandler
  import json

  class Handler(BaseHTTPRequestHandler):
      def do_POST(self):
          length = int(self.headers.get('Content-Length', 0))
          body = self.rfile.read(length).decode()
          print(f'收到请求: {self.path}')
          print(f'Body: {body}')
          self.send_response(200)
          self.send_header('Content-Type', 'application/json')
          self.end_headers()
          self.wfile.write(json.dumps({'success': True}).encode())

  HTTPServer(('0.0.0.0', 9999), Handler).serve_forever()
  "

  然后创建供应商时 baseUrl 设为 http://localhost:9999，就能在终端看到系统转发过来的实际请求内容。

  方案三：验证失败重试 + 死信

  # 创建一个指向不存在地址的供应商，触发投递失败
  curl -b cookies.txt -X POST http://localhost:8080/api/v1/admin/suppliers \
    -H 'Content-Type: application/json' \
    -d '{
      "supplierCode": "FAIL_TEST",
      "supplierName": "失败测试",
      "baseUrl": "http://localhost:19999",
      "httpMethod": "POST",
      "contentTypeBehavior": "APPLICATION_JSON",
      "bodyTemplate": "{\"test\": true}",
      "maxRetryCount": 2,
      "retryBackoffInitialMs": 1000,
      "retryBackoffMultiplier": 2.00,
      "retryBackoffMaxMs": 5000,
      "workerConcurrency": 1,
      "status": 1
    }'

  # 发送通知
  curl -X POST http://localhost:8080/api/v1/notifications/ingest \
    -H 'Content-Type: application/json' \
    -d '{
      "eventId": "fail-001",
      "supplierCode": "FAIL_TEST",
      "eventType": "TEST",
      "payload": {"msg": "会失败的消息"}
    }'

  观察日志，应该看到：
  投递异常 → 第1次重试(1s后) → 第2次重试(2s后) → 进入死信队列

  然后在管理后台的"死信管理"页面能看到这条记录。