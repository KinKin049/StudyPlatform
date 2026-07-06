<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Hide, View } from '@element-plus/icons-vue'

import { chatWithAiPet } from '../api/aiPet'
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

const frames = {
  idle: [idle01, idle02, idle03, idle04],
  talk: [talk01, talk02, talk03, talk04],
  thinking: [thinking01, thinking02, thinking03, thinking04],
  happy: [happy01, happy02, happy03, happy04],
  focus: [focus01, focus02, focus03, focus04],
  sleep: [sleep01, sleep02, sleep03, sleep04],
}

const todoStorageKey = 'study-platform-ai-pet-todos'
const focusStorageKey = 'study-platform-ai-pet-focus-summary'
const positionStorageKey = 'study-platform-ai-pet-position'
const visibilityStorageKey = 'study-platform-ai-pet-hidden'
const petSize = 116
const viewportPadding = 14
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
const positionReady = ref(false)
const dragging = ref(false)
const landing = ref(false)
const celebrating = ref(false)
const suppressClick = ref(false)
const userHidden = ref(false)
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
    text: '喵，我是星云学习猫。现在可以真的帮你跳转页面、创建待办、启动番茄专注啦。',
  },
])

let animationTimer = null
let focusTimer = null
let moodResetTimer = null
let landingTimer = null
let celebrationTimer = null
let suppressClickTimer = null
let petBubbleTimer = null
let actionFeedbackTimer = null
let dragState = null

const hidden = computed(() => route.meta?.hidePet === true)

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

const petImage = computed(() => {
  const currentFrames = frames[activeMood.value] || frames.idle
  return currentFrames[frameIndex.value % currentFrames.length]
})

const completedTodoCount = computed(() => todos.value.filter((todo) => todo.done).length)

const widgetStyle = computed(() => ({
  left: `${displayPosition.value.x}px`,
  top: `${displayPosition.value.y}px`,
}))

const widgetClasses = computed(() => ({
  'is-open': open.value,
  'is-ready': positionReady.value,
  'is-dragging': dragging.value,
  'is-landing': landing.value,
  'is-celebrating': celebrating.value,
  'is-user-hidden': userHidden.value,
  'align-left': widgetPosition.value.x < 300,
  [`mood-${activeMood.value}`]: true,
}))

const displayPosition = computed(() => {
  if (!userHidden.value) {
    return widgetPosition.value
  }
  return {
    x: Math.max(viewportSize.value.width - 22, viewportPadding),
    y: Math.min(
      Math.max(widgetPosition.value.y + 18, viewportPadding),
      Math.max(viewportPadding, viewportSize.value.height - 86),
    ),
  }
})

const focusProgress = computed(() => {
  const total = getPhaseMinutes() * 60
  const elapsed = Math.min(total, Math.max(0, total - focusRemaining.value))
  return Math.round((elapsed / total) * 100)
})

