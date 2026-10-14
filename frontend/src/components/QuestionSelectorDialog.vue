<script setup>
import { nextTick, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import request from '../utils/request'
import { getQuestionTypeLabel } from '../utils/question'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const visible = ref(false)
const loading = ref(false)
const randomLoading = ref(false)
const list = ref([])
const total = ref(0)
const tableRef = ref()
const selectedMap = ref(new Map())
const query = reactive({
  pageNo: 1,
  pageSize: 8,
  keyword: '',
  province: '',
  subjectName: '',
  categoryName: '',
  questionType: '',
  difficultyLevel: ''
})

watch(
  () => props.modelValue,
  (value) => {
    selectedMap.value = new Map((value || []).map((id) => [id, { id }]))
  },
  { immediate: true }
)

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/questions', { params: query })
    list.value = data.records
    total.value = data.total
    await nextTick()
    syncSelection()
  } finally {
    loading.value = false
  }
}

function syncSelection() {
  if (!tableRef.value) {
    return
  }
  tableRef.value.clearSelection()
  list.value.forEach((item) => {
    if (selectedMap.value.has(item.id)) {
      tableRef.value.toggleRowSelection(item, true)
    }
  })
}

function open() {
  visible.value = true
  query.pageNo = 1
  loadData()
}

function handleSelectionChange(rows) {
  const currentPageIds = new Set(list.value.map((item) => item.id))
  currentPageIds.forEach((id) => selectedMap.value.delete(id))
  rows.forEach((item) => selectedMap.value.set(item.id, item))
}

function removeSelected(id) {
  selectedMap.value.delete(id)
  syncSelection()
}

async function randomSelectQuestions() {
  try {
    if (selectedMap.value.size) {
      await ElMessageBox.confirm('随机抽题会替换当前已选题目，确认继续吗？', '随机抽取 120 道', { type: 'warning' })
    }
    randomLoading.value = true
    const questions = await request.get('/api/questions/random', { params: { count: 120 } })
    selectedMap.value = new Map(questions.map((item) => [item.id, item]))
    await nextTick()
    syncSelection()
    ElMessage.success('已随机抽取 120 道题')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '随机抽题失败')
    }
  } finally {
    randomLoading.value = false
  }
}

function confirmSelection() {
  const selected = Array.from(selectedMap.value.values())
  emit('update:modelValue', selected.map((item) => item.id))
  emit('confirm', selected)
  visible.value = false
}

defineExpose({ open })
</script>

<template>
  <el-dialog v-model="visible" title="从题库选择题目" width="min(1280px, 96vw)" class="question-selector-dialog">
    <div class="selector-shell">
      <section class="selector-side">
        <div class="selector-head">
          <span>已选题目</span>
          <div class="selector-head-actions">
            <strong>{{ selectedMap.size }} 道</strong>
            <el-button size="small" type="primary" plain :loading="randomLoading" @click="randomSelectQuestions">随机 120 道</el-button>
          </div>
        </div>
        <div v-if="selectedMap.size" class="selected-list">
          <div v-for="item in Array.from(selectedMap.values())" :key="item.id" class="selected-item">
            <span>#{{ item.id }} {{ item.stem || '' }}</span>
            <el-button link type="danger" @click="removeSelected(item.id)">移除</el-button>
          </div>
        </div>
        <el-empty v-else description="暂无已选题目" />
      </section>

      <section class="selector-main">
        <div class="filter-grid">
          <el-input v-model="query.keyword" placeholder="关键词" clearable />
          <el-input v-model="query.subjectName" placeholder="科目" clearable />
          <el-input v-model="query.categoryName" placeholder="专项" clearable />
          <el-select v-model="query.questionType" placeholder="题型" clearable>
            <el-option label="单选题" value="SINGLE" />
            <el-option label="多选题" value="MULTI" />
          </el-select>
          <el-input v-model="query.province" placeholder="省份" clearable />
          <el-input v-model="query.difficultyLevel" placeholder="难度" clearable />
          <div class="filter-actions">
            <el-button type="primary" @click="query.pageNo = 1; loadData()">查询</el-button>
          </div>
        </div>

        <el-table ref="tableRef" v-loading="loading" :data="list" height="420" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="42" />
          <el-table-column prop="id" label="ID" width="64" />
          <el-table-column prop="subjectName" label="科目" width="90" />
          <el-table-column prop="categoryName" label="专项" width="120" />
          <el-table-column label="题型" width="90">
            <template #default="{ row }">
              {{ getQuestionTypeLabel(row.questionType) }}
            </template>
          </el-table-column>
          <el-table-column prop="difficultyLevel" label="难度" width="76" />
          <el-table-column prop="stem" label="题干" min-width="300" show-overflow-tooltip />
        </el-table>

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
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button type="primary" plain :loading="randomLoading" @click="randomSelectQuestions">随机抽取 120 道</el-button>
        <div>
          <el-button @click="visible = false">取消</el-button>
          <el-button type="primary" @click="confirmSelection">确认选择</el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.selector-shell {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 20px;
  min-height: 588px;
  max-width: 100%;
  overflow: hidden;
}

.selector-side,
.selector-main {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 18px;
  background: #fff;
}

.selector-side {
  display: flex;
  flex-direction: column;
  min-height: 588px;
}

.selector-main {
  display: grid;
  grid-template-rows: auto 1fr auto;
  min-width: 0;
  overflow: hidden;
}

.selector-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  font-weight: 700;
}

.selector-head-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.selector-head strong {
  color: #2563eb;
}

.selected-list {
  display: grid;
  align-content: start;
  gap: 10px;
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.selected-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 10px 12px;
  border-radius: 12px;
  background: rgba(29, 78, 216, 0.06);
}

.selected-item span {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr)) 96px;
  gap: 12px;
  margin-bottom: 16px;
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
}

.filter-actions .el-button {
  width: 96px;
}

.selector-main :deep(.el-table) {
  border-top: 1px solid #edf2f7;
}

.selector-main :deep(.el-table__header th) {
  background: #f8fafc;
  color: #334155;
  font-weight: 700;
}

.selector-main :deep(.el-pagination) {
  margin-top: 16px;
}

.selector-side :deep(.el-empty) {
  flex: 1;
  min-height: 0;
}

:deep(.question-selector-dialog .el-dialog__body) {
  padding-top: 8px;
}

:deep(.question-selector-dialog .el-dialog__footer) {
  padding-top: 18px;
}

.dialog-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

:deep(.question-selector-dialog) {
  max-width: 96vw;
}

:deep(.question-selector-dialog .el-dialog__body) {
  overflow-x: hidden;
}

@media (max-width: 900px) {
  .selector-shell {
    grid-template-columns: 1fr;
    min-height: 0;
  }

  .filter-grid {
    grid-template-columns: 1fr;
  }

  .selector-side {
    min-height: 260px;
  }

  .filter-actions,
  .filter-actions .el-button {
    width: 100%;
  }
}
</style>
