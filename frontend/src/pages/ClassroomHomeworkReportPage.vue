<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'
import { formatAnswerDisplay, getQuestionTypeLabel } from '../utils/question'

const route = useRoute()
const loading = ref(false)
const report = ref({ homework: {}, submission: {}, questions: [] })

async function loadData() {
  loading.value = true
  try {
    report.value = await request.get(`/api/classroom-tasks/homework-submissions/${route.params.id}/report`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section class="glass-card report-card">
      <div class="section-title">{{ report.homework.title }}</div>
      <div class="score-row">
        <div><span>得分</span><strong>{{ report.submission.score }}</strong></div>
        <div><span>正确题数</span><strong>{{ report.submission.correctCount }}</strong></div>
        <div><span>批改状态</span><strong>{{ report.submission.reviewStatus }}</strong></div>
      </div>
      <p v-if="report.submission.reviewRemark" class="review-remark">教师评语：{{ report.submission.reviewRemark }}</p>
    </section>

    <section class="glass-card report-card section-gap">
      <div class="question-list">
        <article v-for="(item, index) in report.questions" :key="item.id" class="question-item">
          <div class="question-title">{{ index + 1 }}. {{ item.stem }}</div>
          <div class="muted">题型：{{ getQuestionTypeLabel(item.questionType) }} ｜ 本题得分：{{ item.answerScore ?? 0 }}/{{ item.score }}</div>
          <div class="muted">你的答案：{{ formatAnswerDisplay(item.userAnswer) }} ｜ 正确答案：{{ formatAnswerDisplay(item.correctAnswer) }}</div>
          <div class="muted">解析：{{ item.analysis }}</div>
          <div v-if="item.teacherComment" class="annotation-box">教师批注：{{ item.teacherComment }}</div>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.report-card {
  padding: 24px;
}

.score-row {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.score-row span,
.muted {
  color: var(--muted);
}

.score-row strong {
  display: block;
  margin-top: 8px;
  font-size: 26px;
  color: var(--primary);
}

.question-list {
  display: grid;
  gap: 14px;
}

.question-item {
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 16px;
  background: #fff;
}

.question-title {
  font-weight: 700;
  margin-bottom: 8px;
}

.review-remark,
.annotation-box {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #fff7ed;
  color: #9a3412;
}
</style>
