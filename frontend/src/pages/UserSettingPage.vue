<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Lock, Message, Phone, UploadFilled, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'
import { resolveAssetUrl, resolveFileUploadUrl } from '../utils/assets'

const DEFAULT_PROVINCES = [
  '全国', '北京', '天津', '河北', '山西', '内蒙古', '辽宁', '吉林', '黑龙江',
  '上海', '江苏', '浙江', '安徽', '福建', '江西', '山东', '河南', '湖北',
  '湖南', '广东', '广西', '海南', '重庆', '四川', '贵州', '云南', '西藏',
  '陕西', '甘肃', '青海', '宁夏', '新疆'
]
const loading = ref(false)
const saving = ref(false)
const passwordSaving = ref(false)
const options = ref({ provinces: [], examTypes: [], subjects: [] })
const profileForm = reactive({
  nickname: '',
  realName: '',
  email: '',
  phone: '',
  avatarUrl: '',
  province: '',
  examType: '',
  preferredSubject: ''
})
const passwordForm = reactive({
  oldPassword: '',
  newPassword: ''
})
const uploadHeaders = { Authorization: `Bearer ${localStorage.getItem('civil-token') || ''}` }
const provinceOptions = computed(() => {
  const merged = ['全国', ...(options.value.provinces || [])]
  return [...new Set(merged.filter(Boolean))]
})
const avatarText = computed(() => (profileForm.nickname || profileForm.realName || 'U1').slice(0, 2).toUpperCase())

