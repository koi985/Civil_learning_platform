<script setup>
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'

let echartsModule = null
async function loadEcharts() {
  if (!echartsModule) {
    echartsModule = await import('echarts')
  }
  return echartsModule
}

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const batchDialogVisible = ref(false)
const selectedRows = ref([])
const payload = ref({
  overview: {},
  filters: {},
  subjectPlanStats: [],
  taskTypeStats: [],
  masteryLevelStats: [],
  testPaperTrend: [],
  weakKnowledgeTop: [],
  archivedPlans: [],
  segmentStats: [],
  focusUsers: [],
  classroomGrowthStats: [],
  teacherCollaborationStats: [],
  followHistory: [],
  reminderSummary: {},
  followReminders: []
})
const query = reactive({
  segment: '',
  minCompletionRate: 0,
  followStatus: ''
})
const followForm = reactive({
  userId: null,
  planId: null,
  latestDiagnosisId: null,
  followStatus: 'IN_PROGRESS',
  priorityLevel: 'MEDIUM',
  remark: '',
  nextFollowUpAt: ''
})
const batchFollowForm = reactive({
  followStatus: 'IN_PROGRESS',
  priorityLevel: 'MEDIUM',
  remark: '',
  nextFollowUpAt: ''
})
const subjectRef = ref(null)
const masteryRef = ref(null)
const trendRef = ref(null)
let subjectChart
let masteryChart
let trendChart

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '暂无'
}

async function loadData() {
  loading.value = true
  try {
    payload.value = await request.get('/api/admin/growth-dashboard', { params: query })
    Object.assign(query, {
      segment: payload.value.filters?.segment || '',
      minCompletionRate: Number(payload.value.filters?.minCompletionRate || 0),
      followStatus: payload.value.filters?.followStatus || ''
    })
    await nextTick()
    await renderCharts()
  } finally {
    loading.value = false
  }
}

async function exportData() {
  await request.download('/api/admin/growth-dashboard/export', query)
}

async function renderCharts() {
  const echarts = await loadEcharts()
  if (subjectRef.value) {
    subjectChart = subjectChart || echarts.init(subjectRef.value)
    subjectChart.setOption({
      tooltip: {},
      xAxis: { type: 'category', data: payload.value.subjectPlanStats.map((item) => item.subjectName) },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: payload.value.subjectPlanStats.map((item) => item.planCount), itemStyle: { color: '#1d4ed8' } }]
    })
  }
  if (masteryRef.value) {
    masteryChart = masteryChart || echarts.init(masteryRef.value)
    masteryChart.setOption({
      tooltip: {},
      series: [{
        type: 'pie',
        radius: ['45%', '70%'],
        data: payload.value.masteryLevelStats.map((item) => ({ name: item.name, value: item.value }))
      }]
    })
  }
  if (trendRef.value) {
    trendChart = trendChart || echarts.init(trendRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['完成测卷数', '平均得分'] },
      xAxis: { type: 'category', data: payload.value.testPaperTrend.map((item) => item.date) },
      yAxis: [{ type: 'value' }, { type: 'value' }],
      series: [
        { name: '完成测卷数', type: 'bar', data: payload.value.testPaperTrend.map((item) => item.count), itemStyle: { color: '#0f766e' } },
        { name: '平均得分', type: 'line', yAxisIndex: 1, data: payload.value.testPaperTrend.map((item) => Number(item.averageScore || 0).toFixed(2)), smooth: true, itemStyle: { color: '#f97316' } }
      ]
    })
  }
}

async function saveFilterPreference() {
  await request.post('/api/admin/growth-dashboard/filter-preference', query)
}

async function handleSearch() {
  await saveFilterPreference()
  await loadData()
}

function openFollowDialog(row) {
  Object.assign(followForm, {
    userId: row.userId,
    planId: row.planId,
    latestDiagnosisId: row.latestDiagnosisId,
    followStatus: row.followStatus || 'IN_PROGRESS',
    priorityLevel: row.priorityLevel || 'MEDIUM',
    remark: row.followRemark || '',
    nextFollowUpAt: row.nextFollowUpAt || ''
  })
  dialogVisible.value = true
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
}

function openBatchFollowDialog() {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择要批量跟进的用户')
    return
  }
  batchDialogVisible.value = true
}

