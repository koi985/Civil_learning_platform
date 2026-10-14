<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const detail = ref({
  news: {},
  comments: [],
  relatedNews: [],
  hotNews: [],
  previousNews: null,
  nextNews: null
})
const loading = ref(false)
const commentContent = ref('')

const articleMeta = computed(() => [
  detail.value.news.categoryName,
  detail.value.news.province,
  detail.value.news.infoCategory,
  formatDateTime(detail.value.news.publishedAt),
  `阅读 ${detail.value.news.readCount || 0}`
].filter(Boolean))

async function loadData() {
  loading.value = true
  try {
    detail.value = await request.get(`/api/news/${route.params.id}`)
  } finally {
    loading.value = false
  }
}

async function submitComment() {
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }
  if (!commentContent.value.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }
  await request.post(`/api/news/${route.params.id}/comments`, { content: commentContent.value.trim() })
  ElMessage.success('评论成功')
  commentContent.value = ''
  await loadData()
}

async function shareNews() {
  await request.post(`/api/news/${route.params.id}/share`)
  ElMessage.success('已记录分享')
  await loadData()
}

function formatDateTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

function openNews(id) {
  router.push(`/news/detail/${id}`)
}

onMounted(loadData)
watch(() => route.params.id, loadData)
</script>

<template>
  <div class="detail-page" v-loading="loading">
    <section class="detail-layout">
      <article class="article-panel">
        <div class="crumb">
          <button type="button" @click="router.push('/news')">考试资讯</button>
          <span>/</span>
          <strong>{{ detail.news.infoCategory || '资讯详情' }}</strong>
        </div>
        <h1>{{ detail.news.title }}</h1>
        <div class="article-meta">
          <span v-for="item in articleMeta" :key="item">{{ item }}</span>
        </div>
        <div class="article-content" v-html="detail.news.content"></div>
        <div class="article-actions">
          <el-button type="primary" plain @click="shareNews">分享资讯</el-button>
          <el-button plain @click="router.push('/news')">返回列表</el-button>
        </div>
        <div class="article-nav">
          <button v-if="detail.previousNews" type="button" @click="openNews(detail.previousNews.id)">
            <span>上一篇</span>
            <strong>{{ detail.previousNews.title }}</strong>
          </button>
          <button v-if="detail.nextNews" type="button" @click="openNews(detail.nextNews.id)">
            <span>下一篇</span>
            <strong>{{ detail.nextNews.title }}</strong>
          </button>
        </div>
      </article>

      <aside class="side-column">
        <section class="side-section">
          <div class="side-title">相关推荐</div>
          <button v-for="item in detail.relatedNews" :key="item.id" type="button" class="side-row" @click="openNews(item.id)">
            <strong>{{ item.title }}</strong>
            <span>{{ item.province || '全国' }} · {{ item.infoCategory || item.categoryName }}</span>
          </button>
        </section>
        <section class="side-section">
          <div class="side-title">热门资讯</div>
          <button v-for="(item, index) in detail.hotNews" :key="`hot-${item.id}`" type="button" class="hot-row" @click="openNews(item.id)">
            <span>{{ index + 1 }}</span>
            <strong>{{ item.title }}</strong>
          </button>
        </section>
      </aside>
    </section>

    <section class="comment-panel">
      <div class="side-title">评论区</div>
      <div class="comment-box">
        <el-input v-model="commentContent" type="textarea" :rows="3" placeholder="写下你的看法或学习收获" />
        <el-button type="primary" @click="submitComment">提交评论</el-button>
      </div>
      <div class="comment-list" v-if="detail.comments.length">
        <article v-for="item in detail.comments" :key="item.id">
          <strong>{{ item.nickname }}</strong>
          <p>{{ item.content }}</p>
        </article>
      </div>
      <el-empty v-else description="暂无评论" />
    </section>
  </div>
</template>

<style scoped>
.detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 28px 18px 52px;
  background: #f5f5f5;
  color: #222;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 822px) 338px;
  gap: 40px;
}

