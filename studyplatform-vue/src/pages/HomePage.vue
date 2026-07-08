<script setup>
// 平台首页：未登录时作为项目展示门面，登录后作为学习仪表盘入口
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { getStoredAuthUser } from '../api/auth'
import { fetchProfileOverview } from '../api/profile'
import academyFutureImage from '../assets/home/academy.jpg'
import gamesFutureImage from '../assets/home/games.jpg'
import labFutureImage from '../assets/home/lab-oj.jpg'
import practiceFutureImage from '../assets/home/practice.jpg'

const authUser = ref(getStoredAuthUser())
const overview = ref(null)
const overviewLoading = ref(false)
const overviewError = ref('')
const modulesRef = ref(null)
const moduleStageRef = ref(null)
const activeModuleIndex = ref(0)
const moduleScrollPhase = ref(0)
const activeDashboardSlide = ref(0)
const dashboardSlideCount = 4
let dashboardCarouselTimer = null

const emptyOverview = {
  stats: [
    { label: '累计学习', value: '0m', hint: '完成学习后自动统计' },
    { label: '今日学习', value: '0m', hint: '今天的真实学习时长' },
    { label: '练习记录', value: '0', hint: '答题 / 背词 / 查答案' },
  ],
  overallProgress: 0,
  difficultyStats: [
    { label: '选择题', solved: 0, total: 0, color: '#4f6fd8' },
    { label: '词汇卡片', solved: 0, total: 0, color: '#79c36a' },
    { label: '主观题', solved: 0, total: 0, color: '#f2c04c' },
  ],
  codingDifficulties: [
    { label: '简单', level: 'EASY', solved: 0, total: 0, color: '#5fbf9f' },
    { label: '中等', level: 'MEDIUM', solved: 0, total: 0, color: '#f2c04c' },
    { label: '困难', level: 'HARD', solved: 0, total: 0, color: '#e87575' },
  ],
}

const practicePalette = ['#EFBBCF', '#C3AED6', '#8675A9']
const modules = [
  {
    key: 'academy',
    eyebrow: 'Academy',
    title: '在线学堂',
    copy: '聚合开放课程、通识课程、微专业、视频学习和精品教材，让课程资源、加入学习、教材订单形成一条完整路径。',
    path: '/academy/home',
    action: '进入学堂',
    tone: 'green',
    background: '#f4fadf',
    image: academyFutureImage,
    visualTitle: 'Course Hub',
    visualMeta: '课程 · 视频 · 教材',
    highlights: ['课程资源', '视频学习', '精品教材'],
  },
  {
    key: 'question-bank',
    eyebrow: 'Practice',
    title: '题库练习',
    copy: '围绕课程题库、错题本、收藏题目和英语词汇训练组织练习反馈，让每一次答题都能沉淀为学习画像。',
    path: '/academy/question-bank',
    action: '开始练习',
    tone: 'blue',
    background: '#e8f6fa',
    image: practiceFutureImage,
    visualTitle: 'Practice Flow',
    visualMeta: '错题 · 收藏 · 词汇',
    highlights: ['课程题库', '错题复习', '词汇训练'],
  },
  {
    key: 'lab',
    eyebrow: 'Lab & OJ',
    title: '实验与 OJ',
    copy: '把算法可视化、空间模型、石油气仿真和在线编程判题放到同一实验入口中，支持从观察到提交的完整实践。',
    path: '/lab',
    action: '进入实验',
    tone: 'violet',
    background: '#f0edff',
    image: labFutureImage,
    visualTitle: 'Simulation Lab',
    visualMeta: '算法 · 空间 · 石油气',
    highlights: ['可视化', '仿真实验', '在线判题'],
  },
  {
    key: 'games',
    eyebrow: 'Games',
    title: '学习游戏',
    copy: '通过万题天梯跳与 Type Warrior 降低练习负担，把闯关、打字、金币激励连接成更轻量的学习循环。',
    path: '/games',
    action: '玩一局',
    tone: 'amber',
    background: '#fff2d9',
    image: gamesFutureImage,
    visualTitle: 'Game Loop',
    visualMeta: '闯关 · 打字 · 激励',
    highlights: ['万题天梯跳', 'Type Warrior', '金币激励'],
  },
]

