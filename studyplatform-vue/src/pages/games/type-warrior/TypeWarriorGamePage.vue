<script setup>
/**
 * Type Warrior游戏主页面
 * 整合游戏战场、HUD界面、技能选择和结果覆盖层，管理游戏核心逻辑
 */
import { ref, watch } from 'vue'
import '../../../assets/games/type-warrior.css'
import { saveTypeWarriorRecord } from '../../../api/games'
import { TYPE_WARRIOR_BALANCE, TYPE_WARRIOR_SKILL_POOL } from './config/typeWarriorConfig'
import { getTypeWarriorFinalWave } from './config/typeWarriorWaveConfig'
import TypeWarriorArena from './components/TypeWarriorArena.vue'
import TypeWarriorHud from './components/TypeWarriorHud.vue'
import TypeWarriorResultOverlay from './components/TypeWarriorResultOverlay.vue'
import TypeWarriorSkillDebugPanel from './components/TypeWarriorSkillDebugPanel.vue'
import TypeWarriorSkillSelectionOverlay from './components/TypeWarriorSkillSelectionOverlay.vue'
import TypeWarriorWaveDebugPanel from './components/TypeWarriorWaveDebugPanel.vue'
import { useTypeWarriorGame } from './composables/useTypeWarriorGame'
import { useUserVouchers } from '../../../composables/useUserVouchers'
import { VOUCHER_KEYS } from '../../../api/vouchers'

/** 定义返回事件 */
const emit = defineEmits(['back'])

/** 是否启用技能调试面板 */
const enableSkillDebugPanel = TYPE_WARRIOR_BALANCE.ui.showSkillDebugPanel
/** 是否启用波次调试面板 */
const enableWaveDebugPanel = TYPE_WARRIOR_BALANCE.ui.showWaveDebugPanel
/** 最终波次数量 */
const debugWaveCount = getTypeWarriorFinalWave()
/** 游戏记录是否已保存 */
const typeWarriorRecordSaved = ref(false)

/** 用户券相关操作 */
const {
  errorMessage: voucherErrorMessage,
  getQuantity,
  consumeVoucher,
} = useUserVouchers()

/** 游戏核心逻辑组合式函数 */
const {
  arenaRef,
  banner,
  boardClass,
  bullets,
  cards,
  combo,
  comboFeedbackCount,
  comboFeedbackTimer,
  currentProjectileDamage,
  currentTarget,
  damageTexts,
  enemies,
  enemyFragments,
  explosionEffects,
  energy,
  freezeTimer,
  freezeStatusLabel,
  health,
  hasGameStarted,
  endGame,
  isChoosingSkill,
  isCriticalHealth,
  isGameOver,
  isPaused,
  isVictory,
  isWordPoolLoading,
  keyBursts,
  purgeCooldownLabel,
  purgeWordState,
  resultStats,
  skillChoices,
  playerRingStyle,
  playerShellClass,
  restartGame,
  refreshSkillChoices,
  reviveGame,
  startGame,
  hudStageHint,
  hudStageLabel,
  survivalSeconds,
  togglePause,
  wave,
  weaponLevel,
  wpmLike,
  applySkillChoice,
  bulletStyle,
  damageTextStyle,
  debugSelectWave,
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
  grantSkillById,
  keyBurstStyle,
  resetSkills,
} = useTypeWarriorGame()

/** 处理开始游戏 */
function handleStartGame() {
  typeWarriorRecordSaved.value = false
  startGame()
}

/**
 * 处理重新开始游戏
 * 游戏结束或胜利时先保存记录再重新开始
 */
function handleRestartGame() {
  if (hasGameStarted.value && (isGameOver.value || isVictory.value) && !typeWarriorRecordSaved.value) {
    syncTypeWarriorRecord()
  }
  typeWarriorRecordSaved.value = false
  restartGame()
}

/** 处理结束游戏并保存记录 */
function handleEndGame() {
  endGame()
  syncTypeWarriorRecord()
}

