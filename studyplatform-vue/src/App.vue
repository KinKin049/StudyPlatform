<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import onlineOpenCourseData from './data/onlineOpenCourses.json'

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

const academyNavItems = [
  {
    label: '首页',
    path: '/academy/home',
    children: [],
    dropdown: false,
  },
  {
    label: '在线开放课程',
    path: '/academy/open-courses',
    children: [],
    dropdown: true,
  },
  {
    label: '通识课程',
    path: '/academy/general-courses',
    children: [],
    dropdown: true,
  },
  {
    label: '微专业课程',
    path: '/academy/micro-majors',
    children: [],
    dropdown: true,
  },
  {
    label: '精品教材',
    path: '/academy/textbooks',
    children: [],
    dropdown: true,
  },
]

const featuredCourses = [
  {
    title: '人工智能导论',
    category: '在线开放课程',
    meta: '32 学时 · 8 个章节',
  },
  {
    title: '大学生创新实践',
    category: '通识课程',
    meta: '24 学时 · 项目制学习',
  },
  {
    title: '数据分析微专业',
    category: '微专业课程',
    meta: '6 门课 · 能力认证',
  },
]

const currentPath = ref(window.location.pathname)
const selectedOnlineCourseCategory = ref('全部')
const onlineCourseKeyword = ref('')

const isAcademyPage = computed(() => currentPath.value.startsWith('/academy'))
const isOnlineOpenCoursePage = computed(() => currentPath.value === '/academy/open-courses')
const onlineOpenCourseCategories = computed(() => [
  '全部',
  ...onlineOpenCourseData.categories.map((category) => category.name),
])
const filteredOnlineOpenCourses = computed(() => {
  const keyword = onlineCourseKeyword.value.trim().toLowerCase()

  return onlineOpenCourseData.courses.filter((course) => {
    const matchesCategory =
      selectedOnlineCourseCategory.value === '全部' ||
      course.category === selectedOnlineCourseCategory.value
    const matchesKeyword =
      !keyword ||
      [course.name, course.teacher, course.school, course.category].some((value) =>
        String(value || '')
          .toLowerCase()
          .includes(keyword),
      )

    return matchesCategory && matchesKeyword
  })
})

const navigateTo = (path) => {
  // 预留路由跳转入口：接入 vue-router 后可替换为 router.push(item.path)
  window.history.pushState({}, '', path)
  currentPath.value = path
}

