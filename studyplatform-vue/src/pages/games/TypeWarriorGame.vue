<script setup>
import TypeWarriorGamePage from './type-warrior/TypeWarriorGamePage.vue'

const emit = defineEmits(['back'])
</script>

<template>
  <TypeWarriorGamePage @back="emit('back')" />
</template>
