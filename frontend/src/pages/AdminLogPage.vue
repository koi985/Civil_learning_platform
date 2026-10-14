<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'

const loading = ref(false)
const summaryLoading = ref(false)
const detailVisible = ref(false)
const currentLog = ref(null)
const list = ref([])
const total = ref(0)
const selectedRows = ref([])
const dateRange = ref([])
const summary = ref({
  totalCount: 0,
  todayCount: 0,
  failureCount: 0,
  sensitiveCount: 0,
  activeOperatorCount: 0,
  modules: []
})
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  moduleName: '',
  success: '',
  keyword: '',
  startTime: '',
  endTime: '',
  sensitiveOnly: false
})

const metricCards = computed(() => [
  { label: '筛选日志', value: summary.value.totalCount || 0, hint: '当前条件命中' },
  { label: '今日日志', value: summary.value.todayCount || 0, hint: '今日产生操作' },
  { label: '失败操作', value: summary.value.failureCount || 0, hint: '需优先排查', danger: summary.value.failureCount > 0 },
  { label: '敏感操作', value: summary.value.sensitiveCount || 0, hint: '删除/退款/权限/配置' },
  { label: '活跃管理员', value: summary.value.activeOperatorCount || 0, hint: '涉及操作人' }
])

function syncDateRange() {
  query.startTime = dateRange.value?.[0] || ''
  query.endTime = dateRange.value?.[1] || ''
}

function buildParams() {
  syncDateRange()
  return {
    pageNo: query.pageNo,
    pageSize: query.pageSize,
    moduleName: query.moduleName,
    success: query.success,
    keyword: query.keyword,
    startTime: query.startTime,
    endTime: query.endTime,
    sensitiveOnly: query.sensitiveOnly || ''
  }
}

async function loadSummary() {
  summaryLoading.value = true
  try {
    summary.value = await request.get('/api/admin/operation-logs/summary', { params: buildParams() })
  } finally {
    summaryLoading.value = false
  }
}

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/admin/operation-logs', { params: buildParams() })
    list.value = data.records
    total.value = data.total
    await loadSummary()
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNo = 1
  loadData()
}

function resetQuery() {
  Object.assign(query, {
    pageNo: 1,
    pageSize: 10,
    moduleName: '',
    success: '',
    keyword: '',
    startTime: '',
    endTime: '',
    sensitiveOnly: false
  })
  dateRange.value = []
  loadData()
}

function applyQuickFilter(type) {
  const now = new Date()
  if (type === 'todayFail') {
    dateRange.value = [`${formatDate(now)} 00:00:00`, `${formatDate(now)} 23:59:59`]
    query.success = 'false'
    query.sensitiveOnly = false
  } else if (type === 'week') {
    const start = new Date(now)
    start.setDate(start.getDate() - 6)
    dateRange.value = [`${formatDate(start)} 00:00:00`, `${formatDate(now)} 23:59:59`]
    query.success = ''
    query.sensitiveOnly = false
  } else if (type === 'sensitive') {
    query.sensitiveOnly = true
  }
  search()
}

function formatDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

