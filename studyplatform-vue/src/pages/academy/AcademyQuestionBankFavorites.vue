<script setup>
/**
 * 收藏题目组件
 * 展示用户收藏的题目列表，支持按题库筛选、搜索和取消收藏
 */
import { computed, onMounted, ref, watch } from 'vue'
import { StarFilled } from '@element-plus/icons-vue'
import { RouterLink, useRoute } from 'vue-router'
import {
  fetchQuestionBankFavorites,
  fetchQuestionBankFavoriteSummary,
  removeQuestionBankFavorite,
} from '../../api/academy'
import { useLearningTimeTracker } from '../../composables/useLearningTimeTracker'

/** 当前路由信息 */
const route = useRoute()

/** 学习时间追踪器 */
useLearningTimeTracker({
  moduleType: 'favorite',
  targetCode: () => setCode.value || 'all',
  targetTitle: '收藏题目复习',
})

/** 收藏统计摘要 */
const summary = ref({
  total: 0,
  sets: [],
})

/** 收藏分页数据 */
const favoritePage = ref(null)

/** 加载状态 */
const loading = ref(false)

/** 错误提示信息 */
const errorMessage = ref('')

/** 取消收藏中的题目 ID 集合 */
const removingFavorites = ref({})

/** 当前页码 */
const page = ref(0)

/** 每页大小 */
const pageSize = ref(20)

/** 跳转页码输入框值 */
const pageJumpInput = ref('1')

/** 搜索关键词 */
const keyword = ref('')

/** 搜索关键词输入框值 */
const keywordInput = ref('')

/** 题库编码筛选 */
const setCode = ref('')

/** 题目列表容器引用 */
const questionListRef = ref(null)

/** 收藏题目列表 */
const favorites = computed(() => favoritePage.value?.items || [])

/** 收藏总数 */
const total = computed(() => favoritePage.value?.total ?? 0)

/** 总页数 */
const totalPages = computed(() => favoritePage.value?.totalPages || 0)

/** 响应返回的每页大小 */
const responsePageSize = computed(() => favoritePage.value?.size || pageSize.value)

/** 安全的总页数 */
const safeTotalPages = computed(() => Math.max(totalPages.value || 1, 1))

/** 当前页起始序号 */
const pageStart = computed(() => (total.value ? page.value * responsePageSize.value + 1 : 0))

/** 当前页结束序号 */
const pageEnd = computed(() => Math.min(total.value, page.value * responsePageSize.value + favorites.value.length))

/** 获取题目类型标签 */
const questionTypeLabel = (type) => {
  const labels = {
    single: '单选题',
    multiple: '多选题',
    short: '应用题',
    vocabulary: '词汇卡片',
  }
  return labels[type] || '题目'
}

/** 格式化日期时间 */
const formatDateTime = (value) => {
  if (!value) return '暂无记录'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '暂无记录'
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

/** 加载收藏统计摘要 */
const loadSummary = async () => {
  try {
    summary.value = await fetchQuestionBankFavoriteSummary()
  } catch (error) {
    console.warn('failed to load question bank favorite summary:', error)
  }
}

/** 加载收藏题目列表 */
const loadFavorites = async (shouldScroll = false) => {
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await fetchQuestionBankFavorites({
      setCode: setCode.value,
      keyword: keyword.value,
      page: page.value,
      size: pageSize.value,
    })
    favoritePage.value = data
    page.value = data?.page ?? page.value
    pageSize.value = data?.size ?? pageSize.value
    pageJumpInput.value = String(page.value + 1)
    if (shouldScroll) {
      scrollToFirstFavorite()
    }
  } catch (error) {
    errorMessage.value = error.message || '收藏题目加载失败'
  } finally {
    loading.value = false
  }
}

