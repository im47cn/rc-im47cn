<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppLayout from '../components/AppLayout.vue'
import { getSubscriptionList, createSubscription, updateSubscription, deleteSubscription } from '../api/subscription'
import { getSupplierByCode } from '../api/supplier'

interface SubscriptionRecord {
  id: number
  subscriberCode: string
  eventTypeCode: string
  status: string
  managedBy: string
  pathTemplate: string | null
  queryTemplate: string | null
  headerTemplate: string | null
  bodyTemplate: string | null
  connectTimeoutMs: number | null
  readTimeoutMs: number | null
  maxRetryCount: number | null
  retryBackoffInitialMs: number | null
  retryBackoffMultiplier: number | null
  retryBackoffMaxMs: number | null
  successHttpCodes: string | null
  successBodyPattern: string | null
  successBodyMatchMode: string | null
  createTime: string
  updateTime: string
}

interface SupplierDefaults {
  supplierName: string
  baseUrl: string
  httpMethod: string
  contentTypeBehavior: string
  connectTimeoutMs: number
  readTimeoutMs: number
  maxRetryCount: number
  retryBackoffInitialMs: number
  retryBackoffMultiplier: number
  retryBackoffMaxMs: number
  successHttpCodes: string
  successBodyPattern: string
  successBodyMatchMode: string
  pathTemplate: string
  queryTemplate: string
  headerTemplate: string
  bodyTemplate: string
}

const subscriberCodeFilter = ref('')
const eventTypeCodeFilter = ref('')
const statusFilter = ref('')
const tableData = ref<SubscriptionRecord[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const dialogVisible = ref(false)
const dialogTitle = ref('新增订阅关系')
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const showOverrides = ref(false)

// 订阅方默认配置
const subscriberDefaults = ref<SupplierDefaults | null>(null)
const loadingDefaults = ref(false)

const form = reactive({
  subscriberCode: '',
  eventTypeCode: '',
  status: 'ACTIVE',
  managedBy: 'SUBSCRIBER',
  pathTemplate: '',
  queryTemplate: '',
  headerTemplate: '',
  bodyTemplate: '',
  connectTimeoutMs: null as number | null,
  readTimeoutMs: null as number | null,
  maxRetryCount: null as number | null,
  retryBackoffInitialMs: null as number | null,
  retryBackoffMultiplier: null as number | null,
  retryBackoffMaxMs: null as number | null,
  successHttpCodes: '',
  successBodyPattern: '',
  successBodyMatchMode: ''
})

// 重试时间线计算（使用实际生效值：订阅覆盖 > 订阅方默认）
const retryTimeline = computed(() => {
  const d = subscriberDefaults.value
  const maxRetry = form.maxRetryCount ?? d?.maxRetryCount ?? 0
  const initial = form.retryBackoffInitialMs ?? d?.retryBackoffInitialMs ?? 1000
  const multiplier = form.retryBackoffMultiplier ?? d?.retryBackoffMultiplier ?? 2.0
  const maxDelay = form.retryBackoffMaxMs ?? d?.retryBackoffMaxMs ?? 30000

  const rows: { attempt: number; interval: number; cumulative: number }[] = []
  let cumulative = 0
  for (let i = 1; i <= maxRetry; i++) {
    const interval = Math.min(initial * Math.pow(multiplier, i - 1), maxDelay)
    cumulative += interval
    rows.push({ attempt: i, interval: Math.round(interval), cumulative: Math.round(cumulative) })
  }
  return rows
})

function formatMs(ms: number): string {
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  return `${(ms / 60000).toFixed(1)}min`
}

// 监听订阅方编码变化，加载默认配置
let debounceTimer: ReturnType<typeof setTimeout> | null = null
watch(() => form.subscriberCode, (newCode) => {
  if (debounceTimer) clearTimeout(debounceTimer)
  if (!newCode || newCode.length < 2) {
    subscriberDefaults.value = null
    return
  }
  debounceTimer = setTimeout(() => fetchSubscriberDefaults(newCode), 500)
})

async function fetchSubscriberDefaults(code: string) {
  loadingDefaults.value = true
  try {
    const res = await getSupplierByCode(code)
    subscriberDefaults.value = res.data as SupplierDefaults
  } catch {
    subscriberDefaults.value = null
  } finally {
    loadingDefaults.value = false
  }
}

async function fetchList() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (subscriberCodeFilter.value) params.subscriberCode = subscriberCodeFilter.value
    if (eventTypeCodeFilter.value) params.eventTypeCode = eventTypeCodeFilter.value
    if (statusFilter.value) params.status = statusFilter.value

    const res = await getSubscriptionList(params as Parameters<typeof getSubscriptionList>[0])
    tableData.value = res.data.records || res.data.content || []
    total.value = res.data.total || 0
  } catch {
    ElMessage.error('加载订阅关系列表失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  currentPage.value = 1
  fetchList()
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchList()
}

function handleSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  fetchList()
}

