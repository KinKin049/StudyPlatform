<script setup>
// AI 学习宠物组件，提供待办管理、番茄钟专注和 AI 对话功能
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Hide, View } from '@element-plus/icons-vue'

import { chatWithAiPet } from '../api/aiPet'
import { getStoredAuthUser } from '../api/auth'
import { AI_PET_BY_KEY, DEFAULT_PET_KEY, PET_SELECTION_EVENT, PET_STORAGE_KEYS } from '../data/aiPetShop'
import { renderMessageMarkdown } from '../utils/markdown'
import idle01 from '../assets/pet/nebula-cat/idle-01.png'
import idle02 from '../assets/pet/nebula-cat/idle-02.png'
import idle03 from '../assets/pet/nebula-cat/idle-03.png'
import idle04 from '../assets/pet/nebula-cat/idle-04.png'
import talk01 from '../assets/pet/nebula-cat/talk-01.png'
import talk02 from '../assets/pet/nebula-cat/talk-02.png'
import talk03 from '../assets/pet/nebula-cat/talk-03.png'
import talk04 from '../assets/pet/nebula-cat/talk-04.png'
import thinking01 from '../assets/pet/nebula-cat/thinking-01.png'
import thinking02 from '../assets/pet/nebula-cat/thinking-02.png'
import thinking03 from '../assets/pet/nebula-cat/thinking-03.png'
import thinking04 from '../assets/pet/nebula-cat/thinking-04.png'
import happy01 from '../assets/pet/nebula-cat/happy-01.png'
import happy02 from '../assets/pet/nebula-cat/happy-02.png'
import happy03 from '../assets/pet/nebula-cat/happy-03.png'
import happy04 from '../assets/pet/nebula-cat/happy-04.png'
import focus01 from '../assets/pet/nebula-cat/focus-01.png'
import focus02 from '../assets/pet/nebula-cat/focus-02.png'
import focus03 from '../assets/pet/nebula-cat/focus-03.png'
import focus04 from '../assets/pet/nebula-cat/focus-04.png'
import sleep01 from '../assets/pet/nebula-cat/sleep-01.png'
import sleep02 from '../assets/pet/nebula-cat/sleep-02.png'
import sleep03 from '../assets/pet/nebula-cat/sleep-03.png'
import sleep04 from '../assets/pet/nebula-cat/sleep-04.png'

const route = useRoute()
const router = useRouter()

// 宠物表情帧动画配置，包含空闲、说话、思考、开心、专注、睡眠六种状态
const frames = {
  idle: [idle01, idle02, idle03, idle04],
  talk: [talk01, talk02, talk03, talk04],
  thinking: [thinking01, thinking02, thinking03, thinking04],
  happy: [happy01, happy02, happy03, happy04],
  focus: [focus01, focus02, focus03, focus04],
  sleep: [sleep01, sleep02, sleep03, sleep04],
}

// 本地存储键名配置
const todoStorageKeyPrefix = 'study-platform-ai-pet-todos'
const focusStorageKey = 'study-platform-ai-pet-focus-summary'
const positionStorageKey = 'study-platform-ai-pet-position'
const visibilityStorageKey = 'study-platform-ai-pet-hidden'
const quietModeStorageKey = 'study-platform-ai-pet-quiet-mode'
const petSize = 116
const viewportPadding = 14
const actionFeedbackReserve = 74
const pageContextTextLimit = 5200
const pageContextSelectionLimit = 800
const pageContextFormLimit = 14
const quietPetVisuals = {
  [DEFAULT_PET_KEY]: {
    scale: 1,
    blinkLeft: '40px',
    blinkTop: '51px',
    blinkGap: '30px',
    blinkWidth: '8px',
    blinkColor: '#241f46',
  },
  'pet-07-lucky-cat': {
    scale: 0.68,
    blinkLeft: '47px',
    blinkTop: '55px',
    blinkGap: '21px',
    blinkWidth: '6px',
    blinkColor: '#2a244b',
  },
  'pet-20-cream-dog': {
    scale: 0.68,
    blinkLeft: '49px',
    blinkTop: '56px',
    blinkGap: '19px',
    blinkWidth: '6px',
    blinkColor: '#2a244b',
  },
  'pet-27-crystal-bunny': {
    scale: 0.68,
    blinkLeft: '47px',
    blinkTop: '52px',
    blinkGap: '22px',
    blinkWidth: '6px',
    blinkColor: '#2a244b',
  },
  'pet-29-bamboo-panda': {
    scale: 0.68,
    blinkLeft: '42px',
    blinkTop: '52px',
    blinkGap: '34px',
    blinkWidth: '10px',
    blinkColor: '#20263f',
  },
  'pet-39-gray-dragon': {
    scale: 0.68,
    blinkLeft: '49px',
    blinkTop: '57px',
    blinkGap: '22px',
    blinkWidth: '7px',
    blinkColor: '#2a244b',
  },
  'pet-44-violet-dragon': {
    scale: 0.68,
    blinkLeft: '49px',
    blinkTop: '57px',
    blinkGap: '22px',
    blinkWidth: '7px',
    blinkColor: '#2a244b',
  },
  'pet-49-pink-dragon': {
    scale: 0.68,
    blinkLeft: '49px',
    blinkTop: '55px',
    blinkGap: '22px',
    blinkWidth: '7px',
    blinkColor: '#3a2b65',
  },
  'pet-75-screenbot': {
    scale: 0.68,
    blinkLeft: '47px',
    blinkTop: '53px',
    blinkGap: '20px',
    blinkWidth: '8px',
    blinkColor: '#255760',
  },
}
const pageContextRootSelectors = [
  'main',
  '[data-ai-page-content]',
  '.admin-page',
  '.auth-page',
  '.exchange-page',
  '.lab-page',
  '.profile-page',
  '.visualization-page',
  '.visual-home',
  '.game-platform',
  '.academy-main',
  '.academy-page',
  '#app',
]
const pageContextIgnoreSelector = [
  '.ai-pet-widget',
  '.site-header',
  '.academy-subnav',
  '.dropdown-menu',
  '.academy-dropdown-menu',
  '.el-overlay',
  '.el-popper',
  'script',
  'style',
  'noscript',
  'svg',
  'canvas',
  '[aria-hidden="true"]',
  '[hidden]',
].join(',')
const sensitiveFieldPattern = /password|pwd|token|secret|key|验证码|密码|密钥|身份证|手机|电话|邮箱|email|账号|用户名/i

