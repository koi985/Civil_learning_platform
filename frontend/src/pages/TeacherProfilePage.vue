<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { Document, EditPen, Files, Medal, UserFilled } from '@element-plus/icons-vue'
import request from '../utils/request'
import { resolveAssetUrl, resolveFileUploadUrl, resolveFileViewUrl } from '../utils/assets'

const loading = ref(false)
const saving = ref(false)
const data = ref({ classroomCount: 0, pendingJoinCount: 0, profile: {}, user: {} })
const form = reactive({
  nickname: '',
  realName: '',
  gender: '',
  email: '',
  phone: '',
  avatarUrl: '',
  certificateFileId: null,
  intro: ''
})
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('civil-token') || ''}` }

const statusTagType = computed(() => {
  if (data.value.profile?.auditStatus === 'APPROVED') {
    return 'success'
  }
  if (data.value.profile?.auditStatus === 'REJECTED') {
    return 'danger'
  }
  return 'warning'
})

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '暂无'
}

async function loadData() {
  loading.value = true
  try {
    const result = await request.get('/api/teacher/profile')
    data.value = result
    Object.assign(form, result.user, result.profile)
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  saving.value = true
  try {
    const result = await request.post('/api/teacher/profile', form)
    data.value = result
    Object.assign(form, result.user, result.profile)
    ElMessage.success('教师资料已更新')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

function handleAvatarSuccess(response) {
  if (response.code === 200) {
    form.avatarUrl = response.data.url
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

function handleCertificateSuccess(response) {
  if (response.code === 200) {
    form.certificateFileId = response.data.id
    data.value.certificateName = response.data.originalName || `文件 #${response.data.id}`
    data.value.certificateUrl = resolveFileViewUrl(response.data.id)
    ElMessage.success('证书上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-shell profile-page" v-loading="loading">
    <section class="glass-card profile-overview">
      <div class="overview-head">
        <div>
          <div class="section-title">教师个人资料</div>
          <p class="muted">维护认证资料、证书文件与教学简介，保持教师端身份信息和审核状态一致。</p>
        </div>
        <el-tag :type="statusTagType" size="large" effect="light">{{ data.statusLabel || data.profile?.auditStatus || '待审核' }}</el-tag>
      </div>
      <div class="summary-grid">
        <div class="summary-card">
          <span class="summary-icon blue"><el-icon><Files /></el-icon></span>
          <div>
            <span>班级数量</span>
            <strong>{{ data.classroomCount }}</strong>
          </div>
        </div>
        <div class="summary-card">
          <span class="summary-icon amber"><el-icon><UserFilled /></el-icon></span>
          <div>
            <span>待审核申请</span>
            <strong>{{ data.pendingJoinCount }}</strong>
          </div>
        </div>
        <div class="summary-card">
          <span class="summary-icon green"><el-icon><Medal /></el-icon></span>
          <div>
            <span>审核时间</span>
            <strong class="small">{{ formatDateTime(data.auditedAt) }}</strong>
          </div>
        </div>
      </div>
      <div class="audit-box">
        <div class="audit-item">
          <span>审核备注</span>
          <strong>{{ data.auditRemark || '暂无审核备注' }}</strong>
        </div>
        <div class="audit-item">
          <span>证书文件</span>
          <strong>{{ data.certificateName || '未上传' }}</strong>
          <a v-if="form.certificateFileId" :href="resolveFileViewUrl(form.certificateFileId)" target="_blank" class="inline-link">查看当前证书</a>
        </div>
      </div>
    </section>

    <section class="glass-card profile-editor">
      <div class="editor-head">
        <div>
          <div class="editor-title">
            <el-icon><EditPen /></el-icon>
            <span>资料编辑</span>
          </div>
          <p class="muted">头像、联系方式和简介会用于教师端展示。</p>
        </div>
        <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
      </div>

      <div class="editor-layout">
        <aside class="identity-card">
          <img v-if="form.avatarUrl" :src="resolveAssetUrl(form.avatarUrl)" alt="" class="teacher-avatar" />
          <div v-else class="avatar-placeholder">头像</div>
          <strong>{{ form.realName || form.nickname || '教师姓名' }}</strong>
          <span>{{ form.nickname || '教师昵称' }}</span>
          <el-upload
            :action="resolveFileUploadUrl('teacher-avatar')"
            :headers="uploadHeaders"
            :show-file-list="false"
            accept="image/*"
            @success="handleAvatarSuccess"
          >
            <el-button plain>上传头像</el-button>
          </el-upload>
        </aside>

        <div class="editor-fields">
          <div class="form-grid">
            <label class="field-block">
              <span>昵称</span>
              <el-input v-model="form.nickname" placeholder="昵称" />
            </label>
            <label class="field-block">
              <span>真实姓名</span>
              <el-input v-model="form.realName" placeholder="真实姓名" />
            </label>
            <label class="field-block">
              <span>性别</span>
              <el-select v-model="form.gender" placeholder="性别">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </label>
            <label class="field-block">
              <span>邮箱</span>
              <el-input v-model="form.email" placeholder="邮箱" />
            </label>
            <label class="field-block">
              <span>手机号</span>
              <el-input v-model="form.phone" placeholder="手机号" />
            </label>
          </div>

          <div class="certificate-panel">
            <div class="certificate-meta">
              <span class="certificate-icon"><el-icon><Document /></el-icon></span>
              <div>
                <span>证书文件</span>
                <strong>{{ data.certificateName || '未上传证书文件' }}</strong>
              </div>
            </div>
            <el-upload
              :action="resolveFileUploadUrl('teacher-certificate')"
              :headers="uploadHeaders"
              :show-file-list="false"
              accept=".pdf,.png,.jpg,.jpeg"
              @success="handleCertificateSuccess"
            >
              <el-button>上传证书文件</el-button>
            </el-upload>
          </div>

          <label class="field-block intro-field">
            <span>教师简介</span>
            <el-input v-model="form.intro" type="textarea" :rows="5" placeholder="教师简介" />
          </label>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.profile-page {
  display: grid;
  gap: 18px;
  max-width: 1320px;
}

.profile-overview,
.profile-editor {
  border-radius: 12px;
  padding: 26px;
}

.overview-head,
.editor-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 20px;
}

