<!-- 课程详情页面组件，展示课程信息、视频播放、课程评价和教师信息 -->
<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import {
  createAcademyCourseReview,
  enrollAcademyCourse,
  fetchAcademyCourse,
  fetchAcademyCourseReviews,
  fetchMyAcademyCourses,
  replyAcademyCourseReview,
} from '../../api/academy'
import { chatWithAiPet } from '../../api/aiPet'
import { getStoredAuthUser } from '../../api/auth'
import { resolveResourceUrl } from '../../api/request'
import CourseReviewThread from '../../components/CourseReviewThread.vue'
import { useVideoLearningTimeTracker } from '../../composables/useLearningTimeTracker'
import { renderMessageMarkdown } from '../../utils/markdown'

// 组件属性定义
const props = defineProps({
  resource: {
    type: String,
    required: true,
  },
  listPath: {
    type: String,
    required: true,
  },
  moduleTitle: {
    type: String,
    required: true,
  },
  courseId: {
    type: String,
    required: true,
  },
})

// 课程数据
const course = ref(null)
// 页面加载状态
const loading = ref(true)
const error = ref('')
// 报名课程状态
const enrolling = ref(false)
const enrollmentState = ref('idle')
const enrollmentMessage = ref('')
const enrollmentToastVisible = ref(false)
let enrollmentToastTimer = null
// 课程评价数据
const reviews = ref([])
const reviewsLoading = ref(false)
const reviewError = ref('')
const authUser = ref(getStoredAuthUser())
// 评价表单数据
const reviewForm = ref({
  rating: 5,
  content: '',
})
const submittingReview = ref(false)
const activeReplyId = ref(null)
const replyDraft = ref('')
const submittingReply = ref(false)
// 课程视频引用
const courseVideoRef = ref(null)

// 使用视频学习时间追踪器
useVideoLearningTimeTracker(courseVideoRef, {
  moduleType: 'video',
  targetCode: () => `${props.resource}:${props.courseId}`,
  targetTitle: () => course.value?.name || props.moduleTitle,
})

// 智慧课堂功能列表
const featureItems = [
  { key: 'ai-assistant', icon: '◌', title: 'AI 助教智能问答', text: '基于课程介绍和概述进行即时答疑' },
]

/**
 * 计算属性：课程简介
 * 优先使用课程数据中的 overview 或 description，特殊课程使用预设简介，否则使用默认提示
 */
const courseIntro = computed(() => {
  if (!course.value) return ''
  if (course.value.overview || course.value.description) {
    return course.value.overview || course.value.description
  }
  if (course.value.description || course.value.comment) {
    return course.value.description || course.value.comment
  }

  if (course.value.id === '46004_1476538444') {
    return '《C语言程序设计(下)》面向已经具备一定程序设计基础的学习者，围绕函数、数组、指针、结构体、文件操作和综合程序设计能力展开训练。课程强调用代码解决实际问题，通过案例讲解、程序调试和阶段练习，帮助学生理解 C 语言的核心机制，形成清晰的算法表达能力和工程化编程习惯。无论你是准备完成课程作业、参加程序设计训练，还是为后续数据结构、操作系统、嵌入式开发等课程打基础，这门课都能帮助你把语法知识转化为稳定的编程能力。'
  }

  return '课程详情暂未提供完整简介，后续可由后台维护课程介绍、目录、学习目标和教学资源。'
})

/**
 * 计算属性：课程封面图片 URL
 */
const coverUrl = computed(() => {
  if (!course.value) return ''
  return resolveResourceUrl(course.value.cover || course.value.coverUrl)
})

/**
 * 计算属性：课程视频 URL
 */
const videoUrl = computed(() => {
  if (!course.value?.video) return ''
  return resolveResourceUrl(course.value.video)
})

/**
 * 计算属性：课程学习进度周数
 */
const courseWeeks = computed(() => {
  if (course.value?.semesterPlan) {
    return course.value.semesterPlan
  }
  if (course.value?.id === '46004_1476538444') {
    return '进行至第1周，共16周'
  }
  return '学习进度待同步'
})

