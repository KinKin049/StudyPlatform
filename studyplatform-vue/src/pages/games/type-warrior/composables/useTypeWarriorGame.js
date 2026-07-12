/**
 * Type Warrior 游戏核心逻辑组合式函数
 * 
 * 该文件包含游戏的全部运行时状态和核心逻辑，包括：
 * - 敌人的生成、移动、碰撞检测
 * - 伤害计算和子弹系统
 * - 技能系统（清屏、冰冻、护盾等）
 * - 单词匹配和输入处理
 * - 游戏流程控制（关卡、波次、胜负判定）
 * - 动画效果和UI样式计算
 * 
 * 页面和展示组件保持简洁，只负责声明式渲染。
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
// 游戏配置和平衡性参数
import {
  TYPE_WARRIOR_BALANCE,
  TYPE_WARRIOR_CONFIG,
  TYPE_WARRIOR_ENEMY_KINDS,
  TYPE_WARRIOR_SKILL_POOL,
  TYPE_WARRIOR_WORD_BANK,
} from '../config/typeWarriorConfig'
// 单词池API接口
import { fetchTypeWarriorWordPool } from '../../../../api/academy'
// 关卡配置相关函数
import { getTypeWarriorFinalWave, getTypeWarriorWaveProfile } from '../config/typeWarriorWaveConfig'
// 数学工具函数
import { clamp, getDistance, normalizeWord, pickRandomItems, randomFrom } from '../utils/typeWarriorMath'
// 单词前缀树（Trie）工具，用于高效匹配敌人单词
import { buildEnemyKeywordTrie, findBestTrieSuffixPrefixMatches } from '../utils/typeWarriorTrie'

// 从配置中解构游戏基础参数
const {
  arenaSize,           // 战场尺寸
  blastEffectDuration, // 爆炸特效持续时间
  playerCollisionRadius, // 玩家碰撞半径
  maxCards,            // 最大技能卡片数量
  keyBurstDuration,    // 按键爆破动画持续时间
  spawnOffset,         // 敌人出生点偏移量
  rollingBufferLimit,  // 输入缓冲区最大长度
} = TYPE_WARRIOR_CONFIG

// 从平衡性配置中解构各模块参数
const {
  player: PLAYER_BALANCE,   // 玩家相关平衡参数
  combat: COMBAT_BALANCE,   // 战斗相关平衡参数
  boss: BOSS_BALANCE,       // 首领相关平衡参数
  enemies: ENEMY_BALANCE,   // 普通敌人相关平衡参数
  skills: SKILL_BALANCE,    // 技能相关平衡参数
} = TYPE_WARRIOR_BALANCE

// 战场中心点坐标
const centerPoint = arenaSize / 2
// 单词切换动画持续时间
const { wordTransitionDuration } = COMBAT_BALANCE
// 最近使用单词冷却队列大小
const RECENT_WORD_COOLDOWN_SIZE = 18
// 低使用频率单词的选择扩展范围
const LOW_USAGE_SPREAD = 1

/**
 * Type Warrior 游戏场景的组合式函数，拥有所有运行时状态
 * 页面和展示组件保持简洁，只负责声明式渲染
 */
