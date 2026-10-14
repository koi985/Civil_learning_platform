<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import request from '../../utils/request'
import { firstArray } from '../../mobile/helpers'

const router = useRouter()
const loading = ref(false)
const data = ref({ summary: {}, joinedClassrooms: [], invitationRecords: [], quickEntries: [], recentReminders: [] })

async function loadData() {
  loading.value = true
  try {
    data.value = await request.get('/api/user-center/classrooms')
  } finally {
    loading.value = false
  }
}

async function confirmInvitation(row, action) {
  try {
    const result = await request.post(`/api/user-center/classroom-invitations/${row.id}/confirm`, null, { params: { action } })
    showSuccessToast(action === 'ACCEPT' ? '已加入班级' : '已拒绝邀请')
    await loadData()
    if (action === 'ACCEPT' && result?.detailPath) router.push(result.detailPath)
  } catch (error) {
    showFailToast(error.message || '处理失败')
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
    <section class="mobile-stat-grid">
      <div class="mobile-stat"><span>已加入</span><strong>{{ data.summary?.joinedCount || firstArray(data.joinedClassrooms).length }}</strong></div>
      <div class="mobile-stat"><span>待邀请</span><strong>{{ data.summary?.pendingInvitationCount || firstArray(data.invitationRecords).length }}</strong></div>
      <div class="mobile-stat"><span>待任务</span><strong>{{ data.summary?.pendingTaskCount || 0 }}</strong></div>
      <div class="mobile-stat"><span>平均成绩</span><strong>{{ data.summary?.averageScore || 0 }}</strong></div>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-head"><h2>待确认邀请</h2></div>
      <div class="mobile-list">
        <article v-for="item in firstArray(data.invitationRecords)" :key="item.id" class="mobile-card">
          <h3>{{ item.name || '班级邀请' }}</h3>
          <p>{{ item.teacherName || '教师' }} · {{ item.subjectName || '综合' }}</p>
          <p>{{ item.remark || '教师邀请你加入班级' }}</p>
          <div class="mobile-actions">
            <van-button type="primary" @click="confirmInvitation(item, 'ACCEPT')">接受</van-button>
            <van-button plain type="danger" @click="confirmInvitation(item, 'REJECT')">拒绝</van-button>
          </div>
        </article>
        <div v-if="!firstArray(data.invitationRecords).length" class="mobile-empty">暂无待确认邀请</div>
      </div>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-head"><h2>我的班级</h2></div>
      <div class="mobile-list">
        <article v-for="item in firstArray(data.joinedClassrooms)" :key="item.classroomId || item.id" class="mobile-card">
          <h3>{{ item.name || '班级' }}</h3>
          <p>{{ item.teacherName || '教师' }} · {{ item.subjectName || '综合' }}</p>
          <div class="mobile-card-meta">
            <span class="mobile-pill">作业 {{ item.pendingHomeworkCount || 0 }}</span>
            <span class="mobile-pill is-warn">考试 {{ item.pendingExamCount || 0 }}</span>
            <span class="mobile-pill is-ok">均分 {{ item.averageScore || 0 }}</span>
          </div>
          <div class="mobile-actions is-single">
            <van-button type="primary" plain @click="router.push(item.detailPath || `/classroom/detail/${item.classroomId}`)">进入班级</van-button>
          </div>
        </article>
        <div v-if="!firstArray(data.joinedClassrooms).length" class="mobile-empty">暂未加入班级</div>
      </div>
    </section>

    <section class="mobile-section">
      <div class="mobile-section-head"><h2>最近提醒</h2></div>
      <van-cell-group inset>
        <van-cell
          v-for="item in firstArray(data.recentReminders).slice(0, 6)"
          :key="`${item.classroomName}-${item.title}-${item.remindAt}`"
          :title="item.title || '班级提醒'"
          :label="`${item.classroomName || '班级'} · ${item.statusLabel || ''}`"
          is-link
          @click="router.push(item.detailPath || '/m/student/classrooms')"
        />
      </van-cell-group>
    </section>
  </div>
</template>
