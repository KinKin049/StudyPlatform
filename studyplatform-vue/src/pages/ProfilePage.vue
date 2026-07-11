<script setup>
// 个人主页组件，展示用户学习数据、成就统计和课程管理功能
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  deletePublishedOnlineOpenCourse,
  fetchAcademyCategories,
  fetchMyPublishedOnlineOpenCourses,
  fetchTeacherWorkbench,
  publishOnlineOpenCourse,
} from '../api/academy'
import {
  checkAdminOjProblem,
  createAdminOjProblem,
  fetchAdminOjCategories,
  fetchAdminOjProblem,
  fetchAdminOjProblems,
  updateAdminOjProblem,
} from '../api/admin'
import {
  fetchProfileOverview,
  fetchProfileUser,
  updateProfileUser,
  uploadProfileAvatar,
} from '../api/profile'
import { getStoredAuthUser, storeAuthUser } from '../api/auth'
import { resolveResourceUrl } from '../api/request'
import {
  difficultyOptions,
  formatAlgorithmCategory,
  formatDifficulty,
  statementLanguageOptions,
} from '../oj/catalog'

// 用户信息兜底数据
const fallbackUser = {
  name: 'Kinkin',
  email: '',
  handle: '@study-platform',
  role: 'StudyPlatform 学习者',
  roleType: 'student',
  teacherName: '',
  bio: '在题库、课程、实验与背单词之间来回穿梭，把零散练习沉淀成稳定的学习曲线。',
  location: 'China',
  metaTags: ['目标：稳稳变强'],
  school: 'StudyPlatform',
  avatarUrl: '',
}

// 学习概览数据兜底
const fallbackOverview = {
  stats: [
    { label: '学习时长', value: '0m', hint: '真实累计时长' },
    { label: '今日学习', value: '0m', hint: '今日真实时长' },
    { label: '练习记录', value: '0', hint: '答题 / 查答案 / 背词' },
    { label: '连续学习', value: '0', hint: '天' },
  ],
  overallProgress: 0,
  difficultyStats: [
    { label: '选择题', solved: 0, total: 0, color: '#2dd4bf' },
    { label: '词汇卡片', solved: 0, total: 0, color: '#60a5fa' },
    { label: '主观题', solved: 0, total: 0, color: '#f59e0b' },
  ],
  skillTracks: [
    { name: '英语四六级', progress: 0, solved: '0 / 0', tone: 'cyan' },
    { name: '公共课', progress: 0, solved: '0 / 0', tone: 'blue' },
    { name: '计算机专业', progress: 0, solved: '0 / 0', tone: 'violet' },
    { name: '职业资格', progress: 0, solved: '0 / 0', tone: 'amber' },
  ],
  recentActivities: [
    { title: '暂无真实练习记录', meta: '完成一道题或标记一个单词后，这里会自动刷新' },
  ],
  badges: [],
  activityDays: Array.from({ length: 119 }, (_, index) => ({
    id: index,
    level: 0,
    count: 0,
  })),
  learningTimes: [
    { label: '学习时长', value: '0m', hint: '暂无学习时长记录', tone: 'cyan' },
    { label: '可视化时长', value: '0m', hint: '暂无可视化时长记录', tone: 'violet' },
  ],
  codingDifficulties: [
    { label: '简单', level: 'EASY', solved: 0, total: 0, color: '#00b8a3' },
    { label: '中等', level: 'MEDIUM', solved: 0, total: 0, color: '#ffc01e' },
    { label: '困难', level: 'HARD', solved: 0, total: 0, color: '#ef476f' },
  ],
  gameMetrics: [
    { title: '游戏时长', value: '0m', meta: '暂无游戏时长记录', tone: 'cyan' },
    { title: '跳跃游戏最高纪录', value: '0 层', meta: '暂无最高纪录', tone: 'blue' },
    { title: '节奏游戏最终得分', value: '0', meta: '暂无最终得分', tone: 'violet' },
    { title: '节奏游戏最高连击', value: '0 Combo', meta: '暂无最高连击', tone: 'amber' },
  ],
  mistakeMetrics: [
    { title: '错题本', value: '0 题', meta: '待复习 0 题 · 已掌握 0 题', tone: 'rose' },
    { title: '薄弱知识点', value: '0 个', meta: '暂无薄弱知识点数据', tone: 'amber' },
  ],
  rankingMetrics: [
    { title: '学习时长排名', value: '#0', meta: '累计学习时长 0m · 暂无排名数据', tone: 'cyan' },
    { title: '累计学习时长', value: '0m', meta: '排名依据：全站累计学习时长', tone: 'blue' },
  ],
  achievementMetrics: [
    { title: '成就点数', value: '0', meta: '暂无成就点数', tone: 'violet' },
    { title: '稀有成就', value: '0 枚', meta: '暂无稀有成就', tone: 'amber' },
  ],
  textbookOrders: [
    { title: '购物车', value: '0 本', meta: '购物车暂无教材', tone: 'blue' },
    { title: '待评价', value: '0 本', meta: '已购教材均已评价或暂无已购教材', tone: 'amber' },
  ],
}
// 数据状�?
const overview = ref(null)
const profileUser = ref(null)
const profileLoading = ref(false)
const profileError = ref('')
const userError = ref('')
const editingProfile = ref(false)
const savingProfile = ref(false)
const avatarUploading = ref(false)
const avatarInputRef = ref(null)
const avatarCropFrameRef = ref(null)
const avatarCropImageRef = ref(null)
const avatarCropVisible = ref(false)
const avatarCropImageUrl = ref('')
const avatarCropZoom = ref(1)
const avatarCropOffset = ref({ x: 0, y: 0 })
const avatarCropBaseSize = ref({ width: 0, height: 0 })
const avatarCropDragging = ref(false)
const avatarCropDragStart = ref({ x: 0, y: 0, offsetX: 0, offsetY: 0 })
const feedbackMessage = ref('')
const feedbackVisible = ref(false)
const teacherCourses = ref([])
const teacherCoursesLoading = ref(false)
const teacherCoursesError = ref('')
const teacherWorkbench = ref(null)
const teacherWorkbenchLoading = ref(false)
const teacherWorkbenchError = ref('')
const deletingCourseId = ref('')
const classAssignments = ref({})
const publishingCourse = ref(false)
const publishCourseDialogOpen = ref(false)
const publishCourseError = ref('')
const publishCourseCoverFile = ref(null)
const publishCourseVideoFile = ref(null)
const publishCourseCategories = ref([])
const publishCourseForm = ref({
  courseName: '',
  startTime: '',
  category: '',
  semesterPlan: '',
  courseDetail: '',
  courseOverview: '',
})
const teacherOjProblems = ref([])
const teacherOjCategories = ref([])
const teacherOjLoading = ref(false)
const teacherOjSaving = ref(false)
const teacherOjError = ref('')
const teacherOjDialogOpen = ref(false)
const teacherOjDialogMode = ref('create')
const teacherOjCheckResult = ref(null)
const teacherOjForm = ref(emptyTeacherOjForm())
const teacherOjSnapshot = ref('')
const teacherOjCloseConfirm = ref(false)
const teacherOjCaseDeleteConfirm = ref({
  open: false,
  index: -1,
})
const achievementDialogOpen = ref(false)
let feedbackTimer = null
let activeTiltElements = new Set()
const profileForm = ref({
  name: fallbackUser.name,
  email: fallbackUser.email,
  bio: fallbackUser.bio,
  location: fallbackUser.location,
  metaTags: [...fallbackUser.metaTags],
  tagDraft: '',
  currentPassword: '',
  newPassword: '',
  confirmNewPassword: '',
})
const courseDeleteConfirm = ref({
  open: false,
  course: null,
})

// 卡片倾斜效果选择�?
const profileTiltSelector = [
  '.profile-card',
  '.profile-summary',
  '.profile-panel',
  '.profile-stats article',
  '.profile-time-list > div',
  '.profile-preview-list > div',
  '.profile-coding-list > div',
  '.profile-difficulty-list > div',
  '.profile-track-item',
  '.profile-activity-list > div',
].join(',')

