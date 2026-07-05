<script setup>
import { watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useRoute } from 'vue-router'
import { resolveResourceUrl } from '../../api/request'
import { useAcademyList } from '../../composables/useAcademyList'

const route = useRoute()

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
} = useAcademyList('online-open-courses', ['name', 'teacher', 'school', 'category'])

const resolveCover = (course) => resolveResourceUrl(course.cover || course.coverUrl)

const useCoverFallback = (event, course) => {
  if (course.coverUrl && event.target.src !== course.coverUrl) {
    event.target.src = course.coverUrl
  }
}

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
  <main class="online-course-main">
    <section class="online-course-hero" aria-labelledby="online-course-title">
      <div>
        <p class="academy-kicker">Online Open Courses</p>
        <h1 id="online-course-title">在线开放课程</h1>
        <p>数据来源于爱课程公开课程列表，由后端 API 从 MySQL 读取并返回给前端渲染。</p>
      </div>

      <div class="online-course-search online-course-hero-search" role="search">
        <input
          v-model="keyword"
          type="search"
          placeholder="搜索课程、讲师、学校或类别"
          aria-label="搜索课程、讲师、学校或类别"
        />
      </div>
    </section>

    <section class="online-course-tools" aria-label="课程筛选">
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

    <section class="online-course-board" aria-label="在线开放课程列表">
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
        <article v-for="course in pagedCourses" :key="course.id" class="online-course-card">
          <RouterLink :to="`/academy/open-courses/${encodeURIComponent(course.id)}`">
            <img
              :src="resolveCover(course)"
              :alt="course.name"
              loading="lazy"
              @error="useCoverFallback($event, course)"
            />
            <div class="online-course-card-body">
              <div class="online-course-card-meta">
                <span>{{ course.category }}</span>
                <strong>{{ course.participants }} 人参加</strong>
              </div>
              <h3>{{ course.name }}</h3>
              <dl>
                <div>
                  <dt>讲师</dt>
                  <dd>{{ course.teacher || '暂未提供' }}</dd>
                </div>
                <div>
                  <dt>开课时间</dt>
                  <dd>{{ course.startTime || '待定' }}</dd>
                </div>
              </dl>
              <p>{{ course.school }}</p>
            </div>
          </RouterLink>
        </article>
      </div>

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
