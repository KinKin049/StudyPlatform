import { createRouter, createWebHistory } from 'vue-router'

const AcademyPage = () => import('../pages/AcademyPage.vue')
const AcademyGeneralCourses = () => import('../pages/academy/AcademyGeneralCourses.vue')
const AcademyHome = () => import('../pages/academy/AcademyHome.vue')
const AcademyMicroMajors = () => import('../pages/academy/AcademyMicroMajors.vue')
const AcademyOpenCourses = () => import('../pages/academy/AcademyOpenCourses.vue')
const AcademyCourseDetail = () => import('../pages/academy/AcademyCourseDetail.vue')
const AcademyQuestionBank = () => import('../pages/academy/AcademyQuestionBank.vue')
const AcademyQuestionBankCourseDetail = () => import('../pages/academy/AcademyQuestionBankCourseDetail.vue')
const AcademyQuestionBankCourses = () => import('../pages/academy/AcademyQuestionBankCourses.vue')
const AcademyQuestionBankFavorites = () => import('../pages/academy/AcademyQuestionBankFavorites.vue')
const AcademyQuestionBankMistakes = () => import('../pages/academy/AcademyQuestionBankMistakes.vue')
const AcademyTextbooks = () => import('../pages/academy/AcademyTextbooks.vue')
const HomePage = () => import('../pages/HomePage.vue')
const LabPlatform = () => import('../pages/LabPlatform.vue')
const OjPlatform = () => import('../pages/OjPlatform.vue')
const PetroleumSimulation = () => import('../pages/petroleum/PetroleumSimulation.vue')
const ProfilePage = () => import('../pages/ProfilePage.vue')
const WellLogSimulation = () => import('../pages/WellLogSimulation.vue')
const VisualizationHome = () => import('../pages/visualization/VisualizationHome.vue')
const DataStructureVisualization = () => import('../pages/visualization/DataStructureVisualization.vue')
const AlgorithmDemoViewer = () => import('../pages/visualization/AlgorithmDemoViewer.vue')
const FunctionGraph2D = () => import('../pages/visualization/FunctionGraph2D.vue')
const SpaceModelGuide = () => import('../pages/visualization/SpaceModelGuide.vue')
const SpaceModel3D = () => import('../pages/visualization/SpaceModel3D.vue')
const GamePlatform = () => import('../pages/games/GamePlatform.vue')

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomePage,
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

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
