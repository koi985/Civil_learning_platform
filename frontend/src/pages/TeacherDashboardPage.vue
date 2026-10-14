<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import {
  Bell,
  CircleCheck,
  Collection,
  DataAnalysis,
  EditPen,
  Files,
  Notebook,
  Plus,
  Reading,
  TrendCharts,
  User
} from '@element-plus/icons-vue'
import request from '../utils/request'

const loading = ref(false)
const data = ref({ profile: {}, classrooms: [], pendingJoinCount: 0, homeworkCount: 0, examCount: 0, pendingReviewCount: 0 })
const recommendationLoading = ref(false)
const selectedRecommendationClassroomId = ref(null)
const recommendationPage = ref({ total: 0, pageNo: 1, pageSize: 6, records: [] })
const invitingIds = ref(new Set())

const classroomOptions = computed(() => data.value.classrooms || [])
const recommendationRows = computed(() => recommendationPage.value.records || [])

async function loadData() {
  loading.value = true
  try {
    data.value = await request.get('/api/teacher/dashboard')
    if (!selectedRecommendationClassroomId.value && data.value.classrooms?.length) {
      selectedRecommendationClassroomId.value = data.value.classrooms[0].id
    }
    await loadRecommendations()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function loadRecommendations() {
  if (!data.value.classrooms?.length) {
    recommendationPage.value = { total: 0, pageNo: 1, pageSize: recommendationPage.value.pageSize, records: [] }
    return
  }
  recommendationLoading.value = true
  try {
    recommendationPage.value = await request.get('/api/teacher/student-recommendations', {
      params: {
        classroomId: selectedRecommendationClassroomId.value || undefined,
        pageNo: recommendationPage.value.pageNo,
        pageSize: recommendationPage.value.pageSize
      }
    })
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    recommendationLoading.value = false
  }
}

function handleRecommendationClassroomChange() {
  recommendationPage.value.pageNo = 1
  loadRecommendations()
}

function handleRecommendationPageChange(pageNo) {
  recommendationPage.value.pageNo = pageNo
  loadRecommendations()
}

async function inviteStudent(row) {
  if (row.classroomFull) {
    ElMessage.warning('班级容量已满')
    return
  }
  const next = new Set(invitingIds.value)
  next.add(row.userId)
  invitingIds.value = next
  try {
    const result = await request.post(`/api/teacher/classrooms/${row.classroomId}/invite-students`, {
      userIds: [row.userId]
    })
    const invitedCount = Number(result.invitedCount || 0)
    ElMessage.success(invitedCount > 0 ? '已发送入班邀请，等待学生确认' : '学生已在班级中或已有待处理申请/邀请')
    await loadData()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    const nextSet = new Set(invitingIds.value)
    nextSet.delete(row.userId)
    invitingIds.value = nextSet
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell teacher-dashboard" v-loading="loading">
    <section class="dashboard-hero">
      <div class="hero-main">
        <div>
          <h1>教师工作台</h1>
          <p>管理班级、发布教学任务、查看成绩与批改进度</p>
        </div>
        <div class="audit-state">
          <span>审核状态</span>
          <strong>{{ data.profile.auditStatus || '待完善' }}</strong>
        </div>
      </div>

      <div class="overview-grid">
        <button class="metric-card urgent" type="button" @click="$router.push('/teacher/review')">
          <span class="metric-icon">
            <el-icon><EditPen /></el-icon>
          </span>
          <span class="metric-copy">
            <span>待批改任务</span>
            <strong>{{ data.pendingReviewCount }}</strong>
            <em>优先处理学生提交内容</em>
          </span>
        </button>
        <button class="metric-card warning" type="button" @click="$router.push('/teacher/class')">
          <span class="metric-icon">
            <el-icon><Bell /></el-icon>
          </span>
          <span class="metric-copy">
            <span>入班申请</span>
            <strong>{{ data.pendingJoinCount }}</strong>
            <em>待审核加入班级</em>
          </span>
        </button>
        <button class="metric-card" type="button" @click="$router.push('/teacher/class')">
          <span class="metric-icon">
            <el-icon><Collection /></el-icon>
          </span>
          <span class="metric-copy">
            <span>我的班级</span>
            <strong>{{ data.classrooms.length }}</strong>
            <em>正在管理的班级</em>
          </span>
        </button>
        <button class="metric-card" type="button" @click="$router.push('/teacher/homework')">
          <span class="metric-icon">
            <el-icon><Notebook /></el-icon>
          </span>
          <span class="metric-copy">
            <span>已发布作业</span>
            <strong>{{ data.homeworkCount }}</strong>
            <em>覆盖班级练习任务</em>
          </span>
        </button>
        <div class="stat-item">
          <span>已发布考试</span>
          <strong>{{ data.examCount }}</strong>
        </div>
      </div>
    </section>

    <section class="quick-panel section-gap">
      <div class="quick-group">
        <div class="quick-title">班级</div>
        <el-button plain @click="$router.push('/teacher/class/create')">
          <el-icon><Plus /></el-icon>
          新建班级
        </el-button>
        <el-button @click="$router.push('/teacher/class')">
          <el-icon><Collection /></el-icon>
          班级管理
        </el-button>
      </div>
      <div class="quick-group primary-group">
        <div class="quick-title">教学发布</div>
        <el-button type="primary" @click="$router.push('/teacher/homework')">
          <el-icon><Notebook /></el-icon>
          发布作业
        </el-button>
        <el-button type="primary" plain @click="$router.push('/teacher/exam')">
          <el-icon><Reading /></el-icon>
          发布考试
        </el-button>
      </div>
      <div class="quick-group">
        <div class="quick-title">教学管理</div>
        <el-button @click="$router.push('/teacher/review')">
          <el-icon><EditPen /></el-icon>
          批改中心
        </el-button>
        <el-button @click="$router.push('/teacher/report')">
          <el-icon><DataAnalysis /></el-icon>
          成绩报表
        </el-button>
        <el-button @click="$router.push('/teacher/profile')">
          <el-icon><User /></el-icon>
          教师资料
        </el-button>
        <el-button @click="$router.push('/teacher/setting')">
          <el-icon><User /></el-icon>
          账号设置
        </el-button>
      </div>
    </section>

    <section class="recommendation-panel section-gap">
      <div class="panel-head">
        <div>
          <h2>推荐学生</h2>
          <p>按班级科目、考试类型和地区匹配可邀请学员</p>
        </div>
        <div class="recommendation-tools">
          <el-select
            v-model="selectedRecommendationClassroomId"
            placeholder="选择班级"
            :disabled="!classroomOptions.length"
            @change="handleRecommendationClassroomChange"
          >
            <el-option v-for="item in classroomOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-button :disabled="!classroomOptions.length" @click="loadRecommendations">刷新</el-button>
        </div>
      </div>
      <el-table v-if="classroomOptions.length" v-loading="recommendationLoading" :data="recommendationRows" class="recommendation-table">
        <el-table-column label="学生" min-width="160">
          <template #default="{ row }">
            <div class="recommendation-student">
              <strong>{{ row.nickname || row.realName || '学员' }}</strong>
              <span>{{ row.province || '未设置地区' }} · {{ row.examType || '未设置考试' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="preferredSubject" label="偏好科目" min-width="110" />
        <el-table-column label="推荐班级" min-width="160">
          <template #default="{ row }">
            <div class="recommendation-student">
              <strong>{{ row.classroomName }}</strong>
              <span>{{ row.classroomSubjectName || '综合' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="匹配原因" min-width="180">
          <template #default="{ row }">
            <div class="reason-tags">
              <el-tag v-for="reason in row.matchReasons" :key="reason" size="small" effect="plain">{{ reason }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="名额" width="110" align="center">
          <template #default="{ row }">{{ row.studentCount }}/{{ row.capacity }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              :disabled="row.classroomFull"
              :loading="invitingIds.has(row.userId)"
              @click="inviteStudent(row)"
            >
              {{ row.classroomFull ? '已满' : '发送邀请' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="classroomOptions.length && !recommendationLoading && !recommendationRows.length" description="暂无可推荐学生" />
      <el-empty v-if="!classroomOptions.length" description="创建班级后即可推荐并邀请学生" />
      <div v-if="recommendationPage.total > recommendationPage.pageSize" class="recommendation-pagination">
        <el-pagination
          layout="prev, pager, next"
          :total="recommendationPage.total"
          :page-size="recommendationPage.pageSize"
          :current-page="recommendationPage.pageNo"
          @current-change="handleRecommendationPageChange"
        />
      </div>
    </section>

    <section class="todo-panel section-gap">
      <div class="panel-head">
        <div>
          <h2>待办提醒</h2>
          <p>需要教师处理的事项会优先出现在这里</p>
        </div>
      </div>
      <div class="todo-list">
        <button class="todo-item" type="button" @click="$router.push('/teacher/class')">
          <span class="todo-icon warning"><el-icon><Bell /></el-icon></span>
          <span>
            <strong>{{ data.pendingJoinCount }} 个入班申请待审核</strong>
            <em>处理后学生即可进入班级学习</em>
          </span>
        </button>
        <button class="todo-item" type="button" @click="$router.push('/teacher/review')">
          <span class="todo-icon urgent"><el-icon><EditPen /></el-icon></span>
          <span>
            <strong>{{ data.pendingReviewCount }} 个任务待批改</strong>
            <em>及时反馈有助于提升练习完成率</em>
          </span>
        </button>
        <button class="todo-item" type="button" @click="$router.push('/teacher/report')">
          <span class="todo-icon"><el-icon><TrendCharts /></el-icon></span>
          <span>
            <strong>查看班级成绩报表</strong>
            <em>跟踪考试与作业表现变化</em>
          </span>
        </button>
      </div>
    </section>

    <section class="classroom-panel section-gap">
      <div class="panel-head">
        <div>
          <h2>我的班级</h2>
          <p>从班级卡片直接进入成员、任务和成绩管理</p>
        </div>
        <el-button type="primary" plain @click="$router.push('/teacher/class/create')">
          <el-icon><Plus /></el-icon>
          新建班级
        </el-button>
      </div>
      <div class="classroom-grid">
        <div v-for="item in data.classrooms" :key="item.id" class="classroom-card">
          <div class="classroom-top">
            <div>
              <strong>{{ item.name }}</strong>
              <span>{{ item.subjectName || '综合课程' }}</span>
            </div>
            <span class="class-status">
              <el-icon><CircleCheck /></el-icon>
              正常
            </span>
          </div>
          <div class="class-meta">
            <span>{{ item.memberCount }} 人</span>
            <span>作业 {{ data.homeworkCount }}</span>
            <span>考试 {{ data.examCount }}</span>
          </div>
          <div class="class-actions">
            <el-button type="primary" text @click="$router.push('/teacher/class')">进入班级</el-button>
            <el-button text @click="$router.push('/teacher/homework')">发布作业</el-button>
            <el-button text @click="$router.push('/teacher/report')">成绩</el-button>
          </div>
        </div>
        <div v-if="!data.classrooms.length" class="empty-classroom">
          <el-icon><Files /></el-icon>
          <strong>还没有创建班级</strong>
          <span>创建第一个班级后，就可以邀请学生、发布作业和查看成绩。</span>
          <el-button type="primary" @click="$router.push('/teacher/class/create')">新建班级</el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.teacher-dashboard {
  max-width: 1320px;
  padding-top: 28px;
}

.dashboard-hero,
.quick-panel,
.recommendation-panel,
.todo-panel,
.classroom-panel {
  border: 1px solid rgba(205, 216, 232, 0.78);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}

.dashboard-hero {
  padding: 28px;
}

.hero-main,
.panel-head,
.classroom-top,
.class-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
}

.hero-main {
  margin-bottom: 22px;
}

h1,
h2,
p {
  margin: 0;
}

h1 {
  color: #0f172a;
  font-size: 30px;
  line-height: 1.25;
  font-weight: 800;
}

h2 {
  color: #0f172a;
  font-size: 24px;
  line-height: 1.3;
  font-weight: 800;
}

.hero-main p,
.panel-head p {
  margin-top: 8px;
  color: var(--muted);
  font-size: 14px;
}

.audit-state {
  min-width: 150px;
  padding: 10px 14px;
  border: 1px solid #d7e1f1;
  border-radius: 8px;
  background: #f8fbff;
  text-align: right;
}

.audit-state span {
  display: block;
  color: var(--muted);
  font-size: 12px;
}

.audit-state strong {
  display: block;
  margin-top: 4px;
  color: #15803d;
  font-size: 15px;
}

.overview-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.metric-card,
.stat-item {
  min-height: 118px;
  border: 1px solid #dbe5f2;
  border-radius: 10px;
  background: #fff;
  padding: 16px;
  text-align: left;
}

.metric-card {
  display: flex;
  gap: 14px;
  cursor: pointer;
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}

.metric-card:hover {
  border-color: #8bb7f5;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.12);
  transform: translateY(-1px);
}

.metric-icon,
.todo-icon {
  flex: 0 0 auto;
  display: grid;
  place-items: center;
  border-radius: 8px;
  color: #2563eb;
  background: #eaf2ff;
}

.metric-icon {
  width: 42px;
  height: 42px;
  font-size: 20px;
}

.metric-card.warning .metric-icon,
.todo-icon.warning {
  color: #d97706;
  background: #fff7e8;
}

.metric-card.urgent .metric-icon,
.todo-icon.urgent {
  color: #dc2626;
  background: #fff1f2;
}

.metric-copy,
.todo-item span:last-child {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.metric-copy span,
.stat-item span {
  color: #475569;
  font-size: 14px;
}

.metric-copy strong,
.stat-item strong {
  margin-top: 6px;
  color: #0f172a;
  font-size: 34px;
  line-height: 1;
  font-weight: 800;
}

.metric-copy em {
  margin-top: 9px;
  color: var(--muted);
  font-size: 12px;
  font-style: normal;
}

.stat-item {
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.stat-item strong {
  font-size: 30px;
}

.quick-panel {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  padding: 18px 22px;
}

.quick-group {
  display: flex;
  align-items: center;
  gap: 10px;
  padding-right: 18px;
  border-right: 1px solid #e2e8f0;
}

.quick-group:last-child {
  border-right: 0;
}

.quick-title {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.quick-group :deep(.el-button) {
  margin-left: 0;
  border-radius: 8px;
  font-weight: 700;
}

.quick-group :deep(.el-icon) {
  margin-right: 4px;
}

.todo-panel,
.recommendation-panel,
.classroom-panel {
  padding: 24px;
}

.recommendation-tools {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 320px;
}

.recommendation-tools :deep(.el-select) {
  flex: 1;
}

.recommendation-table {
  margin-top: 18px;
}

.recommendation-student {
  display: grid;
  gap: 4px;
}

.recommendation-student strong {
  color: #0f172a;
  font-size: 14px;
}

.recommendation-student span {
  color: var(--muted);
  font-size: 12px;
}

.reason-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.recommendation-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.todo-list {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-top: 18px;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 86px;
  border: 1px solid #dbe5f2;
  border-radius: 10px;
  background: #fff;
  padding: 14px;
  text-align: left;
  cursor: pointer;
}

.todo-item:hover {
  border-color: #93c5fd;
  background: #f8fbff;
}

.todo-icon {
  width: 38px;
  height: 38px;
}

.todo-item strong {
  color: #0f172a;
  font-size: 15px;
}

.todo-item em {
  margin-top: 5px;
  color: var(--muted);
  font-size: 12px;
  font-style: normal;
}

.classroom-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-top: 20px;
}

.classroom-card {
  border: 1px solid #dbe5f2;
  border-radius: 10px;
  background: linear-gradient(180deg, #ffffff 0%, #f9fbff 100%);
  padding: 18px;
}

.classroom-top strong {
  display: block;
  color: #0f172a;
  font-size: 18px;
  line-height: 1.35;
}

.classroom-top span:not(.class-status) {
  display: block;
  margin-top: 6px;
  color: var(--muted);
  font-size: 13px;
}

.class-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 8px;
  border-radius: 999px;
  color: #15803d;
  background: #ecfdf3;
  font-size: 12px;
  font-weight: 700;
}

.class-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 18px 0 14px;
}

.class-meta span {
  padding: 6px 10px;
  border-radius: 8px;
  color: #475569;
  background: #eef4fb;
  font-size: 13px;
}

.class-actions {
  justify-content: flex-start;
  border-top: 1px solid #e5edf7;
  padding-top: 10px;
}

.class-actions :deep(.el-button) {
  margin-left: 0;
  padding: 4px 0;
  font-weight: 700;
  min-width: 40px;
  min-height: 36px;
}

.empty-classroom {
  grid-column: 1 / -1;
  display: grid;
  place-items: center;
  gap: 10px;
  min-height: 220px;
  border: 1px dashed #b9c8dc;
  border-radius: 10px;
  color: var(--muted);
  background: #fbfdff;
  text-align: center;
}

.empty-classroom .el-icon {
  color: #94a3b8;
  font-size: 34px;
}

.empty-classroom strong {
  color: #0f172a;
}

@media (max-width: 1080px) {
  .overview-grid,
  .todo-list,
  .classroom-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .quick-panel {
    flex-direction: column;
  }

  .quick-group {
    border-right: 0;
    padding-right: 0;
  }
}

@media (max-width: 640px) {
  .dashboard-hero,
  .todo-panel,
  .classroom-panel {
    padding: 18px;
  }

  .hero-main,
  .panel-head,
  .classroom-top {
    align-items: flex-start;
    flex-direction: column;
  }

  .recommendation-tools {
    width: 100%;
    min-width: 0;
  }

  .audit-state {
    width: 100%;
    text-align: left;
  }

  .overview-grid,
  .todo-list,
  .classroom-grid {
    grid-template-columns: 1fr;
  }

  .quick-group {
    align-items: flex-start;
    flex-direction: column;
  }

  h1 {
    font-size: 26px;
  }
}
</style>
