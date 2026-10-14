<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(true)
const activeBannerIndex = ref(0)
let loadDataSeq = 0
let lastRefreshAt = 0
const home = ref({
  banners: [],
  notices: [],
  recommendCards: [],
  hotCourses: [],
  latestNews: [],
  hotNews: [],
  recommendedClassrooms: [],
  latestExams: [],
  questionTopics: [],
  teacherHighlights: [],
  quickEntries: [],
  authCard: {},
  personalizedResources: { loggedIn: false, resources: [] },
  footerSections: []
})

const recommendTitle = computed(() => home.value.personalizedResources?.loggedIn ? '为你推荐' : '热门推荐')
const workbenchPath = computed(() => {
  if (authStore.roles.includes('ADMIN')) return '/admin/dashboard'
  if (authStore.roles.includes('TEACHER')) return '/teacher/dashboard'
  return '/user'
})
const workbenchText = computed(() => {
  if (authStore.roles.includes('ADMIN')) return '运营后台'
  if (authStore.roles.includes('TEACHER')) return '教师工作台'
  return '个人中心'
})

const serviceItems = [
  { icon: '课', title: '课程学习', desc: '系统课程、讲义资料和进度同步' },
  { icon: '题', title: '题库练习', desc: '专项刷题、错题沉淀和解析回看' },
  { icon: '考', title: '模考训练', desc: '阶段测评、成绩报告和排名参考' },
  { icon: '班', title: '班级教学', desc: '教师带学、作业考试和学情反馈' },
  { icon: '安', title: '数据安全', desc: '账号隔离、权限访问和隐私保护' }
]

const resourceMetaMap = {
  COURSE: { label: '课程', icon: '课', tone: 'course', action: '继续学习' },
  QUESTION: { label: '题库', icon: '题', tone: 'question', action: '去刷题' },
  PRACTICE: { label: '练习', icon: '练', tone: 'question', action: '查看练习' },
  EXAM: { label: '模考', icon: '考', tone: 'exam', action: '进入模考' },
  NEWS: { label: '资讯', icon: '讯', tone: 'news', action: '查看资讯' }
}

function resourceMeta(type) {
  return resourceMetaMap[type] || { label: '资源', icon: '学', tone: 'default', action: '查看详情' }
}

function hasDebugTitle(title) {
  return /playwright|multi-exam|test-|测试/i.test(String(title || ''))
}

function cardTitle(item) {
  const meta = resourceMeta(item.resourceType)
  if (!hasDebugTitle(item.title)) {
    return item.title || `${meta.label}推荐`
  }
  if (item.resourceType === 'EXAM') {
    return '近期模考能力测评'
  }
  if (item.resourceType === 'QUESTION' || item.resourceType === 'PRACTICE') {
    return '高频考点专项练习'
  }
  return `${meta.label}精选资源`
}

function cardMeta(item) {
  return item.tags || item.reason || '根据学习热度与备考阶段推荐'
}

function cardAction(item) {
  return item.actionText || resourceMeta(item.resourceType).action
}

function continueTitle(item) {
  const meta = resourceMeta(item.type)
  if (!hasDebugTitle(item.title)) {
    return item.title || `${meta.label}学习入口`
  }
  return item.type === 'EXAM' ? '最近模考报告' : `${meta.label}学习入口`
}

function teacherMetric(seed, base, range, suffix = '') {
  const value = base + (Number(seed || 0) % range)
  return `${value}${suffix}`
}

function formatNoticeTime(value) {
  return formatDateTime(value, '平台公告')
}

function formatDateTime(value, fallback = '平台动态') {
  if (!value) {
    return fallback
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value).replace('T', ' ').slice(0, 16)
  }
  const pad = (number) => String(number).padStart(2, '0')
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function newsType(title = '', author = '') {
  const text = `${title} ${author}`
  if (/公告|招考|国考|省考/.test(text)) {
    return '招考公告'
  }
  if (/申论|材料|写作/.test(text)) {
    return '申论技巧'
  }
  if (/面试|基层治理|案例/.test(text)) {
    return '面试热点'
  }
  if (/时政|联考|热点/.test(text)) {
    return '时政热点'
  }
  return '备考指导'
}

