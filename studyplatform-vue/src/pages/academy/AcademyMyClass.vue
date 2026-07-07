<!-- 我的班级页面组件，展示班级成员、课程数据和学习排行榜 -->
<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

// 班级统计数据
const classStats = [
  { label: '班级成员', value: '42' },
  { label: '活跃成员', value: '36' },
  { label: '班级课程', value: '8' },
  { label: '待完成任务', value: '5' },
]

// 班级成员列表
const members = [
  { name: '林同学', role: '班长', progress: '92%' },
  { name: '陈同学', role: '学习委员', progress: '88%' },
  { name: '周同学', role: '成员', progress: '81%' },
  { name: '吴同学', role: '成员', progress: '76%' },
]

// 班级课程列表
const classCourses = [
  { title: 'C语言程序设计（下）', meta: '平均进度 78% · 2 项作业' },
  { title: '劳动通论', meta: '平均进度 64% · 1 项讨论' },
  { title: '数据分析微专业', meta: '平均进度 51% · 1 个项目' },
]

// 班级学习排行榜数据
const rankings = [
  { name: '林同学', score: 986, tag: '学习时长' },
  { name: '陈同学', score: 942, tag: '作业完成率' },
  { name: '周同学', score: 906, tag: '题库正确率' },
  { name: '吴同学', score: 872, tag: '考试成绩' },
]
</script>

<template>
  <!-- 我的班级页面主容器 -->
  <main class="academy-main academy-class-main">
    <!-- 页面顶部标题区域 -->
    <section class="academy-class-hero">
      <button type="button" @click="router.push('/academy/home')">返回首页</button>
      <div>
        <p class="academy-kicker">My Class</p>
        <h1>软件工程 2401 班</h1>
        <span>班级成员、课程数据和排行榜集中在这里，后续可接入真实班级数据库。</span>
      </div>
    </section>

    <!-- 班级概览统计 -->
    <section class="academy-class-stats" aria-label="班级概览">
      <article v-for="stat in classStats" :key="stat.label">
        <strong>{{ stat.value }}</strong>
        <span>{{ stat.label }}</span>
      </article>
    </section>

    <!-- 班级详情内容区域 -->
    <section class="academy-class-grid">
      <!-- 班级成员面板 -->
      <article class="academy-class-panel">
        <div class="academy-class-panel-heading">
          <h2>班级成员</h2>
          <span>成员与学习进度</span>
        </div>
        <div class="academy-member-list">
          <div v-for="member in members" :key="member.name" class="academy-member-item">
            <div>
              <strong>{{ member.name }}</strong>
              <span>{{ member.role }}</span>
            </div>
            <em>{{ member.progress }}</em>
          </div>
        </div>
      </article>

      <!-- 班级课程面板 -->
      <article class="academy-class-panel">
        <div class="academy-class-panel-heading">
          <h2>班级课程</h2>
          <span>课程进度与任务</span>
        </div>
        <div class="academy-class-course-list">
          <button
            v-for="course in classCourses"
            :key="course.title"
            type="button"
            @click="router.push({ path: '/academy/open-courses', query: { keyword: course.title } })"
          >
            <strong>{{ course.title }}</strong>
            <span>{{ course.meta }}</span>
          </button>
        </div>
      </article>

      <!-- 班级排行榜面板 -->
      <article class="academy-class-panel academy-ranking-panel">
        <div class="academy-class-panel-heading">
          <h2>班级排行榜</h2>
          <span>按学习表现综合排序</span>
        </div>
        <ol class="academy-ranking-list">
          <li v-for="(student, index) in rankings" :key="student.name">
            <strong>{{ index + 1 }}</strong>
            <div>
              <span>{{ student.name }}</span>
              <em>{{ student.tag }}</em>
            </div>
            <b>{{ student.score }}</b>
          </li>
        </ol>
      </article>
    </section>
  </main>
</template>
