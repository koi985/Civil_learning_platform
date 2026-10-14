<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request'
import { formatAnswerDisplay, getQuestionTypeLabel } from '../utils/question'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const activeIndex = ref(0)
const reviewMode = ref('wrong')
const report = ref({ record: {}, questions: [], assessmentSummary: null })

const isEssayReport = computed(() => report.value.record?.subjectName === '申论' || report.value.questions?.[0]?.subjectName === '申论')
const isPaperReport = computed(() => report.value.record?.practiceType === 'TEST_PAPER' && report.value.record?.practiceMode === 'PAPER')
const currentQuestion = computed(() => report.value.questions?.[activeIndex.value] || {})
const wrongQuestions = computed(() => report.value.questions.filter((item) => !item.isCorrect))
const visibleQuestions = computed(() => reviewMode.value === 'wrong' && !isEssayReport.value ? wrongQuestions.value : report.value.questions)

function formatSeconds(value) {
  const total = Math.max(Number(value) || 0, 0)
  const minute = String(Math.floor(total / 60)).padStart(2, '0')
  const second = String(total % 60).padStart(2, '0')
  return `${minute}:${second}`
}

function answerStatus(question) {
  if (isEssayReport.value) {
    return question.userAnswer ? 'answered' : 'empty'
  }
  if (!question.userAnswer) return 'empty'
  if (String(question.userAnswer).toUpperCase() === 'UNKNOWN') return 'unknown'
  return question.isCorrect ? 'right' : 'wrong'
}

function objectiveAnswerLabel(value) {
  if (!value) return '未作答'
  if (String(value).toUpperCase() === 'UNKNOWN') return '不会'
  return formatAnswerDisplay(value)
}

function objectiveBadgeText(question) {
  if (!question.userAnswer) return '未答'
  if (String(question.userAnswer).toUpperCase() === 'UNKNOWN') return '不会'
  return question.isCorrect ? '答对' : '答错'
}

function objectiveBadgeClass(question) {
  if (!question.userAnswer) return 'muted'
  if (String(question.userAnswer).toUpperCase() === 'UNKNOWN') return 'warning'
  return question.isCorrect ? 'success' : 'danger'
}

function parseEssayAnswer(value) {
  if (!value) return { text: '', images: [], selfRating: '' }
  try {
    return { text: '', images: [], selfRating: '', ...JSON.parse(value) }
  } catch {
    return { text: value, images: [], selfRating: '' }
  }
}

async function loadData() {
  loading.value = true
  try {
    report.value = await request.get(`/api/practices/${route.params.id}/report`)
  } finally {
    loading.value = false
  }
}

function continuePractice() {
  router.push(isPaperReport.value ? '/question-bank?mode=paper' : '/question-bank')
}

