<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
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
const today = formatLocalDate(new Date())
const archive = ref({
  currentPlan: null,
  currentPlanSummary: {},
  achievementSummary: {},
  recentAchievements: [],
  planHistory: [],
  testPaperHighlights: []
})
const tasks = ref({ summary: {}, page: { records: [] } })
const mastery = ref({ summary: {}, page: { records: [] } })
const diagnosis = ref({
  latestDiagnosis: null,
  stageSummary: {},
  focusKnowledgePoints: [],
  actionSuggestions: [],
  testPaperReports: [],
  historyComparison: {}
})
const statistics = ref({ last7Days: [], totals: {}, subjectSummary: [] })
const checkin = ref({ streakDays: 0 })
const reports = ref({ page: { records: [] } })

const radarRef = ref(null)
const trendRef = ref(null)
let radarChart
let trendChart

const taskRecords = computed(() => extractRecords(tasks.value))
const knowledgeRecords = computed(() => extractRecords(mastery.value))
const reportRecords = computed(() => extractRecords(reports.value).length ? extractRecords(reports.value) : archive.value.testPaperHighlights || [])
const currentPlan = computed(() => archive.value.currentPlan)
const planSummary = computed(() => archive.value.currentPlanSummary || {})
const taskSummary = computed(() => tasks.value.summary || {})
const masterySummary = computed(() => mastery.value.summary || {})
const stageSummary = computed(() => diagnosis.value.stageSummary || {})

const todayTasks = computed(() => taskRecords.value.filter((item) => item.taskDate === today || !item.taskDate))
const pendingTasks = computed(() => todayTasks.value.filter((item) => item.status !== 'COMPLETED'))
const completedTasks = computed(() => todayTasks.value.filter((item) => item.status === 'COMPLETED'))
const todayCompletionRate = computed(() => {
  const total = Number(taskSummary.value.todayTaskCount || todayTasks.value.length || planSummary.value.todayTaskCount || 0)
  const completed = Number(taskSummary.value.todayCompletedTaskCount || completedTasks.value.length || planSummary.value.todayCompletedTaskCount || 0)
  return clampPercent(total ? Math.round((completed * 100) / total) : 0)
})
const weekPracticeCount = computed(() => {
  return (statistics.value.last7Days || []).reduce((sum, item) => sum + Number(item.practiceCount || 0) + Number(item.examCount || 0), 0)
})
const weakKnowledgeCount = computed(() => Number(masterySummary.value.weakKnowledgeCount || stageSummary.value.weakKnowledgeCount || 0))
const topWeakKnowledge = computed(() => {
  const focus = diagnosis.value.focusKnowledgePoints || []
  const source = focus.length ? focus : knowledgeRecords.value
  return [...source]
    .sort((left, right) => Number(right.priorityScore || 0) - Number(left.priorityScore || 0))
    .slice(0, 5)
})
const planFocus = computed(() => {
  const focus = currentPlan.value?.focusKnowledgePoints || []
  return focus.length ? focus : topWeakKnowledge.value.map((item) => item.knowledgePoint).slice(0, 4)
})
const nextActions = computed(() => {
  const taskActions = pendingTasks.value.slice(0, 3).map((item) => ({
    key: `task-${item.id}`,
    title: item.title,
    content: item.description,
    path: item.actionPath || '/question-bank',
    actionText: item.actionText || '去完成',
    type: 'task'
  }))
  const diagnosisActions = (diagnosis.value.actionSuggestions || []).slice(0, 2).map((item, index) => ({
    key: `diagnosis-${index}`,
    title: item.title,
    content: item.content,
    path: item.path || '/user/diagnosis',
    actionText: item.actionText || '查看',
    type: 'diagnosis'
  }))
  return [...taskActions, ...diagnosisActions].slice(0, 5)
})
const growthTimeline = computed(() => {
  const achievements = (archive.value.recentAchievements || []).map((item) => ({
    key: `a-${item.id}`,
    time: item.createdAt,
    title: item.title,
    content: item.description,
    value: formatAchievementValue(item),
    type: '成长'
  }))
  const reportItems = reportRecords.value.map((item) => ({
    key: `r-${item.id}`,
    time: item.latestSubmittedAt || item.submittedAt || item.createdAt,
    title: item.title || item.paperTitle || '个性化测卷',
    content: item.recommendationReason || '完成测卷并生成阶段报告。',
    value: item.latestAccuracyRate ? `${item.latestAccuracyRate}%` : `${item.latestScore || item.score || 0}分`,
    path: item.reportPath,
    type: '测卷'
  }))
  return [...achievements, ...reportItems]
    .sort((left, right) => String(right.time || '').localeCompare(String(left.time || '')))
    .slice(0, 10)
})
const heroMetrics = computed(() => [
  { label: '今日完成率', value: `${todayCompletionRate.value}%`, note: `${taskSummary.value.todayCompletedTaskCount || completedTasks.value.length || 0}/${taskSummary.value.todayTaskCount || todayTasks.value.length || 0} 项任务` },
  { label: '连续学习', value: Number(checkin.value.streakDays || 0), note: '天' },
  { label: '本周练习', value: weekPracticeCount.value, note: '次练习/模考' },
  { label: '薄弱点', value: weakKnowledgeCount.value, note: '个待补强' }
])

