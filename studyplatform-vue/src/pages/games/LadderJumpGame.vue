<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { request } from '../../api/request'

const emit = defineEmits(['back'])

const assetBase = '/games/ladder-jump'
const stageWidth = 1800
const stageHeight = 760
const playerSize = { width: 66, height: 78 }
const gravity = 0.78
const moveSpeed = 6.2
const jumpSpeed = 13.8
const maxJumpCount = 3
const questionStartX = 520
const questionGap = 2100
const groundY = 660
const answerPlatformWidth = 1280
const confirmOffset = 430
const answeredLeftPadding = 240
const defaultQuestions = [
  {
    id: 1,
    question: 'Java 中用于声明类继承关系的关键字是？',
    options: ['extends', 'implements', 'instanceof'],
    answerIndex: 0,
    explanation: 'extends 表示一个类继承另一个类。',
  },
  {
    id: 2,
    question: 'Vue 3 组合式 API 中用于创建响应式引用的是？',
    options: ['ref', 'map', 'bind'],
    answerIndex: 0,
    explanation: 'ref 可以创建一个响应式引用值。',
  },
  {
    id: 3,
    question: 'HTTP 状态码 404 通常表示？',
    options: ['请求成功', '资源不存在', '服务器重启'],
    answerIndex: 1,
    explanation: '404 表示客户端请求的资源没有找到。',
  },
]

const questions = ref(defaultQuestions)
const questionIndex = ref(0)
const score = ref(0)
const health = ref(3)
const combo = ref(0)
const feedback = ref('使用 A/D 或 WASD 移动，空格跳跃，落到正确选项平台继续前进')
const isGameOver = ref(false)
const damageFlash = ref(false)
const playerFrame = ref(0)
const player = ref({
  x: 120,
  y: groundY - playerSize.height,
  vx: 0,
  vy: 0,
  onGround: true,
  direction: 1,
  jumpCount: 0,
})
const cameraX = ref(0)
const answeredQuestionIds = ref([])
const routes = ref([])
const selectedPlatform = ref(null)
const lockedQuestionIds = ref([])
const leftBounds = ref({})
const pressedKeys = new Set()
let animationId = 0
let lastTimestamp = 0
let frameTimer = 0

const currentQuestion = computed(() => questions.value[questionIndex.value % questions.value.length])
const playerSprite = computed(() => {
  if (Math.abs(player.value.vx) > 0.1) {
    return `${assetBase}/player/walk_${playerFrame.value % 8}.png`
  }
  return `${assetBase}/player/idle_${playerFrame.value % 4}.png`
})
const heartText = computed(() => '♥'.repeat(Math.max(health.value, 0)))
const currentQuestionBaseX = computed(() => questionStartX + questionIndex.value * questionGap)
const currentQuestionKey = computed(() => `${currentQuestion.value.id}-${questionIndex.value}`)
const visibleQuestionIndexes = computed(() =>
  [questionIndex.value - 1, questionIndex.value, questionIndex.value + 1].filter((index) => index >= 0),
)
const questionCards = computed(() =>
  [questionIndex.value, questionIndex.value + 1].map((index) => {
    const question = questions.value[index % questions.value.length]
    return {
      id: `${question.id}-${index}`,
      index,
      question,
      x: questionStartX + index * questionGap + 820,
      isCurrent: index === questionIndex.value,
    }
  }),
)
function buildQuestionPlatforms(questionOrder) {
  const platformLayout = [
    { xOffset: 400, y: 520 },
    { xOffset: 640, y: 405 },
    { xOffset: 880, y: 290 },
  ]
  const question = questions.value[questionOrder % questions.value.length]
  const questionKey = `${question.id}-${questionOrder}`
  const baseX = questionStartX + questionOrder * questionGap

  return question.options.map((option, index) => ({
    id: `${questionKey}-${index}`,
    questionId: questionKey,
    questionOrder,
    index,
    option,
    x: baseX + platformLayout[index].xOffset,
    y: platformLayout[index].y,
    width: answerPlatformWidth,
    height: 46,
    isCorrect: index === question.answerIndex,
  }))
}
const optionPlatforms = computed(() => visibleQuestionIndexes.value.flatMap((index) => buildQuestionPlatforms(index)))
const activePlatforms = computed(() => {
  const startPlatforms = visibleQuestionIndexes.value.map((index) => ({
    id: `start-${index}`,
    x: questionStartX + index * questionGap - 260,
    y: groundY - 86,
    width: 280,
    height: 42,
  }))
  const basePlatforms = [
    { id: 'ground', x: -200, y: groundY, width: stageWidth + 9000, height: 100, type: 'ground' },
    ...startPlatforms,
    ...optionPlatforms.value,
  ]
  const bridgePlatforms = routes.value
    .filter((route) => route.type === 'correct')
    .flatMap((route) => [
      { id: `bridge-a-${route.questionId}`, x: route.x + answerPlatformWidth - 20, y: route.y + 10, width: 220, height: 34 },
      { id: `bridge-b-${route.questionId}`, x: route.x + answerPlatformWidth + 260, y: groundY - 86, width: 420, height: 42 },
    ])

  return [...basePlatforms, ...bridgePlatforms]
})
const coins = computed(() => {
  return routes.value
    .filter((route) => route.type === 'correct')
    .flatMap((route) =>
      Array.from({ length: 5 }, (_, index) => ({
        id: `${route.questionId}-${index}`,
        questionId: route.questionId,
        coinIndex: index,
        x: route.x + confirmOffset + 120 + index * 96,
        y: route.y - 58 - Math.sin(index / 4) * 18,
        collected: route.collectedCoins.includes(index),
      })),
    )
})
const worldStyle = computed(() => ({
  width: `${stageWidth + (questionIndex.value + 2) * questionGap + 1800}px`,
  height: `${stageHeight}px`,
  transform: `translateX(${-cameraX.value}px)`,
}))
const playerStyle = computed(() => ({
  left: `${player.value.x}px`,
  top: `${player.value.y}px`,
  transform: `scaleX(${-player.value.direction})`,
}))
const currentConfirmX = computed(() => {
  const platform = selectedPlatform.value
  if (!platform || platform.questionId !== currentQuestionKey.value) {
    return currentQuestionBaseX.value + 1280
  }
  return platform.x + confirmOffset
})

