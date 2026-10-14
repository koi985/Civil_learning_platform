<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const loading = ref(false)
const creating = ref(false)
const favoriteLoading = ref(false)
const showAdvanced = ref(false)
const list = ref([])
const total = ref(0)
const dashboard = ref({
  filterOptions: { provinces: [], subjects: [], categories: [], questionTypes: [], difficultyLevels: [], sources: [] },
  favoriteCount: 0,
  favoriteSubjects: [],
  recentPractices: [],
  practiceTrend: []
})
const favoriteData = ref({ records: [], total: 0 })
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  province: '',
  subjectName: '',
  categoryName: '',
  questionType: '',
  difficultyLevel: '',
  sourceName: '',
  sortBy: 'LATEST',
  questionCount: 10
})
const favoriteQuery = reactive({
  pageNo: 1,
  pageSize: 8,
  subjectName: '',
  questionType: ''
})

const emptyDashboard = {
  filterOptions: { provinces: [], subjects: [], categories: [], questionTypes: [], difficultyLevels: [], sources: [] },
  favoriteCount: 0,
  favoriteSubjects: [],
  recentPractices: [],
  practiceTrend: []
}

const defaultQuery = {
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  province: '',
  subjectName: '',
  categoryName: '',
  questionType: '',
  difficultyLevel: '',
  sourceName: '',
  sortBy: 'LATEST',
  questionCount: 10
}

function applyRouteQuery() {
  Object.keys(defaultQuery).forEach((key) => {
    if (route.query[key] === undefined) return
    query[key] = key === 'questionCount' ? Number(route.query[key] || defaultQuery[key]) : String(route.query[key] || '')
  })
}

const sortLabelMap = {
  LATEST: '最新优先',
  OLDEST: '最早录入'
}

const resultsWithVideo = computed(() => list.value.filter((item) => item.videoExplainAvailable || item.videoExplainUrl).length)
const resultSummary = computed(() => total.value ? `当前命中 ${total.value} 题` : '当前没有命中题目')
const practiceCtaText = computed(() => `练这 ${query.questionCount} 题`)
const canReset = computed(() => Object.keys(defaultQuery).some((key) => query[key] !== defaultQuery[key]))
const activeFilters = computed(() => {
  const filters = []
  if (query.keyword) filters.push({ key: 'keyword', label: `关键词: ${query.keyword}` })
  if (query.subjectName) filters.push({ key: 'subjectName', label: `科目: ${query.subjectName}` })
  if (query.questionType) filters.push({ key: 'questionType', label: `题型: ${query.questionType}` })
  if (query.difficultyLevel) filters.push({ key: 'difficultyLevel', label: `难度: ${query.difficultyLevel}` })
  if (query.sourceName) filters.push({ key: 'sourceName', label: `来源: ${query.sourceName}` })
  if (query.province) filters.push({ key: 'province', label: `省份: ${query.province}` })
  if (query.categoryName) filters.push({ key: 'categoryName', label: `分类: ${query.categoryName}` })
  if (query.sortBy !== 'LATEST') filters.push({ key: 'sortBy', label: `排序: ${sortLabelMap[query.sortBy] || query.sortBy}` })
  return filters
})

async function loadDashboard() {
  dashboard.value = await request.get('/api/questions/search-dashboard')
}

