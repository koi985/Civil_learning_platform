<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'
import { useAuthStore } from '../stores/auth'
import { resolveAssetUrl } from '../utils/assets'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const actionLoading = ref(false)
const detail = ref({
  course: {},
  chapters: [],
  materials: [],
  comments: [],
  relatedCourses: [],
  purchased: false,
  inCart: false,
  freeCourse: false,
  canComment: false,
  canTrial: false,
  trialChapterCount: 0
})
const commentSubmitting = ref(false)
let lastRefreshAt = 0
const commentForm = ref({
  rating: 5,
  content: ''
})

const isFreeCourse = computed(() => detail.value.freeCourse || Number(detail.value.course.price || 0) <= 0)

async function loadData() {
  lastRefreshAt = Date.now()
  loading.value = true
  try {
    detail.value = await request.get(`/api/courses/${route.params.id}`)
  } finally {
    loading.value = false
  }
}

async function addToCart() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后再加入购物车')
    router.push('/login')
    return
  }
  await request.post('/api/cart/items', { courseId: Number(route.params.id) })
  ElMessage.success('已加入购物车')
  await loadData()
}

async function startLearning() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后再进入学习')
    router.push('/login')
    return
  }
  actionLoading.value = true
  try {
    if (isFreeCourse.value && !detail.value.purchased) {
      await request.post(`/api/user-center/courses/${route.params.id}/claim-free`)
      ElMessage.success('免费课程已加入我的课程')
    }
    await loadData()
    router.push(`/course/play/${route.params.id}`)
  } finally {
    actionLoading.value = false
  }
}

function previewCourse() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后再试听')
    router.push('/login')
    return
  }
  router.push(`/course/play/${route.params.id}?preview=1`)
}

async function buyNow() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后再创建订单')
    router.push('/login')
    return
  }
  await request.post('/api/orders/direct', {
    courseId: Number(route.params.id),
    paymentChannel: 'ALIPAY',
    remark: '课程直购'
  })
  ElMessage.success('订单已创建，请前往订单中心支付')
  router.push('/user/order')
}

async function submitComment() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后再提交评价')
    router.push('/login')
    return
  }
  if (!commentForm.value.content.trim()) {
    ElMessage.warning('请填写评价内容')
    return
  }
  if (!detail.value.canComment) {
    ElMessage.warning('购买或加入课程后才能评价')
    return
  }
  commentSubmitting.value = true
  try {
    const comment = await request.post(`/api/courses/${route.params.id}/comments`, {
      rating: commentForm.value.rating,
      content: commentForm.value.content.trim()
    })
    detail.value.comments = [comment, ...detail.value.comments]
    detail.value.commentCount = Number(detail.value.commentCount || 0) + 1
    const totalScore = detail.value.comments.reduce((sum, item) => sum + Number(item.rating || 0), 0)
    detail.value.averageRating = (totalScore / detail.value.comments.length).toFixed(1)
    commentForm.value = { rating: 5, content: '' }
    ElMessage.success('评价提交成功')
  } finally {
    commentSubmitting.value = false
  }
}

function refreshOnWindowReturn() {
  if (document.visibilityState === 'hidden') {
    return
  }
  const now = Date.now()
  if (now - lastRefreshAt < 1000) {
    return
  }
  loadData()
}

onMounted(() => {
  loadData()
  window.addEventListener('focus', refreshOnWindowReturn)
  document.addEventListener('visibilitychange', refreshOnWindowReturn)
})

