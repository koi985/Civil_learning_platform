<script setup>
import { computed, onBeforeUnmount, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../../stores/auth'
import { roleName } from '../../mobile/helpers'

const router = useRouter()
const authStore = useAuthStore()
const user = computed(() => authStore.user || {})
const isAdmin = computed(() => authStore.roles.includes('ADMIN'))
const isTeacher = computed(() => authStore.roles.includes('TEACHER'))
const pcProfilePath = computed(() => {
  if (isAdmin.value) return '/admin/setting'
  if (isTeacher.value) return '/teacher/setting'
  return '/user/profile'
})

function logout() {
  authStore.logout()
  router.replace('/m/home')
}

async function fetchMe() {
  if (authStore.token && !authStore.user) await authStore.fetchMe()
}

onMounted(() => {
  fetchMe()
  window.addEventListener('mobile-refresh', fetchMe)
})
onBeforeUnmount(() => window.removeEventListener('mobile-refresh', fetchMe))
</script>

<template>
  <div class="mobile-page">
    <section class="mobile-hero">
      <h1>{{ user.nickname || user.username || '个人中心' }}</h1>
      <p>{{ roleName(authStore.roles) }} · {{ user.email || '未绑定邮箱' }}</p>
    </section>

    <van-cell-group inset>
      <van-cell title="账号" :value="user.username || '-'" />
      <van-cell title="昵称" :value="user.nickname || '-'" />
      <van-cell title="手机号" :value="user.phone || '-'" />
      <van-cell title="邮箱" :value="user.email || '-'" />
    </van-cell-group>

    <van-cell-group inset>
      <van-cell v-if="!isAdmin && !isTeacher" title="我的课程" is-link @click="router.push('/m/student/courses')" />
      <van-cell v-if="!isAdmin && !isTeacher" title="每日任务" is-link @click="router.push('/m/student/tasks')" />
      <van-cell v-if="!isAdmin && !isTeacher" title="我的班级" is-link @click="router.push('/m/student/classrooms')" />
      <van-cell v-if="isTeacher" title="教师工作台" is-link @click="router.push('/m/teacher')" />
      <van-cell v-if="isAdmin" title="运营看板" is-link @click="router.push('/m/admin')" />
      <van-cell title="PC 完整版个人中心" is-link @click="router.push(pcProfilePath)" />
    </van-cell-group>

    <div style="margin: 16px;">
      <van-button block plain type="danger" @click="logout">退出登录</van-button>
    </div>
  </div>
</template>