function newsSummary(item) {
  if (item?.summary) {
    return item.summary
  }
  const type = newsType(item?.title, item?.authorName)
  if (type === '招考公告') {
    return '梳理近期招录公告与重要时间节点，帮助你快速把握备考安排。'
  }
  if (type === '申论技巧') {
    return '围绕材料阅读、观点提炼和规范表达，拆解申论复习重点。'
  }
  if (type === '面试热点') {
    return '结合结构化面试常见话题，沉淀可迁移的答题思路。'
  }
  return '精选公考备考动态与复习方法，辅助你调整阶段学习节奏。'
}

function rankText(index) {
  return String(index + 1).padStart(2, '0')
}

function noticeType(title = '') {
  if (/课程|资料/.test(title)) {
    return '课程通知'
  }
  if (/模考|考试|测评/.test(title)) {
    return '模考提醒'
  }
  if (/系统|上线|维护/.test(title)) {
    return '系统通知'
  }
  return '平台公告'
}

function noticeTone(title = '') {
  if (/重要|紧急|维护|考试|模考/.test(title)) {
    return 'is-urgent'
  }
  if (/课程|资料/.test(title)) {
    return 'is-course'
  }
  return 'is-normal'
}

function handleBannerChange(currentIndex) {
  activeBannerIndex.value = Number(currentIndex) || 0
}

function openActiveBanner() {
  const banner = home.value.banners?.[activeBannerIndex.value]
  router.push(banner?.linkUrl || '/course')
}

async function loadData() {
  const seq = ++loadDataSeq
  lastRefreshAt = Date.now()
  loading.value = true
  try {
    try {
      home.value = await request.get('/api/home/public-overview', { anonymous: true, silent: true })
    } catch (error) {
      home.value = await request.get('/api/home/overview', { anonymous: true })
    }
    activeBannerIndex.value = 0
  } finally {
    loading.value = false
  }
  if (!authStore.isLoggedIn || seq !== loadDataSeq) {
    return
  }
  try {
    const personalized = await request.get('/api/home/personalized', { silent: true })
    if (seq === loadDataSeq) {
      home.value = { ...home.value, ...personalized }
    }
  } catch (error) {
    // Public homepage content is already rendered; personalized blocks can retry on the next refresh.
  }
}

async function openRecommendCard(item) {
  if (item.resourceType === 'PRACTICE' && item.practiceBundleId) {
    const data = await request.post(`/api/recommend/practice-bundles/${item.practiceBundleId}/start`)
    ElMessage.success('练习已生成')
    router.push(`/practice/${data.practiceId}`)
    return
  }
  router.push(item.targetPath || item.path || '/recommend')
}

function openTeacher(item) {
  if (item?.id) {
    router.push(`/teacher/public/${item.id}`)
  }
}

function handleRegionChanged() {
  loadData()
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
  window.addEventListener('civil-region-changed', handleRegionChanged)
  window.addEventListener('focus', refreshOnWindowReturn)
  document.addEventListener('visibilitychange', refreshOnWindowReturn)
})