async function exportLogs() {
  try {
    await request.download('/api/admin/operation-logs/export', buildParams())
    ElMessage.success('日志导出成功')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function openDetail(row) {
  currentLog.value = row
  detailVisible.value = true
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
}

async function copyText(text, label = '内容') {
  if (!text) {
    ElMessage.warning(`${label}为空`)
    return
  }
  try {
    await navigator.clipboard.writeText(String(text))
    ElMessage.success(`${label}已复制`)
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

function copySelectedIds() {
  copyText(selectedRows.value.map((item) => item.id).join(','), '已选日志 ID')
}

function resultType(row) {
  return row.success ? 'success' : 'danger'
}

function riskType(row) {
  if (!row.success) return 'danger'
  if (row.sensitive) return 'warning'
  return 'info'
}

function riskLabel(row) {
  if (!row.success) return '异常'
  if (row.sensitive) return '敏感'
  return '普通'
}

onMounted(loadData)
</script>

<template>
  <div class="admin-page">
    <section class="metric-grid" v-loading="summaryLoading">
      <div v-for="item in metricCards" :key="item.label" class="metric-card" :class="{ danger: item.danger }">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.hint }}</p>
      </div>
    </section>

    <section class="filter-card glass-card">
      <div class="quick-row">
        <el-button plain @click="applyQuickFilter('todayFail')">今日失败</el-button>
        <el-button plain @click="applyQuickFilter('week')">近 7 天</el-button>
        <el-button plain @click="applyQuickFilter('sensitive')">只看敏感操作</el-button>
      </div>
      <div class="filter-grid">
        <el-input v-model="query.keyword" placeholder="搜索操作、地址、操作人或模块" clearable />
        <el-select v-model="query.moduleName" clearable filterable placeholder="模块">
          <el-option v-for="item in summary.modules" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="query.success" clearable placeholder="结果">
          <el-option label="成功" value="true" />
          <el-option label="失败" value="false" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          range-separator="至"
          style="width: 100%"
        />
        <el-checkbox v-model="query.sensitiveOnly">仅敏感操作</el-checkbox>
        <div class="filter-actions">
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button plain @click="exportLogs">导出当前筛选</el-button>
        </div>
      </div>
    </section>

    <section class="glass-card table-card" v-loading="loading">
      <div class="table-head">
        <div>
          <h2>日志明细</h2>
          <p>优先查看失败和敏感操作，完整请求与响应放在详情抽屉中。</p>
        </div>
        <el-button :disabled="!selectedRows.length" plain @click="copySelectedIds">复制已选 ID</el-button>
      </div>
      <el-table :data="list" empty-text="当前筛选条件下暂无系统日志" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="46" />
        <el-table-column prop="createdAt" label="时间" min-width="170" />
        <el-table-column prop="operatorName" label="操作人" width="130">
          <template #default="{ row }">
            <div class="operator-cell">
              <strong>{{ row.operatorName || '-' }}</strong>
              <span>{{ row.operatorRole || '未知角色' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="moduleName" label="模块" width="130" />
        <el-table-column prop="operationName" label="操作名称" min-width="170" show-overflow-tooltip />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="resultType(row)">{{ row.success ? '成功' : '失败' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="风险" width="90">
          <template #default="{ row }">
            <el-tag :type="riskType(row)" effect="plain">{{ riskLabel(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestMethod" label="方法" width="90" />
        <el-table-column prop="requestUri" label="请求地址" min-width="240" show-overflow-tooltip />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看详情</el-button>
            <el-button link @click="copyText(row.id, '日志 ID')">复制 ID</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :page-sizes="[10, 20, 50]"
        :current-page="query.pageNo"
        style="margin-top: 20px; justify-content: center"
        @size-change="(size) => { query.pageSize = size; query.pageNo = 1; loadData() }"
        @current-change="(page) => { query.pageNo = page; loadData() }"
      />
    </section>

    <el-drawer v-model="detailVisible" title="日志详情" size="560px">
      <div v-if="currentLog" class="log-detail">
        <div class="detail-summary">
          <el-tag :type="resultType(currentLog)">{{ currentLog.success ? '成功' : '失败' }}</el-tag>
          <el-tag :type="riskType(currentLog)" effect="plain">{{ riskLabel(currentLog) }}</el-tag>
          <strong>{{ currentLog.operationName }}</strong>
        </div>
        <div class="detail-grid">
          <div><span>日志 ID</span><strong>{{ currentLog.id }}</strong></div>
          <div><span>模块</span><strong>{{ currentLog.moduleName || '-' }}</strong></div>
          <div><span>操作人</span><strong>{{ currentLog.operatorName || '-' }}</strong></div>
          <div><span>角色</span><strong>{{ currentLog.operatorRole || '-' }}</strong></div>
          <div><span>请求方法</span><strong>{{ currentLog.requestMethod || '-' }}</strong></div>
          <div><span>发生时间</span><strong>{{ currentLog.createdAt || '-' }}</strong></div>
          <div><span>IP</span><strong>-</strong></div>
          <div><span>耗时</span><strong>-</strong></div>
          <div class="wide"><span>请求地址</span><strong>{{ currentLog.requestUri || '-' }}</strong></div>
          <div class="wide"><span>错误信息</span><strong>{{ currentLog.errorMessage || '无' }}</strong></div>
        </div>
        <div class="copy-row">
          <el-button size="small" @click="copyText(currentLog.id, '日志 ID')">复制日志 ID</el-button>
          <el-button size="small" @click="copyText(currentLog.requestUri, '请求地址')">复制地址</el-button>
        </div>
        <div class="payload-block">
          <div class="payload-title">请求参数</div>
          <el-input :model-value="currentLog.requestBody || '无'" type="textarea" :rows="8" readonly />
        </div>
        <div class="payload-block">
          <div class="payload-title">响应结果</div>
          <el-input :model-value="currentLog.responseBody || '无'" type="textarea" :rows="8" readonly />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.admin-page {
  display: grid;
  gap: 18px;
  min-width: 0;
  overflow-x: hidden;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
  gap: 14px;
  min-width: 0;
}

.metric-card {
  min-width: 0;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}

.metric-card span,
.metric-card p,
.table-head p,
.operator-cell span,
.detail-grid span,
.payload-title {
  color: #64748b;
}

.metric-card strong {
  display: block;
  margin: 8px 0 4px;
  color: #0f172a;
  font-size: 28px;
  line-height: 1;
}

.metric-card p {
  margin: 0;
  font-size: 13px;
}

.metric-card.danger {
  border-color: #fecaca;
  background: #fff7f7;
}

.filter-card,
.table-card {
  min-width: 0;
  padding: 20px;
}

.table-card {
  overflow-x: auto;
}

.quick-row {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.filter-grid {
  display: grid;
  grid-template-columns: minmax(180px, 1.4fr) minmax(130px, 180px) minmax(110px, 140px) minmax(240px, 360px) minmax(100px, 120px) auto;
  gap: 12px;
  align-items: center;
}

.filter-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.table-card :deep(.el-table) {
  display: block;
  max-width: 100%;
  overflow-x: auto;
}

.table-card :deep(.el-table__inner-wrapper) {
  width: max-content;
  min-width: 100%;
}

.table-head h2 {
  margin: 0;
  font-size: 18px;
}

.table-head p {
  margin: 4px 0 0;
  font-size: 13px;
}

.operator-cell {
  display: grid;
  gap: 2px;
}

.operator-cell strong {
  color: #0f172a;
}

.operator-cell span {
  font-size: 12px;
}

.log-detail {
  display: grid;
  gap: 16px;
}

.detail-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-grid div {
  display: grid;
  gap: 6px;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.detail-grid .wide {
  grid-column: 1 / -1;
}

.detail-grid strong {
  min-width: 0;
  overflow-wrap: anywhere;
  color: #0f172a;
  font-size: 13px;
}

.copy-row {
  display: flex;
  gap: 10px;
}

.payload-block {
  display: grid;
  gap: 8px;
}

.payload-title {
  font-size: 13px;
  font-weight: 700;
}

@media (max-width: 1200px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .metric-grid,
  .filter-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .quick-row,
  .filter-actions,
  .table-head {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
