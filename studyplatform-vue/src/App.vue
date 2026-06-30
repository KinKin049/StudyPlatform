<script setup>
const navItems = [
  {
    label: 'XX学堂',
    path: '/academy',
    children: [],
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

const handleNavigate = (item) => {
  // 预留路由跳转入口：接入 vue-router 后可替换为 router.push(item.path)
  window.history.pushState({}, '', item.path)
}
</script>

<template>
  <div class="home-page">
    <header class="site-header">
      <a class="site-brand" href="/" aria-label="返回首页">XXX</a>

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
</template>
