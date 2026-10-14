<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'
import { useAuthStore } from '../stores/auth'
import { getQuestionTypeLabel } from '../utils/question'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const loading = ref(false)
const checkinLoading = ref(false)
const paperLoading = ref(false)
const groupPreviewLoading = ref(false)
const groupPreviewVisible = ref(false)
const list = ref([])
const papers = ref([])
const shenlunGroups = ref([])
const selectedGroup = ref(null)
const paperTotal = ref(0)
const shenlunGroupTotal = ref(0)
const total = ref(0)
const activeSubject = ref('行测')
const activeMode = ref('行测')
const categoryStats = ref({})
const checkin = ref({ checkedInToday: false, streakDays: 0, totalCheckinCount: 0, recentDates: [] })
const query = reactive({
  pageNo: 1,
  pageSize: 8,
  keyword: '',
  province: '',
  subjectName: '行测',
  categoryName: '',
  questionType: '',
  difficultyLevel: '',
  sourceName: '',
  questionCount: 10,
  mode: 'RANDOM'
})

const shenlunQuery = reactive({
  pageNo: 1,
  pageSize: 6,
  keyword: '',
  province: '',
  sourceName: '',
  year: ''
})

const subjectCategories = {
  行测: ['常识判断', '言语理解与表达', '资料分析', '判断推理', '政治理论', '数量关系'],
  申论: ['归纳概括', '综合分析', '提出对策', '应用文写作', '文章写作']
}

const paperQuery = reactive({
  pageNo: 1,
  pageSize: 12,
  province: ''
})

const currentCategories = computed(() => subjectCategories[activeSubject.value] || [])
const isPaperMode = computed(() => activeMode.value === '试卷')
const isShenlunMode = computed(() => !isPaperMode.value && activeSubject.value === '申论')
const shenlunGroupSummary = computed(() => {
  const questionCount = shenlunGroups.value.reduce((sum, item) => sum + Number(item.questionCount || 0), 0)
  const totalScore = shenlunGroups.value.reduce((sum, item) => sum + Number(item.totalScore || 0), 0)
  return {
    groupCount: shenlunGroupTotal.value,
    questionCount,
    totalScore
  }
})

function progressKey(subject, category) {
  return `civil-practice-progress:${subject}:${category}`
}

function progressIdsKey(subject, category) {
  return `${progressKey(subject, category)}:ids`
}

function readProgressIds(subject, category) {
  try {
    const value = JSON.parse(localStorage.getItem(progressIdsKey(subject, category)) || '[]')
    return Array.isArray(value) ? new Set(value.map(String)) : new Set()
  } catch {
    return new Set()
  }
}

function readPracticedCount(subject, category) {
  const ids = readProgressIds(subject, category)
  if (ids.size > 0) return ids.size
  const value = Number(localStorage.getItem(progressKey(subject, category)) || 0)
  return Number.isFinite(value) ? Math.max(0, value) : 0
}

function normalizedPracticedCount(subject, category, total = 0) {
  const count = readPracticedCount(subject, category)
  return total > 0 ? Math.min(count, total) : count
}

function syncCategoryProgress() {
  currentCategories.value.forEach((category) => {
    const key = `${activeSubject.value}:${category}`
    const totalCount = categoryStats.value[key]?.total || 0
    categoryStats.value[key] = {
      ...(categoryStats.value[key] || { total: 0 }),
      practiced: normalizedPracticedCount(activeSubject.value, category, totalCount)
    }
  })
}

async function loadCategoryTotals() {
  const entries = await Promise.all(currentCategories.value.map(async (category) => {
    const data = await request.get('/api/questions', {
      params: { pageNo: 1, pageSize: 1, subjectName: activeSubject.value, categoryName: category }
    })
    return [category, data.total]
  }))
  entries.forEach(([category, categoryTotal]) => {
    const key = `${activeSubject.value}:${category}`
    categoryStats.value[key] = {
      total: categoryTotal,
      practiced: normalizedPracticedCount(activeSubject.value, category, categoryTotal)
    }
  })
}

