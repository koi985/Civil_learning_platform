<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'
import UserCenterBackButton from '../components/UserCenterBackButton.vue'
import { resolveClassroomCoverUrl } from '../utils/assets'

const router = useRouter()
const loading = ref(false)
const data = ref({
  joinedClassrooms: [],
  pendingClassrooms: [],
  quickEntries: []
})

async function loadData() {
  loading.value = true
  try {
    data.value = await request.get('/api/user-center/classrooms')
  } finally {
    loading.value = false
  }
}

async function handleInvitation(row, action) {
  const actionText = action === 'ACCEPT' ? '接受' : '拒绝'
  try {
    const result = await request.post(`/api/user-center/classroom-invitations/${row.id}/confirm`, null, {
      params: { action }
    })
    ElMessage.success(action === 'ACCEPT' ? '已加入班级' : '已拒绝邀请')
    await loadData()
    if (action === 'ACCEPT' && result.detailPath) {
      router.push(result.detailPath)
    }
  } catch (error) {
    ElMessage.error(error.message || `${actionText}邀请失败`)
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell classroom-page" v-loading="loading">
    <section class="glass-card panel">
      <div class="page-head">
        <div class="section-title">我的班级</div>
        <UserCenterBackButton />
      </div>
      <div class="summary-grid">
        <div><span>已加入班级</span><strong>{{ data.summary?.joinedCount || 0 }}</strong></div>
        <div><span>待审核申请</span><strong>{{ data.summary?.pendingApplyCount || 0 }}</strong></div>
        <div><span>待确认邀请</span><strong>{{ data.summary?.pendingInvitationCount || 0 }}</strong></div>
        <div><span>待完成任务</span><strong>{{ data.summary?.pendingTaskCount || 0 }}</strong></div>
        <div><span>平均成绩</span><strong>{{ data.summary?.averageScore || 0 }}</strong></div>
      </div>
    </section>

    <section class="glass-card panel">
      <div class="section-title small">快捷入口</div>
      <div class="quick-grid">
        <div v-for="item in data.quickEntries" :key="item.title" class="quick-card">
          <strong>{{ item.title }}</strong>
          <span>{{ item.subjectName }}</span>
          <el-button type="primary" plain @click="router.push(item.path)">进入任务</el-button>
        </div>
      </div>
    </section>

    <section class="glass-card panel">
      <div class="section-title small">已加入班级</div>
      <div class="joined-grid">
        <div v-for="item in data.joinedClassrooms" :key="item.classroomId" class="joined-card">
          <img :src="resolveClassroomCoverUrl(item.coverUrl, item.subjectName, item.name)" alt="" />
          <div>
            <strong>{{ item.name }}</strong>
            <p>{{ item.teacherName }} · {{ item.subjectName }}</p>
            <div class="muted">加入时间：{{ item.joinedAt }}</div>
            <div class="meta-row">
              <span>作业待完成 {{ item.pendingHomeworkCount || 0 }}</span>
              <span>考试待参加 {{ item.pendingExamCount || 0 }}</span>
              <span>平均成绩 {{ item.averageScore || 0 }}</span>
            </div>
            <div v-if="item.latestReminder" class="reminder-line">
              最近提醒：{{ item.latestReminder.statusLabel }} · {{ item.latestReminder.title }}
            </div>
          </div>
          <el-button type="primary" plain @click="router.push(item.detailPath)">进入班级</el-button>
        </div>
      </div>
    </section>

    <section class="glass-card panel">
      <div class="section-title small">最近作业 / 考试 / 成绩提醒</div>
      <el-table class="desktop-table" :data="data.recentReminders">
        <el-table-column prop="classroomName" label="班级" min-width="160" />
        <el-table-column prop="statusLabel" label="提醒类型" min-width="120" />
        <el-table-column prop="title" label="任务标题" min-width="220" />
        <el-table-column prop="remindAt" label="提醒时间" min-width="180" />
        <el-table-column label="操作" min-width="120">
          <template #default="{ row }">
            <el-button text type="primary" @click="router.push(row.detailPath)">进入班级</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="mobile-record-list">
        <article v-for="item in data.recentReminders" :key="`${item.classroomName}-${item.title}-${item.remindAt}`" class="mobile-record">
          <strong>{{ item.title }}</strong>
          <span>{{ item.classroomName }} · {{ item.statusLabel }}</span>
          <span>{{ item.remindAt }}</span>
          <el-button type="primary" plain @click="router.push(item.detailPath)">进入班级</el-button>
        </article>
      </div>
    </section>

    <section class="glass-card panel">
      <div class="section-title small">待确认邀请</div>
      <el-table class="desktop-table" :data="data.invitationRecords">
        <el-table-column prop="name" label="班级" />
        <el-table-column prop="teacherName" label="教师" />
        <el-table-column prop="subjectName" label="科目" />
        <el-table-column prop="remark" label="邀请说明" />
        <el-table-column prop="createdAt" label="邀请时间" />
        <el-table-column label="操作" width="180" align="right">
          <template #default="{ row }">
            <el-button type="primary" text @click="handleInvitation(row, 'ACCEPT')">接受</el-button>
            <el-button type="danger" text @click="handleInvitation(row, 'REJECT')">拒绝</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="mobile-record-list">
        <article v-for="item in data.invitationRecords" :key="item.id" class="mobile-record">
          <strong>{{ item.name }}</strong>
          <span>{{ item.teacherName }} · {{ item.subjectName }}</span>
          <span>{{ item.remark || '教师邀请加入班级' }}</span>
          <span>{{ item.createdAt }}</span>
          <div class="mobile-actions">
            <el-button type="primary" @click="handleInvitation(item, 'ACCEPT')">接受</el-button>
            <el-button type="danger" plain @click="handleInvitation(item, 'REJECT')">拒绝</el-button>
          </div>
        </article>
      </div>
      <el-empty v-if="!data.invitationRecords?.length" description="暂无待确认邀请" />
    </section>

    <section class="glass-card panel">
      <div class="section-title small">入班申请记录</div>
      <el-table class="desktop-table" :data="data.applicationRecords">
        <el-table-column prop="name" label="班级" />
        <el-table-column prop="teacherName" label="教师" />
        <el-table-column prop="subjectName" label="科目" />
        <el-table-column prop="remark" label="申请备注" />
        <el-table-column prop="statusLabel" label="状态" />
        <el-table-column prop="createdAt" label="申请时间" />
      </el-table>
      <div class="mobile-record-list">
        <article v-for="item in data.applicationRecords" :key="item.id" class="mobile-record">
          <strong>{{ item.name }}</strong>
          <span>{{ item.teacherName }} · {{ item.subjectName }}</span>
          <span>{{ item.statusLabel }} · {{ item.createdAt }}</span>
          <span>{{ item.remark || '暂无备注' }}</span>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.classroom-page,
.quick-grid,
.summary-grid {
  display: grid;
  gap: 20px;
}

.panel {
  padding: 24px;
}

.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.page-head .section-title {
  margin-bottom: 0;
}

.quick-grid {
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
}

.summary-grid {
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.summary-grid div {
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 16px;
  background: #fff;
}

.summary-grid span,
.muted,
.meta-row,
.reminder-line {
  color: var(--muted);
}

.summary-grid strong {
  display: block;
  margin-top: 8px;
  font-size: 26px;
  color: var(--primary);
}

.quick-card,
.joined-card {
  border: 1px solid var(--border);
  border-radius: 16px;
  padding: 16px;
  background: #fff;
}

.quick-card {
  display: grid;
  gap: 10px;
}

.joined-grid {
  display: grid;
  gap: 14px;
}

.joined-card {
  display: grid;
  grid-template-columns: 120px 1fr auto;
  gap: 16px;
  align-items: center;
}

.joined-card img {
  width: 120px;
  height: 72px;
  object-fit: cover;
  border-radius: 12px;
}

.meta-row {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 8px;
}

.reminder-line {
  margin-top: 8px;
}

.mobile-record-list {
  display: none;
}

.mobile-record {
  display: grid;
  gap: 8px;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
  background: #fff;
}

.mobile-record span {
  color: var(--muted);
  font-size: 13px;
}

.mobile-actions {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.mobile-actions :deep(.el-button) {
  margin-left: 0;
}

@media (max-width: 640px) {
  .panel {
    padding: 18px;
  }

  .quick-grid,
  .summary-grid {
    grid-template-columns: 1fr;
  }

  .joined-card {
    grid-template-columns: 1fr;
  }

  .joined-card img {
    width: 100%;
    height: auto;
    aspect-ratio: 16 / 9;
  }

  .joined-card :deep(.el-button) {
    width: 100%;
    margin-left: 0;
  }

  .desktop-table {
    display: none;
  }

  .mobile-record-list {
    display: grid;
    gap: 12px;
  }
}
</style>
