<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { fetchQuestionBankCourseCatalog } from '../../api/academy'
import { resolveResourceUrl } from '../../api/request'

const router = useRouter()

const categories = ref([])
const activeCategoryCode = ref('')
const loading = ref(false)
const errorMessage = ref('')

const activeCategory = computed(() => {
  return categories.value.find((category) => category.code === activeCategoryCode.value) || categories.value[0]
})

const totalSets = computed(() => {
  return categories.value.reduce((sum, category) => sum + (category.sets?.length || 0), 0)
})

const readySets = computed(() => {
  return categories.value
    .flatMap((category) => category.sets || [])
    .filter((item) => item.routePath && item.questionCount > 0).length
})

const loadCatalog = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await fetchQuestionBankCourseCatalog()
    categories.value = data || []
    activeCategoryCode.value = categories.value[0]?.code || ''
  } catch (error) {
    errorMessage.value = error.message || '课程题库目录加载失败'
  } finally {
    loading.value = false
  }
}

const coverSrc = (set) => {
  return resolveResourceUrl(set.coverUrl || set.fallbackCoverUrl || '')
}

const handleCoverError = (event, set) => {
  if (!set.fallbackCoverUrl || event.currentTarget.dataset.fallbackApplied === 'true') {
    return
  }
  event.currentTarget.dataset.fallbackApplied = 'true'
  event.currentTarget.src = set.fallbackCoverUrl
}

const openSet = (set) => {
  if (set.routePath) {
    router.push(set.routePath)
    return
  }
  // TODO: 接入未完成题库，例如 /api/academy/question-bank/courses/{code}
  console.info('course question bank action reserved:', set.code)
}

onMounted(loadCatalog)
</script>

<template>
  <main class="academy-main question-course-main question-catalog-main">
    <nav class="question-course-breadcrumb" aria-label="题库面包屑">
      <RouterLink to="/academy/home">在线学堂</RouterLink>
      <span>&gt;</span>
      <RouterLink to="/academy/question-bank">题库</RouterLink>
      <span>&gt;</span>
      <strong>课程题库</strong>
    </nav>

    <section class="question-catalog-hero">
      <div>
        <p>Course Question Bank</p>
        <h1>课程题库首页</h1>
        <span>按课程和考试方向整理题库入口，前端通过后端 API 读取 MySQL 目录与题目数据。</span>
      </div>
      <div class="question-catalog-stats" aria-label="题库目录状态">
        <div>
          <strong>{{ categories.length }}</strong>
          <span>大类</span>
        </div>
        <div>
          <strong>{{ totalSets }}</strong>
          <span>小类题库</span>
        </div>
        <div>
          <strong>{{ readySets }}</strong>
          <span>已接入</span>
        </div>
      </div>
    </section>

    <p v-if="errorMessage" class="question-course-message is-error">{{ errorMessage }}</p>

    <section class="question-catalog-shell" aria-label="课程题库目录">
      <div class="question-catalog-tabs">
        <button
          v-for="category in categories"
          :key="category.code"
          type="button"
          :class="{ 'is-active': category.code === activeCategoryCode }"
          @click="activeCategoryCode = category.code"
        >
          <strong>{{ category.name }}</strong>
          <span>{{ category.sets?.length || 0 }} 个方向</span>
        </button>
      </div>

      <div v-if="loading" class="question-course-empty">正在加载课程题库...</div>
      <div v-else-if="!activeCategory" class="question-course-empty">暂无课程题库目录。</div>
      <section v-else class="question-catalog-section">
        <header>
          <div>
            <h2>{{ activeCategory.name }}</h2>
            <p>{{ activeCategory.description }}</p>
          </div>
          <span>{{ activeCategory.sets?.length || 0 }} 个小类</span>
        </header>

        <div class="question-catalog-grid">
          <button
            v-for="set in activeCategory.sets"
            :key="set.code"
            type="button"
            class="question-catalog-card"
            :class="{ 'is-ready': set.routePath }"
            @click="openSet(set)"
          >
            <img :src="coverSrc(set)" :alt="set.title" @error="handleCoverError($event, set)" />
            <div>
              <p>{{ set.subtitle || set.categoryName }}</p>
              <h3>{{ set.title }}</h3>
              <span>{{ set.description }}</span>
              <small>
                {{ set.statusLabel || '建设中' }}
                <template v-if="set.questionCount"> · {{ set.questionCount }} 题</template>
              </small>
            </div>
          </button>
        </div>
      </section>
    </section>
  </main>
</template>
