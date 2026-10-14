<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'

const loading = ref(false)
const data = ref({ entries: [] })

async function loadData() {
  loading.value = true
  try {
    data.value = await request.get('/api/user-center/exports')
  } finally {
    loading.value = false
  }
}

async function handleDownload(path) {
  try {
    await request.download(path)
    ElMessage.success('导出完成')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(loadData)
</script>

<template>
  <div class="profile-page" v-loading="loading">
    <section class="glass-card panel">
      <div class="section-title">导出中心</div>
      <div class="summary-grid">
        <div><span>错题数量</span><strong>{{ data.wrongQuestionCount }}</strong></div>
        <div><span>成绩记录</span><strong>{{ data.scoreRecordCount }}</strong></div>
      </div>
    </section>

    <section class="glass-card panel">
      <div class="entry-grid">
        <article v-for="item in data.entries" :key="item.title" class="entry-card">
          <strong>{{ item.title }}</strong>
          <p>{{ item.description }}</p>
          <div class="muted">当前记录数：{{ item.count }}</div>
          <el-button type="primary" plain @click="handleDownload(item.path)">立即导出</el-button>
        </article>
      </div>
    </section>

    <section class="glass-card panel">
      <div class="section-title">导出说明</div>
      <div class="tip-list">
        <article v-for="item in data.tips || []" :key="item" class="tip-item">{{ item }}</article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.profile-page,
.entry-grid {
  display: grid;
  gap: 20px;
}

.panel {
  padding: 24px;
}

.summary-grid,
.entry-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
}

.tip-list {
  display: grid;
  gap: 12px;
}

.summary-grid div,
.entry-card,
.tip-item {
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 16px;
  background: #fff;
}

.summary-grid span,
.muted {
  color: var(--muted);
}

.summary-grid strong {
  display: block;
  margin-top: 8px;
  font-size: 26px;
}
</style>
