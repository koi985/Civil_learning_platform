<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import {
  Calendar,
  Check,
  Clock,
  Collection,
  Document,
  EditPen,
  Plus,
  Search,
  Select,
  Tickets,
  UploadFilled
} from '@element-plus/icons-vue'
import request from '../utils/request'
import QuestionSelectorDialog from '../components/QuestionSelectorDialog.vue'
import { resolveAssetUrl, resolveFileUploadUrl, resolveFileViewUrl } from '../utils/assets'

const loading = ref(false)
const saving = ref(false)
const list = ref([])
const total = ref(0)
const classroomOptions = ref([])
const selectorRef = ref()
const attachmentName = ref('')
const statusFilter = ref('ALL')
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('civil-token') || ''}` }
const query = reactive({ pageNo: 1, pageSize: 10, classroomId: null })
const form = reactive({ id: null, classroomId: null, title: '', description: '', attachmentFileId: null, deadlineAt: '', status: 'PUBLISHED', questionIds: [] })
const selectedClassroom = computed(() => classroomOptions.value.find((item) => item.id === form.classroomId))
const selectedClassroomName = computed(() => selectedClassroom.value?.name || '未选择班级')
const selectedClassroomMembers = computed(() => selectedClassroom.value?.memberCount || 0)
const estimatedMinutes = computed(() => Math.max(0, form.questionIds.length * 2))
const attachmentStatus = computed(() => (form.attachmentFileId ? (attachmentName.value || `附件 #${form.attachmentFileId}`) : '未上传附件'))
const filteredList = computed(() => {
  if (statusFilter.value === 'ALL') {
    return list.value
  }
  return list.value.filter((item) => {
    const pendingReview = pendingReviewCount(item)
    const deadline = item.deadlineAt ? new Date(String(item.deadlineAt).replace(/-/g, '/')) : null
    const isClosed = deadline ? deadline.getTime() < Date.now() : false
    if (statusFilter.value === 'PENDING_REVIEW') {
      return pendingReview > 0
    }
    if (statusFilter.value === 'CLOSED') {
      return isClosed
    }
    if (statusFilter.value === 'ACTIVE') {
      return !isClosed
    }
    return true
  })
})

async function loadBaseOptions() {
  const classrooms = await request.get('/api/teacher/classrooms', { params: { pageNo: 1, pageSize: 100 } })
  classroomOptions.value = classrooms.records
}

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/teacher/homeworks', { params: query })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, { id: null, classroomId: null, title: '', description: '', attachmentFileId: null, deadlineAt: '', status: 'PUBLISHED', questionIds: [] })
  attachmentName.value = ''
}

