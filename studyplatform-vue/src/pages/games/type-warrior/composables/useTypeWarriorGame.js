import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  TYPE_WARRIOR_ACTIVE_SKILL_WORD_BANK,
  TYPE_WARRIOR_BALANCE,
  TYPE_WARRIOR_CONFIG,
  TYPE_WARRIOR_ENEMY_KINDS,
  TYPE_WARRIOR_SKILL_POOL,
  TYPE_WARRIOR_WORD_BANK,
} from '../config/typeWarriorConfig'
import { getTypeWarriorFinalWave, getTypeWarriorWaveProfile } from '../config/typeWarriorWaveConfig'
import { clamp, getDistance, getSuffixMatchLength, normalizeWord, pickRandomItems, randomFrom } from '../utils/typeWarriorMath'
import { buildEnemyKeywordTrie, findBestTrieSuffixMatch } from '../utils/typeWarriorTrie'

const {
  arenaSize,
  playerCollisionRadius,
  maxCards,
  keyBurstDuration,
  spawnOffset,
  rollingBufferLimit,
} = TYPE_WARRIOR_CONFIG

const {
  player: PLAYER_BALANCE,
  combat: COMBAT_BALANCE,
  boss: BOSS_BALANCE,
  enemies: ENEMY_BALANCE,
  skills: SKILL_BALANCE,
} = TYPE_WARRIOR_BALANCE

const centerPoint = arenaSize / 2
const { wordTransitionDuration } = COMBAT_BALANCE

/**
 * Owns all runtime state for the Type Warrior scene.
 * The page and presentational components stay thin and declarative.
 */