/** 取消题目收藏 */
const removeFavorite = async (question) => {
  if (!question?.questionId || removingFavorites.value[question.questionId]) return
  removingFavorites.value = {
    ...removingFavorites.value,
    [question.questionId]: true,
  }
  try {
    await removeQuestionBankFavorite(question.questionId)
    await Promise.all([loadSummary(), loadFavorites()])
  } catch (error) {
    errorMessage.value = error.message || '取消收藏失败'
  } finally {
    const nextRemoving = { ...removingFavorites.value }
    delete nextRemoving[question.questionId]
    removingFavorites.value = nextRemoving
  }
}

/** 提交搜索 */
const submitSearch = () => {
  keyword.value = keywordInput.value.trim()
  page.value = 0
  loadFavorites()
}

/** 应用筛选条件 */
const applyFilters = () => {
  page.value = 0
  loadFavorites()
}

/** 清空筛选条件 */
const clearFilters = () => {
  keyword.value = ''
  keywordInput.value = ''
  setCode.value = ''
  page.value = 0
  loadFavorites()
}

/** 滚动到第一个收藏题目位置 */
const scrollToFirstFavorite = () => {
  requestAnimationFrame(() => {
    const target = questionListRef.value?.querySelector('.question-bank-question-card, .question-course-empty')
    ;(target || questionListRef.value)?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    })
  })
}

/** 跳转到指定页码 */
const goToPage = (nextPage, shouldScroll = false) => {
  if (nextPage < 0 || (totalPages.value && nextPage >= totalPages.value)) {
    return
  }
  page.value = nextPage
  loadFavorites(shouldScroll)
}

/** 跳转到输入的页码 */
const jumpToPage = () => {
  const requestedPage = Number(pageJumpInput.value)
  if (!Number.isFinite(requestedPage)) {
    pageJumpInput.value = String(page.value + 1)
    return
  }
  const targetPage = Math.min(Math.max(Math.trunc(requestedPage), 1), safeTotalPages.value) - 1
  goToPage(targetPage, true)
}

/** 监听路由参数变化，同步题库编码 */
watch(
  () => route.query.setCode,
  (nextSetCode) => {
    setCode.value = typeof nextSetCode === 'string' ? nextSetCode : ''
    page.value = 0
    loadFavorites()
  },
)

/** 组件挂载时加载统计和收藏数据 */
onMounted(() => {
  setCode.value = typeof route.query.setCode === 'string' ? route.query.setCode : ''
  loadSummary()
  loadFavorites()
})
</script>

