<script setup>
import { computed, onActivated, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const categories = ref([])
const infoCategories = ref([])
const provinces = ref([])
const list = ref([])
const hotList = ref([])
const total = ref(0)

const query = reactive({
  pageNo: 1,
  pageSize: 50,
  keyword: '',
  categoryId: null,
  infoCategory: '',
  province: ''
})

const selectedFilters = computed(() => {
  const filters = []
  const category = categories.value.find((item) => item.id === query.categoryId)
  if (category) filters.push({ key: 'categoryId', label: category.name })
  if (query.province) filters.push({ key: 'province', label: query.province })
  if (query.infoCategory) filters.push({ key: 'infoCategory', label: query.infoCategory })
  if (query.keyword) filters.push({ key: 'keyword', label: query.keyword })
  return filters
})

const latestNews = computed(() => list.value.slice(0, 8))
const sideHotNews = computed(() => hotList.value.slice(0, 10))

async function loadFilterOptions() {
  const data = await request.get('/api/news/filter-options')
  categories.value = data.examProjects || []
  infoCategories.value = data.infoCategories || []
  provinces.value = data.provinces || []
}

function buildParams() {
  return {
    pageNo: query.pageNo,
    pageSize: query.pageSize,
    keyword: query.keyword || undefined,
    categoryId: query.categoryId || undefined,
    infoCategory: query.infoCategory || undefined,
    province: query.province || undefined
  }
}

async function loadData() {
  loading.value = true
  try {
    const [data, hotData] = await Promise.all([
      request.get('/api/news', { params: buildParams() }),
      request.get('/api/news/hot')
    ])
    list.value = data.records || []
    total.value = data.total || 0
    hotList.value = hotData || []
  } finally {
    loading.value = false
  }
}

async function selectCategory(id) {
  query.categoryId = id
  query.pageNo = 1
  await loadData()
}

async function selectFilter(key, value) {
  query[key] = value
  query.pageNo = 1
  await loadData()
}

async function removeFilter(key) {
  query[key] = key === 'categoryId' ? null : ''
  query.pageNo = 1
  await loadData()
}

async function resetFilters() {
  Object.assign(query, { pageNo: 1, keyword: '', categoryId: null, infoCategory: '', province: '' })
  await loadData()
}

async function searchNews() {
  query.pageNo = 1
  await loadData()
}

async function toggleCategorySubscription(item) {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  const data = item.subscribed
    ? await request.delete(`/api/news/categories/${item.id}/subscribe`)
    : await request.post(`/api/news/categories/${item.id}/subscribe`)
  item.subscribed = data.subscribed
  ElMessage.success(data.subscribed ? '已订阅该考试项目' : '已取消订阅')
}

function openNews(id) {
  router.push(`/news/detail/${id}`)
}

function formatDate(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 10)
}

function formatDateShort(value) {
  const date = formatDate(value)
  const [, month, day] = date.match(/^\d{4}-(\d{2})-(\d{2})/) || []
  return month && day ? `${month}-${day}` : date
}

onMounted(async () => {
  await loadFilterOptions()
  await loadData()
})

onActivated(loadData)
</script>

