<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'
import { FILE_UPLOAD_URL, resolveAssetUrl } from '../utils/assets'

const loading = ref(false)
const visible = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNo: 1, pageSize: 10, keyword: '' })
const form = reactive(buildForm())
const hasUnsavedUpload = ref(false)

function buildForm() {
  return {
    id: null,
    title: '',
    coverFileId: null,
    coverUrl: '',
    teacherName: '',
    teacherAvatar: '',
    regionScope: 'NATIONAL',
    province: '全国',
    subjectName: '行测',
    examType: '国考',
    courseType: 'LIVE',
    price: 0,
    originalPrice: 0,
    tags: '',
    learnCount: 0,
    description: '',
    highlights: '',
    targetUsers: '',
    validityDays: 365,
    status: 'PUBLISHED',
    chapters: [createChapter(1)],
    materials: [createMaterial(1)]
  }
}

function createChapter(sortNo) {
  return {
    id: null,
    title: '',
    chapterType: 'RECORDED',
    durationMinutes: 60,
    sortNo,
    liveStartTime: '',
    videoFileId: null,
    videoUrl: '',
    trialAvailable: false
  }
}

function createMaterial(sortNo) {
  return {
    id: null,
    title: '',
    materialType: 'PDF',
    fileId: null,
    fileUrl: '',
    fileName: '',
    sortNo
  }
}

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/admin/courses', { params: query })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, buildForm())
  hasUnsavedUpload.value = false
}

async function openEdit(row) {
  resetForm()
  if (row?.id) {
    const data = await request.get(`/api/admin/courses/${row.id}`)
    Object.assign(form, data.course)
    form.regionScope = form.regionScope || (form.province === '全国' ? 'NATIONAL' : 'PROVINCE')
    form.chapters = (data.chapters || []).map((item, index) => ({
      ...createChapter(index + 1),
      ...item,
      liveStartTime: item.liveStartTime || ''
    }))
    form.materials = (data.materials || []).map((item, index) => ({
      ...createMaterial(index + 1),
      ...item
    }))
    if (!form.chapters.length) {
      form.chapters = [createChapter(1)]
    }
    if (!form.materials.length) {
      form.materials = [createMaterial(1)]
    }
  }
  visible.value = true
}