.section-title {
  margin-bottom: 8px;
  color: #0f172a;
  font-size: 28px;
  line-height: 1.25;
  font-weight: 800;
}

.muted {
  color: var(--muted);
  line-height: 1.6;
  margin: 0;
}

.summary-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
}

.summary-card,
.audit-item {
  border: 1px solid var(--border);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.92);
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 112px;
  padding: 18px;
}

.summary-icon,
.certificate-icon {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 46px;
  height: 46px;
  border-radius: 10px;
  font-size: 22px;
}

.summary-icon.blue {
  color: #2563eb;
  background: #eaf2ff;
}

.summary-icon.amber {
  color: #d97706;
  background: #fff7e8;
}

.summary-icon.green {
  color: #15803d;
  background: #ecfdf3;
}

.summary-grid span,
.audit-item span {
  color: var(--muted);
}

.summary-grid strong,
.audit-item strong {
  display: block;
  margin-top: 6px;
  color: #0f172a;
  font-size: 26px;
  line-height: 1.25;
}

.summary-grid strong.small,
.audit-item strong {
  font-size: 16px;
  line-height: 1.6;
}

.audit-box {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  margin-top: 14px;
}

.audit-item {
  padding: 16px 18px;
}

.inline-link {
  display: inline-block;
  margin-top: 6px;
  color: var(--primary);
  font-size: 14px;
  font-weight: 700;
}

.editor-head {
  align-items: center;
  border-bottom: 1px solid #e5edf7;
  padding-bottom: 18px;
}

.editor-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

.editor-title .el-icon {
  color: var(--primary);
}

.editor-layout {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  gap: 24px;
}

.identity-card {
  display: grid;
  align-content: start;
  justify-items: center;
  gap: 10px;
  border: 1px solid #dbe5f2;
  border-radius: 12px;
  background: #f8fbff;
  padding: 20px;
  text-align: center;
}

.identity-card strong {
  color: #0f172a;
  font-size: 18px;
  line-height: 1.35;
}

.identity-card > span {
  color: var(--muted);
  font-size: 14px;
}

.teacher-avatar,
.avatar-placeholder {
  display: grid;
  place-items: center;
  width: 172px;
  height: 172px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: #fff;
  color: var(--muted);
  font-weight: 700;
  object-fit: cover;
}

.form-grid {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.field-block {
  display: grid;
  gap: 8px;
}

.field-block > span,
.certificate-meta span {
  color: #334155;
  font-size: 14px;
  font-weight: 700;
}

.field-block :deep(.el-input__wrapper),
.field-block :deep(.el-select__wrapper),
.intro-field :deep(.el-textarea__inner) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #dbe5f2 inset;
}

.field-block :deep(.el-input__inner),
.field-block :deep(.el-select__placeholder),
.field-block :deep(.el-select__selected-item) {
  font-size: 14px;
}

.certificate-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-top: 16px;
  border: 1px solid #dbe5f2;
  border-radius: 10px;
  background: #fff;
  padding: 14px;
}

.certificate-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.certificate-icon {
  color: #2563eb;
  background: #eaf2ff;
}

.certificate-meta strong {
  display: block;
  overflow-wrap: anywhere;
  margin-top: 4px;
  color: #0f172a;
  font-size: 15px;
}

.intro-field {
  margin-top: 16px;
}

.profile-editor :deep(.el-button) {
  border-radius: 8px;
  font-weight: 700;
}

@media (max-width: 720px) {
  .overview-head,
  .editor-head,
  .editor-layout,
  .form-grid {
    grid-template-columns: 1fr;
    display: grid;
  }

  .profile-overview,
  .profile-editor {
    padding: 18px;
  }

  .teacher-avatar,
  .avatar-placeholder {
    width: 150px;
    height: 150px;
  }
}
</style>
