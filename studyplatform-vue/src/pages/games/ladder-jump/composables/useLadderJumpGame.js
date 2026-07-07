import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { recordQuestionBankAnswer } from '../../../../api/academy'
import { fetchLadderJumpQuestionBanks, saveLadderJumpRecord } from '../../../../api/games'
import { request } from '../../../../api/request'
import {
  answeredLeftPadding,
  answerPlatformWidth,
  assetBase,
  confirmOffset,
  defaultQuestions,
  excludedQuestionBankCategoryCodes,
  excludedQuestionBankSetCodes,
  gravity,
  groundY,
  initialCars,
  jumpSpeed,
  maxJumpCount,
  moveSpeed,
  platformLayouts,
  playerSize,
  questionGap,
  questionStartX,
  sceneLayers,
  stageHeight,
  stageWidth,
  travelCoinBetweenStartOffset,
  travelCoinBetweenWidth,
  travelCoinCountPerQuestion,
  travelCoinLanes,
} from '../config/ladderJumpConfig'
import { formatAverageDuration, formatDuration, seededRandom } from '../utils/ladderJumpFormat'

export function useLadderJumpGame() {
const questions = ref(defaultQuestions)
const questionBanks = ref([])
const selectedQuestionBankCode = ref('')
const questionBankLoading = ref(false)
const questionDropdownOpen = ref(false)
const questionBankPanelRef = ref(null)
const usingFallbackQuestions = ref(false)
const ladderJumpRecordSaved = ref(false)
const questionIndex = ref(0)
const score = ref(0)
const health = ref(3)
const combo = ref(0)
const correctAnswerCount = ref(0)
const wrongAnswerCount = ref(0)
const elapsedMs = ref(0)
const feedback = ref('使用 A/D 或 WASD 移动，空格跳跃，落到选项平台后向右穿过确认线完成答题')
const isPaused = ref(false)
const isGameOver = ref(false)
const gameOverTitle = ref('挑战结束')
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
const collectedTravelCoinIds = ref([])
const routes = ref([])
const selectedPlatform = ref(null)
const leftBounds = ref({})
const cars = ref(initialCars.map((car) => ({ ...car })))
const pressedKeys = new Set()
let animationId = 0
let frameTimer = 0
let lastTimestamp = 0
let questionPoolRefreshing = false
let activePlayStartedAt = 0
let accumulatedPlayMs = 0

const currentQuestion = computed(() => questions.value[questionIndex.value % questions.value.length])
const selectedQuestionBank = computed(() =>
  questionBanks.value.find((item) => item.code === selectedQuestionBankCode.value) || null,
)
const currentQuestionPoolCount = computed(() => questions.value.length)
const questionBankButtonTitle = computed(() => selectedQuestionBank.value?.title || '全部单选题库')
const questionBankButtonSubtitle = computed(() =>
  selectedQuestionBank.value ? selectedQuestionBank.value.categoryName : '随机混合题池',
)
const questionBankSummary = computed(() => {
  if (selectedQuestionBank.value) {
    return `${selectedQuestionBank.value.title} · 当前可随机题数 ${currentQuestionPoolCount.value}`
  }
  return `全部单选题库 · 当前可随机题数 ${currentQuestionPoolCount.value}`
})
const answeredCount = computed(() => correctAnswerCount.value + wrongAnswerCount.value)
const gameTimeText = computed(() => formatDuration(elapsedMs.value))
const averageTimePerQuestionText = computed(() =>
  answeredCount.value > 0 ? formatAverageDuration(elapsedMs.value / answeredCount.value) : '--',
)
const averageTimePerCorrectText = computed(() =>
  correctAnswerCount.value > 0 ? formatAverageDuration(elapsedMs.value / correctAnswerCount.value) : '--',
)
const overlayStats = computed(() => [
  { label: '本次获得金币', value: String(score.value) },
  { label: '游戏时间', value: gameTimeText.value },
  { label: '答对题目数量', value: String(correctAnswerCount.value) },
  { label: '答错题目数量', value: String(wrongAnswerCount.value) },
  { label: '平均每题耗时', value: averageTimePerQuestionText.value },
  { label: '平均答对题耗时', value: averageTimePerCorrectText.value },
])
const overlayTitle = computed(() => (isPaused.value ? '游戏暂停' : gameOverTitle.value))
const overlaySubtitle = computed(() =>
  isPaused.value ? '当前进度已冻结，继续后将从当前位置恢复。' : '本局统计已结算。'
)
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

function optionLetter(index) {
  return String.fromCharCode(65 + index)
}

function syncElapsed(now = performance.now()) {
  if (!isPaused.value && !isGameOver.value && activePlayStartedAt > 0) {
    elapsedMs.value = accumulatedPlayMs + Math.max(0, now - activePlayStartedAt)
    return
  }
  elapsedMs.value = accumulatedPlayMs
}

function startPlayTimer(now = performance.now()) {
  activePlayStartedAt = now
}

function stopPlayTimer(now = performance.now()) {
  if (activePlayStartedAt > 0) {
    accumulatedPlayMs += Math.max(0, now - activePlayStartedAt)
    activePlayStartedAt = 0
  }
  syncElapsed(now)
}

function buildQuestionRequestPath() {
  const params = new URLSearchParams()
  if (selectedQuestionBankCode.value) {
    params.set('setCode', selectedQuestionBankCode.value)
  }
  const query = params.toString()
  return query ? `/api/games/ladder-jump/questions?${query}` : '/api/games/ladder-jump/questions'
}

function resolvePlatformLayout(index) {
  if (platformLayouts[index]) {
    return platformLayouts[index]
  }
  return {
    xOffset: 1120 + Math.max(0, index - 3) * 240,
    y: Math.max(120, 175 - Math.max(0, index - 3) * 90),
  }
}

function buildQuestionPlatforms(questionOrder) {
  const question = questions.value[questionOrder % questions.value.length]
  const questionKey = `${question.id}-${questionOrder}`
  const baseX = questionStartX + questionOrder * questionGap

  return question.options.map((option, index) => {
    const layout = resolvePlatformLayout(index)
    return {
      id: `${questionKey}-${index}`,
      questionId: questionKey,
      questionOrder,
      index,
      option,
      x: baseX + layout.xOffset,
      y: layout.y,
      width: answerPlatformWidth,
      height: 54,
      isCorrect: index === question.answerIndex,
    }
  })
}

const optionPlatforms = computed(() => visibleQuestionIndexes.value.flatMap((index) => buildQuestionPlatforms(index)))
const activePlatforms = computed(() => {
  const basePlatforms = [
    { id: 'ground', x: -200, y: groundY, width: worldWidth.value + 400, height: 100, type: 'ground' },
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
const coins = computed(() =>
  routes.value
    .filter((route) => route.type === 'correct')
    .flatMap((route) =>
      Array.from({ length: 5 }, (_, index) => ({
        id: `${route.questionId}-${index}`,
        questionId: route.questionId,
        coinIndex: index,
        x: route.x + confirmOffset + 120 + index * 96,
        y: route.y - 68 - Math.sin(index / 4) * 18,
        collected: route.collectedCoins.includes(index),
      })),
    ),
)
const visibleTravelCoinIndexes = computed(() =>
  [questionIndex.value - 1, questionIndex.value, questionIndex.value + 1, questionIndex.value + 2].filter((index) => index >= 0),
)
const travelCoins = computed(() =>
  visibleTravelCoinIndexes.value.flatMap((questionOrder) =>
    Array.from({ length: travelCoinCountPerQuestion }, (_, index) => {
      const id = `travel-${questionOrder}-${index}`
      const baseX = questionStartX + questionOrder * questionGap
      const laneIndex = Math.floor(seededRandom(questionOrder * 97 + index * 31 + 13) * travelCoinLanes.length)
      const xOffset =
        travelCoinBetweenStartOffset + seededRandom(questionOrder * 131 + index * 43 + 7) * travelCoinBetweenWidth

      return {
        id,
        x: baseX + xOffset,
        y: travelCoinLanes[laneIndex],
        collected: collectedTravelCoinIds.value.includes(id),
      }
    }),
  ),
)
const worldWidth = computed(() => stageWidth + (questionIndex.value + 3) * questionGap + 1800)
const worldStyle = computed(() => ({
  width: `${worldWidth.value}px`,
  height: `${stageHeight}px`,
  transform: `translateX(${-cameraX.value}px)`,
}))
const playerStyle = computed(() => ({
  left: `${player.value.x}px`,
  top: `${player.value.y}px`,
  transform: `scaleX(${-player.value.direction})`,
}))

async function loadQuestionBanks() {
  questionBankLoading.value = true
  try {
    const banks = await fetchLadderJumpQuestionBanks()
    questionBanks.value = Array.isArray(banks)
      ? banks
          .filter(
            (set) =>
              !excludedQuestionBankCategoryCodes.has(set?.categoryCode) &&
              !excludedQuestionBankSetCodes.has(set?.setCode || set?.code),
          )
          .map((set) => ({
            code: set.setCode || set.code,
            title: set.title,
            categoryName: set.categoryName || '',
            questionCount: Number(set.questionCount || 0),
          }))
      : []
  } catch {
    questionBanks.value = []
  } finally {
    questionBankLoading.value = false
  }
}

async function loadQuestions() {
  try {
    const data = await request(buildQuestionRequestPath())
    if (Array.isArray(data) && data.length > 0) {
      questions.value = data
      usingFallbackQuestions.value = false
      return
    }
    questions.value = defaultQuestions
    usingFallbackQuestions.value = true
    feedback.value = selectedQuestionBankCode.value
      ? '\u5f53\u524d\u6240\u9009\u9898\u5e93\u6682\u65e0\u53ef\u7528\u4e8e\u5e73\u53f0\u8df3\u8dc3\u7684\u5355\u9009\u9898\uff0c\u5df2\u5207\u6362\u5230\u672c\u5730\u6f14\u793a\u9898\uff0c\u9519\u9898\u4e0d\u4f1a\u5199\u5165\u9519\u9898\u672c\u3002'
      : '\u5f53\u524d\u540e\u7aef\u6682\u65e0\u53ef\u7528\u5355\u9009\u9898\uff0c\u5df2\u5207\u6362\u5230\u672c\u5730\u6f14\u793a\u9898\uff0c\u9519\u9898\u4e0d\u4f1a\u5199\u5165\u9519\u9898\u672c\u3002'
  } catch {
    questions.value = defaultQuestions
    usingFallbackQuestions.value = true
    feedback.value = '\u9898\u5e93\u63a5\u53e3\u6682\u65f6\u4e0d\u53ef\u7528\uff0c\u5df2\u5207\u6362\u5230\u672c\u5730\u6f14\u793a\u9898\uff0c\u9519\u9898\u4e0d\u4f1a\u5199\u5165\u9519\u9898\u672c\u3002'
  }
}

async function refreshQuestionPoolForNextLoop() {
  if (questionPoolRefreshing || questions.value.length === 0) return
  questionPoolRefreshing = true
  try {
    await loadQuestions()
  } finally {
    questionPoolRefreshing = false
  }
}

function toggleQuestionDropdown() {
  if (questionBankLoading.value) return
  questionDropdownOpen.value = !questionDropdownOpen.value
}

async function selectQuestionBank(code) {
  questionDropdownOpen.value = false
  if (selectedQuestionBankCode.value === code) return
  selectedQuestionBankCode.value = code
  await loadQuestions()
  restartGame(false)
  feedback.value = selectedQuestionBank.value
    ? `已切换到题库「${selectedQuestionBank.value.title}」，当前题目将随机抽取。`
    : '已切换到全部单选题库，当前题目将随机抽取。'
}

function handleDocumentPointerDown(event) {
  if (!questionDropdownOpen.value) return
  if (questionBankPanelRef.value?.contains(event.target)) return
  questionDropdownOpen.value = false
}

function pauseGame() {
  if (isPaused.value || isGameOver.value) return
  isPaused.value = true
  pressedKeys.clear()
  stopPlayTimer()
}

function resumeGame() {
  if (!isPaused.value || isGameOver.value) return
  isPaused.value = false
  startPlayTimer()
}

function togglePause() {
  if (isGameOver.value) return
  if (isPaused.value) {
    resumeGame()
    return
  }
  pauseGame()
}

function finishGame(title = '\u6311\u6218\u7ed3\u675f') {
  if (isGameOver.value) return
  gameOverTitle.value = title
  isPaused.value = false
  isGameOver.value = true
  pressedKeys.clear()
  stopPlayTimer()
}

function reviveGame() {
  if (!isGameOver.value) return false
  isGameOver.value = false
  isPaused.value = false
  gameOverTitle.value = '\u6311\u6218\u7ed3\u675f'
  health.value = Math.max(1, health.value)
  damageFlash.value = false
  feedback.value = '已使用复活券，生命恢复并继续挑战。'
  player.value = {
    ...player.value,
    y: groundY - playerSize.height,
    vy: 0,
    onGround: true,
    jumpCount: 0,
  }
  startPlayTimer()
  return true
}


function handleKeyDown(event) {
  if (event.code === 'Escape') {
    event.preventDefault()
    togglePause()
    return
  }

  if (isPaused.value || isGameOver.value) {
    return
  }

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
  syncElapsed(timestamp)

  if (!isPaused.value && !isGameOver.value) {
    updateCars(delta)
    updatePlayer(delta)
    updateCamera()
    collectCoins()
    advanceQuestionAfterLeavingCurrentArea()
  }

  animationId = window.requestAnimationFrame(updateGame)
}

function updateCars(delta) {
  const loopWidth = Math.max(worldWidth.value, stageWidth + 4200)
  cars.value = cars.value.map((car) => {
    const nextX = car.x + car.direction * car.speed * delta
    if (car.direction > 0 && nextX > loopWidth + 180) {
      return { ...car, x: -260 }
    }
    if (car.direction < 0 && nextX < -260) {
      return { ...car, x: loopWidth + 180 }
    }
    return { ...car, x: nextX }
  })
}

function updatePlayer(delta) {
  const movingLeft = pressedKeys.has('KeyA') || pressedKeys.has('ArrowLeft')
  const movingRight = pressedKeys.has('KeyD') || pressedKeys.has('ArrowRight')
  const nextPlayer = { ...player.value }
  const bounds = resolvePlayerBounds()

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
    syncSelectedPlatform(platform)
  }

  if (nextPlayer.y + playerSize.height >= groundY && nextPlayer.vy >= 0) {
    nextPlayer.y = groundY - playerSize.height
    nextPlayer.vy = 0
    nextPlayer.onGround = true
    nextPlayer.jumpCount = 0
  }

  player.value = nextPlayer
  rescuePlayerFromVoid()
  syncSelectedPlatform(findStandingOptionPlatform())
  maybeConfirmSelection()
}

function resolvePlayerBounds() {
  const bounds = leftBounds.value[currentQuestionKey.value]
  const isCurrentQuestionAnswered = answeredQuestionIds.value.includes(currentQuestionKey.value)

  if (isCurrentQuestionAnswered) {
    return {
      min: bounds?.min ?? 0,
      max: bounds?.max ?? Number.POSITIVE_INFINITY,
    }
  }

  const currentQuestionPlatforms = optionPlatforms.value.filter((platform) => platform.questionId === currentQuestionKey.value)
  const rightmostOptionEdge = Math.max(...currentQuestionPlatforms.map((platform) => platform.x + platform.width))

  return {
    min: bounds?.min ?? 0,
    max: Number.isFinite(rightmostOptionEdge) ? rightmostOptionEdge - playerSize.width : Number.POSITIVE_INFINITY,
  }
}

function findStandingOptionPlatform() {
  if (!player.value.onGround) return null

  return optionPlatforms.value.find((platform) => {
    const standingOnPlatform = Math.abs(player.value.y + playerSize.height - platform.y) <= 3
    const overlapsX = player.value.x + playerSize.width > platform.x + 8 && player.value.x < platform.x + platform.width - 8
    return platform.questionId === currentQuestionKey.value && standingOnPlatform && overlapsX
  })
}

function syncSelectedPlatform(platform) {
  if (!platform || platform.questionId !== currentQuestionKey.value) return
  if (answeredQuestionIds.value.includes(platform.questionId)) return

  selectedPlatform.value = platform
  feedback.value = `已进入选项 ${optionLetter(platform.index)}：${platform.option}。继续向右穿过确认线即可作答。`
}

function rescuePlayerFromVoid() {
  if (player.value.y < stageHeight + 80) return

  player.value = {
    ...player.value,
    y: groundY - playerSize.height,
    vy: 0,
    onGround: true,
    jumpCount: 0,
  }
}

function maybeConfirmSelection() {
  const platform = selectedPlatform.value
  if (!platform || platform.questionId !== currentQuestionKey.value) return
  if (answeredQuestionIds.value.includes(platform.questionId)) return
  if (player.value.x + playerSize.width < platform.x + confirmOffset) return

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
    correctAnswerCount.value += 1
    routes.value = [
      ...routes.value,
      { questionId: platform.questionId, type: 'correct', x: platform.x, y: platform.y, collectedCoins: [] },
    ]
    combo.value += 1
    score.value += 10 + combo.value * 2
    feedback.value = `回答正确。${currentQuestion.value.explanation} 继续向右前进，下一题会从屏幕外进入。`
    playAudio('data.mp3')
    return
  }

  wrongAnswerCount.value += 1
  recordWrongQuestionAnswer(platform)
  routes.value = [
    ...routes.value,
    { questionId: platform.questionId, type: 'wrong', x: platform.x, y: platform.y, collectedCoins: [] },
  ]
  combo.value = 0
  health.value -= 1
  applyWrongAnswerKnockback(platform)
  triggerDamageFeedback()
  feedback.value = `回答错误。${currentQuestion.value.explanation} 生命 -1，错题已加入错题本。`
  playAudio('damage.mp3')
  if (health.value <= 0) {
    finishGame('挑战失败')
  }
}

function recordWrongQuestionAnswer(platform) {
  if (usingFallbackQuestions.value) return

  const question = questions.value[platform.questionOrder % questions.value.length]
  if (!question?.id || platform.index === undefined || platform.index === null) return

  recordQuestionBankAnswer({
    questionId: question.id,
    selectedAnswer: optionLetter(platform.index),
  }).catch(() => {})
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
      player.value.x < coin.x + 42 &&
      player.value.y + playerSize.height > coin.y &&
      player.value.y < coin.y + 42
    if (!hit) return

    routes.value = routes.value.map((route) => {
      if (route.questionId !== coin.questionId) return route
      return { ...route, collectedCoins: [...new Set([...route.collectedCoins, coin.coinIndex])] }
    })
    score.value += 3
    playAudio('data.mp3')
  })

  travelCoins.value.forEach((coin) => {
    if (coin.collected) return
    const hit =
      player.value.x + playerSize.width > coin.x &&
      player.value.x < coin.x + 38 &&
      player.value.y + playerSize.height > coin.y &&
      player.value.y < coin.y + 38
    if (!hit) return

    collectedTravelCoinIds.value = [...new Set([...collectedTravelCoinIds.value, coin.id])]
    score.value += 2
    playAudio('data.mp3')
  })
}

