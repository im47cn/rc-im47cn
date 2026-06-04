<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import AppLayout from '../components/AppLayout.vue'
import { getEventType } from '../api/eventType'
import { getFieldFingerprints } from '../api/fieldFingerprint'
import { getChangeRecords, confirmChangeRecord, dismissChangeRecord } from '../api/changeRecord'

interface EventTypeInfo {
  id: number
  eventTypeCode: string
  publisherCode: string
  displayName: string
  description: string
  payloadSchema: string
  status: string
  version: number
}

interface FingerprintRecord {
  id: number
  eventTypeCode: string
  fieldPath: string
  observedType: string
  firstSeenAt: string
  lastSeenAt: string
  sampleCount: number
  status: string
}

interface ChangeRecordItem {
  id: number
  eventTypeCode: string
  changeType: string
  fieldPath: string
  oldValue: string
  newValue: string
  detectionSource: string
  confidence: string
  status: string
  affectedSubscriptions: string
  createdAt: string
}

const route = useRoute()
const router = useRouter()
const activeTab = ref('fields')
const eventTypeInfo = ref<EventTypeInfo | null>(null)
const fingerprints = ref<FingerprintRecord[]>([])
const changeRecords = ref<ChangeRecordItem[]>([])
const loadingInfo = ref(false)
const loadingFingerprints = ref(false)
const loadingChanges = ref(false)

const eventTypeId = Number(route.params.id)

const changeTypeMap: Record<string, { label: string; type: string }> = {
  FIELD_ADDED: { label: '字段新增', type: 'success' },
  FIELD_REMOVED: { label: '字段移除', type: 'danger' },
  FIELD_TYPE_CHANGED: { label: '类型变更', type: 'warning' },
  SCHEMA_UPDATED: { label: 'Schema更新', type: 'info' }
}

const confidenceMap: Record<string, string> = {
  HIGH: 'success',
  MEDIUM: 'warning',
  LOW: 'danger'
}

const changeStatusMap: Record<string, { label: string; type: string }> = {
  PENDING_REVIEW: { label: '待审核', type: 'warning' },
  CONFIRMED: { label: '已确认', type: 'success' },
  DISMISSED: { label: '已驳回', type: 'info' }
}

async function fetchEventType() {
  loadingInfo.value = true
  try {
    const res = await getEventType(eventTypeId)
    eventTypeInfo.value = res.data
  } catch {
    ElMessage.error('加载事件类型失败')
  } finally {
    loadingInfo.value = false
  }
}

async function fetchFingerprints() {
  if (!eventTypeInfo.value) return
  loadingFingerprints.value = true
  try {
    const res = await getFieldFingerprints({ eventTypeCode: eventTypeInfo.value.eventTypeCode })
    fingerprints.value = res.data || []
  } catch {
    fingerprints.value = []
  } finally {
    loadingFingerprints.value = false
  }
}

async function fetchChangeRecords() {
  if (!eventTypeInfo.value) return
  loadingChanges.value = true
  try {
    const res = await getChangeRecords({ eventTypeCode: eventTypeInfo.value.eventTypeCode })
    changeRecords.value = res.data?.records || res.data?.content || res.data || []
  } catch {
    changeRecords.value = []
  } finally {
    loadingChanges.value = false
  }
}

