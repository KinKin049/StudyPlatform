<script setup>
/**
 * 对局结束弹层。
 * 通关和失败共用这一套结果展示。
 */
defineEmits(['restart'])

defineProps({
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
  <div v-if="isGameOver || isVictory" class="type-warrior-overlay">
    <div class="type-warrior-overlay-panel">
      <p>{{ isVictory ? '通关完成' : '战斗结束' }}</p>
      <h2>{{ isVictory ? 'type warrior通关' : '本轮挑战结束' }}</h2>
      <span>到达关卡 {{ wave }} / 武器等级 {{ weaponLevel.toFixed(0) }}</span>

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
          <strong>{{ resultStats.completedWaves }}</strong>
          <span>闯过关卡</span>
        </div>
        <div class="type-warrior-result-item">
          <strong>{{ resultStats.solvedWords }}</strong>
          <span>拼对单词</span>
        </div>
        <div class="type-warrior-result-item">
          <strong>{{ resultStats.durationSeconds.toFixed(1) }}秒</strong>
          <span>用时</span>
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

      <button type="button" @click="$emit('restart')">重新开始</button>
    </div>
  </div>
</template>
