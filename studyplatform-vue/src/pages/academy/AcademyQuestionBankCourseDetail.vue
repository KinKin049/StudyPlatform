<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { RouterLink, useRoute } from 'vue-router'
import {
  addQuestionBankFavorite,
  fetchQuestionBankCourse,
  recordQuestionBankAnswer,
  removeQuestionBankFavorite,
} from '../../api/academy'
import { recordProfileLearningEvent } from '../../api/profile'
import { resolveResourceUrl } from '../../api/request'
import { useLearningTimeTracker } from '../../composables/useLearningTimeTracker'

const route = useRoute()

const detail = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const openedAnswers = ref(new Set())
const selectedOptions = ref({})
const page = ref(0)
const pageSize = ref(30)
const pageJumpInput = ref('1')
const keyword = ref('')
const keywordInput = ref('')
const vocabularyMode = ref('sequence')
const currentCardIndex = ref(0)
const cardAnswerVisible = ref(false)
const shuffleTick = ref(0)
const vocabularyProgress = ref({})
const vocabularyCache = ref({})
const questionListRef = ref(null)
const favoriteSubmitting = ref({})
const practiceStarted = ref(false)
const practiceTimeTracker = useLearningTimeTracker({
  moduleType: 'question_bank',
  targetCode: () => bank.value?.code || route.params.courseCode,
  targetTitle: () => bank.value?.title || '题库练习',
  autoStart: false,
})

const bank = computed(() => detail.value?.bank)
const questions = computed(() => detail.value?.questions || [])
const total = computed(() => detail.value?.total ?? questions.value.length)
const totalPages = computed(() => detail.value?.totalPages || 0)
const responsePageSize = computed(() => detail.value?.size || pageSize.value)
const safeTotalPages = computed(() => Math.max(totalPages.value || 1, 1))

const isVocabularyBank = computed(() => {
  return ['cet4', 'cet6'].includes(bank.value?.code) || questions.value.some((question) => question.type === 'vocabulary')
})

const itemUnit = computed(() => (isVocabularyBank.value ? '词' : '题'))
const pageStart = computed(() => (total.value ? page.value * responsePageSize.value + 1 : 0))
const pageEnd = computed(() => Math.min(total.value, page.value * responsePageSize.value + questions.value.length))