async function uploadCover(option) {
  const fd = new FormData()
  fd.append('file', option.file)
  fd.append('bizType', 'course')
  try {
    const data = await request.post(FILE_UPLOAD_URL, fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    form.coverFileId = data.id
    form.coverUrl = data.url
    ElMessage.success('封面上传成功')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function uploadChapterVideo(option, row) {
  const fd = new FormData()
  fd.append('file', option.file)
  fd.append('bizType', 'course-video')
  try {
    const data = await request.post(FILE_UPLOAD_URL, fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    row.videoFileId = data.id
    row.videoUrl = data.url
    hasUnsavedUpload.value = true
    ElMessage.success('章节视频上传成功，请点击保存后同步到前台')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function uploadMaterialFile(option, row) {
  const fd = new FormData()
  fd.append('file', option.file)
  fd.append('bizType', 'course-material')
  try {
    const data = await request.post(FILE_UPLOAD_URL, fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    row.fileId = data.id
    row.fileUrl = data.url
    row.fileName = option.file.name
    if (!row.title) {
      row.title = option.file.name.replace(/\.[^.]+$/, '')
    }
    ElMessage.success('课程资料上传成功')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function addChapter() {
  form.chapters.push(createChapter(form.chapters.length + 1))
}

function removeChapter(index) {
  form.chapters.splice(index, 1)
  if (!form.chapters.length) {
    form.chapters.push(createChapter(1))
  }
}

function addMaterial() {
  form.materials.push(createMaterial(form.materials.length + 1))
}

function removeMaterial(index) {
  form.materials.splice(index, 1)
  if (!form.materials.length) {
    form.materials.push(createMaterial(1))
  }
}

function normalizeLiveStartTime(value) {
  if (!value) {
    return null
  }
  if (value instanceof Date) {
    const pad = (number) => String(number).padStart(2, '0')
    return [
      value.getFullYear(),
      pad(value.getMonth() + 1),
      pad(value.getDate())
    ].join('-') + ' ' + [
      pad(value.getHours()),
      pad(value.getMinutes()),
      pad(value.getSeconds())
    ].join(':')
  }
  return String(value).trim().replace('T', ' ').replace(/\.\d+$/, '') || null
}

function buildSavePayload() {
  return {
    id: form.id,
    title: form.title,
    coverFileId: form.coverFileId,
    coverUrl: form.coverUrl,
    teacherName: form.teacherName,
    teacherAvatar: form.teacherAvatar,
    regionScope: form.regionScope,
    province: form.province,
    subjectName: form.subjectName,
    examType: form.examType,
    courseType: form.courseType,
    price: form.price,
    originalPrice: form.originalPrice,
    tags: form.tags,
    learnCount: form.learnCount,
    description: form.description,
    highlights: form.highlights,
    targetUsers: form.targetUsers,
    validityDays: form.validityDays,
    status: form.status,
    chapters: form.chapters.map((item, index) => ({
      id: item.id,
      title: item.title,
      chapterType: item.chapterType,
      durationMinutes: item.durationMinutes,
      sortNo: item.sortNo || index + 1,
      liveStartTime: normalizeLiveStartTime(item.liveStartTime),
      videoFileId: item.videoFileId,
      videoUrl: item.videoUrl,
      trialAvailable: item.trialAvailable
    })),
    materials: form.materials.map((item, index) => ({
      id: item.id,
      title: item.title,
      materialType: item.materialType,
      fileId: item.fileId,
      sortNo: item.sortNo || index + 1
    }))
  }
}

async function save() {
  try {
    await request.post('/api/admin/courses', buildSavePayload())
    ElMessage.success('保存成功')
    hasUnsavedUpload.value = false
    visible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function remove(id) {
  try {
    await request.delete(`/api/admin/courses/${id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(loadData)
</script>

<template>
  <div class="admin-page">
    <div class="toolbar glass-card">
      <el-input v-model="query.keyword" placeholder="搜索课程标题" clearable />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="warning" @click="openEdit()">新增课程</el-button>
    </div>

    <div class="glass-card table-card" v-loading="loading">
      <el-table :data="list">
        <el-table-column label="封面">
          <template #default="{ row }">
            <img v-if="row.coverUrl" :src="resolveAssetUrl(row.coverUrl)" alt="" class="table-cover" />
            <span v-else class="muted-inline">未设置</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="课程标题" />
        <el-table-column prop="teacherName" label="讲师" />
        <el-table-column prop="subjectName" label="科目" />
        <el-table-column prop="examType" label="考试类型" />
        <el-table-column prop="courseType" label="类型" />
        <el-table-column prop="price" label="现价" />
        <el-table-column prop="status" label="状态" />
        <el-table-column label="操作">
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

    <el-dialog v-model="visible" title="课程信息" width="960px">
      <el-form label-position="top">
        <el-tabs>
          <el-tab-pane label="基础信息">
            <div class="form-grid">
              <el-form-item label="课程标题"><el-input v-model="form.title" /></el-form-item>
              <el-form-item label="教师名称"><el-input v-model="form.teacherName" /></el-form-item>
              <el-form-item label="适用范围">
                <el-select v-model="form.regionScope">
                  <el-option label="全国" value="NATIONAL" />
                  <el-option label="省份" value="PROVINCE" />
                </el-select>
              </el-form-item>
              <el-form-item label="省份"><el-input v-model="form.province" /></el-form-item>
              <el-form-item label="科目"><el-input v-model="form.subjectName" /></el-form-item>
              <el-form-item label="考试类型"><el-input v-model="form.examType" /></el-form-item>
              <el-form-item label="课程类型">
                <el-select v-model="form.courseType">
                  <el-option label="直播" value="LIVE" />
                  <el-option label="录播" value="RECORDED" />
                </el-select>
              </el-form-item>
              <el-form-item label="现价"><el-input-number v-model="form.price" :min="0" style="width: 100%" /></el-form-item>
              <el-form-item label="原价"><el-input-number v-model="form.originalPrice" :min="0" style="width: 100%" /></el-form-item>
              <el-form-item label="学习人数"><el-input-number v-model="form.learnCount" :min="0" style="width: 100%" /></el-form-item>
              <el-form-item label="有效期"><el-input-number v-model="form.validityDays" :min="1" style="width: 100%" /></el-form-item>
            </div>
            <el-form-item label="课程封面">
              <el-upload :show-file-list="false" :http-request="uploadCover">
                <el-button type="primary">上传图片</el-button>
              </el-upload>
              <div class="muted">{{ form.coverFileId ? `已关联文件 #${form.coverFileId}` : '尚未上传封面' }}</div>
              <img v-if="form.coverUrl" :src="resolveAssetUrl(form.coverUrl)" alt="" class="form-cover" />
            </el-form-item>
            <el-form-item label="标签"><el-input v-model="form.tags" placeholder="直播,行测,系统班" /></el-form-item>
            <el-form-item label="课程简介"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
            <el-form-item label="课程亮点"><el-input v-model="form.highlights" type="textarea" :rows="3" /></el-form-item>
            <el-form-item label="适合人群"><el-input v-model="form.targetUsers" /></el-form-item>
          </el-tab-pane>

          <el-tab-pane label="章节安排">
            <div class="sub-toolbar">
              <div class="sub-copy">
                <strong>章节与视频</strong>
                <span>上传后需点击保存，才会同步到课程详情页和课程学习页。</span>
              </div>
              <el-button type="primary" plain @click="addChapter">新增章节</el-button>
            </div>
            <el-alert
              v-if="hasUnsavedUpload"
              title="章节视频已上传但尚未保存，保存课程后才会同步到前台。"
              type="warning"
              show-icon
              :closable="false"
              class="save-alert"
            />
            <div class="sub-list">
              <div v-for="(item, index) in form.chapters" :key="item.id || index" class="sub-card">
                <div class="sub-card-head">
                  <strong>章节 {{ index + 1 }}</strong>
                  <el-button type="danger" link @click="removeChapter(index)">移除</el-button>
                </div>
                <div class="form-grid">
                  <el-form-item label="章节标题"><el-input v-model="item.title" /></el-form-item>
                  <el-form-item label="章节类型">
                    <el-select v-model="item.chapterType">
                      <el-option label="录播" value="RECORDED" />
                      <el-option label="直播" value="LIVE" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="时长（分钟）"><el-input-number v-model="item.durationMinutes" :min="0" style="width: 100%" /></el-form-item>
                  <el-form-item label="排序"><el-input-number v-model="item.sortNo" :min="1" style="width: 100%" /></el-form-item>
                  <el-form-item label="直播开始时间">
                    <el-date-picker
                      v-model="item.liveStartTime"
                      type="datetime"
                      value-format="YYYY-MM-DD HH:mm:ss"
                      placeholder="录播可留空"
                      style="width: 100%"
                    />
                  </el-form-item>
                  <el-form-item label="允许试听"><el-switch v-model="item.trialAvailable" /></el-form-item>
                </div>
                <el-form-item label="章节视频">
                  <el-upload :show-file-list="false" :http-request="(option) => uploadChapterVideo(option, item)">
                    <el-button type="primary">上传视频</el-button>
                  </el-upload>
                  <div class="muted">{{ item.videoFileId ? `已关联视频文件 #${item.videoFileId}` : '尚未上传章节视频' }}</div>
                  <a v-if="item.videoUrl" :href="resolveAssetUrl(item.videoUrl)" target="_blank" class="preview-link">查看当前视频</a>
                </el-form-item>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="课程资料">
            <div class="sub-toolbar">
              <div class="sub-copy">
                <strong>讲义与课件</strong>
                <span>资料会同步出现在课程详情页和已购课程学习页。</span>
              </div>
              <el-button type="primary" plain @click="addMaterial">新增资料</el-button>
            </div>
            <div class="sub-list">
              <div v-for="(item, index) in form.materials" :key="item.id || index" class="sub-card">
                <div class="sub-card-head">
                  <strong>资料 {{ index + 1 }}</strong>
                  <el-button type="danger" link @click="removeMaterial(index)">移除</el-button>
                </div>
                <div class="form-grid">
                  <el-form-item label="资料标题"><el-input v-model="item.title" /></el-form-item>
                  <el-form-item label="资料类型">
                    <el-select v-model="item.materialType">
                      <el-option label="PDF" value="PDF" />
                      <el-option label="WORD" value="WORD" />
                      <el-option label="ZIP" value="ZIP" />
                      <el-option label="TXT" value="TXT" />
                    </el-select>
                  </el-form-item>
                  <el-form-item label="排序"><el-input-number v-model="item.sortNo" :min="1" style="width: 100%" /></el-form-item>
                </div>
                <el-form-item label="资料文件">
                  <el-upload :show-file-list="false" :http-request="(option) => uploadMaterialFile(option, item)">
                    <el-button type="primary">上传资料</el-button>
                  </el-upload>
                  <div class="muted">{{ item.fileId ? `已关联资料文件 #${item.fileId}` : '尚未上传资料文件' }}</div>
                  <a v-if="item.fileUrl" :href="resolveAssetUrl(item.fileUrl)" target="_blank" class="preview-link">{{ item.fileName || '查看当前资料' }}</a>
                </el-form-item>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
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

.form-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, 1fr);
}

.muted,
.sub-copy span,
.muted-inline {
  color: var(--muted);
}

.muted {
  margin-top: 10px;
}

.sub-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.sub-copy {
  display: grid;
  gap: 4px;
}

.sub-list {
  display: grid;
  gap: 16px;
}

.save-alert {
  margin-bottom: 16px;
}

.sub-card {
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 16px;
}

.sub-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.preview-link {
  display: inline-flex;
  margin-top: 8px;
}

.table-cover {
  width: 88px;
  height: 52px;
  object-fit: cover;
  border-radius: 10px;
}

.form-cover {
  display: block;
  width: 180px;
  height: 108px;
  object-fit: cover;
  border-radius: 12px;
  margin-top: 12px;
}

@media (max-width: 900px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .sub-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
