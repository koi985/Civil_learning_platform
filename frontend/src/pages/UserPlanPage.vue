<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const planDialogVisible = ref(false)
const overview = ref({
  plan: null,
  summary: {},
  todayTasks: [],
  upcomingTasks: [],
  weakKnowledgePoints: []
})
const form = reactive({
  targetExamType: '省考',
  subjectName: '行测',
  dailyStudyMinutes: 60,
  dailyQuestionCount: 15,
  weeklyExamCount: 1,
  durationDays: 14,
  startDate: formatDate(new Date()),
  focusKnowledgePoints: '言语理解,资料分析,判断推理'
})

const planStatus = computed(() => {
  if (!overview.value.plan) return { label: '未创建', type: 'info', desc: '保存计划后会自动生成每日任务' }
  if (Number(overview.value.summary.completionRate || 0) >= 100) {
    return { label: '已完成', type: 'success', desc: '当前计划任务已全部完成' }
  }
  if (Number(overview.value.summary.daysRemaining || 0) <= 0) {
    return { label: '已到期', type: 'warning', desc: '建议调整周期或创建新计划' }
  }
  return { label: '进行中', type: 'primary', desc: '按今日任务推进即可保持节奏' }
})

const todayProgress = computed(() => {
  const total = Number(overview.value.summary.todayTaskCount || 0)
  const done = Number(overview.value.summary.todayCompletedTaskCount || 0)
  return total ? Math.round((done / total) * 100) : 0
})

const rhythmTip = computed(() => {
  const rate = Number(overview.value.summary.completionRate || 0)
  const daysRemaining = Number(overview.value.summary.daysRemaining || 0)
  const total = Number(overview.value.summary.totalTaskCount || 0)
  const completed = Number(overview.value.summary.completedTaskCount || 0)
  const pending = Math.max(total - completed, 0)
  if (!overview.value.plan) return '先保存目标和每日强度，系统会生成可执行任务。'
  if (pending === 0) return '计划已完成，可以进入阶段诊断或创建下一轮计划。'
  if (daysRemaining <= 0) return `计划已到期，仍有 ${pending} 项任务未完成。`
  if (rate < 35 && daysRemaining <= 5) return `当前进度偏慢，剩余 ${daysRemaining} 天需优先完成 ${pending} 项任务。`
  return `今日完成 ${overview.value.summary.todayCompletedTaskCount || 0}/${overview.value.summary.todayTaskCount || 0} 项，剩余 ${pending} 项计划任务。`
})

const focusTags = computed(() =>
  String(form.focusKnowledgePoints || '')
    .split(/[,，]/)
    .map((item) => item.trim())
    .filter(Boolean)
)

const weekTimeline = computed(() => {
  const taskMap = new Map()
  ;[...(overview.value.todayTasks || []), ...(overview.value.upcomingTasks || [])].forEach((task) => {
    const key = formatDateLabel(task.taskDate)
    if (!taskMap.has(key)) taskMap.set(key, [])
    taskMap.get(key).push(task)
  })
  return Array.from({ length: 7 }).map((_, index) => {
    const date = addDays(new Date(), index)
    const key = formatDate(date)
    const tasks = taskMap.get(key) || []
    const completed = tasks.filter((item) => item.status === 'COMPLETED').length
    return {
      key,
      day: ['日', '一', '二', '三', '四', '五', '六'][date.getDay()],
      label: `${date.getMonth() + 1}/${date.getDate()}`,
      tasks,
      completed,
      percent: tasks.length ? Math.round((completed / tasks.length) * 100) : 0,
      today: index === 0
    }
  })
})

