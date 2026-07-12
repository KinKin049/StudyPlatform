/**
 * 路由配置文件
 * 定义应用的所有页面路由，包括路由路径、名称、组件映射及嵌套路由结构
 */
import { createRouter, createWebHistory } from 'vue-router'

// 首页
const AcademyPage = () => import('../pages/AcademyPage.vue')
// 学院聚合页
const AcademyAggregatePage = () => import('../pages/academy/AcademyAggregatePage.vue')
// 作业详情页
const AcademyAssignmentDetail = () => import('../pages/academy/AcademyAssignmentDetail.vue')
// 考试详情页
const AcademyExamDetail = () => import('../pages/academy/AcademyExamDetail.vue')
// 考试介绍页
const AcademyExamIntro = () => import('../pages/academy/AcademyExamIntro.vue')
// 通识课程页
const AcademyGeneralCourses = () => import('../pages/academy/AcademyGeneralCourses.vue')
// 学院首页
const AcademyHome = () => import('../pages/academy/AcademyHome.vue')
// 微专业课程页
const AcademyMicroMajors = () => import('../pages/academy/AcademyMicroMajors.vue')
// 我的课程页
const AcademyMyClass = () => import('../pages/academy/AcademyMyClass.vue')
// 开放课程页
const AcademyOpenCourses = () => import('../pages/academy/AcademyOpenCourses.vue')
// 课程详情页
const AcademyCourseDetail = () => import('../pages/academy/AcademyCourseDetail.vue')
// 题库首页
const AcademyQuestionBank = () => import('../pages/academy/AcademyQuestionBank.vue')
// 题库课程详情页
const AcademyQuestionBankCourseDetail = () => import('../pages/academy/AcademyQuestionBankCourseDetail.vue')
// 题库课程列表页
const AcademyQuestionBankCourses = () => import('../pages/academy/AcademyQuestionBankCourses.vue')
// 题库收藏页
const AcademyQuestionBankFavorites = () => import('../pages/academy/AcademyQuestionBankFavorites.vue')
// 题库错题页
const AcademyQuestionBankMistakes = () => import('../pages/academy/AcademyQuestionBankMistakes.vue')
// 教材首页
const AcademyTextbooks = () => import('../pages/academy/AcademyTextbooks.vue')
// 教材详情页
const AcademyTextbookDetail = () => import('../pages/academy/AcademyTextbookDetail.vue')
// 教材购物车页
const AcademyTextbookCart = () => import('../pages/academy/AcademyTextbookCart.vue')
// 管理后台页
const AdminPage = () => import('../pages/AdminPage.vue')
// 登录页
const AuthLoginPage = () => import('../pages/AuthLoginPage.vue')
// 引导页
const AuthOnboardingPage = () => import('../pages/AuthOnboardingPage.vue')
// 注册页
const AuthRegisterPage = () => import('../pages/AuthRegisterPage.vue')
// 忘记密码页
const AuthForgotPasswordPage = () => import('../pages/AuthForgotPasswordPage.vue')
// 兑换中心页
const ExchangeCenter = () => import('../pages/ExchangeCenter.vue')
// 我的卡券页
const MyVouchers = () => import('../pages/MyVouchers.vue')
// 首页
const HomePage = () => import('../pages/HomePage.vue')
// 实验平台页
const LabPlatform = () => import('../pages/LabPlatform.vue')
// 在线评测平台页
const OjPlatform = () => import('../pages/OjPlatform.vue')
// 石油模拟页
const PetroleumSimulation = () => import('../pages/petroleum/PetroleumSimulation.vue')
// 个人中心页
const ProfilePage = () => import('../pages/ProfilePage.vue')
// 教师信箱页
const TeacherMailboxPage = () => import('../pages/TeacherMailboxPage.vue')
// 测井模拟页
const WellLogSimulation = () => import('../pages/WellLogSimulation.vue')
// 可视化首页
const VisualizationHome = () => import('../pages/visualization/VisualizationHome.vue')
// 数据结构可视化页
const DataStructureVisualization = () => import('../pages/visualization/DataStructureVisualization.vue')
// 算法演示页
const AlgorithmDemoViewer = () => import('../pages/visualization/AlgorithmDemoViewer.vue')
// 二维函数图像页
const FunctionGraph2D = () => import('../pages/visualization/FunctionGraph2D.vue')
// 空间模型引导页
const SpaceModelGuide = () => import('../pages/visualization/SpaceModelGuide.vue')
// 三维空间模型页
const SpaceModel3D = () => import('../pages/visualization/SpaceModel3D.vue')
// 游戏平台页
const GamePlatform = () => import('../pages/games/GamePlatform.vue')

