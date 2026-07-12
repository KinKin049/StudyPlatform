<!-- 在线学堂首页组件，展示学习概览、课程动态和课程卡片入口 -->
<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import {
  enrollAcademyCourse,
  fetchAcademyAssignments,
  fetchAcademyCourses,
  fetchAcademyExams,
  fetchAcademyHome,
  fetchMyAcademyCourses,
  fetchQuestionBankCourseCatalog,
} from '../../api/academy'

const router = useRouter()

// 首页内容区块的默认数据（用于 API 失败时的降级展示）
const fallbackSections = [
  {
    key: 'my-courses',
    title: '我的课程',
    items: [
      { title: '人工智能导论', category: '在线开放课程', meta: '32 学时 · 8 个章节' },
      { title: '大学生创新实践', category: '通识课程', meta: '24 学时 · 项目制学习' },
      { title: '创新工程实践', category: '微专业课程', meta: '创新创业微专业 · 项目制学习' },
    ],
  },
  {
    key: 'course-assignments',
    title: '课程作业',
    items: [
      { title: 'C语言程序设计（下）', category: '待提交', meta: '第 3 章函数练习 · 截止本周五' },
      { title: '劳动通论', category: '进行中', meta: '专题讨论 1 篇 · 已完成 60%' },
      { title: '创新工程实践', category: '待批阅', meta: '项目报告已提交 · 等待教师反馈' },
    ],
  },
  {
    key: 'my-exams',
    title: '我的考试',
    items: [
      { title: '高等数学阶段测验', category: '未开始', meta: '7 月 12 日 09:00 · 60 分钟' },
      { title: '程序设计单元测试', category: '可进入', meta: '7 月 8 日前完成 · 3 次机会' },
      { title: '通识课程结课考试', category: '已预约', meta: '线上闭卷 · 系统自动判分' },
    ],
  },
]

// 首页内容区块数据
const sections = ref(fallbackSections)
// 首页数据加载状态
const loading = ref(false)
const loadError = ref('')
// 添加课程的 UID 输入值
const courseUid = ref('')
// 侧边栏数据加载状态
const sidebarLoading = ref(false)
const sidebarError = ref('')
// 添加课程操作状态
const addCourseLoading = ref(false)
// 课程目录加载状态
const courseCatalogLoading = ref(false)
// 课程搜索框焦点状态
const courseSearchFocused = ref(false)
const academySearchKeyword = ref('')
const academySearchFocused = ref(false)
const academySearchLoading = ref(false)
const academySearchLoaded = ref(false)
const academySearchError = ref('')
const academySearchCatalog = ref({
  courses: [],
  textbooks: [],
  questionBanks: [],
})
// 反馈提示消息
const feedbackMessage = ref('')
const feedbackVisible = ref(false)
// 用户课程、作业、考试数据
const myCourses = ref([])
const assignments = ref([])
const exams = ref([])
// 课程目录缓存
const courseCatalogCache = ref({})
// 定时器引用
let feedbackTimer = null
let courseSearchBlurTimer = null
let academySearchBlurTimer = null

// 资源类型对应的详情页路径映射
const resourceDetailPath = {
  'online-open-courses': '/academy/open-courses',
  'general-courses': '/academy/general-courses',
  'micro-major-courses': '/academy/micro-majors',
}

// 资源类型配置，包含别名用于 UID 解析
const resourceOptions = [
  { type: 'online-open-courses', label: '在线开放课', aliases: ['online-open-courses', 'open', 'online', '在线开放课程', '在线开放课', '开放课'] },
  { type: 'general-courses', label: '通识课程', aliases: ['general-courses', 'general', '通识课程', '通识课'] },
  { type: 'micro-major-courses', label: '微专业课', aliases: ['micro-major-courses', 'micro', '微专业课程', '微专业课', '微专业'] },
]

// 默认学习概览数据
const overviewStats = [
  { label: '已加入课程', value: '12' },
  { label: '进行中', value: '5' },
  { label: '待完成作业', value: '3' },
  { label: '即将考试', value: '2' },
]

// 默认最近学习课程数据
const recentCourses = [
  { title: 'C语言程序设计（下）', meta: '上次学习到：指针与数组', path: '/academy/open-courses' },
  { title: '劳动通论', meta: '专题讨论已完成 60%', path: '/academy/general-courses' },
  { title: '创新工程实践', meta: '项目报告等待反馈', path: '/academy/micro-majors/26341267' },
]

