<script setup>
/**
 * 课程作业详情页面组件
 * 展示作业题目列表，支持多种题型答题、草稿保存和作业提交
 * 内置多个示例作业数据作为兜底
 */
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import {
  fetchAcademyAssignment,
  saveAcademyAssignmentDraft,
  submitAcademyAssignment,
} from '../../api/academy'

/**
 * 组件属性定义
 */
const props = defineProps({
  assignmentId: {
    type: String,
    required: true,
  },
})

/**
 * 作业示例数据目录，作为远程数据加载失败时的兜底
 */
const assignmentCatalog = {
  'c-function-practice': {
    id: 'c-function-practice',
    title: '第 3 章函数练习',
    course: 'C语言程序设计（下）',
    teacher: '余月',
    status: '正在进行',
    deadline: '2026-07-10 23:59',
    attemptsLeft: 3,
    duration: '建议 45 分钟',
    description: '围绕函数定义、参数传递、返回值和递归思想完成本次练习，提交后系统会记录你的答题情况。',
    questions: [
      {
        id: 'q1',
        type: 'single',
        label: '单选题',
        score: 10,
        title: '下列关于 C 语言函数返回值的说法，正确的是哪一项？',
        options: [
          '函数必须返回 int 类型',
          'void 函数不能使用 return 语句',
          '函数返回值类型应与函数声明保持一致',
          '函数只能返回基本数据类型',
        ],
      },
      {
        id: 'q2',
        type: 'multiple',
        label: '多选题',
        score: 15,
        title: '关于函数参数传递，下列说法正确的有：',
        options: [
          '形参只在函数调用期间有效',
          '实参和形参可以同名',
          '数组名作为参数时通常传递首元素地址',
          '值传递会直接修改调用处变量本身',
        ],
      },
      {
        id: 'q3',
        type: 'blank',
        label: '填空题',
        score: 10,
        title: '如果函数没有返回值，函数返回类型通常写作 ______。',
        placeholder: '请输入答案',
      },
      {
        id: 'q4',
        type: 'short',
        label: '简答题',
        score: 20,
        title: '请简要说明“函数封装”对程序设计的意义。',
        placeholder: '从代码复用、结构清晰、调试维护等角度作答',
      },
      {
        id: 'q5',
        type: 'code',
        label: '编程题',
        score: 45,
        title: '编写一个函数 maxOfThree，返回三个整数中的最大值。',
        placeholder: 'int maxOfThree(int a, int b, int c) {\n  // 在这里编写代码\n}',
      },
    ],
  },
  'labor-value-discussion': {
    id: 'labor-value-discussion',
    title: '专题讨论：劳动价值',
    course: '劳动通论',
    teacher: '课程团队',
    status: '正在进行',
    deadline: '2026-07-09 22:00',
    attemptsLeft: 1,
    duration: '建议 30 分钟',
    description: '结合课程材料，围绕新时代劳动价值展开观点表达，提交后进入教师互评流程。',
    questions: [
      {
        id: 'q1',
        type: 'short',
        label: '论述题',
        score: 60,
        title: '结合自身专业学习，谈谈你如何理解劳动创造价值。',
        placeholder: '请写出不少于 150 字的观点',
      },
      {
        id: 'q2',
        type: 'multiple',
        label: '多选题',
        score: 20,
        title: '以下哪些属于劳动素养的重要体现？',
        options: ['尊重劳动成果', '具备协作意识', '重视实践能力', '只关注理论成绩'],
      },
      {
        id: 'q3',
        type: 'blank',
        label: '填空题',
        score: 20,
        title: '劳动教育强调树立正确的劳动观、价值观和 ______。',
        placeholder: '请输入答案',
      },
    ],
  },
  'data-cleaning-report': {
    id: 'data-cleaning-report',
    title: '创新实践项目报告',
    course: '创新工程实践',
    teacher: '项目导师',
    status: '已结束',
    deadline: '2026-07-03 18:00',
    attemptsLeft: 0,
    duration: '项目报告',
    description: '本作业用于展示创新实践方案、原型验证过程和项目复盘。',
    questions: [
      {
        id: 'q1',
        type: 'short',
        label: '报告说明',
        score: 40,
        title: '请概述你的创新实践方案。',
        placeholder: '说明问题背景、设计思路和主要结论',
      },
      {
        id: 'q2',
        type: 'code',
        label: '代码片段',
        score: 60,
        title: '粘贴你用于原型验证或数据分析的核心代码。',
        placeholder: '请粘贴 Python / SQL / R / C++ 等代码片段',
      },
    ],
  },
}

/**
 * 响应式状态定义
 */
const fallbackAssignment = assignmentCatalog['c-function-practice']
const remoteAssignment = ref(null)
const loading = ref(true)
const error = ref('')
const submitResult = ref(null)
const assignment = computed(() => remoteAssignment.value || assignmentCatalog[props.assignmentId] || fallbackAssignment)
const answers = ref({})
const submitDialogVisible = ref(false)
const feedbackMessage = ref('')
const feedbackVisible = ref(false)
let feedbackTimer = null

