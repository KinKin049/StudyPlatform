<script setup>
/**
 * 纯展示型战场组件。
 * 这里只负责渲染角色、敌人、子弹、碎片和战场提示，不处理业务逻辑。
 */
defineProps({
  arenaRef: {
    type: [Object, Function],
    required: true,
  },
  banner: {
    type: String,
    required: true,
  },
  bullets: {
    type: Array,
    required: true,
  },
  currentTarget: {
    type: Object,
    default: null,
  },
  enemies: {
    type: Array,
    required: true,
  },
  enemyFragments: {
    type: Array,
    required: true,
  },
  energy: {
    type: Number,
    required: true,
  },
  health: {
    type: Number,
    required: true,
  },
  keyBursts: {
    type: Array,
    required: true,
  },
  playerRingStyle: {
    type: Object,
    required: true,
  },
  playerShellClass: {
    type: Object,
    required: true,
  },
  survivalSeconds: {
    type: Number,
    required: true,
  },
  bulletStyle: {
    type: Function,
    required: true,
  },
  enemyHealthStyle: {
    type: Function,
    required: true,
  },
  enemyStyle: {
    type: Function,
    required: true,
  },
  enemyWordTransitionStyle: {
    type: Function,
    required: true,
  },
  fragmentStyle: {
    type: Function,
    required: true,
  },
  getEnemyWordParts: {
    type: Function,
    required: true,
  },
  keyBurstStyle: {
    type: Function,
    required: true,
  },
  showFieldRings: {
    type: Boolean,
    default: true,
  },
})
</script>

<template>
  <div :ref="arenaRef" class="type-warrior-arena" :class="{ 'is-field-rings-hidden': !showFieldRings }">
    <div class="type-warrior-field-ring type-warrior-field-ring-outer"></div>
    <div class="type-warrior-field-ring type-warrior-field-ring-inner"></div>

    <div class="type-warrior-player-shell" :class="playerShellClass">
      <div class="type-warrior-player-core" :style="playerRingStyle">
        <div class="type-warrior-player-values">
          <strong>{{ Math.round(health) }}</strong>
          <span>生命 / 能量 {{ Math.round(energy) }}</span>
        </div>
      </div>

      <span
        v-for="burst in keyBursts"
        :key="burst.id"
        class="type-warrior-key-burst"
        :class="{ 'is-failed': burst.failed }"
        :style="keyBurstStyle(burst)"
      >
        {{ burst.letter }}
      </span>

      <p class="type-warrior-banner">{{ banner }}</p>
    </div>

    <div
      v-for="enemy in enemies"
      :key="enemy.id"
      class="type-warrior-enemy"
      :class="[
        `is-${enemy.shape}`,
        {
          'is-target': currentTarget && currentTarget.id === enemy.id,
          'is-boss': enemy.boss,
          'is-emitting': enemy.emitFeedback > 0,
          'is-error': enemy.errorFeedback > 0,
          'is-hit': enemy.hitFeedback > 0,
        },
      ]"
      :style="enemyStyle(enemy)"
    >
      <div class="type-warrior-enemy-body"></div>
      <div class="type-warrior-enemy-tag">
        <div class="type-warrior-enemy-tag-copy" :style="enemyWordTransitionStyle(enemy)">
          <strong>
            <span v-if="getEnemyWordParts(enemy).matched" class="type-warrior-enemy-match">
              {{ getEnemyWordParts(enemy).matched }}
            </span>
            <span>{{ getEnemyWordParts(enemy).rest }}</span>
          </strong>
          <span>{{ enemy.text }}</span>
        </div>
      </div>
      <div class="type-warrior-enemy-health">
        <i :style="enemyHealthStyle(enemy)"></i>
      </div>
    </div>

    <div
      v-for="bullet in bullets"
      :key="bullet.id"
      class="type-warrior-bullet"
      :class="{ 'is-trail': bullet.trail > 0 }"
      :style="bulletStyle(bullet)"
    ></div>

    <div
      v-for="fragment in enemyFragments"
      :key="fragment.id"
      class="type-warrior-fragment"
      :style="fragmentStyle(fragment)"
    ></div>

    <div class="type-warrior-minimap-copy">
      <span>生存 {{ Math.floor(survivalSeconds) }}秒</span>
      <span>{{ enemies.length }} 个威胁</span>
    </div>
  </div>
</template>
