<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Bell,
  ChatDotRound,
  Connection,
  DataAnalysis,
  Document,
  Money,
  Reading,
  ShoppingCart,
  Tickets,
  TrendCharts,
  User
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
const stats = ref({
  todayOrders: 0,
  newUsers: 0,
  newTeacherApplies: 0,
  classroomCount: 0,
  courseCount: 0,
  practiceQuestionCount: 0,
  trends: []
})
const recentOrders = ref([])
const pendingTeachers = ref([])
const chartRef = ref(null)
let chartInstance

const resourceRows = computed(() => [
  { name: '课程', value: toNumber(stats.value.courseCount), color: '#2563eb' },
  { name: '资讯', value: trendValue('资讯'), color: '#0f766e' },
  { name: '班级', value: toNumber(stats.value.classroomCount), color: '#f97316' }
])

const totalResources = computed(() => {
  return resourceRows.value.reduce((sum, item) => sum + item.value, 0) + toNumber(stats.value.practiceQuestionCount)
})

const metricCards = computed(() => [
  {
    label: '今日订单',
    value: stats.value.todayOrders,
    icon: ShoppingCart,
    tone: 'blue',
    note: '待支付订单可在订单管理跟进',
    route: '/admin/order'
  },
  {
    label: '新增用户',
    value: stats.value.newUsers,
    icon: User,
    tone: 'green',
    note: '今日注册用户沉淀',
    route: '/admin/user'
  },
  {
    label: '教师申请',
    value: stats.value.newTeacherApplies,
    icon: Connection,
    tone: 'orange',
    note: '待审核申请',
    route: '/admin/teacher/audit'
  },
  {
    label: '班级数量',
    value: stats.value.classroomCount,
    icon: DataAnalysis,
    tone: 'cyan',
    note: '班级运营存量',
    route: '/admin/stats'
  },
  {
    label: '课程数量',
    value: stats.value.courseCount,
    icon: Reading,
    tone: 'purple',
    note: '已建课程资源',
    route: '/admin/course'
  },
  {
    label: '题库数量',
    value: stats.value.practiceQuestionCount,
    icon: Tickets,
    tone: 'indigo',
    note: '题目资产总量',
    route: '/admin/question'
  }
])

const todoItems = computed(() => [
  {
    label: '教师审核',
    count: stats.value.newTeacherApplies,
    desc: '资质、证书与授课说明待确认',
    icon: Connection,
    route: '/admin/teacher/audit',
    type: stats.value.newTeacherApplies > 0 ? 'warning' : 'success'
  },
  {
    label: '订单处理',
    count: stats.value.todayOrders,
    desc: '核对今日新增订单与支付状态',
    icon: ShoppingCart,
    route: '/admin/order',
    type: stats.value.todayOrders > 0 ? 'primary' : 'info'
  },
  {
    label: '评价治理',
    count: 0,
    desc: '查看课程与资讯评价内容',
    icon: ChatDotRound,
    route: '/admin/evaluation',
    type: 'info'
  },
  {
    label: '公告发布',
    count: 0,
    desc: '维护平台通知和运营公告',
    icon: Bell,
    route: '/admin/notice',
    type: 'info'
  }
])

const quickActions = [
  { label: '新建课程', route: '/admin/course', icon: Reading },
  { label: '维护题库', route: '/admin/question', icon: Tickets },
  { label: '订单核对', route: '/admin/order', icon: Money },
  { label: '发布资讯', route: '/admin/news', icon: Document }
]

async function loadData() {
  loading.value = true
  try {
    const [dashboardResult, ordersResult, teachersResult] = await Promise.allSettled([
      request.get('/api/admin/dashboard'),
      request.get('/api/admin/orders', { params: { pageNo: 1, pageSize: 5 } }),
      request.get('/api/admin/teacher-audits', { params: { pageNo: 1, pageSize: 5, auditStatus: 'PENDING' } })
    ])

    if (dashboardResult.status === 'fulfilled') {
      stats.value = { ...stats.value, ...dashboardResult.value }
    }
    if (ordersResult.status === 'fulfilled') {
      recentOrders.value = ordersResult.value.records || []
    }
    if (teachersResult.status === 'fulfilled') {
      pendingTeachers.value = teachersResult.value.records || []
    }

    await nextTick()
    await renderChart()
  } finally {
    loading.value = false
  }
}

