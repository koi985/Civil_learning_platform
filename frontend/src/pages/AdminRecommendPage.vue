<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { ArrowDown, ArrowUp, Plus, Refresh } from '@element-plus/icons-vue'
import request from '../utils/request'
import { FILE_UPLOAD_URL, resolveAssetUrl } from '../utils/assets'

const loading = ref(false)
const savingConfigs = ref(false)
const savingPinned = ref(false)
const refreshingPool = ref('')
const rebuildingCf = ref(false)
const cfStatus = ref({})
const removingId = ref(null)
const pinDrawerVisible = ref(false)
const resourceSearching = ref(false)
const resourceOptions = ref([])
const latestTouchedId = ref(null)
const selectedPinnedIds = ref([])
const list = ref([])
const pinnedList = ref([])
const bannerList = ref([])
const feedbackList = ref([])
const feedbackTotal = ref(0)
const summary = ref({
  feedbackCount: 0,
  distinctUserCount: 0,
  todayFeedbackCount: 0,
  refreshCount: 0,
  hotPoolUpdatedAt: '',
  userPoolUpdatedAt: '',
  pinnedCount: 0,
  resourceTypeStats: [],
  regionalBehaviorStats: {}
})
const pinnedForm = reactive({
  id: null,
  resourceType: 'COURSE',
  resourceId: null,
  displayTitle: '',
  displayReason: '',
  displayTags: '',
  actionText: '',
  regionScope: 'ALL',
  province: '',
  fallbackEnabled: true,
  sortNo: 0,
  enabled: true
})
const bannerForm = reactive({
  id: null,
  title: '',
  imageUrl: '',
  linkUrl: '',
  sortNo: 0,
  enabled: true
})
const feedbackQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  resourceType: ''
})
const feedbackFilters = reactive({
  keyword: '',
  reasonText: '',
  dateRange: []
})
const pinnedFilters = reactive({
  keyword: '',
  resourceType: '',
  regionScope: '',
  enabled: ''
})

const typeMeta = {
  COURSE: { label: '课程', type: 'primary' },
  PRACTICE: { label: '刷题', type: 'success' },
  EXAM: { label: '模考', type: 'warning' },
  NEWS: { label: '资讯', type: 'info' }
}

const regionMeta = {
  ALL: '全部地区',
  PROVINCE: '指定省份',
  NATIONAL: '全国兜底'
}

const resourceEndpointMap = {
  COURSE: '/api/admin/courses',
  PRACTICE: '/api/admin/questions',
  EXAM: '/api/admin/exams',
  NEWS: '/api/admin/news'
}

const changedConfigs = computed(() =>
  list.value.filter((item) => Number(item.configValue) !== Number(item.originalValue))
)

const weightTotal = computed(() =>
  list.value.reduce((sum, item) => sum + Number(item.configValue || 0), 0)
)

const weightStatus = computed(() => {
  const total = weightTotal.value
  if (!list.value.length) return { label: '未加载', type: 'info' }
  if (Math.abs(total - 1) <= 0.0001) return { label: '权重正常', type: 'success' }
  return { label: `合计 ${total.toFixed(4)}`, type: 'warning' }
})

const topMetrics = computed(() => [
  {
    label: '不感兴趣总数',
    value: summary.value.feedbackCount || 0,
    note: '累计用户负反馈',
    tone: 'danger'
  },
  {
    label: '触达用户数',
    value: summary.value.distinctUserCount || 0,
    note: '产生反馈的用户',
    tone: 'primary'
  },
  {
    label: '今日反馈',
    value: summary.value.todayFeedbackCount || 0,
    note: summary.value.todayFeedbackCount ? '今日已有反馈' : '今日暂无新增',
    tone: summary.value.todayFeedbackCount ? 'warning' : 'success'
  },
  {
    label: '启用置顶数',
    value: summary.value.pinnedCount || 0,
    note: `推荐池刷新 ${summary.value.refreshCount || 0} 次`,
    tone: 'success'
  }
])

const poolCards = computed(() => [
  {
    key: 'hot',
    title: '热门推荐池',
    time: summary.value.hotPoolUpdatedAt,
    action: '刷新热门池'
  },
  {
    key: 'user',
    title: '用户推荐池',
    time: summary.value.userPoolUpdatedAt,
    action: '刷新用户池'
  }
])

const feedbackReasonOptions = computed(() => {
  const values = new Set((feedbackList.value || []).map((item) => item.reasonText).filter(Boolean))
  return Array.from(values)
})

const sortedPinnedList = computed(() =>
  [...(pinnedList.value || [])].sort((a, b) => Number(a.sortNo || 0) - Number(b.sortNo || 0))
)

const filteredPinnedList = computed(() => {
  const keyword = pinnedFilters.keyword.trim().toLowerCase()
  return sortedPinnedList.value.filter((item) => {
    const haystack = `${item.resourceTitle || ''} ${item.displayTitle || ''} ${item.displayReason || ''} ${item.resourceId || ''}`.toLowerCase()
    if (keyword && !haystack.includes(keyword)) return false
    if (pinnedFilters.resourceType && item.resourceType !== pinnedFilters.resourceType) return false
    if (pinnedFilters.regionScope && item.regionScope !== pinnedFilters.regionScope) return false
    if (pinnedFilters.enabled !== '' && String(Boolean(item.enabled)) !== pinnedFilters.enabled) return false
    return true
  })
})

const previewTags = computed(() =>
  String(pinnedForm.displayTags || '')
    .split(/[，,]/)
    .map((item) => item.trim())
    .filter(Boolean)
)

