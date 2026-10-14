<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { firstArray, percentText } from '../../mobile/helpers'

const router = useRouter()
const loading = ref(false)
const overview = ref({ summary: {}, todayTasks: [], upcomingTasks: [], weakKnowledgePoints: [] })
const courses = ref([])
const recommend = ref({ resources: [], recommendCards: [] })

const todayTasks = computed(() => firstArray(overview.value.todayTasks).slice(0, 4))
const courseItems = computed(() => firstArray(courses.value).slice(0, 3))
const recommendItems = computed(() => firstArray(recommend.value.resources, recommend.value.recommendCards).slice(0, 3))

async function loadData() {
  loading.value = true
  try {
    const [overviewResult, coursesResult, recommendResult] = await Promise.allSettled([
      request.get('/api/user-center/overview', { silent: true }),
      request.get('/api/user-center/courses', { silent: true }),
      request.get('/api/recommend/overview', { silent: true })
    ])
    if (overviewResult.status === 'fulfilled') overview.value = overviewResult.value || overview.value
    if (coursesResult.status === 'fulfilled') courses.value = coursesResult.value || []
    if (recommendResult.status === 'fulfilled') recommend.value = recommendResult.value || recommend.value
  } finally {
    loading.value = false
  }
}

function taskPath(task) {
  return task.actionPath || (task.taskType === 'PRACTICE' ? '/question-bank' : '/my-course')
}

onMounted(() => {
  loadData()
  window.addEventListener('mobile-refresh', loadData)
})
onBeforeUnmount(() => window.removeEventListener('mobile-refresh', loadData))
</script>

<template>
  <div class="mobile-page">
    <section class="mobile-hero">
      <h1>学习工作台</h1>
      <p>查看今日任务、课程进度和推荐练习，保持手机端轻量处理。</p>
    </section>

    <van-loading v-if="loading" type="spinner" />

    <section class="mobile-stat-grid">
      <div class="mobile-stat"><span>今日任务</span><strong>{{ overview.summary?.todayTaskCount || todayTasks.length || 0 }}</strong></div>
      <div class="mobile-stat"><span>已完成</span><strong>{{ overview.summary?.todayCompletedTaskCount || 0 }}</strong></div>
      <div class="mobile-stat"><span>完成率</span><strong>{{ percentText(overview.summary?.completionRate) }}%</strong></div>
      <div class="mobile-stat"><span>我的课程</span><strong>{{ courses.length || 0 }}</strong></div>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-head">
        <h2>今日任务</h2>
        <van-button size="small" plain type="primary" @click="router.push('/m/student/tasks')">全部</van-button>
      </div>
      <div class="mobile-list">
        <article v-for="item in todayTasks" :key="item.id" class="mobile-card">
          <h3>{{ item.title || '学习任务' }}</h3>
          <p>{{ item.description || item.taskTypeLabel || '按计划完成今日学习任务' }}</p>
          <van-progress :percentage="percentText(item.progressPercent)" />
          <div class="mobile-actions is-single">
            <van-button type="primary" plain @click="router.push(taskPath(item))">{{ item.actionText || '去完成' }}</van-button>
          </div>
        </article>
        <div v-if="!todayTasks.length" class="mobile-empty">暂无今日任务</div>
      </div>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-head">
        <h2>继续学习</h2>
        <van-button size="small" plain type="primary" @click="router.push('/m/student/courses')">课程</van-button>
      </div>
      <div class="mobile-list">
        <article v-for="item in courseItems" :key="item.id" class="mobile-card mobile-cover-row">
          <img class="mobile-cover" :src="item.coverUrl" alt="" />
          <div>
            <h3>{{ item.title || '我的课程' }}</h3>
            <p>{{ item.teacherName || '平台教师' }} · 进度 {{ percentText(item.progressPercent) }}%</p>
            <van-progress :percentage="percentText(item.progressPercent)" />
            <div class="mobile-actions is-single">
              <van-button type="primary" size="small" @click="router.push(`/course/play/${item.courseId || item.id}`)">进入学习</van-button>
            </div>
          </div>
        </article>
        <div v-if="!courseItems.length" class="mobile-empty">暂无课程，先去课程中心看看</div>
      </div>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-head">
        <h2>推荐练习</h2>
      </div>
      <van-cell-group inset>
        <van-cell
          v-for="item in recommendItems"
          :key="item.id || item.title"
          :title="item.title || item.resourceName || '推荐资源'"
          :label="item.reason || item.subtitle || '根据学习数据推荐'"
          is-link
          @click="router.push(item.path || item.targetPath || '/recommend')"
        />
      </van-cell-group>
    </section>
  </div>
</template>
