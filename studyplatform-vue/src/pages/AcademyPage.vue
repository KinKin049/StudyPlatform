<script setup>
import { computed } from 'vue'
import AcademyGeneralCourses from './academy/AcademyGeneralCourses.vue'
import AcademyHome from './academy/AcademyHome.vue'
import AcademyMicroMajors from './academy/AcademyMicroMajors.vue'
import AcademyOpenCourses from './academy/AcademyOpenCourses.vue'
import AcademyTextbooks from './academy/AcademyTextbooks.vue'

const props = defineProps({
  currentPath: {
    type: String,
    required: true,
  },
})

const emit = defineEmits(['navigate'])

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

const currentAcademyComponent = computed(() => {
  const academyRoutes = {
    '/academy/home': AcademyHome,
    '/academy/open-courses': AcademyOpenCourses,
    '/academy/general-courses': AcademyGeneralCourses,
    '/academy/micro-majors': AcademyMicroMajors,
    '/academy/textbooks': AcademyTextbooks,
  }

  return academyRoutes[props.currentPath] || AcademyHome
})

const navigateTo = (path) => {
  emit('navigate', path)
}
</script>

<template>
  <div class="academy-page">
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

    <component :is="currentAcademyComponent" />
  </div>
</template>
