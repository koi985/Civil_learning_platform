<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import request from '../utils/request'
import QuestionSelectorDialog from '../components/QuestionSelectorDialog.vue'
import { getQuestionTypeLabel } from '../utils/question'

const RANDOM_EXAM_COUNT = 120

const loading = ref(false)
const saving = ref(false)
const randomLoading = ref(false)
const visible = ref(false)
const list = ref([])
const total = ref(0)
const selectorRef = ref()
const selectedQuestionRows = ref([])
const composeMode = ref('CUSTOM')
const query = reactive({ pageNo: 1, pageSize: 10, keyword: '' })
const randomQuery = reactive({
  province: '',
  subjectName: '行测',
  questionType: '',
  difficultyLevel: ''
})
const form = reactive({
  id: null,
  title: '',
  intro: '',
  province: '全国',
  examType: '国考',
  seasonName: '第一季',
  subjectName: '行测',
  durationMinutes: 120,
  totalScore: 100,
  status: 'OPEN',
  rankingEnabled: true,
  startTime: '',
  endTime: '',
  questionIds: []
})

const selectedQuestionCount = computed(() => form.questionIds.length)
const previewRows = computed(() => {
  if (selectedQuestionRows.value.length) {
    return selectedQuestionRows.value
  }
  return form.questionIds.map((id) => ({ id, stem: '已关联题目，重新选择后可查看题干' }))
})

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/admin/exams', { params: query })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: null,
    title: '',
    intro: '',
    province: '全国',
    examType: '国考',
    seasonName: '第一季',
    subjectName: '行测',
    durationMinutes: 120,
    totalScore: 100,
    status: 'OPEN',
    rankingEnabled: true,
    startTime: '',
    endTime: '',
    questionIds: []
  })
  Object.assign(randomQuery, {
    province: '',
    subjectName: '行测',
    questionType: '',
    difficultyLevel: ''
  })
  composeMode.value = 'CUSTOM'
  selectedQuestionRows.value = []
}

function openEdit(row) {
  resetForm()
  if (row) {
    Object.assign(form, {
      ...row,
      questionIds: row.questionIds || []
    })
    composeMode.value = 'CUSTOM'
  }
  visible.value = true
}

function handleSelectedQuestions(rows) {
  selectedQuestionRows.value = rows
}