async function renderChart() {
  const echarts = await loadEcharts()
  if (!chartRef.value) return
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value)
  }
  chartInstance.setOption({
    color: resourceRows.value.map((item) => item.color),
    grid: { top: 28, left: 44, right: 20, bottom: 36 },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      valueFormatter: (value) => `${formatNumber(value)} 个`
    },
    xAxis: {
      type: 'category',
      data: resourceRows.value.map((item) => item.name),
      axisLine: { lineStyle: { color: '#cbd5e1' } },
      axisTick: { show: false },
      axisLabel: { color: '#64748b', fontWeight: 600 }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#e2e8f0' } },
      axisLabel: { color: '#64748b' }
    },
    series: [
      {
        type: 'bar',
        barWidth: 42,
        data: resourceRows.value.map((item) => ({
          value: item.value,
          itemStyle: { color: item.color, borderRadius: [8, 8, 0, 0] }
        }))
      }
    ]
  })
}

function trendValue(name) {
  const item = stats.value.trends?.find((trend) => trend.name === name)
  return toNumber(item?.value)
}

function toNumber(value) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : 0
}

function formatNumber(value) {
  return toNumber(value).toLocaleString('zh-CN')
}

function percent(value) {
  if (!totalResources.value) return 0
  return Math.round((toNumber(value) / totalResources.value) * 100)
}

function statusLabel(value) {
  return value || '-'
}

function resizeChart() {
  chartInstance?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  chartInstance?.dispose()
  chartInstance = null
})
</script>

