import { createRouter, createWebHistory } from 'vue-router'
import PublicLayout from '../layouts/PublicLayout.vue'
import AdminLayout from '../layouts/AdminLayout.vue'
import UserCenterLayout from '../layouts/UserCenterLayout.vue'
import MobileLayout from '../layouts/MobileLayout.vue'
import pinia from '../stores'
import { useAuthStore } from '../stores/auth'
import { roleHomePath } from '../mobile/helpers'

const TEACHER_AUDIT_HINT = {
  PENDING: '您的教师资格申请正在审核中，审核通过后才能进入教师端',
  REJECTED: '您的教师资格申请未通过，请重新提交申请后再进入教师端',
  NONE: '您尚未提交教师资格申请，请先完成申请并通过审核'
}

const USER_CENTER_PRIVILEGED_ALLOWED_PATHS = new Set([
  '/user/order'
])

const MOBILE_PUBLIC_PATHS = new Set(['/m/home', '/m/login'])

function canPrivilegedRoleEnterUserCenter(path) {
  return USER_CENTER_PRIVILEGED_ALLOWED_PATHS.has(path)
}

function isLikelyMobileDevice() {
  if (typeof window === 'undefined') return false
  const ua = navigator.userAgent || ''
  return /Android|iPhone|iPad|iPod|Mobile|Windows Phone/i.test(ua) ||
    (window.matchMedia?.('(max-width: 768px) and (pointer: coarse)').matches ?? false)
}

