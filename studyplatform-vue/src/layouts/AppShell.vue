<script setup>
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AiPetWidget from '../components/AiPetWidget.vue'
import AppNavigation from '../components/AppNavigation.vue'

const route = useRoute()

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
      {
        label: '题库',
        path: '/academy/question-bank',
      },
    ],
  },
  {
    label: '实验平台',
    path: '/lab',
    children: [
      {
        label: '在线编程平台',
        path: '/lab/oj',
      },
      {
        label: '石油气仿真',
        path: '/lab/petroleum',
      },
    ],
  },
  {
    label: '可视化',
    path: '/visualization',
    children: [
      {
        label: '算法结构可视化',
        path: '/visualization/data-structure',
      },
      {
        label: '函数图像实验室',
        path: '/visualization/function-2d',
      },
      {
        label: '空间模型实验室',
        path: '/visualization/space-models',
      },
    ],
  },
  {
    label: '游戏',
    path: '/games',
    children: [
      {
        label: 'type warrior',
        path: '/games/type-warrior',
      },
      {
        label: '\u4e07\u9898\u5929\u68af\u8df3',
        path: '/games/ladder-jump',
      },
    ],
  },
]

const shellClass = computed(() => ({
  'home-page': route.path === '/',
  'profile-shell': route.path.startsWith('/profile'),
  'academy-shell': route.path.startsWith('/academy'),
  'academy-course-detail-shell': route.path.startsWith('/academy/open-courses/'),
  'app-page lab-shell': route.path === '/lab',
  'app-page oj-page-shell': route.path === '/lab/oj',
  'app-page production-shell': route.path === '/lab/petroleum',
  'app-page well-log-shell': route.path === '/lab/well-log',
  'app-page visual-shell': route.path.startsWith('/visualization'),
  'visual-home-shell': route.path === '/visualization',
  'app-page games-shell': route.path.startsWith('/games'),
  'games-home-shell': route.path === '/games',
  'immersive-game-shell': route.path.startsWith('/games/'),
}))

const showNavigation = computed(() => !route.path.startsWith('/games/'))
</script>

<template>
  <div :class="shellClass">
    <AppNavigation v-if="showNavigation" :nav-items="navItems" />
    <RouterView />
    <AiPetWidget />
  </div>
</template>