.article-panel,
.side-section,
.comment-panel {
  background: #fff;
  border: 1px solid #eceff4;
}

.article-panel {
  min-width: 0;
  padding: 30px 36px 34px;
}

.crumb {
  display: flex;
  gap: 8px;
  align-items: center;
  color: #98a2b3;
  font-size: 14px;
}

.crumb button {
  border: 0;
  padding: 0;
  color: #e4393c;
  background: transparent;
  cursor: pointer;
}

.crumb strong {
  color: #667085;
  font-weight: 500;
}

.article-panel h1 {
  margin: 22px 0 14px;
  color: #101828;
  font-size: 30px;
  line-height: 1.35;
  font-weight: 800;
  text-align: center;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 10px 18px;
  padding-bottom: 20px;
  border-bottom: 1px solid #eef1f5;
  color: #98a2b3;
  font-size: 14px;
}

.article-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 24px;
}

.article-content {
  margin-top: 26px;
  color: #344054;
  font-size: 16px;
  line-height: 2;
}

.article-content :deep(p) {
  margin: 0 0 14px;
}

.article-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
}

.article-content :deep(td),
.article-content :deep(th) {
  border: 1px solid #e4e7ec;
  padding: 8px;
}

.article-nav {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 30px;
  padding-top: 22px;
  border-top: 1px solid #eef1f5;
}

.article-nav button {
  min-width: 0;
  border: 1px solid #e4e7ec;
  padding: 14px;
  text-align: left;
  background: #fff;
  cursor: pointer;
}

.article-nav button:hover strong {
  color: #e4393c;
}

.article-nav span {
  display: block;
  color: #98a2b3;
  font-size: 13px;
  margin-bottom: 6px;
}

.article-nav strong {
  display: block;
  overflow: hidden;
  color: #344054;
  font-size: 15px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.side-column {
  display: grid;
  align-content: start;
  gap: 18px;
}

.side-section,
.comment-panel {
  padding: 20px;
}

.side-title {
  position: relative;
  margin-bottom: 12px;
  padding-left: 16px;
  color: #1f2937;
  font-size: 22px;
  font-weight: 800;
}

.side-title::before {
  content: "";
  position: absolute;
  left: 0;
  top: 4px;
  width: 4px;
  height: 24px;
  background: #e4393c;
}

.side-row,
.hot-row {
  width: 100%;
  border: 0;
  border-bottom: 1px solid #f2f4f7;
  padding: 13px 0;
  text-align: left;
  background: transparent;
  cursor: pointer;
}

.side-row strong,
.hot-row strong {
  display: block;
  min-width: 0;
  overflow: hidden;
  color: #344054;
  font-size: 14px;
  font-weight: 500;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.side-row:hover strong,
.hot-row:hover strong {
  color: #e4393c;
}

.side-row span {
  display: block;
  margin-top: 6px;
  color: #98a2b3;
  font-size: 13px;
}

.hot-row {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
}

.hot-row span {
  width: 22px;
  height: 22px;
  display: inline-grid;
  place-items: center;
  color: #667085;
  background: #f2f4f7;
  font-size: 13px;
}

.hot-row:nth-of-type(2) span,
.hot-row:nth-of-type(3) span,
.hot-row:nth-of-type(4) span {
  color: #fff;
  background: #e4393c;
}

.comment-panel {
  margin-top: 18px;
}

.comment-box {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: end;
}

.comment-list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.comment-list article {
  border: 1px solid #eef1f5;
  padding: 14px;
  background: #fff;
}

.comment-list strong {
  color: #344054;
}

.comment-list p {
  margin: 8px 0 0;
  color: #667085;
}

@media (max-width: 960px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .detail-page {
    padding: 16px 10px 34px;
  }

  .article-panel {
    padding: 22px 16px;
  }

  .article-panel h1 {
    font-size: 24px;
  }

  .article-nav,
  .comment-box {
    grid-template-columns: 1fr;
  }
}
</style>