const isLoggedIn = computed(() => Boolean(authUser.value?.id))
const displayName = computed(() => authUser.value?.username || '同学')
const dashboard = computed(() => overview.value || emptyOverview)
const stats = computed(() => dashboard.value.stats?.length ? dashboard.value.stats.slice(0, 3) : emptyOverview.stats)
const findStatValue = (keyword, fallback) =>
  dashboard.value.stats?.find((item) => item.label?.includes(keyword))?.value ?? fallback
const applyChartPalette = (items, palette) =>
  items.map((item, index) => ({
    ...item,
    color: palette[index % palette.length],
  }))
const difficultyStats = computed(() =>
  applyChartPalette(
    dashboard.value.difficultyStats?.length ? dashboard.value.difficultyStats : emptyOverview.difficultyStats,
    practicePalette,
  ),
)
const overallProgress = computed(() => {
  const progress = Number(dashboard.value.overallProgress ?? 0)
  return Math.min(Math.max(Number.isFinite(progress) ? Math.round(progress) : 0, 0), 100)
})
const practiceRingStyle = computed(() => ({
  '--home-practice-progress': `${overallProgress.value}%`,
}))
const donutRenderKey = ref(0)
const practiceSolvedTotal = computed(() =>
  difficultyStats.value.reduce((sum, item) => sum + Number(item.solved || 0), 0),
)

const buildDonutSegments = (items) => {
  const segments = items.map((item) => ({
    label: item.label,
    solved: Number(item.solved || 0),
    color: item.color || '#60a5fa',
  })).filter((item) => item.solved > 0)

  const solvedTotal = segments.reduce((sum, item) => sum + item.solved, 0)
  if (!solvedTotal) return []

  const gap = segments.length > 1 ? 8 : 0
  const available = 360 - gap * segments.length
  let cursor = 0
  const clampAngle = (value) => Math.max(0, Math.min(value, 360))
  const toAngle = (value) => `${clampAngle(value).toFixed(2)}deg`

  if (gap) {
    cursor = gap / 2
  }

  return segments.map((item, index) => {
    const segmentStart = cursor
    const segmentEnd = cursor + (item.solved / solvedTotal) * available
    cursor = segmentEnd

    if (gap) {
      const gapSize = index === segments.length - 1 ? gap / 2 : gap
      cursor += gapSize
    }

    return {
      label: item.label,
      style: {
        '--home-donut-color': item.color,
        '--home-donut-start': toAngle(segmentStart),
        '--home-donut-end': toAngle(segmentEnd),
        '--home-donut-fill-start': toAngle(segmentEnd),
        '--home-donut-index': index,
      },
    }
  })
}

