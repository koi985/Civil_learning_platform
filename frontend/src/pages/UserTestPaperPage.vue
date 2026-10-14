<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const generating = ref(false)
const payload = ref({
  summary: {},
  page: { records: [], total: 0, pageNo: 1, pageSize: 10 }
})
const selectOptions = ref({
  subjectNames: [],
  categoryNames: [],
  difficultyLevels: [],
  knowledgePoints: []
})
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  subjectName: '',
  status: ''
})
const form = reactive({
  subjectName: '',
  categoryName: '',
  difficultyLevel: '',
  knowledgePoint: '',
  generateMode: 'WEAKNESS',
  paperType: 'WEAKNESS',
  onlyWrong: false,
  questionCount: 5,
  excludeRecentDays: 7
})

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '暂无'
}

function sortTextValues(values) {
  return Array.from(new Set(values.filter(Boolean))).sort((left, right) => String(left).localeCompare(String(right), 'zh-Hans-CN'))
}

function extractKnowledgePoints(records) {
  return records.flatMap((item) => String(item.knowledgePoints || '')
    .split(/[，,]/)
    .map((point) => point.trim())
    .filter(Boolean))
}

async function loadData() {
  loading.value = true
  try {
    payload.value = await request.get('/api/user-center/test-papers', { params: query })
  } finally {
    loading.value = false
  }
}

async function loadSelectOptions() {
  const data = await request.get('/api/questions', {
    params: {
      pageNo: 1,
      pageSize: 100
    }
  })
  const records = data.records || []
  selectOptions.value = {
    subjectNames: sortTextValues(records.map((item) => item.subjectName)),
    categoryNames: sortTextValues(records.map((item) => item.categoryName)),
    difficultyLevels: sortTextValues(records.map((item) => item.difficultyLevel)),
    knowledgePoints: sortTextValues(extractKnowledgePoints(records))
  }
}

async function generatePaper() {
  generating.value = true
  try {
    const data = await request.post('/api/user-center/test-papers/generate', form)
    ElMessage.success(`测卷已生成：${data.title}`)
    await loadData()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    generating.value = false
  }
}

async function startPaper(id) {
  const data = await request.post(`/api/user-center/test-papers/${id}/start`)
  ElMessage.success('测卷已生成练习，开始作答')
  router.push(`/practice/${data.practiceId}`)
}

function syncQueryFromRoute() {
  if (route.query.knowledge) {
    form.knowledgePoint = String(route.query.knowledge)
    form.generateMode = 'CUSTOM'
  }
  if (route.query.paperType) {
    form.paperType = String(route.query.paperType)
  }
  if (route.query.subject) {
    form.subjectName = String(route.query.subject)
    query.subjectName = String(route.query.subject)
  }
}

watch(() => route.fullPath, syncQueryFromRoute)

onMounted(async () => {
  syncQueryFromRoute()
  await Promise.all([loadData(), loadSelectOptions()])
})
</script>

