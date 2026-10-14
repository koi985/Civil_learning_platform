<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const loadingCourseId = ref(null)
let lastRefreshAt = 0
const subjectFilters = ['国考', '省考', '事业单位', '选调', '面试', '军队文职']
const query = reactive({
  pageNo: 1,
  pageSize: 8,
  keyword: '',
  province: '',
  subjectName: '',
  examType: '',
  courseType: '',
  minPrice: undefined,
  maxPrice: undefined,
  sortBy: 'RECOMMENDED'
})

const activeFilterCount = computed(() => [
  query.keyword,
  query.province,
  query.subjectName,
  query.examType,
  query.courseType,
  query.minPrice,
  query.maxPrice,
  query.sortBy !== 'RECOMMENDED' ? query.sortBy : ''
].filter(item => item !== '' && item !== undefined && item !== null).length)

async function loadData() {
  lastRefreshAt = Date.now()
  loading.value = true
  try {
    const data = await request.get('/api/courses', { params: query })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handlePage(pageNo) {
  query.pageNo = pageNo
  loadData()
}

function handleSearch() {
  query.pageNo = 1
  loadData()
}

function selectSubjectFilter(item) {
  query.examType = item
  query.pageNo = 1
  loadData()
}

function resetFilters() {
  Object.assign(query, {
    pageNo: 1,
    pageSize: 8,
    keyword: '',
    province: '',
    subjectName: '',
    examType: '',
    courseType: '',
    minPrice: undefined,
    maxPrice: undefined,
    sortBy: 'RECOMMENDED'
  })
  loadData()
}

async function addToCart(course) {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后再加入购物车')
    router.push('/login')
    return
  }
  loadingCourseId.value = course.id
  try {
    await request.post('/api/cart/items', { courseId: course.id })
    ElMessage.success('已加入购物车')
  } finally {
    loadingCourseId.value = null
  }
}

async function claimFreeCourse(course) {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后再进入学习')
    router.push('/login')
    return
  }
  loadingCourseId.value = course.id
  try {
    await request.post(`/api/user-center/courses/${course.id}/claim-free`)
    ElMessage.success('免费课程已加入我的课程')
    await loadData()
    router.push('/my-course')
  } finally {
    loadingCourseId.value = null
  }
}

async function markNotInterested(course) {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('登录后可反馈不感兴趣')
    router.push('/login')
    return
  }
  loadingCourseId.value = course.id
  try {
    await request.post('/api/recommend/feedback', {
      resourceType: 'COURSE',
      resourceId: course.id,
      sourcePage: 'COURSE_LIST',
      reasonText: `课程页反馈：对 ${course.title} 暂不感兴趣`
    })
    ElMessage.success('已减少同类课程推荐')
    await loadData()
  } finally {
    loadingCourseId.value = null
  }
}

function isFree(course) {
  return Number(course.price || 0) <= 0
}

