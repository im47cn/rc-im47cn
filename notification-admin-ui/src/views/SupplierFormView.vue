<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import AppLayout from '../components/AppLayout.vue'
import CredentialForm from '../components/CredentialForm.vue'
import SimulationPanel from '../components/SimulationPanel.vue'
import { getSupplier, createSupplier, updateSupplier } from '../api/supplier'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => !!route.params.id)
const supplierId = computed(() => Number(route.params.id))
const pageTitle = computed(() => isEdit.value ? '编辑供应商' : '新增供应商')

const formRef = ref<FormInstance>()
const credentialRef = ref<InstanceType<typeof CredentialForm>>()
const loading = ref(false)
const submitLoading = ref(false)

// 编辑模式下已有凭证 key 列表
const existingCredentialKeys = ref<string[]>([])

// 当前选中的模板 Tab
const activeTemplateTab = ref('path')

// 表单数据
const form = reactive({
  supplierCode: '',
  supplierName: '',
  description: '',
  baseUrl: '',
  httpMethod: 'POST',
  contentTypeBehavior: 'APPLICATION_JSON',
  connectTimeoutMs: 3000,
  readTimeoutMs: 5000,
  maxRetryCount: 10,
  retryBackoffInitialMs: 1000,
  retryBackoffMultiplier: 2.00,
  retryBackoffMaxMs: 30000,
  successHttpCodes: '200',
  successBodyPattern: '',
  successBodyMatchMode: 'EQUALS',
  successCaseSensitive: true,
  workerConcurrency: 10,
  pathTemplate: '',
  queryTemplate: '',
  headerTemplate: '',
  bodyTemplate: ''
})

const rules: FormRules = {
  supplierCode: [{ required: true, message: '请输入供应商编码', trigger: 'blur' }],
  supplierName: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
  baseUrl: [{ required: true, message: '请输入基础URL', trigger: 'blur' }],
  bodyTemplate: [{ required: true, message: '请输入Body模板', trigger: 'blur' }]
}

// Body 模板提示文本（根据 Content-Type 联动）
const bodyTemplatePlaceholder = computed(() => {
  if (form.contentTypeBehavior === 'APPLICATION_FORM_URLENCODED') {
    return 'Form 格式示例: "key1=" & value1 & "&key2=" & value2'
  }
  return 'JSON 格式示例: { "mobile": mobile, "content": content }'
})

// 重试时间线预览
const retryTimeline = computed(() => {
  const rows: { attempt: number; interval: number; cumulative: number }[] = []
  let cumulative = 0
  for (let i = 1; i <= form.maxRetryCount; i++) {
    const interval = Math.min(
      form.retryBackoffInitialMs * Math.pow(form.retryBackoffMultiplier, i - 1),
      form.retryBackoffMaxMs
    )
    cumulative += interval
    rows.push({ attempt: i, interval: Math.round(interval), cumulative: Math.round(cumulative) })
  }
  return rows
})

