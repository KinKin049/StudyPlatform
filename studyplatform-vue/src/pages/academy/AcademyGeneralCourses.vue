<!-- 通识课程列表页面组件，展示通识教育课程列表，支持搜索和分类筛选 -->
<script setup>
import { watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useRoute } from 'vue-router'
import { resolveResourceUrl } from '../../api/request'
import { useAcademyList } from '../../composables/useAcademyList'

const route = useRoute()

// 使用课程列表组合式函数，获取通识课程数据
const {
  selectedCategory,
  keyword,
  categories,
  filteredItems: filteredCourses,
  pagedItems: pagedCourses,
  currentPage,
  pageSize,
  totalItems,
  loading,
  error,
  loadItems,
} = useAcademyList('general-courses', ['name', 'school', 'category'])

/**
 * 解析课程封面图片 URL
 */
const resolveCover = (course) => resolveResourceUrl(course.cover || course.coverUrl)

/**
 * 封面图片加载失败时使用备用地址
 */
const useCoverFallback = (event, course) => {
  if (course.coverUrl && event.target.src !== course.coverUrl) {
    event.target.src = course.coverUrl
  }
}

// 监听分类列表和路由参数变化，同步筛选条件
watch(
  [categories, () => route.query.category, () => route.query.keyword],
  ([categoryList, category, queryKeyword]) => {
    if (typeof category === 'string' && categoryList.includes(category)) {
      selectedCategory.value = category
    }

    if (typeof queryKeyword === 'string') {
      keyword.value = queryKeyword
    }
  },
  { immediate: true },
)
</script>

<template>
  <!-- 通识课程列表页面主容器 -->
  <main class="online-course-main general-course-main">
    <!-- 页面顶部标题区域 -->
    <section class="online-course-hero" aria-labelledby="general-course-title">
      <div>
        <h1 id="general-course-title">通识课程</h1>
      </div>

      <!-- 搜索框 -->
      <div class="online-course-search online-course-hero-search" role="search">
        <input
          v-model="keyword"
          type="search"
          placeholder="搜索课程、分类或来源"
          aria-label="搜索课程、分类或来源"
        />
      </div>
    </section>

    <!-- 课程分类筛选区域 -->
    <section class="online-course-tools" aria-label="通识课程筛选">
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

    <!-- 课程列表区域 -->
    <section class="online-course-board" aria-label="通识课程列表">
      <div class="online-course-summary">
        <h2>{{ selectedCategory }}课程</h2>
        <span>共 {{ filteredCourses.length }} 门</span>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="academy-state">正在加载课程数据...</div>
      <!-- 错误状态 -->
      <div v-else-if="error" class="academy-state academy-state-error">
        <span>{{ error }}</span>
        <button type="button" @click="loadItems">重试</button>
      </div>
      <!-- 空状态 -->
      <div v-else-if="filteredCourses.length === 0" class="academy-state">暂无匹配课程</div>

      <!-- 课程卡片网格 -->
      <div v-else class="online-course-grid">
        <article v-for="course in pagedCourses" :key="course.id" class="online-course-card">
          <RouterLink :to="`/academy/general-courses/${encodeURIComponent(course.id)}`">
            <img
              :src="resolveCover(course)"
              :alt="course.name"
              loading="lazy"
              @error="useCoverFallback($event, course)"
            />
            <div class="online-course-card-body">
              <div class="online-course-card-meta">
                <span>{{ course.category }}</span>
                <strong>{{ course.participants }} 人学习</strong>
              </div>
              <h3>{{ course.name }}</h3>
              <p>{{ course.school }}</p>
            </div>
          </RouterLink>
        </article>
      </div>

      <!-- 分页组件 -->
      <el-pagination
        v-if="filteredCourses.length > pageSize"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        class="academy-pagination"
        background
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[8, 12, 16, 24]"
        :total="totalItems"
      />
    </section>
  </main>
</template>
