export const QUESTION_TYPE_LABELS = {
  SINGLE: '单选题',
  MULTI: '多选题',
  JUDGE: '判断题',
  SHORT_ANSWER: '简答题'
}

export function getQuestionTypeLabel(type) {
  return QUESTION_TYPE_LABELS[type] || type || '-'
}

export function parseAnswerValue(value) {
  if (!value) {
    return []
  }
  return Array.from(new Set(String(value).split(',').map((item) => item.trim().toUpperCase()).filter(Boolean))).sort()
}

export function normalizeAnswerValue(value) {
  return parseAnswerValue(Array.isArray(value) ? value.join(',') : value).join(',')
}

export function formatAnswerDisplay(value) {
  const values = parseAnswerValue(value)
  return values.length ? values.join('、') : '未作答'
}
