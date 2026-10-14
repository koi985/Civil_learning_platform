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
  taskDate: formatDate(new Date()),
  status: '',
  taskType: ''
})

function formatDate(value) {
  const date = value instanceof Date ? value : new Date(value)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function tagType(status) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'IN_PROGRESS') return 'warning'
  return 'info'
}

async function loadData() {
  loading.value = true
  try {
    payload.value = await request.get('/api/user-center/tasks', { params: query })
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="task-page" v-loading="loading">
    <section class="glass-card summary-card">
      <div class="section-title">每日任务</div>
      <div class="summary-grid">
        <div><span>总任务数</span><strong>{{ payload.summary.totalTaskCount || 0 }}</strong></div>
        <div><span>已完成</span><strong>{{ payload.summary.completedTaskCount || 0 }}</strong></div>
        <div><span>今日打卡</span><strong>{{ payload.summary.todayCheckInCount || 0 }}</strong></div>
        <div><span>今日学习分钟</span><strong>{{ payload.summary.todayLearnMinutes || 0 }}</strong></div>
        <div><span>今日刷题量</span><strong>{{ payload.summary.todayPracticeQuestions || 0 }}</strong></div>
      </div>
    </section>

    <section class="glass-card table-card">
      <div class="toolbar">
        <el-date-picker v-model="query.taskDate" value-format="YYYY-MM-DD" type="date" style="max-width: 180px" />
        <el-select v-model="query.status" clearable placeholder="任务状态">
          <el-option label="待完成" value="PENDING" />
          <el-option label="进行中" value="IN_PROGRESS" />
          <el-option label="已完成" value="COMPLETED" />
        </el-select>
        <el-select v-model="query.taskType" clearable placeholder="任务类型">
          <el-option label="每日打卡" value="CHECK_IN" />
          <el-option label="学习时长" value="LEARN" />
          <el-option label="专项刷题" value="PRACTICE" />
          <el-option label="模考检验" value="EXAM" />
          <el-option label="个性化测卷" value="TEST_PAPER" />
        </el-select>
        <el-button type="primary" @click="loadData">刷新任务</el-button>
      </div>

      <div class="task-list" v-if="payload.page.records?.length">
        <article v-for="item in payload.page.records" :key="item.id" class="task-item">
          <div class="task-main">
            <div class="task-head">
              <strong>{{ item.title }}</strong>
              <el-tag :type="tagType(item.status)">{{ item.statusLabel }}</el-tag>
            </div>
            <p>{{ item.description }}</p>
            <el-progress :percentage="Number(item.progressPercent || 0)" :stroke-width="8" />
            <div class="meta-line">
              <span>{{ item.taskDate }}</span>
              <span>{{ item.completedValue }}/{{ item.targetValue }} {{ item.unitLabel }}</span>
            </div>
          </div>
          <el-button plain @click="router.push(item.actionPath)">{{ item.actionText }}</el-button>
        </article>
      </div>
      <el-empty v-else description="当前筛选条件下暂无任务" />

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
.task-page {
  display: grid;
  gap: 20px;
}

.summary-card,
.table-card {
  padding: 24px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 14px;
}

.summary-grid div,
.task-item {
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 16px;
  background: #fff;
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

.task-list {
  display: grid;
  gap: 14px;
}

.task-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.task-main {
  flex: 1;
  display: grid;
  gap: 10px;
}

.task-head,
.meta-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.task-main p,
.meta-line,
.summary-grid span {
  color: var(--muted);
}
</style>
