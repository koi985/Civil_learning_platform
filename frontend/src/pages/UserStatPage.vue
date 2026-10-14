<script setup>
import { nextTick, onMounted, ref } from 'vue'
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
const stats = ref({
  last7Days: [],
  subjectSummary: [],
  totals: {},
  recentLearningTracks: [],
  continueLearningCourses: [],
  learningDiagnosis: []
})
const trendRef = ref(null)
const subjectRef = ref(null)
let trendChart
let subjectChart

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '暂无'
}

function formatMinutes(value) {
  return `${Math.max(1, Math.floor(Number(value || 0) / 60))} 分钟`
}

function formatHours(value) {
  return `${(Number(value || 0) / 3600).toFixed(1)} 小时`
}

async function loadData() {
  loading.value = true
  try {
    stats.value = await request.get('/api/user-center/statistics')
    await nextTick()
    await renderCharts()
  } finally {
    loading.value = false
  }
}

async function renderCharts() {
  const echarts = await loadEcharts()
  if (trendRef.value) {
    trendChart = trendChart || echarts.init(trendRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['学习时长(分钟)', '练习次数', '模考次数'] },
      xAxis: { type: 'category', data: stats.value.last7Days.map((item) => item.date) },
      yAxis: [{ type: 'value' }, { type: 'value' }],
      series: [
        { name: '学习时长(分钟)', type: 'bar', data: stats.value.last7Days.map((item) => Math.round(Number(item.studySeconds || 0) / 60)), itemStyle: { color: '#1d4ed8' } },
        { name: '练习次数', type: 'line', yAxisIndex: 1, data: stats.value.last7Days.map((item) => item.practiceCount), smooth: true },
        { name: '模考次数', type: 'line', yAxisIndex: 1, data: stats.value.last7Days.map((item) => item.examCount), smooth: true, itemStyle: { color: '#f97316' } }
      ]
    })
  }
  if (subjectRef.value) {
    subjectChart = subjectChart || echarts.init(subjectRef.value)
    subjectChart.setOption({
      tooltip: {},
      xAxis: { type: 'category', data: stats.value.subjectSummary.map((item) => item.subjectName || '未分类') },
      yAxis: { type: 'value', max: 100 },
      series: [
        { name: '掌握度', type: 'bar', data: stats.value.subjectSummary.map((item) => Number(item.masteryScore || 0)), itemStyle: { color: '#0f766e' } },
        { name: '正确率', type: 'line', data: stats.value.subjectSummary.map((item) => Number(item.practiceAccuracy || 0)), itemStyle: { color: '#f97316' } }
      ]
    })
  }
}

onMounted(loadData)
</script>

<template>
  <div class="stat-page" v-loading="loading">
    <section class="glass-card summary-card">
      <div class="summary-grid">
        <div><span>累计练习题量</span><strong>{{ stats.totals.practiceQuestions || 0 }}</strong></div>
        <div><span>累计做对题量</span><strong>{{ stats.totals.practiceCorrect || 0 }}</strong></div>
        <div><span>最佳模考分</span><strong>{{ stats.totals.examBestScore || 0 }}</strong></div>
        <div><span>累计学习时长</span><strong>{{ formatHours(stats.totals.totalStudySeconds) }}</strong></div>
        <div><span>激活课程数</span><strong>{{ stats.totals.activeCourseCount || 0 }}</strong></div>
        <div><span>完成章节数</span><strong>{{ stats.totals.completedChapterCount || 0 }}</strong></div>
      </div>
    </section>

    <section class="glass-card chart-card">
      <div class="section-title">近 7 日学习趋势</div>
      <div ref="trendRef" style="height: 360px"></div>
    </section>

    <section class="glass-card chart-card">
      <div class="section-title">科目掌握度</div>
      <div ref="subjectRef" style="height: 360px"></div>
    </section>

    <section class="double-grid">
      <div class="glass-card panel-card">
        <div class="section-title">继续学习</div>
        <div class="continue-list" v-if="stats.continueLearningCourses?.length">
          <article v-for="item in stats.continueLearningCourses" :key="item.courseId" class="continue-item">
            <img :src="item.coverUrl" alt="" />
            <div class="continue-main">
              <h3>{{ item.title }}</h3>
              <p>{{ item.teacherName }} · {{ item.subjectName }}</p>
              <el-progress :percentage="Number(item.progressPercent || 0)" :stroke-width="8" />
              <div class="meta-line">
                <span>已完成 {{ item.completedChapterCount || 0 }} 章</span>
                <span>{{ formatMinutes(item.totalStudySeconds) }}</span>
              </div>
              <div class="meta-line">
                <span>最近学习</span>
                <span>{{ formatDateTime(item.lastStudyAt) }}</span>
              </div>
            </div>
            <el-button type="primary" plain @click="router.push(item.path)">继续</el-button>
          </article>
        </div>
        <el-empty v-else description="当前没有待继续的课程" />
      </div>

      <div class="glass-card panel-card">
        <div class="section-title">最近学习轨迹</div>
        <div class="track-list" v-if="stats.recentLearningTracks?.length">
          <article v-for="item in stats.recentLearningTracks" :key="`${item.courseId}-${item.chapterId}`" class="track-item" @click="router.push(item.path)">
            <div>
              <h3>{{ item.chapterTitle }}</h3>
              <p>{{ item.courseTitle }}</p>
            </div>
            <div class="track-side">
              <el-tag size="small" :type="item.completed ? 'success' : 'warning'">{{ item.completed ? '已完成' : '进行中' }}</el-tag>
              <span>{{ Number(item.progressPercent || 0) }}%</span>
              <span>{{ formatDateTime(item.lastStudyAt) }}</span>
            </div>
          </article>
        </div>
        <el-empty v-else description="还没有课程学习轨迹" />
      </div>
    </section>

    <section class="glass-card panel-card">
      <div class="section-title">学习建议</div>
      <div class="diagnosis-grid">
        <article v-for="item in stats.learningDiagnosis" :key="item.title" class="diagnosis-item">
          <h3>{{ item.title }}</h3>
          <p>{{ item.content }}</p>
          <el-button text type="primary" @click="router.push(item.path)">{{ item.actionText }}</el-button>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.stat-page {
  display: grid;
  gap: 20px;
}

.summary-card,
.chart-card,
.panel-card {
  padding: 24px;
}

.summary-grid,
.diagnosis-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
}

.summary-grid div,
.diagnosis-item,
.continue-item,
.track-item {
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 16px;
  background: #fff;
}

.summary-grid span,
.continue-item p,
.track-item p {
  color: var(--muted);
}

.summary-grid strong {
  display: block;
  margin-top: 8px;
  font-size: 26px;
  color: var(--primary);
}

.double-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 20px;
}

.continue-list,
.track-list {
  display: grid;
  gap: 14px;
}

.continue-item {
  display: grid;
  grid-template-columns: 92px 1fr auto;
  gap: 14px;
  align-items: center;
}

.continue-item img {
  width: 92px;
  height: 92px;
  border-radius: 14px;
  object-fit: cover;
}

.continue-main {
  display: grid;
  gap: 8px;
}

.meta-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  color: var(--muted);
  font-size: 13px;
}

.track-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  cursor: pointer;
}

.track-side {
  display: grid;
  gap: 8px;
  justify-items: end;
  color: var(--muted);
  font-size: 13px;
}

.diagnosis-item {
  display: grid;
  gap: 10px;
}
</style>
