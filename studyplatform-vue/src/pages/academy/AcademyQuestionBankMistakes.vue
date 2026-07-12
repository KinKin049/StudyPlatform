<script setup>
/**
 * 错题本组件
 * 展示用户错题列表，支持按题库、状态筛选，错题复习和标记掌握
 */
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import {
  fetchQuestionBankMistakes,
  fetchQuestionBankMistakeSummary,
  recordQuestionBankAnswer,
} from '../../api/academy'
import { useLearningTimeTracker } from '../../composables/useLearningTimeTracker'

/** 当前路由信息 */
const route = useRoute()

/** 学习时间追踪器 */
useLearningTimeTracker({
  moduleType: 'mistake',
  targetCode: () => setCode.value || 'all',
  targetTitle: '错题本复习',
})

/** 错题统计摘要 */
const summary = ref({
  total: 0,
  active: 0,
  mastered: 0,
  sets: [],
})

/** 错题分页数据 */
const mistakePage = ref(null)

/** 加载状态 */
const loading = ref(false)

/** 错误提示信息 */
const errorMessage = ref('')

/** 用户选中的选项 */
const selectedOptions = ref({})

/** 答题结果状态 */
const answerStates = ref({})

/** 已展开答案的题目 ID 集合 */
const openedAnswers = ref(new Set())

/** 提交中的题目 ID 集合 */
const submittingAnswers = ref({})

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

/** 状态筛选：active-待复习，mastered-已掌握，all-全部 */
const status = ref('active')

/** 题库编码筛选 */
const setCode = ref('')

/** 题目列表容器引用 */
const questionListRef = ref(null)

/** 错题列表 */
const mistakes = computed(() => mistakePage.value?.items || [])

/** 错题总数 */
const total = computed(() => mistakePage.value?.total ?? 0)

/** 总页数 */
const totalPages = computed(() => mistakePage.value?.totalPages || 0)

/** 响应返回的每页大小 */
const responsePageSize = computed(() => mistakePage.value?.size || pageSize.value)

/** 安全的总页数 */
const safeTotalPages = computed(() => Math.max(totalPages.value || 1, 1))

/** 当前页起始序号 */
const pageStart = computed(() => (total.value ? page.value * responsePageSize.value + 1 : 0))

/** 当前页结束序号 */
const pageEnd = computed(() => Math.min(total.value, page.value * responsePageSize.value + mistakes.value.length))

/** 状态筛选选项 */
const statusOptions = [
  { value: 'active', label: '待复习' },
  { value: 'mastered', label: '已掌握' },
  { value: 'all', label: '全部错题' },
]

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

/** 从选项文本中提取选项键 */
const optionKey = (option) => {
  const match = String(option || '').match(/^\s*([A-Z])[\.\、]/i)
  return match ? match[1].toUpperCase() : String(option || '').trim()
}

/** 将答案字符串解析为选项键数组 */
const answerKeys = (question) => {
  const keys = String(question.answer || '')
    .split(/[,，、\s]+/)
    .map((item) => item.trim().toUpperCase())
    .filter(Boolean)
  if (keys.length === 1 && /^[A-Z]{2,}$/.test(keys[0])) {
    return keys[0].split('')
  }
  return keys
}

/** 获取题目已选中的选项键数组 */
const selectedOptionKeys = (question) => {
  const selected = selectedOptions.value[question.questionId]
  if (Array.isArray(selected)) {
    return selected
  }
  return selected ? [selected] : []
}

/** 判断是否为多选题 */
const isMultipleQuestion = (question) => question.type === 'multiple' || answerKeys(question).length > 1

/** 判断选项是否被选中 */
const isOptionSelected = (question, option) => {
  return selectedOptionKeys(question).includes(optionKey(option))
}

/** 判断两个键数组是否相等 */
const hasSameKeys = (left, right) => {
  return left.length === right.length && left.every((item) => right.includes(item))
}

/** 判断题目答案是否正确 */
const isQuestionCorrect = (question) => {
  const answerState = answerStates.value[question.questionId]
  if (answerState) {
    return answerState.correct
  }
  return hasSameKeys(selectedOptionKeys(question), answerKeys(question))
}

