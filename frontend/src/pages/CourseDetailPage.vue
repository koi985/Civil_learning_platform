<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request'
import { resolveAssetUrl } from '../utils/assets'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref({
  course: {},
  chapters: [],
  materials: [],
  comments: [],
  relatedCourses: []
})

async function loadData() {
  loading.value = true
  try {
    detail.value = await request.get(`/api/courses/${route.params.id}`)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section class="glass-card detail-hero">
      <img :src="detail.course.coverUrl" alt="" class="detail-cover" />
      <div class="detail-copy">
        <div class="detail-tag">{{ detail.course.tags }}</div>
        <h1>{{ detail.course.title }}</h1>
        <p>{{ detail.course.description }}</p>
        <div class="detail-meta">
          <span>{{ detail.course.teacherName }}</span>
          <span>{{ detail.course.province }}</span>
          <span>{{ detail.course.subjectName }}</span>
          <span>{{ detail.course.examType }}</span>
        </div>
        <div class="detail-price">
          <strong>¥{{ detail.course.price }}</strong>
          <span>原价 ¥{{ detail.course.originalPrice }}</span>
        </div>
        <el-button type="primary" size="large">模拟购买</el-button>
      </div>
    </section>

    <section class="glass-card detail-content section-gap">
      <el-tabs>
        <el-tab-pane label="课程介绍">
          <p class="description">{{ detail.course.highlights }}</p>
          <p class="description">适合人群：{{ detail.course.targetUsers }}</p>
          <p class="description">有效期：{{ detail.course.validityDays }} 天</p>
        </el-tab-pane>
        <el-tab-pane label="课程表">
          <el-timeline>
            <el-timeline-item v-for="item in detail.chapters" :key="item.id" :timestamp="item.liveStartTime || `${item.durationMinutes} 分钟`">
              <strong>{{ item.title }}</strong>
              <div>{{ item.chapterType }} · 试听：{{ item.trialAvailable ? '支持' : '不支持' }}</div>
            </el-timeline-item>
          </el-timeline>
        </el-tab-pane>
        <el-tab-pane label="资料下载">
          <div class="material-list">
            <div v-for="item in detail.materials" :key="item.id" class="material-item">
              <span>{{ item.title }}</span>
              <a :href="resolveAssetUrl(item.fileUrl)" target="_blank">下载</a>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="用户评价">
          <div class="comment-list">
            <div v-for="item in detail.comments" :key="item.id" class="comment-item">
              <div class="comment-head">
                <strong>{{ item.nickname }}</strong>
                <span>{{ item.rating }} 分</span>
              </div>
              <div>{{ item.content }}</div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <section class="glass-card detail-content section-gap">
      <div class="section-header">
        <div class="section-title">相关推荐</div>
      </div>
      <div class="course-grid">
        <div v-for="course in detail.relatedCourses" :key="course.id" class="related-card" @click="router.push(`/course/detail/${course.id}`)">
          <div class="related-title">{{ course.title }}</div>
          <div class="related-meta">{{ course.teacherName }} · ¥{{ course.price }}</div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.detail-hero,
.detail-content {
  padding: 24px;
}

.detail-hero {
  display: grid;
  grid-template-columns: 420px 1fr;
  gap: 24px;
}

.detail-cover {
  width: 100%;
  height: 280px;
  object-fit: cover;
  border-radius: 18px;
}

.detail-tag {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(29, 78, 216, 0.12);
  color: var(--primary);
  font-weight: 700;
}

.detail-meta {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  color: var(--muted);
}

.detail-price {
  margin: 18px 0;
  display: flex;
  gap: 16px;
  align-items: center;
}

.detail-price strong {
  color: var(--accent);
  font-size: 32px;
}

.description {
  line-height: 1.9;
}

.material-list,
.comment-list,
.course-grid {
  display: grid;
  gap: 14px;
}

.material-item,
.comment-item,
.related-card {
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 14px;
}

.material-item,
.comment-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.related-card {
  cursor: pointer;
}

.related-title {
  font-weight: 700;
}

.related-meta {
  margin-top: 6px;
  color: var(--muted);
}

@media (max-width: 960px) {
  .detail-hero {
    grid-template-columns: 1fr;
  }
}
</style>