async function randomPickQuestions() {
  randomLoading.value = true
  try {
    const rows = await request.get('/api/admin/questions/random', {
      params: {
        count: RANDOM_EXAM_COUNT,
        province: randomQuery.province,
        subjectName: randomQuery.subjectName,
        questionType: randomQuery.questionType,
        difficultyLevel: randomQuery.difficultyLevel
      }
    })
    selectedQuestionRows.value = rows
    form.questionIds = rows.map((item) => item.id)
    ElMessage.success(`已随机抽取 ${rows.length} 道题`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    randomLoading.value = false
  }
}

function removeSelectedQuestion(id) {
  form.questionIds = form.questionIds.filter((item) => item !== id)
  selectedQuestionRows.value = selectedQuestionRows.value.filter((item) => item.id !== id)
}

function clearQuestions() {
  form.questionIds = []
  selectedQuestionRows.value = []
}

async function save() {
  if (!form.title.trim()) {
    ElMessage.warning('请输入模考名称')
    return
  }
  if (!form.questionIds.length) {
    ElMessage.warning('请先选择题目或随机抽题')
    return
  }
  saving.value = true
  try {
    await request.post('/api/admin/exams', {
      ...form,
      questionIds: Array.from(new Set(form.questionIds))
    })
    ElMessage.success('保存成功')
    visible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

async function remove(id) {
  try {
    await ElMessageBox.confirm('删除后该模考的题目关联也会同步移除，确定删除？', '删除模考', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await request.delete(`/api/admin/exams/${id}`)
    ElMessage.success('删除成功')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除已取消')
    }
  }
}

onMounted(loadData)
</script>

<template>
  <div class="admin-page">
    <div class="toolbar glass-card">
      <el-input v-model="query.keyword" placeholder="搜索模考名称" clearable />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="warning" @click="openEdit()">新增模考</el-button>
    </div>

    <div class="glass-card table-card" v-loading="loading">
      <el-table :data="list">
        <el-table-column prop="title" label="模考名称" min-width="220" />
        <el-table-column prop="province" label="省份" width="110" />
        <el-table-column prop="examType" label="考试类型" width="120" />
        <el-table-column prop="subjectName" label="科目" width="100" />
        <el-table-column prop="questionCount" label="题量" width="90" />
        <el-table-column prop="durationMinutes" label="时长" width="90">
          <template #default="{ row }">{{ row.durationMinutes }} 分钟</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'OPEN' ? 'success' : 'info'">{{ row.status === 'OPEN' ? '开放' : '关闭' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click="openEdit(scope.row)">编辑</el-button>
            <el-button type="danger" link @click="remove(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.pageNo"
        style="margin-top: 20px; justify-content: center"
        @current-change="(page) => { query.pageNo = page; loadData() }"
      />
    </div>

    <el-dialog v-model="visible" :title="form.id ? '编辑模考' : '新增模考'" width="980px" class="exam-dialog">
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="模考名称"><el-input v-model="form.title" placeholder="例如：2026 国考行测全真模考" /></el-form-item>
          <el-form-item label="省份"><el-input v-model="form.province" /></el-form-item>
          <el-form-item label="考试类型"><el-input v-model="form.examType" /></el-form-item>
          <el-form-item label="季数"><el-input v-model="form.seasonName" /></el-form-item>
          <el-form-item label="科目"><el-input v-model="form.subjectName" /></el-form-item>
          <el-form-item label="时长"><el-input-number v-model="form.durationMinutes" :min="1" style="width: 100%" /></el-form-item>
          <el-form-item label="总分"><el-input-number v-model="form.totalScore" :min="1" style="width: 100%" /></el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="开放" value="OPEN" />
              <el-option label="关闭" value="CLOSED" />
            </el-select>
          </el-form-item>
          <el-form-item label="开始时间">
            <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="可不填" style="width: 100%" />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="可不填" style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item label="模考简介"><el-input v-model="form.intro" type="textarea" :rows="3" /></el-form-item>

        <section class="compose-card">
          <div class="compose-head">
            <div>
              <strong>组卷方式</strong>
              <p>当前已选 {{ selectedQuestionCount }} 道题，随机组卷固定抽取 120 道。</p>
            </div>
            <el-segmented v-model="composeMode" :options="[{ label: '自选题目', value: 'CUSTOM' }, { label: '随机抽题', value: 'RANDOM' }]" />
          </div>

          <div v-if="composeMode === 'CUSTOM'" class="picker-bar">
            <div>
              <strong>从题库自选题目</strong>
              <p>支持跨页勾选，确认后写入当前模考题目列表。</p>
            </div>
            <el-button type="primary" plain @click="selectorRef.open()">打开题库选择</el-button>
          </div>

          <div v-else class="random-panel">
            <div class="random-grid">
              <el-input v-model="randomQuery.province" placeholder="省份，不填为不限" clearable />
              <el-input v-model="randomQuery.subjectName" placeholder="科目" clearable />
              <el-select v-model="randomQuery.questionType" placeholder="题型，不限" clearable>
                <el-option label="单选题" value="SINGLE" />
                <el-option label="多选题" value="MULTI" />
                <el-option label="判断题" value="JUDGE" />
                <el-option label="简答题" value="SHORT_ANSWER" />
              </el-select>
              <el-input v-model="randomQuery.difficultyLevel" placeholder="难度，不填为不限" clearable />
            </div>
            <div class="picker-bar">
              <div>
                <strong>随机抽取 120 道题</strong>
                <p>系统会按筛选条件从启用题目中随机抽取，题量不足时会提示放宽条件。</p>
              </div>
              <el-button type="warning" :loading="randomLoading" @click="randomPickQuestions">随机生成</el-button>
            </div>
          </div>
        </section>

        <section class="selected-card">
          <div class="selected-head">
            <strong>题目清单</strong>
            <el-button link type="danger" :disabled="!selectedQuestionCount" @click="clearQuestions">清空</el-button>
          </div>
          <el-table :data="previewRows" height="260">
            <el-table-column type="index" label="#" width="56" />
            <el-table-column prop="id" label="题目 ID" width="90" />
            <el-table-column prop="subjectName" label="科目" width="100" />
            <el-table-column prop="categoryName" label="专项" width="130" />
            <el-table-column label="题型" width="100">
              <template #default="{ row }">{{ getQuestionTypeLabel(row.questionType) }}</template>
            </el-table-column>
            <el-table-column prop="difficultyLevel" label="难度" width="90" />
            <el-table-column prop="stem" label="题干" min-width="260" show-overflow-tooltip />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button link type="danger" @click="removeSelectedQuestion(row.id)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存模考</el-button>
      </template>
    </el-dialog>

    <QuestionSelectorDialog ref="selectorRef" v-model="form.questionIds" @confirm="handleSelectedQuestions" />
  </div>
</template>

<style scoped>
.admin-page {
  display: grid;
  gap: 20px;
}

.toolbar,
.table-card {
  padding: 20px;
}

.toolbar {
  display: flex;
  gap: 12px;
}

.toolbar .el-input {
  max-width: 320px;
}

.form-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.compose-card,
.selected-card {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 16px;
  background: #fff;
}

.compose-card {
  display: grid;
  gap: 16px;
  margin-bottom: 16px;
}

.compose-head,
.picker-bar,
.selected-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.compose-head p,
.picker-bar p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.random-panel {
  display: grid;
  gap: 14px;
}

.random-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

@media (max-width: 900px) {
  .form-grid,
  .random-grid {
    grid-template-columns: 1fr;
  }

  .compose-head,
  .picker-bar,
  .selected-head,
  .toolbar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
