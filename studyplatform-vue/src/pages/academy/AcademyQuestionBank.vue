<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const questionBankModules = [
  {
    key: 'course-questions',
    title: '课程题库',
    label: '按课程练习',
    description: '按计算机专业、英语四六级、公共课和职业资格组织课程题库。',
    count: '课程',
    meta: '/academy/question-bank/courses',
    path: '/academy/question-bank/courses',
  },
  {
    key: 'mistakes',
    title: '错题本',
    label: '重点复盘',
    description: '收集练习和考试中的错题，后续支持按知识点复习。',
    count: '0',
    meta: '预留 /api/academy/question-bank/mistakes',
  },
  {
    key: 'favorites',
    title: '收藏题目',
    label: '个人收藏',
    description: '保存重点题目和高频题型，方便反复查看。',
    count: '0',
    meta: '预留 /api/academy/question-bank/favorites',
  },
  {
    key: 'mock-exam',
    title: '随机组卷',
    label: '模拟训练',
    description: '按课程、难度和题型随机生成模拟试卷。',
    count: '智能',
    meta: '预留 /api/academy/question-bank/mock-exams',
  },
]

const studyStats = [
  { label: '今日练习', value: '0 题' },
  { label: '正确率', value: '--' },
  { label: '连续学习', value: '0 天' },
]

const handleModuleClick = (module) => {
  if (module.path) {
    router.push(module.path)
    return
  }
  // TODO: 接入题库模块路由或后端 API，例如 /academy/question-bank/questions?module=...
  console.info('question bank module action reserved:', module.key)
}
</script>

<template>
  <main class="academy-main question-bank-main">
    <section class="question-bank-hero" aria-labelledby="question-bank-title">
      <div>
        <p>Question Bank</p>
        <h1 id="question-bank-title">题库</h1>
        <span>围绕课程学习建立练习、错题、收藏和模拟组卷入口，后续可接入统一题目 API。</span>
      </div>
      <div class="question-bank-stats" aria-label="题库学习状态">
        <div v-for="item in studyStats" :key="item.label">
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
        </div>
      </div>
    </section>

    <section class="question-bank-panel" aria-label="题库功能模块">
      <div class="question-bank-panel-head">
        <div>
          <h2>学习入口</h2>
          <span>选择一个入口开始练习或整理题目。</span>
        </div>
        <button type="button" @click="handleModuleClick({ key: 'overview' })">学习记录</button>
      </div>

      <div class="question-bank-grid">
        <button
          v-for="module in questionBankModules"
          :key="module.key"
          type="button"
          class="question-bank-card"
          @click="handleModuleClick(module)"
        >
          <div>
            <p>{{ module.label }}</p>
            <h3>{{ module.title }}</h3>
            <span>{{ module.description }}</span>
            <small>{{ module.meta }}</small>
          </div>
          <strong>{{ module.count }}</strong>
        </button>
      </div>
    </section>
  </main>
</template>