/**
 * 计算属性：课程开课时间范围
 */
const coursePeriod = computed(() => {
  if (!course.value?.startTime) {
    return '开课时间待定'
  }
  if (course.value.id === '46004_1476538444') {
    return `${course.value.startTime} ~ 2026年07月12日`
  }
  return course.value.startTime
})

/**
 * 计算属性：授课教师列表
 * 将教师名字字符串分割为数组
 */
const teacherList = computed(() => {
  const teacherNames = String(course.value?.teacher || '')
    .split(/[、,，\s]+/)
    .map((name) => name.trim())
    .filter(Boolean)

  return teacherNames.length > 0 ? teacherNames : ['授课教师待补充']
})

/**
 * 计算属性：评价数量
 */
const countReviews = (items) => items.reduce((total, item) => total + 1 + countReviews(item.replies || []), 0)
const reviewCount = computed(() => countReviews(reviews.value))
const currentUserName = computed(() => authUser.value?.username || '未登录')

const courseAiOpen = ref(false)
const courseAiInput = ref('')
const courseAiLoading = ref(false)
const courseAiError = ref('')
const courseAiMessages = ref([
  {
    role: 'assistant',
    text: '你好，我会结合这门课的介绍、概述和教师信息回答问题。',
  },
])

const courseAiContext = computed(() => {
  const textSnippet = [
    `课程名称：${course.value?.name || ''}`,
    `课程分类：${course.value?.category || ''}`,
    `授课教师：${teacherList.value.join('、')}`,
    `课程简介：${courseIntro.value}`,
    `课程概述：${course.value?.description || course.value?.overview || course.value?.comment || ''}`,
    `学期计划：${course.value?.semesterPlan || courseWeeks.value}`,
    `开课时间：${coursePeriod.value}`,
  ].join('\n')
  return {
    path: window.location.pathname + window.location.search,
    routeName: 'academy-course-detail',
    title: `${course.value?.name || '课程详情'} AI 助手`,
    headings: [course.value?.name || '课程详情', '课程简介', '课程概述', 'AI 助教智能问答'],
    selectedText: '',
    formSnapshot: [
      `资源类型：${props.resource}`,
      `课程 ID：${props.courseId}`,
      `报名状态：${enrollmentState.value}`,
    ],
    contentLength: textSnippet.length,
    textSnippet,
  }
})

const buildCourseAiHistory = () => courseAiMessages.value
  .slice(-8)
  .map((message) => ({ role: message.role === 'user' ? 'user' : 'assistant', text: message.text }))

const toggleCourseAiAssistant = () => {
  courseAiOpen.value = !courseAiOpen.value
}

const submitCourseAiQuestion = async () => {
  const text = courseAiInput.value.trim()
  if (!text || courseAiLoading.value) return
  courseAiOpen.value = true
  courseAiInput.value = ''
  courseAiMessages.value.push({ role: 'user', text })
  courseAiLoading.value = true
  courseAiError.value = ''
  try {
    const response = await chatWithAiPet({
      message: text,
      pageContext: courseAiContext.value,
      history: buildCourseAiHistory(),
    })
    courseAiMessages.value.push({
      role: 'assistant',
      text: response.reply || '我没有拿到有效回复，请换个问题再试一次。',
    })
  } catch (err) {
    const message = err instanceof Error ? err.message : 'AI 助手暂时无法连接'
    courseAiError.value = message
    courseAiMessages.value.push({ role: 'assistant', text: message })
  } finally {
    courseAiLoading.value = false
  }
}

/**
 * 计算属性：分类路由
 * 根据课程分类生成列表页跳转路径
 */
const categoryRoute = computed(() => ({
  path: props.listPath,
  query: course.value?.category ? { category: course.value.category } : {},
}))

/**
 * 加载课程详情数据
 * 依次获取课程信息、报名状态和评价列表
 */
