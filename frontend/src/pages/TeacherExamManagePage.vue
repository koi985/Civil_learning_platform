<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import {
  AlarmClock,
  Calendar,
  Check,
  Clock,
  Collection,
  EditPen,
  Finished,
  Plus,
  Search,
  Select,
  Stopwatch,
  Tickets,
  Warning
} from '@element-plus/icons-vue'
import request from '../utils/request'
import QuestionSelectorDialog from '../components/QuestionSelectorDialog.vue'

const DEFAULT_ANTI_CHEAT_NOTICE = '考试过程中请勿切屏、刷新页面或多人协作答题，系统会保留异常操作记录。'

const loading = ref(false)
const saving = ref(false)
const randomLoading = ref(false)
const list = ref([])
const total = ref(0)
const classroomOptions = ref([])
const selectorRef = ref()
const statusFilter = ref('ALL')
const query = reactive({ pageNo: 1, pageSize: 10, classroomId: null })
const antiRules = reactive({
  switchWarning: true,
  refreshRecord: true,
  collaborateWarning: true,
  anomalyRecord: true
})
const form = reactive({
  id: null,
  classroomId: null,
  title: '',
  description: '',
  durationMinutes: 60,
  startTime: '',
  endTime: '',
  antiCheatEnabled: true,
  antiCheatNotice: DEFAULT_ANTI_CHEAT_NOTICE,
  status: 'PUBLISHED',
  questionIds: []
})

const selectedClassroom = computed(() => classroomOptions.value.find((item) => item.id === form.classroomId))
const selectedClassroomName = computed(() => selectedClassroom.value?.name || '未选择班级')
const selectedClassroomMembers = computed(() => selectedClassroom.value?.memberCount || 0)
const estimatedScore = computed(() => form.questionIds.length * 2)
const activeRuleCount = computed(() => Object.values(antiRules).filter(Boolean).length)
const filteredList = computed(() => {
  if (statusFilter.value === 'ALL') {
    return list.value
  }
  return list.value.filter((item) => {
    const status = getExamStatus(item)
    if (statusFilter.value === 'PENDING_REVIEW') {
      return pendingReviewCount(item) > 0
    }
    return status.value === statusFilter.value
  })
})

async function loadBaseOptions() {
  const classrooms = await request.get('/api/teacher/classrooms', { params: { pageNo: 1, pageSize: 100 } })
  classroomOptions.value = classrooms.records
}

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/teacher/classroom-exams', { params: query })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: null,
    classroomId: null,
    title: '',
    description: '',
    durationMinutes: 60,
    startTime: '',
    endTime: '',
    antiCheatEnabled: true,
    antiCheatNotice: DEFAULT_ANTI_CHEAT_NOTICE,
    status: 'PUBLISHED',
    questionIds: []
  })
  Object.assign(antiRules, {
    switchWarning: true,
    refreshRecord: true,
    collaborateWarning: true,
    anomalyRecord: true
  })
}

function buildAntiCheatNotice() {
  if (!form.antiCheatEnabled) {
    return ''
  }
  const notices = []
  if (antiRules.switchWarning) {
    notices.push('考试过程中请勿切屏')
  }
  if (antiRules.refreshRecord) {
    notices.push('请勿刷新页面')
  }
  if (antiRules.collaborateWarning) {
    notices.push('请勿与他人共享答题内容')
  }
  if (antiRules.anomalyRecord) {
    notices.push('系统会记录异常操作')
  }
  return notices.length ? `${notices.join('，')}。` : DEFAULT_ANTI_CHEAT_NOTICE
}

