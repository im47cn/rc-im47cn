<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppLayout from '../components/AppLayout.vue'
import { getDlqList, retryDlq, batchRetryDlq, ignoreDlq } from '../api/dlq'

interface DlqRecord {
  id: number
  bizSign: string
  biz_sign?: string
  traceId: string
  trace_id?: string
  supplierCode: string
  supplier_code?: string
  errorMsg: string
  error_msg?: string
  retryCount: number
  retry_count?: number
  dlqStatus: number
  dlq_status?: number
  createTime: string
  create_time?: string
}

// 筛选条件
const supplierCode = ref('')
const dlqStatusFilter = ref<number | ''>('')

// 表格数据
const tableData = ref<DlqRecord[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 批量选择
const selectedRows = ref<DlqRecord[]>([])

const statusMap: Record<number, { label: string; type: 'danger' | 'success' | 'info' }> = {
  0: { label: '待处理', type: 'danger' },
  1: { label: '已重试', type: 'success' },
  2: { label: '已忽略', type: 'info' }
}

async function fetchList() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (supplierCode.value) params.supplierCode = supplierCode.value
    if (dlqStatusFilter.value !== '') params.dlqStatus = dlqStatusFilter.value

    const res = await getDlqList(params as Parameters<typeof getDlqList>[0])
    tableData.value = res.data.records || res.data.content || []
    total.value = res.data.total || 0
  } catch {
    ElMessage.error('加载死信队列列表失败')
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

function handleSelectionChange(rows: DlqRecord[]) {
  selectedRows.value = rows
}

/** 弹窗获取操作者名称 */
async function promptOperator(title: string): Promise<string | null> {
  try {
    const { value } = await ElMessageBox.prompt('请输入操作者名称', title, {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '操作者名称不能为空'
    })
    return value
  } catch {
    return null
  }
}

async function handleRetry(row: DlqRecord) {
  const operator = await promptOperator('单条重试')
  if (!operator) return

  try {
    await retryDlq(row.id, operator)
    ElMessage.success('重试请求已提交')
    fetchList()
  } catch {
    ElMessage.error('重试失败')
  }
}

async function handleIgnore(row: DlqRecord) {
  const operator = await promptOperator('标记忽略')
  if (!operator) return

  try {
    await ignoreDlq(row.id, operator)
    ElMessage.success('已标记为忽略')
    fetchList()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleBatchRetry() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要重试的记录')
    return
  }

  const operator = await promptOperator('批量重试')
  if (!operator) return

  try {
    const ids = selectedRows.value.map(r => r.id)
    const res = await batchRetryDlq(ids, operator)
    const data = res.data
    ElMessage.success(`批量重试完成：成功 ${data.successCount ?? data.success_count ?? 0} 条，失败 ${data.failureCount ?? data.failure_count ?? 0} 条`)
    fetchList()
  } catch {
    ElMessage.error('批量重试失败')
  }
}

/** 获取字段值（兼容 camelCase 和 snake_case） */
function getField(row: DlqRecord, camelKey: keyof DlqRecord, snakeKey: keyof DlqRecord) {
  return row[camelKey] ?? row[snakeKey] ?? ''
}

onMounted(fetchList)
</script>

<template>
  <AppLayout>
    <div class="dlq-list">
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-input
          v-model="supplierCode"
          placeholder="供应商编码"
          clearable
          style="width: 200px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="dlqStatusFilter" placeholder="状态筛选" clearable style="width: 140px">
          <el-option label="待处理" :value="0" />
          <el-option label="已重试" :value="1" />
          <el-option label="已忽略" :value="2" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button
          type="warning"
          :disabled="selectedRows.length === 0"
          @click="handleBatchRetry"
        >
          批量重试 ({{ selectedRows.length }})
        </el-button>
      </div>

      <!-- 数据表格 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        border
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="50" />
        <el-table-column label="业务标识" width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ getField(row, 'bizSign', 'biz_sign') }}</template>
        </el-table-column>
        <el-table-column label="链路ID" width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ getField(row, 'traceId', 'trace_id') }}</template>
        </el-table-column>
        <el-table-column label="供应商" width="130">
          <template #default="{ row }">{{ getField(row, 'supplierCode', 'supplier_code') }}</template>
        </el-table-column>
        <el-table-column label="错误信息" min-width="200">
          <template #default="{ row }">
            <el-tooltip
              :content="String(getField(row, 'errorMsg', 'error_msg'))"
              placement="top"
              :show-after="300"
            >
              <span class="error-msg-cell">{{ getField(row, 'errorMsg', 'error_msg') }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="重试次数" width="90" align="center">
          <template #default="{ row }">{{ getField(row, 'retryCount', 'retry_count') }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.dlqStatus ?? row.dlq_status]?.type || 'info'">
              {{ statusMap[row.dlqStatus ?? row.dlq_status]?.label || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ getField(row, 'createTime', 'create_time') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleRetry(row)">重试</el-button>
            <el-button size="small" type="info" @click="handleIgnore(row)">忽略</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
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
    </div>
  </AppLayout>
</template>

<style scoped>
.dlq-list {
  padding: 0;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.error-msg-cell {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-all;
}
</style>
