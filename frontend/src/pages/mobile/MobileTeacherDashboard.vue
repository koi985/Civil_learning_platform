<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { firstArray } from '../../mobile/helpers'

const router = useRouter()
const loading = ref(false)
const data = ref({ profile: {}, classrooms: [], pendingJoinCount: 0, homeworkCount: 0, examCount: 0, pendingReviewCount: 0 })
const homeworks = ref([])

async function loadData() {
  loading.value = true
  try {
    const [dashboardResult, homeworkResult] = await Promise.allSettled([
      request.get('/api/teacher/dashboard'),
      request.get('/api/teacher/homeworks', { params: { pageNo: 1, pageSize: 5 }, silent: true })
    ])
    if (dashboardResult.status === 'fulfilled') data.value = dashboardResult.value || data.value
    if (homeworkResult.status === 'fulfilled') homeworks.value = firstArray(homeworkResult.value?.records)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
  window.addEventListener('mobile-refresh', loadData)
})
onBeforeUnmount(() => window.removeEventListener('mobile-refresh', loadData))
</script>

<template>
  <div class="mobile-page">
    <section class="mobile-hero is-teacher">
      <h1>教师工作台</h1>
      <p>快速查看班级、待批改和近期作业，深度发布与报表在 PC 完整版处理。</p>
    </section>

    <van-loading v-if="loading" />

    <section class="mobile-stat-grid">
      <div class="mobile-stat"><span>班级数</span><strong>{{ firstArray(data.classrooms).length }}</strong></div>
      <div class="mobile-stat"><span>待批改</span><strong>{{ data.pendingReviewCount || 0 }}</strong></div>
      <div class="mobile-stat"><span>入班申请</span><strong>{{ data.pendingJoinCount || 0 }}</strong></div>
      <div class="mobile-stat"><span>已发作业</span><strong>{{ data.homeworkCount || 0 }}</strong></div>
    </section>

    <van-grid :column-num="3" :border="false" :gutter="8">
      <van-grid-item icon="friends-o" text="班级" @click="router.push('/m/teacher/classes')" />
      <van-grid-item icon="edit" text="批改" @click="router.push('/m/teacher/reviews')" />
      <van-grid-item icon="desktop-o" text="PC端" @click="router.push('/teacher/dashboard')" />
    </van-grid>

    <section class="mobile-section">
      <div class="mobile-section-head">
        <h2>我的班级</h2>
        <van-button size="small" plain type="primary" @click="router.push('/m/teacher/classes')">全部</van-button>
      </div>
      <div class="mobile-list">
        <article v-for="item in firstArray(data.classrooms).slice(0, 3)" :key="item.id" class="mobile-card">
          <h3>{{ item.name || '班级' }}</h3>
          <p>{{ item.subjectName || '综合' }} · {{ item.memberCount || 0 }} 人</p>
          <div class="mobile-actions is-single">
            <van-button type="primary" plain @click="router.push('/m/teacher/classes')">查看摘要</van-button>
          </div>
        </article>
        <div v-if="!firstArray(data.classrooms).length" class="mobile-empty">暂无班级</div>
      </div>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-head"><h2>近期作业</h2></div>
      <van-cell-group inset>
        <van-cell
          v-for="item in homeworks"
          :key="item.id"
          :title="item.title || '作业'"
          :label="`${item.classroomName || '班级'} · ${item.status || ''}`"
          is-link
          @click="router.push('/teacher/homework')"
        />
      </van-cell-group>
    </section>
  </div>
</template>
