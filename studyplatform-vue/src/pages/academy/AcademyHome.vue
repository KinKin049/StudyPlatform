<script setup>
import { onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { fetchAcademyHome } from '../../api/academy'

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

const handleAddCourse = () => {
  const normalizedUid = courseUid.value.trim()

  if (!normalizedUid) {
    return
  }

  console.info('add course by uid reserved:', normalizedUid)
  courseUid.value = ''
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

onMounted(loadAcademyHome)
</script>

<template>
  <main class="academy-main">
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

    <div class="academy-home-divider" aria-hidden="true"></div>

    <section class="academy-home-layout" aria-label="学堂控制台">
      <aside class="academy-sidebar" aria-label="学习侧边栏">
        <section class="academy-side-card">
          <div class="academy-side-heading">
            <span>学习概览</span>
            <strong>今日状态</strong>
          </div>
          <div class="academy-overview-grid">
            <div v-for="stat in overviewStats" :key="stat.label" class="academy-overview-item">
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
            <input v-model="courseUid" type="text" placeholder="输入课程 UID" aria-label="输入课程 UID" />
            <button type="submit">添加</button>
          </form>
          <p class="academy-side-note">后续可接入课程邀请码或教师发放的课程唯一编号。</p>
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
              v-for="course in recentCourses"
              :key="course.title"
              type="button"
              @click="router.push({ path: course.path, query: { keyword: course.title } })"
            >
              <strong>{{ course.title }}</strong>
              <span>{{ course.meta }}</span>
            </button>
          </div>
        </section>

        <section class="academy-side-card">
          <div class="academy-side-heading">
            <span>课程动态</span>
            <strong>班级与课程消息</strong>
          </div>
          <ul class="academy-feed-list">
            <li v-for="feed in courseFeeds" :key="feed">{{ feed }}</li>
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
