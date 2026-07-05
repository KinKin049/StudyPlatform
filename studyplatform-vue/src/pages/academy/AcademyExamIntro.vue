<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { fetchAcademyExam, startAcademyExam } from '../../api/academy'

const props = defineProps({
  examId: {
    type: String,
    required: true,
  },
})

const router = useRouter()
const exam = ref(null)
const loading = ref(true)
const error = ref('')
const starting = ref(false)
const feedbackMessage = ref('')
const feedbackVisible = ref(false)
const now = ref(Date.now())
let feedbackTimer = null
let navigationTimer = null

const defaultExam = computed(() => exam.value || {
  id: props.examId,
  title: '考试详情',
  course: '课程考试',
  teacher: '教师待定',
  status: '正在进行',
  startsAt: '',
  deadline: '',
  attemptsLeft: 1,
  durationMinutes: 0,
  totalScore: 0,
  description: '',
  questions: [],
  submissionStatus: null,
  score: null,
  startedAt: null,
  submittedAt: null,
})

const isSubmitted = computed(() => ['pending_review', 'graded'].includes(defaultExam.value.submissionStatus))

const examStarted = computed(() =>
  Boolean(defaultExam.value.startedAt) || ['in_progress', 'draft'].includes(defaultExam.value.submissionStatus),
)

const isDeadlinePassed = computed(() => {
  if (!defaultExam.value.deadline) return false
  const deadlineTime = new Date(defaultExam.value.deadline).getTime()
  return Number.isFinite(deadlineTime) && deadlineTime < now.value
})

const isBeforeStart = computed(() => {
  if (!defaultExam.value.startsAt) return false
  const startTime = new Date(defaultExam.value.startsAt).getTime()
  return Number.isFinite(startTime) && startTime > now.value
})

const isExamEnded = computed(() => defaultExam.value.status === '已结束' || isDeadlinePassed.value)

const canStartExam = computed(() =>
  !examStarted.value && !isSubmitted.value && !isExamEnded.value && !isBeforeStart.value,
)

const enterButtonText = computed(() => {
  if (starting.value) return '进入中...'
  if (isSubmitted.value) return '查看答卷'
  if (examStarted.value) return '继续考试'
  if (isExamEnded.value) return '考试已结束'
  if (isBeforeStart.value) return '尚未开始'
  return '开始考试'
})

const enterButtonDisabled = computed(() =>
  starting.value || (!canStartExam.value && !examStarted.value && !isSubmitted.value),
)

const questionSummary = computed(() => {
  const questions = defaultExam.value.questions || []
  const objectiveCount = questions.filter((question) => ['single', 'multiple', 'blank'].includes(question.type)).length
  const reviewCount = questions.filter((question) => question.requiresTeacherReview).length
  return `${questions.length} 题 · 客观题 ${objectiveCount} 题 · 待批阅题 ${reviewCount} 题`
})

const formatDateTime = (value) => {
  if (!value) return '时间待定'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ').slice(0, 16)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

const showFeedback = (message) => {
  feedbackMessage.value = message
  feedbackVisible.value = true
  window.clearTimeout(feedbackTimer)
  feedbackTimer = window.setTimeout(() => {
    feedbackVisible.value = false
  }, 1800)
}

const takeExamPath = () => `/academy/exams/${encodeURIComponent(defaultExam.value.id)}/take`

const loadExam = async () => {
  loading.value = true
  error.value = ''

  try {
    exam.value = await fetchAcademyExam(props.examId, 1)
  } catch (err) {
    exam.value = null
    error.value = err instanceof Error ? err.message : '考试信息加载失败'
  } finally {
    loading.value = false
  }
}

const enterExam = async () => {
  if (enterButtonDisabled.value) return

  if (examStarted.value || isSubmitted.value) {
    router.push(takeExamPath())
    return
  }

  starting.value = true

  try {
    const data = await startAcademyExam(defaultExam.value.id, 1)
    exam.value = data
    showFeedback('考试已开始')
    window.clearTimeout(navigationTimer)
    navigationTimer = window.setTimeout(() => {
      router.push(`/academy/exams/${encodeURIComponent(data.id)}/take`)
    }, 520)
  } catch (err) {
    showFeedback(err instanceof Error ? err.message : '进入考试失败')
  } finally {
    starting.value = false
  }
}

watch(() => props.examId, loadExam)

onMounted(() => {
  loadExam()
})

onBeforeUnmount(() => {
  window.clearTimeout(feedbackTimer)
  window.clearTimeout(navigationTimer)
})
</script>

<template>
  <main class="academy-main academy-assignment-detail-main academy-exam-intro-main">
    <Transition name="academy-assignment-feedback">
      <div v-if="feedbackVisible" class="academy-assignment-feedback-toast" role="status">
        {{ feedbackMessage }}
      </div>
    </Transition>

    <div v-if="loading" class="academy-aggregate-state">正在加载考试信息...</div>
    <div v-else-if="error" class="academy-aggregate-state academy-aggregate-state-error">
      <span>{{ error }}</span>
      <button type="button" @click="loadExam">重试</button>
    </div>

    <template v-else>
      <nav class="academy-assignment-breadcrumb" aria-label="考试面包屑">
        <RouterLink to="/academy/exams">我的考试</RouterLink>
        <span>/</span>
        <strong>{{ defaultExam.title }}</strong>
      </nav>

      <section class="academy-exam-intro-card">
        <div class="academy-exam-intro-title">
          <span>{{ defaultExam.course }}</span>
          <h1>{{ defaultExam.title }}</h1>
          <p>{{ defaultExam.description }}</p>
        </div>

        <section class="academy-exam-intro-info" aria-label="考试信息">
          <div>
            <span>授课教师</span>
            <strong>{{ defaultExam.teacher }}</strong>
          </div>
          <div>
            <span>考试状态</span>
            <strong>{{ defaultExam.status }}</strong>
          </div>
          <div>
            <span>开始时间</span>
            <strong>{{ formatDateTime(defaultExam.startsAt) }}</strong>
          </div>
          <div>
            <span>结束时间</span>
            <strong>{{ formatDateTime(defaultExam.deadline) }}</strong>
          </div>
          <div>
            <span>考试时长</span>
            <strong>{{ defaultExam.durationMinutes || 0 }} 分钟</strong>
          </div>
          <div>
            <span>考试总分</span>
            <strong>{{ defaultExam.totalScore || 0 }} 分</strong>
          </div>
          <div>
            <span>题目组成</span>
            <strong>{{ questionSummary }}</strong>
          </div>
          <div>
            <span>提交状态</span>
            <strong>{{ isSubmitted ? '已提交' : (examStarted ? '答题中' : '未开始') }}</strong>
          </div>
        </section>

        <div class="academy-exam-intro-actions">
          <button
            type="button"
            class="assignment-primary-button"
            :disabled="enterButtonDisabled"
            @click="enterExam"
          >
            {{ enterButtonText }}
          </button>
          <RouterLink class="academy-assignment-return" to="/academy/exams">返回考试列表</RouterLink>
        </div>
      </section>
    </template>
  </main>
</template>
