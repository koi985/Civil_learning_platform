<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const query = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: ''
})

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/notices', { params: query })
    list.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <section class="glass-card notice-top">
      <div class="section-title">公告中心</div>
      <div class="notice-filter">
        <el-input v-model="query.keyword" placeholder="搜索公告标题" clearable />
        <el-button type="primary" @click="query.pageNo = 1; loadData()">查询</el-button>
      </div>
    </section>

    <section class="glass-card notice-list-box section-gap" v-loading="loading">
      <div class="notice-list">
        <article v-for="item in list" :key="item.id" class="notice-card" @click="router.push(`/notice/detail/${item.id}`)">
          <div class="notice-head">
            <h3>{{ item.title }}</h3>
            <el-tag type="warning" effect="plain">阅读 {{ item.readCount }}</el-tag>
          </div>
          <p>{{ item.summary || '暂无摘要' }}</p>
          <div class="notice-meta">发布时间：{{ item.publishedAt }}</div>
        </article>
      </div>
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.pageNo"
        style="margin-top: 24px; justify-content: center"
        @current-change="(page) => { query.pageNo = page; loadData() }"
      />
    </section>
  </div>
</template>

<style scoped>
.notice-top,
.notice-list-box {
  padding: 24px;
}

.notice-filter {
  display: flex;
  gap: 12px;
}

.notice-list {
  display: grid;
  gap: 16px;
}

.notice-card {
  border: 1px solid var(--border);
  border-radius: 18px;
  background: #fff;
  padding: 20px;
  cursor: pointer;
}

.notice-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.notice-card p,
.notice-meta {
  color: var(--muted);
}

@media (max-width: 768px) {
  .notice-filter,
  .notice-head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
