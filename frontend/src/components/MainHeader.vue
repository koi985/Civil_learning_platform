<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Close, LocationFilled, Menu } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import request from '../utils/request'
import { pcMobilePath, shouldShowMobileEntry } from '../mobile/helpers'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const regionDialogVisible = ref(false)
const mobileMenuVisible = ref(false)
const showMobileEntry = ref(false)
const regionSaving = ref(false)
const confirmedRegion = ref(localStorage.getItem('civil-exam-region') || '')
const draftRegion = ref(confirmedRegion.value || '重庆')

const navs = [
  { label: '首页', path: '/' },
  { label: '课程', path: '/course' },
  { label: '题库', path: '/question-bank' },
  { label: '搜题', path: '/search' },
  { label: '模考', path: '/exam' },
  { label: '资讯', path: '/news' },
  { label: '推荐', path: '/recommend' },
  { label: '班级', path: '/classroom' }
]

const examRegions = [
  '重庆', '四川', '贵州', '新疆', '湖南', '湖北', '陕西', '云南',
  '安徽', '甘肃', '河北', '河南', '吉林', '西藏', '福建', '海南',
  '山西', '北京', '广西', '江苏', '江西', '宁夏', '青海', '辽宁',
  '山东', '上海', '天津', '浙江', '广东', '内蒙古', '黑龙江'
]

const roleText = computed(() => authStore.roles.join(' / '))
const regionButtonText = computed(() => confirmedRegion.value || authStore.user?.province || '报考地区')
const isTeacherArea = computed(() => route.path.startsWith('/teacher'))
const isAdmin = computed(() => authStore.roles.includes('ADMIN'))
const isTeacher = computed(() => authStore.roles.includes('TEACHER'))

function go(path) {
  router.push(path)
}

function goMobile(path) {
  mobileMenuVisible.value = false
  go(path)
}

function updateMobileEntry() {
  showMobileEntry.value = shouldShowMobileEntry()
}

function goMobileVersion() {
  mobileMenuVisible.value = false
  router.push(pcMobilePath(route.path, authStore.roles))
}

function goProfileArea() {
  if (isAdmin.value) {
    router.push('/admin/setting')
    return
  }
  router.push(isTeacher.value ? '/teacher/setting' : '/user/profile')
}

function logout() {
  authStore.logout()
  mobileMenuVisible.value = false
  router.push('/login')
}

function openRegionDialog() {
  mobileMenuVisible.value = false
  draftRegion.value = confirmedRegion.value || authStore.user?.province || '重庆'
  regionDialogVisible.value = true
}

function selectRegion(region) {
  draftRegion.value = region
}

