<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import request from '../utils/request'
import { formatAnswerDisplay, normalizeAnswerValue as normalizeQuestionAnswerValue } from '../utils/question'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const record = ref({})
const questions = ref([])
const shenlunGroup = ref(null)
const timeLimitSeconds = ref(null)
const currentIndex = ref(0)
const durationSeconds = ref(0)
const submitting = ref(false)
const answerCardVisible = ref(false)
const analysisVisible = ref(false)
const lastSavedAt = ref(null)
const activeMaterialNo = ref(null)
const markedMap = reactive({})
const answers = reactive({})
const answeredAtMap = reactive({})
const spentSecondsMap = reactive({})
const essayAiReviewLoading = ref(false)
const essayAiReviews = reactive({})
const essayAiReviewErrors = reactive({})
let timer = null

const currentQuestion = computed(() => questions.value[currentIndex.value] || {})
const isEssayPractice = computed(() => record.value.subjectName === '申论' || currentQuestion.value.subjectName === '申论')
const isPaperPractice = computed(() => record.value.practiceType === 'TEST_PAPER' && record.value.practiceMode === 'PAPER')
const isShenlunGroupPractice = computed(() => isEssayPractice.value && record.value.practiceMode === 'GROUP' && shenlunGroup.value)
const currentAnswer = computed(() => answers[currentQuestion.value.id])
const currentEssayAnswer = computed(() => parseEssayAnswer(currentAnswer.value))
const essayWordLimit = 500
const essayTextLength = computed(() => currentEssayAnswer.value.text.length)
const currentEssayAiReview = computed(() => essayAiReviews[currentQuestion.value.id] || null)
const currentEssayAiReviewError = computed(() => essayAiReviewErrors[currentQuestion.value.id] || '')
const canRequestEssayAiReview = computed(() => isEssayPractice.value && currentEssayAnswer.value.text.trim().length > 0 && !essayAiReviewLoading.value)
const currentQuestionScore = computed(() => currentQuestion.value.questionScore || currentQuestion.value.score || 0)
const essayWordStatus = computed(() => {
  if (essayTextLength.value > essayWordLimit) return 'danger'
  if (essayTextLength.value >= Math.floor(essayWordLimit * 0.85)) return 'warning'
  return 'normal'
})
const lastSavedText = computed(() => {
  if (!lastSavedAt.value) return '等待输入后自动保存'
  return `已自动保存 ${lastSavedAt.value}`
})
const answeredCount = computed(() => questions.value.filter((item) => isAnswered(item)).length)
const currentMaterialRefs = computed(() => {
  const refs = currentQuestion.value.groupMaterialRefs
  return Array.isArray(refs) ? refs : []
})
const groupedQuestions = computed(() => {
  if (isShenlunGroupPractice.value) {
    return [{ name: shenlunGroup.value.title || '材料组题目', items: questions.value.map((question, index) => ({ question, index })) }]
  }
  const groups = []
  questions.value.forEach((question, index) => {
    const name = question.categoryName || '综合'
    let group = groups.find((item) => item.name === name)
    if (!group) {
      group = { name, items: [] }
      groups.push(group)
    }
    group.items.push({ question, index })
  })
  return groups
})
const groupMaterials = computed(() => {
  if (shenlunGroup.value?.materials?.length) {
    return shenlunGroup.value.materials.map((material, index) => ({
      ...material,
      sortNo: Number(material.sortNo || index + 1)
    }))
  }
  if (currentQuestion.value.material) {
    return [{
      id: currentQuestion.value.id,
      sortNo: Number(currentQuestion.value.groupQuestionNo || currentIndex.value + 1),
      label: `给定资料${currentQuestion.value.groupQuestionNo || currentIndex.value + 1}`,
      content: currentQuestion.value.material
    }]
  }
  return []
})
const currentMaterial = computed(() => {
  if (!groupMaterials.value.length) return null
  if (activeMaterialNo.value !== null) {
    return groupMaterials.value.find((item) => Number(item.sortNo) === Number(activeMaterialNo.value)) || groupMaterials.value[0]
  }
  return groupMaterials.value[0]
})

watch(currentQuestion, () => {
  if (!groupMaterials.value.length) {
    activeMaterialNo.value = null
    return
  }
  if (currentMaterialRefs.value.length > 0) {
    const preferred = groupMaterials.value.find((item) => currentMaterialRefs.value.includes(Number(item.sortNo)))
    if (preferred) {
      activeMaterialNo.value = Number(preferred.sortNo)
      return
    }
  }
  activeMaterialNo.value = Number(groupMaterials.value[0].sortNo)
}, { immediate: true })

