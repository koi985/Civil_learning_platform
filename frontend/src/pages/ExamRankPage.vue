<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import request from '../utils/request'

const route = useRoute()
const loading = ref(false)
const ranking = ref({ records: [] })
const scope = ref('GLOBAL')

async function loadData() {
  loading.value = true
  try {
    ranking.value = await request.get(`/api/exams/${route.params.id}/ranking`, { params: { scope: scope.value } })
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell">
    <section class="glass-card rank-card" v-loading="loading">
      <div class="rank-head">
        <div>
          <div class="section-title">模考排行榜</div>
          <p v-if="ranking.scope === 'PROVINCE' && ranking.province">当前按 {{ ranking.province }} 榜单展示</p>
        </div>
        <el-radio-group v-model="scope" @change="loadData">
          <el-radio-button value="GLOBAL">全站</el-radio-button>
          <el-radio-button value="PROVINCE">本省</el-radio-button>
        </el-radio-group>
      </div>
      <el-table :data="ranking.records" stripe>
        <el-table-column prop="rank" label="排名" width="80" />
        <el-table-column prop="nickname" label="用户" />
        <el-table-column prop="province" label="省份" width="120" />
        <el-table-column prop="score" label="得分" width="120" />
        <el-table-column prop="correctCount" label="答对题数" width="120" />
        <el-table-column prop="durationSeconds" label="用时(秒)" width="120" />
        <el-table-column prop="submittedAt" label="交卷时间" min-width="180" />
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.rank-card {
  padding: 24px;
}

.rank-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
}

.rank-head p {
  color: var(--muted);
}
</style>