// 获取当前用户信息（带兜底�?
const user = computed(() => profileUser.value || fallbackUser)
const profileMetaTags = computed(() => {
  const tags = [
    user.value.location,
    ...(Array.isArray(user.value.metaTags) ? user.value.metaTags : []),
  ]
  return tags.map((tag) => String(tag || '').trim()).filter(Boolean)
})
// 判断是否为教师用�?
const isTeacherProfile = computed(() => user.value.roleType === 'teacher')
// 教师课程数量
const teacherCourseCount = computed(() => teacherCourses.value.length)
const teacherOjCount = computed(() => teacherOjProblems.value.length)
const ojDifficultyValues = difficultyOptions.map((item) => item.value)
const teacherOjMetaCategories = computed(() => {
  const current = teacherOjCategories.value.map((item) => ({ name: item.name || item.value || item }))
  const currentNames = new Set(current.map((item) => item.name))
  const fallback = [
    ...difficultyOptions.map((item) => ({ name: item.value })),
    ...statementLanguageOptions.map((item) => ({ name: item.value })),
  ].filter((item) => currentNames.has(item.name))
  return uniqueTeacherOjTags([...current, ...fallback].map((item) => item.name)).map((name) => ({ name }))
})
const canCheckTeacherOjCases = computed(() => Boolean(teacherOjForm.value.standardCode?.trim()))
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
    value: Number(item.value ?? 0),
    color: item.color || '#60a5fa',
  }))
})
const teacherWorkbenchTotal = computed(() =>
  teacherWorkbenchMetrics.value.reduce((sum, item) => sum + item.value, 0),
)
const teacherWorkbenchRingStyle = computed(() => {
  const total = teacherWorkbenchTotal.value
  if (!total) {
    return {
      background: 'rgba(226, 232, 240, 0.82)',
    }
  }

  let cursor = 0
  const segments = teacherWorkbenchMetrics.value
    .filter((item) => item.value > 0)
    .map((item) => {
      const start = cursor
      cursor += (item.value / total) * 360
      return `${item.color} ${start.toFixed(2)}deg ${cursor.toFixed(2)}deg`
    })
  return {
    background: `conic-gradient(from -90deg, ${segments.join(', ')})`,
  }
})
// 学习概览数据（带兜底�?
const dashboard = computed(() => overview.value || fallbackOverview)
// 学习统计数据
const stats = computed(() => dashboard.value.stats?.length ? dashboard.value.stats : fallbackOverview.stats)
// 难度统计数据
const difficultyStats = computed(() =>
  dashboard.value.difficultyStats?.length ? dashboard.value.difficultyStats : fallbackOverview.difficultyStats,
)
// 技能轨道数�?
const skillTracks = computed(() =>
  dashboard.value.skillTracks?.length ? dashboard.value.skillTracks : fallbackOverview.skillTracks,
)
// 最近活动记�?
const recentActivities = computed(() =>
  dashboard.value.recentActivities?.length ? dashboard.value.recentActivities : fallbackOverview.recentActivities,
)
// 成就徽章
const badges = computed(() => dashboard.value.badges?.length ? dashboard.value.badges : fallbackOverview.badges)
// 活动热力图数�?
const activityDays = computed(() =>
  dashboard.value.activityDays?.length ? dashboard.value.activityDays : fallbackOverview.activityDays,
)
// 学习时长数据
const learningTimes = computed(() =>
  dashboard.value.learningTimes?.length ? dashboard.value.learningTimes : fallbackOverview.learningTimes,
)
// 编程难度数据
const codingDifficulties = computed(() =>
  dashboard.value.codingDifficulties?.length ? dashboard.value.codingDifficulties : fallbackOverview.codingDifficulties,
)
// 游戏数据
const gameMetrics = computed(() =>
  dashboard.value.gameMetrics?.length ? dashboard.value.gameMetrics : fallbackOverview.gameMetrics,
)
// 获取金币数量
const profileCoinValue = computed(() => {
  const explicitCoins = dashboard.value.coinTotal ?? dashboard.value.coins ?? dashboard.value.goldCoins
  if (explicitCoins !== undefined && explicitCoins !== null) return String(explicitCoins)

  const coinMetric = gameMetrics.value.find((item) => {
    const title = String(item.title || '').toLowerCase()
    return title.includes('金币') || title.includes('coin')
  })

  return coinMetric?.value || '0'
})
// 错题本数�?
const mistakeMetrics = computed(() =>
  dashboard.value.mistakeMetrics?.length ? dashboard.value.mistakeMetrics : fallbackOverview.mistakeMetrics,
)
// 排名数据
const rankingMetrics = computed(() =>
  dashboard.value.rankingMetrics?.length ? dashboard.value.rankingMetrics : fallbackOverview.rankingMetrics,
)
// 成就数据
const achievementMetrics = computed(() => [
  {
    title: '已解锁成就',
    value: `${achievementSummary.value.unlocked} 枚`,
    meta: `共 ${achievementSummary.value.total} 枚 · ${achievementSummary.value.percent}%`,
    tone: 'violet',
  },
  {
    title: '最近徽章',
    value: unlockedAchievements.value[0]?.title || '0 枚',
    meta: unlockedAchievements.value.length ? '来自成就徽章系统' : '暂无已解锁成就',
    tone: 'amber',
  },
])
// 教材订单数据
const textbookOrders = computed(() =>
  dashboard.value.textbookOrders?.length ? dashboard.value.textbookOrders : fallbackOverview.textbookOrders,
)
// 游戏数据预览区域
const gamePreviewSection = computed(() => ({
  key: 'games',
  eyebrow: 'Games',
  title: '游戏数据',
  items: gameMetrics.value,
}))
// 扩展数据预览区域列表
const previewSections = computed(() => [
  { key: 'mistakes', eyebrow: 'Mistakes', title: '错题本', items: mistakeMetrics.value },
  { key: 'ranking', eyebrow: 'Ranking', title: '排名', items: rankingMetrics.value },
  { key: 'achievements', eyebrow: 'Achievements', title: '成就', items: achievementMetrics.value },
  { key: 'orders', eyebrow: 'Textbooks', title: '教材数据', items: textbookOrders.value },
])
// 整体进度百分�?
const overallProgress = computed(() => {
  const explicitProgress = Number(dashboard.value.overallProgress)
  const difficultySolved = difficultyStats.value.reduce((sum, item) => sum + Number(item.solved || 0), 0)
  const difficultyTotal = difficultyStats.value.reduce((sum, item) => sum + Number(item.total || 0), 0)
  const derivedProgress = difficultyTotal > 0 ? Math.round((difficultySolved / difficultyTotal) * 100) : 0
  const progress = Number.isFinite(explicitProgress) && explicitProgress > 0
    ? explicitProgress
    : derivedProgress
  return Math.min(Math.max(Number.isFinite(progress) ? Math.round(progress) : 0, 0), 100)
})
// 进度环样�?
const progressRingStyle = computed(() => ({
  '--profile-progress': `${overallProgress.value}%`,
  '--profile-progress-deg': `${overallProgress.value * 3.6}deg`,
  '--profile-ring-gradient': practiceDistributionGradient.value,
}))
const practiceDistributionItems = computed(() => {
  const solvedTotal = difficultyStats.value.reduce((sum, item) => sum + Number(item.solved || 0), 0)
  const valueKey = solvedTotal > 0 ? 'solved' : 'total'
  return difficultyStats.value
    .map((item, index) => ({
      label: item.label,
      value: Number(item[valueKey] || 0),
      color: item.color || ['#74ebd5', '#60a5fa', '#f59e0b', '#8b5cf6'][index % 4],
    }))
    .filter((item) => item.value > 0)
})
const practiceDistributionTotal = computed(() =>
  practiceDistributionItems.value.reduce((sum, item) => sum + item.value, 0),
)
const practiceDistributionGradient = computed(() => {
  if (!practiceDistributionTotal.value) {
    return 'conic-gradient(rgba(96, 121, 134, 0.16) 0deg 360deg)'
  }

  let cursor = 0
  const segments = practiceDistributionItems.value.map((item, index) => {
    const start = cursor
    cursor += index === practiceDistributionItems.value.length - 1
      ? 360 - cursor
      : (item.value / practiceDistributionTotal.value) * 360
    return `${item.color} ${start.toFixed(2)}deg ${cursor.toFixed(2)}deg`
  })
  return `conic-gradient(from -90deg, ${segments.join(', ')})`
})
// 用户名称首字�?
const userInitial = computed(() => (user.value.name || 'K').trim().slice(0, 1).toUpperCase())
// 用户头像 URL
const avatarSrc = computed(() => resolveResourceUrl(user.value.avatarUrl))
// 编程题目已完成总数
const codingSolvedTotal = computed(() =>
  codingDifficulties.value.reduce((sum, item) => sum + Number(item.solved || 0), 0),
)
// 编程题目总数
const codingQuestionTotal = computed(() =>
  codingDifficulties.value.reduce((sum, item) => sum + Number(item.total || 0), 0),
)
// 编程完成度百分比
const codingCompletion = computed(() => {
  if (!codingQuestionTotal.value) return 0
  return Math.round((codingSolvedTotal.value / codingQuestionTotal.value) * 100)
})
// 编程难度环形进度样式
const statByKeyword = (keyword) =>
  stats.value.find((item) => String(item.label || '').includes(keyword))

const numericFromText = (value) => {
  const normalized = String(value ?? '').replace(/,/g, '')
  const match = normalized.match(/-?\d+(?:\.\d+)?/)
  return match ? Number(match[0]) : 0
}

const durationMinutesFromText = (value) => {
  const text = String(value ?? '').replace(/\s+/g, '')
  const hourMatch = text.match(/(\d+(?:\.\d+)?)(?:h|小时|时)/i)
  const minuteMatch = text.match(/(\d+(?:\.\d+)?)(?:m|分钟|分)/i)
  const secondMatch = text.match(/(\d+(?:\.\d+)?)(?:s|秒)/i)
  if (hourMatch || minuteMatch || secondMatch) {
    return Math.round(
      Number(hourMatch?.[1] || 0) * 60
      + Number(minuteMatch?.[1] || 0)
      + Number(secondMatch?.[1] || 0) / 60,
    )
  }
  return numericFromText(text)
}

const achievementSource = computed(() => {
  const totalLearningMinutes = durationMinutesFromText(
    statByKeyword('学习时长')?.value
    || learningTimes.value.find((item) => String(item.label || '').includes('学习'))?.value,
  )
  const todayLearningMinutes = durationMinutesFromText(
    statByKeyword('今日')?.value
    || learningTimes.value.find((item) => String(item.label || '').includes('今日'))?.value,
  )
  const practiceCount = numericFromText(statByKeyword('练习')?.value)
  const streakDays = numericFromText(statByKeyword('连续')?.value)
  const activeDays = activityDays.value.filter((day) => Number(day.count || 0) > 0 || Number(day.level || 0) > 0).length

  return {
    totalLearningMinutes,
    todayLearningMinutes,
    practiceCount,
    streakDays,
    activeDays,
    overallProgress: overallProgress.value,
    codingSolved: codingSolvedTotal.value,
    coinTotal: numericFromText(profileCoinValue.value),
    unlockedBackendBadges: badges.value.filter((badge) => {
      const text = String(badge || '')
      return text && !text.includes('等待') && !text.includes('准备')
    }),
  }
})

const createAchievement = ({ key, title, description, metric, target, unit = '', tone = 'cyan' }) => {
  const value = Math.max(Number(metric || 0), 0)
  const safeTarget = Math.max(Number(target || 1), 1)
  const progress = Math.min(Math.round((value / safeTarget) * 100), 100)
  return {
    key,
    title,
    description,
    value,
    target: safeTarget,
    unit,
    tone,
    unlocked: value >= safeTarget,
    progress,
  }
}

const allAchievements = computed(() => {
  const source = achievementSource.value
  const generated = [
    createAchievement({ key: 'first-practice', title: '初次练习', description: '完成第 1 次练习记录', metric: source.practiceCount, target: 1, unit: '次', tone: 'cyan' }),
    createAchievement({ key: 'practice-10', title: '稳定刷题', description: '累计完成 10 次练习记录', metric: source.practiceCount, target: 10, unit: '次', tone: 'blue' }),
    createAchievement({ key: 'practice-50', title: '题感养成', description: '累计完成 50 次练习记录', metric: source.practiceCount, target: 50, unit: '次', tone: 'violet' }),
    createAchievement({ key: 'learn-30m', title: '专注半小时', description: '累计学习时长达到 30 分钟', metric: source.totalLearningMinutes, target: 30, unit: '分钟', tone: 'green' }),
    createAchievement({ key: 'learn-120m', title: '两小时沉浸', description: '累计学习时长达到 120 分钟', metric: source.totalLearningMinutes, target: 120, unit: '分钟', tone: 'cyan' }),
    createAchievement({ key: 'today-15m', title: '今日已开工', description: '今日学习时长达到 15 分钟', metric: source.todayLearningMinutes, target: 15, unit: '分钟', tone: 'amber' }),
    createAchievement({ key: 'streak-3', title: '连续三天', description: '连续学习 3 天', metric: source.streakDays, target: 3, unit: '天', tone: 'green' }),
    createAchievement({ key: 'streak-7', title: '一周不断档', description: '连续学习 7 天', metric: source.streakDays, target: 7, unit: '天', tone: 'violet' }),
    createAchievement({ key: 'progress-25', title: '题库探索者', description: '总体题库完成度达到 25%', metric: source.overallProgress, target: 25, unit: '%', tone: 'blue' }),
    createAchievement({ key: 'progress-60', title: '题库推进者', description: '总体题库完成度达到 60%', metric: source.overallProgress, target: 60, unit: '%', tone: 'violet' }),
    createAchievement({ key: 'coding-1', title: 'OJ 起步', description: '完成第 1 道编程题', metric: source.codingSolved, target: 1, unit: '题', tone: 'cyan' }),
    createAchievement({ key: 'coding-10', title: '代码手感', description: '累计完成 10 道编程题', metric: source.codingSolved, target: 10, unit: '题', tone: 'green' }),
    createAchievement({ key: 'active-days-7', title: '活跃一周', description: '近 17 周内累计 7 天有学习活动', metric: source.activeDays, target: 7, unit: '天', tone: 'amber' }),
    createAchievement({ key: 'coin-100', title: '金币入袋', description: '累计获得 100 金币', metric: source.coinTotal, target: 100, unit: '金币', tone: 'amber' }),
  ]

  const backendBadges = source.unlockedBackendBadges.map((badge, index) => ({
    key: `backend-${index}-${badge}`,
    title: badge,
    description: '由学习数据接口返回的已解锁徽章',
    value: 1,
    target: 1,
    unit: '枚',
    tone: 'violet',
    unlocked: true,
    progress: 100,
  }))

  return [...generated, ...backendBadges]
})

