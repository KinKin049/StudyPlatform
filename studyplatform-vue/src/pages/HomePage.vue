<script setup>
// 平台首页：未登录时作为项目展示门面，登录后作为学习仪表盘入口
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import {
  fetchAcademyAssignments,
  fetchAcademyExams,
  fetchMyAcademyCourses,
  fetchTeacherWorkbench,
} from '../api/academy'
import { getStoredAuthUser } from '../api/auth'
import { fetchProfileOverview } from '../api/profile'
import academyFutureImage from '../assets/home/academy.jpg'
import gamesFutureImage from '../assets/home/games.jpg'
import labFutureImage from '../assets/home/lab-oj.jpg'
import practiceFutureImage from '../assets/home/practice.jpg'

const authUser = ref(getStoredAuthUser())
const overview = ref(null)
const teacherWorkbench = ref(null)
const recommendedAssignments = ref([])
const recommendedExams = ref([])
const recommendedCourses = ref([])
const recommendationsLoading = ref(false)
const overviewLoading = ref(false)
const overviewError = ref('')
const modulesRef = ref(null)
const moduleStageRef = ref(null)
const activeModuleIndex = ref(0)
const moduleScrollPhase = ref(0)
const activeDashboardSlide = ref(0)
const dashboardSlideCount = 4
let dashboardCarouselTimer = null

const handleRecommendationPointerMove = (event) => {
  const card = event.currentTarget
  if (!card) return
  const rect = card.getBoundingClientRect()
  const x = event.clientX - rect.left
  const y = event.clientY - rect.top
  const centerX = x - rect.width / 2
  const centerY = y - rect.height / 2
  const ratioX = centerX / (rect.width / 2)
  const ratioY = centerY / (rect.height / 2)

  card.style.setProperty('--pointer-x', `${x}px`)
  card.style.setProperty('--pointer-y', `${y}px`)
  card.style.setProperty('--tilt-x', `${(-ratioY * 7).toFixed(2)}deg`)
  card.style.setProperty('--tilt-y', `${(ratioX * 9).toFixed(2)}deg`)
  card.style.setProperty('--float-x', `${(ratioX * 7).toFixed(2)}px`)
  card.style.setProperty('--float-y', `${(ratioY * 5).toFixed(2)}px`)
  card.style.setProperty('--glow-scale', '1')
}

const handleRecommendationPointerLeave = (event) => {
  const card = event.currentTarget
  if (!card) return
  card.style.setProperty('--tilt-x', '0deg')
  card.style.setProperty('--tilt-y', '0deg')
  card.style.setProperty('--float-x', '0px')
  card.style.setProperty('--float-y', '0px')
  card.style.setProperty('--glow-scale', '0.72')
}

const emptyOverview = {
  stats: [
    { label: '累计学习', value: '0m', hint: '完成学习后自动统计' },
    { label: '今日学习', value: '0m', hint: '今天的真实学习时长' },
    { label: '练习记录', value: '0', hint: '答题 / 背词 / 查答案' },
  ],
  overallProgress: 0,
  difficultyStats: [
    { label: '选择题', solved: 0, total: 0, color: '#4f6fd8' },
    { label: '词汇卡片', solved: 0, total: 0, color: '#79c36a' },
    { label: '主观题', solved: 0, total: 0, color: '#f2c04c' },
  ],
  codingDifficulties: [
    { label: '简单', level: 'EASY', solved: 0, total: 0, color: '#5fbf9f' },
    { label: '中等', level: 'MEDIUM', solved: 0, total: 0, color: '#f2c04c' },
    { label: '困难', level: 'HARD', solved: 0, total: 0, color: '#e87575' },
  ],
}

const practicePalette = ['#EFBBCF', '#C3AED6', '#8675A9']
const modules = [
  {
    key: 'academy',
    eyebrow: 'Academy',
    title: '在线学堂',
    copy: '聚合开放课程、通识课程、微专业、视频学习和精品教材，让课程资源、加入学习、教材订单形成一条完整路径。',
    path: '/academy/home',
    action: '进入学堂',
    tone: 'green',
    background: '#f4fadf',
    image: academyFutureImage,
    visualTitle: 'Course Hub',
    visualMeta: '课程 · 视频 · 教材',
    highlights: ['课程资源', '视频学习', '精品教材'],
  },
  {
    key: 'question-bank',
    eyebrow: 'Practice',
    title: '题库练习',
    copy: '围绕课程题库、错题本、收藏题目和英语词汇训练组织练习反馈，让每一次答题都能沉淀为学习画像。',
    path: '/academy/question-bank',
    action: '开始练习',
    tone: 'blue',
    background: '#e8f6fa',
    image: practiceFutureImage,
    visualTitle: 'Practice Flow',
    visualMeta: '错题 · 收藏 · 词汇',
    highlights: ['课程题库', '错题复习', '词汇训练'],
  },
  {
    key: 'lab',
    eyebrow: 'Lab & OJ',
    title: '实验与 OJ',
    copy: '把算法可视化、空间模型、石油气仿真和在线编程判题放到同一实验入口中，支持从观察到提交的完整实践。',
    path: '/lab',
    action: '进入实验',
    tone: 'violet',
    background: '#f0edff',
    image: labFutureImage,
    visualTitle: 'Simulation Lab',
    visualMeta: '算法 · 空间 · 石油气',
    highlights: ['可视化', '仿真实验', '在线判题'],
  },
  {
    key: 'games',
    eyebrow: 'Games',
    title: '学习游戏',
    copy: '通过万题天梯跳与 Type Warrior 降低练习负担，把闯关、打字、金币激励连接成更轻量的学习循环。',
    path: '/games',
    action: '玩一局',
    tone: 'amber',
    background: '#fff2d9',
    image: gamesFutureImage,
    visualTitle: 'Game Loop',
    visualMeta: '闯关 · 打字 · 激励',
    highlights: ['万题天梯跳', 'Type Warrior', '金币激励'],
  },
]

