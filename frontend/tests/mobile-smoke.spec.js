import { expect, test } from '@playwright/test'

const baseUrl = process.env.MOBILE_BASE_URL || 'http://127.0.0.1:3000'
const apiUrl = process.env.MOBILE_API_URL || 'http://127.0.0.1:8080'

const accounts = {
  student: { username: 'student1', password: '123456' },
  teacher: { username: 'teacher1', password: '123456' },
  admin: { username: 'admin', password: '123456' }
}

const widths = [375, 390, 414]
const routes = [
  ['public', '/m/home'],
  ['public', '/m/login'],
  ['student', '/m/student'],
  ['student', '/m/student/courses'],
  ['student', '/m/student/tasks'],
  ['student', '/m/student/classrooms'],
  ['student', '/m/student/profile'],
  ['teacher', '/m/teacher'],
  ['teacher', '/m/teacher/classes'],
  ['teacher', '/m/teacher/reviews'],
  ['teacher', '/m/student/profile'],
  ['admin', '/m/admin'],
  ['admin', '/m/admin/audits'],
  ['admin', '/m/admin/refunds'],
  ['admin', '/m/admin/growth'],
  ['admin', '/m/student/profile'],
  ['public', '/'],
  ['public', '/course'],
  ['public', '/search'],
  ['public', '/news'],
  ['public', '/recommend'],
  ['public', '/classroom'],
  ['student', '/my-classroom'],
  ['student', '/user/assessment'],
  ['student', '/user/test-paper-report'],
  ['teacher', '/teacher/dashboard'],
  ['teacher', '/teacher/homework'],
  ['teacher', '/teacher/review'],
  ['admin', '/admin/growth'],
  ['admin', '/admin/question']
]

async function login(request, role) {
  if (role === 'public') {
    return ''
  }
  const response = await request.post(`${apiUrl}/api/auth/login`, {
    data: accounts[role]
  })
  expect(response.ok(), `login ${role}`).toBeTruthy()
  const payload = await response.json()
  return payload?.data?.token || ''
}

async function measure(page) {
  return page.evaluate(() => {
    const doc = document.documentElement
    const body = document.body
    const scrollWidth = Math.max(doc.scrollWidth, body.scrollWidth)
    const viewportWidth = window.innerWidth
    const visibleText = body.innerText.trim()
    const overlayText = visibleText.includes('Internal server error') || visibleText.includes('[plugin:vite]')
    const tinyActions = Array.from(document.querySelectorAll('button, a, [role="button"]'))
      .map((element) => ({
        element,
        text: (element.innerText || element.textContent || '').replace(/\s+/g, ' ').trim()
      }))
      .filter(({ element, text }) => {
        const rect = element.getBoundingClientRect()
        const style = getComputedStyle(element)
        return text && rect.width > 0 && rect.height > 0 && style.visibility !== 'hidden' && style.display !== 'none'
      })
      .filter(({ element }) => {
        const rect = element.getBoundingClientRect()
        return rect.height < 32
      })
      .slice(0, 5)
      .map(({ element, text }) => ({
        text,
        width: Math.round(element.getBoundingClientRect().width),
        height: Math.round(element.getBoundingClientRect().height)
      }))
    return {
      viewportWidth,
      scrollWidth,
      textLength: visibleText.length,
      overlayText,
      tinyActions
    }
  })
}

test.describe('mobile web smoke', () => {
  let tokens = {}

  test.beforeAll(async ({ request }) => {
    tokens = {
      student: await login(request, 'student'),
      teacher: await login(request, 'teacher'),
      admin: await login(request, 'admin')
    }
  })

  for (const width of widths) {
    for (const [role, path] of routes) {
      test(`${width}px ${role} ${path}`, async ({ browser }) => {
        const context = await browser.newContext({
          viewport: { width, height: 844 },
          isMobile: true,
          hasTouch: true
        })
        if (tokens[role]) {
          await context.addInitScript((token) => {
            window.localStorage.setItem('civil-token', token)
          }, tokens[role])
        }
        const page = await context.newPage()
        const errors = []
        page.on('console', (message) => {
          if (message.type() === 'error') {
            errors.push(message.text())
          }
        })
        page.on('pageerror', (error) => errors.push(error.message))

        await page.goto(`${baseUrl}${path}`, { waitUntil: 'domcontentloaded' })
        await page.waitForLoadState('networkidle', { timeout: 10000 }).catch(() => {})
        const result = await measure(page)

        expect(result.textLength, `${path} should render content`).toBeGreaterThan(20)
        expect(result.overlayText, `${path} should not show framework overlay`).toBe(false)
        expect(result.scrollWidth, `${path} should not create page-level horizontal overflow`).toBeLessThanOrEqual(result.viewportWidth + 2)
        expect(errors, `${path} console errors`).toEqual([])
        expect(result.tinyActions, `${path} touch targets below 32px`).toEqual([])

        await context.close()
      })
    }
  }
})
