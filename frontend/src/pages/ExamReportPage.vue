<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'
import { formatAnswerDisplay, getQuestionTypeLabel, normalizeAnswerValue } from '../utils/question'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const leaderboard = ref([])
const reviewMode = ref('wrong')
const reviewSectionRef = ref(null)
const activeQuestionId = ref(null)
const report = ref({ exam: {}, record: {}, questions: [], wrongQuestions: [], analysisCourses: [] })

const totalQuestions = computed(() => report.value.questions?.length || 0)
const correctCount = computed(() => Number(report.value.record?.correctCount || 0))
const wrongCount = computed(() => report.value.questions.filter((item) => getQuestionStatus(item) === 'wrong').length)
const pendingCount = computed(() => report.value.questions.filter((item) => getQuestionStatus(item) === 'pending').length)
const accuracyRate = computed(() => {
  if (!totalQuestions.value) return 0
  return Math.round((correctCount.value / totalQuestions.value) * 1000) / 10
})
const currentQuestionIndex = computed(() => report.value.questions.findIndex((item) => item.id === activeQuestionId.value))
const currentQuestion = computed(() => {
  const found = report.value.questions.find((item) => item.id === activeQuestionId.value)
  return found || report.value.questions[0] || {}
})
const wrongQuestions = computed(() => report.value.questions.filter((item) => getQuestionStatus(item) === 'wrong'))
const reviewQuestions = computed(() => (reviewMode.value === 'wrong' ? wrongQuestions.value : report.value.questions))
const reviewTitle = computed(() => (reviewMode.value === 'wrong' ? '错题解析' : '全部解析'))
const sectionCards = computed(() => {
  const grouped = new Map()
  report.value.questions.forEach((item) => {
    const key = item.categoryName || '未分类'
    if (!grouped.has(key)) {
      grouped.set(key, {
        name: key,
        total: 0,
        correct: 0,
        wrong: 0,
        pending: 0,
        score: 0,
        possibleScore: 0
      })
    }
    const group = grouped.get(key)
    const score = Number(item.score || 0)
    const status = getQuestionStatus(item)
    group.total += 1
    group.possibleScore += score
    if (status === 'correct') {
      group.correct += 1
      group.score += score
    } else if (status === 'wrong') {
      group.wrong += 1
    } else {
      group.pending += 1
    }
  })
  return Array.from(grouped.values())
})

function isAnswered(item) {
  return Boolean(normalizeAnswerValue(item?.userAnswer))
}

function getQuestionStatus(item) {
  if (!isAnswered(item)) return 'pending'
  return item?.isCorrect ? 'correct' : 'wrong'
}

function formatDuration(value) {
  const total = Math.max(Number(value) || 0, 0)
  const hour = Math.floor(total / 3600)
  const minute = Math.floor((total % 3600) / 60)
  const second = total % 60
  if (hour > 0) {
    return `${hour}小时 ${minute}分 ${second}秒`
  }
  if (minute > 0) {
    return `${minute}分 ${second}秒`
  }
  return `${second}秒`
}

function formatDurationCompact(value) {
  const total = Math.max(Number(value) || 0, 0)
  const minute = Math.floor(total / 60)
  const second = total % 60
  if (minute > 0) {
    return `${minute}分${second}秒`
  }
  return `${second}秒`
}

function hasOption(answerValue, optionKey) {
  return normalizeAnswerValue(answerValue).split(',').filter(Boolean).includes(optionKey)
}

function selectQuestion(questionId) {
  activeQuestionId.value = questionId
}

