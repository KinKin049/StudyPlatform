<script setup>
/**
 * 万题天梯跳游戏HUD组件
 * 显示游戏状态信息（金币、连击、时间、生命值）和操作提示
 */
defineProps({
  /** 当前金币数量 */
  score: {
    type: Number,
    required: true,
  },
  /** 当前连击数 */
  combo: {
    type: Number,
    required: true,
  },
  /** 游戏时间文本 */
  gameTimeText: {
    type: String,
    required: true,
  },
  /** 生命值文本（心形图标） */
  heartText: {
    type: String,
    required: true,
  },
})

/** 定义暂停事件 */
const emit = defineEmits(['pause'])
</script>

<template>
  <!-- 游戏状态统计 -->
  <div class="ladder-stats ladder-floating-stats">
    <span>金币 {{ score }}</span>
    <span>Combo {{ combo }}</span>
    <span>Time {{ gameTimeText }}</span>
    <span class="ladder-hearts">{{ heartText }}</span>
    <button type="button" class="ladder-pause-button" @click="emit('pause')">暂停</button>
  </div>

  <!-- 操作提示 -->
  <aside class="ladder-control-hint">
    <span>A/D 或方向键左右移动</span>
    <span>W / 空格 / 上方向键三级跳</span>
    <span>S / 下方向键下落</span>
    <span>Esc 暂停</span>
  </aside>
</template>
