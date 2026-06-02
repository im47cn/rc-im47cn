<script setup lang="ts">
import { ref, watch, onMounted, onBeforeUnmount, shallowRef } from 'vue'
import loader from '@monaco-editor/loader'
import type * as Monaco from 'monaco-editor'

export interface EditorMarker {
  startLineNumber: number
  startColumn: number
  endLineNumber: number
  endColumn: number
  message: string
  severity?: 'error' | 'warning' | 'info'
}

const props = withDefaults(defineProps<{
  modelValue?: string
  language?: string
  readOnly?: boolean
  height?: string
  markers?: EditorMarker[]
}>(), {
  modelValue: '',
  language: 'json',
  readOnly: false,
  height: '300px',
  markers: () => []
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const containerRef = ref<HTMLDivElement>()
const editorInstance = shallowRef<Monaco.editor.IStandaloneCodeEditor>()
let monacoInstance: typeof Monaco | null = null

// 内部标志，防止 watch 触发时重复设值
let isUpdatingFromEditor = false

onMounted(async () => {
  monacoInstance = await loader.init()

  if (!containerRef.value || !monacoInstance) return

  const editor = monacoInstance.editor.create(containerRef.value, {
    value: props.modelValue,
    language: props.language,
    readOnly: props.readOnly,
    minimap: { enabled: false },
    scrollBeyondLastLine: false,
    automaticLayout: true,
    fontSize: 13,
    tabSize: 2
  })

  editorInstance.value = editor

  // 内容变更时同步到父组件
  editor.onDidChangeModelContent(() => {
    const value = editor.getValue()
    isUpdatingFromEditor = true
    emit('update:modelValue', value)
    // 下一个微任务恢复标志
    Promise.resolve().then(() => { isUpdatingFromEditor = false })
  })

  // 初始设置 markers
  applyMarkers()
})

onBeforeUnmount(() => {
  editorInstance.value?.dispose()
})

// 外部值变更时同步到编辑器
watch(() => props.modelValue, (newVal) => {
  if (isUpdatingFromEditor) return
  const editor = editorInstance.value
  if (editor && editor.getValue() !== newVal) {
    editor.setValue(newVal)
  }
})

// 语言变更
watch(() => props.language, (newLang) => {
  const model = editorInstance.value?.getModel()
  if (model && monacoInstance) {
    monacoInstance.editor.setModelLanguage(model, newLang)
  }
})

// readOnly 变更
watch(() => props.readOnly, (newVal) => {
  editorInstance.value?.updateOptions({ readOnly: newVal })
})

// markers 变更时更新编辑器诊断标记
watch(() => props.markers, () => {
  applyMarkers()
}, { deep: true })

function applyMarkers() {
  const editor = editorInstance.value
  const model = editor?.getModel()
  if (!model || !monacoInstance) return

  const severityMap: Record<string, Monaco.MarkerSeverity> = {
    error: monacoInstance.MarkerSeverity.Error,
    warning: monacoInstance.MarkerSeverity.Warning,
    info: monacoInstance.MarkerSeverity.Info
  }

  const monacoMarkers: Monaco.editor.IMarkerData[] = props.markers.map(m => ({
    startLineNumber: m.startLineNumber,
    startColumn: m.startColumn,
    endLineNumber: m.endLineNumber,
    endColumn: m.endColumn,
    message: m.message,
    severity: severityMap[m.severity || 'error'] || monacoInstance!.MarkerSeverity.Error
  }))

  monacoInstance.editor.setModelMarkers(model, 'custom', monacoMarkers)
}
</script>

<template>
  <div ref="containerRef" :style="{ height, width: '100%', border: '1px solid #dcdfe6' }" />
</template>