// 默认课程动态数据
const courseFeeds = [
  '程序设计单元测试已开放，7 月 8 日前完成',
  '劳动通论发布了新的专题讨论',
  '创新工程实践新增项目案例资料',
]

/**
 * 计算属性：待完成作业列表
 * 过滤掉已批阅和已结束的作业，只保留待提交的作业
 */
const pendingAssignments = computed(() =>
  assignments.value
    .filter(isAssignmentUnfinished)
    .sort(sortAssignmentsByPriority),
)

/**
 * 计算属性：即将到来的考试列表
 * 过滤掉已结束的考试，只保留即将开始、正在进行的考试
 */
const upcomingExams = computed(() =>
  exams.value
    .filter(isExamUnfinished)
    .sort(sortExamsByPriority),
)

/**
 * 计算属性：实时学习概览统计数据
 * 基于用户实际数据计算课程数量、作业数量和考试数量
 */
const realOverviewStats = computed(() => [
  { label: '已加入课程', value: String(myCourses.value.length) },
  { label: '进行中', value: String(myCourses.value.length) },
  { label: '待完成作业', value: String(pendingAssignments.value.length) },
  { label: '即将考试', value: String(upcomingExams.value.length) },
])

/**
 * 计算属性：最近学习课程列表
 * 按加入时间倒序排列，取前 3 条
 */
const realRecentCourses = computed(() =>
  [...myCourses.value]
    .sort((left, right) => getTimeValue(right.enrolledAt) - getTimeValue(left.enrolledAt))
    .slice(0, 3)
    .map((course) => ({
      title: course.name,
      meta: `${course.teacher || '授课教师待定'} · ${course.startTime || '开课时间待定'}`,
      path: `${resourceDetailPath[course.resourceType] || '/academy/open-courses'}/${encodeURIComponent(course.id)}`,
    })),
)

/**
 * 计算属性：课程动态消息列表
 * 整合待完成作业和即将考试的提醒消息
 */
const realCourseFeeds = computed(() => {
  const feeds = []
  pendingAssignments.value.slice(0, 2).forEach((assignment) => {
    feeds.push(`${assignment.course || '课程'}：${assignment.title} 待完成${assignment.deadline ? `，截止 ${formatDateTime(assignment.deadline)}` : ''}`)
  })
  upcomingExams.value.slice(0, 2).forEach((exam) => {
    const timeText = exam.status === '即将开始' || isBeforeStart(exam.startsAt)
      ? `开始 ${formatDateTime(exam.startsAt) || '时间待定'}`
      : `截止 ${formatDateTime(exam.deadline) || '时间待定'}`
    feeds.push(`${exam.course || '课程'}：${exam.title} ${timeText}`)
  })
  if (!feeds.length) {
    feeds.push('暂无新的作业或考试提醒，今天可以自由复习。')
  }
  return feeds.slice(0, 4)
})

/**
 * 计算属性：课程搜索建议列表
 * 根据输入的关键词匹配课程，按匹配度排序
 */
const courseSearchSuggestions = computed(() => {
  const rawKeyword = courseUid.value.trim()
  if (!rawKeyword) {
    return []
  }

  const parsed = parseCourseUid(rawKeyword)
  const keyword = normalizeSearchText(parsed.courseId || rawKeyword)
  const resourceFilter = parsed.resourceType
  if (!keyword) {
    return []
  }

  return Object.entries(courseCatalogCache.value)
    .flatMap(([resourceType, courses]) =>
      (Array.isArray(courses) ? courses : []).map((course) => ({
        ...course,
        resourceType,
        resourceLabel: resourceOptions.find((option) => option.type === resourceType)?.label || resourceType,
        uid: `${resourceType}:${course.id}`,
      })),
    )
    .filter((course) => !resourceFilter || course.resourceType === resourceFilter)
    .map((course) => ({
      ...course,
      matchScore: getCourseMatchScore(course, keyword),
    }))
    .filter((course) => course.matchScore > 0)
    .sort((left, right) => right.matchScore - left.matchScore)
    .slice(0, 6)
})

/**
 * 计算属性：是否显示课程搜索建议
 */
const showCourseSuggestions = computed(() =>
  courseSearchFocused.value
  && courseUid.value.trim().length > 0,
)

