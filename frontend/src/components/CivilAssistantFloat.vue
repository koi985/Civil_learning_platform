<script setup>
import { computed, nextTick, onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ChatDotRound,
  Close,
  CopyDocument,
  Delete,
  FullScreen,
  List,
  Plus,
  Promotion,
  RefreshRight,
  ScaleToOriginal
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import request from '../utils/request'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const open = ref(false)
const input = ref('')
const loading = ref(false)
const expanded = ref(false)
const showSessions = ref(false)
const messagesRef = ref(null)
const position = reactive(loadPosition())
const drag = reactive({
  active: false,
  moved: false,
  startX: 0,
  startY: 0,
  startRight: 0,
  startBottom: 0
})

const quickPrompts = [
  '制定一周备考计划',
  '申论怎么入门',
  '行测提速方法'
]

const SESSION_LIST_KEY = 'civil-assistant-sessions'
const CURRENT_SESSION_KEY = 'civil-assistant-current-session-id'
const LEGACY_MESSAGES_KEY = 'civil-assistant-messages'
const LEGACY_CONVERSATION_KEY = 'civil-assistant-conversation-id'
const MAX_SESSION_COUNT = 12
const MAX_MESSAGES_PER_SESSION = 40

const defaultMessages = [
  {
    role: 'assistant',
    content: '你好，我是公考小助手。可以帮你梳理备考计划、拆解题型思路、推荐学习路径。'
  }
]

const initialSessionState = loadSessionState()
const sessions = ref(initialSessionState.sessions)
const currentSessionId = ref(initialSessionState.currentSessionId)
const conversationId = ref(initialSessionState.conversationId)
const messages = ref(initialSessionState.messages)

const sortedSessions = computed(() => [...sessions.value].sort((a, b) => b.updatedAt - a.updatedAt))

const floatStyle = computed(() => ({
  right: `${position.right}px`,
  bottom: `${position.bottom}px`
}))

function getPanelBounds() {
  if (!open.value) {
    return { width: 132, height: 56 }
  }
  const desiredWidth = expanded.value ? 720 : 480
  const desiredHeight = expanded.value ? 720 : 640
  return {
    width: Math.min(desiredWidth, Math.max(0, window.innerWidth - 24)),
    height: Math.min(desiredHeight, Math.max(0, window.innerHeight - 24))
  }
}

function constrainPositionToViewport() {
  const bounds = getPanelBounds()
  const maxRight = Math.max(12, window.innerWidth - bounds.width - 12)
  const maxBottom = Math.max(12, window.innerHeight - bounds.height - 12)
  position.right = clamp(position.right, 12, maxRight)
  position.bottom = clamp(position.bottom, 12, maxBottom)
  savePosition()
}

function createDefaultMessages(content = defaultMessages[0].content) {
  return [{ role: 'assistant', content }]
}

function createSession(options = {}) {
  const sessionMessages = sanitizeMessages(options.messages || createDefaultMessages())
  return {
    id: options.id || `session-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    title: options.title || deriveSessionTitle(sessionMessages),
    conversationId: options.conversationId || '',
    messages: sessionMessages,
    updatedAt: Number.isFinite(options.updatedAt) ? options.updatedAt : Date.now()
  }
}

function loadSessionState() {
  let loadedSessions = []
  try {
    const saved = JSON.parse(localStorage.getItem(SESSION_LIST_KEY) || '[]')
    if (Array.isArray(saved)) {
      loadedSessions = saved
        .map((session) => createSession({
          id: typeof session?.id === 'string' ? session.id : undefined,
          title: typeof session?.title === 'string' ? session.title : '',
          conversationId: typeof session?.conversationId === 'string' ? session.conversationId : '',
          messages: session?.messages,
          updatedAt: Number(session?.updatedAt)
        }))
        .filter((session) => session.messages.length > 0)
    }
  } catch (error) {
    loadedSessions = []
  }

  if (loadedSessions.length === 0) {
    const legacyMessages = loadLegacyMessages()
    const legacyConversationId = localStorage.getItem(LEGACY_CONVERSATION_KEY) || ''
    loadedSessions = [
      createSession({
        title: deriveSessionTitle(legacyMessages),
        conversationId: legacyConversationId,
        messages: legacyMessages
      })
    ]
    localStorage.removeItem(LEGACY_MESSAGES_KEY)
    localStorage.removeItem(LEGACY_CONVERSATION_KEY)
  }

  loadedSessions = normalizeSessions(loadedSessions)
  const savedCurrentId = localStorage.getItem(CURRENT_SESSION_KEY)
  const currentSession = loadedSessions.find((session) => session.id === savedCurrentId) || loadedSessions[0]
  persistSessions(loadedSessions, currentSession.id)

  return {
    sessions: loadedSessions,
    currentSessionId: currentSession.id,
    conversationId: currentSession.conversationId,
    messages: sanitizeMessages(currentSession.messages)
  }
}

function loadLegacyMessages() {
  try {
    return sanitizeMessages(JSON.parse(localStorage.getItem(LEGACY_MESSAGES_KEY) || '[]'))
  } catch (error) {
    // Ignore invalid local cache and fall back to a clean greeting.
  }
  return createDefaultMessages()
}

function sanitizeMessages(value) {
  if (!Array.isArray(value)) {
    return createDefaultMessages()
  }
  const sanitized = value
    .filter((message) => message && ['assistant', 'user'].includes(message.role) && typeof message.content === 'string')
    .map((message) => ({
      role: message.role,
      content: message.content,
      status: message.status === 'error' ? 'error' : undefined,
      retryContent: typeof message.retryContent === 'string' ? message.retryContent : undefined
    }))
    .slice(-MAX_MESSAGES_PER_SESSION)

  return sanitized.length > 0 ? sanitized : createDefaultMessages()
}

function deriveSessionTitle(sessionMessages) {
  const firstUserMessage = sanitizeMessages(sessionMessages).find((message) => message.role === 'user')
  const title = (firstUserMessage?.content || '新会话').replace(/\s+/g, ' ').trim()
  return title.length > 22 ? `${title.slice(0, 22)}...` : title
}

function normalizeSessions(value) {
  const map = new Map()
  value.forEach((session) => {
    const normalized = createSession(session)
    map.set(normalized.id, {
      ...normalized,
      title: normalized.title || deriveSessionTitle(normalized.messages)
    })
  })
  return [...map.values()]
    .sort((a, b) => b.updatedAt - a.updatedAt)
    .slice(0, MAX_SESSION_COUNT)
}

function persistSessions(nextSessions = sessions.value, nextCurrentId = currentSessionId.value) {
  const normalized = normalizeSessions(nextSessions)
  localStorage.setItem(SESSION_LIST_KEY, JSON.stringify(normalized))
  localStorage.setItem(CURRENT_SESSION_KEY, nextCurrentId)
}

function saveAssistantState() {
  let currentSession = sessions.value.find((session) => session.id === currentSessionId.value)
  if (!currentSession) {
    currentSession = createSession()
    sessions.value.unshift(currentSession)
    currentSessionId.value = currentSession.id
  }

  currentSession.conversationId = conversationId.value
  currentSession.messages = sanitizeMessages(messages.value)
  currentSession.title = deriveSessionTitle(currentSession.messages)
  currentSession.updatedAt = Date.now()
  sessions.value = normalizeSessions(sessions.value)
  persistSessions()
}

function loadPosition() {
  try {
    const saved = JSON.parse(localStorage.getItem('civil-assistant-position') || '{}')
    return {
      right: Number.isFinite(saved.right) ? saved.right : 28,
      bottom: Number.isFinite(saved.bottom) ? saved.bottom : 92
    }
  } catch (error) {
    return { right: 28, bottom: 92 }
  }
}

function savePosition() {
  localStorage.setItem('civil-assistant-position', JSON.stringify({
    right: position.right,
    bottom: position.bottom
  }))
}

function getPoint(event) {
  const source = event.touches?.[0] || event.changedTouches?.[0] || event
  return { x: source.clientX, y: source.clientY }
}

function startDrag(event) {
  const point = getPoint(event)
  drag.active = true
  drag.moved = false
  drag.startX = point.x
  drag.startY = point.y
  drag.startRight = position.right
  drag.startBottom = position.bottom
  window.addEventListener('mousemove', moveDrag)
  window.addEventListener('mouseup', stopDrag)
  window.addEventListener('touchmove', moveDrag, { passive: false })
  window.addEventListener('touchend', stopDrag)
}

function moveDrag(event) {
  if (!drag.active) {
    return
  }
  event.preventDefault?.()
  const point = getPoint(event)
  const deltaX = point.x - drag.startX
  const deltaY = point.y - drag.startY
  if (Math.abs(deltaX) > 3 || Math.abs(deltaY) > 3) {
    drag.moved = true
  }
  const bounds = getPanelBounds()
  const maxRight = Math.max(12, window.innerWidth - bounds.width - 12)
  const maxBottom = Math.max(12, window.innerHeight - bounds.height - 12)
  position.right = clamp(drag.startRight - deltaX, 12, maxRight)
  position.bottom = clamp(drag.startBottom - deltaY, 12, maxBottom)
}

function stopDrag() {
  if (!drag.active) {
    return
  }
  drag.active = false
  savePosition()
  window.removeEventListener('mousemove', moveDrag)
  window.removeEventListener('mouseup', stopDrag)
  window.removeEventListener('touchmove', moveDrag)
  window.removeEventListener('touchend', stopDrag)
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max)
}

function togglePanel() {
  if (drag.moved) {
    drag.moved = false
    return
  }
  if (!ensureLoggedIn()) {
    return
  }
  open.value = !open.value
  nextTick(() => {
    constrainPositionToViewport()
    scrollToBottom()
  })
}

function toggleExpanded() {
  expanded.value = !expanded.value
  nextTick(() => {
    constrainPositionToViewport()
    scrollToBottom()
  })
}

function toggleSessions() {
  showSessions.value = !showSessions.value
}

function ensureLoggedIn() {
  if (authStore.isLoggedIn) {
    return true
  }
  open.value = false
  showSessions.value = false
  ElMessage.warning('请先登录后使用公考小助手')
  router.push('/login')
  return false
}

async function createNewSession() {
  if (!ensureLoggedIn()) {
    return
  }
  saveAssistantState()
  const session = createSession({
    messages: createDefaultMessages('已开启新会话。你可以问我备考规划、题型方法或课程学习建议。')
  })
  sessions.value.unshift(session)
  sessions.value = normalizeSessions(sessions.value)
  currentSessionId.value = session.id
  conversationId.value = ''
  messages.value = sanitizeMessages(session.messages)
  input.value = ''
  showSessions.value = false
  persistSessions()
  await nextTick(scrollToBottom)
}

async function openSession(sessionId) {
  if (sessionId !== currentSessionId.value) {
    saveAssistantState()
  }
  const target = sessions.value.find((session) => session.id === sessionId)
  if (!target) {
    return
  }
  target.updatedAt = Date.now()
  currentSessionId.value = target.id
  conversationId.value = target.conversationId || ''
  messages.value = sanitizeMessages(target.messages)
  input.value = ''
  showSessions.value = false
  sessions.value = normalizeSessions(sessions.value)
  persistSessions()
  await nextTick(scrollToBottom)
}

async function deleteSession(sessionId) {
  const target = sessions.value.find((session) => session.id === sessionId)
  if (!target) {
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除“${target.title || '新会话'}”？`, '删除会话', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (error) {
    return
  }

  const wasCurrent = sessionId === currentSessionId.value
  sessions.value = sessions.value.filter((session) => session.id !== sessionId)

  if (sessions.value.length === 0) {
    const session = createSession()
    sessions.value = [session]
    currentSessionId.value = session.id
    conversationId.value = session.conversationId
    messages.value = sanitizeMessages(session.messages)
  } else if (wasCurrent) {
    const nextSession = sortedSessions.value[0]
    currentSessionId.value = nextSession.id
    conversationId.value = nextSession.conversationId || ''
    messages.value = sanitizeMessages(nextSession.messages)
  }

  sessions.value = normalizeSessions(sessions.value)
  persistSessions()
  ElMessage.success('会话已删除')
  await nextTick(scrollToBottom)
}

function getSessionPreview(session) {
  const firstUserMessage = sanitizeMessages(session.messages).find((message) => message.role === 'user')
  const preview = (firstUserMessage?.content || session.messages?.[0]?.content || '').replace(/\s+/g, ' ').trim()
  return preview.length > 32 ? `${preview.slice(0, 32)}...` : preview
}

function formatSessionTime(timestamp) {
  const date = new Date(Number(timestamp) || Date.now())
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (isToday) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  if (date.toDateString() === yesterday.toDateString()) {
    return '昨天'
  }
  return `${date.getMonth() + 1}-${String(date.getDate()).padStart(2, '0')}`
}

async function sendMessage(contentOverride = '', options = {}) {
  if (!ensureLoggedIn()) {
    return
  }
  const hasTextOverride = typeof contentOverride === 'string' && contentOverride.trim()
  const content = (hasTextOverride ? contentOverride : input.value).trim()
  if (!content || loading.value) {
    return
  }
  if (options.appendUser !== false) {
    messages.value.push({ role: 'user', content })
  }
  if (!hasTextOverride) {
    input.value = ''
  }
  saveAssistantState()
  loading.value = true
  await nextTick(scrollToBottom)
  try {
    const data = await request.post('/api/assistant/chat', {
      message: content,
      conversationId: conversationId.value
    }, { timeout: 130000 })
    if (data.conversationId) {
      conversationId.value = data.conversationId
    }
    messages.value.push({ role: 'assistant', content: data.answer || '小助手没有返回内容，请换个问法再试。' })
    saveAssistantState()
  } catch (error) {
    const message = buildErrorMessage(error)
    messages.value.push({
      role: 'assistant',
      status: 'error',
      content: message.content,
      retryContent: content
    })
    saveAssistantState()
    ElMessage.error(message.toast)
  } finally {
    loading.value = false
    await nextTick(scrollToBottom)
  }
}

function buildErrorMessage(error) {
  const rawMessage = error?.message || ''
  if (rawMessage.includes('timeout') || rawMessage.includes('超时')) {
    return {
      content: '小助手响应超时，可能是回答内容较长。你可以重新发送，或把问题拆得更具体。',
      toast: 'AI 响应超时'
    }
  }
  if (rawMessage.includes('Network') || rawMessage.includes('ECONNABORTED') || rawMessage.includes('Failed')) {
    return {
      content: '网络连接不稳定，暂时没有收到小助手回复。',
      toast: '网络连接失败'
    }
  }
  if (rawMessage.includes('配置')) {
    return {
      content: '小助手服务配置异常，请联系管理员检查 Coze 配置。',
      toast: '服务配置异常'
    }
  }
  if (rawMessage.includes('没有返回内容') || rawMessage.includes('为空')) {
    return {
      content: '小助手没有返回内容，请换个问法再试。',
      toast: '回复内容为空'
    }
  }
  return {
    content: '小助手暂时不可用，请稍后再试。',
    toast: rawMessage || '发送失败'
  }
}

async function retryMessage(index) {
  if (!ensureLoggedIn()) {
    return
  }
  const message = messages.value[index]
  if (!message?.retryContent || loading.value) {
    return
  }
  messages.value.splice(index, 1)
  saveAssistantState()
  await sendMessage(message.retryContent, { appendUser: false })
}

async function copyMessage(content) {
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(content)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = content
      textarea.setAttribute('readonly', '')
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    ElMessage.success('已复制')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

function renderMarkdown(content) {
  const lines = String(content || '').split(/\r?\n/)
  const html = []
  let listType = ''

  const closeList = () => {
    if (listType) {
      html.push(`</${listType}>`)
      listType = ''
    }
  }

  lines.forEach((line) => {
    const text = line.trim()
    if (!text) {
      closeList()
      return
    }

    const heading = text.match(/^(#{1,4})\s+(.+)$/)
    if (heading) {
      closeList()
      const level = Math.min(heading[1].length + 2, 5)
      html.push(`<h${level}>${renderInline(heading[2])}</h${level}>`)
      return
    }

    const unordered = text.match(/^[-*]\s+(.+)$/)
    if (unordered) {
      if (listType !== 'ul') {
        closeList()
        html.push('<ul>')
        listType = 'ul'
      }
      html.push(`<li>${renderInline(unordered[1])}</li>`)
      return
    }

    const ordered = text.match(/^\d+[.、]\s+(.+)$/)
    if (ordered) {
      if (listType !== 'ol') {
        closeList()
        html.push('<ol>')
        listType = 'ol'
      }
      html.push(`<li>${renderInline(ordered[1])}</li>`)
      return
    }

    closeList()
    html.push(`<p>${renderInline(text)}</p>`)
  })

  closeList()
  return html.join('')
}

function renderInline(content) {
  return escapeHtml(content)
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
}

function escapeHtml(content) {
  return String(content)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

function scrollToBottom() {
  const el = messagesRef.value
  if (el) {
    el.scrollTop = el.scrollHeight
  }
}

onBeforeUnmount(() => {
  stopDrag()
  saveAssistantState()
})
</script>

<template>
  <div class="assistant-float" :class="{ open, expanded }" :style="floatStyle">
    <section v-if="open" class="assistant-panel">
      <header class="assistant-head" @mousedown="startDrag" @touchstart="startDrag">
        <div class="assistant-avatar">
          <el-icon><ChatDotRound /></el-icon>
        </div>
        <div class="assistant-identity">
          <div class="assistant-title">公考小助手</div>
          <div class="assistant-subtitle">AI 智能体在线答疑</div>
        </div>
        <div class="assistant-actions">
          <el-tooltip content="最近会话" placement="top">
            <button type="button" class="icon-button" :class="{ active: showSessions }" @click.stop="toggleSessions">
              <el-icon><List /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip :content="expanded ? '恢复窗口' : '展开阅读'" placement="top">
            <button type="button" class="icon-button" @click.stop="toggleExpanded">
              <el-icon>
                <ScaleToOriginal v-if="expanded" />
                <FullScreen v-else />
              </el-icon>
            </button>
          </el-tooltip>
          <el-tooltip content="新会话" placement="top">
            <button type="button" class="icon-button" @click.stop="createNewSession">
              <el-icon><Plus /></el-icon>
            </button>
          </el-tooltip>
          <el-tooltip content="关闭" placement="top">
            <button type="button" class="icon-button" @click.stop="open = false">
              <el-icon><Close /></el-icon>
            </button>
          </el-tooltip>
        </div>
      </header>

      <div v-if="showSessions" class="session-panel" @mousedown.stop @touchstart.stop>
        <div class="session-panel-head">
          <span>最近会话</span>
          <button type="button" class="session-new-button" aria-label="新建会话" @click="createNewSession">
            <el-icon><Plus /></el-icon>
          </button>
        </div>
        <div class="session-list">
          <div
            v-for="session in sortedSessions"
            :key="session.id"
            class="session-item"
            :class="{ active: session.id === currentSessionId }"
            role="button"
            tabindex="0"
            @click="openSession(session.id)"
            @keydown.enter.prevent="openSession(session.id)"
          >
            <div class="session-main">
              <span class="session-title">{{ session.title }}</span>
              <span class="session-preview">{{ getSessionPreview(session) }}</span>
            </div>
            <div class="session-side">
              <span class="session-time">{{ formatSessionTime(session.updatedAt) }}</span>
              <button type="button" class="session-delete" aria-label="删除会话" @click.stop="deleteSession(session.id)">
                <el-icon><Delete /></el-icon>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div ref="messagesRef" class="assistant-messages">
        <div
          v-for="(message, index) in messages"
          :key="index"
          class="message-row"
          :class="message.role"
        >
          <div class="message-stack">
            <div
              class="message-bubble"
              :class="{ error: message.status === 'error' }"
            >
              <div
                v-if="message.role === 'assistant'"
                class="markdown-content"
                v-html="renderMarkdown(message.content)"
              />
              <template v-else>{{ message.content }}</template>
            </div>
            <div v-if="message.role === 'assistant'" class="message-tools">
              <el-tooltip content="复制回答" placement="top">
                <button type="button" class="tool-button" @click="copyMessage(message.content)">
                  <el-icon><CopyDocument /></el-icon>
                </button>
              </el-tooltip>
              <el-tooltip v-if="message.status === 'error'" content="重新发送" placement="top">
                <button type="button" class="tool-button retry" @click="retryMessage(index)">
                  <el-icon><RefreshRight /></el-icon>
                </button>
              </el-tooltip>
            </div>
          </div>
        </div>

        <div v-if="loading" class="message-row assistant">
          <div class="message-bubble loading-text">正在思考...</div>
        </div>

        <div v-if="messages.length <= 1 && !loading" class="quick-prompts">
          <button
            v-for="prompt in quickPrompts"
            :key="prompt"
            type="button"
            @click="sendMessage(prompt)"
          >
            {{ prompt }}
          </button>
        </div>
      </div>

      <footer class="assistant-input">
        <el-input
          v-model="input"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 5 }"
          maxlength="1000"
          resize="none"
          :disabled="loading"
          placeholder="问问备考计划、题型方法或学习路径"
          @keydown.enter.exact.prevent="sendMessage"
        />
        <el-button type="primary" :loading="loading" :disabled="!input.trim() || loading" @click="sendMessage">
          <el-icon><Promotion /></el-icon>
        </el-button>
      </footer>
    </section>

    <button
      v-else
      type="button"
      class="assistant-button"
      @mousedown="startDrag"
      @touchstart="startDrag"
      @click="togglePanel"
    >
      <span class="button-icon">
        <el-icon><ChatDotRound /></el-icon>
      </span>
      <span>公考小助手</span>
    </button>
  </div>
</template>

<style scoped>
.assistant-float {
  position: fixed;
  z-index: 80;
  width: 132px;
  max-width: calc(100vw - 24px);
}

.assistant-float.open {
  width: 480px;
}

.assistant-float.open.expanded {
  width: min(720px, calc(100vw - 24px));
}

.assistant-button {
  width: 132px;
  height: 56px;
  border: 0;
  border-radius: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  background: linear-gradient(135deg, #2563eb, #0f766e);
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.24);
  cursor: grab;
  font-weight: 700;
}

.assistant-button:active,
.assistant-head:active {
  cursor: grabbing;
}

.button-icon,
.assistant-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 auto;
}

.button-icon {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.18);
}

.assistant-panel {
  position: relative;
  width: 480px;
  max-width: calc(100vw - 24px);
  height: 640px;
  max-height: calc(100vh - 24px);
  overflow: hidden;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  border: 1px solid rgba(148, 163, 184, 0.34);
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
}

.assistant-float.expanded .assistant-panel {
  width: min(720px, calc(100vw - 24px));
  height: min(720px, calc(100vh - 24px));
}

.assistant-head {
  min-height: 60px;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  color: #fff;
  background: linear-gradient(135deg, #1d4ed8, #0f766e);
  cursor: grab;
}

.assistant-avatar {
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.18);
  font-size: 20px;
}

.assistant-identity {
  min-width: 0;
}

.assistant-title {
  font-size: 18px;
  font-weight: 800;
  line-height: 1.2;
}

.assistant-subtitle {
  margin-top: 2px;
  font-size: 12px;
  line-height: 1.25;
  opacity: 0.82;
}

.assistant-actions {
  display: inline-flex;
  gap: 6px;
}

.icon-button {
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: rgba(255, 255, 255, 0.14);
  cursor: pointer;
  transition: background 0.16s ease, transform 0.16s ease;
}

.icon-button:hover {
  background: rgba(255, 255, 255, 0.24);
}

.icon-button.active {
  background: rgba(255, 255, 255, 0.28);
}

.session-panel {
  position: absolute;
  top: 70px;
  right: 12px;
  z-index: 2;
  width: min(320px, calc(100% - 24px));
  max-height: min(430px, calc(100% - 92px));
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid #dbe3ef;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.18);
}

.session-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 11px 12px;
  border-bottom: 1px solid #e2e8f0;
  color: #0f172a;
  font-size: 14px;
  font-weight: 800;
}

.session-new-button,
.session-delete {
  border: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.session-new-button {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  color: #1d4ed8;
  background: #eff6ff;
}

.session-new-button:hover {
  background: #dbeafe;
}

.session-list {
  min-height: 0;
  overflow-y: auto;
  padding: 6px;
}

.session-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
  outline: none;
}

.session-item:hover,
.session-item:focus-visible {
  background: #f1f5f9;
}

.session-item.active {
  background: #eff6ff;
}

.session-main {
  min-width: 0;
  display: grid;
  gap: 4px;
}

.session-title,
.session-preview {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-title {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
}

.session-preview {
  color: #64748b;
  font-size: 12px;
  line-height: 1.35;
}

.session-side {
  display: grid;
  justify-items: end;
  gap: 6px;
}

.session-time {
  color: #94a3b8;
  font-size: 12px;
  line-height: 1;
}

.session-delete {
  width: 26px;
  height: 26px;
  border-radius: 7px;
  color: #94a3b8;
  background: transparent;
}

.session-delete:hover {
  color: #dc2626;
  background: #fee2e2;
}

.assistant-messages {
  min-height: 0;
  overflow-y: auto;
  padding: 18px;
  background: #f8fafc;
  scrollbar-color: rgba(100, 116, 139, 0.42) transparent;
  scrollbar-width: thin;
}

.assistant-messages::-webkit-scrollbar {
  width: 8px;
}

.assistant-messages::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(100, 116, 139, 0.36);
}

.assistant-messages::-webkit-scrollbar-track {
  background: transparent;
}

.message-row {
  display: flex;
  margin-bottom: 14px;
}

.message-row.user {
  justify-content: flex-end;
}

.message-stack {
  max-width: 92%;
  display: grid;
  gap: 6px;
}

.message-row.user .message-stack {
  max-width: 84%;
}

.message-bubble {
  padding: 12px 14px;
  border-radius: 12px;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
  font-size: 14px;
}

.message-row.assistant .message-bubble {
  color: #0f172a;
  background: #fff;
  border: 1px solid #e2e8f0;
}

.message-row.assistant .message-bubble.error {
  color: #7f1d1d;
  background: #fff7f7;
  border-color: #fecaca;
}

.message-row.user .message-bubble {
  color: #fff;
  background: #2563eb;
}

.message-tools {
  display: inline-flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.16s ease;
}

.message-row:hover .message-tools,
.message-tools:focus-within {
  opacity: 1;
}

.tool-button {
  width: 26px;
  height: 26px;
  border: 1px solid #e2e8f0;
  border-radius: 7px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  background: rgba(255, 255, 255, 0.9);
  cursor: pointer;
}

.tool-button:hover {
  color: #2563eb;
  border-color: #bfdbfe;
}

.tool-button.retry {
  color: #b45309;
}

.loading-text {
  color: #64748b;
}

.quick-prompts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 2px;
}

.quick-prompts button {
  border: 1px solid #dbeafe;
  border-radius: 999px;
  padding: 7px 11px;
  color: #1d4ed8;
  background: #eff6ff;
  font-size: 13px;
  line-height: 1.2;
  cursor: pointer;
}

.quick-prompts button:hover {
  border-color: #93c5fd;
  background: #dbeafe;
}

.assistant-input {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 48px;
  gap: 10px;
  align-items: end;
  padding: 12px 14px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
}

.assistant-input :deep(.el-textarea__inner) {
  min-height: 44px !important;
  padding: 10px 12px;
  line-height: 1.55;
  border-radius: 8px;
  font-size: 14px;
}

.assistant-input :deep(.el-button) {
  width: 48px;
  min-height: 44px;
  padding: 0;
  border-radius: 8px;
}

.markdown-content {
  white-space: normal;
}

.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5) {
  margin: 10px 0 6px;
  color: #0f172a;
  font-weight: 800;
  line-height: 1.35;
}