function normalizeAnswerValue(value) {
  return normalizeQuestionAnswerValue(value)
}

function questionOrderLabel(question, index) {
  return question.groupQuestionNo || index + 1
}

function answerText(question) {
  const value = answers[question.id]
  if (question.subjectName === '申论') {
    const parsed = parseEssayAnswer(value)
    return parsed.text || parsed.selfRating ? '已答' : '未答'
  }
  return normalizeAnswerValue(value)
}

function isAnswered(question) {
  return Boolean(answerText(question))
}

function questionStatus(question) {
  if (answers[question.id] === 'UNKNOWN') return 'unknown'
  return isAnswered(question) ? 'answered' : 'empty'
}

function practiceContext() {
  try {
    return JSON.parse(sessionStorage.getItem(`civil-practice-context:${route.params.id}`) || '{}')
  } catch {
    return {}
  }
}

function parseEssayAnswer(value) {
  if (!value) return { text: '', images: [], selfRating: '' }
  try {
    return { text: '', images: [], selfRating: '', ...JSON.parse(value) }
  } catch {
    return { text: value, images: [], selfRating: '' }
  }
}

function updateEssayAnswer(patch) {
  const oldValue = parseEssayAnswer(answers[currentQuestion.value.id])
  const oldText = oldValue.text
  answers[currentQuestion.value.id] = JSON.stringify({
    text: oldValue.text,
    selfRating: oldValue.selfRating,
    ...patch
  })
  if (Object.prototype.hasOwnProperty.call(patch, 'text') && patch.text !== oldText) {
    clearEssayAiReview(currentQuestion.value.id)
  }
  lastSavedAt.value = new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  recordAnswerTime(currentQuestion.value.id)
}

function clearEssayAnswer() {
  updateEssayAnswer({ text: '' })
}

function clearEssayAiReview(questionId) {
  if (!questionId) return
  delete essayAiReviews[questionId]
  delete essayAiReviewErrors[questionId]
}

function formatAiScore(score) {
  if (score === null || score === undefined || score === '') return '暂无'
  return Number(score).toLocaleString('zh-CN', { maximumFractionDigits: 1 })
}

async function requestEssayAiReview() {
  const question = currentQuestion.value
  const answerText = currentEssayAnswer.value.text.trim()
  if (!answerText) {
    ElMessage.warning('请先输入答案')
    return
  }
  essayAiReviewLoading.value = true
  delete essayAiReviewErrors[question.id]
  try {
    const data = await request.post(
      `/api/practices/${route.params.id}/questions/${question.id}/essay-ai-review`,
      { answerText },
      { timeout: 130000 }
    )
    essayAiReviews[question.id] = data
    if (data.formatWarning) {
      essayAiReviewErrors[question.id] = data.comment || 'AI解析格式异常，但原文已返回。'
    }
  } catch (error) {
    essayAiReviewErrors[question.id] = normalizeEssayAiReviewError(error)
  } finally {
    essayAiReviewLoading.value = false
  }
}

function normalizeEssayAiReviewError(error) {
  const status = error?.response?.status
  const message = error?.response?.data?.message || error?.message || ''
  if (status === 404 || message.includes('status code 404')) {
    return 'AI批改接口未找到，请确认后端已重启并加载最新代码。'
  }
  return message || 'AI批改老师暂时不可用，请稍后重试'
}

function saveEssayDraft() {
  updateEssayAnswer({})
  ElMessage.success('草稿已保存')
}

async function loadData() {
  loading.value = true
  try {
    const data = await request.get(`/api/practices/${route.params.id}`)
    record.value = data.record
    questions.value = data.questions
    shenlunGroup.value = data.shenlunGroup || null
    timeLimitSeconds.value = data.timeLimitSeconds
    questions.value.forEach((item) => {
      markedMap[item.id] = Boolean(item.marked)
      answers[item.id] = item.questionType === 'MULTI'
        ? (item.userAnswer ? item.userAnswer.split(',').filter(Boolean) : [])
        : (item.userAnswer || '')
      spentSecondsMap[item.id] = item.spentSeconds || 0
    })
    if (record.value.status !== 'SUBMITTED') {
      startTimer()
    }
  } finally {
    loading.value = false
  }
}

function startTimer() {
  if (!canRunTimer()) return
  stopTimer()
  timer = window.setInterval(() => {
    durationSeconds.value += 1
    if (timeLimitSeconds.value && durationSeconds.value >= timeLimitSeconds.value) {
      stopTimer()
      submitPractice(true)
    }
  }, 1000)
}

