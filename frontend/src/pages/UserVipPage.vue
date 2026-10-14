<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '../utils/request'

const loading = ref(false)
const overview = ref({ profile: {} })

const vipState = computed(() => {
  const expireAt = overview.value.profile?.vipExpireAt
  if (!expireAt) {
    return { label: '未开通', type: 'info', reminder: '当前账号还未开通 VIP。' }
  }
  const diff = Math.ceil((new Date(expireAt).getTime() - Date.now()) / (24 * 60 * 60 * 1000))
  if (diff < 0) {
    return { label: '已过期', type: 'danger', reminder: 'VIP 已过期，相关权益需要续期后继续使用。' }
  }
  if (diff <= 7) {
    return { label: `即将到期`, type: 'warning', reminder: `距离到期还有 ${diff} 天，请及时续期避免中断。` }
  }
  return { label: '生效中', type: 'success', reminder: `VIP 仍有效，距离到期约 ${diff} 天。` }
})

async function loadData() {
  loading.value = true
  try {
    overview.value = await request.get('/api/user-center/overview')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <section class="glass-card vip-card" v-loading="loading">
    <div class="section-title">VIP 信息</div>
    <div class="vip-box">
      <div>
        <h2>{{ overview.profile.nickname || '学习用户' }}</h2>
        <p>当前备考方向：{{ overview.profile.examType }} · {{ overview.profile.preferredSubject }}</p>
      </div>
      <div class="vip-tag">VIP</div>
    </div>
    <div class="info-grid">
      <div><span>VIP 到期时间</span><strong>{{ overview.profile.vipExpireAt || '未开通' }}</strong></div>
      <div><span>当前状态</span><strong>{{ vipState.label }}</strong></div>
      <div><span>到期提醒</span><strong>{{ vipState.reminder }}</strong></div>
      <div><span>推荐权益</span><strong>题库练习 · 模考作答 · 统计报告 · 收藏沉淀</strong></div>
    </div>
  </section>
</template>

<style scoped>
.vip-card {
  padding: 24px;
}

.vip-box {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.vip-box p,
.info-grid span {
  color: var(--muted);
}

.vip-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 88px;
  height: 88px;
  border-radius: 28px;
  background: linear-gradient(135deg, #f97316, #fb923c);
  color: #fff;
  font-size: 24px;
  font-weight: 700;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
  margin-top: 20px;
}

.info-grid div {
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 16px;
  background: #fff;
}

.info-grid strong {
  display: block;
  margin-top: 8px;
}
</style>
