<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import request from '../utils/request'
import { getQuestionTypeLabel, normalizeAnswerValue, parseAnswerValue } from '../utils/question'

const loading = ref(false)
const paperLoading = ref(false)
const groupLoading = ref(false)
const visible = ref(false)
const paperVisible = ref(false)
const groupVisible = ref(false)

const list = ref([])
const paperList = ref([])
const groupList = ref([])
const total = ref(0)
const paperTotal = ref(0)
const groupTotal = ref(0)

const query = reactive({ pageNo: 1, pageSize: 10, keyword: '' })
const paperQuery = reactive({ pageNo: 1, pageSize: 10, keyword: '' })
const groupQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: '',
  status: '',
  year: null,
  region: '',
  sourceName: ''
})

const questionTypeOptions = [
  { label: '单选题', value: 'SINGLE' },
  { label: '多选题', value: 'MULTI' },
  { label: '简答题', value: 'SHORT_ANSWER' }
]

const shenlunDifficultyOptions = ['基础', '中等', '较难']
const shenlunStatusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已归档', value: 'ARCHIVED' }
]

const shenlunMaterialModeOptions = [
  { label: '组共享材料', value: 'GROUP_SHARED' }
]

function createDefaultOptions(count = 4) {
  return Array.from({ length: count }, (_, index) => ({
    optionKey: String.fromCharCode(65 + index),
    optionContent: ''
  }))
}

function createGroupMaterial(index = 0) {
  return {
    id: null,
    sortNo: index + 1,
    materialLabel: `材料${index + 1}`,
    title: '',
    content: ''
  }
}

function createGroupQuestion(index = 0) {
  return {
    id: null,
    sortNo: index + 1,
    categoryName: '',
    questionType: 'SHORT_ANSWER',
    difficultyLevel: '中等',
    score: 20,
    stem: '',
    analysis: '',
    answerValue: '',
    knowledgePoints: '',
    materialRefs: index === 0 ? ['1'] : []
  }
}

const form = reactive({
  id: null,
  subjectName: '行测',
  categoryName: '言语理解与表达',
  province: '全国',
  questionType: 'SINGLE',
  difficultyLevel: '中等',
  sourceName: '平台自建',
  material: '',
  stem: '',
  analysis: '',
  answerText: 'A',
  answerValues: ['A'],
  knowledgePoints: '',
  enabled: true,
  options: createDefaultOptions()
})

const paperForm = reactive({
  id: null,
  title: '',
  province: '全国',
  subjectName: '行测',
  durationMinutes: 120,
  totalScore: 120,
  status: 'PUBLISHED'
})

const groupForm = reactive({
  id: null,
  title: '',
  year: new Date().getFullYear(),
  region: '全国',
  paperType: '副省级',
  sourceName: '平台录入',
  materialMode: 'GROUP_SHARED',
  status: 'DRAFT',
  materials: [createGroupMaterial(0)],
  questions: [createGroupQuestion(0)]
})

const selectedSingleAnswer = computed({
  get: () => form.answerValues[0] || '',
  set: (value) => {
    form.answerValues = value ? [value] : []
  }
})

const selectedMultiAnswers = computed({
  get: () => form.answerValues,
  set: (value) => {
    form.answerValues = parseAnswerValue(value)
  }
})

const groupMaterialRefOptions = computed(() => groupForm.materials.map((item, index) => ({
  label: item.materialLabel || `材料${index + 1}`,
  value: String(index + 1)
})))

function isGeneratedShenlunPaper(row) {
  return !!row?.source && row.source.startsWith('shenlun-group:')
}

function formatPublishStatus(value) {
  if (value === 2 || value === 'ARCHIVED') {
    return '已归档'
  }
  return value === 1 || value === 'PUBLISHED' ? '已发布' : '草稿'
}

function isArchivedShenlunGroup(row) {
  return row?.status === 'ARCHIVED' || row?.status === 2
}

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/admin/questions', { params: query })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadPapers() {
  paperLoading.value = true
  try {
    const data = await request.get('/api/admin/question-papers', { params: paperQuery })
    paperList.value = data.records
    paperTotal.value = data.total
  } finally {
    paperLoading.value = false
  }
}

