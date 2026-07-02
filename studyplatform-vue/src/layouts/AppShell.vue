<script setup>
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
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
    children: [],
  },
  {
    label: '游戏',
    path: '/games',
    children: [],
  },
]

const shellClass = computed(() => ({
  'home-page': route.path === '/',
  'academy-shell': route.path.startsWith('/academy'),
  'academy-course-detail-shell': route.path.startsWith('/academy/open-courses/'),
  'app-page lab-shell': route.path === '/lab',
  'app-page oj-page-shell': route.path === '/lab/oj',
  'app-page production-shell': route.path === '/lab/petroleum',
  'app-page well-log-shell': route.path === '/lab/well-log',
  'app-page visual-shell': route.path.startsWith('/visualization'),
}))
</script>

<template>
  <div :class="shellClass">
    <AppNavigation :nav-items="navItems" />
    <RouterView />
  </div>
</template>