async function switchReviewMode(mode) {
  reviewMode.value = mode
  const targetList = mode === 'wrong' ? wrongQuestions.value : report.value.questions
  if (targetList.length) {
    activeQuestionId.value = targetList[0].id
  }
  await nextTick()
  reviewSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function loadLeaderboard(examId) {
  const ranking = await request.get(`/api/exams/${examId}/ranking`, { params: { scope: 'GLOBAL' } })
  leaderboard.value = ranking.records?.slice(0, 5) || []
}

async function loadData() {
  loading.value = true
  try {
    report.value = await request.get(`/api/exam-records/${route.params.id}/report`)
    if (report.value.questions?.length) {
      activeQuestionId.value = report.value.questions[0].id
    }
    if (report.value.exam?.id) {
      await loadLeaderboard(report.value.exam.id)
    }
    if (wrongQuestions.value.length) {
      reviewMode.value = 'wrong'
      activeQuestionId.value = wrongQuestions.value[0].id
    } else {
      reviewMode.value = 'all'
    }
  } finally {
    loading.value = false
  }
}

async function toggleFavorite(item) {
  const nextFavorite = !item.favorite
  if (item.favorite) {
    await request.delete(`/api/question-favorites/${item.id}`)
    ElMessage.success('已取消收藏')
  } else {
    await request.post(`/api/question-favorites/${item.id}`)
    ElMessage.success('已加入收藏')
  }
  report.value.questions.forEach((question) => {
    if (question.id === item.id) {
      question.favorite = nextFavorite
    }
  })
}

function continueExam() {
  router.push(`/exam/detail/${report.value.exam.id}`)
}

function backToExam() {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push('/exam')
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section class="report-hero">
      <div class="glass-card hero-main">
        <div class="hero-head">
          <div>
            <div class="section-title">模考成绩报告</div>
            <p class="hero-copy">已生成成绩、排名和逐题解析。先看结果，再按题号复盘。</p>
          </div>
          <div class="hero-actions">
            <el-button plain @click="backToExam">返回</el-button>
            <el-button type="primary" @click="continueExam">继续模考</el-button>
          </div>
        </div>

        <div class="exam-title-card">
          <div class="exam-title-label">模考名称</div>
          <div class="exam-title-value">{{ report.exam.title }}</div>
        </div>

        <div class="summary-grid">
          <div class="metric-card">
            <span>得分</span>
            <strong>{{ report.record.score || 0 }}</strong>
          </div>
          <div class="metric-card">
            <span>排名</span>
            <strong>{{ report.rank || '-' }}</strong>
          </div>
          <div class="metric-card">
            <span>答对题数</span>
            <strong>{{ correctCount }}</strong>
          </div>
          <div class="metric-card">
            <span>正确率</span>
            <strong>{{ accuracyRate }}%</strong>
          </div>
          <div class="metric-card">
            <span>总用时</span>
            <strong>{{ formatDuration(report.record.durationSeconds) }}</strong>
          </div>
          <div class="metric-card">
            <span>总题量</span>
            <strong>{{ totalQuestions }}</strong>
          </div>
        </div>
      </div>

      <aside class="glass-card leaderboard-card">
        <div class="leaderboard-head">
          <div>
            <div class="section-title small">前五排行榜</div>
            <p>按得分、用时排序</p>
          </div>
          <el-button text type="primary" @click="router.push(`/exam/rank/${report.exam.id}`)">全部排行</el-button>
        </div>
        <div v-if="leaderboard.length" class="leaderboard-list">
          <div v-for="item in leaderboard" :key="`${item.rank}-${item.userId}`" class="leaderboard-item">
            <div class="leaderboard-rank">#{{ item.rank }}</div>
            <div class="leaderboard-body">
              <strong>{{ item.nickname }}</strong>
              <span>{{ item.score }} 分 · {{ formatDurationCompact(item.durationSeconds) }}</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无排行榜数据" />
      </aside>
    </section>

    <section class="glass-card section-score-card">
      <div class="section-head">
        <div>
          <div class="section-title">模考成绩卡</div>
          <p>按专项聚合本次作答结果。</p>
        </div>
      </div>
      <div class="section-card-grid">
        <article v-for="item in sectionCards" :key="item.name" class="section-metric-card">
          <h3>{{ item.name }}</h3>
          <div class="section-metric-main">
            <strong>{{ item.score }}</strong>
            <span>/ {{ item.possibleScore }} 分</span>
          </div>
          <p>答对 {{ item.correct }} 题，答错 {{ item.wrong }} 题，未答 {{ item.pending }} 题</p>
        </article>
      </div>
    </section>

    <section class="report-layout">
      <aside class="glass-card nav-card">
        <div class="section-title small">答题卡复盘</div>
        <p class="card-copy">共 {{ totalQuestions }} 题，点击题号查看对应题目。</p>
        <div class="legend">
          <span><i class="legend-dot correct"></i>答对</span>
          <span><i class="legend-dot wrong"></i>答错</span>
          <span><i class="legend-dot pending"></i>未答</span>
        </div>
        <div class="card-grid">
          <button
            v-for="(item, index) in report.questions"
            :key="item.id"
            class="card-cell"
            :class="[getQuestionStatus(item), { active: activeQuestionId === item.id }]"
            @click="selectQuestion(item.id)"
          >
            {{ index + 1 }}
          </button>
        </div>
      </aside>

      <section ref="reviewSectionRef" class="glass-card review-panel">
        <div class="review-toolbar">
          <div>
            <div class="section-title small">{{ reviewTitle }}</div>
            <p>{{ reviewMode === 'wrong' ? `集中复盘 ${wrongCount} 道错题` : `完整查看 ${totalQuestions} 道题解析` }}</p>
          </div>
          <div class="toolbar-actions">
            <el-button :type="reviewMode === 'wrong' ? 'danger' : 'default'" :disabled="wrongCount === 0" @click="switchReviewMode('wrong')">错题解析</el-button>
            <el-button :type="reviewMode === 'all' ? 'primary' : 'default'" @click="switchReviewMode('all')">全部解析</el-button>
            <el-button plain @click="router.push('/wrong-book')">错题本</el-button>
          </div>
        </div>

        <div class="review-summary-strip">
          <span>答对 {{ correctCount }} 题</span>
          <span>答错 {{ wrongCount }} 题</span>
          <span>未答 {{ pendingCount }} 题</span>
        </div>

        <article v-if="currentQuestion.id" class="question-detail">
          <div class="question-head">
            <div>
              <div class="question-number">第 {{ currentQuestionIndex + 1 }} 题</div>
              <p v-if="currentQuestion.material" class="analysis">{{ currentQuestion.material }}</p>
              <h3>{{ currentQuestion.stem }}</h3>
            </div>
            <span :class="['status-badge', getQuestionStatus(currentQuestion)]">
              {{ getQuestionStatus(currentQuestion) === 'correct' ? '答对' : (getQuestionStatus(currentQuestion) === 'wrong' ? '答错' : '未答') }}
            </span>
          </div>

          <div class="meta-row">
            <span>{{ currentQuestion.subjectName }}</span>
            <span>{{ currentQuestion.categoryName }}</span>
            <span>{{ getQuestionTypeLabel(currentQuestion.questionType) }}</span>
            <span>{{ currentQuestion.difficultyLevel }}</span>
            <span>{{ currentQuestion.score }} 分</span>
          </div>

          <div class="option-list">
            <div
              v-for="option in currentQuestion.options"
              :key="option.id"
              class="option-item"
              :class="{
                chosen: hasOption(currentQuestion.userAnswer, option.optionKey),
                correct: hasOption(currentQuestion.correctAnswer, option.optionKey)
              }"
            >
              {{ option.optionKey }}. {{ option.optionContent }}
            </div>
          </div>

          <div class="answer-block">
            <p><span>你的答案</span>{{ formatAnswerDisplay(currentQuestion.userAnswer) }}</p>
            <p><span>正确答案</span>{{ formatAnswerDisplay(currentQuestion.correctAnswer) }}</p>
            <p><span>知识点</span>{{ currentQuestion.knowledgePoints || '未配置知识点' }}</p>
          </div>

          <div class="analysis-panel">
            <div class="analysis-title">题目解析</div>
            <p>{{ currentQuestion.analysis || '暂无解析' }}</p>
          </div>

          <div class="question-actions">
            <a v-if="currentQuestion.videoExplainUrl" :href="currentQuestion.videoExplainUrl" target="_blank" rel="noreferrer">查看视频讲解</a>
            <el-button text type="warning" @click="toggleFavorite(currentQuestion)">{{ currentQuestion.favorite ? '取消收藏' : '收藏题目' }}</el-button>
          </div>
        </article>

        <div class="review-list">
          <button
            v-for="item in reviewQuestions"
            :key="`review-${item.id}`"
            class="review-list-item"
            :class="{ active: activeQuestionId === item.id }"
            @click="selectQuestion(item.id)"
          >
            <span class="review-index">第 {{ report.questions.findIndex((question) => question.id === item.id) + 1 }} 题</span>
            <span class="review-stem">{{ item.stem }}</span>
          </button>
        </div>
      </section>
    </section>

    <section class="glass-card course-card">
      <div class="section-title">模考解析课程推荐</div>
      <div class="course-grid" v-if="report.analysisCourses?.length">
        <article v-for="item in report.analysisCourses" :key="item.courseId" class="course-item">
          <h3>{{ item.title }}</h3>
          <p>{{ item.recommendReason }}</p>
          <div class="course-foot">
            <span>{{ item.teacherName }}</span>
            <el-button text type="primary" @click="router.push(item.path)">查看课程</el-button>
          </div>
        </article>
      </div>
      <el-empty v-else description="当前暂无课程推荐" />
    </section>
  </div>
</template>

<style scoped>
.report-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 20px;
}

