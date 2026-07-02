<script setup>
const subjects = [
  {
    title: '高等数学',
    subtitle: '曲面、梯度与积分区域',
    description: '适合展示二元函数曲面、切平面、旋转体和多元微积分几何意义。',
    models: ['z = sin(x)cos(y)', '旋转抛物面', '梯度方向场'],
  },
  {
    title: '大学物理',
    subtitle: '场、波动与轨迹',
    description: '适合展示电场线、势能面、简谐振动、波动叠加和抛体轨迹。',
    models: ['电场强度分布', '波面传播', '力学轨迹'],
  },
  {
    title: '概率论',
    subtitle: '分布曲面与采样点云',
    description: '适合展示二维正态分布、联合概率密度、条件分布和随机采样结果。',
    models: ['二维正态曲面', '联合分布热力面', '随机点云'],
  },
]
</script>

<template>
  <main class="visual-page space-model-page">
    <section class="visual-hero compact">
      <p class="visual-kicker">3D Math</p>
      <h1>空间模型实验室</h1>
      <p>三维模块先搭建学科入口和模型规范，后续可接入 Three.js 进行真实三维交互渲染。</p>
    </section>

    <section class="space-subject-grid" aria-label="三维数学模块">
      <article v-for="subject in subjects" :key="subject.title" class="space-subject-card">
        <div class="space-preview">
          <span></span>
        </div>
        <div class="space-subject-body">
          <p>{{ subject.subtitle }}</p>
          <h2>{{ subject.title }}</h2>
          <span>{{ subject.description }}</span>
          <ul>
            <li v-for="model in subject.models" :key="model">{{ model }}</li>
          </ul>
          <button type="button">模型接口预留</button>
        </div>
      </article>
    </section>
  </main>
</template>