function advanceQuestionAfterLeavingCurrentArea() {
  const answered = answeredQuestionIds.value.includes(currentQuestionKey.value)
  const leftCurrentQuestionArea = player.value.x > currentQuestionBaseX.value + questionGap - 360
  if (answered && leftCurrentQuestionArea) {
    const nextQuestionIndex = questionIndex.value + 1
    questionIndex.value = nextQuestionIndex
    selectedPlatform.value = null
    if (questions.value.length > 0 && nextQuestionIndex % questions.value.length === 0) {
      refreshQuestionPoolForNextLoop()
    }
    window.requestAnimationFrame(() => {
      syncSelectedPlatform(findStandingOptionPlatform())
    })
    feedback.value = '新题已激活。跳到目标平台并穿过确认线作答。'
  }
}

function updateCamera() {
  const target = Math.max(0, player.value.x - 360)
  cameraX.value += (target - cameraX.value) * 0.12
}

function restartGame(shouldReloadPool = true) {
  isPaused.value = false
  isGameOver.value = false
  ladderJumpRecordSaved.value = false
  gameOverTitle.value = '\u6311\u6218\u7ed3\u675f'
  score.value = 0
  health.value = 3
  combo.value = 0
  correctAnswerCount.value = 0
  wrongAnswerCount.value = 0
  questionIndex.value = 0
  answeredQuestionIds.value = []
  collectedTravelCoinIds.value = []
  routes.value = []
  selectedPlatform.value = null
  leftBounds.value = {}
  feedback.value = '\u4f7f\u7528 A/D \u6216 WASD \u79fb\u52a8\uff0c\u7a7a\u683c\u8df3\u8dc3\uff0c\u843d\u5230\u9009\u9879\u5e73\u53f0\u540e\u5411\u53f3\u7a7f\u8fc7\u786e\u8ba4\u7ebf\u5b8c\u6210\u7b54\u9898'
  accumulatedPlayMs = 0
  elapsedMs.value = 0
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
  startPlayTimer()
  if (shouldReloadPool) {
    refreshQuestionPoolForNextLoop()
  }
}