const focusTimeText = computed(() => {
  const minutes = Math.floor(focusRemaining.value / 60)
  const seconds = focusRemaining.value % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

const focusPhaseTitle = computed(() => (focusPhase.value === 'break' ? '休息倒计时' : '专注倒计时'))

const focusPhaseTip = computed(() => (
  focusPhase.value === 'break'
    ? `休息 ${getPhaseMinutes('break')} 分钟后自动进入下一轮`
    : `每 ${getPhaseMinutes('focus')} 分钟完成一轮番茄钟`
))

const focusPrimaryButtonText = computed(() => {
  if (!focusRunning.value) {
    return '开始'
  }
  return focusPhase.value === 'break' ? '休息中' : '专注中'
})

const pageContext = computed(() => {
  const headings = Array.from(document.querySelectorAll('h1, h2, h3'))
    .slice(0, 5)
    .map((element) => element.textContent?.trim())
    .filter(Boolean)
  const visibleText = document.body?.innerText
    ?.replace(/\s+/g, ' ')
    .trim()
    .slice(0, 1800) || ''
  return {
    path: route.path,
    title: document.title || 'StudyPlatform',
    headings,
    textSnippet: visibleText,
  }
})

const contextSummary = computed(() => {
  const headingText = pageContext.value.headings.length ? pageContext.value.headings.join(' / ') : '当前页面暂无标题摘要'
  return `${pageContext.value.path} · ${headingText}`
})

function setMood(nextMood, duration = 1800) {
  mood.value = nextMood
  if (moodResetTimer) {
    window.clearTimeout(moodResetTimer)
  }
  moodResetTimer = window.setTimeout(() => {
    mood.value = 'idle'
  }, duration)
}

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

function pushPetMessage(text) {
  messages.value.push({
    role: 'pet',
    text,
  })
  showPetBubble(text)
}

function showActionFeedback(text) {
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

function normalizeCommand(text) {
  return text
    .replace(/\s+/g, '')
    .replace(/[，。！？、,.!?]/g, '')
    .toLowerCase()
}

function cleanActionTitle(text) {
  return text
    .replace(/^(请|麻烦|帮我|给我|帮忙|可以)?/g, '')
    .replace(/(创建|添加|新增|记录|安排|加入|加一个|加一条|建一个|建一条)/g, '')
    .replace(/(一个|一条|一项|个|条|项)/g, '')
    .replace(/(待办事项|待办|任务|todo|TODO)/gi, '')
    .replace(/[：:，,。！!？?]/g, ' ')
    .trim()
}

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

function resolveCourseKeyword(text) {
  return text
    .replace(/(请|麻烦|帮我|给我|帮忙|可以|想要|我要|我想)/g, '')
    .replace(/(找一门|找门|找|搜索|查找|看看|打开|进入)/g, '')
    .replace(/(课程|课|相关|有关|关于|的)/g, ' ')
    .replace(/[：:，,。！!？?]/g, ' ')
    .trim()
}

function findNavigationTarget(text) {
  return navigationTargets.find((target) => target.keywords.some((keyword) => text.includes(keyword)))
}

async function handleLocalToolAction(text) {
  const normalizedText = normalizeCommand(text)
  const actionReplies = []

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

  const wantsPomodoro = /(启动|开始|开启|进入).*(番茄|专注)/.test(normalizedText)
    || /(番茄|专注).*(启动|开始|开启)/.test(normalizedText)
  if (wantsPomodoro) {
    startFocus()
    open.value = true
    activeTab.value = 'focus'
    actionReplies.push('已启动番茄专注')
  }

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

  const wantsNavigation = /(打开|跳转|进入|去|查看|带我去)/.test(normalizedText)
  const navigationTarget = wantsNavigation ? findNavigationTarget(text) : null
  if (navigationTarget) {
    await router.push(navigationTarget.path)
    open.value = false
    actionReplies.push(`已打开${navigationTarget.label}`)
  }

  if (!actionReplies.length) {
    return false
  }

  const feedbackText = actionReplies.join('，')
  showActionFeedback(feedbackText)
  pushPetMessage(`${feedbackText}。`)
  setMood('happy', 1800)
  return true
}

function buildChatHistory() {
  return messages.value
    .slice(0, -1)
    .slice(-8)
    .map((message) => ({
      role: message.role === 'user' ? 'user' : 'assistant',
      text: message.text,
    }))
}

function loadTodos() {
  try {
    const savedTodos = JSON.parse(window.localStorage.getItem(todoStorageKey) || '[]')
    todos.value = Array.isArray(savedTodos) ? savedTodos : []
  } catch {
    todos.value = []
  }
}

function saveTodos() {
  window.localStorage.setItem(todoStorageKey, JSON.stringify(todos.value))
}

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

function saveFocusSummary() {
  window.localStorage.setItem(focusStorageKey, JSON.stringify(focusSummary.value))
}

function loadPetVisibility() {
  userHidden.value = window.localStorage.getItem(visibilityStorageKey) === '1'
}

function savePetVisibility() {
  window.localStorage.setItem(visibilityStorageKey, userHidden.value ? '1' : '0')
}

function updateViewportSize() {
  viewportSize.value = {
    width: window.innerWidth || 1280,
    height: window.innerHeight || 720,
  }
}

function clampPosition(position) {
  const maxX = Math.max(viewportPadding, viewportSize.value.width - petSize - viewportPadding)
  const maxY = Math.max(viewportPadding, viewportSize.value.height - petSize - viewportPadding)
  return {
    x: Math.min(Math.max(position.x, viewportPadding), maxX),
    y: Math.min(Math.max(position.y, viewportPadding), maxY),
  }
}

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

function savePosition() {
  window.localStorage.setItem(positionStorageKey, JSON.stringify(widgetPosition.value))
}

function handleResize() {
  updateViewportSize()
  widgetPosition.value = clampPosition(widgetPosition.value)
  savePosition()
}

function togglePanel() {
  if (userHidden.value) {
    return
  }
  open.value = !open.value
  if (open.value) {
    clearPetBubble()
  }
  setMood(open.value ? 'happy' : 'idle', 1200)
}

function handleAvatarClick() {
  if (suppressClick.value || dragging.value) {
    return
  }
  togglePanel()
}

function openPetBubble() {
  clearPetBubble()
  open.value = true
  activeTab.value = 'chat'
  setMood('happy', 1200)
}

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

function clearDragListeners() {
  window.removeEventListener('pointermove', handleDragMove)
  window.removeEventListener('pointerup', endDrag)
  window.removeEventListener('pointercancel', cancelDrag)
}

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

function cancelDrag() {
  clearDragListeners()
  dragState = null
  dragging.value = false
}

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

function hidePet() {
  open.value = false
  dragging.value = false
  landing.value = false
  clearPetBubble()
  userHidden.value = true
  savePetVisibility()
}

function wakePet() {
  userHidden.value = false
  savePetVisibility()
  setMood('happy', 1400)
}

function addTodo() {
  const title = newTodo.value.trim()
  if (!title) {
    return
  }
  addTodoWithTitle(title)
  newTodo.value = ''
}

function toggleTodo(todo) {
  todo.done = !todo.done
  saveTodos()
  setMood(todo.done ? 'happy' : 'idle')
}

function removeTodo(todoId) {
  todos.value = todos.value.filter((todo) => todo.id !== todoId)
  saveTodos()
}

function getPhaseMinutes(phase = focusPhase.value) {
  const fallbackMinutes = phase === 'break' ? 5 : 25
  const rawMinutes = phase === 'break' ? breakMinutes.value : focusMinutes.value
  return Math.max(1, Number(rawMinutes) || fallbackMinutes)
}

function resetFocusDuration(phase = focusPhase.value) {
  focusRemaining.value = getPhaseMinutes(phase) * 60
}

function switchFocusPhase(nextPhase) {
  focusPhase.value = nextPhase
  resetFocusDuration(nextPhase)
}

function startFocus() {
  if (focusRemaining.value <= 0) {
    resetFocusDuration()
  }
  focusRunning.value = true
  activeTab.value = 'focus'
}

function pauseFocus() {
  focusRunning.value = false
  setMood('idle')
}

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

function finishFocusSegment() {
  const completedMinutes = getPhaseMinutes('focus')
  focusSummary.value = {
    sessions: focusSummary.value.sessions + 1,
    minutes: focusSummary.value.minutes + completedMinutes,
  }
  saveFocusSummary()
  pushPetMessage(`本轮 ${completedMinutes} 分钟专注完成啦！星云猫正在放烟花，先休息 ${getPhaseMinutes('break')} 分钟。`)
  playFocusCompleteSound()
  triggerFocusCelebration()
  switchFocusPhase('break')
}

function finishBreak() {
  pushPetMessage('休息结束啦，下一轮番茄钟自动开始。星云猫继续陪你冲！')
  switchFocusPhase('focus')
}

function resetFocus() {
  focusRunning.value = false
  switchFocusPhase('focus')
  setMood('idle')
}

async function sendMessage() {
  const text = chatInput.value.trim()
  if (!text || chatLoading.value) {
    return
  }
  messages.value.push({ role: 'user', text })
  chatInput.value = ''
  if (await handleLocalToolAction(text)) {
    return
  }
  chatLoading.value = true
  setMood('thinking', 8000)
  try {
    const response = await chatWithAiPet({
      message: text,
      pageContext: pageContext.value,
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

watch(focusMinutes, () => {
  if (!focusRunning.value && focusPhase.value === 'focus') {
    resetFocusDuration('focus')
  }
})

watch(breakMinutes, () => {
  if (!focusRunning.value && focusPhase.value === 'break') {
    resetFocusDuration('break')
  }
})

watch(hidden, (isHidden) => {
  if (isHidden) {
    open.value = false
    clearPetBubble()
  }
})

onMounted(() => {
  loadTodos()
  loadFocusSummary()
  loadPetVisibility()
  loadPosition()
  animationTimer = window.setInterval(() => {
    frameIndex.value += 1
  }, 250)
  focusTimer = window.setInterval(tickFocus, 1000)
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
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
  clearDragListeners()
  window.removeEventListener('resize', handleResize)
})
</script>

<template>
  <aside
    v-if="!hidden"
    class="ai-pet-widget"
    :class="widgetClasses"
    :style="widgetStyle"
    aria-label="AI 学习宠物"
  >
    <Transition name="ai-pet-action-feedback">
      <div v-if="actionFeedback.visible" class="ai-pet-action-feedback" role="status">
        {{ actionFeedback.text }}
      </div>
    </Transition>

    <button
      v-if="petBubble.visible && !open && !userHidden && !dragging"
      type="button"
      class="ai-pet-speech-bubble"
      @click="openPetBubble"
    >
      <span>喵～</span>
      <strong>{{ petBubble.text }}</strong>
    </button>

    <section v-if="open" class="ai-pet-panel">
      <div class="ai-pet-panel__header">
        <div>
          <div class="ai-pet-panel__title-row">
            <p>星云学习猫</p>
            <button
              type="button"
              class="ai-pet-panel__hide"
              title="隐藏宠物"
              data-tooltip="隐藏宠物"
              @click="hidePet"
            >
              <el-icon><Hide /></el-icon>
            </button>
          </div>
          <span>待办 · 专注 · AI 助手</span>
        </div>
        <button type="button" class="ai-pet-panel__close" @click="togglePanel">×</button>
      </div>

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

      <div v-if="activeTab === 'chat'" class="ai-pet-content ai-pet-chat">
        <div class="ai-pet-context">
          <strong>页面摘要</strong>
          <span>{{ contextSummary }}</span>
        </div>
        <div class="ai-pet-messages">
          <p
            v-for="(message, index) in messages"
            :key="`${message.role}-${index}`"
            :class="['ai-pet-message', `is-${message.role}`]"
          >
            {{ message.text }}
          </p>
        </div>
        <form class="ai-pet-input-row" @submit.prevent="sendMessage">
          <input v-model="chatInput" type="text" placeholder="问问星云猫当前页面的问题..." />
          <button type="submit" :disabled="chatLoading">{{ chatLoading ? '思考中' : '发送' }}</button>
        </form>
      </div>

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
          <p v-if="!todos.length" class="ai-pet-empty">还没有待办，先给星云猫一个任务吧。</p>
        </div>
      </div>

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

    <button
      type="button"
      class="ai-pet-avatar"
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
      <img :src="petImage" alt="星云学习猫" draggable="false" />
      <span v-if="!open" class="ai-pet-avatar__label">星云猫</span>
    </button>

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