/**
 * 处理调试选择波次
 * @param {number} waveNumber - 波次编号
 */
function handleDebugSelectWave(waveNumber) {
  typeWarriorRecordSaved.value = false
  debugSelectWave(waveNumber)
}

/**
 * 处理刷新技能选择
 * 消耗技能刷新券后刷新技能选项
 */
async function handleRefreshSkillChoices() {
  if (getQuantity(VOUCHER_KEYS.TYPE_WARRIOR_SKILL_REFRESH) <= 0) {
    return
  }
  const consumed = await consumeVoucher(VOUCHER_KEYS.TYPE_WARRIOR_SKILL_REFRESH)
  if (consumed) {
    refreshSkillChoices()
  }
}

/**
 * 处理复活游戏
 * 消耗复活券后调用游戏复活逻辑
 */
async function handleReviveGame() {
  if (getQuantity(VOUCHER_KEYS.GAME_REVIVE) <= 0) {
    return
  }
  const consumed = await consumeVoucher(VOUCHER_KEYS.GAME_REVIVE)
  if (consumed && reviveGame()) {
    typeWarriorRecordSaved.value = false
  }
}

/** 同步保存游戏记录到服务器 */
function syncTypeWarriorRecord() {
  if (typeWarriorRecordSaved.value) {
    return
  }

  typeWarriorRecordSaved.value = true
  saveTypeWarriorRecord({
    reachedWave: resultStats.value.reachedWave,
    completedWaveCount: resultStats.value.completedWaves,
    score: resultStats.value.score,
    maxCombo: resultStats.value.maxCombo,
    solvedWordCount: resultStats.value.solvedWords,
    totalKillCount: resultStats.value.totalKills,
    typedLetterCount: resultStats.value.typedLetters,
    durationSeconds: Number(resultStats.value.durationSeconds.toFixed(2)),
    effectiveTypingSeconds: Number(resultStats.value.effectiveTypingSeconds.toFixed(2)),
  }).catch(() => {
    typeWarriorRecordSaved.value = false
  })
}

/** 监听游戏状态变化，自动保存记录 */
watch(
  () => [isGameOver.value, isVictory.value, hasGameStarted.value],
  ([gameOver, victory, started]) => {
    if (!started || (!gameOver && !victory) || typeWarriorRecordSaved.value) {
      return
    }
    if (gameOver && !victory && getQuantity(VOUCHER_KEYS.GAME_REVIVE) > 0) {
      return
    }

    syncTypeWarriorRecord()
  },
)
</script>

