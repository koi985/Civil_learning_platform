<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'

const router = useRouter()
const loading = ref(false)
const allCourses = ref([])
const pageNo = ref(1)
const pageSize = 8

const pagedCourses = computed(() => allCourses.value.slice((pageNo.value - 1) * pageSize, pageNo.value * pageSize))

async function loadData() {
  loading.value = true
  try {
    allCourses.value = await request.get('/api/user-center/courses')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <section class="glass-card list-card" v-loading="loading">
      <div class="section-title">我的课程</div>
      <div class="course-grid" v-if="pagedCourses.length">
        <article v-for="item in pagedCourses" :key="item.id" class="course-card">
          <img :src="item.coverUrl" alt="" />
          <h3>{{ item.title }}</h3>
          <p>{{ item.teacherName }} · {{ item.subjectName }} · {{ item.examType }}</p>
          <div class="expire">有效期至：{{ item.expireAt || '长期有效' }}</div>
          <el-button type="primary" style="margin-top: 12px" @click="router.push(`/course/play/${item.id}`)">进入学习</el-button>
        </article>
      </div>
      <el-empty v-else description="暂未购课，先去课程中心看看吧" />

      <el-pagination
        v-if="allCourses.length > pageSize"
        background
        layout="prev, pager, next"
        :total="allCourses.length"
        :page-size="pageSize"
        :current-page="pageNo"
        style="margin-top: 24px; justify-content: center"
        @current-change="(page) => { pageNo = page }"
      />
    </section>
  </div>
</template>

<style scoped>
.list-card {
  padding: 24px;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.course-card {
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 16px;
  background: #fff;
}

.course-card img {
  width: 100%;
  height: 160px;
  object-fit: cover;
  border-radius: 14px;
}

.course-card p,
.expire {
  color: var(--muted);
}
</style>