const isLoggedIn = computed(() => Boolean(authUser.value?.id))
const isTeacher = computed(() => authUser.value?.roleType === 'teacher')
const displayName = computed(() => authUser.value?.username || '同学')
const dashboard = computed(() => overview.value || emptyOverview)
const stats = computed(() => dashboard.value.stats?.length ? dashboard.value.stats.slice(0, 3) : emptyOverview.stats)
const findStatValue = (keyword, fallback) =>
  dashboard.value.stats?.find((item) => item.label?.includes(keyword))?.value ?? fallback
const applyChartPalette = (items, palette) =>
  items.map((item, index) => ({
    ...item,
    color: palette[index % palette.length],
  }))
const difficultyStats = computed(() =>
  applyChartPalette(
    dashboard.value.difficultyStats?.length ? dashboard.value.difficultyStats : emptyOverview.difficultyStats,
    practicePalette,
  ),
)
const overallProgress = computed(() => {
  const progress = Number(dashboard.value.overallProgress ?? 0)
  return Math.min(Math.max(Number.isFinite(progress) ? Math.round(progress) : 0, 0), 100)
})
const practiceRingStyle = computed(() => ({
  '--home-practice-progress': `${overallProgress.value}%`,
}))
const donutRenderKey = ref(0)
const teacherWorkbenchMetrics = computed(() => {
  const metrics = teacherWorkbench.value?.metrics?.length
    ? teacherWorkbench.value.metrics
    : [
        { label: '未批改作业数', value: 0, color: '#5fbf9f' },
        { label: '未读评论数', value: 0, color: '#f2c04c' },
        { label: '未批改考试数', value: 0, color: '#e87575' },
      ]

  return metrics.map((item) => ({
    label: item.label,
    solved: Number(item.value ?? item.solved ?? 0),
    total: Number(item.value ?? item.solved ?? 0),
    color: item.color || '#60a5fa',
  }))
})
const firstDashboardStats = computed(() => (isTeacher.value ? teacherWorkbenchMetrics.value : difficultyStats.value))
const firstDashboardTotal = computed(() =>
  firstDashboardStats.value.reduce((sum, item) => sum + Number(item.solved || 0), 0),
)

const buildDonutSegments = (items) => {
  const segments = items.map((item) => ({
    label: item.label,
    solved: Number(item.solved || 0),
    color: item.color || '#60a5fa',
  })).filter((item) => item.solved > 0)

  const solvedTotal = segments.reduce((sum, item) => sum + item.solved, 0)
  if (!solvedTotal) return []

  const gap = segments.length > 1 ? 8 : 0
  const available = 360 - gap * segments.length
  let cursor = 0
  const clampAngle = (value) => Math.max(0, Math.min(value, 360))
  const toAngle = (value) => `${clampAngle(value).toFixed(2)}deg`

  if (gap) {
    cursor = gap / 2
  }

  return segments.map((item, index) => {
    const segmentStart = cursor
    const segmentEnd = cursor + (item.solved / solvedTotal) * available
    cursor = segmentEnd

    if (gap) {
      const gapSize = index === segments.length - 1 ? gap / 2 : gap
      cursor += gapSize
    }

    return {
      label: item.label,
      style: {
        '--home-donut-color': item.color,
        '--home-donut-start': toAngle(segmentStart),
        '--home-donut-end': toAngle(segmentEnd),
        '--home-donut-fill-start': toAngle(segmentEnd),
        '--home-donut-index': index,
      },
    }
  })
}

const practiceDonutSegments = computed(() => buildDonutSegments(firstDashboardStats.value))
const firstDashboardEmptyText = computed(() =>
  isTeacher.value ? '工作已经全部完成啦，休息一下吧~' : '暂无数据',
)
const firstDashboardAriaLabel = computed(() => (
  isTeacher.value
    ? `教师工作台待办总计 ${firstDashboardTotal.value} 项`
    : `练习分布总计 ${firstDashboardTotal.value} 个`
))
const isFinishedSubmission = (item) =>
  ['graded', 'pending_review'].includes(item?.submissionStatus)

const isEndedStatus = (item) => {
  const statusText = String(item?.status || '').toLowerCase()
  if (statusText.includes('ended') || statusText.includes('closed')) return true
  return String(item?.status || '').includes('结束')
}

const getRecommendationCount = (...values) => values
  .map((value) => Number(value ?? 0))
  .find((value) => Number.isFinite(value) && value > 0) || 0

const resourceDetailPath = {
  'online-open-courses': '/academy/open-courses',
  'general-courses': '/academy/general-courses',
  'micro-major-courses': '/academy/micro-majors',
}

const getTimeValue = (value) => {
  const time = new Date(value || 0).getTime()
  return Number.isFinite(time) ? time : 0
}