function formatLocalDate(value) {
  const date = value instanceof Date ? value : new Date(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function extractRecords(value) {
  return value?.page?.records || value?.records || []
}

function clampPercent(value) {
  const number = Number(value || 0)
  return Math.max(0, Math.min(100, Math.round(number)))
}

function formatDate(value) {
  return value ? String(value).slice(0, 10) : '暂无'
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '暂无'
}

function taskTagType(status) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'IN_PROGRESS') return 'warning'
  return 'info'
}

function masteryTagType(level, status) {
  if (status === 'TO_DIAGNOSE') return 'info'
  if (level === 'MASTERED') return 'success'
  if (level === 'IMPROVING') return 'warning'
  if (level === 'FOUNDATION') return 'info'
  return 'danger'
}

function formatAchievementValue(item) {
  if (!item) return '已完成'
  if (String(item.title || '').includes('打卡')) return '已打卡'
  if (String(item.title || '').includes('刷题')) return '已达成'
  const value = Number(item.achievementValue || 0)
  return value > 0 && value <= 100 ? String(value) : '已完成'
}

function goTask(item) {
  router.push(item.actionPath || '/question-bank')
}

function goKnowledgeTest(item) {
  router.push({
    path: '/user/test-paper',
    query: {
      knowledge: item.knowledgePoint,
      subject: item.subjectName,
      paperType: item.knowledgeStatus === 'TO_DIAGNOSE' ? 'DIAGNOSIS' : 'WEAKNESS'
    }
  })
}

async function renderCharts() {
  const echarts = await loadEcharts()
  await renderRadar()
  await renderTrend()
}

async function renderRadar() {
  const echarts = await loadEcharts()
  if (!radarRef.value) return
  const source = topWeakKnowledge.value.length ? topWeakKnowledge.value : knowledgeRecords.value.slice(0, 5)
  radarChart = radarChart || echarts.init(radarRef.value)
  if (!source.length) {
    radarChart.clear()
    return
  }
  const indicators = source.map((item) => ({
    name: item.knowledgePoint || item.subjectName || '知识点',
    max: 100
  }))
  radarChart.setOption({
    tooltip: {},
    radar: {
      indicator: indicators,
      radius: '62%',
      splitNumber: 4,
      axisName: { color: '#334155', fontSize: 12 }
    },
    series: [{
      type: 'radar',
      areaStyle: { color: 'rgba(37, 99, 235, 0.16)' },
      lineStyle: { color: '#2563eb', width: 2 },
      itemStyle: { color: '#2563eb' },
      data: [{ value: source.map((item) => Number(item.masteryScore || item.accuracyRate || 0)), name: '掌握度' }]
    }]
  })
}

