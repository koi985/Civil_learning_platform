<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import {
  Check,
  Collection,
  DataAnalysis,
  DocumentChecked,
  EditPen,
  Finished,
  Refresh,
  Search,
  Tickets,
  User
} from '@element-plus/icons-vue'
import request from '../utils/request'

const router = useRouter()
const loading = ref(false)
const reviewLoading = ref(false)
const list = ref([])
const total = ref(0)
const reviewDialog = ref(false)
const quickView = ref('ALL')
const reviewData = ref({ submission: {}, questions: [], homework: {}, classroomExam: {} })
const form = reactive({ taskType: 'HOMEWORK', submissionId: null, score: 0, reviewStatus: 'REVIEWED', reviewRemark: '', annotations: [] })
const query = reactive({ pageNo: 1, pageSize: 10, taskType: '', reviewStatus: '', keyword: '' })

const totalAnnotatedScore = computed(() => form.annotations.reduce((sum, item) => sum + Number(item.score || 0), 0))
const pendingCount = computed(() => list.value.filter((item) => isPendingReview(item)).length)
const reviewedCount = computed(() => list.value.filter((item) => item.reviewStatus === 'REVIEWED').length)
const autoGradedCount = computed(() => list.value.filter((item) => item.reviewStatus === 'AUTO_GRADED').length)
const averageScore = computed(() => {
  if (!list.value.length) {
    return 0
  }
  const totalScore = list.value.reduce((sum, item) => sum + Number(item.score || 0), 0)
  return Math.round((totalScore / list.value.length) * 10) / 10
})
const filteredList = computed(() => {
  const keyword = query.keyword.trim().toLowerCase()
  return list.value.filter((item) => {
    const quickMatched =
      quickView.value === 'ALL' ||
      (quickView.value === 'PENDING' && isPendingReview(item)) ||
      (quickView.value === 'REVIEWED' && item.reviewStatus === 'REVIEWED') ||
      (quickView.value === 'AUTO_GRADED' && item.reviewStatus === 'AUTO_GRADED') ||
      (quickView.value === 'EXAM' && item.taskType === 'EXAM') ||
      (quickView.value === 'HOMEWORK' && item.taskType === 'HOMEWORK')
    if (!quickMatched) {
      return false
    }
    if (!keyword) {
      return true
    }
    return [item.classroomName, item.studentName, item.taskTitle, item.taskType, item.reviewStatus]
      .filter(Boolean)
      .some((value) => String(value).toLowerCase().includes(keyword))
  })
})