const practiceDonutSegments = computed(() => buildDonutSegments(difficultyStats.value))
const learningOverviewStats = computed(() => {
  const overviewStats = dashboard.value.learningOverview || dashboard.value.academyOverview || {}
  return [
    { label: '已加入课程', value: overviewStats.joinedCourses ?? overviewStats.courseCount ?? 4 },
    { label: '进行中', value: overviewStats.inProgressCourses ?? overviewStats.activeCourses ?? 4 },
    { label: '待完成作业', value: overviewStats.pendingAssignments ?? overviewStats.assignmentCount ?? 1 },
    { label: '即将考试', value: overviewStats.upcomingExams ?? overviewStats.examCount ?? 2 },
  ]
})
const learningTimeStats = computed(() => {
  const timeStats = dashboard.value.learningTime || dashboard.value.timeOverview || {}
  return [
    {
      label: '学习时长',
      value: timeStats.totalText ?? timeStats.total ?? findStatValue('累计学习', '0m'),
      desc: '题库、课程、OJ、可视化和油气仿真累计',
    },
    {
      label: '可视化时长',
      value: timeStats.visualizationText ?? timeStats.visualization ?? '0m',
      desc: '仅统计可视化具体页面',
    },
  ]
})
const gameDataStats = computed(() => {
  const gameStats = dashboard.value.gameStats || dashboard.value.gamesOverview || {}
  const ladder = gameStats.ladderJump || {}
  const typeWarrior = gameStats.typeWarrior || {}
  return [
    {
      label: '游戏总时长',
      value: gameStats.totalDurationText ?? gameStats.totalDuration ?? '0m',
      meta: `总和 ${gameStats.totalDurationText ?? gameStats.totalDuration ?? '0m'} · 最佳 ${gameStats.bestDurationText ?? gameStats.bestDuration ?? '0m'} · 平均 ${gameStats.averageDurationText ?? gameStats.averageDuration ?? '0m'}`,
    },
    {
      label: '万题天梯跳答对题数',
      value: ladder.correctCount ?? ladder.totalCorrect ?? 0,
      meta: `总和 ${ladder.totalCorrect ?? 0} · 最佳 ${ladder.bestCorrect ?? 0} · 平均 ${ladder.averageCorrect ?? '0.0'}`,
    },
    {
      label: 'Type Warrior 得分',
      value: typeWarrior.score ?? typeWarrior.totalScore ?? 0,
      meta: `总和 ${typeWarrior.totalScore ?? 0} · 最佳 ${typeWarrior.bestScore ?? 0} · 平均 ${typeWarrior.averageScore ?? '0.0'}`,
    },
  ]
})
const currentModule = computed(() => modules[activeModuleIndex.value] || modules[0])
const clampNumber = (value, min, max) => Math.min(Math.max(value, min), max)
const moduleShowcaseStyle = computed(() => ({
  '--home-module-bg': currentModule.value.background,
}))
const moduleVisualStyles = computed(() =>
  modules.map((_, index) => {
    const revealProgress = index === modules.length - 1 ? 0 : clampNumber(moduleScrollPhase.value - index, 0, 1)
    const driftProgress = clampNumber(moduleScrollPhase.value - index + 1, 0, 1)

    return {
      '--home-module-image-clip': `${revealProgress * 100}%`,
      '--home-module-image-y': `${60 - driftProgress * 20}%`,
      '--home-module-image-z': modules.length - index,
    }
  }),
)
const refreshDonutAnimation = () => {
  requestAnimationFrame(() => {
    donutRenderKey.value += 1
  })
}
const stopDashboardCarousel = () => {
  if (!dashboardCarouselTimer) return
  window.clearInterval(dashboardCarouselTimer)
  dashboardCarouselTimer = null
}
const setDashboardSlide = (index) => {
  activeDashboardSlide.value = (index + dashboardSlideCount) % dashboardSlideCount
}
const restartDashboardCarousel = () => {
  stopDashboardCarousel()
  if (!isLoggedIn.value) return
  dashboardCarouselTimer = window.setInterval(() => {
    setDashboardSlide(activeDashboardSlide.value + 1)
  }, 2000)
}
const goDashboardSlide = (index) => {
  setDashboardSlide(index)
  restartDashboardCarousel()
}
const shiftDashboardSlide = (offset) => {
  goDashboardSlide(activeDashboardSlide.value + offset)
}

const updateModuleScrollState = () => {
  const scrollTarget = moduleStageRef.value || modulesRef.value
  if (!scrollTarget) return

  const rect = scrollTarget.getBoundingClientRect()
  const scrollableDistance = Math.max(scrollTarget.offsetHeight - window.innerHeight, 1)
  const progress = Math.min(Math.max(-rect.top / scrollableDistance, 0), 1)
  const phase = progress * (modules.length - 1)
  const nextIndex = Math.round(phase)

  moduleScrollPhase.value = phase
  activeModuleIndex.value = Math.min(Math.max(nextIndex, 0), modules.length - 1)
}

const loadOverview = async () => {
  if (!isLoggedIn.value) {
    overview.value = null
    overviewError.value = ''
    return
  }

  overviewLoading.value = true
  overviewError.value = ''
  try {
    overview.value = await fetchProfileOverview()
  } catch (error) {
    overview.value = null
    overviewError.value = '暂时没有读取到学习画像，完成练习后这里会自动刷新。'
    console.warn('failed to load home dashboard:', error)
  } finally {
    overviewLoading.value = false
    refreshDonutAnimation()
  }
}

const handleAuthUpdated = (event) => {
  authUser.value = event.detail || getStoredAuthUser()
}

