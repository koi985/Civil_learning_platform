<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
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
const payload = ref({
  currentPlan: null,
  latestDiagnosis: null,
  stageSummary: {},
  calibrationStatus: { ready: true, hint: '', submittedPracticeCount: 0, knowledgePointCount: 0 },
  trend: [],
  diagnosisHistory: [],
  historyComparison: {},
  focusKnowledgePoints: [],
  actionSuggestions: [],
  testPaperReports: [],
  teacherFollowUps: [],
  adminFollowUp: null,
  teacherFollowTimeline: [],
  adminFollowTimeline: [],
  collaborationTimeline: [],
  collaborationSummary: {},
  assessmentSummary: {},
  assessmentPrediction: {},
  assessmentEntry: {},
  assessmentHistory: [],
  filters: {}
})
const query = reactive({
  dateRange: [],
  periodType: 'WEEK'
})
const trendRef = ref(null)
let trendChart
const detailVisible = ref(false)
const detailRows = ref([])
const detailTitle = ref('')

function formatDate(value) {
  return value ? String(value).slice(0, 10) : '暂无'
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '暂无'
}

async function loadData() {
  loading.value = true
  try {
    payload.value = await request.get('/api/user-center/growth-diagnosis', {
      params: {
        startDate: query.dateRange?.[0] || undefined,
        endDate: query.dateRange?.[1] || undefined
      }
    })
    query.dateRange = payload.value.filters?.startDate && payload.value.filters?.endDate
      ? [payload.value.filters.startDate, payload.value.filters.endDate]
      : []
    await nextTick()
    await renderChart()
  } finally {
    loading.value = false
  }
}

async function downloadDiagnosis() {
  await request.download('/api/user-center/growth-diagnosis/export', {
    startDate: query.dateRange?.[0] || undefined,
    endDate: query.dateRange?.[1] || undefined
  })
}

function printDiagnosis() {
  window.print()
}

async function handleFilter() {
  await loadData()
}

async function renderChart() {
  const echarts = await loadEcharts()
  if (!trendRef.value) {
    return
  }
  trendChart = trendChart || echarts.init(trendRef.value)
  const groupedTrend = groupedTrendData.value
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['成长活力', '计划完成率', '掌握度'] },
    xAxis: { type: 'category', data: groupedTrend.map((item) => item.label) },
    yAxis: { type: 'value', max: 100 },
    series: [
      { name: '成长活力', type: 'line', smooth: true, data: groupedTrend.map((item) => item.vitalityScore), itemStyle: { color: '#1d4ed8' } },
      { name: '计划完成率', type: 'bar', data: groupedTrend.map((item) => item.planCompletionRate), itemStyle: { color: '#0f766e' } },
      { name: '掌握度', type: 'line', smooth: true, data: groupedTrend.map((item) => item.knowledgeMasteryRate), itemStyle: { color: '#f97316' } }
    ]
  })
  trendChart.off('click')
  trendChart.on('click', ({ dataIndex }) => {
    const row = groupedTrend[dataIndex]
    if (!row) return
    detailTitle.value = `${row.label} 详情`
    detailRows.value = row.records || []
    detailVisible.value = true
  })
}

