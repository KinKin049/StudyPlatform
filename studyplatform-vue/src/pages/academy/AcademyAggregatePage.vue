<script setup>
/**
 * 课程聚合页面组件
 * 统一管理课程、作业与考试进度，支持分类切换和状态筛选
 * 包含用户信息、学习时长统计和操作提醒等功能
 */
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  createAcademyRandomExam,
  fetchAcademyAssignments,
  fetchAcademyExams,
  fetchMyAcademyCourses,
  unenrollAcademyCourse,
} from '../../api/academy'
import { fetchProfileOverview, fetchProfileUser } from '../../api/profile'
import { resolveResourceUrl } from '../../api/request'

/**
 * 组件属性定义
 */
const props = defineProps({
  variant: {
    type: String,
    default: 'courses',
  },
})

/**
 * 路由实例
 */
const router = useRouter()

/**
 * 状态筛选选项
 */
const statusFilters = ['全部', '正在进行', '即将开始', '已结束']

/**
 * 页面标题映射
 */
const pageTitles = {
  courses: '我的课程',
  assignments: '课程作业',
  exams: '我的考试',
}

/**
 * 响应式状态定义
 */
const activeCategory = ref(props.variant)
const activeStatus = ref('全部')
const myCourses = ref([])
const myCoursesLoading = ref(false)
const myCoursesError = ref('')
const assignments = ref([])
const assignmentsLoading = ref(false)
const assignmentsError = ref('')
const exams = ref([])
const examsLoading = ref(false)
const examsError = ref('')
const randomExamGenerating = ref(false)
const randomExamForm = ref({
  questionCount: 10,
  durationMinutes: 45,
})
const courseSearchKeyword = ref('')
const sidebarFeedback = ref('')
const profileOverview = ref(null)
const profileUser = ref(null)
const profileUserError = ref('')
const studyTimeLoading = ref(false)
const studyTimeError = ref('')
const droppingCourseKey = ref('')
const pendingDropCourse = ref(null)
const dropFeedbackVisible = ref(false)
let dropFeedbackTimer = null
let sidebarFeedbackTimer = null

/**
 * 资源类型与详情页路径的映射
 */
const resourceDetailPath = {
  'online-open-courses': '/academy/open-courses',
  'general-courses': '/academy/general-courses',
  'micro-major-courses': '/academy/micro-majors',
}

/**
 * 监听variant变化，更新当前分类和状态筛选
 */
watch(
  () => props.variant,
  (variant) => {
    activeCategory.value = variant
    activeStatus.value = '全部'
  },
)

/**
 * 获取当前页面标题
 */
const pageTitle = computed(() => pageTitles[props.variant] || pageTitles.courses)
const displayedProfileUser = computed(() => profileUser.value || {
  name: '同学',
  role: '学生',
  avatarUrl: '',
})
const aggregateUserAvatar = computed(() => resolveResourceUrl(displayedProfileUser.value.avatarUrl))
const aggregateUserInitial = computed(() =>
  (displayedProfileUser.value.name || '同').trim().slice(0, 1).toUpperCase(),
)

/**
 * 获取真实学习时长
 */
const realStudyTime = computed(() => {
  if (studyTimeLoading.value) return '加载中'
  const learningTime = profileOverview.value?.learningTimes?.find((item) => item.label === '学习时长')
    || profileOverview.value?.learningTimes?.[0]
  return learningTime?.value || '0m'
})

/**
 * 将课程数据转换为卡片格式
 */
const myCourseCards = computed(() =>
  myCourses.value.map((course) => {
    const detailBasePath = resourceDetailPath[course.resourceType] || '/academy/open-courses'
    const startTime = course.startTime || '开课时间待定'
    const teacher = course.teacher || '授课教师待补充'

    return {
      key: `${course.resourceType}-${course.id}`,
      type: 'courses',
      resourceType: course.resourceType,
      id: course.id,
      title: course.name,
      category: course.category || '未分类',
      status: '正在进行',
      teacher: `${teacher} · ${startTime}`,
      progress: '已加入我的课程',
      meta: course.school || '课程来源待补充',
      link: `${detailBasePath}/${encodeURIComponent(course.id)}`,
      coverImage: resolveResourceUrl(course.cover || course.coverUrl),
      cover: 'linear-gradient(135deg, #7fd8ee, #74ebd5 52%, #acb6e5)',
      canDrop: true,
    }
  }),
)

