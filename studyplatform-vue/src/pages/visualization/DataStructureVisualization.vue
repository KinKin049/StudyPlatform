<script setup>
import { algorithmDemos } from './algorithmDemos'
</script>

<template>
  <main class="visual-page">
    <section class="visual-hero compact">
      <p class="visual-kicker">Data Structure</p>
      <h1>算法结构可视化</h1>
      <p>这里先承载已有 HTML 动画文件，后续可逐步改造成 Vue 原生组件。</p>
    </section>

    <section class="algorithm-demo-grid" aria-label="数据结构可视化列表">
      <article v-for="demo in algorithmDemos" :key="demo.id" class="algorithm-demo-card">
        <div>
          <p>Algorithm Demo</p>
          <h2>{{ demo.title }}</h2>
          <span>{{ demo.description }}</span>
        </div>
        <RouterLink :to="`/visualization/data-structure/${demo.id}`">打开演示</RouterLink>
      </article>
    </section>
  </main>
</template>
