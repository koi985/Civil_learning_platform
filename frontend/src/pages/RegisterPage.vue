<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'

const DEFAULT_PROVINCES = [
  '全国', '北京', '天津', '河北', '山西', '内蒙古', '辽宁', '吉林', '黑龙江',
  '上海', '江苏', '浙江', '安徽', '福建', '江西', '山东', '河南', '湖北',
  '湖南', '广东', '广西', '海南', '重庆', '四川', '贵州', '云南', '西藏',
  '陕西', '甘肃', '青海', '宁夏', '新疆'
]
const EXAM_TYPES = ['国考', '省考', '事业单位', '选调生', '面试']
const SUBJECTS = ['行测', '申论', '面试', '综合能力']

const router = useRouter()
const loading = ref(false)
const uploading = ref(false)
const form = reactive({
  roleCode: 'USER',
  username: '',
  password: '',
  nickname: '',
  province: '江苏',
  examType: '省考',
  preferredSubject: '行测',
  realName: '',
  gender: '男',
  email: '',
  certificateFileId: null,
  intro: '',
  adminRegisterCode: ''
})

const isTeacherRegister = computed(() => form.roleCode === 'TEACHER')
const isAdminRegister = computed(() => form.roleCode === 'ADMIN')
const provinceOptions = computed(() => {
  const merged = ['全国', ...DEFAULT_PROVINCES]
  return [...new Set(merged.filter(Boolean))]
})
const title = computed(() => {
  if (form.roleCode === 'TEACHER') return '创建辅导教师账号'
  if (form.roleCode === 'ADMIN') return '创建管理员账号'
  return '创建备考账号'
})

function validateForm() {
  if (!form.username.trim()) {
    ElMessage.warning('请输入用户名')
    return false
  }
  if (!form.password) {
    ElMessage.warning('请输入密码')
    return false
  }
  if (!form.nickname.trim()) {
    ElMessage.warning('请输入昵称')
    return false
  }
  if (isTeacherRegister.value) {
    if (!form.realName.trim()) {
      ElMessage.warning('请输入教师真实姓名')
      return false
    }
    if (!form.email.trim()) {
      ElMessage.warning('请输入教师邮箱')
      return false
    }
    if (!form.certificateFileId) {
      ElMessage.warning('请先上传教师资格证 PDF')
      return false
    }
    if (!form.intro.trim()) {
      ElMessage.warning('请输入教师简介')
      return false
    }
  }
  if (isAdminRegister.value && !form.adminRegisterCode.trim()) {
    ElMessage.warning('请输入管理员注册码')
    return false
  }
  return true
}

async function uploadCertificate(option) {
  uploading.value = true
  const fd = new FormData()
  fd.append('file', option.file)
  try {
    const data = await request.post('/api/files/register-certificate', fd, {
      anonymous: true,
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    form.certificateFileId = data.id
    ElMessage.success('资质文件上传成功')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    uploading.value = false
  }
}

function successMessage() {
  if (form.roleCode === 'TEACHER') return '教师账号已注册，认证申请待管理员审核'
  if (form.roleCode === 'ADMIN') return '管理员账号注册成功，请登录后台'
  return '注册成功，请登录'
}

async function submit() {
  if (!validateForm()) {
    return
  }
  loading.value = true
  try {
    await request.post('/api/auth/register', form)
    ElMessage.success(successMessage())
    router.push('/login')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-shell page-shell">
    <div class="auth-card glass-card">
      <h1>{{ title }}</h1>
      <el-form :model="form" label-position="top" @submit.prevent="submit">
        <el-form-item label="注册身份">
          <el-radio-group v-model="form.roleCode">
            <el-radio-button label="USER">备考用户</el-radio-button>
            <el-radio-button label="TEACHER">辅导教师</el-radio-button>
            <el-radio-button label="ADMIN">管理员</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" />
        </el-form-item>
        <div v-if="!isAdminRegister" class="auth-grid">
          <el-form-item label="省份">
            <el-select v-model="form.province" placeholder="请选择地区" filterable style="width: 100%">
              <el-option v-for="item in provinceOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="考试类型">
            <el-select v-model="form.examType" placeholder="请选择考试类型" style="width: 100%">
              <el-option v-for="item in EXAM_TYPES" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item v-if="!isAdminRegister" label="偏好科目">
          <el-select v-model="form.preferredSubject" placeholder="请选择偏好科目" style="width: 100%">
            <el-option v-for="item in SUBJECTS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <template v-if="isTeacherRegister">
          <div class="auth-grid">
            <el-form-item label="真实姓名">
              <el-input v-model="form.realName" />
            </el-form-item>
            <el-form-item label="性别">
              <el-select v-model="form.gender" style="width: 100%">
                <el-option label="男" value="男" />
                <el-option label="女" value="女" />
              </el-select>
            </el-form-item>
          </div>
          <el-form-item label="邮箱">
            <el-input v-model="form.email" />
          </el-form-item>
          <el-form-item label="教师资格证 PDF">
            <el-upload :show-file-list="false" :http-request="uploadCertificate" accept=".pdf">
              <el-button type="primary" plain :loading="uploading">上传证书</el-button>
            </el-upload>
            <div class="muted">已上传文件 ID：{{ form.certificateFileId || '未上传' }}</div>
          </el-form-item>
          <el-form-item label="教师简介">
            <el-input v-model="form.intro" type="textarea" :rows="4" maxlength="1000" show-word-limit />
          </el-form-item>
        </template>
        <el-form-item v-if="isAdminRegister" label="管理员注册码（示例：admin2026）">
          <el-input v-model="form.adminRegisterCode" type="password" show-password />
        </el-form-item>
        <el-button type="primary" :loading="loading" style="width: 100%" @click="submit">注册并进入平台</el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.auth-shell {
  display: grid;
  place-items: center;
  min-height: calc(100vh - 96px);
}

.auth-card {
  width: min(620px, 100%);
  padding: 28px;
}

.auth-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.muted {
  margin-top: 10px;
  color: var(--muted);
}

@media (max-width: 640px) {
  .auth-grid {
    grid-template-columns: 1fr;
  }
}
</style>