/**
 * 判断截止时间是否已过
 */
const isDeadlinePassed = (deadline) => {
  if (!deadline) return false
  const deadlineTime = new Date(deadline).getTime()
  return Number.isFinite(deadlineTime) && deadlineTime < Date.now()
}

/**
 * 判断是否在开始时间之前
 */
const isBeforeStart = (startsAt) => {
  if (!startsAt) return false
  const startTime = new Date(startsAt).getTime()
  return Number.isFinite(startTime) && startTime > Date.now()
}

/**
 * 格式化日期时间为MM-DD HH:mm格式
 */
const formatDateTime = (value) => {
  if (!value) return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ').slice(0, 16)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes}`
}

const getTimeValue = (value) => {
  const time = new Date(value || 0).getTime()
  return Number.isFinite(time) ? time : 0
}

const getNearestTimeValue = (...values) => {
  const times = values.map(getTimeValue).filter((time) => time > 0)
  return times.length ? Math.min(...times) : Number.MAX_SAFE_INTEGER
}

const isAssignmentUnfinished = (assignment) => {
  if (assignment.submissionStatus === 'graded' || assignment.submissionStatus === 'pending_review') {
    return false
  }
  return assignment.status !== '已结束' && !isDeadlinePassed(assignment.deadline)
}

const isExamUnfinished = (exam) => {
  if (exam.submissionStatus === 'graded' || exam.submissionStatus === 'pending_review') {
    return false
  }
  return exam.status !== '已结束' && !isDeadlinePassed(exam.deadline)
}

const sortAssignmentsByPriority = (left, right) => {
  const unfinishedDiff = Number(isAssignmentUnfinished(right)) - Number(isAssignmentUnfinished(left))
  if (unfinishedDiff) return unfinishedDiff
  return getNearestTimeValue(left.deadline) - getNearestTimeValue(right.deadline)
}

const sortExamsByPriority = (left, right) => {
  const unfinishedDiff = Number(isExamUnfinished(right)) - Number(isExamUnfinished(left))
  if (unfinishedDiff) return unfinishedDiff
  return getNearestTimeValue(left.startsAt, left.deadline) - getNearestTimeValue(right.startsAt, right.deadline)
}

const getExamSidebarTime = (exam) => {
  if (exam.status === '即将开始' || isBeforeStart(exam.startsAt)) {
    return `开始 ${formatDateTime(exam.startsAt) || '时间待定'}`
  }
  return `截止 ${formatDateTime(exam.deadline) || '时间待定'}`
}

/**
 * 获取作业状态徽章配置
 */
const getAssignmentStatusBadge = (assignment) => {
  if (assignment.submissionStatus === 'graded') {
    return { label: '已完成', tone: 'done', animated: false }
  }
  if (assignment.submissionStatus === 'pending_review') {
    return { label: '待教师批阅', tone: 'review', animated: false }
  }
  if (assignment.status === '已结束' || isDeadlinePassed(assignment.deadline)) {
    return { label: '已结束', tone: 'ended', animated: false }
  }
  return { label: '待完成', tone: 'todo', animated: true }
}

/**
 * 获取考试状态徽章配置
 */
const getExamStatusBadge = (exam) => {
  if (exam.submissionStatus === 'graded') {
    return { label: '已完成', tone: 'done', animated: false }
  }
  if (exam.submissionStatus === 'pending_review') {
    return { label: '待教师批阅', tone: 'review', animated: false }
  }
  if (exam.status === '已结束' || isDeadlinePassed(exam.deadline)) {
    return { label: '已结束', tone: 'ended', animated: false }
  }
  if (exam.status === '即将开始' || isBeforeStart(exam.startsAt)) {
    return { label: '即将开始', tone: 'review', animated: false }
  }
  return { label: '正在进行', tone: 'todo', animated: true }
}

/**
 * 获取考试进度文本
 */
