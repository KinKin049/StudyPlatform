<script setup>
import '../../../assets/games/type-warrior.css'
import { TYPE_WARRIOR_BALANCE, TYPE_WARRIOR_SKILL_POOL } from './config/typeWarriorConfig'
import TypeWarriorArena from './components/TypeWarriorArena.vue'
import TypeWarriorHud from './components/TypeWarriorHud.vue'
import TypeWarriorResultOverlay from './components/TypeWarriorResultOverlay.vue'
import TypeWarriorSkillDebugPanel from './components/TypeWarriorSkillDebugPanel.vue'
import TypeWarriorSkillSelectionOverlay from './components/TypeWarriorSkillSelectionOverlay.vue'
import { useTypeWarriorGame } from './composables/useTypeWarriorGame'

const emit = defineEmits(['back'])

// 是否显示技能调试面板由配置文件统一控制。
// 调参时改 `TYPE_WARRIOR_BALANCE.ui.showSkillDebugPanel` 即可。
const enableSkillDebugPanel = TYPE_WARRIOR_BALANCE.ui.showSkillDebugPanel

// 页面本身只负责组装展示组件，运行时状态和战斗逻辑都在 composable 内。
const {
  arenaRef,
  banner,
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
  health,
  hasGameStarted,
  isChoosingSkill,
  isCriticalHealth,
  isGameOver,
  isPaused,
  isVictory,
  keyBursts,
  purgeCooldownLabel,
  resultStats,
  skillChoices,
  playerRingStyle,
  playerShellClass,
  restartGame,
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
  enemyHealthStyle,
  enemyStyle,
  enemyWordTransitionStyle,
  fragmentStyle,
  getEnemyWordParts,
  grantSkillById,
  keyBurstStyle,
  resetSkills,
} = useTypeWarriorGame()
</script>

<template>
  <section class="type-warrior-page" :class="{ 'is-critical-health': isCriticalHealth }">
    <section class="type-warrior-board" :class="boardClass">
      <TypeWarriorHud
        :cards="cards"
        :combo="combo"
        :combo-feedback-count="comboFeedbackCount"
        :combo-feedback-timer="comboFeedbackTimer"
        :is-paused="isPaused"
        :purge-cooldown-label="purgeCooldownLabel"
        :stage-hint="hudStageHint"
        :stage-label="hudStageLabel"
        :weapon-level="weaponLevel"
        :wpm-like="wpmLike"
        @back="emit('back')"
        @toggle-pause="togglePause"
      />

      <TypeWarriorArena
        :arena-ref="arenaRef"
        :banner="banner"
        :bullets="bullets"
        :current-target="currentTarget"
        :enemies="enemies"
        :enemy-fragments="enemyFragments"
        :energy="energy"
        :health="health"
        :key-bursts="keyBursts"
        :player-ring-style="playerRingStyle"
        :player-shell-class="playerShellClass"
        :survival-seconds="survivalSeconds"
        :bullet-style="bulletStyle"
        :enemy-health-style="enemyHealthStyle"
        :enemy-style="enemyStyle"
        :enemy-word-transition-style="enemyWordTransitionStyle"
        :fragment-style="fragmentStyle"
        :get-enemy-word-parts="getEnemyWordParts"
        :key-burst-style="keyBurstStyle"
        :show-field-rings="true"
      />

      <TypeWarriorSkillDebugPanel
        v-if="enableSkillDebugPanel"
        :skills="TYPE_WARRIOR_SKILL_POOL"
        :equipped-skills="cards"
        @grant-skill="grantSkillById"
        @reset-skills="resetSkills"
      />

      <div v-if="!hasGameStarted" class="type-warrior-start-overlay">
        <div class="type-warrior-start-panel">
          <p>type warrior</p>
          <h2>开始游戏</h2>
          <span>输入敌人上方的英文单词击退词潮。按 Esc 暂停，按 1 使用主动技能。</span>
          <button type="button" class="type-warrior-start-button" @click="startGame">开始游戏</button>
        </div>
      </div>
    </section>

    <div v-if="isCriticalHealth" class="type-warrior-danger-vignette" aria-hidden="true"></div>

    <TypeWarriorSkillSelectionOverlay
      :visible="isChoosingSkill"
      :skill-choices="skillChoices"
      @choose="applySkillChoice"
    />

    <TypeWarriorResultOverlay
      :is-game-over="isGameOver"
      :is-victory="isVictory"
      :result-stats="resultStats"
      :wave="wave"
      :weapon-level="weaponLevel"
      @restart="restartGame"
    />
  </section>
</template>
