<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import {
  Bell,
  Collection,
  DataAnalysis,
  EditPen,
  FolderOpened,
  MoreFilled,
  Plus,
  Search,
  UserFilled
} from '@element-plus/icons-vue'
import request from '../utils/request'
import ClassroomChatPanel from '../components/ClassroomChatPanel.vue'
import { resolveAssetUrl, resolveClassroomCoverUrl, resolveFileUploadUrl, resolveFileViewUrl } from '../utils/assets'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const detailLoading = ref(false)
const materialSaving = ref(false)
const list = ref([])
const total = ref(0)
const editDrawer = ref(false)
const detailDrawer = ref(false)
const materialPreviewVisible = ref(false)
const materialPreview = ref({})
const detail = ref({ classroom: {}, members: [], joinRequests: [], materials: [], homeworks: [], classroomExams: [] })
const query = reactive({ pageNo: 1, pageSize: 10, keyword: '' })
const statusFilter = ref('ALL')
const form = reactive({
  id: null,
  name: '',
  subjectName: '',
  coverFileId: null,
  coverUrl: '',
  intro: '',
  capacity: 100,
  isPublic: true,
  inviteCode: '',
  status: 'OPEN'
})
const materialForm = reactive({
  id: null,
  classroomId: null,
  materialCategory: 'TRIAL_VIDEO',
  title: '',
  fileId: null,
  fileName: '',
  fileType: '',
  fileUrl: '',
  description: '',
  sortNo: 1,
  status: 'ENABLED'
})
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('civil-token') || ''}` }
const materialVideoAccept = 'video/*,.mp4,.mov,.m4v,.avi,.wmv,.flv,.mkv,.webm'
const materialDocumentAccept = '.pdf,.doc,.docx,.ppt,.pptx,.xls,.xlsx,.csv,.txt,.zip,.rar,.7z'
const materialFileMaxSizeMb = 500
const visibilityMode = computed({
  get: () => (form.isPublic ? 'PUBLIC' : 'PRIVATE'),
  set: (value) => {
    form.isPublic = value === 'PUBLIC'
    if (form.isPublic) {
      form.inviteCode = ''
    } else {
      ensureInviteCode()
    }
  }
})
const filteredList = computed(() => {
  if (statusFilter.value === 'ALL') {
    return list.value
  }
  if (statusFilter.value === 'PUBLIC') {
    return list.value.filter((item) => item.isPublic)
  }
  if (statusFilter.value === 'PRIVATE') {
    return list.value.filter((item) => !item.isPublic)
  }
  return list.value
})
const materialUploadAccept = computed(() => (materialForm.materialCategory === 'TRIAL_VIDEO' ? materialVideoAccept : materialDocumentAccept))
const materialUploadTip = computed(() => (
  materialForm.materialCategory === 'TRIAL_VIDEO'
    ? '支持 MP4、MOV、AVI、WMV、FLV、MKV、WEBM 等视频，单个文件不超过 500MB'
    : '支持 PDF、Word、PPT、Excel、CSV、TXT 和压缩包，单个文件不超过 500MB'
))
const materialPreviewUrl = computed(() => {
  if (materialPreview.value.fileUrl) {
    return resolveAssetUrl(materialPreview.value.fileUrl)
  }
  return resolveFileViewUrl(materialPreview.value.fileId)
})
const officePreviewUrl = computed(() => {
  if (!materialPreviewUrl.value) {
    return ''
  }
  const absoluteUrl = new URL(materialPreviewUrl.value, window.location.origin).href
  return `https://view.officeapps.live.com/op/embed.aspx?src=${encodeURIComponent(absoluteUrl)}`
})
const summary = computed(() => {
  return list.value.reduce(
    (acc, item) => {
      acc.classroomCount += 1
      acc.memberCount += Number(item.memberCount || 0)
      acc.pendingJoinCount += Number(item.pendingJoinCount || 0)
      acc.homeworkCount += Number(item.homeworkCount || 0)
      acc.examCount += Number(item.examCount || 0)
      return acc
    },
    { classroomCount: 0, memberCount: 0, pendingJoinCount: 0, homeworkCount: 0, examCount: 0 }
  )
})

