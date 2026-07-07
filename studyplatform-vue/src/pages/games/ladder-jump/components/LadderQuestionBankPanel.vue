<script setup>
/**
 * 题库选择面板组件
 * 允许玩家选择特定的题库进行游戏，支持下拉菜单切换
 */
defineProps({
  /** 题库是否正在加载 */
  questionBankLoading: {
    type: Boolean,
    required: true,
  },
  /** 当前选中题库的标题 */
  questionBankButtonTitle: {
    type: String,
    required: true,
  },
  /** 当前选中题库的副标题 */
  questionBankButtonSubtitle: {
    type: String,
    required: true,
  },
  /** 下拉菜单是否展开 */
  questionDropdownOpen: {
    type: Boolean,
    required: true,
  },
  /** 当前选中的题库代码 */
  selectedQuestionBankCode: {
    type: String,
    required: true,
  },
  /** 可用题库列表 */
  questionBanks: {
    type: Array,
    required: true,
  },
  /** 题库摘要信息 */
  questionBankSummary: {
    type: String,
    required: true,
  },
})

/** 定义面板交互事件 */
const emit = defineEmits(['toggle', 'select'])
</script>

<template>
  <!-- 题库选择面板 -->
  <section class="ladder-bank-panel" aria-label="题库选择">
    <span class="ladder-bank-panel__label">题库</span>
    <!-- 题库切换按钮 -->
    <button
      type="button"
      class="ladder-bank-panel__trigger"
      :disabled="questionBankLoading"
      @click="emit('toggle')"
    >
      <span class="ladder-bank-panel__trigger-title">{{ questionBankButtonTitle }}</span>
      <span class="ladder-bank-panel__trigger-meta">{{ questionBankButtonSubtitle }}</span>
    </button>

    <!-- 题库下拉菜单 -->
    <div v-if="questionDropdownOpen" class="ladder-bank-panel__menu">
      <!-- 全部题库选项 -->
      <button
        type="button"
        class="ladder-bank-panel__option"
        :class="{ 'is-active': !selectedQuestionBankCode }"
        @click="emit('select', '')"
      >
        <span>全部单选题库</span>
        <small>随机混合题池</small>
      </button>
      <!-- 各个题库选项 -->
      <button
        v-for="bank in questionBanks"
        :key="bank.code"
        type="button"
        class="ladder-bank-panel__option"
        :class="{ 'is-active': bank.code === selectedQuestionBankCode }"
        @click="emit('select', bank.code)"
      >
        <span>{{ bank.title }}</span>
        <small>{{ bank.categoryName }} · {{ bank.questionCount }} 题</small>
      </button>
    </div>

    <!-- 题库摘要信息 -->
    <p class="ladder-bank-panel__summary">{{ questionBankSummary }}</p>
  </section>
</template>