async function saveFollow() {
  saving.value = true
  try {
    await request.post('/api/admin/growth-focus-follows', followForm)
    ElMessage.success('成长跟进已保存')
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

async function saveBatchFollow() {
  saving.value = true
  try {
    await request.post('/api/admin/growth-focus-follows/batch', {
      items: selectedRows.value.map((row) => ({
        userId: row.userId,
        planId: row.planId,
        latestDiagnosisId: row.latestDiagnosisId
      })),
      ...batchFollowForm
    })
    ElMessage.success(`已批量保存 ${selectedRows.value.length} 名用户的成长跟进`)
    batchDialogVisible.value = false
    selectedRows.value = []
    await loadData()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="growth-page" v-loading="loading">
    <section class="stat-grid">
      <div class="glass-card stat-card"><span>活跃计划</span><strong>{{ payload.overview.activePlanCount || 0 }}</strong></div>
      <div class="glass-card stat-card"><span>归档计划</span><strong>{{ payload.overview.archivedPlanCount || 0 }}</strong></div>
      <div class="glass-card stat-card"><span>今日任务达成</span><strong>{{ payload.overview.todayCompletedTaskCount || 0 }}</strong></div>
      <div class="glass-card stat-card"><span>成长激励数</span><strong>{{ payload.overview.achievementCount || 0 }}</strong></div>
      <div class="glass-card stat-card"><span>完成测卷数</span><strong>{{ payload.overview.completedTestPaperCount || 0 }}</strong></div>
      <div class="glass-card stat-card"><span>归档计划平均完成率</span><strong>{{ payload.overview.averagePlanCompletionRate || 0 }}%</strong></div>
      <div class="glass-card stat-card"><span>逾期回访</span><strong>{{ payload.reminderSummary.overdueCount || 0 }}</strong></div>
      <div class="glass-card stat-card"><span>今日回访</span><strong>{{ payload.reminderSummary.todayCount || 0 }}</strong></div>
      <div class="glass-card stat-card"><span>近 7 天回访</span><strong>{{ payload.reminderSummary.upcomingCount || 0 }}</strong></div>
    </section>

    <section class="glass-card filter-card">
      <div class="section-title">重点人群筛选</div>
      <div class="filter-row">
        <el-select v-model="query.segment" clearable placeholder="成长分层">
          <el-option label="重点预警" value="AT_RISK" />
          <el-option label="跟踪补强" value="TRACKING" />
          <el-option label="持续上升" value="RISING" />
          <el-option label="稳定冲刺" value="STABLE" />
        </el-select>
        <el-input-number v-model="query.minCompletionRate" :min="0" :max="100" style="width: 180px" />
        <el-select v-model="query.followStatus" clearable placeholder="跟进状态">
          <el-option label="待跟进" value="PENDING" />
          <el-option label="跟进中" value="IN_PROGRESS" />
          <el-option label="已跟进" value="DONE" />
        </el-select>
        <el-button type="primary" @click="handleSearch">筛选</el-button>
        <el-button type="warning" plain :disabled="!selectedRows.length" @click="openBatchFollowDialog">批量跟进</el-button>
        <el-button plain @click="exportData">导出人群</el-button>
      </div>

      <div class="segment-row">
        <el-tag v-for="item in payload.segmentStats" :key="item.segment" effect="plain">{{ item.segmentLabel }} {{ item.count }}</el-tag>
      </div>
    </section>

    <section class="chart-grid">
      <div class="glass-card chart-card">
        <div class="section-title">计划科目分布</div>
        <div ref="subjectRef" style="height: 320px"></div>
      </div>
      <div class="glass-card chart-card">
        <div class="section-title">知识掌握层级分布</div>
        <div ref="masteryRef" style="height: 320px"></div>
      </div>
      <div class="glass-card chart-card full-width">
        <div class="section-title">近 7 日测卷完成趋势</div>
        <div ref="trendRef" style="height: 320px"></div>
      </div>
    </section>

    <section class="glass-card panel-card">
      <div class="section-title">重点跟进人群</div>
      <el-table :data="payload.focusUsers" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="planName" label="当前计划" min-width="200" />
        <el-table-column prop="segmentLabel" label="成长分层" min-width="120" />
        <el-table-column prop="completionRate" label="完成率" min-width="100" />
        <el-table-column prop="vitalityScore" label="活力分" min-width="100" />
        <el-table-column prop="followStatusLabel" label="跟进状态" min-width="120" />
        <el-table-column prop="priorityLevelLabel" label="优先级" min-width="120" />
        <el-table-column label="薄弱知识点" min-width="220">
          <template #default="{ row }">{{ (row.weakKnowledgePoints || []).join(' / ') || '暂无' }}</template>
        </el-table-column>
        <el-table-column prop="followRemark" label="跟进备注" min-width="220" />
        <el-table-column prop="nextFollowUpAt" label="下次回访" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.nextFollowUpAt) }}</template>
        </el-table-column>
        <el-table-column prop="reminderStatusLabel" label="提醒状态" min-width="120" />
        <el-table-column prop="followUpdatedAt" label="最近更新" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.followUpdatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="140">
          <template #default="{ row }">
            <el-button type="primary" plain @click="openFollowDialog(row)">跟进</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="glass-card panel-card">
      <div class="section-title">近期回访提醒</div>
      <el-table :data="payload.followReminders">
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="planName" label="当前计划" min-width="200" />
        <el-table-column prop="segmentLabel" label="成长分层" min-width="120" />
        <el-table-column prop="followStatusLabel" label="跟进状态" min-width="120" />
        <el-table-column prop="priorityLevelLabel" label="优先级" min-width="120" />
        <el-table-column prop="nextFollowUpAt" label="下次回访" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.nextFollowUpAt) }}</template>
        </el-table-column>
        <el-table-column prop="reminderStatusLabel" label="提醒状态" min-width="120" />
        <el-table-column prop="remark" label="备注" min-width="260" />
      </el-table>
    </section>

    <section class="double-grid">
      <div class="glass-card panel-card">
        <div class="section-title">班级成长分布</div>
        <el-table :data="payload.classroomGrowthStats">
          <el-table-column prop="classroomName" label="班级" min-width="160" />
          <el-table-column prop="teacherName" label="教师" min-width="100" />
          <el-table-column prop="subjectName" label="科目" min-width="100" />
          <el-table-column prop="activePlanStudentCount" label="活跃计划学员" min-width="120" />
          <el-table-column prop="focusStudentCount" label="重点学员" min-width="100" />
          <el-table-column prop="atRiskCount" label="重点预警" min-width="100" />
          <el-table-column prop="averageCompletionRate" label="平均完成率" min-width="120" />
          <el-table-column prop="averageVitalityScore" label="平均活力" min-width="120" />
        </el-table>
      </div>

      <div class="glass-card panel-card">
        <div class="section-title">任务完成类型</div>
        <el-table :data="payload.taskTypeStats">
          <el-table-column prop="taskTypeLabel" label="任务类型" />
          <el-table-column prop="totalCount" label="任务总数" />
          <el-table-column prop="completedCount" label="已完成数" />
        </el-table>
      </div>

      <div class="glass-card panel-card">
        <div class="section-title">重点薄弱知识点</div>
        <el-table :data="payload.weakKnowledgeTop">
          <el-table-column prop="subjectName" label="科目" />
          <el-table-column prop="knowledgePoint" label="知识点" min-width="180" />
          <el-table-column prop="pendingWrongCount" label="待消化错题" />
          <el-table-column prop="masteryScore" label="掌握度" />
          <el-table-column prop="masteryLevelLabel" label="层级" />
        </el-table>
      </div>
    </section>

    <section class="double-grid">
      <div class="glass-card panel-card">
        <div class="section-title">教师协同概览</div>
        <el-table :data="payload.teacherCollaborationStats">
          <el-table-column prop="classroomName" label="班级" min-width="160" />
          <el-table-column prop="teacherName" label="教师" min-width="100" />
          <el-table-column prop="atRiskCount" label="预警人数" min-width="100" />
          <el-table-column prop="pendingCount" label="待跟进" min-width="100" />
          <el-table-column prop="inProgressCount" label="跟进中" min-width="100" />
          <el-table-column prop="doneCount" label="已跟进" min-width="100" />
        </el-table>
      </div>

      <div class="glass-card panel-card">
        <div class="section-title">最近运营跟进轨迹</div>
        <el-table :data="payload.followHistory">
          <el-table-column prop="username" label="用户名" min-width="120" />
          <el-table-column prop="followStatusLabel" label="状态" min-width="100" />
          <el-table-column prop="priorityLevelLabel" label="优先级" min-width="100" />
          <el-table-column prop="remark" label="备注" min-width="220" />
          <el-table-column prop="operatorName" label="操作人" min-width="100" />
          <el-table-column prop="createdAt" label="记录时间" min-width="180">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <section class="glass-card panel-card">
      <div class="section-title">最近归档计划</div>
      <div class="history-list" v-if="payload.archivedPlans?.length">
        <article v-for="item in payload.archivedPlans" :key="`${item.username}-${item.planName}-${item.archivedAt}`" class="history-item">
          <div>
            <strong>{{ item.planName }}</strong>
            <p>{{ item.username }} · {{ item.subjectName }}</p>
          </div>
          <div class="history-side">
            <span>{{ item.completedTaskCount || 0 }}/{{ item.totalTaskCount || 0 }} 任务</span>
            <span>{{ item.completionRate || 0 }}%</span>
            <small>{{ formatDateTime(item.archivedAt) }}</small>
          </div>
        </article>
      </div>
      <el-empty v-else description="当前还没有归档计划数据" />
    </section>

    <el-dialog v-model="dialogVisible" title="保存成长跟进" width="520px">
      <el-form label-position="top">
        <el-form-item label="跟进状态">
          <el-select v-model="followForm.followStatus">
            <el-option label="待跟进" value="PENDING" />
            <el-option label="跟进中" value="IN_PROGRESS" />
            <el-option label="已跟进" value="DONE" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="followForm.priorityLevel">
            <el-option label="高优先级" value="HIGH" />
            <el-option label="中优先级" value="MEDIUM" />
            <el-option label="低优先级" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="跟进备注">
          <el-input v-model="followForm.remark" type="textarea" :rows="4" placeholder="记录本轮跟进建议、复查时间和重点薄弱项" />
        </el-form-item>
        <el-form-item label="下次回访时间">
          <el-date-picker v-model="followForm.nextFollowUpAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveFollow">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchDialogVisible" title="批量保存成长跟进" width="520px">
      <el-alert type="info" :closable="false" show-icon :title="`已选择 ${selectedRows.length} 名重点用户，将统一写入本次运营跟进策略。`" />
      <el-form label-position="top" style="margin-top: 16px">
        <el-form-item label="跟进状态">
          <el-select v-model="batchFollowForm.followStatus">
            <el-option label="待跟进" value="PENDING" />
            <el-option label="跟进中" value="IN_PROGRESS" />
            <el-option label="已跟进" value="DONE" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="batchFollowForm.priorityLevel">
            <el-option label="高优先级" value="HIGH" />
            <el-option label="中优先级" value="MEDIUM" />
            <el-option label="低优先级" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item label="统一备注">
          <el-input v-model="batchFollowForm.remark" type="textarea" :rows="4" placeholder="记录这一批用户的跟进动作、回访节点与风险说明" />
        </el-form-item>
        <el-form-item label="统一下次回访时间">
          <el-date-picker v-model="batchFollowForm.nextFollowUpAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveBatchFollow">批量保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.growth-page {
  display: grid;
  gap: 20px;
  min-width: 0;
}

