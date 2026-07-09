<script setup>
// 顶部导航栏组件，包含品牌标识、主导航菜单和用户操作区域
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getStoredAuthUser } from '../api/auth'
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

// 判断用户是否已登录
const isLoggedIn = computed(() => Boolean(authUser.value?.id))
// 判断当前用户是否为管理员
const isAdmin = computed(() => authUser.value?.email === 'admin@admin.com' && authUser.value?.roleType === 'admin')
// 获取用户显示名称，优先级：认证用户名 > 导航用户名称 > 默认名称
const userDisplayName = computed(() => authUser.value?.username || navigationUser.value.name || 'Kinkin')
// 获取用户头像完整 URL
const avatarSrc = computed(() => resolveResourceUrl(navigationUser.value.avatarUrl))
// 获取用户名称首字母用于头像占位
const avatarInitial = computed(() => (userDisplayName.value || 'K').trim().slice(0, 1).toUpperCase())

// 导航跳转方法，点击后移除按钮焦点
const navigateTo = (path, event) => {
  event?.currentTarget?.blur()
  router.push(path)
}

// 处理用户入口点击，已登录跳转到个人主页，未登录跳转到登录页
const handleUserEntryClick = (event) => {
  navigateTo(isLoggedIn.value ? '/profile' : '/login', event)
}

// 切换用户，保留当前登录态，待新账号登录成功后再覆盖
const switchUser = (event) => {
  event?.currentTarget?.blur()
  router.push('/login')
}

// 注册新用户，保留当前登录态，待注册成功后再覆盖
const registerNewUser = (event) => {
  event?.currentTarget?.blur()
  router.push('/register')
}

// 加载导航栏用户信息
const loadNavigationUser = async () => {
  try {
    navigationUser.value = await fetchProfileUser()
  } catch (error) {
    console.warn('failed to load navigation profile:', error)
  }
}

// 处理个人资料更新事件
const handleProfileUpdated = (event) => {
  if (event.detail) {
    navigationUser.value = event.detail
    return
  }
  loadNavigationUser()
}

// 处理认证状态更新事件
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
  <!-- 页面顶部导航栏 -->
  <header class="site-header">
    <!-- 品牌标识，点击返回首页 -->
    <RouterLink class="site-brand" to="/" aria-label="返回首页">
      <img class="site-brand-logo" src="/brand/epistemehub-logo.png" alt="" aria-hidden="true" />
      <span class="site-brand-name">EpistemeHub</span>
    </RouterLink>

    <!-- 主导航菜单 -->
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

        <!-- 下拉菜单 -->
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

    <!-- 用户操作区域 -->
    <div class="user-area">
      <!-- 用户头像入口按钮 -->
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

      <!-- 用户菜单下拉面板 -->
      <div class="user-menu" role="menu">
        <!-- 已登录状态菜单 -->
        <template v-if="isLoggedIn">
          <div class="user-menu-account">
            <span>当前用户</span>
            <strong>{{ userDisplayName }}</strong>
          </div>
          <button class="user-menu-link" type="button" role="menuitem" @click="navigateTo('/profile', $event)">
            个人主页
          </button>
          <button class="user-menu-link" type="button" role="menuitem" @click="navigateTo('/exchange', $event)">
            金币兑换中心
          </button>
          <button v-if="isAdmin" class="user-menu-link" type="button" role="menuitem" @click="navigateTo('/admin', $event)">
            管理后台
          </button>
          <button class="user-menu-link" type="button" role="menuitem" @click="switchUser">
            切换用户
          </button>
          <button class="user-menu-link" type="button" role="menuitem" @click="registerNewUser">
            注册新账号
          </button>
        </template>
        <!-- 未登录状态菜单 -->
        <template v-else>
          <button class="user-menu-link" type="button" role="menuitem" @click="navigateTo('/login', $event)">
            登录
          </button>
          <button class="user-menu-link" type="button" role="menuitem" @click="navigateTo('/register', $event)">
            注册
          </button>
          <button class="user-menu-link" type="button" role="menuitem" @click="navigateTo('/exchange', $event)">
            金币兑换中心
          </button>
        </template>
      </div>
    </div>
  </header>
</template>
