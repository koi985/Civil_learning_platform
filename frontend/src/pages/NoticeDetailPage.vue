<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref({
  notice: {},
  latestNotices: []
})

async function loadData() {
  loading.value = true
  try {
    detail.value = await request.get(`/api/notices/${route.params.id}`)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section class="two-column">
      <article class="glass-card article-box">
        <div class="article-meta">公告发布于 {{ detail.notice.publishedAt }} · 阅读 {{ detail.notice.readCount }}</div>
        <h1>{{ detail.notice.title }}</h1>
        <div class="article-summary">{{ detail.notice.summary }}</div>
        <div class="article-content">{{ detail.notice.content }}</div>
      </article>

      <aside class="glass-card side-box">
        <div class="section-title">最新公告</div>
        <div class="side-list">
          <div v-for="item in detail.latestNotices" :key="item.id" class="side-item" @click="router.push(`/notice/detail/${item.id}`)">
            <strong>{{ item.title }}</strong>
            <div>{{ item.publishedAt }}</div>
          </div>
        </div>
        <el-button style="margin-top: 16px" plain @click="router.push('/notice')">查看全部公告</el-button>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.article-box,
.side-box {
  padding: 24px;
}

.article-meta,
.article-summary,
.side-item div {
  color: var(--muted);
}

.article-content {
  line-height: 1.9;
  margin-top: 18px;
  white-space: pre-wrap;
}

.side-list {
  display: grid;
  gap: 12px;
}

.side-item {
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 14px;
  background: #fff;
  cursor: pointer;
}
</style>