const getCourseDetailPath = (course) => {
  if (!course?.id) return '/academy/open-courses'
  const basePath = resourceDetailPath[course.resourceType] || '/academy/open-courses'
  return `${basePath}/${encodeURIComponent(course.id)}`
}

const getCourseQuestionBankCode = (course) =>
  course?.questionBankCode || course?.setCode || course?.courseCode || course?.code || ''

const buildQuestionBankCoursePath = (course) => {
  const code = getCourseQuestionBankCode(course)
  return code ? `/academy/question-bank/courses/${encodeURIComponent(code)}` : '/academy/question-bank/courses'
}

const buildQuestionBankFilterPath = (basePath, course) => {
  const code = getCourseQuestionBankCode(course)
  return code ? `${basePath}?setCode=${encodeURIComponent(code)}` : basePath
}

const normalizeRecommendationSubject = (value) =>
  String(value || '')
    .toLowerCase()
    .replace(/[（(].*?[）)]/g, '')
    .replace(/python\s*/g, 'python')
    .replace(/程序设计|程序开发|课程题库|题库|错题本|收藏题目|收藏题|练习|作业|考试|课程|公开课|精品教材|教材/g, '')
    .replace(/[\s·、，,。.:：/\\|_-]+/g, '')
    .trim()

const getRecommendationSubjectKey = (...values) => {
  const normalized = values
    .map(normalizeRecommendationSubject)
    .find((value) => value.length >= 2)
  return normalized ? `subject:${normalized}` : ''
}

const sortedRecommendationCandidates = (candidates) =>
  candidates
    .filter(Boolean)
    .map((item, index) => ({ ...item, score: Number(item.score || 0), order: index }))
    .sort((left, right) => right.score - left.score || left.order - right.order)

const pickTopRecommendations = (candidates, { dedupeSubjects = false } = {}) => {
  const selected = []
  const usedSubjects = new Set()

  for (const item of sortedRecommendationCandidates(candidates)) {
    const subjectKey = item.subjectKey || ''
    if (dedupeSubjects && subjectKey && usedSubjects.has(subjectKey)) continue
    selected.push(item)
    if (dedupeSubjects && subjectKey) usedSubjects.add(subjectKey)
    if (selected.length >= 4) break
  }

  return selected
    .slice(0, 4)
    .map(({ score, order, displayTitle, subjectKey, ...item }) => ({
      ...item,
      title: displayTitle || item.title,
    }))
}

