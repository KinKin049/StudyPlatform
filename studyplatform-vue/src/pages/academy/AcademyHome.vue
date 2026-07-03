<script setup>
import { onMounted, ref } from 'vue'
import { fetchAcademyHome } from '../../api/academy'

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

const handleSectionAction = (section) => {
  // TODO: 接入后端操作接口，例如进入个人课程、作业中心或考试中心。
  console.info('academy section action reserved:', section.key)
}

const handleCardClick = (section, item) => {
  // TODO: 接入后端点击接口，例如打开课程学习页、作业详情或考试详情。
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
        <button type="button">搜索</button>
      </div>
    </section>

    <p v-if="loading" class="academy-home-hint">正在加载学堂首页...</p>
    <p v-else-if="loadError" class="academy-home-hint academy-home-warning">{{ loadError }}</p>

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
  </main>
</template>
