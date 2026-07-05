<script setup>
/**
 * 游戏头部信息层。
 * 负责展示关卡、提示、暂停按钮、连击反馈和已获得技能。
 */
defineEmits(['back', 'toggle-pause'])

defineProps({
  cards: {
    type: Array,
    required: true,
  },
  combo: {
    type: Number,
    required: true,
  },
  comboFeedbackCount: {
    type: Number,
    required: true,
  },
  comboFeedbackTimer: {
    type: Number,
    required: true,
  },
  currentProjectileDamage: {
    type: Number,
    required: true,
  },
  freezeStatusLabel: {
    type: String,
    required: true,
  },
  isPaused: {
    type: Boolean,
    required: true,
  },
  purgeCooldownLabel: {
    type: String,
    required: true,
  },
  stageHint: {
    type: String,
    required: true,
  },
  stageLabel: {
    type: String,
    required: true,
  },
  getSkillMaxLevel: {
    type: Function,
    required: true,
  },
  weaponLevel: {
    type: Number,
    required: true,
  },
  wpmLike: {
    type: Number,
    required: true,
  },
})
</script>

<template>
  <button type="button" class="type-warrior-back type-warrior-floating-back" @click="$emit('back')">返回</button>

  <div class="type-warrior-stage-card">
    <p>type warrior</p>
    <h1>{{ stageLabel }}</h1>
    <span>{{ stageHint }}</span>
  </div>

  <div class="type-warrior-stage-meta type-warrior-floating-meta">
    <div class="type-warrior-top-actions">
      <button type="button" class="type-warrior-pause-button" @click="$emit('toggle-pause')">
        {{ isPaused ? '继续' : '暂停' }}
      </button>
      <span class="type-warrior-damage-chip">伤害 {{ currentProjectileDamage }}</span>
    </div>
    <strong>武器 等级{{ weaponLevel.toFixed(0) }}</strong>
    <span>连击 {{ combo }}</span>
    <span>节奏 {{ wpmLike }}</span>
    <span>清屏 {{ purgeCooldownLabel }}</span>
    <span>冰冻 {{ freezeStatusLabel }}</span>
    <i v-if="comboFeedbackTimer > 0" class="type-warrior-combo-pop">x{{ comboFeedbackCount }}</i>
  </div>

  <div v-if="comboFeedbackTimer > 0" class="type-warrior-combo-center">combo x{{ comboFeedbackCount }}</div>

  <aside class="type-warrior-side-info">
    <div class="type-warrior-tip-card is-mechanic-card">
      <h2>输入规则</h2>
      <p>直接输入敌人上方的英文单词。系统会按当前后缀自动锁定屏幕内最近的可匹配目标，并在完整拼对后自动开火。</p>
    </div>
    <div class="type-warrior-tip-card is-boss-card">
      <h2>操作提示</h2>
      <p>按 `Esc` 可以暂停或继续。获得“清屏指令”后按 `1` 激活，消耗 100 点能量。获得“冰冻”后按 `2` 立即释放，消耗 70 点能量。</p>
    </div>
  </aside>

  <footer v-if="cards.length > 0" class="type-warrior-cardbar">
    <article v-for="card in cards" :key="card.id" class="type-warrior-card">
      <span>{{ card.type }}</span>
      <strong>{{ card.name }}</strong>
      <p>{{ card.description }}</p>
      <em>{{ card.level >= getSkillMaxLevel(card.id) ? '已满级' : `等级 ${card.level}` }}</em>
    </article>
  </footer>
</template>