const homeRecommendations = computed(() => {
  if (!isLoggedIn.value) {
    return pickTopRecommendations([
      {
        key: 'login',
        label: '账号',
        title: '登录账号',
        reason: '登录后才能根据你的课程、作业、考试和练习状态生成个性化入口。',
        path: '/login',
        action: '去登录',
        tone: 'blue',
        score: 104,
      },
      {
        key: 'login-profile',
        label: '个人数据',
        title: '登录后同步学习状态',
        reason: '登录后会读取你的课程、收藏、待办和考试信息，再生成推荐。',
        path: '/login',
        action: '登录同步',
        tone: 'green',
        score: 103,
      },
      {
        key: 'login-progress',
        label: '学习进度',
        title: '登录后恢复进度',
        reason: '登录后可以继续查看学习时长、练习分布、成就和排名。',
        path: '/login',
        action: '登录查看',
        tone: 'violet',
        score: 102,
      },
      {
        key: 'login-recommendation',
        label: '推荐',
        title: '登录后开启推荐',
        reason: '当前未登录，只提供登录入口，不推荐课程、题库或其它任务。',
        path: '/login',
        action: '立即登录',
        tone: 'amber',
        score: 101,
      },
    ])
  }

  if (isTeacher.value) {
    const unread = getRecommendationCount(
      teacherWorkbench.value?.unreadComments,
      teacherWorkbench.value?.mailboxUnreadCount,
      teacherWorkbench.value?.metrics?.find((item) => String(item.label || '').includes('评论'))?.value,
    )
    const assignments = getRecommendationCount(
      teacherWorkbench.value?.ungradedAssignments,
      teacherWorkbench.value?.pendingAssignments,
      teacherWorkbench.value?.metrics?.find((item) => String(item.label || '').includes('作业'))?.value,
    )
    const exams = getRecommendationCount(
      teacherWorkbench.value?.ungradedExams,
      teacherWorkbench.value?.pendingExams,
      teacherWorkbench.value?.metrics?.find((item) => String(item.label || '').includes('考试'))?.value,
    )
    const candidates = [
      unread > 0 && {
        key: 'teacher-mailbox',
        label: '信箱',
        title: '教师信箱',
        reason: `有 ${unread} 条未读课程评论，适合优先回复。`,
        path: '/teacher-mailbox',
        action: '查看信箱',
        tone: 'amber',
        score: 120 + unread * 8,
      },
      assignments > 0 && {
        key: 'teacher-assignments',
        label: '作业',
        title: '课程作业',
        reason: `有 ${assignments} 份作业待批改。`,
        path: '/academy/assignments',
        action: '处理作业',
        tone: 'green',
        score: 110 + assignments * 6,
      },
      exams > 0 && {
        key: 'teacher-exams',
        label: '考试',
        title: '我的考试',
        reason: `有 ${exams} 份考试待批改。`,
        path: '/academy/exams',
        action: '处理考试',
        tone: 'blue',
        score: 106 + exams * 6,
      },
      {
        key: 'teacher-profile',
        label: '管理',
        title: '个人主页',
        reason: '管理课程、个人资料和自己发布的 OJ 题目。',
        path: '/profile',
        action: '进入主页',
        tone: 'violet',
        score: 78,
      },
      {
        key: 'teacher-oj',
        label: 'OJ',
        title: '在线 OJ',
        reason: '查看题目运行效果，也可以配合个人主页维护题目。',
        path: '/lab/oj',
        action: '查看 OJ',
        tone: 'blue',
        score: 70,
      },
      {
        key: 'teacher-question-bank',
        displayTitle: '课程题库列表',
        label: '题库',
        title: '课程题库',
        reason: '检查课程题库、错题和收藏题相关学习资源。',
        path: '/academy/question-bank/courses',
        action: '进入题库',
        tone: 'amber',
        score: 66,
      },
      {
        key: 'teacher-courses',
        displayTitle: 'C语言程序设计(下)',
        label: '课程',
        title: '在线学堂',
        reason: '查看课程资源和学生可能访问的学习入口。',
        path: '/academy/open-courses/46004_1476538444',
        action: '进入学堂',
        tone: 'green',
        score: 62,
      },
      {
        key: 'teacher-visualization',
        displayTitle: '单链表逆置动画',
        label: '可视化',
        title: '算法可视化',
        reason: '用可视化演示辅助课程讲解和课堂展示。',
        path: '/visualization/data-structure/single-linked-list-reverse',
        action: '打开演示',
        tone: 'violet',
        score: 54,
      },
      {
        key: 'teacher-textbooks',
        displayTitle: 'C语言程序设计',
        label: '教材',
        title: '精品教材',
        reason: '补充课程配套教材和教学参考内容。',
        path: '/academy/textbooks/23',
        action: '查看教材',
        tone: 'green',
        score: 50,
      },
    ]

    return pickTopRecommendations(candidates)
  }

  const pendingAssignments = recommendedAssignments.value.filter((item) =>
    !isFinishedSubmission(item) && !isEndedStatus(item),
  )
  const activeExams = recommendedExams.value.filter((item) =>
    !isFinishedSubmission(item) && !isEndedStatus(item),
  )
  const courseCount = recommendedCourses.value.length
  const hasPendingAssignments = pendingAssignments.length > 0
  const hasActiveExams = activeExams.length > 0
  const hasCourses = courseCount > 0
  const nextAssignment = [...pendingAssignments]
    .sort((left, right) => getTimeValue(left.deadline) - getTimeValue(right.deadline))[0]
  const nextExam = [...activeExams]
    .sort((left, right) => getTimeValue(left.startsAt || left.deadline) - getTimeValue(right.startsAt || right.deadline))[0]
  const recentCourse = [...recommendedCourses.value]
    .sort((left, right) => getTimeValue(right.enrolledAt) - getTimeValue(left.enrolledAt))[0]
  const focusedCourse = recentCourse || recommendedCourses.value[0]
  const focusedQuestionBankPath = buildQuestionBankCoursePath(focusedCourse)
  const candidates = [
    hasPendingAssignments && {
      key: 'student-assignments',
      displayTitle: nextAssignment?.title || '课程作业',
      subjectKey: getRecommendationSubjectKey(nextAssignment?.course, nextAssignment?.title),
      label: '待办',
      title: '课程作业',
      reason: `还有 ${pendingAssignments.length} 个作业可以继续完成。`,
      path: nextAssignment?.id ? `/academy/assignments/${encodeURIComponent(nextAssignment.id)}` : '/academy/assignments',
      action: '查看作业',
      tone: 'green',
      score: 120 + pendingAssignments.length * 8,
    },
    hasActiveExams && {
      key: 'student-exams',
      displayTitle: nextExam?.title || '我的考试',
      subjectKey: getRecommendationSubjectKey(nextExam?.course, nextExam?.title),
      label: '考试',
      title: '我的考试',
      reason: `有 ${activeExams.length} 场考试可查看或准备。`,
      path: nextExam?.id ? `/academy/exams/${encodeURIComponent(nextExam.id)}` : '/academy/exams',
      action: '进入考试',
      tone: 'blue',
      score: 112 + activeExams.length * 7,
    },
    hasCourses && {
      key: 'student-courses',
      displayTitle: focusedCourse?.name || '我的课程',
      subjectKey: getRecommendationSubjectKey(focusedCourse?.name, focusedCourse?.id),
      label: '课程',
      title: '我的课程',
      reason: `你已加入 ${courseCount} 门课程，可以继续学习。`,
      path: getCourseDetailPath(focusedCourse),
      action: '继续学习',
      tone: 'violet',
      score: 92 + Math.min(courseCount, 6) * 3,
    },
    {
      key: 'question-bank',
      displayTitle: focusedCourse?.name ? `${focusedCourse.name}题库` : '课程题库列表',
      subjectKey: getRecommendationSubjectKey(focusedCourse?.name, getCourseQuestionBankCode(focusedCourse)),
      label: '练习',
      title: '课程题库',
      reason: '用题库、收藏题和错题本补齐最近学习的薄弱点。',
      path: focusedQuestionBankPath,
      action: '开始刷题',
      tone: 'amber',
      score: hasCourses ? 82 : 70,
    },
    {
      key: 'mistakes',
      displayTitle: focusedCourse?.name ? `${focusedCourse.name}错题本` : '错题本',
      subjectKey: getRecommendationSubjectKey(focusedCourse?.name, getCourseQuestionBankCode(focusedCourse)),
      label: '复盘',
      title: '错题本',
      reason: '优先回看做错的题，比随机练习更容易补短板。',
      path: buildQuestionBankFilterPath('/academy/question-bank/mistakes', focusedCourse),
      action: '复习错题',
      tone: 'green',
      score: hasCourses || hasPendingAssignments ? 78 : 58,
    },
    {
      key: 'favorites',
      displayTitle: focusedCourse?.name ? `${focusedCourse.name}收藏题` : '收藏题目',
      subjectKey: getRecommendationSubjectKey(focusedCourse?.name, getCourseQuestionBankCode(focusedCourse)),
      label: '收藏',
      title: '收藏题目',
      reason: '整理重点题目，适合考试或作业前快速回顾。',
      path: buildQuestionBankFilterPath('/academy/question-bank/favorites', focusedCourse),
      action: '查看收藏',
      tone: 'violet',
      score: hasActiveExams ? 76 : 56,
    },
    {
      key: 'online-oj',
      label: '编程',
      title: '在线 OJ',
      reason: '通过编程题训练算法思路和代码提交能力。',
      path: '/lab/oj',
      action: '进入 OJ',
      tone: 'blue',
      score: 72,
    },
    {
      key: 'academy-home',
      displayTitle: focusedCourse?.name || 'C语言程序设计(下)',
      subjectKey: getRecommendationSubjectKey(focusedCourse?.name, focusedCourse?.id),
      label: '学堂',
      title: '在线学堂',
      reason: '浏览公开课、通识课、微专业和课程资源。',
      path: getCourseDetailPath(focusedCourse),
      action: '进入学堂',
      tone: 'green',
      score: hasCourses ? 62 : 80,
    },
    {
      key: 'open-courses',
      displayTitle: 'C语言程序设计(下)',
      subjectKey: getRecommendationSubjectKey('C语言程序设计'),
      label: '公开课',
      title: '在线公开课',
      reason: '从开放课程中扩展新的学习主题。',
      path: hasCourses ? '/academy/open-courses/46004_1476538444' : '/academy/open-courses/46004_1476538444',
      action: '浏览公开课',
      tone: 'green',
      score: hasCourses ? 58 : 74,
    },
    {
      key: 'general-courses',
      displayTitle: '劳动通论',
      subjectKey: getRecommendationSubjectKey('劳动通论'),
      label: '通识',
      title: '通识课程',
      reason: '在专业学习之外补充跨学科知识。',
      path: '/academy/general-courses/general-labor-001',
      action: '查看通识课',
      tone: 'amber',
      score: 52,
    },
    {
      key: 'micro-majors',
      displayTitle: '数据分析微专业',
      subjectKey: getRecommendationSubjectKey('数据分析微专业'),
      label: '微专业',
      title: '微专业课程',
      reason: '围绕一个方向系统推进专项学习。',
      path: '/academy/micro-majors/micro-data-001',
      action: '查看微专业',
      tone: 'violet',
      score: 50,
    },
    {
      key: 'textbooks',
      displayTitle: 'C语言程序设计',
      subjectKey: getRecommendationSubjectKey('C语言程序设计'),
      label: '教材',
      title: '精品教材',
      reason: '查找课程配套教材，补充系统学习资料。',
      path: '/academy/textbooks/23',
      action: '查看教材',
      tone: 'green',
      score: hasCourses ? 60 : 48,
    },
    {
      key: 'visualization',
      displayTitle: '单链表逆置动画',
      label: '可视化',
      title: '算法可视化',
      reason: '通过动态过程理解数据结构和算法原理。',
      path: '/visualization/data-structure/single-linked-list-reverse',
      action: '进入可视化',
      tone: 'violet',
      score: 68,
    },
    {
      key: 'function-2d',
      displayTitle: '函数图像',
      label: '函数',
      title: '函数图像',
      reason: '用二维图像观察函数变化和数学关系。',
      path: '/visualization/function-2d',
      action: '查看图像',
      tone: 'blue',
      score: 46,
    },
    {
      key: 'space-models',
      displayTitle: '空间模型',
      label: '空间',
      title: '空间模型',
      reason: '用三维模型辅助理解空间结构。',
      path: '/visualization/space-models',
      action: '打开模型',
      tone: 'violet',
      score: 44,
    },
    {
      key: 'games',
      displayTitle: 'Type Warrior',
      label: '游戏',
      title: '游戏学习',
      reason: '用闯关和打字练习在轻量场景里保持手感。',
      path: '/games/type-warrior',
      action: '开始游戏',
      tone: 'amber',
      score: hasPendingAssignments || hasActiveExams ? 42 : 66,
    },
    {
      key: 'profile',
      label: '主页',
      title: '个人主页',
      reason: '查看学习画像、最近动态和个人资料。',
      path: '/profile',
      action: '查看主页',
      tone: 'green',
      score: 40,
    },
  ]

  return pickTopRecommendations(candidates, { dedupeSubjects: true })
})

