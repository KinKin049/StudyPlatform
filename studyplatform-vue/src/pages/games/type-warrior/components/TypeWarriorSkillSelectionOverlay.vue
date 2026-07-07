<script setup>
/**
 * 关卡之间的技能选择层。
 * 玩家必须在三项候选技能中选一项，战斗才会继续。
 */
defineEmits(['choose', 'refresh'])

defineProps({
  refreshAvailable: {
    type: Boolean,
    default: false,
  },
  refreshCount: {
    type: Number,
    default: 0,
  },
  skillChoices: {
    type: Array,
    required: true,
  },
  visible: {
    type: Boolean,
    required: true,
  },
})
</script>

<template>
  <div v-if="visible" class="type-warrior-skill-overlay">
    <div class="type-warrior-skill-panel">
      <p>技能选择</p>
      <h2>选择一项技能</h2>
      <span>当前局内没有初始技能，每次过关后从三项候选技能中选择一项继续推进。</span>
      <button
        type="button"
        class="type-warrior-skill-refresh"
        :disabled="!refreshAvailable"
        @click="$emit('refresh')"
      >
        刷新技能券 x{{ refreshCount }}
      </button>

      <div class="type-warrior-skill-grid">
        <button
          v-for="choice in skillChoices"
          :key="choice.choiceId"
          type="button"
          class="type-warrior-skill-choice"
          @click="$emit('choose', choice)"
        >
          <em>{{ choice.badge }}</em>
          <strong>{{ choice.name }}</strong>
          <span>{{ choice.type }}</span>
          <p>{{ choice.description }}</p>
        </button>
      </div>
    </div>
  </div>
</template>
