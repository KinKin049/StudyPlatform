<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  fetchAcademyAssignments,
  fetchAcademyExams,
  fetchMyAcademyCourses,
  unenrollAcademyCourse,
} from '../../api/academy'
import { fetchProfileOverview } from '../../api/profile'
import { resolveResourceUrl } from '../../api/request'

const props = defineProps({
  variant: {
    type: String,
    default: 'courses',
  },
})

const router = useRouter()

const userInfo = {
  accountId: 'STU-2026-0001',
  role: '学生',
  initial: 'K',
}

const statusFilters = ['全部', '正在进行', '即将开始', '已结束']

const pageTitles = {
  courses: '我的课程',
  assignments: '课程作业',
  exams: '我的考试',
}

const pageSidebars = {
  courses: [
    { title: '快捷操作', items: ['输入课程 UID 添加课程', '查看最近学习记录', '进入课程分类页'] },
    { title: '推荐下一步', items: ['继续学习 C语言程序设计（下）', '补齐数据分析微专业项目资料'] },
    { title: '课程提醒', items: ['2 门课程本周更新章节', '1 门课程即将开课'] },
  ],
  assignments: [
    { title: '快捷操作', items: ['筛选待提交作业', '查看已批阅反馈', '进入错题复盘'] },
    { title: '推荐下一步', items: ['完成第 3 章函数练习', '提交劳动通论专题讨论'] },
    { title: '作业提醒', items: ['2 项作业 3 天内截止', '1 项报告等待教师批阅'] },
  ],
  exams: [
    { title: '快捷操作', items: ['查看可进入考试', '检查考试设备', '查看历史成绩'] },
    { title: '推荐下一步', items: ['完成程序设计单元测试', '预约通识课程结课考试'] },
    { title: '考试提醒', items: ['1 场考试正在开放', '1 场考试即将开始'] },
  ],
}

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
const profileOverview = ref(null)
const studyTimeLoading = ref(false)
const studyTimeError = ref('')
const droppingCourseKey = ref('')
const pendingDropCourse = ref(null)
const dropFeedbackVisible = ref(false)
let dropFeedbackTimer = null

const resourceDetailPath = {
  'online-open-courses': '/academy/open-courses',
  'general-courses': '/academy/general-courses',
  'micro-major-courses': '/academy/micro-majors',
}

watch(
  () => props.variant,
  (variant) => {
    activeCategory.value = variant
    activeStatus.value = '全部'
  },
)

const pageTitle = computed(() => pageTitles[props.variant] || pageTitles.courses)
const realStudyTime = computed(() => {
  if (studyTimeLoading.value) return '加载中'
  const learningTime = profileOverview.value?.learningTimes?.find((item) => item.label === '学习时长')
    || profileOverview.value?.learningTimes?.[0]
  return learningTime?.value || '0m'
})

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

const getExamProgress = (exam) => {
  if (exam.submissionStatus === 'graded') return `已批改 · ${exam.score ?? 0} 分`
  if (exam.submissionStatus === 'pending_review') return `待教师批阅 · 当前 ${exam.score ?? 0} 分`
  if (exam.submissionStatus === 'draft') return '答题中 · 草稿已保存'
  if (exam.submissionStatus === 'in_progress') return '考试已开始'
  if (exam.status === '已结束' || isDeadlinePassed(exam.deadline)) return '考试已结束'
  if (exam.status === '即将开始' || isBeforeStart(exam.startsAt)) return `开始时间 ${formatDateTime(exam.startsAt) || '待定'}`
  return '可进入考试'
}

const getStatusLetters = (status) => Array.from(status || '')

const assignmentCards = computed(() =>
  assignments.value.map((assignment, index) => {
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
      cover: [
        'linear-gradient(135deg, #74ebd5, #bfe9ff 58%, #acb6e5)',
        'linear-gradient(135deg, #d5f8ef, #74ebd5 45%, #ffc1cb)',
        'linear-gradient(135deg, #acb6e5, #bfe9ff 52%, #74ebd5)',
      ][index % 3],
    }
  }),
)

const examCards = computed(() =>
  exams.value.map((exam, index) => {
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
      cover: [
        'linear-gradient(135deg, #7fd8ee, #74ebd5 50%, #acb6e5)',
        'linear-gradient(135deg, #bfe9ff, #acb6e5 48%, #74ebd5)',
        'linear-gradient(135deg, #ffc1cb, #bfe9ff 52%, #74ebd5)',
      ][index % 3],
    }
  }),
)

const categoryTabs = computed(() => [
  { key: 'courses', label: '课程', count: myCourseCards.value.length, path: '/academy/my-courses' },
  { key: 'assignments', label: '作业', count: assignmentCards.value.length, path: '/academy/assignments' },
  { key: 'exams', label: '考试', count: examCards.value.length, path: '/academy/exams' },
])

const allCards = computed(() => [...myCourseCards.value, ...assignmentCards.value, ...examCards.value])