onBeforeUnmount(() => {
  window.removeEventListener('focus', refreshOnWindowReturn)
  document.removeEventListener('visibilitychange', refreshOnWindowReturn)
})
watch(() => route.params.id, loadData)
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
          <strong>{{ isFreeCourse ? '免费' : `¥${detail.course.price}` }}</strong>
          <span v-if="!isFreeCourse">原价 ¥{{ detail.course.originalPrice }}</span>
        </div>
        <div class="rating-row">
          <el-rate :model-value="Number(detail.averageRating || 0)" disabled show-score text-color="#d97706" score-template="{value} 分" />
          <span class="muted">共 {{ detail.commentCount || 0 }} 条评价</span>
        </div>
        <div class="hint-row">
          <el-tag v-if="isFreeCourse" type="success">免费课程</el-tag>
          <el-tag v-else-if="detail.canTrial" type="warning">支持 {{ detail.trialChapterCount }} 节试听</el-tag>
          <el-tag v-else type="info">购课后解锁完整学习</el-tag>
        </div>
        <div class="action-row">
          <template v-if="detail.purchased">
            <el-button type="success" size="large" :loading="actionLoading" @click="startLearning">进入学习</el-button>
            <el-button size="large" @click="router.push('/my-course')">查看我的课程</el-button>
          </template>
          <template v-else-if="isFreeCourse">
            <el-button type="success" size="large" :loading="actionLoading" @click="startLearning">免费加入学习</el-button>
          </template>
          <template v-else>
            <el-button type="primary" size="large" @click="buyNow">立即下单</el-button>
            <el-button size="large" :disabled="detail.inCart" @click="addToCart">
              {{ detail.inCart ? '已在购物车' : '加入购物车' }}
            </el-button>
            <el-button v-if="detail.canTrial" size="large" plain @click="previewCourse">试听章节</el-button>
          </template>
        </div>
      </div>
    </section>

    <section class="glass-card detail-content section-gap">
      <el-tabs>
        <el-tab-pane label="课程介绍">
          <p class="description">{{ detail.course.highlights }}</p>
          <p class="description">适合人群：{{ detail.course.targetUsers }}</p>
          <p class="description">有效期：{{ detail.course.validityDays }} 天</p>
        </el-tab-pane>
        <el-tab-pane label="课程安排">
          <el-timeline>
            <el-timeline-item v-for="item in detail.chapters" :key="item.id" :timestamp="item.liveStartTime || `${item.durationMinutes} 分钟`">
              <div class="timeline-title-row">
                <strong>{{ item.title }}</strong>
                <el-tag v-if="item.trialAvailable" size="small" type="warning">可试听</el-tag>
              </div>
              <div>{{ item.chapterType }} · {{ item.durationMinutes }} 分钟</div>
            </el-timeline-item>
          </el-timeline>
        </el-tab-pane>
        <el-tab-pane label="资料下载">
          <div class="material-list">
            <div v-for="item in detail.materials" :key="item.id" class="material-item">
              <span>{{ item.title }}</span>
              <a v-if="item.fileUrl" :href="resolveAssetUrl(item.fileUrl)" target="_blank">下载</a>
              <span v-else class="muted">{{ isFreeCourse ? '暂无资料' : '购课后下载' }}</span>
            </div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="用户评价">
          <div v-if="detail.canComment" class="comment-editor">
            <div class="comment-editor-head">
              <strong>写下你的评价</strong>
              <el-rate v-model="commentForm.rating" />
            </div>
            <el-input v-model="commentForm.content" type="textarea" :rows="3" maxlength="300" show-word-limit placeholder="说说这门课对你的帮助、资料质量或老师讲解感受" />
            <div class="comment-editor-action">
              <el-button type="primary" :loading="commentSubmitting" @click="submitComment">提交评价</el-button>
            </div>
          </div>
          <div v-else class="comment-editor locked-comment">
            <strong>购买或加入课程后才能评价</strong>
            <span>{{ isFreeCourse ? '请先免费加入学习，再分享你的学习体验。' : '已购学员完成支付后可以提交课程评价。' }}</span>
          </div>
          <div class="comment-list">
            <div v-for="item in detail.comments" :key="item.id" class="comment-item">
              <div class="comment-head">
                <div class="comment-user">
                  <el-avatar :src="item.avatarUrl" :size="32">{{ (item.nickname || '学').slice(0, 1) }}</el-avatar>
                  <strong>{{ item.nickname }}</strong>
                </div>
                <div class="comment-score">
                  <el-rate :model-value="item.rating" disabled size="small" />
                  <span>{{ item.rating }} 分</span>
                </div>
              </div>
              <div>{{ item.content }}</div>
              <div class="comment-time">{{ item.createdAt }}</div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <section class="glass-card detail-content section-gap">
      <div class="section-title">相关推荐</div>
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

.action-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.rating-row,
.hint-row,
.timeline-title-row {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
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

.comment-editor,
.material-item,
.comment-item,
.related-card {
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 14px;
}

.comment-editor-head,
.comment-editor-action,
.comment-user,
.comment-score {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.comment-editor-action {
  justify-content: flex-end;
  margin-top: 12px;
}

.locked-comment {
  display: grid;
  gap: 6px;
  color: var(--muted);
}

.locked-comment strong {
  color: var(--text);
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

.comment-time {
  margin-top: 10px;
  color: var(--muted);
  font-size: 12px;
}

@media (max-width: 960px) {
  .detail-hero {
    grid-template-columns: 1fr;
  }
}
</style>
