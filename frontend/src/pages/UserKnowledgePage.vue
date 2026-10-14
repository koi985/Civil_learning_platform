<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'

const router = useRouter()
const loading = ref(false)
const payload = ref({
  summary: {},
  page: { records: [], total: 0, pageNo: 1, pageSize: 10 }
})
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  subjectName: '',
  keyword: '',
  masteryLevel: ''
})

function tagType(level) {
  if (level === 'MASTERED') return 'success'
  if (level === 'IMPROVING') return 'warning'
  if (level === 'FOUNDATION') return 'info'
  return 'danger'
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '暂无'
}

async function loadData() {
  loading.value = true
  try {
    payload.value = await request.get('/api/user-center/knowledge-mastery', { params: query })
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="knowledge-page" v-loading="loading">
    <section class="glass-card summary-card">
      <div class="section-title">知识点掌握</div>
      <div class="summary-grid">
        <div><span>知识点总数</span><strong>{{ payload.summary.knowledgeCount || 0 }}</strong></div>
        <div><span>重点补强</span><strong>{{ payload.summary.weakKnowledgeCount || 0 }}</strong></div>
        <div><span>掌握稳定</span><strong>{{ payload.summary.masteredCount || 0 }}</strong></div>
        <div><span>平均掌握度</span><strong>{{ payload.summary.averageMasteryScore || 0 }}</strong></div>
      </div>
    </section>

    <section class="glass-card table-card">
      <div class="toolbar">
        <el-input v-model="query.subjectName" placeholder="科目" clearable />
        <el-input v-model="query.keyword" placeholder="搜索知识点" clearable />
        <el-select v-model="query.masteryLevel" clearable placeholder="掌握层级">
          <el-option label="待诊断" value="TO_DIAGNOSE" />
          <el-option label="重点补强" value="WEAK" />
          <el-option label="基础建立" value="FOUNDATION" />
          <el-option label="持续提升" value="IMPROVING" />
          <el-option label="掌握稳定" value="MASTERED" />
        </el-select>
        <el-button type="primary" @click="loadData">刷新数据</el-button>
      </div>

      <el-table :data="payload.page.records">
        <el-table-column prop="subjectName" label="科目" />
        <el-table-column prop="knowledgePoint" label="知识点" min-width="180" />
        <el-table-column label="掌握层级" min-width="120">
          <template #default="scope">
            <el-tag :type="scope.row.knowledgeStatus === 'TO_DIAGNOSE' ? 'info' : tagType(scope.row.masteryLevel)">{{ scope.row.knowledgeStatusLabel || scope.row.masteryLevelLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="masteryScore" label="掌握度" />
        <el-table-column prop="masteryConfidence" label="置信度" />
        <el-table-column prop="priorityScore" label="优先级" />
        <el-table-column prop="accuracyRate" label="正确率" />
        <el-table-column prop="attemptCount" label="练习次数" />
        <el-table-column prop="pendingWrongCount" label="待消化错题" />
        <el-table-column label="最近练习" min-width="160">
          <template #default="scope">{{ formatDateTime(scope.row.lastPracticedAt) }}</template>
        </el-table-column>
        <el-table-column label="建议" min-width="260">
          <template #default="scope">{{ scope.row.diagnosis }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="180">
          <template #default="scope">
            <el-button type="primary" link @click="router.push('/question-bank')">去刷题</el-button>
            <el-button
              type="warning"
              link
              @click="router.push({ path: '/user/test-paper', query: { knowledge: scope.row.knowledgePoint, subject: scope.row.subjectName, paperType: scope.row.knowledgeStatus === 'TO_DIAGNOSE' ? 'DIAGNOSIS' : 'WEAKNESS' } })"
            >
              生成测卷
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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
.knowledge-page {
  display: grid;
  gap: 20px;
  min-width: 0;
}

.summary-card,
.table-card {
  min-width: 0;
  padding: 24px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 14px;
}

.summary-grid div {
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 16px;
  background: #fff;
}

.summary-grid span {
  color: var(--muted);
}

.summary-grid strong {
  display: block;
  margin-top: 8px;
  font-size: 26px;
  color: var(--primary);
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar :deep(.el-input),
.toolbar :deep(.el-select) {
  max-width: 220px;
}

.table-card :deep(.el-table) {
  display: block;
  max-width: 100%;
  overflow-x: auto;
}

.table-card :deep(.el-table__inner-wrapper) {
  width: max-content;
  min-width: 100%;
}
</style>