async function loadQuestions() {
  try {
    const data = await request('/api/games/ladder-jump/questions')
    if (Array.isArray(data) && data.length > 0) {
      questions.value = data
    }
  } catch {
    feedback.value = '后端题库暂不可用，已启用本地默认题库。使用键盘继续游戏'
  }
}

function handleKeyDown(event) {
  if (['Space', 'ArrowUp', 'ArrowLeft', 'ArrowRight'].includes(event.code)) {
    event.preventDefault()
  }
  pressedKeys.add(event.code)
  if (event.repeat) return

  const wantsDrop = event.code === 'KeyS' || event.code === 'ArrowDown'
  if (wantsDrop) {
    dropFromPlatform()
    return
  }

  const wantsJump = event.code === 'Space' || event.code === 'KeyW' || event.code === 'ArrowUp'
  if (wantsJump && player.value.jumpCount < maxJumpCount) {
    player.value = {
      ...player.value,
      vy: -jumpSpeed,
      onGround: false,
      jumpCount: player.value.jumpCount + 1,
    }
    playAudio('jump_remote.mp3')
  }
}

function handleKeyUp(event) {
  pressedKeys.delete(event.code)
}

function dropFromPlatform() {
  if (!player.value.onGround) return

  const standingPlatform = activePlatforms.value.find((platform) => {
    if (platform.id === 'ground') return false
    const standingOnPlatform = Math.abs(player.value.y + playerSize.height - platform.y) <= 2
    const overlapsX = player.value.x + playerSize.width > platform.x + 8 && player.value.x < platform.x + platform.width - 8
    return standingOnPlatform && overlapsX
  })
  if (!standingPlatform) return

  player.value = {
    ...player.value,
    y: player.value.y + 8,
    vy: 2.6,
    onGround: false,
    jumpCount: 1,
  }
  selectedPlatform.value = null
}

function updateGame(timestamp) {
  const delta = Math.min((timestamp - lastTimestamp) / 16.67, 1.8) || 1
  lastTimestamp = timestamp

  if (!isGameOver.value) {
    updatePlayer(delta)
    updateCamera()
    collectCoins()
    advanceQuestionWhenBackOnGround()
  }

  animationId = window.requestAnimationFrame(updateGame)
}

