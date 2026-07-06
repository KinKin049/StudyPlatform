<script setup>
import { useLearningTimeTracker } from '../../composables/useLearningTimeTracker'
import { algorithmDemos } from './algorithmDemos'

useLearningTimeTracker({
  moduleType: 'visualization',
  targetCode: 'data-structure',
  targetTitle: '数据结构可视化',
})
</script>

<template>
  <main class="visual-page algorithm-lab-page">
    <nav class="algorithm-viewer-breadcrumb visual-page-breadcrumb" aria-label="当前位置">
      <RouterLink to="/visualization">可视化</RouterLink>
      <span>&gt;</span>
      <strong>算法结构可视化</strong>
    </nav>

    <section class="algorithm-lab-hero">
      <div>
        <p class="visual-kicker">Data Structure</p>
        <h1>算法结构可视化</h1>
        <span>整合链表、排序、字符串匹配和表达式树动画，把抽象的数据结构过程拆成可观察的执行步骤。</span>
      </div>
    </section>

    <section class="algorithm-demo-grid algorithm-lab-grid" aria-label="数据结构可视化列表">
      <RouterLink
        v-for="demo in algorithmDemos"
        :key="demo.id"
        class="algorithm-demo-card"
        :to="`/visualization/data-structure/${demo.id}`"
      >
        <div class="algorithm-demo-index">
          <span>{{ demo.id.split('-').slice(0, 2).join(' / ') }}</span>
        </div>
        <div class="algorithm-demo-content">
          <p>Algorithm Demo</p>
          <h2>{{ demo.title }}</h2>
          <span>{{ demo.description }}</span>
        </div>
      </RouterLink>
    </section>
  </main>
</template>
