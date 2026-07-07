<script setup>
/**
 * 万题天梯跳游戏入口组件
 * 作为游戏平台与实际游戏页面之间的桥梁，传递返回事件
 */
import LadderJumpGamePage from './ladder-jump/LadderJumpGamePage.vue'

/** 定义返回事件 */
const emit = defineEmits(['back'])
</script>

<template>
  <!-- 渲染万题天梯跳游戏主页面 -->
  <LadderJumpGamePage @back="emit('back')" />
</template>
