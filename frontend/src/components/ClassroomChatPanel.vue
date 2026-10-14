<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ChatRound, Picture, Promotion, Refresh } from '@element-plus/icons-vue'
import request from '../utils/request'
import { useAuthStore } from '../stores/auth'
import { FILE_UPLOAD_URL, resolveAssetUrl } from '../utils/assets'

const props = defineProps({
  classroomId: {
    type: [Number, String],
    default: null
  },
  compact: {
    type: Boolean,
    default: false
  }
})

const authStore = useAuthStore()
const messagesRef = ref(null)
const messages = ref([])
const loading = ref(false)
const sending = ref(false)
const uploading = ref(false)
const connected = ref(false)
const input = ref('')
const pageNo = ref(1)
const pageSize = 30
const total = ref(0)
let socket = null
let reconnectTimer = null
let intentionalClose = false

const emojiList = ['😀', '😊', '👍', '👏', '💪', '🔥', '🎯', '📌', '✅', '🙏', '🌟', '✍️']
const hasMore = computed(() => messages.value.length < total.value)
const canSend = computed(() => Boolean(props.classroomId) && authStore.isLoggedIn && !sending.value && input.value.trim())

function buildWebSocketUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.hostname}:8080/ws/classroom-chat`
}

function isMine(message) {
  return Number(message.senderId) === Number(authStore.user?.id)
}

function messageTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(5, 16)
}

function normalizeMessage(message) {
  return {
    ...message,
    id: Number(message.id),
    classroomId: Number(message.classroomId),
    senderId: Number(message.senderId)
  }
}

function upsertMessage(message) {
  const normalized = normalizeMessage(message)
  const index = messages.value.findIndex((item) => Number(item.id) === normalized.id)
  if (index >= 0) {
    messages.value[index] = normalized
  } else {
    messages.value.push(normalized)
    messages.value.sort((left, right) => Number(left.id) - Number(right.id))
  }
  scrollToBottom()
}

async function scrollToBottom() {
  await nextTick()
  const el = messagesRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

async function loadMessages(options = {}) {
  if (!props.classroomId || loading.value) return
  loading.value = true
  try {
    const targetPage = options.reset ? 1 : pageNo.value
    const data = await request.get(`/api/classrooms/${props.classroomId}/chat/messages`, {
      params: { pageNo: targetPage, pageSize },
      silent: true
    })
    const records = (data.records || []).map(normalizeMessage)
    total.value = Number(data.total || 0)
    if (options.reset) {
      messages.value = records
      pageNo.value = 1
      scrollToBottom()
    } else {
      const existingIds = new Set(messages.value.map((item) => item.id))
      messages.value = [...records.filter((item) => !existingIds.has(item.id)), ...messages.value]
    }
  } catch (error) {
    ElMessage.error(error.message || '班群消息加载失败')
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (!hasMore.value) return
  pageNo.value += 1
  await loadMessages()
}

function connectSocket() {
  if (!props.classroomId || !authStore.token) return
  if (socket && [WebSocket.OPEN, WebSocket.CONNECTING].includes(socket.readyState)) return
  socket = new WebSocket(buildWebSocketUrl())
  socket.onopen = () => {
    connected.value = true
    socket.send(JSON.stringify({ type: 'AUTH', token: authStore.token }))
    socket.send(JSON.stringify({ type: 'SUBSCRIBE_CLASSROOM', classroomId: Number(props.classroomId) }))
  }
  socket.onmessage = (event) => {
    let payload
    try {
      payload = JSON.parse(event.data)
    } catch (error) {
      return
    }
    if (payload.type === 'CLASSROOM_CHAT_MESSAGE' && Number(payload.classroomId) === Number(props.classroomId)) {
      upsertMessage(payload.message)
    }
  }
  socket.onclose = () => {
    connected.value = false
    socket = null
    if (intentionalClose) {
      intentionalClose = false
      return
    }
    scheduleReconnect()
  }
  socket.onerror = () => {
    connected.value = false
  }
}

function scheduleReconnect() {
  if (reconnectTimer || !props.classroomId || !authStore.token) return
  reconnectTimer = window.setTimeout(() => {
    reconnectTimer = null
    connectSocket()
  }, 4000)
}

function closeSocket() {
  if (reconnectTimer) {
    window.clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  if (socket) {
    intentionalClose = true
    socket.close()
    socket = null
  }
  connected.value = false
}

function appendEmoji(emoji) {
  input.value += emoji
}

async function sendText() {
  if (!canSend.value) return
  sending.value = true
  try {
    const data = await request.post(`/api/classrooms/${props.classroomId}/chat/messages`, {
      messageType: 'TEXT',
      content: input.value.trim()
    })
    input.value = ''
    upsertMessage(data)
  } catch (error) {
    ElMessage.error(error.message || '消息发送失败')
  } finally {
    sending.value = false
  }
}

async function uploadImage(option) {
  const file = option.file
  if (!file.type?.startsWith('image/')) {
    ElMessage.warning('只能发送图片')
    option.onError?.()
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 5MB')
    option.onError?.()
    return
  }
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('bizType', 'classroom-chat')
    const uploadResult = await request.post(FILE_UPLOAD_URL, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const message = await request.post(`/api/classrooms/${props.classroomId}/chat/messages`, {
      messageType: 'IMAGE',
      fileId: uploadResult.id,
      content: input.value.trim()
    })
    input.value = ''
    upsertMessage(message)
    option.onSuccess?.(uploadResult)
  } catch (error) {
    ElMessage.error(error.message || '图片发送失败')
    option.onError?.(error)
  } finally {
    uploading.value = false
  }
}

watch(
  () => props.classroomId,
  async (classroomId) => {
    closeSocket()
    messages.value = []
    total.value = 0
    pageNo.value = 1
    if (classroomId) {
      await loadMessages({ reset: true })
      connectSocket()
    }
  },
  { immediate: true }
)

onBeforeUnmount(closeSocket)
</script>

<template>
  <div class="classroom-chat-panel" :class="{ compact }">
    <div class="chat-head">
      <div>
        <strong>班群</strong>
        <span>{{ connected ? '在线' : '离线，可查看历史与发送消息' }}</span>
      </div>
      <el-button link type="primary" :loading="loading" @click="loadMessages({ reset: true })">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <div ref="messagesRef" class="chat-messages">
      <button v-if="hasMore" class="load-more" type="button" :disabled="loading" @click="loadMore">
        {{ loading ? '加载中...' : '加载更早消息' }}
      </button>
      <div v-for="message in messages" :key="message.id" class="chat-row" :class="{ mine: isMine(message) }">
        <div class="message-meta">
          <span>{{ message.senderName }}</span>
          <em>{{ message.senderRole === 'TEACHER' ? '老师' : '学员' }}</em>
          <time>{{ messageTime(message.createdAt) }}</time>
        </div>
        <div class="message-bubble">
          <template v-if="message.messageType === 'IMAGE'">
            <img :src="resolveAssetUrl(message.fileUrl)" :alt="message.fileName || '图片消息'" />
            <p v-if="message.content">{{ message.content }}</p>
          </template>
          <template v-else>{{ message.content }}</template>
        </div>
      </div>
      <el-empty v-if="!messages.length && !loading" description="还没有班群消息" />
    </div>

    <div class="emoji-row">
      <el-icon><ChatRound /></el-icon>
      <button v-for="emoji in emojiList" :key="emoji" type="button" @click="appendEmoji(emoji)">{{ emoji }}</button>
    </div>

    <div class="chat-input">
      <el-input
        v-model="input"
        type="textarea"
        :rows="2"
        maxlength="1000"
        show-word-limit
        resize="none"
        placeholder="输入班群消息"
        @keydown.ctrl.enter.prevent="sendText"
      />
      <div class="chat-actions">
        <el-upload :show-file-list="false" :http-request="uploadImage" accept="image/*">
          <el-button :loading="uploading">
            <el-icon><Picture /></el-icon>
            图片
          </el-button>
        </el-upload>
        <el-button type="primary" :disabled="!canSend" :loading="sending" @click="sendText">
          <el-icon><Promotion /></el-icon>
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.classroom-chat-panel {
  display: grid;
  gap: 14px;
  min-height: 460px;
}

.classroom-chat-panel.compact {
  min-height: 420px;
}

.chat-head,
.chat-actions,
.emoji-row,
.message-meta {
  display: flex;
  align-items: center;
}

.chat-head {
  justify-content: space-between;
}

.chat-head strong {
  display: block;
  font-size: 20px;
  color: #0f172a;
}

.chat-head span,
.message-meta,
.message-bubble p {
  color: #64748b;
  font-size: 13px;
}

.chat-messages {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 320px;
  overflow-y: auto;
  padding: 14px;
  background: #f8fbff;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
}

.chat-row {
  max-width: 76%;
}

.chat-row.mine {
  align-self: flex-end;
}

.message-meta {
  gap: 8px;
  margin-bottom: 4px;
}

.chat-row.mine .message-meta {
  justify-content: flex-end;
}

.message-meta em {
  font-style: normal;
  color: #2563eb;
}

.message-bubble {
  padding: 10px 12px;
  color: #0f172a;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  background: #ffffff;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
}

.chat-row.mine .message-bubble {
  color: #ffffff;
  background: #2563eb;
  border-color: #2563eb;
}

.message-bubble img {
  display: block;
  max-width: min(280px, 54vw);
  max-height: 240px;
  object-fit: contain;
  border-radius: 6px;
  background: #ffffff;
}

.chat-row.mine .message-bubble p {
  color: #dbeafe;
}

.emoji-row {
  gap: 8px;
  flex-wrap: wrap;
  color: #64748b;
}

.emoji-row button {
  width: 32px;
  height: 32px;
  border: 1px solid #dbe7f5;
  border-radius: 8px;
  background: #ffffff;
  cursor: pointer;
}

.chat-input {
  display: grid;
  gap: 10px;
}

.chat-actions {
  justify-content: flex-end;
  gap: 10px;
}

.load-more {
  align-self: center;
  padding: 6px 12px;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  cursor: pointer;
}

@media (max-width: 720px) {
  .chat-row {
    max-width: 92%;
  }

  .chat-messages {
    height: 300px;
  }
}
</style>
