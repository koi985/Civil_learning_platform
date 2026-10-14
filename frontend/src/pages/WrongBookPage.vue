<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { CircleCheck, DataAnalysis, Delete, Reading, RefreshLeft, Search, WarningFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import request from '../utils/request'
import UserCenterBackButton from '../components/UserCenterBackButton.vue'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const expandedIds = ref(new Set())
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  subjectName: '',
  categoryName: ''
})
const viewFilter = ref('ALL')
const sortBy = ref('LAST_WRONG')

const sortedList = computed(() => {
  const records = [...list.value]
  if (sortBy.value === 'WRONG_COUNT') {
    records.sort((left, right) => Number(right.wrongCount || 0) - Number(left.wrongCount || 0))
  }
  if (sortBy.value === 'DIFFICULTY') {
    const order = { '较难': 3, '中等': 2, '基础': 1 }
    records.sort((left, right) => Number(order[right.difficultyLevel] || 0) - Number(order[left.difficultyLevel] || 0))
  }
  return records
})

const visibleList = computed(() => {
  if (viewFilter.value === 'HIGH') {
    return sortedList.value.filter(item => Number(item.wrongCount || 0) >= 6)
  }
  if (viewFilter.value === 'MEDIUM') {
    return sortedList.value.filter(item => Number(item.wrongCount || 0) >= 3 && Number(item.wrongCount || 0) < 6)
  }
  if (viewFilter.value === 'LOW') {
    return sortedList.value.filter(item => Number(item.wrongCount || 0) < 3)
  }
  return sortedList.value
})

const summaryCards = computed(() => {
  const pageWrongTotal = list.value.reduce((sum, item) => sum + Number(item.wrongCount || 0), 0)
  const highCount = list.value.filter(item => Number(item.wrongCount || 0) >= 6).length
  const avgWrong = list.value.length ? (pageWrongTotal / list.value.length).toFixed(1) : '0.0'
  const latest = list.value
    .map(item => item.lastWrongAt)
    .filter(Boolean)
    .sort()
    .at(-1)
  return [
    { label: '错题总数', value: total.value, desc: '当前筛选范围', type: 'primary', icon: Reading },
    { label: '本页高频', value: highCount, desc: '错误 6 次以上', type: 'danger', icon: WarningFilled },
    { label: '平均错误', value: avgWrong, desc: '本页错题均值', type: 'warning', icon: DataAnalysis },
    { label: '最近沉淀', value: formatDate(latest), desc: '最近错误时间', type: 'success', icon: CircleCheck }
  ]
})

function formatDate(value) {
  if (!value) {
    return '暂无'
  }
  return String(value).replace('T', ' ').slice(0, 10)
}

function priorityMeta(item) {
  const count = Number(item.wrongCount || 0)
  if (count >= 6) {
    return { label: '重点攻克', className: 'high', tagType: 'danger' }
  }
  if (count >= 3) {
    return { label: '持续巩固', className: 'medium', tagType: 'warning' }
  }
  return { label: '常规复盘', className: 'low', tagType: 'primary' }
}

function questionTypeLabel(value) {
  const map = {
    SINGLE: '单选',
    MULTI: '多选',
    JUDGE: '判断',
    SHORT_ANSWER: '主观题'
  }
  return map[value] || value || '题型'
}

function answerText(value) {
  return value || '未记录'
}

function toggleAnalysis(id) {
  const next = new Set(expandedIds.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  expandedIds.value = next
}

function isExpanded(id) {
  return expandedIds.value.has(id)
}

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/wrong-questions', { params: query })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNo = 1
  loadData()
}

function resetFilters() {
  query.pageNo = 1
  query.subjectName = ''
  query.categoryName = ''
  viewFilter.value = 'ALL'
  sortBy.value = 'LAST_WRONG'
  loadData()
}

async function removeQuestion(questionId) {
  await ElMessageBox.confirm('确认将这道题移出错题本？', '移除错题', {
    type: 'warning',
    confirmButtonText: '移除',
    cancelButtonText: '取消'
  })
  await request.delete(`/api/wrong-questions/${questionId}`)
  ElMessage.success('已移出错题本')
  loadData()
}