const academySearchResults = computed(() => {
  const keyword = normalizeSearchText(academySearchKeyword.value)
  if (!keyword) {
    return []
  }

  return [
    ...academySearchCatalog.value.courses,
    ...academySearchCatalog.value.textbooks,
    ...academySearchCatalog.value.questionBanks,
  ]
    .map((item) => ({
      ...item,
      matchScore: getAcademySearchScore(item, keyword),
    }))
    .filter((item) => item.matchScore > 0)
    .sort((left, right) => right.matchScore - left.matchScore)
    .slice(0, 8)
})

const showAcademySearchPanel = computed(() =>
  academySearchFocused.value
  && academySearchKeyword.value.trim().length > 0,
)

/**
 * 加载首页内容区块数据
 * 如果 API 请求失败，使用默认的 fallback 数据
 */
const loadAcademyHome = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await fetchAcademyHome()
    if (Array.isArray(data) && data.length > 0) {
      sections.value = data
    }
  } catch (error) {
    loadError.value = error.message || '首页数据暂不可用，已使用本地默认数据'
    sections.value = fallbackSections
  } finally {
    loading.value = false
  }
}

/**
 * 加载侧边栏数据（用户课程、作业、考试）
 * 通过 Promise.all 并行获取三类数据
 */
const loadSidebarData = async () => {
  sidebarLoading.value = true
  sidebarError.value = ''

  try {
    const [courseData, assignmentData, examData] = await Promise.all([
      fetchMyAcademyCourses(1),
      fetchAcademyAssignments(1),
      fetchAcademyExams(1),
    ])
    myCourses.value = Array.isArray(courseData) ? courseData : []
    assignments.value = Array.isArray(assignmentData) ? assignmentData : []
    exams.value = Array.isArray(examData) ? examData : []
  } catch (error) {
    sidebarError.value = error.message || '侧边栏数据加载失败'
  } finally {
    sidebarLoading.value = false
  }
}

// 加载首页统一搜索所需的课程、教材和题库目录
const loadAcademySearchCatalog = async () => {
  if (academySearchLoading.value || academySearchLoaded.value) {
    return
  }

  academySearchLoading.value = true
  academySearchError.value = ''

  try {
    const [courseEntries, textbooks, questionCategories] = await Promise.all([
      Promise.all(
        resourceOptions.map(async (option) => [
          option,
          await getCourseCatalog(option.type),
        ]),
      ),
      fetchAcademyCourses('textbooks'),
      fetchQuestionBankCourseCatalog(),
    ])

    academySearchCatalog.value = {
      courses: courseEntries.flatMap(([option, courses]) =>
        (Array.isArray(courses) ? courses : []).map((course) => ({
          type: 'course',
          typeLabel: '课程',
          title: course.name,
          meta: [option.label, course.teacher, course.category].filter(Boolean).join(' · '),
          path: `${resourceDetailPath[option.type] || '/academy/open-courses'}/${encodeURIComponent(course.id)}`,
          fields: [course.id, course.name, course.teacher, course.category, course.school, option.label],
        })),
      ),
      textbooks: (Array.isArray(textbooks) ? textbooks : []).map((textbook) => ({
        type: 'textbook',
        typeLabel: '教材',
        title: textbook.name,
        meta: [textbook.editor, textbook.publisher, textbook.category, textbook.isbn].filter(Boolean).join(' · '),
        path: `/academy/textbooks/${encodeURIComponent(textbook.id)}`,
        fields: [textbook.id, textbook.name, textbook.editor, textbook.publisher, textbook.category, textbook.isbn, textbook.description],
      })),
      questionBanks: (Array.isArray(questionCategories) ? questionCategories : []).flatMap((category) =>
        (Array.isArray(category.sets) ? category.sets : []).map((set) => ({
          type: 'question-bank',
          typeLabel: '题库',
          title: set.title,
          meta: [category.name, set.subtitle, set.statusLabel, set.questionCount ? `${set.questionCount} 题` : ''].filter(Boolean).join(' · '),
          path: set.routePath || `/academy/question-bank/courses/${encodeURIComponent(set.code)}`,
          fields: [set.code, set.title, set.subtitle, set.description, set.categoryName, category.name],
        })),
      ),
    }
    academySearchLoaded.value = true
  } catch (error) {
    academySearchError.value = error.message || '搜索数据加载失败，请稍后重试'
  } finally {
    academySearchLoading.value = false
  }
}