function formatDate(value) {
  const date = value instanceof Date ? value : new Date(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDateLabel(value) {
  return value ? String(value).slice(0, 10) : '暂无'
}

function addDays(value, amount) {
  const date = new Date(value)
  date.setDate(date.getDate() + amount)
  return date
}

function percentValue(value) {
  return Math.max(0, Math.min(100, Math.round(Number(value || 0))))
}

function taskTone(task) {
  if (task.status === 'COMPLETED') return 'done'
  if (task.taskType === 'PRACTICE') return 'practice'
  if (task.taskType === 'EXAM' || task.taskType === 'TEST_PAPER') return 'exam'
  return 'learn'
}

function taskActionPath(task) {
  if (task.actionPath) return task.actionPath
  if (task.taskType === 'PRACTICE') return '/search'
  if (task.taskType === 'EXAM') return '/exam'
  if (task.taskType === 'TEST_PAPER') return '/user/test-paper'
  return '/my-course'
}

function masteryTone(item) {
  const score = Number(item.masteryScore || 0)
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'exception'
}

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/user-center/plans/current')
    overview.value = data
    if (data.plan) {
      Object.assign(form, {
        targetExamType: data.plan.targetExamType || '省考',
        subjectName: data.plan.subjectName || '行测',
        dailyStudyMinutes: data.plan.dailyStudyMinutes || 60,
        dailyQuestionCount: data.plan.dailyQuestionCount || 15,
        weeklyExamCount: data.plan.weeklyExamCount ?? 1,
        durationDays: data.plan.durationDays || 14,
        startDate: data.plan.startDate ? String(data.plan.startDate).slice(0, 10) : formatDate(new Date()),
        focusKnowledgePoints: (data.plan.focusKnowledgePoints || []).join(',')
      })
    }
  } finally {
    loading.value = false
  }
}

