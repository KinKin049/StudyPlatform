<script setup>
const featuredCourses = [
  {
    title: '人工智能导论',
    category: '在线开放课程',
    meta: '32 学时 · 8 个章节',
  },
  {
    title: '大学生创新实践',
    category: '通识课程',
    meta: '24 学时 · 项目制学习',
  },
  {
    title: '数据分析微专业',
    category: '微专业课程',
    meta: '6 门课 · 能力认证',
  },
]
</script>

<template>
  <main class="academy-main">
    <section class="academy-hero" aria-labelledby="academy-title">
      <div class="academy-hero-copy">
        <p class="academy-kicker">EpistemeHub Academy</p>
        <h1 id="academy-title">在线学堂</h1>
        <p>面向学生的课程学习入口，聚合在线开放课程、通识课程、微专业课程与精品教材资源。</p>
      </div>

      <div class="academy-search" role="search">
        <input type="search" placeholder="搜索课程、教材或专题" aria-label="搜索课程、教材或专题" />
        <button type="button">搜索</button>
      </div>
    </section>

    <section class="academy-content" aria-label="学堂内容">
      <div class="academy-section-heading">
        <h2>推荐课程</h2>
        <button type="button">查看更多</button>
      </div>

      <div class="course-grid">
        <article v-for="course in featuredCourses" :key="course.title" class="course-card">
          <p>{{ course.category }}</p>
          <h3>{{ course.title }}</h3>
          <span>{{ course.meta }}</span>
        </article>
      </div>
    </section>
  </main>
</template>