const currentPreviewTitle = computed(() =>
  pinnedForm.displayTitle || resourceOptions.value.find((item) => item.id === pinnedForm.resourceId)?.title || '将使用资源原始标题'
)

const currentPreviewReason = computed(() =>
  pinnedForm.displayReason || '未填写展示理由时，将使用系统推荐理由或资源默认说明。'
)

const filteredFeedbackList = computed(() => {
  const keyword = feedbackFilters.keyword.trim().toLowerCase()
  const reasonText = feedbackFilters.reasonText
  const [start, end] = feedbackFilters.dateRange || []
  return (feedbackList.value || []).filter((item) => {
    const haystack = `${item.nickname || ''} ${item.resourceTitle || ''}`.toLowerCase()
    const createdAt = item.createdAt ? String(item.createdAt).slice(0, 10) : ''
    if (keyword && !haystack.includes(keyword)) return false
    if (reasonText && item.reasonText !== reasonText) return false
    if (start && createdAt < start) return false
    if (end && createdAt > end) return false
    return true
  })
})

function configPayload(item) {
  return {
    configKey: item.configKey,
    configName: item.configName,
    configValue: item.configValue,
    descriptionText: item.descriptionText
  }
}

function typeLabel(value) {
  return typeMeta[value]?.label || value || '未知'
}

function resourceTitle(item) {
  return item.title || item.resourceTitle || item.stem || item.name || `资源 #${item.id}`
}

function resourceMetaLine(item) {
  return [
    `ID ${item.id}`,
    item.status || item.auditStatus,
    item.subjectName,
    item.examType,
    item.province || item.region
  ].filter(Boolean).join(' / ')
}

function pinnedRowClassName({ row }) {
  return row.id === latestTouchedId.value ? 'is-latest-updated' : ''
}

function poolStatus(time) {
  if (!time) return { label: '未刷新', type: 'info', className: 'is-idle' }
  const normalized = String(time).replace('T', ' ')
  const timestamp = new Date(normalized.replace(/-/g, '/')).getTime()
  if (!Number.isFinite(timestamp)) return { label: '已记录', type: 'success', className: 'is-ok' }
  const hours = (Date.now() - timestamp) / 36e5
  if (hours > 24) return { label: '需刷新', type: 'warning', className: 'is-warning' }
  return { label: '正常', type: 'success', className: 'is-ok' }
}

async function loadData() {
  loading.value = true
  try {
    const [configData, pinnedData, bannerData, summaryData, feedbackData] = await Promise.all([
      request.get('/api/admin/recommend-configs'),
      request.get('/api/admin/recommend-pinned-resources'),
      request.get('/api/admin/banners'),
      request.get('/api/recommend/admin-feedback-summary'),
      request.get('/api/recommend/admin-feedbacks', { params: feedbackQuery })
    ])
    list.value = (configData || []).map((item) => ({
      ...item,
      configValue: Number(item.configValue || 0),
      originalValue: Number(item.configValue || 0)
    }))
    pinnedList.value = pinnedData || []
    bannerList.value = bannerData || []
    summary.value = summaryData || summary.value
    feedbackList.value = feedbackData.records || []
    feedbackTotal.value = feedbackData.total || 0
    try {
      cfStatus.value = await request.get('/api/recommend/admin-cf-status') || {}
    } catch (e) {
      cfStatus.value = {}
    }
  } finally {
    loading.value = false
  }
}

async function triggerCfRebuild() {
  try {
    await ElMessageBox.confirm('全量重算 UserCF 相似度矩阵？大用户量下可能耗时较长。', '协同过滤重建', {
      confirmButtonText: '开始重建',
      cancelButtonText: '取消'
    })
    rebuildingCf.value = true
    const result = await request.post('/api/recommend/admin-cf-rebuild')
    ElMessage.success(`重建完成：覆盖 ${result?.computed || 0} 用户，${result?.withSimilarUsers || 0} 用户已建立相似池，耗时 ${result?.durationMs || 0} ms`)
    await loadData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || '操作已取消')
  } finally {
    rebuildingCf.value = false
  }
}

function resetPinnedForm() {
  Object.assign(pinnedForm, {
    id: null,
    resourceType: 'COURSE',
    resourceId: null,
    displayTitle: '',
    displayReason: '',
    displayTags: '',
    actionText: '',
    regionScope: 'ALL',
    province: '',
    fallbackEnabled: true,
    sortNo: 0,
    enabled: true
  })
  resourceOptions.value = []
}

function resetBannerForm() {
  Object.assign(bannerForm, {
    id: null,
    title: '',
    imageUrl: '',
    linkUrl: '',
    sortNo: 0,
    enabled: true
  })
}

function editBanner(item) {
  Object.assign(bannerForm, {
    id: item.id,
    title: item.title || '',
    imageUrl: item.imageUrl || '',
    linkUrl: item.linkUrl || '',
    sortNo: item.sortNo || 0,
    enabled: item.enabled !== false
  })
}

function validateBannerForm() {
  if (!bannerForm.title.trim()) {
    ElMessage.warning('请填写 Banner 标题')
    return false
  }
  if (!bannerForm.imageUrl.trim()) {
    ElMessage.warning('请填写 Banner 图片 URL')
    return false
  }
  return true
}

