<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { fetchQuestionBankCourse } from '../../api/academy'
import { resolveResourceUrl } from '../../api/request'

const route = useRoute()

const detail = ref(null)
const loading = ref(false)
const errorMessage = ref('')
const openedAnswers = ref(new Set())
const selectedOptions = ref({})

const bank = computed(() => detail.value?.bank)
const questions = computed(() => detail.value?.questions || [])

const questionTypeLabel = (type) => {
  const labels = {
    single: '单选题',
    multiple: '多选题',
    short: '应用题',
  }
  return labels[type] || '题目'
}

const optionKey = (option) => {
  const match = String(option || '').match(/^\s*([A-Z])[\.\、]/i)
  return match ? match[1].toUpperCase() : String(option || '').trim()
}

const answerKeys = (question) => {
  return String(question.answer || '')
    .split(/[,，、\s]+/)
    .map((item) => item.trim().toUpperCase())
    .filter(Boolean)
}

const selectedOption = (question) => selectedOptions.value[question.id]

const isOptionCorrect = (question, option) => {
  return answerKeys(question).includes(optionKey(option))
}

const selectOption = (question, option) => {
  selectedOptions.value = {
    ...selectedOptions.value,
    [question.id]: optionKey(option),
  }
  // TODO: 接入答题记录接口，例如 POST /api/academy/question-bank/courses/{code}/answers
  console.info('course question option selected:', question.id, optionKey(option))
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

const toggleAnswer = (id) => {
  const next = new Set(openedAnswers.value)
  if (next.has(id)) {
    next.delete(id)
  } else {
    next.add(id)
  }
  openedAnswers.value = next
}

const handleStartPractice = () => {
  // TODO: 接入答题提交接口，例如 POST /api/academy/question-bank/courses/{code}/sessions
  console.info('start course question bank practice reserved:', bank.value?.code)
}

const loadDetail = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    detail.value = await fetchQuestionBankCourse(route.params.courseCode)
    selectedOptions.value = {}
    openedAnswers.value = new Set()
  } catch (error) {
    errorMessage.value = error.message || '课程题库加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadDetail)
</script>

<template>
  <main class="academy-main question-course-main question-bank-detail-main">
    <nav class="question-course-breadcrumb" aria-label="题库面包屑">
      <RouterLink to="/academy/home">在线学堂</RouterLink>
      <span>&gt;</span>
      <RouterLink to="/academy/question-bank">题库</RouterLink>
      <span>&gt;</span>
      <RouterLink to="/academy/question-bank/courses">课程题库</RouterLink>
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
            <strong>{{ bank.questionCount }} 题</strong>
            <strong>{{ bank.difficultyLabel }}</strong>
            <strong>{{ bank.statusLabel }}</strong>
          </div>
          <button type="button" @click="handleStartPractice">开始练习</button>
        </div>
      </section>

      <section class="question-bank-source-panel" aria-label="题库来源">
        <div>
          <h2>数据来源</h2>
          <p>
            当前题库数据由后端 API 从 MySQL 读取，来源入口已记录在数据库中，后续可继续扩展批量导入和审核流程。
          </p>
        </div>
        <div>
          <a
            v-for="source in bank.sourceRefs"
            :key="source"
            :href="source"
            target="_blank"
            rel="noreferrer"
          >
            {{ source }}
          </a>
        </div>
      </section>

      <section class="question-bank-question-list" :aria-label="`${bank.title}题目列表`">
        <header>
          <div>
            <h2>题目预览</h2>
            <p>选择题点击选项后显示判定和标准答案，后续可在这里接入做题记录、判分和错题本。</p>
          </div>
        </header>

        <article v-for="(question, index) in questions" :key="question.id" class="question-bank-question-card">
          <div class="question-bank-question-head">
            <span>{{ index + 1 }}</span>
            <div>
              <p>{{ questionTypeLabel(question.type) }} · {{ question.difficultyLabel }}</p>
              <h3>{{ question.stem }}</h3>
            </div>
          </div>

          <ul v-if="question.options?.length" class="question-bank-options">
            <li v-for="option in question.options" :key="option">
              <button
                type="button"
                :class="{
                  'is-selected': selectedOption(question) === optionKey(option),
                  'is-correct': selectedOption(question) === optionKey(option) && isOptionCorrect(question, option),
                  'is-wrong': selectedOption(question) === optionKey(option) && !isOptionCorrect(question, option),
                }"
                @click="selectOption(question, option)"
              >
                {{ option }}
              </button>
            </li>
          </ul>

          <div
            v-if="question.options?.length && selectedOption(question)"
            class="question-bank-answer"
            :class="{
              'is-correct': answerKeys(question).includes(selectedOption(question)),
              'is-wrong': !answerKeys(question).includes(selectedOption(question)),
            }"
          >
            <strong>{{ answerKeys(question).includes(selectedOption(question)) ? '回答正确' : '回答错误' }}</strong>
            <p>标准答案：{{ question.answer }}</p>
            <p>{{ question.explanation }}</p>
          </div>

          <button
            v-if="!question.options?.length"
            type="button"
            class="question-bank-answer-toggle"
            @click="toggleAnswer(question.id)"
          >
            {{ openedAnswers.has(question.id) ? '收起答案' : '查看答案' }}
          </button>

          <div v-if="!question.options?.length && openedAnswers.has(question.id)" class="question-bank-answer">
            <strong>参考答案：{{ question.answer }}</strong>
            <p>{{ question.explanation }}</p>
          </div>
        </article>
      </section>
    </template>
  </main>
</template>