const getAcademySearchScore = (item, keyword) => {
  const titleText = normalizeSearchText(item.title)
  const fieldTexts = (item.fields || []).map(normalizeSearchText).filter(Boolean)

  if (titleText === keyword) return 140
  if (fieldTexts.some((field) => field === keyword)) return 128
  if (titleText.startsWith(keyword)) return 112
  if (fieldTexts.some((field) => field.startsWith(keyword))) return 96
  if (titleText.includes(keyword)) return 84
  if (fieldTexts.some((field) => field.includes(keyword))) return 64
  return 0
}

const handleAcademySearchFocus = () => {
  academySearchFocused.value = true
  window.clearTimeout(academySearchBlurTimer)
  loadAcademySearchCatalog()
}

const handleAcademySearchInput = () => {
  academySearchFocused.value = true
  if (academySearchKeyword.value.trim()) {
    loadAcademySearchCatalog()
  }
}

const handleAcademySearchBlur = () => {
  window.clearTimeout(academySearchBlurTimer)
  academySearchBlurTimer = window.setTimeout(() => {
    academySearchFocused.value = false
  }, 140)
}

const openAcademySearchResult = (item) => {
  if (!item?.path) {
    return
  }
  academySearchKeyword.value = item.title || ''
  academySearchFocused.value = false
  router.push(item.path)
}

const submitAcademySearch = () => {
  const firstResult = academySearchResults.value[0]
  if (firstResult) {
    openAcademySearchResult(firstResult)
    return
  }
  if (academySearchKeyword.value.trim()) {
    loadAcademySearchCatalog()
  }
}

// 区块 key 到路由路径的映射
const aggregateRoutes = {
  'my-courses': '/academy/my-courses',
  'course-assignments': '/academy/assignments',
  'my-exams': '/academy/exams',
}

/**
 * 处理区块"查看更多"按钮点击
 * 根据区块 key 跳转到对应列表页面
 */
const handleSectionAction = (section) => {
  const routePath = aggregateRoutes[section.key]

  if (routePath) {
    router.push(routePath)
    return
  }

  console.info('academy section action reserved:', section.key)
}

/**
 * 处理添加课程操作
 * 解析课程 UID，查找课程并完成报名
 */
const handleAddCourse = async () => {
  const normalizedUid = courseUid.value.trim()

  if (!normalizedUid || addCourseLoading.value) {
    return
  }

  addCourseLoading.value = true
  sidebarError.value = ''

  try {
    const target = await resolveCourseTarget(normalizedUid)
    await enrollAcademyCourse(target.resourceType, target.courseId, { userId: 1 })
    courseUid.value = ''
    showFeedback(`已成功添加课程${target.title ? `「${target.title}」` : ''}`)
    await loadSidebarData()
  } catch (error) {
    showFeedback(error.message || '添加课程失败，请检查课程 UID')
  } finally {
    addCourseLoading.value = false
  }
}

/**
 * 处理课程搜索框获得焦点事件
 * 如果课程目录缓存为空，触发加载所有课程目录
 */
const handleCourseSearchFocus = () => {
  courseSearchFocused.value = true
  window.clearTimeout(courseSearchBlurTimer)
  if (!Object.keys(courseCatalogCache.value).length) {
    loadAllCourseCatalogs()
  }
}

/**
 * 处理课程搜索框输入事件
 * 在有输入内容且缓存为空时加载课程目录
 */
const handleCourseSearchInput = () => {
  courseSearchFocused.value = true
  if (courseUid.value.trim() && !Object.keys(courseCatalogCache.value).length) {
    loadAllCourseCatalogs()
  }
}

/**
 * 处理课程搜索框失去焦点事件
 * 延迟 140ms 后隐藏搜索建议
 */
const handleCourseSearchBlur = () => {
  window.clearTimeout(courseSearchBlurTimer)
  courseSearchBlurTimer = window.setTimeout(() => {
    courseSearchFocused.value = false
  }, 140)
}

/**
 * 选择课程搜索建议
 * 将选中课程的 UID 填入输入框并隐藏建议列表
 */
const selectCourseSuggestion = (course) => {
  courseUid.value = course.uid
  courseSearchFocused.value = false
}