const questionTypeLabel = (type) => {
  const labels = {
    single: '单选题',
    multiple: '多选题',
    short: '应用题',
    vocabulary: '词汇卡片',
  }
  return labels[type] || '题目'
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

const isMultipleQuestion = (question) => question.type === 'multiple' || answerKeys(question).length > 1

const selectedOptionKeys = (question) => {
  const selected = selectedOptions.value[question.id]
  if (Array.isArray(selected)) {
    return selected
  }
  return selected ? [selected] : []
}

const isOptionSelected = (question, option) => {
  return selectedOptionKeys(question).includes(optionKey(option))
}

const hasAnswered = (question) => selectedOptionKeys(question).length > 0

const hasSameKeys = (left, right) => {
  return left.length === right.length && left.every((item) => right.includes(item))
}

const isQuestionCorrect = (question) => {
  return hasSameKeys(selectedOptionKeys(question), answerKeys(question))
}

const isOptionCorrect = (question, option) => {
  return answerKeys(question).includes(optionKey(option))
}

const isQuestionFavorited = (question) => Boolean(question?.favorite)

const updateQuestionFavorite = (questionId, favorite) => {
  if (!detail.value?.questions) return
  detail.value = {
    ...detail.value,
    questions: detail.value.questions.map((question) =>
      question.id === questionId ? { ...question, favorite } : question,
    ),
  }
}

const toggleQuestionFavorite = async (question) => {
  if (!question?.id || favoriteSubmitting.value[question.id]) return
  favoriteSubmitting.value = {
    ...favoriteSubmitting.value,
    [question.id]: true,
  }
  try {
    if (isQuestionFavorited(question)) {
      await removeQuestionBankFavorite(question.id)
      updateQuestionFavorite(question.id, false)
      console.info('question favorite removed:', question.id)
    } else {
      await addQuestionBankFavorite(question.id)
      updateQuestionFavorite(question.id, true)
      console.info('question favorite added:', question.id)
    }
  } catch (error) {
    console.warn('failed to sync question favorite:', error)
  } finally {
    const nextSubmitting = { ...favoriteSubmitting.value }
    delete nextSubmitting[question.id]
    favoriteSubmitting.value = nextSubmitting
  }
}

const recordLearningEvent = (payload) => {
  recordProfileLearningEvent({
    setCode: bank.value?.code || route.params.courseCode,
    ...payload,
  }).catch((error) => {
    console.warn('failed to record profile learning event:', error)
  })
}

const recordChoiceAnswer = (question, selectedKeys) => {
  const correctKeys = answerKeys(question)
  if (!question?.id || !selectedKeys.length || !correctKeys.length) {
    return
  }
  recordLearningEvent({
    eventType: 'answer',
    questionId: question.id,
    questionType: question.type,
    selectedAnswer: selectedKeys.join(','),
    correctAnswer: correctKeys.join(','),
    isCorrect: hasSameKeys(selectedKeys, correctKeys),
  })
  recordQuestionBankAnswer({
    questionId: question.id,
    selectedAnswer: selectedKeys.join(','),
  }).then((result) => {
    console.info('question bank mistake state synced:', question.id, result.message)
  }).catch((error) => {
    console.warn('failed to sync question bank mistake:', error)
  })
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

const selectOption = (question, option) => {
  const key = optionKey(option)
  if (isMultipleQuestion(question)) {
    const selected = selectedOptionKeys(question)
    const nextSelected = selected.includes(key)
      ? selected.filter((item) => item !== key)
      : [...selected, key]
    selectedOptions.value = {
      ...selectedOptions.value,
      [question.id]: nextSelected,
    }
    if (shouldRecordChoiceAnswer(question, nextSelected)) {
      recordChoiceAnswer(question, nextSelected)
    }
    console.info('course question option toggled:', question.id, key)
    return
  }
  selectedOptions.value = {
    ...selectedOptions.value,
    [question.id]: key,
  }
  recordChoiceAnswer(question, [key])
  console.info('course question option selected:', question.id, key)
}

const coverSrc = computed(() => {
  const cover = bank.value?.coverUrl || bank.value?.fallbackCoverUrl || ''
  return resolveResourceUrl(cover)
})

const handleCoverError = (event) => {
  if (!bank.value?.fallbackCoverUrl || event.currentTarget.dataset.fallbackApplied === 'true') {
    return
  }
  event.currentTarget.dataset.fallbackApplied = 'true'
  event.currentTarget.src = bank.value.fallbackCoverUrl
}

const toggleAnswer = (question) => {
  const id = question?.id
  if (!id) return
  const next = new Set(openedAnswers.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
    recordLearningEvent({
      eventType: 'reveal',
      questionId: question.id,
      questionType: question.type,
      correctAnswer: question.answer,
    })
  }
  openedAnswers.value = next
}

const vocabularyStorageKey = (suffix) => {
  return bank.value?.code ? `study-platform:vocabulary:${bank.value.code}:${suffix}` : ''
}

const readStorageObject = (key) => {
  if (!key) return {}
  try {
    return JSON.parse(localStorage.getItem(key) || '{}')
  } catch (error) {
    console.warn('failed to read vocabulary storage:', key, error)
    return {}
  }
}

const writeStorageObject = (key, value) => {
  if (!key) return
  localStorage.setItem(key, JSON.stringify(value))
}

const loadVocabularyState = () => {
  vocabularyProgress.value = readStorageObject(vocabularyStorageKey('progress'))
  vocabularyCache.value = readStorageObject(vocabularyStorageKey('review-cache'))
}

const saveVocabularyState = () => {
  writeStorageObject(vocabularyStorageKey('progress'), vocabularyProgress.value)
  writeStorageObject(vocabularyStorageKey('review-cache'), vocabularyCache.value)
}

const progressFor = (question) => vocabularyProgress.value[question?.stem] || ''

const progressLabel = (status) => {
  const labels = {
    known: '认识',
    fuzzy: '模糊',
    unknown: '不认识',
  }
  return labels[status] || '未标记'
}

const progressSummary = computed(() => {
  return Object.values(vocabularyProgress.value).reduce(
    (summary, status) => ({
      ...summary,
      [status]: (summary[status] || 0) + 1,
    }),
    { known: 0, fuzzy: 0, unknown: 0 },
  )
})

const reviewCards = computed(() => {
  return Object.values(vocabularyCache.value).filter((question) => {
    const status = progressFor(question)
    return status === 'fuzzy' || status === 'unknown'
  })
})

const currentPageReviewCards = computed(() => {
  return questions.value.filter((question) => {
    const status = progressFor(question)
    return status === 'fuzzy' || status === 'unknown'
  })
})

const randomCards = computed(() => {
  shuffleTick.value
  const items = [...questions.value]
  for (let index = items.length - 1; index > 0; index -= 1) {
    const target = Math.floor(Math.random() * (index + 1))
    ;[items[index], items[target]] = [items[target], items[index]]
  }
  return items
})

const activeVocabularyDeck = computed(() => {
  if (vocabularyMode.value === 'review') {
    return reviewCards.value.length ? reviewCards.value : currentPageReviewCards.value
  }
  if (vocabularyMode.value === 'random') {
    return randomCards.value
  }
  return questions.value
})

const currentVocabularyCard = computed(() => activeVocabularyDeck.value[currentCardIndex.value] || null)

const resetVocabularyCard = () => {
  currentCardIndex.value = 0
  cardAnswerVisible.value = false
}

const setVocabularyMode = (mode) => {
  vocabularyMode.value = mode
  if (mode === 'random') {
    shuffleTick.value += 1
  }
  resetVocabularyCard()
}

const nextVocabularyCard = () => {
  if (!activeVocabularyDeck.value.length) return
  currentCardIndex.value = Math.min(currentCardIndex.value + 1, activeVocabularyDeck.value.length - 1)
  cardAnswerVisible.value = false
}

const previousVocabularyCard = () => {
  currentCardIndex.value = Math.max(currentCardIndex.value - 1, 0)
  cardAnswerVisible.value = false
}

const markVocabularyCard = (question, status) => {
  if (!question?.stem) return
  vocabularyProgress.value = {
    ...vocabularyProgress.value,
    [question.stem]: status,
  }
  const nextCache = { ...vocabularyCache.value }
  if (status === 'known') {
    delete nextCache[question.stem]
  } else {
    nextCache[question.stem] = question
  }
  vocabularyCache.value = nextCache
  saveVocabularyState()
  recordLearningEvent({
    eventType: 'vocabulary',
    questionId: question.id,
    questionType: question.type,
    selectedAnswer: status,
    correctAnswer: question.answer,
    isCorrect: status === 'known',
    vocabularyStatus: status,
  })
  recordQuestionBankAnswer({
    questionId: question.id,
    selectedAnswer: status,
  }).then((result) => {
    console.info('vocabulary mistake state synced:', question.id, result.message)
  }).catch((error) => {
    console.warn('failed to sync vocabulary mistake:', error)
  })
  nextVocabularyCard()
}

const selectVocabularyCard = (question) => {
  const activeIndex = activeVocabularyDeck.value.findIndex((item) => item.stem === question.stem)
  if (activeIndex >= 0) {
    currentCardIndex.value = activeIndex
    cardAnswerVisible.value = false
    return
  }
  vocabularyMode.value = 'sequence'
  currentCardIndex.value = Math.max(
    questions.value.findIndex((item) => item.stem === question.stem),
    0,
  )
  cardAnswerVisible.value = false
}

const scrollToFirstQuestion = (forceFirstQuestion = false) => {
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      const target = questionListRef.value?.querySelector(
        forceFirstQuestion
          ? '.vocabulary-card, .question-bank-question-card'
          : '.vocabulary-card, .question-bank-question-card, .question-course-empty',
      )
      if (!target) {
        questionListRef.value?.scrollIntoView({
          behavior: 'smooth',
          block: 'start',
        })
        return
      }
      const headerOffset = 132
      const targetTop = target.getBoundingClientRect().top + window.scrollY - headerOffset
      window.scrollTo({
        top: Math.max(targetTop, 0),
        behavior: 'smooth',
      })
    })
  })
}

