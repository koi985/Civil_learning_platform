<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'
import { resolveAssetUrl } from '../utils/assets'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const actionLoading = ref(false)
const detail = ref({
  course: {},
  chapters: [],
  materials: [],
  studyProgress: {},
  recentLearningRecords: [],
  accessMode: 'FULL',
  canRecordProgress: true,
  purchaseRequired: false,
  previewMessage: ''
})
const activeChapterId = ref(null)
let lastRefreshAt = 0

const activeChapter = computed(() => detail.value.chapters.find((item) => item.id === activeChapterId.value) || detail.value.chapters[0] || null)
const activeChapterVideoUrl = computed(() => resolveAssetUrl(activeChapter.value?.videoUrl))
const activeChapterVideoKey = computed(() => `${activeChapter.value?.id || 'none'}:${activeChapter.value?.videoUrl || ''}`)
const canRecordProgress = computed(() => detail.value.canRecordProgress !== false && detail.value.accessMode !== 'TRIAL')

function formatSeconds(value) {
  const seconds = Number(value || 0)
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return hours > 0 ? `${hours}小时${minutes}分钟` : `${minutes}分钟`
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '暂无'
}

function applyPayload(payload) {
  detail.value = payload
  const currentChapterId = payload.studyProgress?.currentChapterId
  activeChapterId.value = detail.value.chapters.some((item) => item.id === currentChapterId)
    ? currentChapterId
    : (detail.value.chapters[0]?.id || null)
}

async function loadData() {
  lastRefreshAt = Date.now()
  loading.value = true
  try {
    applyPayload(await request.get(`/api/user-center/courses/${route.params.id}/learn`))
  } catch (error) {
    ElMessage.error(error.message)
    if (error.message?.includes('请先购买课程')) {
      router.push(`/course/detail/${route.params.id}`)
    }
  } finally {
    loading.value = false
  }
}

async function saveProgress(completed = false) {
  if (!activeChapter.value || !canRecordProgress.value) return
  actionLoading.value = true
  try {
    const payload = await request.post(`/api/user-center/courses/${route.params.id}/learn-progress`, {
      chapterId: activeChapter.value.id,
      watchSeconds: completed ? 0 : 300,
      completed
    })
    detail.value = {
      ...detail.value,
      ...payload
    }
    ElMessage.success(completed ? '本章节已标记完成' : '已记录5分钟学习进度')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    actionLoading.value = false
  }
}

function refreshOnWindowReturn() {
  if (document.visibilityState === 'hidden') {
    return
  }
  const now = Date.now()
  if (now - lastRefreshAt < 1000) {
    return
  }
  loadData()
}

onMounted(() => {
  loadData()
  window.addEventListener('focus', refreshOnWindowReturn)
  document.addEventListener('visibilitychange', refreshOnWindowReturn)
})

