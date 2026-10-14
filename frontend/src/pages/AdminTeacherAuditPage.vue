<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'
import { resolveFileViewUrl } from '../utils/assets'

const loading = ref(false)
const visible = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNo: 1, pageSize: 10, auditStatus: 'PENDING' })
const form = reactive({ teacherProfileId: null, auditStatus: 'APPROVED', auditRemark: '' })

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/admin/teacher-audits', { params: query })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function openAudit(row, status) {
  form.teacherProfileId = row.id
  form.auditStatus = status
  form.auditRemark = row.auditRemark || ''
  visible.value = true
}

async function save() {
  try {
    await request.post('/api/admin/teacher-audits', form)
    ElMessage.success('审核完成')
    visible.value = false
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
      <el-select v-model="query.auditStatus" placeholder="审核状态" clearable>
        <el-option label="待审核" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已驳回" value="REJECTED" />
      </el-select>
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>

    <div class="glass-card table-card" v-loading="loading">
      <el-table :data="list">
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="auditStatus" label="状态" />
        <el-table-column prop="intro" label="教师简介" />
        <el-table-column label="证书">
          <template #default="scope">
            <a :href="resolveFileViewUrl(scope.row.certificateFileId)" target="_blank">查看 PDF</a>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button type="success" link @click="openAudit(scope.row, 'APPROVED')">通过</el-button>
            <el-button type="danger" link @click="openAudit(scope.row, 'REJECTED')">驳回</el-button>
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

    <el-dialog v-model="visible" title="审核教师申请" width="520px">
      <el-form label-position="top">
        <el-form-item label="审核结果">
          <el-select v-model="form.auditStatus">
            <el-option label="通过" value="APPROVED" />
            <el-option label="驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.auditRemark" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">确认</el-button>
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
</style>
