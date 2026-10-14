<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { Refresh, WarningFilled } from '@element-plus/icons-vue'
import request from '../utils/request'

const loading = ref(false)
const savingGroup = ref('')
const sendingBroadcast = ref(false)
const activeGroup = ref('PARAM')
const summary = ref([])
const broadcastSummary = ref({})
const broadcastList = ref([])
const groups = reactive({ PARAM: [], EXPORT: [], BROADCAST: [] })
const lastSavedAt = reactive({ PARAM: '', EXPORT: '', BROADCAST: '' })
const broadcastForm = reactive({
  title: '',
  content: '',
  scope: 'ALL'
})
const broadcastQuery = reactive({
  pageNo: 1,
  pageSize: 8,
  keyword: ''
})
const broadcastFilters = reactive({
  scope: '',
  dateRange: []
})

const groupMeta = {
  PARAM: {
    label: '基础参数',
    note: '控制平台运行参数、默认行为和业务开关。',
    tone: 'primary'
  },
  EXPORT: {
    label: '导出设置',
    note: '控制统计、日志、数据导出相关默认值。',
    tone: 'success'
  },
  BROADCAST: {
    label: '广播默认值',
    note: '维护全局广播默认标题、内容和推送策略。',
    tone: 'warning'
  }
}

const scopeMeta = {
  ALL: { label: '全体', type: 'primary' },
  USER: { label: '学员', type: 'success' },
  TEACHER: { label: '教师', type: 'warning' }
}

const totalConfigCount = computed(() =>
  Object.values(groups).reduce((sum, items) => sum + items.length, 0)
)

const changedMap = computed(() => {
  const result = {}
  Object.keys(groups).forEach((group) => {
    result[group] = groups[group].filter((item) => String(item.configValue ?? '') !== String(item.originalValue ?? ''))
  })
  return result
})

const activeChangedItems = computed(() => changedMap.value[activeGroup.value] || [])

const systemMetrics = computed(() => [
  {
    label: '配置项总数',
    value: totalConfigCount.value,
    note: '当前可维护系统配置',
    tone: 'primary'
  },
  {
    label: '未保存变更',
    value: Object.values(changedMap.value).reduce((sum, items) => sum + items.length, 0),
    note: '变更保存后立即生效',
    tone: Object.values(changedMap.value).some((items) => items.length) ? 'warning' : 'success'
  },
  {
    label: '在线接收数',
    value: broadcastSummary.value.onlineCount || 0,
    note: '当前 WebSocket 在线连接',
    tone: 'success'
  },
  {
    label: '广播总数',
    value: broadcastSummary.value.broadcastCount || 0,
    note: broadcastSummary.value.latestBroadcastTitle || '暂无最近广播',
    tone: 'primary'
  }
])

const filteredBroadcastList = computed(() => {
  const [start, end] = broadcastFilters.dateRange || []
  return (broadcastList.value || []).filter((item) => {
    const sentAt = item.sentAt ? String(item.sentAt).slice(0, 10) : ''
    if (broadcastFilters.scope && item.scope !== broadcastFilters.scope) return false
    if (start && sentAt < start) return false
    if (end && sentAt > end) return false
    return true
  })
})

function normalizeGroup(items = []) {
  return items.map((item) => ({
    ...item,
    configValue: item.configValue ?? '',
    originalValue: item.configValue ?? ''
  }))
}

async function loadBroadcasts() {
  const page = await request.get('/api/admin/broadcasts', { params: broadcastQuery })
  broadcastList.value = page.records || []
}

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/admin/system-configs')
    summary.value = data.summary || []
    groups.PARAM = normalizeGroup(data.groups?.PARAM)
    groups.EXPORT = normalizeGroup(data.groups?.EXPORT)
    groups.BROADCAST = normalizeGroup(data.groups?.BROADCAST)
    broadcastSummary.value = await request.get('/api/admin/broadcasts/summary')
    await loadBroadcasts()
    broadcastForm.title = broadcastForm.title || groups.BROADCAST.find((item) => item.configKey === 'broadcast_title')?.configValue || ''
    broadcastForm.content = broadcastForm.content || groups.BROADCAST.find((item) => item.configKey === 'broadcast_content')?.configValue || ''
  } finally {
    loading.value = false
  }
}

function inferControlType(item) {
  const key = `${item.configKey || ''} ${item.configName || ''}`.toLowerCase()
  const value = String(item.configValue ?? '').trim()
  if (['true', 'false', '0', '1', 'yes', 'no', 'on', 'off', '启用', '停用'].includes(value.toLowerCase())) {
    return 'switch'
  }
  if (key.includes('scope') || key.includes('范围')) return 'scope'
  if (key.includes('content') || key.includes('template') || key.includes('内容') || value.length > 80) {
    return 'textarea'
  }
  if (/^-?\d+(\.\d+)?$/.test(value) && !key.includes('title') && !key.includes('name')) {
    return 'number'
  }
  return 'text'
}