const learningOverviewStats = computed(() => {
  const overviewStats = dashboard.value.learningOverview || dashboard.value.academyOverview || {}
  return [
    { label: '已加入课程', value: overviewStats.joinedCourses ?? overviewStats.courseCount ?? 4 },
    { label: '进行中', value: overviewStats.inProgressCourses ?? overviewStats.activeCourses ?? 4 },
    { label: '待完成作业', value: overviewStats.pendingAssignments ?? overviewStats.assignmentCount ?? 1 },
    { label: '即将考试', value: overviewStats.upcomingExams ?? overviewStats.examCount ?? 2 },
  ]
})
const learningTimeStats = computed(() => {
  const timeStats = dashboard.value.learningTime || dashboard.value.timeOverview || {}
  return [
    {
      label: '学习时长',
      value: timeStats.totalText ?? timeStats.total ?? findStatValue('累计学习', '0m'),
      desc: '题库、课程、OJ、可视化和油气仿真累计',
    },
    {
      label: '可视化时长',
      value: timeStats.visualizationText ?? timeStats.visualization ?? '0m',
      desc: '仅统计可视化具体页面',
    },
  ]
})
const gameDataStats = computed(() => {
  const gameStats = dashboard.value.gameStats || dashboard.value.gamesOverview || {}
  const ladder = gameStats.ladderJump || {}
  const typeWarrior = gameStats.typeWarrior || {}
  return [
    {
      label: '游戏总时长',
      value: gameStats.totalDurationText ?? gameStats.totalDuration ?? '0m',
      meta: `总和 ${gameStats.totalDurationText ?? gameStats.totalDuration ?? '0m'} · 最佳 ${gameStats.bestDurationText ?? gameStats.bestDuration ?? '0m'} · 平均 ${gameStats.averageDurationText ?? gameStats.averageDuration ?? '0m'}`,
    },
    {
      label: '万题天梯跳答对题数',
      value: ladder.correctCount ?? ladder.totalCorrect ?? 0,
      meta: `总和 ${ladder.totalCorrect ?? 0} · 最佳 ${ladder.bestCorrect ?? 0} · 平均 ${ladder.averageCorrect ?? '0.0'}`,
    },
    {
      label: 'Type Warrior 得分',
      value: typeWarrior.score ?? typeWarrior.totalScore ?? 0,
      meta: `总和 ${typeWarrior.totalScore ?? 0} · 最佳 ${typeWarrior.bestScore ?? 0} · 平均 ${typeWarrior.averageScore ?? '0.0'}`,
    },
  ]
})
const currentModule = computed(() => modules[activeModuleIndex.value] || modules[0])
const clampNumber = (value, min, max) => Math.min(Math.max(value, min), max)
const moduleShowcaseStyle = computed(() => ({
  '--home-module-bg': currentModule.value.background,
}))
const moduleVisualStyles = computed(() =>
  modules.map((_, index) => {
    const revealProgress = index === modules.length - 1 ? 0 : clampNumber(moduleScrollPhase.value - index, 0, 1)
    const driftProgress = clampNumber(moduleScrollPhase.value - index + 1, 0, 1)

    return {
      '--home-module-image-clip': `${revealProgress * 100}%`,
      '--home-module-image-y': `${60 - driftProgress * 20}%`,
      '--home-module-image-z': modules.length - index,
    }
  }),
)
const refreshDonutAnimation = () => {
  requestAnimationFrame(() => {
    donutRenderKey.value += 1
  })
}
const stopDashboardCarousel = () => {
  if (!dashboardCarouselTimer) return
  window.clearInterval(dashboardCarouselTimer)
  dashboardCarouselTimer = null
}
const setDashboardSlide = (index) => {
  activeDashboardSlide.value = (index + dashboardSlideCount) % dashboardSlideCount
}
const restartDashboardCarousel = () => {
  stopDashboardCarousel()
  if (!isLoggedIn.value) return
  dashboardCarouselTimer = window.setInterval(() => {
    setDashboardSlide(activeDashboardSlide.value + 1)
  }, 2000)
}
const goDashboardSlide = (index) => {
  setDashboardSlide(index)
  restartDashboardCarousel()
}
const shiftDashboardSlide = (offset) => {
  goDashboardSlide(activeDashboardSlide.value + offset)
}

