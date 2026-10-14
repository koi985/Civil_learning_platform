<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import { useAuthStore } from '../../stores/auth'
import { roleHomePath } from '../../mobile/helpers'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const form = reactive({
  username: '',
  password: ''
})

async function submit() {
  if (!form.username || !form.password) {
    showFailToast('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    await authStore.login(form)
    showSuccessToast('登录成功')
    router.replace(route.query.redirect || roleHomePath(authStore.roles))
  } catch (error) {
    showFailToast(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="mobile-page">
    <section class="mobile-hero">
      <h1>移动端登录</h1>
      <p>复用现有账号体系，登录后按角色进入学员、教师或运营移动工作台。</p>
    </section>

    <van-form @submit="submit">
      <van-cell-group inset>
        <van-field
          v-model="form.username"
          name="username"
          label="账号"
          placeholder="请输入账号"
          autocomplete="username"
          :rules="[{ required: true, message: '请输入账号' }]"
        />
        <van-field
          v-model="form.password"
          type="password"
          name="password"
          label="密码"
          placeholder="请输入密码"
          autocomplete="current-password"
          :rules="[{ required: true, message: '请输入密码' }]"
        />
      </van-cell-group>
      <div style="margin: 16px;">
        <van-button round block type="primary" native-type="submit" :loading="loading">登录</van-button>
      </div>
    </van-form>
  </div>
</template>
