<script setup>
import { computed } from 'vue'

const props = defineProps({
  title: { type: String, default: '暂无数据' },
  description: { type: String, default: '' },
  imageSize: { type: Number, default: 120 },
  showAction: { type: Boolean, default: false },
  actionText: { type: String, default: '刷新' }
})

const emit = defineEmits(['action'])
const finalDescription = computed(() => props.description || props.title)
</script>

<template>
  <div class="common-empty">
    <el-empty :description="finalDescription" :image-size="imageSize">
      <template #default v-if="$slots.default || showAction">
        <slot>
          <el-button v-if="showAction" type="primary" plain @click="emit('action')">{{ actionText }}</el-button>
        </slot>
      </template>
    </el-empty>
  </div>
</template>

<style scoped>
.common-empty {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}
</style>