const loadCourse = async () => {
  loading.value = true
  error.value = ''
  enrollmentMessage.value = ''
  enrollmentState.value = 'idle'
  enrollmentToastVisible.value = false
  window.clearTimeout(enrollmentToastTimer)

  try {
    course.value = await fetchAcademyCourse(props.resource, props.courseId)
    await loadEnrollmentStatus()
    await loadReviews()
  } catch (err) {
    course.value = null
    error.value = err instanceof Error ? err.message : '课程详情加载失败'
  } finally {
    loading.value = false
  }
}

/**
 * 加载课程报名状态
 * 查询用户已报名课程列表，判断当前课程是否已报名
 */
const loadEnrollmentStatus = async () => {
  try {
    const enrolledCourses = await fetchMyAcademyCourses(1)
    const currentCourseId = String(props.courseId)
    const alreadyEnrolled = enrolledCourses.some(
      (item) => item.resourceType === props.resource && String(item.id) === currentCourseId,
    )
    enrollmentState.value = alreadyEnrolled ? 'joined' : 'idle'
  } catch {
    enrollmentState.value = 'idle'
  }
}

/**
 * 加载课程评价列表
 */
const loadReviews = async () => {
  reviewsLoading.value = true
  reviewError.value = ''

  try {
    reviews.value = await fetchAcademyCourseReviews(props.resource, props.courseId)
  } catch (err) {
    reviewError.value = err instanceof Error ? err.message : '评价加载失败'
    reviews.value = []
  } finally {
    reviewsLoading.value = false
  }
}

/**
 * 处理报名课程操作
 */
const handleEnroll = async () => {
  enrolling.value = true
  enrollmentMessage.value = ''

  try {
    await enrollAcademyCourse(props.resource, props.courseId, { userId: 1 })
    enrollmentState.value = 'success'
    enrollmentToastVisible.value = true
    window.clearTimeout(enrollmentToastTimer)
    enrollmentToastTimer = window.setTimeout(() => {
      enrollmentToastVisible.value = false
    }, 1800)
  } catch (err) {
    enrollmentMessage.value = err instanceof Error ? err.message : '参加课程失败'
  } finally {
    enrolling.value = false
  }
}

/**
 * 提交课程评价
 */
const submitReview = async () => {
  authUser.value = getStoredAuthUser()
  if (!authUser.value?.id) {
    reviewError.value = '请先登录后再发表评论'
    return
  }
  if (!reviewForm.value.content.trim()) {
    reviewError.value = '请先填写评价内容'
    return
  }

  submittingReview.value = true
  reviewError.value = ''

  try {
    await createAcademyCourseReview(props.resource, props.courseId, {
      rating: Number(reviewForm.value.rating),
      content: reviewForm.value.content.trim(),
    })
    reviewForm.value.content = ''
    await loadReviews()
  } catch (err) {
    reviewError.value = err instanceof Error ? err.message : '评价提交失败'
  } finally {
    submittingReview.value = false
  }
}

const startReply = (review) => {
  authUser.value = getStoredAuthUser()
  activeReplyId.value = review.id
  replyDraft.value = ''
  reviewError.value = ''
}

const cancelReply = () => {
  activeReplyId.value = null
  replyDraft.value = ''
}

const submitReply = async (review) => {
  authUser.value = getStoredAuthUser()
  if (!authUser.value?.id) {
    reviewError.value = '请先登录后再回复评论'
    return
  }
  if (!replyDraft.value.trim()) {
    reviewError.value = '请先填写回复内容'
    return
  }

  submittingReply.value = true
  reviewError.value = ''
  try {
    await replyAcademyCourseReview(review.id, {
      rating: 5,
      content: replyDraft.value.trim(),
    })
    cancelReply()
    await loadReviews()
  } catch (err) {
    reviewError.value = err instanceof Error ? err.message : '回复提交失败'
  } finally {
    submittingReply.value = false
  }
}

/**
 * 平滑滚动到评价区域
 */