async function loadData() {
  if (isShenlunMode.value) {
    await loadShenlunGroups()
    return
  }
  loading.value = true
  try {
    query.subjectName = activeSubject.value
    const data = await request.get('/api/questions', { params: query })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadShenlunGroups() {
  loading.value = true
  try {
    const data = await request.get('/api/shenlun-groups', {
      params: {
        ...shenlunQuery,
        year: shenlunQuery.year ? Number(shenlunQuery.year) : undefined
      }
    })
    shenlunGroups.value = data.records
    shenlunGroupTotal.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadCheckin() {
  if (!authStore.isLoggedIn) {
    checkin.value = { checkedInToday: false, streakDays: 0, totalCheckinCount: 0, recentDates: [] }
    return
  }
  checkin.value = await request.get('/api/question-bank/check-in')
}

async function loadPapers() {
  paperLoading.value = true
  try {
    const data = await request.get('/api/questions/papers', { params: paperQuery })
    papers.value = data.records
    paperTotal.value = data.total
  } finally {
    paperLoading.value = false
  }
}

async function submitCheckin() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后再打卡')
    router.push('/login')
    return
  }
  checkinLoading.value = true
  try {
    checkin.value = await request.post('/api/question-bank/check-in')
    ElMessage.success('今日打卡已完成')
  } finally {
    checkinLoading.value = false
  }
}

function syncQueryToRoute() {
  const next = {}
  if (activeMode.value === '试卷') next.mode = 'paper'
  else if (activeSubject.value === '申论') next.subject = '申论'
  if (JSON.stringify(next) !== JSON.stringify(route.query)) {
    router.replace({ path: route.path, query: next })
  }
}

async function changeSubject(subject) {
  activeMode.value = subject
  activeSubject.value = subject
  query.pageNo = 1
  query.subjectName = subject
  query.categoryName = ''
  syncQueryToRoute()
  if (subject === '申论') {
    shenlunQuery.pageNo = 1
    await loadShenlunGroups()
    return
  }
  await Promise.all([loadData(), loadCategoryTotals()])
  syncCategoryProgress()
}

async function changeMode(mode) {
  activeMode.value = mode
  if (mode === '试卷') {
    syncQueryToRoute()
    await loadPapers()
    return
  }
  await changeSubject(mode)
}

watch(
  () => [route.query.mode, route.query.subject],
  async ([nextMode, nextSubject]) => {
    if (nextMode === 'paper' && activeMode.value !== '试卷') {
      activeMode.value = '试卷'
      await loadPapers()
    } else if (nextSubject === '申论' && activeSubject.value !== '申论') {
      activeMode.value = '申论'
      activeSubject.value = '申论'
      await loadShenlunGroups()
    } else if (!nextMode && !nextSubject && activeMode.value !== '行测') {
      await changeSubject('行测')
    }
  }
)

async function startPaper(paper) {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后再开始答题')
    router.push('/login')
    return
  }
  try {
    const data = await request.post(`/api/questions/papers/${paper.id}/start`)
    sessionStorage.setItem(`civil-practice-context:${data.practiceId}`, JSON.stringify({
      subjectName: '行测',
      categoryName: '试卷',
      returnPath: '/question-bank?mode=paper'
    }))
    router.push(`/practice/${data.practiceId}`)
  } catch (error) {
    ElMessage.error(error.message || '试卷生成失败')
  }
}

async function previewShenlunGroup(groupId) {
  groupPreviewLoading.value = true
  groupPreviewVisible.value = true
  try {
    const data = await request.get(`/api/shenlun-groups/${groupId}`)
    selectedGroup.value = data.group
  } catch (error) {
    groupPreviewVisible.value = false
    ElMessage.error(error.message || '材料组详情加载失败')
  } finally {
    groupPreviewLoading.value = false
  }
}

async function startShenlunGroup(group) {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后再开始练习')
    router.push('/login')
    return
  }
  try {
    const data = await request.post(`/api/shenlun-groups/${group.id}/start-practice`)
    sessionStorage.setItem(`civil-practice-context:${data.practiceId}`, JSON.stringify({
      subjectName: '申论',
      groupId: group.id,
      returnPath: '/question-bank'
    }))
    router.push(`/practice/${data.practiceId}`)
  } catch (error) {
    ElMessage.error(error.message || '申论材料组练习生成失败')
  }
}