onBeforeUnmount(() => {
  window.removeEventListener('civil-region-changed', handleRegionChanged)
  window.removeEventListener('focus', refreshOnWindowReturn)
  document.removeEventListener('visibilitychange', refreshOnWindowReturn)
})
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section class="hero glass-card">
      <div class="hero-banner">
        <el-carousel height="360px" @change="handleBannerChange">
          <el-carousel-item v-for="(banner, index) in home.banners" :key="banner.id">
            <div class="banner-card">
              <img
                :src="banner.imageUrl"
                :alt="banner.title"
                width="1160"
                height="360"
                :loading="index === 0 ? 'eager' : 'lazy'"
                decoding="async"
                :fetchpriority="index === 0 ? 'high' : 'auto'"
              />
            </div>
          </el-carousel-item>
        </el-carousel>
        <button type="button" class="banner-hit-area" :aria-label="home.banners?.[activeBannerIndex]?.title || '首页轮播'" @click="openActiveBanner"></button>
      </div>
    </section>

    <section class="glass-card section-box section-gap">
      <div class="section-header">
        <div class="section-title">热卖课程</div>
        <el-button text @click="router.push('/course')">查看全部</el-button>
      </div>
      <div class="card-grid">
        <div v-for="course in home.hotCourses" :key="course.id" class="resource-card" @click="router.push(`/course/detail/${course.id}`)">
          <img :src="course.coverUrl" alt="" width="280" height="150" loading="lazy" decoding="async" />
          <h3>{{ course.title }}</h3>
          <p>{{ course.teacherName }} · {{ course.subjectName }} · {{ course.examType }}</p>
          <div class="resource-foot">
            <span class="price">{{ Number(course.price || 0) <= 0 ? '免费' : `¥${course.price}` }}</span>
            <span>{{ course.learnCount }} 人学习</span>
          </div>
        </div>
      </div>
    </section>

    <section class="two-column section-gap">
      <div class="glass-card section-box">
        <div class="section-header">
          <div class="section-title">{{ recommendTitle }}</div>
          <el-button text @click="router.push('/recommend')">查看全部推荐</el-button>
        </div>
        <div class="recommend-grid">
          <div v-for="item in home.recommendCards" :key="`${item.resourceType}-${item.id}`" class="recommend-card" @click="openRecommendCard(item)">
            <div class="recommend-visual" :class="`is-${resourceMeta(item.resourceType).tone}`">
              <img v-if="item.coverUrl" :src="item.coverUrl" :alt="cardTitle(item)" width="180" height="112" loading="lazy" decoding="async" />
              <span v-else>{{ resourceMeta(item.resourceType).icon }}</span>
            </div>
            <div class="recommend-body">
              <div class="recommend-topline">
                <span class="recommend-tag" :class="`is-${resourceMeta(item.resourceType).tone}`">{{ resourceMeta(item.resourceType).label }}</span>
                <span class="recommend-reason">{{ item.reason || '智能推荐' }}</span>
              </div>
              <div class="recommend-name">{{ cardTitle(item) }}</div>
              <div class="recommend-meta">{{ cardMeta(item) }}</div>
              <div v-if="item.resourceType === 'PRACTICE' && (item.recommendMode || item.thetaRange)" class="recommend-meta">
                {{ item.recommendMode || 'PRACTICE' }}<span v-if="item.thetaRange"> · 能力 {{ item.thetaRange }}</span>
              </div>
              <div class="recommend-action">{{ cardAction(item) }}</div>
            </div>
          </div>
        </div>
      </div>
      <div class="glass-card section-box continue-panel">
        <div class="section-header">
          <div>
            <div class="section-title">最近继续</div>
            <div class="section-subtitle">课程进度、练习记录和模考报告</div>
          </div>
          <el-button v-if="authStore.isLoggedIn" text @click="router.push(workbenchPath)">{{ workbenchText }}</el-button>
          <el-button v-else text @click="router.push('/login')">登录解锁</el-button>
        </div>
        <p class="panel-summary">{{ home.personalizedResources.summary }}</p>
        <div class="list-stack">
          <div v-for="item in home.personalizedResources.resources" :key="`${item.type}-${item.title}`" class="stack-item continue-item" @click="router.push(item.path)">
            <div class="continue-icon" :class="`is-${resourceMeta(item.type).tone}`">{{ resourceMeta(item.type).icon }}</div>
            <div class="continue-copy">
              <div class="stack-title">{{ continueTitle(item) }}</div>
              <div class="stack-meta">{{ item.subtitle || resourceMeta(item.type).label }}</div>
            </div>
            <div class="stack-action">{{ item.actionText || resourceMeta(item.type).action }}</div>
          </div>
          <div v-if="!home.personalizedResources.resources.length" class="empty-card">
            登录后可同步课程学习、练习记录与模考报告。
          </div>
        </div>
      </div>
    </section>

    <section class="two-column section-gap">
      <div class="glass-card section-box gold-teacher-panel">
        <div class="section-header">
          <div>
            <div class="section-title">金牌讲师</div>
            <div class="teacher-panel-subtitle">按学员反馈、授课活跃度和课程完成率精选</div>
          </div>
          <div class="teacher-actions">
            <el-button text @click="router.push('/course')">查看课程</el-button>
            <el-button text @click="router.push('/teacher/apply')">成为教师</el-button>
          </div>
        </div>
        <div class="gold-teacher-grid">
          <div
            v-for="item in home.teacherHighlights.slice(0, 4)"
            :key="item.id"
            class="gold-teacher-card"
            role="button"
            tabindex="0"
            @click="openTeacher(item)"
            @keydown.enter="openTeacher(item)"
          >
            <img :src="item.avatarUrl" :alt="item.nickname || '平台讲师'" class="teacher-avatar" width="56" height="56" loading="lazy" decoding="async" />
            <div class="gold-teacher-info">
              <div class="gold-teacher-head">
                <div class="gold-teacher-name">{{ item.nickname || '平台讲师' }}</div>
                <span>认证讲师</span>
              </div>
              <div class="gold-teacher-meta">{{ item.province || '全国' }} · {{ item.preferredSubject || '综合辅导' }}</div>
              <div class="gold-teacher-intro">{{ item.intro || '专注公考备考辅导，提供系统化学习支持。' }}</div>
              <div class="teacher-metrics">
                <span>{{ teacherMetric(item.id, 3, 5, '年+') }}经验</span>
                <span>{{ teacherMetric(item.id, 960, 520) }}学员</span>
                <span>{{ teacherMetric(item.id, 96, 4, '%') }}好评</span>
              </div>
            </div>
          </div>
          <div v-if="!home.teacherHighlights.length" class="empty-card">
            暂无讲师推荐，后续会在这里展示平台金牌讲师。
          </div>
        </div>
      </div>
      <div class="glass-card section-box notice-panel">
        <div class="section-header">
          <div>
            <div class="section-title">公告中心</div>
            <div class="notice-subtitle">平台通知与重要动态</div>
          </div>
          <el-button text @click="router.push('/notice')">查看全部</el-button>
        </div>
        <div v-if="home.notices.length" class="notice-showcase" @click="router.push(`/notice/detail/${home.notices[0].id}`)">
          <div class="notice-badge-row">
            <span class="notice-badge">最新公告</span>
            <span class="notice-type" :class="noticeTone(home.notices[0].title)">{{ noticeType(home.notices[0].title) }}</span>
          </div>
          <h3>{{ home.notices[0].title }}</h3>
          <p>{{ home.notices[0].summary || '点击查看公告详情' }}</p>
          <div class="notice-meta">
            <span>{{ formatNoticeTime(home.notices[0].publishedAt) }}</span>
            <strong>查看详情</strong>
          </div>
        </div>
        <div class="notice-mini">
          <div
            v-for="notice in home.notices.slice(1, 5)"
            :key="notice.id"
            class="mini-item"
            @click="router.push(`/notice/detail/${notice.id}`)"
          >
            <div class="mini-copy">
              <span>{{ notice.title }}</span>
              <em>{{ noticeType(notice.title) }} · {{ formatNoticeTime(notice.publishedAt) }}</em>
            </div>
            <strong>查看</strong>
          </div>
          <div v-if="!home.notices.length" class="empty-card">
            暂无公告，后续通知会在这里展示。
          </div>
        </div>
      </div>
    </section>

    <section class="two-column section-gap news-section">
      <div class="glass-card section-box latest-news-panel">
        <div class="section-header">
          <div>
            <div class="section-title">最新资讯</div>
            <div class="section-subtitle">公告动态、备考方法和考试节点</div>
          </div>
          <el-button text @click="router.push('/news')">查看全部</el-button>
        </div>
        <div v-if="home.latestNews.length" class="latest-feature" @click="router.push(`/news/detail/${home.latestNews[0].id}`)">
          <div class="news-meta-row">
            <span class="news-tag">{{ newsType(home.latestNews[0].title, home.latestNews[0].authorName) }}</span>
            <span>{{ formatDateTime(home.latestNews[0].publishedAt) }}</span>
          </div>
          <h3>{{ home.latestNews[0].title }}</h3>
          <p>{{ newsSummary(home.latestNews[0]) }}</p>
          <div class="news-feature-foot">
            <span>{{ home.latestNews[0].authorName || '编辑部' }}</span>
            <strong>阅读</strong>
          </div>
        </div>
        <div class="latest-list">
          <div v-for="item in home.latestNews.slice(1, 4)" :key="item.id" class="latest-item" @click="router.push(`/news/detail/${item.id}`)">
            <div>
              <div class="news-item-title">{{ item.title }}</div>
              <div class="news-item-meta">
                <span>{{ newsType(item.title, item.authorName) }}</span>
                <span>{{ item.authorName || '资讯组' }}</span>
                <span>{{ formatDateTime(item.publishedAt) }}</span>
              </div>
            </div>
            <strong>查看</strong>
          </div>
        </div>
      </div>
      <div class="glass-card section-box hot-news-panel">
        <div class="section-header">
          <div>
            <div class="section-title">热门资讯</div>
            <div class="section-subtitle">按阅读热度排序</div>
          </div>
          <el-button text @click="router.push('/news')">查看全部</el-button>
        </div>
        <div class="hot-rank-list">
          <div v-for="(item, index) in home.hotNews.slice(0, 5)" :key="`hot-${item.id}`" class="hot-rank-item" @click="router.push(`/news/detail/${item.id}`)">
            <div class="rank-number">{{ rankText(index) }}</div>
            <div class="rank-copy">
              <div class="news-item-title">{{ item.title }}</div>
              <div class="news-item-meta">
                <span>{{ newsType(item.title, item.authorName) }}</span>
                <span>热度 {{ item.readCount || 0 }}</span>
              </div>
            </div>
            <strong>查看</strong>
          </div>
        </div>
      </div>
    </section>

    <section class="glass-card section-box section-gap service-panel">
      <div class="section-header">
        <div>
          <div class="section-title">平台服务</div>
          <div class="section-subtitle">围绕备考过程提供学习、训练、教学和数据保障</div>
        </div>
      </div>
      <div class="service-grid">
        <div v-for="item in serviceItems" :key="item.title" class="service-card">
          <div class="service-icon">{{ item.icon }}</div>
          <div>
            <div class="service-title">{{ item.title }}</div>
            <div class="service-desc">{{ item.desc }}</div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.hero {
  padding: 16px;
  overflow: hidden;
}

