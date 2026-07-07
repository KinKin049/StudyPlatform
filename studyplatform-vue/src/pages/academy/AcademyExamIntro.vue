<script setup>
/**
 * 考试说明页面组件
 * 展示考试基本信息，包括课程名称、教师、开始/结束时间、时长、总分等
 * 提供开始考试、继续考试、查看答卷等操作入口
 */
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { fetchAcademyExam, startAcademyExam } from '../../api/academy'

/**
 * 组件属性定义
 */
const props = defineProps({
  examId: {
    type: String,
    required: true,
  },
})

/**
 * 路由实例和响应式状态
 */
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

/**
 * 获取考试数据，若无数据则返回默认值
 */
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

/**
 * 判断考试是否已提交
 */
const isSubmitted = computed(() => ['pending_review', 'graded'].includes(defaultExam.value.submissionStatus))

/**
 * 判断考试是否已开始
 */
const examStarted = computed(() =>
  Boolean(defaultExam.value.startedAt) || ['in_progress', 'draft'].includes(defaultExam.value.submissionStatus),
)

/**
 * 判断考试截止时间是否已过
 */
const isDeadlinePassed = computed(() => {
  if (!defaultExam.value.deadline) return false
  const deadlineTime = new Date(defaultExam.value.deadline).getTime()
  return Number.isFinite(deadlineTime) && deadlineTime < now.value
})

/**
 * 判断考试是否在开始时间之前
 */
const isBeforeStart = computed(() => {
  if (!defaultExam.value.startsAt) return false
  const startTime = new Date(defaultExam.value.startsAt).getTime()
  return Number.isFinite(startTime) && startTime > now.value
})

/**
 * 判断考试是否已结束
 */
const isExamEnded = computed(() => defaultExam.value.status === '已结束' || isDeadlinePassed.value)

/**
 * 判断是否可以开始考试
 */
const canStartExam = computed(() =>
  !examStarted.value && !isSubmitted.value && !isExamEnded.value && !isBeforeStart.value,
)

/**
 * 根据考试状态计算进入按钮的文本
 */
const enterButtonText = computed(() => {
  if (starting.value) return '进入中...'
  if (isSubmitted.value) return '查看答卷'
  if (examStarted.value) return '继续考试'
  if (isExamEnded.value) return '考试已结束'
  if (isBeforeStart.value) return '尚未开始'
  return '开始考试'
})

/**
 * 判断进入按钮是否禁用
 */
const enterButtonDisabled = computed(() =>
  starting.value || (!canStartExam.value && !examStarted.value && !isSubmitted.value),
)

/**
 * 生成题目组成摘要，包括总题数、客观题数和待批阅题数
 */
const questionSummary = computed(() => {
  const questions = defaultExam.value.questions || []
  const objectiveCount = questions.filter((question) => ['single', 'multiple', 'blank'].includes(question.type)).length
  const reviewCount = questions.filter((question) => question.requiresTeacherReview).length
  return `${questions.length} 题 · 客观题 ${objectiveCount} 题 · 待批阅题 ${reviewCount} 题`
})

/**
 * 格式化日期时间为YYYY-MM-DD HH:mm格式
 */
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

/**
 * 显示操作反馈提示
 */
const showFeedback = (message) => {
  feedbackMessage.value = message
  feedbackVisible.value = true
  window.clearTimeout(feedbackTimer)
  feedbackTimer = window.setTimeout(() => {
    feedbackVisible.value = false
  }, 1800)
}

/**
 * 生成进入考试答题页面的路径
 */
const takeExamPath = () => `/academy/exams/${encodeURIComponent(defaultExam.value.id)}/take`

/**
 * 加载考试信息
 */
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

/**
 * 进入考试处理逻辑
 */
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

/**
 * 监听考试ID变化，重新加载考试信息
 */
watch(() => props.examId, loadExam)

/**
 * 组件挂载时加载考试信息
 */
onMounted(() => {
  loadExam()
})

/**
 * 组件卸载时清理定时器
 */
onBeforeUnmount(() => {
  window.clearTimeout(feedbackTimer)
  window.clearTimeout(navigationTimer)
})
</script>

<template>
  <main class="academy-main academy-assignment-detail-main academy-exam-intro-main">
    <!-- 操作反馈提示 -->
    <Transition name="academy-assignment-feedback">
      <div v-if="feedbackVisible" class="academy-assignment-feedback-toast" role="status">
        {{ feedbackMessage }}
      </div>
    </Transition>

    <!-- 加载状态 -->
    <div v-if="loading" class="academy-aggregate-state">正在加载考试信息...</div>
    <!-- 错误状态 -->
    <div v-else-if="error" class="academy-aggregate-state academy-aggregate-state-error">
      <span>{{ error }}</span>
      <button type="button" @click="loadExam">重试</button>
    </div>

    <!-- 考试信息主体内容 -->
    <template v-else>
      <!-- 面包屑导航 -->
      <nav class="academy-assignment-breadcrumb" aria-label="考试面包屑">
        <RouterLink to="/academy/exams">我的考试</RouterLink>
        <span>/</span>
        <strong>{{ defaultExam.title }}</strong>
      </nav>

      <!-- 考试说明卡片 -->
      <section class="academy-exam-intro-card">
        <!-- 考试标题区域 -->
        <div class="academy-exam-intro-title">
          <span>{{ defaultExam.course }}</span>
          <h1>{{ defaultExam.title }}</h1>
          <p>{{ defaultExam.description }}</p>
        </div>

        <!-- 考试详细信息 -->
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

        <!-- 操作按钮区域 -->
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
