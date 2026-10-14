<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import request from '../utils/request'
import { normalizeAnswerValue as normalizeQuestionAnswerValue } from '../utils/question'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const exam = ref({})
const record = ref({})
const questions = ref([])
const currentIndex = ref(0)
const remainingSeconds = ref(0)
const answers = reactive({})
let timer

const currentQuestion = computed(() => questions.value[currentIndex.value] || {})

function formatSeconds(value) {
  const total = Math.max(value, 0)
  const hour = String(Math.floor(total / 3600)).padStart(2, '0')
  const minute = String(Math.floor((total % 3600) / 60)).padStart(2, '0')
  const second = String(total % 60).padStart(2, '0')
  return `${hour}:${minute}:${second}`
}

function normalizeAnswerValue(value) {
  return normalizeQuestionAnswerValue(value)
}

async function loadData() {
  loading.value = true
  try {
    const data = await request.get(`/api/exam-records/${route.params.id}`)
    exam.value = data.exam
    record.value = data.record
    questions.value = data.questions
    questions.value.forEach((item) => {
      answers[item.id] = item.questionType === 'MULTI'
        ? (item.userAnswer ? item.userAnswer.split(',').filter(Boolean) : [])
        : (item.userAnswer || '')
    })
    const startedAt = new Date(record.value.startedAt).getTime()
    const elapsed = Math.max(0, Math.floor((Date.now() - startedAt) / 1000))
    remainingSeconds.value = Math.max(0, (exam.value.durationMinutes || 0) * 60 - elapsed)
    startTimer()
  } finally {
    loading.value = false
  }
}

function startTimer() {
  clearInterval(timer)
  timer = window.setInterval(() => {
    remainingSeconds.value -= 1
    if (remainingSeconds.value <= 0) {
      clearInterval(timer)
      submitExam(true)
    }
  }, 1000)
}

async function submitExam(auto = false) {
  if (submitting.value) return
  if (!auto) {
    try {
      await ElMessageBox.confirm('确认交卷吗？交卷后将立即生成成绩报告。', '提交模考', {
        type: 'warning'
      })
    } catch {
      return
    }
  }
  submitting.value = true
  try {
    const durationSeconds = (exam.value.durationMinutes || 0) * 60 - remainingSeconds.value
    const data = await request.post(`/api/exam-records/${route.params.id}/submit`, {
      durationSeconds,
      answers: questions.value.map((item) => ({
        questionId: item.id,
        userAnswer: normalizeAnswerValue(answers[item.id])
      }))
    })
    router.push(`/exam/report/${data.reportId}`)
  } finally {
    submitting.value = false
  }
}