const scrollToReviews = () => {
  document.getElementById('course-reviews')?.scrollIntoView({
    behavior: 'smooth',
    block: 'start',
  })
}

/**
 * 封面图片加载失败时使用备用地址
 */
const useCoverFallback = (event) => {
  if (course.value?.coverUrl && event.target.src !== course.value.coverUrl) {
    event.target.src = course.value.coverUrl
  }
}

onMounted(loadCourse)
onMounted(() => {
  window.addEventListener('study-platform:auth-updated', handleAuthUpdated)
})

onBeforeUnmount(() => {
  window.clearTimeout(enrollmentToastTimer)
  window.removeEventListener('study-platform:auth-updated', handleAuthUpdated)
})

const handleAuthUpdated = () => {
  authUser.value = getStoredAuthUser()
}

// 监听资源类型和课程 ID 变化，重新加载课程数据
watch(() => [props.resource, props.courseId], loadCourse)
</script>

<template>
  <!-- 课程详情页面主容器 -->
  <main class="course-detail-main course-playback-main">
    <!-- 报名成功提示 -->
    <Transition name="course-enrollment-toast">
      <div v-if="enrollmentToastVisible" class="course-enrollment-toast" role="status">
        已成功添加到我的课程
      </div>
    </Transition>

    <!-- 加载状态 -->
    <div v-if="loading" class="academy-state">正在加载课程详情...</div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="academy-state academy-state-error">
      <span>{{ error }}</span>
      <button type="button" @click="loadCourse">重试</button>
    </div>

    <!-- 课程详情内容 -->
    <article v-else-if="course" class="course-playback-shell">
      <!-- 面包屑导航 -->
      <nav class="course-breadcrumb" aria-label="面包屑导航">
        <RouterLink to="/academy/home">首页</RouterLink>
        <span aria-hidden="true">&gt;</span>
        <RouterLink :to="listPath">{{ moduleTitle }}</RouterLink>
        <span aria-hidden="true">&gt;</span>
        <RouterLink :to="categoryRoute">{{ course.category || '未分类' }}</RouterLink>
        <span aria-hidden="true">&gt;</span>
        <RouterLink :to="$route.fullPath">{{ course.name }}</RouterLink>
      </nav>

      <!-- 课程头部信息区域 -->
      <section class="course-hero-panel">
        <!-- 课程封面图 -->
        <div class="course-cover-stage">
          <img :src="coverUrl" :alt="course.name" @error="useCoverFallback" />
        </div>

        <!-- 课程信息卡片 -->
        <section class="course-info-card" aria-labelledby="course-title">
          <div class="course-info-topline">
            <div class="course-badge-group">
              <span>智慧慕课</span>
              <span>认证学习</span>
              <span>一流课程</span>
            </div>
          </div>

          <h1 id="course-title">{{ course.name }}</h1>

          <div class="course-opening-grid">
            <div>
              <span>参与人数</span>
              <strong>已有 {{ course.participants || 0 }} 人参加</strong>
            </div>
          </div>

          <dl class="course-schedule-list">
            <div>
              <dt>开课时间</dt>
              <dd>{{ coursePeriod }}</dd>
            </div>
            <div>
              <dt>学时安排</dt>
              <dd>3-5 小时每周</dd>
            </div>
            <div>
              <dt>学习进度</dt>
              <dd class="course-progress-text">{{ courseWeeks }}</dd>
            </div>
          </dl>

          <!-- 报名按钮 -->
          <div class="course-action-row">
            <button
              class="course-join-button"
              :class="{ 'is-enrolling': enrolling, 'is-joined': !enrolling && enrollmentState !== 'idle' }"
              type="button"
              :disabled="enrolling || enrollmentState !== 'idle'"
              @click="handleEnroll"
            >
              {{ enrolling ? '正在参加...' : enrollmentState === 'success' ? '参加成功' : enrollmentState === 'joined' ? '已参加' : '立即参加' }}
            </button>
            <span v-if="enrollmentMessage" class="course-enrollment-message">{{ enrollmentMessage }}</span>
          </div>
        </section>
      </section>

      <!-- 智慧课堂功能区域 -->
      <section class="smart-mooc-panel" aria-label="智慧课堂功能">
        <div class="smart-mooc-title">
          <strong>智慧课堂</strong>
          <span>利用人工智能技术，为你提供高效、个性的学习服务</span>
        </div>
        <div class="smart-feature-grid">
          <button
            v-for="item in featureItems"
            :key="item.title"
            type="button"
            :class="{ 'is-active': item.key === 'ai-assistant' && courseAiOpen }"
            @click="item.key === 'ai-assistant' ? toggleCourseAiAssistant() : null"
          >
            <span class="smart-feature-icon">{{ item.icon }}</span>
            <strong>{{ item.title }}</strong>
            <small>{{ item.text }}</small>
          </button>
        </div>
        <section v-if="courseAiOpen" class="course-ai-assistant" aria-label="课程 AI 助手">
          <div class="course-ai-messages">
            <article
              v-for="(message, index) in courseAiMessages"
              :key="`${message.role}-${index}`"
              :class="['course-ai-message', `is-${message.role}`]"
              v-html="renderMessageMarkdown(message.text)"
            ></article>
            <article v-if="courseAiLoading" class="course-ai-message is-assistant">正在结合课程介绍思考...</article>
          </div>
          <p v-if="courseAiError" class="course-ai-error">{{ courseAiError }}</p>
          <form class="course-ai-form" @submit.prevent="submitCourseAiQuestion">
            <textarea
              v-model="courseAiInput"
              rows="3"
              placeholder="围绕课程介绍、概述或学习安排提问"
            ></textarea>
            <button type="submit" :disabled="courseAiLoading || !courseAiInput.trim()">发送</button>
          </form>
        </section>
      </section>

      <!-- 课程主体内容布局 -->
      <div class="course-body-layout">
        <!-- 主内容区域 -->
        <section class="course-main-content">
          <!-- 内容切换标签 -->
          <nav class="course-tabs" aria-label="课程内容切换">
            <button class="active" type="button">课程详情</button>
            <button type="button" @click="scrollToReviews">课程评价 ({{ reviewCount }})</button>
          </nav>

          <!-- 课程简介卡片 -->
          <div class="course-description-card">
            <p>{{ courseIntro }}</p>
            <span>—— 课程团队</span>
          </div>

          <!-- 课程概述卡片 -->
          <section class="course-outline-card">
            <h2><span></span>课程概述</h2>
            <h3>一、为什么要学习这门课？</h3>
            <p v-if="course.description">
              {{ course.description }}
            </p>
            <p v-else>
              C 语言是理解计算机系统、算法实现和底层开发的重要基础。本课程通过连续案例训练，帮助学习者把语法知识落实到可运行、可调试、可扩展的程序中。
            </p>
          </section>

          <!-- 课程视频播放区域 -->
          <section class="course-player-page" aria-label="课程视频播放区域">
            <div class="course-player-placeholder">
              <video
                v-if="videoUrl"
                ref="courseVideoRef"
                :src="videoUrl"
                :poster="coverUrl"
                controls
                preload="metadata"
              ></video>
              <img v-else :src="coverUrl" :alt="course.name" @error="useCoverFallback" />
              <div v-if="!videoUrl" class="course-player-overlay">
                <span class="course-play-icon" aria-hidden="true"></span>
                <strong>课程视频播放页</strong>
                <p>视频资源接口预留，当前使用课程封面作为播放页占位。</p>
              </div>
            </div>
          </section>

          <!-- 课程评价区域 -->
          <section id="course-reviews" class="course-review-card" aria-label="课程评价">
            <div class="course-review-heading">
              <h2>课程评价</h2>
              <span>{{ reviewCount }} 条评价</span>
            </div>

            <!-- 评价提交表单 -->
            <form class="course-review-form" @submit.prevent="submitReview">
              <div class="review-form-row">
                <div class="review-user-display">
                  <span>当前用户</span>
                  <strong>{{ currentUserName }}</strong>
                </div>
                <label>
                  <span>评分</span>
                  <select v-model="reviewForm.rating">
                    <option :value="5">5 分</option>
                    <option :value="4">4 分</option>
                    <option :value="3">3 分</option>
                    <option :value="2">2 分</option>
                    <option :value="1">1 分</option>
                  </select>
                </label>
              </div>
              <textarea
                v-model="reviewForm.content"
                rows="4"
                maxlength="500"
                placeholder="写下你对这门课程的学习体验"
              ></textarea>
              <div class="review-form-actions">
                <span v-if="reviewError">{{ reviewError }}</span>
                <button type="submit" :disabled="submittingReview">
                  {{ submittingReview ? '提交中...' : '发布评价' }}
                </button>
              </div>
            </form>

            <!-- 评价列表 -->
            <div v-if="reviewsLoading" class="course-review-state">正在加载评价...</div>
            <div v-else-if="reviews.length === 0" class="course-review-state">暂无评价，欢迎发布第一条评价。</div>
            <div v-else class="course-review-list">
              <CourseReviewThread
                :reviews="reviews"
                :active-reply-id="activeReplyId"
                :reply-draft="replyDraft"
                :submitting-reply="submittingReply"
                @start-reply="startReply"
                @cancel-reply="cancelReply"
                @update:reply-draft="replyDraft = $event"
                @submit-reply="submitReply"
              />
            </div>
          </section>
        </section>

        <!-- 侧边栏信息 -->
        <aside class="course-side-card">
          <!-- 院校信息 -->
          <section class="school-profile">
            <div class="school-logo">{{ course.school?.slice(0, 2) || '院校' }}</div>
            <h2>{{ course.school || '院校信息待补充' }}</h2>
            <p>{{ course.school === '北京理工大学' ? 'BEIJING INSTITUTE OF TECHNOLOGY' : 'UNIVERSITY COURSE PROVIDER' }}</p>
          </section>

          <!-- 教师信息 -->
          <section class="teacher-profile">
            <h3><span></span>{{ teacherList.length }} 位授课老师</h3>
            <div class="teacher-list">
              <article v-for="(teacher, index) in teacherList" :key="teacher" class="teacher-card">
                <div class="teacher-avatar">{{ teacher.slice(0, 1) }}</div>
                <div>
                  <strong>{{ teacher }}</strong>
                  <p>{{ index === 0 ? '主讲教师' : '授课教师' }}</p>
                </div>
              </article>
            </div>
          </section>

          <!-- 侧边栏操作 -->
          <div class="course-side-actions">
            <RouterLink class="course-side-link" :to="listPath">返回课程列表</RouterLink>
          </div>
        </aside>
      </div>
    </article>
  </main>