<template>
  <div class="paper-page" v-loading="loading">
    <section class="glass-card summary-card">
      <div class="summary-head">
        <div class="section-title">个性化测卷</div>
        <el-button type="warning" plain @click="router.push('/user/test-paper-report')">报告中心</el-button>
      </div>
      <div class="summary-grid">
        <div><span>测卷总数</span><strong>{{ payload.summary.paperCount || 0 }}</strong></div>
        <div><span>已完成测卷</span><strong>{{ payload.summary.completedCount || 0 }}</strong></div>
        <div><span>平均得分</span><strong>{{ payload.summary.averageScore || 0 }}</strong></div>
      </div>
    </section>

    <section class="glass-card form-card">
      <div class="section-title small">生成新测卷</div>
      <div class="form-grid">
        <el-select v-model="form.subjectName" clearable filterable placeholder="主攻科目">
          <el-option v-for="item in selectOptions.subjectNames" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="form.categoryName" clearable filterable placeholder="分类">
          <el-option v-for="item in selectOptions.categoryNames" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="form.difficultyLevel" clearable placeholder="难度">
          <el-option v-for="item in selectOptions.difficultyLevels" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="form.knowledgePoint" clearable filterable placeholder="知识点">
          <el-option v-for="item in selectOptions.knowledgePoints" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="form.generateMode">
          <el-option label="薄弱项推荐" value="WEAKNESS" />
          <el-option label="计划驱动" value="PLAN" />
          <el-option label="自定义筛选" value="CUSTOM" />
        </el-select>
        <el-select v-model="form.paperType">
          <el-option label="补弱卷" value="WEAKNESS" />
          <el-option label="诊断卷" value="DIAGNOSIS" />
          <el-option label="巩固卷" value="CONSOLIDATION" />
          <el-option label="混合卷" value="MIXED" />
        </el-select>
        <el-input-number v-model="form.questionCount" :min="3" :max="20" style="width: 100%" />
        <el-input-number v-model="form.excludeRecentDays" :min="0" :max="30" style="width: 100%" />
      </div>
      <div class="action-row">
        <el-checkbox v-model="form.onlyWrong">只从错题中生成</el-checkbox>
        <el-button type="primary" :loading="generating" @click="generatePaper">生成测卷</el-button>
      </div>
    </section>

    <section class="glass-card table-card">
      <div class="toolbar">
        <el-select v-model="query.subjectName" clearable filterable placeholder="按科目筛选">
          <el-option v-for="item in selectOptions.subjectNames" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select v-model="query.status" clearable placeholder="测卷状态">
          <el-option label="未开始" value="GENERATED" />
          <el-option label="进行中" value="IN_PROGRESS" />
          <el-option label="已完成" value="COMPLETED" />
        </el-select>
        <el-button type="primary" @click="loadData">刷新列表</el-button>
      </div>

      <div class="paper-list" v-if="payload.page.records?.length">
        <article v-for="item in payload.page.records" :key="item.id" class="paper-item">
          <div class="paper-main">
            <div class="paper-head">
              <strong>{{ item.title }}</strong>
              <el-tag>{{ item.paperTypeLabel || item.generateModeLabel }}</el-tag>
            </div>
            <p>{{ item.recommendationReason }}</p>
            <div class="meta-line">
              <span>{{ item.subjectName || '综合' }}</span>
              <span>{{ item.questionCount }} 题</span>
              <span>{{ item.status }}</span>
            </div>
            <div class="meta-line">
              <span>目标知识点：{{ (item.targetKnowledgePoints || item.knowledgePoints || []).join(' / ') || '系统自动推荐' }}</span>
              <span>难度分布：{{ item.difficultyDistribution || '自动' }}</span>
              <span>生成时间：{{ formatDateTime(item.createdAt) }}</span>
            </div>
            <div class="meta-line" v-if="item.latestScore !== null">
              <span>最近得分：{{ item.latestScore }}</span>
              <span>最近提交：{{ formatDateTime(item.latestSubmittedAt) }}</span>
            </div>
          </div>
          <div class="paper-actions">
            <el-button type="primary" plain @click="startPaper(item.id)">开始练习</el-button>
            <el-button v-if="item.reportPath" type="warning" plain @click="router.push(item.reportPath)">查看报告</el-button>
          </div>
        </article>
      </div>
      <el-empty v-else description="还没有生成个性化测卷" />

      <el-pagination
        background
        layout="prev, pager, next"
        :total="payload.page.total || 0"
        :page-size="query.pageSize"
        :current-page="query.pageNo"
        style="margin-top: 24px; justify-content: center"
        @current-change="(page) => { query.pageNo = page; loadData() }"
      />
    </section>
  </div>
</template>

<style scoped>
.paper-page {
  display: grid;
  gap: 20px;
}

.summary-card,
.form-card,
.table-card {
  padding: 24px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 14px;
}

.summary-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.summary-grid div,
.paper-item {
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 16px;
  background: #fff;
}

.summary-grid span,
.paper-item p,
.meta-line {
  color: var(--muted);
}

.summary-grid strong {
  display: block;
  margin-top: 8px;
  font-size: 26px;
  color: var(--primary);
}

.section-title.small {
  font-size: 22px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}

.action-row,
.toolbar,
.paper-head,
.meta-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.action-row,
.toolbar {
  margin-top: 16px;
}

.paper-list {
  display: grid;
  gap: 14px;
}

.paper-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.paper-main,
.paper-actions {
  display: grid;
  gap: 10px;
}

.paper-actions {
  align-content: center;
}
</style>
