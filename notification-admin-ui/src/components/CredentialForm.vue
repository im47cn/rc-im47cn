<script setup lang="ts">
import { ref, watch } from 'vue'

export interface CredentialItem {
  key: string
  value: string
  /** 编辑模式下，是否保留原值 */
  keepOriginal: boolean
  /** 编辑模式下标识该项来自已有配置 */
  isExisting: boolean
}

const props = withDefaults(defineProps<{
  /** 编辑模式：true 时显示已有凭证的遮掩值 */
  editMode?: boolean
  /** 编辑模式下已有的凭证 key 列表 */
  existingKeys?: string[]
}>(), {
  editMode: false,
  existingKeys: () => []
})

const credentials = ref<CredentialItem[]>([])

// 编辑模式初始化已有凭证
watch(() => props.existingKeys, (keys) => {
  if (props.editMode && keys.length > 0) {
    credentials.value = keys.map(key => ({
      key,
      value: '******',
      keepOriginal: true,
      isExisting: true
    }))
  }
}, { immediate: true })

function addCredential() {
  credentials.value.push({
    key: '',
    value: '',
    keepOriginal: false,
    isExisting: false
  })
}

function removeCredential(index: number) {
  credentials.value.splice(index, 1)
}

function toggleKeepOriginal(item: CredentialItem) {
  item.keepOriginal = !item.keepOriginal
  if (item.keepOriginal) {
    item.value = '******'
  } else {
    item.value = ''
  }
}

/** 获取需要提交的凭证数据（排除保留原值的项） */
function getSubmitData(): Record<string, string> | null {
  const result: Record<string, string> = {}
  let hasChanges = false

  for (const item of credentials.value) {
    if (!item.key.trim()) continue
    if (item.keepOriginal) continue
    result[item.key] = item.value
    hasChanges = true
  }

  // 新增模式下或有变更时返回数据
  if (!props.editMode || hasChanges) {
    return result
  }
  return null
}

defineExpose({ getSubmitData })
</script>

<template>
  <div class="credential-form">
    <div v-for="(item, index) in credentials" :key="index" class="credential-row">
      <el-input
        v-model="item.key"
        placeholder="凭证名称 (Key)"
        :disabled="item.isExisting"
        style="width: 200px"
      />
      <el-input
        v-model="item.value"
        :placeholder="item.keepOriginal ? '保留原值' : '凭证值 (Value)'"
        :disabled="item.keepOriginal"
        :type="item.keepOriginal ? 'text' : 'password'"
        show-password
        style="flex: 1"
      />
      <el-button
        v-if="item.isExisting"
        :type="item.keepOriginal ? 'info' : 'warning'"
        size="small"
        @click="toggleKeepOriginal(item)"
      >
        {{ item.keepOriginal ? '重新输入' : '保留原值' }}
      </el-button>
      <el-button type="danger" size="small" @click="removeCredential(index)">
        删除
      </el-button>
    </div>
    <el-button type="primary" plain size="small" @click="addCredential">
      + 添加凭证
    </el-button>
  </div>
</template>

<style scoped>
.credential-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.credential-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
