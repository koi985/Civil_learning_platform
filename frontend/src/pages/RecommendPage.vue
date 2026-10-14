<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { useRouter } from 'vue-router'
import request from '../utils/request'

const router = useRouter()
const loading = ref(false)
const loadError = ref('')
const activeTab = ref('ALL')
const recommend = ref({
  weights: {},
  portrait: { labels: [] },
  continueLearningCourses: [],
  recommendCards: [],
  loggedIn: false,
  refreshAt: '',
  refreshCount: 0,
  feedbackEnabled: false
})

const tabs = [
  { label: '全部推荐', value: 'ALL' },
  { label: '课程推荐', value: 'COURSE' },
  { label: '刷题推荐', value: 'PRACTICE' },
  { label: '模考推荐', value: 'EXAM' },
  { label: '资讯推荐', value: 'NEWS' }
]

const filteredCards = computed(() => {
  if (activeTab.value === 'ALL') {
    return recommend.value.recommendCards || []
  }
  return (recommend.value.recommendCards || []).filter((item) => item.resourceType === activeTab.value)
})
const tabCounts = computed(() => {
  const counts = { ALL: (recommend.value.recommendCards || []).length }
  ;(recommend.value.recommendCards || []).forEach((item) => {
    counts[item.resourceType] = (counts[item.resourceType] || 0) + 1
  })
  return counts
})
const isPracticeEmpty = computed(() => activeTab.value === 'PRACTICE' && !filteredCards.value.length && !loading.value)
const isNewsEmpty = computed(() => activeTab.value === 'NEWS' && !filteredCards.value.length && !loading.value)
const emptyDescription = computed(() => {
  if (loadError.value) return '推荐加载失败，请稍后重试'
  if (activeTab.value === 'PRACTICE') {
    return recommend.value.loggedIn
      ? '暂无刷题推荐，完成一次专项练习后会生成更精准的练习包'
      : '登录后会根据练习、错题和模考记录生成刷题推荐'
  }
  if (activeTab.value === 'NEWS') {
    return '系统会根据你的考试类型、地区和近期学习行为推荐公告、热点和备考策略'
  }
  return '当前分类下还没有推荐内容'
})

function weightText(value) {
  const number = Number(value || 0)
  return `${Math.round(number * 100)}%`
}

function scoreText(value) {
  const number = Number(value || 0)
  return `${Math.round(number * 100)}分`
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '暂无'
}

function newsVisualTone(item) {
  const text = `${item.newsType || ''} ${item.tags || ''} ${item.title || ''}`
  if (text.includes('考试动态') || text.includes('公告') || text.includes('时间')) return 'news-visual-blue'
  if (text.includes('复盘') || text.includes('申论') || text.includes('面试')) return 'news-visual-green'
  if (text.includes('策略') || text.includes('方法') || text.includes('资料')) return 'news-visual-amber'
  return 'news-visual-slate'
}

function newsVisualLabel(item) {
  return item.newsType || item.categoryName || '资讯推荐'
}

async function loadData() {
  loading.value = true
  loadError.value = ''
  try {
    recommend.value = await request.get('/api/recommend/overview')
  } catch (error) {
    loadError.value = error.message || '推荐加载失败'
  } finally {
    loading.value = false
  }
}

async function refreshRecommend(showMessage = true) {
  loading.value = true
  try {
    recommend.value = await request.post('/api/recommend/refresh')
    if (showMessage) {
      ElMessage.success('推荐结果已刷新')
    }
  } finally {
    loading.value = false
  }
}

async function markNotInterested(item) {
  if (!recommend.value.feedbackEnabled) {
    ElMessage.warning('登录后可反馈不感兴趣')
    return
  }
  await request.post('/api/recommend/feedback', {
    resourceType: item.resourceType,
    resourceId: item.id,
    sourcePage: 'RECOMMEND',
    reasonText: `页面反馈：对 ${item.title} 暂不感兴趣`
  })
  recommend.value.recommendCards = recommend.value.recommendCards.filter(
    (card) => !(card.resourceType === item.resourceType && card.id === item.id)
  )
  ElMessage.success('已减少同类推荐')
}

async function openRecommendCard(item) {
  if (item.resourceType === 'PRACTICE' && item.practiceBundleId) {
    const data = await request.post(`/api/recommend/practice-bundles/${item.practiceBundleId}/start`)
    ElMessage.success('练习已生成')
    router.push(`/practice/${data.practiceId}`)
    return
  }
  router.push(item.path || '/recommend')
}

async function handleRegionChanged() {
  await refreshRecommend(false)
}