async function startPractice(category) {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后再开始练习')
    router.push('/login')
    return
  }
  const key = `${activeSubject.value}:${category || query.categoryName}`
  const categoryTotal = categoryStats.value[key]?.total || query.questionCount
  const questionCount = Math.min(50, Math.max(1, category ? categoryTotal : query.questionCount))
  try {
    const data = await request.post('/api/practices', {
      keyword: category ? '' : query.keyword,
      province: query.province,
      subjectName: activeSubject.value,
      categoryName: category || query.categoryName,
      questionType: activeSubject.value === '申论' ? '' : query.questionType,
      difficultyLevel: query.difficultyLevel,
      sourceName: query.sourceName,
      questionCount,
      mode: 'TOPIC'
    })
    sessionStorage.setItem(`civil-practice-context:${data.practiceId}`, JSON.stringify({
      subjectName: activeSubject.value,
      categoryName: category || query.categoryName,
      returnPath: '/question-bank'
    }))
    ElMessage.success('练习已生成')
    router.push(`/practice/${data.practiceId}`)
  } catch (error) {
    ElMessage.error(error.message || '练习生成失败')
  }
}

onMounted(async () => {
  if (route.query.mode === 'paper') {
    activeMode.value = '试卷'
    await Promise.all([loadCheckin(), loadPapers()])
    return
  }
  if (route.query.subject === '申论') {
    activeMode.value = '申论'
    activeSubject.value = '申论'
    await Promise.all([loadCheckin(), loadShenlunGroups()])
    return
  }
  await Promise.all([loadData(), loadCheckin(), loadCategoryTotals()])
  syncCategoryProgress()
})
</script>

