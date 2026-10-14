<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import request from '../utils/request'
import { classroomCoverMeta, resolveClassroomCoverUrl } from '../utils/assets'

const router = useRouter()
const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({
  pageNo: 1,
  pageSize: 8,
  keyword: '',
  subjectName: ''
})

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/classrooms', { params: query })
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
    <section class="glass-card filter-box">
      <div class="section-title">班级中心</div>
      <div class="filter-grid">
        <el-input v-model="query.keyword" placeholder="搜索班级名称" />
        <el-input v-model="query.subjectName" placeholder="科目" />
        <el-button type="primary" @click="loadData">查询</el-button>
      </div>
    </section>

    <section class="glass-card list-box section-gap" v-loading="loading">
      <div class="classroom-grid">
        <div v-for="item in list" :key="item.id" class="classroom-card" @click="router.push(`/classroom/detail/${item.id}`)">
          <div class="classroom-cover">
            <img :src="resolveClassroomCoverUrl(item.coverUrl, item.subjectName, item.name)" alt="" />
            <div class="cover-shade"></div>
            <div class="cover-content" :style="{ '--cover-accent': classroomCoverMeta(item.subjectName, item.name).accent }">
              <span class="cover-subject">{{ classroomCoverMeta(item.subjectName, item.name).subject }}</span>
              <strong>{{ classroomCoverMeta(item.subjectName, item.name).title }}</strong>
              <div class="cover-tags">
                <span v-for="chip in classroomCoverMeta(item.subjectName, item.name).chips" :key="chip">{{ chip }}</span>
              </div>
            </div>
          </div>
          <div class="classroom-body">
            <h3>{{ item.name }}</h3>
            <p>{{ item.teacherName }} · {{ item.subjectName }}</p>
            <div class="muted">{{ item.memberCount }} / {{ item.capacity }} 人</div>
          </div>
        </div>
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
.filter-box,
.list-box {
  padding: 24px;
}

.filter-grid,
.classroom-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.classroom-grid {
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}

.classroom-card {
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 12px;
  background: #fff;
  cursor: pointer;
  overflow: hidden;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.classroom-card:hover {
  border-color: rgba(37, 99, 235, 0.3);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.11);
  transform: translateY(-2px);
}

.classroom-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 5 / 2.25;
  overflow: hidden;
  border-radius: 14px;
  background: #dbeafe;
}

.classroom-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transform: scale(1.01);
}

.cover-shade {
  position: absolute;
  inset: 0;
  background:
    linear-gradient(90deg, rgba(8, 22, 48, 0.82) 0%, rgba(8, 22, 48, 0.55) 38%, rgba(8, 22, 48, 0.08) 78%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.02), rgba(15, 23, 42, 0.22));
}

.cover-content {
  position: absolute;
  inset: 18px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  color: #fff;
  min-width: 0;
}

.cover-subject {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-left: 3px solid var(--cover-accent);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 13px;
  font-weight: 700;
}

.cover-content strong {
  margin-top: 12px;
  font-size: 28px;
  line-height: 1.15;
  font-weight: 800;
  letter-spacing: 0;
}

.cover-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 16px;
  max-width: 74%;
}

.cover-tags span {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.16);
  color: rgba(255, 255, 255, 0.94);
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.classroom-body {
  padding: 14px 10px 8px;
}

.classroom-body h3 {
  margin: 0;
  color: #0f172a;
  font-size: 19px;
  line-height: 1.35;
  font-weight: 800;
}

.classroom-body p {
  margin: 16px 0 12px;
  color: #0f172a;
  font-size: 15px;
}

.muted {
  color: var(--muted);
  font-size: 15px;
}

@media (max-width: 720px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .classroom-grid {
    grid-template-columns: 1fr;
  }

  .cover-content {
    inset: 14px;
  }

  .cover-content strong {
    font-size: 24px;
  }

  .cover-tags {
    max-width: 100%;
    margin-top: 12px;
  }
}
</style>
