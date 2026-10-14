<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'
import { FILE_UPLOAD_URL, resolveAssetUrl } from '../utils/assets'

const allowedCategoryNames = ['国考', '省考', '选调', '事业单位', '军队文职', '面试']
const loading = ref(false)
const visible = ref(false)
const list = ref([])
const total = ref(0)
const categories = ref([])
const infoCategories = ref([])
const provinces = ref([])
const query = reactive({ pageNo: 1, pageSize: 10, keyword: '', categoryId: null, infoCategory: '', province: '' })
const form = reactive({
  id: null,
  categoryId: null,
  infoCategory: '考试公告',
  province: '全国',
  title: '',
  summary: '',
  coverFileId: null,
  coverUrl: '',
  content: '',
  tags: '',
  status: 'PUBLISHED',
  publishedAt: ''
})

async function loadOptions() {
  const data = await request.get('/api/news/filter-options')
  categories.value = (data.examProjects || []).filter((item) => allowedCategoryNames.includes(item.name))
  infoCategories.value = data.infoCategories || []
  provinces.value = data.provinces || []
}

function buildQueryParams() {
  return {
    pageNo: query.pageNo,
    pageSize: query.pageSize,
    keyword: query.keyword || undefined,
    categoryId: query.categoryId || undefined,
    infoCategory: query.infoCategory || undefined,
    province: query.province || undefined
  }
}

async function loadData() {
  loading.value = true
  try {
    if (!categories.value.length) {
      await loadOptions()
    }
    const newsData = await request.get('/api/admin/news', { params: buildQueryParams() })
    list.value = newsData.records
    total.value = newsData.total
  } finally {
    loading.value = false
  }
}

function resetForm() {
  Object.assign(form, {
    id: null,
    categoryId: categories.value[0]?.id || null,
    infoCategory: '考试公告',
    province: '全国',
    title: '',
    summary: '',
    coverFileId: null,
    coverUrl: '',
    content: '',
    tags: '',
    status: 'PUBLISHED',
    publishedAt: ''
  })
}

function openEdit(row) {
  if (row) {
    Object.assign(form, {
      ...row,
      infoCategory: row.infoCategory || '考试公告',
      province: row.province || '全国'
    })
  } else {
    resetForm()
  }
  visible.value = true
}

function buildSavePayload() {
  const payload = {
    id: form.id,
    categoryId: form.categoryId,
    infoCategory: form.infoCategory,
    province: form.province,
    title: form.title,
    summary: form.summary,
    coverFileId: form.coverFileId,
    coverUrl: form.coverUrl,
    content: form.content,
    tags: form.tags,
    status: form.status,
    publishedAt: form.publishedAt
  }
  if (!payload.id) delete payload.id
  if (!payload.coverFileId) delete payload.coverFileId
  if (!payload.coverUrl) delete payload.coverUrl
  if (!payload.publishedAt) delete payload.publishedAt
  return payload
}

async function uploadCover(option) {
  const fd = new FormData()
  fd.append('file', option.file)
  fd.append('bizType', 'news')
  try {
    const data = await request.post(FILE_UPLOAD_URL, fd, { headers: { 'Content-Type': 'multipart/form-data' } })
    form.coverFileId = data.id
    form.coverUrl = data.url
    ElMessage.success('封面上传成功')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function save() {
  if (!form.categoryId) {
    ElMessage.warning('请选择考试项目')
    return
  }
  if (!form.infoCategory) {
    ElMessage.warning('请选择资讯分类')
    return
  }
  if (!form.province) {
    ElMessage.warning('请选择招考省份')
    return
  }
  try {
    await request.post('/api/admin/news', buildSavePayload())
    ElMessage.success('保存成功')
    visible.value = false
    loadData()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function remove(id) {
  try {
    await request.delete(`/api/admin/news/${id}`)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function categoryName(id) {
  return categories.value.find((item) => item.id === id)?.name || id
}

onMounted(loadData)
</script>

<template>
  <div class="admin-page">
    <div class="toolbar glass-card">
      <el-input v-model="query.keyword" placeholder="搜索资讯标题" clearable />
      <el-select v-model="query.categoryId" clearable placeholder="考试项目">
        <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-select v-model="query.province" clearable placeholder="招考省份">
        <el-option v-for="item in provinces" :key="item" :label="item" :value="item" />
      </el-select>
      <el-select v-model="query.infoCategory" clearable placeholder="资讯分类">
        <el-option v-for="item in infoCategories" :key="item" :label="item" :value="item" />
      </el-select>
      <el-button type="primary" @click="() => { query.pageNo = 1; loadData() }">查询</el-button>
      <el-button type="warning" @click="openEdit()">新增资讯</el-button>
    </div>

    <div class="glass-card table-card" v-loading="loading">
      <el-table :data="list">
        <el-table-column label="封面" width="110">
          <template #default="{ row }">
            <img v-if="row.coverUrl" :src="resolveAssetUrl(row.coverUrl)" alt="" class="table-cover" />
            <span v-else class="muted">未设置</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="240" />
        <el-table-column label="考试项目" width="110">
          <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
        </el-table-column>
        <el-table-column prop="province" label="招考省份" width="100" />
        <el-table-column prop="infoCategory" label="资讯分类" width="120" />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column label="操作" width="130">
          <template #default="scope">
            <el-button type="primary" link @click="openEdit(scope.row)">编辑</el-button>
            <el-button type="danger" link @click="remove(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="query.pageSize" :current-page="query.pageNo" style="margin-top: 20px; justify-content: center" @current-change="(page) => { query.pageNo = page; loadData() }" />
    </div>

    <el-dialog v-model="visible" title="资讯内容" width="860px">
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="考试项目">
            <el-select v-model="form.categoryId" placeholder="请选择考试项目">
              <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="资讯分类">
            <el-select v-model="form.infoCategory" placeholder="请选择资讯分类">
              <el-option v-for="item in infoCategories" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="招考省份">
            <el-select v-model="form.province" filterable placeholder="请选择招考省份">
              <el-option v-for="item in provinces" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        </div>
        <el-form-item label="封面">
          <el-upload :show-file-list="false" :http-request="uploadCover"><el-button type="primary">上传封面</el-button></el-upload>
          <div class="muted">{{ form.coverFileId ? `已关联文件 #${form.coverFileId}` : '可选，不上传则不展示封面' }}</div>
          <img v-if="form.coverUrl" :src="resolveAssetUrl(form.coverUrl)" alt="" class="form-cover" />
        </el-form-item>
        <el-form-item label="摘要"><el-input v-model="form.summary" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="正文"><el-input v-model="form.content" type="textarea" :rows="8" /></el-form-item>
        <el-form-item label="标签"><el-input v-model="form.tags" placeholder="可选，用逗号分隔" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-page { display: grid; gap: 20px; }
.toolbar, .table-card { padding: 20px; }
.toolbar {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) 150px 150px 150px auto auto;
  gap: 12px;
}
.form-grid { display: grid; gap: 12px; grid-template-columns: repeat(2, minmax(0, 1fr)); }
.muted { margin-top: 10px; color: var(--muted); }
.table-cover { width: 88px; height: 52px; object-fit: cover; border-radius: 10px; }
.form-cover { display: block; width: 180px; height: 108px; object-fit: cover; border-radius: 12px; margin-top: 12px; }

@media (max-width: 980px) {
  .toolbar {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 640px) {
  .toolbar,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