async function saveHomework() {
  if (!form.classroomId) {
    ElMessage.warning('请选择发布班级')
    return
  }
  if (!form.title.trim()) {
    ElMessage.warning('请输入作业标题')
    return
  }
  if (!form.deadlineAt) {
    ElMessage.warning('请选择截止时间')
    return
  }
  if (!form.questionIds.length) {
    ElMessage.warning('请先选择题目')
    return
  }

  saving.value = true
  try {
    await request.post('/api/teacher/homeworks', form)
    ElMessage.success(form.id ? '作业更新成功' : '作业发布成功')
    resetForm()
    await loadData()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

function editHomework(item) {
  Object.assign(form, {
    id: item.id,
    classroomId: item.classroomId,
    title: item.title,
    description: item.description,
    attachmentFileId: item.attachmentFileId,
    deadlineAt: item.deadlineAt,
    status: item.status,
    questionIds: item.questionIds || []
  })
  attachmentName.value = item.attachmentName || ''
}

function handleUploadSuccess(response) {
  if (response.code === 200) {
    form.attachmentFileId = response.data.id
    attachmentName.value = response.data.originalName || response.data.fileName || response.data.name || `附件 #${response.data.id}`
    ElMessage.success('附件上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

function setDeadlineQuick(days) {
  const deadline = new Date()
  deadline.setDate(deadline.getDate() + days)
  deadline.setHours(22, 0, 0, 0)
  const pad = (value) => String(value).padStart(2, '0')
  form.deadlineAt = `${deadline.getFullYear()}-${pad(deadline.getMonth() + 1)}-${pad(deadline.getDate())} ${pad(deadline.getHours())}:${pad(deadline.getMinutes())}:${pad(deadline.getSeconds())}`
}

function pendingReviewCount(row) {
  return Math.max(0, Number(row.submissionCount || 0) - Number(row.reviewedCount || 0))
}

function clearQuestions() {
  form.questionIds = []
}

function openAttachment(row) {
  if (row.attachmentUrl) {
    window.open(resolveAssetUrl(row.attachmentUrl), '_blank')
  }
}

onMounted(async () => {
  await loadBaseOptions()
  await loadData()
})
</script>

<template>
  <div class="homework-page">
    <section class="homework-hero">
      <div>
        <h1>发布作业</h1>
        <p>给班级发布练习任务、附件资料和待批改题目。</p>
      </div>
      <el-button type="primary" @click="resetForm">
        <el-icon><Plus /></el-icon>
        新建作业
      </el-button>
    </section>

    <section class="publish-shell">
      <div class="publish-form">
        <div class="form-section">
          <div class="section-head">
            <h2>作业信息</h2>
            <span>按发布流程填写班级、标题和截止时间</span>
          </div>
          <div class="field-grid">
            <label class="field-block">
              <span>发布班级</span>
              <el-select v-model="form.classroomId" placeholder="选择班级">
                <el-option v-for="item in classroomOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </label>
            <label class="field-block">
              <span>作业标题</span>
              <el-input v-model="form.title" maxlength="60" placeholder="如：行测资料分析专项练习" />
            </label>
          </div>
          <label class="field-block deadline-block">
            <span>截止时间</span>
            <div class="deadline-row">
              <el-date-picker v-model="form.deadlineAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择截止时间" />
              <div class="quick-deadlines">
                <el-button plain @click="setDeadlineQuick(0)">今天 22:00</el-button>
                <el-button plain @click="setDeadlineQuick(1)">明天 22:00</el-button>
                <el-button plain @click="setDeadlineQuick(3)">3 天后</el-button>
                <el-button plain @click="setDeadlineQuick(7)">7 天后</el-button>
              </div>
            </div>
          </label>
          <label class="field-block">
            <span>作业说明</span>
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="4"
              maxlength="300"
              show-word-limit
              placeholder="说明完成要求、提交形式、批改重点或参考资料。"
            />
          </label>
        </div>

        <div class="form-section">
          <div class="section-head">
            <h2>作业附件</h2>
            <span>支持 PDF、Word、图片等补充资料</span>
          </div>
          <el-upload
            :action="resolveFileUploadUrl('homework')"
            :headers="uploadHeaders"
            :show-file-list="false"
            @success="handleUploadSuccess"
          >
            <div class="attachment-card" :class="{ filled: form.attachmentFileId }">
              <el-icon><UploadFilled /></el-icon>
              <div>
                <strong>{{ form.attachmentFileId ? attachmentStatus : '上传作业附件' }}</strong>
                <span>{{ form.attachmentFileId ? '点击可替换附件' : '可上传讲义、答题模板或补充材料' }}</span>
              </div>
            </div>
          </el-upload>
          <a v-if="form.attachmentFileId" class="inline-link" :href="resolveFileViewUrl(form.attachmentFileId)" target="_blank">查看当前附件</a>
        </div>

        <div class="form-section">
          <div class="section-head">
            <h2>题目配置</h2>
            <span>选择题目后会同步到右侧发布预览</span>
          </div>
          <div class="question-config">
            <div class="question-count">
              <el-icon><Tickets /></el-icon>
              <div>
                <strong>已选 {{ form.questionIds.length }} 道题</strong>
                <span>预计 {{ estimatedMinutes }} 分钟完成</span>
              </div>
            </div>
            <div class="question-actions">
              <el-button plain :disabled="!form.questionIds.length" @click="clearQuestions">清空题目</el-button>
              <el-button type="primary" plain @click="selectorRef.open()">选择题目</el-button>
            </div>
          </div>
          <div v-if="!form.questionIds.length" class="question-empty">
            <el-icon><Select /></el-icon>
            <span>还没有选择题目，发布前请从题库选择练习题。</span>
          </div>
        </div>

        <div class="submit-bar">
          <el-button type="primary" size="large" :loading="saving" @click="saveHomework">
            <el-icon><Check /></el-icon>
            {{ form.id ? '更新作业' : '发布作业' }}
          </el-button>
          <el-button size="large" @click="resetForm">{{ form.id ? '取消编辑' : '清空表单' }}</el-button>
        </div>
      </div>

      <aside class="publish-preview">
        <div class="preview-sticky">
          <div class="preview-title">
            <el-icon><Document /></el-icon>
            <span>发布预览</span>
          </div>
          <div class="preview-card">
            <div class="preview-class">{{ selectedClassroomName }}</div>
            <h3>{{ form.title || '作业标题' }}</h3>
            <p>{{ form.description || '作业说明会展示在这里。' }}</p>
            <div class="preview-grid">
              <div>
                <span>题量</span>
                <strong>{{ form.questionIds.length }} 道</strong>
              </div>
              <div>
                <span>预计用时</span>
                <strong>{{ estimatedMinutes }} 分钟</strong>
              </div>
              <div>
                <span>预计提交</span>
                <strong>{{ selectedClassroomMembers }} 人</strong>
              </div>
              <div>
                <span>附件</span>
                <strong>{{ form.attachmentFileId ? '已上传' : '无' }}</strong>
              </div>
            </div>
            <div class="preview-deadline">
              <el-icon><Clock /></el-icon>
              <span>{{ form.deadlineAt || '未设置截止时间' }}</span>
            </div>
          </div>
        </div>
      </aside>
    </section>

    <section class="homework-list-panel" v-loading="loading">
      <div class="list-toolbar">
        <el-select v-model="query.classroomId" clearable placeholder="筛选班级">
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
          <el-option v-for="item in classroomOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-segmented
          v-model="statusFilter"
          :options="[
            { label: '全部', value: 'ALL' },
            { label: '进行中', value: 'ACTIVE' },
            { label: '已截止', value: 'CLOSED' },
            { label: '待批改', value: 'PENDING_REVIEW' }
          ]"
        />
        <div class="toolbar-actions">
          <el-button @click="loadData">查询</el-button>
          <el-button type="primary" @click="resetForm">
            <el-icon><Plus /></el-icon>
            发布作业
          </el-button>
        </div>
      </div>
      <el-table :data="filteredList" class="homework-table desktop-table">
        <el-table-column label="作业" min-width="280">
          <template #default="{ row }">
            <div class="homework-cell">
              <strong>{{ row.title }}</strong>
              <span>{{ row.classroomName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="题量" width="100">
          <template #default="{ row }">
            <strong>{{ row.questionCount || 0 }} 道</strong>
          </template>
        </el-table-column>
        <el-table-column label="提交 / 批改进度" min-width="220">
          <template #default="{ row }">
            <div class="progress-stack">
              <span>已提交 {{ row.submissionCount || 0 }} · 已批改 {{ row.reviewedCount || 0 }}</span>
              <el-tag v-if="pendingReviewCount(row)" type="warning">待批改 {{ pendingReviewCount(row) }}</el-tag>
              <el-tag v-else type="success" effect="plain">无待批改</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="截止时间" min-width="180">
          <template #default="{ row }">
            <span class="deadline-text"><el-icon><Calendar /></el-icon>{{ row.deadlineAt || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="附件" width="120">
          <template #default="{ row }">
            <a v-if="row.attachmentUrl" class="inline-link" :href="resolveAssetUrl(row.attachmentUrl)" target="_blank">查看附件</a>
            <span v-else class="muted-text">无附件</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="editHomework(row)">
              <el-icon><EditPen /></el-icon>
              编辑
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-state">
            <el-icon><Collection /></el-icon>
            <strong>还没有发布作业</strong>
            <span>选择班级和题目后，即可发布第一份练习任务。</span>
          </div>
        </template>
      </el-table>
      <div class="mobile-homework-list">
        <article v-for="row in filteredList" :key="row.id" class="mobile-homework-card">
          <div>
            <strong>{{ row.title }}</strong>
            <span>{{ row.classroomName }}</span>
          </div>
          <div class="mobile-homework-meta">
            <span>{{ row.questionCount || 0 }} 道题</span>
            <span>提交 {{ row.submissionCount || 0 }}</span>
            <span>批改 {{ row.reviewedCount || 0 }}</span>
          </div>
          <div class="mobile-homework-line">
            <el-icon><Calendar /></el-icon>
            <span>{{ row.deadlineAt || '-' }}</span>
          </div>
          <div class="mobile-homework-actions">
            <el-button v-if="row.attachmentUrl" plain @click="openAttachment(row)">查看附件</el-button>
            <el-button type="primary" @click="editHomework(row)">编辑</el-button>
          </div>
        </article>
      </div>
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.pageNo"
        style="margin-top: 16px; justify-content: center"
        @current-change="(page) => { query.pageNo = page; loadData() }"
      />
    </section>

    <QuestionSelectorDialog ref="selectorRef" v-model="form.questionIds" />
  </div>
</template>

<style scoped>
.homework-page {
  display: grid;
  gap: 18px;
  max-width: 1320px;
  margin: 0 auto;
  padding: 28px 16px 40px;
}

.homework-hero,
.publish-shell,
.homework-list-panel {
  border: 1px solid rgba(205, 216, 232, 0.78);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}

.homework-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 24px 28px;
}

h1,
h2,
h3,
p {
  margin: 0;
}

h1 {
  color: #0f172a;
  font-size: 30px;
  line-height: 1.25;
  font-weight: 800;
}

.homework-hero p,
.section-head span,
.attachment-card span,
.question-count span,
.homework-cell span,
.muted-text {
  color: var(--muted);
}

.homework-hero p {
  margin-top: 8px;
  font-size: 14px;
}

.publish-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
}

.publish-form {
  padding: 28px;
  border-right: 1px solid #e5edf7;
}

.publish-preview {
  padding: 28px;
  background: #f8fbff;
}

.form-section + .form-section {
  margin-top: 28px;
}

.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

h2 {
  color: #0f172a;
  font-size: 18px;
  line-height: 1.35;
  font-weight: 800;
}

.section-head span {
  font-size: 13px;
}

.field-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field-block {
  display: grid;
  gap: 8px;
}

.field-block > span {
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}

.deadline-block {
  margin: 16px 0;
}

.deadline-row {
  display: grid;
  gap: 10px;
}

.quick-deadlines,
.question-actions,
.list-toolbar,
.toolbar-actions,
.progress-stack {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.quick-deadlines :deep(.el-button) {
  margin-left: 0;
  border-radius: 8px;
  font-weight: 700;
}

.attachment-card {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 88px;
  border: 1px dashed #b8c8dc;
  border-radius: 10px;
  background: #f8fbff;
  padding: 16px;
  cursor: pointer;
}

.attachment-card.filled {
  border-style: solid;
  background: #f0f7ff;
}

.attachment-card .el-icon {
  color: #2563eb;
  font-size: 28px;
}

.attachment-card strong {
  display: block;
  color: #0f172a;
}

.attachment-card span {
  display: block;
  margin-top: 4px;
  font-size: 13px;
}

.inline-link {
  display: inline-flex;
  margin-top: 10px;
  color: var(--primary);
  text-decoration: none;
}

.question-config {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid #dbe5f2;
  border-radius: 10px;
  background: #fff;
  padding: 16px;
}

.question-count {
  display: flex;
  align-items: center;
  gap: 12px;
}

.question-count .el-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 8px;
  color: #7c3aed;
  background: #f3e8ff;
  font-size: 20px;
}

.question-count strong {
  display: block;
  color: #0f172a;
}

.question-count span {
  display: block;
  margin-top: 4px;
  font-size: 13px;
}

.question-empty {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  border-radius: 8px;
  padding: 10px 12px;
  color: #64748b;
  background: #f8fafc;
  font-size: 13px;
}

.submit-bar {
  display: flex;
  gap: 10px;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e5edf7;
}

.submit-bar :deep(.el-button),
.toolbar-actions :deep(.el-button),
.homework-hero :deep(.el-button) {
  border-radius: 8px;
  font-weight: 700;
}

.submit-bar :deep(.el-icon),
.toolbar-actions :deep(.el-icon),
.homework-hero :deep(.el-icon) {
  margin-right: 4px;
}

.preview-sticky {
  position: sticky;
  top: 116px;
}

.preview-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  color: #0f172a;
  font-weight: 800;
}

.preview-title .el-icon {
  color: #2563eb;
}

.preview-card {
  border: 1px solid #dbe5f2;
  border-radius: 10px;
  background: #fff;
  padding: 16px;
}

.preview-class {
  display: inline-flex;
  border-radius: 999px;
  padding: 5px 9px;
  color: #1d4ed8;
  background: #eaf2ff;
  font-size: 12px;
  font-weight: 700;
}

h3 {
  margin-top: 14px;
  color: #0f172a;
  font-size: 18px;
  line-height: 1.35;
  font-weight: 800;
}

.preview-card p {
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.preview-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 16px;
}

.preview-grid div {
  border-radius: 8px;
  background: #f8fbff;
  padding: 10px;
}

.preview-grid span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.preview-grid strong {
  display: block;
  margin-top: 4px;
  color: #0f172a;
}

.preview-deadline {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 14px;
  color: #475569;
  font-size: 13px;
}

.homework-list-panel {
  padding: 22px;
}

.list-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 320px) auto minmax(220px, 1fr);
  margin-bottom: 16px;
}

.toolbar-actions {
  justify-content: flex-end;
}

.homework-table {
  border: 1px solid #e5edf7;
  border-radius: 10px;
  overflow: hidden;
}

.mobile-homework-list {
  display: none;
}

.mobile-homework-card {
  display: grid;
  gap: 10px;
  border: 1px solid #e5edf7;
  border-radius: 12px;
  padding: 14px;
  background: #fff;
}

.mobile-homework-card strong,
.mobile-homework-card span {
  display: block;
}

.mobile-homework-card span,
.mobile-homework-line {
  color: var(--muted);
  font-size: 13px;
}

.mobile-homework-meta,
.mobile-homework-actions {
  display: grid;
  gap: 8px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.mobile-homework-meta span {
  border-radius: 8px;
  padding: 8px;
  color: #1d4ed8;
  background: #eff6ff;
  text-align: center;
}

.mobile-homework-line {
  display: flex;
  align-items: center;
  gap: 6px;
}

.mobile-homework-actions {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.mobile-homework-actions :deep(.el-button) {
  margin-left: 0;
}

.homework-cell strong {
  display: block;
  color: #0f172a;
  font-size: 15px;
}

.homework-cell span {
  display: block;
  margin-top: 5px;
  font-size: 13px;
}

.progress-stack {
  align-items: flex-start;
  flex-direction: column;
  gap: 6px;
}

.progress-stack span,
.deadline-text {
  color: #475569;
  font-size: 13px;
}

.deadline-text {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.empty-state {
  display: grid;
  place-items: center;
  gap: 10px;
  min-height: 240px;
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

@media (max-width: 1080px) {
  .publish-shell {
    grid-template-columns: 1fr;
  }

  .publish-form {
    border-right: 0;
    border-bottom: 1px solid #e5edf7;
  }

  .preview-sticky {
    position: static;
  }

  .list-toolbar {
    grid-template-columns: 1fr;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .homework-page {
    padding: 18px 12px 32px;
  }

  .homework-hero,
  .question-config {
    align-items: flex-start;
    flex-direction: column;
  }

  .homework-hero,
  .publish-form,
  .publish-preview,
  .homework-list-panel {
    padding: 18px;
  }

  .field-grid,
  .preview-grid {
    grid-template-columns: 1fr;
  }

  .desktop-table {
    display: none;
  }

  .mobile-homework-list {
    display: grid;
    gap: 12px;
  }

  .list-toolbar :deep(.el-select),
  .toolbar-actions,
  .toolbar-actions :deep(.el-button) {
    width: 100%;
  }

  h1 {
    font-size: 26px;
  }
}
</style>
