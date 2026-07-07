<script setup>
/**
 * Type Warrior游戏入口组件
 * 作为游戏平台与实际游戏页面之间的桥梁，传递返回事件
 */
import TypeWarriorGamePage from './type-warrior/TypeWarriorGamePage.vue'

/** 定义返回事件 */
const emit = defineEmits(['back'])
</script>

<template>
  <!-- 渲染Type Warrior游戏主页面 -->
  <TypeWarriorGamePage @back="emit('back')" />
</template>
