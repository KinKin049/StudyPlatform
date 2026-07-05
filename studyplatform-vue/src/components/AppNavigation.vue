<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { clearStoredAuthUser, getStoredAuthUser } from '../api/auth'
import { fetchProfileUser } from '../api/profile'
import { resolveResourceUrl } from '../api/request'

const props = defineProps({
  navItems: {
    type: Array,
    required: true,
  },
})

const router = useRouter()
const authUser = ref(getStoredAuthUser())
const navigationUser = ref({
  name: 'Kinkin',
  avatarUrl: '',
})

const isLoggedIn = computed(() => Boolean(authUser.value?.id))
const userDisplayName = computed(() => authUser.value?.username || navigationUser.value.name || 'Kinkin')
const avatarSrc = computed(() => resolveResourceUrl(navigationUser.value.avatarUrl))
const avatarInitial = computed(() => (userDisplayName.value || 'K').trim().slice(0, 1).toUpperCase())

const navigateTo = (path, event) => {
  event?.currentTarget?.blur()
  router.push(path)
}

const handleUserEntryClick = (event) => {
  navigateTo(isLoggedIn.value ? '/profile' : '/login', event)
}

const switchUser = (event) => {
  event?.currentTarget?.blur()
  clearStoredAuthUser()
  router.push('/login')
}

const registerNewUser = (event) => {
  event?.currentTarget?.blur()
  clearStoredAuthUser()
  router.push('/register')
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

const handleAuthUpdated = (event) => {
  authUser.value = event.detail || getStoredAuthUser()
}

onMounted(() => {
  loadNavigationUser()
  window.addEventListener('study-platform:profile-updated', handleProfileUpdated)
  window.addEventListener('study-platform:auth-updated', handleAuthUpdated)
})

onBeforeUnmount(() => {
  window.removeEventListener('study-platform:profile-updated', handleProfileUpdated)
  window.removeEventListener('study-platform:auth-updated', handleAuthUpdated)
})
</script>

<template>
  <header class="site-header">
    <RouterLink class="site-brand" to="/" aria-label="返回首页">
      EpistemeHub
    </RouterLink>

    <nav class="site-nav" aria-label="主导航">
      <div
        v-for="item in props.navItems"
        :key="item.path"
        :class="['nav-item', { 'nav-item-academy': item.path.startsWith('/academy') }]"
      >
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

    <div class="user-area">
      <button
        class="user-entry"
        type="button"
        :aria-label="isLoggedIn ? '个人主页' : '登录'"
        @click="handleUserEntryClick"
      >
        <span class="user-avatar">
          <img v-if="avatarSrc" :src="avatarSrc" :alt="userDisplayName" />
          <span v-else>{{ avatarInitial }}</span>
        </span>
      </button>

      <div class="user-menu" role="menu">
        <template v-if="isLoggedIn">
          <div class="user-menu-account">
            <span>当前用户</span>
            <strong>{{ userDisplayName }}</strong>
          </div>
          <button class="user-menu-link" type="button" role="menuitem" @click="navigateTo('/profile', $event)">
            个人主页
          </button>
          <button class="user-menu-link" type="button" role="menuitem" @click="switchUser">
            切换用户
          </button>
          <button class="user-menu-link" type="button" role="menuitem" @click="registerNewUser">
            注册新账号
          </button>
        </template>
        <template v-else>
          <button class="user-menu-link" type="button" role="menuitem" @click="navigateTo('/login', $event)">
            登录
          </button>
          <button class="user-menu-link" type="button" role="menuitem" @click="navigateTo('/register', $event)">
            注册
          </button>
        </template>
      </div>
    </div>
  </header>
</template>