function scoreText(value) {
  const number = Number(value || 0)
  return `${Math.round(number * 100)}分`
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
</script>

<template>
  <div class="page-shell">
    <section class="glass-card filter-box">
      <div class="filter-head">
        <div>
          <div class="section-title">课程中心</div>
          <div class="filter-tip">按条件查找适合你的课程</div>
        </div>
        <el-tag type="primary" effect="plain">已启用 {{ activeFilterCount }} 个筛选项</el-tag>
      </div>
      <div class="subject-filter" aria-label="课程科目筛选">
        <el-button
          v-for="item in subjectFilters"
          :key="item"
          :type="query.examType === item ? 'primary' : 'default'"
          :plain="query.examType !== item"
          @click="selectSubjectFilter(item)"
        >
          {{ item }}
        </el-button>
      </div>
      <div class="filter-grid">
        <el-input v-model="query.keyword" placeholder="搜索课程标题" clearable />
        <el-input v-model="query.province" placeholder="省份" clearable />
        <el-input v-model="query.subjectName" placeholder="科目" clearable />
        <el-select v-model="query.examType" placeholder="考试类型" clearable @change="handleSearch" @clear="handleSearch">
          <el-option v-for="item in subjectFilters" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="query.courseType" placeholder="课程类型" clearable>
          <el-option label="直播" value="LIVE" />
          <el-option label="录播" value="RECORDED" />
        </el-select>
        <el-input-number v-model="query.minPrice" :min="0" :step="10" placeholder="最低价" style="width: 100%" />
        <el-input-number v-model="query.maxPrice" :min="0" :step="10" placeholder="最高价" style="width: 100%" />
        <el-select v-model="query.sortBy" placeholder="排序方式">
          <el-option label="为你推荐" value="RECOMMENDED" />
          <el-option label="热门优先" value="POPULAR" />
          <el-option label="最新上架" value="LATEST" />
          <el-option label="价格从低到高" value="PRICE_ASC" />
          <el-option label="价格从高到低" value="PRICE_DESC" />
        </el-select>
      </div>
      <div class="filter-actions">
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </section>

    <section class="glass-card list-box" v-loading="loading">
      <div class="course-grid">
        <div v-for="course in list" :key="course.id" class="course-card">
          <div class="course-cover-wrap">
            <img :src="course.coverUrl" alt="" class="course-cover" />
            <div class="course-cover-shade"></div>
            <div class="course-tags">
              <el-tag size="small" effect="dark">{{ course.courseType === 'LIVE' ? '直播' : '录播' }}</el-tag>
              <span>{{ course.tags }}</span>
            </div>
          </div>
          <div class="course-body">
            <h3 class="course-title" @click="router.push(`/course/detail/${course.id}`)">{{ course.title }}</h3>
            <div class="teacher-row">
              <img :src="course.teacherAvatar || course.coverUrl" alt="" class="teacher-avatar" />
              <span>{{ course.teacherName }}</span>
              <span class="dot">·</span>
              <span>{{ course.province }} {{ course.subjectName }}</span>
            </div>
            <div v-if="course.recommended" class="recommend-panel">
              <div class="recommend-head">
                <span class="recommend-badge">为你推荐</span>
                <span class="recommend-score">{{ scoreText(course.finalScore) }}</span>
              </div>
              <p>{{ course.recommendReason }}</p>
              <div v-if="course.explainTags?.length" class="recommend-tags">
                <el-tag v-for="tag in course.explainTags.slice(0, 3)" :key="`${course.id}-${tag}`" size="small" effect="plain">
                  {{ tag }}
                </el-tag>
              </div>
              <el-button
                text
                size="small"
                :loading="loadingCourseId === course.id"
                @click.stop="markNotInterested(course)"
              >
                不感兴趣
              </el-button>
            </div>
            <div class="course-foot">
              <div class="price-box">
                <span class="price">{{ isFree(course) ? '免费' : `¥${course.price}` }}</span>
                <span v-if="!isFree(course) && course.originalPrice" class="original-price">¥{{ course.originalPrice }}</span>
              </div>
              <span>{{ course.learnCount }} 人学习</span>
            </div>
            <div class="action-row">
              <el-button type="primary" plain @click="router.push(`/course/detail/${course.id}`)">详情</el-button>
              <el-button
                v-if="isFree(course)"
                type="success"
                :loading="loadingCourseId === course.id"
                @click="claimFreeCourse(course)"
              >
                免费学习
              </el-button>
              <el-button
                v-else
                :loading="loadingCourseId === course.id"
                @click="addToCart(course)"
              >
                加购物车
              </el-button>
            </div>
          </div>
        </div>
      </div>
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.pageNo"
        style="margin-top: 24px; justify-content: center"
        @current-change="handlePage"
      />
    </section>
  </div>
</template>

<style scoped>
.filter-box,
.list-box {
  padding: 24px;
}

.list-box {
  margin-top: 24px;
}

.filter-head,
.filter-actions,
.teacher-row,
.action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-tip {
  color: var(--muted);
  margin-top: 6px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.subject-filter {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.filter-actions {
  margin-top: 16px;
  justify-content: flex-end;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(248px, 1fr));
  gap: 20px;
}

.course-card {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 10px;
  background: #fff;
  min-height: 420px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.course-card:hover {
  border-color: rgba(37, 99, 235, 0.32);
  box-shadow: 0 16px 32px rgba(15, 23, 42, 0.1);
  transform: translateY(-2px);
}

.course-cover-wrap {
  position: relative;
  height: 34%;
  min-height: 92px;
  border-radius: 6px;
  overflow: hidden;
  background: #e2e8f0;
}

.course-cover {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.course-cover-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.04), rgba(15, 23, 42, 0.54));
}

.course-tags {
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: 10px;
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.course-tags span:last-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 10px 2px 0;
}

.course-title {
  cursor: pointer;
  margin: 0;
  color: #111827;
  font-size: 16px;
  line-height: 1.32;
  font-weight: 800;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.course-title:hover {
  color: var(--primary);
}

.teacher-row {
  color: var(--muted);
  justify-content: flex-start;
  margin: 8px 0 0;
  font-size: 13px;
  flex-wrap: nowrap;
  overflow: hidden;
}

.teacher-row span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.teacher-avatar {
  width: 24px;
  height: 24px;
  flex: 0 0 24px;
  border-radius: 50%;
  object-fit: cover;
}

.dot {
  color: #cbd5e1;
}

.recommend-panel {
  margin-top: 8px;
  padding: 8px;
  border-radius: 6px;
  background: #f8fbff;
  border: 1px solid #dbeafe;
}

.recommend-head,
.recommend-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.recommend-head {
  justify-content: space-between;
}

.recommend-badge {
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
}

.recommend-score {
  color: #64748b;
  font-size: 12px;
  white-space: nowrap;
}

.recommend-panel p {
  margin: 5px 0 6px;
  color: #475569;
  font-size: 12px;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.recommend-tags {
  flex-wrap: nowrap;
  overflow: hidden;
}

.recommend-tags .el-tag {
  max-width: 88px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.recommend-panel .el-button {
  margin-top: 4px;
  padding: 0;
  height: 20px;
  color: #64748b;
}

.course-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--muted);
  gap: 10px;
  margin-top: auto;
  padding-top: 8px;
  font-size: 13px;
}

.price {
  color: var(--accent);
  font-size: 20px;
  font-weight: 700;
  white-space: nowrap;
}

.price-box {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.original-price {
  color: var(--muted);
  text-decoration: line-through;
  white-space: nowrap;
}

.action-row {
  margin-top: 10px;
  justify-content: space-between;
  flex-wrap: nowrap;
}

.action-row .el-button {
  flex: 1;
  min-width: 0;
  height: 32px;
  font-weight: 700;
  padding: 8px 10px;
}

.action-row .el-button + .el-button {
  margin-left: 0;
}

@media (max-width: 640px) {
  .course-grid {
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    gap: 14px;
  }

  .course-card {
    padding: 8px;
    min-height: 390px;
  }

  .course-cover-wrap {
    min-height: 82px;
  }

  .course-title {
    font-size: 15px;
  }

  .teacher-row,
  .course-foot {
    font-size: 12px;
  }

  .teacher-avatar {
    display: none;
  }

  .price {
    font-size: 18px;
  }

  .action-row .el-button {
    height: 30px;
    padding: 6px 8px;
    font-size: 12px;
  }
}
</style>
