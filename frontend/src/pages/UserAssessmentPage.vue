<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Aim, DataLine, DocumentChecked, Finished, List, TrendCharts } from '@element-plus/icons-vue'
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
const starting = ref(false)
const payload = ref({
  currentPlan: null,
  latestDiagnosis: null,
  assessmentSummary: {},
  assessmentPrediction: {},
  assessmentEntry: {},
  assessmentHistory: [],
  assessmentSummaryByPeriod: [],
  filters: {}
})
const query = reactive({
  dateRange: [],
  periodType: 'WEEK'
})
const trendRef = ref(null)
const factorRef = ref(null)
let trendChart
let factorChart
const drilldownVisible = ref(false)
const drilldownRows = ref([])
const drilldownTitle = ref('')

const predictedPassRate = computed(() => Number(payload.value.assessmentPrediction?.predictedPassRate || 0))
const predictionConfidence = computed(() => Number(payload.value.assessmentPrediction?.predictionConfidence || 0))
const latestAccuracy = computed(() => Number(payload.value.assessmentSummary?.latestAssessmentAccuracy || 0))
const assessmentQuestionCount = computed(() => Number(payload.value.assessmentEntry?.questionCount || payload.value.assessmentSummary?.latestAssessmentQuestionCount || 20))
const hasTrendData = computed(() => groupedAssessmentTrend.value.length > 0)
const hasFactors = computed(() => (payload.value.assessmentPrediction?.factorBreakdown || []).length > 0)

const summaryCards = computed(() => [
  {
    label: '评测次数',
    value: payload.value.assessmentSummary?.sessionCount || 0,
    suffix: '',
    note: '累计发起次数',
    icon: List
  },
  {
    label: '已完成',
    value: payload.value.assessmentSummary?.completedCount || 0,
    suffix: '',
    note: '已提交并生成结果',
    icon: Finished
  },
  {
    label: '待完成',
    value: payload.value.assessmentSummary?.pendingCount || 0,
    suffix: '',
    note: '未提交的评测任务',
    icon: DocumentChecked
  },
  {
    label: '最近准确率',
    value: latestAccuracy.value,
    suffix: '%',
    note: '最近一次评测结果',
    icon: DataLine
  }
])

const assessmentSteps = [
  '预测阶段通过率',
  '生成薄弱知识点诊断',
  '自动更新学习计划建议'
]

function formatDate(value) {
  return value ? String(value).slice(0, 10) : '暂无'
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '暂无'
}

function normalizeDate(value) {
  return value ? String(value).slice(0, 10) : ''
}

async function loadData() {
  loading.value = true
  try {
    payload.value = await request.get('/api/user-center/growth-assessment', {
      params: {
        startDate: query.dateRange?.[0] || undefined,
        endDate: query.dateRange?.[1] || undefined
      }
    })
    query.dateRange = payload.value.filters?.startDate && payload.value.filters?.endDate
      ? [payload.value.filters.startDate, payload.value.filters.endDate]
      : []
    await nextTick()
    await renderCharts()
  } finally {
    loading.value = false
  }
}