.hero-main,
.leaderboard-card,
.section-score-card,
.nav-card,
.review-panel,
.course-card {
  padding: 24px;
}

.hero-head,
.leaderboard-head,
.section-head,
.review-toolbar,
.question-head,
.course-foot {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.hero-copy,
.leaderboard-head p,
.section-head p,
.card-copy,
.review-toolbar p,
.meta-row,
.course-item p {
  color: var(--muted);
}

.hero-actions,
.toolbar-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.exam-title-card {
  margin-top: 20px;
  padding: 18px 20px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: #fff;
}

.exam-title-label {
  color: var(--muted);
  font-size: 14px;
}

.exam-title-value {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 700;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 14px;
  margin-top: 20px;
}

.metric-card,
.section-metric-card,
.course-item {
  border: 1px solid var(--border);
  border-radius: 16px;
  background: #fff;
}

.metric-card {
  padding: 16px;
}

.metric-card span {
  color: var(--muted);
}

.metric-card strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: var(--primary);
}

.leaderboard-list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.leaderboard-item {
  display: flex;
  gap: 12px;
  align-items: center;
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 12px 14px;
  background: #fff;
}

.leaderboard-rank {
  width: 40px;
  font-size: 18px;
  font-weight: 700;
  color: var(--primary);
  text-align: center;
}

