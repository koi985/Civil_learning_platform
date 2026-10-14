<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const detail = ref({ exam: {}, rankingPreview: [], analysisCourses: [] })

async function loadData() {
  loading.value = true
  try {
    detail.value = await request.get(`/api/exams/${route.params.id}`)
  } finally {
    loading.value = false
  }
}

async function startExam() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后再开始模考')
    router.push('/login')
    return
  }
  const data = await request.post(`/api/exams/${route.params.id}/start`)
  router.push(`/exam/start/${data.recordId}`)
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section class="glass-card detail-card">
      <div class="hero-copy">
        <div class="badge">{{ detail.exam.seasonName }}</div>
        <h1>{{ detail.exam.title }}</h1>
        <p>{{ detail.exam.intro }}</p>
        <div class="meta-row">
          <span>{{ detail.exam.province }}</span>
          <span>{{ detail.exam.examType }}</span>
          <span>{{ detail.exam.subjectName }}</span>
          <span>{{ detail.exam.durationMinutes }} 分钟</span>
        </div>
        <div class="meta-row strong">
          <span>总分 {{ detail.totalScore }}</span>
          <span>题量 {{ detail.questionCount }}</span>
        </div>
        <div class="action-row">
          <el-button type="primary" size="large" @click="startExam">开始作答</el-button>
          <el-button plain size="large" @click="router.push(`/exam/rank/${route.params.id}`)">查看排行榜</el-button>
        </div>
      </div>

      <div class="ranking-box">
        <div class="section-title small">排行榜预览</div>
        <div v-if="detail.rankingPreview.length" class="ranking-list">
          <div v-for="item in detail.rankingPreview" :key="item.rank" class="ranking-item">
            <span>#{{ item.rank }}</span>
            <strong>{{ item.nickname }}</strong>
            <span>{{ item.score }} 分</span>
          </div>
        </div>
        <el-empty v-else description="暂无排名数据" />
      </div>
    </section>

    <section class="glass-card related-card">
      <div class="section-title">模考解析课程推荐</div>
      <div class="course-grid" v-if="detail.analysisCourses?.length">
        <article v-for="item in detail.analysisCourses" :key="item.courseId" class="course-card">
          <h3>{{ item.title }}</h3>
          <p>{{ item.teacherName }} · {{ item.subjectName }}</p>
          <p>{{ item.recommendReason }}</p>
          <div class="course-foot">
            <strong>{{ item.price > 0 ? `¥${item.price}` : '免费' }}</strong>
            <el-button text type="primary" @click="router.push(item.path)">查看课程</el-button>
          </div>
        </article>
      </div>
      <el-empty v-else description="当前暂无解析课程推荐" />
    </section>
  </div>
</template>

<style scoped>
.detail-card {
  display: grid;
  grid-template-columns: 1.2fr 360px;
  gap: 24px;
  padding: 28px;
}

.badge {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(29, 78, 216, 0.12);
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
}

.hero-copy p,
.ranking-item span {
  color: var(--muted);
}

.meta-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.meta-row.strong {
  font-weight: 700;
  color: var(--primary);
}

.action-row {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.ranking-box {
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 18px;
  background: #fff;
}

.related-card {
  margin-top: 24px;
  padding: 24px;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.course-card {
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 18px;
  background: #fff;
}

.course-card p {
  color: var(--muted);
}

.course-foot {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.section-title.small {
  font-size: 22px;
}

.ranking-list {
  display: grid;
  gap: 12px;
}

.ranking-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid var(--border);
  padding-bottom: 10px;
}

@media (max-width: 960px) {
  .detail-card {
    grid-template-columns: 1fr;
  }
}
</style>
