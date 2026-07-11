/**
 * Type Warrior 游戏配置文件
 * 定义游戏的基础常量、数值平衡、词库、技能等配置信息
 */

/**
 * Type Warrior 基础常量配置
 * 包含场景尺寸、碰撞范围、输入缓冲长度等不频繁改动的公共常量
 */
export const TYPE_WARRIOR_CONFIG = {
  /** 竞技场尺寸 */
  arenaSize: 760,
  /** 玩家碰撞半径 */
  playerCollisionRadius: 64,
  /** 最大卡片数量 */
  maxCards: 5,
  /** 按键爆发持续时间（秒） */
  keyBurstDuration: 0.68,
  /** 生成偏移量 */
  spawnOffset: 28,
  /** 输入滚动缓冲长度 */
  rollingBufferLimit: 36,
  /** 爆炸效果持续时间（秒） */
  blastEffectDuration: 0.42,
}

/**
 * Type Warrior 统一数值平衡配置
 * 包含玩家、战斗、波次、Boss、敌人、技能等各项数值参数
 */
export const TYPE_WARRIOR_BALANCE = {
  /** UI 调试配置 */
  ui: {
    /** 是否显示技能调试面板 */
    showSkillDebugPanel: false,
    /** 是否显示波次调试面板 */
    showWaveDebugPanel: false,
  },
  /** 玩家属性 */
  player: {
    /** 基础生命值 */
    baseHealth: 100,
    /** 基础能量值 */
    baseEnergy: 72,
    /** 基础最大能量值 */
    baseMaxEnergy: 100,
    /** 临界生命值阈值 */
    criticalHealthThreshold: 30,
  },
  /** 战斗属性 */
  combat: {
    /** 基础伤害 */
    baseDamage: 16,
    /** 每级武器伤害加成 */
    weaponDamagePerLevel: 5,
    /** 连击伤害倍率 */
    comboDamageScale: 1.2,
    /** 打字爆发伤害倍率 */
    typingBurstScale: 1.4,
    /** 基础子弹速度 */
    baseProjectileSpeed: 520,
    /** 子弹速度递增步长 */
    projectileSpeedStep: 32,
    /** 拼词能量获取量 */
    wordEnergyGain: 8,
    /** 击杀非 Boss 能量获取量 */
    nonBossKillEnergyGain: 3,
    /** 击杀 Boss 能量获取量 */
    bossKillEnergyGain: 18,
    /** 碰撞伤害 */
    collisionDamage: 6,
    /** 基础碰撞能量损失 */
    collisionEnergyLossBase: 12,
    /** 每额外敌人碰撞能量损失 */
    collisionEnergyLossPerEnemy: 2,
    /** 击杀非 Boss 得分 */
    nonBossKillScore: 120,
    /** 击杀 Boss 得分 */
    bossKillScore: 1200,
    /** 初始生成冷却时间 */
    initialSpawnCooldown: 0.1,
    /** 词切换持续时间 */
    wordTransitionDuration: 0.14,
    /** 技能选择武器等级提升 */
    skillChoiceWeaponLevelGain: 1,
    /** 最大武器等级 */
    maxWeaponLevel: 12,
    /** 按键爆发效果配置 */
    keyBurst: {
      /** 正常按键基础距离 */
      normalDistanceBase: 34,
      /** 正常按键距离范围 */
      normalDistanceRange: 18,
      /** 失败按键基础距离 */
      failedDistanceBase: 40,
      /** 失败按键距离范围 */
      failedDistanceRange: 20,
      /** 正常按键提升量 */
      normalLift: 10,
      /** 失败按键提升量 */
      failedLift: 12,
    },
    /** 爆炸震动持续时间 */
    explosionShakeDuration: 0.22,
    /** 爆炸震动强度 */
    explosionShakeStrength: 7,
  },
  /** 波次配置 */
  waves: {
    /** 基础目标数量 */
    baseTargetCount: 3,
    /** 每波目标数量递增 */
    targetCountPerWave: 2,
    /** Boss 波次生成延迟 */
    bossWaveSpawnDelay: 0.24,
    /** 普通波次生成延迟 */
    normalWaveSpawnDelay: 0.55,
    /** 激活生成冻结时间 */
    activeSpawnFreeze: 999,
    /** 普通生成最小间隔 */
    normalSpawnMinInterval: 0.72,
    /** 普通生成基础间隔 */
    normalSpawnBaseInterval: 1.42,
    /** 每波生成间隔递减量 */
    normalSpawnWaveStep: 0.08,
  },
  /** Boss 属性 */
  boss: {
    /** 基础生命值 */
    baseHealth: 420,
    /** 每波生命值递增 */
    healthPerWave: 48,
    /** 基础移动速度 */
    baseSpeed: 4.6,
    /** 每波速度递增 */
    speedPerWave: 0.3,
    /** 强调色 */
    accent: '#111111',
    /** 碰撞半径 */
    radius: 30,
    /** 轨道半径 */
    orbitRadius: 308,
    /** 轨道进入距离 */
    orbitEnterDistance: 338,
    /** 轨道角速度 */
    orbitAngularSpeed: 0.552,
    /** 发射持续时间 */
    emissionDuration: 0.34,
    /** 发射距离 */
    emissionDistance: 64,
    /** 发射抖动量 */
    emissionJitter: 0.18,
    /** 生成间隔时间（秒） */
    spawnIntervalSeconds: 30,
    /** 最大随从数量 */
    maxMinions: 9,
    /** 随从初始延迟 */
    minionInitialDelay: 4.6,
    /** 随从生成间隔 */
    minionInterval: 4.2,
    /** 随从发射速度 */
    minionLaunchSpeed: 196,
    /** 随从发射减速 */
    minionLaunchDeceleration: 240,
    /** 随从追击加速度 */
    minionChaseAcceleration: 164,
  },
  /** 敌人属性 */
  enemies: {
    /** 每波生命值递增 */
    healthPerWave: 4,
    /** 每波速度倍率 */
    speedPerWave: 1.05,
    /** 点状敌人半径 */
    dotRadius: 12,
    /** 形状敌人半径 */
    shapedRadius: 16,
    /** 词长度缩放配置 */
    wordLengthScaling: {
      /** 短词最大长度 */
      shortMaxLength: 4,
      /** 中词最大长度 */
      mediumMaxLength: 7,
      /** 长词最大长度 */
      longMaxLength: 10,
      /** 速度倍率 */
      speedMultiplier: {
        short: 1.22,
        medium: 1,
        long: 0.86,
        extraLong: 0.74,
      },
      /** 接触伤害倍率 */
      contactDamageMultiplier: {
        short: 0.72,
        medium: 1,
        long: 1.24,
        extraLong: 1.52,
      },
    },
  },
  /** 技能配置 */
  skills: {
    /** 急速校准 - 成功命中额外恢复能量 */
    rapid: {
      energyOnComplete: [0, 4, 8, 12, 16, 20],
    },
    /** 爆发框架 - 击杀加快武器成长并强化弹体伤害 */
    burst: {
      projectileDamageBonus: [0, 3, 6, 9, 12, 15],
      weaponGrowthPerKill: [0, 0.05, 0.08, 0.11, 0.14, 0.17],
    },
    /** 回声连射 - 普通子弹击杀后额外发射追踪炮弹 */
    echo: {
      killSeekers: [0, 1, 2, 3],
      damageMultiplier: [0, 1.25, 1.25, 1.25],
      speedMultiplier: [0, 0.45, 0.45, 0.45],
      impactEffectRadius: [0, 76, 76, 76],
    },
    /** 护场编织 - 提升生命上限 */
    shield: {
      maxHealthBonus: [0, 12, 24, 36, 48, 60],
    },
    /** 终身治疗 - 固定提升生命上限 */
    lifelong: {
      maxHealthBonus: [0, 10, 20, 30, 40, 50],
    },
    /** 连击聚焦 - 连击层数额外提高伤害 */
    focus: {
      comboDamageBonusPerCombo: [0, 0.6, 1.2, 1.8, 2.4, 3.0],
    },
    /** 束流强化 - 子弹命中附加固定伤害 */
    beam: {
      flatDamageBonus: [0, 2, 4, 6, 8, 10],
    },
    /** 爆炸伤害 - 击杀后引发衰减爆炸 */
    blast: {
      radius: [0, 76, 83.6, 91.96, 101.156, 111.2716],
      damageMultiplier: [0, 1, 1, 1, 1, 1],
      minimumDamageRatio: [0, 0.2, 0.22, 0.24, 0.26, 0.28],
    },
    /** 能量储备 - 提高能量上限并强化击杀回能 */
    reserve: {
      maxEnergyBonus: [0, 18, 36, 54, 72, 90],
      killEnergyBonus: [0, 2, 4, 6, 8, 10],
      bossKillEnergyBonus: [0, 3, 6, 9, 12, 15],
    },
    /** 过载弹群 - 提高子弹速度与基础伤害 */
    overclock: {
      damageBonus: [0, 4, 8, 12, 16, 20],
      projectileSpeedBonus: [0, 72, 144, 216, 288, 360],
    },
    /** 稳态修复 - 击杀回复生命，关卡结束恢复损失生命的一半 */
    repair: {
      onHitHeal: [0, 0],
      onKillHeal: [0, 1],
      waveEndMissingHealthRestoreRatio: 0.5,
    },
    /** 主动防御 - 受到伤害时自动发射反击炮弹 */
    guard: {
      projectileCount: [0, 1, 2, 3],
      damageMultiplier: [0, 1, 1, 1],
    },
    /** 实体子弹 - 子弹飞行途中伤害沿途敌人 */
    solid: {
      trailDamageMultiplier: [0, 1.0],
    },
    /** 子弹穿透 - 命中后继续前进伤害后续敌人 */
    pierce: {
      trailDamageMultiplier: [0, 1.0],
      lifetime: [0, 1.8],
    },
    /** 子弹分裂 - 命中后分裂多枚子弹 */
    split: {
      childCount: [0, 2, 3, 4, 5],
      speedMultiplier: [0, 0.92, 0.92, 0.92, 0.92],
      lifetime: [0, 0.82, 0.82, 0.86, 0.9],
      angleStepDegrees: [0, 10, 10, 10, 10],
    },
    /** 清屏指令 - 清除全部非首领目标 */
    purge: {
      energyCost: 100,
    },
    /** 冰冻 - 降低敌人移动与刷新速度 */
    freeze: {
      energyCost: 70,
      duration: [0, 6],
      speedMultiplier: [0, 0.2],
    },
  },
}

/**
 * Type Warrior 词库配置
 * 定义游戏中出现的词汇，包含中文释义、英文单词和难度等级
 */
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

/**
 * Type Warrior 主动技能词库配置
 * 定义主动技能需要拼写的词汇
 */
export const TYPE_WARRIOR_ACTIVE_SKILL_WORD_BANK = [
  { text: '清屏', word: 'purge' },
  { text: '新星', word: 'nova' },
  { text: '风暴', word: 'storm' },
  { text: '扫荡', word: 'sweep' },
  { text: '涡旋', word: 'vortex' },
]

/**
 * Type Warrior 技能池配置
 * 定义所有可用技能的 ID、名称、类型和描述
 */
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

/**
 * Type Warrior 敌人类型配置
 * 定义不同类型敌人的属性，包括形状、生命值、速度和颜色
 */
export const TYPE_WARRIOR_ENEMY_KINDS = [
  { type: 'dot', shape: 'dot', baseHealth: 12, baseSpeed: 20, accent: '#d85d57' },
  { type: 'triangle', shape: 'triangle', baseHealth: 18, baseSpeed: 17, accent: '#c86b56' },
  { type: 'square', shape: 'square', baseHealth: 26, baseSpeed: 14, accent: '#bb6d63' },
]