export function useTypeWarriorGame() {
  const wave = ref(1)
  const weaponLevel = ref(1)
  const health = ref(PLAYER_BALANCE.baseHealth)
  const maxHealth = ref(PLAYER_BALANCE.baseHealth)
  const energy = ref(PLAYER_BALANCE.baseEnergy)
  const maxEnergy = ref(PLAYER_BALANCE.baseMaxEnergy)
  const hasGameStarted = ref(false)
  const combo = ref(0)
  const typedBuffer = ref('')
  const selectedMatchLength = ref(0)
  const enemies = ref([])
  const bullets = ref([])
  const enemyFragments = ref([])
  const cards = ref([])
  const isGameOver = ref(false)
  const isVictory = ref(false)
  const isChoosingSkill = ref(false)
  const skillChoices = ref([])
  const banner = ref('直接输入敌人上方的英文单词，系统会自动锁定并开火。')
  const bossState = ref('idle')
  const waveSpawned = ref(0)
  const waveTargetCount = ref(7)
  const spawnCooldown = ref(0)
  const bossSpawned = ref(0)
  const bossTargetCount = ref(0)
  const bossSpawnCooldown = ref(0)
  const bossMinionCooldown = ref(0)
  const damageCooldown = ref(0)
  const lastTypedAt = ref(performance.now())
  const typingBurst = ref(0)
  const survivalSeconds = ref(0)
  const targetEnemyId = ref(null)
  const playerHitFeedback = ref(0)
  const keyBursts = ref([])
  const comboFeedbackCount = ref(0)
  const comboFeedbackTimer = ref(0)
  const comboShakeTimer = ref(0)
  const isPaused = ref(false)
  const maxCombo = ref(0)
  const score = ref(0)
  const solvedWordCount = ref(0)
  const typedLetterCount = ref(0)
  const completedWaveCount = ref(0)
  const effectiveTypingSeconds = ref(0)
  const purgeCooldownRemaining = ref(0)
  const purgeUsesThisWave = ref(0)
  const purgeWordState = ref({
    active: false,
    word: '',
    text: '',
    buffer: '',
  })
  const arenaRef = ref(null)
  const pendingWaveNumber = ref(null)
  const currentWaveProfile = ref(getTypeWarriorWaveProfile(1))
  const viewportSpawnBounds = ref({
    left: -spawnOffset,
    right: arenaSize + spawnOffset,
    top: -spawnOffset,
    bottom: arenaSize + spawnOffset,
  })
  const viewportVisibleBounds = ref({
    left: 0,
    right: arenaSize,
    top: 0,
    bottom: arenaSize,
  })

  let animationId = 0
  let lastFrameAt = 0
  let enemyIdSeed = 0
  let bulletIdSeed = 0
  let keyBurstIdSeed = 0
  let fragmentIdSeed = 0
  let enemyKeywordTrie = buildEnemyKeywordTrie([])
  let enemyKeywordTrieDirty = true

  const currentTarget = computed(() => enemies.value.find((enemy) => enemy.id === targetEnemyId.value) || null)
  const isCriticalHealth = computed(() => health.value < PLAYER_BALANCE.criticalHealthThreshold)
  const wpmLike = computed(() => Math.round(typingBurst.value * 8.2))
  const wordsPerSecond = computed(() => (effectiveTypingSeconds.value > 0 ? solvedWordCount.value / effectiveTypingSeconds.value : 0))
  const lettersPerSecond = computed(() => (effectiveTypingSeconds.value > 0 ? typedLetterCount.value / effectiveTypingSeconds.value : 0))
  const resultStats = computed(() => ({
    maxCombo: maxCombo.value,
    score: Math.round(score.value),
    completedWaves: completedWaveCount.value,
    solvedWords: solvedWordCount.value,
    durationSeconds: survivalSeconds.value,
    wordsPerSecond: wordsPerSecond.value,
    lettersPerSecond: lettersPerSecond.value,
  }))
  const purgeCooldownLabel = computed(() => {
    if (!hasSkill('purge')) return '未解锁'
    if (purgeWordState.value.active) return '输入中'
    if (purgeCooldownRemaining.value > 0) return `${Math.ceil(purgeCooldownRemaining.value)}秒`
    if (purgeUsesThisWave.value >= SKILL_BALANCE.purge.maxUsesPerWave) return '本关已用'
    return '可用'
  })
  const playerShellClass = computed(() => ({
    'is-hit': playerHitFeedback.value > 0,
  }))
  const boardClass = computed(() => ({
    'is-combo-shake': comboShakeTimer.value > 0,
    'is-paused': isPaused.value,
  }))
  const hudStageLabel = computed(() => {
    if (!hasGameStarted.value) return '等待开始'
    if (isVictory.value) return '通关完成'
    if (isGameOver.value) return '战斗结束'
    if (bossState.value === 'active') return `第 ${wave.value} 关 / 首领`
    return `第 ${wave.value} 关 / 词潮`
  })
  const hudStageHint = computed(() => {
    if (!hasGameStarted.value) return '点击开始游戏后进入第一关。'
    if (isPaused.value) return '已暂停，按 Esc 继续。'
    if (isGameOver.value) return '生命耗尽，防线失守。'
    if (isVictory.value) return '最终首领已被击败。'
    if (isChoosingSkill.value) return '本关已清空，请先选择一项技能。'
    if (bossState.value === 'active') return '优先锁定首领单词，注意环绕压场与吐词增援。'
    return `当前场上 ${enemies.value.length} 个目标，连击 ${combo.value}。`
  })
  const displayStageLabel = computed(() => {
    if (!hasGameStarted.value) return '等待开始'
    if (isVictory.value) return '通关完成'
    if (bossState.value === 'active') return `第 ${wave.value} 关 / 首领`
    return `第 ${wave.value} 关 / 词潮`
  })
  const displayStageHint = computed(() => {
    if (!hasGameStarted.value) return '点击开始游戏后进入第一关。'
    if (isPaused.value) return '已暂停，按 Esc 继续。'
    if (isGameOver.value) return '生命耗尽，词潮突破防线。'
    if (isVictory.value) return '首领已被清除。'
    if (isChoosingSkill.value) return '本关已清空，请先选择一项技能。'
    if (bossState.value === 'active') return '优先处理首领，注意绕场轨迹与吐词方向。'
    return `当前词潮 ${enemies.value.length} 个目标，连击 ${combo.value}。`
  })
  const playerRingStyle = computed(() => {
    const healthRatio = clamp(health.value / maxHealth.value, 0, 1)
    const energyRatio = clamp(energy.value / maxEnergy.value, 0, 1)

    return {
      background: `
        radial-gradient(circle at center, rgba(255, 252, 244, 0.98) 0 42%, transparent 42%),
        conic-gradient(from -90deg, #d96a59 0 ${healthRatio * 360}deg, rgba(217, 106, 89, 0.16) ${healthRatio * 360}deg 360deg),
        conic-gradient(from 90deg, #8b948e 0 ${energyRatio * 360}deg, rgba(139, 148, 142, 0.16) ${energyRatio * 360}deg 360deg)
      `,
    }
  })
  const stageLabel = computed(() => {
    if (!hasGameStarted.value) return '等待开始'
    if (isVictory.value) return '通关完成'
    if (bossState.value === 'active') return `第 ${wave.value} 关 / 首领`
    return `第 ${wave.value} 关 / 文字浪潮`
  })
  const stageHint = computed(() => {
    if (!hasGameStarted.value) return '点击开始游戏后进入第一关。'
    if (isGameOver.value) return '生命耗尽，文字浪潮突破防线。'
    if (isVictory.value) return '首领已被击败。'
    if (isChoosingSkill.value) return '本关已清空，请先选择一项技能。'
    if (bossState.value === 'active') return '锁定首领单词，同时避开外圈环绕路径。'
    return `当前词潮 ${enemies.value.length} 个目标，连击 ${combo.value}。`
  })

  function sampleRange([min, max]) {
    if (max <= min) return min
    return min + Math.random() * (max - min)
  }

  function getSpawnInterval(ratePerSecond) {
    return ratePerSecond > 0 ? 1 / ratePerSecond : Number.POSITIVE_INFINITY
  }

  function hasConfiguredBosses(profile = currentWaveProfile.value) {
    return (profile?.boss?.totalCount ?? 0) > 0
  }

  function getSkillLevel(skillId) {
    return cards.value.find((card) => card.id === skillId)?.level ?? 0
  }

  function getSkillValue(skillId, fieldName) {
    const level = getSkillLevel(skillId)
    const values = SKILL_BALANCE[skillId]?.[fieldName] ?? []
    if (values.length === 0) return 0
    return values[Math.min(level, values.length - 1)] ?? 0
  }

  function syncMaxCombo() {
    maxCombo.value = Math.max(maxCombo.value, combo.value)
  }

  function markWaveCompleted(currentWave = wave.value) {
    completedWaveCount.value = Math.max(completedWaveCount.value, currentWave)
  }

  function syncDerivedStats({ restoreHealth = false, restoreEnergy = false } = {}) {
    const nextMaxHealth = PLAYER_BALANCE.baseHealth + getSkillValue('shield', 'maxHealthBonus')
    const nextMaxEnergy = PLAYER_BALANCE.baseMaxEnergy + getSkillValue('reserve', 'maxEnergyBonus')
    const healthDelta = nextMaxHealth - maxHealth.value
    const energyDelta = nextMaxEnergy - maxEnergy.value

    maxHealth.value = nextMaxHealth
    maxEnergy.value = nextMaxEnergy
    health.value = restoreHealth && healthDelta > 0 ? clamp(health.value + healthDelta, 0, maxHealth.value) : clamp(health.value, 0, maxHealth.value)
    energy.value = restoreEnergy && energyDelta > 0 ? clamp(energy.value + energyDelta, 0, maxEnergy.value) : clamp(energy.value, 0, maxEnergy.value)
  }

  function createKeyBurstEntry(letter, failed = false) {
    const angle = Math.random() * Math.PI * 2
    const distance =
      (failed ? COMBAT_BALANCE.keyBurst.failedDistanceBase : COMBAT_BALANCE.keyBurst.normalDistanceBase) +
      Math.random() * (failed ? COMBAT_BALANCE.keyBurst.failedDistanceRange : COMBAT_BALANCE.keyBurst.normalDistanceRange)

    return {
      id: `key-burst-${keyBurstIdSeed++}`,
      letter,
      age: 0,
      dx: Math.cos(angle) * distance,
      dy: Math.sin(angle) * distance - (failed ? COMBAT_BALANCE.keyBurst.failedLift : COMBAT_BALANCE.keyBurst.normalLift),
      failed,
    }
  }

  function createKeyBurst(letter) {
    keyBursts.value = [...keyBursts.value, createKeyBurstEntry(letter, false)]
  }

  function createFailedKeyBurst(letter) {
    keyBursts.value = [...keyBursts.value, createKeyBurstEntry(letter, true)]
  }

  function updateViewportSpawnBounds() {
    const arenaElement = arenaRef.value
    if (!arenaElement) return

    const rect = arenaElement.getBoundingClientRect()
    const scale = rect.width / arenaSize || 1

    viewportSpawnBounds.value = {
      left: (0 - rect.left) / scale - spawnOffset,
      right: (window.innerWidth - rect.left) / scale + spawnOffset,
      top: (0 - rect.top) / scale - spawnOffset,
      bottom: (window.innerHeight - rect.top) / scale + spawnOffset,
    }

    viewportVisibleBounds.value = {
      left: (0 - rect.left) / scale,
      right: (window.innerWidth - rect.left) / scale,
      top: (0 - rect.top) / scale,
      bottom: (window.innerHeight - rect.top) / scale,
    }
  }

  function pickWordForWave(currentWave) {
    const tier = currentWave >= 5 ? 4 : currentWave >= 4 ? 3 : currentWave >= 2 ? 2 : 1
    const options = TYPE_WARRIOR_WORD_BANK.filter((item) => item.tier <= tier)
    return randomFrom(options)
  }

  function createSpawnPosition() {
    const { left, right, top, bottom } = viewportSpawnBounds.value
    const side = Math.floor(Math.random() * 4)

    if (side === 0) return { x: left, y: top + Math.random() * (bottom - top) }
    if (side === 1) return { x: right, y: top + Math.random() * (bottom - top) }
    if (side === 2) return { x: left + Math.random() * (right - left), y: top }

    return {
      x: left + Math.random() * (right - left),
      y: bottom,
    }
  }

  function buildEnemy(currentWave, boss = false, options = {}) {
    const word = pickWordForWave(currentWave)
    const kind = boss
      ? { type: 'boss', shape: 'boss', baseHealth: BOSS_BALANCE.baseHealth, baseSpeed: BOSS_BALANCE.baseSpeed, accent: BOSS_BALANCE.accent }
      : randomFrom(TYPE_WARRIOR_ENEMY_KINDS)
    const spawnPoint = options.spawnPoint ?? createSpawnPosition()
    const healthMultiplier = options.healthMultiplier ?? 1
    const baseHealth = boss ? kind.baseHealth + currentWave * BOSS_BALANCE.healthPerWave : kind.baseHealth + currentWave * ENEMY_BALANCE.healthPerWave
    const baseSpeed = boss ? kind.baseSpeed + currentWave * BOSS_BALANCE.speedPerWave : kind.baseSpeed + currentWave * ENEMY_BALANCE.speedPerWave
    const orbitAngle = Math.atan2(spawnPoint.y - centerPoint, spawnPoint.x - centerPoint)
    const emissionVector = options.emissionVector ?? null
    const scaledHealth = Math.max(1, Math.round(baseHealth * healthMultiplier))

    return {
      id: `enemy-${enemyIdSeed++}`,
      text: word.text,
      keyword: normalizeWord(word.word),
      displayWord: word.word.toLowerCase(),
      x: spawnPoint.x,
      y: spawnPoint.y,
      type: kind.type,
      shape: kind.shape,
      radius: boss ? BOSS_BALANCE.radius : kind.shape === 'dot' ? ENEMY_BALANCE.dotRadius : ENEMY_BALANCE.shapedRadius,
      health: scaledHealth,
      maxHealth: scaledHealth,
      speed: baseSpeed,
      accent: kind.accent,
      boss,
      orbitAngle,
      errorFeedback: 0,
      hitFeedback: 0,
      incomingDamage: 0,
      comboRegistered: false,
      wordTransitionState: 'idle',
      wordTransitionTimer: 0,
      pendingWord: null,
      emitFeedback: 0,
      emissionAngle: -Math.PI / 2,
      movementMode: emissionVector ? 'emitted' : 'direct',
      emissionVector,
      launchSpeed: emissionVector ? BOSS_BALANCE.minionLaunchSpeed : 0,
      chaseSpeed: 0,
    }
  }

  function pickBossEmissionAngle(bossEnemy) {
    const centerAngle = Math.atan2(centerPoint - bossEnemy.y, centerPoint - bossEnemy.x)
    const angleOffsets = [0, Math.PI / 2, -Math.PI / 2]
    const angleOffset = randomFrom(angleOffsets)

    return centerAngle + angleOffset + (Math.random() - 0.5) * BOSS_BALANCE.emissionJitter
  }

  function hasSkill(skillId) {
    return getSkillLevel(skillId) > 0
  }

  function resetPurgeWordState() {
    purgeWordState.value = {
      active: false,
      word: '',
      text: '',
      buffer: '',
    }
  }

  function activatePurgeSkill() {
    if (!hasSkill('purge') || isChoosingSkill.value || isGameOver.value || isVictory.value) return
    if (purgeWordState.value.active) return
    if (purgeCooldownRemaining.value > 0) {
      banner.value = `清屏指令冷却中：${Math.ceil(purgeCooldownRemaining.value)}秒`
      return
    }
    if (purgeUsesThisWave.value >= SKILL_BALANCE.purge.maxUsesPerWave) {
      banner.value = '本关清屏指令已经使用过。'
      return
    }

    const nextWord = randomFrom(TYPE_WARRIOR_ACTIVE_SKILL_WORD_BANK)
    typedBuffer.value = ''
    selectedMatchLength.value = 0
    targetEnemyId.value = null
    purgeWordState.value = {
      active: true,
      word: normalizeWord(nextWord.word),
      text: nextWord.text,
      buffer: '',
    }
    banner.value = `清屏指令已激活：${nextWord.word.toLowerCase()} / ${nextWord.text}`
  }

  function triggerPurgeSkill() {
    for (const enemy of enemies.value) {
      if (enemy.boss) continue

      enemy.health = 0
      enemy.incomingDamage = 0
      enemy.hitFeedback = 0.22
    }

    resetPurgeWordState()
    typedBuffer.value = ''
    selectedMatchLength.value = 0
    targetEnemyId.value = null
    purgeUsesThisWave.value += 1
    purgeCooldownRemaining.value = SKILL_BALANCE.purge.cooldownSeconds
    banner.value = '清屏指令已执行。'
  }

  function getLineSegmentDistance(x1, y1, x2, y2, px, py) {
    const dx = x2 - x1
    const dy = y2 - y1
    const lengthSquared = dx * dx + dy * dy
    if (lengthSquared === 0) return getDistance(x1, y1, px, py)

    const projection = clamp(((px - x1) * dx + (py - y1) * dy) / lengthSquared, 0, 1)
    const closestX = x1 + dx * projection
    const closestY = y1 + dy * projection
    return getDistance(closestX, closestY, px, py)
  }

  function applyDamageToEnemy(enemy, damage, { refreshWord = true } = {}) {
    enemy.health -= damage
    enemy.incomingDamage = Math.max(0, enemy.incomingDamage - damage)
    enemy.hitFeedback = 0.22
    if (refreshWord && enemy.health > 0) {
      triggerEnemyWordRefresh(enemy)
    }
  }

  function pickReplacementWord(enemy) {
    let nextWord = pickWordForWave(Math.max(1, wave.value))
    let attempts = 0

    while (nextWord.word.toLowerCase() === enemy.displayWord && attempts < 8) {
      nextWord = pickWordForWave(Math.max(1, wave.value))
      attempts += 1
    }

    return nextWord
  }

  function triggerEnemyWordRefresh(enemy) {
    if (enemy.wordTransitionState !== 'idle' || enemy.health <= 0) return

    enemy.pendingWord = pickReplacementWord(enemy)
    enemy.wordTransitionState = 'fade-out'
    enemy.wordTransitionTimer = wordTransitionDuration
  }

  function buildSkillChoice(skill, mode) {
    const currentLevel = getSkillLevel(skill.id)
    const nextLevel = mode === 'new' ? 1 : currentLevel + 1

    return {
      choiceId: `${skill.id}-${mode}-${nextLevel}`,
      id: skill.id,
      name: skill.name,
      type: skill.type,
      description: skill.description,
      level: nextLevel,
      mode,
      badge: mode === 'new' ? '新技能' : `等级 ${nextLevel}`,
    }
  }

  function buildSkillChoices() {
    const currentIds = new Set(cards.value.map((card) => card.id))
    const choices = []
    const newSkills = TYPE_WARRIOR_SKILL_POOL.filter((skill) => !currentIds.has(skill.id))
    const upgradeSkills = cards.value
      .map((card) => TYPE_WARRIOR_SKILL_POOL.find((skill) => skill.id === card.id))
      .filter(Boolean)

    if (cards.value.length < maxCards && newSkills.length > 0) {
      for (const skill of pickRandomItems(newSkills, 3)) {
        choices.push(buildSkillChoice(skill, 'new'))
      }
    }

    if (choices.length < 3 && upgradeSkills.length > 0) {
      for (const skill of pickRandomItems(upgradeSkills, 3 - choices.length)) {
        choices.push(buildSkillChoice(skill, 'upgrade'))
      }
    }

    if (choices.length < 3) {
      const fallbackSkills = TYPE_WARRIOR_SKILL_POOL.filter((skill) => !choices.some((choice) => choice.id === skill.id))
      for (const skill of pickRandomItems(fallbackSkills, 3 - choices.length)) {
        const mode = currentIds.has(skill.id) ? 'upgrade' : 'new'
        choices.push(buildSkillChoice(skill, mode))
      }
    }

    return choices.slice(0, 3)
  }

  function openSkillSelection(nextWave) {
    pendingWaveNumber.value = nextWave
    skillChoices.value = buildSkillChoices()
    isChoosingSkill.value = true
    typedBuffer.value = ''
    selectedMatchLength.value = 0
    targetEnemyId.value = null
    resetPurgeWordState()
    banner.value = `第 ${nextWave} 关即将开始，请先选择一项技能。`
  }

  function togglePause() {
    if (!hasGameStarted.value || isGameOver.value || isVictory.value || isChoosingSkill.value) return
    isPaused.value = !isPaused.value
    banner.value = isPaused.value ? '游戏已暂停。' : '游戏继续。'
  }

  function startGame() {
    hasGameStarted.value = true
    restartGame()
    banner.value = '游戏开始，保持节奏清理词潮。'
  }

  function applySkillChoice(choice) {
    const existingIndex = cards.value.findIndex((card) => card.id === choice.id)

    if (existingIndex >= 0) {
      const nextCards = [...cards.value]
      nextCards[existingIndex] = {
        ...nextCards[existingIndex],
        level: nextCards[existingIndex].level + 1,
      }
      cards.value = nextCards
    } else {
      const skill = TYPE_WARRIOR_SKILL_POOL.find((item) => item.id === choice.id)
      if (skill) {
        cards.value = [...cards.value, { ...skill, level: 1 }]
      }
    }

    syncDerivedStats({ restoreHealth: true, restoreEnergy: true })

    if (choice.id === 'repair') {
      health.value = clamp(health.value + getSkillValue('repair', 'onSkillPickHeal'), 0, maxHealth.value)
    }

    const nextWave = pendingWaveNumber.value
    isChoosingSkill.value = false
    pendingWaveNumber.value = null
    skillChoices.value = []
    resetPurgeWordState()

    if (nextWave !== null) {
      wave.value = nextWave
      weaponLevel.value = clamp(weaponLevel.value + COMBAT_BALANCE.skillChoiceWeaponLevelGain, 1, COMBAT_BALANCE.maxWeaponLevel)
      banner.value = `${choice.name} 已装配，第 ${nextWave} 关开始。`
      startWave(nextWave)
    }
  }

  /**
   * Debug-only helper for injecting a skill without waiting for wave rewards.
   * The feature stays removable by keeping all entry points local to the page.
   */
  function grantSkillById(skillId) {
    const skill = TYPE_WARRIOR_SKILL_POOL.find((item) => item.id === skillId)
    if (!skill) return

    const existingIndex = cards.value.findIndex((card) => card.id === skillId)
    if (existingIndex >= 0) {
      const nextCards = [...cards.value]
      nextCards[existingIndex] = {
        ...nextCards[existingIndex],
        level: nextCards[existingIndex].level + 1,
      }
      cards.value = nextCards
    } else {
      cards.value = [...cards.value, { ...skill, level: 1 }]
    }

    syncDerivedStats({ restoreHealth: true, restoreEnergy: true })
    banner.value = `${skill.name} 已通过调试面板加入当前局内。`
  }

  /**
   * Clears all equipped skills while leaving the current run active for testing.
   */
  function resetSkills() {
    cards.value = []
    syncDerivedStats()
    health.value = clamp(health.value, 0, maxHealth.value)
    energy.value = clamp(energy.value, 0, maxEnergy.value)
    banner.value = '当前技能已清空，便于继续调试。'
  }

  function startWave(currentWave) {
    const waveProfile = getTypeWarriorWaveProfile(currentWave)

    currentWaveProfile.value = waveProfile
    purgeUsesThisWave.value = 0
    waveSpawned.value = 0
    waveTargetCount.value = waveProfile.normal.totalCount
    bossSpawned.value = 0
    bossTargetCount.value = waveProfile.boss.totalCount
    spawnCooldown.value = getSpawnInterval(waveProfile.normal.spawnRatePerSecond)
    bossSpawnCooldown.value = getSpawnInterval(waveProfile.boss.spawnRatePerSecond)
    bossMinionCooldown.value = hasConfiguredBosses(waveProfile) ? BOSS_BALANCE.minionInitialDelay : 0
    bossState.value = hasConfiguredBosses(waveProfile) ? 'pending' : 'idle'
    banner.value = hasConfiguredBosses(waveProfile)
      ? `第 ${currentWave} 关首领逼近，准备应对压场与吐词。`
      : `第 ${currentWave} 关开始，保持节奏清理词潮。`
  }

  function restartGame() {
    wave.value = 1
    weaponLevel.value = 1
    health.value = PLAYER_BALANCE.baseHealth
    maxHealth.value = PLAYER_BALANCE.baseHealth
    energy.value = PLAYER_BALANCE.baseEnergy
    maxEnergy.value = PLAYER_BALANCE.baseMaxEnergy
    combo.value = 0
    typedBuffer.value = ''
    selectedMatchLength.value = 0
    enemies.value = []
    markEnemyKeywordTrieDirty()
    bullets.value = []
    enemyFragments.value = []
    cards.value = []
    isGameOver.value = false
    isVictory.value = false
    isChoosingSkill.value = false
    skillChoices.value = []
    bossState.value = 'idle'
    waveSpawned.value = 0
    waveTargetCount.value = 7
    bossSpawned.value = 0
    bossTargetCount.value = 0
    spawnCooldown.value = COMBAT_BALANCE.initialSpawnCooldown
    bossSpawnCooldown.value = 0
    bossMinionCooldown.value = 0
    damageCooldown.value = 0
    lastTypedAt.value = performance.now()
    typingBurst.value = 0
    survivalSeconds.value = 0
    targetEnemyId.value = null
    playerHitFeedback.value = 0
    keyBursts.value = []
    comboFeedbackCount.value = 0
    comboFeedbackTimer.value = 0
    comboShakeTimer.value = 0
    isPaused.value = false
    maxCombo.value = 0
    score.value = 0
    solvedWordCount.value = 0
    typedLetterCount.value = 0
    completedWaveCount.value = 0
    effectiveTypingSeconds.value = 0
    purgeCooldownRemaining.value = 0
    purgeUsesThisWave.value = 0
    pendingWaveNumber.value = null
    currentWaveProfile.value = getTypeWarriorWaveProfile(1)
    markEnemyKeywordTrieDirty()
    resetPurgeWordState()
    banner.value = '直接输入屏幕内可见敌人的英文单词即可自动锁定目标。'
    syncDerivedStats()
    startWave(1)
  }

  function getBossEnemy() {
    return enemies.value.find((enemy) => enemy.boss) ?? null
  }

  function getEnemyDistance(enemy) {
    return getDistance(enemy.x, enemy.y, centerPoint, centerPoint)
  }

  /**
   * A word becomes matchable as soon as any visible part of the enemy package
   * reaches the viewport, not only when the enemy center point crosses into view.
   */
  function isEnemyVisible(enemy) {
    const bounds = viewportVisibleBounds.value
    const directionX = centerPoint - enemy.x
    const directionY = centerPoint - enemy.y
    const distance = getDistance(enemy.x, enemy.y, centerPoint, centerPoint) || 1
    const unitX = directionX / distance
    const unitY = directionY / distance
    const tagOffset = enemy.boss ? 80 : 60
    const tagReach = enemy.boss ? 126 : 96
    const enemyPadding = enemy.radius + 10

    const bodyVisible =
      enemy.x + enemyPadding >= bounds.left &&
      enemy.x - enemyPadding <= bounds.right &&
      enemy.y + enemyPadding >= bounds.top &&
      enemy.y - enemyPadding <= bounds.bottom

    if (bodyVisible) return true

    const tagCenterX = enemy.x - unitX * tagOffset
    const tagCenterY = enemy.y - unitY * tagOffset

    return (
      tagCenterX + tagReach >= bounds.left &&
      tagCenterX - tagReach <= bounds.right &&
      tagCenterY + 30 >= bounds.top &&
      tagCenterY - 30 <= bounds.bottom
    )
  }

  function getEnemyEffectiveHealth(enemy) {
    return enemy.health - (enemy.incomingDamage ?? 0)
  }

  function markEnemyKeywordTrieDirty() {
    enemyKeywordTrieDirty = true
  }

  function ensureEnemyKeywordTrie() {
    if (!enemyKeywordTrieDirty) return

    enemyKeywordTrie = buildEnemyKeywordTrie(enemies.value.filter((enemy) => enemy.health > 0))
    enemyKeywordTrieDirty = false
  }

  /**
   * Trie-based suffix matching keeps the current selection rules:
   * 1. longest matched suffix
   * 2. nearest visible target
   * 3. shorter word as the final tiebreaker
   */
  function findBestMatchEntry(buffer, visibleOnly = true, excludeId = null) {
    if (!buffer) return null

    ensureEnemyKeywordTrie()
    const enemyById = new Map(enemies.value.map((enemy) => [enemy.id, enemy]))

    return findBestTrieSuffixMatch(enemyKeywordTrie, buffer, (enemyId, matchLength) => {
      if (enemyId === excludeId) return null

      const enemy = enemyById.get(enemyId)
      if (!enemy) return null
      if (visibleOnly && !isEnemyVisible(enemy)) return null
      if (getEnemyEffectiveHealth(enemy) <= 0) return null

      return {
        enemy,
        matchLength,
        distance: getEnemyDistance(enemy),
      }
    })
  }

  function syncTargetByBuffer(preferredId = targetEnemyId.value) {
    const buffer = typedBuffer.value
    if (!buffer) {
      targetEnemyId.value = null
      selectedMatchLength.value = 0
      return null
    }

    if (preferredId) {
      const preferredEnemy = enemies.value.find((enemy) => enemy.id === preferredId)
      const preferredLength = preferredEnemy ? getSuffixMatchLength(buffer, preferredEnemy.keyword) : 0
      if (preferredEnemy && isEnemyVisible(preferredEnemy) && preferredLength > 0) {
        targetEnemyId.value = preferredEnemy.id
        selectedMatchLength.value = preferredLength
        return {
          enemy: preferredEnemy,
          matchLength: preferredLength,
        }
      }
    }

    const nextEntry = findBestMatchEntry(buffer, true, preferredId)
    targetEnemyId.value = nextEntry?.enemy.id ?? null
    selectedMatchLength.value = nextEntry?.matchLength ?? 0
    return nextEntry
  }

  function triggerEnemyError(enemyId) {
    for (const enemy of enemies.value) {
      enemy.errorFeedback = 0
    }

    const enemy = enemies.value.find((item) => item.id === enemyId)
    if (!enemy) return
    enemy.errorFeedback = 0.42
  }

  function getCurrentTargetMatchLength(enemy) {
    if (targetEnemyId.value !== enemy.id) return 0
    return selectedMatchLength.value
  }

  function getEnemyWordParts(enemy) {
    const matchLength = getCurrentTargetMatchLength(enemy)
    return {
      matched: enemy.displayWord.slice(0, matchLength),
      rest: enemy.displayWord.slice(matchLength),
    }
  }

  function triggerComboFeedback() {
    comboFeedbackCount.value = combo.value
    comboFeedbackTimer.value = 0.72
    comboShakeTimer.value = 0.36
  }

  function fireTypedShot(enemy) {
    const now = performance.now()
    const interval = Math.max(120, now - lastTypedAt.value)
    const typingRate = 60000 / interval
    typingBurst.value = clamp(typingRate, 2, 18)
    lastTypedAt.value = now

    const passiveBonus = getSkillValue('focus', 'comboDamageBonusPerCombo')
    const projectileCount = weaponLevel.value + getSkillValue('echo', 'extraProjectiles')
    const overclockDamageBonus = getSkillValue('overclock', 'damageBonus')
    const overclockSpeedBonus = getSkillValue('overclock', 'projectileSpeedBonus')
    const repairBonus = getSkillValue('repair', 'onHitHeal')
    const baseDamage =
      COMBAT_BALANCE.baseDamage +
      weaponLevel.value * COMBAT_BALANCE.weaponDamagePerLevel +
      combo.value * (COMBAT_BALANCE.comboDamageScale + passiveBonus) +
      typingBurst.value * COMBAT_BALANCE.typingBurstScale +
      overclockDamageBonus
    const burstBonus = getSkillValue('burst', 'projectileDamageBonus')
    const rapidBonus = getSkillValue('rapid', 'energyOnComplete')
    const beamBonus = getSkillValue('beam', 'flatDamageBonus')
    const solidBulletEnabled = hasSkill('solid')
    const piercingBulletEnabled = hasSkill('pierce')
    const solidTrailDamageMultiplier = solidBulletEnabled ? getSkillValue('solid', 'trailDamageMultiplier') : 0
    const pierceTrailDamageMultiplier = piercingBulletEnabled ? getSkillValue('pierce', 'trailDamageMultiplier') : 0
    const piercingBulletLifetime = piercingBulletEnabled ? getSkillValue('pierce', 'lifetime') : 0

    // Reserve incoming damage so the next completed word does not waste shots on an already doomed enemy.
    for (let index = 0; index < projectileCount; index += 1) {
      const projectileDamage = baseDamage + burstBonus + beamBonus
      bullets.value.push({
        id: `bullet-${bulletIdSeed++}`,
        targetId: enemy.id,
        x: centerPoint,
        y: centerPoint,
        lastX: centerPoint,
        lastY: centerPoint,
        damage: projectileDamage,
        speed: COMBAT_BALANCE.baseProjectileSpeed + index * COMBAT_BALANCE.projectileSpeedStep + overclockSpeedBonus,
        trail: index,
        directionX: 0,
        directionY: 0,
        solidTrailEnabled: solidBulletEnabled,
        solidTrailMultiplier: solidTrailDamageMultiplier,
        pierceEnabled: piercingBulletEnabled,
        pierceTrailMultiplier: pierceTrailDamageMultiplier,
        piercedEnemyIds: [],
        solidHitEnemyIds: [],
        flightMode: 'tracking',
        lifetime: piercingBulletLifetime,
      })
      enemy.incomingDamage += projectileDamage
    }

    combo.value += 1
    syncMaxCombo()
    enemy.comboRegistered = true
    solvedWordCount.value += 1
    score.value += Math.round(baseDamage)
    if (combo.value > 5) {
      triggerComboFeedback()
    }

    energy.value = clamp(energy.value + COMBAT_BALANCE.wordEnergyGain + rapidBonus, 0, maxEnergy.value)
    if (repairBonus > 0) {
      health.value = clamp(health.value + repairBonus, 0, maxHealth.value)
    }
    typedBuffer.value = ''
    targetEnemyId.value = null
    selectedMatchLength.value = 0
    banner.value = `已锁定 ${enemy.text}，本次输出 ${Math.round(baseDamage)}。`
  }

  function createEnemyFragments(enemy) {
    const isLongWord = enemy.keyword.length > 6
    const fragmentCount = Math.round((enemy.boss ? 16 : 10) * (isLongWord ? 1.5 : 1))
    const nextFragments = []

    for (let index = 0; index < fragmentCount; index += 1) {
      const angle = (Math.PI * 2 * index) / fragmentCount + (Math.random() - 0.5) * 0.45
      const baseSpeed = (enemy.boss ? 280 : 220) + Math.random() * (enemy.boss ? 180 : 140)
      const speed = baseSpeed * (isLongWord ? 2 : 1)
      const maxLife = 0.48 + Math.random() * 0.2

      nextFragments.push({
        id: `fragment-${fragmentIdSeed++}`,
        x: enemy.x,
        y: enemy.y,
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed,
        rotation: Math.random() * 360,
        vr: (Math.random() - 0.5) * 900,
        size: (enemy.boss ? 10 : 7) + Math.random() * (enemy.boss ? 10 : 7),
        color: enemy.accent,
        life: maxLife,
        maxLife,
      })
    }

    enemyFragments.value = [...enemyFragments.value, ...nextFragments]
  }

  /**
   * Selected targets stay locked until they truly fail.
   * If the new suffix can match another visible word, control switches cleanly without a miss penalty.
   */
  function handleKeydown(event) {
    if (event.ctrlKey || event.metaKey || event.altKey) return

    if (event.key === 'Escape') {
      event.preventDefault()
      togglePause()
      return
    }

    if (!hasGameStarted.value || isGameOver.value || isVictory.value || isChoosingSkill.value || isPaused.value) return

    if (event.key === '1') {
      event.preventDefault()
      activatePurgeSkill()
      return
    }

    if (event.key === 'Backspace') {
      event.preventDefault()
      if (purgeWordState.value.active) {
        purgeWordState.value = {
          ...purgeWordState.value,
          buffer: purgeWordState.value.buffer.slice(0, -1),
        }
        banner.value = `清屏指令输入中：${purgeWordState.value.word} / ${purgeWordState.value.text}`
        return
      }
      typedBuffer.value = typedBuffer.value.slice(0, -1)
      syncTargetByBuffer()
      return
    }

    if (!/^[a-zA-Z]$/.test(event.key)) return

    event.preventDefault()
    const nextLetter = event.key.toLowerCase()

    if (purgeWordState.value.active) {
      typedLetterCount.value += 1
      const nextBuffer = `${purgeWordState.value.buffer}${nextLetter}`.slice(-rollingBufferLimit)
      purgeWordState.value = {
        ...purgeWordState.value,
        buffer: nextBuffer,
      }
      createKeyBurst(nextLetter)

      if (purgeWordState.value.word.startsWith(nextBuffer)) {
        banner.value = `清屏指令输入中：${purgeWordState.value.word} / ${purgeWordState.value.text}`
        if (nextBuffer === purgeWordState.value.word) {
          triggerPurgeSkill()
        }
        return
      }

      createFailedKeyBurst(nextLetter)
      combo.value = 0
      resetPurgeWordState()
      banner.value = '清屏指令输入失败。'
      return
    }

    const previousTargetId = targetEnemyId.value
    const previousTarget = currentTarget.value
    const previousBuffer = typedBuffer.value

    if (previousTarget) {
      typedLetterCount.value += 1
      const expectedLetter = previousTarget.keyword[selectedMatchLength.value] ?? ''
      if (expectedLetter === nextLetter) {
        typedBuffer.value = `${typedBuffer.value}${nextLetter}`.slice(-rollingBufferLimit)
        selectedMatchLength.value += 1
        createKeyBurst(nextLetter)

        if (selectedMatchLength.value >= previousTarget.keyword.length) {
          fireTypedShot(previousTarget)
        }
        return
      }

      const switchedBuffer = `${typedBuffer.value}${nextLetter}`.slice(-rollingBufferLimit)
      typedBuffer.value = switchedBuffer
      const switchedEntry = findBestMatchEntry(switchedBuffer, true, previousTarget.id)
      if (switchedEntry) {
        targetEnemyId.value = switchedEntry.enemy.id
        selectedMatchLength.value = switchedEntry.matchLength
        createKeyBurst(nextLetter)

        if (typedBuffer.value.endsWith(switchedEntry.enemy.keyword)) {
          fireTypedShot(switchedEntry.enemy)
        }
        return
      }

      typedBuffer.value = previousBuffer
      selectedMatchLength.value = getSuffixMatchLength(previousBuffer, previousTarget.keyword)
      createFailedKeyBurst(nextLetter)
      combo.value = 0
      banner.value = '未找到匹配单词，请重新校准输入。'
      triggerEnemyError(previousTargetId)
      return
    }

    const nextBuffer = `${typedBuffer.value}${nextLetter}`.slice(-rollingBufferLimit)
    typedBuffer.value = nextBuffer
    const nextEntry = findBestMatchEntry(nextBuffer, true)
    if (!nextEntry) {
      targetEnemyId.value = null
      selectedMatchLength.value = 0
      return
    }

    typedLetterCount.value += 1
    targetEnemyId.value = nextEntry.enemy.id
    selectedMatchLength.value = nextEntry.matchLength
    createKeyBurst(nextLetter)

    if (typedBuffer.value.endsWith(nextEntry.enemy.keyword)) {
      fireTypedShot(nextEntry.enemy)
    }
  }

  function applyBulletHits(deltaSeconds) {
    const remainingBullets = []

    for (const bullet of bullets.value) {
      const targetEnemy = enemies.value.find((item) => item.id === bullet.targetId && item.health > 0)
      const currentLastX = bullet.lastX ?? bullet.x
      const currentLastY = bullet.lastY ?? bullet.y
      const travel = bullet.speed * deltaSeconds
      let nextBullet = {
        ...bullet,
        lastX: bullet.x,
        lastY: bullet.y,
      }

      if (bullet.flightMode === 'piercing') {
        const nextX = bullet.x + bullet.directionX * travel
        const nextY = bullet.y + bullet.directionY * travel

        nextBullet = {
          ...nextBullet,
          x: nextX,
          y: nextY,
          lifetime: bullet.lifetime - deltaSeconds,
        }
      } else {
        if (!targetEnemy) continue

        const distance = getDistance(bullet.x, bullet.y, targetEnemy.x, targetEnemy.y)
        const directionX = (targetEnemy.x - bullet.x) / distance
        const directionY = (targetEnemy.y - bullet.y) / distance
        nextBullet = {
          ...nextBullet,
          x: bullet.x + directionX * travel,
          y: bullet.y + directionY * travel,
          directionX,
          directionY,
        }
      }

      const pathCandidates = enemies.value.filter(
        (enemy) => enemy.health > 0 && getLineSegmentDistance(currentLastX, currentLastY, nextBullet.x, nextBullet.y, enemy.x, enemy.y) <= enemy.radius + 10
      )

      const solidTrailEnemyIds = new Set(nextBullet.solidHitEnemyIds ?? [])
      const piercedEnemyIds = new Set(nextBullet.piercedEnemyIds ?? [])
      const sortedPathCandidates = pathCandidates.sort(
        (left, right) =>
          getDistance(currentLastX, currentLastY, left.x, left.y) - getDistance(currentLastX, currentLastY, right.x, right.y)
      )

      for (const enemy of sortedPathCandidates) {
        if (nextBullet.flightMode === 'tracking') {
          if (nextBullet.solidTrailEnabled && enemy.id !== nextBullet.targetId && !solidTrailEnemyIds.has(enemy.id)) {
            applyDamageToEnemy(enemy, nextBullet.damage * nextBullet.solidTrailMultiplier)
            solidTrailEnemyIds.add(enemy.id)
          }

          if (enemy.id === nextBullet.targetId) {
            applyDamageToEnemy(enemy, nextBullet.damage)

            if (nextBullet.pierceEnabled) {
              piercedEnemyIds.add(enemy.id)
              nextBullet = {
                ...nextBullet,
                flightMode: 'piercing',
                piercedEnemyIds: [...piercedEnemyIds],
                solidHitEnemyIds: [...solidTrailEnemyIds],
                targetId: null,
                lifetime: nextBullet.lifetime,
              }
            } else {
              nextBullet = null
            }
            break
          }
        } else if (!piercedEnemyIds.has(enemy.id)) {
          applyDamageToEnemy(enemy, nextBullet.damage * nextBullet.pierceTrailMultiplier)
          piercedEnemyIds.add(enemy.id)
        }
      }

      if (!nextBullet) continue
      nextBullet = {
        ...nextBullet,
        solidHitEnemyIds: [...solidTrailEnemyIds],
        piercedEnemyIds: [...piercedEnemyIds],
      }

      if (nextBullet.flightMode === 'piercing') {
        if (nextBullet.lifetime <= 0) continue
        const inBounds =
          nextBullet.x >= viewportSpawnBounds.value.left - 40 &&
          nextBullet.x <= viewportSpawnBounds.value.right + 40 &&
          nextBullet.y >= viewportSpawnBounds.value.top - 40 &&
          nextBullet.y <= viewportSpawnBounds.value.bottom + 40
        if (!inBounds) continue
      }

      remainingBullets.push(nextBullet)
    }

    bullets.value = remainingBullets
  }

  function scoreWaveProgress(enemy) {
    const reserveKillBonus = getSkillValue('reserve', 'killEnergyBonus')
    const reserveBossKillBonus = getSkillValue('reserve', 'bossKillEnergyBonus')

    if (enemy.boss) {
      combo.value += 2
      syncMaxCombo()
      if (combo.value > 5) {
        triggerComboFeedback()
      }
      energy.value = clamp(energy.value + COMBAT_BALANCE.bossKillEnergyGain + reserveBossKillBonus, 0, maxEnergy.value)
      return
    }

    energy.value = clamp(energy.value + COMBAT_BALANCE.nonBossKillEnergyGain + reserveKillBonus, 0, maxEnergy.value)
  }

  function removeDefeatedEnemies() {
    const defeated = enemies.value.filter((enemy) => enemy.health <= 0)
    if (defeated.length === 0) return

    const shieldLevel = getSkillLevel('shield')
    const burstWeaponGrowth = getSkillValue('burst', 'weaponGrowthPerKill')
    let defeatedBossCount = 0

    for (const enemy of defeated) {
      if (!enemy.comboRegistered) {
        combo.value += 1
        syncMaxCombo()
        enemy.comboRegistered = true
        if (combo.value > 5) {
          triggerComboFeedback()
        }
      }

      createEnemyFragments(enemy)
      weaponLevel.value = clamp(weaponLevel.value + (enemy.boss ? 1 : 0) + burstWeaponGrowth, 1, COMBAT_BALANCE.maxWeaponLevel)
      scoreWaveProgress(enemy)
      score.value += enemy.boss ? COMBAT_BALANCE.bossKillScore : COMBAT_BALANCE.nonBossKillScore
      defeatedBossCount += enemy.boss ? 1 : 0
    }

    if (shieldLevel > 0) {
      health.value = clamp(health.value + defeated.length * shieldLevel, 0, maxHealth.value)
    }

    enemies.value = enemies.value.filter((enemy) => enemy.health > 0)
    markEnemyKeywordTrieDirty()
    if (defeatedBossCount > 0) {
      const hasMoreBosses = bossSpawned.value < bossTargetCount.value
      bossState.value = hasMoreBosses ? 'pending' : 'idle'
      if (hasMoreBosses) {
        bossSpawnCooldown.value = getSpawnInterval(currentWaveProfile.value.boss.spawnRatePerSecond)
      }
    }
    syncTargetByBuffer()
  }

  function moveEnemies(deltaSeconds) {
    const nextEnemies = []
    const collidedEnemies = []

    for (const enemy of enemies.value) {
      if (enemy.boss) {
        const distance = getDistance(enemy.x, enemy.y, centerPoint, centerPoint) || 1
        const speedFactor = 1 + Math.max(0, 1 - energy.value / maxEnergy.value) * 0.08
        let nextEnemy

        if (distance > BOSS_BALANCE.orbitEnterDistance) {
          const directionX = (centerPoint - enemy.x) / distance
          const directionY = (centerPoint - enemy.y) / distance
          nextEnemy = {
            ...enemy,
            x: enemy.x + directionX * enemy.speed * speedFactor * deltaSeconds,
            y: enemy.y + directionY * enemy.speed * speedFactor * deltaSeconds,
            orbitAngle: Math.atan2(enemy.y - centerPoint, enemy.x - centerPoint),
          }
        } else {
          const nextAngle = enemy.orbitAngle + 0.92 * deltaSeconds
          nextEnemy = {
            ...enemy,
            orbitAngle: nextAngle,
            x: centerPoint + Math.cos(nextAngle) * BOSS_BALANCE.orbitRadius,
            y: centerPoint + Math.sin(nextAngle) * BOSS_BALANCE.orbitRadius,
          }
        }

        nextEnemies.push(nextEnemy)
        continue
      }

      const distance = getDistance(enemy.x, enemy.y, centerPoint, centerPoint) || 1
      const directionX = (centerPoint - enemy.x) / distance
      const directionY = (centerPoint - enemy.y) / distance
      const speedFactor = 1 + Math.max(0, 1 - energy.value / maxEnergy.value) * 0.18

      if (enemy.movementMode === 'emitted' && enemy.emissionVector) {
        const nextLaunchSpeed = Math.max(0, enemy.launchSpeed - BOSS_BALANCE.minionLaunchDeceleration * deltaSeconds)
        const launchTravel = ((enemy.launchSpeed + nextLaunchSpeed) / 2) * deltaSeconds
        const nextEnemy = {
          ...enemy,
          x: enemy.x + Math.cos(enemy.emissionVector) * launchTravel,
          y: enemy.y + Math.sin(enemy.emissionVector) * launchTravel,
          launchSpeed: nextLaunchSpeed,
        }

        if (nextLaunchSpeed <= 0) {
          nextEnemy.movementMode = 'chasing'
        }

        nextEnemies.push(nextEnemy)
        continue
      }

      if (enemy.movementMode === 'chasing') {
        const nextChaseSpeed = Math.min(enemy.speed, enemy.chaseSpeed + BOSS_BALANCE.minionChaseAcceleration * deltaSeconds)
        const nextEnemy = {
          ...enemy,
          x: enemy.x + directionX * nextChaseSpeed * speedFactor * deltaSeconds,
          y: enemy.y + directionY * nextChaseSpeed * speedFactor * deltaSeconds,
          chaseSpeed: nextChaseSpeed,
        }
        const nextDistance = getDistance(nextEnemy.x, nextEnemy.y, centerPoint, centerPoint)

        if (nextDistance <= playerCollisionRadius + enemy.radius) {
          collidedEnemies.push(enemy)
          continue
        }

        nextEnemies.push(nextEnemy)
        continue
      }

      const nextEnemy = {
        ...enemy,
        x: enemy.x + directionX * enemy.speed * speedFactor * deltaSeconds,
        y: enemy.y + directionY * enemy.speed * speedFactor * deltaSeconds,
      }
      const nextDistance = getDistance(nextEnemy.x, nextEnemy.y, centerPoint, centerPoint)

      if (nextDistance <= playerCollisionRadius + enemy.radius) {
        collidedEnemies.push(enemy)
        continue
      }

      nextEnemies.push(nextEnemy)
    }

    enemies.value = nextEnemies
    return collidedEnemies
  }

  function applyEnemyContacts(collidedEnemies) {
    if (collidedEnemies.length === 0) return

    let totalDamage = 0
    for (const enemy of collidedEnemies) {
      if (!enemy.boss) {
        totalDamage += COMBAT_BALANCE.collisionDamage
      }
    }

    if (totalDamage <= 0) return

    health.value = clamp(health.value - totalDamage, 0, maxHealth.value)
    energy.value = clamp(
      energy.value - COMBAT_BALANCE.collisionEnergyLossBase - collidedEnemies.length * COMBAT_BALANCE.collisionEnergyLossPerEnemy,
      0,
      maxEnergy.value
    )
    combo.value = 0
    damageCooldown.value = 0.22
    playerHitFeedback.value = 0.34
    banner.value = `${collidedEnemies[0].text} 已碰到角色，生命值下降。`
    syncTargetByBuffer()
  }

  function updateEnemyFeedbacks(deltaSeconds) {
    for (const enemy of enemies.value) {
      if (enemy.errorFeedback > 0) {
        enemy.errorFeedback = Math.max(0, enemy.errorFeedback - deltaSeconds)
      }
      if (enemy.hitFeedback > 0) {
        enemy.hitFeedback = Math.max(0, enemy.hitFeedback - deltaSeconds)
      }
      if (enemy.emitFeedback > 0) {
        enemy.emitFeedback = Math.max(0, enemy.emitFeedback - deltaSeconds)
      }
      if (enemy.wordTransitionState !== 'idle') {
        enemy.wordTransitionTimer = Math.max(0, enemy.wordTransitionTimer - deltaSeconds)

        if (enemy.wordTransitionState === 'fade-out' && enemy.wordTransitionTimer <= 0) {
          if (enemy.pendingWord) {
            enemy.text = enemy.pendingWord.text
            enemy.keyword = normalizeWord(enemy.pendingWord.word)
            enemy.displayWord = enemy.pendingWord.word.toLowerCase()
            enemy.pendingWord = null
            markEnemyKeywordTrieDirty()
          }
          enemy.wordTransitionState = 'fade-in'
          enemy.wordTransitionTimer = wordTransitionDuration
        } else if (enemy.wordTransitionState === 'fade-in' && enemy.wordTransitionTimer <= 0) {
          enemy.wordTransitionState = 'idle'
        }
      }
    }
  }

  function updateEnemyFragments(deltaSeconds) {
    enemyFragments.value = enemyFragments.value
      .map((fragment) => ({
        ...fragment,
        x: fragment.x + fragment.vx * deltaSeconds,
        y: fragment.y + fragment.vy * deltaSeconds,
        rotation: fragment.rotation + fragment.vr * deltaSeconds,
        life: fragment.life - deltaSeconds,
      }))
      .filter((fragment) => fragment.life > 0)
  }

  function updateComboFeedback(deltaSeconds) {
    comboFeedbackTimer.value = Math.max(0, comboFeedbackTimer.value - deltaSeconds)
    comboShakeTimer.value = Math.max(0, comboShakeTimer.value - deltaSeconds)
  }

  function updateKeyBursts(deltaSeconds) {
    keyBursts.value = keyBursts.value
      .map((burst) => ({
        ...burst,
        age: burst.age + deltaSeconds,
      }))
      .filter((burst) => burst.age < keyBurstDuration)
  }

  function spawnWaveEnemy() {
    if (waveSpawned.value >= waveTargetCount.value) return

    const normalProfile = currentWaveProfile.value.normal
    const spawnProbability = clamp(sampleRange(normalProfile.spawnProbabilityRange), 0, 1)

    if (Math.random() > spawnProbability) return

    const healthMultiplier = sampleRange(normalProfile.healthMultiplierRange)
    enemies.value = [...enemies.value, buildEnemy(wave.value, false, { healthMultiplier })]
    markEnemyKeywordTrieDirty()
    waveSpawned.value += 1
  }

  function spawnBossEnemy() {
    if (bossState.value !== 'pending') return
    if (bossSpawned.value >= bossTargetCount.value) {
      bossState.value = 'idle'
      return
    }
    if (waveSpawned.value < waveTargetCount.value || enemies.value.length > 0) return

    const bossProfile = currentWaveProfile.value.boss
    const spawnProbability = clamp(sampleRange(bossProfile.spawnProbabilityRange), 0, 1)

    if (Math.random() > spawnProbability) return

    const healthMultiplier = sampleRange(bossProfile.healthMultiplierRange)
    enemies.value = [...enemies.value, buildEnemy(wave.value, true, { healthMultiplier })]
    markEnemyKeywordTrieDirty()
    bossSpawned.value += 1
    bossState.value = 'active'
    bossMinionCooldown.value = BOSS_BALANCE.minionInitialDelay
    banner.value = `第 ${wave.value} 关首领已入场，优先锁定首领单词。`
  }

  function spawnBossMinion() {
    if (bossState.value !== 'active') return

    const bossEnemy = getBossEnemy()
    if (!bossEnemy) return

    const nonBossCount = enemies.value.filter((enemy) => !enemy.boss).length
    if (nonBossCount >= BOSS_BALANCE.maxMinions) return

    const emissionAngle = pickBossEmissionAngle(bossEnemy)
    const spawnPoint = {
      x: bossEnemy.x + Math.cos(emissionAngle) * BOSS_BALANCE.emissionDistance,
      y: bossEnemy.y + Math.sin(emissionAngle) * BOSS_BALANCE.emissionDistance,
    }

    bossEnemy.emitFeedback = BOSS_BALANCE.emissionDuration
    bossEnemy.emissionAngle = emissionAngle
    enemies.value = [...enemies.value, buildEnemy(Math.max(2, wave.value - 1), false, { spawnPoint, emissionVector: emissionAngle })]
    markEnemyKeywordTrieDirty()
  }

  function updateWaveProgress() {
    if (isVictory.value || isGameOver.value || isChoosingSkill.value) return
    if (bossState.value !== 'idle') return
    if (waveSpawned.value < waveTargetCount.value) return
    if (bossSpawned.value < bossTargetCount.value) return
    if (enemies.value.length > 0 || enemyFragments.value.length > 0) return

    markWaveCompleted(wave.value)
    if (wave.value >= getTypeWarriorFinalWave()) {
      isVictory.value = true
      banner.value = '最终首领已清除，战场恢复稳定。'
      return
    }

    openSkillSelection(wave.value + 1)
  }

  function updateLoop(frameAt) {
    const deltaSeconds = Math.min((frameAt - lastFrameAt) / 1000, 0.032) || 0.016
    lastFrameAt = frameAt

    if (hasGameStarted.value && !isGameOver.value && !isVictory.value && !isChoosingSkill.value && !isPaused.value) {
      survivalSeconds.value += deltaSeconds
      if (currentTarget.value || purgeWordState.value.active) {
        effectiveTypingSeconds.value += deltaSeconds
      }
      damageCooldown.value = Math.max(0, damageCooldown.value - deltaSeconds)
      playerHitFeedback.value = Math.max(0, playerHitFeedback.value - deltaSeconds)
      spawnCooldown.value = Math.max(0, spawnCooldown.value - deltaSeconds)
      bossSpawnCooldown.value = Math.max(0, bossSpawnCooldown.value - deltaSeconds)
      bossMinionCooldown.value = Math.max(0, bossMinionCooldown.value - deltaSeconds)
      purgeCooldownRemaining.value = Math.max(0, purgeCooldownRemaining.value - deltaSeconds)
      updateEnemyFeedbacks(deltaSeconds)
      updateEnemyFragments(deltaSeconds)
      updateComboFeedback(deltaSeconds)
      updateKeyBursts(deltaSeconds)

      if (spawnCooldown.value <= 0) {
        spawnWaveEnemy()
        spawnCooldown.value = getSpawnInterval(currentWaveProfile.value.normal.spawnRatePerSecond)
      }

      if (bossSpawnCooldown.value <= 0) {
        spawnBossEnemy()
        bossSpawnCooldown.value = getSpawnInterval(currentWaveProfile.value.boss.spawnRatePerSecond)
      }

      if (bossState.value === 'active' && bossMinionCooldown.value <= 0) {
        spawnBossMinion()
        bossMinionCooldown.value = BOSS_BALANCE.minionInterval
      }

      const collidedEnemies = moveEnemies(deltaSeconds)
      applyEnemyContacts(collidedEnemies)
      applyBulletHits(deltaSeconds)
      removeDefeatedEnemies()
      updateWaveProgress()

      if (health.value <= 0) {
        isGameOver.value = true
        banner.value = '文字防线已崩溃，请重新开始。'
      }
    }

    animationId = window.requestAnimationFrame(updateLoop)
  }

  function enemyStyle(enemy) {
    const directionX = centerPoint - enemy.x
    const directionY = centerPoint - enemy.y
    const distance = getDistance(enemy.x, enemy.y, centerPoint, centerPoint) || 1
    const unitX = directionX / distance
    const unitY = directionY / distance
    const tagOffset = enemy.boss ? 80 : 60
    const tagX = -unitX * tagOffset
    const tagY = -unitY * tagOffset
    const angle = Math.atan2(directionY, directionX) * (180 / Math.PI) + 90

    return {
      left: `${(enemy.x / arenaSize) * 100}%`,
      top: `${(enemy.y / arenaSize) * 100}%`,
      '--enemy-accent': enemy.accent,
      '--enemy-angle': `${angle}deg`,
      '--enemy-tag-x': `${tagX}px`,
      '--enemy-tag-y': `${tagY}px`,
      '--boss-emission-angle': `${enemy.emissionAngle ?? 0}rad`,
    }
  }

  function bulletStyle(bullet) {
    return {
      left: `${(bullet.x / arenaSize) * 100}%`,
      top: `${(bullet.y / arenaSize) * 100}%`,
    }
  }

  function enemyHealthStyle(enemy) {
    return {
      width: `${(enemy.health / enemy.maxHealth) * 100}%`,
    }
  }

  function enemyWordTransitionStyle(enemy) {
    if (enemy.wordTransitionState === 'fade-out') {
      return {
        opacity: `${enemy.wordTransitionTimer / wordTransitionDuration}`,
        transform: 'translateY(-2px) scale(0.985)',
      }
    }

    if (enemy.wordTransitionState === 'fade-in') {
      return {
        opacity: `${1 - enemy.wordTransitionTimer / wordTransitionDuration}`,
        transform: 'translateY(2px) scale(0.985)',
      }
    }

    return {
      opacity: '1',
      transform: 'translateY(0) scale(1)',
    }
  }

  function keyBurstStyle(burst) {
    return {
      '--burst-dx': `${burst.dx}px`,
      '--burst-dy': `${burst.dy}px`,
    }
  }

  function fragmentStyle(fragment) {
    const lifeRatio = Math.max(0, fragment.life / fragment.maxLife)
    return {
      left: `${(fragment.x / arenaSize) * 100}%`,
      top: `${(fragment.y / arenaSize) * 100}%`,
      width: `${fragment.size}px`,
      height: `${Math.max(4, fragment.size * 0.44)}px`,
      background: fragment.color,
      opacity: `${lifeRatio}`,
      transform: `translate(-50%, -50%) rotate(${fragment.rotation}deg) scale(${0.72 + lifeRatio * 0.6})`,
    }
  }

  onMounted(() => {
    restartGame()
    lastFrameAt = performance.now()
    updateViewportSpawnBounds()
    window.addEventListener('keydown', handleKeydown)
    window.addEventListener('resize', updateViewportSpawnBounds)
    animationId = window.requestAnimationFrame(updateLoop)
  })

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleKeydown)
    window.removeEventListener('resize', updateViewportSpawnBounds)
    window.cancelAnimationFrame(animationId)
  })

  return {
    arenaRef,
    boardClass,
    bullets,
    cards,
    combo,
    comboFeedbackCount,
    comboFeedbackTimer,
    currentTarget,
    enemies,
    enemyFragments,
    energy,
    hasGameStarted,
    health,
    isChoosingSkill,
    isCriticalHealth,
    isGameOver,
    isPaused,
    isVictory,
    keyBursts,
    purgeCooldownLabel,
    resultStats,
    skillChoices,
    banner,
    applySkillChoice,
    grantSkillById,
    hudStageHint,
    hudStageLabel,
    playerRingStyle,
    playerShellClass,
    restartGame,
    resetSkills,
    startGame,
    stageHint,
    stageLabel,
    survivalSeconds,
    togglePause,
    wave,
    weaponLevel,
    wpmLike,
    bulletStyle,
    enemyHealthStyle,
    enemyStyle,
    enemyWordTransitionStyle,
    fragmentStyle,
    getEnemyWordParts,
    keyBurstStyle,
  }
}