const updateModuleScrollState = () => {
  const scrollTarget = moduleStageRef.value || modulesRef.value
  if (!scrollTarget) return

  const rect = scrollTarget.getBoundingClientRect()
  const scrollableDistance = Math.max(scrollTarget.offsetHeight - window.innerHeight, 1)
  const progress = Math.min(Math.max(-rect.top / scrollableDistance, 0), 1)
  const phase = progress * (modules.length - 1)
  const nextIndex = Math.round(phase)

  moduleScrollPhase.value = phase
  activeModuleIndex.value = Math.min(Math.max(nextIndex, 0), modules.length - 1)
}

const loadRecommendations = async () => {
  recommendedAssignments.value = []
  recommendedExams.value = []
  recommendedCourses.value = []

  if (!isLoggedIn.value || isTeacher.value) return

  recommendationsLoading.value = true
  try {
    const [courses, assignments, exams] = await Promise.all([
      fetchMyAcademyCourses(1),
      fetchAcademyAssignments(1),
      fetchAcademyExams(1),
    ])
    recommendedCourses.value = Array.isArray(courses) ? courses : []
    recommendedAssignments.value = Array.isArray(assignments) ? assignments : []
    recommendedExams.value = Array.isArray(exams) ? exams : []
  } catch (error) {
    recommendedCourses.value = []
    recommendedAssignments.value = []
    recommendedExams.value = []
    console.warn('failed to load home recommendations:', error)
  } finally {
    recommendationsLoading.value = false
  }
}

