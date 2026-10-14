export function firstArray(...values) {
  let fallback = null
  for (const value of values) {
    if (Array.isArray(value) && value.length) return value
    if (Array.isArray(value) && !fallback) fallback = value
  }
  return fallback || []
}

export function firstPageRecords(value) {
  if (Array.isArray(value)) return value
  return firstArray(value?.records, value?.page?.records, value?.data?.records)
}

export function numberText(value, fallback = 0) {
  const number = Number(value ?? fallback)
  return Number.isFinite(number) ? number : fallback
}

export function percentText(value) {
  return Math.max(0, Math.min(100, Math.round(Number(value || 0))))
}

export function formatDateTime(value, fallback = '暂无') {
  if (!value) return fallback
  return String(value).replace('T', ' ').slice(0, 16)
}

export function roleHomePath(roles = []) {
  if (roles.includes('ADMIN')) return '/m/admin'
  if (roles.includes('TEACHER')) return '/m/teacher'
  return '/m/student'
}

export function roleName(roles = []) {
  if (roles.includes('ADMIN')) return '管理员'
  if (roles.includes('TEACHER')) return '教师'
  return '学员'
}

export function mobilePcPath(path, roles = []) {
  if (!path) return '/'
  if (path.startsWith('/m/home')) return '/'
  if (path.startsWith('/m/login')) return '/login'
  if (path === '/m/student/profile') {
    if (roles.includes('ADMIN')) return '/admin/setting'
    if (roles.includes('TEACHER')) return '/teacher/setting'
    return '/user/profile'
  }
  if (path.startsWith('/m/student/courses')) return '/my-course'
  if (path.startsWith('/m/student/tasks')) return '/user/task'
  if (path.startsWith('/m/student/classrooms')) return '/my-classroom'
  if (path.startsWith('/m/student')) return '/user/profile'
  if (path.startsWith('/m/teacher/classes')) return '/teacher/class'
  if (path.startsWith('/m/teacher/reviews')) return '/teacher/review'
  if (path.startsWith('/m/teacher')) return '/teacher/dashboard'
  if (path.startsWith('/m/admin/audits')) return '/admin/teacher/audit'
  if (path.startsWith('/m/admin/refunds')) return '/admin/refund'
  if (path.startsWith('/m/admin/growth')) return '/admin/growth'
  if (path.startsWith('/m/admin')) return '/admin/dashboard'
  return path
}

export function pcMobilePath(path, roles = []) {
  if (!path) return roleHomePath(roles)
  if (path.startsWith('/admin/teacher/audit')) return '/m/admin/audits'
  if (path.startsWith('/admin/refund')) return '/m/admin/refunds'
  if (path.startsWith('/admin/growth')) return '/m/admin/growth'
  if (path.startsWith('/admin')) return '/m/admin'
  if (path.startsWith('/teacher')) return '/m/teacher'
  if (path.startsWith('/user/profile') || path.startsWith('/user/setting') || path.startsWith('/admin/setting') || path.startsWith('/teacher/setting')) {
    return '/m/student/profile'
  }
  if (path.startsWith('/user/task')) return '/m/student/tasks'
  if (path.startsWith('/user') || path.startsWith('/my-course')) return '/m/student'
  return roles.length ? roleHomePath(roles) : '/m/home'
}

export function shouldShowMobileEntry() {
  if (typeof window === 'undefined') return false
  const switchedFromMobile = localStorage.getItem('civil-mobile-switch') === '1'
  const narrowTouch = window.matchMedia?.('(max-width: 768px) and (pointer: coarse)').matches
  const mobileUa = /Android|iPhone|iPad|iPod|Mobile|Windows Phone/i.test(navigator.userAgent || '')
  return switchedFromMobile || Boolean(narrowTouch || mobileUa)
}