async function loadGroups() {
  groupLoading.value = true
  try {
    const params = { ...groupQuery }
    if (!params.status) {
      delete params.status
    }
    if (!params.region) {
      delete params.region
    }
    if (!params.sourceName) {
      delete params.sourceName
    }
    if (!params.year) {
      delete params.year
    }
    const data = await request.get('/api/admin/shenlun-groups', { params })
    groupList.value = data.records
    groupTotal.value = data.total
  } finally {
    groupLoading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: null,
    subjectName: '行测',
    categoryName: '言语理解与表达',
    province: '全国',
    questionType: 'SINGLE',
    difficultyLevel: '中等',
    sourceName: '平台自建',
    material: '',
    stem: '',
    analysis: '',
    answerText: 'A',
    answerValues: ['A'],
    knowledgePoints: '',
    enabled: true,
    options: createDefaultOptions()
  })
}

function resetGroupForm() {
  Object.assign(groupForm, {
    id: null,
    title: '',
    year: new Date().getFullYear(),
    region: '全国',
    paperType: '副省级',
    sourceName: '平台录入',
    materialMode: 'GROUP_SHARED',
    status: 'DRAFT',
    materials: [createGroupMaterial(0)],
    questions: [createGroupQuestion(0)]
  })
}

function resequenceOptions() {
  const optionKeys = form.options.map((_, index) => String.fromCharCode(65 + index))
  form.options.forEach((item, index) => {
    item.optionKey = optionKeys[index]
  })
  form.answerValues = form.answerValues.filter((item) => optionKeys.includes(item))
  if (!form.answerValues.length && optionKeys.length) {
    form.answerValues = [optionKeys[0]]
  }
  if (form.questionType !== 'MULTI' && form.answerValues.length > 1) {
    form.answerValues = [form.answerValues[0]]
  }
}

function resequenceGroupMaterials() {
  groupForm.materials.forEach((item, index) => {
    item.sortNo = index + 1
    if (!item.materialLabel) {
      item.materialLabel = `材料${index + 1}`
    }
  })
}

function resequenceGroupQuestions() {
  groupForm.questions.forEach((item, index) => {
    item.sortNo = index + 1
    item.questionType = 'SHORT_ANSWER'
  })
}

function normalizeQuestionMaterialRefs() {
  const validRefs = new Set(groupForm.materials.map((_, index) => String(index + 1)))
  groupForm.questions.forEach((question) => {
    const normalized = Array.from(new Set((question.materialRefs || [])
      .map((item) => String(item))
      .filter((item) => validRefs.has(item))))
    question.materialRefs = normalized
  })
}

function openPaperEdit(row) {
  if (row && isGeneratedShenlunPaper(row)) {
    ElMessage.warning('申论自动生成试卷请在材料组管理中编辑')
    return
  }
  Object.assign(paperForm, {
    id: row?.id || null,
    title: row?.title || '',
    province: row?.province || row?.region || '全国',
    subjectName: row?.subjectName || row?.subject || '行测',
    durationMinutes: row?.durationMinutes || row?.duration || 120,
    totalScore: row?.totalScore || 120,
    status: row?.status === 2 || row?.status === 'ARCHIVED'
      ? 'ARCHIVED'
      : (row?.status === 0 || row?.status === 'DRAFT' ? 'DRAFT' : 'PUBLISHED')
  })
  paperVisible.value = true
}

function openEdit(row) {
  if (row) {
    Object.assign(form, {
      id: row.id,
      subjectName: row.subjectName,
      categoryName: row.categoryName,
      province: row.province,
      questionType: row.questionType || 'SINGLE',
      difficultyLevel: row.difficultyLevel,
      sourceName: row.sourceName,
      material: row.material || '',
      stem: row.stem,
      analysis: row.analysis,
      answerText: row.answerValue || '',
      answerValues: parseAnswerValue(row.answerValue || ''),
      knowledgePoints: row.knowledgePoints,
      enabled: row.enabled,
      options: (row.options || []).map((item) => ({
        optionKey: item.optionKey,
        optionContent: item.optionContent
      }))
    })
    resequenceOptions()
  } else {
    resetForm()
  }
  visible.value = true
}