async function loadQuestions() {
  loading.value = true
  try {
    const data = await request.get('/api/questions', { params: query })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadFavorites() {
  if (!authStore.isLoggedIn) {
    favoriteData.value = { records: [], total: 0 }
    return
  }
  favoriteLoading.value = true
  try {
    favoriteData.value = await request.get('/api/question-favorites', { params: favoriteQuery })
  } finally {
    favoriteLoading.value = false
  }
}

async function refreshAll() {
  await Promise.all([loadDashboard(), loadQuestions(), loadFavorites()])
}

async function startPractice() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后再开始练习')
    router.push('/login')
    return
  }
  creating.value = true
  try {
    const data = await request.post('/api/practices', {
      keyword: query.keyword,
      province: query.province,
      subjectName: query.subjectName,
      categoryName: query.categoryName,
      questionType: query.questionType,
      difficultyLevel: query.difficultyLevel,
      sourceName: query.sourceName,
      questionCount: query.questionCount,
      mode: 'SEARCH'
    })
    ElMessage.success('练习已生成')
    router.push(`/practice/${data.practiceId}`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    creating.value = false
  }
}

async function toggleFavorite(row) {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后再收藏题目')
    router.push('/login')
    return
  }
  try {
    if (row.favorite) {
      await request.delete(`/api/question-favorites/${row.id}`)
      row.favorite = false
    } else {
      await request.post(`/api/question-favorites/${row.id}`)
      row.favorite = true
    }
    await Promise.all([loadDashboard(), loadFavorites()])
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function applySearch() {
  query.pageNo = 1
  await loadQuestions()
}

async function resetSearch() {
  Object.assign(query, defaultQuery)
  showAdvanced.value = false
  await loadQuestions()
}

function clearFilter(key) {
  query[key] = defaultQuery[key]
  query.pageNo = 1
  loadQuestions()
}

function questionMeta(item) {
  return [
    item.subjectName || '通用',
    item.categoryName || '未分类',
    item.questionType || '未知题型',
    item.difficultyLevel || '未知难度',
    item.province || '全国',
    item.sourceName || '平台题库'
  ]
}

function favoriteMeta(item) {
  return [
    item.categoryName || '未分类',
    item.sourceName || '平台题库',
    item.favoriteCreatedAt || '已收藏'
  ]
}

onMounted(async () => {
  try {
    applyRouteQuery()
    await refreshAll()
  } catch {
    dashboard.value = emptyDashboard
  }
})
</script>

<template>
  <div class="search-page">
    <section class="glass-card workbench-card">
      <div class="workbench-head">
        <div>
          <div class="section-title small">搜索条件</div>
          <p class="panel-copy">先用高频条件缩小范围，再决定是否展开高级筛选。</p>
        </div>
        <div class="workbench-actions">
          <el-button text @click="showAdvanced = !showAdvanced">{{ showAdvanced ? '收起高级筛选' : '更多筛选' }}</el-button>
          <el-button type="primary" @click="applySearch">搜索题目</el-button>
        </div>
      </div>

      <div class="primary-filter-grid">
        <el-input v-model="query.keyword" placeholder="搜索题干关键词" clearable @keyup.enter="applySearch" />
        <el-select v-model="query.subjectName" clearable placeholder="科目">
          <el-option v-for="item in dashboard.filterOptions.subjects" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="query.questionType" clearable placeholder="题型">
          <el-option v-for="item in dashboard.filterOptions.questionTypes" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="query.difficultyLevel" clearable placeholder="难度">
          <el-option v-for="item in dashboard.filterOptions.difficultyLevels" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="query.sourceName" clearable placeholder="来源">
          <el-option v-for="item in dashboard.filterOptions.sources" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="query.sortBy" placeholder="排序">
          <el-option label="最新优先" value="LATEST" />
          <el-option label="最早录入" value="OLDEST" />
        </el-select>
      </div>

      <div v-if="showAdvanced" class="advanced-panel">
        <div class="advanced-grid">
          <el-select v-model="query.province" clearable placeholder="省份">
            <el-option v-for="item in dashboard.filterOptions.provinces" :key="item" :label="item" :value="item" />
          </el-select>
          <el-select v-model="query.categoryName" clearable placeholder="题型分类">
            <el-option v-for="item in dashboard.filterOptions.categories" :key="item" :label="item" :value="item" />
          </el-select>
          <div class="count-control">
            <span>练习题量</span>
            <el-input-number v-model="query.questionCount" :min="1" :max="50" />
          </div>
        </div>
      </div>

      <div class="filter-foot">
        <div class="active-filter-row">
          <span class="active-label">当前筛选</span>
          <div v-if="activeFilters.length" class="chip-row">
            <button v-for="item in activeFilters" :key="item.key" class="filter-chip" type="button" @click="clearFilter(item.key)">
              {{ item.label }}
            </button>
          </div>
          <span v-else class="empty-hint">未设置筛选条件，将展示全部可用题目。</span>
        </div>
        <div class="workbench-foot-actions">
          <el-button :disabled="!canReset" @click="resetSearch">清空条件</el-button>
          <el-button type="primary" plain :loading="creating" @click="startPractice">按当前条件开始练习</el-button>
        </div>
      </div>
    </section>

    <section class="glass-card hero-card">
      <div class="hero-copy">
        <div class="hero-title-row">
          <div>
            <div class="section-title">搜题工作台</div>
            <p>按关键词和条件快速定位题目，随时收藏、看讲解，并直接生成一组练习。</p>
          </div>
          <div class="hero-actions">
            <el-button type="primary" :loading="creating" @click="startPractice">{{ practiceCtaText }}</el-button>
          </div>
        </div>

        <div class="summary-strip">
          <article class="summary-card is-primary">
            <span>当前命中</span>
            <strong>{{ total }}</strong>
            <em>{{ resultSummary }}</em>
          </article>
          <article class="summary-card">
            <span>可看讲解</span>
            <strong>{{ resultsWithVideo }}</strong>
            <em>优先筛出可回看题目</em>
          </article>
          <article class="summary-card">
            <span>已收藏</span>
            <strong>{{ dashboard.favoriteCount || 0 }}</strong>
            <em>沉淀重点题与错题</em>
          </article>
        </div>
      </div>
    </section>

    <section class="glass-card results-card" v-loading="loading">
      <div class="results-head">
        <div>
          <div class="section-title small">搜题结果</div>
          <p class="panel-copy">题干优先展示，标签帮助快速判断是否值得加入练习或收藏。</p>
        </div>
        <div class="results-pill">{{ resultSummary }}</div>
      </div>

      <div v-if="list.length" class="result-list">
        <article v-for="row in list" :key="row.id" class="result-item">
          <div class="result-main">
            <div class="result-tags">
              <span v-for="tag in questionMeta(row)" :key="`${row.id}-${tag}`">{{ tag }}</span>
            </div>
            <h3 class="result-stem">{{ row.stem }}</h3>
            <div class="result-subline">
              <span>{{ row.videoExplainUrl ? '支持视频讲解' : '暂无视频讲解' }}</span>
              <span>{{ row.favorite ? '已加入收藏' : '可收藏到题集' }}</span>
            </div>
          </div>
          <div class="result-actions">
            <a v-if="row.videoExplainUrl" :href="row.videoExplainUrl" target="_blank" rel="noreferrer" class="video-link">
              {{ row.videoExplainTitle || '查看讲解' }}
            </a>
            <el-button text type="warning" @click="toggleFavorite(row)">{{ row.favorite ? '取消收藏' : '收藏题目' }}</el-button>
          </div>
        </article>
      </div>
      <el-empty v-else description="没有找到符合条件的题目，试试放宽筛选条件。" />

      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.pageNo"
        class="pagination-row"
        @current-change="(page) => { query.pageNo = page; loadQuestions() }"
      />
    </section>

    <section class="glass-card favorites-card">
      <div class="results-head">
        <div>
          <div class="section-title small">我的收藏题目</div>
          <p class="panel-copy">保留重点题和可回看题，方便后续集中复习。</p>
        </div>
      </div>

      <div class="favorite-filter-row">
        <el-select v-model="favoriteQuery.subjectName" clearable placeholder="筛选科目">
          <el-option v-for="item in dashboard.filterOptions.subjects" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="favoriteQuery.questionType" clearable placeholder="筛选题型">
          <el-option v-for="item in dashboard.filterOptions.questionTypes" :key="item" :label="item" :value="item" />
        </el-select>
        <el-button type="primary" @click="favoriteQuery.pageNo = 1; loadFavorites()">筛选收藏</el-button>
      </div>

      <div v-loading="favoriteLoading" class="favorite-grid">
        <article v-for="item in favoriteData.records" :key="item.id" class="favorite-card">
          <div class="result-tags">
            <span>{{ item.subjectName }}</span>
            <span>{{ item.questionType }}</span>
          </div>
          <strong class="favorite-title">{{ item.stem }}</strong>
          <p>{{ favoriteMeta(item).join(' · ') }}</p>
          <div class="result-actions compact-actions">
            <a v-if="item.videoExplainUrl" :href="item.videoExplainUrl" target="_blank" rel="noreferrer" class="video-link">查看讲解</a>
            <el-button text type="danger" @click="toggleFavorite(item)">取消收藏</el-button>
          </div>
        </article>
        <el-empty v-if="!favoriteLoading && !favoriteData.records?.length" description="当前还没有收藏题目" />
      </div>
    </section>
  </div>
</template>

<style scoped>
.search-page {
  display: grid;
  gap: 18px;
}

.hero-card,
.workbench-card,
.results-card,
.favorites-card {
  padding: 24px;
}

.hero-card {
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(37, 99, 235, 0.08), rgba(15, 118, 110, 0.05)),
    rgba(255, 255, 255, 0.96);
}

.hero-copy,
.summary-strip,
.result-list,
.favorite-grid {
  display: grid;
  gap: 16px;
}

.hero-title-row,
.hero-actions,
.workbench-head,
.workbench-actions,
.filter-foot,
.workbench-foot-actions,
.results-head,
.result-actions,
.favorite-filter-row {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
}

.hero-title-row {
  align-items: flex-start;
}

.hero-card p,
.panel-copy,
.summary-card em,
.result-subline,
.favorite-card p,
.empty-hint {
  color: var(--muted);
}

.summary-strip {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 18px;
}

.summary-card {
  display: grid;
  gap: 6px;
  min-height: 118px;
  border: 1px solid rgba(203, 213, 225, 0.8);
  border-radius: 12px;
  padding: 18px;
  background: #fff;
}

.summary-card span {
  font-size: 13px;
  font-weight: 700;
  color: #64748b;
}

.summary-card strong {
  color: var(--primary);
  font-size: 34px;
  line-height: 1;
}

.summary-card em {
  font-size: 13px;
  font-style: normal;
}

.summary-card.is-primary {
  border-color: rgba(37, 99, 235, 0.24);
  box-shadow: 0 12px 30px rgba(37, 99, 235, 0.08);
}

.workbench-card,
.results-card,
.favorites-card {
  display: grid;
  gap: 18px;
}

.primary-filter-grid,
.advanced-grid {
  display: grid;
  gap: 12px;
}

.primary-filter-grid {
  grid-template-columns: minmax(240px, 2fr) repeat(5, minmax(140px, 1fr));
}

.advanced-panel {
  border-radius: 12px;
  padding: 16px;
  background: #f8fafc;
}

.advanced-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.count-control {
  display: grid;
  gap: 8px;
}

.count-control span,
.active-label {
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.filter-foot {
  align-items: flex-start;
  border-top: 1px solid rgba(226, 232, 240, 0.9);
  padding-top: 16px;
}

.active-filter-row {
  display: grid;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

.chip-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.filter-chip {
  border: 1px solid rgba(37, 99, 235, 0.14);
  border-radius: 999px;
  padding: 7px 12px;
  background: rgba(37, 99, 235, 0.08);
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.filter-chip:hover {
  border-color: rgba(37, 99, 235, 0.28);
  background: rgba(37, 99, 235, 0.12);
}

.results-pill {
  border-radius: 999px;
  padding: 8px 14px;
  background: rgba(15, 23, 42, 0.04);
  color: #0f172a;
  font-size: 13px;
  font-weight: 700;
}

.result-list {
  gap: 12px;
}

.result-item,
.favorite-card {
  display: grid;
  gap: 14px;
  border: 1px solid rgba(203, 213, 225, 0.78);
  border-radius: 12px;
  padding: 18px;
  background: #fff;
}

.result-item {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: flex-start;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.result-item:hover,
.favorite-card:hover {
  border-color: rgba(37, 99, 235, 0.26);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.06);
  transform: translateY(-1px);
}

.result-main {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.result-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.result-tags span {
  border-radius: 999px;
  padding: 4px 9px;
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
}

.result-stem,
.favorite-title {
  margin: 0;
  color: #0f172a;
  line-height: 1.55;
}

.result-stem {
  font-size: 18px;
}

.favorite-title {
  font-size: 16px;
}

.result-subline {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  font-size: 13px;
}

.result-actions {
  justify-content: flex-end;
  align-items: flex-start;
  min-width: 120px;
}

.compact-actions {
  justify-content: flex-start;
  min-width: 0;
}

.video-link {
  color: var(--primary);
  font-size: 13px;
  font-weight: 700;
  text-decoration: none;
}

.video-link:hover {
  text-decoration: underline;
}

.favorite-grid {
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
}

.pagination-row {
  justify-content: center;
  margin-top: 6px;
}

.section-title.small {
  margin-bottom: 6px;
  font-size: 24px;
}

@media (max-width: 1200px) {
  .primary-filter-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .hero-card,
  .workbench-card,
  .results-card,
  .favorites-card {
    padding: 18px;
  }

  .summary-strip,
  .advanced-grid,
  .primary-filter-grid,
  .favorite-grid {
    grid-template-columns: 1fr;
  }

  .result-item {
    grid-template-columns: 1fr;
  }

  .result-actions {
    justify-content: flex-start;
    min-width: 0;
  }
}

@media (max-width: 640px) {
  .search-page {
    gap: 14px;
  }

  .summary-card strong {
    font-size: 30px;
  }

  .filter-foot,
  .workbench-foot-actions,
  .hero-actions,
  .workbench-actions {
    width: 100%;
  }

  .hero-actions :deep(.el-button),
  .workbench-actions :deep(.el-button),
  .workbench-foot-actions :deep(.el-button) {
    flex: 1 1 100%;
  }
}
</style>
