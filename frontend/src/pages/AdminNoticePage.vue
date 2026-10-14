<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'

const loading = ref(false)
const visible = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNo: 1, pageSize: 10, keyword: '' })
const form = reactive({ id: null, title: '', summary: '', content: '', status: 'PUBLISHED', publishedAt: '' })

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/admin/notices', { params: query })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  Object.assign(form, {
    id: row?.id ?? null,
    title: row?.title ?? '',
    summary: row?.summary ?? '',
    content: row?.content ?? '',
    status: row?.status ?? 'PUBLISHED',
    publishedAt: row?.publishedAt ? String(row.publishedAt).replace('T', ' ') : ''
  })
  visible.value = true
}

async function save() {
  try {
    await request.post('/api/admin/notices', form)
    ElMessage.success('保存成功')
    visible.value = false
    loadData()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function remove(id) {
  try {
    await request.delete(`/api/admin/notices/${id}`)
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
      <el-input v-model="query.keyword" placeholder="搜索公告标题" clearable />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="warning" @click="openEdit()">新增公告</el-button>
    </div>

    <div class="glass-card table-card" v-loading="loading">
      <el-table :data="list">
        <el-table-column prop="title" label="标题" />
        <el-table-column prop="summary" label="简介" />
        <el-table-column prop="status" label="状态" />
        <el-table-column prop="readCount" label="阅读量" />
        <el-table-column prop="publishedAt" label="发布时间" />
        <el-table-column label="操作">
          <template #default="scope">
            <el-button type="primary" link @click="openEdit(scope.row)">编辑</el-button>
            <el-button type="danger" link @click="remove(scope.row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination background layout="prev, pager, next" :total="total" :page-size="query.pageSize" :current-page="query.pageNo" style="margin-top: 20px; justify-content: center" @current-change="(page) => { query.pageNo = page; loadData() }" />
    </div>

    <el-dialog v-model="visible" title="公告信息" width="720px">
      <el-form label-position="top">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.summary" /></el-form-item>
        <el-form-item label="发布时间">
          <el-date-picker v-model="form.publishedAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="草稿" value="DRAFT" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="8" /></el-form-item>
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
.toolbar { display: flex; gap: 12px; }
</style>