async function uploadBannerImage(option) {
  const fd = new FormData()
  fd.append('file', option.file)
  fd.append('bizType', 'home-banner')
  try {
    const data = await request.post(FILE_UPLOAD_URL, fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    bannerForm.imageUrl = data.url
    ElMessage.success('Banner 图片上传成功')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function openCreatePinned() {
  resetPinnedForm()
  pinDrawerVisible.value = true
}

function editPinned(item) {
  Object.assign(pinnedForm, {
    id: item.id,
    resourceType: item.resourceType,
    resourceId: item.resourceId,
    displayTitle: item.displayTitle || '',
    displayReason: item.displayReason || '',
    displayTags: item.displayTags || '',
    actionText: item.actionText || '',
    regionScope: item.regionScope || 'ALL',
    province: item.province || '',
    fallbackEnabled: item.fallbackEnabled !== false,
    sortNo: item.sortNo || 0,
    enabled: item.enabled !== false
  })
  resourceOptions.value = item.resourceTitle ? [{ id: item.resourceId, title: item.resourceTitle }] : []
  pinDrawerVisible.value = true
}

function resetConfigChanges() {
  list.value = list.value.map((item) => ({ ...item, configValue: item.originalValue }))
}

async function saveConfig(item) {
  try {
    await request.post('/api/admin/recommend-configs', configPayload(item))
    ElMessage.success('配置已保存')
    loadData()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function saveAllConfigs() {
  if (!changedConfigs.value.length) return
  savingConfigs.value = true
  try {
    await Promise.all(changedConfigs.value.map((item) => request.post('/api/admin/recommend-configs', configPayload(item))))
    ElMessage.success('推荐权重已批量保存')
    await loadData()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    savingConfigs.value = false
  }
}

async function triggerPool(type) {
  const title = type === 'hot' ? '刷新热门推荐池' : '刷新用户推荐池'
  try {
    await ElMessageBox.confirm(`${title} 会重新计算推荐候选，确认继续？`, '确认操作', {
      type: 'warning',
      confirmButtonText: '确认刷新',
      cancelButtonText: '取消'
    })
    refreshingPool.value = type
    await request.post(type === 'hot' ? '/api/recommend/admin-refresh-hot-pool' : '/api/recommend/admin-refresh-user-pool')
    ElMessage.success(type === 'hot' ? '热门池已刷新' : '用户池已刷新')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || '操作已取消')
  } finally {
    refreshingPool.value = ''
  }
}

function validatePinnedForm() {
  if (!pinnedForm.resourceType) {
    ElMessage.warning('请选择资源类型')
    return false
  }
  if (!pinnedForm.resourceId) {
    ElMessage.warning('请选择或输入资源 ID')
    return false
  }
  if (pinnedForm.regionScope === 'PROVINCE' && !pinnedForm.province.trim()) {
    ElMessage.warning('指定省份投放时必须填写省份')
    return false
  }
  return true
}

function pinnedPayload(row) {
  return {
    id: row.id,
    resourceType: row.resourceType,
    resourceId: row.resourceId,
    displayTitle: row.displayTitle || '',
    displayReason: row.displayReason || '',
    displayTags: row.displayTags || '',
    actionText: row.actionText || '',
    regionScope: row.regionScope || 'ALL',
    province: row.province || '',
    fallbackEnabled: row.fallbackEnabled !== false,
    sortNo: row.sortNo || 0,
    enabled: row.enabled !== false
  }
}

async function savePinned(options = {}) {
  if (!options.skipValidate && !validatePinnedForm()) return
  savingPinned.value = true
  try {
    await request.post('/api/admin/recommend-pinned-resources', pinnedForm)
    ElMessage.success('置顶资源已保存')
    latestTouchedId.value = pinnedForm.id
    resetPinnedForm()
    pinDrawerVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    savingPinned.value = false
  }
}

async function savePinnedRow(row, message = '置顶资源已更新') {
  await request.post('/api/admin/recommend-pinned-resources', pinnedPayload(row))
  latestTouchedId.value = row.id
  ElMessage.success(message)
  await loadData()
}

async function togglePinnedEnabled(row, value) {
  const original = row.enabled
  try {
    await ElMessageBox.confirm(`确认${value ? '启用' : '停用'}「${row.resourceTitle || row.displayTitle || row.resourceId}」？`, '状态确认', {
      type: 'warning',
      confirmButtonText: '确认',
      cancelButtonText: '取消'
    })
    row.enabled = value
    await savePinnedRow(row, value ? '已启用置顶资源' : '已停用置顶资源')
  } catch (error) {
    row.enabled = original
    if (error !== 'cancel') ElMessage.error(error.message || '操作已取消')
  }
}

async function movePinned(row, direction) {
  const list = sortedPinnedList.value
  const index = list.findIndex((item) => item.id === row.id)
  const target = list[index + direction]
  if (!target) return
  const rowSort = row.sortNo || index + 1
  const targetSort = target.sortNo || index + direction + 1
  try {
    await Promise.all([
      request.post('/api/admin/recommend-pinned-resources', { ...pinnedPayload(row), sortNo: targetSort }),
      request.post('/api/admin/recommend-pinned-resources', { ...pinnedPayload(target), sortNo: rowSort })
    ])
    latestTouchedId.value = row.id
    ElMessage.success('排序已更新')
    await loadData()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function batchDisablePinned() {
  const rows = pinnedList.value.filter((item) => selectedPinnedIds.value.includes(item.id))
  if (!rows.length) return
  try {
    await ElMessageBox.confirm(`确认停用选中的 ${rows.length} 个置顶资源？`, '批量停用', {
      type: 'warning',
      confirmButtonText: '停用',
      cancelButtonText: '取消'
    })
    await Promise.all(rows.map((row) => request.post('/api/admin/recommend-pinned-resources', { ...pinnedPayload(row), enabled: false })))
    selectedPinnedIds.value = []
    ElMessage.success('已批量停用')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || '操作已取消')
  }
}

async function batchRemovePinned() {
  if (!selectedPinnedIds.value.length) return
  try {
    await ElMessageBox.confirm(`确认删除选中的 ${selectedPinnedIds.value.length} 个置顶资源？`, '批量删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await Promise.all(selectedPinnedIds.value.map((id) => request.delete(`/api/admin/recommend-pinned-resources/${id}`)))
    selectedPinnedIds.value = []
    ElMessage.success('已批量删除')
    await loadData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || '操作已取消')
  }
}

async function removePinned(row) {
  try {
    await ElMessageBox.confirm(`确认删除置顶资源「${row.resourceTitle || row.displayTitle || row.resourceId}」？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    removingId.value = row.id
    await request.delete(`/api/admin/recommend-pinned-resources/${row.id}`)
    ElMessage.success('已删除')
    if (pinnedForm.id === row.id) resetPinnedForm()
    await loadData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || '操作已取消')
  } finally {
    removingId.value = null
  }
}

async function saveBanner() {
  if (!validateBannerForm()) return
  try {
    await request.post('/api/admin/banners', bannerForm)
    ElMessage.success('Banner 已保存')
    resetBannerForm()
    await loadData()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function toggleBannerEnabled(row, value) {
  const original = row.enabled
  try {
    row.enabled = value
    await request.post('/api/admin/banners', {
      id: row.id,
      title: row.title,
      imageUrl: row.imageUrl,
      linkUrl: row.linkUrl || '',
      sortNo: row.sortNo || 0,
      enabled: value
    })
    ElMessage.success(value ? 'Banner 已启用' : 'Banner 已停用')
    await loadData()
  } catch (error) {
    row.enabled = original
    ElMessage.error(error.message)
  }
}

async function removeBanner(row) {
  try {
    await ElMessageBox.confirm(`确认删除 Banner「${row.title}」？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    await request.delete(`/api/admin/banners/${row.id}`)
    ElMessage.success('Banner 已删除')
    if (bannerForm.id === row.id) resetBannerForm()
    await loadData()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error.message || '操作已取消')
  }
}

function applyFeedbackFilter() {
  feedbackQuery.pageNo = 1
  loadData()
}

async function searchResources(keyword = '') {
  const endpoint = resourceEndpointMap[pinnedForm.resourceType]
  if (!endpoint) return
  resourceSearching.value = true
  try {
    const data = await request.get(endpoint, {
      params: {
        pageNo: 1,
        pageSize: 10,
        keyword
      }
    })
    resourceOptions.value = (data.records || []).map((item) => ({
      ...item,
      id: item.id,
      title: resourceTitle(item)
    }))
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    resourceSearching.value = false
  }
}

function handleResourceTypeChange() {
  pinnedForm.resourceId = null
  resourceOptions.value = []
  searchResources()
}

function handleResourceSelect(id) {
  const resource = resourceOptions.value.find((item) => item.id === id)
  if (!resource) return
  pinnedForm.displayTitle = pinnedForm.displayTitle || resource.title
}

onMounted(loadData)
</script>

<template>
  <div class="admin-page recommend-admin" v-loading="loading">
    <section class="recommend-hero">
      <div>
        <div class="eyebrow">推荐系统运营台</div>
        <h1>推荐配置</h1>
        <p>集中管理推荐权重、置顶投放、推荐池刷新和用户反馈，优先保障推荐结果稳定可控。</p>
      </div>
      <div class="hero-actions">
        <el-tag :type="weightStatus.type" effect="plain">{{ weightStatus.label }}</el-tag>
        <el-button :icon="Refresh" :loading="loading" @click="loadData">刷新数据</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <article v-for="item in topMetrics" :key="item.label" class="metric-card" :class="`tone-${item.tone}`">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.note }}</p>
      </article>
    </section>

    <section class="workbench-grid">
      <div class="panel config-panel">
        <div class="panel-head">
          <div>
            <div class="section-title small">推荐权重</div>
            <p>调整行为协同、标签匹配和热门热度的占比，合计建议保持为 1。</p>
          </div>
          <div class="head-actions">
            <el-button :disabled="!changedConfigs.length" @click="resetConfigChanges">撤销变更</el-button>
            <el-button
              type="primary"
              :loading="savingConfigs"
              :disabled="!changedConfigs.length"
              @click="saveAllConfigs"
            >
              保存全部
            </el-button>
          </div>
        </div>
        <div class="weight-total" :class="{ warning: weightStatus.type === 'warning' }">
          <span>当前合计</span>
          <strong>{{ weightTotal.toFixed(4) }}</strong>
          <small v-if="changedConfigs.length">{{ changedConfigs.length }} 项未保存</small>
          <small v-else>配置已同步</small>
        </div>
        <div class="config-list">
          <article v-for="item in list" :key="item.configKey" class="config-card">
            <div class="config-copy">
              <div class="config-title">
                <strong>{{ item.configName }}</strong>
                <el-tag v-if="Number(item.configValue) !== Number(item.originalValue)" type="warning" effect="plain">
                  未保存
                </el-tag>
              </div>
              <p>{{ item.descriptionText }}</p>
              <code>{{ item.configKey }}</code>
            </div>
            <div class="config-control">
              <el-slider v-model="item.configValue" :min="0" :max="1" :step="0.01" />
              <el-input-number v-model="item.configValue" :min="0" :max="1" :step="0.1" :precision="4" />
              <el-button text type="primary" @click="saveConfig(item)">单项保存</el-button>
            </div>
          </article>
        </div>
      </div>

      <aside class="panel pool-panel">
        <div class="panel-head compact">
          <div class="section-title small">推荐池状态</div>
        </div>
        <article v-for="item in poolCards" :key="item.key" class="pool-card">
          <div>
            <strong>{{ item.title }}</strong>
            <p>{{ item.time || '暂无刷新记录' }}</p>
          </div>
          <div class="pool-action">
            <el-tag :type="poolStatus(item.time).type" effect="plain">{{ poolStatus(item.time).label }}</el-tag>
            <el-button
              type="primary"
              plain
              :loading="refreshingPool === item.key"
              @click="triggerPool(item.key)"
            >
              {{ item.action }}
            </el-button>
          </div>
        </article>
        <article class="pool-card">
          <div>
            <strong>UserCF 协同过滤</strong>
            <p>
              全量：{{ cfStatus.LAST_FULL_AT || '尚未运行' }}
              <span v-if="cfStatus.LAST_FULL_DURATION_MS" class="muted"> · {{ cfStatus.LAST_FULL_DURATION_MS }} ms</span>
            </p>
            <p>增量：{{ cfStatus.LAST_INCR_AT || '尚未运行' }}</p>
            <p v-if="cfStatus.LAST_FULL_COVERED_USERS" class="muted">
              覆盖 {{ cfStatus.LAST_FULL_COVERED_USERS }} 用户 · 已建立相似池 {{ cfStatus.LAST_FULL_WITH_SIM_USERS || 0 }}
            </p>
          </div>
          <div class="pool-action">
            <el-tag :type="cfStatus.LAST_FULL_AT ? 'success' : 'info'" effect="plain">
              {{ cfStatus.running === 'true' ? '重建中' : (cfStatus.LAST_FULL_AT ? '已就绪' : '待初始化') }}
            </el-tag>
            <el-button type="primary" plain :loading="rebuildingCf" @click="triggerCfRebuild">立即重算</el-button>
          </div>
        </article>
        <div class="type-row">
          <el-tag v-for="item in summary.resourceTypeStats" :key="item.resourceType" round>
            {{ typeLabel(item.resourceType) }}：{{ item.count }}
          </el-tag>
          <span v-if="!summary.resourceTypeStats?.length" class="muted">暂无资源类型统计</span>
        </div>
      </aside>
    </section>

    <section class="panel pin-panel">
      <div class="panel-head">
        <div>
          <div class="section-title small">强制置顶资源</div>
          <p>通过搜索选资源、列表内启停和排序，快速维护推荐流优先曝光内容。</p>
        </div>
        <el-button type="primary" :icon="Plus" @click="openCreatePinned">新增置顶</el-button>
      </div>

      <div class="pin-toolbar">
        <el-input v-model="pinnedFilters.keyword" placeholder="搜索标题、理由或资源ID" clearable />
        <el-select v-model="pinnedFilters.resourceType" placeholder="资源类型" clearable>
          <el-option label="课程" value="COURSE" />
          <el-option label="刷题" value="PRACTICE" />
          <el-option label="模考" value="EXAM" />
          <el-option label="资讯" value="NEWS" />
        </el-select>
        <el-select v-model="pinnedFilters.regionScope" placeholder="地区范围" clearable>
          <el-option label="全部地区" value="ALL" />
          <el-option label="指定省份" value="PROVINCE" />
          <el-option label="全国兜底" value="NATIONAL" />
        </el-select>
        <el-select v-model="pinnedFilters.enabled" placeholder="状态" clearable>
          <el-option label="启用" value="true" />
          <el-option label="停用" value="false" />
        </el-select>
        <el-button :disabled="!selectedPinnedIds.length" @click="batchDisablePinned">批量停用</el-button>
        <el-button type="danger" plain :disabled="!selectedPinnedIds.length" @click="batchRemovePinned">批量删除</el-button>
      </div>

      <el-table
        :data="filteredPinnedList"
        class="admin-table pinned-table"
        border
        row-key="id"
        :row-class-name="pinnedRowClassName"
        @selection-change="(rows) => { selectedPinnedIds = rows.map((item) => item.id) }"
      >
        <el-table-column type="selection" width="46" />
        <el-table-column label="资源类型" width="110">
          <template #default="{ row }">
            <el-tag :type="typeMeta[row.resourceType]?.type || 'info'" effect="plain">
              {{ typeLabel(row.resourceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="resourceId" label="资源ID" width="100" />
        <el-table-column prop="resourceTitle" label="资源标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="displayReason" label="展示理由" min-width="220" show-overflow-tooltip />
        <el-table-column label="地区范围" width="120">
          <template #default="{ row }">{{ regionMeta[row.regionScope] || row.regionScope || '全部地区' }}</template>
        </el-table-column>
        <el-table-column prop="province" label="省份" width="100" />
        <el-table-column label="排序" width="120">
          <template #default="{ row }">
            <div class="sort-tools">
              <span>{{ row.sortNo }}</span>
              <el-button text :icon="ArrowUp" @click="movePinned(row, -1)" />
              <el-button text :icon="ArrowDown" @click="movePinned(row, 1)" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled"
              inline-prompt
              active-text="启用"
              inactive-text="停用"
              @update:model-value="(value) => togglePinnedEnabled(row, value)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="editPinned(row)">编辑</el-button>
            <el-button text type="danger" :loading="removingId === row.id" @click="removePinned(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-drawer
        v-model="pinDrawerVisible"
        :title="pinnedForm.id ? '编辑置顶资源' : '新增置顶资源'"
        size="560px"
        destroy-on-close
      >
        <div class="drawer-body">
          <div class="drawer-group">
            <h3>资源选择</h3>
            <el-form-item label="资源类型">
              <el-select v-model="pinnedForm.resourceType" @change="handleResourceTypeChange">
                <el-option label="课程" value="COURSE" />
                <el-option label="刷题" value="PRACTICE" />
                <el-option label="模考" value="EXAM" />
                <el-option label="资讯" value="NEWS" />
              </el-select>
            </el-form-item>
            <el-form-item label="搜索资源">
              <el-select
                v-model="pinnedForm.resourceId"
                filterable
                remote
                reserve-keyword
                clearable
                :remote-method="searchResources"
                :loading="resourceSearching"
                placeholder="输入标题、题干或关键词搜索"
                @change="handleResourceSelect"
              >
                <el-option v-for="item in resourceOptions" :key="item.id" :label="item.title" :value="item.id">
                  <div class="resource-option">
                    <strong>{{ item.title }}</strong>
                    <span>{{ resourceMetaLine(item) }}</span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="资源 ID">
              <el-input-number v-model="pinnedForm.resourceId" :min="0" placeholder="搜索不可用时可手动输入" />
            </el-form-item>
          </div>

          <div class="drawer-group">
            <h3>展示内容</h3>
            <el-form-item label="展示标题">
              <el-input v-model="pinnedForm.displayTitle" placeholder="留空则使用资源标题" />
            </el-form-item>
            <el-form-item label="展示理由">
              <el-input v-model="pinnedForm.displayReason" type="textarea" :rows="3" placeholder="例如：适合当前薄弱项补强" />
            </el-form-item>
            <el-form-item label="展示标签">
              <el-input v-model="pinnedForm.displayTags" placeholder="使用逗号分隔标签" />
            </el-form-item>
            <el-form-item label="按钮文案">
              <el-input v-model="pinnedForm.actionText" placeholder="例如：立即学习" />
            </el-form-item>
          </div>

          <div class="drawer-group">
            <h3>投放规则</h3>
            <el-form-item label="地区范围">
              <el-select v-model="pinnedForm.regionScope">
                <el-option label="全部地区" value="ALL" />
                <el-option label="指定省份" value="PROVINCE" />
                <el-option label="全国兜底" value="NATIONAL" />
              </el-select>
            </el-form-item>
            <el-form-item label="指定省份">
              <el-input
                v-model="pinnedForm.province"
                :disabled="pinnedForm.regionScope !== 'PROVINCE'"
                placeholder="仅指定省份时必填，如四川"
              />
            </el-form-item>
            <el-form-item label="兜底策略">
              <el-radio-group v-model="pinnedForm.fallbackEnabled">
                <el-radio-button :value="false">仅精准匹配</el-radio-button>
                <el-radio-button :value="true">无匹配时兜底</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="排序">
              <el-input-number v-model="pinnedForm.sortNo" :min="0" />
            </el-form-item>
            <el-form-item label="状态">
              <el-switch v-model="pinnedForm.enabled" inline-prompt active-text="启用" inactive-text="停用" />
            </el-form-item>
          </div>

          <div class="preview-card">
            <div class="preview-head">
              <el-tag :type="typeMeta[pinnedForm.resourceType]?.type || 'info'" effect="plain">
                {{ typeLabel(pinnedForm.resourceType) }}
              </el-tag>
              <span>{{ regionMeta[pinnedForm.regionScope] }}</span>
            </div>
            <strong>{{ currentPreviewTitle }}</strong>
            <p>{{ currentPreviewReason }}</p>
            <div class="preview-tags">
              <el-tag v-for="tag in previewTags" :key="tag" round>{{ tag }}</el-tag>
              <span v-if="!previewTags.length">暂无展示标签</span>
            </div>
            <div class="preview-action">{{ pinnedForm.actionText || '查看详情' }}</div>
          </div>
        </div>

        <template #footer>
          <div class="drawer-footer">
            <el-button @click="pinDrawerVisible = false">取消</el-button>
            <el-button @click="resetPinnedForm">重置</el-button>
            <el-button type="primary" :loading="savingPinned" @click="savePinned()">保存置顶资源</el-button>
          </div>
        </template>
      </el-drawer>
    </section>

    <section class="feedback-grid">
      <div class="panel feedback-panel">
        <div class="panel-head">
          <div>
            <div class="section-title small">反馈明细</div>
            <p>跟踪用户“不感兴趣”反馈，辅助判断推荐内容、标签和地区策略是否需要调整。</p>
          </div>
        </div>
        <div class="toolbar">
          <el-select v-model="feedbackQuery.resourceType" placeholder="资源类型" clearable>
            <el-option label="课程" value="COURSE" />
            <el-option label="刷题" value="PRACTICE" />
            <el-option label="模考" value="EXAM" />
            <el-option label="资讯" value="NEWS" />
          </el-select>
          <el-input v-model="feedbackFilters.keyword" placeholder="搜索用户或资源标题" clearable />
          <el-select v-model="feedbackFilters.reasonText" placeholder="反馈原因" clearable>
            <el-option v-for="item in feedbackReasonOptions" :key="item" :label="item" :value="item" />
          </el-select>
          <el-date-picker
            v-model="feedbackFilters.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
          <el-button type="primary" @click="applyFeedbackFilter">查询</el-button>
        </div>
        <el-table :data="filteredFeedbackList" class="admin-table" border>
          <el-table-column prop="nickname" label="用户" width="140" show-overflow-tooltip />
          <el-table-column label="资源类型" width="110">
            <template #default="{ row }">
              <el-tag :type="typeMeta[row.resourceType]?.type || 'info'" effect="plain">
                {{ typeLabel(row.resourceType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="resourceTitle" label="资源标题" min-width="220" show-overflow-tooltip />
          <el-table-column prop="reasonText" label="反馈原因" min-width="180" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="反馈时间" width="180" />
        </el-table>
        <el-pagination
          background
          layout="prev, pager, next"
          :total="feedbackTotal"
          :page-size="feedbackQuery.pageSize"
          :current-page="feedbackQuery.pageNo"
          class="pagination"
          @current-change="(page) => { feedbackQuery.pageNo = page; loadData() }"
        />
      </div>

      <aside class="panel banner-panel">
        <div class="section-title small">首页 Banner 管理</div>
        <p class="banner-note">维护首页轮播图，启用后按排序展示在首页首屏。</p>

        <div class="banner-form">
          <el-input v-model="bannerForm.title" placeholder="Banner 标题" maxlength="80" />
          <div class="banner-upload-row">
            <el-upload
              :show-file-list="false"
              :http-request="uploadBannerImage"
              accept="image/*"
            >
              <el-button type="primary" plain>{{ bannerForm.imageUrl ? '替换图片' : '上传图片' }}</el-button>
            </el-upload>
            <span>{{ bannerForm.imageUrl ? '图片已上传' : '建议上传首页首屏横幅图' }}</span>
          </div>
          <el-input v-model="bannerForm.linkUrl" placeholder="跳转链接，如 /course" />
          <div class="banner-inline">
            <el-input-number v-model="bannerForm.sortNo" :min="0" placeholder="排序" />
            <el-switch v-model="bannerForm.enabled" inline-prompt active-text="启用" inactive-text="停用" />
          </div>
          <div v-if="bannerForm.imageUrl" class="banner-preview">
            <img :src="resolveAssetUrl(bannerForm.imageUrl)" :alt="bannerForm.title" />
          </div>
          <div class="banner-actions">
            <el-button @click="resetBannerForm">重置</el-button>
            <el-button type="primary" @click="saveBanner">{{ bannerForm.id ? '保存修改' : '新增 Banner' }}</el-button>
          </div>
        </div>

        <div class="banner-list">
          <article v-for="item in bannerList" :key="item.id" class="banner-item">
            <img :src="resolveAssetUrl(item.imageUrl)" :alt="item.title" />
            <div class="banner-item-body">
              <strong>{{ item.title }}</strong>
              <span>{{ item.linkUrl || '未设置跳转' }}</span>
              <div class="banner-item-meta">
                <el-tag effect="plain">排序 {{ item.sortNo }}</el-tag>
                <el-switch
                  :model-value="item.enabled"
                  inline-prompt
                  active-text="启用"
                  inactive-text="停用"
                  @update:model-value="(value) => toggleBannerEnabled(item, value)"
                />
              </div>
              <div class="banner-row-actions">
                <el-button text type="primary" @click="editBanner(item)">编辑</el-button>
                <el-button text type="danger" @click="removeBanner(item)">删除</el-button>
              </div>
            </div>
          </article>
          <el-empty v-if="!bannerList.length" description="暂无 Banner" :image-size="80" />
        </div>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.recommend-admin {
  display: grid;
  gap: 18px;
  min-width: 0;
}

.recommend-hero,
.panel,
.metric-card {
  min-width: 0;
  border: 1px solid #dbe3f0;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.04);
}

.recommend-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 20px 22px;
}

.eyebrow {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.recommend-hero h1 {
  margin: 4px 0 8px;
  color: #0f172a;
  font-size: 24px;
  line-height: 1.25;
}

.recommend-hero p,
.panel-head p,
.config-card p,
.pool-card p {
  margin: 0;
  color: #64748b;
  line-height: 1.6;
}

.hero-actions,
.head-actions,
.pool-action,
.form-actions,
.toolbar,
.type-row,
.switch-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.metric-grid {
  display: grid;
  gap: 14px;
  min-width: 0;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.metric-card {
  padding: 16px;
  border-left: 4px solid #2563eb;
}

.metric-card span {
  color: #64748b;
  font-size: 13px;
}

.metric-card strong {
  display: block;
  margin: 8px 0 4px;
  color: #0f172a;
  font-size: 28px;
  line-height: 1.1;
}

.metric-card p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.metric-card.tone-danger {
  border-left-color: #dc2626;
}

.metric-card.tone-warning {
  border-left-color: #f59e0b;
}

.metric-card.tone-success {
  border-left-color: #16a34a;
}

.workbench-grid {
  display: grid;
  gap: 18px;
  min-width: 0;
  grid-template-columns: minmax(0, 1fr) 320px;
  align-items: start;
}

.feedback-grid {
  display: grid;
  gap: 18px;
  min-width: 0;
  grid-template-columns: minmax(0, 1fr) 320px;
  align-items: start;
}

.panel {
  padding: 18px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.panel-head.compact {
  margin-bottom: 10px;
}

.section-title.small {
  margin: 0 0 6px;
  color: #0f172a;
  font-size: 18px;
}

.weight-total {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
  padding: 12px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
}

.weight-total.warning {
  border-color: #fde68a;
  background: #fffbeb;
}

.weight-total span,
.weight-total small,
.muted {
  color: #64748b;
}

.weight-total strong {
  color: #0f172a;
  font-size: 20px;
}

.config-list {
  display: grid;
  gap: 12px;
}

.config-card {
  display: grid;
  gap: 18px;
  grid-template-columns: minmax(220px, 1fr) minmax(320px, 0.9fr);
  align-items: center;
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.config-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.config-copy code {
  display: inline-block;
  margin-top: 8px;
  color: #475569;
  font-size: 12px;
}

.config-control {
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(120px, 1fr) 130px auto;
  align-items: center;
}

.pool-panel,
.banner-panel {
  position: sticky;
  top: 96px;
}

.pool-card {
  display: grid;
  gap: 12px;
  margin-bottom: 12px;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.pool-card strong {
  color: #0f172a;
}

.pin-toolbar {
  display: grid;
  gap: 10px;
  grid-template-columns: minmax(180px, 1fr) minmax(120px, 140px) minmax(130px, 150px) minmax(100px, 120px) auto auto;
  align-items: center;
  margin-bottom: 14px;
}

.pin-toolbar :deep(.el-input),
.pin-toolbar :deep(.el-select) {
  width: 100%;
}

.pinned-table :deep(.is-latest-updated td) {
  background: #eff6ff;
}

.sort-tools {
  display: flex;
  align-items: center;
  gap: 4px;
}

.sort-tools span {
  min-width: 24px;
  color: #334155;
}

.sort-tools :deep(.el-button) {
  width: 24px;
  height: 24px;
  padding: 0;
}

.pin-form {
  display: grid;
  gap: 14px;
}

.form-group {
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.form-group h3 {
  margin: 0 0 12px;
  color: #0f172a;
  font-size: 15px;
}

.form-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.form-grid :deep(.el-form-item) {
  margin-bottom: 0;
}

.form-grid :deep(.el-select),
.form-grid :deep(.el-input-number),
.toolbar :deep(.el-select),
.toolbar :deep(.el-input) {
  width: 100%;
}

.switch-row {
  align-self: end;
  min-height: 32px;
}

.form-actions {
  justify-content: flex-end;
  margin: 14px 0 16px;
}

.drawer-body {
  display: grid;
  gap: 14px;
  padding-bottom: 12px;
}

.drawer-group {
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.drawer-group h3 {
  margin: 0 0 12px;
  color: #0f172a;
  font-size: 15px;
}

.drawer-group :deep(.el-form-item) {
  display: grid;
  gap: 6px;
  margin-bottom: 12px;
}

.drawer-group :deep(.el-form-item:last-child) {
  margin-bottom: 0;
}

.drawer-group :deep(.el-form-item__label) {
  justify-content: flex-start;
  height: auto;
  line-height: 1.4;
  color: #334155;
  font-weight: 700;
}

.drawer-group :deep(.el-select),
.drawer-group :deep(.el-input-number),
.drawer-group :deep(.el-radio-group) {
  width: 100%;
}

.resource-option {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.resource-option strong {
  overflow: hidden;
  color: #0f172a;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-option span {
  color: #64748b;
  font-size: 12px;
}

.preview-card {
  padding: 14px;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  background: #eff6ff;
}

.preview-head,
.preview-tags,
.drawer-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.preview-head {
  justify-content: space-between;
  margin-bottom: 10px;
}

.preview-head span,
.preview-card p,
.preview-tags span {
  color: #64748b;
}

.preview-card strong {
  display: block;
  color: #0f172a;
  font-size: 16px;
  line-height: 1.5;
}

.preview-card p {
  margin: 8px 0 12px;
  line-height: 1.6;
}

.preview-action {
  display: inline-flex;
  align-items: center;
  height: 32px;
  margin-top: 12px;
  padding: 0 12px;
  border-radius: 6px;
  color: #fff;
  background: #2563eb;
  font-size: 13px;
  font-weight: 700;
}

.drawer-footer {
  justify-content: flex-end;
}

.admin-table {
  display: block;
  width: 100%;
  max-width: 100%;
  overflow-x: auto;
}

.admin-table :deep(.el-table__inner-wrapper) {
  width: max-content;
  min-width: 100%;
}

.admin-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #334155;
  font-weight: 700;
}

.toolbar {
  margin-bottom: 14px;
}

.toolbar :deep(.el-select) {
  width: 150px;
}

.toolbar :deep(.el-input) {
  width: 220px;
}

.toolbar :deep(.el-date-editor) {
  width: 260px;
}

.pagination {
  margin-top: 18px;
  justify-content: center;
}

.banner-form,
.banner-list {
  display: grid;
  gap: 12px;
}

.banner-note {
  margin: 0 0 14px;
  color: #64748b;
  line-height: 1.6;
}

.banner-inline,
.banner-upload-row,
.banner-actions,
.banner-item-meta,
.banner-row-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.banner-upload-row span {
  color: #64748b;
  font-size: 13px;
}

.banner-inline :deep(.el-input-number) {
  flex: 1;
}

.banner-preview {
  aspect-ratio: 16 / 7;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.banner-preview img,
.banner-item img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.banner-actions {
  justify-content: flex-end;
  margin-bottom: 6px;
}

.banner-list {
  margin-top: 16px;
}

.banner-item {
  display: grid;
  gap: 10px;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #f8fafc;
}

.banner-item img {
  aspect-ratio: 16 / 7;
  border-radius: 6px;
}

.banner-item-body {
  display: grid;
  gap: 6px;
}

.banner-item-body strong {
  color: #0f172a;
}

.banner-item-body span {
  overflow: hidden;
  color: #64748b;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.banner-item-meta {
  justify-content: space-between;
}

.banner-row-actions {
  justify-content: flex-end;
}

@media (max-width: 1180px) {
  .workbench-grid,
  .feedback-grid {
    grid-template-columns: 1fr;
  }

  .pool-panel,
  .banner-panel {
    position: static;
  }

  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 820px) {
  .recommend-hero,
  .panel-head {
    align-items: stretch;
    flex-direction: column;
  }

  .metric-grid,
  .config-card,
  .config-control,
  .form-grid,
  .pin-toolbar {
    grid-template-columns: 1fr;
  }

  .toolbar :deep(.el-select),
  .toolbar :deep(.el-input),
  .toolbar :deep(.el-date-editor) {
    width: 100%;
  }
}
</style>
