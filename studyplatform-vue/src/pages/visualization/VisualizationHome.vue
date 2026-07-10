<script setup>
/**
 * 可视化实验中心主页
 * 展示可视化模块入口，包括算法结构可视化、函数图像实验室和空间模型实验室
 */
import { RouterLink } from 'vue-router'

/**
 * 可视化模块配置列表
 * 每个模块包含标题、路由路径、标签、指标、操作按钮和描述信息
 */
const modules = [
  {
    title: '算法结构可视化',
    path: '/visualization/data-structure',
    posterClass: 'visual-module-card--sale',
    titleLines: ['算法结构', '可视化'],
    posterEyebrow: '',
    posterBadge: '',
    posterCaption: '',
    label: 'Data Structure',
    number: '01',
    metric: '6+',
    action: '打开算法库',
    description: '整合已有算法动画 HTML，提供链表、KMP、表达式树和排序过程的可视化入口。',
    points: ['已有资源承载', '算法步骤演示', '适合课程实验'],
  },
  {
    title: '函数图像实验室',
    path: '/visualization/function-2d',
    posterClass: 'visual-module-card--construct',
    titleLines: ['函数图像', '实验室'],
    posterEyebrow: '',
    posterBadge: '',
    posterCaption: '',
    label: '2D Math',
    number: '02',
    metric: '2D',
    action: '绘制函数',
    description: '输入函数表达式后绘制二维曲线，支持范围、采样密度和多种常用数学函数。',
    points: ['表达式绘图', '坐标轴缩放', '数学实验工具'],
  },
  {
    title: '空间模型实验室',
    path: '/visualization/space-models',
    posterClass: 'visual-module-card--eclipse',
    titleLines: ['空间模型', '实验室'],
    posterEyebrow: '',
    posterBadge: '',
    posterCaption: '',
    label: '3D Math',
    number: '03',
    metric: '28+',
    action: '进入模型库',
    description: '面向高等数学、大学物理和概率论的三维知识点讲解与模型绘制。',
    points: ['三维曲面', '物理场景', '概率分布'],
  },
]

const explorations = [
  {
    title: '正弦波',
    formula: 'y = sin(x)',
    path: '/visualization/function-2d',
    tone: 'cyan',
    curve: 'sine',
  },
  {
    title: '抛物线',
    formula: 'y = x^2',
    path: '/visualization/function-2d',
    tone: 'violet',
    curve: 'parabola',
  },
  {
    title: '对数曲线',
    formula: 'y = ln(x)',
    path: '/visualization/function-2d',
    tone: 'rose',
    curve: 'log',
  },
  {
    title: '三维曲面',
    formula: 'z = f(x,y)',
    path: '/visualization/space-models',
    tone: 'blue',
    curve: 'surface',
  },
  {
    title: '概率分布',
    formula: 'P(X)',
    path: '/visualization/space-models',
    tone: 'mint',
    curve: 'distribution',
  },
  {
    title: '算法轨迹',
    formula: 'O(n)',
    path: '/visualization/data-structure',
    tone: 'amber',
    curve: 'algorithm',
  },
]

</script>

<template>
  <main class="visual-page visual-home">
    <!-- 页面头部区域 - 展示标题和能力概览 -->
    <section class="visual-command-hero">
      <div class="visual-command-copy">
        <h1>可视化实验中心</h1>
        <p>Visualization Lab</p>
        <span>算法 · 函数 · 空间模型</span>
      </div>
    </section>

    <!-- 模块网格区域 - 展示各可视化模块入口卡片 -->
    <section class="visual-module-grid" aria-label="可视化模块">
      <RouterLink
        v-for="item in modules"
        :key="item.path"
        :to="item.path"
        :class="['visual-module-card', item.posterClass]"
        :aria-label="`进入${item.title}`"
      >
        <span class="visual-card-art" aria-hidden="true"></span>
        <span v-if="item.posterEyebrow" class="visual-card-eyebrow">{{ item.posterEyebrow }}</span>
        <h2>
          <span v-for="line in item.titleLines" :key="line">{{ line }}</span>
        </h2>
        <span v-if="item.posterBadge" class="visual-card-badge">{{ item.posterBadge }}</span>
        <span v-if="item.posterCaption" class="visual-card-caption">{{ item.posterCaption }}</span>
      </RouterLink>
    </section>

    <section class="visual-deep-lab" aria-labelledby="visual-deep-lab-title">
      <span class="visual-deep-transition" aria-hidden="true"></span>
      <div class="visual-orbit-field" aria-hidden="true"></div>
      <div class="visual-deep-copy">
        <p>Interactive Gallery</p>
        <h2 id="visual-deep-lab-title">继续探索</h2>
        <span>从一条曲线、一个模型或一次算法过程开始。</span>
      </div>
      <div class="visual-explore-grid" aria-label="可交互实验入口">
        <RouterLink
          v-for="item in explorations"
          :key="item.title"
          :to="item.path"
          :class="['visual-explore-card', `visual-explore-card--${item.tone}`]"
        >
          <span class="visual-explore-glow"></span>
          <strong>{{ item.title }}</strong>
          <em>{{ item.formula }}</em>
          <svg
            class="visual-explore-curve"
            viewBox="0 0 280 96"
            aria-hidden="true"
            focusable="false"
          >
            <path
              v-if="item.curve === 'sine'"
              d="M8 52 C 32 24, 56 24, 80 52 S 128 80, 152 52 S 200 24, 224 52 S 256 80, 272 58"
            />
            <path
              v-else-if="item.curve === 'parabola'"
              d="M18 78 C 52 76, 82 70, 108 58 C 132 47, 150 33, 166 18 C 184 38, 205 56, 232 68 C 248 75, 262 78, 274 78"
            />
            <path
              v-else-if="item.curve === 'log'"
              d="M24 82 C 34 64, 46 48, 62 38 C 84 24, 112 19, 146 18 C 188 17, 234 18, 272 18"
            />
            <path
              v-else-if="item.curve === 'surface'"
              d="M18 68 C 58 42, 92 42, 132 66 C 170 88, 215 82, 264 48"
            />
            <path
              v-if="item.curve === 'surface'"
              d="M32 42 C 72 18, 113 18, 154 42 C 190 63, 224 58, 260 34"
            />
            <path
              v-else-if="item.curve === 'distribution'"
              d="M16 80 C 56 80, 74 78, 92 60 C 108 44, 112 20, 132 20 C 154 20, 158 45, 174 61 C 192 80, 222 80, 268 80"
            />
            <path
              v-else-if="item.curve === 'algorithm'"
              d="M22 74 H 74 V 38 H 122 V 58 H 174 V 26 H 228 V 70 H 268"
            />
            <circle v-if="item.curve === 'algorithm'" cx="74" cy="38" r="4" />
            <circle v-if="item.curve === 'algorithm'" cx="122" cy="58" r="4" />
            <circle v-if="item.curve === 'algorithm'" cx="174" cy="26" r="4" />
            <circle v-if="item.curve === 'algorithm'" cx="228" cy="70" r="4" />
          </svg>
        </RouterLink>
      </div>
    </section>
  </main>
</template>