<template>
  <div class="page-shell">
    <section class="glass-card question-bank-hero">
      <div>
        <div class="section-title">题库中心</div>
        <p>行测按专项练习，申论按材料组练习，用户进入后直接围绕整组资料连续作答。</p>
      </div>
      <div class="hero-actions">
        <el-button plain @click="router.push('/search')">搜题</el-button>
        <el-button plain @click="router.push('/wrong-book')">错题本</el-button>
      </div>
    </section>

    <section class="glass-card filter-box">
      <div class="subject-tabs" role="tablist" aria-label="科目筛选">
        <button
          v-for="subject in Object.keys(subjectCategories)"
          :key="subject"
          class="subject-tab"
          :class="{ active: activeMode === subject }"
          @click="changeMode(subject)"
        >
          {{ subject }}
        </button>
        <button class="subject-tab" :class="{ active: activeMode === '试卷' }" @click="changeMode('试卷')">
          试卷
        </button>
      </div>

      <div class="checkin-panel">
        <div>
          <strong>每日打卡</strong>
          <p>连续 {{ checkin.streakDays || 0 }} 天，累计 {{ checkin.totalCheckinCount || 0 }} 次</p>
        </div>
        <div class="checkin-actions">
          <div class="recent-dates" v-if="checkin.recentDates?.length">
            <span v-for="item in checkin.recentDates" :key="item" class="date-chip">{{ item }}</span>
          </div>
          <el-button :type="checkin.checkedInToday ? 'success' : 'warning'" :loading="checkinLoading" @click="submitCheckin">
            {{ checkin.checkedInToday ? '今日已打卡' : '立即打卡' }}
          </el-button>
        </div>
      </div>

      <template v-if="isPaperMode">
        <div class="section-head">
          <div>
            <h2>试卷练习</h2>
            <p>每套试卷包含行测六个专项各 20 题，每题 1 分，总分 120 分。</p>
          </div>
          <el-input v-model="paperQuery.province" placeholder="省份筛选" clearable style="max-width: 180px" @keyup.enter="loadPapers" />
        </div>
        <div class="paper-grid" v-loading="paperLoading">
          <article v-for="paper in papers" :key="paper.id" class="paper-card">
            <h3>{{ paper.title }}</h3>
            <p>共 {{ paper.questionCount }} 题，总时间 {{ paper.durationMinutes }} 分钟</p>
            <p>{{ paper.subjectName }} · {{ paper.province }} · 总分 {{ paper.totalScore }}</p>
            <el-button type="danger" @click="startPaper(paper)">我要答题</el-button>
          </article>
        </div>
        <el-pagination
          v-if="paperTotal > paperQuery.pageSize"
          background
          layout="prev, pager, next"
          :total="paperTotal"
          :page-size="paperQuery.pageSize"
          :current-page="paperQuery.pageNo"
          style="margin-top: 24px; justify-content: center"
          @current-change="(page) => { paperQuery.pageNo = page; loadPapers() }"
        />
      </template>

      <template v-else-if="isShenlunMode">
        <div class="section-head">
          <div>
            <h2>申论材料组练习</h2>
            <p>以一组材料搭配多道小题为单位进入训练，做题时共享材料始终固定显示。</p>
          </div>
        </div>

        <div class="shenlun-overview">
          <div>
            <span>材料组</span>
            <strong>{{ shenlunGroupSummary.groupCount }}</strong>
          </div>
          <div>
            <span>当前页题量</span>
            <strong>{{ shenlunGroupSummary.questionCount }}</strong>
          </div>
          <div>
            <span>当前页总分</span>
            <strong>{{ shenlunGroupSummary.totalScore }}</strong>
          </div>
        </div>

        <div class="list-toolbar shenlun-toolbar">
          <div class="filter-grid">
            <el-input v-model="shenlunQuery.keyword" placeholder="搜索材料组标题或来源" clearable />
            <el-input v-model="shenlunQuery.province" placeholder="省份" clearable />
            <el-input v-model="shenlunQuery.sourceName" placeholder="来源" clearable />
            <el-input v-model="shenlunQuery.year" placeholder="年份" clearable />
          </div>
          <div class="action-row">
            <el-button type="primary" @click="shenlunQuery.pageNo = 1; loadShenlunGroups()">查询材料组</el-button>
          </div>
        </div>

        <div class="shenlun-group-grid" v-loading="loading">
          <article v-for="group in shenlunGroups" :key="group.id" class="shenlun-group-card">
            <div class="question-top">
              <span class="pill">{{ group.year || '未标注年份' }}</span>
              <span class="pill muted">{{ group.paperType || '申论真题' }}</span>
            </div>
            <h3>{{ group.title }}</h3>
            <p>{{ group.province }} · {{ group.questionCount }} 题 · 总分 {{ group.totalScore }} · {{ group.materialCount }} 份材料</p>
            <p>{{ group.sourceName }}</p>
            <div class="category-tags">
              <span v-for="category in group.categories || []" :key="category">{{ category }}</span>
            </div>
            <p v-if="group.previewStem" class="preview-stem">{{ group.previewStem }}</p>
            <div class="group-card-actions">
              <el-button plain @click="previewShenlunGroup(group.id)">查看材料组</el-button>
              <el-button type="primary" @click="startShenlunGroup(group)">开始练习</el-button>
            </div>
          </article>
        </div>

        <el-pagination
          v-if="shenlunGroupTotal > shenlunQuery.pageSize"
          background
          layout="prev, pager, next"
          :total="shenlunGroupTotal"
          :page-size="shenlunQuery.pageSize"
          :current-page="shenlunQuery.pageNo"
          style="margin-top: 24px; justify-content: center"
          @current-change="(page) => { shenlunQuery.pageNo = page; loadShenlunGroups() }"
        />
      </template>

      <template v-else>
        <div class="section-head">
          <div>
            <h2>专项练习</h2>
            <p>{{ activeSubject }} 专项按导入题库实时统计总题量，点击专项后进入对应做题页。</p>
          </div>
        </div>

        <div class="special-list" v-loading="loading">
          <div v-for="category in currentCategories" :key="category" class="special-row">
            <div class="special-main">
              <strong>{{ category }}</strong>
              <span>
                已练 {{ categoryStats[`${activeSubject}:${category}`]?.practiced || 0 }} /
                {{ categoryStats[`${activeSubject}:${category}`]?.total || 0 }} 题
              </span>
            </div>
            <el-button type="primary" :disabled="!(categoryStats[`${activeSubject}:${category}`]?.total)" @click="startPractice(category)">
              开始练习
            </el-button>
          </div>
        </div>
      </template>
    </section>

    <section v-if="!isPaperMode && !isShenlunMode" class="glass-card list-box" v-loading="loading">
      <div class="list-toolbar">
        <div class="filter-grid">
          <el-input v-model="query.keyword" placeholder="搜索题干关键词" clearable />
          <el-input v-model="query.province" placeholder="省份" clearable />
          <el-select v-model="query.categoryName" placeholder="专项" clearable>
            <el-option v-for="category in currentCategories" :key="category" :label="category" :value="category" />
          </el-select>
          <el-select v-if="activeSubject === '行测'" v-model="query.questionType" placeholder="题型" clearable>
            <el-option label="单选题" value="SINGLE" />
            <el-option label="多选题" value="MULTI" />
          </el-select>
          <el-input v-model="query.difficultyLevel" placeholder="难度" clearable />
          <el-input v-model="query.sourceName" placeholder="来源" clearable />
          <el-input-number v-model="query.questionCount" :min="1" :max="50" />
        </div>
        <div class="action-row">
          <el-button type="primary" @click="loadData">查询题目</el-button>
          <el-button type="warning" @click="startPractice()">按筛选练习</el-button>
        </div>
      </div>

      <div class="question-grid">
        <article v-for="item in list" :key="item.id" class="question-card">
          <div class="question-top">
            <span class="pill">{{ item.subjectName }}</span>
            <span class="pill muted">{{ activeSubject === '申论' ? '简答题' : getQuestionTypeLabel(item.questionType) }}</span>
          </div>
          <h3>{{ item.stem }}</h3>
          <p v-if="item.material">{{ item.material.slice(0, 90) }}{{ item.material.length > 90 ? '...' : '' }}</p>
          <p>{{ item.categoryName }} · {{ item.province }} · {{ item.difficultyLevel }}</p>
          <div class="question-foot">{{ item.knowledgePoints || '未配置知识点' }}</div>
        </article>
      </div>

      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.pageNo"
        style="margin-top: 24px; justify-content: center"
        @current-change="(page) => { query.pageNo = page; loadData() }"
      />
    </section>

    <el-dialog v-model="groupPreviewVisible" width="980px" destroy-on-close>
      <template #header>
        <div v-if="selectedGroup">
          <strong>{{ selectedGroup.title }}</strong>
          <p class="dialog-subtitle">
            {{ selectedGroup.province }} · {{ selectedGroup.questionCount }} 题 · 总分 {{ selectedGroup.totalScore }}
          </p>
        </div>
      </template>
      <div v-loading="groupPreviewLoading" class="group-preview" v-if="selectedGroup">
        <section class="group-preview-section">
          <h3>材料列表</h3>
          <article v-for="material in selectedGroup.materials || []" :key="material.id" class="preview-material">
            <div class="preview-material-head">
              <strong>{{ material.label }}</strong>
              <span v-if="material.title">{{ material.title }}</span>
            </div>
            <p>{{ material.content }}</p>
          </article>
        </section>
        <section class="group-preview-section">
          <h3>组内题目</h3>
          <article v-for="question in selectedGroup.questions || []" :key="question.id" class="preview-question">
            <strong>第 {{ question.groupQuestionNo || question.id }} 题 · {{ question.categoryName }}</strong>
            <p>{{ question.stem }}</p>
          </article>
        </section>
      </div>
      <template #footer>
        <el-button @click="groupPreviewVisible = false">关闭</el-button>
        <el-button v-if="selectedGroup" type="primary" @click="startShenlunGroup(selectedGroup)">开始练习</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.question-bank-hero,
