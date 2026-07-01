<script setup>
import { useAcademyList } from '../../composables/useAcademyList'

const {
  selectedCategory,
  keyword,
  categories,
  filteredItems: filteredCourses,
  loading,
  error,
  loadItems,
} = useAcademyList('micro-major-courses', ['name', 'school', 'category'])
</script>

<template>
  <main class="online-course-main micro-major-main">
    <section class="online-course-hero" aria-labelledby="micro-major-title">
      <div>
        <p class="academy-kicker">Micro Major Courses</p>
        <h1 id="micro-major-title">微专业课程</h1>
        <p>
          数据来源于中国石油大学（北京）微专业课程页面，按微专业方向整理展示，封面文件存储在后端
          micro_major_course 目录中。
        </p>
      </div>

      <div class="online-course-search online-course-hero-search" role="search">
        <input
          v-model="keyword"
          type="search"
          placeholder="搜索课程、分类或来源"
          aria-label="搜索课程、分类或来源"
        />
      </div>
    </section>

    <section class="online-course-tools" aria-label="微专业课程筛选">
      <div class="online-category-list">
        <button
          v-for="category in categories"
          :key="category"
          type="button"
          :class="{ active: selectedCategory === category }"
          @click="selectedCategory = category"
        >
          {{ category }}
        </button>
      </div>
    </section>

    <section class="online-course-board" aria-label="微专业课程列表">
      <div class="online-course-summary">
        <h2>{{ selectedCategory }}课程</h2>
        <span>共 {{ filteredCourses.length }} 门</span>
      </div>

      <div v-if="loading" class="academy-state">正在加载课程数据...</div>
      <div v-else-if="error" class="academy-state academy-state-error">
        <span>{{ error }}</span>
        <button type="button" @click="loadItems">重试</button>
      </div>
      <div v-else-if="filteredCourses.length === 0" class="academy-state">暂无匹配课程</div>

      <div v-else class="online-course-grid">
        <article v-for="course in filteredCourses" :key="course.id" class="online-course-card">
          <a :href="course.link" target="_blank" rel="noreferrer">
            <img :src="course.cover" :alt="course.name" loading="lazy" />
            <div class="online-course-card-body">
              <div class="online-course-card-meta">
                <span>{{ course.category }}</span>
                <strong>{{ course.participants }} 人学习</strong>
              </div>
              <h3>{{ course.name }}</h3>
              <p>{{ course.school }}</p>
            </div>
          </a>
        </article>
      </div>
    </section>
  </main>
</template>
