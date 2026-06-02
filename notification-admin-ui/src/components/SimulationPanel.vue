<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import MonacoEditor from './MonacoEditor.vue'
import type { EditorMarker } from './MonacoEditor.vue'
import { transform, fullPreview } from '../api/simulation'

const props = withDefaults(defineProps<{
  /** 供应商基础配置，用于完整请求预览 */
  supplierConfig?: {
    baseUrl: string
    httpMethod: string
    contentTypeBehavior: string
    pathTemplate?: string
    queryTemplate?: string
    headerTemplate?: string
    bodyTemplate?: string
  }
}>(), {
  supplierConfig: undefined
})

// JSONata 表达式（由父组件 v-model 绑定）
const expression = defineModel<string>('expression', { default: '' })

// Mock 输入上下文
const mockContext = ref('{\n  "event": {\n    "type": "SMS",\n    "payload": {}\n  },\n  "auth": {}\n}')

// 转换结果
const resultText = ref('')
const resultMarkers = ref<EditorMarker[]>([])
const previewText = ref('')
const previewLoading = ref(false)

// 500ms 防抖定时器
let debounceTimer: ReturnType<typeof setTimeout> | null = null

function debouncedTransform() {
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(executeTransform, 500)
}

async function executeTransform() {
  // 任一编辑器为空则不调用
  if (!expression.value.trim() || !mockContext.value.trim()) {
    resultText.value = ''
    resultMarkers.value = []
    return
  }

  let parsedContext: unknown
  try {
    parsedContext = JSON.parse(mockContext.value)
  } catch {
    resultText.value = '// Mock Input Context JSON 格式错误'
    resultMarkers.value = []
    return
  }

  try {
    const res = await transform(expression.value, parsedContext)
    resultText.value = typeof res.data.result === 'string'
      ? res.data.result
      : JSON.stringify(res.data.result, null, 2)
    resultMarkers.value = []
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string; offset?: number } } }
    const data = err.response?.data
    resultText.value = `// 错误: ${data?.message || '转换失败'}`

    // 根据后端返回的 offset 设置错误标记
    if (data?.offset !== undefined && data.offset >= 0) {
      const pos = offsetToPosition(expression.value, data.offset)
      resultMarkers.value = [{
        startLineNumber: pos.line,
        startColumn: pos.column,
        endLineNumber: pos.line,
        endColumn: pos.column + 1,
        message: data.message || '表达式错误',
        severity: 'error'
      }]
    } else {
      resultMarkers.value = []
    }
  }
}

/** 将字符偏移量转换为行列号 */
function offsetToPosition(text: string, offset: number): { line: number; column: number } {
  let line = 1
  let column = 1
  for (let i = 0; i < offset && i < text.length; i++) {
    if (text[i] === '\n') {
      line++
      column = 1
    } else {
      column++
    }
  }
  return { line, column }
}

/** 完整请求预览 */
async function handleFullPreview() {
  if (!props.supplierConfig) {
    ElMessage.warning('缺少供应商配置信息')
    return
  }

  let parsedContext: unknown
  try {
    parsedContext = JSON.parse(mockContext.value)
  } catch {
    ElMessage.error('Mock Input Context JSON 格式错误')
    return
  }

  previewLoading.value = true
  try {
    const res = await fullPreview({
      ...props.supplierConfig,
      mockInputContext: parsedContext
    })
    previewText.value = JSON.stringify(res.data, null, 2)
  } catch (error: unknown) {
    const err = error as { response?: { data?: { message?: string } } }
    previewText.value = `// 预览失败: ${err.response?.data?.message || '请求错误'}`
  } finally {
    previewLoading.value = false
  }
}

// 监听表达式和上下文变化，防抖触发转换
watch(expression, debouncedTransform)
watch(mockContext, debouncedTransform)
</script>

<template>
  <div class="simulation-panel">
    <div class="panel-top">
      <div class="panel-editor">
        <div class="panel-label">JSONata 表达式</div>
        <MonacoEditor
          v-model="expression"
          language="plaintext"
          height="200px"
          :markers="resultMarkers"
        />
      </div>
      <div class="panel-editor">
        <div class="panel-label">Mock Input Context</div>
        <MonacoEditor
          v-model="mockContext"
          language="json"
          height="200px"
        />
      </div>
    </div>
    <div class="panel-bottom">
      <div class="panel-editor">
        <div class="panel-label">转换结果</div>
        <MonacoEditor
          :model-value="resultText"
          language="json"
          :read-only="true"
          height="160px"
        />
      </div>
      <div class="panel-editor">
        <div class="panel-label">
          完整请求预览
          <el-button
            size="small"
            type="primary"
            :loading="previewLoading"
            @click="handleFullPreview"
          >
            预览
          </el-button>
        </div>
        <MonacoEditor
          :model-value="previewText"
          language="json"
          :read-only="true"
          height="160px"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.simulation-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.panel-top,
.panel-bottom {
  display: flex;
  gap: 12px;
}

.panel-editor {
  flex: 1;
  min-width: 0;
}

.panel-label {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}
</style>