onMounted(() => {
  loadData()
  window.addEventListener('civil-region-changed', handleRegionChanged)
})

onBeforeUnmount(() => {
  window.removeEventListener('civil-region-changed', handleRegionChanged)
})
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section class="glass-card hero-box">
      <div class="hero-main">
        <div>
          <div class="section-title">智能推荐中心</div>
          <p class="muted">{{ recommend.portrait.summary }}</p>
          <div class="tag-row">
            <el-tag v-for="tag in recommend.portrait.labels" :key="tag" round>{{ tag }}</el-tag>
          </div>
        </div>
        <div class="portrait-card">
          <h3>{{ recommend.portrait.title }}</h3>
          <div class="portrait-grid">
            <div><span>地区</span><strong>{{ recommend.portrait.targetProvince }}</strong></div>
            <div><span>主攻科目</span><strong>{{ recommend.portrait.preferredSubject }}</strong></div>
            <div><span>考试类型</span><strong>{{ recommend.portrait.examType }}</strong></div>
            <div><span>活跃天数</span><strong>{{ recommend.portrait.activeDays || 0 }}</strong></div>
            <div><span>强项</span><strong>{{ recommend.portrait.strongSubject }}</strong></div>
            <div><span>薄弱项</span><strong>{{ recommend.portrait.weakSubject }}</strong></div>
          </div>
        </div>
      </div>
      <div class="config-grid">
        <div class="config-item">行为协同权重：{{ weightText(recommend.weights.collaborative_weight) }}</div>
        <div class="config-item">标签匹配权重：{{ weightText(recommend.weights.content_weight) }}</div>
        <div class="config-item">热门热度权重：{{ weightText(recommend.weights.hot_weight) }}</div>
        <div class="config-item">已刷新次数：{{ recommend.refreshCount || 0 }}</div>
        <div class="config-item">刷新时间：{{ recommend.refreshAt || '刚刚' }}</div>
      </div>
      <div class="hero-actions">
        <el-button type="primary" @click="refreshRecommend">刷新推荐</el-button>
        <el-button plain @click="router.push('/recommend/profile')">查看我的画像</el-button>
        <el-button plain @click="router.push('/notice')">查看公告中心</el-button>
      </div>
    </section>

    <section v-if="recommend.continueLearningCourses?.length" class="glass-card hero-box section-gap">
      <div class="section-title">继续学习优先</div>
      <div class="continue-grid">
        <article v-for="item in recommend.continueLearningCourses" :key="item.courseId" class="continue-card">
          <img :src="item.coverUrl" alt="" />
          <div>
            <h3>{{ item.title }}</h3>
            <p class="muted">{{ item.teacherName }} · {{ item.subjectName }}</p>
            <el-progress :percentage="Number(item.progressPercent || 0)" :stroke-width="8" />
            <div class="muted" style="margin-top: 8px">最近学习：{{ formatDateTime(item.lastStudyAt) }}</div>
          </div>
          <el-button type="primary" plain @click="router.push(item.path)">继续学习</el-button>
        </article>
      </div>
    </section>

    <section class="glass-card hero-box section-gap">
      <div class="tab-row">
        <el-radio-group v-model="activeTab" size="large">
          <el-radio-button v-for="item in tabs" :key="item.value" :value="item.value">
            {{ item.label }} {{ tabCounts[item.value] || 0 }}
          </el-radio-button>
        </el-radio-group>
      </div>
      <div class="recommend-grid">
        <div v-for="item in filteredCards" :key="`${item.resourceType}-${item.id}-${item.path}`" class="recommend-card">
          <div v-if="item.resourceType === 'NEWS'" class="news-visual" :class="newsVisualTone(item)">
            <div class="news-visual-content">
              <span>{{ newsVisualLabel(item) }}</span>
              <strong>{{ item.title }}</strong>
              <small>{{ item.sourceName || item.categoryName || '平台资讯' }} · 阅读 {{ item.readCount || 0 }}</small>
            </div>
            <div class="news-visual-mark">NEWS</div>
          </div>
          <img v-else-if="item.coverUrl" :src="item.coverUrl" alt="" class="card-cover" />
          <div class="recommend-card-head">
            <div class="recommend-tag">{{ item.resourceType }}</div>
            <span v-if="item.resourceType === 'PRACTICE' && item.sourceLabel" class="source-badge">{{ item.sourceLabel }}</span>
            <span v-if="item.resourceType === 'NEWS' && item.newsType" class="source-badge">{{ item.newsType }}</span>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.reason }}</p>
          <div class="muted">{{ item.tags || '推荐系统正在根据你的学习行为持续刷新' }}</div>
          <div v-if="item.resourceType === 'PRACTICE'" class="practice-meta">
            <span>{{ item.questionCount || 20 }} 题</span>
            <span>约 {{ item.estimatedMinutes || 18 }} 分钟</span>
            <span>{{ item.sourceLabel || '练习包' }}</span>
          </div>
          <div v-if="item.resourceType === 'PRACTICE' && (item.thetaRange || item.difficultyRange)" class="practice-meta practice-meta-secondary">
            <span v-if="item.recommendMode">{{ item.recommendMode }}</span>
            <span v-if="item.thetaRange">能力 {{ item.thetaRange }}</span>
            <span v-if="item.difficultyRange">难度 {{ item.difficultyRange }}</span>
          </div>
          <div v-if="item.resourceType === 'NEWS'" class="news-meta">
            <span>{{ item.newsType || '资讯推荐' }}</span>
            <span>阅读 {{ item.readCount || 0 }}</span>
            <span>{{ item.publishedAt || '近期发布' }}</span>
            <span>{{ item.sourceName || item.categoryName || '平台资讯' }}</span>
          </div>
          <div v-if="item.explainTags?.length" class="score-chip-row">
            <el-tag v-for="tag in item.explainTags" :key="`${item.id}-${tag}`" size="small" effect="plain">{{ tag }}</el-tag>
          </div>
          <div v-if="item.explainReasons?.length" class="explain-list">
            <span v-for="reason in item.explainReasons" :key="`${item.id}-${reason}`">{{ reason }}</span>
          </div>
          <div class="score-grid">
            <div class="score-item">
              <span>协同分</span>
              <strong>{{ scoreText(item.collaborativeScore) }}</strong>
            </div>
            <div class="score-item">
              <span>标签分</span>
              <strong>{{ scoreText(item.contentScore) }}</strong>
            </div>
            <div class="score-item">
              <span>热度分</span>
              <strong>{{ scoreText(item.hotScore) }}</strong>
            </div>
            <div class="score-item score-item-final">
              <span>最终分</span>
              <strong>{{ scoreText(item.finalScore) }}</strong>
            </div>
          </div>
          <div class="recommend-actions">
            <el-button type="primary" plain @click="openRecommendCard(item)">{{ item.actionText || '查看' }}</el-button>
            <el-button v-if="recommend.feedbackEnabled" text @click.stop="markNotInterested(item)">不感兴趣</el-button>
          </div>
        </div>
      </div>
      <div v-if="isPracticeEmpty" class="practice-empty">
        <h3>暂无刷题推荐</h3>
        <p>{{ emptyDescription }}</p>
        <div class="empty-actions">
          <el-button type="primary" @click="router.push('/search')">去搜题练习</el-button>
          <el-button plain @click="router.push('/question-bank')">进入题库</el-button>
          <el-button plain @click="router.push(`/search?subjectName=${encodeURIComponent(recommend.portrait.weakSubject || '')}`)">按薄弱项练习</el-button>
        </div>
      </div>
      <div v-else-if="isNewsEmpty" class="practice-empty news-empty">
        <h3>暂无资讯推荐</h3>
        <p>{{ emptyDescription }}</p>
        <div class="empty-actions">
          <el-button type="primary" @click="router.push('/news')">去资讯页</el-button>
          <el-button plain @click="router.push('/news')">查看热门资讯</el-button>
          <el-button plain @click="refreshRecommend">刷新推荐</el-button>
        </div>
      </div>
      <el-empty v-else-if="!filteredCards.length" :description="emptyDescription">
        <el-button v-if="loadError" type="primary" @click="loadData">重试</el-button>
      </el-empty>
    </section>
  </div>
