<script setup>
import { computed } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const widthMap = {
  compact: '1200px',
  default: '1320px',
  wide: '1480px',
  create: '1280px'
}

const wrapStyle = computed(() => ({
  maxWidth: widthMap[route.meta.teacherBackWidth] || widthMap.default
}))

function goBack() {
  const previous = router.options.history.state.back
  if (previous && previous !== route.fullPath) {
    router.back()
    return
  }
  router.push('/teacher/dashboard')
}
</script>

<template>
  <div class="teacher-back-wrap" :style="wrapStyle">
    <el-button class="teacher-back-button" plain @click="goBack">
      <el-icon><ArrowLeft /></el-icon>
      <span>返回上一级</span>
    </el-button>
  </div>
</template>

<style scoped>
.teacher-back-wrap {
  box-sizing: border-box;
  margin: 18px auto -8px;
  padding: 0 16px;
}

.teacher-back-button {
  font-weight: 700;
}

.teacher-back-button .el-icon {
  margin-right: 4px;
}

@media (max-width: 768px) {
  .teacher-back-wrap {
    margin-top: 12px;
    padding: 0 16px;
  }
}
</style>
