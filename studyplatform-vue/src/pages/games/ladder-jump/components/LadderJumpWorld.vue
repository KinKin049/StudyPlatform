<script setup>
/**
 * 万题天梯跳游戏世界组件
 * 负责渲染游戏场景，包括平台、题目卡片、金币、车辆和玩家角色
 */
defineProps({
  /** 资源基础路径 */
  assetBase: {
    type: String,
    required: true,
  },
  /** 游戏世界样式 */
  worldStyle: {
    type: Object,
    required: true,
  },
  /** 场景图层列表 */
  sceneLayers: {
    type: Array,
    required: true,
  },
  /** 题目卡片列表 */
  questionCards: {
    type: Array,
    required: true,
  },
  /** 反馈信息文本 */
  feedback: {
    type: String,
    required: true,
  },
  /** 活动平台列表 */
  activePlatforms: {
    type: Array,
    required: true,
  },
  /** 当前选中的平台 */
  selectedPlatform: {
    type: Object,
    default: null,
  },
  /** 选项字母生成函数 */
  optionLetter: {
    type: Function,
    required: true,
  },
  /** 普通金币列表 */
  coins: {
    type: Array,
    required: true,
  },
  /** 旅行金币列表 */
  travelCoins: {
    type: Array,
    required: true,
  },
  /** 当前问题的唯一标识 */
  currentQuestionKey: {
    type: String,
    required: true,
  },
  /** 已回答的问题ID列表 */
  answeredQuestionIds: {
    type: Array,
    required: true,
  },
  /** 确认线偏移量 */
  confirmOffset: {
    type: Number,
    required: true,
  },
  /** 车辆列表 */
  cars: {
    type: Array,
    required: true,
  },
  /** 是否显示受伤闪烁效果 */
  damageFlash: {
    type: Boolean,
    required: true,
  },
  /** 玩家样式 */
  playerStyle: {
    type: Object,
    required: true,
  },
  /** 玩家精灵图路径 */
  playerSprite: {
    type: String,
    required: true,
  },
})
</script>

<template>
  <!-- 游戏世界容器 -->
  <div class="ladder-world" :style="worldStyle">
    <!-- 场景图层渲染 -->
    <div
      v-for="layer in sceneLayers"
      :key="layer.key"
      :class="layer.className"
    ></div>

    <!-- 题目卡片 -->
    <section
      v-for="card in questionCards"
      :key="card.id"
      class="ladder-question-card"
      :class="{ 'is-next-question': !card.isCurrent }"
      :style="{ left: `${card.x}px` }"
    >
      <span>第 {{ card.index + 1 }} 题</span>
      <h2>{{ card.question.question }}</h2>
      <p>{{ card.isCurrent ? feedback : '继续向右前进，下一题会随着你的移动进入画面。' }}</p>
    </section>

    <!-- 平台渲染（排除地面） -->
    <div
      v-for="platform in activePlatforms.filter((item) => item.id !== 'ground')"
      :key="platform.id"
      class="ladder-platform"
      :class="{
        'is-option': platform.questionId,
        'is-selected-option': selectedPlatform && selectedPlatform.id === platform.id,
        'is-correct-option': platform.questionId && platform.isCorrect,
      }"
      :style="{ left: `${platform.x}px`, top: `${platform.y}px`, width: `${platform.width}px`, height: `${platform.height}px` }"
    >
      <!-- 选项平台显示字母和选项内容 -->
      <template v-if="platform.questionId">
        <span>{{ optionLetter(platform.index) }}</span>
        <strong>{{ platform.option }}</strong>
      </template>
    </div>

    <!-- 普通金币 -->
    <div
      v-for="coin in coins"
      v-show="!coin.collected"
      :key="coin.id"
      class="ladder-coin"
      :style="{ left: `${coin.x}px`, top: `${coin.y}px` }"
    >
      ￥
    </div>

    <!-- 旅行金币 -->
    <div
      v-for="coin in travelCoins"
      v-show="!coin.collected"
      :key="coin.id"
      class="ladder-coin is-travel-coin"
      :style="{ left: `${coin.x}px`, top: `${coin.y}px` }"
    >
      ￥
    </div>

    <!-- 确认线（未回答当前问题时显示） -->
    <div
      v-if="selectedPlatform && selectedPlatform.questionId === currentQuestionKey && !answeredQuestionIds.includes(currentQuestionKey)"
      class="ladder-confirm-line"
      :style="{ left: `${selectedPlatform.x + confirmOffset}px`, top: `${selectedPlatform.y - 106}px`, height: '152px' }"
    >
      <span>确认线</span>
    </div>

    <!-- 地面 -->
    <div class="ladder-ground"></div>

    <!-- 车辆渲染 -->
    <img
      v-for="car in cars"
      :key="car.id"
      class="ladder-car"
      :src="`${assetBase}/cars/${car.file}`"
      alt=""
      :style="{ left: `${car.x}px`, bottom: `${car.bottom}px`, transform: `scaleX(${car.direction})` }"
    />

    <!-- 玩家角色 -->
    <div class="ladder-player" :class="{ 'is-damaged': damageFlash }" :style="playerStyle">
      <img :src="playerSprite" alt="玩家角色" />
    </div>
  </div>
</template>
