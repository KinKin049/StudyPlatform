/**
 * Type Warrior 基础常量。
 * 这里放不会频繁改动的场景尺寸、碰撞范围、输入缓存长度等公共常量。
 */
export const TYPE_WARRIOR_CONFIG = {
  arenaSize: 760,
  playerCollisionRadius: 64,
  maxCards: 5,
  keyBurstDuration: 0.68,
  spawnOffset: 28,
  rollingBufferLimit: 36,
}

/**
 * Type Warrior 统一数值调参表。
 * 你后续要改角色、敌人、技能等级、调试面板开关，优先在这个文件里改。
 */
export const TYPE_WARRIOR_BALANCE = {
  ui: {
    // 是否显示左下角技能调试面板。正式上线时改为 false 即可隐藏。
    showSkillDebugPanel: false,
  },
  player: {
    // 玩家基础属性。
    baseHealth: 100,
    baseEnergy: 72,
    baseMaxEnergy: 100,
    criticalHealthThreshold: 30,
  },
  combat: {
    // 玩家输出、能量、碰撞、按键反馈等通用战斗数值。
    baseDamage: 16,
    weaponDamagePerLevel: 5,
    comboDamageScale: 1.2,
    typingBurstScale: 1.4,
    baseProjectileSpeed: 520,
    projectileSpeedStep: 32,
    wordEnergyGain: 8,
    nonBossKillEnergyGain: 3,
    bossKillEnergyGain: 18,
    collisionDamage: 6,
    collisionEnergyLossBase: 12,
    collisionEnergyLossPerEnemy: 2,
    nonBossKillScore: 120,
    bossKillScore: 1200,
    initialSpawnCooldown: 0.1,
    wordTransitionDuration: 0.14,
    skillChoiceWeaponLevelGain: 1,
    maxWeaponLevel: 12,
    keyBurst: {
      normalDistanceBase: 34,
      normalDistanceRange: 18,
      failedDistanceBase: 40,
      failedDistanceRange: 20,
      normalLift: 10,
      failedLift: 12,
    },
  },
  waves: {
    baseTargetCount: 3,
    targetCountPerWave: 2,
    bossWaveSpawnDelay: 0.24,
    normalWaveSpawnDelay: 0.55,
    activeSpawnFreeze: 999,
    normalSpawnMinInterval: 0.72,
    normalSpawnBaseInterval: 1.42,
    normalSpawnWaveStep: 0.08,
  },
  boss: {
    // boss 本体移动、吐词、环绕等数值。
    baseHealth: 420,
    healthPerWave: 48,
    baseSpeed: 4.6,
    speedPerWave: 0.3,
    accent: '#111111',
    radius: 30,
    orbitRadius: 308,
    orbitEnterDistance: 338,
    emissionDuration: 0.34,
    emissionDistance: 64,
    emissionJitter: 0.18,
    maxMinions: 9,
    minionInitialDelay: 4.6,
    minionInterval: 4.2,
    minionLaunchSpeed: 196,
    minionLaunchDeceleration: 240,
    minionChaseAcceleration: 164,
  },
  enemies: {
    // 普通敌人的基础成长。
    healthPerWave: 4,
    speedPerWave: 1.05,
    dotRadius: 12,
    shapedRadius: 16,
  },
  skills: {
    // 下方每个技能数组都按“索引 = 技能等级”读取。
    // 例如 rapid.energyOnComplete[3] 表示 rapid 技能 3 级时的数值。
    rapid: {
      energyOnComplete: [0, 4, 8, 12, 16, 20],
    },
    burst: {
      projectileDamageBonus: [0, 3, 6, 9, 12, 15],
      weaponGrowthPerKill: [0, 0.05, 0.08, 0.11, 0.14, 0.17],
    },
    echo: {
      extraProjectiles: [0, 1, 2, 3, 4, 5],
    },
    shield: {
      maxHealthBonus: [0, 12, 24, 36, 48, 60],
    },
    focus: {
      comboDamageBonusPerCombo: [0, 0.6, 1.2, 1.8, 2.4, 3.0],
    },
    beam: {
      flatDamageBonus: [0, 2, 4, 6, 8, 10],
    },
    reserve: {
      maxEnergyBonus: [0, 18, 36, 54, 72, 90],
      killEnergyBonus: [0, 2, 4, 6, 8, 10],
      bossKillEnergyBonus: [0, 3, 6, 9, 12, 15],
    },
    overclock: {
      damageBonus: [0, 4, 8, 12, 16, 20],
      projectileSpeedBonus: [0, 72, 144, 216, 288, 360],
    },
    repair: {
      onHitHeal: [0, 1, 2, 3, 4, 5],
      onSkillPickHeal: [0, 10, 12, 14, 16, 18],
    },
    solid: {
      trailDamageMultiplier: [0, 1.0, 1.15, 1.3, 1.45, 1.6],
    },
    pierce: {
      trailDamageMultiplier: [0, 1.0, 1.15, 1.3, 1.45, 1.6],
      lifetime: [0, 1.8, 2.0, 2.2, 2.4, 2.6],
    },
    purge: {
      cooldownSeconds: 60,
      maxUsesPerWave: 1,
    },
  },
}

