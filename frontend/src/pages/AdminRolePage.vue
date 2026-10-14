<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'

const loading = ref(false)
const visible = ref(false)
const list = ref([])
const form = reactive({
  id: null,
  code: '',
  name: '',
  descriptionText: '',
  sortNo: 0,
  enabled: true
})

async function loadData() {
  loading.value = true
  try {
    list.value = await request.get('/api/admin/roles')
  } finally {
    loading.value = false
  }
}

function openEdit(row) {
  Object.assign(form, {
    id: row?.id ?? null,
    code: row?.code ?? '',
    name: row?.name ?? '',
    descriptionText: row?.descriptionText ?? '',
    sortNo: row?.sortNo ?? 0,
    enabled: row?.enabled !== false
  })
  visible.value = true
}

function resetForm() {
  Object.assign(form, {
    id: null,
    code: '',
    name: '',
    descriptionText: '',
    sortNo: 0,
    enabled: true
  })
}

async function save() {
  try {
    await request.post('/api/admin/roles', form)
    ElMessage.success('角色已保存')
    visible.value = false
    resetForm()
    loadData()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function remove(id) {
  try {
    await request.delete(`/api/admin/roles/${id}`)
    ElMessage.success('角色已删除')
    loadData()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(loadData)
</script>

<template>
  <div class="admin-page" v-loading="loading">
    <div class="glass-card panel">
      <div class="head">
        <div>
          <div class="section-title">角色管理</div>
          <p class="muted">维护平台角色定义，并控制角色启停与排序。</p>
        </div>
        <el-button type="primary" @click="openEdit()">新增角色</el-button>
      </div>

      <el-table :data="list">
        <el-table-column prop="code" label="角色编码" />
        <el-table-column prop="name" label="角色名称" />
        <el-table-column prop="descriptionText" label="描述" />
        <el-table-column prop="sortNo" label="排序" />
        <el-table-column label="状态">
          <template #default="{ row }">{{ row.enabled ? '启用' : '停用' }}</template>
        </el-table-column>
        <el-table-column prop="userCount" label="用户数" />
        <el-table-column prop="createdAt" label="创建时间" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="visible" title="角色信息" width="640px">
      <div class="form-grid">
        <el-input v-model="form.code" placeholder="角色编码，如 OPERATOR" />
        <el-input v-model="form.name" placeholder="角色名称" />
        <el-input v-model="form.descriptionText" placeholder="角色描述" />
        <el-input-number v-model="form.sortNo" :min="0" placeholder="排序" />
        <el-switch v-model="form.enabled" active-text="启用" inactive-text="停用" />
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

.panel {
  padding: 20px;
}

.head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
}

.muted {
  color: var(--muted);
}

.form-grid {
  display: grid;
  gap: 12px;
}
</style>
