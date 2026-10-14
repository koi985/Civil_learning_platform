<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { pcMobilePath, shouldShowMobileEntry } from '../mobile/helpers'
import {
  Avatar,
  Bell,
  ChatDotRound,
  Connection,
  DataAnalysis,
  Document,
  Histogram,
  HomeFilled,
  Money,
  Operation,
  Reading,
  Search,
  Setting,
  ShoppingCart,
  SwitchButton,
  Tickets,
  TrendCharts,
  User,
  UserFilled
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const keyword = ref('')
const showMobileEntry = ref(false)

const menuGroups = [
  {
    title: '工作台',
    items: [
      { label: '仪表盘', path: '/admin/dashboard', icon: HomeFilled },
      { label: '成长看板', path: '/admin/growth', icon: TrendCharts },
      { label: '数据统计', path: '/admin/stats', icon: Histogram },
      { label: '账号设置', path: '/admin/setting', icon: Setting }
    ]
  },
  {
    title: '人员',
    items: [
      { label: '用户管理', path: '/admin/user', icon: User },
      { label: '角色管理', path: '/admin/roles', icon: Avatar },
      { label: '教师审核', path: '/admin/teacher/audit', icon: UserFilled },
      { label: '教师管理', path: '/admin/teacher', icon: Connection }
    ]
  },
  {
    title: '内容',
    items: [
      { label: '课程管理', path: '/admin/course', icon: Reading },
      { label: '题库管理', path: '/admin/question', icon: Tickets },
      { label: '模考管理', path: '/admin/exam', icon: DataAnalysis },
      { label: '资讯管理', path: '/admin/news', icon: Document },
      { label: '评价管理', path: '/admin/evaluation', icon: ChatDotRound },
      { label: '公告管理', path: '/admin/notice', icon: Bell }
    ]
  },
  {
    title: '交易',
    items: [
      { label: '订单管理', path: '/admin/order', icon: ShoppingCart },
      { label: '退款处理', path: '/admin/refund', icon: Money },
      { label: '支付日志', path: '/admin/payment-log', icon: Operation }
    ]
  },
  {
    title: '系统',
    items: [
      { label: '推荐配置', path: '/admin/recommend', icon: Setting },
      { label: '系统设置', path: '/admin/system', icon: Setting },
      { label: '系统日志', path: '/admin/log', icon: Document }
    ]
  }
]

const flatMenus = computed(() => menuGroups.flatMap((group) => group.items))
const currentMenu = computed(() => flatMenus.value.find((item) => item.path === router.currentRoute.value.path))
const displayName = computed(() => authStore.user?.nickname || authStore.user?.username || '管理员')

function logout() {
  authStore.logout()
  router.push('/login')
}

function updateMobileEntry() {
  showMobileEntry.value = shouldShowMobileEntry()
}

function goMobileVersion() {
  router.push(pcMobilePath(route.path, authStore.roles))
}

function search() {
  if (!keyword.value.trim()) return
  router.push({ path: '/admin/user', query: { keyword: keyword.value.trim() } })
}

onMounted(() => {
  updateMobileEntry()
  window.addEventListener('resize', updateMobileEntry)
  window.addEventListener('storage', updateMobileEntry)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateMobileEntry)
  window.removeEventListener('storage', updateMobileEntry)
})
</script>

<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="sidebar-brand">
        <div class="brand-mark">C</div>
        <div>
          <div class="sidebar-title">运营后台</div>
          <div class="sidebar-subtitle">Civil Learning</div>
        </div>
      </div>
      <el-menu :default-active="$route.path" router class="admin-menu">
        <div v-for="group in menuGroups" :key="group.title" class="menu-group">
          <div class="menu-group-title">{{ group.title }}</div>
          <el-menu-item v-for="item in group.items" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </el-menu-item>
        </div>
      </el-menu>
      <div class="sidebar-footer">
        <el-button v-if="showMobileEntry" type="success" plain @click="goMobileVersion">移动版</el-button>
        <el-button type="primary" plain @click="$router.push('/')">返回前台</el-button>
        <el-button :icon="SwitchButton" @click="logout">退出登录</el-button>
      </div>
    </aside>
    <section class="admin-shell">
      <header class="admin-topbar">
        <div>
          <div class="topbar-label">当前模块</div>
          <h2>{{ currentMenu?.label || '运营后台' }}</h2>
        </div>
        <div class="topbar-actions">
          <el-input
            v-model="keyword"
            class="topbar-search"
            :prefix-icon="Search"
            placeholder="搜索用户"
            clearable
            @keyup.enter="search"
          />
          <button class="icon-button" type="button" aria-label="通知">
            <el-icon><Bell /></el-icon>
          </button>
          <el-button v-if="showMobileEntry" type="success" plain @click="goMobileVersion">移动版</el-button>
          <div class="admin-profile">
            <el-avatar :size="34" class="admin-avatar">{{ displayName.slice(0, 1) }}</el-avatar>
            <div>
              <strong>{{ displayName }}</strong>
              <span>系统管理员</span>
            </div>
          </div>
        </div>
      </header>
      <main class="admin-main">
        <router-view />
      </main>
    </section>
  </div>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 236px minmax(0, 1fr);
  background: #f4f7fb;
}