.stat-grid,
.chart-grid,
.double-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  min-width: 0;
}

.stat-card,
.chart-card,
.panel-card,
.filter-card {
  padding: 24px;
  min-width: 0;
  max-width: 100%;
}

.panel-card,
.chart-card {
  overflow-x: auto;
}

.stat-card span,
.history-item p,
.history-side {
  color: var(--muted);
}

.stat-card strong {
  display: block;
  margin-top: 10px;
  font-size: 28px;
  color: var(--primary);
}

.filter-row,
.segment-row {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 16px;
  min-width: 0;
}

.full-width {
  grid-column: 1 / -1;
}

.history-list {
  display: grid;
  gap: 12px;
}

.history-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 16px;
  background: #fff;
}

.history-side {
  display: grid;
  gap: 6px;
  justify-items: end;
}

@media (max-width: 640px) {
  .growth-page {
    gap: 14px;
  }

  .stat-grid,
  .chart-grid,
  .double-grid {
    grid-template-columns: 1fr;
  }

  .stat-card,
  .chart-card,
  .panel-card,
  .filter-card {
    padding: 18px;
  }

  .filter-row :deep(.el-select),
  .filter-row :deep(.el-input-number),
  .filter-row :deep(.el-button) {
    width: 100% !important;
  }

  .history-item {
    align-items: flex-start;
    flex-direction: column;
  }

  .history-side {
    justify-items: start;
  }
}
</style>