const groupedTrendData = computed(() => {
  const source = payload.value.diagnosisHistory || []
  const map = new Map()
  source.forEach((item) => {
    const date = String(item.diagnosisDate || '')
    const key = query.periodType === 'MONTH' ? date.slice(0, 7) : `${date.slice(0, 4)}-W${getWeekOfYear(date)}`
    const label = query.periodType === 'MONTH' ? key : `${date.slice(5, 7)}月第${getWeekOfYear(date, true)}周`
    if (!map.has(key)) {
      map.set(key, { key, label, records: [], vitalityScore: 0, planCompletionRate: 0, knowledgeMasteryRate: 0 })
    }
    const bucket = map.get(key)
    bucket.records.push(item)
    bucket.vitalityScore += Number(item.vitalityScore || 0)
    bucket.planCompletionRate += Number(item.planCompletionRate || 0)
    bucket.knowledgeMasteryRate += Number(item.knowledgeMasteryRate || 0)
  })
  return Array.from(map.values()).map((item) => ({
    ...item,
    vitalityScore: Number((item.vitalityScore / item.records.length).toFixed(2)),
    planCompletionRate: Number((item.planCompletionRate / item.records.length).toFixed(2)),
    knowledgeMasteryRate: Number((item.knowledgeMasteryRate / item.records.length).toFixed(2))
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

onMounted(loadData)
</script>

<template>
  <div class="diagnosis-page" v-loading="loading">
    <section class="glass-card hero-card">
      <div>
        <div class="section-title">阶段诊断</div>
        <p v-if="payload.currentPlan">围绕 {{ payload.currentPlan.planName }} 自动汇总计划执行、知识掌握和测卷结果，帮助你判断当前节奏是否健康。</p>
        <p v-else>当前还没有激活中的成长计划，先去创建计划后再查看阶段诊断。</p>
      </div>
      <div class="hero-actions">
        <div class="tag-row">
          <el-date-picker v-model="query.dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
          <el-radio-group v-model="query.periodType" @change="renderChart">
            <el-radio-button value="WEEK">周视图</el-radio-button>
            <el-radio-button value="MONTH">月视图</el-radio-button>
          </el-radio-group>
          <el-button type="primary" @click="handleFilter">筛选</el-button>
          <el-button type="primary" plain @click="downloadDiagnosis">导出诊断</el-button>
          <el-button type="warning" plain @click="router.push('/user/assessment')">独立评测</el-button>
          <el-button plain @click="printDiagnosis">打印视图</el-button>
        </div>
        <div v-if="payload.latestDiagnosis" class="hero-side">
          <strong>{{ payload.latestDiagnosis.vitalityScore || 0 }}</strong>
          <span>{{ payload.latestDiagnosis.diagnosisStageLabel }}</span>
          <small>{{ formatDate(payload.latestDiagnosis.diagnosisDate) }}</small>
        </div>
      </div>
    </section>

    <el-alert
      v-if="payload.calibrationStatus && !payload.calibrationStatus.ready && payload.calibrationStatus.hint"
      :title="payload.calibrationStatus.hint"
      type="warning"
      show-icon
      :closable="false"
      style="margin-top: 16px;"
    />

    <section class="summary-grid">
      <div class="glass-card summary-card"><span>已完成任务</span><strong>{{ payload.stageSummary.completedTaskCount || 0 }}/{{ payload.stageSummary.totalTaskCount || 0 }}</strong></div>
      <div class="glass-card summary-card"><span>薄弱知识点</span><strong>{{ payload.stageSummary.weakKnowledgeCount || 0 }}</strong></div>
      <div class="glass-card summary-card"><span>已完成测卷</span><strong>{{ payload.stageSummary.completedTestPaperCount || 0 }}</strong></div>
      <div class="glass-card summary-card"><span>最佳测卷准确率</span><strong>{{ payload.stageSummary.bestTestPaperAccuracy || 0 }}%</strong></div>
    </section>

    <section class="summary-grid">
      <div class="glass-card summary-card"><span>教师跟进记录</span><strong>{{ payload.collaborationSummary.teacherFollowCount || 0 }}</strong></div>
      <div class="glass-card summary-card"><span>教师协同班级</span><strong>{{ payload.collaborationSummary.teacherClassroomCount || 0 }}</strong></div>
      <div class="glass-card summary-card"><span>后台跟进状态</span><strong>{{ payload.collaborationSummary.hasAdminFollow ? '已介入' : '暂无' }}</strong></div>
      <div class="glass-card summary-card"><span>待完成跟进</span><strong>{{ payload.collaborationSummary.openFollowCount || 0 }}</strong></div>
    </section>

    <section class="summary-grid">
      <div class="glass-card summary-card"><span>预测通过率</span><strong>{{ payload.assessmentPrediction.predictedPassRate || 0 }}%</strong></div>
      <div class="glass-card summary-card"><span>预测置信度</span><strong>{{ payload.assessmentPrediction.predictionConfidence || 0 }}%</strong></div>
      <div class="glass-card summary-card"><span>评测次数</span><strong>{{ payload.assessmentSummary.sessionCount || 0 }}</strong></div>
      <div class="glass-card summary-card"><span>最近评测</span><strong>{{ payload.assessmentSummary.latestAssessmentAccuracy || 0 }}%</strong></div>
    </section>

    <section class="glass-card panel-card">
      <div class="section-title small">成长趋势</div>
      <p class="chart-tip">支持按周 / 月切换，点击图表节点可查看该周期内的诊断详情。</p>
      <div ref="trendRef" style="height: 340px"></div>
    </section>

    <section class="double-grid">
      <div class="glass-card panel-card">
        <div class="section-title small">周期对比</div>
        <div class="comparison-card" v-if="payload.historyComparison">
          <div class="meta-row">
            <span>本期诊断</span>
            <strong>{{ payload.historyComparison.currentStageLabel || '暂无' }}</strong>
          </div>
          <div class="meta-row">
            <span>对比基线</span>
            <strong>{{ payload.historyComparison.baselineStageLabel || '暂无历史周期' }}</strong>
          </div>
          <div class="meta-row">
            <span>整体变化</span>
            <el-tag :type="payload.historyComparison.overallDirection === 'UP' ? 'success' : payload.historyComparison.overallDirection === 'DOWN' ? 'danger' : 'info'">
              {{ payload.historyComparison.overallDirectionLabel }}
            </el-tag>
          </div>
          <div class="delta-grid">
            <div><span>活力变化</span><strong>{{ payload.historyComparison.vitalityDelta || 0 }}</strong></div>
            <div><span>完成率变化</span><strong>{{ payload.historyComparison.completionDelta || 0 }}</strong></div>
            <div><span>掌握度变化</span><strong>{{ payload.historyComparison.masteryDelta || 0 }}</strong></div>
            <div><span>测卷变化</span><strong>{{ payload.historyComparison.testPaperDelta || 0 }}</strong></div>
          </div>
        </div>
      </div>

      <div class="glass-card panel-card">
        <div class="section-title small">协同跟进</div>
        <div class="collaboration-list">
          <article v-if="payload.adminFollowUp" class="report-item">
            <div>
              <strong>后台运营跟进</strong>
              <p>{{ payload.adminFollowUp.remark || '当前已进入后台成长跟进视图。' }}</p>
            </div>
            <div class="knowledge-side">
              <span>{{ payload.adminFollowUp.followStatusLabel }}</span>
              <small>{{ formatDateTime(payload.adminFollowUp.updatedAt) }}</small>
            </div>
          </article>
          <article v-for="item in payload.teacherFollowUps" :key="`${item.classroomId}-${item.updatedAt}`" class="report-item">
            <div>
              <strong>{{ item.classroomName }} · {{ item.teacherName }}</strong>
              <p>{{ item.remark || '教师已纳入本阶段协同跟进。' }}</p>
            </div>
            <div class="knowledge-side">
              <span>{{ item.followStatusLabel }}</span>
              <small>{{ formatDateTime(item.nextFollowUpAt || item.updatedAt) }}</small>
            </div>
          </article>
          <el-empty v-if="!payload.adminFollowUp && !payload.teacherFollowUps?.length" description="当前还没有教师或后台协同跟进记录" />
        </div>
      </div>
    </section>

    <section class="double-grid">
      <div class="glass-card panel-card">
        <div class="section-title small">诊断结论</div>
        <div v-if="payload.latestDiagnosis" class="diagnosis-card">
          <div class="meta-row">
            <span>阶段判定</span>
            <el-tag type="primary">{{ payload.latestDiagnosis.diagnosisStageLabel }}</el-tag>
          </div>
          <div class="meta-row">
            <span>计划完成率</span>
            <strong>{{ payload.latestDiagnosis.planCompletionRate || 0 }}%</strong>
          </div>
          <div class="meta-row">
            <span>知识掌握度</span>
            <strong>{{ payload.latestDiagnosis.knowledgeMasteryRate || 0 }}%</strong>
          </div>
          <div class="meta-row">
            <span>测卷平均准确率</span>
            <strong>{{ payload.latestDiagnosis.testPaperAverageScore || 0 }}%</strong>
          </div>
          <div class="meta-row">
            <span>预测通过率</span>
            <strong>{{ payload.latestDiagnosis.predictedPassRate || payload.assessmentPrediction.predictedPassRate || 0 }}%</strong>
          </div>
          <p class="diagnosis-text">{{ payload.latestDiagnosis.recommendationText }}</p>
          <div class="tag-row">
            <el-tag v-for="item in payload.latestDiagnosis.weakKnowledgePoints || []" :key="item" type="warning" effect="plain">{{ item }}</el-tag>
          </div>
        </div>
        <el-empty v-else description="当前暂无诊断结果" />
      </div>

      <div class="glass-card panel-card">
        <div class="section-title small">下一步动作</div>
        <div class="action-list" v-if="payload.actionSuggestions?.length">
          <article v-for="item in payload.actionSuggestions" :key="item.title" class="action-item">
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.content }}</p>
            </div>
            <el-button type="primary" plain @click="router.push(item.path)">{{ item.actionText }}</el-button>
          </article>
        </div>
        <el-empty v-else description="当前暂无阶段建议" />
      </div>
    </section>

    <section class="double-grid">
      <div class="glass-card panel-card">
        <div class="section-title small">重点知识点</div>
        <div class="knowledge-list" v-if="payload.focusKnowledgePoints?.length">
          <article v-for="item in payload.focusKnowledgePoints" :key="item.id" class="knowledge-item">
            <div>
              <strong>{{ item.knowledgePoint }}</strong>
              <p>{{ item.subjectName }} · {{ item.masteryLevelLabel }}</p>
            </div>
            <div class="knowledge-side">
              <span>{{ item.masteryScore }} 分</span>
              <el-button text type="primary" @click="router.push({ path: '/user/test-paper', query: { knowledge: item.knowledgePoint, subject: item.subjectName } })">生成测卷</el-button>
            </div>
          </article>
        </div>
        <el-empty v-else description="当前暂无重点知识点数据" />
      </div>

      <div class="glass-card panel-card">
        <div class="section-title small">最近测卷报告</div>
        <div class="report-list" v-if="payload.testPaperReports?.length">
          <article v-for="item in payload.testPaperReports" :key="item.id" class="report-item">
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.recommendationReason }}</p>
              <div class="meta-row">
                <span>{{ item.subjectName || '综合' }}</span>
                <span>{{ item.latestAccuracyRate || 0 }}%</span>
                <span>{{ formatDateTime(item.latestSubmittedAt) }}</span>
              </div>
            </div>
            <el-button type="warning" plain @click="router.push(item.reportPath)">查看报告</el-button>
          </article>
        </div>
        <el-empty v-else description="当前还没有可回看的测卷报告" />
      </div>
    </section>

    <section class="double-grid">
      <div class="glass-card panel-card">
        <div class="section-title small">协同跟进时间线</div>
        <div class="timeline-list" v-if="payload.collaborationTimeline?.length">
          <article
            v-for="item in payload.collaborationTimeline"
            :key="`${item.sourceType}-${item.createdAt}-${item.classroomId || 'admin'}`"
            class="timeline-item"
          >
            <div>
              <div class="tag-row">
                <el-tag size="small" :type="item.sourceType === 'TEACHER' ? 'success' : 'primary'">{{ item.sourceTypeLabel }}</el-tag>
                <el-tag size="small" effect="plain">{{ item.actionTypeLabel }}</el-tag>
                <el-tag size="small" effect="plain" type="warning">{{ item.priorityLevelLabel }}</el-tag>
              </div>
              <strong>{{ item.classroomName || '后台成长运营' }}</strong>
              <p>{{ item.remark || '本次已记录协同跟进动作。' }}</p>
              <div class="meta-row">
                <span>{{ item.followStatusLabel }}</span>
                <span>处理人：{{ item.operatorName || '系统' }}</span>
                <span>记录于 {{ formatDateTime(item.createdAt) }}</span>
              </div>
            </div>
            <div class="knowledge-side" v-if="item.nextFollowUpAt">
              <span>下次回访</span>
              <small>{{ formatDateTime(item.nextFollowUpAt) }}</small>
            </div>
          </article>
        </div>
        <el-empty v-else description="当前还没有协同跟进轨迹" />
      </div>

      <div class="glass-card panel-card">
        <div class="section-title small">跟进概览</div>
        <div class="diagnosis-card">
          <div class="meta-row">
            <span>最近协同时间</span>
            <strong>{{ formatDateTime(payload.collaborationSummary.latestFollowAt) }}</strong>
          </div>
          <div class="meta-row">
            <span>最近待回访时间</span>
            <strong>{{ formatDateTime(payload.collaborationSummary.nextFollowUpAt) }}</strong>
          </div>
          <div class="meta-row">
            <span>教师轨迹数</span>
            <strong>{{ payload.teacherFollowTimeline?.length || 0 }}</strong>
          </div>
          <div class="meta-row">
            <span>后台轨迹数</span>
            <strong>{{ payload.adminFollowTimeline?.length || 0 }}</strong>
          </div>
          <p class="diagnosis-text">
            教师与后台的阶段跟进记录会同步沉淀在这里，方便你回看每次被纳入重点关注的原因、处理动作和下次回访安排。
          </p>
        </div>
      </div>
    </section>

    <section class="glass-card panel-card">
      <div class="section-title small">诊断历史</div>
      <el-table :data="payload.diagnosisHistory">
        <el-table-column prop="diagnosisDate" label="诊断日期" min-width="120" />
        <el-table-column prop="diagnosisStageLabel" label="阶段判定" min-width="120" />
        <el-table-column prop="vitalityScore" label="成长活力" min-width="100" />
        <el-table-column prop="planCompletionRate" label="完成率" min-width="100" />
        <el-table-column prop="knowledgeMasteryRate" label="掌握度" min-width="100" />
        <el-table-column prop="testPaperAverageScore" label="测卷准确率" min-width="120" />
        <el-table-column prop="predictedPassRate" label="预测通过率" min-width="120" />
        <el-table-column label="薄弱知识点" min-width="220">
          <template #default="{ row }">{{ (row.weakKnowledgePoints || []).join(' / ') || '暂无' }}</template>
        </el-table-column>
      </el-table>
    </section>

    <section class="glass-card panel-card">
      <div class="section-title small">独立评测入口</div>
      <div class="assessment-entry">
        <div>
          <strong>{{ payload.assessmentEntry.title || '独立入学评测' }}</strong>
          <p>{{ payload.assessmentEntry.description || `${payload.assessmentEntry.questionCount || 20} 题独立评测入口，先校准通过率预测。` }}</p>
        </div>
        <div class="assessment-side">
          <span>{{ payload.assessmentEntry.questionCount || 20 }} 题</span>
          <small>{{ payload.assessmentPrediction.predictionStageLabel || '未生成预测' }}</small>
        </div>
      </div>
      <div v-if="payload.assessmentPrediction.abilityCalibration" class="diagnosis-card">
        <div class="meta-row">
          <span>Rasch 能力校准</span>
          <el-tag :type="payload.assessmentPrediction.abilityCalibration.applied ? 'success' : 'info'">
            {{ payload.assessmentPrediction.abilityCalibration.applied ? '已纳入校准' : '仅展示参考' }}
          </el-tag>
        </div>
        <div class="meta-row">
          <span>能力分 / 置信度</span>
          <strong>{{ payload.assessmentPrediction.abilityCalibration.abilityScore || 0 }} / {{ payload.assessmentPrediction.abilityCalibration.abilityConfidence || 0 }}%</strong>
        </div>
        <div class="meta-row" v-if="payload.assessmentPrediction.abilityCalibration.applied">
          <span>校准幅度</span>
          <strong>{{ payload.assessmentPrediction.abilityCalibration.delta || 0 }} 分</strong>
        </div>
        <p class="diagnosis-text">{{ payload.assessmentPrediction.abilityCalibration.hint }}</p>
      </div>
      <div class="factor-list" v-if="payload.assessmentPrediction.factorBreakdown?.length">
        <article v-for="item in payload.assessmentPrediction.factorBreakdown" :key="item.label" class="factor-item">
          <div>
            <strong>{{ item.label }}</strong>
            <p>{{ item.note }}</p>
          </div>
          <span>{{ item.contribution }} 分 · 权重 {{ item.weight }}%</span>
        </article>
      </div>
    </section>

    <section class="glass-card panel-card print-summary">
      <div class="section-title small">打印摘要</div>
      <div class="print-grid">
        <div><span>最新诊断</span><strong>{{ payload.latestDiagnosis?.diagnosisStageLabel || '暂无' }}</strong></div>
        <div><span>预测通过率</span><strong>{{ payload.assessmentPrediction.predictedPassRate || 0 }}%</strong></div>
        <div><span>最近评测</span><strong>{{ payload.assessmentSummary.latestAssessmentAccuracy || 0 }}%</strong></div>
        <div><span>下次动作</span><strong>{{ payload.assessmentEntry.actionText || '立即评测' }}</strong></div>
      </div>
    </section>

    <el-drawer v-model="detailVisible" :title="detailTitle" size="520px">
      <el-table :data="detailRows">
        <el-table-column prop="diagnosisDate" label="诊断日期" min-width="120" />
        <el-table-column prop="diagnosisStageLabel" label="阶段判定" min-width="120" />
        <el-table-column prop="vitalityScore" label="成长活力" min-width="100" />
        <el-table-column prop="planCompletionRate" label="完成率" min-width="100" />
        <el-table-column prop="knowledgeMasteryRate" label="掌握度" min-width="100" />
      </el-table>
    </el-drawer>
  </div>
</template>

<style scoped>
.diagnosis-page {
  display: grid;
  gap: 20px;
}

.hero-card,
.summary-card,
.panel-card {
  padding: 24px;
}

.hero-card,
.meta-row,
.action-item,
.knowledge-item,
.report-item,
.timeline-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.hero-card p,
.chart-tip,
.action-item p,
.knowledge-item p,
.report-item p,
.diagnosis-text,
.meta-row {
  color: var(--muted);
}

.hero-actions {
  display: grid;
  gap: 12px;
  justify-items: end;
}

.hero-side {
  display: grid;
  justify-items: end;
}

.hero-side strong,
.summary-card strong {
  font-size: 28px;
  color: var(--primary);
}

.summary-grid,
.double-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.summary-card,
.diagnosis-card,
.action-item,
.knowledge-item,
.report-item,
.timeline-item {
  border: 1px solid var(--border);
  border-radius: 16px;
  background: #fff;
}

.section-title.small {
  font-size: 22px;
}

.diagnosis-card,
.action-list,
.knowledge-list,
.report-list,
.timeline-list {
  display: grid;
  gap: 12px;
}

.diagnosis-card {
  padding: 16px;
}

.action-item,
.knowledge-item,
.report-item,
.timeline-item {
  padding: 16px;
}

.tag-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.comparison-card,
.collaboration-list {
  display: grid;
  gap: 12px;
}

.delta-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.delta-grid div {
  border: 1px solid var(--border);
  border-radius: 14px;
  background: #fff;
  padding: 14px;
}

.delta-grid span {
  color: var(--muted);
}

.delta-grid strong {
  display: block;
  margin-top: 6px;
  color: var(--primary);
}

.knowledge-side {
  display: grid;
  justify-items: end;
  gap: 6px;
  color: var(--primary);
  font-weight: 600;
}

.assessment-entry {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: #fff;
  padding: 16px;
  margin-bottom: 16px;
}

.assessment-entry p,
.print-grid span,
.factor-item p {
  color: var(--muted);
}

.assessment-side {
  display: grid;
  justify-items: end;
  gap: 6px;
  color: var(--primary);
  font-weight: 600;
}

.factor-list {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.factor-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: #fff;
  padding: 14px 16px;
}

.print-summary {
  display: none;
}

.print-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
}

.print-grid div {
  border: 1px solid var(--border);
  border-radius: 14px;
  background: #fff;
  padding: 14px;
}

.print-grid strong {
  display: block;
  margin-top: 6px;
  color: var(--primary);
}

.timeline-item strong {
  display: block;
  margin-top: 8px;
}

@media print {
  .hero-actions .el-button,
  .hero-actions :deep(.el-date-editor),
  .factor-list,
  .assessment-entry .el-button,
  .el-drawer,
  .action-list .el-button {
    display: none !important;
  }

  .diagnosis-page {
    gap: 12px;
  }

  .print-summary {
    display: block;
  }
}
</style>
