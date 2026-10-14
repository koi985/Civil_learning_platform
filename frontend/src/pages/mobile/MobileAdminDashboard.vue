<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { firstArray, firstPageRecords } from '../../mobile/helpers'

const router = useRouter()
const loading = ref(false)
const dashboard = ref({})
const audits = ref({ records: [] })
const refunds = ref({ records: [] })

async function loadData() {
  loading.value = true
  try {
    const [dashboardResult, auditResult, refundResult] = await Promise.allSettled([
      request.get('/api/admin/dashboard'),
      request.get('/api/admin/teacher-audits', { params: { pageNo: 1, pageSize: 5, auditStatus: 'PENDING' }, silent: true }),
      request.get('/api/admin/refunds', { params: { pageNo: 1, pageSize: 5, refundStatus: 'REQUESTED' }, silent: true })
    ])
    if (dashboardResult.status === 'fulfilled') dashboard.value = dashboardResult.value || {}
    if (auditResult.status === 'fulfilled') audits.value = auditResult.value || { records: [] }
    if (refundResult.status === 'fulfilled') refunds.value = refundResult.value || { records: [] }
  } finally {
    loading.value = false
  }
}

function statValue(...keys) {
  for (const key of keys) {
    if (dashboard.value?.[key] !== undefined) return dashboard.value[key]
    if (dashboard.value?.overview?.[key] !== undefined) return dashboard.value.overview[key]
  }
  return 0
}

onMounted(() => {
  loadData()
  window.addEventListener('mobile-refresh', loadData)
})
onBeforeUnmount(() => window.removeEventListener('mobile-refresh', loadData))
</script>

<template>
  <div class="mobile-page">
    <section class="mobile-hero is-admin">
      <h1>运营看板</h1>
      <p>移动端只保留核心指标与高频审核，完整后台继续在 PC 端使用。</p>
    </section>

    <van-loading v-if="loading" />

    <section class="mobile-stat-grid">
      <div class="mobile-stat"><span>用户数</span><strong>{{ statValue('userCount', 'totalUserCount') }}</strong></div>
      <div class="mobile-stat"><span>订单数</span><strong>{{ statValue('orderCount', 'totalOrderCount') }}</strong></div>
      <div class="mobile-stat"><span>待教师审核</span><strong>{{ firstArray(firstPageRecords(audits)).length }}</strong></div>
      <div class="mobile-stat"><span>待退款</span><strong>{{ firstArray(firstPageRecords(refunds)).length }}</strong></div>
    </section>

    <van-grid :column-num="3" :border="false" :gutter="8">
      <van-grid-item icon="certificate" text="教师审核" @click="router.push('/m/admin/audits')" />
      <van-grid-item icon="refund-o" text="退款审核" @click="router.push('/m/admin/refunds')" />
      <van-grid-item icon="chart-trending-o" text="成长预警" @click="router.push('/m/admin/growth')" />
    </van-grid>

    <section class="mobile-section">
      <div class="mobile-section-head">
        <h2>待教师审核</h2>
        <van-button size="small" plain type="primary" @click="router.push('/m/admin/audits')">处理</van-button>
      </div>
      <van-cell-group inset>
        <van-cell
          v-for="item in firstPageRecords(audits)"
          :key="item.id"
          :title="item.realName || item.nickname || '教师申请'"
          :label="item.email || item.intro || '待审核'"
          is-link
          @click="router.push('/m/admin/audits')"
        />
      </van-cell-group>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-head">
        <h2>待退款审核</h2>
        <van-button size="small" plain type="primary" @click="router.push('/m/admin/refunds')">处理</van-button>
      </div>
      <van-cell-group inset>
        <van-cell
          v-for="item in firstPageRecords(refunds)"
          :key="item.id"
          :title="item.refundNo || item.orderNo || '退款申请'"
          :label="`${item.nickname || '用户'} · ¥${item.refundAmount || 0}`"
          is-link
          @click="router.push('/m/admin/refunds')"
        />
      </van-cell-group>
    </section>
  </div>
</template>