.filter-box,
.list-box {
  padding: 24px;
}

.question-bank-hero,
.section-head,
.checkin-panel,
.special-row,
.list-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.filter-box,
.list-box {
  margin-top: 20px;
}

.question-bank-hero p,
.section-head p,
.question-card p,
.question-foot,
.special-main span,
.dialog-subtitle {
  color: var(--muted);
}

.hero-actions,
.action-row,
.checkin-actions,
.recent-dates,
.group-card-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.subject-tabs {
  display: inline-flex;
  padding: 4px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: #f8fafc;
  margin-bottom: 18px;
}

.subject-tab {
  min-width: 84px;
  height: 36px;
  border: 0;
  border-radius: 8px;
  background: transparent;
  color: #475569;
  font-weight: 700;
  cursor: pointer;
}

.subject-tab.active {
  background: var(--primary);
  color: #fff;
}

.checkin-panel {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 16px 18px;
  background: linear-gradient(135deg, rgba(29, 78, 216, 0.08), rgba(249, 115, 22, 0.1));
  margin-bottom: 18px;
}

.checkin-panel p,
.section-head h2 {
  margin: 6px 0 0;
}

.date-chip {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: #475569;
  font-size: 12px;
}

.special-list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.paper-grid,
.question-grid,
.shenlun-group-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 18px;
  margin-top: 18px;
}