/** 判断题目是否已作答 */
const hasAnswered = (question) => {
  return Boolean(answerStates.value[question.questionId]) || selectedOptionKeys(question).length > 0
}

/** 判断选项是否为正确答案 */
const isOptionCorrect = (question, option) => {
  return answerKeys(question).includes(optionKey(option))
}

/** 获取错题答案展开状态使用的稳定 ID */
const answerToggleId = (question) => question?.questionId || question?.id

/** 判断题目答案是否已展开 */
const isAnswerOpened = (question) => openedAnswers.value.has(answerToggleId(question))

/** 切换题目答案展开状态 */
const toggleAnswer = (question) => {
  const id = answerToggleId(question)
  if (!id) return
  const next = new Set(openedAnswers.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  openedAnswers.value = next
}

/** 判断是否应该记录选择题作答 */
const shouldRecordChoiceAnswer = (question, selectedKeys) => {
  const correctKeys = answerKeys(question)
  if (!selectedKeys.length || !correctKeys.length) {
    return false
  }
  if (!isMultipleQuestion(question)) {
    return true
  }
  const hasWrongKey = selectedKeys.some((key) => !correctKeys.includes(key))
  return hasWrongKey || selectedKeys.length >= correctKeys.length
}

/** 更新错题状态 */
const updateMistakeState = (questionId, result) => {
  if (!mistakePage.value?.items) {
    return
  }
  mistakePage.value = {
    ...mistakePage.value,
    items: mistakePage.value.items.map((item) =>
      item.questionId === questionId
        ? {
            ...item,
            wrongCount: result.wrongCount,
            correctStreak: result.correctStreak,
            mastered: result.mastered,
          }
        : item,
    ),
  }
}

/** 提交错题答案 */
const submitAnswer = async (question, selectedAnswer) => {
  if (!question?.questionId || submittingAnswers.value[question.questionId]) {
    return
  }
  submittingAnswers.value = {
    ...submittingAnswers.value,
    [question.questionId]: true,
  }
  try {
    const result = await recordQuestionBankAnswer({
      questionId: question.questionId,
      selectedAnswer,
    })
    answerStates.value = {
      ...answerStates.value,
      [question.questionId]: result,
    }
    updateMistakeState(question.questionId, result)
    fetchQuestionBankMistakeSummary().then((data) => {
      summary.value = data
    }).catch((error) => {
      console.warn('failed to refresh mistake summary:', error)
    })
  } catch (error) {
    console.warn('failed to submit mistake review answer:', error)
    errorMessage.value = error.message || '错题复习提交失败'
  } finally {
    const nextSubmitting = { ...submittingAnswers.value }
    delete nextSubmitting[question.questionId]
    submittingAnswers.value = nextSubmitting
  }
}

/** 选择题目选项 */
const selectOption = (question, option) => {
  const key = optionKey(option)
  if (isMultipleQuestion(question)) {
    const selected = selectedOptionKeys(question)
    const nextSelected = selected.includes(key)
      ? selected.filter((item) => item !== key)
      : [...selected, key]
    selectedOptions.value = {
      ...selectedOptions.value,
      [question.questionId]: nextSelected,
    }
    if (shouldRecordChoiceAnswer(question, nextSelected)) {
      submitAnswer(question, nextSelected.join(','))
    }
    return
  }
  selectedOptions.value = {
    ...selectedOptions.value,
    [question.questionId]: key,
  }
  submitAnswer(question, key)
}

/** 标记词汇掌握状态 */
const markVocabulary = (question, value) => {
  selectedOptions.value = {
    ...selectedOptions.value,
    [question.questionId]: value,
  }
  submitAnswer(question, value)
}

/** 滚动到第一个错题位置 */
const scrollToFirstMistake = () => {
  requestAnimationFrame(() => {
    const target = questionListRef.value?.querySelector('.question-bank-question-card, .question-course-empty')
    ;(target || questionListRef.value)?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    })
  })
}

/** 加载错题统计摘要 */
const loadSummary = async () => {
  try {
    summary.value = await fetchQuestionBankMistakeSummary()
  } catch (error) {
    console.warn('failed to load question bank mistake summary:', error)
  }
}