function stopTimer() {
  if (!timer) return
  clearInterval(timer)
  timer = null
}

function canRunTimer() {
  return record.value.status !== 'SUBMITTED' && questions.value.length > 0
}

function resumeTimerIfNeeded() {
  if (!timer && canRunTimer()) {
    startTimer()
  }
}

function formatSeconds(value) {
  const total = Math.max(Number(value) || 0, 0)
  const hour = String(Math.floor(total / 3600)).padStart(2, '0')
  const minute = String(Math.floor((total % 3600) / 60)).padStart(2, '0')
  const second = String(total % 60).padStart(2, '0')
  return `${hour}:${minute}:${second}`
}

function recordAnswerTime(questionId) {
  if (!answeredAtMap[questionId]) {
    answeredAtMap[questionId] = durationSeconds.value
  }
  spentSecondsMap[questionId] = Math.max(1, durationSeconds.value - answeredAtMap[questionId] + 1)
}

function chooseUnknown() {
  answers[currentQuestion.value.id] = 'UNKNOWN'
  recordAnswerTime(currentQuestion.value.id)
}

function afterObjectiveChange() {
  recordAnswerTime(currentQuestion.value.id)
}

async function toggleFavorite() {
  const question = currentQuestion.value
  if (!question.id) return
  if (question.favorite) {
    await request.delete(`/api/question-favorites/${question.id}`)
    question.favorite = false
    ElMessage.success('已取消收藏')
    return
  }
  await request.post(`/api/question-favorites/${question.id}`)
  question.favorite = true
  ElMessage.success('已收藏到错题本')
}

