<script setup>
/**
 * 题库首页组件
 * 提供课程题库、错题本、收藏题目和随机组卷的入口，展示学习统计数据
 */
import { computed, onMounted, ref } from 'vue'
import { Collection, Connection, Notebook, Reading } from '@element-plus/icons-vue'
import { RouterLink, useRouter } from 'vue-router'
import { fetchQuestionBankFavoriteSummary, fetchQuestionBankMistakeSummary } from '../../api/academy'

/** 路由实例 */
const router = useRouter()

/** 错题统计数据 */
const mistakeSummary = ref({
  total: 0,
  active: 0,
  mastered: 0,
  sets: [],
})

/** 收藏统计数据 */
const favoriteSummary = ref({
  total: 0,
  sets: [],
})

/** 预留功能模块（暂未实现） */
const reservedModules = [
  {
    key: 'mock-exam',
    title: '随机组卷',
    description: '按课程、难度和题型随机生成模拟试卷。',
    count: '智能',
  },
]

/** 待复习错题数量 */
const activeMistakeCount = computed(() => Number(mistakeSummary.value.active || 0))

/** 已掌握错题数量 */
const masteredMistakeCount = computed(() => Number(mistakeSummary.value.mastered || 0))

/** 收藏题目数量 */
const favoriteCount = computed(() => Number(favoriteSummary.value.total || 0))

/** 学习统计数据列表 */
const studyStats = computed(() => [
  { label: '今日练习', value: '0 题' },
  { label: '待复习错题', value: `${activeMistakeCount.value} 题` },
  { label: '收藏题目', value: `${favoriteCount.value} 题` },
])

/** 加载错题统计数据 */
const loadMistakeSummary = async () => {
  try {
    mistakeSummary.value = await fetchQuestionBankMistakeSummary()
  } catch (error) {
    console.warn('failed to load question bank mistake summary:', error)
  }
}

/** 加载收藏统计数据 */
const loadFavoriteSummary = async () => {
  try {
    favoriteSummary.value = await fetchQuestionBankFavoriteSummary()
  } catch (error) {
    console.warn('failed to load question bank favorite summary:', error)
  }
}

/** 处理预留模块点击事件 */
const handleModuleClick = (module) => {
  if (module.path) {
    router.push(module.path)
    return
  }
  console.info('question bank module action reserved:', module.key)
}

/** 组件挂载时加载统计数据 */
onMounted(() => {
  loadMistakeSummary()
  loadFavoriteSummary()
})
</script>

<template>
  <main class="academy-main question-bank-main">
    <!-- 题库头部区域：标题和学习统计 -->
    <section class="question-bank-hero" aria-labelledby="question-bank-title">
      <div>
        <h1 id="question-bank-title">题库</h1>
        <span>围绕课程学习建立练习、错题、收藏和模拟组卷入口，后续可接入统一题目 API。</span>
      </div>
      <!-- 学习统计数据展示 -->
      <div class="question-bank-stats" aria-label="题库学习状态">
        <div v-for="item in studyStats" :key="item.label">
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
        </div>
      </div>
    </section>

    <!-- 题库功能模块区域 -->
    <section class="question-bank-panel" aria-label="题库功能模块">
      <div class="question-bank-panel-head">
        <div>
          <h2>学习入口</h2>
          <span>选择一个入口开始练习或整理题目。</span>
        </div>
        <button type="button" @click="router.push('/academy/question-bank/mistakes')">我的错题</button>
      </div>

      <!-- 功能模块卡片网格 -->
      <div class="question-bank-grid">
        <!-- 课程题库入口 -->
        <RouterLink to="/academy/question-bank/courses" class="question-bank-card">
          <div>
            <h3><el-icon class="question-bank-card-icon"><Reading /></el-icon>课程题库</h3>
            <span>按计算机专业、英语四六级、公共课和职业资格组织课程题库。</span>
          </div>
          <strong>课程</strong>
        </RouterLink>

        <!-- 错题本入口 -->
        <RouterLink to="/academy/question-bank/mistakes" class="question-bank-card">
          <div>
            <h3><el-icon class="question-bank-card-icon"><Notebook /></el-icon>错题本</h3>
            <span>自动收集课程题库中的错误答案，支持按题库筛选、分页复习和连续答对后标记掌握。</span>
          </div>
          <strong>{{ activeMistakeCount }}</strong>
        </RouterLink>

        <!-- 收藏题目入口 -->
        <RouterLink to="/academy/question-bank/favorites" class="question-bank-card">
          <div>
            <h3><el-icon class="question-bank-card-icon"><Collection /></el-icon>收藏题目</h3>
            <span>保存重点题目和高频题型，支持按题库筛选、快速查看和一键取消收藏。</span>
          </div>
          <strong>{{ favoriteCount }}</strong>
        </RouterLink>

        <!-- 预留功能模块 -->
        <button
          v-for="module in reservedModules"
          :key="module.key"
          type="button"
          class="question-bank-card"
          @click="handleModuleClick(module)"
        >
          <div>
            <h3><el-icon class="question-bank-card-icon"><Connection /></el-icon>{{ module.title }}</h3>
            <span>{{ module.description }}</span>
          </div>
          <strong>{{ module.count }}</strong>
        </button>
      </div>
    </section>
  </main>
</template>