.leaderboard-body {
  display: grid;
  gap: 4px;
}

.leaderboard-body span {
  color: var(--muted);
  font-size: 13px;
}

.section-score-card,
.course-card {
  margin-top: 24px;
}

.section-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.section-metric-card {
  padding: 18px;
}

.section-metric-card h3 {
  margin: 0;
  font-size: 18px;
}

.section-metric-main {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-top: 14px;
}

.section-metric-main strong {
  font-size: 28px;
  color: var(--primary);
}

.section-metric-card p {
  margin: 12px 0 0;
  color: var(--muted);
}

.report-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: 20px;
  margin-top: 24px;
}

.section-title.small {
  font-size: 22px;
}

.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin-top: 14px;
  color: var(--muted);
  font-size: 13px;
}

.legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
}

.legend-dot.correct {
  background: #10b981;
}

.legend-dot.wrong {
  background: #ef4444;
}

.legend-dot.pending {
  background: #cbd5e1;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  margin-top: 18px;
}

.card-cell {
  height: 42px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
  font-weight: 600;
  transition: 0.2s ease;
}

.card-cell.correct {
  background: #dcfce7;
  border-color: #10b981;
  color: #047857;
}

.card-cell.wrong {
  background: #fee2e2;
  border-color: #ef4444;
  color: #b91c1c;
}

.card-cell.pending {
  background: #f8fafc;
  color: #64748b;
}

.card-cell.active {
  box-shadow: inset 0 0 0 2px var(--primary);
}

.review-summary-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
}

.review-summary-strip span {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  background: #f8fafc;
  color: var(--muted);
}

.question-detail {
  margin-top: 20px;
  padding: 20px;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: #fff;
}

.question-number {
  color: var(--muted);
  font-size: 14px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.status-badge.correct {
  background: #dcfce7;
  color: #047857;
}

.status-badge.wrong {
  background: #fee2e2;
  color: #b91c1c;
}

.status-badge.pending {
  background: #e2e8f0;
  color: #475569;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin-top: 14px;
}

.option-list {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}

.option-item {
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 12px 14px;
  background: #fff;
}

.option-item.chosen {
  border-color: rgba(37, 99, 235, 0.45);
  background: rgba(37, 99, 235, 0.06);
}

.option-item.correct {
  border-color: rgba(16, 185, 129, 0.5);
  background: rgba(16, 185, 129, 0.08);
}

.answer-block {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}

.answer-block p {
  margin: 0;
  display: grid;
  gap: 4px;
}

.answer-block span,
.analysis-title {
  color: var(--muted);
  font-size: 14px;
}

.analysis-panel {
  margin-top: 18px;
  border-radius: 14px;
  background: #f8fafc;
  padding: 16px;
}

.analysis-panel p {
  margin: 8px 0 0;
}

.question-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}

.review-list {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}

.review-list-item {
  display: grid;
  gap: 6px;
  text-align: left;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: #fff;
  padding: 12px 14px;
  cursor: pointer;
}

.review-list-item.active {
  border-color: rgba(37, 99, 235, 0.35);
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.18);
  background: rgba(37, 99, 235, 0.04);
}

.review-index {
  color: var(--primary);
  font-size: 13px;
  font-weight: 700;
}

.review-stem {
  color: #1f2937;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
  margin-top: 18px;
}

.course-item {
  padding: 18px;
}

@media (max-width: 1100px) {
  .report-hero,
  .report-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .hero-head,
  .leaderboard-head,
  .review-toolbar,
  .question-head,
  .course-foot {
    flex-direction: column;
  }

  .hero-actions,
  .toolbar-actions {
    width: 100%;
  }

  .hero-actions :deep(.el-button),
  .toolbar-actions :deep(.el-button) {
    flex: 1;
  }

  .card-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}
</style>