async function saveExam() {
  if (!form.classroomId) {
    ElMessage.warning('请选择发布班级')
    return
  }
  if (!form.title.trim()) {
    ElMessage.warning('请输入考试标题')
    return
  }
  if (!form.durationMinutes || form.durationMinutes < 1) {
    ElMessage.warning('请设置考试时长')
    return
  }
  if (!form.startTime || !form.endTime) {
    ElMessage.warning('请设置考试开始和结束时间')
    return
  }
  const start = parseTime(form.startTime)
  const end = parseTime(form.endTime)
  if (!start || !end || end.getTime() <= start.getTime()) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }
  if (!form.questionIds.length) {
    ElMessage.warning('请先选择题目')
    return
  }

  form.antiCheatNotice = buildAntiCheatNotice()
  saving.value = true
  try {
    await request.post('/api/teacher/classroom-exams', form)
    ElMessage.success(form.id ? '考试更新成功' : '考试发布成功')
    resetForm()
    await loadData()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

function editExam(item) {
  Object.assign(form, {
    id: item.id,
    classroomId: item.classroomId,
    title: item.title,
    description: item.description,
    durationMinutes: item.durationMinutes,
    startTime: item.startTime,
    endTime: item.endTime,
    antiCheatEnabled: item.antiCheatEnabled,
    antiCheatNotice: item.antiCheatNotice || DEFAULT_ANTI_CHEAT_NOTICE,
    status: item.status,
    questionIds: item.questionIds || []
  })
  const enabled = item.antiCheatEnabled !== false
  Object.assign(antiRules, {
    switchWarning: enabled,
    refreshRecord: enabled,
    collaborateWarning: enabled,
    anomalyRecord: enabled
  })
}

function setDuration(minutes) {
  form.durationMinutes = minutes
  if (form.startTime) {
    form.endTime = formatDateTime(addMinutes(parseTime(form.startTime), minutes))
  }
}

function setStartQuick(days, hour = 9) {
  const start = new Date()
  start.setDate(start.getDate() + days)
  start.setHours(hour, 0, 0, 0)
  form.startTime = formatDateTime(start)
  form.endTime = formatDateTime(addMinutes(start, form.durationMinutes || 60))
}

function clearQuestions() {
  form.questionIds = []
}

async function randomPickQuestions() {
  try {
    if (form.questionIds.length) {
      await ElMessageBox.confirm('随机抽题会替换当前已选题目，确认继续吗？', '随机抽取 120 道', { type: 'warning' })
    }
    randomLoading.value = true
    const questions = await request.get('/api/questions/random', { params: { count: 120 } })
    form.questionIds = questions.map((item) => item.id)
    ElMessage.success('已随机抽取 120 道题')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '随机抽题失败')
    }
  } finally {
    randomLoading.value = false
  }
}

function parseTime(value) {
  if (!value) {
    return null
  }
  const normalized = String(value).replace('T', ' ').replace(/-/g, '/')
  const date = new Date(normalized)
  return Number.isNaN(date.getTime()) ? null : date
}

function addMinutes(date, minutes) {
  if (!date) {
    return new Date()
  }
  return new Date(date.getTime() + Number(minutes || 0) * 60 * 1000)
}