watch(isLoggedIn, () => {
  loadOverview()
  nextTick(restartDashboardCarousel)
})

onMounted(() => {
  loadOverview()
  restartDashboardCarousel()
  window.addEventListener('study-platform:auth-updated', handleAuthUpdated)
  window.addEventListener('scroll', updateModuleScrollState, { passive: true })
  window.addEventListener('resize', updateModuleScrollState)
  nextTick(updateModuleScrollState)
})

onBeforeUnmount(() => {
  stopDashboardCarousel()
  window.removeEventListener('study-platform:auth-updated', handleAuthUpdated)
  window.removeEventListener('scroll', updateModuleScrollState)
  window.removeEventListener('resize', updateModuleScrollState)
})
</script>

<template>
  <main class="home-main">
    <section class="home-hero" :class="{ 'is-dashboard': isLoggedIn }" aria-labelledby="home-title">
      <div class="home-hero-copy">
        <p v-if="!isLoggedIn" class="home-kicker">StudyPlatform</p>
        <h1 id="home-title">
          {{ isLoggedIn ? `欢迎回来，${displayName}` : '把课程、练习、实验与游戏放进同一个学习空间' }}
        </h1>
        <p v-if="!isLoggedIn" class="home-copy">
          {{
            'StudyPlatform 面向课程学习、题库训练、实验仿真与游戏化练习，帮助学习过程从“打开页面”变成可追踪、可反馈、可激励的成长路径。'
          }}
        </p>
        <div class="home-hero-actions">
          <RouterLink class="home-primary-action" :to="isLoggedIn ? '/profile' : '/login'">
            {{ isLoggedIn ? '查看完整个人主页' : '登录后查看仪表盘' }}
          </RouterLink>
          <RouterLink class="home-secondary-action" :to="isLoggedIn ? '/academy/home' : '/register'">
            {{ isLoggedIn ? '继续学习' : '注册账号' }}
          </RouterLink>
        </div>
      </div>

      <div v-if="isLoggedIn" class="home-hero-rings">
        <article class="home-dashboard-carousel" aria-label="学习仪表盘轮播">
          <button
            type="button"
            class="home-dashboard-carousel-arrow is-prev"
            aria-label="上一页"
            @click="shiftDashboardSlide(-1)"
          >
            ‹
          </button>
          <button
            type="button"
            class="home-dashboard-carousel-arrow is-next"
            aria-label="下一页"
            @click="shiftDashboardSlide(1)"
          >
            ›
          </button>

          <div class="home-dashboard-carousel-window">
            <div
              class="home-dashboard-carousel-track"
              :style="{ transform: `translate3d(${-activeDashboardSlide * 100}%, 0, 0)` }"
            >
              <section class="home-dashboard-slide" aria-label="练习分布">
                <div class="home-dashboard-card is-practice-only">
                  <div class="home-dashboard-card-head">
                    <span>总计 {{ practiceSolvedTotal }} 个</span>
                  </div>
                  <div class="home-donut-card-body">
                    <div class="home-dashboard-legend">
                      <div v-for="item in difficultyStats" :key="item.label">
                        <i :style="{ background: item.color }"></i>
                        <span>{{ item.label }}</span>
                        <small>{{ item.solved }} / {{ item.total }}</small>
                      </div>
                    </div>
                    <div class="home-donut-wrap" :key="`practice-${donutRenderKey}`">
                      <div
                        v-if="practiceDonutSegments.length"
                        class="home-donut-ring"
                        role="img"
                        :aria-label="`练习分布总计 ${practiceSolvedTotal} 个`"
                      >
                        <span
                          v-for="segment in practiceDonutSegments"
                          :key="segment.label"
                          class="home-donut-segment"
                          :style="segment.style"
                          aria-hidden="true"
                        ></span>
                      </div>
                      <span v-else class="home-donut-empty">暂无数据</span>
                    </div>
                  </div>
                </div>
              </section>

              <section class="home-dashboard-slide" aria-label="学习概览">
                <div class="home-dashboard-panel">
                  <p>学习概览</p>
                  <h3>今日状态</h3>
                  <div class="home-dashboard-metric-grid">
                    <div v-for="item in learningOverviewStats" :key="item.label">
                      <strong>{{ item.value }}</strong>
                      <span>{{ item.label }}</span>
                    </div>
                  </div>
                </div>
              </section>

              <section class="home-dashboard-slide" aria-label="学习时长">
                <div class="home-dashboard-panel">
                  <p>学习时长</p>
                  <div class="home-dashboard-list">
                    <div v-for="item in learningTimeStats" :key="item.label">
                      <span>{{ item.label }}</span>
                      <strong>{{ item.value }}</strong>
                      <small>{{ item.desc }}</small>
                    </div>
                  </div>
                </div>
              </section>

              <section class="home-dashboard-slide" aria-label="游戏数据">
                <div class="home-dashboard-panel">
                  <p>游戏数据</p>
                  <div class="home-dashboard-list is-compact">
                    <div v-for="item in gameDataStats" :key="item.label">
                      <span>{{ item.label }}</span>
                      <strong>{{ item.value }}</strong>
                      <small>{{ item.meta }}</small>
                    </div>
                  </div>
                </div>
              </section>
            </div>
          </div>

          <div class="home-dashboard-carousel-dots" aria-label="轮播分页">
            <button
              v-for="index in dashboardSlideCount"
              :key="index"
              type="button"
              :class="{ 'is-active': activeDashboardSlide === index - 1 }"
              :aria-label="`切换到第 ${index} 页`"
              @click="goDashboardSlide(index - 1)"
            ></button>
          </div>
        </article>
      </div>

      <div v-else class="home-hero-visual" aria-hidden="true">
        <div class="home-orbit-card is-main">
          <span>Course</span>
          <strong>课堂学习</strong>
        </div>
        <div class="home-orbit-card is-practice">
          <span>Practice</span>
          <strong>题库反馈</strong>
        </div>
        <div class="home-orbit-card is-lab">
          <span>Lab</span>
          <strong>实验探索</strong>
        </div>
      </div>
    </section>

    <section
      ref="modulesRef"
      class="home-modules"
      :style="moduleShowcaseStyle"
      aria-labelledby="home-modules-title"
    >
      <div class="home-section-head">
        <p>{{ isLoggedIn ? 'Continue Learning' : 'Platform Modules' }}</p>
        <h2 id="home-modules-title">{{ isLoggedIn ? '回到你的四条学习路径' : '四个核心模块' }}</h2>
        <span>{{ isLoggedIn ? '选择一个入口继续推进今天的学习。' : '先了解平台能力，再选择适合自己的学习入口。' }}</span>
      </div>

      <div ref="moduleStageRef" class="home-module-stage">
        <div class="home-module-copy-sticky">
          <div class="home-module-copy-window">
            <div class="home-module-copy-track">
              <article v-for="module in modules" :key="module.key" :class="['home-module-card', `is-${module.tone}`]">
                <div class="home-module-copy">
                  <p>{{ module.eyebrow }}</p>
                  <h3>{{ module.title }}</h3>
                  <p>{{ module.copy }}</p>
                  <div class="home-module-tags">
                    <span v-for="tag in module.highlights" :key="tag">{{ tag }}</span>
                  </div>
                  <RouterLink :to="module.path">{{ module.action }}</RouterLink>
                </div>
              </article>
            </div>
          </div>
        </div>

        <div class="home-module-visual-sticky" aria-hidden="true">
          <div class="home-module-visual-window">
            <div class="home-module-visual-track">
              <div
                v-for="(module, index) in modules"
                :key="`visual-${module.key}`"
                :class="['home-module-illustration', `is-${module.tone}`, { 'has-image': module.image }]"
                :style="moduleVisualStyles[index]"
              >
                <img v-if="module.image" :src="module.image" :alt="`${module.title}未来感展示图`" />
                <div class="home-visual-sky"></div>
                <div class="home-visual-grid"></div>
                <div class="home-visual-panel">
                  <span>{{ module.visualTitle }}</span>
                  <strong>{{ module.visualMeta }}</strong>
                </div>
                <i></i>
                <b></b>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div class="home-module-scroll-spacer" aria-hidden="true">
        <span v-for="module in modules" :key="`spacer-${module.key}`"></span>
      </div>
    </section>
  </main>
</template>
