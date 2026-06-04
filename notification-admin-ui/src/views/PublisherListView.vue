<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CopyDocument, Refresh } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { getPublisherList, createPublisher, updatePublisher, rotateApiKey } from '../api/publisher'

interface PublisherRecord {
  id: number
  publisherCode: string
  publisherName: string
  apiKey: string
  status: number
  contactInfo: string
  createTime: string
  updateTime: string
}

const keyword = ref('')
const statusFilter = ref<number | ''>('')
const tableData = ref<PublisherRecord[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('新增发布方')
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const form = reactive({
  publisherCode: '',
  publisherName: '',
  contactInfo: ''
})

async function fetchList() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value !== '') params.status = statusFilter.value

    const res = await getPublisherList(params as Parameters<typeof getPublisherList>[0])
    tableData.value = res.data.records || res.data.content || []
    total.value = res.data.total || 0
  } catch {
    ElMessage.error('加载发布方列表失败')
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
  dialogTitle.value = '新增发布方'
  isEdit.value = false
  editingId.value = null
  form.publisherCode = ''
  form.publisherName = ''
  form.contactInfo = ''
  dialogVisible.value = true
}

function handleEdit(row: PublisherRecord) {
  dialogTitle.value = '编辑发布方'
  isEdit.value = true
  editingId.value = row.id
  form.publisherCode = row.publisherCode
  form.publisherName = row.publisherName
  form.contactInfo = row.contactInfo || ''
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!form.publisherName) {
    ElMessage.warning('请填写发布方名称')
    return
  }
  try {
    if (isEdit.value && editingId.value) {
      await updatePublisher(editingId.value, {
        publisherName: form.publisherName,
        contactInfo: form.contactInfo
      })
      ElMessage.success('更新成功')
    } else {
      if (!form.publisherCode) {
        ElMessage.warning('请填写发布方编码')
        return
      }
      const res = await createPublisher({
        publisherCode: form.publisherCode,
        publisherName: form.publisherName,
        contactInfo: form.contactInfo
      })
      ElMessage.success('创建成功，API Key: ' + res.data.apiKey)
    }
    dialogVisible.value = false
    fetchList()
  } catch (error: unknown) {
    const msg = (error as { response?: { data?: { message?: string } } })?.response?.data?.message || '操作失败'
    ElMessage.error(msg)
  }
}

async function handleRotateKey(row: PublisherRecord) {
  try {
    await ElMessageBox.confirm(
      `确认轮换发布方 "${row.publisherName}" 的 API Key？旧 Key 将立即失效。`,
      '轮换 API Key',
      { type: 'warning' }
    )
    const res = await rotateApiKey(row.id)
    ElMessage.success('新 API Key: ' + res.data.apiKey)
    fetchList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('轮换失败')
    }
  }
}

function maskApiKey(key: string): string {
  if (!key) return ''
  return key.length > 12 ? key.substring(0, 12) + '...' : key
}

async function copyApiKey(key: string) {
  try {
    await navigator.clipboard.writeText(key)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

onMounted(fetchList)
</script>

<template>
  <AppLayout>
    <div class="publisher-list">
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索发布方编码/名称"
          clearable
          style="width: 250px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 140px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button type="success" @click="handleCreate">新增发布方</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="publisherCode" label="发布方编码" width="160" />
        <el-table-column prop="publisherName" label="发布方名称" width="160" />
        <el-table-column label="API Key" min-width="200">
          <template #default="{ row }">
            <span>{{ maskApiKey(row.apiKey) }}</span>
            <el-button :icon="CopyDocument" size="small" text @click="copyApiKey(row.apiKey)" />
          </template>
        </el-table-column>
        <el-table-column prop="contactInfo" label="联系方式" width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="warning" :icon="Refresh" @click="handleRotateKey(row)">
              轮换Key
            </el-button>
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

      <!-- 新增/编辑弹窗 -->
      <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
        <el-form label-width="100px">
          <el-form-item label="发布方编码" required>
            <el-input v-model="form.publisherCode" :disabled="isEdit" placeholder="如 order-service" />
          </el-form-item>
          <el-form-item label="发布方名称" required>
            <el-input v-model="form.publisherName" placeholder="如 订单服务" />
          </el-form-item>
          <el-form-item label="联系方式">
            <el-input v-model="form.contactInfo" placeholder="如 dev@example.com" />
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
.publisher-list {
  padding: 0;
}
.search-bar {
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
</style>