async function startAssessment() {
  starting.value = true
  try {
    const data = await request.post('/api/user-center/growth-assessment/start')
    ElMessage.success(`评测已开始，题量 ${data.questionCount} 题`)
    router.push(`/practice/${data.practiceId}`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    starting.value = false
  }
}

async function renderCharts() {
  const echarts = await loadEcharts()
  const grouped = groupedAssessmentTrend.value
  if (trendRef.value) {
    trendChart = trendChart || echarts.init(trendRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['预测通过率', '实际准确率'] },
      xAxis: { type: 'category', data: grouped.map((item) => item.label) },
      yAxis: { type: 'value', max: 100 },
      series: [
        { name: '预测通过率', type: 'line', smooth: true, data: grouped.map((item) => item.predictedPassRate), itemStyle: { color: '#1d4ed8' } },
        { name: '实际准确率', type: 'bar', data: grouped.map((item) => item.actualAccuracy), itemStyle: { color: '#0f766e' } }
      ]
    })
    trendChart.off('click')
    trendChart.on('click', ({ dataIndex }) => {
      const row = grouped[dataIndex]
      if (!row) return
      drilldownTitle.value = `${row.label} 评测详情`
      drilldownRows.value = row.records || []
      drilldownVisible.value = true
    })
  }

  if (factorRef.value) {
    factorChart = factorChart || echarts.init(factorRef.value)
    const factors = payload.value.assessmentPrediction?.factorBreakdown || []
    factorChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: factors.map((item) => item.label) },
      yAxis: { type: 'value', max: 100 },
      series: [
        { name: '加权贡献', type: 'bar', data: factors.map((item) => Number(item.contribution || 0)), itemStyle: { color: '#f97316' } }
      ]
    })
  }
}

const groupedAssessmentTrend = computed(() => {
  const source = payload.value.assessmentHistory || []
  const map = new Map()
  source.forEach((item) => {
    const rawDate = normalizeDate(item.submittedAt || item.createdAt)
    if (!rawDate) {
      return
    }
    const key = query.periodType === 'MONTH' ? rawDate.slice(0, 7) : `${rawDate.slice(0, 4)}-W${getWeekOfYear(rawDate)}`
    const label = query.periodType === 'MONTH' ? key : `${rawDate.slice(5, 7)}月第${getWeekOfYear(rawDate, true)}周`
    if (!map.has(key)) {
      map.set(key, { key, label, records: [], predictedPassRate: 0, actualAccuracy: 0 })
    }
    const bucket = map.get(key)
    bucket.records.push(item)
    bucket.predictedPassRate += Number(item.predictedPassRate || 0)
    bucket.actualAccuracy += Number(item.latestAccuracyRate || 0)
  })
  return Array.from(map.values()).map((item) => ({
    ...item,
    predictedPassRate: Number((item.predictedPassRate / item.records.length).toFixed(2)),
    actualAccuracy: Number((item.actualAccuracy / item.records.length).toFixed(2))
  }))
})

function getWeekOfYear(value, monthWeek = false) {
  if (!value) return 0
  const date = new Date(value)
  if (monthWeek) {
    return Math.ceil(date.getDate() / 7)
  }
  const start = new Date(date.getFullYear(), 0, 1)
  return Math.ceil((((date - start) / 86400000) + start.getDay() + 1) / 7)
}