</template>

<style scoped>
.hero-box {
  padding: 24px;
}

.hero-main {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(300px, 0.8fr);
  gap: 20px;
}

.muted {
  color: var(--muted);
}

.tag-row,
.config-grid,
.recommend-grid,
.continue-grid,
.score-grid {
  display: grid;
  gap: 16px;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  margin-top: 16px;
}

.config-grid,
.recommend-grid {
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.continue-grid {
  margin-top: 16px;
}

.portrait-card,
.config-item,
.recommend-card,
.continue-card {
  border: 1px solid var(--border);
  border-radius: 16px;
  background: #fff;
  padding: 16px;
}

.portrait-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.portrait-grid span {
  display: block;
  color: var(--muted);
  font-size: 13px;
}

.portrait-grid strong {
  display: block;
  margin-top: 6px;
  color: var(--primary);
}

.recommend-card {
  display: grid;
  align-content: start;
  gap: 12px;
}

.hero-actions,
.recommend-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.hero-actions {
  margin-top: 16px;
}

.recommend-card-head,
.practice-meta,
.news-meta,
.empty-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.recommend-card-head {
  justify-content: space-between;
}

.recommend-tag {
  display: inline-flex;
  width: fit-content;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(249, 115, 22, 0.12);
  color: var(--accent);
  font-size: 12px;
  margin-bottom: 10px;
}

.source-badge {
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.practice-meta {
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(37, 99, 235, 0.06);
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
}

.news-meta {
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(249, 115, 22, 0.08);
  color: #c2410c;
  font-size: 13px;
  font-weight: 700;
}

.practice-empty {
  border: 1px dashed rgba(37, 99, 235, 0.35);
  border-radius: 16px;
  padding: 28px;
  background: rgba(37, 99, 235, 0.05);
}

.news-empty {
  border-color: rgba(249, 115, 22, 0.35);
  background: rgba(249, 115, 22, 0.06);
}

.practice-empty h3 {
  margin: 0 0 10px;
  color: #0f172a;
}

.practice-empty p {
  margin: 0 0 18px;
  color: var(--muted);
  line-height: 1.7;
}

.score-chip-row,
.explain-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.explain-list span {
  border-radius: 999px;
  padding: 4px 10px;
  background: rgba(37, 99, 235, 0.08);
  color: #2563eb;
  font-size: 12px;
}

.score-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.score-item {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 10px 12px;
  background: #f8fafc;
}

.score-item span {
  display: block;
  color: var(--muted);
  font-size: 12px;
}

.score-item strong {
  display: block;
  margin-top: 6px;
  color: var(--primary);
  font-size: 18px;
}

.score-item-final {
  background: rgba(37, 99, 235, 0.08);
  border-color: rgba(37, 99, 235, 0.18);
}

.card-cover {
  width: 100%;
  height: 150px;
  object-fit: cover;
  border-radius: 12px;
  margin-bottom: 12px;
}

.news-visual {
  position: relative;
  min-height: 172px;
  overflow: hidden;
  border-radius: 14px;
  padding: 22px;
  margin-bottom: 12px;
  color: #fff;
  background: linear-gradient(135deg, #1d4ed8, #0f766e);
}

.news-visual::before,
.news-visual::after {
  content: '';
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
}

.news-visual::before {
  width: 180px;
  height: 180px;
  right: -54px;
  top: -56px;
  background: rgba(255, 255, 255, 0.16);
}

.news-visual::after {
  width: 130px;
  height: 130px;
  right: 54px;
  bottom: -72px;
  border: 22px solid rgba(255, 255, 255, 0.12);
}

.news-visual-content {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 14px;
  max-width: 76%;
}

.news-visual-content span {
  width: fit-content;
  border-radius: 999px;
  padding: 5px 10px;
  background: rgba(255, 255, 255, 0.18);
  font-size: 12px;
  font-weight: 700;
}

.news-visual-content strong {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  font-size: 24px;
  line-height: 1.3;
}

.news-visual-content small {
  color: rgba(255, 255, 255, 0.82);
  font-weight: 700;
}

.news-visual-mark {
  position: absolute;
  right: 22px;
  bottom: 18px;
  z-index: 1;
  color: rgba(255, 255, 255, 0.32);
  font-size: 38px;
  font-weight: 900;
  letter-spacing: 0;
}

.news-visual-blue {
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.1), rgba(15, 23, 42, 0)),
    linear-gradient(135deg, #2563eb, #0891b2);
}

.news-visual-green {
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.1), rgba(15, 23, 42, 0)),
    linear-gradient(135deg, #0f766e, #16a34a);
}

.news-visual-amber {
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.12), rgba(15, 23, 42, 0)),
    linear-gradient(135deg, #b45309, #ea580c);
}

.news-visual-slate {
  background:
    linear-gradient(135deg, rgba(15, 23, 42, 0.08), rgba(15, 23, 42, 0)),
    linear-gradient(135deg, #334155, #2563eb);
}

.continue-card {
  display: grid;
  grid-template-columns: 120px 1fr auto;
  gap: 16px;
  align-items: center;
}

.continue-card img {
  width: 120px;
  height: 80px;
  border-radius: 12px;
  object-fit: cover;
}

.tab-row {
  margin-bottom: 20px;
}

@media (max-width: 900px) {
  .hero-main,
  .continue-card {
    grid-template-columns: 1fr;
  }
}
</style>
