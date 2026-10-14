<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import request from '../utils/request'
import { resolveFileViewUrl } from '../utils/assets'

const loading = ref(false)
const visible = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNo: 1, pageSize: 10, keyword: '', auditStatus: 'APPROVED' })
const form = reactive({
  teacherProfileId: null,
  nickname: '',
  realName: '',
  email: '',
  phone: '',
  intro: '',
  auditStatus: 'APPROVED',
  auditRemark: '',
  enabled: true,
  locked: false
})

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/admin/teachers', { params: query })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function editRow(row) {
  Object.assign(form, row)
  visible.value = true
}

async function save() {
  try {
    await request.put('/api/admin/teachers', form)
    ElMessage.success('教师信息已更新')
    visible.value = false
    loadData()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function removeTeacher(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除教师「${row.realName || row.nickname || row.username}」？删除后该账号将不再拥有教师身份。`,
      '删除教师',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger'
      }
    )
    await request.delete(`/api/admin/teachers/${row.teacherProfileId}`)
    ElMessage.success('教师已删除')
    if (list.value.length === 1 && query.pageNo > 1) {
      query.pageNo -= 1
    }
    loadData()
  } catch (error) {
    if (error === 'cancel' || error === 'close') {
      return
    }
    ElMessage.error(error.message || '删除失败')
  }
}

onMounted(loadData)
</script>

<template>
  <div class="admin-page">
    <div class="toolbar glass-card">
      <el-input v-model="query.keyword" placeholder="搜索教师" />
      <el-select v-model="query.auditStatus" clearable placeholder="审核状态">
        <el-option label="待审核" value="PENDING" />
        <el-option label="已通过" value="APPROVED" />
        <el-option label="已驳回" value="REJECTED" />
      </el-select>
      <el-button type="primary" @click="query.pageNo = 1; loadData()">查询</el-button>
    </div>

    <div class="glass-card table-card" v-loading="loading">
      <el-table :data="list">
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="phone" label="手机号" />
        <el-table-column prop="auditStatus" label="审核状态" />
        <el-table-column prop="classroomCount" label="班级数" />
        <el-table-column label="证书">
          <template #default="{ row }">
            <a v-if="row.certificateFileId" :href="resolveFileViewUrl(row.certificateFileId)" target="_blank">查看证书</a>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="editRow(row)">维护</el-button>
            <el-button link type="danger" @click="removeTeacher(row)">删除</el-button>
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

    <el-dialog v-model="visible" title="教师维护" width="640px">
      <div class="form-grid">
        <el-input v-model="form.nickname" placeholder="昵称" />
        <el-input v-model="form.realName" placeholder="真实姓名" />
        <el-input v-model="form.email" placeholder="邮箱" />
        <el-input v-model="form.phone" placeholder="手机号" />
        <el-select v-model="form.auditStatus" placeholder="审核状态">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-switch v-model="form.enabled" active-text="启用" inactive-text="禁用" />
        <el-switch v-model="form.locked" active-text="已锁定" inactive-text="未锁定" />
      </div>
      <el-input v-model="form.auditRemark" type="textarea" :rows="2" placeholder="审核备注" style="margin-top: 12px" />
      <el-input v-model="form.intro" type="textarea" :rows="4" placeholder="教师简介" style="margin-top: 12px" />
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

.toolbar,
.form-grid {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