function updatePlayer(delta) {
  const movingLeft = pressedKeys.has('KeyA') || pressedKeys.has('ArrowLeft')
  const movingRight = pressedKeys.has('KeyD') || pressedKeys.has('ArrowRight')
  const nextPlayer = { ...player.value }
  const bounds = leftBounds.value[currentQuestionKey.value]

  nextPlayer.vx = 0
  if (movingLeft) {
    nextPlayer.vx = -moveSpeed
    nextPlayer.direction = -1
  }
  if (movingRight) {
    nextPlayer.vx = moveSpeed
    nextPlayer.direction = 1
  }

  const minX = bounds?.min ?? 0
  const maxX = bounds?.max ?? Number.POSITIVE_INFINITY
  nextPlayer.x = Math.min(maxX, Math.max(minX, nextPlayer.x + nextPlayer.vx * delta))
  nextPlayer.vy += gravity * delta
  nextPlayer.y += nextPlayer.vy * delta
  nextPlayer.onGround = false

  const previousBottom = player.value.y + playerSize.height
  const nextBottom = nextPlayer.y + playerSize.height
  const platform = activePlatforms.value.find((item) => {
    const crossesTop = previousBottom <= item.y && nextBottom >= item.y
    const overlapsX = nextPlayer.x + playerSize.width > item.x + 8 && nextPlayer.x < item.x + item.width - 8
    return nextPlayer.vy >= 0 && crossesTop && overlapsX
  })

  if (platform) {
    nextPlayer.y = platform.y - playerSize.height
    nextPlayer.vy = 0
    nextPlayer.onGround = true
    nextPlayer.jumpCount = 0
    if (platform.questionId === currentQuestionKey.value && !answeredQuestionIds.value.includes(platform.questionId)) {
      selectedPlatform.value = platform
      feedback.value = `Standing on option ${String.fromCharCode(65 + platform.index)}: ${platform.option}. Move right across the confirm line to answer.`
    }
  }

  player.value = nextPlayer
  maybeConfirmSelection()
}

function maybeConfirmSelection() {
  const platform = selectedPlatform.value
  if (!platform || platform.questionId !== currentQuestionKey.value) return
  if (answeredQuestionIds.value.includes(platform.questionId)) return
  if (player.value.x + playerSize.width < platform.x + confirmOffset) return

  lockedQuestionIds.value = [...lockedQuestionIds.value, platform.questionId]
  leftBounds.value = {
    ...leftBounds.value,
    [platform.questionId]: {
      min: platform.x + confirmOffset - playerSize.width - answeredLeftPadding,
      max: Number.POSITIVE_INFINITY,
    },
  }
  judgePlatform(platform)
}

function judgePlatform(platform) {
  if (!platform.questionId || answeredQuestionIds.value.includes(platform.questionId)) return

  answeredQuestionIds.value = [...answeredQuestionIds.value, platform.questionId]
  if (platform.isCorrect) {
    routes.value = [
      ...routes.value,
      { questionId: platform.questionId, type: 'correct', x: platform.x, y: platform.y, collectedCoins: [] },
    ]
    combo.value += 1
    score.value += 10 + combo.value * 2
    feedback.value = `Correct: ${currentQuestion.value.explanation} You can still move inside this question area. Keep moving right for the next question.`
    playAudio('data.mp3')
    return
  }

  routes.value = [
    ...routes.value,
    { questionId: platform.questionId, type: 'wrong', x: platform.x, y: platform.y, collectedCoins: [] },
  ]
  combo.value = 0
  health.value -= 1
  applyWrongAnswerKnockback(platform)
  triggerDamageFeedback()
  feedback.value = `Wrong: ${currentQuestion.value.explanation} Health -1. Keep moving right for the next question.`
  playAudio('damage.mp3')
  if (health.value <= 0) {
    isGameOver.value = true
  }
}

function applyWrongAnswerKnockback(platform) {
  const bounds = leftBounds.value[platform.questionId]
  const nextX = Math.max(bounds?.min ?? 0, player.value.x - 86)
  player.value = {
    ...player.value,
    x: nextX,
    vy: -5.2,
    onGround: false,
    jumpCount: Math.min(player.value.jumpCount + 1, maxJumpCount),
    direction: -1,
  }
}

function collectCoins() {
  coins.value.forEach((coin) => {
    if (coin.collected) return
    const hit =
      player.value.x + playerSize.width > coin.x &&
      player.value.x < coin.x + 34 &&
      player.value.y + playerSize.height > coin.y &&
      player.value.y < coin.y + 34
    if (!hit) return

    routes.value = routes.value.map((route) => {
      if (route.questionId !== coin.questionId) return route
      return { ...route, collectedCoins: [...new Set([...route.collectedCoins, coin.coinIndex])] }
    })
    score.value += 3
    playAudio('data.mp3')
  })
}

function advanceQuestionWhenBackOnGround() {
  const answered = answeredQuestionIds.value.includes(currentQuestionKey.value)
  const hasReturnedToGround = player.value.onGround && player.value.y + playerSize.height >= groundY - 1
  const crossedGate = player.value.x > currentQuestionBaseX.value + questionGap - 360
  if (answered && hasReturnedToGround && crossedGate) {
    questionIndex.value += 1
    selectedPlatform.value = null
    feedback.value = '新的题目环节已开始，可再次左右移动选择平台'
  }
}

function updateCamera() {
  const target = Math.max(0, player.value.x - 360)
  cameraX.value += (target - cameraX.value) * 0.12
}

