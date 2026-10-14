<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { firstPageRecords } from '../../mobile/helpers'

const router = useRouter()
const loading = ref(false)
const payload = ref({ records: [] })
const query = reactive({ pageNo: 1, pageSize: 30, reviewStatus: '', taskType: '' })

async function loadData() {
  loading.value = true
  try {
    payload.value = await request.get('/api/teacher/reviews', { params: query })
  } finally {
    loading.value = false
  }
}

function reportPath(row) {
  return row.taskType === 'EXAM'
    ? `/classroom/exam/report/${row.submissionId}`
    : `/classroom/homework/report/${row.submissionId}`
}

function filterPending() {
  query.reviewStatus = query.reviewStatus ? '' : 'AUTO_GRADED'
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
      <van-button size="small" :type="query.reviewStatus ? 'primary' : 'default'" @click="filterPending">待复核/未完成</van-button>
      <van-button size="small" plain type="primary" @click="router.push('/teacher/review')">PC 批改</van-button>
    </div>

    <div class="mobile-list">
      <article v-for="item in firstPageRecords(payload)" :key="`${item.taskType}-${item.submissionId}`" class="mobile-card">
        <h3>{{ item.taskTitle || '批改任务' }}</h3>
        <p>{{ item.studentName || '学员' }} · {{ item.classroomName || '班级' }}</p>
        <div class="mobile-card-meta">
          <span class="mobile-pill">{{ item.taskType === 'EXAM' ? '考试' : '作业' }}</span>
          <span class="mobile-pill is-warn">{{ item.reviewStatus || '待批改' }}</span>
          <span class="mobile-pill is-ok">得分 {{ item.score ?? 0 }}</span>
        </div>
        <div class="mobile-actions">
          <van-button type="primary" plain @click="router.push(reportPath(item))">查看报告</van-button>
          <van-button plain @click="router.push('/teacher/review')">完整批改</van-button>
        </div>
      </article>
      <van-loading v-if="loading" />
      <div v-if="!loading && !firstPageRecords(payload).length" class="mobile-empty">暂无待批改任务</div>
    </div>
  </div>
</template>