function generateInviteCode() {
  return String(Math.floor(100000 + Math.random() * 900000))
}

function ensureInviteCode() {
  if (!/^\d{6}$/.test(form.inviteCode || '')) {
    form.inviteCode = generateInviteCode()
  }
}

function refreshInviteCode() {
  form.inviteCode = generateInviteCode()
}

function statusText(status) {
  if (status === 'OPEN') {
    return '开放中'
  }
  if (status === 'DISMISSED') {
    return '已解散'
  }
  return status || '未设置'
}

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/teacher/classrooms', { params: query })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, { id: null, name: '', subjectName: '', coverFileId: null, coverUrl: '', intro: '', capacity: 100, isPublic: true, inviteCode: '', status: 'OPEN' })
}

function resetMaterialForm(classroomId = detail.value.classroom?.id || null) {
  Object.assign(materialForm, {
    id: null,
    classroomId,
    materialCategory: 'TRIAL_VIDEO',
    title: '',
    fileId: null,
    fileName: '',
    fileType: '',
    fileUrl: '',
    description: '',
    sortNo: 1,
    status: 'ENABLED'
  })
}

async function saveClassroom() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入班级名称')
    return
  }
  if (!form.subjectName.trim()) {
    ElMessage.warning('请输入科目')
    return
  }
  if (form.isPublic) {
    form.inviteCode = ''
  } else {
    ensureInviteCode()
  }

  saving.value = true
  try {
    await request.post('/api/teacher/classrooms', form)
    ElMessage.success(form.id ? '班级更新成功' : '班级保存成功')
    resetForm()
    editDrawer.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

function editClassroom(item) {
  Object.assign(form, {
    id: item.id,
    name: item.name,
    subjectName: item.subjectName,
    coverFileId: item.coverFileId,
    coverUrl: item.coverUrl,
    intro: item.intro,
    capacity: item.capacity,
    isPublic: item.isPublic,
    inviteCode: item.inviteCode || '',
    status: item.status
  })
  editDrawer.value = true
}

function handleUploadSuccess(response) {
  if (response.code === 200) {
    form.coverFileId = response.data.id
    form.coverUrl = response.data.url
    ElMessage.success('封面上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

function handleMaterialUploadSuccess(response) {
  if (response.code === 200) {
    materialForm.fileId = response.data.id
    materialForm.fileName = response.data.name || ''
    materialForm.fileType = response.data.fileType || ''
    materialForm.fileUrl = response.data.url || resolveFileViewUrl(response.data.id)
    ElMessage.success('材料文件上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

function beforeMaterialUpload(file) {
  const sizeMb = file.size / 1024 / 1024
  if (sizeMb > materialFileMaxSizeMb) {
    ElMessage.warning(`文件不能超过 ${materialFileMaxSizeMb}MB`)
    return false
  }
  return true
}

function handleMaterialUploadError(error) {
  let message = '材料文件上传失败'
  try {
    const payload = JSON.parse(error?.message || '{}')
    message = payload.message || message
  } catch (ignored) {
    if (error?.message) {
      message = error.message
    }
  }
  ElMessage.error(message)
}

async function loadDetail(classroomId) {
  detailLoading.value = true
  try {
    detail.value = await request.get(`/api/teacher/classrooms/${classroomId}/detail`)
    resetMaterialForm(classroomId)
  } finally {
    detailLoading.value = false
  }
}

async function openDetail(classroomId) {
  detailDrawer.value = true
  await loadDetail(classroomId)
}

function handleClassCommand(command, row) {
  if (command === 'edit') {
    editClassroom(row)
  } else if (command === 'detail') {
    openDetail(row.id)
  } else if (command === 'homework') {
    router.push('/teacher/homework')
  } else if (command === 'exam') {
    router.push('/teacher/exam')
  } else if (command === 'report') {
    router.push('/teacher/report')
  }
}

async function reviewRequest(id, status) {
  try {
    await request.post('/api/teacher/classrooms/join-requests/review', { requestId: id, status })
    ElMessage.success('审核完成')
    await Promise.all([loadDetail(detail.value.classroom.id), loadData()])
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function removeMember(row) {
  try {
    await ElMessageBox.confirm(`确认将 ${row.nickname} 移出班级吗？`, '移除成员', { type: 'warning' })
    await request.delete(`/api/teacher/classrooms/${detail.value.classroom.id}/members/${row.userId}`)
    ElMessage.success('成员已移除')
    await Promise.all([loadDetail(detail.value.classroom.id), loadData()])
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '移除失败')
    }
  }
}

function editMaterial(item) {
  Object.assign(materialForm, {
    id: item.id,
    classroomId: item.classroomId,
    materialCategory: item.materialCategory,
    title: item.title,
    fileId: item.fileId,
    fileName: item.fileName || '',
    fileType: item.fileType || '',
    fileUrl: item.fileUrl || '',
    description: item.description,
    sortNo: item.sortNo,
    status: item.status
  })
}

function fileExtension(item) {
  const name = item?.fileName || item?.fileUrl || ''
  const matched = String(name).toLowerCase().match(/\.([a-z0-9]+)(?:\?|#|$)/)
  return matched ? matched[1] : ''
}

function materialMime(item) {
  return String(item?.fileType || '').toLowerCase()
}

function isVideoMaterial(item) {
  const extension = fileExtension(item)
  return materialMime(item).startsWith('video/') || ['mp4', 'mov', 'm4v', 'avi', 'wmv', 'flv', 'mkv', 'webm'].includes(extension)
}

function isPdfMaterial(item) {
  return materialMime(item) === 'application/pdf' || fileExtension(item) === 'pdf'
}

function isImageMaterial(item) {
  return materialMime(item).startsWith('image/') || ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp', 'svg'].includes(fileExtension(item))
}

function isTextMaterial(item) {
  return materialMime(item).startsWith('text/') || ['txt', 'csv', 'md', 'json', 'xml'].includes(fileExtension(item))
}

function isOfficeMaterial(item) {
  return ['doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx'].includes(fileExtension(item))
}

function previewMaterial(item) {
  if (!item?.fileId && !item?.fileUrl) {
    ElMessage.warning('请先上传材料文件')
    return
  }
  materialPreview.value = { ...item }
  materialPreviewVisible.value = true
}

function previewCurrentMaterial() {
  previewMaterial({
    title: materialForm.title || '当前材料',
    materialCategory: materialForm.materialCategory,
    fileId: materialForm.fileId,
    fileName: materialForm.fileName,
    fileType: materialForm.fileType,
    fileUrl: materialForm.fileUrl
  })
}

async function saveMaterial() {
  materialSaving.value = true
  try {
    await request.post('/api/teacher/classrooms/materials', materialForm)
    ElMessage.success(materialForm.id ? '材料更新成功' : '材料保存成功')
    await loadDetail(detail.value.classroom.id)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    materialSaving.value = false
  }
}

async function deleteMaterial(id) {
  try {
    await ElMessageBox.confirm('确认删除这条材料吗？', '删除材料', { type: 'warning' })
    await request.delete(`/api/teacher/classrooms/materials/${id}`)
    ElMessage.success('材料已删除')
    await loadDetail(detail.value.classroom.id)
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

async function dismissClassroom(classroomId = detail.value.classroom?.id) {
  if (!classroomId) {
    return
  }
  try {
    await ElMessageBox.confirm('解散后班级将转为不可加入状态，学员会被移出班级。确认继续吗？', '解散班级', { type: 'warning' })
    await request.post(`/api/teacher/classrooms/${classroomId}/dismiss`)
    ElMessage.success('班级已解散')
    if (detailDrawer.value && detail.value.classroom?.id === classroomId) {
      detailDrawer.value = false
    }
    if (form.id === classroomId) {
      resetForm()
    }
    await loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '解散失败')
    }
  }
}

onMounted(loadData)
</script>

<template>
  <div class="teacher-page">
    <section class="class-hero">
      <div class="hero-copy">
        <h1>班级管理</h1>
        <p>管理班级成员、入班申请、教学材料和任务数据。</p>
      </div>
      <el-button type="primary" @click="router.push('/teacher/class/create')">
        <el-icon><Plus /></el-icon>
        新建班级
      </el-button>
    </section>

    <section class="summary-grid">
      <div class="summary-card">
        <span class="summary-icon blue"><el-icon><Collection /></el-icon></span>
        <div>
          <span>我的班级</span>
          <strong>{{ summary.classroomCount }}</strong>
        </div>
      </div>
      <div class="summary-card">
        <span class="summary-icon green"><el-icon><UserFilled /></el-icon></span>
        <div>
          <span>总学员数</span>
          <strong>{{ summary.memberCount }}</strong>
        </div>
      </div>
      <div class="summary-card">
        <span class="summary-icon amber"><el-icon><Bell /></el-icon></span>
        <div>
          <span>待审核申请</span>
          <strong>{{ summary.pendingJoinCount }}</strong>
        </div>
      </div>
      <div class="summary-card">
        <span class="summary-icon violet"><el-icon><DataAnalysis /></el-icon></span>
        <div>
          <span>作业 / 考试</span>
          <strong>{{ summary.homeworkCount + summary.examCount }}</strong>
        </div>
      </div>
    </section>

    <section class="class-list-panel" v-loading="loading">
      <div class="list-toolbar">
        <el-input
          v-model="query.keyword"
          class="search-input"
          clearable
          placeholder="搜索班级名称 / 科目"
          @keyup.enter="loadData"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-segmented
          v-model="statusFilter"
          class="status-segmented"
          :options="[
            { label: '全部', value: 'ALL' },
            { label: '公开', value: 'PUBLIC' },
            { label: '私密', value: 'PRIVATE' }
          ]"
        />
        <div class="toolbar-actions">
          <el-button @click="loadData">查询</el-button>
          <el-button type="primary" @click="router.push('/teacher/class/create')">
            <el-icon><Plus /></el-icon>
            新建班级
          </el-button>
        </div>
      </div>

      <el-table :data="filteredList" class="class-table" table-layout="fixed">
        <el-table-column label="班级" min-width="340">
          <template #default="{ row }">
            <div class="class-cell">
              <img :src="resolveClassroomCoverUrl(row.coverUrl, row.subjectName, row.name)" alt="" class="table-cover" />
              <div>
                <strong>{{ row.name }}</strong>
                <span>{{ row.subjectName || '未设置科目' }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="招生状态" min-width="140">
          <template #default="{ row }">
            <div class="status-stack">
              <el-tag :type="row.isPublic ? 'primary' : 'info'">{{ row.isPublic ? '公开招生' : '私密邀请' }}</el-tag>
              <span>{{ statusText(row.status) }}</span>
              <span v-if="!row.isPublic && row.inviteCode" class="invite-code">邀请码 {{ row.inviteCode }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="学员" min-width="140">
          <template #default="{ row }">
            <div class="metric-stack">
              <strong>{{ row.memberCount || 0 }} / {{ row.capacity || 0 }}</strong>
              <span>剩余 {{ row.remainingSeats || 0 }} 个名额</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="待处理" min-width="180">
          <template #default="{ row }">
            <div v-if="row.pendingJoinCount" class="pending-tags">
              <el-tag v-if="row.pendingJoinCount" type="warning">入班申请 {{ row.pendingJoinCount }}</el-tag>
            </div>
            <span v-else class="muted-text">无待办</span>
          </template>
        </el-table-column>
        <el-table-column label="教学内容" min-width="190">
          <template #default="{ row }">
            <div class="content-chips">
              <span>材料 {{ row.materialCount || 0 }}</span>
              <span>作业 {{ row.homeworkCount || 0 }}</span>
              <span>考试 {{ row.examCount || 0 }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button link type="primary" @click="openDetail(row.id)">进入班级</el-button>
              <el-button link @click="openDetail(row.id)">成员</el-button>
              <el-button link @click="router.push('/teacher/homework')">任务</el-button>
              <el-button v-if="row.status !== 'DISMISSED'" link type="danger" @click="dismissClassroom(row.id)">解散</el-button>
              <el-dropdown trigger="click" @command="(command) => handleClassCommand(command, row)">
                <el-button link>
                  更多
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">
                      <el-icon><EditPen /></el-icon>
                      编辑资料
                    </el-dropdown-item>
                    <el-dropdown-item command="detail">
                      <el-icon><FolderOpened /></el-icon>
                      教学材料
                    </el-dropdown-item>
                    <el-dropdown-item command="exam">发布考试</el-dropdown-item>
                    <el-dropdown-item command="report">成绩报表</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-state">
            <el-icon><Collection /></el-icon>
            <strong>还没有班级</strong>
            <span>创建班级后可以发布作业、考试并管理学员。</span>
            <el-button type="primary" @click="router.push('/teacher/class/create')">新建班级</el-button>
          </div>
        </template>
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

    <el-drawer v-model="editDrawer" size="520px" title="编辑班级资料" @closed="resetForm">
      <div class="edit-form">
        <label class="field-block">
          <span>班级名称</span>
          <el-input v-model="form.name" placeholder="班级名称" />
        </label>
        <label class="field-block">
          <span>所属科目</span>
          <el-input v-model="form.subjectName" placeholder="科目" />
        </label>
        <label class="field-block">
          <span>班级容量</span>
          <el-input-number v-model="form.capacity" :min="1" :max="999" controls-position="right" />
        </label>
        <label class="field-block">
          <span>招生方式</span>
          <el-radio-group v-model="visibilityMode" class="visibility-options">
            <el-radio-button value="PUBLIC">公开招生</el-radio-button>
            <el-radio-button value="PRIVATE">私密邀请</el-radio-button>
          </el-radio-group>
        </label>
        <label v-if="!form.isPublic" class="field-block invite-field">
          <span>邀请码</span>
          <el-input v-model="form.inviteCode" readonly maxlength="6">
            <template #append>
              <el-button @click="refreshInviteCode">重新生成</el-button>
            </template>
          </el-input>
        </label>
        <label class="field-block">
          <span>班级简介</span>
          <el-input v-model="form.intro" type="textarea" :rows="4" placeholder="班级简介" />
        </label>
        <div class="field-block">
          <span>班级封面</span>
          <el-upload
            :action="resolveFileUploadUrl('classroom')"
            :headers="uploadHeaders"
            :show-file-list="false"
            accept="image/*"
            @success="handleUploadSuccess"
          >
            <div class="drawer-upload">
              <img :src="resolveClassroomCoverUrl(form.coverUrl, form.subjectName, form.name)" alt="" />
              <span>{{ form.coverUrl ? '替换封面' : '上传封面' }}</span>
            </div>
          </el-upload>
          <el-alert
            v-if="form.id && form.coverUrl && !form.coverFileId"
            title="当前是历史封面数据，可直接保留；如需切到统一文件链路，请重新上传替换。"
            type="warning"
            :closable="false"
          />
        </div>
        <div class="drawer-actions">
          <el-button type="primary" :loading="saving" @click="saveClassroom">保存修改</el-button>
          <el-button @click="editDrawer = false">取消</el-button>
        </div>
      </div>
    </el-drawer>

    <el-drawer v-model="detailDrawer" size="72%" title="班级运营工作台">
      <div v-loading="detailLoading" class="detail-layout">
        <section class="detail-summary">
          <img
            :src="resolveClassroomCoverUrl(detail.classroom.coverUrl, detail.classroom.subjectName, detail.classroom.name)"
            alt=""
            class="detail-cover"
          />
          <div class="detail-meta">
            <div class="section-title">{{ detail.classroom.name || '班级详情' }}</div>
            <p class="muted-text">{{ detail.classroom.intro }}</p>
            <div class="meta-grid">
              <div>科目：{{ detail.classroom.subjectName || '-' }}</div>
              <div>公开状态：{{ detail.classroom.visibilityLabel || '-' }}</div>
              <div>班级状态：{{ detail.classroom.status || '-' }}</div>
              <div>容量：{{ detail.classroom.studentCount || 0 }}/{{ detail.classroom.capacity || 0 }}</div>
              <div>待审核：{{ detail.classroom.pendingJoinCount || 0 }}</div>
              <div>材料数：{{ detail.classroom.materialCount || 0 }}</div>
            </div>
            <div class="summary-actions">
              <el-button
                v-if="detail.classroom.id && detail.classroom.status !== 'DISMISSED'"
                type="danger"
                plain
                @click="dismissClassroom()"
              >
                解散班级
              </el-button>
            </div>
          </div>
        </section>

        <section class="detail-card">
          <ClassroomChatPanel :classroom-id="detail.classroom.id" />
        </section>

        <section class="detail-card">
          <div class="section-title">成员管理</div>
          <el-table :data="detail.members">
            <el-table-column prop="nickname" label="昵称" min-width="120" />
            <el-table-column prop="realName" label="姓名" min-width="120" />
            <el-table-column prop="roleCode" label="角色" min-width="100" />
            <el-table-column prop="phone" label="手机号" min-width="140" />
            <el-table-column prop="province" label="省份" min-width="100" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button v-if="row.roleCode === 'STUDENT'" link type="danger" @click="removeMember(row)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="detail-card">
          <div class="section-title">入班申请</div>
          <el-table :data="detail.joinRequests">
            <el-table-column prop="nickname" label="学员" min-width="120" />
            <el-table-column prop="province" label="省份" min-width="100" />
            <el-table-column prop="examType" label="考试类型" min-width="100" />
            <el-table-column prop="remark" label="备注" min-width="180" />
            <el-table-column prop="status" label="状态" min-width="100" />
            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button v-if="row.status === 'PENDING'" link type="primary" @click="reviewRequest(row.id, 'APPROVED')">通过</el-button>
                <el-button v-if="row.status === 'PENDING'" link type="danger" @click="reviewRequest(row.id, 'REJECTED')">拒绝</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>

        <section class="detail-card">
          <div class="section-title">教学材料维护</div>
          <div class="material-form">
            <el-select v-model="materialForm.materialCategory">
              <el-option label="试听视频" value="TRIAL_VIDEO" />
              <el-option label="试用资料" value="TRIAL_DOCUMENT" />
            </el-select>
            <el-input v-model="materialForm.title" placeholder="材料标题" />
            <el-input-number v-model="materialForm.sortNo" :min="1" />
          </div>
          <el-input v-model="materialForm.description" type="textarea" :rows="2" placeholder="材料说明" style="margin-top: 12px" />
          <div class="material-action-row">
            <el-upload
              :action="resolveFileUploadUrl('classroom-material')"
              :headers="uploadHeaders"
              :show-file-list="false"
              :accept="materialUploadAccept"
              :before-upload="beforeMaterialUpload"
              @success="handleMaterialUploadSuccess"
              @error="handleMaterialUploadError"
            >
              <el-button>{{ materialForm.fileId ? '替换材料文件' : '上传材料文件' }}</el-button>
            </el-upload>
            <el-button v-if="materialForm.fileId" link type="primary" @click="previewCurrentMaterial">预览当前文件</el-button>
            <el-button type="primary" :loading="materialSaving" @click="saveMaterial">保存材料</el-button>
            <el-button v-if="materialForm.id" @click="resetMaterialForm()">取消编辑</el-button>
          </div>
          <div class="upload-tip">{{ materialUploadTip }}</div>
          <el-table :data="detail.materials" style="margin-top: 16px">
            <el-table-column prop="materialCategoryLabel" label="类型" min-width="100" />
            <el-table-column prop="title" label="标题" min-width="160" />
            <el-table-column prop="fileName" label="文件" min-width="180" />
            <el-table-column prop="sortNo" label="排序" min-width="80" />
            <el-table-column prop="status" label="状态" min-width="100" />
            <el-table-column label="操作" width="160">
              <template #default="{ row }">
                <el-button link type="primary" @click="editMaterial(row)">编辑</el-button>
                <el-button link type="primary" @click="previewMaterial(row)">预览</el-button>
                <el-button link type="danger" @click="deleteMaterial(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </div>
    </el-drawer>

    <el-dialog
      v-model="materialPreviewVisible"
      :title="materialPreview.title || materialPreview.fileName || '材料预览'"
      width="min(960px, 92vw)"
      destroy-on-close
      append-to-body
    >
      <div class="material-preview">
        <video
          v-if="isVideoMaterial(materialPreview)"
          :src="materialPreviewUrl"
          class="preview-video"
          controls
          preload="metadata"
        />
        <iframe
          v-else-if="isPdfMaterial(materialPreview) || isTextMaterial(materialPreview)"
          :src="materialPreviewUrl"
          class="preview-frame"
          title="材料预览"
        />
        <img
          v-else-if="isImageMaterial(materialPreview)"
          :src="materialPreviewUrl"
          :alt="materialPreview.fileName || materialPreview.title"
          class="preview-image"
        />
        <iframe
          v-else-if="isOfficeMaterial(materialPreview)"
          :src="officePreviewUrl"
          class="preview-frame"
          title="Office 文档预览"
        />
        <div v-else class="preview-empty">
          <strong>当前文件类型暂不支持嵌入预览</strong>
          <span>可以在新窗口打开或下载后查看。</span>
        </div>
      </div>
      <template #footer>
        <a :href="materialPreviewUrl" target="_blank" class="inline-link">新窗口打开</a>
        <el-button @click="materialPreviewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.teacher-page {
  display: grid;
  gap: 18px;
  width: 100%;
  max-width: 1320px;
  margin: 0 auto;
  padding: 28px 16px 40px;
  overflow-x: clip;
}

.class-hero,
.summary-card,
.class-list-panel {
  border: 1px solid rgba(205, 216, 232, 0.78);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}

.class-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 24px 28px;
  min-width: 0;
}

h1,
h2,
p {
  margin: 0;
}

h1 {
  color: #0f172a;
  font-size: 30px;
  line-height: 1.25;
  font-weight: 800;
}

.hero-copy p {
  margin-top: 8px;
  color: var(--muted);
  font-size: 14px;
}

.class-hero :deep(.el-button),
.toolbar-actions :deep(.el-button),
.drawer-actions :deep(.el-button) {
  border-radius: 8px;
  font-weight: 700;
}

.class-hero :deep(.el-icon),
.toolbar-actions :deep(.el-icon) {
  margin-right: 4px;
}

.summary-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  min-width: 0;
}

.summary-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 8px;
  font-size: 20px;
}

.summary-icon.blue {
  color: #2563eb;
  background: #eaf2ff;
}

.summary-icon.green {
  color: #15803d;
  background: #ecfdf3;
}

.summary-icon.amber {
  color: #d97706;
  background: #fff7e8;
}

.summary-icon.violet {
  color: #7c3aed;
  background: #f3e8ff;
}

.summary-card span:not(.summary-icon) {
  color: #64748b;
  font-size: 13px;
}

.summary-card strong {
  display: block;
  margin-top: 4px;
  color: #0f172a;
  font-size: 30px;
  line-height: 1;
  font-weight: 800;
}

.class-list-panel {
  padding: 22px;
  min-width: 0;
  overflow: hidden;
}

.list-toolbar {
  display: grid;
  grid-template-columns: minmax(240px, 360px) max-content minmax(220px, 1fr);
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  min-width: 0;
}

.search-input {
  min-width: 0;
  max-width: 360px;
}

.status-segmented {
  width: max-content;
}

.toolbar-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}

.class-table {
  width: 100%;
  border: 1px solid #e5edf7;
  border-radius: 10px;
  overflow: hidden;
}

.class-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.table-cover {
  flex: 0 0 auto;
  width: 112px;
  height: 64px;
  object-fit: cover;
  border-radius: 8px;
  background: #eef4fb;
}

.class-cell strong {
  display: block;
  color: #0f172a;
  font-size: 15px;
  line-height: 1.45;
}

.class-cell span,
.status-stack span,
.metric-stack span,
.muted-text {
  color: var(--muted);
  font-size: 13px;
}

.status-stack,
.metric-stack {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.invite-code {
  font-family: Consolas, "Courier New", monospace;
}

.metric-stack strong {
  color: #0f172a;
  font-size: 16px;
}

.pending-tags,
.content-chips,
.row-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.content-chips span {
  border-radius: 8px;
  padding: 6px 9px;
  color: #475569;
  background: #eef4fb;
  font-size: 12px;
  font-weight: 700;
}

.row-actions :deep(.el-button) {
  margin-left: 0;
  font-weight: 700;
}

.empty-state {
  display: grid;
  place-items: center;
  gap: 10px;
  min-height: 260px;
  color: var(--muted);
  text-align: center;
}

.empty-state .el-icon {
  color: #94a3b8;
  font-size: 36px;
}

.empty-state strong {
  color: #0f172a;
  font-size: 18px;
}

.edit-form {
  display: grid;
  gap: 18px;
}

.edit-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field-block {
  display: grid;
  gap: 8px;
}

.field-block > span {
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}

.field-block :deep(.el-input-number),
.field-block :deep(.el-select),
.visibility-options {
  width: 100%;
}

.visibility-options {
  display: flex;
}

.invite-field {
  max-width: 360px;
}

.cover-tip,
.muted-text {
  color: var(--muted);
}

.drawer-upload {
  position: relative;
  overflow: hidden;
  width: 100%;
  aspect-ratio: 16 / 9;
  border: 1px dashed #b8c8dc;
  border-radius: 10px;
  cursor: pointer;
}

.drawer-upload img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.drawer-upload span {
  position: absolute;
  right: 12px;
  bottom: 12px;
  border-radius: 8px;
  padding: 8px 10px;
  color: #fff;
  background: rgba(15, 23, 42, 0.72);
  font-size: 13px;
  font-weight: 700;
}

.drawer-actions {
  display: flex;
  gap: 10px;
  padding-top: 16px;
  border-top: 1px solid #e5edf7;
}

.detail-layout {
  display: grid;
  gap: 20px;
}

.detail-summary,
.detail-card {
  border: 1px solid #dbe5f2;
  border-radius: 12px;
  background: #fff;
  padding: 18px;
}

.detail-summary {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 18px;
}

.detail-cover {
  width: 100%;
  height: 140px;
  object-fit: cover;
  border-radius: 10px;
}

.detail-meta {
  display: grid;
  gap: 12px;
}

.meta-grid {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  color: var(--muted);
}

.summary-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.material-action-row {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 12px;
}

.upload-tip {
  margin-top: 8px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.5;
}

.material-preview {
  min-height: 360px;
  border: 1px solid #dbe5f2;
  border-radius: 10px;
  background: #f8fafc;
  overflow: hidden;
}

.preview-video,
.preview-frame,
.preview-image {
  display: block;
  width: 100%;
}

.preview-video {
  max-height: 68vh;
  background: #020617;
}

.preview-frame {
  height: 68vh;
  border: 0;
  background: #fff;
}

.preview-image {
  max-height: 68vh;
  object-fit: contain;
  background: #fff;
}

.preview-empty {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  min-height: 360px;
  color: var(--muted);
}

.preview-empty strong {
  color: #1f2937;
}

.inline-link {
  color: var(--primary);
  text-decoration: none;
}

@media (max-width: 1080px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .list-toolbar {
    grid-template-columns: 1fr;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }
}

@media (max-width: 900px) {
  .detail-summary {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .teacher-page {
    padding: 18px 12px 32px;
  }

  .class-hero {
    align-items: flex-start;
    flex-direction: column;
    padding: 18px;
  }

  h1 {
    font-size: 26px;
  }

  .summary-grid,
  .edit-grid {
    grid-template-columns: 1fr;
  }

  .class-list-panel {
    padding: 14px;
  }

  .class-cell {
    align-items: flex-start;
    flex-direction: column;
  }

  .table-cover {
    width: 160px;
    max-width: 100%;
    height: 90px;
  }
}
</style>