/**
 * 处理课程卡片点击事件
 * 根据区块类型跳转到对应页面
 */
const handleCardClick = (section, item) => {
  if (section.key === 'course-assignments' || section.key === 'my-exams') {
    router.push(aggregateRoutes[section.key])
    return
  }

  if (section.key === 'my-courses') {
    router.push('/academy/my-courses')
    return
  }

  console.info('academy card action reserved:', section.key, item.title)
}

/**
 * 打开最近学习的课程详情页
 */
const openRecentCourse = (course) => {
  router.push(course.path)
}

/**
 * 显示操作反馈提示
 * 1800ms 后自动隐藏
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
 * 解析课程 UID
 * 支持格式：课程类型:课程ID 或 仅课程ID
 */
const parseCourseUid = (value) => {
  const normalized = value.trim()
  const separatorMatch = normalized.match(/^([^:/：]+)[:/：](.+)$/)
  if (!separatorMatch) {
    return {
      resourceType: '',
      courseId: normalized,
    }
  }

  return {
    resourceType: resolveResourceType(separatorMatch[1]),
    courseId: separatorMatch[2].trim(),
  }
}

/**
 * 根据输入值解析资源类型
 * 通过别名匹配对应的资源类型
 */
const resolveResourceType = (value) => {
  const normalized = value.trim().toLowerCase()
  return resourceOptions.find((option) => option.aliases.some((alias) => alias.toLowerCase() === normalized))?.type || ''
}

/**
 * 根据课程 UID 解析目标课程信息
 * 如果指定了资源类型则在该类型中查找，否则遍历所有类型
 */
const resolveCourseTarget = async (uid) => {
  const parsed = parseCourseUid(uid)
  if (!parsed.courseId) {
    throw new Error('请输入课程 UID')
  }

  if (parsed.resourceType) {
    const course = await findCourseInResource(parsed.resourceType, parsed.courseId)
    return {
      resourceType: parsed.resourceType,
      courseId: parsed.courseId,
      title: course?.name || '',
    }
  }

  for (const option of resourceOptions) {
    const course = await findCourseInResource(option.type, parsed.courseId)
    if (course) {
      return {
        resourceType: option.type,
        courseId: String(course.id),
        title: course.name,
      }
    }
  }

  throw new Error('没有找到对应课程，请尝试输入“课程类型:课程ID”')
}

/**
 * 在指定资源类型中查找课程
 * 支持按 ID 或名称匹配
 */
const findCourseInResource = async (resourceType, courseId) => {
  const courses = await getCourseCatalog(resourceType)
  return courses.find((course) => String(course.id) === String(courseId) || course.name === courseId)
}

/**
 * 获取指定资源类型的课程目录
 * 使用缓存避免重复请求
 */
const getCourseCatalog = async (resourceType) => {
  if (!courseCatalogCache.value[resourceType]) {
    courseCatalogCache.value = {
      ...courseCatalogCache.value,
      [resourceType]: await fetchAcademyCourses(resourceType),
    }
  }
  return Array.isArray(courseCatalogCache.value[resourceType]) ? courseCatalogCache.value[resourceType] : []
}

/**
 * 加载所有资源类型的课程目录
 * 并行获取在线开放课程、通识课程和微专业课程的目录
 */
const loadAllCourseCatalogs = async () => {
  if (courseCatalogLoading.value) {
    return
  }
  courseCatalogLoading.value = true

  try {
    const entries = await Promise.all(
      resourceOptions.map(async (option) => [option.type, await getCourseCatalog(option.type)]),
    )
    courseCatalogCache.value = {
      ...courseCatalogCache.value,
      ...Object.fromEntries(entries),
    }
  } catch (error) {
    sidebarError.value = error.message || '课程匹配数据加载失败'
  } finally {
    courseCatalogLoading.value = false
  }
}

/**
 * 计算课程与关键词的匹配得分
 * 根据匹配字段和匹配方式给出不同权重的分数
 */
