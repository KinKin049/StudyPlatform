<script setup>
import { RouterLink } from 'vue-router'

const modules = [
  {
    title: '算法结构可视化',
    path: '/visualization/data-structure',
    label: 'Data Structure',
    description: '整合已有算法动画 HTML，提供链表、KMP、表达式树和排序过程的可视化入口。',
    points: ['已有资源承载', '算法步骤演示', '适合课程实验'],
  },
  {
    title: '函数图像实验室',
    path: '/visualization/function-2d',
    label: '2D Math',
    description: '输入函数表达式后绘制二维曲线，支持范围、采样密度和多种常用数学函数。',
    points: ['表达式绘图', '坐标轴缩放', '数学实验工具'],
  },
  {
    title: '空间模型实验室',
    path: '/visualization/space-3d',
    label: '3D Math',
    description: '面向高等数学、大学物理和概率论的三维知识点讲解与模型绘制。',
    points: ['三维曲面', '物理场景', '概率分布'],
  },
]
</script>

<template>
  <main class="visual-page visual-home">
    <section class="visual-hero">
      <p class="visual-kicker">Visualization Lab</p>
      <h1>可视化</h1>
      <p>把算法、函数与空间模型变成可交互的学习对象，支撑课程讲解、实验演示和自主探索。</p>
    </section>

    <section class="visual-module-grid" aria-label="可视化模块">
      <article v-for="item in modules" :key="item.path" class="visual-module-card">
        <p>{{ item.label }}</p>
        <h2>{{ item.title }}</h2>
        <span>{{ item.description }}</span>
        <ul>
          <li v-for="point in item.points" :key="point">{{ point }}</li>
        </ul>
        <RouterLink :to="item.path">进入模块</RouterLink>
      </article>
    </section>
  </main>
</template>
