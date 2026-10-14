<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { firstArray, formatDateTime, percentText } from '../../mobile/helpers'

const router = useRouter()
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const list = ref([])
const shownCount = ref(8)
const visibleList = computed(() => firstArray(list.value).slice(0, shownCount.value))

async function loadData() {
  loading.value = true
  try {
    list.value = await request.get('/api/user-center/courses')
    finished.value = visibleList.value.length >= list.value.length
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function loadMore() {
  shownCount.value += 8
  finished.value = visibleList.value.length >= list.value.length
}

onMounted(() => {
  loadData()
  window.addEventListener('mobile-refresh', loadData)
})
onBeforeUnmount(() => window.removeEventListener('mobile-refresh', loadData))
</script>

<template>
  <div class="mobile-page">
    <van-list v-model:loading="loading" :finished="finished" finished-text="没有更多课程" @load="loadMore">
      <div class="mobile-list">
        <article v-for="item in visibleList" :key="item.id" class="mobile-card mobile-cover-row">
          <img class="mobile-cover" :src="item.coverUrl" alt="" />
          <div>
            <h3>{{ item.title || '课程学习' }}</h3>
            <p>{{ item.teacherName || '平台教师' }} · {{ item.subjectName || '综合' }}</p>
            <van-progress :percentage="percentText(item.progressPercent)" />
            <div class="mobile-card-meta">
              <span class="mobile-pill">{{ item.freeCourse ? '免费课程' : '已购课程' }}</span>
              <span class="mobile-pill is-ok">完成 {{ item.completedChapterCount || 0 }} 章</span>
            </div>
            <p>最近学习：{{ formatDateTime(item.lastStudyAt, '暂无记录') }}</p>
            <div class="mobile-actions is-single">
              <van-button type="primary" @click="router.push(`/course/play/${item.courseId || item.id}`)">进入学习</van-button>
            </div>
          </div>
        </article>
      </div>
    </van-list>
    <div v-if="!loading && !list.length" class="mobile-empty">暂无课程</div>
  </div>
</template>
