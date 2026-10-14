<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import request from '../utils/request'
import { FILE_UPLOAD_URL } from '../utils/assets'

const loading = ref(false)
const form = reactive({
  realName: '',
  gender: '男',
  email: '',
  certificateFileId: null,
  intro: ''
})

async function uploadCertificate(option) {
  const fd = new FormData()
  fd.append('file', option.file)
  fd.append('bizType', 'certificate')
  try {
    const data = await request.post(FILE_UPLOAD_URL, fd, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    form.certificateFileId = data.id
    ElMessage.success('资质文件上传成功')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function submit() {
  loading.value = true
  try {
    await request.post('/api/teacher/apply', form)
    ElMessage.success('教师申请已提交')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="page-shell">
    <section class="glass-card apply-box">
      <div class="section-title">教师认证申请</div>
      <el-form :model="form" label-position="top">
        <div class="two-col">
          <el-form-item label="姓名">
            <el-input v-model="form.realName" />
          </el-form-item>
          <el-form-item label="性别">
            <el-select v-model="form.gender">
              <el-option label="男" value="男" />
              <el-option label="女" value="女" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="教师资格证 PDF">
          <el-upload :show-file-list="false" :http-request="uploadCertificate">
            <el-button type="primary">上传证书</el-button>
          </el-upload>
          <div class="muted">已上传文件 ID：{{ form.certificateFileId || '未上传' }}</div>
        </el-form-item>
        <el-form-item label="教师简介">
          <el-input v-model="form.intro" type="textarea" :rows="5" />
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="submit">提交审核</el-button>
      </el-form>
    </section>
  </div>
</template>

<style scoped>
.apply-box {
  padding: 24px;
}

.two-col {
  display: grid;
  gap: 14px;
  grid-template-columns: 1fr 1fr;
}

.muted {
  margin-top: 10px;
  color: var(--muted);
}
</style>