const loadOverview = async () => {
  if (!isLoggedIn.value) {
    overview.value = null
    teacherWorkbench.value = null
    recommendedAssignments.value = []
    recommendedExams.value = []
    recommendedCourses.value = []
    recommendationsLoading.value = false
    overviewError.value = ''
    return
  }

  overviewLoading.value = true
  recommendationsLoading.value = !isTeacher.value
  overviewError.value = ''
  try {
    const [profileOverview, workbench] = await Promise.all([
      fetchProfileOverview(),
      isTeacher.value ? fetchTeacherWorkbench() : Promise.resolve(null),
    ])
    overview.value = profileOverview
    teacherWorkbench.value = workbench
    await loadRecommendations()
  } catch (error) {
    overview.value = null
    teacherWorkbench.value = null
    recommendedAssignments.value = []
    recommendedExams.value = []
    recommendedCourses.value = []
    recommendationsLoading.value = false
    overviewError.value = '暂时没有读取到学习画像，完成练习后这里会自动刷新。'
    console.warn('failed to load home dashboard:', error)
  } finally {
    overviewLoading.value = false
    refreshDonutAnimation()
  }
}

const handleAuthUpdated = (event) => {
  authUser.value = event.detail || getStoredAuthUser()
}

watch(isLoggedIn, () => {
  loadOverview()
  nextTick(restartDashboardCarousel)
})

onMounted(() => {
  loadOverview()
  restartDashboardCarousel()
  window.addEventListener('study-platform:auth-updated', handleAuthUpdated)
  window.addEventListener('scroll', updateModuleScrollState, { passive: true })
  window.addEventListener('resize', updateModuleScrollState)
  nextTick(updateModuleScrollState)
})

onBeforeUnmount(() => {
  stopDashboardCarousel()
  window.removeEventListener('study-platform:auth-updated', handleAuthUpdated)
  window.removeEventListener('scroll', updateModuleScrollState)
  window.removeEventListener('resize', updateModuleScrollState)
})
</script>

