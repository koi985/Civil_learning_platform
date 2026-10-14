<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { firstArray, formatDateTime } from '../../mobile/helpers'

const router = useRouter()
const loading = ref(false)
const payload = ref({ overview: {}, reminderSummary: {}, segmentStats: [], focusUsers: [], followReminders: [], weakKnowledgeTop: [] })
const query = reactive({ segment: '', minCompletionRate: 0, followStatus: '' })

async function loadData() {
  loading.value = true
  try {
    payload.value = await request.get('/api/admin/growth-dashboard', { params: query })
  } finally {
    loading.value = false
  }
}

function switchSegment(segment) {
  query.segment = query.segment === segment ? '' : segment
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
      <div class="mobile-stat"><span>活跃计划</span><strong>{{ payload.overview?.activePlanCount || 0 }}</strong></div>
      <div class="mobile-stat"><span>今日任务达成</span><strong>{{ payload.overview?.todayCompletedTaskCount || 0 }}</strong></div>
      <div class="mobile-stat"><span>逾期回访</span><strong>{{ payload.reminderSummary?.overdueCount || 0 }}</strong></div>
      <div class="mobile-stat"><span>今日回访</span><strong>{{ payload.reminderSummary?.todayCount || 0 }}</strong></div>
    </section>

    <div class="mobile-filter-row">
      <van-button size="small" :type="query.segment === 'AT_RISK' ? 'primary' : 'default'" @click="switchSegment('AT_RISK')">重点预警</van-button>
      <van-button size="small" :type="query.segment === 'TRACKING' ? 'primary' : 'default'" @click="switchSegment('TRACKING')">跟踪补强</van-button>
      <van-button size="small" :type="query.segment === 'RISING' ? 'primary' : 'default'" @click="switchSegment('RISING')">持续上升</van-button>
    </div>

    <section class="mobile-section">
      <div class="mobile-section-head">
        <h2>重点跟进人群</h2>
        <van-button size="small" plain type="primary" @click="router.push('/admin/growth')">PC跟进</van-button>
      </div>
      <div class="mobile-list">
        <article v-for="item in firstArray(payload.focusUsers).slice(0, 12)" :key="`${item.userId}-${item.planId}`" class="mobile-card">
          <h3>{{ item.username || '学员' }}</h3>
          <p>{{ item.planName || '学习计划' }}</p>
          <div class="mobile-card-meta">
            <span class="mobile-pill is-warn">{{ item.segmentLabel || item.segment || '关注' }}</span>
            <span class="mobile-pill">完成率 {{ item.completionRate || 0 }}%</span>
            <span class="mobile-pill is-ok">{{ item.followStatusLabel || item.followStatus || '待跟进' }}</span>
          </div>
          <p>{{ firstArray(item.weakKnowledgePoints).join(' / ') || '暂无薄弱知识点' }}</p>
        </article>
        <van-loading v-if="loading" />
        <div v-if="!loading && !firstArray(payload.focusUsers).length" class="mobile-empty">暂无重点跟进人群</div>
      </div>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-head"><h2>近期回访</h2></div>
      <van-cell-group inset>
        <van-cell
          v-for="item in firstArray(payload.followReminders).slice(0, 8)"
          :key="`${item.userId}-${item.nextFollowUpAt}`"
          :title="item.username || '学员'"
          :label="`${item.planName || '学习计划'} · ${formatDateTime(item.nextFollowUpAt, '暂无时间')}`"
        />
      </van-cell-group>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-head"><h2>高频薄弱知识点</h2></div>
      <van-cell-group inset>
        <van-cell
          v-for="item in firstArray(payload.weakKnowledgeTop).slice(0, 8)"
          :key="`${item.subjectName}-${item.knowledgePoint}`"
          :title="item.knowledgePoint || '知识点'"
          :label="`${item.subjectName || '科目'} · 待消化 ${item.pendingWrongCount || 0} 题 · 掌握度 ${item.masteryScore || 0}`"
        />
      </van-cell-group>
    </section>
  </div>
</template>
