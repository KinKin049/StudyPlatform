import { createRouter, createWebHistory } from 'vue-router'

const AcademyPage = () => import('../pages/AcademyPage.vue')
const AcademyGeneralCourses = () => import('../pages/academy/AcademyGeneralCourses.vue')
const AcademyHome = () => import('../pages/academy/AcademyHome.vue')
const AcademyMicroMajors = () => import('../pages/academy/AcademyMicroMajors.vue')
const AcademyOpenCourses = () => import('../pages/academy/AcademyOpenCourses.vue')
const AcademyTextbooks = () => import('../pages/academy/AcademyTextbooks.vue')
const HomePage = () => import('../pages/HomePage.vue')
const LabPlatform = () => import('../pages/LabPlatform.vue')
const OjPlatform = () => import('../pages/OjPlatform.vue')
const WellLogSimulation = () => import('../pages/WellLogSimulation.vue')

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
        path: 'general-courses',
        name: 'academy-general-courses',
        component: AcademyGeneralCourses,
      },
      {
        path: 'micro-majors',
        name: 'academy-micro-majors',
        component: AcademyMicroMajors,
      },
      {
        path: 'textbooks',
        name: 'academy-textbooks',
        component: AcademyTextbooks,
      },
    ],
  },
  {
    path: '/lab',
    name: 'lab',
    component: LabPlatform,
  },
  {
    path: '/lab/oj',
    name: 'lab-oj',
    component: OjPlatform,
  },
  {
    path: '/lab/well-log',
    name: 'well-log',
    component: WellLogSimulation,
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