<template>
  <main class="home-main">
    <section class="home-hero" :class="{ 'is-dashboard': isLoggedIn }" aria-labelledby="home-title">
      <div class="home-hero-copy">
        <p v-if="!isLoggedIn" class="home-kicker">StudyPlatform</p>
        <h1 id="home-title">
          {{ isLoggedIn ? `欢迎回来，${displayName}` : '把课程、练习、实验与游戏放进同一个学习空间' }}
        </h1>
        <p v-if="!isLoggedIn" class="home-copy">
          {{
            'StudyPlatform 面向课程学习、题库训练、实验仿真与游戏化练习，帮助学习过程从“打开页面”变成可追踪、可反馈、可激励的成长路径。'
          }}
        </p>
        <div class="home-hero-actions">
          <RouterLink class="home-primary-action" :to="isLoggedIn ? '/profile' : '/login'">
            {{ isLoggedIn ? '查看完整个人主页' : '登录后查看仪表盘' }}
          </RouterLink>
          <RouterLink class="home-secondary-action" :to="isLoggedIn ? '/academy/home' : '/register'">
            {{ isLoggedIn ? '继续学习' : '注册账号' }}
          </RouterLink>
        </div>
      </div>

      <div v-if="isLoggedIn" class="home-hero-rings">
        <article class="home-dashboard-carousel" aria-label="学习仪表盘轮播">
          <button
            type="button"
            class="home-dashboard-carousel-arrow is-prev"
            aria-label="上一页"
            @click="shiftDashboardSlide(-1)"
          >
            ‹
          </button>
          <button
            type="button"
            class="home-dashboard-carousel-arrow is-next"
            aria-label="下一页"
            @click="shiftDashboardSlide(1)"
          >
            ›
          </button>

          <div class="home-dashboard-carousel-window">
            <div
              class="home-dashboard-carousel-track"
              :style="{ transform: `translate3d(${-activeDashboardSlide * 100}%, 0, 0)` }"
            >
              <section class="home-dashboard-slide" :aria-label="isTeacher ? '教师工作台' : '练习分布'">
                <div class="home-dashboard-card is-practice-only">
                  <div class="home-dashboard-card-head">
                    <span>总计 {{ firstDashboardTotal }} {{ isTeacher ? '项' : '个' }}</span>
                  </div>
                  <div class="home-donut-card-body">
                    <div class="home-dashboard-legend">
                      <div v-for="item in firstDashboardStats" :key="item.label">
                        <i :style="{ background: item.color }"></i>
                        <span>{{ item.label }}</span>
                        <small>{{ isTeacher ? `${item.solved} 项` : `${item.solved} / ${item.total}` }}</small>
                      </div>
                    </div>
                    <div class="home-donut-wrap" :key="`practice-${donutRenderKey}`">
                      <div
                        v-if="practiceDonutSegments.length"
                        class="home-donut-ring"
                        role="img"
                        :aria-label="firstDashboardAriaLabel"
                      >
                        <span
                          v-for="segment in practiceDonutSegments"
                          :key="segment.label"
                          class="home-donut-segment"
                          :style="segment.style"
                          aria-hidden="true"
                        ></span>
                      </div>
                      <span
                        v-else
                        :class="['home-donut-empty', { 'is-complete': isTeacher }]"
                      >
                        {{ firstDashboardEmptyText }}
                      </span>
                    </div>
                  </div>
                </div>
              </section>

              <section class="home-dashboard-slide" aria-label="学习概览">
                <div class="home-dashboard-panel">
                  <p>学习概览</p>
                  <h3>今日状态</h3>
                  <div class="home-dashboard-metric-grid">
                    <div v-for="item in learningOverviewStats" :key="item.label">
                      <strong>{{ item.value }}</strong>
                      <span>{{ item.label }}</span>
                    </div>
                  </div>
                </div>
              </section>

              <section class="home-dashboard-slide" aria-label="学习时长">
                <div class="home-dashboard-panel">
                  <p>学习时长</p>
                  <div class="home-dashboard-list">
                    <div v-for="item in learningTimeStats" :key="item.label">
                      <span>{{ item.label }}</span>
                      <strong>{{ item.value }}</strong>
                      <small>{{ item.desc }}</small>
                    </div>
                  </div>
                </div>
              </section>

              <section class="home-dashboard-slide" aria-label="游戏数据">
                <div class="home-dashboard-panel">
                  <p>游戏数据</p>
                  <div class="home-dashboard-list is-compact">
                    <div v-for="item in gameDataStats" :key="item.label">
                      <span>{{ item.label }}</span>
                      <strong>{{ item.value }}</strong>
                      <small>{{ item.meta }}</small>
                    </div>
                  </div>
                </div>
              </section>
            </div>
          </div>

          <div class="home-dashboard-carousel-dots" aria-label="轮播分页">
            <button
              v-for="index in dashboardSlideCount"
              :key="index"
              type="button"
              :class="{ 'is-active': activeDashboardSlide === index - 1 }"
              :aria-label="`切换到第 ${index} 页`"
              @click="goDashboardSlide(index - 1)"
            ></button>
          </div>
        </article>
      </div>

      <div v-else class="home-hero-visual" aria-hidden="true">
        <div class="home-orbit-card is-main">
          <span>Course</span>
          <strong>课堂学习</strong>
        </div>
        <div class="home-orbit-card is-practice">
          <span>Practice</span>
          <strong>题库反馈</strong>
        </div>
        <div class="home-orbit-card is-lab">
          <span>Lab</span>
          <strong>实验探索</strong>
        </div>
      </div>
    </section>

    <section
      ref="modulesRef"
      class="home-modules"
      :style="moduleShowcaseStyle"
      aria-labelledby="home-modules-title"
    >
      <div class="home-section-head">
        <p>{{ isLoggedIn ? 'Continue Learning' : 'Platform Modules' }}</p>
        <h2 id="home-modules-title">{{ isLoggedIn ? '回到你的四条学习路径' : '四个核心模块' }}</h2>
        <span>{{ isLoggedIn ? '选择一个入口继续推进今天的学习。' : '先了解平台能力，再选择适合自己的学习入口。' }}</span>
      </div>

      <div ref="moduleStageRef" class="home-module-stage">
        <div class="home-module-copy-sticky">
          <div class="home-module-copy-window">
            <div class="home-module-copy-track">
              <article v-for="module in modules" :key="module.key" :class="['home-module-card', `is-${module.tone}`]">
                <div class="home-module-copy">
                  <p>{{ module.eyebrow }}</p>
                  <h3>{{ module.title }}</h3>
                  <p>{{ module.copy }}</p>
                  <div class="home-module-tags">
                    <span v-for="tag in module.highlights" :key="tag">{{ tag }}</span>
                  </div>
                  <RouterLink :to="module.path">{{ module.action }}</RouterLink>
                </div>
              </article>
            </div>
          </div>
        </div>

        <div class="home-module-visual-sticky" aria-hidden="true">
          <div class="home-module-visual-window">
            <div class="home-module-visual-track">
              <div
                v-for="(module, index) in modules"
                :key="`visual-${module.key}`"
                :class="['home-module-illustration', `is-${module.tone}`, { 'has-image': module.image }]"
                :style="moduleVisualStyles[index]"
              >
                <img v-if="module.image" :src="module.image" :alt="`${module.title}未来感展示图`" />
                <div class="home-visual-sky"></div>
                <div class="home-visual-grid"></div>
                <div class="home-visual-panel">
                  <span>{{ module.visualTitle }}</span>
                  <strong>{{ module.visualMeta }}</strong>
                </div>
                <i></i>
                <b></b>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="home-module-scroll-spacer" aria-hidden="true">
        <span v-for="module in modules" :key="`spacer-${module.key}`"></span>
      </div>
    </section>

    <section class="home-recommendations" :style="moduleShowcaseStyle" aria-labelledby="home-recommendations-title">
      <div class="home-section-head">
        <p>Recommended</p>
        <h2 id="home-recommendations-title">为你推荐</h2>
        <span>
          {{
            isLoggedIn
              ? (recommendationsLoading ? '正在根据当前数据更新推荐入口。' : '根据当前待办和学习状态生成入口。')
              : '登录后会根据你的课程、作业和考试更新推荐。'
          }}
        </span>
      </div>

      <div class="home-recommendation-corner">个性化推荐</div>

      <div class="home-recommendation-grid">
        <RouterLink
          v-for="item in homeRecommendations"
          :key="item.key"
          :to="item.path"
          :class="['home-recommendation-card', `is-${item.tone}`]"
          @pointermove="handleRecommendationPointerMove"
          @pointerleave="handleRecommendationPointerLeave"
        >
          <span>{{ item.label }}</span>
          <strong>{{ item.title }}</strong>
          <p>{{ item.reason }}</p>
          <small>{{ item.action }}</small>
        </RouterLink>
      </div>
    </section>
  </main>
</template>
