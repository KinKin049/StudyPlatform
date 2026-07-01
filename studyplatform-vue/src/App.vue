<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import AppNavigation from './components/AppNavigation.vue'
import AcademyPage from './pages/AcademyPage.vue'
import HomePage from './pages/HomePage.vue'
import LabPlatform from './pages/LabPlatform.vue'

const navItems = [
  {
    label: '在线学堂',
    path: '/academy/home',
    children: [
      {
        label: '学堂首页',
        path: '/academy/home',
      },
      {
        label: '在线开放课程',
        path: '/academy/open-courses',
      },
      {
        label: '通识课程',
        path: '/academy/general-courses',
      },
      {
        label: '微专业课程',
        path: '/academy/micro-majors',
      },
      {
        label: '精品教材',
        path: '/academy/textbooks',
      },
    ],
  },
  {
    label: '实验平台',
    path: '/lab',
    children: [],
  },
  {
    label: '可视化',
    path: '/visualization',
    children: [],
  },
  {
    label: '游戏',
    path: '/games',
    children: [],
  },
]

const currentPath = ref(window.location.pathname)

const isAcademyPage = computed(() => currentPath.value.startsWith('/academy'))
const isLabPage = computed(() => currentPath.value === '/lab')

const navigateTo = (path) => {
  // 预留路由跳转入口：接入 vue-router 后可替换为 router.push(path)
  window.history.pushState({}, '', path)
  currentPath.value = path
}

const handlePopState = () => {
  currentPath.value = window.location.pathname
}

onMounted(() => {
  window.addEventListener('popstate', handlePopState)
})

onUnmounted(() => {
  window.removeEventListener('popstate', handlePopState)
})
</script>

<template>
  <div v-if="isAcademyPage" class="academy-shell">
    <AppNavigation :nav-items="navItems" @navigate="navigateTo" />
    <AcademyPage :current-path="currentPath" @navigate="navigateTo" />
  </div>

  <div v-else-if="isLabPage" class="app-page lab-shell">
    <AppNavigation :nav-items="navItems" @navigate="navigateTo" />
    <LabPlatform />
  </div>

  <HomePage v-else :nav-items="navItems" @navigate="navigateTo" />
</template>