function switchReviewMode(mode) {
  reviewMode.value = mode
  if (mode === 'wrong' && wrongQuestions.value.length) {
    const index = report.value.questions.findIndex((question) => question.id === wrongQuestions.value[0].id)
    activeIndex.value = Math.max(index, 0)
  }
  if (mode === 'all') {
    activeIndex.value = 0
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section class="glass-card summary-card">
      <div class="report-head">
        <div>
          <div class="section-title">{{ isPaperReport ? '试卷成绩报告' : (report.record.subjectName === '申论' ? '申论练习报告' : '行测练习报告') }}</div>
          <p>{{ isPaperReport ? report.record.title : (currentQuestion.categoryName || report.record.title) }}</p>
        </div>
        <el-button plain @click="router.push('/question-bank')">返回</el-button>
      </div>

      <div class="summary-grid" :class="{ essay: isEssayReport }">
        <div><span>{{ isPaperReport ? '试卷名称' : '专项名称' }}</span><strong>{{ isPaperReport ? report.record.title : (currentQuestion.categoryName || report.record.subjectName) }}</strong></div>
        <div v-if="!isEssayReport"><span>答对题数</span><strong>{{ report.record.correctCount }}</strong></div>
        <div v-if="!isEssayReport"><span>正确率</span><strong>{{ report.record.accuracyRate }}%</strong></div>
        <div><span>总用时</span><strong>{{ formatSeconds(report.record.durationSeconds) }}</strong></div>
      </div>
    </section>

    <section class="report-layout">
      <aside class="glass-card review-card">
        <div class="section-title small">答题卡复盘</div>
        <div class="card-grid">
          <button
            v-for="(item, index) in report.questions"
            :key="item.id"
            class="card-cell"
            :class="[answerStatus(item), { active: activeIndex === index }]"
            @click="activeIndex = index"
          >
            {{ index + 1 }}
          </button>
        </div>
        <div class="legend" v-if="isEssayReport">
          <span><i class="answered"></i>已答</span>
          <span><i class="empty"></i>未答</span>
        </div>
        <div class="legend" v-else>
          <span><i class="right"></i>答对</span>
          <span><i class="wrong"></i>答错</span>
          <span><i class="unknown"></i>不会</span>
          <span><i class="empty"></i>未答</span>
        </div>
      </aside>

      <section class="glass-card review-card">
        <div class="section-title small">{{ isPaperReport ? '试卷成绩卡' : '专项成绩卡' }}</div>
        <div class="score-card">
          <div>
            <span>{{ isPaperReport ? report.record.title : (currentQuestion.categoryName || report.record.title) }}</span>
            <strong v-if="!isEssayReport">{{ report.record.correctCount }} / {{ report.record.questionCount }}</strong>
            <strong v-else>{{ report.record.questionCount }} 题</strong>
          </div>
          <el-progress v-if="!isEssayReport" :percentage="Number(report.record.accuracyRate || 0)" />
          <el-progress v-else :percentage="Math.round((report.record.answeredCount || 0) * 100 / Math.max(report.record.questionCount || 1, 1))" />
        </div>

        <div class="action-row">
          <el-button v-if="!isEssayReport" :type="reviewMode === 'wrong' ? 'danger' : 'default'" @click="switchReviewMode('wrong')">错题解析</el-button>
          <el-button :type="reviewMode === 'all' || isEssayReport ? 'primary' : 'default'" @click="switchReviewMode('all')">全部解析</el-button>
          <el-button type="warning" @click="continuePractice">继续练习</el-button>
          <el-button plain @click="router.push('/question-bank')">返回</el-button>
        </div>

        <article class="question-review">
          <div class="review-head">
            <h3>第 {{ activeIndex + 1 }} 题</h3>
            <span v-if="!isEssayReport" :class="['badge', objectiveBadgeClass(currentQuestion)]">{{ objectiveBadgeText(currentQuestion) }}</span>
            <span v-else :class="['badge', currentQuestion.userAnswer ? 'success' : 'muted']">{{ currentQuestion.userAnswer ? '已答' : '未答' }}</span>
          </div>
          <p class="stem">{{ currentQuestion.stem }}</p>
          <p class="analysis">题型：{{ isEssayReport ? '简答题' : getQuestionTypeLabel(currentQuestion.questionType) }}</p>
          <div v-if="!isEssayReport" class="option-list">
            <div v-for="option in currentQuestion.options" :key="option.id" class="option-item">
              {{ option.optionKey }}. {{ option.optionContent }}
            </div>
          </div>
          <template v-if="isEssayReport">
            <p class="answer">你的作答：{{ parseEssayAnswer(currentQuestion.userAnswer).text || '未作答' }}</p>
            <p class="answer">自评：{{ parseEssayAnswer(currentQuestion.userAnswer).selfRating || '未自评' }}</p>
          </template>
          <template v-else>
            <p class="answer">你的答案：{{ objectiveAnswerLabel(currentQuestion.userAnswer) }}</p>
            <p class="answer">正确答案：{{ formatAnswerDisplay(currentQuestion.correctAnswer) }}</p>
          </template>
          <p class="analysis">{{ currentQuestion.analysis }}</p>
          <p class="analysis">知识点：{{ currentQuestion.knowledgePoints || '未配置知识点' }}</p>
        </article>
      </section>
    </section>

    <section class="review-list" v-if="visibleQuestions.length">
      <article v-for="(item, index) in visibleQuestions" :key="item.id" class="glass-card review-card compact">
        <div class="review-head">
          <h3>{{ reviewMode === 'wrong' && !isEssayReport ? '错题解析' : '全部解析' }} · 第 {{ index + 1 }} 题</h3>
          <el-button link type="primary" @click="activeIndex = report.questions.findIndex((question) => question.id === item.id)">查看题目</el-button>
        </div>
        <p class="stem">{{ item.stem }}</p>
        <p class="analysis">{{ item.analysis || '暂无解析' }}</p>
      </article>
    </section>
  </div>
</template>

<style scoped>
.summary-card,
.review-card {
  padding: 24px;
}

.report-head,
.review-head,
.action-row {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  flex-wrap: wrap;
}

.report-head p,
.summary-grid span,
.analysis {
  color: var(--muted);
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 14px;
  margin-top: 20px;
}

.summary-grid div {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}

.summary-grid strong {
  display: block;
  margin-top: 8px;
  font-size: 24px;
  color: var(--primary);
}

.report-layout {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 20px;
  margin-top: 20px;
}

.section-title.small {
  font-size: 22px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
}

.card-cell {
  height: 38px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
}

.card-cell.active {
  box-shadow: inset 0 0 0 2px var(--primary);
}

.card-cell.right {
  background: #dcfce7;
  border-color: #10b981;
}

.card-cell.wrong {
  background: #fee2e2;
  border-color: #ef4444;
}

.card-cell.answered {
  background: #dbeafe;
  border-color: var(--primary);
}

.card-cell.unknown {
  background: #fff7ed;
  border-color: #f97316;
}

.score-card {
  display: grid;
  gap: 14px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #f8fafc;
}

.score-card span {
  color: var(--muted);
}

.score-card strong {
  display: block;
  margin-top: 6px;
  font-size: 24px;
}

.action-row {
  justify-content: flex-start;
  margin: 18px 0;
}

.question-review,
.review-list {
  margin-top: 20px;
}

.review-list {
  display: grid;
  gap: 16px;
}

.review-card.compact {
  padding: 18px;
}

.stem {
  font-weight: 700;
  line-height: 1.7;
}

.option-list {
  display: grid;
  gap: 8px;
  margin: 14px 0;
}

.option-item {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 12px;
}

.answer {
  margin: 6px 0;
}

.badge {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  color: #fff;
  font-size: 12px;
}

.badge.success {
  background: #10b981;
}

.badge.danger {
  background: #ef4444;
}

.badge.muted {
  background: #64748b;
}

.legend {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 18px;
}

.legend i {
  display: inline-block;
  width: 12px;
  height: 12px;
  border-radius: 3px;
  margin-right: 6px;
  border: 1px solid var(--border);
  vertical-align: -1px;
}

.legend .right {
  background: #dcfce7;
  border-color: #10b981;
}

.legend .wrong {
  background: #fee2e2;
  border-color: #ef4444;
}

.legend .answered {
  background: #dbeafe;
  border-color: var(--primary);
}

.legend .unknown {
  background: #fff7ed;
  border-color: #f97316;
}

@media (max-width: 960px) {
  .report-layout {
    grid-template-columns: 1fr;
  }
}
</style>