</template>

<style scoped>
.review-user-display {
  display: grid;
  gap: 6px;
  min-width: 180px;
}

.review-user-display span {
  color: #64748b;
  font-size: 13px;
}

.review-user-display strong {
  color: #0f172a;
}

.course-ai-assistant {
  display: grid;
  gap: 12px;
  margin-top: 16px;
  padding: 16px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.72);
}

.course-ai-messages {
  display: grid;
  gap: 10px;
  max-height: 280px;
  overflow-y: auto;
}

.course-ai-message {
  max-width: 88%;
  border-radius: 14px;
  padding: 10px 12px;
  color: #0f172a;
  line-height: 1.7;
  white-space: pre-wrap;
}

.course-ai-message :deep(p) {
  margin: 0;
}

.course-ai-message :deep(p + p),
.course-ai-message :deep(pre),
.course-ai-message :deep(ul),
.course-ai-message :deep(ol),
.course-ai-message :deep(blockquote) {
  margin: 8px 0 0;
}

.course-ai-message :deep(code) {
  border-radius: 6px;
  background: rgba(15, 23, 42, 0.08);
  padding: 2px 5px;
  font-family: Consolas, 'Courier New', monospace;
}

.course-ai-message :deep(pre) {
  overflow-x: auto;
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.08);
  padding: 10px;
}