<template>
  <div class="news-page" v-loading="loading">
    <section class="filter-board">
      <div class="board-head">
        <div>
          <h1>考试资讯</h1>
          <p>按考试项目、招考省份、资讯分类快速定位公告、职位表、报名入口和成绩查询信息。</p>
        </div>
        <div class="search-box">
          <el-input v-model="query.keyword" placeholder="搜索资讯标题" clearable @keyup.enter="searchNews" />
          <el-button type="primary" @click="searchNews">查询</el-button>
        </div>
      </div>

      <div class="selected-row">
        <span>已选：</span>
        <button v-for="item in selectedFilters" :key="item.key" type="button" @click="removeFilter(item.key)">
          {{ item.label }} ×
        </button>
        <button v-if="selectedFilters.length" type="button" class="clear-btn" @click="resetFilters">清空</button>
        <em v-else>全部资讯</em>
      </div>

      <div class="filter-row">
        <strong>考试项目</strong>
        <div class="filter-options">
          <button type="button" :class="{ active: query.categoryId === null }" @click="selectCategory(null)">全部</button>
          <button
            v-for="item in categories"
            :key="item.id"
            type="button"
            :class="{ active: query.categoryId === item.id }"
            @click="selectCategory(item.id)"
          >
            {{ item.name }}
          </button>
        </div>
      </div>

      <div class="filter-row">
        <strong>招考省份</strong>
        <div class="filter-options province-options">
          <button type="button" :class="{ active: !query.province }" @click="selectFilter('province', '')">全部</button>
          <button
            v-for="item in provinces"
            :key="item"
            type="button"
            :class="{ active: query.province === item }"
            @click="selectFilter('province', item)"
          >
            {{ item }}
          </button>
        </div>
      </div>

      <div class="filter-row">
        <strong>资讯分类</strong>
        <div class="filter-options">
          <button type="button" :class="{ active: !query.infoCategory }" @click="selectFilter('infoCategory', '')">全部信息</button>
          <button
            v-for="item in infoCategories"
            :key="item"
            type="button"
            :class="{ active: query.infoCategory === item }"
            @click="selectFilter('infoCategory', item)"
          >
            {{ item }}
          </button>
        </div>
      </div>
    </section>

    <section class="content-layout">
      <main class="info-column">
        <div class="section-head">
          <h2>招考信息</h2>
          <span>共 {{ total }} 条</span>
        </div>
        <div class="info-list" v-if="list.length">
          <article v-for="item in list" :key="item.id" class="info-row" @click="openNews(item.id)">
            <div class="row-sub">【{{ item.categoryName || '资讯' }}】</div>
            <h3>{{ item.title }}</h3>
            <span class="row-tag">{{ item.province || '全国' }}</span>
            <span class="row-date">{{ formatDate(item.publishedAt) }}</span>
          </article>
        </div>
        <el-empty v-else description="暂无相关内容，敬请期待" />
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="query.pageSize"
          :current-page="query.pageNo"
          class="pager"
          @current-change="(page) => { query.pageNo = page; loadData() }"
        />
      </main>

      <aside class="side-column">
        <section class="side-section">
          <div class="side-title">最新资讯</div>
          <button v-for="item in latestNews" :key="`latest-${item.id}`" type="button" class="rank-row" @click="openNews(item.id)">
            <span>{{ formatDateShort(item.publishedAt) }}</span>
            <strong>{{ item.title }}</strong>
          </button>
        </section>

        <section class="side-section">
          <div class="side-title">热门资讯</div>
          <button v-for="(item, index) in sideHotNews" :key="`hot-${item.id}`" type="button" class="hot-row" @click="openNews(item.id)">
            <span>{{ index + 1 }}</span>
            <strong>{{ item.title }}</strong>
          </button>
        </section>

        <section class="side-section subscribe-section">
          <div class="side-title">项目订阅</div>
          <button
            v-for="item in categories"
            :key="`sub-${item.id}`"
            type="button"
            :class="{ active: item.subscribed }"
            @click="toggleCategorySubscription(item)"
          >
            {{ item.subscribed ? '已订阅' : '订阅' }} {{ item.name }}
          </button>
        </section>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.news-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 28px 18px 52px;
  color: #222;
  background: #f5f5f5;
}

.filter-board,
.info-column,
.side-section {
  background: #fff;
  border: 1px solid #eceff4;
}

.filter-board {
  padding: 22px 28px 10px;
}

.board-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 380px;
  gap: 24px;
  align-items: center;
  padding-bottom: 18px;
  border-bottom: 1px solid #eef1f5;
}

.board-head h1 {
  margin: 0;
  color: #1f2937;
  font-size: 30px;
  line-height: 1.2;
  font-weight: 800;
}

.board-head p {
  margin: 8px 0 0;
  color: #6b7280;
  font-size: 14px;
}

.search-box {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

.selected-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  min-height: 42px;
  border-bottom: 1px solid #eef1f5;
  color: #667085;
  font-size: 14px;
}

.selected-row button,
.filter-options button,
.subscribe-section button {
  border: 0;
  background: transparent;
  cursor: pointer;
  font-size: 14px;
}

.selected-row button {
  height: 26px;
  padding: 0 10px;
  border-radius: 2px;
  color: #f04438;
  background: #fff1f0;
}

.selected-row .clear-btn {
  color: #667085;
  background: #f2f4f7;
}

.selected-row em {
  color: #98a2b3;
  font-style: normal;
}