.admin-shell {
  min-width: 0;
}

.admin-sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  display: grid;
  grid-template-rows: auto 1fr auto;
  padding: 20px 14px;
  overflow: hidden;
  background: #0b1220;
  color: #fff;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 6px 18px;
}

.brand-mark {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #2563eb, #14b8a6);
  font-size: 20px;
  font-weight: 800;
}

.sidebar-title {
  font-size: 20px;
  font-weight: 700;
  line-height: 1.2;
}

.sidebar-subtitle {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 12px;
}

.admin-menu {
  min-width: 0;
  overflow-y: auto;
  border-right: 0;
  background: transparent;
}

.menu-group {
  margin-bottom: 12px;
}

.menu-group-title {
  padding: 10px 10px 6px;
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.admin-menu :deep(.el-menu-item) {
  height: 40px;
  margin: 2px 0;
  border-radius: 8px;
  color: #cbd5e1;
  background: transparent;
}

.admin-menu :deep(.el-menu-item:hover) {
  color: #fff;
  background: rgba(148, 163, 184, 0.14);
}

.admin-menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background: #2563eb;
}

.admin-menu :deep(.el-icon) {
  font-size: 17px;
}

.sidebar-footer {
  display: grid;
  gap: 12px;
  padding-top: 16px;
}

.sidebar-footer :deep(.el-button) {
  width: 100%;
  margin-left: 0;
}

.admin-shell {
  min-width: 0;
  display: grid;
  grid-template-rows: auto 1fr;
}

.admin-topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  min-height: 72px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 14px 24px;
  background: rgba(255, 255, 255, 0.9);
  border-bottom: 1px solid #e2e8f0;
  backdrop-filter: blur(14px);
}

.topbar-label {
  color: #64748b;
  font-size: 12px;
  font-weight: 700;
}

.admin-topbar h2 {
  margin: 3px 0 0;
  color: #0f172a;
  font-size: 20px;
  line-height: 1.25;
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.topbar-search {
  width: 260px;
}

.icon-button {
  width: 38px;
  height: 38px;
  border: 1px solid #dbe3f0;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: #475569;
  background: #fff;
  cursor: pointer;
}

.admin-profile {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 150px;
}

.admin-profile strong,
.admin-profile span {
  display: block;
}

.admin-profile strong {
  color: #0f172a;
  font-size: 14px;
}

.admin-profile span {
  margin-top: 2px;
  color: #64748b;
  font-size: 12px;
}

.admin-avatar {
  background: #1d4ed8;
}

.admin-main {
  min-width: 0;
  max-width: 100%;
  padding: 24px;
}

@media (max-width: 900px) {
  .admin-layout {
    grid-template-columns: 1fr;
    max-width: 100vw;
    overflow-x: hidden;
  }

  .admin-sidebar {
    position: relative;
    height: auto;
    max-height: 380px;
    overflow-y: auto;
    grid-template-rows: auto auto auto;
  }

  .admin-topbar {
    position: relative;
    align-items: flex-start;
    flex-direction: column;
  }

  .topbar-actions {
    width: 100%;
    flex-wrap: wrap;
  }

  .topbar-search {
    width: min(100%, 320px);
  }

  .admin-menu {
    max-height: 210px;
  }

  .sidebar-footer {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .admin-sidebar {
    max-height: 300px;
    padding: 16px 12px;
  }

  .admin-topbar {
    gap: 12px;
    padding: 14px 12px;
  }

  .admin-main {
    width: 100%;
    max-width: 100vw;
    overflow-x: hidden;
    padding: 16px 12px 32px;
  }

  .topbar-search,
  .topbar-actions,
  .topbar-actions :deep(.el-input) {
    width: 100%;
  }

  .admin-profile {
    min-width: 0;
  }
}
</style>