function progressKey(subject, category) {
  return `civil-practice-progress:${subject || '未知'}:${category || '综合'}`
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

function updateProgressStorage() {
  const progressByCategory = new Map()
  questions.value.forEach((question) => {
    if (!isAnswered(question)) return
    const subjectName = question.subjectName || record.value.subjectName
    const categoryName = question.categoryName || record.value.categoryName || '综合'
    const key = progressKey(subjectName, categoryName)
    if (!progressByCategory.has(key)) {
      progressByCategory.set(key, {
        subjectName,
        categoryName,
        ids: readProgressIds(subjectName, categoryName)
      })
    }
    progressByCategory.get(key).ids.add(String(question.id))
  })
  progressByCategory.forEach(({ subjectName, categoryName, ids }) => {
    localStorage.setItem(progressIdsKey(subjectName, categoryName), JSON.stringify([...ids]))
    localStorage.setItem(progressKey(subjectName, categoryName), String(ids.size))
  })
}

async function confirmReturn() {
  try {
    await ElMessageBox.confirm('是否提交本次练习？', '返回', {
      confirmButtonText: '提交',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await submitPractice(true)
  } catch {
    // cancel only closes the dialog.
  }
}

async function submitPractice(forceSubmit = false) {
  if (submitting.value) return
  if (!forceSubmit) {
    try {
      await ElMessageBox.confirm('是否提交本次练习？', '提交练习', {
        confirmButtonText: '提交',
        cancelButtonText: '取消',
        type: 'warning'
      })
    } catch {
      return
    }
  }
  submitting.value = true
  try {
    const payload = {
      durationSeconds: durationSeconds.value,
      answers: questions.value.map((item) => ({
        questionId: item.id,
        userAnswer: item.subjectName === '申论' ? (answers[item.id] || '') : normalizeAnswerValue(answers[item.id]),
        marked: markedMap[item.id],
        spentSeconds: spentSecondsMap[item.id] || 0
      }))
    }
    const data = await request.post(`/api/practices/${route.params.id}/submit`, payload)
    updateProgressStorage()
    stopTimer()
    ElMessage.success('练习已提交')
    router.push(`/practice/report/${data.reportId}`)
  } finally {
    submitting.value = false
  }
}

function pausePractice() {
  stopTimer()
  ElMessage.info('已暂停，再次点击下一题或上一题会继续计时')
}

function moveQuestion(step) {
  currentIndex.value = Math.min(Math.max(currentIndex.value + step, 0), questions.value.length - 1)
  resumeTimerIfNeeded()
}

function jumpQuestion(index) {
  currentIndex.value = Math.min(Math.max(index, 0), questions.value.length - 1)
  resumeTimerIfNeeded()
}

function goBack() {
  const context = practiceContext()
  router.push(context.returnPath || '/question-bank')
}

onMounted(loadData)
onBeforeUnmount(stopTimer)
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section class="practice-topbar glass-card">
      <el-button plain @click="confirmReturn">返回</el-button>
      <div>
        <h1>{{ isPaperPractice ? record.title : (isShenlunGroupPractice ? shenlunGroup?.title || record.title : (currentQuestion.categoryName || record.title)) }}</h1>
        <p>
          {{ record.subjectName }}
          <template v-if="record.province"> · {{ record.province }}</template>
          · {{ formatSeconds(timeLimitSeconds ? Math.max(timeLimitSeconds - durationSeconds, 0) : durationSeconds) }}
        </p>
      </div>
      <el-button type="warning" :loading="submitting" @click="submitPractice">{{ isPaperPractice ? '交卷' : '提交' }}</el-button>
    </section>

    <section class="practice-layout">
      <aside class="glass-card nav-card">
        <div class="nav-card-head">
          <div class="section-title small">答题卡</div>
          <span class="progress-ratio">{{ answeredCount }}/{{ questions.length }}</span>
        </div>
        <div class="progress">当前 {{ currentIndex + 1 }} / {{ questions.length }} 题</div>
        <div class="question-nav-group" v-for="group in groupedQuestions" :key="group.name">
          <strong>{{ group.name }}</strong>
          <div class="question-nav">
            <button
              v-for="{ question: item, index } in group.items"
              :key="item.id"
              class="nav-btn"
              :class="{ active: currentIndex === index, answered: isAnswered(item), unknown: questionStatus(item) === 'unknown', marked: markedMap[item.id] }"
              @click="jumpQuestion(index)"
            >
              <span>{{ questionOrderLabel(item, index) }}</span>
              <i v-if="item.favorite" class="favorite-dot">★</i>
            </button>
          </div>
        </div>
        <div class="legend compact-legend">
          <span><i class="answered"></i>已答</span>
          <span><i class="empty"></i>未答</span>
          <span><i class="marked"></i>待复查</span>
        </div>
      </aside>

      <section class="glass-card content-card">
        <div class="question-head">
          <div>
            <div class="section-title small">{{ isShenlunGroupPractice ? (currentQuestion.categoryName || '申论题目') : currentQuestion.categoryName }}</div>
            <p class="meta">当前题序：{{ currentIndex + 1 }} / {{ questions.length }} 题</p>
          </div>
          <el-button class="favorite-button" :type="currentQuestion.favorite ? 'warning' : 'default'" @click="toggleFavorite">
            {{ currentQuestion.favorite ? '已收藏' : '收藏' }}
          </el-button>
        </div>

        <template v-if="isEssayPractice">
          <div class="material-panel">
            <div class="material-panel-head">
              <div>
                <strong>给定资料</strong>
                <p class="meta material-meta">
                  {{ shenlunGroup?.materialCount || groupMaterials.length }} 份材料
                  <template v-if="currentMaterialRefs.length"> · 当前题参考资料 {{ currentMaterialRefs.join('、') }}</template>
                </p>
              </div>
              <span v-if="currentQuestionScore" class="score-pill">{{ currentQuestionScore }} 分</span>
            </div>
            <div v-if="groupMaterials.length" class="material-tabs">
              <button
                v-for="material in groupMaterials"
                :key="material.id"
                class="material-tab"
                :class="{ active: Number(material.sortNo) === Number(activeMaterialNo), linked: currentMaterialRefs.includes(Number(material.sortNo)) }"
                @click="activeMaterialNo = Number(material.sortNo)"
              >
                {{ material.label || `给定资料${material.sortNo}` }}
              </button>
            </div>
            <article v-if="currentMaterial" class="material-box">
              <div class="material-title-row">
                <strong>{{ currentMaterial.label || `给定资料${currentMaterial.sortNo}` }}</strong>
                <span v-if="currentMaterial.title">{{ currentMaterial.title }}</span>
              </div>
              <p>{{ currentMaterial.content || '暂无材料' }}</p>
            </article>
          </div>

          <p class="meta">题型：简答题</p>
          <div class="essay-stem-head">
            <h2>{{ currentQuestion.stem }}</h2>
            <span v-if="currentQuestionScore">{{ currentQuestionScore }}分</span>
          </div>
          <div class="essay-answer">
            <div class="essay-input-wrap" :class="essayWordStatus">
              <el-input
                :model-value="currentEssayAnswer.text"
                type="textarea"
                :rows="10"
                :maxlength="essayWordLimit"
                resize="vertical"
                placeholder="在线输入作答内容"
                @update:model-value="(value) => updateEssayAnswer({ text: value })"
              />
              <span class="word-count">{{ essayTextLength }} / {{ essayWordLimit }}</span>
            </div>
            <div class="ai-review-actions">
              <div>
                <strong>申论AI批改老师</strong>
                <span>根据当前答案生成评分、问题诊断和参考答案</span>
              </div>
              <el-button
                type="primary"
                plain
                :loading="essayAiReviewLoading"
                :disabled="!canRequestEssayAiReview"
                :title="currentEssayAnswer.text.trim() ? '' : '请先输入答案'"
                @click="requestEssayAiReview"
              >
                {{ essayAiReviewLoading ? 'AI批改中' : (currentEssayAiReview ? '重新批改' : '查看AI解析') }}
              </el-button>
            </div>
            <div v-if="currentEssayAiReviewError && !currentEssayAiReview" class="ai-review-error">
              {{ currentEssayAiReviewError }}
            </div>
            <section v-if="currentEssayAiReview" class="ai-review-card">
              <div class="ai-review-head">
                <div>
                  <span>AI评分</span>
                  <strong>{{ formatAiScore(currentEssayAiReview.score) }} / {{ currentEssayAiReview.maxScore || currentQuestionScore || 25 }}</strong>
                </div>
                <em v-if="currentEssayAiReview.level">{{ currentEssayAiReview.level }}</em>
              </div>
              <p v-if="currentEssayAiReviewError" class="ai-review-warning">{{ currentEssayAiReviewError }}</p>
              <p v-if="currentEssayAiReview.comment" class="ai-review-comment">{{ currentEssayAiReview.comment }}</p>
              <p v-else-if="currentEssayAiReview.rawText" class="ai-review-comment">{{ currentEssayAiReview.rawText }}</p>
              <div class="ai-score-breakdown">
                <span v-if="currentEssayAiReview.contentScore !== null && currentEssayAiReview.contentScore !== undefined">内容要点 {{ formatAiScore(currentEssayAiReview.contentScore) }}</span>
                <span v-if="currentEssayAiReview.structureScore !== null && currentEssayAiReview.structureScore !== undefined">结构层次 {{ formatAiScore(currentEssayAiReview.structureScore) }}</span>
                <span v-if="currentEssayAiReview.logicScore !== null && currentEssayAiReview.logicScore !== undefined">逻辑论证 {{ formatAiScore(currentEssayAiReview.logicScore) }}</span>
                <span v-if="currentEssayAiReview.languageScore !== null && currentEssayAiReview.languageScore !== undefined">语言表达 {{ formatAiScore(currentEssayAiReview.languageScore) }}</span>
                <span v-if="currentEssayAiReview.formatScore !== null && currentEssayAiReview.formatScore !== undefined">格式规范 {{ formatAiScore(currentEssayAiReview.formatScore) }}</span>
              </div>
              <div class="ai-review-grid">
                <div>
                  <h3>优点</h3>
                  <ul v-if="currentEssayAiReview.strengths?.length">
                    <li v-for="item in currentEssayAiReview.strengths" :key="item">{{ item }}</li>
                  </ul>
                  <p v-else>暂无明确优点，可结合参考答案补充要点。</p>
                </div>
                <div>
                  <h3>主要问题</h3>
                  <ul v-if="currentEssayAiReview.problems?.length">
                    <li v-for="item in currentEssayAiReview.problems" :key="item">{{ item }}</li>
                  </ul>
                  <p v-else>暂无明显问题。</p>
                </div>
                <div>
                  <h3>改进建议</h3>
                  <ul v-if="currentEssayAiReview.improvements?.length">
                    <li v-for="item in currentEssayAiReview.improvements" :key="item">{{ item }}</li>
                  </ul>
                  <p v-else>继续保持结构化表达，注意紧扣材料。</p>
                </div>
              </div>
              <div v-if="currentEssayAiReview.referenceAnswer" class="ai-reference-answer">
                <h3>优化参考答案</h3>
                <p>{{ currentEssayAiReview.referenceAnswer }}</p>
              </div>
            </section>
          </div>
        </template>

        <template v-else>
          <p class="meta">题型：{{ currentQuestion.questionType === 'MULTI' ? '多选题' : '单选题' }}</p>
          <h2>题干：{{ currentQuestion.stem }}</h2>
          <div class="option-list">
            <el-radio-group v-if="currentQuestion.questionType !== 'MULTI'" v-model="answers[currentQuestion.id]" class="group-full" @change="afterObjectiveChange">
              <el-radio v-for="option in currentQuestion.options" :key="option.id" :value="option.optionKey" border>
                {{ option.optionKey }}. {{ option.optionContent }}
              </el-radio>
            </el-radio-group>
            <el-checkbox-group v-else v-model="answers[currentQuestion.id]" class="group-full" @change="afterObjectiveChange">
              <el-checkbox v-for="option in currentQuestion.options" :key="option.id" :value="option.optionKey" border>
                {{ option.optionKey }}. {{ option.optionContent }}
              </el-checkbox>
            </el-checkbox-group>
          </div>
          <div class="objective-actions">
            <el-button plain @click="chooseUnknown">不会</el-button>
            <el-button plain @click="analysisVisible = !analysisVisible">{{ analysisVisible ? '收起解析' : '查看解析' }}</el-button>
          </div>
          <div v-if="isAnswered(currentQuestion)" class="answer-feedback">
            <span>正确答案：{{ formatAnswerDisplay(currentQuestion.correctAnswer) }}</span>
            <span>你的答案：{{ formatAnswerDisplay(answerText(currentQuestion)) }}</span>
            <span>答题时长：{{ spentSecondsMap[currentQuestion.id] || 1 }}s</span>
          </div>
        </template>

        <div v-if="analysisVisible" class="analysis-box">
          <strong>正确答案：{{ isEssayPractice ? (currentQuestion.correctAnswer || '参考答案见解析') : formatAnswerDisplay(currentQuestion.correctAnswer) }}</strong>
          <p>{{ currentQuestion.analysis || '暂无解析' }}</p>
          <p>知识点：{{ currentQuestion.knowledgePoints || '未配置知识点' }}</p>
        </div>

        <div class="sticky-action-bar">
          <el-button @click="moveQuestion(-1)" :disabled="currentIndex === 0">上一题</el-button>
          <div class="action-status">
            <template v-if="isEssayPractice">
              <span>{{ lastSavedText }}</span>
              <span :class="['inline-count', essayWordStatus]">{{ essayTextLength }} / {{ essayWordLimit }}</span>
              <div class="rating-segment">
                <span>自评</span>
                <button
                  :class="{ active: currentEssayAnswer.selfRating === '掌握', success: true }"
                  @click="updateEssayAnswer({ selfRating: '掌握' })"
                >掌握</button>
                <button
                  :class="{ active: currentEssayAnswer.selfRating === '模糊', warning: true }"
                  @click="updateEssayAnswer({ selfRating: '模糊' })"
                >模糊</button>
                <button
                  :class="{ active: currentEssayAnswer.selfRating === '不会', danger: true }"
                  @click="updateEssayAnswer({ selfRating: '不会' })"
                >不会</button>
              </div>
            </template>
            <template v-else>
              <span>已答 {{ answeredCount }}/{{ questions.length }}</span>
              <span>用时 {{ formatSeconds(durationSeconds) }}</span>
            </template>
          </div>
          <div class="action-buttons">
            <el-button v-if="isEssayPractice" plain @click="clearEssayAnswer" :disabled="!currentEssayAnswer.text">清空</el-button>
            <el-button v-if="isEssayPractice" plain @click="saveEssayDraft">保存草稿</el-button>
            <el-button @click="pausePractice">暂停</el-button>
            <el-button @click="answerCardVisible = true">答题卡</el-button>
            <el-button
              v-if="currentIndex < questions.length - 1"
              type="primary"
              @click="moveQuestion(1)"
            >下一题</el-button>
            <el-button
              v-else
              type="primary"
              :loading="submitting"
              @click="submitPractice"
            >完成练习</el-button>
          </div>
        </div>
      </section>
    </section>

    <el-drawer v-model="answerCardVisible" title="答题卡" size="360px">
      <div v-for="group in groupedQuestions" :key="`drawer-${group.name}`" class="question-nav-group">
        <strong>{{ group.name }}</strong>
        <div class="drawer-grid">
          <button
            v-for="{ question: item, index } in group.items"
            :key="item.id"
            class="nav-btn"
            :class="{ active: currentIndex === index, answered: isAnswered(item), unknown: questionStatus(item) === 'unknown' }"
            @click="jumpQuestion(index); answerCardVisible = false"
          >
            {{ questionOrderLabel(item, index) }}
          </button>
        </div>
      </div>
      <div class="legend">
        <span><i class="ok"></i>已答</span>
        <span><i class="empty"></i>未答</span>
        <span><i class="unknown"></i>不会</span>
      </div>
      <div class="drawer-footer">
        <el-button plain @click="goBack">返回题库</el-button>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.practice-topbar,
.practice-layout {
  display: grid;
  gap: 20px;
}

.practice-topbar {
  grid-template-columns: auto 1fr auto;
  align-items: center;
  padding: 18px 22px;
  margin-bottom: 20px;
  border-radius: 12px;
}

.practice-topbar h1,
.practice-topbar p {
  margin: 0;
}

.practice-topbar p,
.progress,
.meta {
  color: var(--muted);
}

.practice-layout {
  grid-template-columns: 270px minmax(0, 1fr);
  align-items: start;
}

.nav-card,
.content-card {
  padding: 22px;
  border-radius: 12px;
}

.nav-card {
  position: sticky;
  top: 18px;
}

.content-card {
  padding-bottom: 110px;
}

.nav-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-title.small {
  font-size: 21px;
  margin-bottom: 10px;
}

.progress-ratio {
  color: #047857;
  background: #ecfdf5;
  border: 1px solid #bbf7d0;
  border-radius: 999px;
  font-weight: 700;
  padding: 4px 10px;
}

.question-nav,
.drawer-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-top: 16px;
}

.question-nav-group {
  display: grid;
  gap: 8px;
  margin-top: 16px;
}

.question-nav-group strong {
  color: #334155;
  font-size: 14px;
}

.nav-btn {
  position: relative;
  height: 42px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  color: #0f172a;
  font-weight: 700;
  transition: border-color 0.2s ease, background 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.nav-btn.active {
  background: var(--primary);
  border-color: var(--primary);
  color: #fff;
}

.nav-btn.answered {
  border-color: #10b981;
  background: #ecfdf5;
  color: #047857;
}

.nav-btn.answered.active {
  background: var(--primary);
  border-color: var(--primary);
  color: #fff;
}

.nav-btn.unknown {
  border-color: #f97316;
  background: #fff7ed;
}

.nav-btn.marked::after {
  content: "";
  position: absolute;
  right: -1px;
  top: -1px;
  width: 0;
  height: 0;
  border-top: 12px solid #f97316;
  border-left: 12px solid transparent;
}

.favorite-dot {
  position: absolute;
  right: 4px;
  top: 2px;
  color: #f59e0b;
  font-size: 11px;
  line-height: 1;
}

.question-head,
.answer-feedback,
.legend,
.objective-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.question-head {
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid #e2e8f0;
  margin-bottom: 16px;
}

.favorite-button {
  min-width: 88px;
}

.content-card h2 {
  font-size: 20px;
  line-height: 1.75;
  margin: 18px 0;
  color: #0f172a;
  white-space: pre-wrap;
}

.material-panel,
.material-box,
.answer-feedback,
.analysis-box {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 16px 18px;
  background: #f8fbff;
  margin-bottom: 16px;
}

.material-panel {
  display: grid;
  gap: 14px;
}

.material-panel-head,
.material-title-row,
.essay-stem-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.material-meta {
  margin: 6px 0 0;
}

.material-tabs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.material-tab {
  min-height: 36px;
  padding: 0 14px;
  border: 1px solid #cbd5e1;
  border-radius: 999px;
  background: #fff;
  color: #334155;
  font-weight: 700;
  cursor: pointer;
}

.material-tab.active {
  background: var(--primary);
  border-color: var(--primary);
  color: #fff;
}

.material-tab.linked {
  box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.12);
}

.material-box p {
  margin: 0;
  color: #0f172a;
  line-height: 1.85;
  white-space: pre-wrap;
}

.material-title-row strong {
  color: #475569;
  font-size: 16px;
}

.material-title-row span,
.essay-stem-head span,
.score-pill {
  flex: 0 0 auto;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 800;
}

.material-title-row span,
.score-pill {
  padding: 4px 10px;
  font-size: 13px;
}

.essay-stem-head h2 {
  flex: 1;
}

.essay-stem-head span {
  margin-top: 18px;
  padding: 6px 12px;
  font-size: 15px;
}

.option-list,
.essay-answer {
  margin-top: 20px;
  display: grid;
  gap: 14px;
}

.essay-input-wrap {
  position: relative;
}

.essay-input-wrap :deep(.el-textarea__inner) {
  min-height: 240px !important;
  padding: 14px 16px 36px;
  border-radius: 8px;
  font-size: 16px;
  line-height: 1.8;
  box-shadow: 0 0 0 1px #dbe3f0 inset;
}

.essay-input-wrap :deep(.el-textarea__inner:focus) {
  box-shadow: 0 0 0 1px var(--primary) inset, 0 0 0 3px rgba(29, 78, 216, 0.12);
}

.word-count {
  position: absolute;
  right: 14px;
  bottom: 10px;
  color: #64748b;
  font-size: 14px;
}

.ai-review-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: #f8fbff;
}

.ai-review-actions div {
  display: grid;
  gap: 4px;
}

.ai-review-actions strong {
  color: #0f172a;
}

.ai-review-actions span {
  color: #64748b;
  font-size: 14px;
}

.ai-review-error,
.ai-review-warning {
  padding: 10px 12px;
  border-radius: 8px;
  background: #fff7ed;
  color: #c2410c;
  font-size: 14px;
}

.ai-review-card {
  display: grid;
  gap: 16px;
  padding: 18px;
  border: 1px solid #bfdbfe;
  border-radius: 10px;
  background: #f8fbff;
}

.ai-review-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid #dbeafe;
}