.markdown-content :deep(h3:first-child),
.markdown-content :deep(h4:first-child),
.markdown-content :deep(h5:first-child),
.markdown-content :deep(p:first-child) {
  margin-top: 0;
}

.markdown-content :deep(p) {
  margin: 8px 0;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 8px 0 10px;
  padding-left: 20px;
}

.markdown-content :deep(li) {
  margin: 5px 0;
  padding-left: 2px;
}

.markdown-content :deep(strong) {
  font-weight: 800;
}

.markdown-content :deep(code) {
  padding: 1px 5px;
  border-radius: 5px;
  color: #0f172a;
  background: #f1f5f9;
  font-family: ui-monospace, SFMono-Regular, Consolas, 'Liberation Mono', monospace;
  font-size: 0.92em;
}

@media (max-width: 560px) {
  .assistant-float.open,
  .assistant-float.open.expanded {
    right: 12px !important;
    bottom: 12px !important;
    width: calc(100vw - 24px);
  }

  .assistant-panel,
  .assistant-float.expanded .assistant-panel {
    width: calc(100vw - 24px);
    height: min(680px, calc(100vh - 24px));
  }

  .assistant-head {
    gap: 10px;
  }

  .assistant-title {
    font-size: 17px;
  }

  .assistant-actions {
    gap: 4px;
  }

  .session-panel {
    left: 12px;
    right: 12px;
    width: auto;
    max-height: min(390px, calc(100% - 88px));
  }

  .assistant-messages {
    padding: 14px;
  }

  .message-stack,
  .message-row.user .message-stack {
    max-width: 94%;
  }
}
</style>
