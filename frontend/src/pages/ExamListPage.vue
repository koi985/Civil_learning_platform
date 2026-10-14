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
  pageSize: 8,
  keyword: '',
  province: '',
  examType: '',
  seasonName: '',
  subjectName: '',
  status: ''
})

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/exams', { params: query })
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
      <div class="section-title">模考中心</div>
      <div class="filter-grid">
        <el-input v-model="query.keyword" placeholder="搜索模考名称" clearable />
        <el-input v-model="query.province" placeholder="省份" clearable />
        <el-input v-model="query.examType" placeholder="考试类型" clearable />
        <el-input v-model="query.seasonName" placeholder="季次" clearable />
        <el-input v-model="query.subjectName" placeholder="科目" clearable />
        <el-select v-model="query.status" placeholder="状态" clearable>
          <el-option label="未开始" value="NOT_STARTED" />
          <el-option label="可练习" value="ONGOING" />
          <el-option label="已结束" value="ENDED" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
      </div>
    </section>

    <section class="glass-card list-box" v-loading="loading">
      <div class="exam-grid">
        <article v-for="item in list" :key="item.id" class="exam-card" @click="router.push(`/exam/detail/${item.id}`)">
          <div class="card-head">
            <div class="badge">{{ item.seasonName }}</div>
            <el-tag :type="item.availabilityStatus === 'ONGOING' ? 'success' : item.availabilityStatus === 'NOT_STARTED' ? 'warning' : 'info'">
              {{ item.availabilityStatusLabel }}
            </el-tag>
          </div>
          <h3>{{ item.title }}</h3>
          <p>{{ item.province }} · {{ item.examType }} · {{ item.subjectName }}</p>
          <div class="exam-foot">
            <span>{{ item.durationMinutes }} 分钟</span>
            <span>{{ item.totalScore }} 分</span>
          </div>
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
.filter-box,
.list-box {
  padding: 24px;
}

.list-box {
  margin-top: 24px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 14px;
}

.exam-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.exam-card {
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 18px;
  background: #fff;
  cursor: pointer;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.exam-card p {
  color: var(--muted);
}

.exam-foot {
  display: flex;
  justify-content: space-between;
  color: var(--muted);
}

.badge {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(249, 115, 22, 0.12);
  color: var(--accent);
  font-size: 12px;
  font-weight: 700;
}
</style>
