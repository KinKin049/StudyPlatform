<script setup>
/**
 * 万题天梯跳游戏覆盖层组件
 * 显示暂停和游戏结束状态，提供继续、复活、重新开始和结束操作按钮
 */
defineProps({
  /** 是否有复活券可用 */
  reviveAvailable: {
    type: Boolean,
    default: false,
  },
  /** 复活券数量 */
  reviveCount: {
    type: Number,
    default: 0,
  },
  /** 是否处于暂停状态 */
  isPaused: {
    type: Boolean,
    required: true,
  },
  /** 是否游戏结束 */
  isGameOver: {
    type: Boolean,
    required: true,
  },
  /** 覆盖层标题 */
  overlayTitle: {
    type: String,
    required: true,
  },
  /** 当前金币数量 */
  score: {
    type: Number,
    required: true,
  },
  /** 覆盖层副标题 */
  overlaySubtitle: {
    type: String,
    required: true,
  },
  /** 本局统计数据列表 */
  overlayStats: {
    type: Array,
    required: true,
  },
})

/** 定义游戏操作事件 */
const emit = defineEmits(['resume', 'restart', 'finish', 'revive'])
</script>

<template>
  <!-- 游戏覆盖层（暂停或结束时显示） -->
  <div v-if="isPaused || isGameOver" class="ladder-game-over">
    <p>{{ overlayTitle }}</p>
    <h2>{{ score }} 金币</h2>
    <span class="ladder-overlay-subtitle">{{ overlaySubtitle }}</span>

    <!-- 本局统计信息 -->
    <section class="ladder-overlay-stats" aria-label="本局统计">
      <article v-for="item in overlayStats" :key="item.label">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <!-- 操作按钮 -->
    <div class="ladder-overlay-actions">
      <button v-if="isPaused" type="button" @click="emit('resume')">继续游戏</button>
      <button v-if="isGameOver" type="button" :disabled="!reviveAvailable" @click="emit('revive')">
        使用复活券 x{{ reviveCount }}
      </button>
      <button type="button" @click="emit('restart')">重新开始</button>
      <button v-if="isPaused" type="button" class="is-danger" @click="emit('finish')">立即结束</button>
    </div>
  </div>
</template>
