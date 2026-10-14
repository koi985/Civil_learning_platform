<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
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
const timeDimension = ref('week')
const overview = ref({
  profile: {},
  recentOrders: [],
  recentPractices: [],
  recentExams: []
})
const statistics = ref({
  last7Days: [],
  totals: {}
})
const knowledgeMastery = ref({
  summary: {},
  records: []
})
const dailyTasks = ref([])
const testPaperReports = ref([])

const masteryRef = ref(null)
const learningRef = ref(null)
const scoreTrendRef = ref(null)
let masteryChart
let learningChart
let scoreTrendChart

const coreMetrics = computed(() => [
  { label: '已购课程', value: Number(overview.value.courseCount || 0), unit: '门' },
  { label: '已支付订单', value: Number(overview.value.paidOrderCount || 0), unit: '笔' },
  { label: '练习次数', value: Number(overview.value.practiceCount || 0), unit: '次' },
  { label: '模考次数', value: Number(overview.value.examCount || 0), unit: '次' },
  { label: '错题数量', value: Number(overview.value.wrongQuestionCount || 0), unit: '题' }
])

const recentPractices = computed(() => overview.value.recentPractices || [])
const recentExams = computed(() => overview.value.recentExams || [])
const chartDays = computed(() => (timeDimension.value === 'month' ? 30 : 7))
const learningSeries = computed(() => buildLearningSeries(chartDays.value))
const scoreTrendSeries = computed(() => buildScoreTrendSeries(chartDays.value))
const hasScoreTrendData = computed(() => scoreTrendSeries.value.some((item) => item.score > 0))
const masteryChartData = computed(() => {
  const records = knowledgeMastery.value.records || []
  const labels = {
    MASTERED: '已掌握',
    IMPROVING: '提升中',
    FOUNDATION: '打基础',
    WEAK: '薄弱项'
  }
  const grouped = records.reduce((acc, item) => {
    const key = item.masteryLevel || 'WEAK'
    const label = item.masteryLevelLabel || labels[key] || '待强化'
    acc[label] = (acc[label] || 0) + 1
    return acc
  }, {})
  return Object.entries(grouped).map(([name, value]) => ({ name, value }))
})
const hasMasteryData = computed(() => masteryChartData.value.length > 0)

function formatDate(value) {
  if (!value) return '暂无'
  return String(value).replace('T', ' ').slice(0, 16)
}

function asNumber(value) {
  return Number(value || 0)
}

function extractRecords(payload) {
  return payload?.page?.records || payload?.records || []
}

function toDateKey(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 10)
}

function dateKeyOfOffset(offset) {
  const date = new Date()
  date.setDate(date.getDate() + offset)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function buildDateBuckets(days) {
  return Array.from({ length: days }, (_, index) => {
    const key = dateKeyOfOffset(index - days + 1)
    return { key, label: key.slice(5), value: 0 }
  })
}

function withinCurrentWindow(value, days) {
  const key = toDateKey(value)
  if (!key) return false
  const start = dateKeyOfOffset(1 - days)
  const end = dateKeyOfOffset(0)
  return key >= start && key <= end
}

function buildLearningSeries(days) {
  const buckets = buildDateBuckets(days)
  const bucketMap = new Map(buckets.map((item) => [item.key, item]))
  dailyTasks.value
    .filter((task) => task.taskType === 'LEARN')
    .forEach((task) => {
      const bucket = bucketMap.get(toDateKey(task.taskDate))
      if (bucket) {
        bucket.value += asNumber(task.completedValue)
      }
    })
  return buckets
}

function buildScoreTrendSeries(days) {
  return testPaperReports.value
    .filter((item) => withinCurrentWindow(item.latestSubmittedAt || item.submittedAt || item.createdAt, days))
    .map((item) => ({
      id: item.latestPracticeId || item.practiceId || item.id,
      reportPath: item.reportPath,
      title: item.title || item.paperTitle || `测卷 ${item.id}`,
      date: toDateKey(item.latestSubmittedAt || item.submittedAt || item.createdAt),
      score: asNumber(item.latestScore ?? item.score)
    }))
    .sort((left, right) => `${left.date}-${left.id}`.localeCompare(`${right.date}-${right.id}`))
    .slice(-12)
}

async function loadData() {
  loading.value = true
  try {
    const [overviewData, statisticsData, masteryData, tasksData, reportsData] = await Promise.all([
      request.get('/api/user-center/overview'),
      request.get('/api/user-center/statistics'),
      request.get('/api/user-center/knowledge-mastery', { params: { pageNo: 1, pageSize: 100 } }),
      request.get('/api/user-center/tasks', { params: { pageNo: 1, pageSize: 100 } }),
      request.get('/api/user-center/test-paper-reports', { params: { pageNo: 1, pageSize: 80 } })
    ])
    overview.value = overviewData
    statistics.value = statisticsData
    knowledgeMastery.value = {
      summary: masteryData?.summary || {},
      records: extractRecords(masteryData)
    }
    dailyTasks.value = extractRecords(tasksData)
    testPaperReports.value = extractRecords(reportsData)
    await nextTick()
    await renderCharts()
  } finally {
    loading.value = false
  }
}

async function renderCharts() {
  const echarts = await loadEcharts()
  await renderMasteryChart()
  await renderLearningChart()
  await renderScoreTrendChart()
}

async function renderMasteryChart() {
  const echarts = await loadEcharts()
  if (!masteryRef.value || !hasMasteryData.value) {
    masteryChart?.dispose()
    masteryChart = null
    return
  }
  if (masteryChart && masteryChart.getDom() !== masteryRef.value) {
    masteryChart.dispose()
    masteryChart = null
  }
  masteryChart = masteryChart || echarts.init(masteryRef.value)
  masteryChart.setOption({
    color: ['#16a34a', '#2563eb', '#f59e0b', '#ef4444'],
    tooltip: { trigger: 'item', formatter: '{b}: {c} 个 ({d}%)' },
    legend: { bottom: 0, itemWidth: 10, itemHeight: 10 },
    series: [
      {
        name: '知识点掌握率',
        type: 'pie',
        radius: ['46%', '72%'],
        center: ['50%', '44%'],
        avoidLabelOverlap: true,
        label: { formatter: '{b}\n{d}%' },
        data: masteryChartData.value
      }
    ]
  })
  masteryChart.off('click')
  masteryChart.on('click', () => router.push('/user/knowledge'))
}

async function renderLearningChart() {
  const echarts = await loadEcharts()
  if (!learningRef.value) return
  if (learningChart && learningChart.getDom() !== learningRef.value) {
    learningChart.dispose()
    learningChart = null
  }
  const series = learningSeries.value
  learningChart = learningChart || echarts.init(learningRef.value)
  learningChart.setOption({
    color: ['#2563eb'],
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 18, top: 24, bottom: 34 },
    xAxis: { type: 'category', data: series.map((item) => item.label), axisTick: { show: false } },
    yAxis: {
      type: 'value',
      name: '分钟',
      minInterval: 10,
      splitLine: { lineStyle: { color: '#e5e7eb' } }
    },
    series: [
      {
        name: '每日学习时长',
        type: 'line',
        smooth: true,
        symbolSize: 8,
        areaStyle: { opacity: 0.12 },
        data: series.map((item) => item.value)
      }
    ]
  })
  learningChart.off('click')
  learningChart.on('click', () => router.push('/user/stat'))
}

