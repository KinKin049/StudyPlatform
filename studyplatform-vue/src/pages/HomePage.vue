<script setup>
import AppNavigation from '../components/AppNavigation.vue'

defineProps({
  navItems: {
    type: Array,
    required: true,
  },
})

const emit = defineEmits(['navigate'])
</script>

<template>
  <div class="home-page">
    <AppNavigation :nav-items="navItems" @navigate="emit('navigate', $event)" />

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
