export const FILE_UPLOAD_URL = '/api/files/upload'
export const FILE_VIEW_PREFIX = '/api/files/view/'

export function resolveFileUploadUrl(bizType) {
  return bizType ? `${FILE_UPLOAD_URL}?bizType=${encodeURIComponent(bizType)}` : FILE_UPLOAD_URL
}

export function resolveAssetUrl(path) {
  if (!path) {
    return ''
  }
  if (/^https?:\/\//i.test(path)) {
    return path
  }
  return path.startsWith('/') ? path : `/${path}`
}

export function resolveFileViewUrl(fileId) {
  return fileId ? `${FILE_VIEW_PREFIX}${fileId}` : ''
}

const classroomCoverMap = {
  xingce: '/classroom-assets/covers/classroom-xingce.webp',
  shenlun: '/classroom-assets/covers/classroom-shenlun.webp',
  mianshi: '/classroom-assets/covers/classroom-mianshi.webp',
  default: '/classroom-assets/covers/classroom-xingce.webp'
}

const classroomCoverMetaMap = {
  xingce: {
    subject: '行测',
    title: '行测突破',
    chips: ['直播陪跑', '刷题打卡', '限时训练'],
    accent: '#3b82f6'
  },
  shenlun: {
    subject: '申论',
    title: '申论精讲',
    chips: ['写作批改', '素材拆解', '框架训练'],
    accent: '#f59e0b'
  },
  mianshi: {
    subject: '面试',
    title: '面试实战',
    chips: ['模拟答辩', '即时点评', '表达训练'],
    accent: '#14b8a6'
  },
  default: {
    subject: '班级',
    title: '专项提升',
    chips: ['直播辅导', '任务跟练', '阶段反馈'],
    accent: '#3b82f6'
  }
}

function classroomCoverKey(subjectName = '', name = '') {
  const text = `${subjectName} ${name}`
  if (text.includes('申论')) {
    return 'shenlun'
  }
  if (text.includes('面试')) {
    return 'mianshi'
  }
  if (text.includes('行测')) {
    return 'xingce'
  }
  return 'default'
}

export function resolveClassroomCoverUrl(path, subjectName = '', name = '') {
  const key = classroomCoverKey(subjectName, name)
  if (!path || path.includes('dummyimage.com')) {
    return classroomCoverMap[key] || classroomCoverMap.default
  }
  return resolveAssetUrl(path)
}

export function classroomCoverMeta(subjectName = '', name = '') {
  const key = classroomCoverKey(subjectName, name)
  return classroomCoverMetaMap[key] || classroomCoverMetaMap.default
}
