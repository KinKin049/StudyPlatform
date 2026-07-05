<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Hide, View } from '@element-plus/icons-vue'

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

const open = ref(false)
const activeTab = ref('chat')
const frameIndex = ref(0)
const mood = ref('idle')
const chatInput = ref('')
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
const messages = ref([
  {
    role: 'pet',
    text: '喵，我是星云学习猫。现在先陪你管理待办和专注，下一步就能接真实 AI 对话。',
  },
])

let animationTimer = null
let focusTimer = null
let moodResetTimer = null
let landingTimer = null
let celebrationTimer = null
let suppressClickTimer = null
let petBubbleTimer = null
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
  return {
    path: route.path,
    title: document.title || 'StudyPlatform',
    headings,
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
  todos.value.unshift({
    id: Date.now(),
    title,
    done: false,
    createdAt: new Date().toISOString(),
  })
  newTodo.value = ''
  saveTodos()
  setMood('happy')
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

function sendMessage() {
  const text = chatInput.value.trim()
  if (!text) {
    return
  }
  messages.value.push({ role: 'user', text })
  chatInput.value = ''
  setMood('thinking', 1000)
  window.setTimeout(() => {
    pushPetMessage('我已经拿到当前页面摘要啦。真实大模型接口接入后，我会结合页面内容直接帮你分析和操作。')
    setMood('talk', 1800)
  }, 600)
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
          <button type="submit">发送</button>
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