/** 加载错题列表数据 */
const loadMistakes = async (shouldScroll = false) => {
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await fetchQuestionBankMistakes({
      setCode: setCode.value,
      status: status.value,
      keyword: keyword.value,
      page: page.value,
      size: pageSize.value,
    })
    mistakePage.value = data
    page.value = data?.page ?? page.value
    pageSize.value = data?.size ?? pageSize.value
    pageJumpInput.value = String(page.value + 1)
    selectedOptions.value = {}
    answerStates.value = {}
    openedAnswers.value = new Set()
    if (shouldScroll) {
      scrollToFirstMistake()
    }
  } catch (error) {
    errorMessage.value = error.message || '错题本加载失败'
  } finally {
    loading.value = false
  }
}

/** 提交搜索 */
const submitSearch = () => {
  keyword.value = keywordInput.value.trim()
  page.value = 0
  loadMistakes()
}

/** 应用筛选条件 */
const applyFilters = () => {
  page.value = 0
  loadMistakes()
}

/** 清空筛选条件 */
const clearFilters = () => {
  keyword.value = ''
  keywordInput.value = ''
  status.value = 'active'
  setCode.value = ''
  page.value = 0
  loadMistakes()
}

/** 跳转到指定页码 */
const goToPage = (nextPage, shouldScroll = false) => {
  if (nextPage < 0 || (totalPages.value && nextPage >= totalPages.value)) {
    return
  }
  page.value = nextPage
  loadMistakes(shouldScroll)
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
    loadMistakes()
  },
)