async function restartWrongPractice() {
  const data = await request.post('/api/wrong-questions/practice')
  ElMessage.success(`已生成 ${data.questionCount || ''} 道错题练习`)
  router.push(`/practice/${data.practiceId}`)
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell wrong-book-page">
    <section class="glass-card wrong-workbench">
      <div class="page-head">
        <div>
          <div class="section-title">错题本</div>
          <p>按错误频次、科目与分类快速定位薄弱题，优先处理高频错题。</p>
        </div>
        <div class="page-actions">
          <UserCenterBackButton />
          <el-button type="warning" :icon="RefreshLeft" @click="restartWrongPractice">错题再练</el-button>
        </div>
      </div>

      <div class="summary-grid">
        <div v-for="item in summaryCards" :key="item.label" class="summary-card" :class="item.type">
          <div class="summary-icon">
            <el-icon><component :is="item.icon" /></el-icon>
          </div>
          <div>
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.desc }}</small>
          </div>
        </div>
      </div>

      <div class="tool-panel">
        <div class="filter-grid">
          <el-input v-model="query.subjectName" placeholder="科目" clearable @keyup.enter="handleSearch" />
          <el-input v-model="query.categoryName" placeholder="分类" clearable @keyup.enter="handleSearch" />
          <el-select v-model="sortBy" placeholder="排序方式">
            <el-option label="最近错误优先" value="LAST_WRONG" />
            <el-option label="错误次数优先" value="WRONG_COUNT" />
            <el-option label="难度优先" value="DIFFICULTY" />
          </el-select>
          <div class="filter-actions">
            <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
            <el-button @click="resetFilters">重置</el-button>
          </div>
        </div>
        <el-segmented
          v-model="viewFilter"
          :options="[
            { label: '全部', value: 'ALL' },
            { label: '重点攻克', value: 'HIGH' },
            { label: '持续巩固', value: 'MEDIUM' },
            { label: '常规复盘', value: 'LOW' }
          ]"
        />
      </div>
    </section>

    <section class="wrong-list" v-loading="loading">
      <article
        v-for="item in visibleList"
        :key="item.id"
        class="glass-card wrong-card"
        :class="priorityMeta(item).className"
      >
        <div class="wrong-main">
          <div class="wrong-head">
            <div class="wrong-title-block">
              <div class="tag-row">
                <el-tag :type="priorityMeta(item).tagType" effect="light">{{ priorityMeta(item).label }}</el-tag>
                <el-tag type="info" effect="plain">{{ item.subjectName }} · {{ item.categoryName }}</el-tag>
                <el-tag effect="plain">{{ questionTypeLabel(item.questionType) }}</el-tag>
                <el-tag v-if="item.difficultyLevel" type="warning" effect="plain">{{ item.difficultyLevel }}</el-tag>
              </div>
              <h3>{{ item.stem }}</h3>
              <div class="wrong-meta">
                <span>错误 {{ item.wrongCount || 0 }} 次</span>
                <span>最近错误 {{ formatDate(item.lastWrongAt) }}</span>
                <span v-if="item.knowledgePoints">{{ item.knowledgePoints }}</span>
              </div>
            </div>
            <div class="wrong-count">
              <strong>{{ item.wrongCount || 0 }}</strong>
              <span>次</span>
            </div>
          </div>

          <div class="answer-grid">
            <div class="answer-box wrong-answer">
              <span>最近错误</span>
              <strong>{{ answerText(item.latestAnswer) }}</strong>
            </div>
            <div class="answer-box correct-answer">
              <span>正确答案</span>
              <strong>{{ answerText(item.correctAnswer) }}</strong>
            </div>
            <div class="answer-box source-box">
              <span>来源</span>
              <strong>{{ item.sourceName || item.province || '题库练习' }}</strong>
            </div>
          </div>

          <div v-if="isExpanded(item.id)" class="analysis-box">
            <span>解析</span>
            <p>{{ item.analysis || '暂无解析' }}</p>
          </div>
        </div>

        <div class="card-actions">
          <el-button type="primary" plain :icon="Reading" @click="toggleAnalysis(item.id)">
            {{ isExpanded(item.id) ? '收起解析' : '查看解析' }}
          </el-button>
          <el-button type="warning" plain :icon="RefreshLeft" @click="restartWrongPractice">生成练习</el-button>
          <el-button text type="danger" :icon="Delete" @click="removeQuestion(item.id)">移除</el-button>
        </div>
      </article>

      <el-empty v-if="!loading && !visibleList.length" description="当前筛选下暂无错题" />

      <div class="bottom-bar">
        <span>按当前错题本生成练习，系统会优先抽取最近沉淀的错题。</span>
        <el-button type="warning" :icon="RefreshLeft" @click="restartWrongPractice">生成错题练习</el-button>
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
  </div>
</template>

<style scoped>
.wrong-book-page {
  display: grid;
  gap: 24px;
}

.wrong-workbench,
.wrong-card {
  padding: 24px;
}

.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.page-head .section-title {
  margin-bottom: 10px;
}

.page-head p {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
}

.page-actions,
.filter-actions,
.card-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-top: 22px;
}

.summary-card {
  display: grid;
  grid-template-columns: 42px 1fr;
  gap: 12px;
  align-items: center;
  min-width: 0;
  min-height: 102px;
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 14px;
  background: #fff;
}

.summary-card > div:last-child {
  min-width: 0;
}

.summary-icon {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  color: var(--primary);
  background: rgba(37, 99, 235, 0.1);
  font-size: 20px;
}

.summary-card span,
.summary-card small {
  display: block;
  color: var(--muted);
}

.summary-card strong {
  display: block;
  margin: 4px 0;
  color: #0f172a;
  font-size: 26px;
  line-height: 1;
}

