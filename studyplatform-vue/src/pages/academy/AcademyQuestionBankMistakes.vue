<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import {
  fetchQuestionBankMistakes,
  fetchQuestionBankMistakeSummary,
  recordQuestionBankAnswer,
} from '../../api/academy'

const route = useRoute()

const summary = ref({
  total: 0,
  active: 0,
  mastered: 0,
  sets: [],
})
const mistakePage = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const selectedOptions = ref({})
const answerStates = ref({})
const submittingAnswers = ref({})
const page = ref(0)
const pageSize = ref(20)
const pageJumpInput = ref('1')
const keyword = ref('')
const keywordInput = ref('')
const status = ref('active')
const setCode = ref('')
const questionListRef = ref(null)

const mistakes = computed(() => mistakePage.value?.items || [])
const total = computed(() => mistakePage.value?.total ?? 0)
const totalPages = computed(() => mistakePage.value?.totalPages || 0)
const responsePageSize = computed(() => mistakePage.value?.size || pageSize.value)
const safeTotalPages = computed(() => Math.max(totalPages.value || 1, 1))
const pageStart = computed(() => (total.value ? page.value * responsePageSize.value + 1 : 0))
const pageEnd = computed(() => Math.min(total.value, page.value * responsePageSize.value + mistakes.value.length))

const statusOptions = [
  { value: 'active', label: '待复习' },
  { value: 'mastered', label: '已掌握' },
  { value: 'all', label: '全部错题' },
]

const questionTypeLabel = (type) => {
  const labels = {
    single: '单选题',
    multiple: '多选题',
    short: '应用题',
    vocabulary: '词汇卡片',
  }
  return labels[type] || '题目'
}

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

const optionKey = (option) => {
  const match = String(option || '').match(/^\s*([A-Z])[\.\、]/i)
  return match ? match[1].toUpperCase() : String(option || '').trim()
}

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

const selectedOptionKeys = (question) => {
  const selected = selectedOptions.value[question.questionId]
  if (Array.isArray(selected)) {
    return selected
  }
  return selected ? [selected] : []
}

const isMultipleQuestion = (question) => question.type === 'multiple' || answerKeys(question).length > 1

const isOptionSelected = (question, option) => {
  return selectedOptionKeys(question).includes(optionKey(option))
}

const hasSameKeys = (left, right) => {
  return left.length === right.length && left.every((item) => right.includes(item))
}

const isQuestionCorrect = (question) => {
  const answerState = answerStates.value[question.questionId]
  if (answerState) {
    return answerState.correct
  }
  return hasSameKeys(selectedOptionKeys(question), answerKeys(question))
}

const hasAnswered = (question) => {
  return Boolean(answerStates.value[question.questionId]) || selectedOptionKeys(question).length > 0
}

const isOptionCorrect = (question, option) => {
  return answerKeys(question).includes(optionKey(option))
}

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

const markVocabulary = (question, value) => {
  selectedOptions.value = {
    ...selectedOptions.value,
    [question.questionId]: value,
  }
  submitAnswer(question, value)
}

const scrollToFirstMistake = () => {
  requestAnimationFrame(() => {
    const target = questionListRef.value?.querySelector('.question-bank-question-card, .question-course-empty')
    ;(target || questionListRef.value)?.scrollIntoView({
      behavior: 'smooth',
      block: 'start',
    })
  })
}

const loadSummary = async () => {
  try {
    summary.value = await fetchQuestionBankMistakeSummary()
  } catch (error) {
    console.warn('failed to load question bank mistake summary:', error)
  }
}

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
    if (shouldScroll) {
      scrollToFirstMistake()
    }
  } catch (error) {
    errorMessage.value = error.message || '错题本加载失败'
  } finally {
    loading.value = false
  }
}

const submitSearch = () => {
  keyword.value = keywordInput.value.trim()
  page.value = 0
  loadMistakes()
}

const applyFilters = () => {
  page.value = 0
  loadMistakes()
}

const clearFilters = () => {
  keyword.value = ''
  keywordInput.value = ''
  status.value = 'active'
  setCode.value = ''
  page.value = 0
  loadMistakes()
}

const goToPage = (nextPage, shouldScroll = false) => {
  if (nextPage < 0 || (totalPages.value && nextPage >= totalPages.value)) {
    return
  }
  page.value = nextPage
  loadMistakes(shouldScroll)
}

const jumpToPage = () => {
  const requestedPage = Number(pageJumpInput.value)
  if (!Number.isFinite(requestedPage)) {
    pageJumpInput.value = String(page.value + 1)
    return
  }
  const targetPage = Math.min(Math.max(Math.trunc(requestedPage), 1), safeTotalPages.value) - 1
  goToPage(targetPage, true)
}

watch(
  () => route.query.setCode,
  (nextSetCode) => {
    setCode.value = typeof nextSetCode === 'string' ? nextSetCode : ''
    page.value = 0
    loadMistakes()
  },
)

