<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterView, useRouter } from 'vue-router'
import { fetchAcademyCategories } from '../api/academy'

const router = useRouter()
const categoryMap = ref({
  'online-open-courses': [],
  'general-courses': [],
  'micro-major-courses': [],
  textbooks: [],
})

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
]

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

const navigateTo = (target, event) => {
  event?.currentTarget?.blur()
  router.push(target)
}

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

onMounted(loadNavCategories)
</script>

<template>
  <div class="academy-page">
    <nav class="academy-subnav" aria-label="在线学堂导航">
      <div v-for="item in navItemsWithCategories" :key="item.path" class="academy-nav-item">
        <button class="academy-nav-button" type="button" @click="navigateTo(item.path, $event)">
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
            @click.prevent="navigateTo({ path: child.path, query: child.query }, $event)"
          >
            {{ child.label }}
          </a>
          <span v-if="item.children.length === 0" class="dropdown-empty">菜单预留</span>
        </div>
      </div>
    </nav>

    <RouterView />
  </div>
</template>
