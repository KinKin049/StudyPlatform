/**
 * Type Warrior 基础常量。
 * 这里放不频繁改动的场景尺寸、碰撞范围、输入缓冲长度等公共常量。
 */
export const TYPE_WARRIOR_CONFIG = {
  arenaSize: 760,
  playerCollisionRadius: 64,
  maxCards: 5,
  keyBurstDuration: 0.68,
  spawnOffset: 28,
  rollingBufferLimit: 36,
  blastEffectDuration: 0.42,
}

/**
 * Type Warrior 统一数值调参表。
 * 角色、敌人、技能、调试开关都优先在这里调整。
 */
export const TYPE_WARRIOR_BALANCE = {
  ui: {
    showSkillDebugPanel: true,
    showWaveDebugPanel: true,
  },
  player: {
    baseHealth: 100,
    baseEnergy: 72,
    baseMaxEnergy: 100,
    criticalHealthThreshold: 30,
  },
  combat: {
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
    explosionShakeDuration: 0.22,
    explosionShakeStrength: 7,
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
    baseHealth: 420,
    healthPerWave: 48,
    baseSpeed: 4.6,
    speedPerWave: 0.3,
    accent: '#111111',
    radius: 30,
    orbitRadius: 308,
    orbitEnterDistance: 338,
    orbitAngularSpeed: 0.552,
    emissionDuration: 0.34,
    emissionDistance: 64,
    emissionJitter: 0.18,
    spawnIntervalSeconds: 30,
    maxMinions: 9,
    minionInitialDelay: 4.6,
    minionInterval: 4.2,
    minionLaunchSpeed: 196,
    minionLaunchDeceleration: 240,
    minionChaseAcceleration: 164,
  },
  enemies: {
    healthPerWave: 4,
    speedPerWave: 1.05,
    dotRadius: 12,
    shapedRadius: 16,
    wordLengthScaling: {
      shortMaxLength: 4,
      mediumMaxLength: 7,
      longMaxLength: 10,
      speedMultiplier: {
        short: 1.22,
        medium: 1,
        long: 0.86,
        extraLong: 0.74,
      },
      contactDamageMultiplier: {
        short: 0.72,
        medium: 1,
        long: 1.24,
        extraLong: 1.52,
      },
    },
  },
  skills: {
    rapid: {
      energyOnComplete: [0, 4, 8, 12, 16, 20],
    },
    burst: {
      projectileDamageBonus: [0, 3, 6, 9, 12, 15],
      weaponGrowthPerKill: [0, 0.05, 0.08, 0.11, 0.14, 0.17],
    },
    echo: {
      killSeekers: [0, 1, 2, 3],
      damageMultiplier: [0, 1.25, 1.25, 1.25],
      speedMultiplier: [0, 0.45, 0.45, 0.45],
      impactEffectRadius: [0, 76, 76, 76],
    },
    shield: {
      maxHealthBonus: [0, 12, 24, 36, 48, 60],
    },
    lifelong: {
      maxHealthBonus: [0, 10, 20, 30, 40, 50],
    },
    focus: {
      comboDamageBonusPerCombo: [0, 0.6, 1.2, 1.8, 2.4, 3.0],
    },
    beam: {
      flatDamageBonus: [0, 2, 4, 6, 8, 10],
    },
    blast: {
      radius: [0, 76, 83.6, 91.96, 101.156, 111.2716],
      damageMultiplier: [0, 1, 1, 1, 1, 1],
      minimumDamageRatio: [0, 0.2, 0.22, 0.24, 0.26, 0.28],
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
      onHitHeal: [0, 0],
      onKillHeal: [0, 1],
      waveEndMissingHealthRestoreRatio: 0.5,
    },
    guard: {
      projectileCount: [0, 1, 2, 3],
      damageMultiplier: [0, 1, 1, 1],
    },
    solid: {
      trailDamageMultiplier: [0, 1.0],
    },
    pierce: {
      trailDamageMultiplier: [0, 1.0],
      lifetime: [0, 1.8],
    },
    split: {
      childCount: [0, 2, 3, 4, 5],
      speedMultiplier: [0, 0.92, 0.92, 0.92, 0.92],
      lifetime: [0, 0.82, 0.82, 0.86, 0.9],
      angleStepDegrees: [0, 10, 10, 10, 10],
    },
    purge: {
      energyCost: 100,
    },
    freeze: {
      energyCost: 70,
      duration: [0, 6],
      speedMultiplier: [0, 0.2],
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
  { id: 'guard', name: '主动防御', type: '被动', description: '角色受到伤害时自动发射反击炮弹，数量等于技能等级，最高 3 级。' },
  { id: 'freeze', name: '冰冻', type: '主动', description: '按 2 立即释放，敌人移动与刷新速度降为原来的 0.2 倍，消耗 70 点能量，最高 1 级。' },
  { id: 'rapid', name: '急速校准', type: '被动', description: '成功命中后额外恢复能量。' },
  { id: 'burst', name: '爆发框架', type: '被动', description: '每次击杀都会加快武器成长并强化弹体伤害。' },
  { id: 'echo', name: '回声连射', type: '被动', description: '普通子弹击杀敌人后会额外发射追踪炮弹。' },
  { id: 'shield', name: '护场编织', type: '被动', description: '提升生命上限并增强容错。' },
  { id: 'focus', name: '连击聚焦', type: '被动', description: '连击层数会额外提高每次命中的伤害。' },
  { id: 'beam', name: '束流强化', type: '被动', description: '子弹命中主目标时附加固定伤害。' },
  { id: 'blast', name: '爆炸伤害', type: '被动', description: '子弹击杀敌人后在周围引发衰减爆炸，中心伤害等同当前子弹伤害。' },
  { id: 'reserve', name: '能量储备', type: '被动', description: '提高能量上限，并强化击杀后的回能。' },
  { id: 'overclock', name: '过载弹群', type: '被动', description: '提高子弹飞行速度与基础伤害。' },
  { id: 'repair', name: '稳态修复', type: '被动', description: '仅击杀敌人时回复 1 点生命，关卡结束后额外恢复当前损失生命的一半。' },
  { id: 'solid', name: '实体子弹', type: '被动', description: '子弹飞向主目标途中会伤害沿途敌人。' },
  { id: 'pierce', name: '子弹穿透', type: '被动', description: '子弹命中目标后沿原路径继续前进，并伤害后续敌人。' },
  { id: 'purge', name: '清屏指令', type: '主动', description: '按 1 激活，拼对指令词后清除全部非首领目标，消耗 100 点能量。' },
  { id: 'lifelong', name: '终身治疗', type: '被动', description: '固定提升生命上限，1 级 +10，每升 1 级再 +10。' },
  { id: 'split', name: '子弹分裂', type: '被动', description: '子弹命中敌人后分裂，1 级分裂 2 枚，每升 1 级再多 1 枚，最高 4 级。' },
]

export const TYPE_WARRIOR_ENEMY_KINDS = [
  { type: 'dot', shape: 'dot', baseHealth: 12, baseSpeed: 20, accent: '#d85d57' },
  { type: 'triangle', shape: 'triangle', baseHealth: 18, baseSpeed: 17, accent: '#c86b56' },
  { type: 'square', shape: 'square', baseHealth: 26, baseSpeed: 14, accent: '#bb6d63' },
]