.summary-card.danger .summary-icon {
  color: #dc2626;
  background: rgba(220, 38, 38, 0.1);
}

.summary-card.warning .summary-icon {
  color: #d97706;
  background: rgba(245, 158, 11, 0.13);
}

.summary-card.success .summary-icon {
  color: #059669;
  background: rgba(5, 150, 105, 0.1);
}

.tool-panel {
  display: grid;
  gap: 14px;
  margin-top: 22px;
}

.filter-grid {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(180px, 1fr) 220px auto;
  gap: 14px;
  align-items: center;
}

.wrong-list {
  display: grid;
  gap: 16px;
}

.wrong-card {
  position: relative;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 20px;
  overflow: hidden;
  border-left: 6px solid #3b82f6;
}

.wrong-card.medium {
  border-left-color: #f59e0b;
}

.wrong-card.high {
  border-left-color: #ef4444;
}

.wrong-main {
  min-width: 0;
}

.wrong-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
}

.wrong-title-block {
  min-width: 0;
}

.tag-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.wrong-card h3 {
  margin: 14px 0 10px;
  color: #0f172a;
  font-size: 20px;
  line-height: 1.45;
  font-weight: 800;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.wrong-meta {
  display: grid;
  grid-template-columns: repeat(3, max-content);
  gap: 8px 12px;
  color: var(--muted);
  font-size: 14px;
}

.wrong-meta span {
  min-width: 0;
}

.wrong-count {
  width: 68px;
  height: 68px;
  flex: 0 0 68px;
  display: grid;
  place-items: center;
  align-content: center;
  border-radius: 14px;
  color: #dc2626;
  background: #fff1f2;
}

.wrong-count strong {
  font-size: 26px;
  line-height: 1;
}

.wrong-count span {
  margin-top: 4px;
  font-size: 12px;
}

.answer-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.answer-box {
  min-height: 76px;
  border-radius: 12px;
  padding: 12px;
  background: #f8fafc;
}

.answer-box span,
.analysis-box span {
  display: block;
  color: var(--muted);
  font-size: 13px;
  font-weight: 700;
}

.answer-box strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 18px;
  line-height: 1.3;
}

.wrong-answer {
  background: #fff7ed;
}

.wrong-answer strong {
  color: #c2410c;
}

.correct-answer {
  background: #ecfdf5;
}

.correct-answer strong {
  color: #047857;
}

.analysis-box {
  margin-top: 14px;
  border-radius: 12px;
  padding: 14px;
  background: #f8fafc;
}

.analysis-box p {
  margin: 8px 0 0;
  color: #475569;
  line-height: 1.7;
}

.card-actions {
  flex-direction: column;
  align-items: stretch;
  min-width: 112px;
}

.card-actions .el-button {
  margin-left: 0;
}

.bottom-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  border: 1px solid rgba(245, 158, 11, 0.28);
  border-radius: 14px;
  padding: 14px 16px;
  color: #92400e;
  background: #fffbeb;
}

.pager {
  justify-content: center;
}

@media (max-width: 960px) {
  .summary-grid,
  .filter-grid,
  .answer-grid {
    grid-template-columns: 1fr 1fr;
  }

  .filter-actions {
    justify-content: flex-start;
  }

  .wrong-card {
    grid-template-columns: 1fr;
  }

  .card-actions {
    flex-direction: row;
    justify-content: flex-start;
  }

  .wrong-meta {
    grid-template-columns: repeat(2, minmax(0, max-content));
  }
}

@media (max-width: 640px) {
  .wrong-workbench,
  .wrong-card {
    padding: 18px;
  }

  .page-head,
  .bottom-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .page-actions {
    justify-content: flex-start;
  }

  .summary-grid,
  .filter-grid,
  .answer-grid {
    grid-template-columns: 1fr;
  }

  .summary-grid {
    gap: 10px;
  }

  .summary-card {
    grid-template-columns: 38px 1fr;
    min-height: auto;
    padding: 12px;
    border-radius: 12px;
    box-shadow: 0 8px 22px rgba(15, 23, 42, 0.04);
  }

  .summary-icon {
    width: 38px;
    height: 38px;
    border-radius: 9px;
    font-size: 18px;
  }

  .summary-card strong {
    font-size: 22px;
  }

  .wrong-head {
    flex-direction: column;
    gap: 12px;
  }

  .wrong-count {
    width: 100%;
    height: auto;
    grid-template-columns: auto auto;
    justify-content: center;
    gap: 6px;
    padding: 10px;
  }

  .wrong-meta {
    grid-template-columns: 1fr;
    gap: 4px;
  }

  .answer-grid {
    gap: 8px;
    margin-top: 14px;
  }

  .answer-box {
    min-height: auto;
    padding: 10px 12px;
  }

  .answer-box strong {
    margin-top: 4px;
    font-size: 16px;
  }

  .card-actions {
    gap: 8px;
  }

  .wrong-card h3 {
    font-size: 18px;
  }
}
</style>