// 格式化毫秒为可读时间
function formatMs(ms: number): string {
  if (ms < 1000) return `${ms}ms`
  if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`
  return `${(ms / 60000).toFixed(1)}min`
}

// 供应商配置（传给 SimulationPanel 的完整预览）
const supplierConfig = computed(() => ({
  baseUrl: form.baseUrl,
  httpMethod: form.httpMethod,
  contentTypeBehavior: form.contentTypeBehavior,
  pathTemplate: form.pathTemplate || undefined,
  queryTemplate: form.queryTemplate || undefined,
  headerTemplate: form.headerTemplate || undefined,
  bodyTemplate: form.bodyTemplate || undefined
}))

// 编辑模式加载数据
onMounted(async () => {
  if (!isEdit.value) return
  loading.value = true
  try {
    const res = await getSupplier(supplierId.value)
    const data = res.data
    // 填充表单（兼容 camelCase 和 snake_case）
    form.supplierCode = data.supplierCode || data.supplier_code || ''
    form.supplierName = data.supplierName || data.supplier_name || ''
    form.description = data.description || ''
    form.baseUrl = data.baseUrl || data.base_url || ''
    form.httpMethod = data.httpMethod || data.http_method || 'POST'
    form.contentTypeBehavior = data.contentTypeBehavior || data.content_type_behavior || 'APPLICATION_JSON'
    form.connectTimeoutMs = data.connectTimeoutMs ?? data.connect_timeout_ms ?? 3000
    form.readTimeoutMs = data.readTimeoutMs ?? data.read_timeout_ms ?? 5000
    form.maxRetryCount = data.maxRetryCount ?? data.max_retry_count ?? 3
    form.retryBackoffInitialMs = data.retryBackoffInitialMs ?? data.retry_backoff_initial_ms ?? 1000
    form.retryBackoffMultiplier = data.retryBackoffMultiplier ?? data.retry_backoff_multiplier ?? 2.0
    form.retryBackoffMaxMs = data.retryBackoffMaxMs ?? data.retry_backoff_max_ms ?? 30000
    form.successHttpCodes = data.successHttpCodes || data.success_http_codes || '200'
    form.successBodyPattern = data.successBodyPattern || data.success_body_pattern || ''
    form.successBodyMatchMode = data.successBodyMatchMode || data.success_body_match_mode || 'EQUALS'
    const caseSensitiveRaw = data.successCaseSensitive ?? data.success_case_sensitive ?? 1
    form.successCaseSensitive = caseSensitiveRaw === true || caseSensitiveRaw === 1
    form.workerConcurrency = data.workerConcurrency ?? data.worker_concurrency ?? 1
    form.pathTemplate = data.pathTemplate || data.path_template || ''
    form.queryTemplate = data.queryTemplate || data.query_template || ''
    form.headerTemplate = data.headerTemplate || data.header_template || ''
    form.bodyTemplate = data.bodyTemplate || data.body_template || ''

    // 凭证 key 列表
    const credKeys = data.credentialKeys || data.credential_keys || []
    existingCredentialKeys.value = Array.isArray(credKeys) ? credKeys : []
  } catch {
    ElMessage.error('加载供应商信息失败')
  } finally {
    loading.value = false
  }
})

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const submitData: Record<string, unknown> = {
      ...form,
      successCaseSensitive: form.successCaseSensitive ? 1 : 0,
      // 空字符串模板转为 null，避免 @NotBlank 校验拒绝
      pathTemplate: form.pathTemplate || null,
      queryTemplate: form.queryTemplate || null,
      headerTemplate: form.headerTemplate || null,
      bodyTemplate: form.bodyTemplate || null,
      successBodyPattern: form.successBodyPattern || null
    }

    // 处理凭证数据
    const credentialData = credentialRef.value?.getSubmitData()
    if (credentialData !== null && credentialData !== undefined) {
      submitData.credentials = credentialData
    }

    if (isEdit.value) {
      await updateSupplier(supplierId.value, submitData)
      ElMessage.success('更新成功')
    } else {
      await createSupplier(submitData)
      ElMessage.success('创建成功')
    }
    router.push('/suppliers')
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } } }
    ElMessage.error(err.response?.data?.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

function handleCancel() {
  router.push('/suppliers')
}
</script>

<template>
  <AppLayout>
    <div v-loading="loading" class="supplier-form">
      <div class="form-header">
        <h2>{{ pageTitle }}</h2>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
        <!-- 基础信息区 -->
        <el-divider content-position="left">基础信息</el-divider>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="供应商编码" prop="supplierCode">
              <el-input v-model="form.supplierCode" :disabled="isEdit" placeholder="如 ALI_YUN" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商名称" prop="supplierName">
              <el-input v-model="form.supplierName" placeholder="供应商业务名称" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="备注说明" />
        </el-form-item>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="基础URL" prop="baseUrl">
              <el-input v-model="form.baseUrl" placeholder="https://api.example.com" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="HTTP方法">
              <el-select v-model="form.httpMethod" style="width: 100%">
                <el-option label="POST" value="POST" />
                <el-option label="PUT" value="PUT" />
                <el-option label="PATCH" value="PATCH" />
                <el-option label="GET" value="GET" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="Content-Type">
              <el-select v-model="form.contentTypeBehavior" style="width: 100%">
                <el-option label="JSON" value="APPLICATION_JSON" />
                <el-option label="Form" value="APPLICATION_FORM_URLENCODED" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="6">
            <el-form-item label="连接超时(ms)">
              <el-input-number v-model="form.connectTimeoutMs" :min="100" :max="30000" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="读取超时(ms)">
              <el-input-number v-model="form.readTimeoutMs" :min="100" :max="60000" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="并发数">
              <el-input-number v-model="form.workerConcurrency" :min="1" :max="50" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 重试策略 -->
        <el-divider content-position="left">重试策略</el-divider>

        <el-row :gutter="24">
          <el-col :span="6">
            <el-form-item label="最大重试次数">
              <el-input-number v-model="form.maxRetryCount" :min="0" :max="10" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="初始延迟(ms)">
              <el-input-number v-model="form.retryBackoffInitialMs" :min="100" :max="60000" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="退避倍数">
              <el-input-number v-model="form.retryBackoffMultiplier" :min="1" :max="10" :precision="2" :step="0.5" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="最大延迟(ms)">
              <el-input-number v-model="form.retryBackoffMaxMs" :min="1000" :max="3600000" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 重试时间线预览 -->
        <el-form-item v-if="form.maxRetryCount > 0" label="">
          <el-table :data="retryTimeline" size="small" border style="width: 100%" max-height="300">
            <el-table-column prop="attempt" label="重试次数" width="100" align="center">
              <template #default="{ row }">第 {{ row.attempt }} 次</template>
            </el-table-column>
            <el-table-column prop="interval" label="等待间隔" width="150" align="center">
              <template #default="{ row }">{{ formatMs(row.interval) }}</template>
            </el-table-column>
            <el-table-column prop="cumulative" label="累计延迟" width="150" align="center">
              <template #default="{ row }">{{ formatMs(row.cumulative) }}</template>
            </el-table-column>
          </el-table>
        </el-form-item>

        <!-- 成功判定规则 -->
        <el-divider content-position="left">成功判定规则</el-divider>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="成功HTTP状态码">
              <el-input v-model="form.successHttpCodes" placeholder="200,201 (逗号分隔)" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="响应体匹配表达式">
              <el-input v-model="form.successBodyPattern" placeholder="可选" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="匹配模式">
              <el-select v-model="form.successBodyMatchMode" style="width: 100%">
                <el-option label="精确匹配 (EQUALS)" value="EQUALS" />
                <el-option label="包含匹配 (CONTAINS)" value="CONTAINS" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4">
            <el-form-item label="大小写敏感">
              <el-switch v-model="form.successCaseSensitive" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 凭证区 -->
        <el-divider content-position="left">凭证配置</el-divider>

        <el-form-item label="">
          <CredentialForm
            ref="credentialRef"
            :edit-mode="isEdit"
            :existing-keys="existingCredentialKeys"
          />
        </el-form-item>

        <!-- 模板编辑区 -->
        <el-divider content-position="left">JSONata 模板</el-divider>

        <el-tabs v-model="activeTemplateTab" type="border-card">
          <el-tab-pane label="Path 模板" name="path">
            <SimulationPanel
              v-model:expression="form.pathTemplate"
              :supplier-config="supplierConfig"
            />
          </el-tab-pane>
          <el-tab-pane label="Query 模板" name="query">
            <SimulationPanel
              v-model:expression="form.queryTemplate"
              :supplier-config="supplierConfig"
            />
          </el-tab-pane>
          <el-tab-pane label="Header 模板" name="header">
            <SimulationPanel
              v-model:expression="form.headerTemplate"
              :supplier-config="supplierConfig"
            />
          </el-tab-pane>
          <el-tab-pane label="Body 模板" name="body">
            <el-alert
              :title="bodyTemplatePlaceholder"
              :type="form.contentTypeBehavior === 'APPLICATION_FORM_URLENCODED' ? 'warning' : 'info'"
              :closable="false"
              show-icon
              style="margin-bottom: 12px"
            />
            <SimulationPanel
              v-model:expression="form.bodyTemplate"
              :supplier-config="supplierConfig"
            />
          </el-tab-pane>
        </el-tabs>

        <!-- 提交按钮 -->
        <div class="form-actions">
          <el-button @click="handleCancel">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            {{ isEdit ? '保存' : '创建' }}
          </el-button>
        </div>
      </el-form>
    </div>
  </AppLayout>
</template>

<style scoped>
.supplier-form {
  max-width: 1200px;
}

.form-header {
  margin-bottom: 20px;
}

.form-header h2 {
  margin: 0;
  font-size: 18px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #e6e6e6;
}
</style>
