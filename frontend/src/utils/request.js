import axios from 'axios'

const request = axios.create({
  baseURL: '',
  timeout: 15000
})

// 防抖：避免拦截器与页面 catch 同时弹出相同文案的双重 toast
const recentMessages = new Map()
async function showErrorToast(message) {
  if (!message) return
  const now = Date.now()
  const last = recentMessages.get(message) || 0
  if (now - last < 600) return
  recentMessages.set(message, now)
  if (typeof window !== 'undefined' && window.location.pathname.startsWith('/m')) {
    const { showFailToast } = await import('vant')
    showFailToast(message)
    setTimeout(() => recentMessages.delete(message), 1500)
    return
  }
  const { ElMessage } = await import('element-plus/es/components/message/index')
  ElMessage.error(message)
  setTimeout(() => recentMessages.delete(message), 1500)
}

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('civil-token')
  if (token && !config.anonymous) {
    config.headers.Authorization = `Bearer ${token}`
  }
  const examRegion = localStorage.getItem('civil-exam-region')
  if (examRegion) {
    config.headers['X-Exam-Region'] = encodeURIComponent(examRegion)
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const payload = response.data
    if (payload && payload.code !== 200) {
      const message = payload.message || '请求失败'
      if (!response.config?.silent) {
        showErrorToast(message)
      }
      return Promise.reject(new Error(message))
    }
    return payload.data
  },
  (error) => {
    const status = error?.response?.status
    let message = error?.response?.data?.message
    if (!message) {
      if (status === 401) message = '登录已失效，请重新登录'
      else if (status === 403) message = '无权访问该资源'
      else if (status === 413) message = '上传文件大小超出限制'
      else if (status === 404) message = '请求的资源不存在'
      else if (status >= 500) message = '服务器繁忙，请稍后重试'
      else if (error?.code === 'ECONNABORTED') message = '请求超时，请稍后重试'
      else message = error?.message || '网络异常，请检查后重试'
    }
    if (!error?.config?.silent) {
      showErrorToast(message)
    }
    if (status === 401 && typeof window !== 'undefined') {
      const path = window.location.pathname
      if (path !== '/login' && !path.startsWith('/register')) {
        localStorage.removeItem('civil-token')
      }
    }
    return Promise.reject(new Error(message))
  }
)

request.download = async (url, params = {}) => {
  const search = new URLSearchParams()
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.append(key, value)
    }
  })
  const token = localStorage.getItem('civil-token')
  const response = await fetch(search.toString() ? `${url}?${search.toString()}` : url, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  if (!response.ok) {
    throw new Error('下载失败')
  }
  const disposition = response.headers.get('content-disposition') || ''
  const matched = disposition.match(/filename\*=UTF-8''([^;]+)|filename=\"?([^\";]+)\"?/)
  const fileName = decodeURIComponent((matched && (matched[1] || matched[2])) || 'download.xlsx')
  const blob = await response.blob()
  const downloadUrl = window.URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = downloadUrl
  anchor.download = fileName
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)
  window.URL.revokeObjectURL(downloadUrl)
}

export default request
