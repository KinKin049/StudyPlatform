<script setup>
import { RouterLink } from 'vue-router'

const modules = [
  {
    title: '算法结构可视化',
    path: '/visualization/data-structure',
    label: 'Data Structure',
    metric: '6+',
    action: '打开算法库',
    description: '整合已有算法动画 HTML，提供链表、KMP、表达式树和排序过程的可视化入口。',
    points: ['已有资源承载', '算法步骤演示', '适合课程实验'],
  },
  {
    title: '函数图像实验室',
    path: '/visualization/function-2d',
    label: '2D Math',
    metric: '2D',
    action: '绘制函数',
    description: '输入函数表达式后绘制二维曲线，支持范围、采样密度和多种常用数学函数。',
    points: ['表达式绘图', '坐标轴缩放', '数学实验工具'],
  },
  {
    title: '空间模型实验室',
    path: '/visualization/space-models',
    label: '3D Math',
    metric: '28+',
    action: '进入模型库',
    description: '面向高等数学、大学物理和概率论的三维知识点讲解与模型绘制。',
    points: ['三维曲面', '物理场景', '概率分布'],
  },
]

const metrics = [
  { value: '3', label: '核心实验模块' },
  { value: '28+', label: '三维知识模型' },
  { value: 'Real-time', label: '交互式参数控制' },
]
</script>

<template>
  <main class="visual-page visual-home">
    <section class="visual-command-hero">
      <div class="visual-command-copy">
        <p class="visual-kicker">Visualization Lab</p>
        <h1>可视化实验中心</h1>
        <span>把算法、函数与空间模型变成可交互的学习对象，支撑课程讲解、实验演示和自主探索。</span>
      </div>

      <div class="visual-command-panel" aria-label="可视化能力概览">
        <div v-for="item in metrics" :key="item.label">
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
        </div>
      </div>
    </section>

    <section class="visual-module-grid" aria-label="可视化模块">
      <article v-for="item in modules" :key="item.path" class="visual-module-card">
        <div class="visual-module-head">
          <p>{{ item.label }}</p>
          <strong>{{ item.metric }}</strong>
        </div>
        <h2>{{ item.title }}</h2>
        <span>{{ item.description }}</span>
        <div class="visual-module-tags">
          <small v-for="point in item.points" :key="point">{{ point }}</small>
        </div>
        <RouterLink :to="item.path">{{ item.action }}</RouterLink>
      </article>
    </section>
  </main>
</template>