.course-ai-message :deep(pre code) {
  background: transparent;
  padding: 0;
}

.course-ai-message :deep(a) {
  color: inherit;
  font-weight: 800;
  text-decoration: underline;
}

.course-ai-message.is-assistant {
  justify-self: start;
  background: #eef2ff;
}

.course-ai-message.is-user {
  justify-self: end;
  background: #2563eb;
  color: #fff;
}

.course-ai-error {
  margin: 0;
  color: #dc2626;
  font-size: 13px;
}

.course-ai-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
  align-items: end;
}

.course-ai-form textarea {
  resize: vertical;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  background: #ffffff;
  padding: 10px 12px;
  color: #0f172a;
}

.course-ai-form button {
  border: 0;
  border-radius: 999px;
  background: #4f46e5;
  color: #fff;
  cursor: pointer;
  font-weight: 800;
  padding: 10px 18px;
}

.course-ai-form button:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

:deep(.smart-feature-grid button.is-active) {
  border-color: rgba(79, 70, 229, 0.35);
  box-shadow: 0 18px 36px rgba(79, 70, 229, 0.14);
}

@media (max-width: 640px) {
  .course-ai-form {
    grid-template-columns: 1fr;
  }
}

:deep(.course-review-thread) {
  display: grid;
  gap: 14px;
}