const handleStartPractice = async () => {
  if (isVocabularyBank.value) {
    setVocabularyMode('sequence')
  }
  window.dispatchEvent(new CustomEvent('academy-question-practice-start'))
  practiceStarted.value = true
  practiceTimeTracker.start()
  await nextTick()
  scrollToFirstQuestion(true)
}

const loadDetail = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await fetchQuestionBankCourse(route.params.courseCode, {
      page: page.value,
      size: pageSize.value,
      keyword: keyword.value,
    })
    detail.value = data
    page.value = data?.page ?? page.value
    pageSize.value = data?.size ?? pageSize.value
    pageJumpInput.value = String(page.value + 1)
    selectedOptions.value = {}
    openedAnswers.value = new Set()
    if (isVocabularyBank.value) {
      loadVocabularyState()
      resetVocabularyCard()
    }
  } catch (error) {
    errorMessage.value = error.message || '课程题库加载失败'
  } finally {
    loading.value = false
  }
}

const submitSearch = () => {
  keyword.value = keywordInput.value.trim()
  page.value = 0
  loadDetail()
}

const clearSearch = () => {
  keywordInput.value = ''
  keyword.value = ''
  page.value = 0
  loadDetail()
}

const goToPage = (nextPage, shouldScroll = false) => {
  if (nextPage < 0 || (totalPages.value && nextPage >= totalPages.value)) {
    return
  }
  page.value = nextPage
  loadDetail().then(() => {
    if (shouldScroll) {
      scrollToFirstQuestion()
    }
  })
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

watch(activeVocabularyDeck, () => {
  if (currentCardIndex.value >= activeVocabularyDeck.value.length) {
    currentCardIndex.value = Math.max(activeVocabularyDeck.value.length - 1, 0)
  }
})

watch(currentVocabularyCard, () => {
  cardAnswerVisible.value = false
})

onMounted(loadDetail)
</script>

<template>
  <main class="academy-main question-course-main question-bank-detail-main">
    <nav class="question-course-breadcrumb" aria-label="题库面包屑">
      <RouterLink to="/academy/home">在线学堂</RouterLink>
      <span>&gt;</span>
      <RouterLink to="/academy/question-bank">题库</RouterLink>
      <span>&gt;</span>
      <RouterLink :to="{ path: '/academy/question-bank/courses', query: { category: bank?.categoryCode } }">
        课程题库
      </RouterLink>
      <span>&gt;</span>
      <strong>{{ bank?.title || '题库详情' }}</strong>
    </nav>

    <p v-if="errorMessage" class="question-course-message is-error">{{ errorMessage }}</p>
    <div v-if="loading" class="question-course-empty">正在加载题库...</div>

    <template v-else-if="bank">
      <section class="question-bank-detail-hero">
        <img :src="coverSrc" :alt="bank.title" @error="handleCoverError" />
        <div>
          <p>{{ bank.categoryName }} · {{ bank.subtitle }}</p>
          <h1>{{ bank.title }}</h1>
          <span>{{ bank.description }}</span>
          <div class="question-bank-detail-meta">
            <strong>{{ bank.questionCount }} {{ itemUnit }}</strong>
            <strong>{{ bank.difficultyLabel }}</strong>
            <strong>{{ bank.statusLabel }}</strong>
          </div>
          <div class="question-bank-detail-actions">
            <button type="button" @click="handleStartPractice">
              {{ isVocabularyBank ? '开始背词' : '开始练习' }}
            </button>
            <RouterLink :to="{ path: '/academy/question-bank/mistakes', query: { setCode: bank.code } }">
              本题库错题
            </RouterLink>
          </div>
        </div>
      </section>

      <section
        v-if="practiceStarted"
        ref="questionListRef"
        class="question-bank-question-list"
        :aria-label="`${bank.title}题目列表`"
      >
        <header class="question-bank-list-header">
          <div>
            <h2>{{ isVocabularyBank ? '背单词工作台' : '开始练习' }}</h2>
            <p>
              {{
                isVocabularyBank
                  ? '按页加载词汇卡片，支持顺序背词、随机抽词和本地错词复习。'
                  : '选择题点击选项后显示判定和标准答案，非选择题可展开参考答案。'
              }}
            </p>
          </div>
          <form class="question-bank-filter" @submit.prevent="submitSearch">
            <input
              v-model="keywordInput"
              type="search"
              :placeholder="isVocabularyBank ? '搜索单词或释义' : '搜索题干或答案'"
            />
            <button type="submit">搜索</button>
            <button v-if="keyword" type="button" @click="clearSearch">清空</button>
          </form>
        </header>

        <template v-if="isVocabularyBank">
          <div class="vocabulary-toolbar" aria-label="背词模式">
            <button
              type="button"
              :class="{ 'is-active': vocabularyMode === 'sequence' }"
              @click="setVocabularyMode('sequence')"
            >
              顺序背词
            </button>
            <button
              type="button"
              :class="{ 'is-active': vocabularyMode === 'random' }"
              @click="setVocabularyMode('random')"
            >
              随机抽词
            </button>
            <button
              type="button"
              :class="{ 'is-active': vocabularyMode === 'review' }"
              @click="setVocabularyMode('review')"
            >
              薄弱词复习
            </button>
          </div>

          <div class="vocabulary-progress-grid" aria-label="背词进度">
            <div>
              <strong>{{ progressSummary.known || 0 }}</strong>
              <span>认识</span>
            </div>
            <div>
              <strong>{{ progressSummary.fuzzy || 0 }}</strong>
              <span>模糊</span>
            </div>
            <div>
              <strong>{{ progressSummary.unknown || 0 }}</strong>
              <span>不认识</span>
            </div>
          </div>

          <div v-if="!activeVocabularyDeck.length" class="question-course-empty">
            当前模式下暂无词汇。可以先在顺序模式中标记“模糊”或“不认识”，它们会进入薄弱词复习。
          </div>

          <div v-else class="vocabulary-workbench">
            <article class="vocabulary-card">
              <div class="vocabulary-card-head">
                <p>{{ questionTypeLabel(currentVocabularyCard.type) }} · {{ currentVocabularyCard.difficultyLabel }}</p>
              </div>
              <h3>{{ currentVocabularyCard.stem }}</h3>
              <span>第 {{ currentCardIndex + 1 }} / {{ activeVocabularyDeck.length }} 张</span>

              <button type="button" class="vocabulary-reveal" @click="cardAnswerVisible = !cardAnswerVisible">
                {{ cardAnswerVisible ? '收起释义' : '查看释义' }}
              </button>

              <div v-if="cardAnswerVisible" class="vocabulary-answer">
                <strong>{{ currentVocabularyCard.answer }}</strong>
                <p>{{ currentVocabularyCard.explanation }}</p>
              </div>

              <div class="vocabulary-actions">
                <div class="vocabulary-memory-actions">
                  <button type="button" class="is-known" @click="markVocabularyCard(currentVocabularyCard, 'known')">
                    认识
                  </button>
                  <button type="button" class="is-fuzzy" @click="markVocabularyCard(currentVocabularyCard, 'fuzzy')">
                    模糊
                  </button>
                  <button type="button" class="is-unknown" @click="markVocabularyCard(currentVocabularyCard, 'unknown')">
                    不认识
                  </button>
                </div>
                <button
                  type="button"
                  class="question-bank-favorite-button vocabulary-favorite-button"
                  :class="{ 'is-favorite': isQuestionFavorited(currentVocabularyCard) }"
                  :disabled="favoriteSubmitting[currentVocabularyCard.id]"
                  :aria-label="isQuestionFavorited(currentVocabularyCard) ? '取消收藏' : '收藏题目'"
                  :title="isQuestionFavorited(currentVocabularyCard) ? '取消收藏' : '收藏题目'"
                  @click="toggleQuestionFavorite(currentVocabularyCard)"
                >
                  <StarFilled v-if="isQuestionFavorited(currentVocabularyCard)" aria-hidden="true" />
                  <Star v-else aria-hidden="true" />
                </button>
              </div>

              <div class="vocabulary-card-nav">
                <button type="button" :disabled="currentCardIndex === 0" @click="previousVocabularyCard">上一词</button>
                <button
                  type="button"
                  :disabled="currentCardIndex >= activeVocabularyDeck.length - 1"
                  @click="nextVocabularyCard"
                >
                  下一词
                </button>
              </div>
            </article>

            <aside class="vocabulary-page-list" aria-label="当前页词汇">
              <h3>当前页词汇</h3>
              <button
                v-for="(question, index) in questions"
                :key="question.id"
                type="button"
                :class="{ 'is-active': currentVocabularyCard?.stem === question.stem }"
                @click="selectVocabularyCard(question)"
              >
                <strong>{{ question.stem }}</strong>
                <span>{{ progressLabel(progressFor(question)) }}</span>
              </button>
            </aside>
          </div>
        </template>

        <template v-else>
          <article v-for="(question, index) in questions" :key="question.id" class="question-bank-question-card">
            <div class="question-bank-question-head">
              <span>{{ page * responsePageSize + index + 1 }}</span>
              <div class="question-bank-question-title">
                <div>
                  <p>{{ questionTypeLabel(question.type) }} · {{ question.difficultyLabel }}</p>
                  <h3>{{ question.stem }}</h3>
                </div>
                <button
                  type="button"
                  class="question-bank-favorite-button"
                  :class="{ 'is-favorite': isQuestionFavorited(question) }"
                  :disabled="favoriteSubmitting[question.id]"
                  :aria-label="isQuestionFavorited(question) ? '取消收藏' : '收藏题目'"
                  :title="isQuestionFavorited(question) ? '取消收藏' : '收藏题目'"
                  @click="toggleQuestionFavorite(question)"
                >
                  <StarFilled v-if="isQuestionFavorited(question)" aria-hidden="true" />
                  <Star v-else aria-hidden="true" />
                </button>
              </div>
            </div>

            <ul v-if="question.options?.length" class="question-bank-options">
              <li v-for="option in question.options" :key="option">
                <button
                  type="button"
                  :aria-pressed="isOptionSelected(question, option)"
                  :class="{
                    'is-selected': isOptionSelected(question, option),
                    'is-correct':
                      hasAnswered(question) &&
                      isQuestionCorrect(question) &&
                      isOptionSelected(question, option) &&
                      isOptionCorrect(question, option),
                    'is-wrong':
                      hasAnswered(question) &&
                      !isQuestionCorrect(question) &&
                      isOptionSelected(question, option),
                  }"
                  @click="selectOption(question, option)"
                >
                  {{ option }}
                </button>
              </li>
            </ul>

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
            </div>

            <button
              v-if="!question.options?.length"
              type="button"
              class="question-bank-answer-toggle"
              @click="toggleAnswer(question)"
            >
              {{ openedAnswers.has(question.id) ? '收起答案' : '查看答案' }}
            </button>

            <div v-if="!question.options?.length && openedAnswers.has(question.id)" class="question-bank-answer">
              <strong>参考答案：{{ question.answer }}</strong>
              <p>{{ question.explanation }}</p>
            </div>
          </article>
        </template>

        <footer class="question-bank-pagination">
          <span>当前 {{ pageStart }}-{{ pageEnd }} / 共 {{ total }} {{ itemUnit }}</span>
          <div>
            <button type="button" :disabled="page <= 0" @click="goToPage(page - 1)">上一页</button>
            <strong>第 {{ page + 1 }} / {{ safeTotalPages }} 页</strong>
            <button type="button" :disabled="page >= safeTotalPages - 1" @click="goToPage(page + 1)">
              下一页
            </button>
          </div>
          <form class="question-bank-page-jump" @submit.prevent="jumpToPage">
            <label for="question-bank-page-jump">跳转到</label>
            <input
              id="question-bank-page-jump"
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
    </template>
  </main>
</template>