// 导航目标配置，用于语义理解跳转
const navigationTargets = [
  { label: '在线学堂首页', path: '/academy/home', keywords: ['在线学堂', '学堂首页', '课程首页'] },
  { label: '我的课程', path: '/academy/my-courses', keywords: ['我的课程', '课程聚合页'] },
  { label: '课程作业', path: '/academy/assignments', keywords: ['课程作业', '作业页', '作业列表', '作业'] },
  { label: '考试中心', path: '/academy/exams', keywords: ['考试中心', '考试页', '考试列表', '考试'] },
  { label: '课程题库', path: '/academy/question-bank', keywords: ['课程题库', '题库首页', '题库'] },
  { label: '错题本', path: '/academy/question-bank/mistakes', keywords: ['错题本', '我的错题', '错题'] },
  { label: '收藏题目', path: '/academy/question-bank/favorites', keywords: ['收藏题目', '我的收藏', '收藏'] },
  { label: '个人主页', path: '/profile', keywords: ['个人主页', '个人中心', '主页资料'] },
  { label: '可视化学习', path: '/visualization', keywords: ['可视化', '可视化学习'] },
  { label: '游戏学习', path: '/games', keywords: ['游戏学习', '小游戏', '游戏'] },
]

// 面板状态
const open = ref(false)
const activeTab = ref('chat')
const frameIndex = ref(0)
const mood = ref('idle')
const chatInput = ref('')
const chatLoading = ref(false)
const newTodo = ref('')
const todos = ref([])
const focusMinutes = ref(25)
const breakMinutes = ref(5)
const focusPhase = ref('focus')
const focusRemaining = ref(25 * 60)
const focusRunning = ref(false)
const focusSummary = ref({ sessions: 0, minutes: 0 })
const widgetPosition = ref({ x: 0, y: 0 })
const viewportSize = ref({ width: 1280, height: 720 })
const chatMessagesRef = ref(null)
const positionReady = ref(false)
const dragging = ref(false)
const landing = ref(false)
const celebrating = ref(false)
const suppressClick = ref(false)
const userHidden = ref(false)
const quietMode = ref(false)
const selectedPetKey = ref(DEFAULT_PET_KEY)
const petBubble = ref({
  visible: false,
  text: '',
})
const actionFeedback = ref({
  visible: false,
  text: '',
})
const messages = ref([
  {
    role: 'pet',
    text: '你好，我是你的 AI 学习伙伴。现在可以帮你理解当前页面、跳转功能、创建待办和启动番茄专注。',
  },
])

// 定时器引用
let animationTimer = null
let focusTimer = null
let moodResetTimer = null
let landingTimer = null
let celebrationTimer = null
let suppressClickTimer = null
let petBubbleTimer = null
let actionFeedbackTimer = null
let contextRefreshTimer = null
let dragState = null

// 根据路由元信息判断是否隐藏宠物
const hidden = computed(() => route.meta?.hidePet === true)

// 计算当前活跃的宠物心情状态
const activeMood = computed(() => {
  if (celebrating.value) {
    return 'happy'
  }
  if (focusRunning.value && focusPhase.value === 'focus') {
    return 'focus'
  }
  if (focusRunning.value && focusPhase.value === 'break') {
    return 'sleep'
  }
  return mood.value
})

const selectedPet = computed(() => AI_PET_BY_KEY[selectedPetKey.value] || null)

const petDisplayName = computed(() => selectedPet.value?.shortName || '星云猫')
const petFullName = computed(() => selectedPet.value?.name || petDisplayName.value)

function getCurrentUserStorageId() {
  const user = getStoredAuthUser()
  const rawId = user?.id || user?.email || user?.username || 'guest'
  return String(rawId).replace(/[^\w.-]/g, '_')
}

const currentTodoStorageKey = () => `${todoStorageKeyPrefix}:${getCurrentUserStorageId()}`

const selectedPetAction = computed(() => {
  if (!selectedPet.value?.actions) {
    return ''
  }
  const actionMap = {
    idle: 'idle',
    talk: 'happy',
    thinking: 'study',
    happy: celebrating.value ? 'levelup' : 'happy',
    focus: 'study',
    sleep: 'sleep',
  }
  return actionMap[activeMood.value] || 'idle'
})

const quietIdle = computed(() => quietMode.value && activeMood.value === 'idle')

const quietVisual = computed(() => quietPetVisuals[selectedPetKey.value] || quietPetVisuals[DEFAULT_PET_KEY])

const avatarStyle = computed(() => ({
  '--ai-pet-quiet-scale': quietVisual.value.scale,
  '--ai-pet-blink-left': quietVisual.value.blinkLeft,
  '--ai-pet-blink-top': quietVisual.value.blinkTop,
  '--ai-pet-blink-gap': quietVisual.value.blinkGap,
  '--ai-pet-blink-width': quietVisual.value.blinkWidth,
  '--ai-pet-blink-color': quietVisual.value.blinkColor,
}))

// 获取当前显示的宠物图片帧
const petImage = computed(() => {
  if (quietIdle.value) {
    return selectedPet.value?.image || frames.idle[0]
  }
  if (selectedPet.value?.actions) {
    return selectedPet.value.actions[selectedPetAction.value] || selectedPet.value.actions.idle || selectedPet.value.image
  }
  const currentFrames = frames[activeMood.value] || frames.idle
  return currentFrames[frameIndex.value % currentFrames.length]
})

// 已完成待办数量
const completedTodoCount = computed(() => todos.value.filter((todo) => todo.done).length)

// 宠物挂件样式
const widgetStyle = computed(() => ({
  left: `${displayPosition.value.x}px`,
  top: `${displayPosition.value.y}px`,
}))

// 宠物挂件 CSS 类名
const widgetClasses = computed(() => ({
  'is-open': open.value,
  'is-ready': positionReady.value,
  'is-dragging': dragging.value,
  'is-landing': landing.value,
  'is-celebrating': celebrating.value,
  'is-user-hidden': userHidden.value,
  'is-hidden-left': userHidden.value && hiddenToLeft.value,
  'is-quiet': quietMode.value,
  'align-left': widgetPosition.value.x < 300,
  [`mood-${activeMood.value}`]: true,
}))

const hiddenToLeft = computed(() => widgetPosition.value.x < viewportSize.value.width / 2)