function restartGame() {
  score.value = 0
  health.value = 3
  combo.value = 0
  questionIndex.value = 0
  answeredQuestionIds.value = []
  routes.value = []
  selectedPlatform.value = null
  lockedQuestionIds.value = []
  leftBounds.value = {}
  isGameOver.value = false
  feedback.value = '使用 A/D 或 WASD 移动，空格跳跃，落到正确选项平台继续前进'
  player.value = {
    x: 120,
    y: groundY - playerSize.height,
    vx: 0,
    vy: 0,
    onGround: true,
    direction: 1,
    jumpCount: 0,
  }
  cameraX.value = 0
  damageFlash.value = false
  pressedKeys.clear()
}

function triggerDamageFeedback() {
  damageFlash.value = false
  window.requestAnimationFrame(() => {
    damageFlash.value = true
    window.setTimeout(() => {
      damageFlash.value = false
    }, 520)
  })
}

function playAudio(fileName) {
  const audio = new Audio(`${assetBase}/audio/${fileName}`)
  audio.volume = 0.32
  audio.play().catch(() => {})
}

onMounted(() => {
  loadQuestions()
  window.addEventListener('keydown', handleKeyDown)
  window.addEventListener('keyup', handleKeyUp)
  frameTimer = window.setInterval(() => {
    playerFrame.value += 1
  }, 120)
  animationId = window.requestAnimationFrame(updateGame)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeyDown)
  window.removeEventListener('keyup', handleKeyUp)
  window.clearInterval(frameTimer)
  window.cancelAnimationFrame(animationId)
})
</script>

<template>
  <section class="ladder-game-page">
    <header class="ladder-game-header">
      <button type="button" class="ladder-back-button" @click="emit('back')">返回游戏模块</button>
      <div>
        <p>Platform Quiz Runner</p>
        <h1>万题天梯跳</h1>
      </div>
      <div class="ladder-stats">
        <span>金币 {{ score }}</span>
        <span>连击 {{ combo }}</span>
        <span class="ladder-hearts">{{ heartText }}</span>
      </div>
    </header>

    <main class="ladder-stage" aria-label="万题天梯跳游戏区域" tabindex="0">
      <div class="ladder-world" :style="worldStyle">
        <div class="ladder-bg-layer ladder-bg-sky"></div>
        <div class="ladder-bg-layer ladder-bg-far"></div>
        <div class="ladder-bg-layer ladder-bg-mid"></div>
        <div class="ladder-bg-layer ladder-bg-near"></div>

        <section
          v-for="card in questionCards"
          :key="card.id"
          class="ladder-question-card"
          :class="{ 'is-next-question': !card.isCurrent }"
          :style="{ left: `${card.x}px` }"
        >
          <span>第 {{ card.index + 1 }} 题</span>
          <h2>{{ card.question.question }}</h2>
          <p>{{ card.isCurrent ? feedback : '继续前进，下一题即将到达' }}</p>
        </section>

        <div
          v-for="platform in activePlatforms.filter((item) => item.id !== 'ground')"
          :key="platform.id"
          class="ladder-platform"
          :class="{
            'is-option': platform.questionId,
            'is-selected-option': selectedPlatform && selectedPlatform.id === platform.id,
            'is-correct-option': platform.questionId && platform.isCorrect,
          }"
          :style="{ left: `${platform.x}px`, top: `${platform.y}px`, width: `${platform.width}px`, height: `${platform.height}px` }"
        >
          <template v-if="platform.questionId">
            <span>{{ String.fromCharCode(65 + platform.index) }}</span>
            <strong>{{ platform.option }}</strong>
          </template>
          <template v-else>起跳区</template>
        </div>

        <div
          v-for="coin in coins"
          v-show="!coin.collected"
          :key="coin.id"
          class="ladder-coin"
          :style="{ left: `${coin.x}px`, top: `${coin.y}px` }"
        >
          ¥
        </div>

        <div
          v-if="selectedPlatform && selectedPlatform.questionId === currentQuestionKey && !answeredQuestionIds.includes(currentQuestionKey)"
          class="ladder-confirm-line"
          :style="{ left: `${selectedPlatform.x + confirmOffset}px`, top: `${selectedPlatform.y - 106}px`, height: '152px' }"
        >
          <span>确认线</span>
        </div>

        <div class="ladder-ground"></div>

        <div class="ladder-player" :class="{ 'is-damaged': damageFlash }" :style="playerStyle">
          <img :src="playerSprite" alt="玩家角色" />
        </div>
      </div>

      <aside class="ladder-control-hint">
        <span>A/D 或 ←/→ 移动</span>
        <span>W / 空格 / ↑ 三级跳</span>
        <span>S / ↓ 下落</span>
      </aside>

      <div v-if="isGameOver" class="ladder-game-over">
        <p>挑战结束</p>
        <h2>最终金币 {{ score }}</h2>
        <button type="button" @click="restartGame">重新开始</button>
      </div>
    </main>
  </section>
</template>