.paper-card,
.question-card,
.shenlun-group-card {
  min-height: 170px;
  display: grid;
  align-content: start;
  gap: 10px;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 22px 18px;
  background: #fff;
}

.shenlun-group-card {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.paper-card h3,
.question-card h3,
.shenlun-group-card h3 {
  margin: 0;
  color: #111827;
  font-size: 20px;
  line-height: 1.35;
}

.shenlun-group-card h3 {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.paper-card p,
.shenlun-group-card p {
  margin: 0;
  color: var(--muted);
}

.special-row {
  min-height: 72px;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 14px 16px;
  background: #fff;
}

.special-main {
  display: grid;
  gap: 6px;
}

.filter-grid {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 12px;
}

.list-toolbar {
  align-items: flex-start;
  margin-bottom: 18px;
}

.shenlun-toolbar {
  margin-top: 18px;
}

.question-card {
  padding: 18px;
}

.question-card h3,
.preview-stem {
  font-size: 16px;
  line-height: 1.6;
}

.question-top {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.pill {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(29, 78, 216, 0.12);
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
}

.pill.muted {
  background: rgba(15, 23, 42, 0.08);
  color: #475569;
}

.shenlun-overview {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-top: 18px;
}

.shenlun-overview div {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}

.shenlun-overview span {
  display: block;
  color: var(--muted);
}

.shenlun-overview strong {
  display: block;
  margin-top: 8px;
  font-size: 24px;
  color: var(--primary);
}

.category-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.category-tags span {
  padding: 4px 8px;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
}

.preview-stem {
  color: #334155;
}

.shenlun-group-card .preview-stem {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 8;
}

.group-card-actions {
  flex-wrap: nowrap;
  margin-top: auto;
  padding-top: 12px;
}

.group-card-actions :deep(.el-button) {
  flex: 1;
  min-width: 0;
  height: 40px;
  margin-left: 0;
  justify-content: center;
}

.group-preview {
  display: grid;
  gap: 20px;
  max-height: 70vh;
  overflow: auto;
}

.group-preview-section {
  display: grid;
  gap: 12px;
}

.group-preview-section h3 {
  margin: 0;
}

.preview-material,
.preview-question {
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 16px;
  background: #f8fbff;
}

.preview-material-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.preview-material p,
.preview-question p {
  margin: 0;
  color: #334155;
  line-height: 1.8;
  white-space: pre-wrap;
}

@media (max-width: 960px) {
  .question-bank-hero,
  .checkin-panel,
  .section-head,
  .special-row,
  .list-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .shenlun-overview {
    grid-template-columns: 1fr;
  }
}
</style>