/**
 * 路由配置数组
 * 定义应用的所有路由规则，包含路径、名称、组件及嵌套路由
 */
const routes = [
  {
    path: '/',
    name: 'home',
    component: HomePage,
  },
  {
    path: '/login',
    name: 'login',
    component: AuthLoginPage,
  },
  {
    path: '/register',
    name: 'register',
    component: AuthRegisterPage,
  },
  {
    path: '/forgot-password',
    name: 'forgot-password',
    component: AuthForgotPasswordPage,
  },
  {
    path: '/onboarding',
    name: 'onboarding',
    component: AuthOnboardingPage,
    meta: { hidePet: true },
  },
  {
    path: '/admin',
    name: 'admin',
    component: AdminPage,
  },
  {
    path: '/exchange',
    name: 'exchange',
    component: ExchangeCenter,
    meta: { requiresAuth: true },
  },
  {
    path: '/exchange/vouchers',
    name: 'exchange-vouchers',
    component: MyVouchers,
    meta: { requiresAuth: true },
  },
  {
    path: '/academy',
    redirect: '/academy/home',
  },
  {
    path: '/academy',
    name: 'academy',
    component: AcademyPage,
    children: [
      {
        path: 'home',
        name: 'academy-home',
        component: AcademyHome,
      },
      {
        path: 'my-class',
        name: 'academy-my-class',
        component: AcademyMyClass,
      },
      {
        path: 'my-courses',
        name: 'academy-my-courses',
        component: AcademyAggregatePage,
        props: { variant: 'courses' },
      },
      {
        path: 'assignments',
        name: 'academy-assignments',
        component: AcademyAggregatePage,
        props: { variant: 'assignments' },
      },
      {
        path: 'assignments/:assignmentId',
        name: 'academy-assignment-detail',
        component: AcademyAssignmentDetail,
        meta: { hidePet: true },
        props: (route) => ({
          assignmentId: route.params.assignmentId,
        }),
      },
      {
        path: 'exams',
        name: 'academy-exams',
        component: AcademyAggregatePage,
        props: { variant: 'exams' },
      },
      {
        path: 'exams/:examId/take',
        name: 'academy-exam-take',
        component: AcademyExamDetail,
        meta: { hidePet: true },
        props: (route) => ({
          examId: route.params.examId,
        }),
      },
      {
        path: 'exams/:examId',
        name: 'academy-exam-detail',
        component: AcademyExamIntro,
        meta: { hidePet: true },
        props: (route) => ({
          examId: route.params.examId,
        }),
      },
      {
        path: 'open-courses',
        name: 'academy-open-courses',
        component: AcademyOpenCourses,
      },
      {
        path: 'open-courses/:id',
        name: 'academy-open-course-detail',
        component: AcademyCourseDetail,
        props: (route) => ({
          resource: 'online-open-courses',
          listPath: '/academy/open-courses',
          moduleTitle: '在线开放课程',
          courseId: route.params.id,
        }),
      },
      {
        path: 'general-courses',
        name: 'academy-general-courses',
        component: AcademyGeneralCourses,
      },
      {
        path: 'general-courses/:id',
        name: 'academy-general-course-detail',
        component: AcademyCourseDetail,
        props: (route) => ({
          resource: 'general-courses',
          listPath: '/academy/general-courses',
          moduleTitle: '通识课程',
          courseId: route.params.id,
        }),
      },
      {
        path: 'micro-majors',
        name: 'academy-micro-majors',
        component: AcademyMicroMajors,
      },
      {
        path: 'micro-majors/:id',
        name: 'academy-micro-major-detail',
        component: AcademyCourseDetail,
        props: (route) => ({
          resource: 'micro-major-courses',
          listPath: '/academy/micro-majors',
          moduleTitle: '微专业课程',
          courseId: route.params.id,
        }),
      },
      {
        path: 'textbooks',
        name: 'academy-textbooks',
        component: AcademyTextbooks,
      },
      {
        path: 'textbooks/:id',
        name: 'academy-textbook-detail',
        component: AcademyTextbookDetail,
        props: (route) => ({
          textbookId: route.params.id,
        }),
      },
      {
        path: 'textbook-cart',
        name: 'academy-textbook-cart',
        component: AcademyTextbookCart,
      },
      {
        path: 'question-bank',
        name: 'academy-question-bank',
        component: AcademyQuestionBank,
      },
      {
        path: 'question-bank/courses',
        name: 'academy-question-bank-courses',
        component: AcademyQuestionBankCourses,
      },
      {
        path: 'question-bank/mistakes',
        name: 'academy-question-bank-mistakes',
        component: AcademyQuestionBankMistakes,
      },
      {
        path: 'question-bank/favorites',
        name: 'academy-question-bank-favorites',
        component: AcademyQuestionBankFavorites,
      },
      {
        path: 'question-bank/courses/:courseCode',
        name: 'academy-question-bank-course-detail',
        component: AcademyQuestionBankCourseDetail,
      },
    ],
  },
  {
    path: '/lab',
    name: 'lab',
    component: LabPlatform,
  },
  {
    path: '/profile',
    name: 'profile',
    component: ProfilePage,
  },
  {
    path: '/teacher-mailbox',
    name: 'teacher-mailbox',
    component: TeacherMailboxPage,
  },
  {
    path: '/lab/oj',
    name: 'lab-oj',
    component: OjPlatform,
  },
  {
    path: '/lab/petroleum',
    name: 'lab-petroleum',
    component: PetroleumSimulation,
  },
  {
    path: '/lab/well-log',
    name: 'well-log',
    component: WellLogSimulation,
  },
  {
    path: '/lab/production',
    redirect: '/lab/petroleum',
  },
  {
    path: '/visualization',
    name: 'visualization',
    component: VisualizationHome,
  },
  {
    path: '/visualization/data-structure',
    name: 'visualization-data-structure',
    component: DataStructureVisualization,
  },
  {
    path: '/visualization/data-structure/:demoId',
    name: 'visualization-data-structure-demo',
    component: AlgorithmDemoViewer,
  },
  {
    path: '/visualization/function-2d',
    name: 'visualization-function-2d',
    component: FunctionGraph2D,
  },
  {
    path: '/visualization/space-models',
    name: 'visualization-space-models',
    component: SpaceModelGuide,
  },
  {
    path: '/visualization/space-3d',
    name: 'visualization-space-3d',
    component: SpaceModel3D,
  },
  {
    path: '/games',
    name: 'games',
    component: GamePlatform,
  },
  {
    path: '/games/:gameId',
    name: 'game-detail',
    component: GamePlatform,
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

/**
 * 创建路由实例
 * 使用 history 模式，加载定义好的路由配置
 */
const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  if (!to.matched.some((record) => record.meta?.requiresAuth)) return true
  try {
    const raw = localStorage.getItem('study-platform-auth-user')
    const user = raw ? JSON.parse(raw) : null
    if (user?.id && typeof user?.token === 'string' && user.token) return true
    localStorage.removeItem('study-platform-auth-user')
  } catch {
    // Fall through to login.
  }
  return {
    path: '/login',
    query: { redirect: to.fullPath },
  }
})

export default router