async function confirmRegion() {
  if (!draftRegion.value) {
    return
  }
  regionSaving.value = true
  const nextRegion = draftRegion.value
  const previousRegion = confirmedRegion.value || localStorage.getItem('civil-exam-region') || ''
  try {
    if (authStore.isLoggedIn) {
      await request.post('/api/recommend/exam-region', { province: nextRegion })
      if (authStore.user) {
        authStore.user.province = nextRegion
      }
    }
    confirmedRegion.value = nextRegion
    localStorage.setItem('civil-exam-region', nextRegion)
    window.dispatchEvent(new CustomEvent('civil-region-changed', {
      detail: { province: nextRegion, previousProvince: previousRegion }
    }))
    regionDialogVisible.value = false
    const { ElMessage } = await import('element-plus/es/components/message/index')
    ElMessage.success('报考地区已更新')
  } catch (error) {
    confirmedRegion.value = previousRegion
    if (previousRegion) {
      localStorage.setItem('civil-exam-region', previousRegion)
    } else {
      localStorage.removeItem('civil-exam-region')
    }
    const { ElMessage } = await import('element-plus/es/components/message/index')
    ElMessage.error(error.message || '报考地区保存失败')
  } finally {
    regionSaving.value = false
  }
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
  <header class="header-wrap" :class="{ 'teacher-area': isTeacherArea }">
    <div class="header-inner page-shell">
      <div class="brand" @click="go('/')">
        <div class="brand-mark">公</div>
        <div>
          <div class="brand-title">公考学习平台</div>
          <div class="brand-subtitle">课程 · 题库 · 模考 · 推荐 · 班级</div>
        </div>
      </div>
      <nav class="header-nav">
        <a
          v-for="item in navs"
          :key="item.path"
          :class="{ active: route.path === item.path || route.path.startsWith(`${item.path}/`) }"
          @click.prevent="go(item.path)"
        >
          {{ item.label }}
        </a>
      </nav>
      <div class="header-user">
        <el-button v-if="showMobileEntry" type="primary" plain @click="goMobileVersion">移动版</el-button>
        <template v-if="authStore.isLoggedIn && authStore.user">
          <div class="user-box" @click="goProfileArea">
            <div class="user-name">{{ authStore.user.nickname }}</div>
            <div class="user-role">{{ roleText || 'USER' }}</div>
          </div>
          <el-button v-if="!isTeacherArea" class="cart-entry" plain @click="go('/cart')">购物车</el-button>
          <el-button plain @click="openRegionDialog">{{ regionButtonText }}</el-button>
          <el-button v-if="!isAdmin && !isTeacher" type="primary" plain @click="go('/user/profile')">用户中心</el-button>
          <el-button
            v-if="authStore.roles.includes('TEACHER')"
            :type="isTeacherArea ? 'primary' : 'warning'"
            :plain="!isTeacherArea"
            @click="go('/teacher/dashboard')"
          >
            教师端
          </el-button>
          <el-button v-if="authStore.roles.includes('ADMIN')" type="success" plain @click="go('/admin/dashboard')">后台</el-button>
          <el-button @click="logout">退出</el-button>
        </template>
        <template v-else>
          <el-button @click="go('/login')">登录</el-button>
          <el-button type="primary" @click="go('/register')">注册</el-button>
        </template>
      </div>
      <button class="mobile-menu-button" type="button" aria-label="打开导航菜单" @click="mobileMenuVisible = true">
        <el-icon><Menu /></el-icon>
      </button>
    </div>

    <Teleport to="body">
      <el-drawer v-model="mobileMenuVisible" title="公考学习平台" size="100%" direction="rtl" class="mobile-nav-drawer">
        <div class="mobile-nav-panel">
          <button
            v-for="item in navs"
            :key="item.path"
            type="button"
            class="mobile-nav-item"
            :class="{ active: route.path === item.path || route.path.startsWith(`${item.path}/`) }"
            @click="goMobile(item.path)"
          >
            {{ item.label }}
          </button>

          <div class="mobile-nav-actions">
            <el-button v-if="showMobileEntry" type="primary" plain @click="goMobileVersion">移动版</el-button>
            <template v-if="authStore.isLoggedIn && authStore.user">
              <button type="button" class="mobile-profile" @click="goMobile(isTeacher ? '/teacher/dashboard' : '/user/profile')">
                <strong>{{ authStore.user.nickname }}</strong>
                <span>{{ roleText || 'USER' }}</span>
              </button>
              <el-button plain @click="openRegionDialog">{{ regionButtonText }}</el-button>
              <el-button v-if="!isTeacherArea" plain @click="goMobile('/cart')">购物车</el-button>
              <el-button v-if="!isAdmin && !isTeacher" type="primary" plain @click="goMobile('/user/profile')">用户中心</el-button>
              <el-button v-if="isTeacher" type="warning" plain @click="goMobile('/teacher/dashboard')">教师端</el-button>
              <el-button v-if="isAdmin" type="success" plain @click="goMobile('/admin/dashboard')">后台</el-button>
              <el-button @click="logout">退出</el-button>
            </template>
            <template v-else>
              <el-button @click="goMobile('/login')">登录</el-button>
              <el-button type="primary" @click="goMobile('/register')">注册</el-button>
            </template>
          </div>
        </div>
      </el-drawer>

      <div v-if="regionDialogVisible" class="region-overlay" @click.self="regionDialogVisible = false">
        <section class="region-panel" role="dialog" aria-modal="true" aria-labelledby="region-dialog-title">
          <button type="button" class="region-confirm" :disabled="regionSaving" @click="confirmRegion">
            {{ regionSaving ? '保存中' : '确定' }}
          </button>
          <button type="button" class="region-close" aria-label="关闭" @click="regionDialogVisible = false">
            <el-icon><Close /></el-icon>
          </button>
          <div class="region-head">
            <h2 id="region-dialog-title">Hi，同学</h2>
            <p>选择你要报考的地区、考试，为您定制学习内容</p>
          </div>
          <section class="region-section">
            <div class="region-title">
              <el-icon><LocationFilled /></el-icon>
              <span>报考地区</span>
            </div>
            <div class="region-grid">
              <button
                v-for="region in examRegions"
                :key="region"
                type="button"
                class="region-card"
                :class="{ active: draftRegion === region }"
                @click="selectRegion(region)"
              >
                {{ region }}
              </button>
            </div>
          </section>
        </section>
      </div>
    </Teleport>
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

.header-wrap.teacher-area {
  background: rgba(248, 251, 255, 0.9);
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
  font-size: 20px;
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

.header-nav a.active {
  color: var(--primary);
}

.header-user {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.mobile-menu-button {
  display: none;
  width: 40px;
  height: 40px;
  border: 1px solid #dbe3f0;
  border-radius: 8px;
  place-items: center;
  color: #334155;
  background: #fff;
  cursor: pointer;
}

.mobile-menu-button .el-icon {
  font-size: 20px;
}

.cart-entry {
  color: #64748b;
}

.user-box {
  text-align: right;
  cursor: pointer;
}

.user-name {
  font-weight: 700;
}

.region-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  background: rgba(15, 23, 42, 0.16);
}

.region-panel {
  position: relative;
  box-sizing: border-box;
  width: min(100vw, 1524px);
  min-height: 676px;
  padding: 54px 54px 70px;
  background: #fff;
}

.region-confirm {
  position: absolute;
  top: 20px;
  right: 70px;
  min-width: 82px;
  height: 40px;
  border: 1px solid #df4f55;
  border-radius: 6px;
  color: #fff;
  background: #df4f55;
  font-size: 18px;
  font-weight: 700;
  cursor: pointer;
}

.region-confirm:hover {
  background: #d94249;
}

.region-confirm:disabled {
  cursor: wait;
  opacity: 0.72;
}

.region-close {
  position: absolute;
  top: 22px;
  right: 24px;
  border: 0;
  padding: 0;
  color: #c2c7cf;
  background: transparent;
  font-size: 30px;
  line-height: 1;
  cursor: pointer;
}

.region-close:hover {
  color: #9098a3;
}

.mobile-nav-panel {
  display: grid;
  gap: 10px;
}

.mobile-nav-item,
.mobile-profile {
  width: 100%;
  min-height: 44px;
  border: 1px solid #dbe3f0;
  border-radius: 8px;
  padding: 10px 12px;
  color: #334155;
  background: #fff;
  text-align: left;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
}

.mobile-nav-item.active {
  border-color: #bfdbfe;
  color: var(--primary);
  background: #eff6ff;
}

.mobile-nav-actions {
  display: grid;
  gap: 10px;
  margin-top: 10px;
  padding-top: 14px;
  border-top: 1px solid #e2e8f0;
}

.mobile-nav-actions :deep(.el-button) {
  width: 100%;
  margin-left: 0;
}

.mobile-profile strong,
.mobile-profile span {
  display: block;
}

.mobile-profile span {
  margin-top: 4px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 600;
}

.region-head h2 {
  margin: 0;
  color: #343844;
  font-size: 34px;
  line-height: 1.2;
  font-weight: 800;
}

.region-head p {
  margin: 18px 0 0;
  color: #5f6672;
  font-size: 20px;
  line-height: 1.5;
}

.region-section {
  margin-top: 62px;
}

.region-title {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #343844;
  font-size: 26px;
  font-weight: 700;
}

.region-title .el-icon {
  color: #df4f55;
  font-size: 24px;
}

.region-grid {
  display: grid;
  grid-template-columns: repeat(8, minmax(0, 1fr));
  gap: 30px;
  margin-top: 30px;
}

.region-card {
  height: 60px;
  border: 1px solid #e4e7ec;
  border-radius: 6px;
  color: #5f6672;
  background: #fff;
  font-size: 24px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.16s ease, border-color 0.16s ease, color 0.16s ease;
}

.region-card:hover,
.region-card.active {
  border-color: #df4f55;
  color: #fff;
  background: #df4f55;
}

@media (max-width: 1080px) {
  .header-inner {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-nav,
  .header-user {
    width: 100%;
  }

  .region-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .header-inner {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }

  .brand-title {
    font-size: 16px;
  }

  .brand-subtitle,
  .header-nav,
  .header-user {
    display: none;
  }

  .mobile-menu-button {
    display: grid;
    flex: 0 0 auto;
  }

  .region-panel {
    min-height: 100vh;
    padding: 34px 20px 40px;
  }

  .region-head h2 {
    font-size: 28px;
  }

  .region-head p {
    font-size: 16px;
  }

  .region-section {
    margin-top: 38px;
  }

  .region-title {
    font-size: 22px;
  }

  .region-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16px;
  }

  .region-card {
    height: 52px;
    font-size: 18px;
  }
}
</style>