async function savePlan() {
  saving.value = true
  try {
    await request.post('/api/user-center/plans', form)
    ElMessage.success('学习计划已生成，系统已关联知识点与题库并刷新每日任务')
    planDialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="plan-page" v-loading="loading">
    <section class="glass-card plan-hero">
      <div class="hero-copy">
        <el-tag :type="planStatus.type" effect="dark">{{ planStatus.label }}</el-tag>
        <div class="section-title">学习计划</div>
        <p>{{ planStatus.desc }} {{ rhythmTip }}</p>
        <div class="plan-meta">
          <span>{{ overview.plan?.targetExamType || form.targetExamType }}</span>
          <span>{{ overview.plan?.subjectName || form.subjectName }}</span>
          <span>{{ formatDateLabel(overview.plan?.startDate || form.startDate) }} 至 {{ formatDateLabel(overview.plan?.endDate) }}</span>
        </div>
      </div>
      <div class="hero-progress">
        <el-progress type="dashboard" :percentage="percentValue(overview.summary.completionRate)" :stroke-width="12" />
        <strong>总体完成率</strong>
        <span>今日 {{ overview.summary.todayCompletedTaskCount || 0 }}/{{ overview.summary.todayTaskCount || 0 }} 项</span>
      </div>
      <div class="hero-actions">
        <el-button type="primary" @click="planDialogVisible = true">新建计划</el-button>
        <el-button plain @click="router.push('/user/diagnosis')">查看阶段诊断</el-button>
      </div>
    </section>

    <section class="summary-grid">
      <article class="metric-card">
        <span>剩余天数</span>
        <strong>{{ overview.summary.daysRemaining || 0 }}</strong>
        <p>{{ planStatus.label }}</p>
      </article>
      <article class="metric-card">
        <span>计划任务</span>
        <strong>{{ overview.summary.totalTaskCount || 0 }}</strong>
        <p>已完成 {{ overview.summary.completedTaskCount || 0 }} 项</p>
      </article>
      <article class="metric-card">
        <span>今日进度</span>
        <strong>{{ todayProgress }}%</strong>
        <p>{{ overview.summary.todayTaskCount || 0 }} 项今日任务</p>
      </article>
      <article class="metric-card">
        <span>重点知识点</span>
        <strong>{{ overview.summary.focusKnowledgeCount || focusTags.length }}</strong>
        <p>用于生成测卷和补强任务</p>
      </article>
    </section>

    <section class="glass-card automation-card">
      <div>
        <div class="section-title small">计划生成逻辑</div>
        <p>点击“新建计划”配置目标后保存，系统会自动匹配对应知识点、题库题目和模考节奏，并生成每日学习任务。</p>
      </div>
      <div class="automation-flow">
        <span>考试目标</span>
        <span>知识点关联</span>
        <span>题库匹配</span>
        <span>每日任务</span>
      </div>
      <el-button type="primary" plain @click="planDialogVisible = true">新建计划</el-button>
    </section>

    <section class="workbench-grid">
      <div class="glass-card panel-card today-panel">
        <div class="panel-head">
          <div>
            <div class="section-title small">今日任务</div>
            <p>{{ rhythmTip }}</p>
          </div>
          <el-progress type="circle" :width="74" :percentage="todayProgress" />
        </div>
        <div class="task-list" v-if="overview.todayTasks?.length">
          <article v-for="item in overview.todayTasks" :key="item.id" class="task-item" :class="`task-${taskTone(item)}`">
            <div class="task-main">
              <span>{{ item.taskTypeLabel }}</span>
              <strong>{{ item.title }}</strong>
              <p>{{ item.description || `${item.completedValue}/${item.targetValue} ${item.unitLabel}` }}</p>
              <el-progress :percentage="percentValue(item.progressPercent)" :stroke-width="8" />
            </div>
            <div class="task-side">
              <el-tag :type="item.status === 'COMPLETED' ? 'success' : 'warning'" effect="plain">{{ item.statusLabel }}</el-tag>
              <strong>{{ item.completedValue || 0 }}/{{ item.targetValue || 0 }}</strong>
              <el-button plain @click="router.push(taskActionPath(item))">{{ item.actionText }}</el-button>
            </div>
          </article>
        </div>
        <el-empty v-else description="保存计划后会自动生成今日任务">
          <el-button type="primary" @click="planDialogVisible = true">新建计划</el-button>
        </el-empty>
      </div>

      <div class="glass-card panel-card weak-panel">
        <div class="panel-head">
          <div>
            <div class="section-title small">薄弱知识点优先级</div>
            <p>按掌握度和待复盘错题排序。</p>
          </div>
          <el-button text type="primary" @click="router.push('/user/knowledge')">查看全部</el-button>
        </div>
        <div class="weak-list" v-if="overview.weakKnowledgePoints?.length">
          <article v-for="item in overview.weakKnowledgePoints" :key="item.id" class="weak-item">
            <div class="weak-main">
              <div class="weak-title">
                <strong>{{ item.knowledgePoint }}</strong>
                <span>{{ item.masteryScore || 0 }} 分</span>
              </div>
              <p>{{ item.subjectName }} · {{ item.masteryLevelLabel }} · 待复盘 {{ item.pendingWrongCount || 0 }} 题</p>
              <el-progress :percentage="percentValue(item.masteryScore)" :status="masteryTone(item)" :stroke-width="8" />
              <small>{{ item.diagnosis }}</small>
            </div>
            <div class="weak-actions">
              <el-button plain @click="router.push(item.practicePath || '/question-bank')">专项刷题</el-button>
              <el-button type="primary" plain @click="router.push({ path: item.testPaperPath || '/user/test-paper', query: { knowledge: item.knowledgePoint, subject: item.subjectName } })">生成测卷</el-button>
            </div>
          </article>
        </div>
        <el-empty v-else description="当前暂无薄弱知识点数据">
          <el-button plain @click="router.push('/user/test-paper')">先做一套测卷</el-button>
        </el-empty>
      </div>
    </section>

    <section class="glass-card panel-card">
      <div class="panel-head">
        <div>
          <div class="section-title small">7 天节奏</div>
          <p>快速查看每天任务密度和完成情况。</p>
        </div>
        <el-button plain @click="router.push('/user/task')">每日任务中心</el-button>
      </div>
      <div class="week-grid">
        <article v-for="day in weekTimeline" :key="day.key" class="day-card" :class="{ today: day.today }">
          <span>周{{ day.day }}</span>
          <strong>{{ day.label }}</strong>
          <el-progress :percentage="day.percent" :stroke-width="7" :show-text="false" />
          <p>{{ day.completed }}/{{ day.tasks.length }} 项</p>
        </article>
      </div>
    </section>

    <section class="glass-card panel-card">
      <div class="panel-head">
        <div>
          <div class="section-title small">后续任务安排</div>
          <p>未来任务按日期排序，方便提前安排时间。</p>
        </div>
      </div>
      <div class="upcoming-list" v-if="overview.upcomingTasks?.length">
        <article v-for="item in overview.upcomingTasks" :key="item.id" class="upcoming-item">
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ formatDateLabel(item.taskDate) }} · {{ item.taskTypeLabel }} · {{ item.statusLabel }}</p>
          </div>
          <div class="upcoming-side">
            <span>{{ item.targetValue }} {{ item.unitLabel }}</span>
            <el-button text type="primary" @click="router.push(taskActionPath(item))">{{ item.actionText }}</el-button>
          </div>
        </article>
      </div>
      <el-empty v-else description="还没有生成后续任务" />
    </section>

    <el-dialog
      v-model="planDialogVisible"
      title="新建学习计划"
      width="860px"
      class="plan-dialog"
      destroy-on-close
    >
      <div class="dialog-tip">
        保存后系统会自动关联对应知识点和题库，生成每日学习、刷题、模考和测卷任务。
      </div>
      <el-form label-position="top">
        <div class="config-sections dialog-config">
          <div class="config-group">
            <h3>考试目标</h3>
            <el-form-item label="目标考试">
              <el-select v-model="form.targetExamType">
                <el-option label="国考" value="国考" />
                <el-option label="省考" value="省考" />
                <el-option label="事业编" value="事业编" />
                <el-option label="通用" value="通用" />
              </el-select>
            </el-form-item>
            <el-form-item label="主攻科目">
              <el-input v-model="form.subjectName" />
            </el-form-item>
          </div>
          <div class="config-group">
            <h3>每日强度</h3>
            <el-form-item label="每日学习时长(分钟)">
              <el-input-number v-model="form.dailyStudyMinutes" :min="10" :max="600" style="width: 100%" />
            </el-form-item>
            <el-form-item label="每日刷题量(题)">
              <el-input-number v-model="form.dailyQuestionCount" :min="5" :max="200" style="width: 100%" />
            </el-form-item>
          </div>
          <div class="config-group">
            <h3>周期安排</h3>
            <el-form-item label="每周模考次数">
              <el-input-number v-model="form.weeklyExamCount" :min="0" :max="7" style="width: 100%" />
            </el-form-item>
            <el-form-item label="计划周期(天)">
              <el-input-number v-model="form.durationDays" :min="7" :max="180" style="width: 100%" />
            </el-form-item>
            <el-form-item label="开始日期">
              <el-date-picker v-model="form.startDate" value-format="YYYY-MM-DD" type="date" style="width: 100%" />
            </el-form-item>
          </div>
          <div class="config-group config-group-wide">
            <h3>重点知识点</h3>
            <el-form-item label="知识点">
              <el-input v-model="form.focusKnowledgePoints" placeholder="使用逗号分隔，如：言语理解,资料分析" />
            </el-form-item>
            <div class="focus-tags">
              <el-tag v-for="tag in focusTags" :key="tag" round>{{ tag }}</el-tag>
            </div>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="planDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="savePlan">保存并生成任务</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.plan-page {
  display: grid;
  gap: 20px;
}