const getExamProgress = (exam) => {
  if (exam.submissionStatus === 'graded') return `已批改 · ${exam.score ?? 0} 分`
  if (exam.submissionStatus === 'pending_review') return `待教师批阅 · 当前 ${exam.score ?? 0} 分`
  if (exam.submissionStatus === 'draft') return '答题中 · 草稿已保存'
  if (exam.submissionStatus === 'in_progress') return '考试已开始'
  if (exam.status === '已结束' || isDeadlinePassed(exam.deadline)) return '考试已结束'
  if (exam.status === '即将开始' || isBeforeStart(exam.startsAt)) return `开始时间 ${formatDateTime(exam.startsAt) || '待定'}`
  return '可进入考试'
}

/**
 * 将状态文本转换为字符数组，用于动画效果
 */
const getStatusLetters = (status) => Array.from(status || '')

const isAssignmentOrExamCard = (card) => card?.type === 'assignments' || card?.type === 'exams'
const shouldShowCardCover = (card) => !isAssignmentOrExamCard(card) || Boolean(card?.coverImage)
const cardCoverStyle = (card) => (card?.cover ? { background: card.cover } : {})

/**
 * 将作业数据转换为卡片格式
 */
const assignmentCards = computed(() =>
  [...assignments.value].sort(sortAssignmentsByPriority).map((assignment) => {
    const deadline = assignment.deadline ? assignment.deadline.replace('T', ' ').slice(0, 16) : '截止时间待定'
    const attempts = assignment.attemptsLeft ?? 0
    const statusBadge = getAssignmentStatusBadge(assignment)
    const progress = assignment.submissionStatus === 'pending_review'
      ? `待教师批改 · 当前 ${assignment.score ?? 0} 分`
      : assignment.submissionStatus === 'graded'
        ? `已批改 · ${assignment.score ?? 0} 分`
        : assignment.submissionStatus === 'draft'
          ? '草稿已保存'
          : '未提交'

    return {
      key: `assignment-${assignment.id}`,
      id: assignment.id,
      type: 'assignments',
      title: assignment.title,
      category: assignment.course || '课程作业',
      status: assignment.status || '正在进行',
      displayStatus: statusBadge.label,
      statusTone: statusBadge.tone,
      statusAnimated: statusBadge.animated,
      teacher: `${assignment.teacher || '教师待定'} · ${deadline}`,
      progress,
      meta: `${assignment.questionCount || 0} 题 · 可提交 ${attempts} 次`,
      link: `/academy/assignments/${encodeURIComponent(assignment.id)}`,
    }
  }),
)

/**
 * 将考试数据转换为卡片格式
 */
const examCards = computed(() =>
  [...exams.value].sort(sortExamsByPriority).map((exam) => {
    const statusBadge = getExamStatusBadge(exam)
    const deadline = formatDateTime(exam.deadline) || '结束时间待定'
    const startsAt = formatDateTime(exam.startsAt) || '开始时间待定'

    return {
      key: `exam-${exam.id}`,
      id: exam.id,
      type: 'exams',
      title: exam.title,
      category: exam.course || '课程考试',
      status: exam.status || '正在进行',
      displayStatus: statusBadge.label,
      statusTone: statusBadge.tone,
      statusAnimated: statusBadge.animated,
      teacher: `${exam.teacher || '教师待定'} · ${exam.status === '即将开始' ? startsAt : deadline}`,
      progress: getExamProgress(exam),
      meta: `${exam.questionCount || 0} 题 · ${exam.durationMinutes || 0} 分钟 · ${exam.totalScore || 100} 分`,
      link: `/academy/exams/${encodeURIComponent(exam.id)}`,
    }
  }),
)

/**
 * 生成分类标签数据
 */
const categoryTabs = computed(() => [
  { key: 'courses', label: '课程', count: myCourseCards.value.length, path: '/academy/my-courses' },
  { key: 'assignments', label: '作业', count: assignmentCards.value.length, path: '/academy/assignments' },
  { key: 'exams', label: '考试', count: examCards.value.length, path: '/academy/exams' },
])

/**
 * 获取所有卡片数据
 */
const allCards = computed(() => [...myCourseCards.value, ...assignmentCards.value, ...examCards.value])

/**
 * 根据分类和状态筛选可见卡片
 */
