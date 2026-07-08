<script setup>
/**
 * 课程题库目录组件
 * 展示按大类组织的课程题库列表，支持分类切换和封面图显示
 */
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { fetchQuestionBankCourseCatalog } from '../../api/academy'
import { resolveResourceUrl } from '../../api/request'

/** 路由实例 */
const router = useRouter()

/** 当前路由信息 */
const route = useRoute()

/** 题库分类列表 */
const categories = ref([])

/** 当前激活的分类编码 */
const activeCategoryCode = ref('')

/** 加载状态 */
const loading = ref(false)

/** 错误提示信息 */
const errorMessage = ref('')

/** 当前激活的分类对象 */
const activeCategory = computed(() => {
  return categories.value.find((category) => category.code === activeCategoryCode.value) || categories.value[0]
})

/** 总题库数量 */
const totalSets = computed(() => {
  return categories.value.reduce((sum, category) => sum + (category.sets?.length || 0), 0)
})

/** 已接入题库数量 */
const readySets = computed(() => {
  return categories.value
    .flatMap((category) => category.sets || [])
    .filter((item) => item.routePath && item.questionCount > 0).length
})

/** 从路由参数同步激活分类 */
const syncActiveCategoryFromRoute = () => {
  const categoryCode = String(route.query.category || '')
  activeCategoryCode.value = categories.value.some((category) => category.code === categoryCode)
    ? categoryCode
    : categories.value[0]?.code || ''
}

/** 加载课程题库目录数据 */
const loadCatalog = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await fetchQuestionBankCourseCatalog()
    categories.value = data || []
    syncActiveCategoryFromRoute()
  } catch (error) {
    errorMessage.value = error.message || '课程题库目录加载失败'
  } finally {
    loading.value = false
  }
}

/** 设置激活分类并更新路由参数 */
const setActiveCategory = (categoryCode) => {
  activeCategoryCode.value = categoryCode
  router.replace({
    query: {
      ...route.query,
      category: categoryCode,
    },
  })
}

/** 获取题库封面图 URL */
const coverSrc = (set) => {
  return resolveResourceUrl(set.coverUrl || set.fallbackCoverUrl || '')
}

/** 处理封面图加载失败，切换备用封面 */
const handleCoverError = (event, set) => {
  if (!set.fallbackCoverUrl || event.currentTarget.dataset.fallbackApplied === 'true') {
    return
  }
  event.currentTarget.dataset.fallbackApplied = 'true'
  event.currentTarget.src = set.fallbackCoverUrl
}

/** 打开题库详情页 */
const openSet = (set) => {
  if (set.routePath) {
    router.push(set.routePath)
    return
  }
  // TODO: 接入未完成题库，例如 /api/academy/question-bank/courses/{code}
  console.info('course question bank action reserved:', set.code)
}

/** 监听路由参数变化，同步分类状态 */
watch(
  () => route.query.category,
  () => {
    if (categories.value.length) {
      syncActiveCategoryFromRoute()
    }
  },
)

/** 组件挂载时加载目录数据 */
onMounted(loadCatalog)
</script>

<template>
  <main class="academy-main question-course-main question-catalog-main">
    <!-- 面包屑导航 -->
    <nav class="question-course-breadcrumb" aria-label="题库面包屑">
      <RouterLink to="/academy/home">在线学堂</RouterLink>
      <span>&gt;</span>
      <RouterLink to="/academy/question-bank">题库</RouterLink>
      <span>&gt;</span>
      <strong>课程题库</strong>
    </nav>

    <!-- 头部区域：标题和统计数据 -->
    <section class="question-catalog-hero">
      <div>
        <h1>课程题库首页</h1>
      </div>
      <!-- 目录统计数据 -->
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

    <!-- 错误提示 -->
    <p v-if="errorMessage" class="question-course-message is-error">{{ errorMessage }}</p>

    <!-- 题库目录主体区域 -->
    <section class="question-catalog-shell" aria-label="课程题库目录">
      <!-- 分类标签页 -->
      <div class="question-catalog-tabs">
        <button
          v-for="category in categories"
          :key="category.code"
          type="button"
          :class="{ 'is-active': category.code === activeCategoryCode }"
          @click="setActiveCategory(category.code)"
        >
          <strong>{{ category.name }}</strong>
          <span>{{ category.sets?.length || 0 }} 个方向</span>
        </button>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="question-course-empty">正在加载课程题库...</div>

      <!-- 空状态 -->
      <div v-else-if="!activeCategory" class="question-course-empty">暂无课程题库目录。</div>

      <!-- 题库列表 -->
      <section v-else class="question-catalog-section">
        <header>
          <div>
            <h2>{{ activeCategory.name }}</h2>
            <p>{{ activeCategory.description }}</p>
          </div>
          <span>{{ activeCategory.sets?.length || 0 }} 个小类</span>
        </header>

        <!-- 题库卡片网格 -->
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