const unlockedAchievements = computed(() => allAchievements.value.filter((achievement) => achievement.unlocked))
const achievementPreview = computed(() => {
  const unlocked = unlockedAchievements.value.slice(0, 6)
  return unlocked.length
    ? unlocked
    : [{ key: 'empty-achievement', title: '0 枚', tone: 'violet', unlocked: false }]
})
const achievementSummary = computed(() => ({
  unlocked: unlockedAchievements.value.length,
  total: allAchievements.value.length,
  percent: allAchievements.value.length
    ? Math.round((unlockedAchievements.value.length / allAchievements.value.length) * 100)
    : 0,
}))

const codingRingStyle = computed(() => {
  const safeSolved = Math.max(codingSolvedTotal.value, 1)
  const easy = Number(codingDifficulties.value[0]?.solved || 0) / safeSolved * 180
  const medium = Number(codingDifficulties.value[1]?.solved || 0) / safeSolved * 180
  const hard = Number(codingDifficulties.value[2]?.solved || 0) / safeSolved * 180
  return {
    '--coding-easy-end': `${easy}deg`,
    '--coding-medium-end': `${easy + medium}deg`,
    '--coding-hard-end': `${easy + medium + hard}deg`,
  }
})
// 头像裁剪图片样式
const avatarCropImageStyle = computed(() => ({
  width: `${avatarCropBaseSize.value.width}px`,
  height: `${avatarCropBaseSize.value.height}px`,
  left: `calc(50% + ${avatarCropOffset.value.x}px)`,
  top: `calc(50% + ${avatarCropOffset.value.y}px)`,
  transform: `translate(-50%, -50%) scale(${avatarCropZoom.value})`,
}))

// 显示操作反馈提示
const showFeedback = (message) => {
  feedbackMessage.value = message
  feedbackVisible.value = true
  if (feedbackTimer) {
    clearTimeout(feedbackTimer)
  }
  feedbackTimer = window.setTimeout(() => {
    feedbackVisible.value = false
    feedbackTimer = null
  }, 1800)
}

function emptyTeacherOjCase(sortOrder = 1) {
  return {
    id: null,
    inputData: '',
    expectedOutput: '',
    sample: sortOrder === 1,
    weight: 1,
    sortOrder,
  }
}

function emptyTeacherOjForm() {
  return {
    id: null,
    title: '',
    slug: '',
    category: '',
    description: '',
    inputDescription: '',
    outputDescription: '',
    standardCode: '',
    difficulty: 'EASY',
    timeLimitMs: 1000,
    memoryLimitKb: 262144,
    tags: '',
    status: 'DRAFT',
    testCases: [emptyTeacherOjCase()],
  }
}