function booleanValue(value) {
  return ['true', '1', 'yes', 'on', '启用'].includes(String(value).toLowerCase())
}

function updateBoolean(item, value) {
  item.configValue = value ? 'true' : 'false'
}

function updateNumber(item, value) {
  item.configValue = value === null || value === undefined ? '' : String(value)
}

function resetGroup(configGroup) {
  groups[configGroup] = groups[configGroup].map((item) => ({
    ...item,
    configValue: item.originalValue
  }))
}

async function saveGroup(configGroup) {
  savingGroup.value = configGroup
  try {
    await request.post('/api/admin/system-configs', {
      configGroup,
      items: groups[configGroup].map((item) => ({
        configKey: item.configKey,
        configValue: String(item.configValue ?? '')
      }))
    })
    lastSavedAt[configGroup] = new Date().toLocaleString()
    ElMessage.success(`${groupMeta[configGroup]?.label || configGroup}已保存`)
    await loadData()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    savingGroup.value = ''
  }
}

function validateBroadcast() {
  if (!broadcastForm.title.trim()) {
    ElMessage.warning('请填写广播标题')
    return false
  }
  if (!broadcastForm.content.trim()) {
    ElMessage.warning('请填写广播内容')
    return false
  }
  if (broadcastForm.title.length > 128) {
    ElMessage.warning('广播标题不能超过 128 个字符')
    return false
  }
  if (broadcastForm.content.length > 1000) {
    ElMessage.warning('广播内容不能超过 1000 个字符')
    return false
  }
  return true
}

async function sendBroadcast() {
  if (!validateBroadcast()) return
  const scopeLabel = scopeMeta[broadcastForm.scope]?.label || broadcastForm.scope
  try {
    await ElMessageBox.confirm(
      `广播将实时发送给「${scopeLabel}」范围用户，当前在线接收数约 ${broadcastSummary.value.onlineCount || 0}，确认发送？`,
      '发送确认',
      {
        type: 'warning',
        confirmButtonText: '确认发送',
        cancelButtonText: '取消'
      }
    )
    sendingBroadcast.value = true
    await request.post('/api/admin/broadcasts', {
      title: broadcastForm.title,
      content: broadcastForm.content,
      scope: broadcastForm.scope
    })
    ElMessage.success('广播已发送')
    broadcastForm.title = groups.BROADCAST.find((item) => item.configKey === 'broadcast_title')?.configValue || ''
    broadcastForm.content = groups.BROADCAST.find((item) => item.configKey === 'broadcast_content')?.configValue || ''
    broadcastSummary.value = await request.get('/api/admin/broadcasts/summary')
    await loadBroadcasts()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || '操作已取消')
  } finally {
    sendingBroadcast.value = false
  }
}

function searchBroadcasts() {
  broadcastQuery.pageNo = 1
  loadBroadcasts()
}

onMounted(loadData)
</script>

