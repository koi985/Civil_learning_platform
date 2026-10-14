<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { firstArray } from '../../mobile/helpers'
import { useAuthStore } from '../../stores/auth'
import { roleHomePath } from '../../mobile/helpers'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const home = ref({
  hotCourses: [],
  recommendCards: [],
  personalizedResources: { resources: [] },
  latestNews: [],
  quickEntries: []
})

const quickEntries = computed(() => [
  { title: '我的学习', desc: '课程、任务和班级', path: authStore.isLoggedIn ? roleHomePath(authStore.roles) : '/m/login' },
  { title: '课程中心', desc: '查看热门课程', path: '/course' },
  { title: '题库练习', desc: '快速刷题入口', path: '/question-bank' },
  { title: '推荐资源', desc: '查看智能推荐', path: '/recommend' }
])

async function loadData() {
  loading.value = true
  try {
    home.value = await request.get('/api/home/public-overview', { anonymous: true, silent: true })
    if (authStore.isLoggedIn) {
      try {
        const personalized = await request.get('/api/home/personalized', { silent: true })
        home.value = { ...home.value, ...personalized }
      } catch (error) {
        // Public data is enough for the mobile homepage.
      }
    }
  } finally {
    loading.value = false
  }
}

function openPath(path) {
  router.push(path || '/m/home')
}

function openCourse(course) {
  router.push(course.id ? `/course/detail/${course.id}` : '/course')
}

function resourceTitle(item) {
  return item.title || item.name || item.resourceName || '推荐资源'
}

onMounted(() => {
  loadData()
  window.addEventListener('mobile-refresh', loadData)
})
onBeforeUnmount(() => window.removeEventListener('mobile-refresh', loadData))
</script>

<template>
  <div class="mobile-page" v-loading="loading">
    <section class="mobile-hero">
      <h1>公考学习移动端</h1>
      <p>快速查看课程、任务、班级和推荐资源，复杂管理操作保留在 PC 完整版。</p>
      <van-button type="default" round size="small" @click="openPath(authStore.isLoggedIn ? roleHomePath(authStore.roles) : '/m/login')">
        {{ authStore.isLoggedIn ? '进入工作台' : '登录查看' }}
      </van-button>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-head">
        <h2>快捷入口</h2>
      </div>
      <van-grid :column-num="2" :border="false" :gutter="8">
        <van-grid-item v-for="item in quickEntries" :key="item.title" icon="apps-o" :text="item.title" @click="openPath(item.path)" />
      </van-grid>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-head">
        <h2>热门课程</h2>
        <van-button size="small" plain type="primary" @click="router.push('/course')">全部</van-button>
      </div>
      <div class="mobile-list">
        <article v-for="course in firstArray(home.hotCourses).slice(0, 4)" :key="course.id" class="mobile-card mobile-cover-row" @click="openCourse(course)">
          <img class="mobile-cover" :src="course.coverUrl" alt="" />
          <div>
            <h3>{{ course.title || '课程学习' }}</h3>
            <p>{{ course.teacherName || '平台教师' }} · {{ course.subjectName || '综合' }}</p>
            <div class="mobile-card-meta">
              <span class="mobile-pill">{{ Number(course.price || 0) <= 0 ? '免费' : `¥${course.price}` }}</span>
              <span class="mobile-pill is-ok">{{ course.learnCount || 0 }} 人学习</span>
            </div>
          </div>
        </article>
        <div v-if="!firstArray(home.hotCourses).length" class="mobile-empty">暂无热门课程</div>
      </div>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-head">
        <h2>推荐资源</h2>
        <van-button size="small" plain type="primary" @click="router.push('/recommend')">更多</van-button>
      </div>
      <div class="mobile-list">
        <van-cell
          v-for="item in firstArray(home.recommendCards, home.personalizedResources?.resources).slice(0, 5)"
          :key="`${item.resourceType || item.type}-${item.id || item.title}`"
          :title="resourceTitle(item)"
          :label="item.reason || item.subtitle || item.tags || '根据学习热度与阶段推荐'"
          is-link
          @click="router.push(item.targetPath || item.path || '/recommend')"
        />
        <div v-if="!firstArray(home.recommendCards, home.personalizedResources?.resources).length" class="mobile-empty">登录后查看更多个性化推荐</div>
      </div>
    </section>
  </div>
</template>
