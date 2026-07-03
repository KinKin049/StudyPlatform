<script setup>
import { algorithmDemos } from './algorithmDemos'

const stats = [
  { value: algorithmDemos.length, label: '算法动画' },
  { value: 'HTML', label: '现有资源接入' },
  { value: 'Step', label: '过程可视化' },
]
</script>

<template>
  <main class="visual-page algorithm-lab-page">
    <section class="algorithm-lab-hero">
      <div>
        <RouterLink class="algorithm-lab-back" to="/visualization">可视化实验中心</RouterLink>
        <p class="visual-kicker">Data Structure</p>
        <h1>算法结构可视化</h1>
        <span>整合链表、排序、字符串匹配和表达式树动画，把抽象的数据结构过程拆成可观察的执行步骤。</span>
      </div>

      <aside class="algorithm-lab-status" aria-label="算法结构可视化状态">
        <div v-for="item in stats" :key="item.label">
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
        </div>
      </aside>
    </section>

    <section class="algorithm-demo-grid algorithm-lab-grid" aria-label="数据结构可视化列表">
      <article v-for="demo in algorithmDemos" :key="demo.id" class="algorithm-demo-card">
        <div class="algorithm-demo-index">
          <span>{{ demo.id.split('-').slice(0, 2).join(' / ') }}</span>
        </div>
        <div class="algorithm-demo-content">
          <p>Algorithm Demo</p>
          <h2>{{ demo.title }}</h2>
          <span>{{ demo.description }}</span>
          <RouterLink :to="`/visualization/data-structure/${demo.id}`">打开演示</RouterLink>
        </div>
      </article>
    </section>
  </main>
</template>
