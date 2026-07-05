<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import {
  fetchAcademyExam,
  saveAcademyExamDraft,
  submitAcademyExam,
} from '../../api/academy'

const props = defineProps({
  examId: {
    type: String,
    required: true,
  },
})

const remoteExam = ref(null)
const loading = ref(true)
const error = ref('')
const answers = ref({})
const submitResult = ref(null)
const submitDialogVisible = ref(false)
const feedbackMessage = ref('')
const feedbackVisible = ref(false)
const saving = ref(false)
const submitting = ref(false)
const now = ref(Date.now())
const autoSubmitTriggered = ref(false)
let feedbackTimer = null
let countdownTimer = null

const stopCountdownTimer = () => {
  window.clearInterval(countdownTimer)
  countdownTimer = null
}

const startCountdownTimer = () => {
  stopCountdownTimer()
  countdownTimer = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
}

const exam = computed(() => remoteExam.value || {
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
  draftAnswers: {},
  submissionStatus: null,
  score: null,
  pendingTeacherReview: false,
  startedAt: null,
  submittedAt: null,
})

const resultLabelMap = {
  single: '单选题得分',
  multiple: '多选题得分',
  blank: '填空题得分',
  short: '主观题批改',
  code: '编程题判题',
}

const totalScore = computed(() => {
  if (exam.value.totalScore) return exam.value.totalScore
  return exam.value.questions.reduce((sum, question) => sum + (question.score || 0), 0)
})

const answeredCount = computed(() =>
  exam.value.questions.filter((question) => isQuestionAnswered(question)).length,
)

const progressPercent = computed(() => {
  const questionCount = exam.value.questions.length
  return questionCount > 0 ? Math.round((answeredCount.value / questionCount) * 100) : 0
})

const examStarted = computed(() =>
  Boolean(exam.value.startedAt) || ['in_progress', 'draft', 'pending_review', 'graded'].includes(exam.value.submissionStatus),
)

const isSubmitted = computed(() => ['pending_review', 'graded'].includes(exam.value.submissionStatus))

const isDeadlinePassed = computed(() => {
  if (!exam.value.deadline) return false
  const deadlineTime = new Date(exam.value.deadline).getTime()
  return Number.isFinite(deadlineTime) && deadlineTime < now.value
})

const isBeforeStart = computed(() => {
  if (!exam.value.startsAt) return false
  const startTime = new Date(exam.value.startsAt).getTime()
  return Number.isFinite(startTime) && startTime > now.value
})

const isExamEnded = computed(() => exam.value.status === '已结束' || isDeadlinePassed.value)

const durationMs = computed(() => Math.max(exam.value.durationMinutes || 0, 0) * 60 * 1000)

const remainingMs = computed(() => {
  if (!durationMs.value) return 0
  if (!examStarted.value || !exam.value.startedAt) return durationMs.value
  const startedTime = new Date(exam.value.startedAt).getTime()
  if (!Number.isFinite(startedTime)) return durationMs.value
  return Math.max(0, startedTime + durationMs.value - now.value)
})

const isTimeUp = computed(() =>
  durationMs.value > 0 && examStarted.value && !isSubmitted.value && remainingMs.value <= 0,
)

const answerDisabled = computed(() =>
  !examStarted.value || isSubmitted.value || isExamEnded.value || isTimeUp.value,
)

