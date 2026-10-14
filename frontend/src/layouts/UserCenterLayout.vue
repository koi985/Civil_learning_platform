<script setup>
const menus = [
  { label: '概览', path: '/user/profile' },
  { label: '学习计划', path: '/user/plan' },
  { label: '成长档案', path: '/user/archive' },
  { label: '阶段诊断', path: '/user/diagnosis' },
  { label: '独立评测', path: '/user/assessment' },
  { label: '每日任务', path: '/user/task' },
  { label: '知识点掌握', path: '/user/knowledge' },
  { label: '个性化测卷', path: '/user/test-paper' },
  { label: '测卷报告', path: '/user/test-paper-report' },
  { label: '订单中心', path: '/user/order' },
  { label: '学习统计', path: '/user/stat' },
  { label: 'VIP 信息', path: '/user/vip' },
  { label: '导出中心', path: '/user/export' },
  { label: '账号设置', path: '/user/setting' }
]
</script>

<template>
  <div class="user-layout">
    <aside class="user-sidebar glass-card">
      <div class="sidebar-title">用户中心</div>
      <el-menu :default-active="$route.path" router>
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          {{ item.label }}
        </el-menu-item>
        <el-menu-item index="/my-course">我的课程</el-menu-item>
        <el-menu-item index="/wrong-book">错题本</el-menu-item>
        <el-menu-item index="/my-classroom">我的班级</el-menu-item>
      </el-menu>
    </aside>
    <main class="user-main">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.user-layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 20px;
  width: min(1800px, calc(100% - 48px));
  margin: 24px auto 0;
  box-sizing: border-box;
}

.user-sidebar {
  padding: 18px;
  height: fit-content;
}

.sidebar-title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 16px;
}

.user-sidebar :deep(.el-menu) {
  border-right: 0;
}

.user-sidebar :deep(.el-menu-item) {
  height: 42px;
  border-radius: 8px;
  margin: 4px 0;
  color: #334155;
}

.user-sidebar :deep(.el-menu-item:hover) {
  color: var(--primary);
  background: #eff6ff;
}

.user-sidebar :deep(.el-menu-item.is-active) {
  position: relative;
  color: var(--primary);
  background: #eff6ff;
  font-weight: 700;
}

.user-sidebar :deep(.el-menu-item.is-active::before) {
  content: "";
  position: absolute;
  left: 0;
  top: 9px;
  width: 3px;
  height: 24px;
  border-radius: 999px;
  background: var(--primary);
}

.user-main {
  min-width: 0;
}

@media (max-width: 900px) {
  .user-layout {
    grid-template-columns: 1fr;
    width: min(100% - 24px, 720px);
    margin-top: 16px;
  }
}
</style>