export const TYPE_WARRIOR_WORD_BANK = [
  { text: '石油', word: 'oil', tier: 1 },
  { text: '岩心', word: 'core', tier: 1 },
  { text: '测井', word: 'logging', tier: 1 },
  { text: '算法', word: 'algorithm', tier: 1 },
  { text: '数据', word: 'data', tier: 1 },
  { text: '图像', word: 'image', tier: 1 },
  { text: '压裂', word: 'fracture', tier: 2 },
  { text: '钻井', word: 'drilling', tier: 2 },
  { text: '储层', word: 'reservoir', tier: 2 },
  { text: '地震', word: 'seismic', tier: 2 },
  { text: '网络', word: 'network', tier: 2 },
  { text: '模型', word: 'model', tier: 2 },
  { text: '孔隙度', word: 'porosity', tier: 3 },
  { text: '渗透率', word: 'permeability', tier: 3 },
  { text: '采收率', word: 'recovery', tier: 3 },
  { text: '可视化', word: 'visual', tier: 3 },
  { text: '程序设计', word: 'coding', tier: 3 },
  { text: '人工智能', word: 'intelligence', tier: 4 },
  { text: '地层压力', word: 'pressure', tier: 4 },
  { text: '油气层', word: 'payzone', tier: 4 },
]

export const TYPE_WARRIOR_ACTIVE_SKILL_WORD_BANK = [
  { text: '清屏', word: 'purge' },
  { text: '新星', word: 'nova' },
  { text: '风暴', word: 'storm' },
  { text: '扫荡', word: 'sweep' },
  { text: '涡旋', word: 'vortex' },
]

export const TYPE_WARRIOR_SKILL_POOL = [
  { id: 'rapid', name: '急速校准', type: '被动', description: '成功命中后额外恢复能量。' },
  { id: 'burst', name: '爆发框架', type: '被动', description: '每次击杀都会加快武器成长并强化弹体伤害。' },
  { id: 'echo', name: '回声连射', type: '被动', description: '每次完整拼对单词会额外发射追加子弹。' },
  { id: 'shield', name: '护场编织', type: '被动', description: '提升生命上限并增强容错。' },
  { id: 'focus', name: '连击聚焦', type: '被动', description: '连击层数会额外提高每次命中的伤害。' },
  { id: 'beam', name: '束流强化', type: '被动', description: '子弹命中主目标时附加固定伤害。' },
  { id: 'reserve', name: '能量储备', type: '被动', description: '提高能量上限，并强化击杀后的回能。' },
  { id: 'overclock', name: '过载弹群', type: '被动', description: '提高子弹飞行速度与基础伤害。' },
  { id: 'repair', name: '稳态修复', type: '被动', description: '命中敌人与获得技能时恢复生命值。' },
  { id: 'solid', name: '实体子弹', type: '被动', description: '子弹飞向主目标途中会伤害沿途敌人。' },
  { id: 'pierce', name: '子弹穿透', type: '被动', description: '子弹命中目标后沿原路径继续前进，并伤害后续敌人。' },
  { id: 'purge', name: '清屏指令', type: '主动', description: '按 1 激活，冷却 60 秒，每回合最多使用一次。' },
]

export const TYPE_WARRIOR_ENEMY_KINDS = [
  { type: 'dot', shape: 'dot', baseHealth: 12, baseSpeed: 20, accent: '#d85d57' },
  { type: 'triangle', shape: 'triangle', baseHealth: 18, baseSpeed: 17, accent: '#c86b56' },
  { type: 'square', shape: 'square', baseHealth: 26, baseSpeed: 14, accent: '#bb6d63' },
]
