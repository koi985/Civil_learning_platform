<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { mobilePcPath, roleHomePath } from '../mobile/helpers'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const refreshing = ref(false)
const actionVisible = ref(false)

const title = computed(() => route.meta.mobileTitle || '公考学习')
const activeTab = computed(() => {
  if (route.path === '/m/student/profile') return '/m/student/profile'
  if (route.path.startsWith('/m/teacher')) return '/m/teacher'
  if (route.path.startsWith('/m/admin')) return '/m/admin'
  if (route.path.startsWith('/m/student')) return '/m/student'
  if (route.path.startsWith('/m/login')) return '/m/login'
  return '/m/home'
})
const canGoBack = computed(() => route.path !== '/m/home' && route.path !== roleHomePath(authStore.roles))
const showTabs = computed(() => !route.meta.hideMobileTabs)
const actions = computed(() => [
  { name: 'PC 完整版', key: 'pc' },
  ...(authStore.isLoggedIn ? [{ name: '退出登录', key: 'logout', color: '#ee0a24' }] : [])
])

function goBack() {
  if (canGoBack.value) {
    router.back()
  }
}

function changeTab(path) {
  if ((path === '/m/student' || path === '/m/student/profile') && !authStore.isLoggedIn) {
    router.push('/m/login')
    return
  }
  router.push(path)
}

function openPc() {
  localStorage.setItem('civil-last-mobile-path', route.fullPath)
  localStorage.setItem('civil-mobile-switch', '1')
  window.location.href = mobilePcPath(route.path, authStore.roles)
}

function logout() {
  authStore.logout()
  actionVisible.value = false
  router.replace('/m/home')
}

function handleAction(action) {
  if (action.key === 'logout') {
    logout()
    return
  }
  actionVisible.value = false
  openPc()
}

function refreshCurrent() {
  refreshing.value = true
  window.dispatchEvent(new CustomEvent('mobile-refresh'))
  window.setTimeout(() => {
    refreshing.value = false
  }, 450)
}
</script>

<template>
  <div class="mobile-shell">
    <van-nav-bar
      fixed
      placeholder
      safe-area-inset-top
      :title="title"
      :left-arrow="canGoBack"
      @click-left="goBack"
    >
      <template #right>
        <van-button class="mobile-more-button" size="small" plain type="primary" @click="actionVisible = true">更多</van-button>
      </template>
    </van-nav-bar>

    <van-pull-refresh v-model="refreshing" @refresh="refreshCurrent">
      <main class="mobile-scroll">
        <router-view />
      </main>
    </van-pull-refresh>

    <van-tabbar v-if="showTabs" :model-value="activeTab" fixed safe-area-inset-bottom @change="changeTab">
      <van-tabbar-item name="/m/home" icon="home-o">首页</van-tabbar-item>
      <van-tabbar-item name="/m/student" icon="todo-list-o">学习</van-tabbar-item>
      <van-tabbar-item name="/m/teacher" icon="records-o">教师</van-tabbar-item>
      <van-tabbar-item name="/m/admin" icon="chart-trending-o">运营</van-tabbar-item>
      <van-tabbar-item name="/m/student/profile" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>

    <van-action-sheet
      v-model:show="actionVisible"
      :actions="actions"
      cancel-text="取消"
      close-on-click-action
      @select="handleAction"
    />
  </div>
</template>

<style scoped>
.mobile-more-button {
  min-width: 48px;
  min-height: 32px;
}
</style>
