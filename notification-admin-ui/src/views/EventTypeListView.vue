<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppLayout from '../components/AppLayout.vue'
import { getEventTypeList, createEventType, updateEventType } from '../api/eventType'

interface EventTypeRecord {
  id: number
  eventTypeCode: string
  publisherCode: string
  displayName: string
  description: string
  payloadSchema: string
  status: string
  version: number
  createTime: string
  updateTime: string
}

const router = useRouter()
const keyword = ref('')
const publisherCodeFilter = ref('')
const statusFilter = ref('')
const tableData = ref<EventTypeRecord[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const dialogVisible = ref(false)
const dialogTitle = ref('新增事件类型')
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  eventTypeCode: '',
  publisherCode: '',
  displayName: '',
  description: '',
  payloadSchema: '',
  status: 'DRAFT'
})

const statusOptions = [
  { label: 'DRAFT', value: 'DRAFT', type: 'info' as const },
  { label: 'ACTIVE', value: 'ACTIVE', type: 'success' as const },
  { label: 'DEPRECATED', value: 'DEPRECATED', type: 'warning' as const }
]

function getStatusType(status: string) {
  return statusOptions.find(o => o.value === status)?.type || 'info'
}

async function fetchList() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (keyword.value) params.keyword = keyword.value
    if (publisherCodeFilter.value) params.publisherCode = publisherCodeFilter.value
    if (statusFilter.value) params.status = statusFilter.value

    const res = await getEventTypeList(params as Parameters<typeof getEventTypeList>[0])
    tableData.value = res.data.records || res.data.content || []
    total.value = res.data.total || 0
  } catch {
    ElMessage.error('加载事件类型列表失败')
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

function handleCreate() {
  dialogTitle.value = '新增事件类型'
  isEdit.value = false
  editingId.value = null
  form.eventTypeCode = ''
  form.publisherCode = ''
  form.displayName = ''
  form.description = ''
  form.payloadSchema = ''
  form.status = 'DRAFT'
  dialogVisible.value = true
}

function handleEdit(row: EventTypeRecord) {
  dialogTitle.value = '编辑事件类型'
  isEdit.value = true
  editingId.value = row.id
  form.eventTypeCode = row.eventTypeCode
  form.publisherCode = row.publisherCode
  form.displayName = row.displayName
  form.description = row.description || ''
  form.payloadSchema = row.payloadSchema || ''
  form.status = row.status
  dialogVisible.value = true
}

function handleDetail(id: number) {
  router.push(`/event-types/${id}`)
}

async function handleSubmit() {
  if (!form.displayName) {
    ElMessage.warning('请填写显示名称')
    return
  }
  try {
    if (isEdit.value && editingId.value) {
      await updateEventType(editingId.value, {
        displayName: form.displayName,
        description: form.description,
        payloadSchema: form.payloadSchema || null,
        status: form.status
      })
      ElMessage.success('更新成功')
    } else {
      if (!form.eventTypeCode || !form.publisherCode) {
        ElMessage.warning('请填写事件类型编码和发布方编码')
        return
      }
      await createEventType({
        eventTypeCode: form.eventTypeCode,
        publisherCode: form.publisherCode,
        displayName: form.displayName,
        description: form.description,
        payloadSchema: form.payloadSchema || null
      })
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch (error: unknown) {
    const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message || '操作失败'
    ElMessage.error(msg)
  }
}

function truncate(str: string, len: number): string {
  if (!str) return ''
  return str.length > len ? str.substring(0, len) + '...' : str
}

onMounted(fetchList)
</script>

<template>
  <AppLayout>
    <div class="event-type-list">
      <div class="search-bar">
        <el-input v-model="keyword" placeholder="搜索事件类型编码/名称" clearable style="width: 250px" @keyup.enter="handleSearch" />
        <el-input v-model="publisherCodeFilter" placeholder="发布方编码" clearable style="width: 160px" @keyup.enter="handleSearch" />
        <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 140px">
          <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button type="success" @click="handleCreate">新增事件类型</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="eventTypeCode" label="事件类型编码" width="180" show-overflow-tooltip />
        <el-table-column prop="displayName" label="显示名称" width="150" />
        <el-table-column prop="publisherCode" label="发布方" width="140" />
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="70" align="center" />
        <el-table-column label="Payload Schema" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="schema-cell">{{ truncate(row.payloadSchema, 60) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="primary" @click="handleDetail(row.id)">详情</el-button>
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

      <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
        <el-form label-width="120px">
          <el-form-item label="事件类型编码" required>
            <el-input v-model="form.eventTypeCode" :disabled="isEdit" placeholder="如 ORDER_CREATED" />
          </el-form-item>
          <el-form-item label="发布方编码" required>
            <el-input v-model="form.publisherCode" :disabled="isEdit" placeholder="如 order-service" />
          </el-form-item>
          <el-form-item label="显示名称" required>
            <el-input v-model="form.displayName" placeholder="如 订单创建事件" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="form.description" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="Payload Schema">
            <el-input v-model="form.payloadSchema" type="textarea" :rows="6" placeholder="JSON Schema（可选）" />
          </el-form-item>
          <el-form-item label="状态" v-if="isEdit">
            <el-select v-model="form.status">
              <el-option v-for="opt in statusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
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
.event-type-list { padding: 0; }
.search-bar { display: flex; gap: 12px; margin-bottom: 16px; align-items: center; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
.schema-cell { font-family: monospace; font-size: 12px; color: #909399; }
</style>
