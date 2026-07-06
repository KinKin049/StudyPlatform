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
} from '../../api/academy'

const router = useRouter()

const fallbackSections = [
  {
    key: 'my-courses',
    title: '我的课程',
    items: [
      { title: '人工智能导论', category: '在线开放课程', meta: '32 学时 · 8 个章节' },
      { title: '大学生创新实践', category: '通识课程', meta: '24 学时 · 项目制学习' },
      { title: '数据分析微专业', category: '微专业课程', meta: '6 门课 · 能力认证' },
    ],
  },
  {
    key: 'course-assignments',
    title: '课程作业',
    items: [
      { title: 'C语言程序设计（下）', category: '待提交', meta: '第 3 章函数练习 · 截止本周五' },
      { title: '劳动通论', category: '进行中', meta: '专题讨论 1 篇 · 已完成 60%' },
      { title: '数据分析微专业', category: '待批阅', meta: '项目报告已提交 · 等待教师反馈' },
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

const sections = ref(fallbackSections)
const loading = ref(false)
const loadError = ref('')
const courseUid = ref('')
const sidebarLoading = ref(false)
const sidebarError = ref('')
const addCourseLoading = ref(false)
const courseCatalogLoading = ref(false)
const courseSearchFocused = ref(false)
const feedbackMessage = ref('')
const feedbackVisible = ref(false)
const myCourses = ref([])
const assignments = ref([])
const exams = ref([])
const courseCatalogCache = ref({})
let feedbackTimer = null
let courseSearchBlurTimer = null

const resourceDetailPath = {
  'online-open-courses': '/academy/open-courses',
  'general-courses': '/academy/general-courses',
  'micro-major-courses': '/academy/micro-majors',
}

const resourceOptions = [
  { type: 'online-open-courses', label: '在线开放课', aliases: ['online-open-courses', 'open', 'online', '在线开放课程', '在线开放课', '开放课'] },
  { type: 'general-courses', label: '通识课程', aliases: ['general-courses', 'general', '通识课程', '通识课'] },
  { type: 'micro-major-courses', label: '微专业课', aliases: ['micro-major-courses', 'micro', '微专业课程', '微专业课', '微专业'] },
]

const overviewStats = [
  { label: '已加入课程', value: '12' },
  { label: '进行中', value: '5' },
  { label: '待完成作业', value: '3' },
  { label: '即将考试', value: '2' },
]

const recentCourses = [
  { title: 'C语言程序设计（下）', meta: '上次学习到：指针与数组', path: '/academy/open-courses' },
  { title: '劳动通论', meta: '专题讨论已完成 60%', path: '/academy/general-courses' },
  { title: '数据分析微专业', meta: '项目报告等待反馈', path: '/academy/micro-majors' },
]

const courseFeeds = [
  '程序设计单元测试已开放，7 月 8 日前完成',
  '劳动通论发布了新的专题讨论',
  '数据分析微专业新增项目案例资料',
]

const pendingAssignments = computed(() =>
  assignments.value.filter((assignment) => {
    if (assignment.submissionStatus === 'graded' || assignment.submissionStatus === 'pending_review') {
      return false
    }
    return assignment.status !== '已结束' && !isDeadlinePassed(assignment.deadline)
  }),
)

const upcomingExams = computed(() =>
  exams.value.filter((exam) => {
    if (exam.status === '已结束' || isDeadlinePassed(exam.deadline)) {
      return false
    }
    return exam.status === '即将开始' || isBeforeStart(exam.startsAt) || exam.status === '正在进行'
  }),
)

const realOverviewStats = computed(() => [
  { label: '已加入课程', value: String(myCourses.value.length) },
  { label: '进行中', value: String(myCourses.value.length) },
  { label: '待完成作业', value: String(pendingAssignments.value.length) },
  { label: '即将考试', value: String(upcomingExams.value.length) },
])

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

const showCourseSuggestions = computed(() =>
  courseSearchFocused.value
  && courseUid.value.trim().length > 0,
)

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

const aggregateRoutes = {
  'my-courses': '/academy/my-courses',
  'course-assignments': '/academy/assignments',
  'my-exams': '/academy/exams',
}

const handleSectionAction = (section) => {
  const routePath = aggregateRoutes[section.key]

  if (routePath) {
    router.push(routePath)
    return
  }

  console.info('academy section action reserved:', section.key)
}

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

const handleCourseSearchFocus = () => {
  courseSearchFocused.value = true
  window.clearTimeout(courseSearchBlurTimer)
  if (!Object.keys(courseCatalogCache.value).length) {
    loadAllCourseCatalogs()
  }
}

const handleCourseSearchInput = () => {
  courseSearchFocused.value = true
  if (courseUid.value.trim() && !Object.keys(courseCatalogCache.value).length) {
    loadAllCourseCatalogs()
  }
}

const handleCourseSearchBlur = () => {
  window.clearTimeout(courseSearchBlurTimer)
  courseSearchBlurTimer = window.setTimeout(() => {
    courseSearchFocused.value = false
  }, 140)
}

const selectCourseSuggestion = (course) => {
  courseUid.value = course.uid
  courseSearchFocused.value = false
}

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

const openRecentCourse = (course) => {
  router.push(course.path)
}

const showFeedback = (message) => {
  feedbackMessage.value = message
  feedbackVisible.value = true
  window.clearTimeout(feedbackTimer)
  feedbackTimer = window.setTimeout(() => {
    feedbackVisible.value = false
  }, 1800)
}

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

const resolveResourceType = (value) => {
  const normalized = value.trim().toLowerCase()
  return resourceOptions.find((option) => option.aliases.some((alias) => alias.toLowerCase() === normalized))?.type || ''
}

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

const findCourseInResource = async (resourceType, courseId) => {
  const courses = await getCourseCatalog(resourceType)
  return courses.find((course) => String(course.id) === String(courseId) || course.name === courseId)
}

const getCourseCatalog = async (resourceType) => {
  if (!courseCatalogCache.value[resourceType]) {
    courseCatalogCache.value = {
      ...courseCatalogCache.value,
      [resourceType]: await fetchAcademyCourses(resourceType),
    }
  }
  return Array.isArray(courseCatalogCache.value[resourceType]) ? courseCatalogCache.value[resourceType] : []
}

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

const normalizeSearchText = (value) =>
  String(value || '')
    .toLowerCase()
    .replace(/\s+/g, '')
    .replace(/\s+/g, '')

const isDeadlinePassed = (deadline) => {
  if (!deadline) return false
  const deadlineTime = new Date(deadline).getTime()
  return Number.isFinite(deadlineTime) && deadlineTime < Date.now()
}

const isBeforeStart = (startsAt) => {
  if (!startsAt) return false
  const startTime = new Date(startsAt).getTime()
  return Number.isFinite(startTime) && startTime > Date.now()
}

const getTimeValue = (value) => {
  const time = new Date(value || 0).getTime()
  return Number.isFinite(time) ? time : 0
}

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
})

onBeforeUnmount(() => {
  window.clearTimeout(feedbackTimer)
  window.clearTimeout(courseSearchBlurTimer)
})
</script>

<template>
  <main class="academy-main">
    <Transition name="academy-drop-feedback">
      <div v-if="feedbackVisible" class="academy-drop-feedback-toast" role="status">
        {{ feedbackMessage }}
      </div>
    </Transition>

    <section class="academy-hero" aria-labelledby="academy-title">
      <div class="academy-hero-copy">
        <h1 id="academy-title">在线学堂</h1>
      </div>

      <div class="academy-search" role="search">
        <input type="search" placeholder="搜索课程、教材或专题" aria-label="搜索课程、教材或专题" />
        <button type="button" aria-label="搜索">
          <el-icon><Search /></el-icon>
        </button>
      </div>
    </section>

    <p v-if="loading" class="academy-home-hint">正在加载学堂首页...</p>
    <p v-else-if="loadError" class="academy-home-hint academy-home-warning">{{ loadError }}</p>
    <p v-if="sidebarLoading" class="academy-home-hint">正在同步你的课程、作业和考试...</p>
    <p v-else-if="sidebarError" class="academy-home-hint academy-home-warning">{{ sidebarError }}</p>

    <div class="academy-home-divider" aria-hidden="true"></div>

    <section class="academy-home-layout" aria-label="学堂控制台">
      <aside class="academy-sidebar" aria-label="学习侧边栏">
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

        <button class="academy-class-card" type="button" @click="router.push('/academy/my-class')">
          <span>我的班级</span>
          <strong>软件工程 2401 班</strong>
          <em>成员 42 人 · 本周活跃 36 人</em>
        </button>

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

      <section class="academy-dashboard" aria-label="学堂内容">
        <section v-for="section in sections" :key="section.key" class="academy-content">
          <div class="academy-section-heading">
            <h2>{{ section.title }}</h2>
            <button type="button" @click="handleSectionAction(section)">查看更多</button>
          </div>

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