<template>
  <div class="dashboard-page" v-loading="loading">
    <section class="dashboard-hero">
      <div>
        <h1>运营仪表盘</h1>
        <p>聚合订单、用户、教师审核和资源资产，优先暴露今天需要处理的运营动作。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" :icon="TrendCharts" @click="router.push('/admin/stats')">查看统计</el-button>
        <el-button :icon="Bell" @click="router.push('/admin/notice')">发布公告</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <button
        v-for="item in metricCards"
        :key="item.label"
        class="metric-card"
        :class="`tone-${item.tone}`"
        type="button"
        @click="router.push(item.route)"
      >
        <span class="metric-icon"><el-icon><component :is="item.icon" /></el-icon></span>
        <span class="metric-label">{{ item.label }}</span>
        <strong>{{ formatNumber(item.value) }}</strong>
        <span class="metric-note">{{ item.note }}</span>
      </button>
    </section>

    <section class="workbench-grid">
      <div class="panel todo-panel">
        <div class="panel-head">
          <div>
            <h2>运营待办</h2>
            <p>优先处理会影响课程交付和交易闭环的事项。</p>
          </div>
        </div>
        <div class="todo-list">
          <button v-for="item in todoItems" :key="item.label" class="todo-row" type="button" @click="router.push(item.route)">
            <span class="todo-icon"><el-icon><component :is="item.icon" /></el-icon></span>
            <span class="todo-content">
              <strong>{{ item.label }}</strong>
              <small>{{ item.desc }}</small>
            </span>
            <el-tag :type="item.type" round>{{ formatNumber(item.count) }}</el-tag>
          </button>
        </div>
      </div>

      <div class="panel question-panel">
        <div class="panel-head">
          <div>
            <h2>题库资产</h2>
            <p>题库单独展示，避免挤压课程、资讯和班级图表比例。</p>
          </div>
        </div>
        <div class="question-total">{{ formatNumber(stats.practiceQuestionCount) }}</div>
        <div class="question-subtitle">占全部资源 {{ percent(stats.practiceQuestionCount) }}%</div>
        <div class="resource-bars">
          <div v-for="item in [...resourceRows, { name: '题库', value: toNumber(stats.practiceQuestionCount), color: '#4f46e5' }]" :key="item.name" class="resource-row">
            <span>{{ item.name }}</span>
            <div class="resource-track">
              <i :style="{ width: `${percent(item.value)}%`, background: item.color }"></i>
            </div>
            <strong>{{ percent(item.value) }}%</strong>
          </div>
        </div>
      </div>
    </section>

    <section class="analytics-grid">
      <div class="panel chart-panel">
        <div class="panel-head">
          <div>
            <h2>资源结构概览</h2>
            <p>聚焦课程、资讯和班级，题库在右侧独立分析。</p>
          </div>
        </div>
        <div ref="chartRef" class="chart-box"></div>
      </div>

      <div class="panel quick-panel">
        <div class="panel-head">
          <div>
            <h2>快捷入口</h2>
            <p>覆盖后台首页高频动作。</p>
          </div>
        </div>
        <div class="quick-grid">
          <button v-for="item in quickActions" :key="item.label" class="quick-action" type="button" @click="router.push(item.route)">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </button>
        </div>
      </div>
    </section>

    <section class="table-grid">
      <div class="panel list-panel">
        <div class="panel-head inline">
          <div>
            <h2>最近订单</h2>
            <p>展示最新交易记录，便于快速核对状态。</p>
          </div>
          <el-button link type="primary" @click="router.push('/admin/order')">全部订单</el-button>
        </div>
        <el-table :data="recentOrders" empty-text="暂无订单" size="large">
          <el-table-column prop="orderNo" label="订单号" min-width="150" show-overflow-tooltip />
          <el-table-column prop="nickname" label="用户" min-width="100" show-overflow-tooltip />
          <el-table-column prop="totalAmount" label="金额" width="100" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.orderStatus === 'PAID' ? 'success' : 'warning'" round>{{ statusLabel(row.orderStatus) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="panel list-panel">
        <div class="panel-head inline">
          <div>
            <h2>最新教师申请</h2>
            <p>优先处理待审核教师资料。</p>
          </div>
          <el-button link type="primary" @click="router.push('/admin/teacher/audit')">去审核</el-button>
        </div>
        <el-table :data="pendingTeachers" empty-text="暂无待审核教师" size="large">
          <el-table-column prop="realName" label="姓名" min-width="90" />
          <el-table-column prop="nickname" label="昵称" min-width="100" show-overflow-tooltip />
          <el-table-column prop="email" label="邮箱" min-width="150" show-overflow-tooltip />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag type="warning" round>{{ statusLabel(row.auditStatus) }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.dashboard-page {
  display: grid;
  gap: 18px;
}

.dashboard-hero,
.panel,
.metric-card {
  border: 1px solid #dbe3f0;
  background: #fff;
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.06);
}

.dashboard-hero {
  min-height: 132px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 24px 28px;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.1), transparent 44%),
    linear-gradient(90deg, #fff, #f8fafc);
}

.dashboard-hero h1,
.panel h2 {
  margin: 0;
  color: #0f172a;
  letter-spacing: 0;
}

.dashboard-hero h1 {
  font-size: 30px;
  line-height: 1.25;
}

.dashboard-hero p,
.panel p {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.metric-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(6, minmax(0, 1fr));
}

