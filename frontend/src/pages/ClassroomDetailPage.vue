<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'
import { useAuthStore } from '../stores/auth'
import ClassroomChatPanel from '../components/ClassroomChatPanel.vue'
import { classroomCoverMeta, resolveAssetUrl, resolveClassroomCoverUrl } from '../utils/assets'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const detail = ref({ classroom: {}, members: [], currentUser: {}, materials: [], homeworks: [], classroomExams: [] })
const loading = ref(false)

async function loadData() {
  loading.value = true
  try {
    detail.value = await request.get(`/api/classrooms/${route.params.id}`)
  } finally {
    loading.value = false
  }
}

async function applyJoin() {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await request.post(`/api/classrooms/${route.params.id}/apply`, null, { params: { remark: '希望加入班级学习' } })
    ElMessage.success('申请已提交')
    await loadData()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function startHomework(homeworkId) {
  try {
    const data = await request.post(`/api/classroom-tasks/homeworks/${homeworkId}/start`)
    router.push(data.submitted ? `/classroom/homework/report/${data.submissionId}` : `/classroom/homework/start/${data.submissionId}`)
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function startClassroomExam(examId) {
  try {
    const data = await request.post(`/api/classroom-tasks/classroom-exams/${examId}/start`)
    router.push(data.submitted ? `/classroom/exam/report/${data.submissionId}` : `/classroom/exam/start/${data.submissionId}`)
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function homeworkStatus(item) {
  if (item.submissionStatus === 'SUBMITTED') {
    return {
      type: item.reviewStatus === 'REVIEWED' ? 'success' : 'warning',
      text: item.reviewStatus === 'REVIEWED' ? `已完成 · ${item.score ?? 0} 分` : '已提交待评阅'
    }
  }
  if (item.deadlineExpired) {
    return { type: 'info', text: '已截止' }
  }
  return { type: 'primary', text: '进行中' }
}

function examStatus(item) {
  if (item.submissionStatus === 'SUBMITTED') {
    return {
      type: item.reviewStatus === 'REVIEWED' ? 'success' : 'warning',
      text: item.reviewStatus === 'REVIEWED' ? `已完成 · ${item.score ?? 0} 分` : '已提交待评阅'
    }
  }
  if (item.notStarted) {
    return { type: 'info', text: '未开始' }
  }
  if (item.ended) {
    return { type: 'danger', text: '已结束' }
  }
  return { type: 'warning', text: '进行中' }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell" v-loading="loading">
    <section class="glass-card detail-box">
      <div class="cover-hero">
        <img :src="resolveClassroomCoverUrl(detail.classroom.coverUrl, detail.classroom.subjectName, detail.classroom.name)" alt="" />
        <div class="cover-hero-shade"></div>
        <div class="cover-hero-content" :style="{ '--cover-accent': classroomCoverMeta(detail.classroom.subjectName, detail.classroom.name).accent }">
          <span>{{ classroomCoverMeta(detail.classroom.subjectName, detail.classroom.name).subject }}</span>
          <strong>{{ classroomCoverMeta(detail.classroom.subjectName, detail.classroom.name).title }}</strong>
        </div>
      </div>
      <div>
        <div class="section-title">{{ detail.classroom.name }}</div>
        <p class="muted">{{ detail.classroom.intro }}</p>
        <div class="meta-row">
          <span>教师：{{ detail.classroom.teacherName }}</span>
          <span>科目：{{ detail.classroom.subjectName }}</span>
          <span>人数：{{ detail.classroom.memberCount }}/{{ detail.classroom.capacity }}</span>
        </div>
        <el-button
          v-if="!detail.currentUser.isMember && detail.currentUser.joinStatus !== 'PENDING'"
          type="primary"
          @click="applyJoin"
        >
          申请入班
        </el-button>
        <el-tag v-else-if="detail.currentUser.joinStatus === 'PENDING'" type="warning">申请审核中</el-tag>
        <el-tag v-else-if="detail.currentUser.isMember" type="success">已加入班级</el-tag>
      </div>
    </section>

    <section class="glass-card detail-box section-gap">
      <div class="section-title">班级成员</div>
      <div class="member-list">
        <div v-for="item in detail.members" :key="item.userId" class="member-item">
          <strong>{{ item.nickname }}</strong>
          <span>{{ item.roleCode }}</span>
        </div>
      </div>
    </section>

    <section v-if="detail.currentUser.isMember" class="glass-card detail-box section-gap">
      <ClassroomChatPanel :classroom-id="detail.classroom.id" compact />
    </section>

    <section class="glass-card detail-box section-gap">
      <div class="section-title">试听与试用资料</div>
      <div v-if="detail.materials.length" class="material-grid">
        <div v-for="item in detail.materials.filter((material) => material.materialCategory === 'TRIAL_VIDEO')" :key="item.id" class="material-card">
          <strong>{{ item.title }}</strong>
          <p class="muted">{{ item.description || '教师提供的试听视频，可先了解班级授课方式。' }}</p>
          <video v-if="item.fileUrl" :src="resolveAssetUrl(item.fileUrl)" class="trial-video" controls preload="none"></video>
        </div>
        <div class="material-card">
          <strong>试用资料</strong>
          <a
            v-for="item in detail.materials.filter((material) => material.materialCategory === 'TRIAL_DOCUMENT')"
            :key="item.id"
            :href="resolveAssetUrl(item.fileUrl)"
            target="_blank"
            class="attachment-link"
          >
            {{ item.title }} · {{ item.fileName || '下载资料' }}
          </a>
          <span v-if="!detail.materials.some((material) => material.materialCategory === 'TRIAL_DOCUMENT')" class="muted">当前暂无试用资料</span>
        </div>
      </div>
      <el-empty v-else description="当前班级暂未开放试听材料" />
    </section>

    <section class="glass-card detail-box section-gap">
      <div class="section-title">班级作业</div>
      <div class="task-list" v-if="detail.homeworks.length">
        <div v-for="item in detail.homeworks" :key="item.id" class="task-item task-card">
          <div class="task-main">
            <div class="task-head">
              <strong>{{ item.title }}</strong>
              <el-tag :type="homeworkStatus(item).type">{{ homeworkStatus(item).text }}</el-tag>
            </div>
            <div class="muted">{{ item.description }}</div>
            <div class="task-meta">
              <span>题量 {{ item.questionCount }}</span>
              <span>总分 {{ item.totalScore }}</span>
              <span>截止 {{ item.deadlineAt || '未设置' }}</span>
            </div>
            <a v-if="item.attachmentUrl" :href="resolveAssetUrl(item.attachmentUrl)" target="_blank" class="attachment-link">
              附件：{{ item.attachmentName || '查看附件' }}
            </a>
          </div>
          <el-button :disabled="!detail.currentUser.isMember || !item.allowStart" type="primary" @click="startHomework(item.id)">
            {{ item.submissionStatus === 'SUBMITTED' ? '查看报告' : '开始作业' }}
          </el-button>
        </div>
      </div>
      <el-empty v-else description="当前班级暂无作业" />
    </section>

    <section class="glass-card detail-box section-gap">
      <div class="section-title">班级考试</div>
      <div class="task-list" v-if="detail.classroomExams.length">
        <div v-for="item in detail.classroomExams" :key="item.id" class="task-item task-card">
          <div class="task-main">
            <div class="task-head">
              <strong>{{ item.title }}</strong>
              <el-tag :type="examStatus(item).type">{{ examStatus(item).text }}</el-tag>
            </div>
            <div class="muted">{{ item.description }}</div>
            <div class="task-meta">
              <span>题量 {{ item.questionCount }}</span>
              <span>总分 {{ item.totalScore }}</span>
              <span>时长 {{ item.durationMinutes }} 分钟</span>
              <span>开始 {{ item.startTime || '未设置' }}</span>
              <span>结束 {{ item.endTime || '未设置' }}</span>
            </div>
            <div v-if="item.antiCheatEnabled" class="anti-cheat-tip">防作弊提示：{{ item.antiCheatNotice || '考试过程中请勿切屏或离开页面。' }}</div>
          </div>
          <el-button :disabled="!detail.currentUser.isMember || !item.allowStart" type="warning" @click="startClassroomExam(item.id)">
            {{ item.submissionStatus === 'SUBMITTED' ? '查看成绩' : '参加考试' }}
          </el-button>
        </div>
      </div>
      <el-empty v-else description="当前班级暂无考试" />
    </section>
  </div>
</template>

<style scoped>
.detail-box {
  padding: 24px;
}

.cover-hero {
  position: relative;
  width: 100%;
  max-height: 280px;
  aspect-ratio: 5 / 1.8;
  overflow: hidden;
  border-radius: 18px;
  margin-bottom: 20px;
  background: #dbeafe;
}

.cover-hero img {
  width: 100%;
  height: 100%;
  max-height: 280px;
  object-fit: cover;
  display: block;
}

.cover-hero-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(8, 22, 48, 0.82), rgba(8, 22, 48, 0.36), rgba(8, 22, 48, 0.06));
}

.cover-hero-content {
  position: absolute;
  inset: 26px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  color: #fff;
}

.cover-hero-content span {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-left: 3px solid var(--cover-accent);
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 13px;
  font-weight: 700;
}

.cover-hero-content strong {
  margin-top: 14px;
  font-size: 34px;
  line-height: 1.12;
  font-weight: 800;
}

.meta-row,
.muted {
  color: var(--muted);
}

.meta-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  margin: 18px 0;
}

.member-list {
  display: grid;
  gap: 12px;
}

.attachment-link {
  display: inline-block;
  margin-top: 8px;
}

.material-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
}

.material-card {
  display: grid;
  gap: 10px;
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 14px;
  background: #fff;
}

.trial-video {
  width: 100%;
  border-radius: 12px;
  background: #000;
}

.task-list {
  display: grid;
  gap: 14px;
}

.anti-cheat-tip {
  margin-top: 10px;
  color: #b45309;
}

.task-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
  background: #fff;
}

.task-card {
  align-items: stretch;
}

.task-main {
  flex: 1;
}

.task-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 8px;
}

.task-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 10px;
  color: var(--muted);
}

.member-item {
  display: flex;
  justify-content: space-between;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 14px;
}

@media (max-width: 720px) {
  .task-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .task-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