/** 组件挂载时加载统计和错题数据 */
onMounted(() => {
  setCode.value = typeof route.query.setCode === 'string' ? route.query.setCode : ''
  loadSummary()
  loadMistakes()
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
      <strong>错题本</strong>
    </nav>

    <!-- 头部区域：标题和统计数据 -->
    <section class="question-bank-mistake-hero">
      <div>
        <h1>我的错题本</h1>
        <span>自动收集课程题库中的错误答案；连续答对 2 次后，错题会标记为已掌握。</span>
      </div>
      <!-- 错题统计数据 -->
      <div class="question-bank-stats" aria-label="错题本统计">
        <div>
          <strong>{{ summary.active || 0 }}</strong>
          <span>待复习</span>
        </div>
        <div>
          <strong>{{ summary.mastered || 0 }}</strong>
          <span>已掌握</span>
        </div>
        <div>
          <strong>{{ summary.total || 0 }}</strong>
          <span>累计错题</span>
        </div>
      </div>
    </section>

    <!-- 错误提示 -->
    <p v-if="errorMessage" class="question-course-message is-error">{{ errorMessage }}</p>

    <!-- 错题列表区域 -->
    <section ref="questionListRef" class="question-bank-question-list" aria-label="错题列表">
      <!-- 列表头部：标题和筛选 -->
      <header class="question-bank-list-header question-bank-mistake-list-header">
        <div class="question-bank-mistake-list-title">
          <h2>错题复习</h2>
          <p>按题库、状态和关键词筛选错题，也可以直接开始当前筛选范围的复习。</p>
        </div>
        <!-- 筛选表单 -->
        <form class="question-bank-filter question-bank-mistake-filter" @submit.prevent="submitSearch">
          <!-- 题库筛选 -->
          <select v-model="setCode" @change="applyFilters">
            <option value="">全部题库</option>
            <option v-for="item in summary.sets" :key="item.setCode" :value="item.setCode">
              {{ item.setTitle }}（{{ item.active }}）
            </option>
          </select>
          <!-- 状态筛选 -->
          <select v-model="status" @change="applyFilters">
            <option v-for="item in statusOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </option>
          </select>
          <!-- 关键词搜索 -->
          <input v-model="keywordInput" type="search" placeholder="搜索题干、答案或题库" />
          <button type="submit">搜索</button>
          <button type="button" @click="clearFilters">重置</button>
          <button type="button" @click="scrollToFirstMistake">重新练习错题</button>
        </form>
      </header>

      <!-- 加载状态 -->
      <div v-if="loading" class="question-course-empty">正在加载错题...</div>

      <!-- 空状态 -->
      <div v-else-if="!mistakes.length" class="question-course-empty">
        当前筛选范围内没有错题。去课程题库练几道题，小本本就会自己长出来。
      </div>

      <!-- 错题列表内容 -->
      <template v-else>
        <article v-for="(question, index) in mistakes" :key="question.id" class="question-bank-question-card">
          <!-- 题目头部信息 -->
          <div class="question-bank-question-head">
            <span>{{ page * responsePageSize + index + 1 }}</span>
            <div>
              <p>{{ question.categoryName }} · {{ question.setTitle }} · {{ questionTypeLabel(question.type) }}</p>
              <h3>{{ question.stem }}</h3>
              <!-- 错题元信息 -->
              <div class="question-bank-mistake-meta">
                <strong>错误 {{ question.wrongCount }} 次</strong>
                <strong>连续答对 {{ question.correctStreak }} 次</strong>
                <strong>最近错误 {{ formatDateTime(question.lastWrongAt) }}</strong>
                <strong :class="{ 'is-mastered': question.mastered }">
                  {{ question.mastered ? '已掌握' : '待复习' }}
                </strong>
              </div>
              <!-- 错题快照 -->
              <div class="question-bank-mistake-snapshot">
                <span>上次错选：{{ question.selectedAnswer || '未记录' }}</span>
                <span v-if="isAnswerOpened(question)">正确答案：{{ question.correctAnswer || question.answer || '暂无' }}</span>
              </div>
            </div>
          </div>

          <button type="button" class="question-bank-answer-toggle" @click="toggleAnswer(question)">
            {{ isAnswerOpened(question) ? '隐藏答案' : '查看答案' }}
          </button>

          <!-- 选择题选项 -->
          <ul v-if="question.options?.length" class="question-bank-options">
            <li v-for="option in question.options" :key="option">
              <button
                type="button"
                :aria-pressed="isOptionSelected(question, option)"
                :disabled="submittingAnswers[question.questionId]"
                :class="{
                  'is-selected': isOptionSelected(question, option),
                  'is-correct':
                    isAnswerOpened(question) &&
                    hasAnswered(question) &&
                    isOptionCorrect(question, option) &&
                    (isQuestionCorrect(question) || !isOptionSelected(question, option)),
                  'is-wrong':
                    isAnswerOpened(question) &&
                    hasAnswered(question) &&
                    !isQuestionCorrect(question) &&
                    isOptionSelected(question, option) &&
                    !isOptionCorrect(question, option),
                }"
                @click="selectOption(question, option)"
              >
                {{ option }}
              </button>
            </li>
          </ul>

          <!-- 词汇错题 -->
          <div v-else-if="question.type === 'vocabulary'" class="question-bank-mistake-vocabulary">
            <template v-if="isAnswerOpened(question)">
              <strong>{{ question.answer }}</strong>
              <p>{{ question.explanation }}</p>
            </template>
            <div>
              <button type="button" @click="markVocabulary(question, 'known')">认识</button>
              <button type="button" @click="markVocabulary(question, 'fuzzy')">模糊</button>
              <button type="button" @click="markVocabulary(question, 'unknown')">不认识</button>
            </div>
          </div>

          <!-- 非选择题答案 -->
          <div v-else-if="isAnswerOpened(question)" class="question-bank-answer">
            <strong>参考答案：{{ question.answer }}</strong>
            <p>{{ question.explanation }}</p>
          </div>

          <!-- 选择题答案判定 -->
          <div
            v-if="question.options?.length && hasAnswered(question) && isAnswerOpened(question)"
            class="question-bank-answer"
            :class="{
              'is-correct': isQuestionCorrect(question),
              'is-wrong': !isQuestionCorrect(question),
            }"
          >
            <strong>{{ isQuestionCorrect(question) ? '回答正确' : '回答错误' }}</strong>
            <p>标准答案：{{ question.answer }}</p>
            <p>{{ question.explanation }}</p>
            <p v-if="answerStates[question.questionId]">{{ answerStates[question.questionId].message }}</p>
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
          <label for="question-bank-mistake-page-jump">跳转到</label>
          <input
            id="question-bank-mistake-page-jump"
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
