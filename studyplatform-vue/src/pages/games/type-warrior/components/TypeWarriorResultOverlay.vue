<script setup>
/**
 * Renders the pause, failure, and victory statistic overlay for Type Warrior.
 */
defineEmits(['end-game', 'restart', 'resume'])

defineProps({
  isPaused: {
    type: Boolean,
    default: false,
  },
  isGameOver: {
    type: Boolean,
    required: true,
  },
  isVictory: {
    type: Boolean,
    required: true,
  },
  resultStats: {
    type: Object,
    required: true,
  },
  wave: {
    type: Number,
    required: true,
  },
  weaponLevel: {
    type: Number,
    required: true,
  },
})
</script>

<template>
  <div v-if="isPaused || isGameOver || isVictory" class="type-warrior-overlay">
    <div class="type-warrior-overlay-panel">
      <p>{{ isPaused ? '游戏暂停' : isVictory ? '通关完成' : '战斗结束' }}</p>
      <h2>{{ isPaused ? '暂停中' : isVictory ? 'type warrior 通关' : '本轮挑战结束' }}</h2>
      <span>当前关卡 {{ wave }} / 武器等级 {{ weaponLevel.toFixed(0) }}</span>

      <div class="type-warrior-result-grid">
        <div class="type-warrior-result-item">
          <strong>{{ resultStats.maxCombo }}</strong>
          <span>最大连击</span>
        </div>
        <div class="type-warrior-result-item">
          <strong>{{ resultStats.score }}</strong>
          <span>得分</span>
        </div>
        <div class="type-warrior-result-item">
          <strong>{{ resultStats.coins }}</strong>
          <span>金币</span>
        </div>
        <div class="type-warrior-result-item">
          <strong>{{ resultStats.completedWaves }}</strong>
          <span>闯过关卡</span>
        </div>
        <div class="type-warrior-result-item">
          <strong>{{ resultStats.solvedWords }}</strong>
          <span>拼对单词</span>
        </div>
        <div class="type-warrior-result-item">
          <strong>{{ resultStats.totalKills }}</strong>
          <span>总击杀数</span>
        </div>
        <div class="type-warrior-result-item">
          <strong>{{ resultStats.durationSeconds.toFixed(1) }} 秒</strong>
          <span>用时</span>
        </div>
        <div class="type-warrior-result-item">
          <strong>{{ resultStats.killsPerSecond.toFixed(2) }}</strong>
          <span>每秒击杀数</span>
        </div>
        <div class="type-warrior-result-item">
          <strong>{{ resultStats.wordsPerSecond.toFixed(2) }}</strong>
          <span>每秒单词数</span>
        </div>
        <div class="type-warrior-result-item">
          <strong>{{ resultStats.lettersPerSecond.toFixed(2) }}</strong>
          <span>每秒字母数</span>
        </div>
      </div>

      <div class="type-warrior-overlay-actions">
        <button v-if="isPaused" type="button" @click="$emit('resume')">继续游戏</button>
        <button v-if="isPaused" type="button" class="is-danger" @click="$emit('end-game')">结束游戏</button>
        <button type="button" @click="$emit('restart')">重新开始</button>
      </div>
    </div>
  </div>
</template>