function parseTeacherOjTags(tags) {
  return String(tags || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function uniqueTeacherOjTags(values) {
  return Array.from(new Set(values.filter(Boolean)))
}

function writeTeacherOjTags(form, values) {
  form.tags = uniqueTeacherOjTags(values).join(',')
}

function selectedTeacherOjCategories(form = teacherOjForm.value) {
  return uniqueTeacherOjTags([form.category, form.difficulty, ...parseTeacherOjTags(form.tags)])
}

function teacherOjCategorySummary(form = teacherOjForm.value) {
  const labels = selectedTeacherOjCategories(form).map(formatTeacherOjCategoryName).filter(Boolean)
  return labels.length ? labels.join('、') : '请选择OJ分类'
}

function applyTeacherOjCategoryValues(form, values) {
  writeTeacherOjTags(form, values)
  const algorithmCategory = values.find(
    (value) => !ojDifficultyValues.includes(value) && !statementLanguageOptions.some((item) => item.value === value),
  )
  const difficultyCategory = values.find((value) => ojDifficultyValues.includes(value))
  form.category = algorithmCategory || ''
  form.difficulty = difficultyCategory || 'EASY'
  teacherOjCheckResult.value = null
}

function updateTeacherOjCategories(event) {
  const values = Array.from(event.target.selectedOptions).map((option) => option.value)
  applyTeacherOjCategoryValues(teacherOjForm.value, values)
}

function toggleTeacherOjCategory(value, checked) {
  const values = selectedTeacherOjCategories()
  const nextValues = checked
    ? uniqueTeacherOjTags([...values, value])
    : values.filter((item) => item !== value)
  applyTeacherOjCategoryValues(teacherOjForm.value, nextValues)
}

function syncTeacherOjCategory(form = teacherOjForm.value) {
  if (!form.category) return
  const tags = parseTeacherOjTags(form.tags)
  if (!tags.includes(form.category)) {
    writeTeacherOjTags(form, [form.category, ...tags])
  }
}

function ensureTeacherOjFormCategory(form = teacherOjForm.value) {
  const validCategories = teacherOjCategories.value.map((item) => item.name)
  const tags = parseTeacherOjTags(form.tags).filter((tag) => validCategories.includes(tag))
  if (!form.category && tags.length > 0) {
    form.category = tags.find((tag) => !ojDifficultyValues.includes(tag)) || tags[0]
  }
  if ((!form.category || !validCategories.includes(form.category)) && teacherOjCategories.value.length > 0) {
    const category = teacherOjCategories.value.find((item) => !ojDifficultyValues.includes(item.name)) || teacherOjCategories.value[0]
    form.category = category.name
  }
  syncTeacherOjCategory(form)
}

function normalizedTeacherOjTagsForPayload(form = teacherOjForm.value) {
  const tags = parseTeacherOjTags(form.tags)
  return uniqueTeacherOjTags([form.category, form.difficulty, ...tags]).join(',')
}

function teacherOjPayload(form = teacherOjForm.value) {
  const tags = normalizedTeacherOjTagsForPayload(form)
  return {
    id: form.id,
    title: form.title,
    slug: form.slug,
    category: form.category || parseTeacherOjTags(tags)[0] || '',
    description: form.description,
    inputDescription: form.inputDescription,
    outputDescription: form.outputDescription,
    standardCode: String(form.standardCode || ''),
    difficulty: form.difficulty,
    timeLimitMs: Number(form.timeLimitMs || 1000),
    memoryLimitKb: Number(form.memoryLimitKb || 262144),
    tags,
    status: form.status,
    testCases: form.testCases.map((item, index) => ({
      id: item.id || null,
      inputData: item.inputData,
      expectedOutput: item.expectedOutput,
      sample: Boolean(item.sample),
      weight: Number(item.weight || 1),
      sortOrder: index + 1,
    })),
  }
}

function teacherOjDetailToForm(problem) {
  return {
    id: problem.id || null,
    title: problem.title || '',
    slug: problem.slug || '',
    category: problem.category || '',
    description: problem.description || '',
    inputDescription: problem.inputDescription || '',
    outputDescription: problem.outputDescription || '',
    standardCode: problem.standardCode || '',
    difficulty: problem.difficulty || 'EASY',
    timeLimitMs: problem.timeLimitMs || 1000,
    memoryLimitKb: problem.memoryLimitKb || 262144,
    tags: problem.tags || problem.category || '',
    status: problem.status || 'DRAFT',
    testCases: Array.isArray(problem.testCases) && problem.testCases.length > 0
      ? problem.testCases.map((item, index) => ({
        id: item.id || null,
        inputData: item.inputData || '',
        expectedOutput: item.expectedOutput || '',
        sample: Boolean(item.sample),
        weight: item.weight || 1,
        sortOrder: item.sortOrder || index + 1,
      }))
      : [emptyTeacherOjCase()],
  }
}

function captureTeacherOjSnapshot() {
  teacherOjSnapshot.value = JSON.stringify(teacherOjPayload())
}

function isTeacherOjDirty() {
  return teacherOjDialogOpen.value && JSON.stringify(teacherOjPayload()) !== teacherOjSnapshot.value
}

function formatTeacherOjCategoryName(value) {
  const difficulty = difficultyOptions.find((item) => item.value === value)
  if (difficulty) return `${difficulty.label} ${difficulty.labelEn}`
  const language = statementLanguageOptions.find((item) => item.value === value)
  if (language) return `${language.label} ${language.labelEn}`
  return formatAlgorithmCategory(value || '')
}

function formatTeacherOjActualOutput(caseResult) {
  const output = caseResult?.actualOutput
  if (output === null || output === undefined) {
    return '实际输出：沙箱未返回 actualOutput 字段'
  }
  if (String(output).length === 0) {
    return '实际输出：程序输出为空'
  }
  return `实际输出：\n${output}`
}

// 重置单个元素的倾斜效果
const resetTiltElement = (element) => {
  if (!element) return
  element.classList.remove('is-tilting')
  element.style.removeProperty('--profile-tilt-x')
  element.style.removeProperty('--profile-tilt-y')
}

// 重置所有元素的倾斜效果
const resetProfileTilt = () => {
  activeTiltElements.forEach(resetTiltElement)
  activeTiltElements = new Set()
}

// 收集需要应用倾斜效果的元�?
const collectProfileTiltElements = (target, container) => {
  const tiltElements = []
  let current = target?.closest?.(profileTiltSelector)

  while (current && current !== container) {
    if (container.contains(current) && current.matches(profileTiltSelector)) {
      tiltElements.unshift(current)
    }
    current = current.parentElement?.closest?.(profileTiltSelector)
  }

  return tiltElements
}

// 应用元素倾斜效果
const applyProfileTilt = (element, event) => {
  const rect = element.getBoundingClientRect()
  const x = (event.clientX - rect.left) / rect.width
  const y = (event.clientY - rect.top) / rect.height
  const rotateX = (0.5 - y) * 7
  const rotateY = (x - 0.5) * 9

  element.classList.add('is-tilting')
  element.style.setProperty('--profile-tilt-x', `${rotateX.toFixed(2)}deg`)
  element.style.setProperty('--profile-tilt-y', `${rotateY.toFixed(2)}deg`)
}

// 处理页面鼠标移动时的倾斜效果
const handleProfileTilt = (event) => {
  const tiltElements = collectProfileTiltElements(event.target, event.currentTarget)

  if (!tiltElements.length) {
    resetProfileTilt()
    return
  }

  const nextTiltElements = new Set(tiltElements)
  activeTiltElements.forEach((element) => {
    if (!nextTiltElements.has(element)) {
      resetTiltElement(element)
    }
  })

  tiltElements.forEach((element) => applyProfileTilt(element, event))
  activeTiltElements = nextTiltElements
}

// 加载学习概览数据
const loadProfileOverview = async () => {
  profileLoading.value = true
  profileError.value = ''
  try {
    overview.value = await fetchProfileOverview()
  } catch (error) {
    profileError.value = error.message || '真实数据加载失败'
    console.warn('profile overview load failed:', error)
  } finally {
    profileLoading.value = false
  }
}

// 加载教师课程列表
const loadTeacherCourses = async () => {
  teacherCoursesLoading.value = true
  teacherCoursesError.value = ''
  try {
    teacherCourses.value = await fetchMyPublishedOnlineOpenCourses()
  } catch (error) {
    teacherCourses.value = []
    teacherCoursesError.value = error.message || '课程管理数据加载失败'
  } finally {
    teacherCoursesLoading.value = false
  }
}

const loadTeacherWorkbench = async () => {
  teacherWorkbenchLoading.value = true
  teacherWorkbenchError.value = ''
  try {
    teacherWorkbench.value = await fetchTeacherWorkbench()
  } catch (error) {
    teacherWorkbench.value = null
    teacherWorkbenchError.value = error.message || '教师工作台数据加载失败'
  } finally {
    teacherWorkbenchLoading.value = false
  }
}

const loadTeacherOjProblems = async () => {
  teacherOjLoading.value = true
  teacherOjError.value = ''
  try {
    const [problems, categories] = await Promise.all([
      fetchAdminOjProblems(),
      fetchAdminOjCategories(),
    ])
    teacherOjProblems.value = problems
    teacherOjCategories.value = (categories || [])
      .map((category) => ({ name: category.name || category.value || category }))
      .filter((category) => category.name)
    ensureTeacherOjFormCategory()
  } catch (error) {
    teacherOjProblems.value = []
    teacherOjCategories.value = []
    teacherOjError.value = error.message || 'OJ题目管理数据加载失败'
  } finally {
    teacherOjLoading.value = false
  }
}

const openTeacherOjCreateDialog = () => {
  teacherOjDialogMode.value = 'create'
  teacherOjForm.value = emptyTeacherOjForm()
  ensureTeacherOjFormCategory()
  teacherOjCheckResult.value = null
  teacherOjError.value = ''
  teacherOjCloseConfirm.value = false
  teacherOjCaseDeleteConfirm.value = { open: false, index: -1 }
  teacherOjDialogOpen.value = true
  captureTeacherOjSnapshot()
}

const openTeacherOjEditDialog = async (problem) => {
  teacherOjError.value = ''
  teacherOjLoading.value = true
  try {
    const detail = await fetchAdminOjProblem(problem.id)
    teacherOjDialogMode.value = 'edit'
    teacherOjForm.value = teacherOjDetailToForm(detail)
    ensureTeacherOjFormCategory()
    teacherOjCheckResult.value = null
    teacherOjCloseConfirm.value = false
    teacherOjCaseDeleteConfirm.value = { open: false, index: -1 }
    teacherOjDialogOpen.value = true
    captureTeacherOjSnapshot()
  } catch (error) {
    teacherOjError.value = error.message || 'OJ题目详情加载失败'
    showFeedback(teacherOjError.value)
  } finally {
    teacherOjLoading.value = false
  }
}

const requestCloseTeacherOjDialog = () => {
  if (teacherOjSaving.value) return
  if (isTeacherOjDirty()) {
    teacherOjCloseConfirm.value = true
    return
  }
  closeTeacherOjDialog(true)
}

const closeTeacherOjDialog = (force = false) => {
  if (teacherOjSaving.value && !force) return
  teacherOjDialogOpen.value = false
  teacherOjCloseConfirm.value = false
  teacherOjCaseDeleteConfirm.value = { open: false, index: -1 }
  teacherOjCheckResult.value = null
}

const addTeacherOjCase = () => {
  teacherOjForm.value.testCases.push(emptyTeacherOjCase(teacherOjForm.value.testCases.length + 1))
  teacherOjCheckResult.value = null
}

const requestRemoveTeacherOjCase = (index) => {
  teacherOjCaseDeleteConfirm.value = {
    open: true,
    index,
  }
}

const closeTeacherOjCaseDeleteConfirm = () => {
  teacherOjCaseDeleteConfirm.value = { open: false, index: -1 }
}

const confirmRemoveTeacherOjCase = () => {
  const index = teacherOjCaseDeleteConfirm.value.index
  if (index < 0) return
  if (teacherOjForm.value.testCases.length <= 1) {
    teacherOjForm.value.testCases = [emptyTeacherOjCase()]
  } else {
    teacherOjForm.value.testCases.splice(index, 1)
  }
  teacherOjCheckResult.value = null
  closeTeacherOjCaseDeleteConfirm()
  showFeedback('测试点已删除')
}

const checkTeacherOjCases = async () => {
  if (!teacherOjForm.value.standardCode?.trim()) {
    teacherOjError.value = '请先填写标准代码后再校验测试点'
    showFeedback(teacherOjError.value)
    return
  }
  teacherOjSaving.value = true
  teacherOjError.value = ''
  try {
    teacherOjCheckResult.value = await checkAdminOjProblem(teacherOjPayload())
    let filledCount = 0
    teacherOjCheckResult.value?.cases?.forEach((item, index) => {
      if (!teacherOjForm.value.testCases[index]?.expectedOutput?.trim() && item.actualOutput !== null && item.actualOutput !== undefined) {
        teacherOjForm.value.testCases[index].expectedOutput = item.actualOutput
        filledCount += 1
      }
    })
    if (filledCount > 0) {
      teacherOjCheckResult.value = {
        ...teacherOjCheckResult.value,
        message: `${teacherOjCheckResult.value?.message || '校验完成'}，已自动填充 ${filledCount} 个输出样例`,
      }
    }
    showFeedback('OJ测试点已校验')
  } catch (error) {
    teacherOjError.value = error.message || 'OJ测试点校验失败'
    showFeedback(teacherOjError.value)
  } finally {
    teacherOjSaving.value = false
  }
}

const submitTeacherOjProblem = async () => {
  teacherOjSaving.value = true
  teacherOjError.value = ''
  try {
    if (teacherOjDialogMode.value === 'edit' && teacherOjForm.value.id) {
      await updateAdminOjProblem(teacherOjForm.value.id, teacherOjPayload())
    } else {
      await createAdminOjProblem(teacherOjPayload())
    }
    await loadTeacherOjProblems()
    closeTeacherOjDialog(true)
    showFeedback('OJ题目已保存')
  } catch (error) {
    teacherOjError.value = error.message || 'OJ题目保存失败'
    showFeedback(teacherOjError.value)
  } finally {
    teacherOjSaving.value = false
  }
}

// 为课程分配班�?
const assignCourseClass = (course) => {
  const value = classAssignments.value[course.id]?.trim()
  if (!value) {
    showFeedback('请先填写班级名称')
    return
  }
  showFeedback(`已为《${course.name}》分配班级：${value}`)
}

// 打开发布课程对话�?
const openPublishCourseDialog = () => {
  publishCourseDialogOpen.value = true
  publishCourseError.value = ''
  loadPublishCourseCategories()
}

// 关闭发布课程对话�?
const closePublishCourseDialog = () => {
  if (publishingCourse.value) return
  publishCourseDialogOpen.value = false
  publishCourseError.value = ''
}

// 处理课程封面选择
const handlePublishCourseCoverSelected = (event) => {
  publishCourseCoverFile.value = event.target.files?.[0] || null
}

// 处理课程视频选择
const handlePublishCourseVideoSelected = (event) => {
  publishCourseVideoFile.value = event.target.files?.[0] || null
}

// 重置发布课程表单
const resetPublishCourseForm = () => {
  publishCourseForm.value = {
    courseName: '',
    startTime: '',
    category: '',
    semesterPlan: '',
    courseDetail: '',
    courseOverview: '',
  }
  publishCourseCoverFile.value = null
  publishCourseVideoFile.value = null
}

// 提交发布课程表单
const submitPublishCourseFromProfile = async () => {
  publishCourseError.value = ''
  if (!publishCourseCoverFile.value || !publishCourseVideoFile.value) {
    publishCourseError.value = '请上传课程封面和视频'
    return
  }
  if (!publishCourseForm.value.category) {
    publishCourseError.value = '请选择课程分类'
    return
  }
  const formData = new FormData()
  Object.entries(publishCourseForm.value).forEach(([key, value]) => {
    formData.append(key, String(value || '').trim())
  })
  formData.append('cover', publishCourseCoverFile.value)
  formData.append('video', publishCourseVideoFile.value)

  publishingCourse.value = true
  try {
    await publishOnlineOpenCourse(formData)
    resetPublishCourseForm()
    await loadTeacherCourses()
    publishCourseDialogOpen.value = false
    showFeedback('课程发布成功')
  } catch (error) {
    publishCourseError.value = error.message || '课程发布失败'
  } finally {
    publishingCourse.value = false
  }
}

const loadPublishCourseCategories = async () => {
  try {
    publishCourseCategories.value = (await fetchAcademyCategories('online-open-courses'))
      .map((category) => category.name)
      .filter(Boolean)
  } catch (error) {
    publishCourseCategories.value = []
    publishCourseError.value = error.message || '课程分类加载失败'
  }
}

// 删除教师课程
const openTeacherCourseDeleteConfirm = (course) => {
  courseDeleteConfirm.value = {
    open: true,
    course,
  }
}

const closeTeacherCourseDeleteConfirm = () => {
  if (deletingCourseId.value) return
  courseDeleteConfirm.value = {
    open: false,
    course: null,
  }
}

const removeTeacherCourse = async () => {
  const course = courseDeleteConfirm.value.course
  if (!course) return
  deletingCourseId.value = course.id
  teacherCoursesError.value = ''
  try {
    await deletePublishedOnlineOpenCourse(course.id)
    teacherCourses.value = teacherCourses.value.filter((item) => item.id !== course.id)
    courseDeleteConfirm.value = { open: false, course: null }
    showFeedback('课程已删除')
  } catch (error) {
    teacherCoursesError.value = error.message || '删除课程失败'
    showFeedback(teacherCoursesError.value)
  } finally {
    deletingCourseId.value = ''
  }
}

// 应用用户信息更新
const applyProfileUser = (nextUser) => {
  profileUser.value = nextUser
  profileForm.value = {
    name: nextUser?.name || fallbackUser.name,
    email: nextUser?.email || fallbackUser.email,
    bio: nextUser?.bio || fallbackUser.bio,
    location: nextUser?.location || fallbackUser.location,
    metaTags: Array.isArray(nextUser?.metaTags) ? [...nextUser.metaTags] : [...fallbackUser.metaTags],
    tagDraft: '',
    currentPassword: '',
    newPassword: '',
    confirmNewPassword: '',
  }
  syncStoredAuthUser(nextUser)
  window.dispatchEvent(new CustomEvent('study-platform:profile-updated', { detail: nextUser }))
  if (nextUser?.roleType === 'teacher') {
    overview.value = null
    loadTeacherWorkbench()
    loadTeacherCourses()
    loadTeacherOjProblems()
  } else {
    teacherWorkbench.value = null
    loadProfileOverview()
  }
}

// 加载用户资料
const loadProfileUser = async () => {
  userError.value = ''
  try {
    applyProfileUser(await fetchProfileUser())
  } catch (error) {
    userError.value = error.message || '用户资料加载失败'
    console.warn('profile user load failed:', error)
  }
}

// 开始编辑个人资�?
const startProfileEdit = () => {
  profileForm.value = {
    name: user.value.name,
    email: user.value.email || '',
    bio: user.value.bio,
    location: user.value.location || '',
    metaTags: Array.isArray(user.value.metaTags) ? [...user.value.metaTags] : [],
    tagDraft: '',
    currentPassword: '',
    newPassword: '',
    confirmNewPassword: '',
  }
  editingProfile.value = true
  userError.value = ''
}

// 取消编辑个人资料
const cancelProfileEdit = () => {
  editingProfile.value = false
  userError.value = ''
}

const syncStoredAuthUser = (nextUser) => {
  const storedUser = getStoredAuthUser()
  if (!storedUser || !nextUser?.userId || Number(storedUser.id) !== Number(nextUser.userId)) {
    return
  }
  storeAuthUser({
    ...storedUser,
    email: nextUser.email || storedUser.email,
    username: nextUser.name || storedUser.username,
    roleType: nextUser.roleType || storedUser.roleType,
  })
}

const addProfileMetaTag = () => {
  const tag = profileForm.value.tagDraft.trim()
  if (!tag) return
  if (tag.length > 32) {
    userError.value = '标签最多 32 个字'
    showFeedback(userError.value)
    return
  }
  const nextTags = profileForm.value.metaTags.filter((item) => item !== tag)
  if (nextTags.length >= 12) {
    userError.value = '最多添加 12 个标签'
    showFeedback(userError.value)
    return
  }
  profileForm.value.metaTags = [...nextTags, tag]
  profileForm.value.tagDraft = ''
  userError.value = ''
}

const removeProfileMetaTag = (index) => {
  profileForm.value.metaTags = profileForm.value.metaTags.filter((_, currentIndex) => currentIndex !== index)
}

// 保存个人资料编辑
const saveProfileEdit = async () => {
  const name = profileForm.value.name.trim()
  const email = profileForm.value.email.trim()
  const location = profileForm.value.location.trim()
  if (!name) {
    userError.value = '昵称不能为空'
    console.warn('profile user update rejected: blank name')
    showFeedback('昵称不能为空')
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    userError.value = '邮箱格式不正确'
    showFeedback(userError.value)
    return
  }
  if (!location) {
    userError.value = '位置标签不能为空'
    showFeedback(userError.value)
    return
  }
  const passwordTouched = Boolean(
    profileForm.value.currentPassword
    || profileForm.value.newPassword
    || profileForm.value.confirmNewPassword,
  )
  if (passwordTouched) {
    if (!profileForm.value.currentPassword) {
      userError.value = '请填写旧密码'
      showFeedback(userError.value)
      return
    }
    if (profileForm.value.newPassword.length < 6 || profileForm.value.newPassword.length > 72) {
      userError.value = '新密码长度需为 6-72 位'
      showFeedback(userError.value)
      return
    }
    if (profileForm.value.newPassword !== profileForm.value.confirmNewPassword) {
      userError.value = '两次输入的新密码不一致'
      showFeedback(userError.value)
      return
    }
  }
  savingProfile.value = true
  userError.value = ''
  try {
    const updatedUser = await updateProfileUser({
      name,
      email,
      bio: profileForm.value.bio.trim(),
      location,
      metaTags: profileForm.value.metaTags.map((tag) => tag.trim()).filter(Boolean),
      currentPassword: passwordTouched ? profileForm.value.currentPassword : '',
      newPassword: passwordTouched ? profileForm.value.newPassword : '',
      confirmNewPassword: passwordTouched ? profileForm.value.confirmNewPassword : '',
    })
    applyProfileUser(updatedUser)
    console.info('profile user updated successfully:', updatedUser)
    showFeedback('修改个人信息成功')
    editingProfile.value = false
  } catch (error) {
    userError.value = error.message || '资料保存失败'
    console.warn('profile user update failed:', error)
    showFeedback(userError.value)
  } finally {
    savingProfile.value = false
  }
}

// 打开头像选择�?
const openAvatarPicker = () => {
  avatarInputRef.value?.click()
}

// 重置头像裁剪状�?
const resetAvatarCrop = () => {
  const frame = avatarCropFrameRef.value
  const image = avatarCropImageRef.value
  if (!frame || !image?.naturalWidth || !image?.naturalHeight) return
  const frameRect = frame.getBoundingClientRect()
  const baseScale = Math.max(frameRect.width / image.naturalWidth, frameRect.height / image.naturalHeight)
  avatarCropBaseSize.value = {
    width: image.naturalWidth * baseScale,
    height: image.naturalHeight * baseScale,
  }
  avatarCropZoom.value = 1
  avatarCropOffset.value = { x: 0, y: 0 }
}

// 限制头像裁剪偏移量在有效范围�?
const limitAvatarCropOffset = (offset, zoom = avatarCropZoom.value) => {
  const frame = avatarCropFrameRef.value
  if (!frame) return offset
  const frameRect = frame.getBoundingClientRect()
  const renderedWidth = avatarCropBaseSize.value.width * zoom
  const renderedHeight = avatarCropBaseSize.value.height * zoom
  const maxX = Math.max(0, (renderedWidth - frameRect.width) / 2)
  const maxY = Math.max(0, (renderedHeight - frameRect.height) / 2)
  return {
    x: Math.min(Math.max(offset.x, -maxX), maxX),
    y: Math.min(Math.max(offset.y, -maxY), maxY),
  }
}

// 规范化头像裁剪偏移量
const normalizeAvatarCropOffset = () => {
  avatarCropOffset.value = limitAvatarCropOffset(avatarCropOffset.value)
}

// 开始拖动头像裁剪图�?
const startAvatarCropDrag = (event) => {
  if (avatarUploading.value) return
  avatarCropDragging.value = true
  avatarCropDragStart.value = {
    x: event.clientX,
    y: event.clientY,
    offsetX: avatarCropOffset.value.x,
    offsetY: avatarCropOffset.value.y,
  }
  event.currentTarget.setPointerCapture?.(event.pointerId)
}

// 移动头像裁剪图片
const moveAvatarCropDrag = (event) => {
  if (!avatarCropDragging.value) return
  avatarCropOffset.value = limitAvatarCropOffset({
    x: avatarCropDragStart.value.offsetX + event.clientX - avatarCropDragStart.value.x,
    y: avatarCropDragStart.value.offsetY + event.clientY - avatarCropDragStart.value.y,
  })
}

// 停止拖动头像裁剪图片
const stopAvatarCropDrag = (event) => {
  avatarCropDragging.value = false
  event.currentTarget.releasePointerCapture?.(event.pointerId)
}

// 关闭头像裁剪�?
const closeAvatarCropper = (force = false) => {
  if (avatarUploading.value && !force) return
  if (avatarCropImageUrl.value) {
    URL.revokeObjectURL(avatarCropImageUrl.value)
  }
  avatarCropVisible.value = false
  avatarCropImageUrl.value = ''
  avatarCropZoom.value = 1
  avatarCropOffset.value = { x: 0, y: 0 }
  avatarCropBaseSize.value = { width: 0, height: 0 }
  avatarCropDragging.value = false
}

// 处理头像文件选择
const handleAvatarSelected = async (event) => {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!file.type.startsWith('image/')) {
    userError.value = '请选择图片文件'
    console.warn('profile avatar update rejected: non-image file:', file)
    showFeedback(userError.value)
    return
  }
  if (avatarCropImageUrl.value) {
    URL.revokeObjectURL(avatarCropImageUrl.value)
  }
  avatarCropImageUrl.value = URL.createObjectURL(file)
  avatarCropVisible.value = true
  userError.value = ''
}

// 创建裁剪后的头像文件
const createCroppedAvatarFile = async () => {
  const frame = avatarCropFrameRef.value
  const image = avatarCropImageRef.value
  if (!frame || !image?.naturalWidth || !image?.naturalHeight) {
    throw new Error('头像图片尚未加载完成')
  }
  const frameRect = frame.getBoundingClientRect()
  const renderedWidth = avatarCropBaseSize.value.width * avatarCropZoom.value
  const renderedHeight = avatarCropBaseSize.value.height * avatarCropZoom.value
  const renderedLeft = frameRect.width / 2 + avatarCropOffset.value.x - renderedWidth / 2
  const renderedTop = frameRect.height / 2 + avatarCropOffset.value.y - renderedHeight / 2
  const sourceX = (0 - renderedLeft) / renderedWidth * image.naturalWidth
  const sourceY = (0 - renderedTop) / renderedHeight * image.naturalHeight
  const sourceSize = frameRect.width / renderedWidth * image.naturalWidth
  const canvasSize = 512
  const canvas = document.createElement('canvas')
  canvas.width = canvasSize
  canvas.height = canvasSize
  const context = canvas.getContext('2d')
  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, canvasSize, canvasSize)
  context.drawImage(
    image,
    sourceX,
    sourceY,
    sourceSize,
    sourceSize,
    0,
    0,
    canvasSize,
    canvasSize,
  )
  const blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/png'))
  if (!blob) {
    throw new Error('头像裁剪失败')
  }
  return new File([blob], 'avatar-cropped.png', { type: 'image/png' })
}