const getCourseMatchScore = (course, keyword) => {
  const idText = normalizeSearchText(course.id)
  const nameText = normalizeSearchText(course.name)
  const teacherText = normalizeSearchText(course.teacher)
  const categoryText = normalizeSearchText(course.category)
  const schoolText = normalizeSearchText(course.school)

  if (idText === keyword) return 120
  if (nameText === keyword) return 115
  if (idText.startsWith(keyword)) return 100
  if (nameText.startsWith(keyword)) return 92
  if (idText.includes(keyword)) return 84
  if (nameText.includes(keyword)) return 76
  if (teacherText.includes(keyword)) return 56
  if (categoryText.includes(keyword)) return 44
  if (schoolText.includes(keyword)) return 36
  return 0
}

/**
 * 标准化搜索文本
 * 转小写并移除空格
 */
const normalizeSearchText = (value) =>
  String(value || '')
    .toLowerCase()
    .replace(/\s+/g, '')
    .replace(/\s+/g, '')

/**
 * 判断截止时间是否已过
 */
const isDeadlinePassed = (deadline) => {
  if (!deadline) return false
  const deadlineTime = new Date(deadline).getTime()
  return Number.isFinite(deadlineTime) && deadlineTime < Date.now()
}

/**
 * 判断开始时间是否在未来
 */
const isBeforeStart = (startsAt) => {
  if (!startsAt) return false
  const startTime = new Date(startsAt).getTime()
  return Number.isFinite(startTime) && startTime > Date.now()
}

/**
 * 获取时间戳数值
 */
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

