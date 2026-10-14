<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref({ classroomExam: {}, submission: {}, questions: [] })
const durationSeconds = ref(0)
let timer = null

async function loadData() {
  loading.value = true
  try {
    detail.value = await request.get(`/api/classroom-tasks/classroom-exam-submissions/${route.params.id}`)
    detail.value.questions = (detail.value.questions || []).map((item) => ({
      ...item,
      userAnswer: item.questionType === 'MULTI'
        ? (item.userAnswer ? item.userAnswer.split(',').filter(Boolean) : [])
        : (item.userAnswer || '')
    }))
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function submit() {
  try {
    await request.post(`/api/classroom-tasks/classroom-exam-submissions/${route.params.id}/submit`, {
      durationSeconds: durationSeconds.value,
      answers: detail.value.questions.map((item) => ({
        questionId: item.id,
        userAnswer: Array.isArray(item.userAnswer) ? item.userAnswer.join(',') : (item.userAnswer || '')
      }))
    })
    ElMessage.success('考试提交成功')
    router.push(`/classroom/exam/report/${route.params.id}`)
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(() => {
  loadData()
  timer = window.setInterval(() => {
    durationSeconds.value += 1
  }, 1000)
})

onUnmounted(() => {
  if (timer) {
    window.clearInterval(timer)
  }
})
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section class="glass-card answer-card">
      <div class="section-title">{{ detail.classroomExam.title }}</div>
      <p class="muted">{{ detail.classroomExam.description }}</p>
      <el-alert
        v-if="detail.classroomExam.antiCheatEnabled"
        :title="detail.classroomExam.antiCheatNotice || '考试过程中请勿切屏或离开页面。'"
        type="warning"
        :closable="false"
        style="margin: 12px 0"
      />
      <div class="muted">已作答时长：{{ durationSeconds }} 秒</div>
    </section>

    <section class="glass-card answer-card section-gap">
      <div class="question-list">
        <article v-for="(item, index) in detail.questions" :key="item.id" class="question-item">
          <div class="question-title">{{ index + 1 }}. {{ item.stem }}</div>
          <el-radio-group v-if="item.questionType !== 'MULTI' && !item.subjective" v-model="item.userAnswer" class="group-full">
            <el-radio v-for="option in item.options" :key="option.id" :value="option.optionKey">
              {{ option.optionKey }}. {{ option.optionContent }}
            </el-radio>
          </el-radio-group>
          <el-checkbox-group v-else-if="item.questionType === 'MULTI'" v-model="item.userAnswer" class="group-full">
            <el-checkbox v-for="option in item.options" :key="option.id" :value="option.optionKey">
              {{ option.optionKey }}. {{ option.optionContent }}
            </el-checkbox>
          </el-checkbox-group>
          <el-input
            v-else
            v-model="item.userAnswer"
            type="textarea"
            :rows="5"
            placeholder="请输入主观题答案"
          />
        </article>
      </div>
      <el-button type="warning" size="large" @click="submit">提交考试</el-button>
    </section>
  </div>
</template>

<style scoped>
.answer-card {
  padding: 24px;
}

.muted {
  color: var(--muted);
}

.question-list {
  display: grid;
  gap: 18px;
  margin-bottom: 20px;
}

.question-item {
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 16px;
  background: #fff;
}

.question-title {
  margin-bottom: 12px;
  font-weight: 700;
}

.group-full {
  width: 100%;
  display: grid;
  gap: 10px;
}

.group-full :deep(.el-radio),
.group-full :deep(.el-checkbox) {
  width: 100%;
  height: auto;
  min-height: 36px;
  margin-right: 0;
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
</style>