.hero-banner {
  position: relative;
  width: 100%;
}

.hero-banner :deep(.el-carousel__item) {
  pointer-events: none;
}

.hero-banner :deep(.el-carousel__item.is-active) {
  pointer-events: auto;
}

.banner-card {
  height: 100%;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  background: #e2e8f0;
}

.banner-card img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.banner-hit-area {
  position: absolute;
  inset: 0 0 34px;
  z-index: 2;
  border: 0;
  background: transparent;
  cursor: pointer;
}

.hero-banner :deep(.el-carousel__indicators) {
  z-index: 3;
}

.section-gap {
  margin-top: 24px;
}

.section-box {
  padding: 24px;
}

.teacher-grid,
.footer-grid,
.gold-teacher-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.section-header .section-title {
  margin-bottom: 0;
}

.section-subtitle {
  margin-top: 6px;
  color: var(--muted);
  font-size: 14px;
}

.recommend-grid,
.card-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.recommend-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: stretch;
}

.recommend-card,
.resource-card,
.stack-item,
.notice-item,
.teacher-card,
.gold-teacher-card,
.footer-card,
.empty-card {
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 16px;
  background: #fff;
}

.recommend-card {
  display: grid;
  grid-template-rows: 86px 1fr;
  min-height: 210px;
  border-radius: 8px;
  padding: 12px;
  border-color: rgba(203, 213, 225, 0.78);
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.recommend-card:hover {
  border-color: rgba(37, 99, 235, 0.3);
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.recommend-visual {
  position: relative;
  display: grid;
  place-items: center;
  border-radius: 8px;
  overflow: hidden;
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #0f766e);
}

.recommend-visual::after {
  content: "";
  position: absolute;
  inset: auto 16px -26px auto;
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.18);
}

.recommend-visual img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.recommend-visual span {
  position: relative;
  z-index: 1;
  font-size: 30px;
  font-weight: 800;
}

.recommend-visual.is-question {
  background: linear-gradient(135deg, #0f766e, #2563eb);
}

.recommend-visual.is-exam {
  background: linear-gradient(135deg, #f97316, #2563eb);
}

.recommend-visual.is-news {
  background: linear-gradient(135deg, #475569, #0f766e);
}

.recommend-body {
  display: flex;
  min-width: 0;
  flex-direction: column;
  padding: 12px 2px 0;
}

.recommend-topline {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.recommend-tag {
  display: inline-flex;
  flex: 0 0 auto;
  padding: 4px 10px;
  background: rgba(37, 99, 235, 0.1);
  color: var(--primary);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
}

.recommend-tag.is-question {
  background: rgba(15, 118, 110, 0.1);
  color: #0f766e;
}

.recommend-tag.is-exam {
  background: rgba(249, 115, 22, 0.12);
  color: var(--accent);
}

.recommend-name {
  margin-top: 10px;
  color: #0f172a;
  font-size: 19px;
  font-weight: 700;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.recommend-action,
.teacher-name,
.gold-teacher-name,
.footer-title,
.mini-title {
  font-weight: 700;
}

.recommend-action {
  margin-top: auto;
  padding-top: 10px;
  color: var(--primary);
}

.recommend-reason,
.recommend-meta,
.notice-summary,
.stack-meta,
.resource-card p,
.auth-copy,
.teacher-intro,
.gold-teacher-meta,
.gold-teacher-intro,
.footer-line,
.panel-summary {
  color: var(--muted);
}

.recommend-reason {
  min-width: 0;
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-meta {
  margin-top: 6px;
  min-height: 20px;
  overflow: hidden;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-card,
.recommend-card,
.notice-showcase,
.mini-item,
.teacher-card,
.gold-teacher-card {
  cursor: pointer;
}

.resource-card img {
  width: 100%;
  height: 160px;
  border-radius: 14px;
  object-fit: cover;
}

.resource-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--muted);
}

.price {
  color: var(--accent);
  font-size: 20px;
  font-weight: 700;
}

.list-stack,
.notice-list {
  display: grid;
  gap: 14px;
}

.continue-panel {
  align-self: start;
}

.continue-item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  border-radius: 8px;
  padding: 14px;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.continue-item:hover {
  border-color: rgba(37, 99, 235, 0.3);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.07);
  transform: translateY(-1px);
}

.continue-icon {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 8px;
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #0f766e);
  font-size: 18px;
  font-weight: 800;
}

.continue-icon.is-question {
  background: linear-gradient(135deg, #0f766e, #2563eb);
}

.continue-icon.is-exam {
  background: linear-gradient(135deg, #f97316, #2563eb);
}

.continue-copy {
  min-width: 0;
}

.stack-title {
  overflow: hidden;
  color: #0f172a;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stack-action {
  color: var(--primary);
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.notice-mini {
  margin-top: 16px;
}

.gold-teacher-panel {
  position: relative;
  overflow: hidden;
}

.gold-teacher-panel::before {
  content: "";
  position: absolute;
  inset: 0 0 auto;
  height: 4px;
  background: linear-gradient(90deg, #f97316, #2563eb);
}

.teacher-panel-subtitle {
  margin-top: 6px;
  color: var(--muted);
  font-size: 14px;
}

.gold-teacher-grid {
  margin-top: 18px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.gold-teacher-card {
  display: flex;
  min-height: 178px;
  gap: 16px;
  align-items: flex-start;
  border-radius: 8px;
  padding: 18px;
  background:
    linear-gradient(135deg, rgba(248, 250, 252, 0.92), rgba(239, 246, 255, 0.82)),
    #fff;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.gold-teacher-card:hover,
.gold-teacher-card:focus-visible {
  outline: none;
  border-color: rgba(249, 115, 22, 0.3);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.gold-teacher-info {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}

.teacher-avatar {
  width: 72px;
  height: 72px;
  flex: 0 0 auto;
  border: 3px solid #fff;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
}

.gold-teacher-head {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.gold-teacher-name {
  overflow: hidden;
  color: #0f172a;
  font-size: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gold-teacher-head span {
  flex: 0 0 auto;
  border-radius: 6px;
  padding: 3px 7px;
  background: rgba(249, 115, 22, 0.12);
  color: var(--accent);
  font-size: 12px;
  font-weight: 700;
}

.gold-teacher-meta {
  margin-top: 4px;
  font-size: 13px;
}

.gold-teacher-intro {
  margin-top: 8px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.teacher-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: auto;
  padding-top: 12px;
}

.teacher-metrics span {
  border-radius: 6px;
  padding: 4px 8px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
}

.teacher-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  justify-content: flex-end;
}

.notice-panel {
  position: relative;
  overflow: hidden;
}

.notice-panel::before {
  content: "";
  position: absolute;
  inset: 0 0 auto;
  height: 4px;
  background: linear-gradient(90deg, var(--primary), #0f766e, var(--accent));
}

.notice-subtitle {
  margin-top: 6px;
  color: var(--muted);
  font-size: 14px;
}

.notice-showcase {
  margin-top: 18px;
  border: 1px solid rgba(37, 99, 235, 0.16);
  border-radius: 8px;
  padding: 18px;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.1), rgba(15, 118, 110, 0.08)),
    #fff;
}

.notice-showcase h3 {
  margin: 12px 0 8px;
  color: #0f172a;
  font-size: 22px;
  line-height: 1.35;
}

.notice-showcase p {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notice-badge {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 6px;
  background: rgba(37, 99, 235, 0.12);
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
}

.notice-badge-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.notice-type {
  display: inline-flex;
  border-radius: 6px;
  padding: 4px 10px;
  background: rgba(100, 116, 139, 0.1);
  color: #475569;
  font-size: 12px;
  font-weight: 700;
}

.notice-type.is-urgent {
  background: rgba(249, 115, 22, 0.13);
  color: var(--accent);
}

.notice-type.is-course {
  background: rgba(15, 118, 110, 0.1);
  color: #0f766e;
}

.notice-meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  color: var(--muted);
  font-size: 13px;
}

.notice-meta strong {
  color: var(--primary);
  font-size: 13px;
}

.mini-item + .mini-item {
  margin-top: 8px;
}

.mini-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 12px 14px;
  background: #f8fafc;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.mini-item:hover {
  border-color: rgba(37, 99, 235, 0.3);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  transform: translateY(-1px);
}

.mini-copy {
  min-width: 0;
}

.mini-copy span {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #0f172a;
  font-weight: 700;
}

.mini-copy em {
  display: block;
  margin-top: 5px;
  color: var(--muted);
  font-size: 12px;
  font-style: normal;
  white-space: nowrap;
}

.mini-item strong {
  color: var(--primary);
  font-size: 13px;
}

.teacher-card {
  display: flex;
  gap: 14px;
  align-items: flex-start;
}

.teacher-intro {
  margin-top: 8px;
}

.news-section .section-box {
  align-self: start;
}

.latest-feature {
  border: 1px solid rgba(37, 99, 235, 0.16);
  border-radius: 8px;
  padding: 18px;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.08), rgba(15, 118, 110, 0.06)),
    #fff;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.latest-feature:hover,
.latest-item:hover,
.hot-rank-item:hover,
.service-card:hover {
  border-color: rgba(37, 99, 235, 0.28);
  box-shadow: 0 10px 22px rgba(15, 23, 42, 0.07);
  transform: translateY(-1px);
}

.news-meta-row,
.news-feature-foot,
.news-item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  color: var(--muted);
  font-size: 13px;
}

.news-tag,
.news-item-meta span:first-child {
  border-radius: 6px;
  padding: 4px 8px;
  background: rgba(37, 99, 235, 0.1);
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
}

.latest-feature h3 {
  margin: 14px 0 8px;
  color: #0f172a;
  font-size: 24px;
  line-height: 1.35;
}

.latest-feature p {
  margin: 0;
  color: #475569;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.news-feature-foot {
  justify-content: space-between;
  margin-top: 16px;
}

.news-feature-foot strong,
.latest-item strong,
.hot-rank-item strong {
  color: var(--primary);
  font-size: 13px;
  white-space: nowrap;
}

.hot-rank-item strong {
  align-self: start;
  padding-top: 6px;
}

.latest-list,
.hot-rank-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.latest-item,
.hot-rank-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  border: 1px solid transparent;
  border-radius: 8px;
  padding: 13px 14px;
  background: #f8fafc;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.hot-rank-item {
  grid-template-columns: 44px minmax(0, 1fr) auto;
  align-items: start;
  min-height: 118px;
}

.rank-number {
  color: rgba(37, 99, 235, 0.7);
  font-size: 22px;
  font-weight: 800;
  line-height: 1;
  padding-top: 6px;
}

.rank-copy {
  min-width: 0;
}

.news-item-title {
  overflow: hidden;
  color: #0f172a;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.news-item-meta {
  margin-top: 6px;
}

.service-panel {
  padding-top: 22px;
  padding-bottom: 22px;
  background:
    linear-gradient(135deg, rgba(15, 118, 110, 0.06), rgba(37, 99, 235, 0.05)),
    rgba(255, 255, 255, 0.92);
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.service-card {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  min-height: 92px;
  border: 1px solid rgba(203, 213, 225, 0.74);
  border-radius: 8px;
  padding: 14px;
  background: #fff;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.service-icon {
  display: grid;
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 8px;
  background: linear-gradient(135deg, var(--primary), #0f766e);
  color: #fff;
  font-weight: 800;
}

.service-title {
  color: #0f172a;
  font-weight: 800;
}

.service-desc {
  margin-top: 5px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.5;
}

@media (max-width: 960px) {
  .hero {
    padding: 10px;
  }

  .hero :deep(.el-carousel__container) {
    height: 220px !important;
  }

  .recommend-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .gold-teacher-grid {
    grid-template-columns: 1fr;
  }

  .continue-item {
    grid-template-columns: 42px minmax(0, 1fr);
  }

  .stack-action {
    grid-column: 2;
  }

  .service-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .recommend-grid {
    grid-template-columns: 1fr;
  }

  .hot-rank-item {
    grid-template-columns: 38px minmax(0, 1fr);
  }

  .hot-rank-item strong {
    grid-column: 2;
  }

  .service-grid {
    grid-template-columns: 1fr;
  }
}
</style>
