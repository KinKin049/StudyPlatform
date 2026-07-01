<script setup>
const props = defineProps({
  navItems: {
    type: Array,
    required: true,
  },
})

const emit = defineEmits(['navigate'])

const navigateTo = (path) => {
  emit('navigate', path)
}
</script>

<template>
  <header class="site-header">
    <a class="site-brand" href="/" aria-label="返回首页" @click.prevent="navigateTo('/')">
      EpistemeHub
    </a>

    <nav class="site-nav" aria-label="主导航">
      <div v-for="item in props.navItems" :key="item.path" class="nav-item">
        <button class="nav-button" type="button" @click="navigateTo(item.path)">
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
</template>