<template>
  <div class="system-page" v-loading="loading">
    <section class="system-hero">
      <div>
        <div class="eyebrow">系统控制台</div>
        <h1>系统设置</h1>
        <p>集中维护平台参数、导出策略和广播推送，配置变更保存后立即生效。</p>
      </div>
      <div class="hero-actions">
        <el-tag :type="Object.values(changedMap).some((items) => items.length) ? 'warning' : 'success'" effect="plain">
          {{ Object.values(changedMap).some((items) => items.length) ? '存在未保存变更' : '配置已同步' }}
        </el-tag>
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新数据</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <article v-for="item in systemMetrics" :key="item.label" class="metric-card" :class="`tone-${item.tone}`">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.note }}</p>
      </article>
    </section>

    <section class="workbench-grid">
      <div class="panel config-panel">
        <div class="panel-head">
          <div>
            <div class="section-title small">配置中心</div>
            <p>不同类型配置使用对应控件，减少误填；修改后可按组保存。</p>
          </div>
          <div class="head-actions">
            <el-button :disabled="!activeChangedItems.length" @click="resetGroup(activeGroup)">撤销本组</el-button>
            <el-button
              type="primary"
              :loading="savingGroup === activeGroup"
              :disabled="!activeChangedItems.length"
              @click="saveGroup(activeGroup)"
            >
              保存本组
            </el-button>
          </div>
        </div>

        <el-tabs v-model="activeGroup" class="config-tabs">
          <el-tab-pane v-for="group in ['PARAM', 'EXPORT', 'BROADCAST']" :key="group" :name="group">
            <template #label>
              <span class="tab-label">
                {{ groupMeta[group].label }}
                <em v-if="changedMap[group]?.length">{{ changedMap[group].length }}</em>
              </span>
            </template>

            <div class="group-summary">
              <div>
                <strong>{{ groupMeta[group].label }}</strong>
                <span>{{ groupMeta[group].note }}</span>
              </div>
              <el-tag :type="changedMap[group]?.length ? 'warning' : 'success'" effect="plain">
                {{ changedMap[group]?.length ? `${changedMap[group].length} 项未保存` : '已同步' }}
              </el-tag>
            </div>

            <div class="config-list">
              <article v-for="item in groups[group]" :key="item.configKey" class="config-card">
                <div class="config-copy">
                  <div class="config-title">
                    <strong>{{ item.configName }}</strong>
                    <el-tag v-if="String(item.configValue ?? '') !== String(item.originalValue ?? '')" type="warning" effect="plain">
                      未保存
                    </el-tag>
                  </div>
                  <p>{{ item.descriptionText || '暂无配置说明' }}</p>
                  <code>{{ item.configKey }}</code>
                </div>
                <div class="config-control">
                  <el-switch
                    v-if="inferControlType(item) === 'switch'"
                    :model-value="booleanValue(item.configValue)"
                    inline-prompt
                    active-text="启用"
                    inactive-text="停用"
                    @update:model-value="(value) => updateBoolean(item, value)"
                  />
                  <el-input-number
                    v-else-if="inferControlType(item) === 'number'"
                    :model-value="Number(item.configValue || 0)"
                    controls-position="right"
                    @update:model-value="(value) => updateNumber(item, value)"
                  />
                  <el-select v-else-if="inferControlType(item) === 'scope'" v-model="item.configValue">
                    <el-option label="全体" value="ALL" />
                    <el-option label="学员" value="USER" />
                    <el-option label="教师" value="TEACHER" />
                  </el-select>
                  <el-input
                    v-else-if="inferControlType(item) === 'textarea'"
                    v-model="item.configValue"
                    type="textarea"
                    :rows="3"
                    maxlength="1000"
                    show-word-limit
                  />
                  <el-input v-else v-model="item.configValue" maxlength="1000" show-word-limit />
                </div>
              </article>
              <el-empty v-if="!groups[group].length" description="暂无配置项" />
            </div>

            <div class="save-foot">
              <span>{{ lastSavedAt[group] ? `最近保存：${lastSavedAt[group]}` : '本次会话尚未保存' }}</span>
              <el-button
                type="primary"
                plain
                :loading="savingGroup === group"
                :disabled="!changedMap[group]?.length"
                @click="saveGroup(group)"
              >
                保存{{ groupMeta[group].label }}
              </el-button>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>

      <aside class="panel broadcast-send-panel">
        <div class="section-title small">全局广播</div>
        <p class="panel-note">发送后会通过 WebSocket 实时推送，请确认范围与内容。</p>
        <div class="broadcast-stat">
          <div>
            <span>在线接收数</span>
            <strong>{{ broadcastSummary.onlineCount || 0 }}</strong>
          </div>
          <div>
            <span>最近广播</span>
            <strong>{{ broadcastSummary.latestBroadcastTitle || '暂无' }}</strong>
          </div>
        </div>
        <div class="broadcast-form">
          <el-input v-model="broadcastForm.title" placeholder="广播标题" maxlength="128" show-word-limit />
          <el-input
            v-model="broadcastForm.content"
            type="textarea"
            :rows="5"
            placeholder="广播内容"
            maxlength="1000"
            show-word-limit
          />
          <el-select v-model="broadcastForm.scope" placeholder="发送范围">
            <el-option label="全体" value="ALL" />
            <el-option label="学员" value="USER" />
            <el-option label="教师" value="TEACHER" />
          </el-select>
          <el-button type="primary" :loading="sendingBroadcast" @click="sendBroadcast">发送广播</el-button>
        </div>
        <div class="risk-tip">
          <el-icon><WarningFilled /></el-icon>
          广播属于即时触达操作，发送前会进行二次确认。
        </div>
      </aside>
    </section>

    <section class="panel broadcast-record-panel">
      <div class="panel-head">
        <div>
          <div class="section-title small">广播记录</div>
          <p>按标题、内容、范围和发送时间回看历史广播。</p>
        </div>
      </div>
      <div class="toolbar">
        <el-input v-model="broadcastQuery.keyword" placeholder="搜索标题或内容" clearable @keyup.enter="searchBroadcasts" />
        <el-select v-model="broadcastFilters.scope" placeholder="范围" clearable>
          <el-option label="全体" value="ALL" />
          <el-option label="学员" value="USER" />
          <el-option label="教师" value="TEACHER" />
        </el-select>
        <el-date-picker
          v-model="broadcastFilters.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
        <el-button type="primary" @click="searchBroadcasts">查询</el-button>
      </div>
      <el-table :data="filteredBroadcastList" border class="admin-table">
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
        <el-table-column label="范围" width="100">
          <template #default="{ row }">
            <el-tag :type="scopeMeta[row.scope]?.type || 'info'" effect="plain">
              {{ scopeMeta[row.scope]?.label || row.scope }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="senderName" label="发送人" width="120" show-overflow-tooltip />
        <el-table-column prop="receiverCount" label="在线接收数" width="120" />
        <el-table-column prop="sentAt" label="发送时间" width="180" />
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.system-page {
  display: grid;
  gap: 18px;
}

.system-hero,
.panel,
.metric-card {
  border: 1px solid #dbe3f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.04);
}

.system-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 20px 22px;
}

