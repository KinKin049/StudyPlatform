<script setup>
import { useAcademyList } from '../../composables/useAcademyList'

const {
  selectedCategory,
  keyword,
  categories,
  filteredItems: filteredTextbooks,
  loading,
  error,
  loadItems,
} = useAcademyList('textbooks', ['name', 'editor', 'category', 'publisher', 'isbn', 'description'])
</script>

<template>
  <main class="online-course-main textbook-main">
    <section class="online-course-hero" aria-labelledby="textbook-title">
      <div>
        <p class="academy-kicker">Excellent Textbooks</p>
        <h1 id="textbook-title">精品教材</h1>
        <p>数据来源于爱课程精品教材页面，由后端 API 从 MySQL 读取并返回给前端渲染。</p>
      </div>

      <div class="online-course-search online-course-hero-search" role="search">
        <input
          v-model="keyword"
          type="search"
          placeholder="搜索教材、主编、分类或 ISBN"
          aria-label="搜索教材、主编、分类或 ISBN"
        />
      </div>
    </section>

    <section class="online-course-tools" aria-label="精品教材筛选">
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

    <section class="online-course-board" aria-label="精品教材列表">
      <div class="online-course-summary">
        <h2>{{ selectedCategory }}教材</h2>
        <span>共 {{ filteredTextbooks.length }} 本</span>
      </div>

      <div v-if="loading" class="academy-state">正在加载教材数据...</div>
      <div v-else-if="error" class="academy-state academy-state-error">
        <span>{{ error }}</span>
        <button type="button" @click="loadItems">重试</button>
      </div>
      <div v-else-if="filteredTextbooks.length === 0" class="academy-state">暂无匹配教材</div>

      <div v-else class="textbook-grid">
        <article v-for="textbook in filteredTextbooks" :key="textbook.id" class="textbook-card">
          <a :href="textbook.link" target="_blank" rel="noreferrer">
            <div class="textbook-cover">
              <img :src="textbook.cover" :alt="textbook.name" loading="lazy" />
            </div>
            <div class="textbook-card-body">
              <div class="online-course-card-meta">
                <span>{{ textbook.category }}</span>
                <strong>{{ textbook.publisher }}</strong>
              </div>
              <h3>{{ textbook.name }}</h3>
              <dl>
                <div>
                  <dt>主编</dt>
                  <dd>{{ textbook.editor || '暂无' }}</dd>
                </div>
                <div>
                  <dt>ISBN</dt>
                  <dd>{{ textbook.isbn || '暂无' }}</dd>
                </div>
              </dl>
              <p>{{ textbook.description || '暂无简介' }}</p>
            </div>
          </a>
        </article>
      </div>
    </section>
  </main>
</template>
