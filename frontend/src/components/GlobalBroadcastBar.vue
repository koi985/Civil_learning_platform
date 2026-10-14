<script setup>
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useBroadcastStore } from '../stores/broadcast'

const broadcastStore = useBroadcastStore()
const AUTO_CLOSE_MS = 30000
const visible = ref(false)
let autoCloseTimer = null

const current = computed(() => broadcastStore.latest)
const currentKey = computed(() => broadcastStore.latestKey)
let recentTimer = null
let socketTimer = null
let displayedKey = null

function clearAutoCloseTimer() {
  if (autoCloseTimer) {
    window.clearTimeout(autoCloseTimer)
    autoCloseTimer = null
  }
}

function startAutoCloseTimer() {
  clearAutoCloseTimer()
  autoCloseTimer = window.setTimeout(() => {
    hideCurrentBroadcast()
    autoCloseTimer = null
  }, AUTO_CLOSE_MS)
}

function hideCurrentBroadcast() {
  if (current.value) {
    broadcastStore.markSeen(current.value)
  }
  visible.value = false
  clearAutoCloseTimer()
}

function closeBroadcast() {
  hideCurrentBroadcast()
}

watch(
  () => currentKey.value,
  (key) => {
    clearAutoCloseTimer()
    if (!key) {
      visible.value = false
      return
    }
    if (key === displayedKey) {
      return
    }
    displayedKey = key
    visible.value = true
    startAutoCloseTimer()
  }
)

onMounted(() => {
  recentTimer = window.setTimeout(() => {
    recentTimer = null
    broadcastStore.loadRecent().catch(() => {})
  }, 1800)
  socketTimer = window.setTimeout(() => {
    socketTimer = null
    broadcastStore.connect({ skipRecent: true })
  }, 5000)
})

onBeforeUnmount(() => {
  clearAutoCloseTimer()
  if (recentTimer) {
    window.clearTimeout(recentTimer)
    recentTimer = null
  }
  if (socketTimer) {
    window.clearTimeout(socketTimer)
    socketTimer = null
  }
  broadcastStore.disconnect()
})
</script>

<template>
  <transition name="broadcast-slide">
    <div v-if="visible && current" class="broadcast-bar">
      <div class="broadcast-main">
        <div class="broadcast-tag">全局广播</div>
        <div class="broadcast-title">{{ current.title }}</div>
        <div class="broadcast-content">{{ current.content }}</div>
      </div>
      <div class="broadcast-meta">
        <div>{{ current.senderName || '系统' }}</div>
        <div>{{ current.sentAt }}</div>
        <div v-if="current.receiverCount !== undefined">在线 {{ current.receiverCount }}</div>
      </div>
      <el-button text class="broadcast-close" @click="closeBroadcast">关闭</el-button>
    </div>
  </transition>
</template>

<style scoped>
.broadcast-bar {
  position: sticky;
  top: 0;
  z-index: 40;
  display: flex;
  gap: 14px;
  align-items: flex-start;
  padding: 14px 20px;
  background: linear-gradient(135deg, rgba(29, 78, 216, 0.96), rgba(15, 23, 42, 0.96));
  color: #fff;
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.22);
}

.broadcast-main {
  flex: 1;
  min-width: 0;
}

.broadcast-tag {
  display: inline-flex;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.broadcast-title {
  margin-top: 8px;
  font-size: 16px;
  font-weight: 700;
}

.broadcast-content {
  margin-top: 6px;
  opacity: 0.92;
  line-height: 1.6;
}

.broadcast-meta {
  min-width: 140px;
  text-align: right;
  font-size: 12px;
  opacity: 0.86;
  display: grid;
  gap: 4px;
}

.broadcast-close {
  color: #fff;
}

.broadcast-slide-enter-active,
.broadcast-slide-leave-active {
  transition: all 0.25s ease;
}

.broadcast-slide-enter-from,
.broadcast-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

@media (max-width: 900px) {
  .broadcast-bar {
    flex-direction: column;
  }

  .broadcast-meta {
    text-align: left;
    min-width: 0;
  }
}
</style>