async function renderScoreTrendChart() {
  const echarts = await loadEcharts()
  if (!scoreTrendRef.value || !hasScoreTrendData.value) {
    scoreTrendChart?.dispose()
    scoreTrendChart = null
    return
  }
  if (scoreTrendChart && scoreTrendChart.getDom() !== scoreTrendRef.value) {
    scoreTrendChart.dispose()
    scoreTrendChart = null
  }
  const series = scoreTrendSeries.value
  scoreTrendChart = scoreTrendChart || echarts.init(scoreTrendRef.value)
  scoreTrendChart.setOption({
    color: ['#0f766e'],
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 18, top: 24, bottom: 70 },
    xAxis: {
      type: 'category',
      data: series.map((item) => item.title),
      axisLabel: { interval: 0, rotate: 26, width: 96, overflow: 'truncate' }
    },
    yAxis: { type: 'value', max: 100, splitLine: { lineStyle: { color: '#e5e7eb' } } },
    series: [
      {
        name: '测试分数',
        type: 'bar',
        barMaxWidth: 24,
        data: series.map((item) => ({ value: item.score, reportPath: item.reportPath }))
      }
    ]
  })
  scoreTrendChart.off('click')
  scoreTrendChart.on('click', (params) => {
    const path = params.data?.reportPath || '/user/test-paper-report'
    router.push(path)
  })
}

function resizeCharts() {
  ;[masteryChart, learningChart, scoreTrendChart].forEach((chart) => chart?.resize())
}

watch(timeDimension, async () => {
  await nextTick()
  await renderLearningChart()
  await renderScoreTrendChart()
})

onMounted(() => {
  loadData()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  ;[masteryChart, learningChart, scoreTrendChart].forEach((chart) => chart?.dispose())
})
</script>