const visibleCards = computed(() =>
  allCards.value.filter((card) => {
    const matchesCategory = activeCategory.value === 'all' || card.type === activeCategory.value
    const matchesStatus = activeStatus.value === '全部' || card.status === activeStatus.value

    return matchesCategory && matchesStatus
  }),
)

const pendingAssignments = computed(() =>
  assignments.value
    .filter(isAssignmentUnfinished)
    .sort(sortAssignmentsByPriority),
)

const reviewedAssignments = computed(() =>
  assignments.value.filter((assignment) =>
    assignment.submissionStatus === 'graded' || assignment.submissionStatus === 'pending_review',
  ),
)

const upcomingExams = computed(() =>
  exams.value
    .filter(isExamUnfinished)
    .sort(sortExamsByPriority),
)

const recentCourseActions = computed(() =>
  [...myCourses.value]
    .sort((left, right) => getTimeValue(right.enrolledAt) - getTimeValue(left.enrolledAt))
    .slice(0, 3)
    .map((course) => ({
      key: `recent-${course.resourceType}-${course.id}`,
      label: `继续学习 ${course.name}`,
      meta: `${course.teacher || '授课教师待定'} · ${course.startTime || '开课时间待定'}`,
      path: `${resourceDetailPath[course.resourceType] || '/academy/open-courses'}/${encodeURIComponent(course.id)}`,
    })),
)

const courseReminderActions = computed(() => [
  ...pendingAssignments.value.slice(0, 2).map((assignment) => ({
    key: `assignment-reminder-${assignment.id}`,
    label: assignment.title || '未命名作业',
    meta: `${assignment.course || '课程作业'} · 截止 ${formatDateTime(assignment.deadline) || '时间待定'}`,
    path: `/academy/assignments/${encodeURIComponent(assignment.id)}`,
  })),
  ...upcomingExams.value.slice(0, 2).map((exam) => ({
    key: `exam-reminder-${exam.id}`,
    label: exam.title || '未命名考试',
    meta: `${exam.course || '课程考试'} · ${getExamSidebarTime(exam)}`,
    path: `/academy/exams/${encodeURIComponent(exam.id)}`,
  })),
])

const assignmentReminderActions = computed(() =>
  pendingAssignments.value.slice(0, 4).map((assignment) => ({
    key: `assignment-${assignment.id}`,
    label: assignment.title || '未命名作业',
    meta: `${assignment.course || '课程作业'} · 截止 ${formatDateTime(assignment.deadline) || '时间待定'}`,
    path: `/academy/assignments/${encodeURIComponent(assignment.id)}`,
  })),
)

const examReminderActions = computed(() =>
  upcomingExams.value.slice(0, 4).map((exam) => ({
    key: `exam-${exam.id}`,
    label: exam.title || '未命名考试',
    meta: `${exam.course || '课程考试'} · ${getExamSidebarTime(exam)}`,
    path: `/academy/exams/${encodeURIComponent(exam.id)}`,
  })),
)

