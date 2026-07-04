<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchProfileUser } from '../api/profile'
import { resolveResourceUrl } from '../api/request'

const props = defineProps({
  navItems: {
    type: Array,
    required: true,
  },
})

const router = useRouter()
const navigationUser = ref({
  name: 'Kinkin',
  avatarUrl: '',
})

const avatarSrc = computed(() => resolveResourceUrl(navigationUser.value.avatarUrl))
const avatarInitial = computed(() => (navigationUser.value.name || 'K').trim().slice(0, 1).toUpperCase())

const navigateTo = (path, event) => {
  event?.currentTarget?.blur()
  router.push(path)
}

const loadNavigationUser = async () => {
  try {
    navigationUser.value = await fetchProfileUser()
  } catch (error) {
    console.warn('failed to load navigation profile:', error)
  }
}

const handleProfileUpdated = (event) => {
  if (event.detail) {
    navigationUser.value = event.detail
    return
  }
  loadNavigationUser()
}

onMounted(() => {
  loadNavigationUser()
  window.addEventListener('study-platform:profile-updated', handleProfileUpdated)
})

onBeforeUnmount(() => {
  window.removeEventListener('study-platform:profile-updated', handleProfileUpdated)
})
</script>

<template>
  <header class="site-header">
    <RouterLink class="site-brand" to="/" aria-label="返回首页">
      EpistemeHub
    </RouterLink>

    <nav class="site-nav" aria-label="主导航">
      <div v-for="item in props.navItems" :key="item.path" class="nav-item">
        <button class="nav-button" type="button" @click="navigateTo(item.path, $event)">
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
            @click.prevent="navigateTo(child.path, $event)"
          >
            {{ child.label }}
          </a>
          <span v-if="item.children.length === 0" class="dropdown-empty">菜单预留</span>
        </div>
      </div>
    </nav>

    <button class="user-entry" type="button" aria-label="个人主页" @click="navigateTo('/profile', $event)">
      <span class="user-avatar">
        <img v-if="avatarSrc" :src="avatarSrc" :alt="navigationUser.name" />
        <span v-else>{{ avatarInitial }}</span>
      </span>
    </button>
  </header>
</template>