<template>
  <!-- 游戏页面主容器（低血量时添加危险状态样式） -->
  <section class="type-warrior-page" :class="{ 'is-critical-health': isCriticalHealth }">
    <!-- 游戏棋盘区域 -->
    <section class="type-warrior-board" :class="boardClass">
      <!-- 游戏状态HUD -->
      <TypeWarriorHud
        :cards="cards"
        :combo="combo"
        :combo-feedback-count="comboFeedbackCount"
        :combo-feedback-timer="comboFeedbackTimer"
        :current-projectile-damage="currentProjectileDamage"
        :freeze-status-label="freezeStatusLabel"
        :is-paused="isPaused"
        :purge-cooldown-label="purgeCooldownLabel"
        :stage-hint="hudStageHint"
        :stage-label="hudStageLabel"
        :get-skill-max-level="getSkillMaxLevel"
        :weapon-level="weaponLevel"
        :wpm-like="wpmLike"
        @back="emit('back')"
        @toggle-pause="togglePause"
      />

      <!-- 游戏战场区域 -->
      <TypeWarriorArena
        :arena-ref="arenaRef"
        :banner="banner"
        :bullets="bullets"
        :current-target="currentTarget"
        :damage-texts="damageTexts"
        :enemies="enemies"
        :enemy-fragments="enemyFragments"
        :explosion-effects="explosionEffects"
        :energy="energy"
        :freeze-timer="freezeTimer"
        :health="health"
        :key-bursts="keyBursts"
        :purge-word-state="purgeWordState"
        :player-ring-style="playerRingStyle"
        :player-shell-class="playerShellClass"
        :survival-seconds="survivalSeconds"
        :bullet-style="bulletStyle"
        :damage-text-style="damageTextStyle"
        :enemy-health-style="enemyHealthStyle"
        :enemy-style="enemyStyle"
        :enemy-word-transition-style="enemyWordTransitionStyle"
        :explosion-style="explosionStyle"
        :fragment-style="fragmentStyle"
        :get-enemy-word-parts="getEnemyWordParts"
        :get-purge-word-parts="getPurgeWordParts"
        :is-enemy-bullet-target="isEnemyBulletTarget"
        :is-enemy-matched-target="isEnemyMatchedTarget"
        :key-burst-style="keyBurstStyle"
        :show-field-rings="true"
      />

      <!-- 技能调试面板（开发模式） -->
      <TypeWarriorSkillDebugPanel
        v-if="enableSkillDebugPanel"
        :get-skill-max-level="getSkillMaxLevel"
        :skills="TYPE_WARRIOR_SKILL_POOL"
        :equipped-skills="cards"
        @grant-skill="grantSkillById"
        @reset-skills="resetSkills"
      />

      <!-- 波次调试面板（开发模式） -->
      <TypeWarriorWaveDebugPanel
        v-if="enableWaveDebugPanel"
        :current-wave="wave"
        :total-waves="debugWaveCount"
        @select-wave="handleDebugSelectWave"
      />

      <!-- 游戏开始覆盖层 -->
      <div v-if="!hasGameStarted" class="type-warrior-start-overlay">
        <div class="type-warrior-start-panel">
          <p>type warrior</p>
          <h2>开始游戏</h2>
          <span>输入敌人上方的英文单词即可自动锁定开火。按 `Esc` 暂停，按 `1` 触发主动技能。</span>
          <button type="button" class="type-warrior-start-button" :disabled="isWordPoolLoading" @click="handleStartGame">
            {{ isWordPoolLoading ? '词库加载中...' : '开始游戏' }}
          </button>
        </div>
      </div>
    </section>

    <!-- 危险状态暗角效果 -->
    <div v-if="isCriticalHealth" class="type-warrior-danger-vignette" aria-hidden="true"></div>
    <!-- 冻结状态暗角效果 -->
    <div v-if="freezeTimer > 0" class="type-warrior-freeze-vignette" aria-hidden="true"></div>

    <!-- 技能选择覆盖层 -->
    <TypeWarriorSkillSelectionOverlay
      :visible="isChoosingSkill"
      :skill-choices="skillChoices"
      :refresh-available="getQuantity(VOUCHER_KEYS.TYPE_WARRIOR_SKILL_REFRESH) > 0"
      :refresh-count="getQuantity(VOUCHER_KEYS.TYPE_WARRIOR_SKILL_REFRESH)"
      @choose="applySkillChoice"
      @refresh="handleRefreshSkillChoices"
    />

    <!-- 结果覆盖层（暂停/游戏结束/胜利） -->
    <TypeWarriorResultOverlay
      :is-paused="isPaused"
      :is-game-over="isGameOver"
      :is-victory="isVictory"
      :result-stats="resultStats"
      :revive-available="getQuantity(VOUCHER_KEYS.GAME_REVIVE) > 0"
      :revive-count="getQuantity(VOUCHER_KEYS.GAME_REVIVE)"
      :wave="wave"
      :weapon-level="weaponLevel"
      @end-game="handleEndGame"
      @revive="handleReviveGame"
      @resume="togglePause"
      @restart="handleRestartGame"
    />

    <!-- 券操作错误提示 -->
    <p v-if="voucherErrorMessage" class="type-warrior-voucher-error">{{ voucherErrorMessage }}</p>
  </section>
</template>