// 确认头像裁剪并上�?
const confirmAvatarCrop = async () => {
  avatarUploading.value = true
  userError.value = ''
  try {
    const croppedFile = await createCroppedAvatarFile()
    const updatedUser = await uploadProfileAvatar(croppedFile)
    applyProfileUser(updatedUser)
    console.info('profile avatar updated successfully:', {
      fileName: croppedFile.name,
      fileSize: croppedFile.size,
      avatarUrl: updatedUser.avatarUrl,
    })
    showFeedback('头像修改成功')
    closeAvatarCropper(true)
  } catch (error) {
    userError.value = error.message || '头像上传失败'
    console.warn('profile avatar update failed:', error)
    showFeedback(userError.value)
  } finally {
    avatarUploading.value = false
  }
}

onMounted(() => {
  loadProfileUser()
})

onBeforeUnmount(() => {
  if (feedbackTimer) {
    clearTimeout(feedbackTimer)
  }
  resetProfileTilt()
  if (avatarCropImageUrl.value) {
    URL.revokeObjectURL(avatarCropImageUrl.value)
  }
})
</script>

<template>
  <!-- 个人主页主容�?-->
  <main class="profile-main" @pointermove="handleProfileTilt" @pointerleave="resetProfileTilt">
    <!-- 用户信息区域 -->
    <section class="profile-hero">
      <!-- 用户卡片 -->
      <div class="profile-card">
        <!-- 金币数量（学生用户显示） -->
        <div v-if="!isTeacherProfile" class="profile-coin-pill" aria-label="金币数量">
          <span>金币</span>
          <strong>{{ profileCoinValue }}</strong>
        </div>
        <!-- 头像上传按钮 -->
        <button
          class="profile-avatar"
          type="button"
          :disabled="avatarUploading"
          :aria-label="avatarUploading ? '头像上传中' : '上传头像'"
          @click="openAvatarPicker"
        >
          <img v-if="avatarSrc" :src="avatarSrc" :alt="user.name" />
          <span v-else>{{ userInitial }}</span>
          <small>{{ avatarUploading ? '上传中' : '换头像' }}</small>
        </button>
        <!-- 隐藏的头像文件输�?-->
        <input
          ref="avatarInputRef"
          class="profile-avatar-input"
          type="file"
          accept="image/png,image/jpeg,image/webp"
          @change="handleAvatarSelected"
        />
        <!-- 用户身份信息 -->
        <div class="profile-identity">
          <p class="profile-role">{{ user.role }}</p>
          <h1>{{ user.name }}</h1>
          <span>{{ user.handle }}</span>
        </div>
        <!-- 用户简�?-->
        <p class="profile-bio">{{ user.bio }}</p>
        <!-- 编辑资料按钮 -->
        <button class="profile-edit-button" type="button" @click="startProfileEdit">编辑资料</button>
        <!-- 错误提示 -->
        <p v-if="userError && !editingProfile" class="profile-user-message">{{ userError }}</p>
        <!-- 用户元信�?-->
        <div class="profile-meta">
          <span v-if="isTeacherProfile">教师姓名：{{ user.teacherName || user.name }}</span>
          <span v-if="user.school && String(user.school).trim()">{{ isTeacherProfile ? `所属学校：${user.school}` : user.school }}</span>
          <span v-for="tag in profileMetaTags" :key="tag">{{ tag }}</span>
        </div>
      </div>

      <section v-if="isTeacherProfile" class="profile-panel profile-teacher-workbench-panel">
        <div class="profile-panel-head">
          <div>
            <p>Teacher Workbench</p>
            <h2>待办工作台</h2>
          </div>
          <span>{{ teacherWorkbenchTotal }} 项</span>
        </div>
        <div v-if="teacherWorkbenchLoading" class="profile-teacher-state">正在加载工作台...</div>
        <div v-else-if="teacherWorkbenchError" class="profile-teacher-state is-error">
          <span>{{ teacherWorkbenchError }}</span>
          <button type="button" @click="loadTeacherWorkbench">重试</button>
        </div>
        <div v-else class="profile-teacher-workbench-body">
          <div class="profile-teacher-workbench-ring" :style="teacherWorkbenchRingStyle">
            <div class="profile-teacher-workbench-ring-core">
              <strong v-if="teacherWorkbenchTotal">{{ teacherWorkbenchTotal }}</strong>
              <span v-if="teacherWorkbenchTotal">待处理事项</span>
              <span v-else>工作已经全部完成啦，休息一下吧~</span>
            </div>
          </div>
          <div class="profile-teacher-workbench-list">
            <div v-for="item in teacherWorkbenchMetrics" :key="item.label">
              <i :style="{ background: item.color }"></i>
              <span>{{ item.label }}</span>
              <strong>{{ item.value }} 项</strong>
            </div>
          </div>
        </div>
      </section>

      <!-- 教师课程管理面板 -->
      <section v-if="isTeacherProfile" class="profile-panel profile-teacher-course-panel">
        <div class="profile-panel-head">
          <div>
            <p>Course Management</p>
            <h2>课程管理</h2>
          </div>
          <div class="profile-teacher-head-actions">
            <span>{{ teacherCourseCount }} 门课程</span>
            <button type="button" @click="openPublishCourseDialog">添加课程</button>
          </div>
        </div>

        <!-- 加载状�?-->
        <div v-if="teacherCoursesLoading" class="profile-teacher-state">正在加载课程...</div>
        <!-- 错误状�?-->
        <div v-else-if="teacherCoursesError" class="profile-teacher-state is-error">
          <span>{{ teacherCoursesError }}</span>
          <button type="button" @click="loadTeacherCourses">重试</button>
        </div>
        <!-- 空状�?-->
        <div v-else-if="teacherCourses.length === 0" class="profile-teacher-state">
          <span>暂无自己发布的课程</span>
          <button type="button" @click="openPublishCourseDialog">添加课程</button>
        </div>
        <!-- 课程列表 -->
        <div v-else class="profile-teacher-course-list">
          <article v-for="course in teacherCourses" :key="course.id" class="profile-teacher-course-card">
            <img :src="resolveResourceUrl(course.cover || course.coverUrl)" :alt="course.name" />
            <div class="profile-teacher-course-body">
              <div class="profile-teacher-course-title">
                <span>{{ course.category || '教师发布' }}</span>
                <h3>{{ course.name }}</h3>
                <p>{{ course.school }} · {{ course.startTime || '开课时间待定' }}</p>
              </div>
              <!-- 班级分配 -->
              <div class="profile-teacher-course-class">
                <label>
                  分配班级
                  <input
                    v-model="classAssignments[course.id]"
                    type="text"
                    placeholder="例如：计科 2301 班"
                  />
                </label>
                <button type="button" @click="assignCourseClass(course)">确认分配</button>
              </div>
              <!-- 课程操作 -->
              <div class="profile-teacher-course-actions">
                <RouterLink :to="`/academy/open-courses/${encodeURIComponent(course.id)}`">查看课程</RouterLink>
                <button type="button">布置作业</button>
                <button
                  type="button"
                  class="is-danger"
                  :disabled="deletingCourseId === course.id"
                  @click="openTeacherCourseDeleteConfirm(course)"
                >
                  {{ deletingCourseId === course.id ? '删除中...' : '删除课程' }}
                </button>
              </div>
            </div>
          </article>
        </div>
      </section>

      <section v-if="isTeacherProfile" class="profile-panel profile-teacher-oj-panel">
        <div class="profile-panel-head">
          <div>
            <p>OJ Problem Management</p>
            <h2>OJ题目管理</h2>
          </div>
          <div class="profile-teacher-head-actions">
            <span>{{ teacherOjCount }} 道题目</span>
            <button type="button" @click="openTeacherOjCreateDialog">新增OJ题目</button>
          </div>
        </div>

        <div v-if="teacherOjLoading && !teacherOjDialogOpen" class="profile-teacher-state">正在加载OJ题目...</div>
        <div v-else-if="teacherOjError && !teacherOjDialogOpen" class="profile-teacher-state is-error">
          <span>{{ teacherOjError }}</span>
          <button type="button" @click="loadTeacherOjProblems">重试</button>
        </div>
        <div v-else-if="teacherOjProblems.length === 0" class="profile-teacher-state">
          <span>暂无自己创建的OJ题目</span>
          <button type="button" @click="openTeacherOjCreateDialog">新增OJ题目</button>
        </div>
        <div v-else class="profile-teacher-oj-list">
          <article v-for="problem in teacherOjProblems" :key="problem.id" class="profile-teacher-oj-card">
            <div>
              <span>{{ formatTeacherOjCategoryName(problem.category) || '未分类' }}</span>
              <h3>{{ problem.title }}</h3>
              <p>{{ problem.slug }} · {{ formatDifficulty(problem.difficulty) }} · {{ problem.status }}</p>
            </div>
            <button type="button" @click="openTeacherOjEditDialog(problem)">编辑题目</button>
          </article>
        </div>
      </section>

      <!-- 学生学习概览面板 -->
      <div v-if="!isTeacherProfile" class="profile-summary">
        <p>{{ profileLoading ? 'Syncing Data' : 'Learning Dashboard' }}</p>
        <h2>今天也有一点进步，被系统悄悄记下来了。</h2>
        <span v-if="profileError" class="profile-data-note">暂时使用兜底数据：{{ profileError }}</span>
        <!-- 学习统计数据 -->
        <div class="profile-stats">
          <article v-for="item in stats" :key="item.label">
            <strong>{{ item.value }}</strong>
            <span>{{ item.label }}</span>
            <small>{{ item.hint }}</small>
          </article>
        </div>
      </div>
    </section>

    <section v-if="!isTeacherProfile" class="profile-insight-grid">
      <div class="profile-insight-stack">
        <article class="profile-panel profile-time-panel">
          <div class="profile-panel-head">
            <div>
              <p>Learning Time</p>
              <h2>&#x5b66;&#x4e60;&#x65f6;&#x957f;</h2>
            </div>
            <span>&#x6570;&#x636e;&#x5e93;</span>
          </div>
          <div class="profile-time-list">
            <div v-for="item in learningTimes" :key="item.label" :class="`is-${item.tone}`">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
              <small>{{ item.hint }}</small>
            </div>
          </div>
        </article>

        <article class="profile-panel profile-preview-panel profile-games-panel">
          <div class="profile-panel-head">
            <div>
              <p>{{ gamePreviewSection.eyebrow }}</p>
              <h2>{{ gamePreviewSection.title }}</h2>
            </div>
          </div>
          <div class="profile-preview-list profile-preview-list-horizontal">
            <div v-for="item in gamePreviewSection.items" :key="item.title" :class="`is-${item.tone}`">
              <span>{{ item.title }}</span>
              <strong>{{ item.value }}</strong>
              <small>{{ item.meta }}</small>
            </div>
          </div>
        </article>
      </div>

      <article class="profile-panel profile-coding-panel">
        <div class="profile-panel-head">
          <div>
            <p>Code Progress</p>
            <h2>&#x7f16;&#x7a0b;&#x9898;&#x96be;&#x5ea6;</h2>
          </div>
          <span>OJ &#x6570;&#x636e;</span>
        </div>
        <div class="profile-coding-content">
          <div class="profile-coding-ring" :style="codingRingStyle">
            <div class="profile-coding-ring-mask">
              <strong>{{ codingSolvedTotal }}</strong>
              <span>&#x5df2;&#x5b8c;&#x6210; / {{ codingQuestionTotal }}</span>
              <small>{{ codingCompletion }}%</small>
            </div>
          </div>
          <div class="profile-coding-list">
            <div v-for="item in codingDifficulties" :key="item.level">
              <span>
                <i :style="{ background: item.color }"></i>
                {{ item.label }}
              </span>
              <strong>{{ item.solved }} / {{ item.total }}</strong>
            </div>
          </div>
        </div>
      </article>

      <article class="profile-panel profile-badge-panel">
        <div class="profile-panel-head">
          <div>
            <p>Badges</p>
            <h2>&#x6210;&#x5c31;&#x5fbd;&#x7ae0;</h2>
          </div>
          <span>{{ achievementSummary.unlocked }} / {{ achievementSummary.total }}</span>
        </div>
        <div class="profile-badges">
          <span
            v-for="achievement in achievementPreview"
            :key="achievement.key"
            :class="[`is-${achievement.tone}`, { 'is-locked': !achievement.unlocked }]"
          >
            {{ achievement.title }}
          </span>
        </div>
        <button type="button" class="profile-achievement-all-button" @click="achievementDialogOpen = true">
          查看所有成就
        </button>
      </article>

      <article class="profile-panel profile-heatmap-panel">
        <div class="profile-panel-head">
          <div>
            <p>Activity</p>
            <h2>&#x5b66;&#x4e60;&#x70ed;&#x529b;&#x56fe;</h2>
          </div>
          <span>&#x8fd1; 17 &#x5468;</span>
        </div>
        <div class="profile-heatmap" aria-label="&#x8fd1; 17 &#x5468;&#x5b66;&#x4e60;&#x6d3b;&#x8dc3;&#x5ea6;">
          <span
            v-for="day in activityDays"
            :key="day.id"
            :class="`is-level-${day.level}`"
          ></span>
        </div>
        <div class="profile-heatmap-legend">
          <span>&#x5c11;</span>
          <i class="is-level-1"></i>
          <i class="is-level-2"></i>
          <i class="is-level-3"></i>
          <i class="is-level-4"></i>
          <span>&#x591a;</span>
        </div>
      </article>
    </section>

    <section v-if="!isTeacherProfile" class="profile-preview-grid" aria-label="个人主页扩展数据预览">
      <article v-for="section in previewSections" :key="section.key" class="profile-panel profile-preview-panel">
        <div class="profile-panel-head">
          <div>
            <p>{{ section.eyebrow }}</p>
            <h2>{{ section.title }}</h2>
          </div>
        </div>
        <div class="profile-preview-list">
          <div v-for="item in section.items" :key="item.title" :class="`is-${item.tone}`">
            <span>{{ item.title }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.meta }}</small>
          </div>
        </div>
      </article>
    </section>

    <section v-if="!isTeacherProfile" class="profile-grid">
      <div class="profile-column">
        <article class="profile-panel profile-progress-panel">
          <div class="profile-panel-head">
            <div>
              <p>Progress</p>
              <h2>练习分布</h2>
            </div>
          </div>
          <div
            class="profile-ring"
            :style="progressRingStyle"
            :aria-label="`整体题库完成度 ${overallProgress}%`"
          >
            <span>{{ overallProgress }}%</span>
          </div>
          <div class="profile-difficulty-list">
            <div v-for="item in difficultyStats" :key="item.label">
              <span>
                <i :style="{ background: item.color }"></i>
                {{ item.label }}
              </span>
              <strong>{{ item.solved }} / {{ item.total }}</strong>
            </div>
          </div>
        </article>

      </div>

      <div class="profile-column">
        <article class="profile-panel profile-track-panel">
          <div class="profile-panel-head">
            <div>
              <p>Question Banks</p>
              <h2>题库进度</h2>
            </div>
          </div>
          <div class="profile-track-list">
            <div v-for="track in skillTracks" :key="track.name" class="profile-track-item">
              <div>
                <strong>{{ track.name }}</strong>
                <span>{{ track.solved }}</span>
              </div>
              <div class="profile-track-bar">
                <i :class="`is-${track.tone}`" :style="{ width: `${track.progress}%` }"></i>
              </div>
              <em>{{ track.progress }}%</em>
            </div>
          </div>
        </article>

        <article id="recent-activity" class="profile-panel profile-activity-panel">
          <div class="profile-panel-head">
            <div>
              <p>Recent</p>
              <h2>最近动态</h2>
            </div>
          </div>
          <div class="profile-activity-list">
            <div v-for="activity in recentActivities" :key="activity.title">
              <i></i>
              <div>
                <strong>{{ activity.title }}</strong>
                <span>{{ activity.meta }}</span>
              </div>
            </div>
          </div>
        </article>
      </div>
    </section>

    <div
      v-if="publishCourseDialogOpen"
      class="online-course-publish-backdrop"
      role="presentation"
      @click.self="closePublishCourseDialog"
    >
      <section
        class="online-course-publish-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="profile-publish-course-title"
      >
        <div class="online-course-publish-head">
          <div>
            <p>Teacher Course</p>
            <h2 id="profile-publish-course-title">添加课程</h2>
          </div>
          <button
            type="button"
            :disabled="publishingCourse"
            aria-label="关闭添加课程窗口"
            @click="closePublishCourseDialog"
          >
            ×
          </button>
        </div>

        <form class="online-course-publish-form" @submit.prevent="submitPublishCourseFromProfile">
          <label>
            课程名称
            <input v-model="publishCourseForm.courseName" type="text" maxlength="120" required />
          </label>
          <label>
            开课时间
            <input
              v-model="publishCourseForm.startTime"
              type="text"
              maxlength="64"
              placeholder="例如 2026-09-01"
              required
            />
          </label>
          <label>
            课程分类
            <select v-model="publishCourseForm.category" required>
              <option value="" disabled>请选择分类</option>
              <option v-for="category in publishCourseCategories" :key="category" :value="category">
                {{ category }}
              </option>
            </select>
          </label>
          <label>
            学期安排
            <input
              v-model="publishCourseForm.semesterPlan"
              type="text"
              maxlength="512"
              placeholder="例如 16 周，每周 2 学时"
              required
            />
          </label>
          <label>
            课程概述
            <textarea v-model="publishCourseForm.courseOverview" rows="3" maxlength="1200" required></textarea>
          </label>
          <label>
            课程详情
            <textarea v-model="publishCourseForm.courseDetail" rows="5" maxlength="4000" required></textarea>
          </label>
          <div class="online-course-upload-grid">
            <label>
              上传课程封面
              <input type="file" accept="image/png,image/jpeg,image/webp" required @change="handlePublishCourseCoverSelected" />
              <span>{{ publishCourseCoverFile?.name || '未选择文件' }}</span>
            </label>
            <label>
              上传课程视频
              <input
                type="file"
                accept="video/mp4,video/webm,video/ogg,video/quicktime"
                required
                @change="handlePublishCourseVideoSelected"
              />
              <span>{{ publishCourseVideoFile?.name || '未选择文件' }}</span>
            </label>
          </div>
          <p v-if="publishCourseError" class="online-course-publish-message is-error">{{ publishCourseError }}</p>
          <div class="online-course-publish-actions">
            <button type="submit" :disabled="publishingCourse">
              {{ publishingCourse ? '发布中...' : '确认发布' }}
            </button>
            <button type="button" :disabled="publishingCourse" @click="closePublishCourseDialog">取消</button>
          </div>
        </form>
      </section>
    </div>

    <div
      v-if="teacherOjDialogOpen"
      class="profile-modal-backdrop"
      role="presentation"
      @click.self="requestCloseTeacherOjDialog"
    >
      <section
        class="profile-edit-dialog profile-teacher-oj-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="profile-teacher-oj-title"
      >
        <div class="profile-edit-head">
          <div>
            <p>Teacher OJ</p>
            <h2 id="profile-teacher-oj-title">{{ teacherOjDialogMode === 'edit' ? '编辑OJ题目' : '新增OJ题目' }}</h2>
          </div>
          <button type="button" :disabled="teacherOjSaving" aria-label="关闭OJ题目窗口" @click="requestCloseTeacherOjDialog">
            ×
          </button>
        </div>

        <form class="profile-teacher-oj-form" @submit.prevent="submitTeacherOjProblem">
          <div class="profile-teacher-oj-grid">
            <label>
              题目标题
              <input v-model="teacherOjForm.title" type="text" maxlength="128" required />
            </label>
            <label>
              题目标识
              <input v-model="teacherOjForm.slug" type="text" maxlength="128" placeholder="例如 two-sum" required />
            </label>
          </div>
          <div class="profile-teacher-oj-field">
            OJ分类（可多选）
            <details class="profile-teacher-oj-dropdown">
              <summary>{{ teacherOjCategorySummary() }}</summary>
              <div class="profile-teacher-oj-dropdown-menu">
                <label v-for="category in teacherOjMetaCategories" :key="category.name">
                  <input
                    type="checkbox"
                    :checked="selectedTeacherOjCategories().includes(category.name)"
                    @change="toggleTeacherOjCategory(category.name, $event.target.checked)"
                  />
                  <span>{{ formatTeacherOjCategoryName(category.name) }}</span>
                </label>
              </div>
            </details>
          </div>
          <textarea v-model="teacherOjForm.description" rows="5" placeholder="题目描述" required></textarea>
          <div class="profile-teacher-oj-grid">
            <textarea v-model="teacherOjForm.inputDescription" rows="3" placeholder="输入描述"></textarea>
            <textarea v-model="teacherOjForm.outputDescription" rows="3" placeholder="输出描述"></textarea>
          </div>
          <textarea
            v-model="teacherOjForm.standardCode"
            class="profile-teacher-oj-code"
            rows="8"
            placeholder="标准代码（C++）。有标准代码时可自动生成未填写的测试点输出"
          ></textarea>
          <div class="profile-teacher-oj-grid is-three">
            <label>
              状态
              <select v-model="teacherOjForm.status">
                <option value="DRAFT">草稿</option>
                <option value="PUBLISHED">发布</option>
                <option value="ARCHIVED">归档</option>
              </select>
            </label>
            <label>
              时间限制(ms)
              <input v-model.number="teacherOjForm.timeLimitMs" type="number" min="100" max="30000" />
            </label>
            <label>
              内存限制(KB)
              <input v-model.number="teacherOjForm.memoryLimitKb" type="number" min="1024" max="1048576" />
            </label>
          </div>
          <p class="profile-teacher-oj-derived">难度：{{ formatDifficulty(teacherOjForm.difficulty) }}</p>

          <div class="profile-teacher-oj-cases">
            <article v-for="(testCase, index) in teacherOjForm.testCases" :key="index" class="profile-teacher-oj-case">
              <header>
                <strong>测试点 {{ index + 1 }}</strong>
                <button type="button" aria-label="删除测试点" @click="requestRemoveTeacherOjCase(index)">×</button>
              </header>
              <textarea v-model="testCase.inputData" rows="3" placeholder="测试点输入" required></textarea>
              <textarea v-model="testCase.expectedOutput" rows="3" placeholder="测试点输出（可留空，由标准代码生成）"></textarea>
              <label>
                权重
                <input v-model.number="testCase.weight" type="number" min="1" />
              </label>
              <div v-if="teacherOjCheckResult?.cases?.[index]" class="profile-teacher-oj-result">
                <p :class="{ danger: !teacherOjCheckResult.cases[index].matched }">
                  {{ teacherOjCheckResult.cases[index].message }}
                </p>
                <pre>{{ formatTeacherOjActualOutput(teacherOjCheckResult.cases[index]) }}</pre>
              </div>
            </article>
            <button type="button" class="profile-teacher-oj-add-case" @click="addTeacherOjCase">+</button>
          </div>

          <div v-if="teacherOjCheckResult" :class="['profile-teacher-oj-check', { danger: !teacherOjCheckResult.passed }]">
            <strong>{{ teacherOjCheckResult.passed ? '校验通过' : '校验存在提示' }}</strong>
            <span>{{ teacherOjCheckResult.message }}</span>
          </div>
          <p v-if="teacherOjError" class="online-course-publish-message is-error">{{ teacherOjError }}</p>
          <div class="online-course-publish-actions">
            <button type="button" :disabled="!canCheckTeacherOjCases || teacherOjSaving" @click="checkTeacherOjCases">
              校验测试点
            </button>
            <button type="submit" :disabled="teacherOjSaving">
              {{ teacherOjSaving ? '保存中...' : '保存OJ题目' }}
            </button>
            <button type="button" :disabled="teacherOjSaving" @click="requestCloseTeacherOjDialog">取消</button>
          </div>
        </form>
      </section>
    </div>

    <div
      v-if="teacherOjCloseConfirm"
      class="profile-modal-backdrop profile-confirm-layer"
      role="presentation"
      @click.self="teacherOjCloseConfirm = false"
    >
      <section class="profile-edit-dialog profile-confirm-dialog" role="dialog" aria-modal="true" aria-label="确认关闭OJ题目编辑">
        <h2>放弃修改</h2>
        <p>当前OJ题目还有未保存的修改，确认关闭吗？</p>
        <div class="profile-edit-actions">
          <button type="button" @click="closeTeacherOjDialog(true)">确认关闭</button>
          <button type="button" @click="teacherOjCloseConfirm = false">继续编辑</button>
        </div>
      </section>
    </div>

    <div
      v-if="teacherOjCaseDeleteConfirm.open"
      class="profile-modal-backdrop profile-confirm-layer"
      role="presentation"
      @click.self="closeTeacherOjCaseDeleteConfirm"
    >
      <section class="profile-edit-dialog profile-confirm-dialog" role="dialog" aria-modal="true" aria-label="确认删除测试点">
        <h2>删除测试点</h2>
        <p>确认删除测试点 {{ teacherOjCaseDeleteConfirm.index + 1 }} 吗？</p>
        <div class="profile-edit-actions">
          <button type="button" @click="confirmRemoveTeacherOjCase">确认删除</button>
          <button type="button" @click="closeTeacherOjCaseDeleteConfirm">取消</button>
        </div>
      </section>
    </div>

    <div
      v-if="courseDeleteConfirm.open"
      class="profile-modal-backdrop"
      role="presentation"
      @click.self="closeTeacherCourseDeleteConfirm"
    >
      <section class="profile-edit-dialog profile-confirm-dialog" role="dialog" aria-modal="true" aria-label="确认删除课程">
        <h2>删除课程</h2>
        <p>确认删除课程《{{ courseDeleteConfirm.course?.name }}》吗？</p>
        <div class="profile-edit-actions">
          <button type="button" :disabled="Boolean(deletingCourseId)" @click="removeTeacherCourse">
            {{ deletingCourseId ? '删除中...' : '确认删除' }}
          </button>
          <button type="button" :disabled="Boolean(deletingCourseId)" @click="closeTeacherCourseDeleteConfirm">取消</button>
        </div>
      </section>
    </div>

    <div
      v-if="achievementDialogOpen"
      class="profile-modal-backdrop"
      role="presentation"
      @click.self="achievementDialogOpen = false"
    >
      <section
        class="profile-edit-dialog profile-achievement-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="profile-achievement-title"
      >
        <div class="profile-edit-head">
          <div>
            <p>Achievements</p>
            <h2 id="profile-achievement-title">全部成就</h2>
          </div>
          <button type="button" aria-label="关闭全部成就窗口" @click="achievementDialogOpen = false">
            &times;
          </button>
        </div>
        <div class="profile-achievement-summary">
          <strong>{{ achievementSummary.unlocked }} / {{ achievementSummary.total }}</strong>
          <span>已解锁 {{ achievementSummary.percent }}%</span>
        </div>
        <div class="profile-achievement-list">
          <article
            v-for="achievement in allAchievements"
            :key="achievement.key"
            :class="['profile-achievement-item', `is-${achievement.tone}`, { 'is-locked': !achievement.unlocked }]"
          >
            <div class="profile-achievement-icon">
              {{ achievement.unlocked ? '✓' : achievement.progress }}
            </div>
            <div>
              <header>
                <strong>{{ achievement.title }}</strong>
                <span>{{ achievement.unlocked ? '已解锁' : '进行中' }}</span>
              </header>
              <p>{{ achievement.description }}</p>
              <div class="profile-achievement-progress">
                <i :style="{ width: `${achievement.progress}%` }"></i>
              </div>
              <small>
                {{ achievement.value }} / {{ achievement.target }}{{ achievement.unit }}
              </small>
            </div>
          </article>
        </div>
      </section>
    </div>

    <div
      v-if="avatarCropVisible"
      class="profile-modal-backdrop profile-crop-backdrop"
      role="presentation"
      @click.self="closeAvatarCropper()"
    >
      <section
        class="profile-edit-dialog profile-avatar-crop-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="profile-avatar-crop-title"
      >
        <div class="profile-edit-head">
          <div>
            <p>Avatar Crop</p>
            <h2 id="profile-avatar-crop-title">选取头像区域</h2>
          </div>
          <button type="button" :disabled="avatarUploading" aria-label="关闭头像裁剪" @click="closeAvatarCropper()">
            ×
          </button>
        </div>

        <div
          ref="avatarCropFrameRef"
          class="profile-avatar-crop-frame"
          :class="{ 'is-dragging': avatarCropDragging }"
          @pointerdown="startAvatarCropDrag"
          @pointermove="moveAvatarCropDrag"
          @pointerup="stopAvatarCropDrag"
          @pointercancel="stopAvatarCropDrag"
        >
          <img
            ref="avatarCropImageRef"
            :src="avatarCropImageUrl"
            alt="待裁剪头像"
            :style="avatarCropImageStyle"
            draggable="false"
            @dragstart.prevent
            @load="resetAvatarCrop"
          />
          <span class="profile-avatar-crop-ring" aria-hidden="true"></span>
        </div>

        <div class="profile-avatar-crop-tools">
          <label>
            缩放
            <input
              v-model.number="avatarCropZoom"
              type="range"
              min="1"
              max="3"
              step="0.01"
              :disabled="avatarUploading"
              @input="normalizeAvatarCropOffset"
            />
          </label>
          <p>拖动图片调整位置，圆形区域会成为最终头像。</p>
        </div>

        <p v-if="userError" class="profile-user-message">{{ userError }}</p>
        <div class="profile-edit-actions">
          <button type="button" :disabled="avatarUploading" @click="confirmAvatarCrop">
            {{ avatarUploading ? '上传中' : '确认使用' }}
          </button>
          <button type="button" :disabled="avatarUploading" @click="closeAvatarCropper()">取消</button>
        </div>
      </section>
    </div>

    <div
      v-if="editingProfile"
      class="profile-modal-backdrop"
      role="presentation"
      @click.self="cancelProfileEdit"
    >
      <section
        class="profile-edit-dialog profile-user-edit-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="profile-edit-title"
      >
        <div class="profile-edit-head">
          <div>
            <p>Profile Settings</p>
            <h2 id="profile-edit-title">编辑个人资料</h2>
          </div>
          <button type="button" :disabled="savingProfile" aria-label="关闭编辑窗口" @click="cancelProfileEdit">
            ×
          </button>
        </div>

        <form class="profile-edit-form" @submit.prevent="saveProfileEdit">
          <label>
            昵称
            <input v-model="profileForm.name" type="text" maxlength="64" />
          </label>
          <label>
            邮箱
            <input v-model="profileForm.email" type="email" maxlength="128" autocomplete="email" />
          </label>
          <label>
            简介
            <textarea v-model="profileForm.bio" maxlength="512" rows="4"></textarea>
          </label>
          <label>
            位置标签
            <input v-model="profileForm.location" type="text" maxlength="64" />
          </label>
          <div class="profile-edit-tag-field">
            <span>个人标签</span>
            <div class="profile-edit-tag-list">
              <button
                v-for="(tag, index) in profileForm.metaTags"
                :key="`${tag}-${index}`"
                type="button"
                @click="removeProfileMetaTag(index)"
              >
                {{ tag }} ×
              </button>
            </div>
            <div class="profile-edit-tag-add">
              <input
                v-model="profileForm.tagDraft"
                type="text"
                maxlength="32"
                placeholder="例如：目标：稳稳变强"
                @keydown.enter.prevent="addProfileMetaTag"
              />
              <button type="button" @click="addProfileMetaTag">添加</button>
            </div>
          </div>
          <div class="profile-edit-password-section">
            <strong>修改密码</strong>
            <label>
              旧密码
              <input v-model="profileForm.currentPassword" type="password" autocomplete="current-password" />
            </label>
            <label>
              新密码
              <input v-model="profileForm.newPassword" type="password" autocomplete="new-password" minlength="6" maxlength="72" />
            </label>
            <label>
              确认新密码
              <input v-model="profileForm.confirmNewPassword" type="password" autocomplete="new-password" />
            </label>
          </div>
          <p v-if="userError" class="profile-user-message">{{ userError }}</p>
          <div class="profile-edit-actions">
            <button type="submit" :disabled="savingProfile">
              {{ savingProfile ? '保存中' : '保存资料' }}
            </button>
            <button type="button" :disabled="savingProfile" @click="cancelProfileEdit">取消</button>
          </div>
        </form>
      </section>
    </div>

    <div
      v-if="feedbackVisible"
      class="profile-feedback-toast"
      role="status"
      aria-live="polite"
    >
      {{ feedbackMessage }}
    </div>
  </main>
</template>