const handleNavigate = (item) => {
  navigateTo(item.path)
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
  <div v-if="!isAcademyPage" class="home-page">
    <header class="site-header">
      <a class="site-brand" href="/" aria-label="返回首页" @click.prevent="navigateTo('/')">
        EpistemeHub
      </a>

      <nav class="site-nav" aria-label="主导航">
        <div v-for="item in navItems" :key="item.path" class="nav-item">
          <button class="nav-button" type="button" @click="handleNavigate(item)">
            <span>{{ item.label }}</span>
            <span class="nav-arrow" aria-hidden="true">▾</span>
          </button>

          <div class="dropdown-menu" role="menu">
            <a
              v-for="child in item.children"
              :key="child.path"
              class="dropdown-link"
              :href="child.path"
              role="menuitem"
              @click.prevent="navigateTo(child.path)"
            >
              {{ child.label }}
            </a>
            <span v-if="item.children.length === 0" class="dropdown-empty">菜单预留</span>
          </div>
        </div>
      </nav>

      <button class="user-entry" type="button" aria-label="用户中心">
        <span class="user-avatar">U</span>
      </button>
    </header>

    <main class="home-main">
      <section class="intro-panel" aria-labelledby="home-title">
        <p class="intro-kicker">Study Platform</p>
        <h1 id="home-title">学习平台入口</h1>
        <p class="intro-copy">
          汇聚课程学习、实验训练、数据可视化与互动游戏模块，为后续子页面接入保留统一入口。
        </p>
      </section>
    </main>
  </div>

  <div v-else class="academy-page">
    <header class="academy-header">
      <a class="academy-brand" href="/academy/home" @click.prevent="navigateTo('/academy/home')">
        在线学堂
      </a>

      <button class="user-entry academy-user-entry" type="button" aria-label="用户中心">
        <span class="user-avatar">U</span>
      </button>
    </header>

    <nav class="academy-subnav" aria-label="在线学堂导航">
      <div v-for="item in academyNavItems" :key="item.path" class="academy-nav-item">
        <button class="academy-nav-button" type="button" @click="navigateTo(item.path)">
          <span>{{ item.label }}</span>
          <span v-if="item.dropdown" class="nav-arrow" aria-hidden="true">▾</span>
        </button>

        <div v-if="item.dropdown" class="academy-dropdown-menu" role="menu">
          <a
            v-for="child in item.children"
            :key="child.path"
            class="dropdown-link"
            :href="child.path"
            role="menuitem"
            @click.prevent="navigateTo(child.path)"
          >
            {{ child.label }}
          </a>
          <span v-if="item.children.length === 0" class="dropdown-empty">菜单预留</span>
        </div>
      </div>
    </nav>

    <main v-if="!isOnlineOpenCoursePage" class="academy-main">
      <section class="academy-hero" aria-labelledby="academy-title">
        <div class="academy-hero-copy">
          <p class="academy-kicker">EpistemeHub Academy</p>
          <h1 id="academy-title">在线学堂</h1>
          <p>
            面向学生的课程学习入口，聚合在线开放课程、通识课程、微专业课程与精品教材资源。
          </p>
        </div>

        <div class="academy-search" role="search">
          <input type="search" placeholder="搜索课程、教材或专题" aria-label="搜索课程、教材或专题" />
          <button type="button">搜索</button>
        </div>
      </section>

      <section class="academy-content" aria-label="学堂内容">
        <div class="academy-section-heading">
          <h2>推荐课程</h2>
          <button type="button">查看更多</button>
        </div>

        <div class="course-grid">
          <article v-for="course in featuredCourses" :key="course.title" class="course-card">
            <p>{{ course.category }}</p>
            <h3>{{ course.title }}</h3>
            <span>{{ course.meta }}</span>
          </article>
        </div>
      </section>
    </main>

    <main v-else class="online-course-main">
      <section class="online-course-hero" aria-labelledby="online-course-title">
        <div>
          <p class="academy-kicker">Online Open Courses</p>
          <h1 id="online-course-title">在线开放课程</h1>
          <p>
            数据来源于爱课程公开课程列表，按课程类别整理展示，后续可接入后端接口与本地文件存储。
          </p>
        </div>

        <div class="online-course-search online-course-hero-search" role="search">
          <input
            v-model="onlineCourseKeyword"
            type="search"
            placeholder="搜索课程、讲师、学校或类别"
            aria-label="搜索课程、讲师、学校或类别"
          />
        </div>
      </section>

      <section class="online-course-tools" aria-label="课程筛选">
        <div class="online-category-list">
          <button
            v-for="category in onlineOpenCourseCategories"
            :key="category"
            type="button"
            :class="{ active: selectedOnlineCourseCategory === category }"
            @click="selectedOnlineCourseCategory = category"
          >
            {{ category }}
          </button>
        </div>
      </section>

      <section class="online-course-board" aria-label="在线开放课程列表">
        <div class="online-course-summary">
          <h2>{{ selectedOnlineCourseCategory }}课程</h2>
          <span>共 {{ filteredOnlineOpenCourses.length }} 门</span>
        </div>

        <div class="online-course-grid">
          <article v-for="course in filteredOnlineOpenCourses" :key="course.id" class="online-course-card">
            <a :href="course.link" target="_blank" rel="noreferrer">
              <img :src="course.cover" :alt="course.name" loading="lazy" />
              <div class="online-course-card-body">
                <div class="online-course-card-meta">
                  <span>{{ course.category }}</span>
                  <strong>{{ course.participants }} 人参加</strong>
                </div>
                <h3>{{ course.name }}</h3>
                <dl>
                  <div>
                    <dt>讲师</dt>
                    <dd>{{ course.teacher || '暂未提供' }}</dd>
                  </div>
                  <div>
                    <dt>开课时间</dt>
                    <dd>{{ course.startTime || '待定' }}</dd>
                  </div>
                </dl>
                <p>{{ course.school }}</p>
              </div>
            </a>
          </article>
        </div>
      </section>
    </main>
  </div>
</template>
