<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppLayout from '../components/AppLayout.vue'
import { getSupplierList, toggleSupplierStatus } from '../api/supplier'

const router = useRouter()

// 搜索条件
const keyword = ref('')
const statusFilter = ref<number | ''>('')

// 表格数据
const tableData = ref<Record<string, unknown>[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

async function fetchList() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: currentPage.value,
      size: pageSize.value
    }
    if (keyword.value) params.keyword = keyword.value
    if (statusFilter.value !== '') params.status = statusFilter.value

    const res = await getSupplierList(params as Parameters<typeof getSupplierList>[0])
    tableData.value = res.data.records || res.data.content || []
    total.value = res.data.total || 0
  } catch {
    ElMessage.error('加载供应商列表失败')
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

async function handleToggleStatus(row: Record<string, unknown>) {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确认${action}供应商 "${row.supplierName || row.supplier_name}" ?`, '提示', {
      type: 'warning'
    })
    await toggleSupplierStatus(row.id as number, newStatus)
    ElMessage.success(`${action}成功`)
    fetchList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${action}失败`)
    }
  }
}

function handleEdit(id: number) {
  router.push(`/suppliers/${id}/edit`)
}

function handleSimulate(id: number) {
  router.push(`/suppliers/${id}/simulate`)
}

function handleCreate() {
  router.push('/suppliers/create')
}

onMounted(fetchList)
</script>

<template>
  <AppLayout>
    <div class="supplier-list">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索供应商编码/名称"
          clearable
          style="width: 250px"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="statusFilter"
          placeholder="状态筛选"
          clearable
          style="width: 140px"
        >
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button type="success" @click="handleCreate">新增供应商</el-button>
      </div>

      <!-- 数据表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="supplierCode" label="供应商编码" width="160">
          <template #default="{ row }">
            {{ row.supplierCode || row.supplier_code }}
          </template>
        </el-table-column>
        <el-table-column prop="supplierName" label="供应商名称" width="160">
          <template #default="{ row }">
            {{ row.supplierName || row.supplier_name }}
          </template>
        </el-table-column>
        <el-table-column prop="baseUrl" label="基础URL" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.baseUrl || row.base_url }}
          </template>
        </el-table-column>
        <el-table-column prop="httpMethod" label="HTTP方法" width="100">
          <template #default="{ row }">
            {{ row.httpMethod || row.http_method }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="workerConcurrency" label="并发数" width="80" align="center">
          <template #default="{ row }">
            {{ row.workerConcurrency || row.worker_concurrency }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row.id)">编辑</el-button>
            <el-button
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="handleToggleStatus(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button size="small" type="info" @click="handleSimulate(row.id)">仿真</el-button>
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
.supplier-list {
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
