<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { firstPageRecords, percentText } from '../../mobile/helpers'

const router = useRouter()
const loading = ref(false)
const payload = ref({ summary: {}, page: { records: [] } })
const query = reactive({ pageNo: 1, pageSize: 20, status: '', taskType: '' })

async function loadData() {
  loading.value = true
  try {
    payload.value = await request.get('/api/user-center/tasks', { params: query })
  } finally {
    loading.value = false
  }
}

function filter(status) {
  query.status = query.status === status ? '' : status
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
    <section class="mobile-stat-grid">
      <div class="mobile-stat"><span>总任务</span><strong>{{ payload.summary?.totalTaskCount || 0 }}</strong></div>
      <div class="mobile-stat"><span>已完成</span><strong>{{ payload.summary?.completedTaskCount || 0 }}</strong></div>
      <div class="mobile-stat"><span>学习分钟</span><strong>{{ payload.summary?.todayLearnMinutes || 0 }}</strong></div>
      <div class="mobile-stat"><span>今日刷题</span><strong>{{ payload.summary?.todayPracticeQuestions || 0 }}</strong></div>
    </section>

    <div class="mobile-filter-row">
      <van-button size="small" :type="query.status === 'PENDING' ? 'primary' : 'default'" @click="filter('PENDING')">待完成</van-button>
      <van-button size="small" :type="query.status === 'IN_PROGRESS' ? 'primary' : 'default'" @click="filter('IN_PROGRESS')">进行中</van-button>
      <van-button size="small" :type="query.status === 'COMPLETED' ? 'primary' : 'default'" @click="filter('COMPLETED')">已完成</van-button>
    </div>

    <div class="mobile-list">
      <article v-for="item in firstPageRecords(payload)" :key="item.id" class="mobile-card">
        <h3>{{ item.title || '每日任务' }}</h3>
        <p>{{ item.description || item.taskTypeLabel || '学习计划任务' }}</p>
        <van-progress :percentage="percentText(item.progressPercent)" />
        <div class="mobile-card-meta">
          <span class="mobile-pill">{{ item.statusLabel || item.status || '待处理' }}</span>
          <span class="mobile-pill is-ok">{{ item.completedValue || 0 }}/{{ item.targetValue || 0 }} {{ item.unitLabel || '' }}</span>
        </div>
        <div class="mobile-actions is-single">
          <van-button type="primary" plain @click="router.push(item.actionPath || '/m/student')">{{ item.actionText || '查看' }}</van-button>
        </div>
      </article>
      <van-loading v-if="loading" />
      <div v-if="!loading && !firstPageRecords(payload).length" class="mobile-empty">当前没有任务</div>
    </div>
  </div>
</template>