onBeforeUnmount(() => {
  window.removeEventListener('focus', refreshOnWindowReturn)
  document.removeEventListener('visibilitychange', refreshOnWindowReturn)
})
watch(() => route.params.id, loadData)
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section class="glass-card learn-hero">
      <div class="hero-main">
        <div class="section-title">{{ detail.course.title }}</div>
        <p class="muted">{{ detail.course.description }}</p>
        <div class="meta-row">
          <span>{{ detail.course.teacherName }}</span>
          <span>{{ detail.course.subjectName }}</span>
          <span>{{ detail.course.examType }}</span>
        </div>
      </div>
      <div class="hero-progress">
        <el-alert
          v-if="detail.accessMode === 'TRIAL'"
          :title="detail.previewMessage || '当前仅开放试听章节'"
          type="warning"
          :closable="false"
        />
        <div class="progress-label">课程进度</div>
        <div class="progress-value">{{ detail.studyProgress?.progressPercent || 0 }}%</div>
        <el-progress :percentage="Number(detail.studyProgress?.progressPercent || 0)" :stroke-width="10" />
        <div class="hero-stat">
          <span>已完成 {{ detail.studyProgress?.completedChapterCount || 0 }}/{{ detail.studyProgress?.totalChapterCount || 0 }} 章</span>
          <span>累计学习 {{ formatSeconds(detail.studyProgress?.totalStudySeconds) }}</span>
        </div>
        <div class="hero-stat">
          <span>最近学习 {{ formatDateTime(detail.studyProgress?.lastStudyAt) }}</span>
        </div>
      </div>
    </section>

    <div class="learn-grid section-gap">
      <section class="glass-card player-card">
        <div class="player-head">
          <div>
            <div class="section-title">{{ activeChapter?.title || '请选择章节' }}</div>
            <p class="muted">{{ activeChapter?.chapterType }} · {{ activeChapter?.durationMinutes || 0 }} 分钟</p>
          </div>
          <div class="player-actions">
            <el-button :loading="actionLoading" :disabled="!canRecordProgress" @click="saveProgress(false)">记录5分钟</el-button>
            <el-button type="primary" :loading="actionLoading" :disabled="!canRecordProgress" @click="saveProgress(true)">标记完成</el-button>
            <el-button v-if="detail.accessMode === 'TRIAL'" type="warning" plain @click="router.push(`/course/detail/${route.params.id}`)">去购买完整课程</el-button>
          </div>
        </div>
        <video v-if="activeChapterVideoUrl" :key="activeChapterVideoKey" controls class="player" :src="activeChapterVideoUrl"></video>
        <el-empty v-else description="当前章节暂无视频资源" />
        <div v-if="activeChapter" class="chapter-progress-panel">
          <el-tag :type="activeChapter.completed ? 'success' : 'info'">{{ activeChapter.completed ? '已完成' : '学习中' }}</el-tag>
          <span class="muted">章节进度 {{ activeChapter.progressPercent || 0 }}%</span>
          <span class="muted">学习时长 {{ formatSeconds(activeChapter.studySeconds) }}</span>
          <span class="muted">最近学习 {{ formatDateTime(activeChapter.lastStudyAt) }}</span>
        </div>
      </section>

      <section class="glass-card side-card">
        <div class="section-title">课程章节</div>
        <div class="chapter-list">
          <button
            v-for="item in detail.chapters"
            :key="item.id"
            class="chapter-item"
            :class="{ active: item.id === activeChapter?.id }"
            @click="activeChapterId = item.id"
          >
            <div class="chapter-title-row">
              <strong>{{ item.title }}</strong>
              <div class="chapter-tag-row">
                <el-tag v-if="item.trialAvailable" size="small" type="warning">试听</el-tag>
                <el-tag size="small" :type="item.completed ? 'success' : 'warning'">{{ item.completed ? '已完成' : '未完成' }}</el-tag>
              </div>
            </div>
            <span>{{ item.chapterType }} · {{ item.durationMinutes }} 分钟</span>
            <span>进度 {{ item.progressPercent || 0 }}% · 已学 {{ formatSeconds(item.studySeconds) }}</span>
          </button>
        </div>

        <div class="section-title" style="margin-top: 20px">学习资料</div>
        <div class="material-list">
          <a v-for="item in detail.materials" :key="item.id" :href="resolveAssetUrl(item.fileUrl)" target="_blank" class="material-item">
            <strong>{{ item.title }}</strong>
            <span>{{ item.materialType }}</span>
          </a>
          <el-empty v-if="!detail.materials?.length" :description="detail.accessMode === 'TRIAL' ? '试听模式暂不开放资料下载' : '当前暂无学习资料'" />
        </div>

        <div class="section-title" style="margin-top: 20px">最近学习记录</div>
        <div class="material-list">
          <div v-for="item in detail.recentLearningRecords" :key="item.chapterId" class="material-item">
            <strong>{{ item.chapterTitle }}</strong>
            <span>进度 {{ item.progressPercent || 0 }}% · {{ formatSeconds(item.studySeconds) }}</span>
            <span>{{ formatDateTime(item.lastStudyAt) }}</span>
          </div>
          <el-empty v-if="!detail.recentLearningRecords?.length" description="还没有学习记录" />
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.learn-hero,
.player-card,
.side-card {
  padding: 24px;
}

.learn-hero,
.learn-grid {
  display: grid;
  grid-template-columns: 1.4fr 0.9fr;
  gap: 20px;
}

.hero-progress,
.chapter-progress-panel {
  display: grid;
  gap: 12px;
}

.progress-label,
.progress-value {
  font-weight: 700;
}

.progress-value {
  font-size: 28px;
  color: var(--primary);
}

.hero-stat {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  color: var(--muted);
}

.player-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.player-actions {
  display: flex;
  gap: 10px;
}

.player {
  width: 100%;
  min-height: 360px;
  border-radius: 16px;
  background: #0f172a;
}

.muted,
.meta-row span,
.chapter-item span,
.material-item span {
  color: var(--muted);
}

.meta-row {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.chapter-list,
.material-list {
  display: grid;
  gap: 12px;
}

.chapter-item,
.material-item {
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 14px;
  background: #fff;
  display: grid;
  gap: 6px;
  text-align: left;
}

.chapter-title-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.chapter-tag-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.chapter-item.active {
  border-color: var(--primary);
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.18);
}

@media (max-width: 960px) {
  .learn-hero,
  .learn-grid {
    grid-template-columns: 1fr;
  }

  .player-head {
    flex-direction: column;
  }
}
</style>
