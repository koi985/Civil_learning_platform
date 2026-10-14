<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const navs = [
  { label: '首页', path: '/' },
  { label: '课程', path: '/course' },
  { label: '资讯', path: '/news' },
  { label: '智能推荐', path: '/recommend' },
  { label: '班级', path: '/classroom' },
  { label: '成为教师', path: '/teacher/apply' }
]

const roleText = computed(() => authStore.roles.join(' / '))

function go(path) {
  router.push(path)
}

function logout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <header class="header-wrap">
    <div class="header-inner page-shell">
      <div class="brand" @click="go('/')">
        <div class="brand-mark">公</div>
        <div>
          <div class="brand-title">公考学习平台</div>
          <div class="brand-subtitle">课程 · 资讯 · 班级 · 推荐</div>
        </div>
      </div>
      <nav class="header-nav">
        <a v-for="item in navs" :key="item.path" @click.prevent="go(item.path)">{{ item.label }}</a>
      </nav>
      <div class="header-user">
        <template v-if="authStore.isLoggedIn && authStore.user">
          <div class="user-box">
            <div class="user-name">{{ authStore.user.nickname }}</div>
            <div class="user-role">{{ roleText || 'USER' }}</div>
          </div>
          <el-button v-if="authStore.roles.includes('ADMIN')" type="primary" plain @click="go('/admin/dashboard')">后台</el-button>
          <el-button v-if="authStore.roles.includes('TEACHER')" type="warning" plain @click="go('/teacher/dashboard')">教师端</el-button>
          <el-button @click="logout">退出</el-button>
        </template>
        <template v-else>
          <el-button @click="go('/login')">登录</el-button>
          <el-button type="primary" @click="go('/register')">注册</el-button>
        </template>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header-wrap {
  position: sticky;
  top: 0;
  z-index: 20;
  backdrop-filter: blur(16px);
  background: rgba(245, 247, 251, 0.82);
  border-bottom: 1px solid rgba(219, 227, 240, 0.8);
}

.header-inner {
  display: flex;
  align-items: center;
  gap: 20px;
  padding-top: 14px;
  padding-bottom: 14px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.brand-mark {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #1d4ed8, #2563eb);
  color: #fff;
  font-weight: 700;
}

.brand-title {
  font-size: 18px;
  font-weight: 700;
}

.brand-subtitle,
.user-role {
  font-size: 12px;
  color: var(--muted);
}

.header-nav {
  flex: 1;
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
}

.header-nav a {
  font-weight: 600;
  color: #334155;
}

.header-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-box {
  text-align: right;
}

.user-name {
  font-weight: 700;
}

@media (max-width: 980px) {
  .header-inner {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-user {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