async function handleConfirm(record: ChangeRecordItem) {
  try {
    await confirmChangeRecord(record.id)
    ElMessage.success('已确认')
    fetchChangeRecords()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function handleDismiss(record: ChangeRecordItem) {
  try {
    await dismissChangeRecord(record.id)
    ElMessage.success('已驳回')
    fetchChangeRecords()
  } catch {
    ElMessage.error('操作失败')
  }
}

function parseAffected(json: string): string[] {
  if (!json) return []
  try { return JSON.parse(json) } catch { return [] }
}

function goBack() {
  router.push('/event-types')
}

onMounted(async () => {
  await fetchEventType()
  fetchFingerprints()
  fetchChangeRecords()
})
</script>

<template>
  <AppLayout>
    <div class="event-type-detail" v-loading="loadingInfo">
      <div class="detail-header">
        <el-button :icon="ArrowLeft" @click="goBack">返回</el-button>
        <template v-if="eventTypeInfo">
          <h3 style="margin: 0 16px">{{ eventTypeInfo.displayName }}</h3>
          <el-tag :type="eventTypeInfo.status === 'ACTIVE' ? 'success' : eventTypeInfo.status === 'DEPRECATED' ? 'warning' : 'info'">
            {{ eventTypeInfo.status }}
          </el-tag>
          <span class="meta-info">
            编码: {{ eventTypeInfo.eventTypeCode }} | 发布方: {{ eventTypeInfo.publisherCode }} | 版本: v{{ eventTypeInfo.version }}
          </span>
        </template>
      </div>

      <el-tabs v-model="activeTab" style="margin-top: 16px">
        <!-- Tab 1: 字段结构 -->
        <el-tab-pane label="字段结构" name="fields">
          <el-table :data="fingerprints" v-loading="loadingFingerprints" border stripe empty-text="暂无字段指纹数据">
            <el-table-column prop="fieldPath" label="字段路径" min-width="220" show-overflow-tooltip />
            <el-table-column prop="observedType" label="观测类型" width="120" align="center">
              <template #default="{ row }">
                <el-tag size="small">{{ row.observedType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" size="small">
                  {{ row.status === 'ACTIVE' ? '活跃' : '已消失' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="sampleCount" label="采样数" width="90" align="center" />
            <el-table-column prop="firstSeenAt" label="首次出现" width="170" />
            <el-table-column prop="lastSeenAt" label="最后出现" width="170" />
          </el-table>
        </el-tab-pane>

        <!-- Tab 2: 变更历史 -->
        <el-tab-pane label="变更历史" name="changes">
          <el-table :data="changeRecords" v-loading="loadingChanges" border stripe empty-text="暂无变更记录">
            <el-table-column label="变更类型" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="(changeTypeMap[row.changeType]?.type as 'success' | 'danger' | 'warning' | 'info') || 'info'" size="small">
                  {{ changeTypeMap[row.changeType]?.label || row.changeType }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="fieldPath" label="字段路径" min-width="200" show-overflow-tooltip />
            <el-table-column label="变更内容" width="200">
              <template #default="{ row }">
                <span v-if="row.oldValue || row.newValue">
                  <span class="old-value">{{ row.oldValue || '(无)' }}</span>
                  →
                  <span class="new-value">{{ row.newValue || '(无)' }}</span>
                </span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column label="检测来源" width="120" align="center">
              <template #default="{ row }">
                {{ row.detectionSource === 'SCHEMA_DIFF' ? 'Schema Diff' : '运行时推断' }}
              </template>
            </el-table-column>
            <el-table-column label="置信度" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="(confidenceMap[row.confidence] as 'success' | 'warning' | 'danger') || 'info'" size="small">
                  {{ row.confidence }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="(changeStatusMap[row.status]?.type as 'warning' | 'success' | 'info') || 'info'" size="small">
                  {{ changeStatusMap[row.status]?.label || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="检测时间" width="170" />
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 'PENDING_REVIEW'">
                  <el-button size="small" type="success" @click="handleConfirm(row)">确认</el-button>
                  <el-button size="small" type="info" @click="handleDismiss(row)">驳回</el-button>
                </template>
                <span v-else class="action-done">已处理</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Tab 3: 受影响订阅 -->
        <el-tab-pane label="受影响订阅" name="affected">
          <div v-if="changeRecords.filter(r => r.affectedSubscriptions).length === 0" class="empty-tip">
            暂无受影响的订阅关系
          </div>
          <el-collapse v-else>
            <el-collapse-item
              v-for="record in changeRecords.filter(r => r.affectedSubscriptions)"
              :key="record.id"
              :title="`${changeTypeMap[record.changeType]?.label || record.changeType}: ${record.fieldPath}`"
            >
              <div class="affected-list">
                <el-tag
                  v-for="sub in parseAffected(record.affectedSubscriptions)"
                  :key="sub"
                  style="margin: 4px"
                >
                  {{ sub }}
                </el-tag>
              </div>
            </el-collapse-item>
          </el-collapse>
        </el-tab-pane>
      </el-tabs>
    </div>
  </AppLayout>
</template>

<style scoped>
.event-type-detail { padding: 0; }
.detail-header { display: flex; align-items: center; gap: 8px; }
.meta-info { color: #909399; font-size: 13px; margin-left: 16px; }
.old-value { color: #F56C6C; text-decoration: line-through; }
.new-value { color: #67C23A; font-weight: 500; }
.action-done { color: #909399; font-size: 13px; }
.empty-tip { text-align: center; color: #909399; padding: 40px 0; }
.affected-list { padding: 8px; }
</style>
