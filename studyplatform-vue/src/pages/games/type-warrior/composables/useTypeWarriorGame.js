import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  TYPE_WARRIOR_BALANCE,
  TYPE_WARRIOR_CONFIG,
  TYPE_WARRIOR_ENEMY_KINDS,
  TYPE_WARRIOR_SKILL_POOL,
  TYPE_WARRIOR_WORD_BANK,
} from '../config/typeWarriorConfig'
import { fetchTypeWarriorWordPool } from '../../../../api/academy'
import { getTypeWarriorFinalWave, getTypeWarriorWaveProfile } from '../config/typeWarriorWaveConfig'
import { clamp, getDistance, normalizeWord, pickRandomItems, randomFrom } from '../utils/typeWarriorMath'
import { buildEnemyKeywordTrie, findBestTrieSuffixPrefixMatches } from '../utils/typeWarriorTrie'

const {
  arenaSize,
  blastEffectDuration,
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
const RECENT_WORD_COOLDOWN_SIZE = 18
const LOW_USAGE_SPREAD = 1

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
  const explosionEffects = ref([])
  const damageTexts = ref([])
  const cards = ref([])
  const isGameOver = ref(false)
  const isVictory = ref(false)
  const isChoosingSkill = ref(false)
  const isWordPoolLoading = ref(false)
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
  const matchedEnemyIds = ref(new Set())
  const playerHitFeedback = ref(0)
  const keyBursts = ref([])
  const comboFeedbackCount = ref(0)
  const comboFeedbackTimer = ref(0)
  const comboShakeTimer = ref(0)
  const explosionShakeTimer = ref(0)
  const isPaused = ref(false)
  const maxCombo = ref(0)
  const score = ref(0)
  const solvedWordCount = ref(0)
  const typedLetterCount = ref(0)
  const totalKillCount = ref(0)
  const completedWaveCount = ref(0)
  const effectiveTypingSeconds = ref(0)
  const purgeWordState = ref({
    active: false,
    word: '',
    text: '',
    buffer: '',
  })
  const freezeTimer = ref(0)
  const arenaRef = ref(null)
  const pendingWaveNumber = ref(null)
  const pendingWaveEndHeal = ref(0)
  const currentWaveProfile = ref(getTypeWarriorWaveProfile(1))
  const wordPools = ref(buildWordPools(TYPE_WARRIOR_WORD_BANK.map((item) => ({ ...item, familiarity: 'unmarked' }))))
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
  let explosionEffectIdSeed = 0
  let damageTextIdSeed = 0
  let wordPoolLoadPromise = null
  let hasPreloadedWordPool = false
  let recentWordQueue = []
  let wordUsageCounts = new Map()
  let enemyKeywordTrie = buildEnemyKeywordTrie([])
  let enemyKeywordTrieDirty = true

  const currentTarget = computed(() => enemies.value.find((enemy) => enemy.id === targetEnemyId.value) || null)
  const isCriticalHealth = computed(() => health.value < PLAYER_BALANCE.criticalHealthThreshold)
  const wpmLike = computed(() => Math.round(typingBurst.value * 8.2))
  const currentProjectileDamage = computed(() => {
    const passiveBonus = getSkillValue('focus', 'comboDamageBonusPerCombo')
    const overclockDamageBonus = getSkillValue('overclock', 'damageBonus')
    const burstBonus = getSkillValue('burst', 'projectileDamageBonus')
    const beamBonus = getSkillValue('beam', 'flatDamageBonus')

    return Math.round(
      COMBAT_BALANCE.baseDamage +
        weaponLevel.value * COMBAT_BALANCE.weaponDamagePerLevel +
        combo.value * (COMBAT_BALANCE.comboDamageScale + passiveBonus) +
        typingBurst.value * COMBAT_BALANCE.typingBurstScale +
        overclockDamageBonus +
        burstBonus +
        beamBonus
    )
  })
  const wordsPerSecond = computed(() => (effectiveTypingSeconds.value > 0 ? solvedWordCount.value / effectiveTypingSeconds.value : 0))
  const lettersPerSecond = computed(() => (effectiveTypingSeconds.value > 0 ? typedLetterCount.value / effectiveTypingSeconds.value : 0))
  const killsPerSecond = computed(() => (survivalSeconds.value > 0 ? totalKillCount.value / survivalSeconds.value : 0))
  const resultStats = computed(() => ({
    reachedWave: wave.value,
    maxCombo: maxCombo.value,
    score: Math.round(score.value),
    coins: Math.round(Math.round(score.value) / 100),
    completedWaves: completedWaveCount.value,
    solvedWords: solvedWordCount.value,
    typedLetters: typedLetterCount.value,
    totalKills: totalKillCount.value,
    durationSeconds: survivalSeconds.value,
    effectiveTypingSeconds: effectiveTypingSeconds.value,
    killsPerSecond: killsPerSecond.value,
    wordsPerSecond: wordsPerSecond.value,
    lettersPerSecond: lettersPerSecond.value,
  }))
  const purgeCooldownLabel = computed(() => {
    if (!hasSkill('purge')) return '未解锁'
    if (purgeWordState.value.active) return '输入中'
    if (energy.value < SKILL_BALANCE.purge.energyCost) return `能量不足(${SKILL_BALANCE.purge.energyCost})`
    return `消耗${SKILL_BALANCE.purge.energyCost}`
  })
  const freezeStatusLabel = computed(() => {
    if (!hasSkill('freeze')) return '未解锁'
    if (freezeTimer.value > 0) return `持续 ${freezeTimer.value.toFixed(1)}s`
    if (energy.value < SKILL_BALANCE.freeze.energyCost) return `能量不足(${SKILL_BALANCE.freeze.energyCost})`
    return `消耗${SKILL_BALANCE.freeze.energyCost}`
  })
  const playerShellClass = computed(() => ({
    'is-hit': playerHitFeedback.value > 0,
  }))
  const boardClass = computed(() => ({
    'is-combo-shake': comboShakeTimer.value > 0,
    'is-explosion-shake': explosionShakeTimer.value > 0,
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

  function getSkillMaxLevel(skillId) {
    const config = SKILL_BALANCE[skillId]
    if (!config) return 1

    const arrayLevels = Object.values(config)
      .filter((value) => Array.isArray(value))
      .map((value) => Math.max(0, value.length - 1))

    if (arrayLevels.length === 0) return 1
    return Math.max(...arrayLevels)
  }

  function isSkillAtMaxLevel(skillId, level = getSkillLevel(skillId)) {
    return level >= getSkillMaxLevel(skillId)
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
    const nextMaxHealth =
      PLAYER_BALANCE.baseHealth +
      getSkillValue('shield', 'maxHealthBonus') +
      getSkillValue('lifelong', 'maxHealthBonus')
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

  function triggerExplosionShake() {
    explosionShakeTimer.value = COMBAT_BALANCE.explosionShakeDuration
  }

  function createExplosionEffect(x, y, radius) {
    explosionEffects.value = [
      ...explosionEffects.value,
      {
        id: `explosion-${explosionEffectIdSeed++}`,
        x,
        y,
        radius,
        life: blastEffectDuration,
        maxLife: blastEffectDuration,
      },
    ]
  }

  function createDamageText(x, y, damage, source = 'bullet') {
    const roundedDamage = Math.max(1, Math.round(damage))
    const angle = -Math.PI / 2 + (Math.random() - 0.5) * 0.7
    const distance = 20 + Math.random() * 14

    damageTexts.value = [
      ...damageTexts.value,
      {
        id: `damage-${damageTextIdSeed++}`,
        x,
        y,
        dx: Math.cos(angle) * distance,
        dy: Math.sin(angle) * distance - 8,
        value: roundedDamage,
        source,
        life: 0.56,
        maxLife: 0.56,
      },
    ]
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

  function normalizeWordStatus(status) {
    return ['unknown', 'unmarked', 'fuzzy', 'known'].includes(status) ? status : 'unmarked'
  }

  function buildWordPools(words) {
    const pools = {
      unknown: [],
      unmarked: [],
      fuzzy: [],
      known: [],
      all: [],
    }

    for (const word of words) {
      const normalizedKeyword = normalizeWord(word.word)
      if (!normalizedKeyword) continue

      const normalizedStatus = normalizeWordStatus(word.familiarity)
      const normalizedEntry = {
        text: word.text || normalizedKeyword,
        word: normalizedKeyword,
        tier: Math.max(1, Math.min(4, Number(word.tier) || 1)),
        familiarity: normalizedStatus,
      }

      pools[normalizedStatus].push(normalizedEntry)
      pools.all.push(normalizedEntry)
    }

    return pools
  }

  function resetWordSelectionState() {
    recentWordQueue = []
    wordUsageCounts = new Map()
  }

  function markWordRecentlyUsed(word) {
    const keyword = normalizeWord(word?.word)
    if (!keyword) return

    recentWordQueue = recentWordQueue.filter((item) => item !== keyword)
    recentWordQueue.push(keyword)
    if (recentWordQueue.length > RECENT_WORD_COOLDOWN_SIZE) {
      recentWordQueue = recentWordQueue.slice(-RECENT_WORD_COOLDOWN_SIZE)
    }
    wordUsageCounts.set(keyword, (wordUsageCounts.get(keyword) ?? 0) + 1)
  }

  function getActiveKeywordSet() {
    return new Set(enemies.value.map((enemy) => enemy.keyword))
  }

  function filterWordCandidates(candidates, { excludeRecent = true, excludeActive = true } = {}) {
    const activeKeywordSet = excludeActive ? getActiveKeywordSet() : null
    const recentKeywordSet = excludeRecent ? new Set(recentWordQueue) : null

    return candidates.filter((candidate) => {
      if (excludeActive && activeKeywordSet?.has(candidate.word)) return false
      if (excludeRecent && recentKeywordSet?.has(candidate.word)) return false
      return true
    })
  }

  function pickWordFromCandidates(candidates) {
    if (candidates.length === 0) return null

    let minUsage = Number.POSITIVE_INFINITY
    for (const candidate of candidates) {
      minUsage = Math.min(minUsage, wordUsageCounts.get(candidate.word) ?? 0)
    }

    const lowUsageCandidates = candidates.filter((candidate) => (wordUsageCounts.get(candidate.word) ?? 0) <= minUsage + LOW_USAGE_SPREAD)
    return randomFrom(lowUsageCandidates.length > 0 ? lowUsageCandidates : candidates)
  }

  async function loadWordPool(force = false) {
    if (!force && hasPreloadedWordPool) {
      return wordPools.value
    }
    if (!force && wordPoolLoadPromise) {
      return wordPoolLoadPromise
    }

    isWordPoolLoading.value = true
    wordPoolLoadPromise = fetchTypeWarriorWordPool()
      .then((response) => {
        const remoteWords = Array.isArray(response?.words) ? response.words : []
        if (remoteWords.length > 0) {
          wordPools.value = buildWordPools(remoteWords)
        }
        hasPreloadedWordPool = true
        return wordPools.value
      })
      .catch((error) => {
        console.warn('failed to load type warrior word pool from database:', error)
        hasPreloadedWordPool = true
        return wordPools.value
      })
      .finally(() => {
        isWordPoolLoading.value = false
        wordPoolLoadPromise = null
      })

    return wordPoolLoadPromise
  }

  function getWaveWordTier(currentWave) {
    if (currentWave >= 5) return 4
    if (currentWave >= 4) return 3
    if (currentWave >= 2) return 2
    return 1
  }

  function getWordLengthTier(word) {
    const length = normalizeWord(word).length
    const scaling = ENEMY_BALANCE.wordLengthScaling

    if (length <= scaling.shortMaxLength) return 'short'
    if (length <= scaling.mediumMaxLength) return 'medium'
    if (length <= scaling.longMaxLength) return 'long'
    return 'extraLong'
  }

  function pickWeightedStatus(availablePools, weights) {
    const entries = Object.entries(weights || {})
      .map(([status, weight]) => ({
        status,
        weight: Math.max(0, Number(weight) || 0),
      }))
      .filter((entry) => entry.weight > 0 && availablePools[entry.status]?.length)

    if (entries.length === 0) return null

    const totalWeight = entries.reduce((sum, entry) => sum + entry.weight, 0)
    let randomValue = Math.random() * totalWeight
    for (const entry of entries) {
      randomValue -= entry.weight
      if (randomValue <= 0) {
        return entry.status
      }
    }
    return entries[entries.length - 1]?.status ?? null
  }

  function dedupeWordCandidates(candidates) {
    const seen = new Set()
    return candidates.filter((candidate) => {
      if (seen.has(candidate.word)) return false
      seen.add(candidate.word)
      return true
    })
  }

  function pickWordForWave(currentWave) {
    const tier = getWaveWordTier(currentWave)
    const tierPools = {
      unknown: wordPools.value.unknown.filter((item) => item.tier <= tier),
      unmarked: wordPools.value.unmarked.filter((item) => item.tier <= tier),
      fuzzy: wordPools.value.fuzzy.filter((item) => item.tier <= tier),
      known: wordPools.value.known.filter((item) => item.tier <= tier),
    }
    const filteredPools = {
      unknown: filterWordCandidates(tierPools.unknown),
      unmarked: filterWordCandidates(tierPools.unmarked),
      fuzzy: filterWordCandidates(tierPools.fuzzy),
      known: filterWordCandidates(tierPools.known),
    }
    const relaxedRecentPools = {
      unknown: filterWordCandidates(tierPools.unknown, { excludeRecent: false }),
      unmarked: filterWordCandidates(tierPools.unmarked, { excludeRecent: false }),
      fuzzy: filterWordCandidates(tierPools.fuzzy, { excludeRecent: false }),
      known: filterWordCandidates(tierPools.known, { excludeRecent: false }),
    }
    const selectedStatus =
      pickWeightedStatus(filteredPools, currentWaveProfile.value.wordFamiliarityWeights) ??
      pickWeightedStatus(relaxedRecentPools, currentWaveProfile.value.wordFamiliarityWeights) ??
      pickWeightedStatus(tierPools, currentWaveProfile.value.wordFamiliarityWeights)

    const selectedCandidates = selectedStatus
      ? filteredPools[selectedStatus].length > 0
        ? filteredPools[selectedStatus]
        : relaxedRecentPools[selectedStatus].length > 0
          ? relaxedRecentPools[selectedStatus]
          : tierPools[selectedStatus]
      : []

    const fallbackCandidates = dedupeWordCandidates([
      ...filteredPools.unknown,
      ...filteredPools.unmarked,
      ...filteredPools.fuzzy,
      ...filteredPools.known,
      ...relaxedRecentPools.unknown,
      ...relaxedRecentPools.unmarked,
      ...relaxedRecentPools.fuzzy,
      ...relaxedRecentPools.known,
      ...tierPools.unknown,
      ...tierPools.unmarked,
      ...tierPools.fuzzy,
      ...tierPools.known,
    ])

    const pickedWord = pickWordFromCandidates(selectedCandidates) || pickWordFromCandidates(fallbackCandidates)
    return pickedWord || randomFrom(TYPE_WARRIOR_WORD_BANK)
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
    markWordRecentlyUsed(word)
    const normalizedKeyword = normalizeWord(word.word)
    const wordLengthTier = getWordLengthTier(normalizedKeyword)
    const kind = boss
      ? { type: 'boss', shape: 'boss', baseHealth: BOSS_BALANCE.baseHealth, baseSpeed: BOSS_BALANCE.baseSpeed, accent: BOSS_BALANCE.accent }
      : randomFrom(TYPE_WARRIOR_ENEMY_KINDS)
    const spawnPoint = options.spawnPoint ?? createSpawnPosition()
    const healthMultiplier = options.healthMultiplier ?? 1
    const baseHealth = boss ? kind.baseHealth + currentWave * BOSS_BALANCE.healthPerWave : kind.baseHealth + currentWave * ENEMY_BALANCE.healthPerWave
    const baseSpeed = boss ? kind.baseSpeed + currentWave * BOSS_BALANCE.speedPerWave : kind.baseSpeed + currentWave * ENEMY_BALANCE.speedPerWave
    const speedMultiplier = ENEMY_BALANCE.wordLengthScaling.speedMultiplier[wordLengthTier] ?? 1
    const contactDamageMultiplier = ENEMY_BALANCE.wordLengthScaling.contactDamageMultiplier[wordLengthTier] ?? 1
    const orbitAngle = Math.atan2(spawnPoint.y - centerPoint, spawnPoint.x - centerPoint)
    const emissionVector = options.emissionVector ?? null
    const scaledHealth = Math.max(1, Math.round(baseHealth * healthMultiplier))
    const contactDamage = boss ? 0 : Math.max(1, Math.round(COMBAT_BALANCE.collisionDamage * contactDamageMultiplier))

    return {
      id: `enemy-${enemyIdSeed++}`,
      text: word.text,
      keyword: normalizedKeyword,
      displayWord: word.word.toLowerCase(),
      x: spawnPoint.x,
      y: spawnPoint.y,
      type: kind.type,
      shape: kind.shape,
      radius: boss ? BOSS_BALANCE.radius : kind.shape === 'dot' ? ENEMY_BALANCE.dotRadius : ENEMY_BALANCE.shapedRadius,
      health: scaledHealth,
      maxHealth: scaledHealth,
      speed: baseSpeed * speedMultiplier,
      contactDamage,
      wordLengthTier,
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
    if (energy.value < SKILL_BALANCE.purge.energyCost) {
      banner.value = `清屏指令需要 ${SKILL_BALANCE.purge.energyCost} 点能量。`
      return
    }

    const nextWord = pickWordForWave(Math.max(1, wave.value))
    typedBuffer.value = ''
    selectedMatchLength.value = 0
    targetEnemyId.value = null
    purgeWordState.value = {
      active: true,
      word: normalizeWord(nextWord.word),
      text: nextWord.text,
      buffer: '',
    }
    matchedEnemyIds.value = new Set()
    banner.value = `清屏指令已激活：${nextWord.word.toLowerCase()} / ${nextWord.text}`
  }

  function triggerPurgeSkill() {
    for (const enemy of enemies.value) {
      if (enemy.boss) continue

      enemy.health = 0
      enemy.incomingDamage = 0
      enemy.hitFeedback = 0.22
      enemy.deathSource = 'purge'
      enemy.deathSourceDamage = 0
    }

    resetPurgeWordState()
    typedBuffer.value = ''
    selectedMatchLength.value = 0
    targetEnemyId.value = null
    matchedEnemyIds.value = new Set()
    energy.value = clamp(energy.value - SKILL_BALANCE.purge.energyCost, 0, maxEnergy.value)
    banner.value = '清屏指令已执行。'
  }

  function activateFreezeSkill() {
    if (!hasSkill('freeze') || isChoosingSkill.value || isGameOver.value || isVictory.value) return
    if (freezeTimer.value > 0) {
      banner.value = '冰冻仍在持续中。'
      return
    }
    if (energy.value < SKILL_BALANCE.freeze.energyCost) {
      banner.value = `冰冻需要 ${SKILL_BALANCE.freeze.energyCost} 点能量。`
      return
    }

    energy.value = clamp(energy.value - SKILL_BALANCE.freeze.energyCost, 0, maxEnergy.value)
    freezeTimer.value = getSkillValue('freeze', 'duration')
    banner.value = '冰冻已释放，敌人移动与刷怪节奏已减速。'
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

  function applyDamageToEnemy(enemy, damage, { refreshWord = false, source = 'bullet', sourceDamage = damage } = {}) {
    if (damage > 0) {
      createDamageText(enemy.x, enemy.y, damage, source)
    }

    enemy.health -= damage
    enemy.incomingDamage = Math.max(0, enemy.incomingDamage - damage)
    enemy.hitFeedback = 0.22
    enemy.lastDamageSource = source
    enemy.lastSourceDamage = sourceDamage
    if (enemy.health <= 0) {
      enemy.deathSource = source
      enemy.deathSourceDamage = sourceDamage
    }
    if (refreshWord && enemy.health > 0) {
      triggerEnemyWordRefresh(enemy)
    }
  }

  function triggerBlastExplosion(defeatedEnemy) {
    const blastLevel = getSkillLevel('blast')
    if (blastLevel <= 0) return
    if (defeatedEnemy.deathSource !== 'bullet') return

    const radius = getSkillValue('blast', 'radius')
    const damageMultiplier = getSkillValue('blast', 'damageMultiplier')
    const minimumDamageRatio = getSkillValue('blast', 'minimumDamageRatio')
    const baseDamage = Math.max(0, Number(defeatedEnemy.deathSourceDamage) || 0)

    if (radius <= 0 || baseDamage <= 0) return

    createExplosionEffect(defeatedEnemy.x, defeatedEnemy.y, radius)
    triggerExplosionShake()

    for (const enemy of enemies.value) {
      if (enemy.id === defeatedEnemy.id) continue
      if (enemy.health <= 0) continue

      const distance = getDistance(defeatedEnemy.x, defeatedEnemy.y, enemy.x, enemy.y)
      if (distance > radius) continue

      const distanceRatio = clamp(1 - distance / radius, 0, 1)
      const damageRatio = minimumDamageRatio + (1 - minimumDamageRatio) * distanceRatio
      const explosionDamage = baseDamage * damageMultiplier * damageRatio
      if (explosionDamage <= 0) continue

      applyDamageToEnemy(enemy, explosionDamage, {
        refreshWord: false,
        source: 'explosion',
        sourceDamage: baseDamage,
      })
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
    markWordRecentlyUsed(enemy.pendingWord)
    enemy.wordTransitionState = 'fade-out'
    enemy.wordTransitionTimer = wordTransitionDuration
  }

  function buildSkillChoice(skill, mode) {
    const currentLevel = getSkillLevel(skill.id)
    const nextLevel = mode === 'new' ? 1 : currentLevel + 1
    const maxLevel = getSkillMaxLevel(skill.id)

    return {
      choiceId: `${skill.id}-${mode}-${nextLevel}`,
      id: skill.id,
      name: skill.name,
      type: skill.type,
      description: skill.description,
      level: nextLevel,
      maxLevel,
      mode,
      badge: mode === 'new' ? '新技能' : nextLevel >= maxLevel ? `等级 ${nextLevel} / 满级` : `等级 ${nextLevel}`,
    }
  }

  function buildSkillChoices() {
    const currentIds = new Set(cards.value.map((card) => card.id))
    const choices = []
    const newSkills = TYPE_WARRIOR_SKILL_POOL.filter((skill) => !currentIds.has(skill.id))
    const upgradeSkills = cards.value
      .filter((card) => !isSkillAtMaxLevel(card.id, card.level))
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
      const fallbackSkills = TYPE_WARRIOR_SKILL_POOL.filter((skill) => {
        if (choices.some((choice) => choice.id === skill.id)) return false
        const currentLevel = getSkillLevel(skill.id)
        return currentLevel === 0 || !isSkillAtMaxLevel(skill.id, currentLevel)
      })
      for (const skill of pickRandomItems(fallbackSkills, 3 - choices.length)) {
        const mode = currentIds.has(skill.id) ? 'upgrade' : 'new'
        choices.push(buildSkillChoice(skill, mode))
      }
    }

    return choices.slice(0, 3)
  }

  function openSkillSelection(nextWave) {
    const nextChoices = buildSkillChoices()
    if (nextChoices.length === 0) {
      pendingWaveNumber.value = null
      isChoosingSkill.value = false
      wave.value = nextWave
      weaponLevel.value = clamp(weaponLevel.value + COMBAT_BALANCE.skillChoiceWeaponLevelGain, 1, COMBAT_BALANCE.maxWeaponLevel)
      banner.value = `第 ${nextWave} 关开始。`
      startWave(nextWave)
      if (pendingWaveEndHeal.value > 0) {
        health.value = clamp(health.value + pendingWaveEndHeal.value, 0, maxHealth.value)
      }
      pendingWaveEndHeal.value = 0
      return
    }

    pendingWaveNumber.value = nextWave
    skillChoices.value = nextChoices
    isChoosingSkill.value = true
    typedBuffer.value = ''
    selectedMatchLength.value = 0
    targetEnemyId.value = null
    matchedEnemyIds.value = new Set()
    resetPurgeWordState()
    banner.value = `第 ${nextWave} 关即将开始，请先选择一项技能。`
  }

  function refreshSkillChoices() {
    if (!isChoosingSkill.value) return false
    const nextChoices = buildSkillChoices()
    if (nextChoices.length === 0) {
      banner.value = '当前没有可刷新的技能候选。'
      return false
    }
    skillChoices.value = nextChoices
    banner.value = '技能候选已刷新，请重新选择。'
    return true
  }

  function togglePause() {
    if (!hasGameStarted.value || isGameOver.value || isVictory.value || isChoosingSkill.value) return
    isPaused.value = !isPaused.value
    banner.value = isPaused.value ? '游戏已暂停。' : '游戏继续。'
  }

  function endGame() {
    if (!hasGameStarted.value || isGameOver.value || isVictory.value) return
    isPaused.value = false
    isChoosingSkill.value = false
    isGameOver.value = true
    typedBuffer.value = ''
    selectedMatchLength.value = 0
    targetEnemyId.value = null
    matchedEnemyIds.value = new Set()
    resetPurgeWordState()
    banner.value = '本局已主动结束，数据已进入结算。'
  }

  async function startGame() {
    await loadWordPool()
    hasGameStarted.value = true
    restartGame()
    banner.value = '游戏开始，保持节奏清理词潮。'
  }

  function applySkillChoice(choice) {
    const existingIndex = cards.value.findIndex((card) => card.id === choice.id)

    if (existingIndex >= 0) {
      if (isSkillAtMaxLevel(choice.id, cards.value[existingIndex].level)) {
        banner.value = `${choice.name} 已满级。`
        return
      }
      const nextCards = [...cards.value]
      nextCards[existingIndex] = {
        ...nextCards[existingIndex],
        level: nextCards[existingIndex].level + 1,
      }
      cards.value = nextCards
    } else {
      const skill = TYPE_WARRIOR_SKILL_POOL.find((item) => item.id === choice.id)
      if (skill) {
        cards.value = [...cards.value, { ...skill, level: 1, maxLevel: getSkillMaxLevel(skill.id) }]
      }
    }

    syncDerivedStats({ restoreHealth: true, restoreEnergy: true })

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
      if (pendingWaveEndHeal.value > 0) {
        health.value = clamp(health.value + pendingWaveEndHeal.value, 0, maxHealth.value)
      }
    }

    pendingWaveEndHeal.value = 0
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
      if (isSkillAtMaxLevel(skillId, cards.value[existingIndex].level)) {
        banner.value = `${skill.name} 已满级。`
        return
      }
      const nextCards = [...cards.value]
      nextCards[existingIndex] = {
        ...nextCards[existingIndex],
        level: nextCards[existingIndex].level + 1,
      }
      cards.value = nextCards
    } else {
      cards.value = [...cards.value, { ...skill, level: 1, maxLevel: getSkillMaxLevel(skill.id) }]
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

  /**
   * Debug-only wave jump. It keeps the current debug skills, but rebuilds the
   * combat scene so old enemies, bullets, input buffers, and overlays cannot
   * leak into the selected wave.
   */
  async function debugSelectWave(waveNumber) {
    const finalWave = getTypeWarriorFinalWave()
    const selectedWave = clamp(Math.floor(Number(waveNumber) || 1), 1, finalWave)

    await loadWordPool()
    resetWordSelectionState()
    hasGameStarted.value = true
    wave.value = selectedWave
    combo.value = 0
    typedBuffer.value = ''
    selectedMatchLength.value = 0
    freezeTimer.value = 0
    enemies.value = []
    bullets.value = []
    enemyFragments.value = []
    explosionEffects.value = []
    damageTexts.value = []
    isGameOver.value = false
    isVictory.value = false
    isChoosingSkill.value = false
    skillChoices.value = []
    damageCooldown.value = 0
    lastTypedAt.value = performance.now()
    typingBurst.value = 0
    survivalSeconds.value = 0
    targetEnemyId.value = null
    matchedEnemyIds.value = new Set()
    playerHitFeedback.value = 0
    keyBursts.value = []
    comboFeedbackCount.value = 0
    comboFeedbackTimer.value = 0
    comboShakeTimer.value = 0
    explosionShakeTimer.value = 0
    isPaused.value = false
    maxCombo.value = 0
    score.value = 0
    solvedWordCount.value = 0
    typedLetterCount.value = 0
    totalKillCount.value = 0
    completedWaveCount.value = Math.max(0, selectedWave - 1)
    effectiveTypingSeconds.value = 0
    pendingWaveNumber.value = null
    pendingWaveEndHeal.value = 0
    resetPurgeWordState()
    syncDerivedStats({ restoreHealth: true, restoreEnergy: true })
    markEnemyKeywordTrieDirty()
    startWave(selectedWave)
    banner.value = `调试模式：已切换到第 ${selectedWave} 关。`
  }

  function startWave(currentWave) {
    const waveProfile = getTypeWarriorWaveProfile(currentWave)

    currentWaveProfile.value = waveProfile
    waveSpawned.value = 0
    waveTargetCount.value = waveProfile.normal.totalCount
    bossSpawned.value = 0
    bossTargetCount.value = waveProfile.boss.totalCount
    spawnCooldown.value = getSpawnInterval(waveProfile.normal.spawnRatePerSecond)
    bossSpawnCooldown.value = hasConfiguredBosses(waveProfile) ? 0 : Number.POSITIVE_INFINITY
    bossMinionCooldown.value = hasConfiguredBosses(waveProfile) ? BOSS_BALANCE.minionInitialDelay : 0
    bossState.value = hasConfiguredBosses(waveProfile) ? 'pending' : 'idle'
    banner.value = hasConfiguredBosses(waveProfile)
      ? `第 ${currentWave} 关首领逼近，准备应对压场与吐词。`
      : `第 ${currentWave} 关开始，保持节奏清理词潮。`
  }

  function restartGame() {
    resetWordSelectionState()
    wave.value = 1
    weaponLevel.value = 1
    health.value = PLAYER_BALANCE.baseHealth
    maxHealth.value = PLAYER_BALANCE.baseHealth
    energy.value = PLAYER_BALANCE.baseEnergy
    maxEnergy.value = PLAYER_BALANCE.baseMaxEnergy
    combo.value = 0
    typedBuffer.value = ''
    selectedMatchLength.value = 0
    freezeTimer.value = 0
    enemies.value = []
    markEnemyKeywordTrieDirty()
    bullets.value = []
    enemyFragments.value = []
    explosionEffects.value = []
    damageTexts.value = []
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
    matchedEnemyIds.value = new Set()
    playerHitFeedback.value = 0
    keyBursts.value = []
    comboFeedbackCount.value = 0
    comboFeedbackTimer.value = 0
    comboShakeTimer.value = 0
    explosionShakeTimer.value = 0
    isPaused.value = false
    maxCombo.value = 0
    score.value = 0
    solvedWordCount.value = 0
    typedLetterCount.value = 0
    totalKillCount.value = 0
    completedWaveCount.value = 0
    effectiveTypingSeconds.value = 0
    pendingWaveNumber.value = null
    pendingWaveEndHeal.value = 0
    currentWaveProfile.value = getTypeWarriorWaveProfile(1)
    markEnemyKeywordTrieDirty()
    resetPurgeWordState()
    banner.value = '直接输入屏幕内可见敌人的英文单词即可自动锁定目标。'
    syncDerivedStats()
    startWave(1)
  }

  function reviveGame() {
    if (!isGameOver.value || isVictory.value) return false
    isGameOver.value = false
    isPaused.value = false
    health.value = maxHealth.value
    typedBuffer.value = ''
    selectedMatchLength.value = 0
    targetEnemyId.value = null
    matchedEnemyIds.value = new Set()
    damageCooldown.value = 2
    playerHitFeedback.value = 0
    resetPurgeWordState()
    banner.value = '已使用复活券，生命恢复并继续战斗。'
    return true
  }

  function getBossEnemy() {
    const aliveBosses = enemies.value.filter((enemy) => enemy.boss && enemy.health > 0)
    return randomFrom(aliveBosses) ?? null
  }

  function getAliveBossCount() {
    return enemies.value.filter((enemy) => enemy.boss && enemy.health > 0).length
  }

  function syncBossState() {
    if (getAliveBossCount() > 0) {
      bossState.value = 'active'
      return
    }
    bossState.value = bossSpawned.value < bossTargetCount.value ? 'pending' : 'idle'
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

  function findPrefixMatchEntries(buffer, visibleOnly = true) {
    if (!buffer) {
      return {
        matches: [],
        matchLength: 0,
      }
    }

    ensureEnemyKeywordTrie()
    const enemyById = new Map(enemies.value.map((enemy) => [enemy.id, enemy]))

    const result = findBestTrieSuffixPrefixMatches(enemyKeywordTrie, buffer, (enemyId, matchLength) => {
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

    return {
      matchLength: result.matchLength,
      matches: result.matches.sort((left, right) => getEnemyDistance(left.enemy) - getEnemyDistance(right.enemy)),
    }
  }

  function syncTargetByBuffer(preferredId = targetEnemyId.value) {
    const buffer = typedBuffer.value
    if (!buffer) {
      targetEnemyId.value = null
      selectedMatchLength.value = 0
      matchedEnemyIds.value = new Set()
      return null
    }

    const prefixResult = findPrefixMatchEntries(buffer, true)
    matchedEnemyIds.value = new Set(prefixResult.matches.map((entry) => entry.enemy.id))
    selectedMatchLength.value = prefixResult.matchLength

    if (preferredId && matchedEnemyIds.value.has(preferredId)) {
      targetEnemyId.value = preferredId
      const preferredEnemy = enemies.value.find((enemy) => enemy.id === preferredId)
      return preferredEnemy
        ? {
            enemy: preferredEnemy,
            matchLength: prefixResult.matchLength,
          }
        : null
    }

    const nextEntry = prefixResult.matches[0] ?? null
    targetEnemyId.value = nextEntry?.enemy.id ?? null
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
    if (!matchedEnemyIds.value.has(enemy.id)) return 0
    return selectedMatchLength.value
  }

  function getEnemyWordParts(enemy) {
    const matchLength = getCurrentTargetMatchLength(enemy)
    return {
      matched: enemy.displayWord.slice(0, matchLength),
      rest: enemy.displayWord.slice(matchLength),
    }
  }

  function getPurgeWordParts() {
    const matchLength = purgeWordState.value.buffer.length
    return {
      matched: purgeWordState.value.word.slice(0, matchLength),
      rest: purgeWordState.value.word.slice(matchLength),
    }
  }

  function pickRandomAliveEnemy(excludeIds = []) {
    const excludeIdSet = new Set(excludeIds)
    const candidates = enemies.value.filter((enemy) => enemy.health > 0 && !excludeIdSet.has(enemy.id))
    return randomFrom(candidates)
  }

  function getBulletDamageSource(bullet) {
    if (bullet.bulletKind === 'echo') return 'echo'
    if (bullet.bulletKind === 'split') return 'split'
    return 'bullet'
  }

  function spawnEchoSeekers(defeatedEnemy) {
    const echoCount = getSkillValue('echo', 'killSeekers')
    if (echoCount <= 0) return
    if (defeatedEnemy.deathSource !== 'bullet') return

    const damageMultiplier = getSkillValue('echo', 'damageMultiplier')
    const speedMultiplier = getSkillValue('echo', 'speedMultiplier')
    const baseDamage = Math.max(0, Number(defeatedEnemy.deathSourceDamage) || Number(defeatedEnemy.lastSourceDamage) || 0)
    if (baseDamage <= 0) return

    for (let index = 0; index < echoCount; index += 1) {
      const target = pickRandomAliveEnemy([defeatedEnemy.id])
      if (!target) break

      bullets.value.push({
        id: `bullet-${bulletIdSeed++}`,
        bulletKind: 'echo',
        targetId: target.id,
        x: defeatedEnemy.x,
        y: defeatedEnemy.y,
        lastX: defeatedEnemy.x,
        lastY: defeatedEnemy.y,
        damage: baseDamage * damageMultiplier,
        speed: COMBAT_BALANCE.baseProjectileSpeed * speedMultiplier + index * 18,
        trail: 0,
        directionX: 0,
        directionY: -1,
        solidTrailEnabled: false,
        solidTrailMultiplier: 0,
        pierceEnabled: false,
        pierceTrailMultiplier: 0,
        refreshesWordOnHit: false,
        piercedEnemyIds: [],
        solidHitEnemyIds: [],
        flightMode: 'tracking',
        lifetime: 0,
      })
    }
  }

  function spawnSplitBullets(sourceBullet, hitEnemy) {
    const splitCount = getSkillValue('split', 'childCount')
    if (splitCount <= 0 || !sourceBullet.canSplit) return

    const speedMultiplier = getSkillValue('split', 'speedMultiplier')
    const splitLifetime = getSkillValue('split', 'lifetime')
    const angleStepDegrees = getSkillValue('split', 'angleStepDegrees')
    const baseDirectionX = sourceBullet.directionX || 1
    const baseDirectionY = sourceBullet.directionY || 0
    const baseAngle = Math.atan2(baseDirectionY, baseDirectionX)
    const angleStep = (angleStepDegrees * Math.PI) / 180

    for (let index = 0; index < splitCount; index += 1) {
      const angleOffset = (index - (splitCount - 1) / 2) * angleStep
      const nextAngle = baseAngle + angleOffset

      bullets.value.push({
        id: `bullet-${bulletIdSeed++}`,
        bulletKind: 'split',
        canSplit: false,
        targetId: null,
        x: hitEnemy.x,
        y: hitEnemy.y,
        lastX: hitEnemy.x,
        lastY: hitEnemy.y,
        damage: sourceBullet.damage * 0.7,
        speed: sourceBullet.speed * speedMultiplier + index * 12,
        trail: 0,
        directionX: Math.cos(nextAngle),
        directionY: Math.sin(nextAngle),
        solidTrailEnabled: sourceBullet.solidTrailEnabled,
        solidTrailMultiplier: sourceBullet.solidTrailMultiplier ?? 0,
        pierceEnabled: sourceBullet.pierceEnabled,
        pierceTrailMultiplier: sourceBullet.pierceTrailMultiplier ?? 0,
        refreshesWordOnHit: false,
        piercedEnemyIds: [hitEnemy.id],
        solidHitEnemyIds: [hitEnemy.id],
        flightMode: 'straight',
        lifetime: splitLifetime,
      })
    }
  }

  function triggerComboFeedback() {
    comboFeedbackCount.value = combo.value
    comboFeedbackTimer.value = 0.72
    comboShakeTimer.value = 0.36
  }

  function getStandardProjectileTraits() {
    const splitEnabled = getSkillValue('split', 'childCount') > 0
    const solidBulletEnabled = hasSkill('solid')
    const piercingBulletEnabled = hasSkill('pierce')

    return {
      splitEnabled,
      solidTrailEnabled: solidBulletEnabled,
      solidTrailMultiplier: solidBulletEnabled ? getSkillValue('solid', 'trailDamageMultiplier') : 0,
      pierceEnabled: piercingBulletEnabled,
      pierceTrailMultiplier: piercingBulletEnabled ? getSkillValue('pierce', 'trailDamageMultiplier') : 0,
      piercingBulletLifetime: piercingBulletEnabled ? getSkillValue('pierce', 'lifetime') : 0,
      overclockSpeedBonus: getSkillValue('overclock', 'projectileSpeedBonus'),
    }
  }

  function appendTrackingBullet(target, {
    damage,
    trail = 0,
    bulletKind = 'standard',
    canSplit = false,
    solidTrailEnabled = false,
    solidTrailMultiplier = 0,
    pierceEnabled = false,
    pierceTrailMultiplier = 0,
    piercingBulletLifetime = 0,
    overclockSpeedBonus = 0,
    refreshesWordOnHit = false,
  }) {
    const initialDistance = getDistance(centerPoint, centerPoint, target.x, target.y) || 1
    const initialDirectionX = (target.x - centerPoint) / initialDistance
    const initialDirectionY = (target.y - centerPoint) / initialDistance

    bullets.value.push({
      id: `bullet-${bulletIdSeed++}`,
      bulletKind,
      canSplit,
      targetId: target.id,
      x: centerPoint,
      y: centerPoint,
      lastX: centerPoint,
      lastY: centerPoint,
      damage,
      speed: COMBAT_BALANCE.baseProjectileSpeed + trail * COMBAT_BALANCE.projectileSpeedStep + overclockSpeedBonus,
      trail,
      directionX: initialDirectionX,
      directionY: initialDirectionY,
      solidTrailEnabled,
      solidTrailMultiplier,
      pierceEnabled,
      pierceTrailMultiplier,
      refreshesWordOnHit,
      piercedEnemyIds: [],
      solidHitEnemyIds: [],
      flightMode: 'tracking',
      lifetime: piercingBulletLifetime,
    })

    target.incomingDamage += damage
  }

  function getCounterGuardTargets(projectileCount) {
    const aliveTargets = enemies.value
      .filter((enemy) => enemy.health > 0 && getEnemyEffectiveHealth(enemy) > 0)
      .sort((left, right) => getEnemyDistance(left) - getEnemyDistance(right))

    if (aliveTargets.length === 0) return []

    return Array.from({ length: projectileCount }, (_, index) => aliveTargets[index % aliveTargets.length])
  }

  function triggerGuardCounterFire() {
    const projectileCount = getSkillValue('guard', 'projectileCount')
    if (projectileCount <= 0) return false

    const targets = getCounterGuardTargets(projectileCount)
    if (targets.length === 0) return false

    const traits = getStandardProjectileTraits()
    const projectileDamage =
      (COMBAT_BALANCE.baseDamage +
        weaponLevel.value * COMBAT_BALANCE.weaponDamagePerLevel +
        getSkillValue('overclock', 'damageBonus') +
        getSkillValue('burst', 'projectileDamageBonus') +
        getSkillValue('beam', 'flatDamageBonus')) *
      (getSkillValue('guard', 'damageMultiplier') || 1)

    for (let index = 0; index < targets.length; index += 1) {
      appendTrackingBullet(targets[index], {
        damage: projectileDamage,
        trail: index,
        bulletKind: 'standard',
        canSplit: traits.splitEnabled,
        solidTrailEnabled: traits.solidTrailEnabled,
        solidTrailMultiplier: traits.solidTrailMultiplier,
        pierceEnabled: traits.pierceEnabled,
        pierceTrailMultiplier: traits.pierceTrailMultiplier,
        piercingBulletLifetime: traits.piercingBulletLifetime,
        overclockSpeedBonus: traits.overclockSpeedBonus,
        refreshesWordOnHit: false,
      })
    }

    return true
  }

  function fireTypedShot(enemy) {
    const now = performance.now()
    const interval = Math.max(120, now - lastTypedAt.value)
    const typingRate = 60000 / interval
    typingBurst.value = clamp(typingRate, 2, 18)
    lastTypedAt.value = now

    const passiveBonus = getSkillValue('focus', 'comboDamageBonusPerCombo')
    const projectileCount = weaponLevel.value
    const overclockDamageBonus = getSkillValue('overclock', 'damageBonus')
    const traits = getStandardProjectileTraits()
    const baseDamage =
      COMBAT_BALANCE.baseDamage +
      weaponLevel.value * COMBAT_BALANCE.weaponDamagePerLevel +
      combo.value * (COMBAT_BALANCE.comboDamageScale + passiveBonus) +
      typingBurst.value * COMBAT_BALANCE.typingBurstScale +
      overclockDamageBonus
    const burstBonus = getSkillValue('burst', 'projectileDamageBonus')
    const rapidBonus = getSkillValue('rapid', 'energyOnComplete')
    const beamBonus = getSkillValue('beam', 'flatDamageBonus')

    // Reserve incoming damage so the next completed word does not waste shots on an already doomed enemy.
    for (let index = 0; index < projectileCount; index += 1) {
      const projectileDamage = baseDamage + burstBonus + beamBonus
      appendTrackingBullet(enemy, {
        damage: projectileDamage,
        trail: index,
        bulletKind: 'standard',
        canSplit: traits.splitEnabled,
        solidTrailEnabled: traits.solidTrailEnabled,
        solidTrailMultiplier: traits.solidTrailMultiplier,
        pierceEnabled: traits.pierceEnabled,
        pierceTrailMultiplier: traits.pierceTrailMultiplier,
        piercingBulletLifetime: traits.piercingBulletLifetime,
        overclockSpeedBonus: traits.overclockSpeedBonus,
        refreshesWordOnHit: true,
      })
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
    typedBuffer.value = ''
    targetEnemyId.value = null
    selectedMatchLength.value = 0
    matchedEnemyIds.value = new Set()
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

    if (event.key === '2') {
      event.preventDefault()
      activateFreezeSkill()
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
    const hadActiveMatches = matchedEnemyIds.value.size > 0
    const previousBuffer = typedBuffer.value

    const nextBuffer = `${typedBuffer.value}${nextLetter}`.slice(-rollingBufferLimit)
    typedBuffer.value = nextBuffer
    const prefixResult = findPrefixMatchEntries(nextBuffer, true)
    if (prefixResult.matches.length === 0) {
      targetEnemyId.value = null
      selectedMatchLength.value = 0
      matchedEnemyIds.value = new Set()
      if (hadActiveMatches) {
        typedBuffer.value = previousBuffer
        syncTargetByBuffer(previousTargetId)
        createFailedKeyBurst(nextLetter)
        combo.value = 0
        banner.value = '未找到匹配单词，请重新校准输入。'
        triggerEnemyError(previousTargetId)
      }
      return
    }

    typedLetterCount.value += 1
    matchedEnemyIds.value = new Set(prefixResult.matches.map((entry) => entry.enemy.id))
    selectedMatchLength.value = prefixResult.matchLength
    const preferredEntry =
      previousTargetId && matchedEnemyIds.value.has(previousTargetId)
        ? prefixResult.matches.find((entry) => entry.enemy.id === previousTargetId)
        : null
    const nextEntry = preferredEntry ?? prefixResult.matches[0]
    targetEnemyId.value = nextEntry.enemy.id
    createKeyBurst(nextLetter)

    const completedEntries = prefixResult.matches.filter((entry) => nextBuffer.endsWith(entry.enemy.keyword))
    if (completedEntries.length > 0) {
      const completedEntry = completedEntries.sort((left, right) => getEnemyDistance(left.enemy) - getEnemyDistance(right.enemy))[0]
      targetEnemyId.value = completedEntry.enemy.id
      selectedMatchLength.value = completedEntry.enemy.keyword.length
      fireTypedShot(completedEntry.enemy)
    }
  }

  function applyBulletHits(deltaSeconds) {
    const remainingBullets = []

    for (const bullet of bullets.value) {
      let targetEnemy = enemies.value.find((item) => item.id === bullet.targetId && item.health > 0)
      if (!targetEnemy && bullet.bulletKind === 'echo') {
        const redirectedTarget = pickRandomAliveEnemy()
        if (!redirectedTarget) {
          continue
        }
        targetEnemy = redirectedTarget
        bullet.targetId = redirectedTarget.id
      }

      const currentLastX = bullet.lastX ?? bullet.x
      const currentLastY = bullet.lastY ?? bullet.y
      const travel = bullet.speed * deltaSeconds
      let nextBullet = {
        ...bullet,
        lastX: bullet.x,
        lastY: bullet.y,
      }

      if (bullet.flightMode === 'piercing' || bullet.flightMode === 'straight') {
        const nextX = bullet.x + bullet.directionX * travel
        const nextY = bullet.y + bullet.directionY * travel

        nextBullet = {
          ...nextBullet,
          x: nextX,
          y: nextY,
        }
      } else {
        if (!targetEnemy) {
          nextBullet = {
            ...nextBullet,
            flightMode: 'straight',
            targetId: null,
            x: bullet.x + bullet.directionX * travel,
            y: bullet.y + bullet.directionY * travel,
          }
        } else {
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
            applyDamageToEnemy(enemy, nextBullet.damage * nextBullet.solidTrailMultiplier, {
              refreshWord: nextBullet.refreshesWordOnHit === true,
              source: getBulletDamageSource(nextBullet),
              sourceDamage: nextBullet.damage,
            })
            solidTrailEnemyIds.add(enemy.id)
          }

          if (enemy.id === nextBullet.targetId) {
            applyDamageToEnemy(enemy, nextBullet.damage, {
              refreshWord: nextBullet.refreshesWordOnHit === true,
              source: getBulletDamageSource(nextBullet),
              sourceDamage: nextBullet.damage,
            })
            if (nextBullet.bulletKind === 'echo') {
              createExplosionEffect(enemy.x, enemy.y, getSkillValue('echo', 'impactEffectRadius'))
            }
            if (nextBullet.bulletKind !== 'echo') {
              spawnSplitBullets(nextBullet, enemy)
            }

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
        } else if (nextBullet.flightMode === 'straight') {
          const distanceToPath = getLineSegmentDistance(currentLastX, currentLastY, nextBullet.x, nextBullet.y, enemy.x, enemy.y)
          const isDirectHit = distanceToPath <= enemy.radius + 4

          if (!isDirectHit) {
            if (nextBullet.solidTrailEnabled && !solidTrailEnemyIds.has(enemy.id)) {
              applyDamageToEnemy(enemy, nextBullet.damage * nextBullet.solidTrailMultiplier, {
                refreshWord: nextBullet.refreshesWordOnHit === true,
                source: getBulletDamageSource(nextBullet),
                sourceDamage: nextBullet.damage,
              })
              solidTrailEnemyIds.add(enemy.id)
            }
            continue
          }

          if (piercedEnemyIds.has(enemy.id)) {
            continue
          }

          applyDamageToEnemy(enemy, nextBullet.damage, {
            refreshWord: nextBullet.refreshesWordOnHit === true,
            source: getBulletDamageSource(nextBullet),
            sourceDamage: nextBullet.damage,
          })
          piercedEnemyIds.add(enemy.id)

          if (!nextBullet.pierceEnabled) {
            nextBullet = null
            break
          }
        } else if (!piercedEnemyIds.has(enemy.id)) {
          applyDamageToEnemy(enemy, nextBullet.damage * nextBullet.pierceTrailMultiplier, {
            refreshWord: nextBullet.refreshesWordOnHit === true,
            source: getBulletDamageSource(nextBullet),
            sourceDamage: nextBullet.damage,
          })
          piercedEnemyIds.add(enemy.id)
        }
      }

      if (!nextBullet) continue
      nextBullet = {
        ...nextBullet,
        solidHitEnemyIds: [...solidTrailEnemyIds],
        piercedEnemyIds: [...piercedEnemyIds],
      }

      if (nextBullet.flightMode === 'piercing' || nextBullet.flightMode === 'straight') {
        const bulletExitMargin = bullet.bulletKind === 'echo' ? 18 : bullet.bulletKind === 'split' ? 8 : 10
        const inBounds =
          nextBullet.x >= viewportVisibleBounds.value.left - bulletExitMargin &&
          nextBullet.x <= viewportVisibleBounds.value.right + bulletExitMargin &&
          nextBullet.y >= viewportVisibleBounds.value.top - bulletExitMargin &&
          nextBullet.y <= viewportVisibleBounds.value.bottom + bulletExitMargin
        if (!inBounds) continue
      }

      remainingBullets.push(nextBullet)
    }

    bullets.value = remainingBullets
  }

  function scoreWaveProgress(enemy) {
    if (enemy.deathSource === 'purge') {
      return
    }

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
    const repairKillHeal = getSkillValue('repair', 'onKillHeal')
    const burstWeaponGrowth = getSkillValue('burst', 'weaponGrowthPerKill')
    let defeatedBossCount = 0

    for (const enemy of defeated) {
      totalKillCount.value += 1

      if (!enemy.comboRegistered) {
        combo.value += 1
        syncMaxCombo()
        enemy.comboRegistered = true
        if (combo.value > 5) {
          triggerComboFeedback()
        }
      }

      createEnemyFragments(enemy)
      triggerBlastExplosion(enemy)
      spawnEchoSeekers(enemy)
      weaponLevel.value = clamp(weaponLevel.value + (enemy.boss ? 1 : 0) + burstWeaponGrowth, 1, COMBAT_BALANCE.maxWeaponLevel)
      scoreWaveProgress(enemy)
      score.value += enemy.boss ? COMBAT_BALANCE.bossKillScore : COMBAT_BALANCE.nonBossKillScore
      if (repairKillHeal > 0) {
        health.value = clamp(health.value + repairKillHeal, 0, maxHealth.value)
      }
      defeatedBossCount += enemy.boss ? 1 : 0
    }

    if (shieldLevel > 0) {
      health.value = clamp(health.value + defeated.length * shieldLevel, 0, maxHealth.value)
    }

    enemies.value = enemies.value.filter((enemy) => enemy.health > 0)
    markEnemyKeywordTrieDirty()
    if (defeatedBossCount > 0) {
      syncBossState()
    }
    syncTargetByBuffer()
  }

  function moveEnemies(deltaSeconds) {
    const nextEnemies = []
    const collidedEnemies = []
    const freezeMovementScale = freezeTimer.value > 0 ? getSkillValue('freeze', 'speedMultiplier') : 1
    const movementDeltaSeconds = deltaSeconds * freezeMovementScale

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
            x: enemy.x + directionX * enemy.speed * speedFactor * movementDeltaSeconds,
            y: enemy.y + directionY * enemy.speed * speedFactor * movementDeltaSeconds,
            orbitAngle: Math.atan2(enemy.y - centerPoint, enemy.x - centerPoint),
          }
        } else {
          const nextAngle = enemy.orbitAngle + BOSS_BALANCE.orbitAngularSpeed * movementDeltaSeconds
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
        const nextLaunchSpeed = Math.max(0, enemy.launchSpeed - BOSS_BALANCE.minionLaunchDeceleration * movementDeltaSeconds)
        const launchTravel = ((enemy.launchSpeed + nextLaunchSpeed) / 2) * movementDeltaSeconds
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
        const nextChaseSpeed = Math.min(enemy.speed, enemy.chaseSpeed + BOSS_BALANCE.minionChaseAcceleration * movementDeltaSeconds)
        const nextEnemy = {
          ...enemy,
          x: enemy.x + directionX * nextChaseSpeed * speedFactor * movementDeltaSeconds,
          y: enemy.y + directionY * nextChaseSpeed * speedFactor * movementDeltaSeconds,
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
        x: enemy.x + directionX * enemy.speed * speedFactor * movementDeltaSeconds,
        y: enemy.y + directionY * enemy.speed * speedFactor * movementDeltaSeconds,
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
        totalDamage += enemy.contactDamage ?? COMBAT_BALANCE.collisionDamage
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
    const guardTriggered = triggerGuardCounterFire()
    banner.value = guardTriggered
      ? `${collidedEnemies[0].text} 已碰到角色，主动防御已自动反击。`
      : `${collidedEnemies[0].text} 已碰到角色，生命值下降。`
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

  function updateExplosionEffects(deltaSeconds) {
    explosionEffects.value = explosionEffects.value
      .map((effect) => ({
        ...effect,
        life: effect.life - deltaSeconds,
      }))
      .filter((effect) => effect.life > 0)
  }

  function updateDamageTexts(deltaSeconds) {
    damageTexts.value = damageTexts.value
      .map((text) => ({
        ...text,
        x: text.x + text.dx * deltaSeconds,
        y: text.y + text.dy * deltaSeconds,
        life: text.life - deltaSeconds,
      }))
      .filter((text) => text.life > 0)
  }

  function updateComboFeedback(deltaSeconds) {
    comboFeedbackTimer.value = Math.max(0, comboFeedbackTimer.value - deltaSeconds)
    comboShakeTimer.value = Math.max(0, comboShakeTimer.value - deltaSeconds)
    explosionShakeTimer.value = Math.max(0, explosionShakeTimer.value - deltaSeconds)
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
    const isBossPhaseActive = getAliveBossCount() > 0 || bossSpawned.value < bossTargetCount.value
    if (!isBossPhaseActive && waveSpawned.value >= waveTargetCount.value) return

    const normalProfile = currentWaveProfile.value.normal
    const spawnProbability = clamp(sampleRange(normalProfile.spawnProbabilityRange), 0, 1)

    if (Math.random() > spawnProbability) return

    const healthMultiplier = sampleRange(normalProfile.healthMultiplierRange)
    enemies.value = [...enemies.value, buildEnemy(wave.value, false, { healthMultiplier })]
    markEnemyKeywordTrieDirty()
    waveSpawned.value += 1
  }

  function spawnBossEnemy() {
    if (bossTargetCount.value <= 0) return false
    if (bossSpawned.value >= bossTargetCount.value) {
      bossState.value = 'idle'
      return false
    }

    const bossProfile = currentWaveProfile.value.boss
    const spawnProbability = clamp(sampleRange(bossProfile.spawnProbabilityRange), 0, 1)

    if (Math.random() > spawnProbability) return false

    const healthMultiplier = sampleRange(bossProfile.healthMultiplierRange)
    enemies.value = [...enemies.value, buildEnemy(wave.value, true, { healthMultiplier })]
    markEnemyKeywordTrieDirty()
    bossSpawned.value += 1
    bossState.value = 'active'
    bossMinionCooldown.value = BOSS_BALANCE.minionInitialDelay
    banner.value =
      bossTargetCount.value > 1
        ? `第 ${wave.value} 关首领 ${bossSpawned.value}/${bossTargetCount.value} 已入场。`
        : `第 ${wave.value} 关首领已入场，优先锁定首领单词。`
    return true
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

    const waveEndRestoreRatio = SKILL_BALANCE.repair.waveEndMissingHealthRestoreRatio ?? 0
    if (waveEndRestoreRatio > 0) {
      const missingHealth = Math.max(0, maxHealth.value - health.value)
      pendingWaveEndHeal.value = missingHealth * waveEndRestoreRatio
    } else {
      pendingWaveEndHeal.value = 0
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
      const freezeSpawnScale = freezeTimer.value > 0 ? getSkillValue('freeze', 'speedMultiplier') : 1
      damageCooldown.value = Math.max(0, damageCooldown.value - deltaSeconds)
      playerHitFeedback.value = Math.max(0, playerHitFeedback.value - deltaSeconds)
      freezeTimer.value = Math.max(0, freezeTimer.value - deltaSeconds)
      spawnCooldown.value = Math.max(0, spawnCooldown.value - deltaSeconds * freezeSpawnScale)
      bossSpawnCooldown.value = Math.max(0, bossSpawnCooldown.value - deltaSeconds * freezeSpawnScale)
      bossMinionCooldown.value = Math.max(0, bossMinionCooldown.value - deltaSeconds * freezeSpawnScale)
      updateEnemyFeedbacks(deltaSeconds)
      updateEnemyFragments(deltaSeconds)
      updateExplosionEffects(deltaSeconds)
      updateDamageTexts(deltaSeconds)
      updateComboFeedback(deltaSeconds)
      updateKeyBursts(deltaSeconds)

      if (spawnCooldown.value <= 0) {
        spawnWaveEnemy()
        spawnCooldown.value = getSpawnInterval(currentWaveProfile.value.normal.spawnRatePerSecond)
      }

      if (bossSpawnCooldown.value <= 0) {
        const didSpawnBoss = spawnBossEnemy()
        if (bossSpawned.value >= bossTargetCount.value) {
          bossSpawnCooldown.value = Number.POSITIVE_INFINITY
        } else {
          bossSpawnCooldown.value = didSpawnBoss ? BOSS_BALANCE.spawnIntervalSeconds : 0.5
        }
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
    const style = {
      left: `${(bullet.x / arenaSize) * 100}%`,
      top: `${(bullet.y / arenaSize) * 100}%`,
    }

    if (bullet.bulletKind === 'echo') {
      const angle = Math.atan2(bullet.directionY ?? -1, bullet.directionX ?? 0) * (180 / Math.PI) + 90
      style.transform = `translate(-50%, -50%) rotate(${angle}deg)`
    }

    return style
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

  function isEnemyBulletTarget(enemy) {
    return bullets.value.some((bullet) => bullet.flightMode === 'tracking' && bullet.targetId === enemy.id)
  }

  function isEnemyMatchedTarget(enemy) {
    return matchedEnemyIds.value.has(enemy.id)
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

  function explosionStyle(effect) {
    const lifeRatio = Math.max(0, effect.life / effect.maxLife)
    const expandRatio = 1 - lifeRatio

    return {
      left: `${(effect.x / arenaSize) * 100}%`,
      top: `${(effect.y / arenaSize) * 100}%`,
      width: `${effect.radius * 2}px`,
      height: `${effect.radius * 2}px`,
      opacity: `${lifeRatio * 0.94}`,
      transform: `translate(-50%, -50%) scale(${0.28 + expandRatio * 1.06})`,
    }
  }

  function damageTextStyle(text) {
    const lifeRatio = Math.max(0, text.life / text.maxLife)
    const liftRatio = 1 - lifeRatio
    const scale = text.source === 'explosion' ? 1.12 : text.source === 'echo' ? 1.04 : 1

    return {
      left: `${(text.x / arenaSize) * 100}%`,
      top: `${(text.y / arenaSize) * 100}%`,
      opacity: `${Math.min(1, lifeRatio * 1.25)}`,
      transform: `translate(-50%, -50%) translateY(${-18 * liftRatio}px) scale(${0.84 + liftRatio * 0.28 * scale})`,
    }
  }

  onMounted(() => {
    restartGame()
    loadWordPool()
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
    currentProjectileDamage,
    damageTexts,
    enemies,
    enemyFragments,
    explosionEffects,
    energy,
    freezeTimer,
    freezeStatusLabel,
    hasGameStarted,
    health,
    isChoosingSkill,
    isCriticalHealth,
    isGameOver,
    isPaused,
    isVictory,
    isWordPoolLoading,
    keyBursts,
    purgeCooldownLabel,
    resultStats,
    skillChoices,
    banner,
    applySkillChoice,
    debugSelectWave,
    grantSkillById,
    refreshSkillChoices,
    reviveGame,
    hudStageHint,
    hudStageLabel,
    playerRingStyle,
    playerShellClass,
    endGame,
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
    damageTextStyle,
    enemyHealthStyle,
    enemyStyle,
    enemyWordTransitionStyle,
    explosionStyle,
    fragmentStyle,
    getEnemyWordParts,
    getPurgeWordParts,
    getSkillMaxLevel,
    isEnemyBulletTarget,
    isEnemyMatchedTarget,
    keyBurstStyle,
    purgeWordState,
  }
}


