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
  subjectName: ''
})

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '暂无'
}

async function loadData() {
  loading.value = true
  try {
    payload.value = await request.get('/api/user-center/test-paper-reports', { params: query })
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNo = 1
  loadData()
}

function handleCurrentChange(pageNo) {
  query.pageNo = pageNo
  loadData()
}

onMounted(loadData)
</script>

<template>
  <div class="report-center-page" v-loading="loading">
    <section class="glass-card summary-card">
      <div class="page-head">
        <div>
          <div class="section-title">测卷报告聚合</div>
          <p>把已经完成的个性化测卷统一收口到一个结果中心，方便持续回看表现变化和高频薄弱项。</p>
        </div>
        <el-button type="primary" plain @click="router.push('/user/test-paper')">再生成一套</el-button>
      </div>

      <div class="summary-grid">
        <div><span>已完成测卷</span><strong>{{ payload.summary.completedCount || 0 }}</strong></div>
        <div><span>平均准确率</span><strong>{{ payload.summary.averageAccuracyRate || 0 }}%</strong></div>
        <div><span>高分测卷</span><strong>{{ payload.summary.highScoreCount || 0 }}</strong></div>
        <div><span>最近提交</span><strong class="small-text">{{ formatDateTime(payload.summary.latestSubmittedAt) }}</strong></div>
      </div>
    </section>

    <section class="glass-card filter-card">
      <div class="filter-row">
        <el-input v-model="query.subjectName" placeholder="按科目筛选，如 行测 / 申论" clearable />
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>
    </section>

    <section class="glass-card table-card">
      <el-table class="desktop-table" :data="payload.page.records">
        <el-table-column prop="title" label="测卷标题" min-width="220" />
        <el-table-column prop="subjectName" label="科目" min-width="120" />
        <el-table-column prop="generateModeLabel" label="生成方式" min-width="120" />
        <el-table-column label="知识点" min-width="220">
          <template #default="{ row }">
            {{ (row.knowledgePoints || []).join(' / ') || '系统推荐' }}
          </template>
        </el-table-column>
        <el-table-column prop="attemptCount" label="作答次数" min-width="100" />
        <el-table-column prop="latestAccuracyRate" label="最近准确率" min-width="120" />
        <el-table-column prop="latestSubmittedAt" label="最近提交时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.latestSubmittedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="180">
          <template #default="{ row }">
            <el-button v-if="row.reportPath" type="warning" plain @click="router.push(row.reportPath)">查看报告</el-button>
            <el-button type="primary" plain @click="router.push('/user/test-paper')">再生成</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="mobile-report-list">
        <article v-for="row in payload.page.records" :key="row.id || row.title" class="mobile-report-card">
          <div>
            <strong>{{ row.title }}</strong>
            <span>{{ row.subjectName || '综合' }} · {{ row.generateModeLabel || '系统生成' }}</span>
          </div>
          <p>{{ (row.knowledgePoints || []).join(' / ') || '系统推荐' }}</p>
          <div class="mobile-report-meta">
            <span>作答 {{ row.attemptCount || 0 }} 次</span>
            <span>最近准确率 {{ row.latestAccuracyRate || 0 }}%</span>
            <span>{{ formatDateTime(row.latestSubmittedAt) }}</span>
          </div>
          <div class="mobile-report-actions">
            <el-button v-if="row.reportPath" type="warning" plain @click="router.push(row.reportPath)">查看报告</el-button>
            <el-button type="primary" plain @click="router.push('/user/test-paper')">再生成</el-button>
          </div>
        </article>
      </div>

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="query.pageNo"
          :page-size="query.pageSize"
          :total="payload.page.total || 0"
          @current-change="handleCurrentChange"
        />
      </div>
    </section>
  </div>
</template>

<style scoped>
.report-center-page {
  display: grid;
  gap: 20px;
}

.summary-card,
.filter-card,
.table-card {
  padding: 24px;
}

.page-head,
.filter-row {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.page-head p {
  color: var(--muted);
}

.summary-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  margin-top: 18px;
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

.small-text {
  font-size: 14px !important;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.mobile-report-list {
  display: none;
}

.mobile-report-card {
  display: grid;
  gap: 10px;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
  background: #fff;
}

.mobile-report-card strong,
.mobile-report-card span {
  display: block;
}

.mobile-report-card span,
.mobile-report-card p,
.mobile-report-meta {
  color: var(--muted);
  font-size: 13px;
}

.mobile-report-card p {
  margin: 0;
  line-height: 1.6;
}

.mobile-report-meta,
.mobile-report-actions {
  display: grid;
  gap: 8px;
}

.mobile-report-actions {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.mobile-report-actions :deep(.el-button) {
  margin-left: 0;
}

@media (max-width: 640px) {
  .summary-card,
  .filter-card,
  .table-card {
    padding: 18px;
  }

  .filter-row,
  .filter-row :deep(.el-input),
  .filter-row :deep(.el-button) {
    width: 100%;
  }

  .desktop-table {
    display: none;
  }

  .mobile-report-list {
    display: grid;
    gap: 12px;
  }

  .pagination-wrap {
    justify-content: center;
  }
}
</style>