function resetForm() {
  form.subscriberCode = ''
  form.eventTypeCode = ''
  form.status = 'ACTIVE'
  form.managedBy = 'SUBSCRIBER'
  form.pathTemplate = ''
  form.queryTemplate = ''
  form.headerTemplate = ''
  form.bodyTemplate = ''
  form.connectTimeoutMs = null
  form.readTimeoutMs = null
  form.maxRetryCount = null
  form.retryBackoffInitialMs = null
  form.retryBackoffMultiplier = null
  form.retryBackoffMaxMs = null
  form.successHttpCodes = ''
  form.successBodyPattern = ''
  form.successBodyMatchMode = ''
  subscriberDefaults.value = null
}

function handleCreate() {
  dialogTitle.value = '新增订阅关系'
  isEdit.value = false
  editingId.value = null
  showOverrides.value = false
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: SubscriptionRecord) {
  dialogTitle.value = '编辑订阅关系'
  isEdit.value = true
  editingId.value = row.id
  form.subscriberCode = row.subscriberCode
  form.eventTypeCode = row.eventTypeCode
  form.status = row.status
  form.managedBy = row.managedBy
  form.pathTemplate = row.pathTemplate || ''
  form.queryTemplate = row.queryTemplate || ''
  form.headerTemplate = row.headerTemplate || ''
  form.bodyTemplate = row.bodyTemplate || ''
  form.connectTimeoutMs = row.connectTimeoutMs
  form.readTimeoutMs = row.readTimeoutMs
  form.maxRetryCount = row.maxRetryCount
  form.retryBackoffInitialMs = row.retryBackoffInitialMs
  form.retryBackoffMultiplier = row.retryBackoffMultiplier
  form.retryBackoffMaxMs = row.retryBackoffMaxMs
  form.successHttpCodes = row.successHttpCodes || ''
  form.successBodyPattern = row.successBodyPattern || ''
  form.successBodyMatchMode = row.successBodyMatchMode || ''
  showOverrides.value = hasOverrides(row)
  dialogVisible.value = true
  fetchSubscriberDefaults(row.subscriberCode)
}

function hasOverrides(row: SubscriptionRecord): boolean {
  return !!(row.pathTemplate || row.queryTemplate || row.headerTemplate || row.bodyTemplate ||
    row.connectTimeoutMs || row.readTimeoutMs || row.maxRetryCount ||
    row.successHttpCodes || row.successBodyPattern)
}

function countOverrides(row: SubscriptionRecord): number {
  let count = 0
  if (row.pathTemplate) count++
  if (row.queryTemplate) count++
  if (row.headerTemplate) count++
  if (row.bodyTemplate) count++
  if (row.connectTimeoutMs) count++
  if (row.readTimeoutMs) count++
  if (row.maxRetryCount) count++
  if (row.retryBackoffInitialMs) count++
  if (row.successHttpCodes) count++
  if (row.successBodyPattern) count++
  return count
}

