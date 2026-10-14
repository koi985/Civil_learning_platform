<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'
import { resolveAssetUrl } from '../utils/assets'

const route = useRoute()
const loading = ref(false)
const detail = ref({ course: {}, chapters: [], materials: [] })
const activeChapterId = ref(null)

const activeChapter = computed(() => detail.value.chapters.find((item) => item.id === activeChapterId.value) || detail.value.chapters[0] || null)

async function loadData() {
  loading.value = true
  try {
    detail.value = await request.get(`/api/user-center/courses/${route.params.id}/learn`)
    activeChapterId.value = detail.value.chapters[0]?.id || null
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section class="glass-card learn-hero">
      <div>
        <div class="section-title">{{ detail.course.title }}</div>
        <p class="muted">{{ detail.course.description }}</p>
        <div class="meta-row">
          <span>{{ detail.course.teacherName }}</span>
          <span>{{ detail.course.subjectName }}</span>
          <span>{{ detail.course.examType }}</span>
        </div>
      </div>
    </section>

    <div class="learn-grid section-gap">
      <section class="glass-card player-card">
        <div class="section-title">{{ activeChapter?.title || '请选择章节' }}</div>
        <video v-if="activeChapter?.videoUrl" controls class="player" :src="activeChapter.videoUrl"></video>
        <el-empty v-else description="当前章节暂无视频链接" />
        <p class="muted" style="margin-top: 14px">
          {{ activeChapter?.chapterType }} · {{ activeChapter?.durationMinutes || 0 }} 分钟
        </p>
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
            <strong>{{ item.title }}</strong>
            <span>{{ item.chapterType }} · {{ item.durationMinutes }} 分钟</span>
          </button>
        </div>

        <div class="section-title" style="margin-top: 20px">学习资料</div>
        <div class="material-list">
          <a v-for="item in detail.materials" :key="item.id" :href="resolveAssetUrl(item.fileUrl)" target="_blank" class="material-item">
            <strong>{{ item.title }}</strong>
            <span>{{ item.materialType }}</span>
          </a>
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

.learn-grid {
  display: grid;
  grid-template-columns: 1.4fr 0.9fr;
  gap: 20px;
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

.chapter-item.active {
  border-color: var(--primary);
  box-shadow: 0 0 0 1px rgba(37, 99, 235, 0.18);
}

@media (max-width: 960px) {
  .learn-grid {
    grid-template-columns: 1fr;
  }
}
</style>