const countdownText = computed(() => {
  if (isSubmitted.value) return '已提交'
  if (!durationMs.value) return '不限时'
  const totalSeconds = Math.ceil(remainingMs.value / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  return [hours, minutes, seconds].map((value) => String(value).padStart(2, '0')).join(':')
})

const actionHint = computed(() => {
  if (isSubmitted.value) return '考试已提交，可查看提交结果与答题记录。'
  if (isExamEnded.value) return '考试已结束，仅可查看历史内容。'
  if (isBeforeStart.value) return '考试尚未开始，请在开放时间后进入。'
  if (!examStarted.value) return '请先从考试说明页点击“开始考试”。'
  if (isTimeUp.value) return '考试时间已到，系统会尝试自动交卷。'
  return '答题过程中可保存草稿，确认无误后提交试卷。'
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

const isQuestionAnswered = (question) => {
  const answer = answers.value[question.id]
  if (Array.isArray(answer)) return answer.length > 0
  return String(answer ?? '').trim().length > 0
}

const getResultQuestion = (result) =>
  exam.value.questions.find((question) => String(question.id) === String(result.questionId))

const getResultLabel = (result) => {
  const question = getResultQuestion(result)
  if (!question) return '题目得分'
  return resultLabelMap[question.type] || `${question.label || '题目'}得分`
}

const updateMultipleAnswer = (questionId, option, checked) => {
  if (answerDisabled.value) return
  const currentAnswer = Array.isArray(answers.value[questionId]) ? answers.value[questionId] : []
  answers.value = {
    ...answers.value,
    [questionId]: checked
      ? [...currentAnswer, option]
      : currentAnswer.filter((selectedOption) => selectedOption !== option),
  }
}

const showFeedback = (message) => {
  feedbackMessage.value = message
  feedbackVisible.value = true
  window.clearTimeout(feedbackTimer)
  feedbackTimer = window.setTimeout(() => {
    feedbackVisible.value = false
  }, 1800)
}

const loadExam = async () => {
  loading.value = true
  error.value = ''
  submitResult.value = null
  autoSubmitTriggered.value = false

  try {
    const data = await fetchAcademyExam(props.examId, 1)
    remoteExam.value = data
    answers.value = data.draftAnswers || {}
    if (['pending_review', 'graded'].includes(data.submissionStatus)) {
      stopCountdownTimer()
    } else if (!countdownTimer) {
      startCountdownTimer()
    }
  } catch (err) {
    remoteExam.value = null
    answers.value = {}
    error.value = err instanceof Error ? err.message : '考试详情加载失败'
  } finally {
    loading.value = false
  }
}

const saveDraft = async () => {
  if (!examStarted.value) {
    showFeedback('请先开始考试')
    return
  }
  if (answerDisabled.value || saving.value) return
  saving.value = true

  try {
    const result = await saveAcademyExamDraft(exam.value.id, answers.value, 1)
    remoteExam.value = {
      ...exam.value,
      submissionStatus: 'draft',
      draftAnswers: answers.value,
    }
    showFeedback(result.message || '考试草稿保存成功')
  } catch (err) {
    showFeedback(err instanceof Error ? err.message : '草稿保存失败')
  } finally {
    saving.value = false
  }
}

const openSubmitDialog = () => {
  if (!examStarted.value) {
    showFeedback('请先开始考试')
    return
  }
  if (answerDisabled.value && !isTimeUp.value) return
  submitDialogVisible.value = true
}

const closeSubmitDialog = () => {
  submitDialogVisible.value = false
}

const submitExam = async (auto = false) => {
  if (submitting.value || isSubmitted.value) return
  if (!examStarted.value) {
    showFeedback('请先开始考试')
    return
  }
  submitting.value = true

  try {
    const result = await submitAcademyExam(exam.value.id, answers.value, 1)
    submitResult.value = result
    submitDialogVisible.value = false
    remoteExam.value = {
      ...exam.value,
      submissionStatus: result.status,
      score: result.score,
      submittedAt: new Date().toISOString(),
      draftAnswers: answers.value,
    }
    autoSubmitTriggered.value = true
    stopCountdownTimer()
    showFeedback(auto ? '考试时间到，已自动交卷' : (result.message || '考试提交成功'))
  } catch (err) {
    showFeedback(err instanceof Error ? err.message : '考试提交失败')
  } finally {
    submitting.value = false
  }
}

watch(() => props.examId, loadExam)

watch(remainingMs, (value) => {
  if (
    durationMs.value > 0 &&
    value <= 0 &&
    examStarted.value &&
    !isSubmitted.value &&
    !autoSubmitTriggered.value
  ) {
    autoSubmitTriggered.value = true
    submitExam(true)
  }
})

onMounted(() => {
  loadExam()
  startCountdownTimer()
})

onBeforeUnmount(() => {
  window.clearTimeout(feedbackTimer)
  stopCountdownTimer()
})
</script>

<template>
  <main class="academy-main academy-assignment-detail-main academy-exam-detail-main">
    <Transition name="academy-assignment-dialog">
      <div
        v-if="submitDialogVisible"
        class="academy-assignment-dialog-backdrop"
        role="presentation"
        @click.self="closeSubmitDialog"
      >
        <section class="academy-assignment-dialog" role="dialog" aria-modal="true" aria-label="确认提交考试">
          <h2>确认提交考试</h2>
          <p>当前已完成 {{ answeredCount }}/{{ exam.questions.length }} 题，提交后客观题会立即自动批改。</p>
          <div class="academy-assignment-dialog-actions">
            <button type="button" class="assignment-ghost-button" @click="closeSubmitDialog">继续检查</button>
            <button type="button" class="assignment-primary-button" :disabled="submitting" @click="submitExam(false)">
              {{ submitting ? '提交中...' : '确认提交' }}
            </button>
          </div>
        </section>
      </div>
    </Transition>

    <Transition name="academy-assignment-feedback">
      <div v-if="feedbackVisible" class="academy-assignment-feedback-toast" role="status">
        {{ feedbackMessage }}
      </div>
    </Transition>

    <div v-if="loading" class="academy-aggregate-state">正在加载考试详情...</div>
    <div v-else-if="error" class="academy-aggregate-state academy-aggregate-state-error">
      <span>{{ error }}</span>
      <button type="button" @click="loadExam">重试</button>
    </div>

    <template v-else>
      <nav class="academy-assignment-breadcrumb" aria-label="考试面包屑">
        <RouterLink to="/academy/exams">我的考试</RouterLink>
        <span>/</span>
        <strong>{{ exam.title }}</strong>
      </nav>

      <section class="academy-assignment-hero academy-exam-hero">
        <div>
          <span>{{ exam.course }}</span>
          <h1>{{ exam.title }}</h1>
          <p>{{ exam.description }}</p>
        </div>
        <aside class="academy-exam-countdown" :class="{ 'is-warning': remainingMs <= 5 * 60 * 1000 && examStarted }">
          <span>{{ isSubmitted ? '考试状态' : (examStarted ? '剩余时间' : '考试时长') }}</span>
          <strong>{{ countdownText }}</strong>
          <em>已完成 {{ answeredCount }}/{{ exam.questions.length }} 题</em>
        </aside>
      </section>

      <section class="academy-assignment-layout">
        <div class="academy-assignment-question-list">
          <article
            v-for="(question, index) in exam.questions"
            :id="`exam-question-${question.id}`"
            :key="question.id"
            :class="['academy-assignment-question-card', { 'is-disabled': answerDisabled }]"
          >
            <header>
              <div>
                <span>{{ question.label }}</span>
                <h2>{{ index + 1 }}. {{ question.title }}</h2>
              </div>
              <strong>{{ question.score }} 分</strong>
            </header>

            <div v-if="question.type === 'single'" class="academy-assignment-options">
              <label v-for="option in question.options" :key="option">
                <input
                  v-model="answers[question.id]"
                  type="radio"
                  :name="String(question.id)"
                  :value="option"
                  :disabled="answerDisabled"
                />
                <span>{{ option }}</span>
              </label>
            </div>

            <div v-else-if="question.type === 'multiple'" class="academy-assignment-options">
              <label v-for="option in question.options" :key="option">
                <input
                  type="checkbox"
                  :checked="Array.isArray(answers[question.id]) && answers[question.id].includes(option)"
                  :disabled="answerDisabled"
                  @change="updateMultipleAnswer(question.id, option, $event.target.checked)"
                />
                <span>{{ option }}</span>
              </label>
            </div>

            <input
              v-else-if="question.type === 'blank'"
              v-model="answers[question.id]"
              class="academy-assignment-blank"
              type="text"
              :placeholder="question.placeholder"
              :disabled="answerDisabled"
            />

            <textarea
              v-else
              v-model="answers[question.id]"
              :class="['academy-assignment-textarea', { 'is-code': question.type === 'code' }]"
              :placeholder="question.placeholder"
              :rows="question.type === 'code' ? 8 : 5"
              :disabled="answerDisabled"
            ></textarea>
          </article>

          <section v-if="submitResult" class="academy-assignment-result-card academy-exam-result-card">
            <h2>提交结果</h2>
            <p>{{ submitResult.message }}，当前自动得分 {{ submitResult.autoScore }} 分，待批改 {{ submitResult.pendingScore }} 分。</p>
            <ul>
              <li v-for="result in submitResult.questionResults" :key="result.questionId">
                <span>{{ getResultLabel(result) }}</span>
                <strong>{{ result.score }}/{{ result.maxScore }} 分</strong>
                <em>{{ result.message }}</em>
              </li>
            </ul>
          </section>
        </div>

        <aside class="academy-assignment-side academy-exam-side">
          <section>
            <h2>考试信息</h2>
            <dl>
              <div>
                <dt>授课教师</dt>
                <dd>{{ exam.teacher }}</dd>
              </div>
              <div>
                <dt>开始时间</dt>
                <dd>{{ formatDateTime(exam.startsAt) }}</dd>
              </div>
              <div>
                <dt>结束时间</dt>
                <dd>{{ formatDateTime(exam.deadline) }}</dd>
              </div>
              <div>
                <dt>考试时长</dt>
                <dd>{{ exam.durationMinutes || 0 }} 分钟</dd>
              </div>
              <div>
                <dt>总分</dt>
                <dd>{{ totalScore }} 分</dd>
              </div>
            </dl>
          </section>

          <section>
            <h2>答题卡</h2>
            <div class="academy-exam-answer-sheet">
              <a
                v-for="(question, index) in exam.questions"
                :key="`sheet-${question.id}`"
                :href="`#exam-question-${question.id}`"
                :class="{ 'is-answered': isQuestionAnswered(question), 'is-locked': answerDisabled }"
              >
                {{ index + 1 }}
              </a>
            </div>
            <p>绿色表示已作答，点击题号可快速定位。</p>
          </section>

          <section>
            <h2>答题进度</h2>
            <div class="academy-assignment-progress">
              <span :style="{ width: `${progressPercent}%` }"></span>
            </div>
            <p>{{ actionHint }}</p>
          </section>

          <div class="academy-assignment-actions">
            <template v-if="examStarted">
              <button type="button" class="assignment-ghost-button" :disabled="answerDisabled || saving" @click="saveDraft">
                {{ saving ? '保存中...' : '保存草稿' }}
              </button>
              <button
                type="button"
                class="assignment-primary-button"
                :disabled="isSubmitted || isExamEnded || submitting"
                @click="openSubmitDialog"
              >
                {{ isSubmitted ? '已提交' : (submitting ? '提交中...' : '提交试卷') }}
              </button>
            </template>
            <RouterLink v-else class="assignment-primary-button" :to="`/academy/exams/${encodeURIComponent(exam.id)}`">
              返回考试说明
            </RouterLink>
          </div>

          <RouterLink class="academy-assignment-return" to="/academy/exams">返回考试列表</RouterLink>
        </aside>
      </section>
    </template>
  </main>
</template>