function buildPayload() {
  const data: Record<string, unknown> = {
    subscriberCode: form.subscriberCode,
    eventTypeCode: form.eventTypeCode,
    status: form.status,
    managedBy: form.managedBy
  }
  if (form.pathTemplate) data.pathTemplate = form.pathTemplate
  if (form.queryTemplate) data.queryTemplate = form.queryTemplate
  if (form.headerTemplate) data.headerTemplate = form.headerTemplate
  if (form.bodyTemplate) data.bodyTemplate = form.bodyTemplate
  if (form.connectTimeoutMs != null) data.connectTimeoutMs = form.connectTimeoutMs
  if (form.readTimeoutMs != null) data.readTimeoutMs = form.readTimeoutMs
  if (form.maxRetryCount != null) data.maxRetryCount = form.maxRetryCount
  if (form.retryBackoffInitialMs != null) data.retryBackoffInitialMs = form.retryBackoffInitialMs
  if (form.retryBackoffMultiplier != null) data.retryBackoffMultiplier = form.retryBackoffMultiplier
  if (form.retryBackoffMaxMs != null) data.retryBackoffMaxMs = form.retryBackoffMaxMs
  if (form.successHttpCodes) data.successHttpCodes = form.successHttpCodes
  if (form.successBodyPattern) data.successBodyPattern = form.successBodyPattern
  if (form.successBodyMatchMode) data.successBodyMatchMode = form.successBodyMatchMode
  return data
}