async function openGroupEdit(row) {
  if (!row?.id) {
    resetGroupForm()
    groupVisible.value = true
    return
  }
  try {
    const data = await request.get(`/api/admin/shenlun-groups/${row.id}`)
    const group = data.group || {}
    Object.assign(groupForm, {
      id: group.id || null,
      title: group.title || '',
      year: group.year || new Date().getFullYear(),
      region: group.region || '全国',
      paperType: group.paperType || '副省级',
      sourceName: group.sourceName || '平台录入',
      materialMode: group.materialMode || 'GROUP_SHARED',
      status: group.status || 'DRAFT',
      materials: (group.materials || []).map((item, index) => ({
        id: item.id || null,
        sortNo: item.sortNo || index + 1,
        materialLabel: item.materialLabel || `材料${index + 1}`,
        title: item.title || '',
        content: item.content || ''
      })),
      questions: (group.questions || []).map((item, index) => ({
        id: item.id || null,
        sortNo: item.sortNo || index + 1,
        categoryName: item.categoryName || '',
        questionType: 'SHORT_ANSWER',
        difficultyLevel: item.difficultyLevel || '中等',
        score: item.score || 20,
        stem: item.stem || '',
        analysis: item.analysis || '',
        answerValue: item.answerValue || '',
        knowledgePoints: item.knowledgePoints || '',
        materialRefs: (item.materialRefs || []).map((ref) => String(ref))
      }))
    })
    if (!groupForm.materials.length) {
      groupForm.materials = [createGroupMaterial(0)]
    }
    if (!groupForm.questions.length) {
      groupForm.questions = [createGroupQuestion(0)]
    }
    resequenceGroupMaterials()
    resequenceGroupQuestions()
    normalizeQuestionMaterialRefs()
    groupVisible.value = true
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function addOption() {
  form.options.push({ optionKey: '', optionContent: '' })
  resequenceOptions()
}

function removeOption(index) {
  if (form.options.length <= 2) {
    ElMessage.warning('客观题至少保留两个选项')
    return
  }
  form.options.splice(index, 1)
  resequenceOptions()
}

function addGroupMaterial() {
  groupForm.materials.push(createGroupMaterial(groupForm.materials.length))
  resequenceGroupMaterials()
}

function removeGroupMaterial(index) {
  if (groupForm.materials.length <= 1) {
    ElMessage.warning('至少保留一份材料')
    return
  }
  const removedRef = String(index + 1)
  groupForm.materials.splice(index, 1)
  groupForm.questions.forEach((question) => {
    question.materialRefs = (question.materialRefs || [])
      .map((item) => String(item))
      .filter((item) => item !== removedRef)
      .map((item) => {
        const value = Number(item)
        return value > Number(removedRef) ? String(value - 1) : item
      })
  })
  resequenceGroupMaterials()
  normalizeQuestionMaterialRefs()
}

function addGroupQuestion() {
  const question = createGroupQuestion(groupForm.questions.length)
  if (groupForm.materials.length) {
    question.materialRefs = [String(groupForm.materials.length)]
  }
  groupForm.questions.push(question)
  resequenceGroupQuestions()
}

function removeGroupQuestion(index) {
  if (groupForm.questions.length <= 1) {
    ElMessage.warning('至少保留一道题目')
    return
  }
  groupForm.questions.splice(index, 1)
  resequenceGroupQuestions()
}

function validateGroupForm() {
  if (!groupForm.title.trim()) {
    throw new Error('请填写材料组标题')
  }
  if (!groupForm.year) {
    throw new Error('请填写年份')
  }
  if (!groupForm.region.trim()) {
    throw new Error('请填写地区')
  }
  if (!groupForm.paperType.trim()) {
    throw new Error('请填写卷型')
  }
  if (!groupForm.sourceName.trim()) {
    throw new Error('请填写来源')
  }
  if (!groupForm.materials.length) {
    throw new Error('至少添加一份材料')
  }
  if (!groupForm.questions.length) {
    throw new Error('至少添加一道题目')
  }
  const labels = new Set()
  groupForm.materials.forEach((item, index) => {
    const label = (item.materialLabel || '').trim()
    const content = (item.content || '').trim()
    if (!label) {
      throw new Error(`请填写材料${index + 1}标签`)
    }
    if (labels.has(label)) {
      throw new Error('材料标签不能重复')
    }
    labels.add(label)
    if (!content) {
      throw new Error(`请填写材料${index + 1}正文`)
    }
  })
  groupForm.questions.forEach((item, index) => {
    if (!(item.categoryName || '').trim()) {
      throw new Error(`请填写第${index + 1}题分类`)
    }
    if (!(item.stem || '').trim()) {
      throw new Error(`请填写第${index + 1}题题干`)
    }
    if (!(item.analysis || '').trim()) {
      throw new Error(`请填写第${index + 1}题答案解析`)
    }
    if (!(item.answerValue || '').trim()) {
      throw new Error(`请填写第${index + 1}题参考答案`)
    }
    if (!item.score || item.score < 1) {
      throw new Error(`请填写第${index + 1}题分值`)
    }
    if (!(item.materialRefs || []).length) {
      throw new Error(`请至少为第${index + 1}题关联一份材料`)
    }
  })
}

function buildGroupPayload() {
  resequenceGroupMaterials()
  resequenceGroupQuestions()
  normalizeQuestionMaterialRefs()
  return {
    id: groupForm.id,
    title: groupForm.title.trim(),
    year: groupForm.year,
    region: groupForm.region.trim(),
    paperType: groupForm.paperType.trim(),
    sourceName: groupForm.sourceName.trim(),
    materialMode: groupForm.materialMode,
    status: groupForm.status,
    materials: groupForm.materials.map((item, index) => ({
      id: item.id,
      sortNo: index + 1,
      materialLabel: (item.materialLabel || '').trim(),
      title: item.title?.trim() || '',
      content: (item.content || '').trim()
    })),
    questions: groupForm.questions.map((item, index) => ({
      id: item.id,
      sortNo: index + 1,
      categoryName: (item.categoryName || '').trim(),
      questionType: 'SHORT_ANSWER',
      difficultyLevel: item.difficultyLevel,
      score: item.score,
      stem: (item.stem || '').trim(),
      analysis: (item.analysis || '').trim(),
      answerValue: (item.answerValue || '').trim(),
      knowledgePoints: item.knowledgePoints?.trim() || '',
      materialRefs: [...(item.materialRefs || [])].map((ref) => String(ref))
    }))
  }
}

async function save() {
  try {
    await request.post('/api/admin/questions', {
      id: form.id,
      subjectName: form.subjectName,
      categoryName: form.categoryName,
      province: form.province,
      questionType: form.questionType,
      difficultyLevel: form.difficultyLevel,
      sourceName: form.sourceName,
      material: form.material,
      stem: form.stem,
      analysis: form.analysis,
      answerValue: form.questionType === 'SHORT_ANSWER' ? form.answerText : normalizeAnswerValue(form.answerValues),
      knowledgePoints: form.knowledgePoints,
      enabled: form.enabled,
      options: form.questionType === 'SHORT_ANSWER' ? [] : form.options.map((item) => ({
        optionKey: item.optionKey,
        optionContent: item.optionContent
      }))
    })
    ElMessage.success('保存成功')
    visible.value = false
    loadData()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function savePaper() {
  try {
    await request.post('/api/admin/question-papers', paperForm)
    ElMessage.success('试卷保存成功')
    paperVisible.value = false
    loadPapers()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function saveGroup() {
  try {
    validateGroupForm()
    await request.post('/api/admin/shenlun-groups', buildGroupPayload())
    ElMessage.success('材料组保存成功')
    groupVisible.value = false
    await Promise.all([loadGroups(), loadData(), loadPapers()])
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function publishGroup(row) {
  const id = row?.id || groupForm.id
  if (!id) {
    ElMessage.warning('请先保存材料组')
    return
  }
  const target = row || groupForm
  if (isArchivedShenlunGroup(target)) {
    ElMessage.warning('已归档的材料组请先恢复后再发布')
    return
  }
  try {
    await ElMessageBox.confirm('发布后会同步生成前台练习和后台试卷，是否继续？', '发布确认', {
      type: 'warning'
    })
    await request.post(`/api/admin/shenlun-groups/${id}/publish`)
    ElMessage.success('发布成功')
    if (groupVisible.value && groupForm.id === id) {
      groupForm.status = 'PUBLISHED'
    }
    await Promise.all([loadGroups(), loadData(), loadPapers()])
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '发布失败')
    }
  }
}

async function removePaper(row) {
  try {
    if (isGeneratedShenlunPaper(row)) {
      ElMessage.warning('申论自动生成试卷请在材料组管理中归档')
      return
    }
    await ElMessageBox.confirm('确认删除这份试卷吗？', '删除确认', { type: 'warning' })
    await request.delete(`/api/admin/question-papers/${row.id}`)
    ElMessage.success('删除成功')
    loadPapers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message)
    }
  }
}

async function remove(id) {
  try {
    await ElMessageBox.confirm('确认删除这道题目吗？', '删除确认', { type: 'warning' })
    await request.delete(`/api/admin/questions/${id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message)
    }
  }
}

async function removeGroup(row) {
  try {
    await ElMessageBox.confirm('归档后该材料组会从前台和后台默认列表隐藏，历史练习会保留，是否继续？', '归档确认', {
      type: 'warning'
    })
    await request.delete(`/api/admin/shenlun-groups/${row.id}`)
    ElMessage.success('归档成功')
    await Promise.all([loadGroups(), loadData(), loadPapers()])
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message)
    }
  }
}

async function restoreGroup(row, status = 'DRAFT') {
  try {
    const actionText = status === 'PUBLISHED' ? '恢复并发布' : '恢复为草稿'
    await ElMessageBox.confirm(`${actionText}后，该材料组会重新回到后台默认列表${status === 'PUBLISHED' ? '并重新出现在前台' : ''}，是否继续？`, '恢复确认', {
      type: 'warning'
    })
    await request.post(`/api/admin/shenlun-groups/${row.id}/restore`, null, { params: { status } })
    ElMessage.success('恢复成功')
    await Promise.all([loadGroups(), loadData(), loadPapers()])
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message)
    }
  }
}

watch(() => form.questionType, (value, previous) => {
  if (value === previous) {
    return
  }
  if (value !== 'MULTI' && form.answerValues.length > 1) {
    form.answerValues = [form.answerValues[0]]
  }
  if (value === 'SHORT_ANSWER') {
    form.answerValues = []
    return
  }
  if (!form.answerValues.length && form.options.length) {
    form.answerValues = [form.options[0].optionKey]
  }
})

watch(() => groupForm.materials.length, () => {
  resequenceGroupMaterials()
  normalizeQuestionMaterialRefs()
})

onMounted(async () => {
  await Promise.all([loadData(), loadPapers(), loadGroups()])
})
</script>

<template>
  <div class="admin-page">
    <div class="toolbar glass-card">
      <el-input v-model="query.keyword" placeholder="搜索题干" clearable />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="warning" @click="openEdit()">新增题目</el-button>
      <el-button type="danger" @click="openPaperEdit()">新增试卷</el-button>
      <el-button type="success" @click="openGroupEdit()">新增材料组</el-button>
    </div>

    <div class="glass-card table-card" v-loading="loading">
      <el-table :data="list">
        <el-table-column prop="subjectName" label="科目" />
        <el-table-column prop="categoryName" label="分类" />
        <el-table-column label="题型">
          <template #default="{ row }">
            <el-tag :type="row.questionType === 'MULTI' ? 'warning' : 'primary'" effect="plain">
              {{ getQuestionTypeLabel(row.questionType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="difficultyLevel" label="难度" />
        <el-table-column prop="stem" label="题干" show-overflow-tooltip />
        <el-table-column label="操作" width="140">
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

    <div class="glass-card table-card" v-loading="paperLoading">
      <div class="paper-toolbar">
        <div class="section-title" style="font-size: 20px">试卷管理</div>
        <div class="paper-search">
          <el-input v-model="paperQuery.keyword" placeholder="搜索试卷名称" clearable />
          <el-button type="primary" @click="loadPapers">查询</el-button>
        </div>
      </div>
      <el-table :data="paperList">
        <el-table-column prop="title" label="试卷名称" show-overflow-tooltip />
        <el-table-column prop="subject" label="科目" width="90" />
        <el-table-column label="地区">
          <template #default="{ row }">{{ row.province || row.region }}</template>
        </el-table-column>
        <el-table-column prop="questionCount" label="题量" width="90" />
        <el-table-column prop="totalScore" label="总分" width="90" />
        <el-table-column label="时长" width="90">
          <template #default="{ row }">{{ row.durationMinutes || row.duration }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">{{ formatPublishStatus(row.status) }}</template>
        </el-table-column>
        <el-table-column label="来源" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ isGeneratedShenlunPaper(row) ? '申论材料组自动生成' : (row.source || '平台组卷') }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170">
          <template #default="scope">
            <el-button
              type="primary"
              link
              :disabled="isGeneratedShenlunPaper(scope.row)"
              @click="openPaperEdit(scope.row)"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              link
              :disabled="isGeneratedShenlunPaper(scope.row)"
              @click="removePaper(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        background
        layout="prev, pager, next"
        :total="paperTotal"
        :page-size="paperQuery.pageSize"
        :current-page="paperQuery.pageNo"
        style="margin-top: 20px; justify-content: center"
        @current-change="(page) => { paperQuery.pageNo = page; loadPapers() }"
      />
    </div>

    <div class="glass-card table-card" v-loading="groupLoading">
      <div class="section-stack">
        <div class="paper-toolbar">
          <div class="section-title" style="font-size: 20px">申论材料组管理</div>
          <div class="paper-search">
            <el-input v-model="groupQuery.keyword" placeholder="搜索标题/来源" clearable />
            <el-select v-model="groupQuery.status" placeholder="状态" clearable style="width: 120px">
              <el-option v-for="item in shenlunStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <el-button type="primary" @click="loadGroups">查询</el-button>
          </div>
        </div>
        <div class="group-filter-grid">
          <el-input-number v-model="groupQuery.year" :min="2000" :max="2099" placeholder="年份" style="width: 100%" />
          <el-input v-model="groupQuery.region" placeholder="地区" clearable />
          <el-input v-model="groupQuery.sourceName" placeholder="来源" clearable />
        </div>
      </div>
      <el-table :data="groupList">
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="year" label="年份" width="90" />
        <el-table-column prop="region" label="地区" width="110" />
        <el-table-column prop="paperType" label="卷型" width="120" />
        <el-table-column prop="sourceName" label="来源" min-width="140" show-overflow-tooltip />
        <el-table-column prop="materialCount" label="材料数" width="90" />
        <el-table-column prop="questionCount" label="题目数" width="90" />
        <el-table-column prop="totalScore" label="总分" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">{{ formatPublishStatus(row.status) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button type="primary" link @click="openGroupEdit(scope.row)">
              {{ isArchivedShenlunGroup(scope.row) ? '查看' : '编辑' }}
            </el-button>
            <el-button
              v-if="!isArchivedShenlunGroup(scope.row)"
              type="success"
              link
              @click="publishGroup(scope.row)"
            >
              发布生成练习
            </el-button>
            <el-button
              v-if="!isArchivedShenlunGroup(scope.row)"
              type="danger"
              link
              @click="removeGroup(scope.row)"
            >
              归档
            </el-button>
            <el-button
              v-if="isArchivedShenlunGroup(scope.row)"
              type="warning"
              link
              @click="restoreGroup(scope.row, 'DRAFT')"
            >
              恢复为草稿
            </el-button>
            <el-button
              v-if="isArchivedShenlunGroup(scope.row)"
              type="success"
              link
              @click="restoreGroup(scope.row, 'PUBLISHED')"
            >
              恢复并发布
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        background
        layout="prev, pager, next"
        :total="groupTotal"
        :page-size="groupQuery.pageSize"
        :current-page="groupQuery.pageNo"
        style="margin-top: 20px; justify-content: center"
        @current-change="(page) => { groupQuery.pageNo = page; loadGroups() }"
      />
    </div>

    <el-dialog v-model="visible" title="题目信息" width="840px">
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="科目"><el-input v-model="form.subjectName" /></el-form-item>
          <el-form-item label="分类"><el-input v-model="form.categoryName" /></el-form-item>
          <el-form-item label="省份"><el-input v-model="form.province" /></el-form-item>
          <el-form-item label="题型">
            <el-select v-model="form.questionType" style="width: 100%">
              <el-option v-for="item in questionTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="难度"><el-input v-model="form.difficultyLevel" /></el-form-item>
          <el-form-item label="来源"><el-input v-model="form.sourceName" /></el-form-item>
          <el-form-item label="是否启用">
            <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
          </el-form-item>
        </div>
        <el-form-item label="材料"><el-input v-model="form.material" type="textarea" :rows="6" /></el-form-item>
        <el-form-item label="题干"><el-input v-model="form.stem" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="解析"><el-input v-model="form.analysis" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="知识点"><el-input v-model="form.knowledgePoints" /></el-form-item>
        <el-form-item v-if="form.questionType !== 'SHORT_ANSWER'" label="正确答案">
          <el-radio-group v-if="form.questionType !== 'MULTI'" v-model="selectedSingleAnswer" class="answer-picker">
            <el-radio v-for="item in form.options" :key="item.optionKey" :value="item.optionKey" border>
              {{ item.optionKey }}
            </el-radio>
          </el-radio-group>
          <el-checkbox-group v-else v-model="selectedMultiAnswers" class="answer-picker">
            <el-checkbox v-for="item in form.options" :key="item.optionKey" :value="item.optionKey" border>
              {{ item.optionKey }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item v-else label="参考答案">
          <el-input v-model="form.answerText" type="textarea" :rows="4" />
        </el-form-item>
        <div v-if="form.questionType !== 'SHORT_ANSWER'" class="option-head">
          <div class="section-title" style="font-size: 18px">选项</div>
          <el-button type="primary" link @click="addOption">新增选项</el-button>
        </div>
        <div v-if="form.questionType !== 'SHORT_ANSWER'" class="option-list">
          <div v-for="(item, index) in form.options" :key="item.optionKey" class="option-row">
            <el-input v-model="item.optionKey" style="max-width: 80px" disabled />
            <el-input v-model="item.optionContent" />
            <el-button link type="danger" @click="removeOption(index)">移除</el-button>
          </div>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="paperVisible" title="试卷信息" width="620px">
      <el-form label-position="top">
        <el-form-item label="试卷名称"><el-input v-model="paperForm.title" /></el-form-item>
        <div class="form-grid">
          <el-form-item label="地区"><el-input v-model="paperForm.province" /></el-form-item>
          <el-form-item label="科目"><el-input v-model="paperForm.subjectName" disabled /></el-form-item>
          <el-form-item label="时长（分钟）"><el-input-number v-model="paperForm.durationMinutes" :min="1" :max="300" style="width: 100%" /></el-form-item>
          <el-form-item label="总分"><el-input-number v-model="paperForm.totalScore" :min="1" :max="300" style="width: 100%" /></el-form-item>
          <el-form-item label="状态">
            <el-select v-model="paperForm.status" style="width: 100%">
              <el-option label="已发布" value="PUBLISHED" />
              <el-option label="草稿" value="DRAFT" />
            </el-select>
          </el-form-item>
        </div>
        <p class="muted">保存后系统自动从行测六个专项各抽取20题组卷，每题1分，共120题。</p>
      </el-form>
      <template #footer>
        <el-button @click="paperVisible = false">取消</el-button>
        <el-button type="primary" @click="savePaper">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="groupVisible" :title="groupForm.id ? '编辑申论材料组' : '新增申论材料组'" width="1100px" top="4vh">
      <el-form label-position="top">
        <div class="section-card">
          <div class="section-title">基本信息</div>
          <div class="form-grid form-grid-3">
            <el-form-item label="标题"><el-input v-model="groupForm.title" /></el-form-item>
            <el-form-item label="年份">
              <el-input-number v-model="groupForm.year" :min="2000" :max="2099" style="width: 100%" />
            </el-form-item>
            <el-form-item label="地区"><el-input v-model="groupForm.region" /></el-form-item>
            <el-form-item label="卷型"><el-input v-model="groupForm.paperType" /></el-form-item>
            <el-form-item label="来源"><el-input v-model="groupForm.sourceName" /></el-form-item>
            <el-form-item label="状态">
              <el-select v-model="groupForm.status" style="width: 100%">
                <el-option v-for="item in shenlunStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="材料模式">
              <el-select v-model="groupForm.materialMode" style="width: 100%">
                <el-option
                  v-for="item in shenlunMaterialModeOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </div>
        </div>

        <div class="section-card">
          <div class="section-head">
            <div class="section-title">材料列表</div>
            <el-button type="primary" link @click="addGroupMaterial">新增材料</el-button>
          </div>
          <div class="editor-list">
            <div v-for="(item, index) in groupForm.materials" :key="item.id || `material-${index}`" class="editor-card">
              <div class="editor-head">
                <div class="editor-title">材料 {{ index + 1 }}</div>
                <el-button type="danger" link @click="removeGroupMaterial(index)">删除</el-button>
              </div>
              <div class="form-grid form-grid-2">
                <el-form-item label="材料标签"><el-input v-model="item.materialLabel" /></el-form-item>
                <el-form-item label="材料标题"><el-input v-model="item.title" /></el-form-item>
              </div>
              <el-form-item label="材料正文">
                <el-input v-model="item.content" type="textarea" :rows="6" />
              </el-form-item>
            </div>
          </div>
        </div>

        <div class="section-card">
          <div class="section-head">
            <div class="section-title">题目列表</div>
            <el-button type="primary" link @click="addGroupQuestion">新增题目</el-button>
          </div>
          <div class="editor-list">
            <div v-for="(item, index) in groupForm.questions" :key="item.id || `question-${index}`" class="editor-card">
              <div class="editor-head">
                <div class="editor-title">第 {{ index + 1 }} 题</div>
                <el-button type="danger" link @click="removeGroupQuestion(index)">删除</el-button>
              </div>
              <div class="form-grid form-grid-4">
                <el-form-item label="分类"><el-input v-model="item.categoryName" /></el-form-item>
                <el-form-item label="题型"><el-input :model-value="'简答题'" disabled /></el-form-item>
                <el-form-item label="难度">
                  <el-select v-model="item.difficultyLevel" style="width: 100%">
                    <el-option v-for="option in shenlunDifficultyOptions" :key="option" :label="option" :value="option" />
                  </el-select>
                </el-form-item>
                <el-form-item label="分值">
                  <el-input-number v-model="item.score" :min="1" :max="100" style="width: 100%" />
                </el-form-item>
              </div>
              <el-form-item label="关联材料">
                <el-select v-model="item.materialRefs" multiple collapse-tags collapse-tags-tooltip style="width: 100%">
                  <el-option
                    v-for="option in groupMaterialRefOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="题干">
                <el-input v-model="item.stem" type="textarea" :rows="3" />
              </el-form-item>
              <div class="form-grid form-grid-2">
                <el-form-item label="参考答案">
                  <el-input v-model="item.answerValue" type="textarea" :rows="5" />
                </el-form-item>
                <el-form-item label="答案解析">
                  <el-input v-model="item.analysis" type="textarea" :rows="5" />
                </el-form-item>
              </div>
              <el-form-item label="知识点">
                <el-input v-model="item.knowledgePoints" />
              </el-form-item>
            </div>
          </div>
        </div>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <span class="muted">未保存的材料组不能发布；归档后前台不可见、后台默认隐藏，历史练习会保留。</span>
          <div class="footer-actions">
            <el-button @click="groupVisible = false">取消</el-button>
            <el-button type="primary" @click="saveGroup">保存</el-button>
            <el-button type="success" :disabled="!groupForm.id || isArchivedShenlunGroup(groupForm)" @click="publishGroup()">发布生成练习</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-page { display: grid; gap: 20px; min-width: 0; }
.toolbar, .table-card { padding: 20px; min-width: 0; max-width: 100%; }
.table-card { overflow-x: auto; }
.toolbar { display: flex; gap: 12px; flex-wrap: wrap; }
.paper-toolbar { display: flex; gap: 12px; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.paper-search { display: flex; gap: 10px; width: min(520px, 100%); flex-wrap: wrap; justify-content: flex-end; }
.muted { color: var(--muted); }
.form-grid { display: grid; gap: 12px; grid-template-columns: repeat(2, 1fr); }
.form-grid-2 { grid-template-columns: repeat(2, 1fr); }
.form-grid-3 { grid-template-columns: repeat(3, 1fr); }
.form-grid-4 { grid-template-columns: repeat(4, 1fr); }
.option-list { display: grid; gap: 10px; }
.option-row { display: grid; gap: 10px; grid-template-columns: 90px 1fr 64px; }
.option-head, .section-head, .editor-head, .dialog-footer, .footer-actions { display: flex; align-items: center; }
.option-head, .section-head, .editor-head { justify-content: space-between; }
.answer-picker { display: flex; gap: 12px; flex-wrap: wrap; }
.section-stack { display: grid; gap: 14px; }
.group-filter-grid { display: grid; gap: 12px; grid-template-columns: 120px 1fr 1fr; }
.section-card { display: grid; gap: 16px; padding: 18px; border-radius: 16px; background: rgba(255, 255, 255, 0.5); }
.section-title { font-weight: 600; }
.editor-list { display: grid; gap: 16px; }
.editor-card { display: grid; gap: 14px; padding: 16px; border: 1px solid rgba(15, 23, 42, 0.08); border-radius: 14px; background: rgba(255, 255, 255, 0.78); }
.editor-title { font-weight: 600; }
.dialog-footer { justify-content: space-between; gap: 12px; width: 100%; }
.footer-actions { gap: 12px; justify-content: flex-end; }

@media (max-width: 1080px) {
  .form-grid,
  .form-grid-2,
  .form-grid-3,
  .form-grid-4,
  .group-filter-grid {
    grid-template-columns: 1fr;
  }

  .paper-toolbar,
  .dialog-footer {
    flex-direction: column;
    align-items: stretch;
  }

  .paper-search,
  .footer-actions {
    width: 100%;
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .toolbar,
  .table-card {
    padding: 16px;
  }

  .toolbar :deep(.el-input),
  .toolbar :deep(.el-select),
  .toolbar :deep(.el-button),
  .paper-search,
  .paper-search :deep(.el-input),
  .paper-search :deep(.el-select),
  .paper-search :deep(.el-button) {
    width: 100%;
  }

  .footer-actions {
    display: grid;
    grid-template-columns: 1fr;
  }

  .footer-actions :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }
}
</style>
