<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import {
  Bell,
  Collection,
  DataAnalysis,
  EditPen,
  Finished,
  Refresh,
  Search,
  TrendCharts,
  UserFilled,
  WarningFilled
} from '@element-plus/icons-vue'
import request from '../utils/request'

let echartsModule = null
async function loadEcharts() {
  if (!echartsModule) {
    echartsModule = await import('echarts')
  }
  return echartsModule
}

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const batchDialogVisible = ref(false)
const selectedRows = ref([])
const report = ref({ classrooms: [], summary: {}, classroomStats: [], homeworkStats: [], examStats: [], scoreDistribution: [] })
const growth = ref({ classroomOptions: [], summary: {}, segmentStats: [], trend: [], focusStudents: [], classroomBoards: [], followHistory: [], reminderSummary: {}, followReminders: [], filters: {} })
const query = reactive({
  classroomId: null,
  segment: '',
  followStatus: ''
})
const dashboardFilters = reactive({
  timeRange: 'LAST_30_DAYS',
  taskType: 'ALL',
  subjectName: ''
})
const followForm = reactive({
  classroomId: null,
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
const scoreChartRef = ref(null)
const growthChartRef = ref(null)
const distributionChartRef = ref(null)
let scoreChart
let growthChart
let distributionChart

const timeRangeOptions = [
  { label: '近 7 天', value: 'LAST_7_DAYS' },
  { label: '近 30 天', value: 'LAST_30_DAYS' },
  { label: '本学期', value: 'SEMESTER' },
  { label: '全部时间', value: 'ALL' }
]

const taskTypeOptions = [
  { label: '作业 + 考试', value: 'ALL' },
  { label: '仅作业', value: 'HOMEWORK' },
  { label: '仅考试', value: 'EXAM' }
]

const subjectOptions = computed(() => {
  const subjects = new Set()
  growth.value.classroomOptions.forEach((item) => {
    if (item.subjectName) {
      subjects.add(item.subjectName)
    }
  })
  report.value.classroomStats.forEach((item) => {
    if (item.subjectName) {
      subjects.add(item.subjectName)
    }
  })
  return Array.from(subjects)
})

const filteredClassroomStats = computed(() => {
  if (!dashboardFilters.subjectName) {
    return report.value.classroomStats
  }
  return report.value.classroomStats.filter((item) => item.subjectName === dashboardFilters.subjectName)
})

const selectedClassroomLabel = computed(() => {
  const selected = growth.value.classroomOptions.find((item) => item.id === query.classroomId)
  return selected?.name || '全部班级'
})

const timeRangeLabel = computed(() => timeRangeOptions.find((item) => item.value === dashboardFilters.timeRange)?.label || '近 30 天')
const taskTypeLabel = computed(() => taskTypeOptions.find((item) => item.value === dashboardFilters.taskType)?.label || '作业 + 考试')

const kpiCards = computed(() => [
  {
    label: '教学规模',
    value: `${report.value.summary.classroomCount || 0} 班`,
    desc: `覆盖 ${report.value.summary.memberCount || 0} 名学员`,
    group: '规模',
    tone: 'neutral',
    icon: Collection
  },
  {
    label: '重点学员',
    value: growth.value.summary.focusStudentCount || 0,
    desc: '需持续观察学习状态',
    group: '风险',
    tone: 'warning',
    icon: UserFilled,
    filter: { segment: '', followStatus: '' }
  },
  {
    label: '重点预警',
    value: growth.value.summary.atRiskCount || 0,
    desc: '建议优先查看低完成率学生',
    group: '风险',
    tone: 'danger',
    icon: WarningFilled,
    filter: { segment: 'AT_RISK', followStatus: '' }
  },
  {
    label: '待教师跟进',
    value: growth.value.summary.teacherFollowPendingCount || 0,
    desc: '点击筛出待跟进名单',
    group: '待办',
    tone: 'warning',
    icon: Bell,
    filter: { segment: '', followStatus: 'PENDING' }
  },
  {
    label: '平均完成率',
    value: `${formatNumber(growth.value.summary.averageCompletionRate, 2)}%`,
    desc: completionDesc.value,
    group: '效果',
    tone: Number(growth.value.summary.averageCompletionRate || 0) < 60 ? 'danger' : 'success',
    icon: Finished
  },
  {
    label: '逾期回访',
    value: growth.value.reminderSummary.overdueCount || 0,
    desc: `今日 ${growth.value.reminderSummary.todayCount || 0} 个，近 7 天 ${growth.value.reminderSummary.upcomingCount || 0} 个`,
    group: '待办',
    tone: growth.value.reminderSummary.overdueCount ? 'danger' : 'neutral',
    icon: EditPen
  }
])

const completionDesc = computed(() => {
  const rate = Number(growth.value.summary.averageCompletionRate || 0)
  if (rate < 60) return '低于目标，建议集中补强'
  if (rate < 80) return '处于追踪区间，关注波动'
  return '完成表现稳定'
})

const actionHints = computed(() => {
  const hints = []
  const atRiskCount = Number(growth.value.summary.atRiskCount || 0)
  const pendingCount = Number(growth.value.summary.teacherFollowPendingCount || 0)
  const overdueCount = Number(growth.value.reminderSummary.overdueCount || 0)
  const completionRate = Number(growth.value.summary.averageCompletionRate || 0)
  const pendingReviewCount = Number(report.value.summary.pendingReviewCount || 0)

  if (atRiskCount > 0) {
    hints.push({
      type: 'AT_RISK',
      tone: 'danger',
      title: `${atRiskCount} 名学生处于重点预警`,
      desc: '优先查看完成率、成长活力和薄弱知识点，安排下一次跟进。',
      action: '查看预警'
    })
  }
  if (pendingCount > 0) {
    hints.push({
      type: 'PENDING',
      tone: 'warning',
      title: `${pendingCount} 名学生待教师跟进`,
      desc: '筛出未处理名单，批量记录跟进状态或下次回访时间。',
      action: '处理跟进'
    })
  }
  if (overdueCount > 0) {
    hints.push({
      type: 'REMINDER',
      tone: 'danger',
      title: `${overdueCount} 个回访已逾期`,
      desc: '先处理逾期回访，再查看今日和近 7 天提醒。',
      action: '查看提醒'
    })
  }
  if (completionRate > 0 && completionRate < 60) {
    hints.push({
      type: 'LOW_COMPLETION',
      tone: 'warning',
      title: '平均完成率低于 60%',
      desc: '建议按完成率排序，找出作业和计划推进慢的班级。',
      action: '定位班级'
    })
  }
  if (pendingReviewCount > 0) {
    hints.push({
      type: 'REVIEW',
      tone: 'info',
      title: `${pendingReviewCount} 个提交等待批改`,
      desc: '及时批改可以让成绩趋势和正确率统计更准确。',
      action: '去批改'
    })
  }
  if (!hints.length) {
    hints.push({
      type: 'STABLE',
      tone: 'success',
      title: '暂无高优先级异常',
      desc: '可以继续查看班级统计，维护常规回访节奏。',
      action: '查看统计'
    })
  }
  return hints.slice(0, 4)
})

const priorityFocusStudents = computed(() => {
  const segmentOrder = { AT_RISK: 0, TRACKING: 1, RISING: 2, STABLE: 3 }
  const followOrder = { PENDING: 0, IN_PROGRESS: 1, DONE: 2 }
  return [...growth.value.focusStudents]
    .sort((left, right) => {
      const segmentCompare = (segmentOrder[left.segment] ?? 9) - (segmentOrder[right.segment] ?? 9)
      if (segmentCompare !== 0) return segmentCompare
      return (followOrder[left.teacherFollowStatus] ?? 9) - (followOrder[right.teacherFollowStatus] ?? 9)
    })
    .slice(0, 5)
})

const hasScoreData = computed(() => {
  const showHomework = dashboardFilters.taskType !== 'EXAM'
  const showExam = dashboardFilters.taskType !== 'HOMEWORK'
  return (showHomework && report.value.homeworkStats.some((item) => Number(item.submissionCount || item.avgScore || 0) > 0))
    || (showExam && report.value.examStats.some((item) => Number(item.submissionCount || item.avgScore || 0) > 0))
})

const hasDistributionData = computed(() => report.value.scoreDistribution.some((item) => Number(item.totalCount || 0) > 0))
const hasGrowthData = computed(() => growth.value.trend.some((item) => Number(item.studentCount || item.averageVitality || 0) > 0))

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '暂无'
}

function formatNumber(value, digits = 1) {
  const number = Number(value || 0)
  return Number.isFinite(number) ? number.toFixed(digits) : Number(0).toFixed(digits)
}

function toPercentNumber(value) {
  const number = Number(value || 0)
  if (!Number.isFinite(number)) return 0
  return Math.max(0, Math.min(100, Number(number.toFixed(2))))
}

function progressStatus(value) {
  const number = Number(value || 0)
  if (number < 60) return 'exception'
  if (number < 80) return 'warning'
  return 'success'
}

function countTagType(value) {
  return Number(value || 0) > 0 ? 'warning' : 'info'
}

function followTagType(status) {
  if (status === 'DONE') return 'success'
  if (status === 'IN_PROGRESS') return 'warning'
  return 'danger'
}

function buildSeriesData(source, labels) {
  const map = new Map(source.map((item) => [item.title, Number(item.avgScore || 0)]))
  return labels.map((label) => map.get(label) || 0)
}

async function loadData() {
  loading.value = true
  try {
    const filterParams = {
      ...query,
      timeRange: dashboardFilters.timeRange,
      subjectName: dashboardFilters.subjectName,
      taskType: dashboardFilters.taskType
    }
    const [reportData, growthData] = await Promise.all([
      request.get('/api/teacher/reports', { params: filterParams }),
      request.get('/api/teacher/growth-dashboard', { params: filterParams })
    ])
    report.value = reportData
    growth.value = growthData
    Object.assign(query, {
      classroomId: growth.value.filters?.classroomId ?? null,
      segment: growth.value.filters?.segment || '',
      followStatus: growth.value.filters?.followStatus || ''
    })
    Object.assign(dashboardFilters, {
      timeRange: growth.value.filters?.timeRange || report.value.filters?.timeRange || dashboardFilters.timeRange,
      subjectName: growth.value.filters?.subjectName || report.value.filters?.subjectName || '',
      taskType: growth.value.filters?.taskType || report.value.filters?.taskType || dashboardFilters.taskType
    })
    await nextTick()
    await renderCharts()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function saveFilterPreference() {
  await request.post('/api/teacher/growth-dashboard/filter-preference', query)
}

async function handleSearch() {
  await saveFilterPreference()
  await loadData()
}

function resetFilters() {
  Object.assign(query, { classroomId: null, segment: '', followStatus: '' })
  Object.assign(dashboardFilters, { timeRange: 'LAST_30_DAYS', taskType: 'ALL', subjectName: '' })
  handleSearch()
}

async function renderCharts() {
  const echarts = await loadEcharts()
  const showHomework = dashboardFilters.taskType !== 'EXAM'
  const showExam = dashboardFilters.taskType !== 'HOMEWORK'

  if (scoreChartRef.value) {
    scoreChart = scoreChart || echarts.init(scoreChartRef.value)
    const labels = Array.from(new Set([
      ...(showHomework ? report.value.homeworkStats.map((item) => item.title) : []),
      ...(showExam ? report.value.examStats.map((item) => item.title) : [])
    ])).slice(0, 6)
    const series = []
    if (showHomework) {
      series.push({
        name: '作业均分',
        type: 'bar',
        barMaxWidth: 42,
        data: buildSeriesData(report.value.homeworkStats, labels),
        itemStyle: { color: '#2563eb', borderRadius: [6, 6, 0, 0] }
      })
    }
    if (showExam) {
      series.push({
        name: '考试均分',
        type: 'line',
        data: buildSeriesData(report.value.examStats, labels),
        smooth: true,
        symbolSize: 8,
        lineStyle: { width: 3, color: '#f97316' },
        itemStyle: { color: '#f97316' }
      })
    }
    scoreChart.setOption({
      color: ['#2563eb', '#f97316'],
      tooltip: { trigger: 'axis' },
      legend: { top: 0, right: 8, itemWidth: 18, itemHeight: 10 },
      grid: { left: 42, right: 22, top: 46, bottom: 38 },
      xAxis: { type: 'category', data: labels, axisTick: { alignWithLabel: true }, axisLabel: { color: '#64748b' } },
      yAxis: { type: 'value', axisLabel: { color: '#64748b' }, splitLine: { lineStyle: { color: '#e5edf7' } } },
      series
    }, true)
  }
  if (growthChartRef.value) {
    growthChart = growthChart || echarts.init(growthChartRef.value)
    growthChart.setOption({
      color: ['#0f766e', '#dc2626'],
      tooltip: { trigger: 'axis' },
      legend: { top: 0, right: 8 },
      grid: { left: 42, right: 42, top: 46, bottom: 38 },
      xAxis: { type: 'category', data: growth.value.trend.map((item) => item.date), axisLabel: { color: '#64748b' } },
      yAxis: [
        { type: 'value', axisLabel: { color: '#64748b' }, splitLine: { lineStyle: { color: '#e5edf7' } } },
        { type: 'value', max: 100, axisLabel: { color: '#64748b' }, splitLine: { show: false } }
      ],
      series: [
        { name: '诊断人数', type: 'bar', barMaxWidth: 34, data: growth.value.trend.map((item) => item.studentCount), itemStyle: { color: '#0f766e', borderRadius: [6, 6, 0, 0] } },
        { name: '平均成长活力', type: 'line', yAxisIndex: 1, data: growth.value.trend.map((item) => Number(item.averageVitality || 0).toFixed(2)), smooth: true, symbolSize: 8, lineStyle: { width: 3, color: '#dc2626' }, itemStyle: { color: '#dc2626' } }
      ]
    }, true)
  }
  if (distributionChartRef.value) {
    distributionChart = distributionChart || echarts.init(distributionChartRef.value)
    const distributionSeries = []
    if (showHomework) {
      distributionSeries.push({
        name: '作业',
        type: 'bar',
        barMaxWidth: 42,
        data: report.value.scoreDistribution.map((item) => item.homeworkCount),
        itemStyle: { color: '#2563eb', borderRadius: [6, 6, 0, 0] }
      })
    }
    if (showExam) {
      distributionSeries.push({
        name: '考试',
        type: 'bar',
        barMaxWidth: 42,
        data: report.value.scoreDistribution.map((item) => item.examCount),
        itemStyle: { color: '#f97316', borderRadius: [6, 6, 0, 0] }
      })
    }
    distributionChart.setOption({
      color: ['#2563eb', '#f97316'],
      tooltip: { trigger: 'axis' },
      legend: { top: 0, right: 8 },
      grid: { left: 42, right: 22, top: 46, bottom: 38 },
      xAxis: { type: 'category', data: report.value.scoreDistribution.map((item) => item.label), axisLabel: { color: '#64748b' } },
      yAxis: { type: 'value', axisLabel: { color: '#64748b' }, splitLine: { lineStyle: { color: '#e5edf7' } } },
      series: distributionSeries
    }, true)
  }
}

function resizeCharts() {
  scoreChart?.resize()
  growthChart?.resize()
  distributionChart?.resize()
}

function applyKpiFilter(card) {
  if (!card.filter) return
  Object.assign(query, card.filter)
  handleSearch()
}

async function handleAction(type) {
  if (type === 'AT_RISK') {
    Object.assign(query, { segment: 'AT_RISK', followStatus: '' })
    await handleSearch()
    return
  }
  if (type === 'PENDING') {
    Object.assign(query, { segment: '', followStatus: 'PENDING' })
    await handleSearch()
    return
  }
  if (type === 'REMINDER') {
    document.querySelector('#follow-reminders')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    return
  }
  if (type === 'LOW_COMPLETION' || type === 'STABLE') {
    document.querySelector('#classroom-stats')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
    return
  }
  if (type === 'REVIEW') {
    router.push('/teacher/review')
  }
}

function openFollowDialog(row) {
  Object.assign(followForm, {
    classroomId: row.classroomId,
    userId: row.userId,
    planId: row.planId,
    latestDiagnosisId: row.latestDiagnosisId,
    followStatus: row.teacherFollowStatus || 'IN_PROGRESS',
    priorityLevel: row.teacherPriorityLevel || 'MEDIUM',
    remark: row.teacherFollowRemark || '',
    nextFollowUpAt: row.nextFollowUpAt || ''
  })
  dialogVisible.value = true
}

function openReminderDraft(row) {
  openFollowDialog(row)
  if (!followForm.remark) {
    followForm.remark = `提醒 ${row.studentName} 完成当前计划，并复查薄弱知识点。`
  }
}

async function saveFollow() {
  saving.value = true
  try {
    await request.post('/api/teacher/growth-focus-follows', followForm)
    ElMessage.success('学员成长跟进已保存')
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

async function exportData() {
  try {
    await request.download('/api/teacher/growth-dashboard/export', {
      ...query,
      timeRange: dashboardFilters.timeRange,
      subjectName: dashboardFilters.subjectName,
      taskType: dashboardFilters.taskType
    })
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function exportReportData() {
  try {
    await request.download('/api/teacher/reports/export', {
      classroomId: query.classroomId,
      timeRange: dashboardFilters.timeRange,
      subjectName: dashboardFilters.subjectName,
      taskType: dashboardFilters.taskType
    })
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
}

function openBatchFollowDialog() {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择需要批量跟进的学员')
    return
  }
  batchDialogVisible.value = true
}

async function saveBatchFollow() {
  saving.value = true
  try {
    await request.post('/api/teacher/growth-focus-follows/batch', {
      items: selectedRows.value.map((row) => ({
        classroomId: row.classroomId,
        userId: row.userId,
        planId: row.planId,
        latestDiagnosisId: row.latestDiagnosisId
      })),
      ...batchFollowForm
    })
    ElMessage.success(`已批量保存 ${selectedRows.value.length} 名学员的跟进记录`)
    batchDialogVisible.value = false
    selectedRows.value = []
    await loadData()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

watch(() => dashboardFilters.taskType, async () => {
  await nextTick()
  await renderCharts()
})

onMounted(() => {
  loadData()
  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts)
  scoreChart?.dispose()
  growthChart?.dispose()
  distributionChart?.dispose()
})
</script>

<template>
  <div class="teacher-page" v-loading="loading">
    <section class="report-hero">
      <div>
        <h1>教师成绩报表</h1>
        <p>{{ selectedClassroomLabel }} · {{ timeRangeLabel }} · {{ taskTypeLabel }}</p>
      </div>
      <div class="hero-actions">
        <el-button :icon="Refresh" plain @click="resetFilters">重置</el-button>
        <el-button :icon="Finished" plain @click="exportReportData">导出报表</el-button>
        <el-button :icon="DataAnalysis" type="primary" plain @click="exportData">导出成长协同</el-button>
      </div>
    </section>

    <section class="filter-card">
      <div class="filter-title">
        <el-icon><Search /></el-icon>
        <span>报表筛选</span>
      </div>
      <div class="filter-grid">
        <el-select v-model="dashboardFilters.timeRange" placeholder="时间范围">
          <el-option v-for="item in timeRangeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.classroomId" clearable placeholder="全部班级">
          <el-option v-for="item in growth.classroomOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="dashboardFilters.subjectName" clearable placeholder="全部科目">
          <el-option v-for="item in subjectOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="dashboardFilters.taskType" placeholder="任务类型">
          <el-option v-for="item in taskTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.segment" clearable placeholder="成长分层">
          <el-option label="重点预警" value="AT_RISK" />
          <el-option label="跟踪补强" value="TRACKING" />
          <el-option label="持续上升" value="RISING" />
          <el-option label="稳定冲刺" value="STABLE" />
        </el-select>
        <el-select v-model="query.followStatus" clearable placeholder="教师跟进状态">
          <el-option label="待跟进" value="PENDING" />
          <el-option label="跟进中" value="IN_PROGRESS" />
          <el-option label="已跟进" value="DONE" />
        </el-select>
        <el-button class="filter-submit" type="primary" :icon="Search" @click="handleSearch">筛选</el-button>
      </div>
    </section>

    <section class="stat-grid">
      <button v-for="item in kpiCards" :key="item.label" class="stat-card" :class="[item.tone, { clickable: item.filter }]" type="button" @click="applyKpiFilter(item)">
        <span class="stat-icon">
          <el-icon><component :is="item.icon" /></el-icon>
        </span>
        <span class="stat-copy">
          <span class="stat-group">{{ item.group }}</span>
          <span class="stat-label">{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em>{{ item.desc }}</em>
        </span>
      </button>
    </section>

    <section class="decision-grid">
      <div class="decision-card">
        <div class="section-head">
          <div>
            <h2>建议动作</h2>
            <p>按风险和待办优先级处理</p>
          </div>
        </div>
        <div class="action-list">
          <div v-for="item in actionHints" :key="item.title" class="action-item" :class="item.tone">
            <div>
              <strong>{{ item.title }}</strong>
              <span>{{ item.desc }}</span>
            </div>
            <el-button link type="primary" @click="handleAction(item.type)">{{ item.action }}</el-button>
          </div>
        </div>
      </div>

      <div class="decision-card">
        <div class="section-head">
          <div>
            <h2>需要关注的学生</h2>
            <p>优先展示预警和待跟进学生</p>
          </div>
          <el-button type="warning" plain :disabled="!selectedRows.length" @click="openBatchFollowDialog">批量跟进</el-button>
        </div>
        <div v-if="priorityFocusStudents.length" class="student-list">
          <div v-for="item in priorityFocusStudents" :key="`${item.classroomId}-${item.userId}-${item.planId}`" class="student-item">
            <div>
              <strong>{{ item.studentName }}</strong>
              <span>{{ item.classroomName }} · {{ item.segmentLabel }}</span>
            </div>
            <div class="student-meta">
              <el-tag size="small" :type="followTagType(item.teacherFollowStatus)" effect="plain">{{ item.teacherFollowStatusLabel }}</el-tag>
              <span>{{ formatNumber(item.completionRate, 1) }}%</span>
              <el-button size="small" type="primary" plain @click="openFollowDialog(item)">跟进</el-button>
            </div>
          </div>
        </div>
        <el-empty v-else :image-size="72" description="暂无重点关注学生" />
      </div>
    </section>

    <section class="chart-grid">
      <div class="chart-card main-chart">
        <div class="section-head">
          <div>
            <h2>教学成绩趋势</h2>
            <p>作业为蓝色，考试为橙色；样本少时以提交数量解释数据。</p>
          </div>
        </div>
        <div ref="scoreChartRef" class="chart-box"></div>
        <div v-if="!hasScoreData" class="chart-empty">当前筛选下暂无足够成绩样本</div>
      </div>
      <div class="chart-card">
        <div class="section-head">
          <div>
            <h2>正确率分布</h2>
            <p>快速识别低于 60% 的提交集中度。</p>
          </div>
        </div>
        <div ref="distributionChartRef" class="chart-box"></div>
        <div v-if="!hasDistributionData" class="chart-empty">当前仅有少量提交，分布参考价值有限</div>
      </div>
      <div class="chart-card side-chart">
        <div class="section-head">
          <div>
            <h2>成长诊断趋势</h2>
            <p>用于判断近期成长活力是否回升。</p>
          </div>
        </div>
        <div ref="growthChartRef" class="chart-box compact"></div>
        <div v-if="!hasGrowthData" class="chart-empty">暂无成长诊断记录</div>
      </div>
    </section>

    <section id="classroom-stats" class="table-card">
      <div class="section-head table-head">
        <div>
          <h2>班级维度教学统计</h2>
          <p>按完成效果、预警和待审核状态快速扫描班级表现。</p>
        </div>
        <el-button plain @click="exportReportData">导出当前报表</el-button>
      </div>
      <el-table :data="filteredClassroomStats" empty-text="暂无班级统计数据">
        <el-table-column prop="classroomName" label="班级" min-width="180" sortable />
        <el-table-column prop="subjectName" label="科目" min-width="100" />
        <el-table-column prop="visibilityLabel" label="公开状态" min-width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.visibilityLabel === '公开' ? 'success' : 'info'" effect="plain">{{ row.visibilityLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="capacity" label="招生上限" min-width="100" align="right" sortable />
        <el-table-column prop="studentCount" label="当前学员" min-width="100" align="right" sortable />
        <el-table-column prop="pendingJoinCount" label="待审核" min-width="100" align="center" sortable>
          <template #default="{ row }">
            <el-tag size="small" :type="countTagType(row.pendingJoinCount)" effect="plain">{{ row.pendingJoinCount ? `${row.pendingJoinCount} 人待审` : '无待审' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="materialCount" label="材料数" min-width="90" align="right" sortable />
        <el-table-column prop="homeworkAverageScore" label="作业均分" min-width="120" align="right" sortable>
          <template #default="{ row }">
            <strong class="score-value homework">{{ formatNumber(row.homeworkAverageScore, 1) }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="examAverageScore" label="考试均分" min-width="120" align="right" sortable>
          <template #default="{ row }">
            <strong class="score-value exam">{{ formatNumber(row.examAverageScore, 1) }}</strong>
          </template>
        </el-table-column>
        <el-table-column prop="averageCorrectRate" label="平均正确率" min-width="180" sortable>
          <template #default="{ row }">
            <div class="rate-cell">
              <span>{{ formatNumber(row.averageCorrectRate, 2) }}%</span>
              <el-progress :percentage="toPercentNumber(row.averageCorrectRate)" :status="progressStatus(row.averageCorrectRate)" :show-text="false" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="router.push('/teacher/class')">查看详情</el-button>
            <el-button link type="warning" @click="ElMessage.info(`${row.classroomName} 的提醒入口已在重点学员跟进中处理`)">提醒学生</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="table-card">
      <div class="section-head">
        <div>
          <h2>成长分层概览</h2>
          <p>分层用于判断跟进策略是否集中在高风险学生。</p>
        </div>
      </div>
      <div class="tag-row">
        <el-tag v-for="item in growth.segmentStats" :key="item.segment" effect="plain">{{ item.segmentLabel }} {{ item.count }}</el-tag>
      </div>
    </section>

    <section class="table-card">
      <div class="section-head">
        <div>
          <h2>班级成长看板</h2>
          <p>用完成率、活力和跟进状态判断班级是否需要干预。</p>
        </div>
      </div>
      <el-table :data="growth.classroomBoards" empty-text="暂无班级成长数据">
        <el-table-column prop="classroomName" label="班级" min-width="160" sortable />
        <el-table-column prop="subjectName" label="科目" min-width="100" />
        <el-table-column prop="memberCount" label="学员数" min-width="90" align="right" sortable />
        <el-table-column prop="focusStudentCount" label="重点学员" min-width="100" align="right" sortable />
        <el-table-column prop="atRiskCount" label="预警人数" min-width="100" align="right" sortable />
        <el-table-column prop="pendingFollowCount" label="待跟进" min-width="90" align="right" sortable />
        <el-table-column prop="doneFollowCount" label="已跟进" min-width="90" align="right" sortable />
        <el-table-column prop="averageCompletionRate" label="平均完成率" min-width="180" sortable>
          <template #default="{ row }">
            <div class="rate-cell">
              <span>{{ formatNumber(row.averageCompletionRate, 2) }}%</span>
              <el-progress :percentage="toPercentNumber(row.averageCompletionRate)" :status="progressStatus(row.averageCompletionRate)" :show-text="false" />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="averageVitalityScore" label="平均活力" min-width="120" align="right" sortable>
          <template #default="{ row }">{{ formatNumber(row.averageVitalityScore, 1) }}</template>
        </el-table-column>
      </el-table>
    </section>

    <section class="table-card">
      <div class="section-head table-head">
        <div>
          <h2>班级重点学员协同跟进</h2>
          <p>低完成率、低活力和薄弱知识点是本表的主要处理依据。</p>
        </div>
        <el-button type="warning" plain :disabled="!selectedRows.length" @click="openBatchFollowDialog">批量跟进</el-button>
      </div>
      <el-table :data="growth.focusStudents" empty-text="暂无重点学员" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" />
        <el-table-column prop="classroomName" label="班级" min-width="140" />
        <el-table-column prop="studentName" label="学员" min-width="120" />
        <el-table-column prop="planName" label="当前计划" min-width="200" />
        <el-table-column prop="segmentLabel" label="成长分层" min-width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="row.segment === 'AT_RISK' ? 'danger' : 'warning'" effect="plain">{{ row.segmentLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="completionRate" label="完成率" min-width="160" sortable>
          <template #default="{ row }">
            <div class="rate-cell">
              <span>{{ formatNumber(row.completionRate, 1) }}%</span>
              <el-progress :percentage="toPercentNumber(row.completionRate)" :status="progressStatus(row.completionRate)" :show-text="false" />
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="vitalityScore" label="成长活力" min-width="100" align="right" sortable />
        <el-table-column prop="teacherFollowStatusLabel" label="教师跟进" min-width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="followTagType(row.teacherFollowStatus)" effect="plain">{{ row.teacherFollowStatusLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="adminFollowStatusLabel" label="后台跟进" min-width="120" />
        <el-table-column label="薄弱知识点" min-width="220">
          <template #default="{ row }">{{ (row.weakKnowledgePoints || []).join(' / ') || '暂无' }}</template>
        </el-table-column>
        <el-table-column prop="teacherFollowRemark" label="教师备注" min-width="220" />
        <el-table-column prop="nextFollowUpAt" label="下次跟进" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.nextFollowUpAt) }}</template>
        </el-table-column>
        <el-table-column prop="reminderStatusLabel" label="提醒状态" min-width="120" />
        <el-table-column prop="teacherFollowUpdatedAt" label="最近更新" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.teacherFollowUpdatedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="170">
          <template #default="{ row }">
            <el-button link type="primary" @click="openFollowDialog(row)">跟进</el-button>
            <el-button link type="warning" @click="openReminderDraft(row)">提醒</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section id="follow-reminders" class="table-card">
      <div class="section-head">
        <div>
          <h2>近期回访提醒</h2>
          <p>逾期、今日和近 7 天提醒集中处理。</p>
        </div>
      </div>
      <el-table :data="growth.followReminders" empty-text="暂无近期回访提醒">
        <el-table-column prop="classroomName" label="班级" min-width="140" />
        <el-table-column prop="studentName" label="学员" min-width="120" />
        <el-table-column prop="planName" label="当前计划" min-width="200" />
        <el-table-column prop="teacherFollowStatusLabel" label="教师跟进" min-width="120" />
        <el-table-column prop="teacherPriorityLevelLabel" label="优先级" min-width="120" />
        <el-table-column prop="nextFollowUpAt" label="下次回访" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.nextFollowUpAt) }}</template>
        </el-table-column>
        <el-table-column prop="reminderStatusLabel" label="提醒状态" min-width="120" />
        <el-table-column prop="teacherFollowRemark" label="教师备注" min-width="260" />
      </el-table>
    </section>

    <section class="table-card">
      <div class="section-head">
        <div>
          <h2>最近跟进轨迹</h2>
          <p>复盘教师和后台对重点学生的最近处理记录。</p>
        </div>
      </div>
      <el-table :data="growth.followHistory" empty-text="暂无跟进轨迹">
        <el-table-column prop="classroomName" label="班级" min-width="150" />
        <el-table-column prop="studentName" label="学员" min-width="120" />
        <el-table-column prop="followStatusLabel" label="状态" min-width="100" />
        <el-table-column prop="priorityLevelLabel" label="优先级" min-width="100" />
        <el-table-column prop="remark" label="备注" min-width="260" />
        <el-table-column prop="nextFollowUpAt" label="下次跟进" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.nextFollowUpAt) }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" min-width="100" />
        <el-table-column prop="createdAt" label="记录时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="dialogVisible" title="保存学员成长跟进" width="520px">
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
        <el-form-item label="下次跟进时间">
          <el-date-picker v-model="followForm.nextFollowUpAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="跟进备注">
          <el-input v-model="followForm.remark" type="textarea" :rows="4" placeholder="记录课堂观察、补强建议和下轮复查重点" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveFollow">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchDialogVisible" title="批量保存学员成长跟进" width="520px">
      <el-alert type="info" :closable="false" show-icon :title="`已选择 ${selectedRows.length} 名学员，将统一写入本次跟进策略。`" />
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
        <el-form-item label="下次跟进时间">
          <el-date-picker v-model="batchFollowForm.nextFollowUpAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="统一备注">
          <el-input v-model="batchFollowForm.remark" type="textarea" :rows="4" placeholder="记录这一批学员的统一跟进动作和复查重点" />
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
.teacher-page {
  display: grid;
  gap: 18px;
  max-width: 1480px;
  margin: 0 auto;
  padding: 24px 20px 44px;
}

.report-hero,
.filter-card,
.stat-card,
.decision-card,
.chart-card,
.table-card {
  border: 1px solid rgba(214, 224, 238, 0.9);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.report-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 22px 24px;
}

h1,
h2,
p {
  margin: 0;
}

h1 {
  color: #0f172a;
  font-size: 28px;
  line-height: 1.25;
  font-weight: 800;
}

h2 {
  color: #0f172a;
  font-size: 20px;
  line-height: 1.3;
  font-weight: 800;
}

.report-hero p,
.section-head p {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

.hero-actions,
.section-head,
.table-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.filter-card {
  padding: 18px;
}

.filter-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: #334155;
  font-size: 14px;
  font-weight: 800;
}

.filter-title .el-icon {
  color: #2563eb;
}

.filter-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: 150px minmax(180px, 1fr) 150px 150px 150px 150px 104px;
}

.filter-submit {
  width: 100%;
}

.stat-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.stat-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  min-height: 132px;
  padding: 18px;
  border-color: #dce6f2;
  text-align: left;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.stat-card.clickable {
  cursor: pointer;
}

.stat-card.clickable:hover {
  border-color: #93c5fd;
  box-shadow: 0 14px 34px rgba(37, 99, 235, 0.12);
  transform: translateY(-1px);
}

.stat-icon {
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 9px;
  color: #2563eb;
  background: #eef5ff;
  font-size: 19px;
}

.stat-card.warning .stat-icon {
  color: #d97706;
  background: #fff7e8;
}

.stat-card.danger .stat-icon {
  color: #dc2626;
  background: #fff1f2;
}

.stat-card.success .stat-icon {
  color: #15803d;
  background: #ecfdf3;
}

.stat-copy {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.stat-group {
  color: #94a3b8;
  font-size: 12px;
  font-weight: 800;
}

.stat-label {
  margin-top: 4px;
  color: #475569;
  font-size: 14px;
}

.stat-copy strong {
  margin-top: 8px;
  color: #0f172a;
  font-size: 30px;
  line-height: 1;
  font-weight: 800;
}

.stat-copy em {
  margin-top: 10px;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
  line-height: 1.45;
}

.decision-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
}

.decision-card,
.chart-card,
.table-card {
  min-width: 0;
  padding: 22px;
}

.action-list,
.student-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.action-item,
.student-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  min-height: 72px;
  border: 1px solid #e1e8f2;
  border-radius: 10px;
  background: #fbfdff;
  padding: 12px 14px;
}

.action-item {
  border-left: 4px solid #2563eb;
}

.action-item.warning {
  border-left-color: #f59e0b;
}

.action-item.danger {
  border-left-color: #dc2626;
}

.action-item.success {
  border-left-color: #16a34a;
}

.action-item strong,
.student-item strong {
  display: block;
  color: #0f172a;
  font-size: 15px;
}

.action-item span,
.student-item span {
  display: block;
  margin-top: 5px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.45;
}

.student-meta {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  min-width: 210px;
}

.student-meta > span {
  margin-top: 0;
  color: #0f172a;
  font-size: 13px;
  font-weight: 800;
}

.chart-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(0, 1.15fr) minmax(0, 1fr) minmax(320px, 0.75fr);
}

.main-chart {
  grid-column: span 1;
}

.chart-card {
  position: relative;
  min-height: 390px;
}

.chart-box {
  width: 100%;
  height: 280px;
  margin-top: 8px;
}

.chart-box.compact {
  height: 250px;
}

.chart-empty {
  position: absolute;
  right: 22px;
  bottom: 18px;
  left: 22px;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  background: rgba(248, 250, 252, 0.9);
  padding: 10px 12px;
  color: #64748b;
  font-size: 13px;
  text-align: center;
}

.tag-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.score-value {
  font-size: 14px;
}

.score-value.homework {
  color: #2563eb;
}

.score-value.exam {
  color: #f97316;
}

.rate-cell {
  display: grid;
  gap: 6px;
  min-width: 128px;
}

.rate-cell span {
  color: #0f172a;
  font-size: 13px;
  font-weight: 800;
}

.rate-cell :deep(.el-progress-bar__outer) {
  height: 7px !important;
  background: #e8eef7;
}

.table-card :deep(.el-table) {
  width: 100%;
  margin-top: 16px;
}

.table-card :deep(.el-table__inner-wrapper) {
  min-width: 0;
}

.table-card :deep(.el-table th.el-table__cell) {
  background: #f8fafc;
  color: #64748b;
  font-weight: 800;
}

.teacher-page :deep(.el-button) {
  border-radius: 8px;
  font-weight: 700;
}

.teacher-page :deep(.el-select),
.teacher-page :deep(.el-input) {
  width: 100%;
}

@media (max-width: 1280px) {
  .stat-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .filter-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .chart-grid,
  .decision-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .teacher-page {
    padding: 18px 12px 32px;
  }

  .report-hero,
  .hero-actions,
  .section-head,
  .table-head,
  .student-item,
  .action-item {
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-actions,
  .student-meta {
    width: 100%;
  }

  .filter-grid,
  .stat-grid {
    grid-template-columns: 1fr;
  }

  h1 {
    font-size: 24px;
  }
}
</style>