.ai-review-head div {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.ai-review-head span {
  color: #475569;
}

.ai-review-head strong {
  color: var(--primary);
  font-size: 26px;
}

.ai-review-head em {
  min-width: 58px;
  padding: 5px 10px;
  border-radius: 999px;
  background: #ecfdf5;
  color: #047857;
  font-style: normal;
  font-weight: 700;
  text-align: center;
}

.ai-review-comment {
  margin: 0;
  color: #0f172a;
  line-height: 1.75;
}

.ai-score-breakdown {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.ai-score-breakdown span {
  padding: 6px 10px;
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: #fff;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
}

.ai-review-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.ai-review-grid > div,
.ai-reference-answer {
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #fff;
}

.ai-review-card h3 {
  margin: 0 0 10px;
  color: #0f172a;
  font-size: 15px;
}

.ai-review-card ul {
  display: grid;
  gap: 8px;
  padding-left: 18px;
  margin: 0;
  color: #334155;
  line-height: 1.65;
}

.ai-review-card p {
  margin: 0;
  color: #334155;
  line-height: 1.75;
}

.essay-input-wrap.warning .word-count,
.inline-count.warning {
  color: #f97316;
}

.essay-input-wrap.danger .word-count,
.inline-count.danger {
  color: #dc2626;
}

.group-full {
  width: 100%;
  display: grid;
  gap: 12px;
}

.objective-actions {
  margin-top: 14px;
}

.answer-feedback {
  margin-top: 18px;
  background: #f0fdf4;
}

.analysis-box {
  margin-top: 18px;
  background: #fff7ed;
}

.legend {
  margin-top: 20px;
}

.legend span {
  color: var(--muted);
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

.legend .ok,
.legend .answered {
  background: #dcfce7;
  border-color: #10b981;
}

.legend .unknown,
.legend .marked {
  background: #ffedd5;
  border-color: #f97316;
}

.compact-legend {
  gap: 10px;
  font-size: 13px;
}

.sticky-action-bar {
  position: sticky;
  bottom: 0;
  z-index: 5;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  margin-top: 26px;
  padding: 14px 0;
  background: #fff;
  border-top: 1px solid #e2e8f0;
}

.action-status,
.action-buttons,
.rating-segment,
.drawer-footer {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.action-status {
  justify-content: center;
  color: #64748b;
  font-size: 14px;
}

.inline-count {
  font-weight: 700;
}

.rating-segment {
  padding: 4px;
  border: 1px solid #dbe3f0;
  border-radius: 8px;
  background: #f8fafc;
}

.rating-segment span {
  padding: 0 4px 0 6px;
  color: #475569;
}

.rating-segment button {
  height: 30px;
  min-width: 48px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #334155;
  font-weight: 700;
  cursor: pointer;
}

.rating-segment button.success.active {
  color: #fff;
  background: #10b981;
}

.rating-segment button.warning.active {
  color: #fff;
  background: #f97316;
}

.rating-segment button.danger.active {
  color: #fff;
  background: #ef4444;
}

.drawer-footer {
  margin-top: 18px;
}

@media (max-width: 960px) {
  .practice-topbar,
  .practice-layout {
    grid-template-columns: 1fr;
  }

  .nav-card {
    position: static;
  }

  .sticky-action-bar {
    position: static;
    grid-template-columns: 1fr;
  }

  .action-status {
    justify-content: flex-start;
  }

  .ai-review-actions,
  .ai-review-head,
  .material-panel-head,
  .material-title-row,
  .essay-stem-head {
    align-items: stretch;
    flex-direction: column;
  }

  .ai-review-grid {
    grid-template-columns: 1fr;
  }
}
</style>