/**
 * 计算作业总分
 */
const totalScore = computed(() =>
  assignment.value.questions.reduce((sum, question) => sum + question.score, 0),
)

/**
 * 计算已作答题目数量
 */
const answeredCount = computed(() =>
  assignment.value.questions.filter((question) => {
    const answer = answers.value[question.id]
    if (Array.isArray(answer)) return answer.length > 0
    return String(answer ?? '').trim().length > 0
  }).length,
)

/**
 * 计算答题进度百分比
 */
const progressPercent = computed(() =>
  Math.round((answeredCount.value / assignment.value.questions.length) * 100),
)

/**
 * 判断作业是否已锁定（已结束且无剩余提交次数）
 */
const isSubmittedLocked = computed(() => assignment.value.status === '已结束' && assignment.value.attemptsLeft <= 0)

/**
 * 题目类型与结果标签的映射关系
 */
const resultLabelMap = {
  single: '单选题得分',
  multiple: '多选题得分',
  blank: '填空题得分',
  short: '简答题批改',
  code: '编程题判题',
}

/**
 * 根据结果获取对应的题目信息
 */
const getResultQuestion = (result) =>
  assignment.value.questions.find((question) => String(question.id) === String(result.questionId))

/**
 * 根据题目类型获取结果标签
 */
const getResultLabel = (result) => {
  const question = getResultQuestion(result)
  if (!question) return '题目得分'
  return resultLabelMap[question.type] || `${question.label || '题目'}得分`
}

/**
 * 更新多选题答案
 */