// 宠物挂件显示位置（隐藏状态时有特殊处理）
const displayPosition = computed(() => {
  if (!userHidden.value) {
    return widgetPosition.value
  }
  return {
    x: hiddenToLeft.value
      ? 22 - petSize
      : Math.max(viewportSize.value.width - 22, viewportPadding),
    y: Math.min(
      Math.max(widgetPosition.value.y + 18, viewportPadding),
      Math.max(viewportPadding, viewportSize.value.height - 86),
    ),
  }
})

// 番茄钟进度百分比
const focusProgress = computed(() => {
  const total = getPhaseMinutes() * 60
  const elapsed = Math.min(total, Math.max(0, total - focusRemaining.value))
  return Math.round((elapsed / total) * 100)
})

// 番茄钟时间显示文本
const focusTimeText = computed(() => {
  const minutes = Math.floor(focusRemaining.value / 60)
  const seconds = focusRemaining.value % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

// 番茄钟阶段标题
const focusPhaseTitle = computed(() => (focusPhase.value === 'break' ? '休息倒计时' : '专注倒计时'))

// 番茄钟阶段提示信息
const focusPhaseTip = computed(() => (
  focusPhase.value === 'break'
    ? `休息 ${getPhaseMinutes('break')} 分钟后自动进入下一轮`
    : `每 ${getPhaseMinutes('focus')} 分钟完成一轮番茄钟`
))

// 番茄钟主按钮文本
const focusPrimaryButtonText = computed(() => {
  if (!focusRunning.value) {
    return '开始'
  }
  return focusPhase.value === 'break' ? '休息中' : '专注中'
})

// 当前页面上下文信息，用于 AI 对话
const pageContext = ref(createEmptyPageContext())

// 页面上下文摘要
const contextSummary = computed(() => {
  const headingText = pageContext.value.headings.length ? pageContext.value.headings.join(' / ') : '当前页面暂无标题摘要'
  const textCount = pageContext.value.contentLength || 0
  return `${pageContext.value.path} · ${headingText} · 已读取约 ${textCount} 字`
})

// 创建空页面上下文，避免组件挂载前访问 DOM
function createEmptyPageContext() {
  return {
    path: route.fullPath || route.path,
    routeName: route.name ? String(route.name) : '',
    title: 'StudyPlatform',
    headings: [],
    selectedText: '',
    formSnapshot: [],
    contentLength: 0,
    textSnippet: '',
  }
}

// 主动刷新页面上下文，确保 AI 对话拿到最新页面内容
async function refreshPageContext() {
  await nextTick()
  pageContext.value = capturePageContext()
  return pageContext.value
}

// 延迟刷新页面上下文，用于等待路由页面完成渲染
function schedulePageContextRefresh(delay = 160) {
  if (typeof window === 'undefined') {
    return
  }
  if (contextRefreshTimer) {
    window.clearTimeout(contextRefreshTimer)
  }
  contextRefreshTimer = window.setTimeout(() => {
    contextRefreshTimer = null
    refreshPageContext()
  }, delay)
}

// 采集当前页面可见内容、标题、选中文本和安全的表单摘要
function capturePageContext() {
  if (typeof document === 'undefined') {
    return createEmptyPageContext()
  }
  const root = findPageContextRoot()
  const visibleText = collectVisibleText(root, pageContextTextLimit)
  const selectedText = normalizeContextText(window.getSelection?.().toString() || '', pageContextSelectionLimit)
  const formSnapshot = collectFormSnapshot(root)
  const textSections = [
    visibleText ? `页面可见内容：${visibleText}` : '',
    selectedText ? `用户当前选中文本：${selectedText}` : '',
    formSnapshot.length ? `页面表单/输入状态：${formSnapshot.join('；')}` : '',
  ].filter(Boolean)

  return {
    path: route.fullPath || route.path,
    routeName: route.name ? String(route.name) : '',
    title: document.title || 'StudyPlatform',
    headings: collectHeadings(root),
    selectedText,
    formSnapshot,
    contentLength: visibleText.length,
    textSnippet: textSections.join('\n'),
  }
}

// 寻找页面主体容器，优先使用 main，避免读入宠物自身内容
function findPageContextRoot() {
  const candidates = pageContextRootSelectors
    .flatMap((selector) => Array.from(document.querySelectorAll(selector)))
    .filter((element) => element instanceof HTMLElement)
    .filter((element) => !element.closest('.ai-pet-widget'))
    .filter(isVisibleElement)

  return candidates[0] || document.body
}

// 采集页面标题层级
function collectHeadings(root) {
  return Array.from(root.querySelectorAll('h1, h2, h3, [data-ai-heading]'))
    .filter((element) => !shouldIgnoreContextElement(element))
    .filter(isVisibleElement)
    .map((element) => normalizeContextText(element.textContent || '', 120))
    .filter(Boolean)
    .filter(uniqueText)
    .slice(0, 8)
}

// 采集可见文本节点，排除导航、宠物、脚本和不可见元素
function collectVisibleText(root, maxLength) {
  const fragments = []
  const walker = document.createTreeWalker(root, window.NodeFilter.SHOW_TEXT, {
    acceptNode(node) {
      const parent = node.parentElement
      if (!parent || shouldIgnoreContextElement(parent) || !isVisibleElement(parent)) {
        return window.NodeFilter.FILTER_REJECT
      }
      const text = normalizeContextText(node.nodeValue || '', 220)
      if (!text || text.length < 2) {
        return window.NodeFilter.FILTER_REJECT
      }
      return window.NodeFilter.FILTER_ACCEPT
    },
  })

  while (walker.nextNode()) {
    const text = normalizeContextText(walker.currentNode.nodeValue || '', 220)
    if (text && !fragments.includes(text)) {
      fragments.push(text)
    }
    if (fragments.join(' ').length >= maxLength) {
      break
    }
  }

  return normalizeContextText(fragments.join(' '), maxLength)
}

// 采集非敏感表单状态，帮助 AI 理解筛选、搜索和当前输入
function collectFormSnapshot(root) {
  return Array.from(root.querySelectorAll('input, textarea, select'))
    .filter((element) => !shouldIgnoreContextElement(element))
    .filter(isVisibleElement)
    .map(describeFormField)
    .filter(Boolean)
    .filter(uniqueText)
    .slice(0, pageContextFormLimit)
}

// 描述单个表单字段，敏感字段只提示已填写，不上传明文
function describeFormField(element) {
  const label = getFieldLabel(element)
  const type = (element.getAttribute('type') || element.tagName || '').toLowerCase()
  if (type === 'hidden' || type === 'file' || type === 'password') {
    return ''
  }
  const isSensitive = sensitiveFieldPattern.test(`${label} ${element.name || ''} ${element.id || ''} ${element.placeholder || ''}`)
  if (element instanceof HTMLSelectElement) {
    const selectedText = Array.from(element.selectedOptions).map((option) => option.textContent?.trim()).filter(Boolean).join('/')
    return `${label || '选择框'}：${normalizeContextText(selectedText || '未选择', 80)}`
  }
  if (element instanceof HTMLInputElement && ['checkbox', 'radio'].includes(type)) {
    return `${label || element.value || type}：${element.checked ? '已选中' : '未选中'}`
  }
  const rawValue = element.value || ''
  if (isSensitive && rawValue) {
    return `${label || '输入框'}：已填写`
  }
  const value = normalizeContextText(rawValue, 120)
  return value ? `${label || '输入框'}：${value}` : `${label || element.placeholder || '输入框'}：未填写`
}

// 获取表单字段可读标签
function getFieldLabel(element) {
  const explicitLabel = element.id && window.CSS?.escape
    ? document.querySelector(`label[for="${window.CSS.escape(element.id)}"]`)
    : null
  const implicitLabel = element.closest('label')
  return normalizeContextText(
    element.getAttribute('aria-label') ||
      explicitLabel?.textContent ||
      implicitLabel?.textContent ||
      element.placeholder ||
      element.name ||
      '',
    80,
  )
}

// 判断元素是否应从页面上下文中忽略
function shouldIgnoreContextElement(element) {
  return Boolean(element.closest(pageContextIgnoreSelector))
}

// 判断元素是否可见
function isVisibleElement(element) {
  if (!(element instanceof HTMLElement)) {
    return false
  }
  const style = window.getComputedStyle(element)
  if (style.display === 'none' || style.visibility === 'hidden') {
    return false
  }
  return element.getClientRects().length > 0 || element === document.body
}

// 文本归一化和截断
function normalizeContextText(value, maxLength = 500) {
  const text = String(value || '').replace(/\s+/g, ' ').trim()
  if (text.length <= maxLength) {
    return text
  }
  return `${text.slice(0, maxLength)}…`
}

// 数组去重过滤器
function uniqueText(value, index, array) {
  return array.indexOf(value) === index
}

// 设置宠物心情状态，duration 毫秒后自动恢复为 idle
function setMood(nextMood, duration = 1800) {
  mood.value = nextMood
  if (moodResetTimer) {
    window.clearTimeout(moodResetTimer)
  }
  moodResetTimer = window.setTimeout(() => {
    mood.value = 'idle'
  }, duration)
}

// 清除宠物气泡消息
function clearPetBubble() {
  petBubble.value = {
    visible: false,
    text: '',
  }
  if (petBubbleTimer) {
    window.clearTimeout(petBubbleTimer)
    petBubbleTimer = null
  }
}

// 显示宠物气泡消息
function showPetBubble(text) {
  if (open.value || userHidden.value || hidden.value || dragging.value) {
    return
  }
  petBubble.value = {
    visible: true,
    text,
  }
  if (petBubbleTimer) {
    window.clearTimeout(petBubbleTimer)
  }
  petBubbleTimer = window.setTimeout(() => {
    clearPetBubble()
  }, 7200)
}

// 添加宠物消息并显示气泡
function pushPetMessage(text) {
  messages.value.push({
    role: 'pet',
    text,
  })
  showPetBubble(text)
}

async function scrollChatMessagesToBottom() {
  if (!open.value || activeTab.value !== 'chat') {
    return
  }
  await nextTick()
  const messagesElement = chatMessagesRef.value
  if (!messagesElement) {
    return
  }
  window.requestAnimationFrame(() => {
    messagesElement.scrollTop = messagesElement.scrollHeight
  })
}

// 显示操作反馈提示
function showActionFeedback(text) {
  widgetPosition.value = clampPosition(widgetPosition.value, true)
  actionFeedback.value = {
    visible: true,
    text,
  }
  if (actionFeedbackTimer) {
    window.clearTimeout(actionFeedbackTimer)
  }
  actionFeedbackTimer = window.setTimeout(() => {
    actionFeedback.value.visible = false
    actionFeedbackTimer = null
  }, 1800)
}

// 规范化命令文本，去除空格和标点
function normalizeCommand(text) {
  return text
    .replace(/\s+/g, '')
    .replace(/[，。！？、,.!?]/g, '')
    .toLowerCase()
}

// 清理待办标题文本，去除多余前缀和后缀
function cleanActionTitle(text) {
  return text
    .replace(/^(请|麻烦|帮我|给我|帮忙|可以)?/g, '')
    .replace(/(创建|添加|新增|记录|安排|加入|加一个|加一条|建一个|建一条)/g, '')
    .replace(/(一个|一条|一项|个|条|项)/g, '')
    .replace(/(待办事项|待办|任务|todo|TODO)/gi, '')
    .replace(/[：:，,。！!？?]/g, ' ')
    .trim()
}

// 解析待办标题，支持冒号格式和引号格式
function resolveTodoTitle(text) {
  const colonTitle = text.match(/(?:待办|任务|todo|TODO)[：:]\s*(.+)$/)
  if (colonTitle?.[1]?.trim()) {
    return colonTitle[1].trim()
  }
  const quotedTitle = text.match(/[“"']([^“”"']+)[”"']/)
  if (quotedTitle?.[1]?.trim()) {
    return quotedTitle[1].trim()
  }
  return cleanActionTitle(text)
}

// 添加待办事项
function addTodoWithTitle(title) {
  todos.value.unshift({
    id: Date.now(),
    title,
    done: false,
    createdAt: new Date().toISOString(),
  })
  saveTodos()
  activeTab.value = 'todo'
  setMood('happy')
}

// 解析课程搜索关键词
function resolveCourseKeyword(text) {
  return text
    .replace(/(请|麻烦|帮我|给我|帮忙|可以|想要|我要|我想)/g, '')
    .replace(/(找一门|找门|找|搜索|查找|看看|打开|进入)/g, '')
    .replace(/(课程|课|相关|有关|关于|的)/g, ' ')
    .replace(/[：:，,。！!？?]/g, ' ')
    .trim()
}

// 根据关键词查找导航目标
function findNavigationTarget(text) {
  return navigationTargets.find((target) => target.keywords.some((keyword) => text.includes(keyword)))
}

// 处理本地工具操作，包括创建待办、启动番茄钟、搜索课程和页面导航
async function handleLocalToolAction(text) {
  const normalizedText = normalizeCommand(text)
  const actionReplies = []

  // 处理创建待办命令
  const wantsTodo = /(创建|添加|新增|记录|安排|加入|加一个|加一条|建一个|建一条).*(待办|任务|todo)/i.test(text)
    || /(待办|任务|todo).*(创建|添加|新增|记录|安排|加入)/i.test(text)
  if (wantsTodo) {
    const title = resolveTodoTitle(text)
    if (!title) {
      pushPetMessage('可以呀，把待办内容也告诉我，例如：创建待办：复习数据结构。')
      return true
    }
    addTodoWithTitle(title)
    actionReplies.push(`已创建待办「${title}」`)
  }

  // 处理启动番茄钟命令
  const wantsPomodoro = /(启动|开始|开启|进入).*(番茄|专注)/.test(normalizedText)
    || /(番茄|专注).*(启动|开始|开启)/.test(normalizedText)
  if (wantsPomodoro) {
    startFocus()
    open.value = true
    activeTab.value = 'focus'
    actionReplies.push('已启动番茄专注')
  }

  // 处理课程搜索命令
  const wantsCourseSearch = /(找|搜索|查找).*(课程|课)/.test(normalizedText)
    || /(课程|课).*(找|搜索|查找)/.test(normalizedText)
  if (wantsCourseSearch) {
    const keyword = resolveCourseKeyword(text)
    if (!keyword) {
      pushPetMessage('可以呀，你想找哪类课程？例如：帮我找 Python 课程。')
      return true
    }
    await router.push({
      path: '/academy/open-courses',
      query: { keyword },
    })
    open.value = false
    actionReplies.push(`已为你搜索「${keyword}」课程`)
  }

  // 处理页面导航命令
  const wantsNavigation = /(打开|跳转|进入|去|查看|带我去)/.test(normalizedText)
  const navigationTarget = wantsNavigation ? findNavigationTarget(text) : null
  if (navigationTarget) {
    await router.push(navigationTarget.path)
    open.value = false
    actionReplies.push(`已打开${navigationTarget.label}`)
  }

  // 无匹配操作时返回 false，交给 AI 处理
  if (!actionReplies.length) {
    return false
  }

  // 显示操作反馈
  const feedbackText = actionReplies.join('，')
  showActionFeedback(feedbackText)
  pushPetMessage(`${feedbackText}。`)
  setMood('happy', 1800)
  return true
}

// 构建聊天历史，用于 AI 对话上下文
function buildChatHistory() {
  return messages.value
    .slice(0, -1)
    .slice(-8)
    .map((message) => ({
      role: message.role === 'user' ? 'user' : 'assistant',
      text: message.text,
    }))
}

// 从本地存储加载待办列表
function loadTodos() {
  try {
    const savedTodos = JSON.parse(window.localStorage.getItem(currentTodoStorageKey()) || '[]')
    todos.value = Array.isArray(savedTodos) ? savedTodos : []
  } catch {
    todos.value = []
  }
}

// 保存待办列表到本地存储
function saveTodos() {
  window.localStorage.setItem(currentTodoStorageKey(), JSON.stringify(todos.value))
}

// 从本地存储加载专注统计数据
function loadFocusSummary() {
  try {
    const savedSummary = JSON.parse(window.localStorage.getItem(focusStorageKey) || '{}')
    focusSummary.value = {
      sessions: Number(savedSummary.sessions) || 0,
      minutes: Number(savedSummary.minutes) || 0,
    }
  } catch {
    focusSummary.value = { sessions: 0, minutes: 0 }
  }
}

// 保存专注统计数据到本地存储
function saveFocusSummary() {
  window.localStorage.setItem(focusStorageKey, JSON.stringify(focusSummary.value))
}

// 从本地存储加载宠物隐藏状态
function loadPetVisibility() {
  userHidden.value = window.localStorage.getItem(visibilityStorageKey) === '1'
}

// 保存宠物隐藏状态到本地存储
function savePetVisibility() {
  window.localStorage.setItem(visibilityStorageKey, userHidden.value ? '1' : '0')
}

function loadQuietMode() {
  quietMode.value = window.localStorage.getItem(quietModeStorageKey) === '1'
}

function saveQuietMode() {
  window.localStorage.setItem(quietModeStorageKey, quietMode.value ? '1' : '0')
}

function toggleQuietMode() {
  quietMode.value = !quietMode.value
  saveQuietMode()
  setMood('idle')
  showActionFeedback(quietMode.value ? '已进入安静模式' : '已恢复活跃模式')
}

function loadSelectedPet() {
  const nextKey = window.localStorage.getItem(PET_STORAGE_KEYS.active) || DEFAULT_PET_KEY
  selectedPetKey.value = AI_PET_BY_KEY[nextKey] ? nextKey : DEFAULT_PET_KEY
}

function handlePetSelectionChanged() {
  const previousName = petDisplayName.value
  loadSelectedPet()
  if (petDisplayName.value !== previousName) {
    setMood('happy', 2200)
    showActionFeedback(`已切换为 ${petDisplayName.value}`)
    showPetBubble(`${petDisplayName.value} 来陪你学习啦！`)
  }
}

function handlePetStorageChanged(event) {
  if (event.key === PET_STORAGE_KEYS.active) {
    handlePetSelectionChanged()
  }
}

function handleAuthUpdated() {
  loadTodos()
}

// 更新视口大小
function updateViewportSize() {
  viewportSize.value = {
    width: window.innerWidth || 1280,
    height: window.innerHeight || 720,
  }
}

// 限制宠物位置在视口范围内
function clampPosition(position, reserveActionFeedback = false) {
  const bottomReserve = reserveActionFeedback ? actionFeedbackReserve : 0
  const maxX = Math.max(viewportPadding, viewportSize.value.width - petSize - viewportPadding)
  const maxY = Math.max(viewportPadding, viewportSize.value.height - petSize - viewportPadding - bottomReserve)
  return {
    x: Math.min(Math.max(position.x, viewportPadding), maxX),
    y: Math.min(Math.max(position.y, viewportPadding), maxY),
  }
}

// 从本地存储加载宠物位置
function loadPosition() {
  updateViewportSize()
  try {
    const savedPosition = JSON.parse(window.localStorage.getItem(positionStorageKey) || '{}')
    if (Number.isFinite(savedPosition.x) && Number.isFinite(savedPosition.y)) {
      widgetPosition.value = clampPosition(savedPosition)
      positionReady.value = true
      return
    }
  } catch {
    // use default position
  }
  widgetPosition.value = clampPosition({
    x: viewportSize.value.width - petSize - 28,
    y: viewportSize.value.height - petSize - 28,
  })
  positionReady.value = true
}

// 保存宠物位置到本地存储
function savePosition() {
  window.localStorage.setItem(positionStorageKey, JSON.stringify(widgetPosition.value))
}

// 处理窗口大小变化
function handleResize() {
  updateViewportSize()
  widgetPosition.value = clampPosition(widgetPosition.value)
  savePosition()
}

// 切换面板展开/收起状态
function togglePanel() {
  if (userHidden.value) {
    return
  }
  open.value = !open.value
  if (open.value) {
    clearPetBubble()
    scrollChatMessagesToBottom()
  }
  setMood(open.value ? 'happy' : 'idle', 1200)
}

// 处理宠物头像点击事件
function handleAvatarClick() {
  if (suppressClick.value || dragging.value) {
    return
  }
  togglePanel()
}

// 打开宠物气泡并切换到聊天面板
function openPetBubble() {
  clearPetBubble()
  open.value = true
  activeTab.value = 'chat'
  scrollChatMessagesToBottom()
  setMood('happy', 1200)
}

// 开始拖动宠物挂件
function beginDrag(event) {
  if (userHidden.value) {
    return
  }
  if (event.button !== undefined && event.button !== 0) {
    return
  }
  dragState = {
    pointerId: event.pointerId,
    startX: event.clientX,
    startY: event.clientY,
    offsetX: event.clientX - widgetPosition.value.x,
    offsetY: event.clientY - widgetPosition.value.y,
    activated: false,
  }
  event.currentTarget.setPointerCapture?.(event.pointerId)
  window.addEventListener('pointermove', handleDragMove)
  window.addEventListener('pointerup', endDrag)
  window.addEventListener('pointercancel', cancelDrag)
}

// 处理拖动移动事件
function handleDragMove(event) {
  if (!dragState || event.pointerId !== dragState.pointerId) {
    return
  }
  const distance = Math.hypot(event.clientX - dragState.startX, event.clientY - dragState.startY)
  if (!dragState.activated && distance < 6) {
    return
  }
  if (!dragState.activated) {
    dragState.activated = true
    dragging.value = true
    landing.value = false
    open.value = false
    clearPetBubble()
  }
  event.preventDefault()
  widgetPosition.value = clampPosition({
    x: event.clientX - dragState.offsetX,
    y: event.clientY - dragState.offsetY,
  })
}

// 清除拖动事件监听
function clearDragListeners() {
  window.removeEventListener('pointermove', handleDragMove)
  window.removeEventListener('pointerup', endDrag)
  window.removeEventListener('pointercancel', cancelDrag)
}

// 结束拖动操作
function endDrag(event) {
  if (!dragState || event.pointerId !== dragState.pointerId) {
    clearDragListeners()
    dragState = null
    return
  }
  const wasDragging = dragState.activated
  clearDragListeners()
  dragState = null
  if (!wasDragging) {
    return
  }
  dragging.value = false
  suppressClick.value = true
  if (suppressClickTimer) {
    window.clearTimeout(suppressClickTimer)
  }
  suppressClickTimer = window.setTimeout(() => {
    suppressClick.value = false
  }, 180)
  savePosition()
  triggerLanding()
}

// 取消拖动操作
function cancelDrag() {
  clearDragListeners()
  dragState = null
  dragging.value = false
}

// 触发宠物着陆动画效果
function triggerLanding() {
  landing.value = true
  setMood('happy', 1200)
  if (landingTimer) {
    window.clearTimeout(landingTimer)
  }
  landingTimer = window.setTimeout(() => {
    landing.value = false
  }, 720)
}

// 隐藏宠物挂件
function hidePet() {
  open.value = false
  dragging.value = false
  landing.value = false
  clearPetBubble()
  userHidden.value = true
  savePetVisibility()
}

// 唤醒宠物挂件
function wakePet() {
  userHidden.value = false
  savePetVisibility()
  setMood('happy', 1400)
}

// 添加待办事项（从输入框）
function addTodo() {
  const title = newTodo.value.trim()
  if (!title) {
    return
  }
  addTodoWithTitle(title)
  newTodo.value = ''
}

// 切换待办事项完成状态
function toggleTodo(todo) {
  todo.done = !todo.done
  saveTodos()
  setMood(todo.done ? 'happy' : 'idle')
}

// 删除待办事项
function removeTodo(todoId) {
  todos.value = todos.value.filter((todo) => todo.id !== todoId)
  saveTodos()
}

// 获取当前阶段的分钟数（专注或休息）
function getPhaseMinutes(phase = focusPhase.value) {
  const fallbackMinutes = phase === 'break' ? 5 : 25
  const rawMinutes = phase === 'break' ? breakMinutes.value : focusMinutes.value
  return Math.max(1, Number(rawMinutes) || fallbackMinutes)
}

// 重置专注倒计时时长
function resetFocusDuration(phase = focusPhase.value) {
  focusRemaining.value = getPhaseMinutes(phase) * 60
}

// 切换专注阶段（专注/休息）
function switchFocusPhase(nextPhase) {
  focusPhase.value = nextPhase
  resetFocusDuration(nextPhase)
}

// 开始番茄钟专注
function startFocus() {
  if (focusRemaining.value <= 0) {
    resetFocusDuration()
  }
  focusRunning.value = true
  activeTab.value = 'focus'
}

// 暂停番茄钟
function pauseFocus() {
  focusRunning.value = false
  setMood('idle')
}

// 播放专注完成提示音效
function playFocusCompleteSound() {
  try {
    const AudioContextConstructor = window.AudioContext || window.webkitAudioContext
    if (!AudioContextConstructor) {
      return
    }
    const audioContext = new AudioContextConstructor()
    const startAt = audioContext.currentTime + 0.02
    const notes = [523.25, 659.25, 783.99, 1046.5]
    notes.forEach((frequency, index) => {
      const oscillator = audioContext.createOscillator()
      const gainNode = audioContext.createGain()
      const noteStart = startAt + index * 0.11
      oscillator.type = 'triangle'
      oscillator.frequency.setValueAtTime(frequency, noteStart)
      gainNode.gain.setValueAtTime(0.0001, noteStart)
      gainNode.gain.exponentialRampToValueAtTime(0.14, noteStart + 0.025)
      gainNode.gain.exponentialRampToValueAtTime(0.0001, noteStart + 0.22)
      oscillator.connect(gainNode)
      gainNode.connect(audioContext.destination)
      oscillator.start(noteStart)
      oscillator.stop(noteStart + 0.24)
    })
    window.setTimeout(() => {
      audioContext.close?.()
    }, 900)
  } catch {}
}

// 触发专注完成庆祝动画
function triggerFocusCelebration() {
  celebrating.value = true
  setMood('happy', 4200)
  if (celebrationTimer) {
    window.clearTimeout(celebrationTimer)
  }
  celebrationTimer = window.setTimeout(() => {
    celebrating.value = false
  }, 2200)
}

// 完成一轮专注
function finishFocusSegment() {
  const completedMinutes = getPhaseMinutes('focus')
  focusSummary.value = {
    sessions: focusSummary.value.sessions + 1,
    minutes: focusSummary.value.minutes + completedMinutes,
  }
  saveFocusSummary()
  pushPetMessage(`本轮 ${completedMinutes} 分钟专注完成啦！${petDisplayName.value} 正在放烟花，先休息 ${getPhaseMinutes('break')} 分钟。`)
  playFocusCompleteSound()
  triggerFocusCelebration()
  switchFocusPhase('break')
}

// 完成休息阶段
function finishBreak() {
  pushPetMessage(`休息结束啦，下一轮番茄钟自动开始。${petDisplayName.value} 继续陪你冲！`)
  switchFocusPhase('focus')
}

// 重置番茄钟
function resetFocus() {
  focusRunning.value = false
  switchFocusPhase('focus')
  setMood('idle')
}

// 发送消息给 AI 宠物
async function sendMessage() {
  const text = chatInput.value.trim()
  if (!text || chatLoading.value) {
    return
  }
  messages.value.push({ role: 'user', text })
  chatInput.value = ''
  // 先尝试本地工具操作
  if (await handleLocalToolAction(text)) {
    return
  }
  // 本地无匹配则调用 AI
  chatLoading.value = true
  setMood('thinking', 8000)
  try {
    const currentPageContext = await refreshPageContext()
    const response = await chatWithAiPet({
      message: text,
      petName: petFullName.value,
      petShortName: petDisplayName.value,
      petKey: selectedPetKey.value,
      pageContext: currentPageContext,
      history: buildChatHistory(),
    })
    pushPetMessage(response.reply || '喵，我刚刚有点走神，没有拿到有效回复。')
    setMood('talk', 2200)
  } catch (error) {
    pushPetMessage(error.message || '喵，AI 中转站现在没有连上，请稍后再试。')
    setMood('thinking', 1600)
  } finally {
    chatLoading.value = false
  }
}

// 番茄钟倒计时 tick 函数
function tickFocus() {
  if (!focusRunning.value) {
    return
  }
  focusRemaining.value = Math.max(0, focusRemaining.value - 1)
  if (focusRemaining.value <= 0) {
    if (focusPhase.value === 'focus') {
      finishFocusSegment()
      return
    }
    finishBreak()
  }
}

// 监听专注时长变化
watch(focusMinutes, () => {
  if (!focusRunning.value && focusPhase.value === 'focus') {
    resetFocusDuration('focus')
  }
})

// 监听休息时长变化
watch(breakMinutes, () => {
  if (!focusRunning.value && focusPhase.value === 'break') {
    resetFocusDuration('break')
  }
})

// 监听路由隐藏宠物状态变化
watch(hidden, (isHidden) => {
  if (isHidden) {
    open.value = false
    clearPetBubble()
  }
})

// 路由切换后刷新页面上下文，等待新页面完成渲染
watch(
  () => route.fullPath,
  () => {
    schedulePageContextRefresh()
  },
  { flush: 'post' },
)

watch(
  () => [messages.value.length, open.value, activeTab.value],
  () => {
    scrollChatMessagesToBottom()
  },
  { flush: 'post' },
)

onMounted(() => {
  // 加载本地存储数据
  loadTodos()
  loadFocusSummary()
  loadPetVisibility()
  loadQuietMode()
  loadSelectedPet()
  loadPosition()
  // 启动帧动画定时器
  animationTimer = window.setInterval(() => {
    frameIndex.value += 1
  }, 250)
  // 启动番茄钟 tick 定时器
  focusTimer = window.setInterval(tickFocus, 1000)
  // 监听窗口大小变化
  window.addEventListener('resize', handleResize)
  window.addEventListener(PET_SELECTION_EVENT, handlePetSelectionChanged)
  window.addEventListener('storage', handlePetStorageChanged)
  window.addEventListener('study-platform:auth-updated', handleAuthUpdated)
  // 首次挂载后读取当前页面内容
  schedulePageContextRefresh(80)
})

onBeforeUnmount(() => {
  // 清理所有定时器
  if (animationTimer) {
    window.clearInterval(animationTimer)
  }
  if (focusTimer) {
    window.clearInterval(focusTimer)
  }
  if (moodResetTimer) {
    window.clearTimeout(moodResetTimer)
  }
  if (landingTimer) {
    window.clearTimeout(landingTimer)
  }
  if (celebrationTimer) {
    window.clearTimeout(celebrationTimer)
  }
  if (suppressClickTimer) {
    window.clearTimeout(suppressClickTimer)
  }
  if (petBubbleTimer) {
    window.clearTimeout(petBubbleTimer)
  }
  if (actionFeedbackTimer) {
    window.clearTimeout(actionFeedbackTimer)
  }
  if (contextRefreshTimer) {
    window.clearTimeout(contextRefreshTimer)
  }
  // 清理拖动事件监听
  clearDragListeners()
  window.removeEventListener('resize', handleResize)
  window.removeEventListener(PET_SELECTION_EVENT, handlePetSelectionChanged)
  window.removeEventListener('storage', handlePetStorageChanged)
  window.removeEventListener('study-platform:auth-updated', handleAuthUpdated)
})
</script>

<template>
  <!-- AI 学习宠物挂件主容器 -->
  <aside
    v-if="!hidden"
    class="ai-pet-widget"
    :class="widgetClasses"
    :style="widgetStyle"
    aria-label="AI 学习宠物"
  >
    <!-- 操作反馈提示 -->
    <Transition name="ai-pet-action-feedback">
      <div v-if="actionFeedback.visible" class="ai-pet-action-feedback" role="status">
        {{ actionFeedback.text }}
      </div>
    </Transition>

    <!-- 宠物气泡消息 -->
    <button
      v-if="petBubble.visible && !open && !userHidden && !dragging"
      type="button"
      class="ai-pet-speech-bubble"
      @click="openPetBubble"
    >
      <span>喵～</span>
      <strong>{{ petBubble.text }}</strong>
    </button>

    <!-- 功能面板 -->
    <section v-if="open" class="ai-pet-panel">
      <!-- 面板头部 -->
      <div class="ai-pet-panel__header">
        <div>
          <div class="ai-pet-panel__title-row">
            <p>{{ petDisplayName }}</p>
            <button
              type="button"
              class="ai-pet-panel__hide"
              title="隐藏宠物"
              data-tooltip="隐藏宠物"
              @click="hidePet"
            >
              <el-icon><Hide /></el-icon>
            </button>
            <button
              type="button"
              class="ai-pet-panel__quiet"
              :class="{ active: quietMode }"
              :title="quietMode ? '恢复活跃待机' : '安静待机'"
              @click="toggleQuietMode"
            >
              安静
            </button>
          </div>
          <span>待办 · 专注 · AI 助手</span>
        </div>
        <button type="button" class="ai-pet-panel__close" @click="togglePanel">×</button>
      </div>

      <!-- 功能标签页 -->
      <div class="ai-pet-tabs">
        <button
          type="button"
          :class="{ active: activeTab === 'chat' }"
          @click="activeTab = 'chat'"
        >
          对话
        </button>
        <button
          type="button"
          :class="{ active: activeTab === 'todo' }"
          @click="activeTab = 'todo'"
        >
          待办
        </button>
        <button
          type="button"
          :class="{ active: activeTab === 'focus' }"
          @click="activeTab = 'focus'"
        >
          番茄钟
        </button>
      </div>

      <!-- 对话面板 -->
      <div v-if="activeTab === 'chat'" class="ai-pet-content ai-pet-chat">
        <div class="ai-pet-context">
          <strong>页面摘要</strong>
          <span>{{ contextSummary }}</span>
        </div>
        <div ref="chatMessagesRef" class="ai-pet-messages">
          <article
            v-for="(message, index) in messages"
            :key="`${message.role}-${index}`"
            :class="['ai-pet-message', `is-${message.role}`]"
            v-html="renderMessageMarkdown(message.text)"
          ></article>
        </div>
        <form class="ai-pet-input-row" @submit.prevent="sendMessage">
          <input v-model="chatInput" type="text" placeholder="问问 AI 学习伙伴当前页面的问题..." />
          <button type="submit" :disabled="chatLoading">{{ chatLoading ? '思考中' : '发送' }}</button>
        </form>
      </div>

      <!-- 待办面板 -->
      <div v-else-if="activeTab === 'todo'" class="ai-pet-content">
        <div class="ai-pet-stat">
          <span>今日待办</span>
          <strong>{{ completedTodoCount }}/{{ todos.length }}</strong>
        </div>
        <form class="ai-pet-input-row" @submit.prevent="addTodo">
          <input v-model="newTodo" type="text" placeholder="添加一个学习任务..." />
          <button type="submit">添加</button>
        </form>
        <div class="ai-pet-todos">
          <article v-for="todo in todos" :key="todo.id" class="ai-pet-todo" :class="{ done: todo.done }">
            <button type="button" class="ai-pet-check" @click="toggleTodo(todo)">
              {{ todo.done ? '✓' : '' }}
            </button>
            <span>{{ todo.title }}</span>
            <button type="button" class="ai-pet-delete" @click="removeTodo(todo.id)">删除</button>
          </article>
          <p v-if="!todos.length" class="ai-pet-empty">还没有待办，先给 AI 学习伙伴一个任务吧。</p>
        </div>
      </div>

      <!-- 番茄钟面板 -->
      <div v-else class="ai-pet-content">
        <div class="ai-pet-focus-card">
          <span>{{ focusPhaseTitle }}</span>
          <strong>{{ focusTimeText }}</strong>
          <em>{{ focusPhaseTip }}</em>
          <div class="ai-pet-progress">
            <i :style="{ width: `${focusProgress}%` }"></i>
          </div>
        </div>
        <div class="ai-pet-duration-grid">
          <label class="ai-pet-duration">
            <span>每轮专注</span>
            <input v-model.number="focusMinutes" type="number" min="1" max="120" />
          </label>
          <label class="ai-pet-duration">
            <span>休息分钟</span>
            <input v-model.number="breakMinutes" type="number" min="1" max="30" />
          </label>
        </div>
        <div class="ai-pet-actions">
          <button type="button" @click="startFocus">{{ focusPrimaryButtonText }}</button>
          <button type="button" @click="pauseFocus">暂停</button>
          <button type="button" @click="resetFocus">重置</button>
        </div>
        <p class="ai-pet-focus-summary">
          已完成 {{ focusSummary.sessions }} 轮 · 累计 {{ focusSummary.minutes }} 分钟
        </p>
      </div>
    </section>

    <!-- 宠物头像按钮（可点击展开面板，可拖动） -->
    <button
      type="button"
      class="ai-pet-avatar"
      :style="avatarStyle"
      @click="handleAvatarClick"
      @pointerdown="beginDrag"
    >
      <span class="ai-pet-drag-hand" aria-hidden="true">
        <i></i>
        <i></i>
        <i></i>
        <i></i>
      </span>
      <span class="ai-pet-land-stars" aria-hidden="true">
        <i></i>
        <i></i>
        <i></i>
      </span>
      <span class="ai-pet-quiet-shadow" aria-hidden="true"></span>
      <img :src="petImage" :alt="petDisplayName" draggable="false" />
      <span v-if="!open" class="ai-pet-avatar__label">{{ petDisplayName }}</span>
    </button>

    <!-- 唤醒宠物按钮（宠物隐藏时显示） -->
    <button
      v-if="userHidden"
      type="button"
      class="ai-pet-wake-tab"
      title="唤醒宠物"
      @click="wakePet"
    >
      <el-icon><View /></el-icon>
      <span>唤醒宠物</span>
    </button>
  </aside>
</template>