function persistLadderJumpRecord() {
  if (ladderJumpRecordSaved.value) return

  ladderJumpRecordSaved.value = true
  saveLadderJumpRecord({
    questionBankCode: selectedQuestionBankCode.value || null,
    totalCoins: score.value,
    correctCount: correctAnswerCount.value,
    wrongCount: wrongAnswerCount.value,
    durationSeconds: Number((elapsedMs.value / 1000).toFixed(2)),
  }).catch(() => {
    ladderJumpRecordSaved.value = false
  })
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

onMounted(async () => {
  await loadQuestionBanks()
  await loadQuestions()
  startPlayTimer()
  window.addEventListener('keydown', handleKeyDown)
  window.addEventListener('keyup', handleKeyUp)
  document.addEventListener('pointerdown', handleDocumentPointerDown)
  frameTimer = window.setInterval(() => {
    playerFrame.value += 1
  }, 120)
  animationId = window.requestAnimationFrame(updateGame)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleKeyDown)
  window.removeEventListener('keyup', handleKeyUp)
  document.removeEventListener('pointerdown', handleDocumentPointerDown)
  window.clearInterval(frameTimer)
  window.cancelAnimationFrame(animationId)
})

  return {
    assetBase,
    worldStyle,
    sceneLayers,
    questionCards,
    feedback,
    activePlatforms,
    selectedPlatform,
    optionLetter,
    coins,
    travelCoins,
    currentQuestionKey,
    answeredQuestionIds,
    confirmOffset,
    cars,
    damageFlash,
    playerStyle,
    playerSprite,
    questionBankPanelRef,
    questionBankLoading,
    toggleQuestionDropdown,
    questionBankButtonTitle,
    questionBankButtonSubtitle,
    questionDropdownOpen,
    selectedQuestionBankCode,
    selectQuestionBank,
    questionBanks,
    questionBankSummary,
    score,
    combo,
    gameTimeText,
    heartText,
    pauseGame,
    isPaused,
    isGameOver,
    overlayTitle,
    overlaySubtitle,
    overlayStats,
    resumeGame,
    restartGame,
    finishGame,
    reviveGame,
    persistLadderJumpRecord,
  }
}

