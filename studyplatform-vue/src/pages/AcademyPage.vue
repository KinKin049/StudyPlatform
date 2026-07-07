<!-- 在线学堂主页面框架组件，负责渲染学堂二级导航栏和内容区域 -->
<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ArrowDown } from '@element-plus/icons-vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { fetchAcademyCategories } from '../api/academy'

const route = useRoute()
const router = useRouter()

// 课程分类映射，存储各资源类型的分类列表
const categoryMap = ref({
  'online-open-courses': [],
  'general-courses': [],
  'micro-major-courses': [],
  textbooks: [],
})

// 导航菜单项配置
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
    resource: 'online-open-courses',
    children: [],
    dropdown: true,
  },
  {
    label: '通识课程',
    path: '/academy/general-courses',
    resource: 'general-courses',
    children: [],
    dropdown: true,
  },
  {
    label: '微专业课程',
    path: '/academy/micro-majors',
    resource: 'micro-major-courses',
    children: [],
    dropdown: true,
  },
  {
    label: '精品教材',
    path: '/academy/textbooks',
    resource: 'textbooks',
    children: [],
    dropdown: true,
  },
  {
    label: '题库',
    path: '/academy/question-bank',
    children: [],
    dropdown: false,
  },
]

// 计算属性：为导航项添加分类子菜单
const navItemsWithCategories = computed(() =>
  academyNavItems.map((item) => ({
    ...item,
    children: item.resource
      ? categoryMap.value[item.resource].map((category) => ({
          label: category.name,
          path: item.path,
          query: { category: category.name },
        }))
      : item.children,
  })),
)

// 需要折叠二级导航的页面路径列表
const collapsedSubnavPaths = ['/academy/my-courses', '/academy/assignments', '/academy/exams']
// 题库练习模式导航状态
const questionPracticeSubnavActive = ref(false)
const questionPracticeSubnavCollapsed = ref(false)

// 计算属性：判断二级导航是否需要折叠
const isSubnavCollapsed = computed(() =>
  collapsedSubnavPaths.some((path) => route.path === path || route.path.startsWith(`${path}/`)) ||
  questionPracticeSubnavCollapsed.value,
)

// 计算属性：判断当前是否在题库课程详情页
const isQuestionBankCourseDetail = computed(() =>
  route.path.startsWith('/academy/question-bank/courses/'),
)

/**
 * 导航跳转处理函数
 * @param {Object|string} target - 目标路由
 * @param {Event} event - 点击事件
 */
const navigateTo = (target, event) => {
  event?.currentTarget?.blur()
  router.push(target)
}

/**
 * 加载导航分类数据
 * 通过 API 批量获取各资源类型的分类列表
 */
const loadNavCategories = async () => {
  const resources = ['online-open-courses', 'general-courses', 'micro-major-courses', 'textbooks']
  const results = await Promise.all(
    resources.map(async (resource) => {
      try {
        return [resource, await fetchAcademyCategories(resource)]
      } catch {
        return [resource, []]
      }
    }),
  )

  categoryMap.value = Object.fromEntries(results)
}

/**
 * 更新题库练习模式的导航折叠状态
 * 根据滚动位置判断是否折叠导航
 */
const updateQuestionPracticeSubnav = () => {
  if (!questionPracticeSubnavActive.value || !isQuestionBankCourseDetail.value) {
    questionPracticeSubnavCollapsed.value = false
    return
  }
  questionPracticeSubnavCollapsed.value = window.scrollY > 80
}

/**
 * 处理题库练习开始事件
 * 进入练习模式时激活导航折叠功能
 */
const handleQuestionPracticeStart = () => {
  if (!isQuestionBankCourseDetail.value) return
  questionPracticeSubnavActive.value = true
  questionPracticeSubnavCollapsed.value = true
}

// 监听路由变化，重置题库练习导航状态
watch(
  () => route.path,
  () => {
    if (!isQuestionBankCourseDetail.value) {
      questionPracticeSubnavActive.value = false
      questionPracticeSubnavCollapsed.value = false
    } else {
      updateQuestionPracticeSubnav()
    }
  },
)

onMounted(() => {
  loadNavCategories()
  window.addEventListener('scroll', updateQuestionPracticeSubnav, { passive: true })
  window.addEventListener('academy-question-practice-start', handleQuestionPracticeStart)
})

onBeforeUnmount(() => {
  window.removeEventListener('scroll', updateQuestionPracticeSubnav)
  window.removeEventListener('academy-question-practice-start', handleQuestionPracticeStart)
})
</script>

<template>
  <!-- 在线学堂主容器，包含导航栏和内容区域 -->
  <div
    :class="[
      'academy-page',
      {
        'academy-page-subnav-collapsed': isSubnavCollapsed,
        'academy-page-question-practice-collapsed': questionPracticeSubnavCollapsed,
      },
    ]"
  >
    <!-- 导航折叠展开按钮 -->
    <button
      v-if="isSubnavCollapsed"
      class="academy-subnav-trigger"
      type="button"
      aria-label="展开在线学堂二级导航"
    >
      <el-icon><ArrowDown /></el-icon>
    </button>

    <!-- 在线学堂二级导航栏 -->
    <nav class="academy-subnav" aria-label="在线学堂导航">
      <div v-for="item in navItemsWithCategories" :key="item.path" class="academy-nav-item">
        <button class="academy-nav-button" type="button" @click="navigateTo(item.path, $event)">
          <span>{{ item.label }}</span>
          <span v-if="item.dropdown" class="nav-arrow" aria-hidden="true">▾</span>
        </button>

        <!-- 下拉菜单 -->
        <div v-if="item.dropdown" class="academy-dropdown-menu" role="menu">
          <a
            v-for="child in item.children"
            :key="child.path"
            class="dropdown-link"
            :href="child.path"
            role="menuitem"
            @click.prevent="navigateTo({ path: child.path, query: child.query }, $event)"
          >
            {{ child.label }}
          </a>
          <span v-if="item.children.length === 0" class="dropdown-empty">菜单预留</span>
        </div>
      </div>
    </nav>

    <!-- 子页面内容渲染区域 -->
    <RouterView />
  </div>
</template>