const updateMultipleAnswer = (questionId, option, checked) => {
  const currentAnswer = Array.isArray(answers.value[questionId]) ? answers.value[questionId] : []
  answers.value = {
    ...answers.value,
    [questionId]: checked
      ? [...currentAnswer, option]
      : currentAnswer.filter((selectedOption) => selectedOption !== option),
  }
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
 * 加载作业详情数据
 */
const loadAssignment = async () => {
  loading.value = true
  error.value = ''
  submitResult.value = null

  try {
    const data = await fetchAcademyAssignment(props.assignmentId, 1)
    remoteAssignment.value = data
    answers.value = data.draftAnswers || {}
  } catch (err) {
    remoteAssignment.value = null
    answers.value = {}
    error.value = err instanceof Error ? err.message : '作业详情加载失败'
  } finally {
    loading.value = false
  }
}

/**
 * 保存作业草稿
 */
const saveDraft = async () => {
  try {
    const result = await saveAcademyAssignmentDraft(assignment.value.id, answers.value, 1)
    showFeedback(result.message || '作业草稿保存成功')
  } catch (err) {
    showFeedback(err instanceof Error ? err.message : '草稿保存失败')
  }
}

/**
 * 打开提交确认对话框
 */
const openSubmitDialog = () => {
  if (isSubmittedLocked.value) return
  submitDialogVisible.value = true
}

/**
 * 关闭提交确认对话框
 */
const closeSubmitDialog = () => {
  submitDialogVisible.value = false
}

/**
 * 提交作业
 */
const submitAssignment = async () => {
  try {
    const result = await submitAcademyAssignment(assignment.value.id, answers.value, 1)
    submitResult.value = result
    submitDialogVisible.value = false
    showFeedback(result.message || '作业提交成功')
  } catch (err) {
    showFeedback(err instanceof Error ? err.message : '作业提交失败')
  }
}

/**
 * 组件挂载时加载作业信息
 */
onMounted(loadAssignment)

/**
 * 监听作业ID变化，重新加载作业信息
 */
watch(() => props.assignmentId, loadAssignment)

/**
 * 组件卸载时清理定时器
 */
onBeforeUnmount(() => {
  window.clearTimeout(feedbackTimer)
})
</script>

<template>
  <main class="academy-main academy-assignment-detail-main">
    <!-- 提交确认对话框 -->
    <Transition name="academy-assignment-dialog">
      <div
        v-if="submitDialogVisible"
        class="academy-assignment-dialog-backdrop"
        role="presentation"
        @click.self="closeSubmitDialog"
      >
        <section class="academy-assignment-dialog" role="dialog" aria-modal="true" aria-label="确认提交作业">
          <h2>确认提交作业</h2>
          <p>当前已完成 {{ answeredCount }}/{{ assignment.questions.length }} 题，提交后会生成作业记录。</p>
          <div class="academy-assignment-dialog-actions">
            <button type="button" class="assignment-ghost-button" @click="closeSubmitDialog">继续检查</button>
            <button type="button" class="assignment-primary-button" @click="submitAssignment">确认提交</button>
          </div>
        </section>
      </div>
    </Transition>

    <!-- 操作反馈提示 -->
    <Transition name="academy-assignment-feedback">
      <div v-if="feedbackVisible" class="academy-assignment-feedback-toast" role="status">
        {{ feedbackMessage }}
      </div>
    </Transition>

    <!-- 加载状态 -->
    <div v-if="loading" class="academy-aggregate-state">正在加载作业详情...</div>
    <!-- 错误状态 -->
    <div v-else-if="error" class="academy-aggregate-state academy-aggregate-state-error">
      <span>{{ error }}</span>
      <button type="button" @click="loadAssignment">重试</button>
    </div>

    <!-- 作业详情主体内容 -->
    <template v-else>
      <!-- 面包屑导航 -->
      <nav class="academy-assignment-breadcrumb" aria-label="作业面包屑">
        <RouterLink to="/academy/assignments">课程作业</RouterLink>
        <span>/</span>
        <strong>{{ assignment.title }}</strong>
      </nav>

      <!-- 作业头部信息 -->
      <section class="academy-assignment-hero">
        <div>
          <span>{{ assignment.course }}</span>
          <h1>{{ assignment.title }}</h1>
          <p>{{ assignment.description }}</p>
        </div>
        <aside>
          <strong>{{ progressPercent }}%</strong>
          <span>已完成 {{ answeredCount }}/{{ assignment.questions.length }} 题</span>
        </aside>
      </section>

      <!-- 作业布局区域 -->
      <section class="academy-assignment-layout">
        <!-- 题目列表区域 -->
        <div class="academy-assignment-question-list">
          <!-- 题目卡片 -->
          <article
            v-for="(question, index) in assignment.questions"
            :key="question.id"
            class="academy-assignment-question-card"
          >
            <header>
              <div>
                <span>{{ question.label }}</span>
                <h2>{{ index + 1 }}. {{ question.title }}</h2>
              </div>
              <strong>{{ question.score }} 分</strong>
            </header>

            <!-- 单选题选项 -->
            <div v-if="question.type === 'single'" class="academy-assignment-options">
              <label v-for="option in question.options" :key="option">
                <input v-model="answers[question.id]" type="radio" :name="question.id" :value="option" />
                <span>{{ option }}</span>
              </label>
            </div>

            <!-- 多选题选项 -->
            <div v-else-if="question.type === 'multiple'" class="academy-assignment-options">
              <label v-for="option in question.options" :key="option">
                <input
                  type="checkbox"
                  :checked="Array.isArray(answers[question.id]) && answers[question.id].includes(option)"
                  @change="updateMultipleAnswer(question.id, option, $event.target.checked)"
                />
                <span>{{ option }}</span>
              </label>
            </div>

            <!-- 填空题输入框 -->
            <input
              v-else-if="question.type === 'blank'"
              v-model="answers[question.id]"
              class="academy-assignment-blank"
              type="text"
              :placeholder="question.placeholder"
            />

            <!-- 简答题/编程题输入框 -->
            <textarea
              v-else
              v-model="answers[question.id]"
              :class="['academy-assignment-textarea', { 'is-code': question.type === 'code' }]"
              :placeholder="question.placeholder"
              :rows="question.type === 'code' ? 8 : 5"
            ></textarea>
          </article>

          <!-- 提交结果展示 -->
          <section v-if="submitResult" class="academy-assignment-result-card">
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

        <!-- 右侧侧边栏 -->
        <aside class="academy-assignment-side">
          <!-- 作业信息 -->
          <section>
            <h2>作业信息</h2>
            <dl>
              <div>
                <dt>授课教师</dt>
                <dd>{{ assignment.teacher }}</dd>
              </div>
              <div>
                <dt>截止时间</dt>
                <dd>{{ assignment.deadline }}</dd>
              </div>
              <div>
                <dt>剩余次数</dt>
                <dd>{{ assignment.attemptsLeft }} 次</dd>
              </div>
              <div>
                <dt>总分</dt>
                <dd>{{ totalScore }} 分</dd>
              </div>
            </dl>
          </section>

          <!-- 答题进度 -->
          <section>
            <h2>答题进度</h2>
            <div class="academy-assignment-progress">
              <span :style="{ width: `${progressPercent}%` }"></span>
            </div>
            <p>{{ isSubmittedLocked ? '作业已结束，仅可查看历史内容。' : '可先保存草稿，确认无误后提交。' }}</p>
          </section>

          <!-- 操作按钮 -->
          <div class="academy-assignment-actions">
            <button type="button" class="assignment-ghost-button" @click="saveDraft">保存草稿</button>
            <button
              type="button"
              class="assignment-primary-button"
              :disabled="isSubmittedLocked"
              @click="openSubmitDialog"
            >
              提交作业
            </button>
          </div>

          <!-- 返回作业列表 -->
          <RouterLink class="academy-assignment-return" to="/academy/assignments">返回作业列表</RouterLink>
        </aside>
      </section>
    </template>
  </main>
</template>