export function useTypeWarriorGame() {
  // ==================== 游戏状态 ====================
  const wave = ref(1)                          // 当前关卡数
  const weaponLevel = ref(1)                   // 武器等级（影响子弹数量）
  const health = ref(PLAYER_BALANCE.baseHealth)         // 当前生命值
  const maxHealth = ref(PLAYER_BALANCE.baseHealth)      // 最大生命值
  const energy = ref(PLAYER_BALANCE.baseEnergy)         // 当前能量值
  const maxEnergy = ref(PLAYER_BALANCE.baseMaxEnergy)   // 最大能量值
  const hasGameStarted = ref(false)            // 游戏是否已开始
  const combo = ref(0)                         // 当前连击数
  const typedBuffer = ref('')                  // 用户输入的字符缓冲区
  const selectedMatchLength = ref(0)           // 当前选中目标的匹配长度
  const enemies = ref([])                      // 敌人列表
  const bullets = ref([])                      // 子弹列表
  const enemyFragments = ref([])               // 敌人死亡碎片效果列表
  const explosionEffects = ref([])             // 爆炸特效列表
  const damageTexts = ref([])                  // 伤害数字文本列表
  const cards = ref([])                        // 已装备的技能卡片列表
  const isGameOver = ref(false)                // 游戏是否结束（失败）
  const isVictory = ref(false)                 // 是否胜利
  const isChoosingSkill = ref(false)           // 是否正在选择技能
  const isWordPoolLoading = ref(false)         // 单词池是否正在加载
  const skillChoices = ref([])                 // 当前可选的技能列表
  const banner = ref('直接输入敌人上方的英文单词，系统会自动锁定并开火。') // 顶部提示横幅
  const bossState = ref('idle')                // 首领状态：idle/pending/active
  const waveSpawned = ref(0)                   // 当前波次已生成的敌人数
  const waveTargetCount = ref(7)               // 当前波次目标敌人数
  const spawnCooldown = ref(0)                 // 敌人生成冷却时间
  const bossSpawned = ref(0)                   // 已生成的首领数
  const bossTargetCount = ref(0)               // 目标首领数
  const bossSpawnCooldown = ref(0)             // 首领生成冷却时间
  const bossMinionCooldown = ref(0)            // 首领召唤小怪冷却时间
  const damageCooldown = ref(0)                // 玩家受伤后无敌冷却时间
  const lastTypedAt = ref(performance.now())   // 上次输入时间戳
  const typingBurst = ref(0)                   // 当前打字爆发值（影响伤害）
  const survivalSeconds = ref(0)               // 存活时间（秒）
  const targetEnemyId = ref(null)              // 当前锁定的目标敌人ID
  const matchedEnemyIds = ref(new Set())       // 与输入匹配的敌人ID集合
  const playerHitFeedback = ref(0)             // 玩家被击中的反馈计时器
  const keyBursts = ref([])                    // 按键爆破效果列表
  const comboFeedbackCount = ref(0)            // 连击反馈显示数字
  const comboFeedbackTimer = ref(0)            // 连击反馈计时器
  const comboShakeTimer = ref(0)               // 连击震动计时器
  const explosionShakeTimer = ref(0)           // 爆炸震动计时器
  const isPaused = ref(false)                  // 是否暂停
  const maxCombo = ref(0)                      // 本局最大连击数
  const score = ref(0)                         // 当前分数
  const solvedWordCount = ref(0)               // 已解决的单词数
  const typedLetterCount = ref(0)              // 已输入的字母数
  const totalKillCount = ref(0)                // 总击杀数
  const completedWaveCount = ref(0)            // 已完成的关卡数
  const effectiveTypingSeconds = ref(0)        // 有效打字时间（秒）
  const purgeWordState = ref({                 // 清屏技能状态
    active: false,                             // 是否激活
    word: '',                                  // 需要输入的单词
    text: '',                                  // 单词中文释义
    buffer: '',                                // 当前输入进度
  })
  const freezeTimer = ref(0)                   // 冰冻技能剩余持续时间
  const arenaRef = ref(null)                   // 战场DOM引用
  const pendingWaveNumber = ref(null)          // 待进入的下一关关卡号
  const pendingWaveEndHeal = ref(0)            // 关卡结束时待恢复的生命值
  const currentWaveProfile = ref(getTypeWarriorWaveProfile(1)) // 当前关卡配置
  const wordPools = ref(buildWordPools(TYPE_WARRIOR_WORD_BANK.map((item) => ({ ...item, familiarity: 'unmarked' })))) // 单词池
  const viewportSpawnBounds = ref({            // 视口生成边界（敌人生成区域）
    left: -spawnOffset,
    right: arenaSize + spawnOffset,
    top: -spawnOffset,
    bottom: arenaSize + spawnOffset,
  })
  const viewportVisibleBounds = ref({          // 视口可见边界（单词可匹配区域）
    left: 0,
    right: arenaSize,
    top: 0,
    bottom: arenaSize,
  })

  // ==================== 内部变量（非响应式） ====================
  let animationId = 0                          // 动画帧ID
  let lastFrameAt = 0                          // 上一帧时间戳
  let enemyIdSeed = 0                          // 敌人ID生成器
  let bulletIdSeed = 0                         // 子弹ID生成器
  let keyBurstIdSeed = 0                       // 按键爆破ID生成器
  let fragmentIdSeed = 0                       // 碎片ID生成器
  let explosionEffectIdSeed = 0                // 爆炸特效ID生成器
  let damageTextIdSeed = 0                     // 伤害文本ID生成器
  let wordPoolLoadPromise = null               // 单词池加载Promise（防止重复加载）
  let hasPreloadedWordPool = false             // 是否已预加载单词池
  let recentWordQueue = []                     // 最近使用的单词队列（用于冷却）
  let wordUsageCounts = new Map()              // 单词使用次数统计
  let enemyKeywordTrie = buildEnemyKeywordTrie([]) // 敌人关键词前缀树（用于快速匹配）
  let enemyKeywordTrieDirty = true             // 前缀树是否需要重新构建

  // ==================== 计算属性 ====================
  const currentTarget = computed(() => enemies.value.find((enemy) => enemy.id === targetEnemyId.value) || null) // 当前锁定的敌人对象
  const isCriticalHealth = computed(() => health.value < PLAYER_BALANCE.criticalHealthThreshold) // 是否处于低血量状态
  const wpmLike = computed(() => Math.round(typingBurst.value * 8.2)) // 模拟WPM（每分钟单词数）

  /**
   * 当前子弹伤害计算
   * 基础伤害 + 武器等级加成 + 连击加成 + 打字爆发加成 + 各种技能加成
   */
  const currentProjectileDamage = computed(() => {
    const passiveBonus = getSkillValue('focus', 'comboDamageBonusPerCombo')      // 专注技能的连击伤害加成
    const overclockDamageBonus = getSkillValue('overclock', 'damageBonus')       // 超频技能的伤害加成
    const burstBonus = getSkillValue('burst', 'projectileDamageBonus')           // 爆发技能的子弹伤害加成
    const beamBonus = getSkillValue('beam', 'flatDamageBonus')                   // 光束技能的固定伤害加成

    return Math.round(
      COMBAT_BALANCE.baseDamage +                                 // 基础伤害
        weaponLevel.value * COMBAT_BALANCE.weaponDamagePerLevel + // 武器等级加成
        combo.value * (COMBAT_BALANCE.comboDamageScale + passiveBonus) + // 连击伤害加成
        typingBurst.value * COMBAT_BALANCE.typingBurstScale +     // 打字爆发加成
        overclockDamageBonus +                                     // 超频加成
        burstBonus +                                               // 爆发加成
        beamBonus                                                  // 光束加成
    )
  })

  const wordsPerSecond = computed(() => (effectiveTypingSeconds.value > 0 ? solvedWordCount.value / effectiveTypingSeconds.value : 0)) // 每秒解决单词数
  const lettersPerSecond = computed(() => (effectiveTypingSeconds.value > 0 ? typedLetterCount.value / effectiveTypingSeconds.value : 0)) // 每秒输入字母数
  const killsPerSecond = computed(() => (survivalSeconds.value > 0 ? totalKillCount.value / survivalSeconds.value : 0)) // 每秒击杀数

  /**
   * 游戏结算统计数据
   */
  const resultStats = computed(() => ({
    reachedWave: wave.value,                   // 到达的最高关卡
    maxCombo: maxCombo.value,                  // 最大连击数
    score: Math.round(score.value),            // 最终分数
    coins: Math.round(Math.round(score.value) / 100), // 获得金币（分数/100）
    completedWaves: completedWaveCount.value,  // 完成的关卡数
    solvedWords: solvedWordCount.value,        // 解决的单词数
    typedLetters: typedLetterCount.value,      // 输入的字母数
    totalKills: totalKillCount.value,          // 总击杀数
    durationSeconds: survivalSeconds.value,    // 存活时长
    effectiveTypingSeconds: effectiveTypingSeconds.value, // 有效打字时长
    killsPerSecond: killsPerSecond.value,      // 击杀效率
    wordsPerSecond: wordsPerSecond.value,      // 单词解决效率
    lettersPerSecond: lettersPerSecond.value,  // 打字效率
  }))

  /**
   * 清屏技能状态标签
   */
  const purgeCooldownLabel = computed(() => {
    if (!hasSkill('purge')) return '未解锁'                                   // 未解锁
    if (purgeWordState.value.active) return '输入中'                          // 正在输入
    if (energy.value < SKILL_BALANCE.purge.energyCost) return `能量不足(${SKILL_BALANCE.purge.energyCost})` // 能量不足
    return `消耗${SKILL_BALANCE.purge.energyCost}`                            // 显示消耗
  })

  /**
   * 冰冻技能状态标签
   */
  const freezeStatusLabel = computed(() => {
    if (!hasSkill('freeze')) return '未解锁'                                  // 未解锁
    if (freezeTimer.value > 0) return `持续 ${freezeTimer.value.toFixed(1)}s` // 冷却中
    if (energy.value < SKILL_BALANCE.freeze.energyCost) return `能量不足(${SKILL_BALANCE.freeze.energyCost})` // 能量不足
    return `消耗${SKILL_BALANCE.freeze.energyCost}`                           // 显示消耗
  })

  /**
   * 玩家外壳样式类（用于被击中动画）
   */
  const playerShellClass = computed(() => ({
    'is-hit': playerHitFeedback.value > 0,   // 被击中状态
  }))

  /**
   * 游戏面板样式类（用于震动效果）
   */
  const boardClass = computed(() => ({
    'is-combo-shake': comboShakeTimer.value > 0,       // 连击震动
    'is-explosion-shake': explosionShakeTimer.value > 0, // 爆炸震动
    'is-paused': isPaused.value,                      // 暂停状态
  }))

  /**
   * HUD关卡标签（顶部状态栏）
   */
  const hudStageLabel = computed(() => {
    if (!hasGameStarted.value) return '等待开始'
    if (isVictory.value) return '通关完成'
    if (isGameOver.value) return '战斗结束'
    if (bossState.value === 'active') return `第 ${wave.value} 关 / 首领`
    return `第 ${wave.value} 关 / 词潮`
  })

  /**
   * HUD关卡提示（顶部状态栏）
   */
  const hudStageHint = computed(() => {
    if (!hasGameStarted.value) return '点击开始游戏后进入第一关。'
    if (isPaused.value) return '已暂停，按 Esc 继续。'
    if (isGameOver.value) return '生命耗尽，防线失守。'
    if (isVictory.value) return '最终首领已被击败。'
    if (isChoosingSkill.value) return '本关已清空，请先选择一项技能。'
    if (bossState.value === 'active') return '优先锁定首领单词，注意环绕压场与吐词增援。'
    return `当前场上 ${enemies.value.length} 个目标，连击 ${combo.value}。`
  })

  /**
   * 显示关卡标签（中间舞台）
   */
  const displayStageLabel = computed(() => {
    if (!hasGameStarted.value) return '等待开始'
    if (isVictory.value) return '通关完成'
    if (bossState.value === 'active') return `第 ${wave.value} 关 / 首领`
    return `第 ${wave.value} 关 / 词潮`
  })

  /**
   * 显示关卡提示（中间舞台）
   */
  const displayStageHint = computed(() => {
    if (!hasGameStarted.value) return '点击开始游戏后进入第一关。'
    if (isPaused.value) return '已暂停，按 Esc 继续。'
    if (isGameOver.value) return '生命耗尽，词潮突破防线。'
    if (isVictory.value) return '首领已被清除。'
    if (isChoosingSkill.value) return '本关已清空，请先选择一项技能。'
    if (bossState.value === 'active') return '优先处理首领，注意绕场轨迹与吐词方向。'
    return `当前词潮 ${enemies.value.length} 个目标，连击 ${combo.value}。`
  })

  /**
   * 玩家圆环样式（生命值和能量值显示）
   * 使用CSS径向渐变和圆锥渐变实现双层进度环
   */
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

  /**
   * 关卡标签（结算界面）
   */
  const stageLabel = computed(() => {
    if (!hasGameStarted.value) return '等待开始'
    if (isVictory.value) return '通关完成'
    if (bossState.value === 'active') return `第 ${wave.value} 关 / 首领`
    return `第 ${wave.value} 关 / 文字浪潮`
  })

  /**
   * 关卡提示（结算界面）
   */
  const stageHint = computed(() => {
    if (!hasGameStarted.value) return '点击开始游戏后进入第一关。'
    if (isGameOver.value) return '生命耗尽，文字浪潮突破防线。'
    if (isVictory.value) return '首领已被击败。'
    if (isChoosingSkill.value) return '本关已清空，请先选择一项技能。'
    if (bossState.value === 'active') return '锁定首领单词，同时避开外圈环绕路径。'
    return `当前词潮 ${enemies.value.length} 个目标，连击 ${combo.value}。`
  })

  // ==================== 工具函数 ====================

  /**
   * 在给定范围内随机采样一个数值
   * @param {number[]} [min, max] - 范围数组，包含最小值和最大值
   * @returns {number} 随机采样的数值
   */
  function sampleRange([min, max]) {
    if (max <= min) return min
    return min + Math.random() * (max - min)
  }

  /**
   * 根据每秒生成率计算生成间隔时间（秒）
   * @param {number} ratePerSecond - 每秒生成数量
   * @returns {number} 生成间隔时间
   */
  function getSpawnInterval(ratePerSecond) {
    return ratePerSecond > 0 ? 1 / ratePerSecond : Number.POSITIVE_INFINITY
  }

  /**
   * 检查当前关卡配置是否包含首领
   * @param {object} profile - 关卡配置（默认为当前关卡）
   * @returns {boolean} 是否配置了首领
   */
  function hasConfiguredBosses(profile = currentWaveProfile.value) {
    return (profile?.boss?.totalCount ?? 0) > 0
  }

  /**
   * 获取技能当前等级
   * @param {string} skillId - 技能ID
   * @returns {number} 当前技能等级
   */
  function getSkillLevel(skillId) {
    return cards.value.find((card) => card.id === skillId)?.level ?? 0
  }

  /**
   * 获取技能最大等级
   * 通过检查技能配置中所有数组类型字段的长度来确定最大等级
   * @param {string} skillId - 技能ID
   * @returns {number} 技能最大等级
   */
  function getSkillMaxLevel(skillId) {
    const config = SKILL_BALANCE[skillId]
    if (!config) return 1

    const arrayLevels = Object.values(config)
      .filter((value) => Array.isArray(value))
      .map((value) => Math.max(0, value.length - 1))

    if (arrayLevels.length === 0) return 1
    return Math.max(...arrayLevels)
  }

  /**
   * 检查技能是否已达到最高等级
   * @param {string} skillId - 技能ID
   * @param {number} level - 要检查的等级（默认使用当前等级）
   * @returns {boolean} 是否已满级
   */
  function isSkillAtMaxLevel(skillId, level = getSkillLevel(skillId)) {
    return level >= getSkillMaxLevel(skillId)
  }

  /**
   * 获取技能在当前等级下的属性值
   * @param {string} skillId - 技能ID
   * @param {string} fieldName - 属性字段名
   * @returns {number} 属性值
   */
  function getSkillValue(skillId, fieldName) {
    const level = getSkillLevel(skillId)
    const values = SKILL_BALANCE[skillId]?.[fieldName] ?? []
    if (values.length === 0) return 0
    return values[Math.min(level, values.length - 1)] ?? 0
  }

  /**
   * 同步最大连击记录
   */
  function syncMaxCombo() {
    maxCombo.value = Math.max(maxCombo.value, combo.value)
  }

  /**
   * 标记关卡完成
   * @param {number} currentWave - 当前关卡数
   */
  function markWaveCompleted(currentWave = wave.value) {
    completedWaveCount.value = Math.max(completedWaveCount.value, currentWave)
  }

  /**
   * 同步派生属性（生命值上限、能量上限）
   * 当技能变化时需要调用此函数更新玩家属性上限
   * @param {object} options - 选项
   * @param {boolean} options.restoreHealth - 是否恢复新增的生命值上限差值
   * @param {boolean} options.restoreEnergy - 是否恢复新增的能量上限差值
   */
  function syncDerivedStats({ restoreHealth = false, restoreEnergy = false } = {}) {
    const nextMaxHealth =
      PLAYER_BALANCE.baseHealth +
      getSkillValue('shield', 'maxHealthBonus') +           // 护盾技能的生命值加成
      getSkillValue('lifelong', 'maxHealthBonus')          // 成长技能的生命值加成
    const nextMaxEnergy = PLAYER_BALANCE.baseMaxEnergy + getSkillValue('reserve', 'maxEnergyBonus') // 储备技能的能量加成
    const healthDelta = nextMaxHealth - maxHealth.value
    const energyDelta = nextMaxEnergy - maxEnergy.value

    maxHealth.value = nextMaxHealth
    maxEnergy.value = nextMaxEnergy
    // 如果需要恢复且上限有提升，则按差值恢复
    health.value = restoreHealth && healthDelta > 0 ? clamp(health.value + healthDelta, 0, maxHealth.value) : clamp(health.value, 0, maxHealth.value)
    energy.value = restoreEnergy && energyDelta > 0 ? clamp(energy.value + energyDelta, 0, maxEnergy.value) : clamp(energy.value, 0, maxEnergy.value)
  }

  /**
   * 创建按键爆破效果条目
   * @param {string} letter - 按键字母
   * @param {boolean} failed - 是否为输入失败状态（影响颜色和运动轨迹）
   * @returns {object} 按键爆破效果对象
   */
  function createKeyBurstEntry(letter, failed = false) {
    const angle = Math.random() * Math.PI * 2  // 随机角度（0~360度）
    const distance =
      (failed ? COMBAT_BALANCE.keyBurst.failedDistanceBase : COMBAT_BALANCE.keyBurst.normalDistanceBase) +
      Math.random() * (failed ? COMBAT_BALANCE.keyBurst.failedDistanceRange : COMBAT_BALANCE.keyBurst.normalDistanceRange)

    return {
      id: `key-burst-${keyBurstIdSeed++}`,
      letter,                    // 显示的字母
      age: 0,                    // 存在时间
      dx: Math.cos(angle) * distance,    // X方向偏移
      dy: Math.sin(angle) * distance - (failed ? COMBAT_BALANCE.keyBurst.failedLift : COMBAT_BALANCE.keyBurst.normalLift), // Y方向偏移
      failed,                    // 是否失败状态
    }
  }

  /**
   * 创建成功按键爆破效果
   * @param {string} letter - 按键字母
   */
  function createKeyBurst(letter) {
    keyBursts.value = [...keyBursts.value, createKeyBurstEntry(letter, false)]
  }

  /**
   * 创建失败按键爆破效果（输入错误时触发）
   * @param {string} letter - 按键字母
   */
  function createFailedKeyBurst(letter) {
    keyBursts.value = [...keyBursts.value, createKeyBurstEntry(letter, true)]
  }

  /**
   * 触发爆炸震动效果
   */
  function triggerExplosionShake() {
    explosionShakeTimer.value = COMBAT_BALANCE.explosionShakeDuration
  }

  /**
   * 创建爆炸特效
   * @param {number} x - 爆炸位置X坐标
   * @param {number} y - 爆炸位置Y坐标
   * @param {number} radius - 爆炸半径
   */
  function createExplosionEffect(x, y, radius) {
    explosionEffects.value = [
      ...explosionEffects.value,
      {
        id: `explosion-${explosionEffectIdSeed++}`,
        x,
        y,
        radius,
        life: blastEffectDuration,    // 当前生命周期
        maxLife: blastEffectDuration, // 总生命周期
      },
    ]
  }

  /**
   * 创建伤害数字文本
   * @param {number} x - 显示位置X坐标
   * @param {number} y - 显示位置Y坐标
   * @param {number} damage - 伤害值
   * @param {string} source - 伤害来源（bullet/explosion/echo）
   */
  function createDamageText(x, y, damage, source = 'bullet') {
    const roundedDamage = Math.max(1, Math.round(damage))  // 伤害值取整，最小为1
    const angle = -Math.PI / 2 + (Math.random() - 0.5) * 0.7  // 向上发散角度
    const distance = 20 + Math.random() * 14  // 随机偏移距离

    damageTexts.value = [
      ...damageTexts.value,
      {
        id: `damage-${damageTextIdSeed++}`,
        x,
        y,
        dx: Math.cos(angle) * distance,  // X方向移动量
        dy: Math.sin(angle) * distance - 8,  // Y方向移动量（向上）
        value: roundedDamage,  // 显示的伤害值
        source,                // 伤害来源标识
        life: 0.56,            // 当前生命周期
        maxLife: 0.56,         // 总生命周期
      },
    ]
  }

  /**
   * 更新视口边界（敌人生成区域和可见区域）
   * 在窗口大小变化或战场元素位置变化时调用
   */
  function updateViewportSpawnBounds() {
    const arenaElement = arenaRef.value
    if (!arenaElement) return

    const rect = arenaElement.getBoundingClientRect()
    const scale = rect.width / arenaSize || 1  // 计算缩放比例

    // 更新敌人生成边界（带偏移量，允许敌人在屏幕外生成）
    viewportSpawnBounds.value = {
      left: (0 - rect.left) / scale - spawnOffset,
      right: (window.innerWidth - rect.left) / scale + spawnOffset,
      top: (0 - rect.top) / scale - spawnOffset,
      bottom: (window.innerHeight - rect.top) / scale + spawnOffset,
    }

    // 更新可见边界（单词可匹配区域）
    viewportVisibleBounds.value = {
      left: (0 - rect.left) / scale,
      right: (window.innerWidth - rect.left) / scale,
      top: (0 - rect.top) / scale,
      bottom: (window.innerHeight - rect.top) / scale,
    }
  }

  /**
   * 规范化单词熟悉度状态
   * @param {string} status - 原始状态值
   * @returns {string} 规范化后的状态（unknown/unmarked/fuzzy/known）
   */
  function normalizeWordStatus(status) {
    return ['unknown', 'unmarked', 'fuzzy', 'known'].includes(status) ? status : 'unmarked'
  }

  /**
   * 构建单词池，按熟悉度分类
   * @param {array} words - 单词列表
   * @returns {object} 按熟悉度分类的单词池
   */
  function buildWordPools(words) {
    const pools = {
      unknown: [],   // 未知单词
      unmarked: [],  // 未标记单词
      fuzzy: [],     // 模糊单词
      known: [],     // 已知单词
      all: [],       // 所有单词
    }

    for (const word of words) {
      const normalizedKeyword = normalizeWord(word.word)  // 规范化单词格式
      if (!normalizedKeyword) continue

      const normalizedStatus = normalizeWordStatus(word.familiarity)  // 规范化熟悉度状态
      const normalizedEntry = {
        text: word.text || normalizedKeyword,      // 单词中文释义
        word: normalizedKeyword,                   // 规范化的英文单词
        tier: Math.max(1, Math.min(4, Number(word.tier) || 1)), // 单词等级（1-4）
        familiarity: normalizedStatus,             // 熟悉度状态
      }

      pools[normalizedStatus].push(normalizedEntry)
      pools.all.push(normalizedEntry)
    }

    return pools
  }

  /**
   * 重置单词选择状态（清空使用记录）
   */
  function resetWordSelectionState() {
    recentWordQueue = []
    wordUsageCounts = new Map()
  }

  /**
   * 标记单词最近被使用过（用于冷却机制）
   * @param {object} word - 单词对象
   */
  function markWordRecentlyUsed(word) {
    const keyword = normalizeWord(word?.word)
    if (!keyword) return

    // 先移除旧的引用，再添加到队尾
    recentWordQueue = recentWordQueue.filter((item) => item !== keyword)
    recentWordQueue.push(keyword)
    // 保持队列长度限制
    if (recentWordQueue.length > RECENT_WORD_COOLDOWN_SIZE) {
      recentWordQueue = recentWordQueue.slice(-RECENT_WORD_COOLDOWN_SIZE)
    }
    // 更新使用次数统计
    wordUsageCounts.set(keyword, (wordUsageCounts.get(keyword) ?? 0) + 1)
  }

  /**
   * 获取当前场上活跃的敌人关键词集合
   * @returns {Set} 关键词集合
   */
  function getActiveKeywordSet() {
    return new Set(enemies.value.map((enemy) => enemy.keyword))
  }

  /**
   * 过滤单词候选列表
   * @param {array} candidates - 候选单词列表
   * @param {object} options - 过滤选项
   * @param {boolean} options.excludeRecent - 是否排除最近使用的单词
   * @param {boolean} options.excludeActive - 是否排除当前活跃的单词
   * @returns {array} 过滤后的候选列表
   */
  function filterWordCandidates(candidates, { excludeRecent = true, excludeActive = true } = {}) {
    const activeKeywordSet = excludeActive ? getActiveKeywordSet() : null
    const recentKeywordSet = excludeRecent ? new Set(recentWordQueue) : null

    return candidates.filter((candidate) => {
      if (excludeActive && activeKeywordSet?.has(candidate.word)) return false  // 排除场上已有的单词
      if (excludeRecent && recentKeywordSet?.has(candidate.word)) return false  // 排除最近使用的单词
      return true
    })
  }

  /**
   * 从候选列表中选择单词（优先选择使用频率低的）
   * @param {array} candidates - 候选单词列表
   * @returns {object|null} 选中的单词对象
   */
  function pickWordFromCandidates(candidates) {
    if (candidates.length === 0) return null

    // 找到使用频率最低的单词
    let minUsage = Number.POSITIVE_INFINITY
    for (const candidate of candidates) {
      minUsage = Math.min(minUsage, wordUsageCounts.get(candidate.word) ?? 0)
    }

    // 选择使用频率在最低值附近的单词，增加多样性
    const lowUsageCandidates = candidates.filter((candidate) => (wordUsageCounts.get(candidate.word) ?? 0) <= minUsage + LOW_USAGE_SPREAD)
    return randomFrom(lowUsageCandidates.length > 0 ? lowUsageCandidates : candidates)
  }

  /**
   * 加载单词池（从服务器获取）
   * 支持缓存机制，避免重复请求
   * @param {boolean} force - 是否强制重新加载
   * @returns {Promise} 单词池加载Promise
   */
  async function loadWordPool(force = false) {
    // 如果已预加载且不强制刷新，直接返回
    if (!force && hasPreloadedWordPool) {
      return wordPools.value
    }
    // 如果正在加载中，返回已有Promise避免重复请求
    if (!force && wordPoolLoadPromise) {
      return wordPoolLoadPromise
    }

    isWordPoolLoading.value = true
    wordPoolLoadPromise = fetchTypeWarriorWordPool()
      .then((response) => {
        const remoteWords = Array.isArray(response?.words) ? response.words : []
        if (remoteWords.length > 0) {
          wordPools.value = buildWordPools(remoteWords)  // 构建新的单词池
        }
        hasPreloadedWordPool = true
        return wordPools.value
      })
      .catch((error) => {
        console.warn('failed to load type warrior word pool from database:', error)
        hasPreloadedWordPool = true  // 即使失败也标记已尝试，避免无限重试
        return wordPools.value
      })
      .finally(() => {
        isWordPoolLoading.value = false
        wordPoolLoadPromise = null
      })

    return wordPoolLoadPromise
  }

  /**
   * 根据关卡获取单词等级
   * @param {number} currentWave - 当前关卡数
   * @returns {number} 单词等级（1-4）
   */
  function getWaveWordTier(currentWave) {
    if (currentWave >= 5) return 4
    if (currentWave >= 4) return 3
    if (currentWave >= 2) return 2
    return 1
  }

  /**
   * 根据单词长度获取长度等级
   * @param {string} word - 单词
   * @returns {string} 长度等级（short/medium/long/extraLong）
   */
  function getWordLengthTier(word) {
    const length = normalizeWord(word).length
    const scaling = ENEMY_BALANCE.wordLengthScaling

    if (length <= scaling.shortMaxLength) return 'short'
    if (length <= scaling.mediumMaxLength) return 'medium'
    if (length <= scaling.longMaxLength) return 'long'
    return 'extraLong'
  }

  /**
   * 根据权重随机选择单词熟悉度状态
   * @param {object} availablePools - 可用的单词池
   * @param {object} weights - 各状态的权重
   * @returns {string|null} 选中的状态
   */
  function pickWeightedStatus(availablePools, weights) {
    const entries = Object.entries(weights || {})
      .map(([status, weight]) => ({
        status,
        weight: Math.max(0, Number(weight) || 0),  // 确保权重为非负数
      }))
      .filter((entry) => entry.weight > 0 && availablePools[entry.status]?.length)  // 过滤权重为0或没有单词的状态

    if (entries.length === 0) return null

    // 计算总权重，使用加权随机算法
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

  /**
   * 去重单词候选列表（按单词内容）
   * @param {array} candidates - 候选单词列表
   * @returns {array} 去重后的列表
   */
  function dedupeWordCandidates(candidates) {
    const seen = new Set()
    return candidates.filter((candidate) => {
      if (seen.has(candidate.word)) return false
      seen.add(candidate.word)
      return true
    })
  }

  /**
   * 为当前关卡选择一个单词
   * 按照等级过滤 -> 熟悉度权重选择 -> 使用频率选择 的优先级
   * @param {number} currentWave - 当前关卡数
   * @returns {object} 选中的单词对象
   */
  function pickWordForWave(currentWave) {
    const tier = getWaveWordTier(currentWave)
    // 1. 根据等级过滤单词池
    const tierPools = {
      unknown: wordPools.value.unknown.filter((item) => item.tier <= tier),
      unmarked: wordPools.value.unmarked.filter((item) => item.tier <= tier),
      fuzzy: wordPools.value.fuzzy.filter((item) => item.tier <= tier),
      known: wordPools.value.known.filter((item) => item.tier <= tier),
    }
    // 2. 严格过滤（排除活跃和最近使用的单词）
    const filteredPools = {
      unknown: filterWordCandidates(tierPools.unknown),
      unmarked: filterWordCandidates(tierPools.unmarked),
      fuzzy: filterWordCandidates(tierPools.fuzzy),
      known: filterWordCandidates(tierPools.known),
    }
    // 3. 宽松过滤（只排除活跃单词）
    const relaxedRecentPools = {
      unknown: filterWordCandidates(tierPools.unknown, { excludeRecent: false }),
      unmarked: filterWordCandidates(tierPools.unmarked, { excludeRecent: false }),
      fuzzy: filterWordCandidates(tierPools.fuzzy, { excludeRecent: false }),
      known: filterWordCandidates(tierPools.known, { excludeRecent: false }),
    }
    // 4. 根据熟悉度权重选择状态，依次尝试严格过滤、宽松过滤、原始池
    const selectedStatus =
      pickWeightedStatus(filteredPools, currentWaveProfile.value.wordFamiliarityWeights) ??
      pickWeightedStatus(relaxedRecentPools, currentWaveProfile.value.wordFamiliarityWeights) ??
      pickWeightedStatus(tierPools, currentWaveProfile.value.wordFamiliarityWeights)

    // 5. 根据选中的状态获取候选单词
    const selectedCandidates = selectedStatus
      ? filteredPools[selectedStatus].length > 0
        ? filteredPools[selectedStatus]
        : relaxedRecentPools[selectedStatus].length > 0
          ? relaxedRecentPools[selectedStatus]
          : tierPools[selectedStatus]
      : []

    // 6. 构建兜底候选列表（所有可用单词）
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

    // 7. 从候选中选择单词
    const pickedWord = pickWordFromCandidates(selectedCandidates) || pickWordFromCandidates(fallbackCandidates)
    return pickedWord || randomFrom(TYPE_WARRIOR_WORD_BANK)  // 最后的兜底
  }

  /**
   * 创建敌人出生位置（随机选择四边之一）
   * @returns {object} 出生位置坐标 {x, y}
   */
  function createSpawnPosition() {
    const { left, right, top, bottom } = viewportSpawnBounds.value
    const side = Math.floor(Math.random() * 4)  // 随机选择0:左 1:右 2:上 3:下

    if (side === 0) return { x: left, y: top + Math.random() * (bottom - top) }    // 左边
    if (side === 1) return { x: right, y: top + Math.random() * (bottom - top) }   // 右边
    if (side === 2) return { x: left + Math.random() * (right - left), y: top }    // 上边

    return {  // 下边
      x: left + Math.random() * (right - left),
      y: bottom,
    }
  }

  /**
   * 构建敌人对象
   * @param {number} currentWave - 当前关卡数
   * @param {boolean} boss - 是否为首领
   * @param {object} options - 选项
   * @param {object} options.spawnPoint - 指定出生位置
   * @param {number} options.healthMultiplier - 生命值倍率
   * @param {number} options.emissionVector - 发射向量（用于首领召唤的小怪）
   * @returns {object} 敌人对象
   */
  function buildEnemy(currentWave, boss = false, options = {}) {
    const word = pickWordForWave(currentWave)  // 为敌人选择一个单词
    markWordRecentlyUsed(word)                 // 标记单词已使用
    const normalizedKeyword = normalizeWord(word.word)  // 规范化单词
    const wordLengthTier = getWordLengthTier(normalizedKeyword)  // 获取单词长度等级

    // 选择敌人类型（首领固定类型，普通敌人随机）
    const kind = boss
      ? { type: 'boss', shape: 'boss', baseHealth: BOSS_BALANCE.baseHealth, baseSpeed: BOSS_BALANCE.baseSpeed, accent: BOSS_BALANCE.accent }
      : randomFrom(TYPE_WARRIOR_ENEMY_KINDS)

    const spawnPoint = options.spawnPoint ?? createSpawnPosition()  // 出生位置
    const healthMultiplier = options.healthMultiplier ?? 1          // 生命值倍率
    // 计算基础生命值（随关卡增长）
    const baseHealth = boss ? kind.baseHealth + currentWave * BOSS_BALANCE.healthPerWave : kind.baseHealth + currentWave * ENEMY_BALANCE.healthPerWave
    // 计算基础速度（随关卡增长）
    const baseSpeed = boss ? kind.baseSpeed + currentWave * BOSS_BALANCE.speedPerWave : kind.baseSpeed + currentWave * ENEMY_BALANCE.speedPerWave
    // 根据单词长度调整速度
    const speedMultiplier = ENEMY_BALANCE.wordLengthScaling.speedMultiplier[wordLengthTier] ?? 1
    // 根据单词长度调整接触伤害
    const contactDamageMultiplier = ENEMY_BALANCE.wordLengthScaling.contactDamageMultiplier[wordLengthTier] ?? 1
    // 计算环绕角度（用于首领移动）
    const orbitAngle = Math.atan2(spawnPoint.y - centerPoint, spawnPoint.x - centerPoint)
    const emissionVector = options.emissionVector ?? null  // 发射向量（小怪用）
    const scaledHealth = Math.max(1, Math.round(baseHealth * healthMultiplier))  // 最终生命值
    const contactDamage = boss ? 0 : Math.max(1, Math.round(COMBAT_BALANCE.collisionDamage * contactDamageMultiplier))  // 接触伤害（首领无接触伤害）

    return {
      id: `enemy-${enemyIdSeed++}`,       // 唯一ID
      text: word.text,                    // 中文释义
      keyword: normalizedKeyword,         // 规范化英文单词（用于匹配）
      displayWord: word.word.toLowerCase(), // 显示用的英文单词
      x: spawnPoint.x,                    // X坐标
      y: spawnPoint.y,                    // Y坐标
      type: kind.type,                    // 类型
      shape: kind.shape,                  // 形状
      radius: boss ? BOSS_BALANCE.radius : kind.shape === 'dot' ? ENEMY_BALANCE.dotRadius : ENEMY_BALANCE.shapedRadius, // 碰撞半径
      health: scaledHealth,               // 当前生命值
      maxHealth: scaledHealth,            // 最大生命值
      speed: baseSpeed * speedMultiplier, // 移动速度
      contactDamage,                      // 接触伤害
      wordLengthTier,                     // 单词长度等级
      accent: kind.accent,                // 强调颜色
      boss,                               // 是否为首领
      orbitAngle,                         // 环绕角度
      errorFeedback: 0,                   // 错误反馈计时器
      hitFeedback: 0,                     // 被击中反馈计时器
      incomingDamage: 0,                  // 即将到来的伤害（用于预扣血）
      comboRegistered: false,             // 是否已记录连击
      wordTransitionState: 'idle',        // 单词切换状态（idle/fade-out/fade-in）
      wordTransitionTimer: 0,             // 单词切换计时器
      pendingWord: null,                  // 待切换的单词
      emitFeedback: 0,                    // 发射反馈计时器（首领专用）
      emissionAngle: -Math.PI / 2,        // 发射角度（首领专用）
      movementMode: emissionVector ? 'emitted' : 'direct',  // 移动模式
      emissionVector,                     // 发射向量（小怪用）
      launchSpeed: emissionVector ? BOSS_BALANCE.minionLaunchSpeed : 0, // 初始发射速度
      chaseSpeed: 0,                      // 追逐速度（发射后的加速）
    }
  }

  /**
   * 为首领选择小怪发射角度
   * @param {object} bossEnemy - 首领敌人对象
   * @returns {number} 发射角度（弧度）
   */
  function pickBossEmissionAngle(bossEnemy) {
    const centerAngle = Math.atan2(centerPoint - bossEnemy.y, centerPoint - bossEnemy.x)  // 朝向中心的角度
    const angleOffsets = [0, Math.PI / 2, -Math.PI / 2]  // 偏移选项：正向、左前方、右前方
    const angleOffset = randomFrom(angleOffsets)

    return centerAngle + angleOffset + (Math.random() - 0.5) * BOSS_BALANCE.emissionJitter  // 添加随机抖动
  }

  // ==================== 技能系统 ====================

  /**
   * 检查是否拥有指定技能
   * @param {string} skillId - 技能ID
   * @returns {boolean} 是否拥有该技能
   */
  function hasSkill(skillId) {
    return getSkillLevel(skillId) > 0
  }

  /**
   * 重置清屏技能状态
   */
  function resetPurgeWordState() {
    purgeWordState.value = {
      active: false,
      word: '',
      text: '',
      buffer: '',
    }
  }

  /**
   * 激活清屏技能
   * 清屏技能需要输入指定单词来触发，成功后清除所有非首领敌人
   */
  function activatePurgeSkill() {
    // 检查技能是否可用
    if (!hasSkill('purge') || isChoosingSkill.value || isGameOver.value || isVictory.value) return
    // 检查是否正在激活中
    if (purgeWordState.value.active) return
    // 检查能量是否足够
    if (energy.value < SKILL_BALANCE.purge.energyCost) {
      banner.value = `清屏指令需要 ${SKILL_BALANCE.purge.energyCost} 点能量。`
      return
    }

    // 选择一个清屏单词
    const nextWord = pickWordForWave(Math.max(1, wave.value))
    // 清空当前输入状态
    typedBuffer.value = ''
    selectedMatchLength.value = 0
    targetEnemyId.value = null
    // 设置清屏技能状态
    purgeWordState.value = {
      active: true,
      word: normalizeWord(nextWord.word),  // 需要输入的单词
      text: nextWord.text,                 // 中文释义
      buffer: '',                          // 当前输入进度
    }
    matchedEnemyIds.value = new Set()
    banner.value = `清屏指令已激活：${nextWord.word.toLowerCase()} / ${nextWord.text}`
  }

  /**
   * 触发清屏技能效果
   * 清除所有非首领敌人
   */
  function triggerPurgeSkill() {
    for (const enemy of enemies.value) {
      if (enemy.boss) continue  // 首领不受清屏影响

      enemy.health = 0                    // 直接击杀
      enemy.incomingDamage = 0
      enemy.hitFeedback = 0.22
      enemy.deathSource = 'purge'         // 标记死亡来源
      enemy.deathSourceDamage = 0
    }

    // 重置状态
    resetPurgeWordState()
    typedBuffer.value = ''
    selectedMatchLength.value = 0
    targetEnemyId.value = null
    matchedEnemyIds.value = new Set()
    energy.value = clamp(energy.value - SKILL_BALANCE.purge.energyCost, 0, maxEnergy.value)  // 消耗能量
    banner.value = '清屏指令已执行。'
  }

  /**
   * 激活冰冻技能
   * 冰冻技能减速所有敌人的移动和生成速度
   */
  function activateFreezeSkill() {
    // 检查技能是否可用
    if (!hasSkill('freeze') || isChoosingSkill.value || isGameOver.value || isVictory.value) return
    // 检查是否正在冷却中
    if (freezeTimer.value > 0) {
      banner.value = '冰冻仍在持续中。'
      return
    }
    // 检查能量是否足够
    if (energy.value < SKILL_BALANCE.freeze.energyCost) {
      banner.value = `冰冻需要 ${SKILL_BALANCE.freeze.energyCost} 点能量。`
      return
    }

    energy.value = clamp(energy.value - SKILL_BALANCE.freeze.energyCost, 0, maxEnergy.value)  // 消耗能量
    freezeTimer.value = getSkillValue('freeze', 'duration')  // 设置持续时间
    banner.value = '冰冻已释放，敌人移动与刷怪节奏已减速。'
  }

  /**
   * 计算点到线段的最短距离
   * @param {number} x1 - 线段起点X坐标
   * @param {number} y1 - 线段起点Y坐标
   * @param {number} x2 - 线段终点X坐标
   * @param {number} y2 - 线段终点Y坐标
   * @param {number} px - 点X坐标
   * @param {number} py - 点Y坐标
   * @returns {number} 最短距离
   */
  function getLineSegmentDistance(x1, y1, x2, y2, px, py) {
    const dx = x2 - x1
    const dy = y2 - y1
    const lengthSquared = dx * dx + dy * dy
    if (lengthSquared === 0) return getDistance(x1, y1, px, py)  // 线段长度为0，直接计算点距离

    // 计算点在直线上的投影参数（0~1表示在线段内）
    const projection = clamp(((px - x1) * dx + (py - y1) * dy) / lengthSquared, 0, 1)
    const closestX = x1 + dx * projection
    const closestY = y1 + dy * projection
    return getDistance(closestX, closestY, px, py)
  }

  /**
   * 对敌人应用伤害
   * @param {object} enemy - 敌人对象
   * @param {number} damage - 伤害值
   * @param {object} options - 选项
   * @param {boolean} options.refreshWord - 是否刷新敌人单词
   * @param {string} options.source - 伤害来源
   * @param {number} options.sourceDamage - 原始伤害值（用于计算爆炸等连锁伤害）
   */
  function applyDamageToEnemy(enemy, damage, { refreshWord = false, source = 'bullet', sourceDamage = damage } = {}) {
    if (damage > 0) {
      createDamageText(enemy.x, enemy.y, damage, source)  // 创建伤害数字显示
    }

    enemy.health -= damage                               // 扣除生命值
    enemy.incomingDamage = Math.max(0, enemy.incomingDamage - damage)  // 减少预扣伤害
    enemy.hitFeedback = 0.22                             // 设置被击中反馈
    enemy.lastDamageSource = source                      // 记录伤害来源
    enemy.lastSourceDamage = sourceDamage                // 记录原始伤害值
    if (enemy.health <= 0) {
      enemy.deathSource = source                         // 标记死亡来源
      enemy.deathSourceDamage = sourceDamage
    }
    if (refreshWord && enemy.health > 0) {
      triggerEnemyWordRefresh(enemy)                     // 触发单词刷新
    }
  }

  /**
   * 触发爆炸技能效果（击杀敌人时触发）
   * 对周围敌人造成范围伤害
   * @param {object} defeatedEnemy - 被击杀的敌人
   */
  function triggerBlastExplosion(defeatedEnemy) {
    const blastLevel = getSkillLevel('blast')
    if (blastLevel <= 0) return                         // 未解锁爆炸技能
    if (defeatedEnemy.deathSource !== 'bullet') return  // 只对子弹击杀生效

    const radius = getSkillValue('blast', 'radius')
    const damageMultiplier = getSkillValue('blast', 'damageMultiplier')
    const minimumDamageRatio = getSkillValue('blast', 'minimumDamageRatio')
    const baseDamage = Math.max(0, Number(defeatedEnemy.deathSourceDamage) || 0)

    if (radius <= 0 || baseDamage <= 0) return          // 参数无效

    createExplosionEffect(defeatedEnemy.x, defeatedEnemy.y, radius)  // 创建爆炸特效
    triggerExplosionShake()                                       // 触发震动效果

    // 对范围内的敌人造成伤害
    for (const enemy of enemies.value) {
      if (enemy.id === defeatedEnemy.id) continue      // 跳过被击杀的敌人
      if (enemy.health <= 0) continue                  // 跳过已死亡的敌人

      const distance = getDistance(defeatedEnemy.x, defeatedEnemy.y, enemy.x, enemy.y)
      if (distance > radius) continue                  // 超出爆炸范围

      // 距离越近伤害越高
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

  /**
   * 选择敌人的替换单词（确保不与当前单词相同）
   * @param {object} enemy - 敌人对象
   * @returns {object} 新单词对象
   */
  function pickReplacementWord(enemy) {
    let nextWord = pickWordForWave(Math.max(1, wave.value))
    let attempts = 0

    // 最多尝试8次，确保新单词与当前单词不同
    while (nextWord.word.toLowerCase() === enemy.displayWord && attempts < 8) {
      nextWord = pickWordForWave(Math.max(1, wave.value))
      attempts += 1
    }

    return nextWord
  }

  /**
   * 触发敌人单词刷新（子弹击中时可能触发）
   * @param {object} enemy - 敌人对象
   */
  function triggerEnemyWordRefresh(enemy) {
    if (enemy.wordTransitionState !== 'idle' || enemy.health <= 0) return  // 正在切换或已死亡则不刷新

    enemy.pendingWord = pickReplacementWord(enemy)  // 选择新单词
    markWordRecentlyUsed(enemy.pendingWord)         // 标记已使用
    enemy.wordTransitionState = 'fade-out'          // 开始淡出动画
    enemy.wordTransitionTimer = wordTransitionDuration
  }

  /**
   * 构建技能选择项
   * @param {object} skill - 技能对象
   * @param {string} mode - 模式（new: 新技能, upgrade: 升级）
   * @returns {object} 技能选择项对象
   */
  function buildSkillChoice(skill, mode) {
    const currentLevel = getSkillLevel(skill.id)
    const nextLevel = mode === 'new' ? 1 : currentLevel + 1  // 新技能从1级开始，升级则+1
    const maxLevel = getSkillMaxLevel(skill.id)

    return {
      choiceId: `${skill.id}-${mode}-${nextLevel}`,  // 唯一选择ID
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

  /**
   * 构建技能选择列表（每次关卡结束后显示）
   * 优先展示新技能，其次展示可升级技能，最后使用兜底
   * @returns {array} 技能选择项列表（最多3个）
   */
  function buildSkillChoices() {
    const currentIds = new Set(cards.value.map((card) => card.id))  // 当前已装备的技能ID集合
    const choices = []
    // 筛选未解锁的新技能
    const newSkills = TYPE_WARRIOR_SKILL_POOL.filter((skill) => !currentIds.has(skill.id))
    // 筛选可升级的技能
    const upgradeSkills = cards.value
      .filter((card) => !isSkillAtMaxLevel(card.id, card.level))
      .map((card) => TYPE_WARRIOR_SKILL_POOL.find((skill) => skill.id === card.id))
      .filter(Boolean)

    // 第一步：优先选择新技能（最多3个）
    if (cards.value.length < maxCards && newSkills.length > 0) {
      for (const skill of pickRandomItems(newSkills, 3)) {
        choices.push(buildSkillChoice(skill, 'new'))
      }
    }

    // 第二步：补充可升级技能（凑够3个）
    if (choices.length < 3 && upgradeSkills.length > 0) {
      for (const skill of pickRandomItems(upgradeSkills, 3 - choices.length)) {
        choices.push(buildSkillChoice(skill, 'upgrade'))
      }
    }

    // 第三步：兜底选择（确保有3个选项）
    if (choices.length < 3) {
      const fallbackSkills = TYPE_WARRIOR_SKILL_POOL.filter((skill) => {
        if (choices.some((choice) => choice.id === skill.id)) return false  // 排除已选技能
        const currentLevel = getSkillLevel(skill.id)
        return currentLevel === 0 || !isSkillAtMaxLevel(skill.id, currentLevel)  // 未解锁或未满级
      })
      for (const skill of pickRandomItems(fallbackSkills, 3 - choices.length)) {
        const mode = currentIds.has(skill.id) ? 'upgrade' : 'new'
        choices.push(buildSkillChoice(skill, mode))
      }
    }

    return choices.slice(0, 3)  // 确保最多3个选项
  }

  // ==================== 游戏流程控制 ====================

  /**
   * 打开技能选择界面（关卡结束后调用）
   * @param {number} nextWave - 下一关关卡号
   */
  function openSkillSelection(nextWave) {
    const nextChoices = buildSkillChoices()
    // 如果没有可选技能（所有技能都已解锁并满级），直接进入下一关
    if (nextChoices.length === 0) {
      pendingWaveNumber.value = null
      isChoosingSkill.value = false
      wave.value = nextWave
      weaponLevel.value = clamp(weaponLevel.value + COMBAT_BALANCE.skillChoiceWeaponLevelGain, 1, COMBAT_BALANCE.maxWeaponLevel)  // 武器升级
      banner.value = `第 ${nextWave} 关开始。`
      startWave(nextWave)
      // 恢复关卡结束时的生命值奖励
      if (pendingWaveEndHeal.value > 0) {
        health.value = clamp(health.value + pendingWaveEndHeal.value, 0, maxHealth.value)
      }
      pendingWaveEndHeal.value = 0
      return
    }

    // 设置技能选择状态
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

  /**
   * 刷新技能选择列表
   * @returns {boolean} 是否刷新成功
   */
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

  /**
   * 切换游戏暂停状态
   */
  function togglePause() {
    // 在游戏未开始、结束、胜利或选择技能时不能暂停
    if (!hasGameStarted.value || isGameOver.value || isVictory.value || isChoosingSkill.value) return
    isPaused.value = !isPaused.value
    banner.value = isPaused.value ? '游戏已暂停。' : '游戏继续。'
  }

  /**
   * 结束游戏（主动放弃）
   */
  function endGame() {
    if (!hasGameStarted.value || isGameOver.value || isVictory.value) return
    isPaused.value = false
    isChoosingSkill.value = false
    isGameOver.value = true
    // 清空输入状态
    typedBuffer.value = ''
    selectedMatchLength.value = 0
    targetEnemyId.value = null
    matchedEnemyIds.value = new Set()
    resetPurgeWordState()
    banner.value = '本局已主动结束，数据已进入结算。'
  }

  /**
   * 开始游戏
   */
  async function startGame() {
    await loadWordPool()  // 加载单词池
    hasGameStarted.value = true
    restartGame()         // 初始化游戏状态
    banner.value = '游戏开始，保持节奏清理词潮。'
  }

  /**
   * 应用技能选择（选择技能后调用）
   * @param {object} choice - 选中的技能选择项
   */
  function applySkillChoice(choice) {
    const existingIndex = cards.value.findIndex((card) => card.id === choice.id)

    if (existingIndex >= 0) {
      // 升级现有技能
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
      // 添加新技能
      const skill = TYPE_WARRIOR_SKILL_POOL.find((item) => item.id === choice.id)
      if (skill) {
        cards.value = [...cards.value, { ...skill, level: 1, maxLevel: getSkillMaxLevel(skill.id) }]
      }
    }

    // 同步派生属性（生命值上限、能量上限）
    syncDerivedStats({ restoreHealth: true, restoreEnergy: true })

    const nextWave = pendingWaveNumber.value
    isChoosingSkill.value = false
    pendingWaveNumber.value = null
    skillChoices.value = []
    resetPurgeWordState()

    // 进入下一关
    if (nextWave !== null) {
      wave.value = nextWave
      weaponLevel.value = clamp(weaponLevel.value + COMBAT_BALANCE.skillChoiceWeaponLevelGain, 1, COMBAT_BALANCE.maxWeaponLevel)  // 武器升级
      banner.value = `${choice.name} 已装配，第 ${nextWave} 关开始。`
      startWave(nextWave)
      // 恢复关卡结束时的生命值奖励
      if (pendingWaveEndHeal.value > 0) {
        health.value = clamp(health.value + pendingWaveEndHeal.value, 0, maxHealth.value)
      }
    }

    pendingWaveEndHeal.value = 0
  }

  /**
   * 调试专用：直接授予技能（无需等待关卡奖励）
   * 通过将所有入口点保持在页面本地，该功能可以方便地移除
   * @param {string} skillId - 技能ID
   */
  function grantSkillById(skillId) {
    const skill = TYPE_WARRIOR_SKILL_POOL.find((item) => item.id === skillId)
    if (!skill) return

    const existingIndex = cards.value.findIndex((card) => card.id === skillId)
    if (existingIndex >= 0) {
      // 升级现有技能
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
      // 添加新技能
      cards.value = [...cards.value, { ...skill, level: 1, maxLevel: getSkillMaxLevel(skill.id) }]
    }

    // 同步派生属性
    syncDerivedStats({ restoreHealth: true, restoreEnergy: true })
    banner.value = `${skill.name} 已通过调试面板加入当前局内。`
  }

  /**
   * 调试专用：清空所有已装备技能（保留当前游戏进程）
   */
  function resetSkills() {
    cards.value = []
    syncDerivedStats()
    health.value = clamp(health.value, 0, maxHealth.value)
    energy.value = clamp(energy.value, 0, maxEnergy.value)
    banner.value = '当前技能已清空，便于继续调试。'
  }

  /**
   * 调试专用：跳转到指定关卡
   * 保留当前调试技能，但重建战斗场景，防止旧敌人、子弹、输入缓冲区和覆盖层泄漏到新关卡
   * @param {number} waveNumber - 目标关卡号
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
    completedWaveCount.value = Math.max(0, selectedWave - 1)  // 设置已完成关卡数
    effectiveTypingSeconds.value = 0
    pendingWaveNumber.value = null
    pendingWaveEndHeal.value = 0
    resetPurgeWordState()
    syncDerivedStats({ restoreHealth: true, restoreEnergy: true })
    markEnemyKeywordTrieDirty()
    startWave(selectedWave)
    banner.value = `调试模式：已切换到第 ${selectedWave} 关。`
  }

  /**
   * 开始新的关卡
   * @param {number} currentWave - 当前关卡数
   */
  function startWave(currentWave) {
    const waveProfile = getTypeWarriorWaveProfile(currentWave)

    currentWaveProfile.value = waveProfile
    waveSpawned.value = 0                                  // 已生成敌人数重置
    waveTargetCount.value = waveProfile.normal.totalCount  // 设置目标敌人数
    bossSpawned.value = 0                                  // 已生成首领数重置
    bossTargetCount.value = waveProfile.boss.totalCount    // 设置目标首领数
    spawnCooldown.value = getSpawnInterval(waveProfile.normal.spawnRatePerSecond)  // 设置敌人生成间隔
    bossSpawnCooldown.value = hasConfiguredBosses(waveProfile) ? 0 : Number.POSITIVE_INFINITY  // 设置首领生成间隔
    bossMinionCooldown.value = hasConfiguredBosses(waveProfile) ? BOSS_BALANCE.minionInitialDelay : 0  // 设置小怪生成延迟
    bossState.value = hasConfiguredBosses(waveProfile) ? 'pending' : 'idle'  // 设置首领状态
    banner.value = hasConfiguredBosses(waveProfile)
      ? `第 ${currentWave} 关首领逼近，准备应对压场与吐词。`
      : `第 ${currentWave} 关开始，保持节奏清理词潮。`
  }

  /**
   * 重新开始游戏（重置所有状态）
   */
  function restartGame() {
    resetWordSelectionState()
    // 重置玩家状态
    wave.value = 1
    weaponLevel.value = 1
    health.value = PLAYER_BALANCE.baseHealth
    maxHealth.value = PLAYER_BALANCE.baseHealth
    energy.value = PLAYER_BALANCE.baseEnergy
    maxEnergy.value = PLAYER_BALANCE.baseMaxEnergy
    combo.value = 0
    // 重置输入状态
    typedBuffer.value = ''
    selectedMatchLength.value = 0
    // 重置技能状态
    freezeTimer.value = 0
    // 重置游戏对象
    enemies.value = []
    markEnemyKeywordTrieDirty()
    bullets.value = []
    enemyFragments.value = []
    explosionEffects.value = []
    damageTexts.value = []
    cards.value = []
    // 重置游戏状态标志
    isGameOver.value = false
    isVictory.value = false
    isChoosingSkill.value = false
    skillChoices.value = []
    bossState.value = 'idle'
    // 重置生成状态
    waveSpawned.value = 0
    waveTargetCount.value = 7
    bossSpawned.value = 0
    bossTargetCount.value = 0
    spawnCooldown.value = COMBAT_BALANCE.initialSpawnCooldown
    bossSpawnCooldown.value = 0
    bossMinionCooldown.value = 0
    damageCooldown.value = 0
    // 重置时间和统计
    lastTypedAt.value = performance.now()
    typingBurst.value = 0
    survivalSeconds.value = 0
    // 重置目标状态
    targetEnemyId.value = null
    matchedEnemyIds.value = new Set()
    playerHitFeedback.value = 0
    // 重置特效状态
    keyBursts.value = []
    comboFeedbackCount.value = 0
    comboFeedbackTimer.value = 0
    comboShakeTimer.value = 0
    explosionShakeTimer.value = 0
    isPaused.value = false
    // 重置结算统计
    maxCombo.value = 0
    score.value = 0
    solvedWordCount.value = 0
    typedLetterCount.value = 0
    totalKillCount.value = 0
    completedWaveCount.value = 0
    effectiveTypingSeconds.value = 0
    // 重置关卡状态
    pendingWaveNumber.value = null
    pendingWaveEndHeal.value = 0
    currentWaveProfile.value = getTypeWarriorWaveProfile(1)
    markEnemyKeywordTrieDirty()
    resetPurgeWordState()
    banner.value = '直接输入屏幕内可见敌人的英文单词即可自动锁定目标。'
    syncDerivedStats()
    startWave(1)  // 开始第一关
  }

  /**
   * 复活游戏（游戏失败后使用）
   * @returns {boolean} 是否复活成功
   */
  function reviveGame() {
    if (!isGameOver.value || isVictory.value) return false
    isGameOver.value = false
    isPaused.value = false
    health.value = maxHealth.value  // 恢复满生命值
    // 重置输入状态
    typedBuffer.value = ''
    selectedMatchLength.value = 0
    targetEnemyId.value = null
    matchedEnemyIds.value = new Set()
    damageCooldown.value = 2  // 给予2秒无敌时间
    playerHitFeedback.value = 0
    resetPurgeWordState()
    banner.value = '已使用复活券，生命恢复并继续战斗。'
    return true
  }

  /**
   * 获取一个活着的首领敌人（随机选择）
   * @returns {object|null} 首领敌人对象
   */
  function getBossEnemy() {
    const aliveBosses = enemies.value.filter((enemy) => enemy.boss && enemy.health > 0)
    return randomFrom(aliveBosses) ?? null
  }

  /**
   * 获取活着的首领数量
   * @returns {number} 首领数量
   */
  function getAliveBossCount() {
    return enemies.value.filter((enemy) => enemy.boss && enemy.health > 0).length
  }

  /**
   * 同步首领状态
   */
  function syncBossState() {
    if (getAliveBossCount() > 0) {
      bossState.value = 'active'  // 有活着的首领，状态为活跃
      return
    }
    // 根据是否还有未生成的首领，决定状态为pending或idle
    bossState.value = bossSpawned.value < bossTargetCount.value ? 'pending' : 'idle'
  }

  // ==================== 敌人和子弹系统 ====================

  /**
   * 获取敌人到中心的距离
   * @param {object} enemy - 敌人对象
   * @returns {number} 距离
   */
  function getEnemyDistance(enemy) {
    return getDistance(enemy.x, enemy.y, centerPoint, centerPoint)
  }

  /**
   * 判断敌人是否可见（单词可匹配）
   * 只要敌人的任何可见部分到达视口，单词就可以匹配，而不仅仅是敌人中心点进入视野
   * @param {object} enemy - 敌人对象
   * @returns {boolean} 是否可见
   */
  function isEnemyVisible(enemy) {
    const bounds = viewportVisibleBounds.value
    const directionX = centerPoint - enemy.x
    const directionY = centerPoint - enemy.y
    const distance = getDistance(enemy.x, enemy.y, centerPoint, centerPoint) || 1
    const unitX = directionX / distance
    const unitY = directionY / distance
    const tagOffset = enemy.boss ? 80 : 60   // 单词标签相对于敌人的偏移量
    const tagReach = enemy.boss ? 126 : 96   // 单词标签的可见范围
    const enemyPadding = enemy.radius + 10   // 敌人碰撞体的额外padding

    // 检查敌人本体是否在可见范围内
    const bodyVisible =
      enemy.x + enemyPadding >= bounds.left &&
      enemy.x - enemyPadding <= bounds.right &&
      enemy.y + enemyPadding >= bounds.top &&
      enemy.y - enemyPadding <= bounds.bottom

    if (bodyVisible) return true

    // 计算单词标签的位置（在敌人朝向中心的反方向上方）
    const tagCenterX = enemy.x - unitX * tagOffset
    const tagCenterY = enemy.y - unitY * tagOffset

    // 检查单词标签是否在可见范围内
    return (
      tagCenterX + tagReach >= bounds.left &&
      tagCenterX - tagReach <= bounds.right &&
      tagCenterY + 30 >= bounds.top &&
      tagCenterY - 30 <= bounds.bottom
    )
  }

  /**
   * 获取敌人的有效生命值（扣除即将到来的伤害）
   * @param {object} enemy - 敌人对象
   * @returns {number} 有效生命值
   */
  function getEnemyEffectiveHealth(enemy) {
    return enemy.health - (enemy.incomingDamage ?? 0)
  }

  /**
   * 标记敌人关键词前缀树需要重新构建
   */
  function markEnemyKeywordTrieDirty() {
    enemyKeywordTrieDirty = true
  }

  /**
   * 确保敌人关键词前缀树已构建（懒加载）
   */
  function ensureEnemyKeywordTrie() {
    if (!enemyKeywordTrieDirty) return

    enemyKeywordTrie = buildEnemyKeywordTrie(enemies.value.filter((enemy) => enemy.health > 0))
    enemyKeywordTrieDirty = false
  }

  /**
   * 根据输入缓冲区查找匹配的敌人单词
   * 使用前缀树实现高效的模糊匹配
   * @param {string} buffer - 输入缓冲区
   * @param {boolean} visibleOnly - 是否只匹配可见敌人
   * @returns {object} 匹配结果
   */
  function findPrefixMatchEntries(buffer, visibleOnly = true) {
    if (!buffer) {
      return {
        matches: [],
        matchLength: 0,
      }
    }

    ensureEnemyKeywordTrie()  // 确保前缀树已构建
    const enemyById = new Map(enemies.value.map((enemy) => [enemy.id, enemy]))  // 创建ID到敌人的映射

    // 使用前缀树查找最佳匹配
    const result = findBestTrieSuffixPrefixMatches(enemyKeywordTrie, buffer, (enemyId, matchLength) => {
      const enemy = enemyById.get(enemyId)
      if (!enemy) return null                              // 敌人不存在
      if (visibleOnly && !isEnemyVisible(enemy)) return null  // 不可见
      if (getEnemyEffectiveHealth(enemy) <= 0) return null   // 已死亡

      return {
        enemy,
        matchLength,
        distance: getEnemyDistance(enemy),
      }
    })

    // 按距离排序（最近的优先）
    return {
      matchLength: result.matchLength,
      matches: result.matches.sort((left, right) => getEnemyDistance(left.enemy) - getEnemyDistance(right.enemy)),
    }
  }

  /**
   * 根据输入缓冲区同步锁定目标
   * @param {string} preferredId - 首选目标ID（保持目标连续性）
   * @returns {object|null} 匹配结果
   */
  function syncTargetByBuffer(preferredId = targetEnemyId.value) {
    const buffer = typedBuffer.value
    if (!buffer) {
      // 清空目标状态
      targetEnemyId.value = null
      selectedMatchLength.value = 0
      matchedEnemyIds.value = new Set()
      return null
    }

    const prefixResult = findPrefixMatchEntries(buffer, true)
    matchedEnemyIds.value = new Set(prefixResult.matches.map((entry) => entry.enemy.id))
    selectedMatchLength.value = prefixResult.matchLength

    // 如果首选目标仍然在匹配列表中，继续锁定它
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

    // 否则选择最近的匹配目标
    const nextEntry = prefixResult.matches[0] ?? null
    targetEnemyId.value = nextEntry?.enemy.id ?? null
    return nextEntry
  }

  /**
   * 触发敌人错误反馈（输入错误时调用）
   * @param {string} enemyId - 敌人ID
   */
  function triggerEnemyError(enemyId) {
    // 先重置所有敌人的错误反馈
    for (const enemy of enemies.value) {
      enemy.errorFeedback = 0
    }

    const enemy = enemies.value.find((item) => item.id === enemyId)
    if (!enemy) return
    enemy.errorFeedback = 0.42  // 设置错误反馈计时器
  }

  /**
   * 获取当前目标的匹配长度
   * @param {object} enemy - 敌人对象
   * @returns {number} 匹配长度
   */
  function getCurrentTargetMatchLength(enemy) {
    if (!matchedEnemyIds.value.has(enemy.id)) return 0
    return selectedMatchLength.value
  }

  /**
   * 获取敌人单词的已匹配和未匹配部分
   * @param {object} enemy - 敌人对象
   * @returns {object} 包含matched和rest字段的对象
   */
  function getEnemyWordParts(enemy) {
    const matchLength = getCurrentTargetMatchLength(enemy)
    return {
      matched: enemy.displayWord.slice(0, matchLength),  // 已匹配部分
      rest: enemy.displayWord.slice(matchLength),        // 未匹配部分
    }
  }

  /**
   * 获取清屏技能单词的已输入和未输入部分
   * @returns {object} 包含matched和rest字段的对象
   */
  function getPurgeWordParts() {
    const matchLength = purgeWordState.value.buffer.length
    return {
      matched: purgeWordState.value.word.slice(0, matchLength),  // 已输入部分
      rest: purgeWordState.value.word.slice(matchLength),        // 未输入部分
    }
  }

  /**
   * 随机选择一个活着的敌人
   * @param {array} excludeIds - 需要排除的敌人ID列表
   * @returns {object|null} 敌人对象
   */
  function pickRandomAliveEnemy(excludeIds = []) {
    const excludeIdSet = new Set(excludeIds)
    const candidates = enemies.value.filter((enemy) => enemy.health > 0 && !excludeIdSet.has(enemy.id))
    return randomFrom(candidates)
  }

  /**
   * 获取子弹的伤害来源类型
   * @param {object} bullet - 子弹对象
   * @returns {string} 伤害来源（echo/split/bullet）
   */
  function getBulletDamageSource(bullet) {
    if (bullet.bulletKind === 'echo') return 'echo'
    if (bullet.bulletKind === 'split') return 'split'
    return 'bullet'
  }

  /**
   * 生成回声追踪弹（击杀敌人时触发）
   * 从被击杀敌人位置发射追踪弹攻击其他敌人
   * @param {object} defeatedEnemy - 被击杀的敌人
   */
  function spawnEchoSeekers(defeatedEnemy) {
    const echoCount = getSkillValue('echo', 'killSeekers')
    if (echoCount <= 0) return                     // 未解锁回声技能
    if (defeatedEnemy.deathSource !== 'bullet') return  // 只对子弹击杀生效

    const damageMultiplier = getSkillValue('echo', 'damageMultiplier')
    const speedMultiplier = getSkillValue('echo', 'speedMultiplier')
    const baseDamage = Math.max(0, Number(defeatedEnemy.deathSourceDamage) || Number(defeatedEnemy.lastSourceDamage) || 0)
    if (baseDamage <= 0) return                    // 基础伤害无效

    // 生成追踪弹
    for (let index = 0; index < echoCount; index += 1) {
      const target = pickRandomAliveEnemy([defeatedEnemy.id])  // 随机选择目标（排除被击杀的敌人）
      if (!target) break

      bullets.value.push({
        id: `bullet-${bulletIdSeed++}`,
        bulletKind: 'echo',           // 子弹类型：回声弹
        targetId: target.id,          // 目标ID
        x: defeatedEnemy.x,           // 起始位置
        y: defeatedEnemy.y,
        lastX: defeatedEnemy.x,
        lastY: defeatedEnemy.y,
        damage: baseDamage * damageMultiplier,  // 伤害（基础伤害 * 倍率）
        speed: COMBAT_BALANCE.baseProjectileSpeed * speedMultiplier + index * 18,  // 速度（逐个递增）
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
        flightMode: 'tracking',       // 飞行模式：追踪
        lifetime: 0,
      })
    }
  }

  /**
   * 生成分裂子弹（子弹击中敌人时触发）
   * 从击中位置向多个方向发射分裂子弹
   * @param {object} sourceBullet - 原始子弹
   * @param {object} hitEnemy - 被击中的敌人
   */
  function spawnSplitBullets(sourceBullet, hitEnemy) {
    const splitCount = getSkillValue('split', 'childCount')
    if (splitCount <= 0 || !sourceBullet.canSplit) return  // 未解锁分裂技能或子弹不可分裂

    const speedMultiplier = getSkillValue('split', 'speedMultiplier')
    const splitLifetime = getSkillValue('split', 'lifetime')
    const angleStepDegrees = getSkillValue('split', 'angleStepDegrees')
    const baseDirectionX = sourceBullet.directionX || 1
    const baseDirectionY = sourceBullet.directionY || 0
    const baseAngle = Math.atan2(baseDirectionY, baseDirectionX)  // 原始方向角度
    const angleStep = (angleStepDegrees * Math.PI) / 180          // 角度步进（弧度）

    // 生成分裂子弹（对称分布）
    for (let index = 0; index < splitCount; index += 1) {
      const angleOffset = (index - (splitCount - 1) / 2) * angleStep  // 对称角度偏移
      const nextAngle = baseAngle + angleOffset

      bullets.value.push({
        id: `bullet-${bulletIdSeed++}`,
        bulletKind: 'split',           // 子弹类型：分裂弹
        canSplit: false,               // 分裂弹不再分裂
        targetId: null,                // 无目标（直线飞行）
        x: hitEnemy.x,                 // 起始位置（敌人位置）
        y: hitEnemy.y,
        lastX: hitEnemy.x,
        lastY: hitEnemy.y,
        damage: sourceBullet.damage * 0.7,  // 伤害衰减为原始的70%
        speed: sourceBullet.speed * speedMultiplier + index * 12,  // 速度递增
        trail: 0,
        directionX: Math.cos(nextAngle),  // 方向
        directionY: Math.sin(nextAngle),
        solidTrailEnabled: sourceBullet.solidTrailEnabled,  // 继承穿透和固痕属性
        solidTrailMultiplier: sourceBullet.solidTrailMultiplier ?? 0,
        pierceEnabled: sourceBullet.pierceEnabled,
        pierceTrailMultiplier: sourceBullet.pierceTrailMultiplier ?? 0,
        refreshesWordOnHit: false,
        piercedEnemyIds: [hitEnemy.id],        // 记录已穿透的敌人
        solidHitEnemyIds: [hitEnemy.id],       // 记录已固痕的敌人
        flightMode: 'straight',                // 飞行模式：直线
        lifetime: splitLifetime,               // 存活时间
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