const sidebarModel = computed(() => {
  if (props.variant === 'assignments') {
    return [
      {
        title: '快捷操作',
        actions: [
          { key: 'assignment-todo', label: '筛选待提交作业', meta: '只看仍需完成的任务', variant: 'assignments', status: '正在进行' },
          { key: 'assignment-feedback', label: '查看已批阅反馈', meta: reviewedAssignments.value.length ? `${reviewedAssignments.value.length} 项已提交记录` : '暂无已批阅反馈', variant: 'assignments', status: '全部' },
          { key: 'assignment-mistakes', label: '进入错题复盘', meta: '跳转题库错题本', path: '/academy/question-bank/mistakes' },
        ],
      },
      {
        title: '推荐下一步',
        actions: assignmentReminderActions.value.slice(0, 2),
        empty: '暂无待处理作业，可以先复盘错题或预习下一节。',
      },
      {
        title: '作业提醒',
        actions: assignmentReminderActions.value,
        empty: '暂无作业提醒。',
      },
    ]
  }

  if (props.variant === 'exams') {
    return [
      {
        title: '随机组卷',
        kind: 'random-exam',
        actions: [],
      },
      {
        title: '快捷操作',
        actions: [
          { key: 'exam-open', label: '查看可进入考试', meta: '筛选当前正在进行的考试', variant: 'exams', status: '正在进行' },
          { key: 'exam-device', label: '检查考试设备', meta: '进入最近一场考试说明页', path: examReminderActions.value[0]?.path || '/academy/exams' },
          { key: 'exam-history', label: '查看历史成绩', meta: '筛选已结束考试', variant: 'exams', status: '已结束' },
        ],
      },
      {
        title: '推荐下一步',
        actions: examReminderActions.value.slice(0, 2),
        empty: '暂无临近考试，可以继续完成课程作业。',
      },
      {
        title: '考试提醒',
        actions: examReminderActions.value,
        empty: '暂无考试提醒。',
      },
    ]
  }

  return [
    {
      title: '快捷操作',
      kind: 'course-search',
      actions: [
        { key: 'recent-activity', label: '查看最近学习记录', meta: '跳转个人主页的最近动态', path: '/profile', hash: '#recent-activity' },
        { key: 'course-category', label: '进入课程分类页', meta: '浏览在线开放课、通识课和微专业', path: '/academy/open-courses' },
      ],
    },
    {
      title: '推荐下一步',
      actions: recentCourseActions.value.length
        ? recentCourseActions.value
        : [
            { key: 'open-courses', label: '浏览在线开放课', meta: '从课程目录选择下一门课', path: '/academy/open-courses' },
            { key: 'general-courses', label: '补充通识课程', meta: '完善跨学科学习结构', path: '/academy/general-courses' },
          ],
    },
    {
      title: '课程提醒',
      actions: courseReminderActions.value,
      empty: '暂无课程提醒。',
    },
  ]
})

/**
 * 获取空状态提示文本
 */
const emptyStateText = computed(() => {
  if (props.variant === 'courses') return '暂无匹配内容，去课程详情页点击“立即参加”后会出现在这里。'
  if (props.variant === 'assignments') return '暂无匹配作业，后续教师发布后会出现在这里。'
  if (props.variant === 'exams') return '暂无匹配考试，后续开放考试后会出现在这里。'
  return '暂无匹配内容。'
})

/**
 * 选择分类并跳转路由
 */
const selectCategory = (tab) => {
  activeCategory.value = tab.key

  if (tab.key !== 'all') {
    router.push(tab.path)
  }
}

const handleCourseSearchSubmit = () => {
  const keyword = courseSearchKeyword.value.trim()
  router.push({
    path: '/academy/home',
    query: keyword ? { keyword } : {},
  })
  showSidebarFeedback(keyword ? `已跳转到在线学堂搜索：${keyword}` : '已跳转到在线学堂首页')
}

const openSidebarAction = (action) => {
  if (action.path) {
    router.push({ path: action.path, hash: action.hash || '' })
    return
  }

  if (action.variant) {
    activeCategory.value = action.variant
    activeStatus.value = action.status || '全部'
    const targetTab = categoryTabs.value.find((tab) => tab.key === action.variant)
    if (targetTab && props.variant !== action.variant) {
      router.push(targetTab.path)
    }
  }
}

const showSidebarFeedback = (message) => {
  sidebarFeedback.value = message
  window.clearTimeout(sidebarFeedbackTimer)
  sidebarFeedbackTimer = window.setTimeout(() => {
    sidebarFeedback.value = ''
  }, 1800)
}

const createRandomExam = async () => {
  if (randomExamGenerating.value) return
  randomExamGenerating.value = true
  examsError.value = ''
  try {
    const exam = await createAcademyRandomExam({
      userId: 1,
      questionCount: Number(randomExamForm.value.questionCount || 10),
      durationMinutes: Number(randomExamForm.value.durationMinutes || 45),
    })
    await loadExams()
    showSidebarFeedback('随机试卷已生成')
    router.push(`/academy/exams/${encodeURIComponent(exam.id)}`)
  } catch (error) {
    examsError.value = error instanceof Error ? error.message : '随机组卷失败'
    showSidebarFeedback(examsError.value)
  } finally {
    randomExamGenerating.value = false
  }
}

/**
 * 加载我的课程列表
 */
const loadMyCourses = async () => {
  myCoursesLoading.value = true
  myCoursesError.value = ''

  try {
    myCourses.value = await fetchMyAcademyCourses(1)
  } catch (error) {
    myCoursesError.value = error instanceof Error ? error.message : '我的课程加载失败'
    myCourses.value = []
  } finally {
    myCoursesLoading.value = false
  }
}