<template>
  <main class="academy-main question-course-main question-bank-detail-main">
    <!-- 面包屑导航 -->
    <nav class="question-course-breadcrumb" aria-label="题库面包屑">
      <RouterLink to="/academy/home">在线学堂</RouterLink>
      <span>&gt;</span>
      <RouterLink to="/academy/question-bank">题库</RouterLink>
      <span>&gt;</span>
      <strong>收藏题目</strong>
    </nav>

    <!-- 头部区域：标题和统计数据 -->
    <section class="question-bank-mistake-hero">
      <div>
        <h1>收藏题目</h1>
        <span>把重点题目、词汇卡片和高频题型放进这里，之后可以按题库快速复盘。</span>
      </div>
      <!-- 收藏统计数据 -->
      <div class="question-bank-stats" aria-label="收藏统计">
        <div>
          <strong>{{ summary.total || 0 }}</strong>
          <span>收藏总数</span>
        </div>
        <div>
          <strong>{{ summary.sets?.length || 0 }}</strong>
          <span>涉及题库</span>
        </div>
        <div>
          <strong>{{ total || 0 }}</strong>
          <span>当前筛选</span>
        </div>
      </div>
    </section>

    <!-- 错误提示 -->
    <p v-if="errorMessage" class="question-course-message is-error">{{ errorMessage }}</p>

    <!-- 收藏列表区域 -->
    <section ref="questionListRef" class="question-bank-question-list" aria-label="收藏题目列表">
      <!-- 列表头部：标题和筛选 -->
      <header class="question-bank-list-header">
        <div>
          <h2>收藏列表</h2>
          <p>可以按题库或关键词筛选收藏题目，取消收藏后会立即从列表中移除。</p>
        </div>
        <!-- 筛选表单 -->
        <form class="question-bank-filter question-bank-mistake-filter" @submit.prevent="submitSearch">
          <!-- 题库筛选 -->
          <select v-model="setCode" @change="applyFilters">
            <option value="">全部题库</option>
            <option v-for="item in summary.sets" :key="item.setCode" :value="item.setCode">
              {{ item.setTitle }}（{{ item.total }}）
            </option>
          </select>
          <!-- 关键词搜索 -->
          <input v-model="keywordInput" type="search" placeholder="搜索题干、答案或题库" />
          <button type="submit">搜索</button>
          <button type="button" @click="clearFilters">重置</button>
        </form>
      </header>

      <!-- 加载状态 -->
      <div v-if="loading" class="question-course-empty">正在加载收藏题目...</div>

      <!-- 空状态 -->
      <div v-else-if="!favorites.length" class="question-course-empty">
        当前没有收藏题目。去课程题库点一下“收藏”，这里就会长出你的重点清单。
      </div>

      <!-- 收藏列表内容 -->
      <template v-else>
        <article v-for="(question, index) in favorites" :key="question.id" class="question-bank-question-card">
          <!-- 题目头部信息 -->
          <div class="question-bank-question-head">
            <span>{{ page * responsePageSize + index + 1 }}</span>
            <div class="question-bank-question-title">
              <div>
                <p>{{ question.categoryName }} · {{ question.setTitle }} · {{ questionTypeLabel(question.type) }}</p>
                <h3>{{ question.stem }}</h3>
                <!-- 收藏元信息 -->
                <div class="question-bank-mistake-meta">
                  <strong>收藏于 {{ formatDateTime(question.createdAt) }}</strong>
                  <strong>{{ question.difficultyLabel || '默认难度' }}</strong>
                </div>
              </div>
              <!-- 取消收藏按钮 -->
              <button
                type="button"
                class="question-bank-favorite-button is-favorite"
                :disabled="removingFavorites[question.questionId]"
                aria-label="取消收藏"
                title="取消收藏"
                @click="removeFavorite(question)"
              >
                <StarFilled aria-hidden="true" />
              </button>
            </div>
          </div>

          <!-- 选择题选项 -->
          <ul v-if="question.options?.length" class="question-bank-favorite-options">
            <li v-for="option in question.options" :key="option">{{ option }}</li>
          </ul>

          <!-- 答案和解析 -->
          <div class="question-bank-answer">
            <strong>参考答案：{{ question.answer || '暂无' }}</strong>
            <p>{{ question.explanation || '暂无解析。' }}</p>
            <p>
              <RouterLink :to="`/academy/question-bank/courses/${question.setCode}`">
                返回 {{ question.setTitle }} 继续练习
              </RouterLink>
            </p>
          </div>
        </article>
      </template>

      <!-- 分页导航 -->
      <footer class="question-bank-pagination">
        <span>当前 {{ pageStart }}-{{ pageEnd }} / 共 {{ total }} 题</span>
        <div>
          <button type="button" :disabled="page <= 0" @click="goToPage(page - 1)">上一页</button>
          <strong>第 {{ page + 1 }} / {{ safeTotalPages }} 页</strong>
          <button type="button" :disabled="page >= safeTotalPages - 1" @click="goToPage(page + 1)">
            下一页
          </button>
        </div>
        <!-- 跳转页码表单 -->
        <form class="question-bank-page-jump" @submit.prevent="jumpToPage">
          <label for="question-bank-favorite-page-jump">跳转到</label>
          <input
            id="question-bank-favorite-page-jump"
            v-model="pageJumpInput"
            type="number"
            min="1"
            :max="safeTotalPages"
            inputmode="numeric"
          />
          <span>页</span>
          <button type="submit">跳转</button>
        </form>
      </footer>
    </section>
  </main>
</template>