.metric-card {
  min-height: 148px;
  border-radius: 12px;
  padding: 18px;
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.metric-card:hover {
  transform: translateY(-2px);
  border-color: #bfdbfe;
  box-shadow: 0 18px 42px rgba(37, 99, 235, 0.12);
}

.metric-icon {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  margin-bottom: 14px;
  color: #fff;
  font-size: 18px;
}

.metric-label,
.metric-note {
  display: block;
}

.metric-label {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.metric-card strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 30px;
  line-height: 1;
}

.metric-note {
  margin-top: 12px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.tone-blue .metric-icon { background: #2563eb; }
.tone-green .metric-icon { background: #0f766e; }
.tone-orange .metric-icon { background: #f97316; }
.tone-cyan .metric-icon { background: #0891b2; }
.tone-purple .metric-icon { background: #7c3aed; }
.tone-indigo .metric-icon { background: #4f46e5; }

.workbench-grid,
.analytics-grid,
.table-grid {
  display: grid;
  gap: 18px;
}

.workbench-grid {
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
}

.analytics-grid {
  grid-template-columns: minmax(0, 1fr) 340px;
}

.table-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.panel {
  border-radius: 14px;
  padding: 22px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}

.panel-head.inline {
  align-items: flex-start;
}

.panel h2 {
  font-size: 20px;
  line-height: 1.3;
}

.todo-list {
  display: grid;
  gap: 10px;
}

.todo-row {
  min-height: 72px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  display: grid;
  grid-template-columns: 42px 1fr auto;
  align-items: center;
  gap: 12px;
  padding: 12px;
  text-align: left;
  background: #f8fafc;
  cursor: pointer;
}

.todo-row:hover {
  border-color: #bfdbfe;
  background: #eff6ff;
}

.todo-icon {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: #2563eb;
  background: #dbeafe;
  font-size: 18px;
}

.todo-content strong,
.todo-content small {
  display: block;
}

.todo-content strong {
  color: #0f172a;
  font-size: 15px;
}

.todo-content small {
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.45;
}

.question-panel {
  background:
    linear-gradient(135deg, rgba(219, 234, 254, 0.95), rgba(240, 253, 250, 0.9)),
    #f8fbff;
  border-color: #bfdbfe;
  color: #0f172a;
}

.question-panel h2,
.question-panel p {
  color: #0f172a;
}

.question-panel p,
.question-subtitle {
  color: #475569;
}

.question-total {
  margin-top: 18px;
  color: #1d4ed8;
  font-size: 42px;
  font-weight: 800;
  line-height: 1;
}

.question-subtitle {
  margin-top: 10px;
  font-size: 13px;
}

.resource-bars {
  display: grid;
  gap: 12px;
  margin-top: 26px;
}

.resource-row {
  display: grid;
  grid-template-columns: 40px 1fr 42px;
  align-items: center;
  gap: 10px;
  color: #334155;
  font-size: 13px;
}

.resource-row strong {
  text-align: right;
  color: #0f172a;
}

.resource-track {
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.22);
}

.resource-track i {
  display: block;
  height: 100%;
  min-width: 4px;
  border-radius: inherit;
}

.chart-box {
  height: 320px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.quick-action {
  min-height: 86px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  display: grid;
  place-items: center;
  gap: 8px;
  color: #1e293b;
  background: #f8fafc;
  cursor: pointer;
}

.quick-action:hover {
  color: #1d4ed8;
  border-color: #bfdbfe;
  background: #eff6ff;
}

.quick-action .el-icon {
  font-size: 22px;
}

.list-panel {
  min-width: 0;
}

.list-panel :deep(.el-table) {
  --el-table-header-bg-color: #f8fafc;
  --el-table-border-color: #edf2f7;
}

.list-panel :deep(.el-table th.el-table__cell) {
  color: #475569;
  font-weight: 700;
}

@media (max-width: 1280px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 1080px) {
  .workbench-grid,
  .analytics-grid,
  .table-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .dashboard-hero {
    align-items: flex-start;
    flex-direction: column;
    padding: 20px;
  }

  .dashboard-hero h1 {
    font-size: 26px;
  }

  .metric-grid,
  .quick-grid {
    grid-template-columns: 1fr;
  }

  .panel {
    padding: 18px;
  }

  .todo-row {
    grid-template-columns: 38px 1fr;
  }

  .todo-row .el-tag {
    grid-column: 2;
    width: fit-content;
  }
}
</style>
