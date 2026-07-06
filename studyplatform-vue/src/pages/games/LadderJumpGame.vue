<script setup>
import LadderJumpGamePage from './ladder-jump/LadderJumpGamePage.vue'

const emit = defineEmits(['back'])
</script>

<template>
  <LadderJumpGamePage @back="emit('back')" />
</template>