.eyebrow {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.system-hero h1 {
  margin: 4px 0 8px;
  color: #0f172a;
  font-size: 24px;
  line-height: 1.25;
}

.system-hero p,
.panel-head p,
.panel-note,
.config-card p {
  margin: 0;
  color: #64748b;
  line-height: 1.6;
}

.hero-actions,
.head-actions,
.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.metric-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.metric-card {
  padding: 16px;
  border-left: 4px solid #2563eb;
}

.metric-card.tone-warning {
  border-left-color: #f59e0b;
}

.metric-card.tone-success {
  border-left-color: #16a34a;
}

.metric-card span {
  color: #64748b;
  font-size: 13px;
}

.metric-card strong {
  display: block;
  margin: 8px 0 4px;
  color: #0f172a;
  font-size: 28px;
  line-height: 1.1;
}

.metric-card p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workbench-grid {
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(0, 1fr) 340px;
  align-items: start;
}

.panel {
  padding: 18px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.section-title.small {
  margin: 0 0 6px;
  color: #0f172a;
  font-size: 18px;
}

.config-tabs :deep(.el-tabs__header) {
  margin-bottom: 14px;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.tab-label em {
  min-width: 18px;
  height: 18px;
  border-radius: 999px;
  display: inline-grid;
  place-items: center;
  color: #fff;
  background: #f59e0b;
  font-size: 12px;
  font-style: normal;
}

.group-summary {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.group-summary strong,
.group-summary span {
  display: block;
}

.group-summary span {
  margin-top: 4px;
  color: #64748b;
}

.config-list {
  display: grid;
  gap: 12px;
}

.config-card {
  display: grid;
  grid-template-columns: minmax(240px, 0.85fr) minmax(280px, 1fr);
  gap: 18px;
  align-items: center;
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
}

.config-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.config-copy code {
  display: inline-block;
  margin-top: 8px;
  color: #475569;
  font-size: 12px;
}

.config-control :deep(.el-input-number),
.config-control :deep(.el-select) {
  width: 100%;
}

.save-foot {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #e2e8f0;
}

.save-foot span {
  color: #64748b;
}

.broadcast-send-panel {
  position: sticky;
  top: 96px;
}

.broadcast-stat {
  display: grid;
  gap: 10px;
  margin: 14px 0;
}

.broadcast-stat div {
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.broadcast-stat span,
.risk-tip {
  color: #64748b;
}

.broadcast-stat strong {
  display: block;
  margin-top: 6px;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.broadcast-form {
  display: grid;
  gap: 12px;
}

.broadcast-form :deep(.el-select) {
  width: 100%;
}

.risk-tip {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 14px;
  padding: 10px;
  border-radius: 8px;
  background: #fffbeb;
}

.toolbar {
  margin-bottom: 14px;
}

.toolbar :deep(.el-input) {
  width: 240px;
}

.toolbar :deep(.el-select) {
  width: 140px;
}

.toolbar :deep(.el-date-editor) {
  width: 260px;
}

.admin-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #334155;
  font-weight: 700;
}

@media (max-width: 1180px) {
  .workbench-grid {
    grid-template-columns: 1fr;
  }

  .broadcast-send-panel {
    position: static;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .system-hero,
  .panel-head,
  .group-summary,
  .save-foot {
    align-items: stretch;
    flex-direction: column;
  }

  .metric-grid,
  .config-card {
    grid-template-columns: 1fr;
  }

  .toolbar :deep(.el-input),
  .toolbar :deep(.el-select),
  .toolbar :deep(.el-date-editor) {
    width: 100%;
  }
}
</style>
