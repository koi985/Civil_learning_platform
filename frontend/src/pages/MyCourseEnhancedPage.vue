<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import UserCenterBackButton from '../components/UserCenterBackButton.vue'

const router = useRouter()
const loading = ref(false)
const allCourses = ref([])
const pageNo = ref(1)
const pageSize = 8

const pagedCourses = computed(() => allCourses.value.slice((pageNo.value - 1) * pageSize, pageNo.value * pageSize))

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '长期有效'
}

function formatMinutesBySeconds(value) {
  return `${Math.floor(Number(value || 0) / 60)}分钟`
}

async function loadData() {
  loading.value = true
  try {
    allCourses.value = await request.get('/api/user-center/courses')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <section class="glass-card list-card" v-loading="loading">
      <div class="page-head">
        <div class="section-title">我的课程</div>
        <UserCenterBackButton />
      </div>
      <div class="course-grid" v-if="pagedCourses.length">
        <article v-for="item in pagedCourses" :key="item.id" class="course-card">
          <img :src="item.coverUrl" alt="" />
          <h3>{{ item.title }}</h3>
          <p>{{ item.teacherName }} · {{ item.subjectName }} · {{ item.examType }}</p>
          <div class="tag-row">
            <el-tag v-if="item.freeCourse" type="success">免费课程</el-tag>
            <el-tag v-else type="warning">已购课程</el-tag>
          </div>
          <el-progress :percentage="Number(item.progressPercent || 0)" :stroke-width="8" />
          <div class="meta-line">
            <span>已完成 {{ item.completedChapterCount || 0 }} 章</span>
            <span>累计 {{ formatMinutesBySeconds(item.totalStudySeconds) }}</span>
          </div>
          <div class="meta-line">
            <span>最近学习 {{ formatDateTime(item.lastStudyAt) }}</span>
          </div>
          <div class="expire">有效期至：{{ formatDateTime(item.expireAt) }}</div>
          <el-button type="primary" style="margin-top: 12px" @click="router.push(`/course/play/${item.courseId}`)">进入学习</el-button>
        </article>
      </div>
      <el-empty v-else description="暂未购课，先去课程中心看看吧" />

      <el-pagination
        v-if="allCourses.length > pageSize"
        background
        layout="prev, pager, next"
        :total="allCourses.length"
        :page-size="pageSize"
        :current-page="pageNo"
        style="margin-top: 24px; justify-content: center"
        @current-change="(page) => { pageNo = page }"
      />
    </section>
  </div>
</template>

<style scoped>
.list-card {
  padding: 24px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.page-head .section-title {
  margin-bottom: 0;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.course-card {
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 16px;
  background: #fff;
  display: grid;
  gap: 12px;
}

.tag-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.course-card img {
  width: 100%;
  height: 160px;
  object-fit: cover;
  border-radius: 14px;
}

.course-card p,
.expire,
.meta-line {
  color: var(--muted);
}

.meta-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
