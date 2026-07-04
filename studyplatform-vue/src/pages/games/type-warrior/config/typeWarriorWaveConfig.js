/**
 * 关卡总控配置。
 * `totalWaves` 控制总关卡数，后续如果要增加到 12 关、15 关，只需要改这里。
 * 如果新增的关卡没有手动写入 `TYPE_WARRIOR_WAVE_PROFILES`，系统会自动用末尾模板外推。
 */
export const TYPE_WARRIOR_WAVE_SETTINGS = {
  totalWaves: 10,
}

/**
 * 单关默认生成模板。
 * 只要某一关没有显式覆盖，就会从这里继承基础字段。
 */
export const TYPE_WARRIOR_WAVE_DEFAULT = {
  normal: {
    totalCount: 5,
    spawnRatePerSecond: 1.2,
    spawnProbabilityRange: [0.92, 1],
    healthMultiplierRange: [0.95, 1.08],
  },
  boss: {
    totalCount: 0,
    spawnRatePerSecond: 0.18,
    spawnProbabilityRange: [1, 1],
    healthMultiplierRange: [1, 1],
  },
}

/**
 * 逐关生成配置。
 * 这里是你后续手动调关卡难度的主文件：
 * 1. `normal.totalCount`：该关普通单词总量
 * 2. `normal.spawnRatePerSecond`：普通单词单位时间生成频率
 * 3. `normal.spawnProbabilityRange`：普通单词生成概率区间
 * 4. `normal.healthMultiplierRange`：普通单词血量倍率区间
 * 5. `boss.totalCount`：该关 boss 数量
 * 6. `boss.spawnRatePerSecond`：boss 单位时间生成频率
 * 7. `boss.spawnProbabilityRange`：boss 生成概率区间
 * 8. `boss.healthMultiplierRange`：boss 血量倍率区间
 */
export const TYPE_WARRIOR_WAVE_PROFILES = {
  1: {
    normal: {
      totalCount: 5,
      spawnRatePerSecond: 1.05,
      spawnProbabilityRange: [1, 1],
      healthMultiplierRange: [0.88, 0.98],
    },
  },
  2: {
    normal: {
      totalCount: 6,
      spawnRatePerSecond: 1.12,
      spawnProbabilityRange: [0.94, 1],
      healthMultiplierRange: [0.92, 1.02],
    },
  },
  3: {
    normal: {
      totalCount: 6,
      spawnRatePerSecond: 1.16,
      spawnProbabilityRange: [0.95, 1],
      healthMultiplierRange: [0.96, 1.08],
    },
    boss: {
      totalCount: 1,
      spawnRatePerSecond: 0.2,
      spawnProbabilityRange: [1, 1],
      healthMultiplierRange: [0.96, 1.06],
    },
  },
  4: {
    normal: {
      totalCount: 8,
      spawnRatePerSecond: 1.22,
      spawnProbabilityRange: [0.94, 1],
      healthMultiplierRange: [1.0, 1.12],
    },
  },
  5: {
    normal: {
      totalCount: 9,
      spawnRatePerSecond: 1.28,
      spawnProbabilityRange: [0.93, 1],
      healthMultiplierRange: [1.04, 1.16],
    },
  },
  6: {
    normal: {
      totalCount: 9,
      spawnRatePerSecond: 1.32,
      spawnProbabilityRange: [0.94, 1],
      healthMultiplierRange: [1.06, 1.18],
    },
    boss: {
      totalCount: 1,
      spawnRatePerSecond: 0.2,
      spawnProbabilityRange: [1, 1],
      healthMultiplierRange: [1.06, 1.18],
    },
  },
  7: {
    normal: {
      totalCount: 10,
      spawnRatePerSecond: 1.36,
      spawnProbabilityRange: [0.93, 1],
      healthMultiplierRange: [1.08, 1.22],
    },
  },
  8: {
    normal: {
      totalCount: 11,
      spawnRatePerSecond: 1.42,
      spawnProbabilityRange: [0.92, 1],
      healthMultiplierRange: [1.12, 1.28],
    },
  },
  9: {
    normal: {
      totalCount: 12,
      spawnRatePerSecond: 1.48,
      spawnProbabilityRange: [0.92, 1],
      healthMultiplierRange: [1.16, 1.34],
    },
  },
  10: {
    normal: {
      totalCount: 12,
      spawnRatePerSecond: 1.52,
      spawnProbabilityRange: [0.92, 1],
      healthMultiplierRange: [1.18, 1.36],
    },
    boss: {
      totalCount: 1,
      spawnRatePerSecond: 0.22,
      spawnProbabilityRange: [1, 1],
      healthMultiplierRange: [1.22, 1.42],
    },
  },
}