const visibleCards = computed(() =>
  allCards.value.filter((card) => {
    const matchesCategory = activeCategory.value === 'all' || card.type === activeCategory.value
    const matchesStatus = activeStatus.value === '全部' || card.status === activeStatus.value

    return matchesCategory && matchesStatus
  }),
)

const sidebarSections = computed(() => pageSidebars[props.variant] || pageSidebars.courses)

const emptyStateText = computed(() => {
  if (props.variant === 'courses') return '暂无匹配内容，去课程详情页点击“立即参加”后会出现在这里。'
  if (props.variant === 'assignments') return '暂无匹配作业，后续教师发布后会出现在这里。'
  if (props.variant === 'exams') return '暂无匹配考试，后续开放考试后会出现在这里。'
  return '暂无匹配内容。'
})

const selectCategory = (tab) => {
  activeCategory.value = tab.key

  if (tab.key !== 'all') {
    router.push(tab.path)
  }
}

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

const openDropCourseDialog = (card) => {
  if (!card.canDrop || droppingCourseKey.value) return
  pendingDropCourse.value = card
}

const closeDropCourseDialog = () => {
  if (droppingCourseKey.value) return
  pendingDropCourse.value = null
}

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

onMounted(() => {
  loadProfileStudyTime()
  loadMyCourses()
  loadAssignments()
  loadExams()
})
onBeforeUnmount(() => {
  window.clearTimeout(dropFeedbackTimer)
})
</script>

<template>
  <main class="academy-main academy-aggregate-main">
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

    <Transition name="academy-drop-feedback">
      <div v-if="dropFeedbackVisible" class="academy-drop-feedback-toast" role="status">
        已成功退出课程
      </div>
    </Transition>

    <section class="academy-aggregate-userbar" aria-label="用户信息">
      <div class="academy-aggregate-user">
        <div class="academy-aggregate-avatar" aria-hidden="true">{{ userInfo.initial }}</div>
        <div>
          <strong>{{ userInfo.accountId }}</strong>
          <span>{{ userInfo.role }}</span>
        </div>
      </div>

      <div class="academy-aggregate-stat">
        <span>学习时长</span>
        <strong>{{ realStudyTime }}</strong>
        <small v-if="studyTimeError">{{ studyTimeError }}</small>
      </div>
    </section>

    <section class="academy-aggregate-layout">
      <section class="academy-aggregate-body">
        <div class="academy-aggregate-heading">
          <div>
            <h1>{{ pageTitle }}</h1>
          </div>
          <span>统一管理课程、作业与考试进度</span>
        </div>

        <section class="academy-aggregate-tools" aria-label="课程管理筛选">
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

        <div v-if="props.variant === 'courses' && myCoursesLoading" class="academy-aggregate-state">
          正在加载我的课程...
        </div>
        <div v-else-if="props.variant === 'courses' && myCoursesError" class="academy-aggregate-state academy-aggregate-state-error">
          <span>{{ myCoursesError }}</span>
          <button type="button" @click="loadMyCourses">重试</button>
        </div>
        <div v-else-if="props.variant === 'assignments' && assignmentsLoading" class="academy-aggregate-state">
          正在加载课程作业...
        </div>
        <div v-else-if="props.variant === 'assignments' && assignmentsError" class="academy-aggregate-state academy-aggregate-state-error">
          <span>{{ assignmentsError }}</span>
          <button type="button" @click="loadAssignments">重试</button>
        </div>
        <div v-else-if="props.variant === 'exams' && examsLoading" class="academy-aggregate-state">
          正在加载我的考试...
        </div>
        <div v-else-if="props.variant === 'exams' && examsError" class="academy-aggregate-state academy-aggregate-state-error">
          <span>{{ examsError }}</span>
          <button type="button" @click="loadExams">重试</button>
        </div>
        <div v-else-if="visibleCards.length === 0" class="academy-aggregate-state">
          {{ emptyStateText }}
        </div>

        <section v-else class="academy-aggregate-grid" aria-label="课程卡片展示区">
          <article
            v-for="card in visibleCards"
            :key="card.key || `${card.type}-${card.title}`"
            :class="['online-course-card', { 'academy-assignment-card': card.type === 'assignments' || card.type === 'exams' }]"
          >
            <RouterLink :to="card.link">
              <div class="academy-aggregate-cover" :style="{ background: card.cover }">
                <img v-if="card.coverImage" :src="card.coverImage" :alt="card.title" />
              </div>
              <div class="online-course-card-body">
                <div class="online-course-card-meta">
                  <span>{{ card.category }}</span>
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

      <aside class="academy-aggregate-sidebar" aria-label="右侧功能分区">
        <section v-for="section in sidebarSections" :key="section.title">
          <h2>{{ section.title }}</h2>
          <ul>
            <li v-for="item in section.items" :key="item">{{ item }}</li>
          </ul>
        </section>
      </aside>
    </section>
  </main>
</template>