onMounted(() => {
  setCode.value = typeof route.query.setCode === 'string' ? route.query.setCode : ''
  loadSummary()
  loadMistakes()
})
</script>

<template>
  <main class="academy-main question-course-main question-bank-detail-main">
    <nav class="question-course-breadcrumb" aria-label="题库面包屑">
      <RouterLink to="/academy/home">在线学堂</RouterLink>
      <span>&gt;</span>
      <RouterLink to="/academy/question-bank">题库</RouterLink>
      <span>&gt;</span>
      <strong>错题本</strong>
    </nav>

    <section class="question-bank-mistake-hero">
      <div>
        <h1>我的错题本</h1>
        <span>自动收集课程题库中的错误答案；连续答对 2 次后，错题会标记为已掌握。</span>
      </div>
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

    <p v-if="errorMessage" class="question-course-message is-error">{{ errorMessage }}</p>

    <section ref="questionListRef" class="question-bank-question-list" aria-label="错题列表">
      <header class="question-bank-list-header question-bank-mistake-list-header">
        <div class="question-bank-mistake-list-title">
          <h2>错题复习</h2>
          <p>按题库、状态和关键词筛选错题，也可以直接开始当前筛选范围的复习。</p>
        </div>
        <form class="question-bank-filter question-bank-mistake-filter" @submit.prevent="submitSearch">
          <select v-model="setCode" @change="applyFilters">
            <option value="">全部题库</option>
            <option v-for="item in summary.sets" :key="item.setCode" :value="item.setCode">
              {{ item.setTitle }}（{{ item.active }}）
            </option>
          </select>
          <select v-model="status" @change="applyFilters">
            <option v-for="item in statusOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </option>
          </select>
          <input v-model="keywordInput" type="search" placeholder="搜索题干、答案或题库" />
          <button type="submit">搜索</button>
          <button type="button" @click="clearFilters">重置</button>
          <button type="button" @click="scrollToFirstMistake">重新练习错题</button>
        </form>
      </header>

      <div v-if="loading" class="question-course-empty">正在加载错题...</div>

      <div v-else-if="!mistakes.length" class="question-course-empty">
        当前筛选范围内没有错题。去课程题库练几道题，小本本就会自己长出来。
      </div>

      <template v-else>
        <article v-for="(question, index) in mistakes" :key="question.id" class="question-bank-question-card">
          <div class="question-bank-question-head">
            <span>{{ page * responsePageSize + index + 1 }}</span>
            <div>
              <p>{{ question.categoryName }} · {{ question.setTitle }} · {{ questionTypeLabel(question.type) }}</p>
              <h3>{{ question.stem }}</h3>
              <div class="question-bank-mistake-meta">
                <strong>错误 {{ question.wrongCount }} 次</strong>
                <strong>连续答对 {{ question.correctStreak }} 次</strong>
                <strong>最近错误 {{ formatDateTime(question.lastWrongAt) }}</strong>
                <strong :class="{ 'is-mastered': question.mastered }">
                  {{ question.mastered ? '已掌握' : '待复习' }}
                </strong>
              </div>
              <div class="question-bank-mistake-snapshot">
                <span>上次错选：{{ question.selectedAnswer || '未记录' }}</span>
                <span>正确答案：{{ question.correctAnswer || question.answer || '暂无' }}</span>
              </div>
            </div>
          </div>

          <ul v-if="question.options?.length" class="question-bank-options">
            <li v-for="option in question.options" :key="option">
              <button
                type="button"
                :aria-pressed="isOptionSelected(question, option)"
                :disabled="submittingAnswers[question.questionId]"
                :class="{
                  'is-selected': isOptionSelected(question, option),
                  'is-correct':
                    hasAnswered(question) &&
                    isOptionCorrect(question, option) &&
                    (isQuestionCorrect(question) || !isOptionSelected(question, option)),
                  'is-wrong':
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

          <div v-else-if="question.type === 'vocabulary'" class="question-bank-mistake-vocabulary">
            <strong>{{ question.answer }}</strong>
            <p>{{ question.explanation }}</p>
            <div>
              <button type="button" @click="markVocabulary(question, 'known')">认识</button>
              <button type="button" @click="markVocabulary(question, 'fuzzy')">模糊</button>
              <button type="button" @click="markVocabulary(question, 'unknown')">不认识</button>
            </div>
          </div>

          <div v-else class="question-bank-answer">
            <strong>参考答案：{{ question.answer }}</strong>
            <p>{{ question.explanation }}</p>
          </div>

          <div
            v-if="question.options?.length && hasAnswered(question)"
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

      <footer class="question-bank-pagination">
        <span>当前 {{ pageStart }}-{{ pageEnd }} / 共 {{ total }} 题</span>
        <div>
          <button type="button" :disabled="page <= 0" @click="goToPage(page - 1)">上一页</button>
          <strong>第 {{ page + 1 }} / {{ safeTotalPages }} 页</strong>
          <button type="button" :disabled="page >= safeTotalPages - 1" @click="goToPage(page + 1)">
            下一页
          </button>
        </div>
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
