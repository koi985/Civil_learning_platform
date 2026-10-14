<script setup>
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../utils/request'
import { firstPageRecords } from '../../mobile/helpers'

const router = useRouter()
const loading = ref(false)
const payload = ref({ records: [] })
const query = reactive({ pageNo: 1, pageSize: 30 })

async function loadData() {
  loading.value = true
  try {
    payload.value = await request.get('/api/teacher/classrooms', { params: query })
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
  window.addEventListener('mobile-refresh', loadData)
})
onBeforeUnmount(() => window.removeEventListener('mobile-refresh', loadData))
</script>

<template>
  <div class="mobile-page">
    <div class="mobile-list">
      <article v-for="item in firstPageRecords(payload)" :key="item.id" class="mobile-card">
        <h3>{{ item.name || '班级' }}</h3>
        <p>{{ item.teacherName || '我' }} · {{ item.subjectName || '综合' }}</p>
        <div class="mobile-card-meta">
          <span class="mobile-pill">成员 {{ item.memberCount || item.studentCount || 0 }}</span>
          <span class="mobile-pill is-ok">{{ item.status || 'OPEN' }}</span>
          <span class="mobile-pill is-warn">{{ item.isPublic === false ? '私密' : '公开' }}</span>
        </div>
        <div class="mobile-actions">
          <van-button type="primary" plain @click="router.push('/teacher/class')">完整管理</van-button>
          <van-button plain @click="router.push('/teacher/report')">成绩</van-button>
        </div>
      </article>
      <van-loading v-if="loading" />
      <div v-if="!loading && !firstPageRecords(payload).length" class="mobile-empty">暂无班级</div>
    </div>
  </div>
</template>
