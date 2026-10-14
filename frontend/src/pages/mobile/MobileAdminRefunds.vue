<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant'
import request from '../../utils/request'
import { firstPageRecords, formatDateTime } from '../../mobile/helpers'

const loading = ref(false)
const payload = ref({ records: [] })
const query = reactive({ pageNo: 1, pageSize: 30, refundStatus: 'REQUESTED', keyword: '' })

async function loadData() {
  loading.value = true
  try {
    payload.value = await request.get('/api/admin/refunds', { params: query })
  } finally {
    loading.value = false
  }
}

async function processRefund(row, action) {
  try {
    await showConfirmDialog({
      title: action === 'approve' ? '通过退款' : '驳回退款',
      message: `${row.refundNo || row.orderNo || '退款单'} · ¥${row.refundAmount || 0}`
    })
    await request.post(`/api/admin/refunds/${row.id}/${action}`, {
      reviewRemark: action === 'approve' ? '移动端快速通过' : '移动端快速驳回'
    })
    showSuccessToast(action === 'approve' ? '退款已通过' : '退款已驳回')
    await loadData()
  } catch (error) {
    if (error) showFailToast(error.message || '操作取消')
  }
}

function switchStatus(status) {
  query.refundStatus = status
  query.pageNo = 1
  loadData()
}

onMounted(() => {
  loadData()
  window.addEventListener('mobile-refresh', loadData)
})
onBeforeUnmount(() => window.removeEventListener('mobile-refresh', loadData))
</script>

<template>
  <div class="mobile-page">
    <div class="mobile-filter-row">
      <van-button size="small" :type="query.refundStatus === 'REQUESTED' ? 'primary' : 'default'" @click="switchStatus('REQUESTED')">申请中</van-button>
      <van-button size="small" :type="query.refundStatus === 'APPROVED' ? 'primary' : 'default'" @click="switchStatus('APPROVED')">已通过</van-button>
      <van-button size="small" :type="query.refundStatus === 'REJECTED' ? 'primary' : 'default'" @click="switchStatus('REJECTED')">已驳回</van-button>
    </div>

    <div class="mobile-list">
      <article v-for="item in firstPageRecords(payload)" :key="item.id" class="mobile-card">
        <h3>{{ item.refundNo || '退款申请' }}</h3>
        <p>{{ item.orderNo || '订单' }} · {{ item.nickname || '用户' }}</p>
        <div class="mobile-card-meta">
          <span class="mobile-pill is-warn">¥{{ item.refundAmount || 0 }}</span>
          <span class="mobile-pill">{{ item.refundStatus || 'REQUESTED' }}</span>
          <span class="mobile-pill is-ok">{{ formatDateTime(item.requestedAt, '暂无时间') }}</span>
        </div>
        <p>{{ item.refundReason || '暂无退款原因' }}</p>
        <div v-if="item.refundStatus === 'REQUESTED'" class="mobile-actions">
          <van-button type="primary" @click="processRefund(item, 'approve')">通过</van-button>
          <van-button plain type="danger" @click="processRefund(item, 'reject')">驳回</van-button>
        </div>
      </article>
      <van-loading v-if="loading" />
      <div v-if="!loading && !firstPageRecords(payload).length" class="mobile-empty">暂无退款记录</div>
    </div>
  </div>
</template>