function handleFilter() {
  loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="assessment-page" v-loading="loading">
    <section class="glass-card hero-card">
      <div class="hero-copy">
        <div class="section-title">独立入学评测</div>
        <p>{{ assessmentQuestionCount }} 题 · 约 {{ assessmentQuestionCount }} 分钟 · 系统会根据结果校准通过率、诊断薄弱点，并更新后续学习建议。</p>
        <div class="hero-meta">
          <span v-for="item in assessmentSteps" :key="item">{{ item }}</span>
        </div>
        <div class="hero-actions">
          <el-button type="primary" size="large" :icon="Aim" :loading="starting" @click="startAssessment">开始 {{ assessmentQuestionCount }} 题评测</el-button>
          <el-button plain @click="router.push('/user/diagnosis')">返回诊断页</el-button>
        </div>
      </div>
      <div class="hero-side">
        <div class="hero-score">
          <strong>{{ predictedPassRate }}%</strong>
          <span>{{ payload.assessmentPrediction.predictionStageLabel || '暂无预测' }}</span>
          <small>置信度 {{ predictionConfidence }}%</small>
        </div>
      </div>
    </section>

    <section class="summary-grid">
      <div v-for="item in summaryCards" :key="item.label" class="glass-card summary-card">
        <span class="summary-icon"><el-icon><component :is="item.icon" /></el-icon></span>
        <span class="summary-label">{{ item.label }}</span>
        <strong>{{ item.value }}{{ item.suffix }}</strong>
        <small>{{ item.note }}</small>
      </div>
    </section>

    <section class="double-grid">
      <div class="glass-card panel-card">
        <div class="section-title small">通过率预测</div>
        <div class="prediction-card">
          <div class="progress-head">
            <span>预测通过率</span>
            <strong>{{ predictedPassRate }}%</strong>
          </div>
          <el-progress :percentage="predictedPassRate" :stroke-width="12" :show-text="false" />
          <div class="meta-row">
            <span>预测置信度</span>
            <strong>{{ predictionConfidence }}%</strong>
          </div>
          <el-progress :percentage="predictionConfidence" :stroke-width="8" color="#0f766e" :show-text="false" />
          <div class="meta-row">
            <span>最近评测</span>
            <strong>{{ payload.assessmentPrediction.latestAssessmentAccuracy || 0 }}%</strong>
          </div>
          <div v-if="!payload.assessmentPrediction.latestAssessmentSubmittedAt" class="empty-inline">
            完成首次评测后，将展示最近准确率和预测置信度变化。
          </div>
          <p class="diagnosis-text">{{ payload.assessmentPrediction.recommendationText }}</p>
        </div>
      </div>

      <div class="glass-card panel-card">
        <div class="section-title small">评测摘要</div>
        <div class="assessment-entry">
          <div>
            <strong>{{ payload.assessmentEntry.title || '独立入学评测' }}</strong>
            <p>{{ payload.assessmentEntry.description || `${assessmentQuestionCount} 题独立评测入口` }}</p>
            <div class="entry-tags">
              <span>{{ assessmentQuestionCount }} 题</span>
              <span>约 {{ assessmentQuestionCount }} 分钟</span>
              <span>{{ payload.assessmentPrediction.latestAssessmentSubmittedAt ? '已提交过评测' : '尚未提交' }}</span>
            </div>
          </div>
          <div class="assessment-side">
            <el-button type="primary" plain :loading="starting" @click="startAssessment">开始评测</el-button>
            <small>{{ payload.assessmentPrediction.latestAssessmentSubmittedAt ? `最近提交 ${formatDateTime(payload.assessmentPrediction.latestAssessmentSubmittedAt)}` : '还未提交过评测' }}</small>
          </div>
        </div>
      </div>
    </section>

    <section class="glass-card panel-card">
      <div class="section-title small">周 / 月趋势对比</div>
      <div class="trend-toolbar">
        <el-radio-group v-model="query.periodType" @change="renderCharts">
          <el-radio-button value="WEEK">周视图</el-radio-button>
          <el-radio-button value="MONTH">月视图</el-radio-button>
        </el-radio-group>
        <el-button type="primary" plain @click="handleFilter">筛选</el-button>
      </div>
      <div v-if="hasTrendData" ref="trendRef" class="chart-block"></div>
      <div v-else class="empty-state">
        <el-icon><TrendCharts /></el-icon>
        <strong>暂无趋势数据</strong>
        <p>完成首次评测后，将展示通过率趋势和准确率变化。</p>
        <el-button type="primary" plain :loading="starting" @click="startAssessment">开始首次评测</el-button>
      </div>
    </section>

    <section v-if="hasFactors" class="glass-card panel-card">
      <div class="section-title small">预测因素联动</div>
      <p class="chart-tip">图表展示各因素按权重折算后的贡献，方便你看到是计划、掌握度、测卷还是评测在拉高或拉低通过率。</p>
      <div ref="factorRef" class="chart-block"></div>
      <div class="factor-list">
        <article v-for="item in payload.assessmentPrediction.factorBreakdown || []" :key="item.label" class="factor-item">
          <div>
            <strong>{{ item.label }}</strong>
            <p>{{ item.note }}</p>
          </div>
          <span>{{ item.contribution }} 分 · 权重 {{ item.weight }}%</span>
        </article>
      </div>
    </section>

    <section class="glass-card panel-card">
      <div class="section-title small">评测历史</div>
      <el-table class="desktop-table" :data="payload.assessmentHistory">
        <el-table-column prop="title" label="评测标题" min-width="220" />
        <el-table-column prop="assessmentTypeLabel" label="类型" min-width="120" />
        <el-table-column prop="questionCount" label="题量" min-width="100" />
        <el-table-column prop="predictedPassRate" label="预测通过率" min-width="120" />
        <el-table-column prop="latestAccuracyRate" label="实际准确率" min-width="120" />
        <el-table-column prop="assessmentStatusLabel" label="状态" min-width="100" />
        <el-table-column prop="submittedAt" label="提交时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.submittedAt || row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="160">
          <template #default="{ row }">
            <el-button v-if="row.practicePath" type="primary" plain @click="router.push(row.practicePath)">继续作答</el-button>
            <el-button v-if="row.reportPath" type="warning" plain @click="router.push(row.reportPath)">查看报告</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="mobile-assessment-list">
        <article v-for="row in payload.assessmentHistory" :key="row.id || row.practiceId || row.title" class="mobile-assessment-card">
          <div>
            <strong>{{ row.title }}</strong>
            <span>{{ row.assessmentTypeLabel }} · {{ row.assessmentStatusLabel }}</span>
          </div>
          <div class="mobile-assessment-meta">
            <span>{{ row.questionCount || 0 }} 题</span>
            <span>预测 {{ row.predictedPassRate || 0 }}%</span>
            <span>实际 {{ row.latestAccuracyRate || 0 }}%</span>
          </div>
          <span class="mobile-time">{{ formatDateTime(row.submittedAt || row.createdAt) }}</span>
          <div class="mobile-assessment-actions">
            <el-button v-if="row.practicePath" type="primary" plain @click="router.push(row.practicePath)">继续作答</el-button>
            <el-button v-if="row.reportPath" type="warning" plain @click="router.push(row.reportPath)">查看报告</el-button>
          </div>
        </article>
      </div>
    </section>

    <el-drawer v-model="drilldownVisible" :title="drilldownTitle" size="540px">
      <el-table :data="drilldownRows">
        <el-table-column prop="title" label="评测标题" min-width="220" />
        <el-table-column prop="predictedPassRate" label="预测通过率" min-width="120" />
        <el-table-column prop="latestAccuracyRate" label="实际准确率" min-width="120" />
        <el-table-column prop="submittedAt" label="提交时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.submittedAt || row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<style scoped>
.assessment-page {
  display: grid;
  gap: 18px;
  min-width: 0;
}

.assessment-page > *,
.summary-grid,
.double-grid,
.trend-toolbar,
.factor-list {
  min-width: 0;
  max-width: 100%;
}

.hero-card,
.summary-card,
.panel-card {
  padding: 22px;
  border-radius: 12px;
  min-width: 0;
}

.hero-card,
.assessment-entry,
.meta-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.hero-copy p,
.diagnosis-text,
.chart-tip,
.factor-item p,
.assessment-entry p {
  color: var(--muted);
}

.hero-card {
  min-height: 172px;
  background:
    linear-gradient(135deg, rgba(29, 78, 216, 0.08), rgba(15, 118, 110, 0.04)),
    #fff;
}

.panel-card {
  overflow: hidden;
}

.panel-card:last-of-type {
  overflow-x: auto;
}

.panel-card :deep(.el-table) {
  min-width: 760px;
}

.hero-actions,
.trend-toolbar,
.factor-list,
.hero-meta,
.entry-tags {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-side {
  display: grid;
  justify-items: end;
}

.hero-copy,
.hero-side,
.prediction-card,
.assessment-entry,
.chart-block,
.empty-state {
  min-width: 0;
  max-width: 100%;
}

.hero-copy {
  flex: 1 1 520px;
}

.hero-side {
  flex: 0 0 auto;
}

.hero-score {
  min-width: 158px;
  border-left: 1px solid var(--border);
  padding-left: 28px;
  text-align: right;
}

.hero-side strong,
.summary-card strong {
  color: var(--primary);
}

.hero-side strong {
  display: block;
  font-size: 36px;
  line-height: 1;
}

.hero-side span,
.hero-side small {
  display: block;
  margin-top: 8px;
}

.hero-meta {
  margin: 14px 0 18px;
}

.hero-meta span,
.entry-tags span {
  border: 1px solid #dbeafe;
  border-radius: 8px;
  padding: 6px 10px;
  color: #1d4ed8;
  background: #eff6ff;
  font-size: 13px;
}

.summary-grid,
.double-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.summary-card,
.prediction-card,
.factor-item,
.assessment-entry {
  border: 1px solid var(--border);
  border-radius: 12px;
  background: #fff;
}

.summary-card {
  min-height: 132px;
  display: grid;
  gap: 8px;
  align-content: start;
  padding: 18px;
}

.summary-icon {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: #1d4ed8;
  background: #dbeafe;
  font-size: 18px;
}

.summary-label,
.summary-card small {
  color: var(--muted);
}

.summary-card strong {
  font-size: 28px;
  line-height: 1;
}

.prediction-card,
.assessment-entry {
  padding: 18px;
}

.section-title.small {
  font-size: 21px;
}

.chart-block {
  width: 100%;
  height: 320px;
}

.progress-head,
.meta-row {
  margin: 12px 0 8px;
}

.progress-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.empty-inline,
.empty-state {
  border: 1px dashed #bfdbfe;
  border-radius: 12px;
  color: #64748b;
  background: #f8fbff;
}

.empty-inline {
  margin-top: 14px;
  padding: 12px;
  font-size: 13px;
}

.empty-state {
  min-height: 280px;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 10px;
  text-align: center;
}

.empty-state .el-icon {
  color: #1d4ed8;
  font-size: 34px;
}

.empty-state strong {
  color: var(--text);
  font-size: 18px;
}

.empty-state p {
  margin: 0 0 4px;
  color: var(--muted);
}

.factor-list {
  margin-top: 16px;
}

.factor-item {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  padding: 14px 16px;
  flex: 1 1 260px;
}

.assessment-side {
  display: grid;
  justify-items: end;
  gap: 6px;
  color: var(--primary);
  font-weight: 600;
}

.mobile-assessment-list {
  display: none;
}

.mobile-assessment-card {
  display: grid;
  gap: 10px;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
  background: #fff;
}

.mobile-assessment-card strong,
.mobile-assessment-card span {
  display: block;
}

.mobile-assessment-card span,
.mobile-time {
  color: var(--muted);
  font-size: 13px;
}

.mobile-assessment-meta {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.mobile-assessment-meta span {
  border-radius: 8px;
  padding: 8px;
  color: #1d4ed8;
  background: #eff6ff;
  text-align: center;
}

.mobile-assessment-actions {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.mobile-assessment-actions :deep(.el-button) {
  margin-left: 0;
}

@media (max-width: 760px) {
  .summary-grid,
  .double-grid {
    grid-template-columns: 1fr;
  }

  .panel-card {
    overflow-x: hidden;
  }

  .panel-card:last-of-type {
    overflow-x: auto;
  }

  .chart-block {
    height: 260px;
  }

  .hero-card,
  .assessment-entry,
  .meta-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .hero-copy {
    flex: 0 1 auto;
  }

  .hero-score {
    width: 100%;
    border-left: 0;
    border-top: 1px solid var(--border);
    padding: 18px 0 0;
    text-align: left;
  }

  .assessment-side {
    justify-items: start;
  }

  .desktop-table {
    display: none;
  }

  .mobile-assessment-list {
    display: grid;
    gap: 12px;
  }
}

</style>