/**
 * 格式化日期时间为 MM-DD HH:mm 格式
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

onMounted(() => {
  loadAcademyHome()
  loadSidebarData()
  loadAllCourseCatalogs()
  loadAcademySearchCatalog()
})

onBeforeUnmount(() => {
  window.clearTimeout(feedbackTimer)
  window.clearTimeout(courseSearchBlurTimer)
  window.clearTimeout(academySearchBlurTimer)
})
</script>

<template>
  <!-- 在线学堂首页主容器 -->
  <main class="academy-main">
    <!-- 操作反馈提示 -->
    <Transition name="academy-drop-feedback">
      <div v-if="feedbackVisible" class="academy-drop-feedback-toast" role="status">
        {{ feedbackMessage }}
      </div>
    </Transition>

    <!-- 页面顶部标题区域 -->
    <section class="academy-hero" aria-labelledby="academy-title">
      <div class="academy-hero-copy">
        <h1 id="academy-title">在线学堂</h1>
      </div>

      <!-- 全局搜索框 -->
      <form class="academy-search" role="search" @submit.prevent="submitAcademySearch">
        <input
          v-model="academySearchKeyword"
          type="search"
          placeholder="搜索课程、教材或题库"
          aria-label="搜索课程、教材或题库"
          autocomplete="off"
          @focus="handleAcademySearchFocus"
          @input="handleAcademySearchInput"
          @blur="handleAcademySearchBlur"
        />
        <button type="submit" aria-label="搜索">
          <el-icon><Search /></el-icon>
        </button>
        <div v-if="showAcademySearchPanel" class="academy-search-panel" role="listbox">
          <p v-if="academySearchLoading" class="academy-search-state">正在加载课程、教材和题库...</p>
          <p v-else-if="academySearchError" class="academy-search-state is-error">{{ academySearchError }}</p>
          <template v-else-if="academySearchResults.length">
            <button
              v-for="item in academySearchResults"
              :key="`${item.type}-${item.path}`"
              type="button"
              class="academy-search-result"
              @mousedown.prevent="openAcademySearchResult(item)"
            >
              <span>{{ item.typeLabel }}</span>
              <strong>{{ item.title }}</strong>
              <em>{{ item.meta || '暂无更多信息' }}</em>
            </button>
          </template>
          <p v-else class="academy-search-state">没有找到匹配的课程、教材或题库</p>
        </div>
      </form>
    </section>

    <!-- 加载状态提示 -->
    <p v-if="loading" class="academy-home-hint">正在加载学堂首页...</p>
    <p v-else-if="loadError" class="academy-home-hint academy-home-warning">{{ loadError }}</p>
    <p v-if="sidebarLoading" class="academy-home-hint">正在同步你的课程、作业和考试...</p>
    <p v-else-if="sidebarError" class="academy-home-hint academy-home-warning">{{ sidebarError }}</p>

    <div class="academy-home-divider" aria-hidden="true"></div>

    <!-- 首页布局区域：侧边栏 + 内容区 -->
    <section class="academy-home-layout" aria-label="学堂控制台">
      <!-- 侧边栏 -->
      <aside class="academy-sidebar" aria-label="学习侧边栏">
        <!-- 学习概览卡片 -->
        <section class="academy-side-card">
          <div class="academy-side-heading">
            <span>学习概览</span>
            <strong>今日状态</strong>
          </div>
          <div class="academy-overview-grid">
            <div v-for="stat in realOverviewStats" :key="stat.label" class="academy-overview-item">
              <strong>{{ stat.value }}</strong>
              <span>{{ stat.label }}</span>
            </div>
          </div>
        </section>

        <!-- 添加课程卡片 -->
        <section class="academy-side-card">
          <div class="academy-side-heading">
            <span>添加课程</span>
            <strong>课程 UID</strong>
          </div>
          <form class="academy-add-course" @submit.prevent="handleAddCourse">
            <input
              v-model="courseUid"
              type="text"
              placeholder="如 online-open-courses:课程ID"
              aria-label="输入课程 UID"
              :disabled="addCourseLoading"
              @focus="handleCourseSearchFocus"
              @input="handleCourseSearchInput"
              @blur="handleCourseSearchBlur"
            />
            <button type="submit" :disabled="addCourseLoading">{{ addCourseLoading ? '添加中' : '添加' }}</button>
          </form>
          <!-- 课程搜索建议列表 -->
          <div v-if="showCourseSuggestions" class="academy-course-suggestions" role="listbox">
            <p v-if="courseCatalogLoading" class="academy-course-suggestion-state">正在匹配课程...</p>
            <template v-else-if="courseSearchSuggestions.length">
              <button
                v-for="course in courseSearchSuggestions"
                :key="course.uid"
                type="button"
                class="academy-course-suggestion"
                @mousedown.prevent="selectCourseSuggestion(course)"
              >
                <span>{{ course.resourceLabel }}</span>
                <strong>{{ course.name }}</strong>
                <em>{{ course.teacher || '教师待定' }} · {{ course.id }}</em>
              </button>
            </template>
            <p v-else class="academy-course-suggestion-state">暂无匹配课程，请试试课程名、教师或课程 ID。</p>
          </div>
          <p class="academy-side-note">支持“课程类型:课程ID”，也可以只输入课程 ID 自动查找。</p>
        </section>

        <!-- 我的班级入口 -->
        <button class="academy-class-card" type="button" @click="router.push('/academy/my-class')">
          <span>我的班级</span>
          <strong>软件工程 2401 班</strong>
          <em>成员 42 人 · 本周活跃 36 人</em>
        </button>

        <!-- 最近学习课程列表 -->
        <section class="academy-side-card">
          <div class="academy-side-heading">
            <span>最近学习</span>
            <strong>继续上次进度</strong>
          </div>
          <div class="academy-recent-list">
            <button
              v-for="course in realRecentCourses"
              :key="course.title"
              type="button"
              @click="openRecentCourse(course)"
            >
              <strong>{{ course.title }}</strong>
              <span>{{ course.meta }}</span>
            </button>
            <p v-if="!realRecentCourses.length" class="academy-side-note">还没有加入课程，输入课程 UID 或去课程详情页参加课程。</p>
          </div>
        </section>

        <!-- 课程动态消息 -->
        <section class="academy-side-card">
          <div class="academy-side-heading">
            <span>课程动态</span>
            <strong>班级与课程消息</strong>
          </div>
          <ul class="academy-feed-list">
            <li v-for="feed in realCourseFeeds" :key="feed">{{ feed }}</li>
          </ul>
        </section>
      </aside>

      <!-- 主内容区：课程卡片展示 -->
      <section class="academy-dashboard" aria-label="学堂内容">
        <section v-for="section in sections" :key="section.key" class="academy-content">
          <div class="academy-section-heading">
            <h2>{{ section.title }}</h2>
            <button type="button" @click="handleSectionAction(section)">查看更多</button>
          </div>

          <!-- 课程卡片网格 -->
          <div class="course-grid">
            <button
              v-for="item in section.items"
              :key="`${section.key}-${item.title}`"
              type="button"
              class="course-card"
              @click="handleCardClick(section, item)"
            >
              <p>{{ item.category }}</p>
              <h3>{{ item.title }}</h3>
              <span>{{ item.meta }}</span>
            </button>
          </div>
        </section>
      </section>
    </section>
  </main>
</template>