async function renderTrend() {
  const echarts = await loadEcharts()
  if (!trendRef.value) return
  const days = statistics.value.last7Days || []
  trendChart = trendChart || echarts.init(trendRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 28, right: 16, top: 28, bottom: 28 },
    xAxis: { type: 'category', data: days.map((item) => item.date) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '练习次数', type: 'bar', data: days.map((item) => Number(item.practiceCount || 0)), itemStyle: { color: '#2563eb' } },
      { name: '模考次数', type: 'line', smooth: true, data: days.map((item) => Number(item.examCount || 0)), itemStyle: { color: '#f59e0b' } }
    ]
  })
}

async function loadData() {
  loading.value = true
  try {
    const [archiveData, tasksData, masteryData, diagnosisData, statisticsData, reportData, checkinData] = await Promise.all([
      request.get('/api/user-center/growth-archive'),
      request.get('/api/user-center/tasks', { params: { pageNo: 1, pageSize: 100, taskDate: today } }),
      request.get('/api/user-center/knowledge-mastery', { params: { pageNo: 1, pageSize: 100 } }),
      request.get('/api/user-center/growth-diagnosis'),
      request.get('/api/user-center/statistics'),
      request.get('/api/user-center/test-paper-reports', { params: { pageNo: 1, pageSize: 6 } }),
      request.get('/api/question-bank/check-in').catch(() => ({ streakDays: 0 }))
    ])
    archive.value = archiveData
    tasks.value = tasksData
    mastery.value = masteryData
    diagnosis.value = diagnosisData
    statistics.value = statisticsData
    reports.value = reportData
    checkin.value = checkinData
    await nextTick()
    await renderCharts()
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
onBeforeUnmount(() => {
  radarChart?.dispose()
  trendChart?.dispose()
})
</script>

<template>
  <div class="archive-page" v-loading="loading">
    <section class="glass-card hero-card">
      <div class="hero-copy">
        <div class="section-title">成长档案</div>
        <p>把计划、今日任务、能力画像、薄弱点强化和成长轨迹集中在一个页面，打开后直接知道当前状态和下一步动作。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" @click="router.push('/question-bank')">继续练习</el-button>
        <el-button plain @click="router.push('/user/diagnosis')">阶段诊断</el-button>
        <el-button plain @click="router.push('/user/test-paper')">生成测卷</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <div v-for="item in heroMetrics" :key="item.label" class="glass-card metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.note }}</small>
      </div>
    </section>

    <section class="main-grid">
      <div class="glass-card plan-panel">
        <div class="panel-head">
          <div>
            <div class="section-title small">当前成长计划</div>
            <p>计划进度、今日任务和重点知识点集中在这里。</p>
          </div>
          <el-button text type="primary" @click="router.push('/user/plan')">调整计划</el-button>
        </div>
        <div v-if="currentPlan" class="plan-body">
          <div class="plan-title">
            <h3>{{ currentPlan.planName }}</h3>
            <el-tag effect="plain">{{ currentPlan.targetExamType }} · {{ currentPlan.subjectName }}</el-tag>
          </div>
          <el-progress :percentage="clampPercent(planSummary.completionRate)" :stroke-width="10" />
          <div class="plan-meta">
            <span>周期 {{ currentPlan.durationDays || 0 }} 天</span>
            <span>今日 {{ planSummary.todayCompletedTaskCount || 0 }}/{{ planSummary.todayTaskCount || 0 }}</span>
            <span>重点 {{ planSummary.focusKnowledgeCount || planFocus.length || 0 }} 个</span>
          </div>
          <div class="tag-row">
            <el-tag v-for="item in planFocus" :key="item" effect="plain">{{ item }}</el-tag>
          </div>
          <div class="action-row">
            <el-button type="primary" @click="router.push('/question-bank')">继续学习</el-button>
            <el-button plain @click="router.push('/user/diagnosis')">查看诊断</el-button>
          </div>
        </div>
        <el-empty v-else description="当前还没有激活中的成长计划">
          <el-button type="primary" @click="router.push('/user/plan')">创建学习计划</el-button>
        </el-empty>
      </div>

      <div class="glass-card task-panel">
        <div class="panel-head">
          <div>
            <div class="section-title small">今日任务</div>
            <p>{{ completedTasks.length }} 项已完成，{{ pendingTasks.length }} 项待处理。</p>
          </div>
          <el-progress type="circle" :percentage="todayCompletionRate" :width="72" />
        </div>
        <div class="task-list" v-if="todayTasks.length">
          <article v-for="item in todayTasks.slice(0, 5)" :key="item.id" class="task-item">
            <div>
              <div class="item-title">
                <strong>{{ item.title }}</strong>
                <el-tag size="small" :type="taskTagType(item.status)">{{ item.statusLabel || item.status }}</el-tag>
              </div>
              <p>{{ item.description }}</p>
              <el-progress :percentage="clampPercent(item.progressPercent)" :stroke-width="7" />
              <small>{{ item.completedValue || 0 }}/{{ item.targetValue || 0 }} {{ item.unitLabel }}</small>
            </div>
            <el-button :type="item.status === 'COMPLETED' ? 'success' : 'primary'" plain @click="goTask(item)">
              {{ item.actionText || (item.status === 'COMPLETED' ? '查看' : '去完成') }}
            </el-button>
          </article>
        </div>
        <el-empty v-else description="今日暂无任务" />
      </div>
    </section>

    <section class="insight-grid">
      <div class="glass-card panel-card">
        <div class="panel-head">
          <div>
            <div class="section-title small">能力画像</div>
            <p>围绕当前优先级最高的知识点展示掌握度。</p>
          </div>
          <el-button text type="primary" @click="router.push('/user/knowledge')">全部知识点</el-button>
        </div>
        <div ref="radarRef" class="chart-box"></div>
      </div>

      <div class="glass-card panel-card">
        <div class="panel-head">
          <div>
            <div class="section-title small">薄弱点强化</div>
            <p>优先处理低掌握度、高优先级的知识点。</p>
          </div>
        </div>
        <div class="knowledge-list" v-if="topWeakKnowledge.length">
          <article v-for="item in topWeakKnowledge" :key="item.id || item.knowledgePoint" class="knowledge-item">
            <div>
              <div class="item-title">
                <strong>{{ item.knowledgePoint }}</strong>
                <el-tag size="small" :type="masteryTagType(item.masteryLevel, item.knowledgeStatus)">
                  {{ item.knowledgeStatusLabel || item.masteryLevelLabel || '待强化' }}
                </el-tag>
              </div>
              <p>{{ item.subjectName || '综合' }} · 掌握度 {{ item.masteryScore || 0 }} · 优先级 {{ item.priorityScore || 0 }}</p>
            </div>
            <el-button type="warning" plain @click="goKnowledgeTest(item)">生成测卷</el-button>
          </article>
        </div>
        <el-empty v-else description="当前暂无薄弱知识点" />
      </div>
    </section>

    <section class="insight-grid">
      <div class="glass-card panel-card">
        <div class="panel-head">
          <div>
            <div class="section-title small">本周趋势</div>
            <p>练习和模考活跃度，用于判断本周节奏是否稳定。</p>
          </div>
          <el-button text type="primary" @click="router.push('/user/stat')">学习统计</el-button>
        </div>
        <div ref="trendRef" class="chart-box compact"></div>
      </div>

      <div class="glass-card panel-card">
        <div class="panel-head">
          <div>
            <div class="section-title small">下一步动作</div>
            <p>按今日待办和阶段诊断自动整理。</p>
          </div>
        </div>
        <div class="action-list" v-if="nextActions.length">
          <article v-for="item in nextActions" :key="item.key" class="action-item">
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.content }}</p>
            </div>
            <el-button type="primary" plain @click="router.push(item.path)">{{ item.actionText }}</el-button>
          </article>
        </div>
        <el-empty v-else description="当前暂无新的建议动作" />
      </div>
    </section>

    <section class="glass-card timeline-panel">
      <div class="panel-head">
        <div>
          <div class="section-title small">成长时间线</div>
          <p>按时间聚合成长激励和测卷成果，弱化重复噪声。</p>
        </div>
        <el-button text type="primary" @click="router.push('/user/test-paper-report')">测卷报告</el-button>
      </div>
      <div class="timeline-list" v-if="growthTimeline.length">
        <article v-for="item in growthTimeline" :key="item.key" class="timeline-item">
          <div class="timeline-dot"></div>
          <div>
            <div class="item-title">
              <strong>{{ item.title }}</strong>
              <el-tag size="small" effect="plain">{{ item.type }}</el-tag>
            </div>
            <p>{{ item.content }}</p>
            <small>{{ formatDateTime(item.time) }}</small>
          </div>
          <div class="timeline-side">
            <span>{{ item.value }}</span>
            <el-button v-if="item.path" text type="primary" @click="router.push(item.path)">查看</el-button>
          </div>
        </article>
      </div>
      <el-empty v-else description="当前还没有成长轨迹" />
    </section>

    <section class="glass-card archive-panel">
      <div class="panel-head">
        <div>
          <div class="section-title small">历史计划归档</div>
          <p>保留历史计划，但作为辅助信息放在页面底部。</p>
        </div>
      </div>
      <div class="history-list" v-if="archive.planHistory?.length">
        <article v-for="item in archive.planHistory" :key="item.id" class="history-item">
          <div>
            <strong>{{ item.planName }}</strong>
            <p>{{ item.targetExamType }} · {{ item.subjectName }} · 归档于 {{ formatDate(item.archivedAt) }}</p>
          </div>
          <div class="history-side">
            <span>{{ item.completedTaskCount || 0 }}/{{ item.totalTaskCount || 0 }} 任务</span>
            <el-tag type="success">{{ item.completionRate || 0 }}%</el-tag>
          </div>
        </article>
      </div>
      <el-empty v-else description="还没有历史归档计划" />
    </section>
  </div>
