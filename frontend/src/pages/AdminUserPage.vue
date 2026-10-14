<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import request from '../utils/request'

const loading = ref(false)
const visible = ref(false)
const list = ref([])
const roleOptions = ref([])
const total = ref(0)
const query = reactive({ pageNo: 1, pageSize: 10, keyword: '' })
const form = reactive({ userId: null, nickname: '', roleCode: 'USER', roleCodes: ['USER'], province: '', examType: '', preferredSubject: '', enabled: true, locked: false, vipExpireAt: '' })

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/admin/users', { params: query })
    list.value = data.records
    total.value = data.total
    roleOptions.value = await request.get('/api/admin/roles')
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  Object.assign(form, {
    userId: row.id,
    nickname: row.nickname,
    roleCode: row.roles?.[0] || 'USER',
    roleCodes: row.roles?.length ? row.roles : ['USER'],
    province: row.province,
    examType: row.examType,
    preferredSubject: row.preferredSubject,
    enabled: row.enabled,
    locked: row.locked,
    vipExpireAt: row.vipExpireAt
  })
  visible.value = true
}

async function save() {
  try {
    form.roleCode = form.roleCodes?.[0] || form.roleCode || 'USER'
    await request.put('/api/admin/users', form)
    ElMessage.success('更新成功')
    visible.value = false
    loadData()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function removeUser(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除用户「${row.nickname || row.username}」？删除后该用户将无法登录。`,
      '删除用户',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger'
      }
    )
    await request.delete(`/api/admin/users/${row.id}`)
    ElMessage.success('删除成功')
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
      <el-input v-model="query.keyword" placeholder="搜索用户名或昵称" clearable />
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>

    <div class="glass-card table-card" v-loading="loading">
      <el-table :data="list">
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="province" label="省份" />
        <el-table-column prop="examType" label="考试类型" />
        <el-table-column prop="preferredSubject" label="偏好科目" />
        <el-table-column label="角色">
          <template #default="scope">{{ scope.row.roles?.join(', ') }}</template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="scope">{{ scope.row.enabled ? '启用' : '禁用' }} / {{ scope.row.locked ? '锁定' : '正常' }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="120">
          <template #default="scope">
            <el-button type="primary" link @click="openEdit(scope.row)">编辑</el-button>
            <el-button type="danger" link @click="removeUser(scope.row)">删除</el-button>
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

    <el-dialog v-model="visible" title="编辑用户" width="680px">
      <div class="form-grid">
        <el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleCodes" multiple collapse-tags collapse-tags-tooltip style="width: 100%">
            <el-option v-for="item in roleOptions" :key="item.id" :label="`${item.name} (${item.code})`" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="省份"><el-input v-model="form.province" /></el-form-item>
        <el-form-item label="考试类型"><el-input v-model="form.examType" /></el-form-item>
        <el-form-item label="偏好科目"><el-input v-model="form.preferredSubject" /></el-form-item>
        <el-form-item label="VIP 到期"><el-input v-model="form.vipExpireAt" placeholder="YYYY-MM-DD HH:mm:ss" /></el-form-item>
      </div>
      <div class="form-grid">
        <el-switch v-model="form.enabled" active-text="启用" inactive-text="禁用" />
        <el-switch v-model="form.locked" active-text="锁定" inactive-text="正常" />
      </div>
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
  grid-template-columns: 1fr 1fr;
}
</style>