async function loadData() {
  loading.value = true
  try {
    const params = {
      pageNo: query.pageNo,
      pageSize: query.pageSize,
      taskType: query.taskType,
      reviewStatus: query.reviewStatus
    }
    const data = await request.get('/api/teacher/reviews', { params })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetFilters() {
  Object.assign(query, { pageNo: 1, taskType: '', reviewStatus: '', keyword: '' })
  quickView.value = 'ALL'
  loadData()
}

function buildAnnotation(question) {
  return {
    questionId: question.id,
    score: Number(question.answerScore || 0),
    teacherComment: question.teacherComment || ''
  }
}

async function openReview(row) {
  reviewLoading.value = true
  reviewDialog.value = true
  try {
    const data = row.taskType === 'HOMEWORK'
      ? await request.get(`/api/classroom-tasks/homework-submissions/${row.submissionId}/report`)
      : await request.get(`/api/classroom-tasks/classroom-exam-submissions/${row.submissionId}/report`)
    reviewData.value = {
      submission: {},
      questions: [],
      homework: {},
      classroomExam: {},
      ...data
    }
    Object.assign(form, {
      taskType: row.taskType,
      submissionId: row.submissionId,
      score: Number(reviewData.value.submission?.score || 0),
      reviewStatus: 'REVIEWED',
      reviewRemark: reviewData.value.submission?.reviewRemark || '',
      annotations: (reviewData.value.questions || []).map(buildAnnotation)
    })
  } catch (error) {
    reviewDialog.value = false
    ElMessage.error(error.message)
  } finally {
    reviewLoading.value = false
  }
}

async function submitReview() {
  try {
    form.score = totalAnnotatedScore.value
    await request.post('/api/teacher/reviews', form)
    ElMessage.success('批改成功')
    reviewDialog.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function openReport(row) {
  if (row.taskType === 'HOMEWORK') {
    router.push(`/classroom/homework/report/${row.submissionId}`)
  } else {
    router.push(`/classroom/exam/report/${row.submissionId}`)
  }
}

function taskTypeLabel(value) {
  return value === 'EXAM' ? '考试' : '作业'
}

function reviewStatusMeta(value) {
  if (value === 'REVIEWED') {
    return { label: '已批改', type: 'success' }
  }
  if (value === 'AUTO_GRADED') {
    return { label: '自动批改', type: 'info' }
  }
  return { label: '待批改', type: 'warning' }
}

function isPendingReview(row) {
  return row.reviewStatus !== 'REVIEWED'
}

function scoreRisk(row) {
  const score = Number(row.score || 0)
  if (score <= 2) {
    return 'low'
  }
  if (score <= 4) {
    return 'medium'
  }
  return 'normal'
}

onMounted(loadData)
</script>

<template>
  <div class="review-page">
    <section class="review-hero">
      <div>
        <h1>批改中心</h1>
        <p>集中处理作业和考试提交，优先关注待批改、低分和异常任务。</p>
      </div>
      <el-button type="primary" @click="loadData">
        <el-icon><Refresh /></el-icon>
        刷新任务
      </el-button>
    </section>

    <section class="stat-grid">
      <div class="stat-card urgent">
        <span>待批改</span>
        <strong>{{ pendingCount }}</strong>
      </div>
      <div class="stat-card">
        <span>已批改</span>
        <strong>{{ reviewedCount }}</strong>
      </div>
      <div class="stat-card">
        <span>自动批改</span>
        <strong>{{ autoGradedCount }}</strong>
      </div>
      <div class="stat-card">
        <span>当前均分</span>
        <strong>{{ averageScore }}</strong>
      </div>
    </section>

    <section class="review-panel" v-loading="loading">
      <div class="toolbar">
        <el-input v-model="query.keyword" clearable placeholder="搜索班级、学员或任务">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select v-model="query.taskType" clearable placeholder="任务类型" @change="query.pageNo = 1; loadData()">
          <el-option label="作业" value="HOMEWORK" />
          <el-option label="考试" value="EXAM" />
        </el-select>
        <el-select v-model="query.reviewStatus" clearable placeholder="批改状态" @change="query.pageNo = 1; loadData()">
          <el-option label="自动批改" value="AUTO_GRADED" />
          <el-option label="已批改" value="REVIEWED" />
        </el-select>
        <div class="toolbar-actions">
          <el-button @click="loadData">查询</el-button>
          <el-button plain @click="resetFilters">重置</el-button>
        </div>
      </div>

      <div class="quick-tabs">
        <el-segmented
          v-model="quickView"
          :options="[
            { label: '全部', value: 'ALL' },
            { label: '待批改', value: 'PENDING' },
            { label: '已批改', value: 'REVIEWED' },
            { label: '自动批改', value: 'AUTO_GRADED' },
            { label: '考试', value: 'EXAM' },
            { label: '作业', value: 'HOMEWORK' }
          ]"
        />
      </div>

      <el-table :data="filteredList" class="review-table desktop-table" row-key="submissionId">
        <el-table-column label="学员信息" min-width="260">
          <template #default="{ row }">
            <div class="student-cell">
              <div class="avatar">{{ row.studentName?.slice(0, 1) || '学' }}</div>
              <div>
                <strong>{{ row.studentName || '-' }}</strong>
                <span>{{ row.classroomName || '-' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="任务信息" min-width="260">
          <template #default="{ row }">
            <div class="task-cell">
              <el-tag :type="row.taskType === 'EXAM' ? 'warning' : 'primary'" effect="plain">{{ taskTypeLabel(row.taskType) }}</el-tag>
              <div>
                <strong>{{ row.taskTitle || '-' }}</strong>
                <span>{{ row.taskType === 'EXAM' ? '考试提交' : '作业提交' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="得分" width="130">
          <template #default="{ row }">
            <div class="score-cell" :class="scoreRisk(row)">
              <strong>{{ row.score ?? 0 }}</strong>
              <span>{{ scoreRisk(row) === 'low' ? '低分关注' : '当前得分' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="批改状态" min-width="180">
          <template #default="{ row }">
            <div class="status-stack">
              <el-tag :type="reviewStatusMeta(row.reviewStatus).type">
                {{ reviewStatusMeta(row.reviewStatus).label }}
              </el-tag>
              <span>{{ isPendingReview(row) ? '建议优先处理' : '可查看报告或复查' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="批改进度" min-width="170">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-icon><DataAnalysis /></el-icon>
              <span>{{ row.reviewStatus === 'AUTO_GRADED' ? '系统已判分，待教师复核' : '教师批改记录已生成' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button link @click="openReport(row)">查看报告</el-button>
              <el-button :type="isPendingReview(row) ? 'primary' : 'default'" @click="openReview(row)">
                <el-icon><EditPen /></el-icon>
                {{ isPendingReview(row) ? '批改' : '复查' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-state">
            <el-icon><Collection /></el-icon>
            <strong>暂无待处理任务</strong>
            <span>调整筛选条件或刷新后查看全部批改记录。</span>
            <el-button type="primary" plain @click="resetFilters">查看全部任务</el-button>
          </div>
        </template>
      </el-table>
      <div class="mobile-review-list">
        <article v-for="row in filteredList" :key="row.submissionId" class="mobile-review-card">
          <div class="mobile-review-head">
            <div class="avatar">{{ row.studentName?.slice(0, 1) || '学' }}</div>
            <div>
              <strong>{{ row.studentName || '-' }}</strong>
              <span>{{ row.classroomName || '-' }}</span>
            </div>
          </div>
          <div class="mobile-review-task">
            <el-tag :type="row.taskType === 'EXAM' ? 'warning' : 'primary'" effect="plain">{{ taskTypeLabel(row.taskType) }}</el-tag>
            <strong>{{ row.taskTitle || '-' }}</strong>
          </div>
          <div class="mobile-review-meta">
            <span>得分 {{ row.score ?? 0 }}</span>
            <span>{{ reviewStatusMeta(row.reviewStatus).label }}</span>
            <span>{{ isPendingReview(row) ? '建议优先处理' : '可查看报告' }}</span>
          </div>
          <div class="mobile-review-actions">
            <el-button plain @click="openReport(row)">查看报告</el-button>
            <el-button :type="isPendingReview(row) ? 'primary' : 'default'" @click="openReview(row)">
              {{ isPendingReview(row) ? '批改' : '复查' }}
            </el-button>
          </div>
        </article>
      </div>
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.pageNo"
        class="pager"
        @current-change="(page) => { query.pageNo = page; loadData() }"
      />
    </section>

    <el-dialog v-model="reviewDialog" title="批改任务" width="960px" class="review-dialog">
      <div v-loading="reviewLoading" class="review-dialog-body">
        <div class="review-summary">
          <div>
            <el-icon><Tickets /></el-icon>
            <span>任务</span>
            <strong>{{ reviewData.homework?.title || reviewData.classroomExam?.title || '-' }}</strong>
          </div>
          <div>
            <el-icon><Finished /></el-icon>
            <span>当前总分</span>
            <strong>{{ totalAnnotatedScore }}</strong>
          </div>
          <div>
            <el-icon><DocumentChecked /></el-icon>
            <span>原始得分</span>
            <strong>{{ reviewData.submission.score ?? 0 }}</strong>
          </div>
          <div>
            <el-icon><User /></el-icon>
            <span>题目数</span>
            <strong>{{ reviewData.questions.length }}</strong>
          </div>
        </div>
        <div class="review-question-list">
          <article v-for="(item, index) in reviewData.questions" :key="item.id" class="review-question-card">
            <div class="review-question-title">{{ index + 1 }}. {{ item.stem }}</div>
            <div class="review-meta">
              <span>题型：{{ item.questionType }}</span>
              <span>分值：{{ item.score }}</span>
              <span>作答得分：{{ form.annotations[index]?.score ?? 0 }}</span>
            </div>
            <div class="answer-block">
              <div><span>学员答案</span><p>{{ item.userAnswer || '未作答' }}</p></div>
              <div><span>标准答案</span><p>{{ item.correctAnswer || '教师批改题' }}</p></div>
              <div><span>解析</span><p>{{ item.analysis || '暂无解析' }}</p></div>
            </div>
            <el-input
              v-model="form.annotations[index].teacherComment"
              type="textarea"
              :rows="3"
              placeholder="填写该题批注，支持指出问题、修改建议和得分依据"
            />
            <div class="score-editor">
              <el-input-number
                v-model="form.annotations[index].score"
                :min="0"
                :max="Number(item.score || 0)"
                :disabled="!item.subjective"
              />
              <span v-if="!item.subjective">客观题得分沿用系统判分；可补充题目批注。</span>
            </div>
          </article>
        </div>
        <el-input v-model="form.reviewRemark" type="textarea" :rows="4" placeholder="整体批改评语" />
      </div>
      <template #footer>
        <el-button @click="reviewDialog = false">取消</el-button>
        <el-button type="primary" @click="submitReview">
          <el-icon><Check /></el-icon>
          提交批改
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.review-page {
  display: grid;
  gap: 18px;
  max-width: 1320px;
  margin: 0 auto;
  padding: 28px 16px 40px;
}

.review-hero,
.review-panel,
.stat-card {
  border: 1px solid rgba(205, 216, 232, 0.78);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}

.review-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 24px 28px;
}

h1,
p {
  margin: 0;
}

h1 {
  color: #0f172a;
  font-size: 30px;
  line-height: 1.25;
  font-weight: 800;
}

.review-hero p {
  margin-top: 8px;
  color: var(--muted);
  font-size: 14px;
}

.review-hero :deep(.el-button),
.toolbar-actions :deep(.el-button),
.row-actions :deep(.el-button) {
  border-radius: 8px;
  font-weight: 700;
}

.review-hero :deep(.el-icon),
.row-actions :deep(.el-icon) {
  margin-right: 4px;
}

.stat-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.stat-card {
  padding: 18px;
}

.stat-card span {
  display: block;
  color: var(--muted);
  font-size: 13px;
}

.stat-card strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 30px;
  line-height: 1;
}

.stat-card.urgent {
  border-color: #fed7aa;
  background: #fffaf2;
}

.stat-card.urgent strong {
  color: #c2410c;
}

.review-panel {
  padding: 22px;
}

.toolbar {
  display: grid;
  gap: 12px;
  grid-template-columns: minmax(260px, 1fr) 180px 180px auto;
  margin-bottom: 12px;
}

.toolbar-actions,
.quick-tabs,
.row-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.toolbar-actions {
  justify-content: flex-end;
}

.quick-tabs {
  margin-bottom: 16px;
}

.review-table {
  border: 1px solid #e5edf7;
  border-radius: 10px;
  overflow: hidden;
}

.mobile-review-list {
  display: none;
}

.mobile-review-card {
  display: grid;
  gap: 12px;
  border: 1px solid #e5edf7;
  border-radius: 12px;
  padding: 14px;
  background: #fff;
}

.mobile-review-head,
.mobile-review-task {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mobile-review-head strong,
.mobile-review-head span,
.mobile-review-task strong {
  display: block;
}

.mobile-review-head span,
.mobile-review-meta {
  color: var(--muted);
  font-size: 13px;
}

.mobile-review-task {
  align-items: flex-start;
  flex-direction: column;
}

.mobile-review-meta,
.mobile-review-actions {
  display: grid;
  gap: 8px;
}

.mobile-review-meta {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.mobile-review-meta span {
  border-radius: 8px;
  padding: 8px;
  color: #1d4ed8;
  background: #eff6ff;
  text-align: center;
}

.mobile-review-actions {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.mobile-review-actions :deep(.el-button) {
  margin-left: 0;
}

.student-cell,
.task-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.avatar {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 10px;
  color: #fff;
  background: #2563eb;
  font-weight: 800;
}

.student-cell strong,
.task-cell strong {
  display: block;
  color: #0f172a;
}

.student-cell span,
.task-cell span,
.status-stack span,
.score-cell span,
.progress-cell span {
  display: block;
  margin-top: 5px;
  color: var(--muted);
  font-size: 13px;
}

.score-cell strong {
  color: #0f172a;
  font-size: 22px;
}

.score-cell.low strong {
  color: #dc2626;
}

.score-cell.medium strong {
  color: #d97706;
}

.status-stack {
  display: grid;
  gap: 6px;
  justify-items: flex-start;
}

.progress-cell {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  color: #475569;
}

.progress-cell .el-icon {
  margin-top: 2px;
  color: #2563eb;
}

.row-actions {
  justify-content: flex-end;
}

.empty-state {
  display: grid;
  place-items: center;
  gap: 10px;
  min-height: 260px;
  color: var(--muted);
  text-align: center;
}

.empty-state .el-icon {
  color: #94a3b8;
  font-size: 36px;
}

.empty-state strong {
  color: #0f172a;
  font-size: 18px;
}

.pager {
  margin-top: 16px;
  justify-content: center;
}

.review-dialog-body,
.review-question-list {
  display: grid;
  gap: 16px;
}

.review-summary {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.review-summary > div {
  border: 1px solid #e5edf7;
  border-radius: 10px;
  background: #f8fbff;
  padding: 14px;
}

.review-summary .el-icon {
  color: #2563eb;
}

.review-summary span,
.review-meta,
.score-editor span,
.answer-block span {
  color: var(--muted);
}

.review-summary span {
  display: block;
  margin-top: 6px;
  font-size: 12px;
}

.review-summary strong {
  display: block;
  margin-top: 6px;
  color: #0f172a;
  font-size: 18px;
}

.review-question-card {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid #dbe5f2;
  border-radius: 10px;
  background: #fff;
}

.review-question-title {
  color: #0f172a;
  font-weight: 800;
  line-height: 1.6;
}

.review-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.answer-block {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.answer-block div {
  border-radius: 8px;
  background: #f8fafc;
  padding: 10px;
}

.answer-block span {
  display: block;
  font-size: 12px;
  font-weight: 700;
}

.answer-block p {
  margin-top: 6px;
  color: #334155;
  font-size: 13px;
  line-height: 1.55;
}

.score-editor {
  display: flex;
  align-items: center;
  gap: 12px;
}

@media (max-width: 1080px) {
  .stat-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .toolbar,
  .review-summary,
  .answer-block {
    grid-template-columns: 1fr;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .review-page {
    padding: 18px 12px 32px;
  }

  .review-hero {
    align-items: flex-start;
    flex-direction: column;
  }

  .review-hero,
  .review-panel {
    padding: 18px;
  }

  .stat-grid {
    grid-template-columns: 1fr;
  }

  .desktop-table {
    display: none;
  }

  .mobile-review-list {
    display: grid;
    gap: 12px;
  }

  .quick-tabs {
    overflow-x: auto;
    padding-bottom: 4px;
  }

  .quick-tabs :deep(.el-segmented) {
    min-width: max-content;
  }

  h1 {
    font-size: 26px;
  }
}
</style>
