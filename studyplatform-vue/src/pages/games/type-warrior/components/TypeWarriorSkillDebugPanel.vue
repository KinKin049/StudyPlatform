<script setup>
/**
 * 局内技能调试面板。
 * 是否显示由配置文件统一控制，后续上线时可以一处关闭。
 */
defineEmits(['grant-skill', 'reset-skills'])

defineProps({
  getSkillMaxLevel: {
    type: Function,
    required: true,
  },
  skills: {
    type: Array,
    required: true,
  },
  equippedSkills: {
    type: Array,
    required: true,
  },
})

function getSkillLevel(equippedSkills, skillId) {
  return equippedSkills.find((skill) => skill.id === skillId)?.level ?? 0
}
</script>

<template>
  <aside class="type-warrior-skill-debug" aria-label="技能调试面板">
    <div class="type-warrior-skill-debug-header">
      <div>
        <p>调试面板</p>
        <strong>技能注入</strong>
      </div>
      <button type="button" class="type-warrior-skill-debug-reset" @click="$emit('reset-skills')">清空技能</button>
    </div>

    <div class="type-warrior-skill-debug-grid">
      <button
        v-for="skill in skills"
        :key="skill.id"
        type="button"
        class="type-warrior-skill-debug-item"
        :disabled="getSkillLevel(equippedSkills, skill.id) >= getSkillMaxLevel(skill.id)"
        @click="$emit('grant-skill', skill.id)"
      >
        <em>{{
          getSkillLevel(equippedSkills, skill.id) >= getSkillMaxLevel(skill.id)
            ? '已满级'
            : `等级 ${getSkillLevel(equippedSkills, skill.id)}`
        }}</em>
        <strong>{{ skill.name }}</strong>
        <span>{{ skill.type }}</span>
      </button>
    </div>
  </aside>
</template>