:deep(.course-review-item) {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 12px;
}

:deep(.review-avatar) {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: #e0f2fe;
  color: #0369a1;
  font-weight: 800;
}

:deep(.review-avatar-small) {
  width: 34px;
  height: 34px;
  font-size: 13px;
}

:deep(.review-body) {
  min-width: 0;
}

:deep(.review-body header) {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

:deep(.review-author) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

:deep(.review-body p) {
  margin: 8px 0;
  color: #334155;
  line-height: 1.7;
}

:deep(.review-stars) {
  color: #f59e0b;
  font-size: 13px;
}

:deep(.review-reply-target) {
  color: #64748b;
  font-size: 13px;
}

:deep(.review-role-badge) {
  padding: 2px 8px;
  border-radius: 999px;
  background: #e0f2fe;
  color: #0369a1;
  font-size: 12px;
  font-weight: 700;
}

:deep(.review-body footer) {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #94a3b8;
  font-size: 13px;
}

:deep(.review-body footer button),
:deep(.course-reply-form button) {
  border: 0;
  border-radius: 999px;
  background: #e0f2fe;
  color: #0369a1;
  cursor: pointer;
  font-weight: 700;
  padding: 6px 12px;
}

:deep(.course-review-replies) {
  display: grid;
  gap: 10px;
  margin-top: 12px;
  padding: 12px;
  border-radius: 10px;
  background: #f8fbff;
  border: 1px solid #e0f2fe;
}

:deep(.course-reply-item) {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
}

:deep(.course-reply-item + .course-reply-item) {
  padding-top: 10px;
  border-top: 1px solid #e2e8f0;
}

:deep(.course-review-reply) {
  display: grid;
  gap: 6px;
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #eef6ff;
  color: #1e3a8a;
}

:deep(.course-review-reply strong) {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

:deep(.course-review-reply span) {
  padding: 2px 8px;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 12px;
}

:deep(.course-review-reply p) {
  margin: 0;
}

:deep(.course-reply-form) {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

:deep(.course-reply-form textarea) {
  min-height: 86px;
  resize: vertical;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  background: #ffffff;
  padding: 10px 12px;
  color: #0f172a;
}

:deep(.course-reply-form textarea:focus) {
  background: #ffffff;
  color: #0f172a;
}

:deep(.course-reply-form div) {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

:deep(.course-reply-form button[type='submit']) {
  background: #2563eb;
  color: #fff;
}

:deep(.course-reply-form button:disabled) {
  cursor: not-allowed;
  opacity: 0.65;
}
</style>
