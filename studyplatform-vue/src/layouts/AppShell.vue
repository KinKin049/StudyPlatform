<script setup>
// 应用根布局组件，提供全局导航、路由视图和 AI 宠物组件的容器
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import AiPetWidget from '../components/AiPetWidget.vue'
import AppNavigation from '../components/AppNavigation.vue'

const route = useRoute()

// 导航菜单项配置，包含在线学堂、实验平台、可视化和游戏四大模块
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
        label: '万题天梯跳',
        path: '/games/ladder-jump',
      },
    ],
  },
]

// 根据当前路由动态计算布局容器的 CSS 类名，用于不同页面的样式差异化
const shellClass = computed(() => ({
  'home-page': route.path === '/',
  'auth-shell': route.path === '/login' || route.path === '/register' || route.path === '/forgot-password' || route.path === '/onboarding',
  'profile-shell': route.path.startsWith('/profile'),
  'exchange-shell': route.path.startsWith('/exchange'),
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

// 判断是否显示顶部导航栏，游戏详情页和认证相关页面不显示导航
const showNavigation = computed(() =>
  !route.path.startsWith('/games/') &&
    route.path !== '/login' &&
    route.path !== '/register' &&
    route.path !== '/forgot-password' &&
    route.path !== '/onboarding',
)
</script>

<template>
  <!-- 应用根布局容器，根据路由动态添加样式类 -->
  <div :class="shellClass">
    <!-- 顶部导航栏组件，条件渲染 -->
    <AppNavigation v-if="showNavigation" :nav-items="navItems" />
    <!-- 路由视图容器，渲染当前路由对应的页面组件 -->
    <RouterView />
    <!-- AI 学习宠物组件，全局显示 -->
    <AiPetWidget />
  </div>
</template>