</template>

<style scoped>
.archive-page {
  display: grid;
  gap: 20px;
}

.hero-card,
.panel-card,
.plan-panel,
.task-panel,
.timeline-panel,
.archive-panel {
  padding: 24px;
}

.hero-card,
.panel-head,
.item-title,
.history-item,
.timeline-item,
.action-item,
.knowledge-item,
.task-item,
.plan-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.hero-copy p,
.panel-head p,
.task-item p,
.knowledge-item p,
.action-item p,
.timeline-item p,
.history-item p,
.plan-meta,
.metric-card small,
.task-item small,
.timeline-item small {
  color: var(--muted);
}

.hero-actions,
.action-row,
.tag-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.metric-card {
  padding: 18px 20px;
}

.metric-card span,
.metric-card small {
  display: block;
}

.metric-card strong {
  display: block;
  margin: 8px 0 4px;
  color: var(--primary);
  font-size: 30px;
  line-height: 1;
}

.main-grid,
.insight-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 20px;
}

.section-title.small {
  font-size: 22px;
}

.plan-body,
.task-list,
.knowledge-list,
.action-list,
.timeline-list,
.history-list {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.plan-body h3 {
  margin: 0;
  font-size: 22px;
}

.plan-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.task-item,
.knowledge-item,
.action-item,
.timeline-item,
.history-item {
  min-height: 76px;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 14px 16px;
  background: #fff;
}

.task-item > div,
.knowledge-item > div,
.action-item > div,
.timeline-item > div:nth-child(2),
.history-item > div:first-child {
  min-width: 0;
}

.task-item p,
.knowledge-item p,
.action-item p,
.timeline-item p,
.history-item p {
  margin: 8px 0;
  line-height: 1.6;
}

.chart-box {
  height: 320px;
  margin-top: 10px;
}

.chart-box.compact {
  height: 260px;
}

.timeline-item {
  position: relative;
  align-items: flex-start;
  padding-left: 34px;
}

.timeline-dot {
  position: absolute;
  left: 14px;
  top: 22px;
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: var(--primary);
  box-shadow: 0 0 0 5px rgba(37, 99, 235, 0.12);
}

.timeline-side,
.history-side {
  display: grid;
  gap: 8px;
  justify-items: end;
  flex: 0 0 auto;
}

.timeline-side span {
  color: var(--primary);
  font-size: 18px;
  font-weight: 800;
}

@media (max-width: 1180px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .main-grid,
  .insight-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hero-card,
  .panel-head,
  .task-item,
  .knowledge-item,
  .action-item,
  .history-item,
  .timeline-item {
    align-items: stretch;
    flex-direction: column;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .timeline-side,
  .history-side {
    justify-items: start;
  }
}
</style>