.filter-row {
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  gap: 14px;
  padding: 15px 0;
  border-bottom: 1px solid #f1f3f6;
}

.filter-row:last-child {
  border-bottom: 0;
}

.filter-row strong {
  color: #344054;
  font-size: 15px;
  line-height: 30px;
}

.filter-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 10px;
}

.filter-options button {
  min-width: 58px;
  height: 30px;
  padding: 0 10px;
  color: #344054;
  border-radius: 2px;
}

.province-options button {
  min-width: 46px;
}

.filter-options button:hover,
.filter-options button.active {
  color: #fff;
  background: #e4393c;
}

.content-layout {
  display: grid;
  grid-template-columns: minmax(0, 822px) 338px;
  gap: 40px;
  margin-top: 18px;
}

.info-column {
  min-width: 0;
  padding: 22px 26px 26px;
}

.section-head,
.side-title {
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-left: 16px;
}

.section-head::before,
.side-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 4px;
  width: 4px;
  height: 24px;
  background: #e4393c;
}

.section-head h2,
.side-title {
  margin: 0;
  color: #1f2937;
  font-size: 22px;
  font-weight: 800;
}

.section-head span {
  color: #98a2b3;
  font-size: 14px;
}

.info-list {
  margin-top: 14px;
}

.info-row {
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr) 72px 92px;
  gap: 12px;
  align-items: center;
  min-height: 46px;
  border-bottom: 1px solid #f2f4f7;
  cursor: pointer;
}

.info-row:hover h3 {
  color: #e4393c;
}

.row-sub,
.row-tag,
.row-date {
  white-space: nowrap;
  font-size: 14px;
}

.row-sub {
  color: #e4393c;
}

.info-row h3 {
  min-width: 0;
  overflow: hidden;
  margin: 0;
  color: #1f2937;
  font-size: 16px;
  line-height: 1.45;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-tag {
  color: #667085;
}

.row-date {
  color: #98a2b3;
  text-align: right;
}

.pager {
  margin-top: 24px;
  justify-content: center;
}

.side-column {
  display: grid;
  align-content: start;
  gap: 18px;
}

.side-section {
  padding: 20px;
}

.rank-row,
.hot-row {
  width: 100%;
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  border: 0;
  border-bottom: 1px solid #f2f4f7;
  padding: 13px 0;
  text-align: left;
  background: transparent;
  cursor: pointer;
}

.rank-row span,
.hot-row span {
  color: #98a2b3;
  font-size: 13px;
}

.hot-row span {
  width: 22px;
  height: 22px;
  display: inline-grid;
  place-items: center;
  border-radius: 2px;
  color: #667085;
  background: #f2f4f7;
}

.hot-row:nth-of-type(2) span,
.hot-row:nth-of-type(3) span,
.hot-row:nth-of-type(4) span {
  color: #fff;
  background: #e4393c;
}

.rank-row strong,
.hot-row strong {
  min-width: 0;
  overflow: hidden;
  color: #344054;
  font-size: 14px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-row:hover strong,
.hot-row:hover strong {
  color: #e4393c;
}

.subscribe-section {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.subscribe-section .side-title {
  flex: 0 0 100%;
  margin-bottom: 4px;
}

.subscribe-section button {
  height: 30px;
  padding: 0 10px;
  border: 1px solid #e4e7ec;
  color: #475467;
  background: #fff;
}

.subscribe-section button.active {
  color: #e4393c;
  border-color: #fecdca;
  background: #fff1f0;
}

@media (max-width: 960px) {
  .board-head,
  .content-layout {
    grid-template-columns: 1fr;
  }

  .search-box {
    grid-template-columns: minmax(0, 1fr) auto;
  }
}

@media (max-width: 640px) {
  .news-page {
    padding: 16px 10px 34px;
  }

  .filter-board,
  .info-column,
  .side-section {
    padding-left: 14px;
    padding-right: 14px;
  }

  .filter-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .filter-options button,
  .selected-row button,
  .subscribe-section button {
    min-height: 36px;
  }

  .info-row {
    grid-template-columns: 1fr auto;
    gap: 4px 12px;
    padding: 10px 0;
  }

  .row-sub,
  .row-tag {
    display: none;
  }

  .row-date {
    text-align: left;
  }
}
</style>
