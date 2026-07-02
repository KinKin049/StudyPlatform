<script setup>
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { algorithmDemos, getAlgorithmAssetPath } from './algorithmDemos'

const route = useRoute()

const currentDemo = computed(() => algorithmDemos.find((demo) => demo.id === route.params.demoId))
const demoSrc = computed(() => (currentDemo.value ? getAlgorithmAssetPath(currentDemo.value.file) : ''))
</script>

<template>
  <main class="visual-page algorithm-viewer-page">
    <section v-if="currentDemo" class="algorithm-viewer-shell">
      <nav class="algorithm-viewer-breadcrumb" aria-label="当前位置">
        <RouterLink to="/visualization">可视化</RouterLink>
        <span>&gt;</span>
        <RouterLink to="/visualization/data-structure">算法结构可视化</RouterLink>
        <span>&gt;</span>
        <strong>{{ currentDemo.title }}</strong>
      </nav>

      <iframe
        class="algorithm-demo-frame"
        :src="demoSrc"
        :title="currentDemo.title"
      />
    </section>

    <section v-else class="algorithm-viewer-empty">
      <h1>演示不存在</h1>
      <RouterLink to="/visualization/data-structure">返回算法结构可视化</RouterLink>
    </section>
  </main>
</template>