function formatDateTime(date) {
  const pad = (value) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function getExamStatus(item) {
  const now = Date.now()
  const start = parseTime(item.startTime)
  const end = parseTime(item.endTime)
  if (start && start.getTime() > now) {
    return { label: '未开始', value: 'NOT_STARTED', type: 'info' }
  }
  if (end && end.getTime() < now) {
    return { label: '已结束', value: 'CLOSED', type: 'warning' }
  }
  return { label: '进行中', value: 'ACTIVE', type: 'success' }
}

function pendingReviewCount(row) {
  return Math.max(0, Number(row.submissionCount || 0) - Number(row.reviewedCount || 0))
}

onMounted(async () => {
  await loadBaseOptions()
  await loadData()
})
</script>

<template>
  <div class="exam-page">
    <section class="exam-hero">
      <div>
        <h1>{{ form.id ? '编辑考试' : '发布考试' }}</h1>
        <p>配置班级考试、时间窗口、防作弊规则和题目范围。</p>
      </div>
      <el-button type="warning" @click="resetForm">
        <el-icon><Plus /></el-icon>
        新建考试
      </el-button>
    </section>

    <section class="publish-shell">
      <div class="publish-form">
        <div class="form-section">
          <div class="section-head">
            <h2>基础信息</h2>
            <span>先确定考试对象和考试名称</span>
          </div>
          <div class="field-grid">
            <label class="field-block">
              <span>发布班级</span>
              <el-select v-model="form.classroomId" placeholder="选择班级">
                <el-option v-for="item in classroomOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </label>
            <label class="field-block">
              <span>考试标题</span>
              <el-input v-model="form.title" maxlength="60" placeholder="如：班级月考一" />
            </label>
          </div>
          <label class="field-block description-block">
            <span>考试说明</span>
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="4"
              maxlength="300"
              show-word-limit
              placeholder="说明考试范围、答题要求、提交方式或注意事项。"
            />
          </label>
        </div>

        <div class="form-section">
          <div class="section-head">
            <h2>时间规则</h2>
            <span>开始、结束和考试时长会影响学生进入考试</span>
          </div>
          <div class="time-grid">
            <label class="field-block">
              <span>考试时长</span>
              <el-input-number v-model="form.durationMinutes" :min="1" :max="300" controls-position="right" />
            </label>
            <label class="field-block">
              <span>开始时间</span>
              <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择开始时间" />
            </label>
            <label class="field-block">
              <span>结束时间</span>
              <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择结束时间" />
            </label>
          </div>
          <div class="quick-row">
            <el-button plain @click="setStartQuick(0, 19)">今晚 19:00</el-button>
            <el-button plain @click="setStartQuick(1, 9)">明天 09:00</el-button>
            <el-button plain @click="setStartQuick(6, 9)">本周末</el-button>
            <el-button plain @click="setDuration(90)">90 分钟</el-button>
            <el-button plain @click="setDuration(120)">120 分钟</el-button>
          </div>
        </div>

        <div class="form-section">
          <div class="section-head">
            <h2>考试规则</h2>
            <span>防作弊提示会展示在学生考试开始页</span>
          </div>
          <div class="rule-master">
            <div>
              <strong>防作弊提示</strong>
              <span>{{ form.antiCheatEnabled ? `已启用 ${activeRuleCount} 项提醒` : '当前未启用防作弊提醒' }}</span>
            </div>
            <el-switch v-model="form.antiCheatEnabled" />
          </div>
          <div class="rule-grid" :class="{ disabled: !form.antiCheatEnabled }">
            <div class="rule-item">
              <div><strong>切屏提醒</strong><span>提醒学生不要离开考试页</span></div>
              <el-switch v-model="antiRules.switchWarning" :disabled="!form.antiCheatEnabled" />
            </div>
            <div class="rule-item">
              <div><strong>刷新记录</strong><span>提示刷新页面会留下异常记录</span></div>
              <el-switch v-model="antiRules.refreshRecord" :disabled="!form.antiCheatEnabled" />
            </div>
            <div class="rule-item">
              <div><strong>协作提醒</strong><span>禁止共享答题内容或多人协作</span></div>
              <el-switch v-model="antiRules.collaborateWarning" :disabled="!form.antiCheatEnabled" />
            </div>
            <div class="rule-item">
              <div><strong>异常记录</strong><span>说明系统会保留异常操作</span></div>
              <el-switch v-model="antiRules.anomalyRecord" :disabled="!form.antiCheatEnabled" />
            </div>
          </div>
          <el-input
            v-model="form.antiCheatNotice"
            class="notice-input"
            type="textarea"
            :rows="2"
            maxlength="180"
            show-word-limit
            placeholder="请输入考试开始页展示的防作弊提示"
          />
        </div>

        <div class="form-section">
          <div class="section-head">
            <h2>题目配置</h2>
            <span>选题后同步计算题量和预估分值</span>
          </div>
          <div class="question-config">
            <div class="question-count">
              <el-icon><Tickets /></el-icon>
              <div>
                <strong>已选 {{ form.questionIds.length }} 道题</strong>
                <span>预计 {{ estimatedScore }} 分</span>
              </div>
            </div>
            <div class="question-actions">
              <el-button plain :disabled="!form.questionIds.length" @click="clearQuestions">清空题目</el-button>
              <el-button type="primary" plain :loading="randomLoading" @click="randomPickQuestions">随机抽取 120 道</el-button>
              <el-button type="warning" plain @click="selectorRef.open()">选择题目</el-button>
            </div>
          </div>
          <div v-if="!form.questionIds.length" class="question-empty">
            <el-icon><Select /></el-icon>
            <span>还没有选择题目，发布考试前请先从题库组卷。</span>
          </div>
        </div>

        <div class="submit-bar">
          <el-button type="warning" size="large" :loading="saving" @click="saveExam">
            <el-icon><Check /></el-icon>
            {{ form.id ? '更新考试' : '发布考试' }}
          </el-button>
          <el-button size="large" @click="resetForm">{{ form.id ? '取消编辑' : '清空表单' }}</el-button>
        </div>
      </div>

      <aside class="publish-preview">
        <div class="preview-sticky">
          <div class="preview-title">
            <el-icon><Finished /></el-icon>
            <span>考试预览</span>
          </div>
          <div class="preview-card">
            <div class="preview-class">{{ selectedClassroomName }}</div>
            <h3>{{ form.title || '考试标题' }}</h3>
            <p>{{ form.description || '考试说明会展示在这里。' }}</p>
            <div class="preview-grid">
              <div>
                <span>题量</span>
                <strong>{{ form.questionIds.length }} 道</strong>
              </div>
              <div>
                <span>预计分值</span>
                <strong>{{ estimatedScore }} 分</strong>
              </div>
              <div>
                <span>考试时长</span>
                <strong>{{ form.durationMinutes }} 分钟</strong>
              </div>
              <div>
                <span>预计参加</span>
                <strong>{{ selectedClassroomMembers }} 人</strong>
              </div>
            </div>
            <div class="preview-lines">
              <div><el-icon><Calendar /></el-icon><span>{{ form.startTime || '未设置开始时间' }}</span></div>
              <div><el-icon><Clock /></el-icon><span>{{ form.endTime || '未设置结束时间' }}</span></div>
              <div><el-icon><Warning /></el-icon><span>{{ form.antiCheatEnabled ? `防作弊 ${activeRuleCount} 项` : '未启用防作弊' }}</span></div>
            </div>
          </div>
        </div>
      </aside>
    </section>

    <section class="exam-list-panel" v-loading="loading">
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
            { label: '未开始', value: 'NOT_STARTED' },
            { label: '进行中', value: 'ACTIVE' },
            { label: '已结束', value: 'CLOSED' },
            { label: '待批改', value: 'PENDING_REVIEW' }
          ]"
        />
        <div class="toolbar-actions">
          <el-button @click="loadData">查询</el-button>
          <el-button type="warning" @click="resetForm">
            <el-icon><Plus /></el-icon>
            发布考试
          </el-button>
        </div>
      </div>

      <el-table :data="filteredList" class="exam-table">
        <el-table-column label="考试" min-width="280">
          <template #default="{ row }">
            <div class="exam-cell">
              <strong>{{ row.title }}</strong>
              <span>{{ row.classroomName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="getExamStatus(row).type">{{ getExamStatus(row).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="题量 / 时长" width="140">
          <template #default="{ row }">
            <div class="compact-stack">
              <strong>{{ row.questionCount || 0 }} 题</strong>
              <span>{{ row.durationMinutes || 0 }} 分钟</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="提交进度" min-width="170">
          <template #default="{ row }">
            <div class="compact-stack">
              <strong>已提交 {{ row.submissionCount || 0 }}</strong>
              <el-tag v-if="pendingReviewCount(row)" type="warning">待批改 {{ pendingReviewCount(row) }}</el-tag>
              <span v-else>暂无待批改</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="时间窗口" min-width="260">
          <template #default="{ row }">
            <div class="time-window">
              <div><el-icon><AlarmClock /></el-icon><span>{{ row.startTime || '-' }}</span></div>
              <div><el-icon><Stopwatch /></el-icon><span>{{ row.endTime || '-' }}</span></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="规则" min-width="190">
          <template #default="{ row }">
            <div class="rule-summary">
              <el-tag :type="row.antiCheatEnabled ? 'warning' : 'info'" effect="plain">
                {{ row.antiCheatEnabled ? '防作弊开启' : '防作弊关闭' }}
              </el-tag>
              <span>{{ row.antiCheatNotice || '无额外提示' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="editExam(row)">
              <el-icon><EditPen /></el-icon>
              编辑
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-state">
            <el-icon><Collection /></el-icon>
            <strong>还没有发布考试</strong>
            <span>配置班级、时间和题目后，即可发布第一场考试。</span>
          </div>
        </template>
      </el-table>
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

    <QuestionSelectorDialog ref="selectorRef" v-model="form.questionIds" />
  </div>
</template>

<style scoped>
.exam-page {
  display: grid;
  gap: 18px;
  box-sizing: border-box;
  width: 100%;
  max-width: 1320px;
  margin: 0 auto;
  padding: 28px 16px 40px;
  overflow-x: clip;
}

.exam-hero,
.publish-shell,
.exam-list-panel {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  border: 1px solid rgba(205, 216, 232, 0.78);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}

.exam-hero {
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

.exam-hero p,
.section-head span,
.rule-master span,
.rule-item span,
.question-count span,
.exam-cell span,
.compact-stack span,
.rule-summary span {
  color: var(--muted);
}

.exam-hero p {
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
  background: #fffaf2;
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

.field-grid,
.time-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.time-grid {
  grid-template-columns: 170px repeat(2, minmax(0, 1fr));
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

.description-block {
  margin-top: 16px;
}

.quick-row,
.question-actions,
.list-toolbar,
.toolbar-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.quick-row {
  margin-top: 12px;
}

.quick-row :deep(.el-button) {
  margin-left: 0;
  border-radius: 8px;
  font-weight: 700;
}

.rule-master,
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

.rule-master strong,
.rule-master span {
  display: block;
}

.rule-master span {
  margin-top: 4px;
  font-size: 13px;
}

.rule-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 12px;
}

.rule-grid.disabled {
  opacity: 0.62;
}

.rule-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #e5edf7;
  border-radius: 10px;
  background: #f8fbff;
  padding: 14px;
}

.rule-item strong,
.rule-item span {
  display: block;
}

.rule-item span {
  margin-top: 4px;
  font-size: 12px;
}

.notice-input {
  margin-top: 12px;
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
  color: #d97706;
  background: #fff3d6;
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
.exam-hero :deep(.el-button) {
  border-radius: 8px;
  font-weight: 700;
}

.submit-bar :deep(.el-icon),
.toolbar-actions :deep(.el-icon),
.exam-hero :deep(.el-icon) {
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
  color: #d97706;
}

.preview-card {
  border: 1px solid #ead9ba;
  border-radius: 10px;
  background: #fff;
  padding: 16px;
}

.preview-class {
  display: inline-flex;
  border-radius: 999px;
  padding: 5px 9px;
  color: #92400e;
  background: #fff3d6;
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
  background: #fffaf2;
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

.preview-lines {
  display: grid;
  gap: 9px;
  margin-top: 14px;
}

.preview-lines div,
.time-window div {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #475569;
  font-size: 13px;
}

.exam-list-panel {
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

.exam-table {
  border: 1px solid #e5edf7;
  border-radius: 10px;
  overflow: hidden;
}

.exam-cell strong,
.compact-stack strong {
  display: block;
  color: #0f172a;
}

.exam-cell strong {
  font-size: 15px;
}

.exam-cell span,
.compact-stack span {
  display: block;
  margin-top: 5px;
  font-size: 13px;
}

.compact-stack {
  display: grid;
  gap: 6px;
}

.time-window {
  display: grid;
  gap: 6px;
}

.rule-summary {
  display: grid;
  gap: 7px;
}

.rule-summary span {
  display: -webkit-box;
  overflow: hidden;
  font-size: 12px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
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

.pager {
  margin-top: 16px;
  justify-content: center;
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

  .time-grid,
  .list-toolbar {
    grid-template-columns: 1fr;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .exam-page {
    padding: 18px 12px 32px;
  }

  .exam-hero,
  .question-config,
  .rule-master {
    align-items: flex-start;
    flex-direction: column;
  }

  .exam-hero,
  .publish-form,
  .publish-preview,
  .exam-list-panel {
    padding: 18px;
  }

  .field-grid,
  .rule-grid,
  .preview-grid {
    grid-template-columns: 1fr;
  }

  h1 {
    font-size: 26px;
  }
}
</style>