function mergeWaveSection(defaultSection, overrideSection = {}) {
  return {
    ...defaultSection,
    ...overrideSection,
  }
}

/**
 * 返回总关卡数。
 * 运行时胜利判定直接读取这里，不再依赖其他配置文件。
 */
export function getTypeWarriorFinalWave() {
  return Math.max(1, TYPE_WARRIOR_WAVE_SETTINGS.totalWaves)
}

function getLastDefinedWaveNumber() {
  const waveNumbers = Object.keys(TYPE_WARRIOR_WAVE_PROFILES).map(Number).filter((waveNumber) => Number.isFinite(waveNumber))
  return waveNumbers.length > 0 ? Math.max(...waveNumbers) : 1
}

function buildExtendedWaveProfile(waveNumber) {
  const lastDefinedWaveNumber = getLastDefinedWaveNumber()
  const lastDefinedProfile = TYPE_WARRIOR_WAVE_PROFILES[lastDefinedWaveNumber] ?? {}
  const extraWaveCount = Math.max(0, waveNumber - lastDefinedWaveNumber)
  const baseNormal = mergeWaveSection(TYPE_WARRIOR_WAVE_DEFAULT.normal, lastDefinedProfile.normal)
  const baseBoss = mergeWaveSection(TYPE_WARRIOR_WAVE_DEFAULT.boss, lastDefinedProfile.boss)
  const isFinalWave = waveNumber >= getTypeWarriorFinalWave()

  return {
    normal: {
      ...baseNormal,
      totalCount: baseNormal.totalCount + extraWaveCount,
      spawnRatePerSecond: baseNormal.spawnRatePerSecond + extraWaveCount * 0.05,
      spawnProbabilityRange: [Math.max(0.9, baseNormal.spawnProbabilityRange[0] - extraWaveCount * 0.01), baseNormal.spawnProbabilityRange[1]],
      healthMultiplierRange: [
        baseNormal.healthMultiplierRange[0] + extraWaveCount * 0.04,
        baseNormal.healthMultiplierRange[1] + extraWaveCount * 0.05,
      ],
    },
    boss: isFinalWave
      ? {
          ...baseBoss,
          totalCount: Math.max(1, baseBoss.totalCount),
          spawnRatePerSecond: Math.max(baseBoss.spawnRatePerSecond, 0.22),
          spawnProbabilityRange: [1, 1],
          healthMultiplierRange: [
            Math.max(baseBoss.healthMultiplierRange[0], 1.2 + extraWaveCount * 0.05),
            Math.max(baseBoss.healthMultiplierRange[1], 1.4 + extraWaveCount * 0.06),
          ],
        }
      : {
          ...TYPE_WARRIOR_WAVE_DEFAULT.boss,
        },
  }
}

/**
 * 返回指定关卡的完整生成配置。
 * 如果该关没有手动写配置，则基于最后一关模板自动外推。
 */
export function getTypeWarriorWaveProfile(waveNumber) {
  const overrideProfile = TYPE_WARRIOR_WAVE_PROFILES[waveNumber]

  if (!overrideProfile) {
    return buildExtendedWaveProfile(waveNumber)
  }

  return {
    normal: mergeWaveSection(TYPE_WARRIOR_WAVE_DEFAULT.normal, overrideProfile.normal),
    boss: mergeWaveSection(TYPE_WARRIOR_WAVE_DEFAULT.boss, overrideProfile.boss),
  }
}
