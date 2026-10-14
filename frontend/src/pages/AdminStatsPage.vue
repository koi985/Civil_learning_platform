<script setup>
import { nextTick, onMounted, ref } from 'vue'
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
const stats = ref({ overview: {}, salesTrend: [], subjectSales: [], refundStatusStats: [], roleStats: [], commentStats: [], broadcastStats: [] })
const trendRef = ref(null)
const subjectRef = ref(null)
const refundRef = ref(null)
const roleRef = ref(null)
let trendChart
let subjectChart
let refundChart
let roleChart

async function loadData() {
  loading.value = true
  try {
    stats.value = await request.get('/api/admin/stats')
    await nextTick()
    await renderCharts()
  } finally {
    loading.value = false
  }
}

async function exportStats() {
  try {
    await request.download('/api/admin/stats/export')
    ElMessage.success('统计导出成功')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function renderCharts() {
  const echarts = await loadEcharts()
  if (trendRef.value) {
    trendChart = trendChart || echarts.init(trendRef.value)
    trendChart.setOption({
      tooltip: {},
      xAxis: { type: 'category', data: stats.value.salesTrend.map((item) => item.date) },
      yAxis: { type: 'value' },
      series: [{ type: 'line', data: stats.value.salesTrend.map((item) => item.orderCount), smooth: true, itemStyle: { color: '#1d4ed8' } }]
    })
  }
  if (subjectRef.value) {
    subjectChart = subjectChart || echarts.init(subjectRef.value)
    subjectChart.setOption({
      tooltip: {},
      series: [{
        type: 'pie',
        radius: ['45%', '70%'],
        data: stats.value.subjectSales.map((item) => ({ name: item.subjectName, value: Number(item.salesAmount) }))
      }]
    })
  }
  if (refundRef.value) {
    refundChart = refundChart || echarts.init(refundRef.value)
    refundChart.setOption({
      tooltip: {},
      series: [{
        type: 'pie',
        radius: ['45%', '70%'],
        data: stats.value.refundStatusStats.map((item) => ({ name: item.name, value: Number(item.value) }))
      }]
    })
  }
  if (roleRef.value) {
    roleChart = roleChart || echarts.init(roleRef.value)
    roleChart.setOption({
      tooltip: {},
      xAxis: { type: 'category', data: stats.value.roleStats.map((item) => item.roleName || item.roleCode) },
      yAxis: { type: 'value' },
      series: [{
        type: 'bar',
        data: stats.value.roleStats.map((item) => item.userCount),
        itemStyle: { color: '#0f766e' }
      }]
    })
  }
}

onMounted(loadData)
</script>

<template>
  <div class="stats-page" v-loading="loading">
    <section class="glass-card action-bar">
      <div>
        <div class="section-title">平台统计</div>
        <div class="muted">订单、退款、评价、广播、角色分布统一汇总</div>
      </div>
      <el-button type="primary" plain @click="exportStats">导出统计</el-button>
    </section>

    <section class="stat-grid">
      <div class="glass-card stat-card"><span>已支付订单</span><strong>{{ stats.overview.paidOrderCount }}</strong></div>
      <div class="glass-card stat-card"><span>销售额</span><strong>{{ stats.overview.salesAmount }}</strong></div>
      <div class="glass-card stat-card"><span>用户数</span><strong>{{ stats.overview.userCount }}</strong></div>
      <div class="glass-card stat-card"><span>练习提交数</span><strong>{{ stats.overview.practiceCount }}</strong></div>
      <div class="glass-card stat-card"><span>退款单数</span><strong>{{ stats.overview.refundCount }}</strong></div>
      <div class="glass-card stat-card"><span>课程评价数</span><strong>{{ stats.overview.courseCommentCount }}</strong></div>
      <div class="glass-card stat-card"><span>资讯评价数</span><strong>{{ stats.overview.newsCommentCount }}</strong></div>
      <div class="glass-card stat-card"><span>广播消息数</span><strong>{{ stats.overview.broadcastCount }}</strong></div>
    </section>

    <section class="chart-grid">
      <div class="glass-card chart-card">
        <div class="section-title">近 7 日订单趋势</div>
        <div ref="trendRef" style="height: 320px"></div>
      </div>
      <div class="glass-card chart-card">
        <div class="section-title">科目销售分布</div>
        <div ref="subjectRef" style="height: 320px"></div>
      </div>
      <div class="glass-card chart-card">
        <div class="section-title">退款状态分布</div>
        <div ref="refundRef" style="height: 320px"></div>
      </div>
      <div class="glass-card chart-card">
        <div class="section-title">角色用户数</div>
        <div ref="roleRef" style="height: 320px"></div>
      </div>
    </section>

    <section class="info-grid">
      <div class="glass-card info-card">
        <div class="section-title small">评价统计</div>
        <div v-for="item in stats.commentStats" :key="item.resourceType" class="info-row">
          <span>{{ item.resourceType }}</span>
          <strong>{{ item.count }}</strong>
        </div>
      </div>
      <div class="glass-card info-card">
        <div class="section-title small">广播统计</div>
        <div v-for="item in stats.broadcastStats" :key="item.name" class="info-row">
          <span>{{ item.name }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
      <div class="glass-card info-card">
        <div class="section-title small">角色统计</div>
        <div v-for="item in stats.roleStats" :key="item.roleId" class="info-row">
          <span>{{ item.roleName || item.roleCode }}</span>
          <strong>{{ item.userCount }}</strong>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.stats-page {
  display: grid;
  gap: 20px;
}

.action-bar {
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.stat-grid,
.chart-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.info-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
}

.stat-card,
.chart-card {
  padding: 24px;
}

.stat-card span {
  color: var(--muted);
}

.stat-card strong {
  display: block;
  margin-top: 10px;
  font-size: 28px;
  color: var(--primary);
}

.info-card {
  padding: 20px 24px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border);
}

.info-row:last-child {
  border-bottom: 0;
}

.muted {
  color: var(--muted);
}
</style>