const routes = [
  {
    path: '/m',
    component: MobileLayout,
    children: [
      { path: '', redirect: '/m/home' },
      { path: 'home', component: () => import('../pages/mobile/MobileHomePage.vue'), meta: { mobileTitle: '移动首页' } },
      { path: 'login', component: () => import('../pages/mobile/MobileLoginPage.vue'), meta: { mobileTitle: '移动登录', guestOnly: true, hideMobileTabs: true } },
      { path: 'student', component: () => import('../pages/mobile/MobileStudentDashboard.vue'), meta: { requiresAuth: true, mobileTitle: '学习工作台' } },
      { path: 'student/courses', component: () => import('../pages/mobile/MobileStudentCourses.vue'), meta: { requiresAuth: true, mobileTitle: '我的课程' } },
      { path: 'student/tasks', component: () => import('../pages/mobile/MobileStudentTasks.vue'), meta: { requiresAuth: true, mobileTitle: '每日任务' } },
      { path: 'student/classrooms', component: () => import('../pages/mobile/MobileStudentClassrooms.vue'), meta: { requiresAuth: true, mobileTitle: '我的班级' } },
      { path: 'student/profile', component: () => import('../pages/mobile/MobileStudentProfile.vue'), meta: { requiresAuth: true, mobileTitle: '个人中心' } },
      { path: 'teacher', component: () => import('../pages/mobile/MobileTeacherDashboard.vue'), meta: { requiresAuth: true, role: 'TEACHER', mobileTitle: '教师工作台' } },
      { path: 'teacher/classes', component: () => import('../pages/mobile/MobileTeacherClasses.vue'), meta: { requiresAuth: true, role: 'TEACHER', mobileTitle: '班级列表' } },
      { path: 'teacher/reviews', component: () => import('../pages/mobile/MobileTeacherReviews.vue'), meta: { requiresAuth: true, role: 'TEACHER', mobileTitle: '待批改' } },
      { path: 'admin', component: () => import('../pages/mobile/MobileAdminDashboard.vue'), meta: { requiresAuth: true, role: 'ADMIN', mobileTitle: '运营看板' } },
      { path: 'admin/audits', component: () => import('../pages/mobile/MobileAdminAudits.vue'), meta: { requiresAuth: true, role: 'ADMIN', mobileTitle: '教师审核' } },
      { path: 'admin/refunds', component: () => import('../pages/mobile/MobileAdminRefunds.vue'), meta: { requiresAuth: true, role: 'ADMIN', mobileTitle: '退款审核' } },
      { path: 'admin/growth', component: () => import('../pages/mobile/MobileAdminGrowth.vue'), meta: { requiresAuth: true, role: 'ADMIN', mobileTitle: '成长预警' } }
    ]
  },
  {
    path: '/',
    component: PublicLayout,
    children: [
      { path: '', component: () => import('../pages/HomePage.vue') },
      { path: 'login', component: () => import('../pages/LoginPage.vue'), meta: { guestOnly: true } },
      { path: 'register', component: () => import('../pages/RegisterPage.vue'), meta: { guestOnly: true } },
      { path: 'course', component: () => import('../pages/CourseListPage.vue') },
      { path: 'course/detail/:id', component: () => import('../pages/CoursePurchaseDetailPage.vue') },
      { path: 'question-bank', component: () => import('../pages/QuestionBankPage.vue') },
      { path: 'search', component: () => import('../pages/SearchPage.vue') },
      { path: 'practice/:id', component: () => import('../pages/PracticePage.vue'), meta: { requiresAuth: true } },
      { path: 'practice/report/:id', component: () => import('../pages/PracticeReportPage.vue'), meta: { requiresAuth: true } },
      { path: 'wrong-book', component: () => import('../pages/WrongBookPage.vue'), meta: { requiresAuth: true } },
      { path: 'exam', component: () => import('../pages/ExamListPage.vue') },
      { path: 'exam/detail/:id', alias: '/practice/exam_detail/:id', component: () => import('../pages/ExamDetailPage.vue') },
      { path: 'exam/start/:id', component: () => import('../pages/ExamStartPage.vue'), meta: { requiresAuth: true } },
      { path: 'exam/report/:id', component: () => import('../pages/ExamReportPage.vue'), meta: { requiresAuth: true } },
      { path: 'exam/rank/:id', component: () => import('../pages/ExamRankPage.vue') },
      { path: 'news', component: () => import('../pages/NewsListPage.vue') },
      { path: 'news/detail/:id', component: () => import('../pages/NewsDetailPage.vue') },
      { path: 'notice', component: () => import('../pages/NoticeListPage.vue') },
      { path: 'notice/detail/:id', component: () => import('../pages/NoticeDetailPage.vue') },
      { path: 'recommend', component: () => import('../pages/RecommendPage.vue') },
      { path: 'recommend/profile', component: () => import('../pages/RecommendProfilePage.vue'), meta: { requiresAuth: true } },
      { path: 'classroom', component: () => import('../pages/ClassroomListPage.vue') },
      { path: 'classroom/detail/:id', component: () => import('../pages/ClassroomDetailPage.vue') },
      { path: 'my-classroom', component: () => import('../pages/MyClassroomPage.vue'), meta: { requiresAuth: true } },
      { path: 'classroom/homework/start/:id', component: () => import('../pages/ClassroomHomeworkStartPage.vue'), meta: { requiresAuth: true } },
      { path: 'classroom/homework/report/:id', component: () => import('../pages/ClassroomHomeworkReportPage.vue'), meta: { requiresAuth: true } },
      { path: 'classroom/exam/start/:id', component: () => import('../pages/ClassroomExamStartPage.vue'), meta: { requiresAuth: true } },
      { path: 'classroom/exam/report/:id', component: () => import('../pages/ClassroomExamReportPage.vue'), meta: { requiresAuth: true } },
      { path: 'cart', component: () => import('../pages/CartPage.vue'), meta: { requiresAuth: true } },
      { path: 'my-course', component: () => import('../pages/MyCourseEnhancedPage.vue'), meta: { requiresAuth: true } },
      { path: 'course/play/:id', component: () => import('../pages/CourseLearnEnhancedPage.vue'), meta: { requiresAuth: true } },
      { path: 'teacher/apply', component: () => import('../pages/TeacherApplyPage.vue'), meta: { requiresAuth: true } },
      { path: 'teacher/public/:id', component: () => import('../pages/TeacherPublicDetailPage.vue') },
      { path: 'teacher/dashboard', component: () => import('../pages/TeacherDashboardPage.vue'), meta: { requiresAuth: true, role: 'TEACHER' } },
      { path: 'teacher/class', component: () => import('../pages/TeacherClassPage.vue'), meta: { requiresAuth: true, role: 'TEACHER', teacherBack: true, teacherBackWidth: 'default' } },
      { path: 'teacher/class/create', component: () => import('../pages/TeacherClassCreatePage.vue'), meta: { requiresAuth: true, role: 'TEACHER', teacherBack: true, teacherBackWidth: 'create' } },
      { path: 'teacher/homework', component: () => import('../pages/TeacherHomeworkPage.vue'), meta: { requiresAuth: true, role: 'TEACHER', teacherBack: true, teacherBackWidth: 'default' } },
      { path: 'teacher/exam', component: () => import('../pages/TeacherExamManagePage.vue'), meta: { requiresAuth: true, role: 'TEACHER', teacherBack: true, teacherBackWidth: 'default' } },
      { path: 'teacher/review', component: () => import('../pages/TeacherReviewPage.vue'), meta: { requiresAuth: true, role: 'TEACHER', teacherBack: true, teacherBackWidth: 'default' } },
      { path: 'teacher/report', component: () => import('../pages/TeacherReportPage.vue'), meta: { requiresAuth: true, role: 'TEACHER', teacherBack: true, teacherBackWidth: 'wide' } },
      { path: 'teacher/profile', component: () => import('../pages/TeacherProfilePage.vue'), meta: { requiresAuth: true, role: 'TEACHER', teacherBack: true, teacherBackWidth: 'compact' } },
      { path: 'teacher/setting', component: () => import('../pages/UserSettingPage.vue'), meta: { requiresAuth: true, role: 'TEACHER', teacherBack: true, teacherBackWidth: 'compact' } },
      {
        path: 'user',
        component: UserCenterLayout,
        meta: { requiresAuth: true },
        children: [
          { path: '', redirect: '/user/profile' },
          { path: 'profile', component: () => import('../pages/UserProfilePage.vue') },
          { path: 'plan', component: () => import('../pages/UserPlanPage.vue') },
          { path: 'archive', component: () => import('../pages/UserGrowthArchivePage.vue') },
          { path: 'diagnosis', component: () => import('../pages/UserDiagnosisPage.vue') },
          { path: 'assessment', component: () => import('../pages/UserAssessmentPage.vue') },
          { path: 'task', component: () => import('../pages/UserTaskPage.vue') },
          { path: 'knowledge', component: () => import('../pages/UserKnowledgePage.vue') },
          { path: 'test-paper', component: () => import('../pages/UserTestPaperPage.vue') },
          { path: 'test-paper-report', component: () => import('../pages/UserTestPaperReportCenterPage.vue') },
          { path: 'order', component: () => import('../pages/UserOrderPage.vue') },
          { path: 'stat', component: () => import('../pages/UserStatPage.vue') },
          { path: 'vip', component: () => import('../pages/UserVipPage.vue') },
          { path: 'export', component: () => import('../pages/UserExportPage.vue') },
          { path: 'setting', component: () => import('../pages/UserSettingPage.vue') }
        ]
      }
    ]
  },
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, role: 'ADMIN' },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', component: () => import('../pages/AdminDashboardPage.vue') },
      { path: 'growth', component: () => import('../pages/AdminGrowthPage.vue') },
      { path: 'setting', component: () => import('../pages/UserSettingPage.vue') },
      { path: 'user', component: () => import('../pages/AdminUserPage.vue') },
      { path: 'roles', component: () => import('../pages/AdminRolePage.vue') },
      { path: 'teacher/audit', component: () => import('../pages/AdminTeacherAuditPage.vue') },
      { path: 'teacher', component: () => import('../pages/AdminTeacherPage.vue') },
      { path: 'course', component: () => import('../pages/AdminCoursePage.vue') },
      { path: 'question', component: () => import('../pages/AdminQuestionPage.vue') },
      { path: 'exam', component: () => import('../pages/AdminExamPage.vue') },
      { path: 'news', component: () => import('../pages/AdminNewsPage.vue') },
      { path: 'evaluation', component: () => import('../pages/AdminEvaluationPage.vue') },
      { path: 'notice', component: () => import('../pages/AdminNoticePage.vue') },
      { path: 'recommend', component: () => import('../pages/AdminRecommendPage.vue') },
      { path: 'order', component: () => import('../pages/AdminOrderPage.vue') },
      { path: 'refund', component: () => import('../pages/AdminRefundPage.vue') },
      { path: 'payment-log', component: () => import('../pages/AdminPaymentLogPage.vue') },
      { path: 'stats', component: () => import('../pages/AdminStatsPage.vue') },
      { path: 'system', component: () => import('../pages/AdminSystemPage.vue') },
      { path: 'log', component: () => import('../pages/AdminLogPage.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore(pinia)
  if (to.path === '/' && isLikelyMobileDevice()) {
    return '/m/home'
  }

  const isMobilePath = to.path.startsWith('/m')
  const needsUser = to.meta.requiresAuth || to.meta.role || to.meta.guestOnly || to.path.startsWith('/user')
  if (needsUser && authStore.token && !authStore.user) {
    await authStore.fetchMe()
  }

  if (to.meta.guestOnly && authStore.isLoggedIn) {
    if (isMobilePath) {
      return roleHomePath(authStore.roles)
    }
    return '/'
  }
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    if (isMobilePath) {
      return { path: '/m/login', query: { redirect: to.fullPath } }
    }
    return '/login'
  }
  if (isMobilePath && !MOBILE_PUBLIC_PATHS.has(to.path) && authStore.isLoggedIn && to.path.startsWith('/m/student') && to.path !== '/m/student/profile' && (authStore.roles.includes('ADMIN') || authStore.roles.includes('TEACHER'))) {
    return roleHomePath(authStore.roles)
  }
  if (to.path.startsWith('/user') && authStore.roles.includes('ADMIN') && !canPrivilegedRoleEnterUserCenter(to.path)) {
    return '/admin/dashboard'
  }
  if (to.path.startsWith('/user') && authStore.roles.includes('TEACHER') && !canPrivilegedRoleEnterUserCenter(to.path)) {
    return '/teacher/dashboard'
  }
  if (to.meta.role && !authStore.roles.includes(to.meta.role)) {
    if (isMobilePath) {
      return roleHomePath(authStore.roles)
    }
    return '/'
  }
  if (to.meta.role === 'TEACHER') {
    const status = authStore.user?.teacherAuditStatus || 'NONE'
    if (status !== 'APPROVED') {
      const { ElMessage } = await import('element-plus/es/components/message/index')
      ElMessage.warning(TEACHER_AUDIT_HINT[status] || TEACHER_AUDIT_HINT.NONE)
      if (isMobilePath) {
        return '/m/home'
      }
      return '/teacher/apply'
    }
  }
  return true
})

export default router