.plan-hero,
.automation-card,
.panel-card {
  padding: 28px 32px;
  box-sizing: border-box;
}

.plan-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 170px auto;
  gap: 24px;
  align-items: center;
}

.hero-copy {
  display: grid;
  gap: 12px;
  min-width: 0;
  padding-left: 2px;
}

.hero-copy .section-title {
  line-height: 1.25;
  overflow: visible;
}

.hero-copy p,
.panel-head p,
.metric-card p,
.task-item p,
.weak-item p,
.upcoming-item p {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
}

.plan-meta,
.focus-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.plan-meta span {
  border-radius: 999px;
  padding: 6px 12px;
  background: rgba(37, 99, 235, 0.08);
  color: #1d4ed8;
  font-weight: 700;
}

.hero-progress {
  display: grid;
  justify-items: center;
  gap: 6px;
}

.hero-progress strong {
  color: #0f172a;
}

.hero-progress span {
  color: var(--muted);
  font-size: 13px;
}

.hero-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.automation-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: 22px;
  align-items: center;
}

.automation-card p,
.dialog-tip {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
}

.automation-flow {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.automation-flow span {
  border-radius: 999px;
  padding: 7px 12px;
  background: rgba(37, 99, 235, 0.08);
  color: #1d4ed8;
  font-weight: 700;
  white-space: nowrap;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 16px;
}

.metric-card,
.task-item,
.weak-item,
.upcoming-item,
.day-card,
.config-group {
  border: 1px solid var(--border);
  border-radius: 16px;
  background: #fff;
}

.metric-card {
  padding: 18px;
}

.metric-card span {
  color: var(--muted);
}

.metric-card strong {
  display: block;
  margin-top: 8px;
  font-size: 30px;
  color: var(--primary);
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.section-title.small {
  font-size: 22px;
}

.config-sections {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.dialog-config {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 16px;
}

.config-group {
  padding: 16px;
}

.config-group h3 {
  margin: 0 0 14px;
  font-size: 17px;
}

.config-group-wide {
  display: grid;
  align-content: start;
}

.workbench-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(360px, 0.9fr);
  gap: 20px;
}

.task-list,
.weak-list,
.upcoming-list {
  display: grid;
  gap: 12px;
}

.task-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 150px;
  gap: 18px;
  padding: 16px;
  border-left: 4px solid #2563eb;
}

.task-done {
  border-left-color: #16a34a;
  background: #f8fff9;
}

.task-practice {
  border-left-color: #2563eb;
}

.task-exam {
  border-left-color: #f97316;
}

.task-learn {
  border-left-color: #0f766e;
}

.task-main {
  display: grid;
  gap: 8px;
}

.task-main span {
  width: fit-content;
  border-radius: 999px;
  padding: 4px 10px;
  background: rgba(37, 99, 235, 0.08);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.task-main strong,
.weak-title strong,
.upcoming-item strong {
  color: #0f172a;
}

.task-side,
.upcoming-side {
  display: grid;
  justify-items: end;
  align-content: center;
  gap: 10px;
}

.task-side strong,
.upcoming-side span {
  color: var(--primary);
  font-weight: 800;
}

.weak-item {
  display: grid;
  gap: 14px;
  padding: 16px;
}

.weak-main {
  display: grid;
  gap: 8px;
}

.weak-title {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.weak-title span {
  color: var(--primary);
  font-weight: 800;
}

.weak-main small {
  color: var(--muted);
  line-height: 1.6;
}

.weak-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.week-grid {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 12px;
}

.day-card {
  padding: 14px;
  display: grid;
  gap: 10px;
}

.day-card.today {
  border-color: rgba(37, 99, 235, 0.45);
  background: rgba(37, 99, 235, 0.05);
}

.day-card span,
.day-card p {
  color: var(--muted);
  margin: 0;
}

.day-card strong {
  color: #0f172a;
  font-size: 18px;
}

.upcoming-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 16px;
}

@media (max-width: 1200px) {
  .plan-hero,
  .workbench-grid,
  .automation-card {
    grid-template-columns: 1fr;
  }

  .hero-progress {
    justify-items: start;
  }

  .hero-actions {
    justify-content: flex-start;
  }

  .config-sections {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .week-grid {
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  }
}

@media (max-width: 700px) {
  .config-sections,
  .dialog-config,
  .task-item {
    grid-template-columns: 1fr;
  }

  .task-side,
  .upcoming-side {
    justify-items: start;
  }

  .upcoming-item,
  .panel-head {
    display: grid;
  }
}
</style>
