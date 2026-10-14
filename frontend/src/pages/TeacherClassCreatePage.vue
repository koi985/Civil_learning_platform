<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Collection, Picture, Plus, UserFilled, View } from '@element-plus/icons-vue'
import request from '../utils/request'
import { resolveAssetUrl, resolveClassroomCoverUrl, resolveFileUploadUrl } from '../utils/assets'

const router = useRouter()
const saving = ref(false)
const form = reactive({
  name: '',
  subjectName: '',
  coverFileId: null,
  coverUrl: '',
  intro: '',
  capacity: 100,
  isPublic: true,
  inviteCode: '',
  status: 'OPEN'
})
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('civil-token') || ''}` }
const visibilityMode = computed({
  get: () => (form.isPublic ? 'PUBLIC' : 'PRIVATE'),
  set: (value) => {
    form.isPublic = value === 'PUBLIC'
    if (form.isPublic) {
      form.inviteCode = ''
    } else {
      ensureInviteCode()
    }
  }
})
const coverPreviewUrl = computed(() => resolveClassroomCoverUrl(form.coverUrl, form.subjectName, form.name))
const visibilityText = computed(() => (form.isPublic ? '公开招生' : '私密邀请'))
const capacityText = computed(() => `${form.capacity || 0} 人上限`)

function generateInviteCode() {
  return String(Math.floor(100000 + Math.random() * 900000))
}

function ensureInviteCode() {
  if (!/^\d{6}$/.test(form.inviteCode || '')) {
    form.inviteCode = generateInviteCode()
  }
}

function refreshInviteCode() {
  form.inviteCode = generateInviteCode()
}

function handleUploadSuccess(response) {
  if (response.code === 200) {
    form.coverFileId = response.data.id
    form.coverUrl = response.data.url
    ElMessage.success('封面上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

async function saveClassroom() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入班级名称')
    return
  }
  if (!form.subjectName.trim()) {
    ElMessage.warning('请输入所属科目')
    return
  }
  if (form.isPublic) {
    form.inviteCode = ''
  } else {
    ensureInviteCode()
  }

  saving.value = true
  try {
    await request.post('/api/teacher/classrooms', form)
    ElMessage.success('班级创建成功')
    router.push('/teacher/class')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page-shell class-create-page">
    <section class="create-shell">
      <header class="page-head">
        <div>
          <h1>创建班级</h1>
          <p>创建用于作业、考试、成员管理的教学班级。</p>
        </div>
        <el-button plain @click="router.push('/teacher/class')">返回列表</el-button>
      </header>

      <div class="create-layout">
        <section class="form-panel">
          <div class="form-section">
            <div class="section-head">
              <h2>基础信息</h2>
              <span>学生会在班级列表中看到这些内容</span>
            </div>
            <div class="field-grid">
              <label class="field-block">
                <span>班级名称</span>
                <el-input v-model="form.name" maxlength="40" placeholder="如：2026 国考行测冲刺班" />
              </label>
              <label class="field-block">
                <span>所属科目</span>
                <el-input v-model="form.subjectName" maxlength="20" placeholder="如：行测、申论、面试" />
              </label>
            </div>
            <label class="field-block intro-field">
              <span>班级简介</span>
              <el-input
                v-model="form.intro"
                type="textarea"
                :rows="5"
                maxlength="300"
                show-word-limit
                placeholder="说明班级目标、适合人群、练习节奏和服务内容。"
              />
            </label>
          </div>

          <div class="form-section">
            <div class="section-head">
              <h2>招生设置</h2>
              <span>控制学生如何加入这个班级</span>
            </div>
            <div class="setting-grid">
              <label class="field-block capacity-field">
                <span>班级容量</span>
                <el-input-number v-model="form.capacity" :min="1" :max="999" controls-position="right" />
                <em>建议 20-100 人，达到上限后停止申请。</em>
              </label>
              <div class="field-block">
                <span>招生方式</span>
                <el-radio-group v-model="visibilityMode" class="visibility-options">
                  <el-radio-button value="PUBLIC">公开招生</el-radio-button>
                  <el-radio-button value="PRIVATE">私密邀请</el-radio-button>
                </el-radio-group>
                <em>{{ form.isPublic ? '学生可在班级列表申请加入。' : '仅通过邀请码或教师邀请加入。' }}</em>
              </div>
              <label v-if="!form.isPublic" class="field-block invite-field">
                <span>邀请码</span>
                <el-input v-model="form.inviteCode" readonly maxlength="6">
                  <template #append>
                    <el-button @click="refreshInviteCode">重新生成</el-button>
                  </template>
                </el-input>
                <em>系统自动生成 6 位数字邀请码，创建后可在班级管理中查看。</em>
              </label>
            </div>
          </div>

          <div class="form-section">
            <div class="section-head">
              <h2>视觉设置</h2>
              <span>建议使用 16:9 图片，突出课程类型和班级主题</span>
            </div>
            <el-upload
              :action="resolveFileUploadUrl('classroom')"
              :headers="uploadHeaders"
              :show-file-list="false"
              accept="image/*"
              @success="handleUploadSuccess"
            >
              <div class="upload-card" :class="{ filled: form.coverUrl }">
                <img v-if="form.coverUrl" :src="resolveAssetUrl(form.coverUrl)" alt="班级封面预览" />
                <div v-else class="upload-empty">
                  <el-icon><Picture /></el-icon>
                  <strong>上传班级封面</strong>
                  <span>支持 JPG、PNG，推荐 1280 x 720</span>
                </div>
                <div v-if="form.coverUrl" class="upload-mask">
                  <el-icon><Picture /></el-icon>
                  <span>重新上传</span>
                </div>
              </div>
            </el-upload>
          </div>

          <div class="submit-bar">
            <el-button type="primary" size="large" :loading="saving" @click="saveClassroom">
              <el-icon><Plus /></el-icon>
              创建班级
            </el-button>
            <el-button size="large" @click="router.push('/teacher/class')">取消</el-button>
          </div>
        </section>

        <aside class="preview-panel">
          <div class="preview-sticky">
            <div class="preview-title">
              <el-icon><View /></el-icon>
              <span>班级预览</span>
            </div>
            <div class="class-preview-card">
              <img :src="coverPreviewUrl" alt="班级封面预览" />
              <div class="preview-body">
                <div class="preview-status">
                  <span>{{ visibilityText }}</span>
                  <span>{{ form.status === 'OPEN' ? '开放中' : '未开放' }}</span>
                </div>
                <h3>{{ form.name || '2026 国考行测冲刺班' }}</h3>
                <p>{{ form.subjectName || '所属科目' }} · {{ capacityText }}</p>
                <div class="preview-intro">
                  {{ form.intro || '班级简介会展示在这里，帮助学生判断是否适合加入。' }}
                </div>
                <div class="preview-meta">
                  <span><el-icon><Collection /></el-icon> 作业 / 考试</span>
                  <span><el-icon><UserFilled /></el-icon> 成员管理</span>
                </div>
              </div>
            </div>
            <div class="helper-list">
              <strong>创建后可继续维护</strong>
              <span>审核入班申请、发布作业和考试、上传教学材料。</span>
            </div>
          </div>
        </aside>
      </div>
    </section>
  </div>
</template>

<style scoped>
.class-create-page {
  max-width: 1280px;
  padding-top: 28px;
}

.create-shell {
  border: 1px solid rgba(205, 216, 232, 0.78);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}

.page-head,
.submit-bar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
}

.page-head {
  padding: 24px 28px;
  border-bottom: 1px solid #e5edf7;
}

h1,
h2,
h3,
p {
  margin: 0;
}

h1 {
  color: #0f172a;
  font-size: 30px;
  line-height: 1.25;
  font-weight: 800;
}

.page-head p,
.section-head span,
.field-block em,
.helper-list span {
  color: var(--muted);
}

.page-head p {
  margin-top: 8px;
  font-size: 14px;
}

.create-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 0;
}

.form-panel {
  padding: 28px;
  border-right: 1px solid #e5edf7;
}

.form-section + .form-section {
  margin-top: 28px;
}

.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

h2 {
  color: #0f172a;
  font-size: 18px;
  line-height: 1.35;
  font-weight: 800;
}

.section-head span {
  font-size: 13px;
}

.field-grid,
.setting-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.field-block {
  display: grid;
  gap: 8px;
}

.field-block > span {
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}

.field-block em {
  font-size: 12px;
  font-style: normal;
  line-height: 1.5;
}

.intro-field {
  margin-top: 16px;
}

.capacity-field :deep(.el-input-number) {
  width: 180px;
}

.visibility-options {
  display: flex;
}

.invite-field {
  grid-column: 1 / -1;
  max-width: 380px;
}

.visibility-options :deep(.el-radio-button__inner) {
  min-width: 104px;
  font-weight: 700;
}

.upload-card {
  position: relative;
  display: grid;
  place-items: center;
  width: 100%;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  border: 1px dashed #b8c8dc;
  border-radius: 10px;
  background: #f8fbff;
  cursor: pointer;
  transition: border-color 0.16s ease, background-color 0.16s ease;
}

.upload-card:hover {
  border-color: #60a5fa;
  background: #f0f7ff;
}

.upload-card img,
.class-preview-card > img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-empty {
  display: grid;
  place-items: center;
  gap: 8px;
  color: #64748b;
  text-align: center;
}

.upload-empty .el-icon {
  color: #3b82f6;
  font-size: 34px;
}

.upload-empty strong {
  color: #0f172a;
  font-size: 16px;
}

.upload-empty span {
  font-size: 13px;
}

.upload-mask {
  position: absolute;
  inset: auto 12px 12px auto;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 8px;
  padding: 8px 10px;
  color: #fff;
  background: rgba(15, 23, 42, 0.72);
  font-size: 13px;
  font-weight: 700;
}

.submit-bar {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e5edf7;
  justify-content: start;
}

.submit-bar :deep(.el-button) {
  margin-left: 0;
  border-radius: 8px;
  font-weight: 700;
}

.submit-bar :deep(.el-icon) {
  margin-right: 4px;
}

.preview-panel {
  padding: 28px;
  background: #f8fbff;
}

.preview-sticky {
  position: sticky;
  top: 116px;
}

.preview-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  color: #0f172a;
  font-weight: 800;
}

.preview-title .el-icon {
  color: #2563eb;
}

.class-preview-card {
  overflow: hidden;
  border: 1px solid #dbe5f2;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.08);
}

.class-preview-card > img {
  display: block;
  height: 176px;
}

.preview-body {
  padding: 16px;
}

.preview-status {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.preview-status span {
  border-radius: 999px;
  padding: 5px 9px;
  color: #1d4ed8;
  background: #eaf2ff;
  font-size: 12px;
  font-weight: 700;
}

h3 {
  color: #0f172a;
  font-size: 18px;
  line-height: 1.35;
  font-weight: 800;
}

.preview-body p {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

.preview-intro {
  min-height: 58px;
  margin-top: 14px;
  color: #475569;
  font-size: 13px;
  line-height: 1.6;
}

.preview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.preview-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  border-radius: 8px;
  padding: 7px 9px;
  color: #475569;
  background: #eef4fb;
  font-size: 12px;
  font-weight: 700;
}

.helper-list {
  display: grid;
  gap: 6px;
  margin-top: 16px;
  border: 1px solid #dbe5f2;
  border-radius: 10px;
  background: #fff;
  padding: 14px;
}

.helper-list strong {
  color: #0f172a;
  font-size: 14px;
}

.helper-list span {
  font-size: 13px;
  line-height: 1.55;
}

@media (max-width: 1080px) {
  .create-layout {
    grid-template-columns: 1fr;
  }

  .form-panel {
    border-right: 0;
    border-bottom: 1px solid #e5edf7;
  }

  .preview-sticky {
    position: static;
  }
}

@media (max-width: 640px) {
  .page-head,
  .submit-bar,
  .field-grid,
  .setting-grid {
    grid-template-columns: 1fr;
  }

  .page-head,
  .form-panel,
  .preview-panel {
    padding: 18px;
  }

  .section-head {
    align-items: flex-start;
    flex-direction: column;
    gap: 6px;
  }

  h1 {
    font-size: 26px;
  }

  .capacity-field :deep(.el-input-number) {
    width: 100%;
  }

  .visibility-options {
    width: 100%;
  }

  .visibility-options :deep(.el-radio-button) {
    flex: 1;
  }

  .visibility-options :deep(.el-radio-button__inner) {
    width: 100%;
    min-width: 0;
  }
}
</style>