async function handleSubmit() {
  if (!form.subscriberCode || !form.eventTypeCode) {
    ElMessage.warning('请填写订阅方编码和事件类型编码')
    return
  }
  try {
    if (isEdit.value && editingId.value) {
      await updateSubscription(editingId.value, buildPayload())
      ElMessage.success('更新成功')
    } else {
      await createSubscription(buildPayload())
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch (error: unknown) {
    const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message || '操作失败'
    ElMessage.error(msg)
  }
}

async function handleDelete(row: SubscriptionRecord) {
  try {
    await ElMessageBox.confirm(
      `确认删除订阅关系 "${row.subscriberCode} × ${row.eventTypeCode}" ?`,
      '删除确认',
      { type: 'warning' }
    )
    await deleteSubscription(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

onMounted(fetchList)
</script>

<template>
  <AppLayout>
    <div class="subscription-list">
      <div class="search-bar">
        <el-input v-model="subscriberCodeFilter" placeholder="订阅方编码" clearable style="width: 160px" @keyup.enter="handleSearch" />
        <el-input v-model="eventTypeCodeFilter" placeholder="事件类型编码" clearable style="width: 180px" @keyup.enter="handleSearch" />
        <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 130px">
          <el-option label="ACTIVE" value="ACTIVE" />
          <el-option label="SUSPENDED" value="SUSPENDED" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button type="success" @click="handleCreate">新增订阅</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="subscriberCode" label="订阅方" width="150" />
        <el-table-column prop="eventTypeCode" label="事件类型" width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'warning'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="managedBy" label="管理方" width="120" align="center">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.managedBy }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="配置覆盖" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="countOverrides(row) > 0" type="warning" size="small">
              {{ countOverrides(row) }} 项覆盖
            </el-tag>
            <span v-else class="no-override">继承默认</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>

      <!-- 新增/编辑对话框 -->
      <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" top="3vh">
        <el-form label-width="130px">
          <el-form-item label="订阅方编码" required>
            <div class="field-with-default">
              <el-input v-model="form.subscriberCode" :disabled="isEdit" placeholder="已有订阅方（供应商）编码" />
              <span v-if="subscriberDefaults" class="default-badge">{{ subscriberDefaults.supplierName }}</span>
              <span v-else-if="form.subscriberCode && !loadingDefaults" class="default-hint default-hint-warn">未找到订阅方</span>
              <span v-else-if="loadingDefaults" class="default-hint">加载中...</span>
            </div>
          </el-form-item>
          <el-form-item label="事件类型编码" required>
            <el-input v-model="form.eventTypeCode" :disabled="isEdit" placeholder="已注册的事件类型编码" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status">
              <el-option label="ACTIVE" value="ACTIVE" />
              <el-option label="SUSPENDED" value="SUSPENDED" />
            </el-select>
          </el-form-item>
          <el-form-item label="管理方">
            <el-select v-model="form.managedBy">
              <el-option label="SUBSCRIBER" value="SUBSCRIBER" />
              <el-option label="PUBLISHER" value="PUBLISHER" />
              <el-option label="PLATFORM" value="PLATFORM" />
            </el-select>
          </el-form-item>

          <el-divider content-position="left">
            <el-button text type="primary" @click="showOverrides = !showOverrides">
              {{ showOverrides ? '收起' : '展开' }}配置覆盖
            </el-button>
            <span class="override-hint">留空则继承订阅方通道默认配置</span>
          </el-divider>

          <template v-if="showOverrides">
            <!-- 超时配置 -->
            <el-form-item label="连接超时(ms)">
              <div class="field-with-default">
                <el-input-number v-model="form.connectTimeoutMs" :min="100" :max="30000" />
                <span v-if="subscriberDefaults" class="default-val">默认 {{ subscriberDefaults.connectTimeoutMs }}ms</span>
              </div>
            </el-form-item>
            <el-form-item label="读取超时(ms)">
              <div class="field-with-default">
                <el-input-number v-model="form.readTimeoutMs" :min="100" :max="60000" />
                <span v-if="subscriberDefaults" class="default-val">默认 {{ subscriberDefaults.readTimeoutMs }}ms</span>
              </div>
            </el-form-item>

            <!-- 重试策略 -->
            <el-divider content-position="left">重试策略</el-divider>
            <el-form-item label="最大重试次数">
              <div class="field-with-default">
                <el-input-number v-model="form.maxRetryCount" :min="0" :max="10" />
                <span v-if="subscriberDefaults" class="default-val">默认 {{ subscriberDefaults.maxRetryCount }} 次</span>
              </div>
            </el-form-item>
            <el-form-item label="退避初始(ms)">
              <div class="field-with-default">
                <el-input-number v-model="form.retryBackoffInitialMs" :min="100" :max="60000" />
                <span v-if="subscriberDefaults" class="default-val">默认 {{ formatMs(subscriberDefaults.retryBackoffInitialMs) }}</span>
              </div>
            </el-form-item>
            <el-form-item label="退避倍数">
              <div class="field-with-default">
                <el-input-number v-model="form.retryBackoffMultiplier" :min="1" :max="10" :precision="2" :step="0.5" />
                <span v-if="subscriberDefaults" class="default-val">默认 {{ subscriberDefaults.retryBackoffMultiplier }}x</span>
              </div>
            </el-form-item>
            <el-form-item label="退避上限(ms)">
              <div class="field-with-default">
                <el-input-number v-model="form.retryBackoffMaxMs" :min="1000" :max="600000" />
                <span v-if="subscriberDefaults" class="default-val">默认 {{ formatMs(subscriberDefaults.retryBackoffMaxMs) }}</span>
              </div>
            </el-form-item>

            <!-- 重试时间线预览 -->
            <el-form-item v-if="retryTimeline.length > 0" label="">
              <div class="retry-timeline-label">实际重试延迟预览</div>
              <el-table :data="retryTimeline" size="small" border style="width: 100%" max-height="200">
                <el-table-column prop="attempt" label="重试次数" width="90" align="center">
                  <template #default="{ row }">第 {{ row.attempt }} 次</template>
                </el-table-column>
                <el-table-column prop="interval" label="等待间隔" width="120" align="center">
                  <template #default="{ row }">{{ formatMs(row.interval) }}</template>
                </el-table-column>
                <el-table-column prop="cumulative" label="累计延迟" width="120" align="center">
                  <template #default="{ row }">{{ formatMs(row.cumulative) }}</template>
                </el-table-column>
              </el-table>
            </el-form-item>

            <!-- 成功判定 -->
            <el-divider content-position="left">成功判定</el-divider>
            <el-form-item label="成功状态码">
              <div class="field-with-default">
                <el-input v-model="form.successHttpCodes" placeholder="如 200,201" />
                <span v-if="subscriberDefaults" class="default-val">默认 {{ subscriberDefaults.successHttpCodes || '200' }}</span>
              </div>
            </el-form-item>
            <el-form-item label="成功体匹配">
              <div class="field-with-default">
                <el-input v-model="form.successBodyPattern" placeholder="响应体匹配模式" />
                <span v-if="subscriberDefaults?.successBodyPattern" class="default-val">默认 {{ subscriberDefaults.successBodyPattern }}</span>
              </div>
            </el-form-item>
            <el-form-item label="匹配模式">
              <div class="field-with-default">
                <el-select v-model="form.successBodyMatchMode" clearable placeholder="默认继承">
                  <el-option label="EQUALS" value="EQUALS" />
                  <el-option label="CONTAINS" value="CONTAINS" />
                </el-select>
                <span v-if="subscriberDefaults" class="default-val">默认 {{ subscriberDefaults.successBodyMatchMode || 'EQUALS' }}</span>
              </div>
            </el-form-item>

            <!-- 模板覆盖 -->
            <el-divider content-position="left">模板覆盖</el-divider>
            <el-form-item label="Path 模板">
              <div class="field-with-default field-with-default-block">
                <el-input v-model="form.pathTemplate" placeholder="覆盖路径模板" />
                <span v-if="subscriberDefaults?.pathTemplate" class="default-val mono">默认 {{ subscriberDefaults.pathTemplate }}</span>
              </div>
            </el-form-item>
            <el-form-item label="Query 模板">
              <div class="field-with-default field-with-default-block">
                <el-input v-model="form.queryTemplate" type="textarea" :rows="2" placeholder="覆盖查询参数模板" />
                <span v-if="subscriberDefaults?.queryTemplate" class="default-val mono">默认 {{ subscriberDefaults.queryTemplate }}</span>
              </div>
            </el-form-item>
            <el-form-item label="Header 模板">
              <div class="field-with-default field-with-default-block">
                <el-input v-model="form.headerTemplate" type="textarea" :rows="2" placeholder="覆盖请求头模板" />
                <span v-if="subscriberDefaults?.headerTemplate" class="default-val mono">默认 {{ subscriberDefaults.headerTemplate }}</span>
              </div>
            </el-form-item>
            <el-form-item label="Body 模板">
              <div class="field-with-default field-with-default-block">
                <el-input v-model="form.bodyTemplate" type="textarea" :rows="4" placeholder="覆盖请求体模板（JSONata）" />
                <span v-if="subscriberDefaults?.bodyTemplate" class="default-val mono">默认 {{ subscriberDefaults.bodyTemplate }}</span>
              </div>
            </el-form-item>
          </template>
        </el-form>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </template>
      </el-dialog>
    </div>
  </AppLayout>
</template>

<style scoped>
.subscription-list { padding: 0; }
.search-bar { display: flex; gap: 12px; margin-bottom: 16px; align-items: center; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
.no-override { color: #909399; font-size: 12px; }
.override-hint { color: #909399; font-size: 12px; margin-left: 8px; }

/* 输入框右侧默认值 */
.field-with-default {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}
.field-with-default-block {
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}
.default-val {
  flex-shrink: 0;
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}
.default-val.mono {
  white-space: normal;
  word-break: break-all;
  font-family: monospace;
  font-size: 11px;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  line-height: 1.4;
  max-width: 100%;
}
.default-badge {
  flex-shrink: 0;
  font-size: 12px;
  color: #67c23a;
  font-weight: 500;
}
.default-hint {
  flex-shrink: 0;
  font-size: 12px;
  color: #c0c4cc;
}
.default-hint-warn {
  color: #e6a23c;
}
.retry-timeline-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}
</style>
