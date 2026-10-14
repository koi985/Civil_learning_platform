<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant'
import request from '../../utils/request'
import { firstPageRecords } from '../../mobile/helpers'

const loading = ref(false)
const payload = ref({ records: [] })
const query = reactive({ pageNo: 1, pageSize: 30, auditStatus: 'PENDING' })

async function loadData() {
  loading.value = true
  try {
    payload.value = await request.get('/api/admin/teacher-audits', { params: query })
  } finally {
    loading.value = false
  }
}

async function audit(row, auditStatus) {
  try {
    await showConfirmDialog({
      title: auditStatus === 'APPROVED' ? '通过教师申请' : '驳回教师申请',
      message: row.realName || row.nickname || '确认处理该教师申请？'
    })
    await request.post('/api/admin/teacher-audits', {
      teacherProfileId: row.id,
      auditStatus,
      auditRemark: auditStatus === 'APPROVED' ? '移动端快速通过' : '移动端快速驳回'
    })
    showSuccessToast('审核完成')
    await loadData()
  } catch (error) {
    if (error) showFailToast(error.message || '操作取消')
  }
}

function switchStatus(status) {
  query.auditStatus = status
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
      <van-button size="small" :type="query.auditStatus === 'PENDING' ? 'primary' : 'default'" @click="switchStatus('PENDING')">待审核</van-button>
      <van-button size="small" :type="query.auditStatus === 'APPROVED' ? 'primary' : 'default'" @click="switchStatus('APPROVED')">已通过</van-button>
      <van-button size="small" :type="query.auditStatus === 'REJECTED' ? 'primary' : 'default'" @click="switchStatus('REJECTED')">已驳回</van-button>
    </div>

    <div class="mobile-list">
      <article v-for="item in firstPageRecords(payload)" :key="item.id" class="mobile-card">
        <h3>{{ item.realName || item.nickname || '教师申请' }}</h3>
        <p>{{ item.email || '未填写邮箱' }}</p>
        <p>{{ item.intro || '暂无教师简介' }}</p>
        <div class="mobile-card-meta">
          <span class="mobile-pill is-warn">{{ item.auditStatus || 'PENDING' }}</span>
        </div>
        <div v-if="item.auditStatus === 'PENDING'" class="mobile-actions">
          <van-button type="primary" @click="audit(item, 'APPROVED')">通过</van-button>
          <van-button plain type="danger" @click="audit(item, 'REJECTED')">驳回</van-button>
        </div>
      </article>
      <van-loading v-if="loading" />
      <div v-if="!loading && !firstPageRecords(payload).length" class="mobile-empty">暂无教师审核记录</div>
    </div>
  </div>
</template>
