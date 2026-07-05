<script setup>
defineEmits(['select-wave'])

defineProps({
  currentWave: {
    type: Number,
    required: true,
  },
  totalWaves: {
    type: Number,
    required: true,
  },
})
</script>

<template>
  <aside class="type-warrior-wave-debug" aria-label="关卡调试面板">
    <div class="type-warrior-skill-debug-header">
      <div>
        <p>调试面板</p>
        <strong>选择关卡</strong>
      </div>
      <span>当前 {{ currentWave }}</span>
    </div>

    <div class="type-warrior-wave-debug-grid">
      <button
        v-for="waveNumber in totalWaves"
        :key="waveNumber"
        type="button"
        class="type-warrior-wave-debug-item"
        :class="{ 'is-current': waveNumber === currentWave }"
        @click="$emit('select-wave', waveNumber)"
      >
        {{ waveNumber }}
      </button>
    </div>
  </aside>
</template>
