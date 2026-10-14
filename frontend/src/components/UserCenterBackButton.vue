<script setup>
import { ArrowLeft } from '@element-plus/icons-vue'
</script>

<template>
  <el-button class="user-center-back" plain @click="$router.push('/user/profile')">
    <el-icon><ArrowLeft /></el-icon>
    <span>返回用户中心</span>
  </el-button>
</template>

<style scoped>
.user-center-back {
  font-weight: 700;
}

.user-center-back .el-icon {
  margin-right: 4px;
}
</style>