async function loadData() {
  loading.value = true
  try {
    const data = await request.get('/api/user-center/settings')
    Object.assign(profileForm, data.profile)
    options.value = {
      provinces: data.preferenceOptions?.provinces?.length ? data.preferenceOptions.provinces : DEFAULT_PROVINCES,
      examTypes: data.preferenceOptions?.examTypes || [],
      subjects: data.preferenceOptions?.subjects || []
    }
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  saving.value = true
  try {
    const data = await request.post('/api/user-center/settings', profileForm)
    Object.assign(profileForm, data.profile)
    ElMessage.success('设置已保存')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

async function savePassword() {
  passwordSaving.value = true
  try {
    await request.post('/api/user-center/settings/password', passwordForm)
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    ElMessage.success('密码修改成功')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    passwordSaving.value = false
  }
}

function handleUploadSuccess(response) {
  if (response.code === 200) {
    profileForm.avatarUrl = response.data.url
    ElMessage.success('头像上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

onMounted(loadData)
</script>

<template>
  <div class="setting-page" v-loading="loading">
    <section class="setting-hero">
      <div>
        <div class="section-eyebrow">User Settings</div>
        <h1>账号设置</h1>
        <p>维护基础资料与备考偏好，推荐、课程和题库会优先参考这些信息。</p>
      </div>
      <div class="profile-summary">
        <span>{{ profileForm.province || '全国' }}</span>
        <span>{{ profileForm.examType || '未设置考试' }}</span>
        <span>{{ profileForm.preferredSubject || '未设置科目' }}</span>
      </div>
    </section>

    <section class="settings-layout">
      <aside class="glass-card avatar-panel">
        <div class="avatar-card">
          <img v-if="profileForm.avatarUrl" :src="resolveAssetUrl(profileForm.avatarUrl)" :alt="profileForm.nickname || '用户头像'" />
          <div v-else class="avatar-placeholder">{{ avatarText }}</div>
          <div class="avatar-copy">
            <strong>{{ profileForm.nickname || '未设置昵称' }}</strong>
            <span>{{ profileForm.email || '未设置邮箱' }}</span>
          </div>
          <el-upload
            :action="resolveFileUploadUrl('avatar')"
            :headers="uploadHeaders"
            :show-file-list="false"
            accept="image/*"
            @success="handleUploadSuccess"
          >
            <el-button plain :icon="UploadFilled">上传头像</el-button>
          </el-upload>
        </div>
      </aside>

      <div class="settings-stack">
        <section class="glass-card setting-panel">
          <div class="panel-head">
            <div>
              <div class="section-title">基础信息</div>
              <p>用于学习记录、班级申请和平台通知展示。</p>
            </div>
            <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
          </div>
          <div class="form-grid">
            <label class="field">
              <span>昵称</span>
              <el-input v-model="profileForm.nickname" :prefix-icon="User" placeholder="请输入昵称" />
            </label>
            <label class="field">
              <span>真实姓名</span>
              <el-input v-model="profileForm.realName" placeholder="请输入真实姓名" />
            </label>
            <label class="field">
              <span>邮箱</span>
              <el-input v-model="profileForm.email" :prefix-icon="Message" placeholder="请输入邮箱" />
            </label>
            <label class="field">
              <span>手机号</span>
              <el-input v-model="profileForm.phone" :prefix-icon="Phone" placeholder="请输入手机号" />
            </label>
          </div>
        </section>

        <section class="glass-card setting-panel">
          <div class="panel-head">
            <div>
              <div class="section-title">备考偏好</div>
              <p>地区支持选择全国，适合多省份或暂未确定目标地区的备考用户。</p>
            </div>
          </div>
          <div class="preference-grid">
            <label class="field">
              <span>目标地区</span>
              <el-select v-model="profileForm.province" placeholder="请选择地区" filterable>
                <el-option v-for="item in provinceOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </label>
            <label class="field">
              <span>考试类型</span>
              <el-select v-model="profileForm.examType" placeholder="请选择考试类型">
                <el-option v-for="item in options.examTypes" :key="item" :label="item" :value="item" />
              </el-select>
            </label>
            <label class="field">
              <span>偏好科目</span>
              <el-select v-model="profileForm.preferredSubject" placeholder="请选择偏好科目">
                <el-option v-for="item in options.subjects" :key="item" :label="item" :value="item" />
              </el-select>
            </label>
          </div>
          <div class="action-row">
            <el-button type="primary" :loading="saving" @click="saveProfile">保存偏好</el-button>
          </div>
        </section>

        <section class="glass-card setting-panel">
          <div class="panel-head">
            <div>
              <div class="section-title">密码修改</div>
              <p>建议定期更新密码，避免使用与其他网站相同的密码。</p>
            </div>
          </div>
          <div class="password-grid">
            <label class="field">
              <span>原密码</span>
              <el-input v-model="passwordForm.oldPassword" :prefix-icon="Lock" show-password placeholder="请输入原密码" />
            </label>
            <label class="field">
              <span>新密码</span>
              <el-input v-model="passwordForm.newPassword" :prefix-icon="Lock" show-password placeholder="请输入新密码" />
            </label>
            <div class="password-action">
              <el-button type="primary" :loading="passwordSaving" @click="savePassword">更新密码</el-button>
            </div>
          </div>
        </section>
      </div>
    </section>
  </div>
</template>

<style scoped>
.setting-page {
  display: grid;
  gap: 22px;
  padding-bottom: 24px;
}

.setting-hero {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-end;
  padding: 4px 2px 0;
}

.section-eyebrow {
  color: var(--primary);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0;
  text-transform: uppercase;
}

.setting-hero h1 {
  margin: 6px 0 8px;
  font-size: 30px;
  line-height: 1.2;
  color: var(--text);
}

.setting-hero p,
.panel-head p {
  margin: 0;
  color: var(--muted);
  line-height: 1.7;
}

.profile-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.profile-summary span {
  padding: 8px 12px;
  border: 1px solid rgba(59, 130, 246, 0.18);
  border-radius: 8px;
  background: rgba(59, 130, 246, 0.08);
  color: #1d4ed8;
  font-weight: 700;
}

.settings-layout {
  display: grid;
  grid-template-columns: minmax(220px, 280px) minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.avatar-panel,
.setting-panel {
  padding: 24px;
}

.avatar-panel {
  position: sticky;
  top: 96px;
}

.avatar-card {
  display: grid;
  gap: 16px;
  justify-items: center;
  text-align: center;
}

.avatar-card img,
.avatar-placeholder {
  width: 176px;
  height: 176px;
  border-radius: 8px;
  border: 1px solid rgba(148, 163, 184, 0.28);
  object-fit: cover;
  box-shadow: 0 18px 38px rgba(15, 23, 42, 0.08);
}

.avatar-placeholder {
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #f97316, #f59e0b);
  color: #fff;
  font-size: 56px;
  font-weight: 800;
}

.avatar-copy {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.avatar-copy strong {
  color: var(--text);
  font-size: 20px;
}

.avatar-copy span {
  color: var(--muted);
  word-break: break-word;
}

.settings-stack {
  display: grid;
  gap: 20px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 20px;
}

.section-title {
  color: var(--text);
  font-size: 24px;
  font-weight: 800;
  line-height: 1.3;
}

.form-grid,
.preference-grid,
.password-grid {
  display: grid;
  gap: 16px;
}

.form-grid {
  grid-template-columns: repeat(2, minmax(220px, 1fr));
}

.preference-grid {
  grid-template-columns: repeat(3, minmax(180px, 1fr));
}

.password-grid {
  grid-template-columns: repeat(2, minmax(220px, 1fr)) auto;
  align-items: end;
}

.field {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.field > span {
  color: #475569;
  font-size: 14px;
  font-weight: 700;
}

.field :deep(.el-input__wrapper),
.field :deep(.el-select__wrapper) {
  min-height: 44px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px rgba(148, 163, 184, 0.28) inset;
}

.field :deep(.el-input__wrapper.is-focus),
.field :deep(.el-select__wrapper.is-focused) {
  box-shadow: 0 0 0 1px var(--primary) inset, 0 0 0 3px rgba(59, 130, 246, 0.12);
}

.action-row {
  margin-top: 18px;
  display: flex;
  justify-content: flex-end;
}

.password-action {
  display: flex;
}

@media (max-width: 1100px) {
  .settings-layout,
  .form-grid,
  .preference-grid,
  .password-grid {
    grid-template-columns: 1fr;
  }

  .avatar-panel {
    position: static;
  }

  .password-action,
  .action-row {
    justify-content: flex-start;
  }
}

@media (max-width: 720px) {
  .setting-hero,
  .panel-head {
    display: grid;
  }

  .profile-summary {
    justify-content: flex-start;
  }

  .setting-hero h1 {
    font-size: 26px;
  }

  .avatar-panel,
  .setting-panel {
    padding: 18px;
  }
}
</style>
