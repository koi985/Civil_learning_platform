<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({ pageNo: 1, pageSize: 10, source: '', keyword: '' })

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/admin/evaluations', { params: query })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function remove(row) {
  try {
    await request.delete(`/api/admin/evaluations/${row.source}/${row.id}`)
    ElMessage.success('评价已删除')
    loadData()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(loadData)
</script>

<template>
  <div class="admin-page">
    <div class="glass-card panel">
      <div class="toolbar">
        <el-select v-model="query.source" clearable placeholder="资源类型">
          <el-option label="课程" value="COURSE" />
          <el-option label="资讯" value="NEWS" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="搜索资源、用户或内容" />
        <el-button type="primary" @click="query.pageNo = 1; loadData()">查询</el-button>
      </div>
    </div>

    <div class="glass-card panel" v-loading="loading">
      <el-table :data="list">
        <el-table-column prop="resourceType" label="类型" />
        <el-table-column prop="resourceTitle" label="资源" />
        <el-table-column prop="nickname" label="用户" />
        <el-table-column prop="rating" label="评分" />
        <el-table-column prop="content" label="内容" />
        <el-table-column prop="createdAt" label="时间" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button link type="danger" @click="remove(row)">删除</el-button>
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

.toolbar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
</style>
