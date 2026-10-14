<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request'
import { classroomCoverMeta, resolveAssetUrl, resolveClassroomCoverUrl } from '../utils/assets'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const detail = ref({
  teacher: {},
  stats: {},
  classrooms: []
})

const teacherName = computed(() => detail.value.teacher?.realName || detail.value.teacher?.nickname || '平台讲师')
const avatarInitial = computed(() => teacherName.value.slice(0, 1))

async function loadData() {
  loading.value = true
  try {
    detail.value = await request.get(`/api/teachers/${route.params.id}/public-detail`)
  } finally {
    loading.value = false
  }
}

function openClassroom(classroom) {
  router.push(`/classroom/detail/${classroom.id}`)
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell teacher-public-page" v-loading="loading">
    <section class="glass-card teacher-hero">
      <div class="teacher-identity">
        <img
          v-if="detail.teacher.avatarUrl"
          :src="resolveAssetUrl(detail.teacher.avatarUrl)"
          :alt="teacherName"
          class="teacher-avatar-large"
        />
        <div v-else class="teacher-avatar-large avatar-fallback">{{ avatarInitial }}</div>
        <div class="teacher-copy">
          <div class="teacher-title-row">
            <h1>{{ teacherName }}</h1>
            <el-tag type="warning" effect="light">认证讲师</el-tag>
          </div>
          <div class="teacher-meta">
            <span>{{ detail.teacher.province || '全国' }}</span>
            <span>{{ detail.teacher.preferredSubject || '综合辅导' }}</span>
            <span>{{ detail.teacher.gender || '教师' }}</span>
          </div>
          <p>{{ detail.teacher.intro || '专注公考备考辅导，提供系统化学习支持。' }}</p>
        </div>
      </div>
      <div class="teacher-stats">
        <div>
          <span>教学经验</span>
          <strong>{{ detail.stats.experienceYears || 3 }}年+</strong>
        </div>
        <div>
          <span>累计学员</span>
          <strong>{{ detail.stats.studentCount || 0 }}</strong>
        </div>
        <div>
          <span>好评率</span>
          <strong>{{ detail.stats.goodRate || 96 }}%</strong>
        </div>
        <div>
          <span>公开班级</span>
          <strong>{{ detail.stats.classroomCount || 0 }}</strong>
        </div>
      </div>
    </section>

    <section class="glass-card classroom-section">
      <div class="section-head">
        <div>
          <div class="section-title">公开班级</div>
          <p>查看该讲师当前开放报名的班级，进入班级详情后可申请入班。</p>
        </div>
        <el-button plain @click="router.push('/classroom')">全部班级</el-button>
      </div>

      <div v-if="detail.classrooms.length" class="classroom-grid">
        <article
          v-for="item in detail.classrooms"
          :key="item.id"
          class="classroom-card"
          role="button"
          tabindex="0"
          @click="openClassroom(item)"
          @keydown.enter="openClassroom(item)"
        >
          <div class="classroom-cover">
            <img :src="resolveClassroomCoverUrl(item.coverUrl, item.subjectName, item.name)" :alt="item.name" />
            <div class="cover-shade"></div>
            <div class="cover-content" :style="{ '--cover-accent': classroomCoverMeta(item.subjectName, item.name).accent }">
              <span>{{ classroomCoverMeta(item.subjectName, item.name).subject }}</span>
              <strong>{{ classroomCoverMeta(item.subjectName, item.name).title }}</strong>
            </div>
          </div>
          <div class="classroom-body">
            <h3>{{ item.name }}</h3>
            <p>{{ item.intro || '教师带学班级，提供阶段任务、资料和学习反馈。' }}</p>
            <div class="classroom-foot">
              <span>{{ item.subjectName || '综合辅导' }}</span>
              <strong>{{ item.memberCount || 0 }} / {{ item.capacity || 0 }} 人</strong>
            </div>
          </div>
        </article>
      </div>
      <el-empty v-else description="该讲师暂无公开班级" />
    </section>
  </div>
</template>

<style scoped>
.teacher-public-page {
  display: grid;
  gap: 22px;
  max-width: 1280px;
}

.teacher-hero,
.classroom-section {
  padding: 28px;
  border-radius: 12px;
}

.teacher-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 28px;
  align-items: center;
}

.teacher-identity {
  display: flex;
  gap: 20px;
  align-items: center;
  min-width: 0;
}

.teacher-avatar-large {
  width: 112px;
  height: 112px;
  flex: 0 0 auto;
  border: 4px solid #fff;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.14);
}

.avatar-fallback {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #2563eb, #0f766e);
  color: #fff;
  font-size: 38px;
  font-weight: 800;
}

.teacher-copy {
  min-width: 0;
}

.teacher-title-row,
.teacher-meta,
.classroom-foot,
.section-head {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.teacher-title-row h1 {
  margin: 0;
  color: #0f172a;
  font-size: 34px;
  line-height: 1.2;
}

.teacher-meta {
  margin-top: 10px;
  color: #64748b;
  font-weight: 700;
}

.teacher-meta span {
  border-radius: 6px;
  padding: 5px 9px;
  background: #f8fafc;
}

.teacher-copy p {
  max-width: 720px;
  margin: 16px 0 0;
  color: #475569;
  font-size: 16px;
  line-height: 1.8;
}

.teacher-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.teacher-stats div {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 16px;
  background: #f8fafc;
}

.teacher-stats span {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.teacher-stats strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
  font-size: 24px;
}

.section-head {
  justify-content: space-between;
  margin-bottom: 20px;
}

.section-title {
  color: #0f172a;
  font-size: 26px;
  line-height: 1.25;
  font-weight: 800;
}

.section-head p {
  margin: 6px 0 0;
  color: #64748b;
}

.classroom-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 18px;
}

.classroom-card {
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.classroom-card:hover,
.classroom-card:focus-visible {
  outline: none;
  border-color: rgba(37, 99, 235, 0.35);
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.1);
  transform: translateY(-2px);
}

.classroom-cover {
  position: relative;
  aspect-ratio: 5 / 2.25;
  overflow: hidden;
  background: #dbeafe;
}

.classroom-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.cover-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, rgba(8, 22, 48, 0.82), rgba(8, 22, 48, 0.2));
}

.cover-content {
  position: absolute;
  inset: 18px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  color: #fff;
}

.cover-content span {
  width: fit-content;
  border-left: 3px solid var(--cover-accent);
  border-radius: 6px;
  padding: 4px 10px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 13px;
  font-weight: 800;
}

.cover-content strong {
  margin-top: 10px;
  font-size: 26px;
  line-height: 1.2;
}

.classroom-body {
  padding: 16px;
}

.classroom-body h3 {
  margin: 0;
  color: #0f172a;
  font-size: 19px;
  line-height: 1.35;
}

.classroom-body p {
  min-height: 46px;
  margin: 10px 0 14px;
  color: #64748b;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.classroom-foot {
  justify-content: space-between;
  color: #475569;
}

.classroom-foot strong {
  color: #2563eb;
}

@media (max-width: 900px) {
  .teacher-hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .teacher-hero,
  .classroom-section {
    padding: 20px;
  }

  .teacher-identity {
    align-items: flex-start;
  }

  .teacher-avatar-large {
    width: 82px;
    height: 82px;
  }

  .teacher-title-row h1 {
    font-size: 26px;
  }

  .teacher-stats,
  .classroom-grid {
    grid-template-columns: 1fr;
  }
}
</style>