/**
 * 加载课程作业列表
 */
const loadAssignments = async () => {
  assignmentsLoading.value = true
  assignmentsError.value = ''

  try {
    assignments.value = await fetchAcademyAssignments(1)
  } catch (error) {
    assignmentsError.value = error instanceof Error ? error.message : '课程作业加载失败'
    assignments.value = []
  } finally {
    assignmentsLoading.value = false
  }
}

/**
 * 加载我的考试列表
 */
const loadExams = async () => {
  examsLoading.value = true
  examsError.value = ''

  try {
    exams.value = await fetchAcademyExams(1)
  } catch (error) {
    examsError.value = error instanceof Error ? error.message : '我的考试加载失败'
    exams.value = []
  } finally {
    examsLoading.value = false
  }
}

/**
 * 加载用户学习时长
 */
const loadProfileStudyTime = async () => {
  studyTimeLoading.value = true
  studyTimeError.value = ''

  try {
    profileOverview.value = await fetchProfileOverview()
  } catch (error) {
    studyTimeError.value = error instanceof Error ? error.message : '学习时长加载失败'
    profileOverview.value = null
  } finally {
    studyTimeLoading.value = false
  }
}

const loadProfileUser = async () => {
  profileUserError.value = ''
  try {
    profileUser.value = await fetchProfileUser()
  } catch (error) {
    profileUser.value = null
    profileUserError.value = error instanceof Error ? error.message : '个人信息加载失败'
  }
}

/**
 * 打开退课确认对话框
 */
const openDropCourseDialog = (card) => {
  if (!card.canDrop || droppingCourseKey.value) return
  pendingDropCourse.value = card
}

/**
 * 关闭退课确认对话框
 */
const closeDropCourseDialog = () => {
  if (droppingCourseKey.value) return
  pendingDropCourse.value = null
}

/**
 * 确认退课
 */
const confirmDropCourse = async () => {
  const card = pendingDropCourse.value
  if (!card || droppingCourseKey.value) return
  droppingCourseKey.value = card.key
  myCoursesError.value = ''

  try {
    await unenrollAcademyCourse(card.resourceType, card.id, 1)
    myCourses.value = myCourses.value.filter(
      (course) => !(course.resourceType === card.resourceType && String(course.id) === String(card.id)),
    )
    pendingDropCourse.value = null
    dropFeedbackVisible.value = true
    window.clearTimeout(dropFeedbackTimer)
    dropFeedbackTimer = window.setTimeout(() => {
      dropFeedbackVisible.value = false
    }, 1800)
  } catch (error) {
    myCoursesError.value = error instanceof Error ? error.message : '退课失败，请稍后重试'
  } finally {
    droppingCourseKey.value = ''
  }
}

/**
 * 组件挂载时加载所有数据
 */
onMounted(() => {
  loadProfileUser()
  loadProfileStudyTime()
  loadMyCourses()
  loadAssignments()
  loadExams()
})

/**
 * 组件卸载时清理定时器
 */
onBeforeUnmount(() => {
  window.clearTimeout(dropFeedbackTimer)
  window.clearTimeout(sidebarFeedbackTimer)
})
</script>