onMounted(loadData)
onBeforeUnmount(() => clearInterval(timer))
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section class="practice-layout">
      <aside class="glass-card nav-card">
        <div class="section-title small">模考答题卡</div>
        <div class="timer">{{ formatSeconds(remainingSeconds) }}</div>
        <div class="progress">{{ exam.title }}</div>
        <div class="nav-legend">
          <span class="legend-item">
            <span class="legend-dot current"></span>
            当前题
          </span>
          <span class="legend-item">
            <span class="legend-dot answered"></span>
            已答
          </span>
          <span class="legend-item">
            <span class="legend-dot pending"></span>
            未答
          </span>
        </div>
        <div class="question-nav">
          <button
            v-for="(item, index) in questions"
            :key="item.id"
            class="nav-btn"
            :class="{ active: currentIndex === index, answered: normalizeAnswerValue(answers[item.id]) }"
            @click="currentIndex = index"
          >
            {{ index + 1 }}
          </button>
        </div>
        <el-button type="warning" style="margin-top: 18px; width: 100%" :loading="submitting" @click="submitExam()">交卷</el-button>
      </aside>

      <section class="glass-card content-card">
        <div class="question-head">
          <div class="section-title small">第 {{ currentIndex + 1 }} 题</div>
          <div class="score-tag">本题 {{ currentQuestion.score }} 分</div>
        </div>
        <p v-if="currentQuestion.material" class="meta">{{ currentQuestion.material }}</p>
        <h2>{{ currentQuestion.stem }}</h2>
        <p class="meta">{{ currentQuestion.subjectName }} · {{ currentQuestion.categoryName }} · {{ currentQuestion.difficultyLevel }}</p>
        <el-alert
          v-if="currentQuestion.questionType === 'MULTI'"
          title="本题为多选题，必须完整选中全部正确项。"
          type="info"
          :closable="false"
          style="margin-bottom: 16px"
        />

        <div class="option-list">
          <el-radio-group
            v-if="currentQuestion.questionType !== 'MULTI'"
            v-model="answers[currentQuestion.id]"
            class="group-full"
          >
            <el-radio
              v-for="option in currentQuestion.options"
              :key="option.id"
              :value="option.optionKey"
              border
            >
              {{ option.optionKey }}. {{ option.optionContent }}
            </el-radio>
          </el-radio-group>

          <el-checkbox-group v-else v-model="answers[currentQuestion.id]" class="group-full">
            <el-checkbox
              v-for="option in currentQuestion.options"
              :key="option.id"
              :value="option.optionKey"
              border
            >
              {{ option.optionKey }}. {{ option.optionContent }}
            </el-checkbox>
          </el-checkbox-group>
        </div>

        <div class="footer-actions">
          <el-button :disabled="currentIndex === 0" @click="currentIndex -= 1">上一题</el-button>
          <el-button type="primary" :disabled="currentIndex === questions.length - 1" @click="currentIndex += 1">下一题</el-button>
        </div>
      </section>
    </section>
  </div>
</template>

<style scoped>
.practice-layout {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 20px;
}

.nav-card,
.content-card {
  padding: 24px;
}

.section-title.small {
  font-size: 22px;
}

.timer {
  font-size: 30px;
  font-weight: 700;
  color: #dc2626;
}

.progress,
.meta {
  color: var(--muted);
}

.nav-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  margin-top: 14px;
  color: var(--muted);
  font-size: 13px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: #fff;
}

.legend-dot.current {
  background: var(--primary);
  border-color: var(--primary);
}

.legend-dot.answered {
  background: #10b981;
  border-color: #10b981;
}

.question-nav {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
  margin-top: 16px;
}

.nav-btn {
  position: relative;
  height: 40px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  font-weight: 600;
  transition: background-color 0.2s ease, border-color 0.2s ease, color 0.2s ease, box-shadow 0.2s ease;
}

.nav-btn.active {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.22);
}

.nav-btn.answered {
  border-color: #10b981;
  background: rgba(16, 185, 129, 0.14);
  color: #047857;
  box-shadow: inset 0 0 0 1px rgba(16, 185, 129, 0.12);
}

.nav-btn.active.answered {
  color: #fff;
  background: var(--primary);
  border-color: var(--primary);
  box-shadow:
    0 10px 24px rgba(37, 99, 235, 0.22),
    inset 0 0 0 2px rgba(255, 255, 255, 0.22);
}

.question-head,
.footer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.score-tag {
  color: var(--accent);
  font-weight: 700;
}

.option-list {
  margin-top: 24px;
}

.group-full {
  width: 100%;
  display: grid;
  gap: 12px;
}

.group-full :deep(.el-radio),
.group-full :deep(.el-checkbox) {
  width: 100%;
  height: auto;
  min-height: 40px;
  margin-right: 0;
  padding: 10px 14px;
  display: flex;
  align-items: flex-start;
  line-height: 1.5;
  white-space: normal;
}

.group-full :deep(.el-radio__input),
.group-full :deep(.el-checkbox__input) {
  flex: 0 0 auto;
  margin-top: 4px;
}

.group-full :deep(.el-radio__label),
.group-full :deep(.el-checkbox__label) {
  flex: 1;
  min-width: 0;
  padding-left: 10px;
  line-height: 1.5;
  white-space: normal;
  word-break: break-word;
}

.footer-actions {
  margin-top: 28px;
}

@media (max-width: 960px) {
  .practice-layout {
    grid-template-columns: 1fr;
  }
}
</style>