<template>
  <div class="profile-page" v-loading="loading">
    <section class="profile-hero">
      <div>
        <div class="eyebrow">用户中心概览</div>
        <h1>{{ overview.profile.nickname || '学习用户' }}</h1>
        <p>{{ overview.profile.province || '未设置地区' }} · {{ overview.profile.examType || '未设置考试' }} · {{ overview.profile.preferredSubject || '未设置科目' }}</p>
      </div>
      <div class="hero-score">
        <span>最佳模考分</span>
        <strong>{{ overview.bestExamScore || 0 }}</strong>
      </div>
    </section>

    <section class="metric-grid">
      <article v-for="item in coreMetrics" :key="item.label" class="metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <em>{{ item.unit }}</em>
      </article>
    </section>

    <section class="chart-dashboard">
      <div class="section-head">
        <div>
          <h2>学习数据看板</h2>
          <p>掌握率、学习时长和测试分数趋势会跟随学习行为刷新</p>
        </div>
        <el-radio-group v-model="timeDimension" size="large">
          <el-radio-button label="week">按周</el-radio-button>
          <el-radio-button label="month">按月</el-radio-button>
        </el-radio-group>
      </div>

      <div class="dashboard-grid">
        <article class="glass-card chart-card">
          <div class="card-head">
            <div>
              <h2>知识点掌握率</h2>
              <p>按掌握阶段聚合当前知识点，点击查看知识点明细</p>
            </div>
          </div>
          <div v-if="hasMasteryData" ref="masteryRef" class="chart-box"></div>
          <el-empty v-else description="暂无知识点掌握数据" />
        </article>

        <article class="glass-card chart-card">
          <div class="card-head">
            <div>
              <h2>每日学习时长</h2>
              <p>{{ timeDimension === 'week' ? '近 7 天' : '近 30 天' }}学习任务完成时长，点击查看学习统计</p>
            </div>
          </div>
          <div ref="learningRef" class="chart-box"></div>
        </article>

        <article class="glass-card chart-card wide">
          <div class="card-head">
            <div>
              <h2>测试分数趋势</h2>
              <p>{{ timeDimension === 'week' ? '近 7 天' : '近 30 天' }}测卷得分，点击柱状图查看对应报告</p>
            </div>
          </div>
          <div v-if="hasScoreTrendData" ref="scoreTrendRef" class="chart-box"></div>
          <el-empty v-else description="暂无测试分数数据" />
        </article>
      </div>
    </section>

    <section class="recent-grid">
      <article class="glass-card panel">
        <div class="card-head">
          <h2>最近练习明细</h2>
        </div>
        <div v-if="recentPractices.length" class="record-list">
          <div v-for="item in recentPractices" :key="item.id" class="record-item">
            <div>
              <strong>{{ item.title }}</strong>
              <span>{{ formatDate(item.createdAt) }}</span>
            </div>
            <em>{{ item.score || 0 }} 分 · {{ item.accuracyRate || 0 }}%</em>
          </div>
        </div>
        <el-empty v-else description="暂无练习记录" />
      </article>

      <article class="glass-card panel">
        <div class="card-head">
          <h2>最近模考明细</h2>
        </div>
        <div v-if="recentExams.length" class="record-list">
          <div v-for="item in recentExams" :key="item.id" class="record-item">
            <div>
              <strong>{{ item.title }}</strong>
              <span>{{ formatDate(item.createdAt) }}</span>
            </div>
            <em>{{ item.score || 0 }} 分</em>
          </div>
        </div>
        <el-empty v-else description="暂无模考记录" />
      </article>
    </section>
  </div>
</template>

<style scoped>
.profile-page {
  display: grid;
  gap: 20px;
}

.profile-hero {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  padding: 24px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fff;
}

.eyebrow {
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
}

.profile-hero h1 {
  margin: 8px 0;
  font-size: 30px;
}

.profile-hero p,
.card-head p,
.record-item span {
  color: var(--muted);
}

.hero-score {
  min-width: 140px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 8px;
  text-align: center;
  background: #f8fafc;
}

.hero-score span,
.metric-card span {
  display: block;
  color: var(--muted);
}

.hero-score strong {
  display: block;
  margin-top: 6px;
  color: #0f766e;
  font-size: 34px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 14px;
}

.metric-card {
  position: relative;
  padding: 18px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fff;
}

.metric-card strong {
  display: inline-block;
  margin-top: 10px;
  color: var(--primary);
  font-size: 30px;
}

.metric-card em {
  margin-left: 6px;
  color: var(--muted);
  font-style: normal;
}

.chart-dashboard {
  display: grid;
  gap: 16px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 20px 22px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fff;
}

.section-head h2 {
  margin: 0;
  font-size: 22px;
}

.section-head p {
  margin: 6px 0 0;
  color: var(--muted);
}

.dashboard-grid,
.recent-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.chart-card,
.panel {
  padding: 22px;
}

.chart-card.wide {
  grid-column: span 2;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 12px;
}

.card-head h2 {
  margin: 0;
  font-size: 20px;
}

.card-head p {
  margin: 6px 0 0;
}

.chart-box {
  width: 100%;
  height: 320px;
}

.record-list {
  display: grid;
  gap: 12px;
}

.record-item {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fff;
}

.record-item div {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.record-item strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-item em {
  flex: 0 0 auto;
  color: #f97316;
  font-style: normal;
  font-weight: 700;
}

@media (max-width: 1080px) {
  .dashboard-grid,
  .recent-grid {
    grid-template-columns: 1fr;
  }

  .chart-card.wide {
    grid-column: span 1;
  }
}

@media (max-width: 720px) {
  .profile-hero,
  .section-head,
  .record-item {
    align-items: stretch;
    flex-direction: column;
  }

  .profile-hero h1 {
    font-size: 24px;
  }

  .chart-box {
    height: 280px;
  }
}
</style>