<template>
  <main class="academy-main academy-aggregate-main">
    <!-- 退课确认对话框 -->
    <Transition name="academy-drop-confirm">
      <div
        v-if="pendingDropCourse"
        class="academy-drop-confirm-backdrop"
        role="presentation"
        @click.self="closeDropCourseDialog"
      >
        <section class="academy-drop-confirm-dialog" role="dialog" aria-modal="true" aria-label="确认退课">
          <h2>确认退课</h2>
          <p>确定要退出「{{ pendingDropCourse.title }}」吗？</p>
          <div class="academy-drop-confirm-actions">
            <button type="button" class="academy-drop-confirm-cancel" @click="closeDropCourseDialog">取消</button>
            <button
              type="button"
              class="academy-drop-confirm-submit"
              :disabled="droppingCourseKey === pendingDropCourse.key"
              @click="confirmDropCourse"
            >
              {{ droppingCourseKey === pendingDropCourse.key ? '退课中...' : '确认退课' }}
            </button>
          </div>
        </section>
      </div>
    </Transition>

    <!-- 退课成功反馈提示 -->
    <Transition name="academy-drop-feedback">
      <div v-if="dropFeedbackVisible" class="academy-drop-feedback-toast" role="status">
        已成功退出课程
      </div>
    </Transition>

    <!-- 用户信息栏 -->
    <section class="academy-aggregate-userbar" aria-label="用户信息">
      <div class="academy-aggregate-user">
        <div class="academy-aggregate-avatar" aria-hidden="true">
          <img v-if="aggregateUserAvatar" :src="aggregateUserAvatar" :alt="displayedProfileUser.name" />
          <span v-else>{{ aggregateUserInitial }}</span>
        </div>
        <div>
          <strong>{{ displayedProfileUser.name }}</strong>
          <span>{{ displayedProfileUser.role }}</span>
          <small v-if="profileUserError">{{ profileUserError }}</small>
        </div>
      </div>

      <div class="academy-aggregate-stat">
        <span>学习时长</span>
        <strong>{{ realStudyTime }}</strong>
        <small v-if="studyTimeError">{{ studyTimeError }}</small>
      </div>
    </section>

    <!-- 主布局区域 -->
    <section class="academy-aggregate-layout">
      <!-- 主体内容区 -->
      <section class="academy-aggregate-body">
        <!-- 页面标题 -->
        <div class="academy-aggregate-heading">
          <div>
            <h1>{{ pageTitle }}</h1>
          </div>
          <span>统一管理课程、作业与考试进度</span>
        </div>

        <!-- 筛选工具区域 -->
        <section class="academy-aggregate-tools" aria-label="课程管理筛选">
          <!-- 分类标签栏 -->
          <div class="academy-aggregate-tabs" aria-label="分类标签栏">
            <button
              v-for="tab in categoryTabs"
              :key="tab.key"
              type="button"
              :class="{ active: activeCategory === tab.key }"
              @click="selectCategory(tab)"
            >
              {{ tab.label }} <strong>{{ tab.count }}</strong>
            </button>
          </div>

          <!-- 状态筛选 -->
          <div class="academy-aggregate-status" aria-label="状态筛选">
            <button
              v-for="status in statusFilters"
              :key="status"
              type="button"
              :class="{ active: activeStatus === status }"
              @click="activeStatus = status"
            >
              {{ status }}
            </button>
          </div>
        </section>

        <!-- 加载状态 -->
        <div v-if="props.variant === 'courses' && myCoursesLoading" class="academy-aggregate-state">
          正在加载我的课程...
        </div>
        <!-- 课程错误状态 -->
        <div v-else-if="props.variant === 'courses' && myCoursesError" class="academy-aggregate-state academy-aggregate-state-error">
          <span>{{ myCoursesError }}</span>
          <button type="button" @click="loadMyCourses">重试</button>
        </div>
        <!-- 作业加载状态 -->
        <div v-else-if="props.variant === 'assignments' && assignmentsLoading" class="academy-aggregate-state">
          正在加载课程作业...
        </div>
        <!-- 作业错误状态 -->
        <div v-else-if="props.variant === 'assignments' && assignmentsError" class="academy-aggregate-state academy-aggregate-state-error">
          <span>{{ assignmentsError }}</span>
          <button type="button" @click="loadAssignments">重试</button>
        </div>
        <!-- 考试加载状态 -->
        <div v-else-if="props.variant === 'exams' && examsLoading" class="academy-aggregate-state">
          正在加载我的考试...
        </div>
        <!-- 考试错误状态 -->
        <div v-else-if="props.variant === 'exams' && examsError" class="academy-aggregate-state academy-aggregate-state-error">
          <span>{{ examsError }}</span>
          <button type="button" @click="loadExams">重试</button>
        </div>
        <!-- 空状态 -->
        <div v-else-if="visibleCards.length === 0" class="academy-aggregate-state">
          {{ emptyStateText }}
        </div>

        <!-- 卡片网格展示区 -->
        <section v-else class="academy-aggregate-grid" aria-label="课程卡片展示区">
          <article
            v-for="card in visibleCards"
            :key="card.key || `${card.type}-${card.title}`"
            :class="['online-course-card', { 'academy-assignment-card': isAssignmentOrExamCard(card) }]"
          >
            <RouterLink :to="card.link">
              <!-- 卡片封面 -->
              <div v-if="shouldShowCardCover(card)" class="academy-aggregate-cover" :style="cardCoverStyle(card)">
                <img v-if="card.coverImage" :src="card.coverImage" :alt="card.title" />
              </div>
              <!-- 卡片内容 -->
              <div class="online-course-card-body">
                <div class="online-course-card-meta">
                  <span>{{ card.category }}</span>
                  <!-- 状态徽章 -->
                  <strong
                    :class="[
                      'academy-assignment-status-badge',
                      card.statusTone ? `is-${card.statusTone}` : '',
                      { 'is-bouncy': card.statusAnimated },
                    ]"
                    :aria-label="card.displayStatus || card.status"
                  >
                    <template v-if="card.statusAnimated">
                      <span
                        v-for="(letter, index) in getStatusLetters(card.displayStatus)"
                        :key="`${card.key}-status-${index}`"
                        :style="{ '--jump-index': index }"
                      >
                        {{ letter }}
                      </span>
                    </template>
                    <template v-else>{{ card.displayStatus || card.status }}</template>
                  </strong>
                </div>
                <h3>{{ card.title }}</h3>
                <dl>
                  <div>
                    <dt>教师/时间</dt>
                    <dd>{{ card.teacher }}</dd>
                  </div>
                  <div>
                    <dt>进度</dt>
                    <dd>{{ card.progress }}</dd>
                  </div>
                </dl>
                <p>{{ card.meta }}</p>
              </div>
            </RouterLink>
            <!-- 退课按钮 -->
            <button
              v-if="card.canDrop"
              class="academy-drop-course-button"
              type="button"
              :disabled="droppingCourseKey === card.key"
              @click.stop="openDropCourseDialog(card)"
            >
              {{ droppingCourseKey === card.key ? '退课中...' : '退课' }}
            </button>
          </article>
        </section>
      </section>

      <!-- 右侧侧边栏 -->
      <aside class="academy-aggregate-sidebar" aria-label="右侧功能分区">
        <section v-for="section in sidebarModel" :key="section.title">
          <h2>{{ section.title }}</h2>

          <form
            v-if="section.kind === 'course-search'"
            class="academy-aggregate-search-form"
            @submit.prevent="handleCourseSearchSubmit"
          >
            <input
              v-model="courseSearchKeyword"
              type="text"
              placeholder="输入课程名、教师、分类或课程 ID"
              aria-label="搜索在线学堂课程"
            />
            <button type="submit">搜索</button>
          </form>

          <form
            v-if="section.kind === 'random-exam'"
            class="academy-random-exam-form"
            @submit.prevent="createRandomExam"
          >
            <label>
              <span>题目数</span>
              <input v-model.number="randomExamForm.questionCount" type="number" min="5" max="30" />
            </label>
            <label>
              <span>时长</span>
              <input v-model.number="randomExamForm.durationMinutes" type="number" min="10" max="180" />
            </label>
            <button type="submit" :disabled="randomExamGenerating">
              {{ randomExamGenerating ? '组卷中...' : '生成试卷' }}
            </button>
            <small>从已选课程关联题库中随机抽题。</small>
          </form>

          <div class="academy-aggregate-side-actions">
            <button
              v-for="action in section.actions"
              :key="action.key"
              type="button"
              class="academy-aggregate-side-button"
              @click="openSidebarAction(action)"
            >
              <strong>{{ action.label }}</strong>
              <span>{{ action.meta }}</span>
            </button>
          </div>

          <p v-if="section.empty && !section.actions.length" class="academy-aggregate-empty-note">
            {{ section.empty }}
          </p>

          <p v-if="section.kind === 'course-search'" class="academy-aggregate-side-note">
            输入内容会带到在线学堂首页搜索框。
          </p>
        </section>

        <p v-if="sidebarFeedback" class="academy-aggregate-side-feedback" role="status">
          {{ sidebarFeedback }}
        </p>
      </aside>
    </section>
  </main>
</template>
