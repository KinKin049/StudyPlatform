<script setup>
/**
 * 算法演示查看器页面
 * 通过 iframe 嵌入已有的算法动画 HTML 资源
 * 支持链表、排序、KMP、表达式树等算法的可视化展示
 */
import { computed } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useLearningTimeTracker } from '../../composables/useLearningTimeTracker'
import { algorithmDemos, getAlgorithmAssetPath } from './algorithmDemos'

const route = useRoute()

/** 当前选中的演示配置 */
const currentDemo = computed(() => algorithmDemos.find((demo) => demo.id === route.params.demoId))
/** 演示资源的完整路径 */
const demoSrc = computed(() => (currentDemo.value ? getAlgorithmAssetPath(currentDemo.value.file) : ''))

useLearningTimeTracker({
  moduleType: 'visualization',
  targetCode: () => `data-structure:${route.params.demoId || 'unknown'}`,
  targetTitle: () => currentDemo.value?.title || '数据结构可视化',
})
</script>

<template>
  <main class="visual-page algorithm-viewer-page">
    <!-- 演示内容区域（演示存在时显示） -->
    <section v-if="currentDemo" class="algorithm-viewer-shell">
      <!-- 面包屑导航 -->
      <nav class="algorithm-viewer-breadcrumb" aria-label="当前位置">
        <RouterLink to="/visualization">可视化</RouterLink>
        <span>&gt;</span>
        <RouterLink to="/visualization/data-structure">算法结构可视化</RouterLink>
        <span>&gt;</span>
        <strong>{{ currentDemo.title }}</strong>
      </nav>

      <!-- 算法演示 iframe -->
      <iframe
        class="algorithm-demo-frame"
        :src="demoSrc"
        :title="currentDemo.title"
      />
    </section>

    <!-- 演示不存在时的错误提示 -->
    <section v-else class="algorithm-viewer-empty">
      <h1>演示不存在</h1>
      <RouterLink to="/visualization/data-structure">返回算法结构可视化</RouterLink>
    </section>
  </main>
</template>
