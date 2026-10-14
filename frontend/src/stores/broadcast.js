import { defineStore } from 'pinia'
import request from '../utils/request'

let socket = null
let reconnectTimer = null
let intentionalDisconnect = false

const SEEN_STORAGE_KEY = 'civil_seen_broadcast_ids'
const MAX_SEEN_COUNT = 100

function resolveBroadcastKey(payload = {}) {
  if (payload.id !== undefined && payload.id !== null && payload.id !== '') {
    return `id:${payload.id}`
  }
  return `fallback:${payload.title || ''}|${payload.content || ''}|${payload.sentAt || ''}`
}

function readSeenKeys() {
  try {
    const raw = window.localStorage.getItem(SEEN_STORAGE_KEY)
    const parsed = raw ? JSON.parse(raw) : []
    return Array.isArray(parsed) ? parsed : []
  } catch (error) {
    return []
  }
}

function writeSeenKeys(keys) {
  try {
    window.localStorage.setItem(SEEN_STORAGE_KEY, JSON.stringify(keys.slice(0, MAX_SEEN_COUNT)))
  } catch (error) {
    // localStorage can be unavailable in private or restricted browser modes.
  }
}

function hasSeenBroadcast(payload) {
  return readSeenKeys().includes(resolveBroadcastKey(payload))
}

function rememberBroadcast(payload) {
  const key = resolveBroadcastKey(payload)
  const keys = readSeenKeys()
  writeSeenKeys([key, ...keys.filter((item) => item !== key)])
  return key
}

function mergeBroadcastHistory(existing, payload) {
  const key = resolveBroadcastKey(payload)
  return [payload, ...existing.filter((item) => resolveBroadcastKey(item) !== key)].slice(0, 10)
}

function normalizeBroadcastHistory(items) {
  const seen = new Set()
  const normalized = []
  for (const item of items) {
    const key = resolveBroadcastKey(item)
    if (seen.has(key)) continue
    seen.add(key)
    normalized.push(item)
    if (normalized.length >= 10) break
  }
  return normalized
}

function buildWebSocketUrl() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  return `${protocol}//${window.location.hostname}:8080/ws/broadcast`
}

export const useBroadcastStore = defineStore('broadcast', {
  state: () => ({
    connected: false,
    latest: null,
    latestKey: null,
    history: []
  }),
  actions: {
    async loadRecent() {
      const data = await request.get('/api/broadcasts/recent')
      const recent = Array.isArray(data) ? normalizeBroadcastHistory(data) : []
      this.history = recent
      if (recent[0]) {
        this.ingestBroadcast(recent[0], { notify: true })
      }
    },
    getBroadcastKey(payload) {
      return resolveBroadcastKey(payload)
    },
    markSeen(payload) {
      if (!payload) return null
      return rememberBroadcast(payload)
    },
    ingestBroadcast(payload, options = {}) {
      if (!payload) {
        return { key: null, seen: true }
      }
      const key = resolveBroadcastKey(payload)
      const seen = hasSeenBroadcast(payload)
      this.history = mergeBroadcastHistory(this.history, payload)
      if (seen) {
        return { key, seen: true }
      }
      rememberBroadcast(payload)
      this.latest = payload
      this.latestKey = key
      if (options.notify) {
        import('element-plus/es/components/notification/index').then(({ ElNotification }) => {
          ElNotification({
            title: payload.title || '全局广播',
            message: payload.content || '',
            type: 'info',
            duration: 6000,
            position: 'bottom-right'
          })
        })
      }
      return { key, seen: false }
    },
    connect(options = {}) {
      if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
        return
      }
      if (reconnectTimer) {
        window.clearTimeout(reconnectTimer)
        reconnectTimer = null
      }
      intentionalDisconnect = false
      if (!options.skipRecent) {
        this.loadRecent().catch(() => {})
      }
      socket = new WebSocket(buildWebSocketUrl())
      socket.onopen = () => {
        this.connected = true
      }
      socket.onmessage = (event) => {
        let payload
        try {
          payload = JSON.parse(event.data)
        } catch (error) {
          payload = { title: '全局广播', content: event.data }
        }
        this.ingestBroadcast(payload, { notify: true })
      }
      socket.onclose = () => {
        this.connected = false
        socket = null
        if (intentionalDisconnect) {
          intentionalDisconnect = false
          return
        }
        if (!reconnectTimer) {
          reconnectTimer = window.setTimeout(() => {
            reconnectTimer = null
            this.connect({ skipRecent: true })
          }, 5000)
        }
      }
      socket.onerror = () => {
        this.connected = false
      }
    },
    disconnect() {
      if (reconnectTimer) {
        window.clearTimeout(reconnectTimer)
        reconnectTimer = null
      }
      intentionalDisconnect = true
      if (socket) {
        socket.close()
        socket = null
      } else {
        intentionalDisconnect = false
      }
      this.connected = false
    }
  }
})
