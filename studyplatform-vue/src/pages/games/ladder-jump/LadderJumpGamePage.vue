<script setup>
/**
 * 万题天梯跳游戏主页面
 * 整合游戏世界、HUD界面、题库选择面板和游戏覆盖层，管理游戏核心逻辑
 */
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LadderGameOverlay from './components/LadderGameOverlay.vue'
import LadderHud from './components/LadderHud.vue'
import LadderQuestionBankPanel from './components/LadderQuestionBankPanel.vue'
import LadderJumpWorld from './components/LadderJumpWorld.vue'
import { useLadderJumpGame } from './composables/useLadderJumpGame'
import { useUserVouchers } from '../../../composables/useUserVouchers'
import { VOUCHER_KEYS } from '../../../api/vouchers'
import './styles/ladderJump.css'

/** 定义返回事件 */
const emit = defineEmits(['back'])
const route = useRoute()
const router = useRouter()

const routeQuestionBankCode = () => {
  const queryCode = route.query.setCode
  return typeof queryCode === 'string' ? queryCode.trim() : ''
}

function syncQuestionBankRoute(code) {
  const normalizedCode = String(code || '').trim()
  const nextQuery = { ...route.query }
  if (normalizedCode) {
    nextQuery.setCode = normalizedCode
  } else {
    delete nextQuery.setCode
  }
  router.replace({ query: nextQuery })
}

/** 用户券相关操作 */
const {
  errorMessage: voucherErrorMessage,
  getQuantity,
  consumeVoucher,
} = useUserVouchers()

/** 游戏核心逻辑组合式函数 */
const {
  assetBase,
  worldStyle,
  sceneLayers,
  questionCards,
  feedback,
  activePlatforms,
  selectedPlatform,
  optionLetter,
  coins,
  travelCoins,
  currentQuestionKey,
  answeredQuestionIds,
  confirmOffset,
  cars,
  damageFlash,
  playerStyle,
  playerSprite,
  questionBankPanelRef,
  questionBankLoading,
  toggleQuestionDropdown,
  questionBankButtonTitle,
  questionBankButtonSubtitle,
  questionDropdownOpen,
  selectedQuestionBankCode,
  selectQuestionBank,
  questionBanks,
  questionBankSummary,
  score,
  combo,
  gameTimeText,
  heartText,
  pauseGame,
  isPaused,
  isGameOver,
  overlayTitle,
  overlaySubtitle,
  overlayStats,
  resumeGame,
  restartGame,
  finishGame,
  reviveGame,
  persistLadderJumpRecord,
} = useLadderJumpGame({
  initialQuestionBankCode: routeQuestionBankCode(),
  onQuestionBankChange: syncQuestionBankRoute,
})

/**
 * 设置题库面板引用
 * @param {HTMLElement|VueComponent} element - 题库面板元素或组件
 */
function setQuestionBankPanelRef(element) {
  questionBankPanelRef.value = element?.$el || element
}

/**
 * 处理复活游戏
 * 消耗复活券后调用游戏复活逻辑
 */
async function handleReviveGame() {
  if (getQuantity(VOUCHER_KEYS.GAME_REVIVE) <= 0) {
    return
  }
  const consumed = await consumeVoucher(VOUCHER_KEYS.GAME_REVIVE)
  if (consumed) {
    reviveGame()
  }
}

/**
 * 处理重新开始游戏
 * 游戏结束时先保存记录再重新开始
 */
function handleRestartGame() {
  if (isGameOver.value) {
    persistLadderJumpRecord()
  }
  restartGame()
}

/**
 * 处理结束游戏
 * 结束当前局并保存游戏记录
 */
function handleFinishGame() {
  finishGame('本局结束')
  persistLadderJumpRecord()
}

/** 监听路由题库参数，支持从题库入口直接进入游戏 */
watch(
  () => route.query.setCode,
  (nextCode) => {
    const normalizedCode = typeof nextCode === 'string' ? nextCode.trim() : ''
    if (normalizedCode !== selectedQuestionBankCode.value) {
      selectQuestionBank(normalizedCode)
    }
  },
)

/** 监听游戏结束状态，无复活券时自动保存记录 */
watch(
  () => isGameOver.value,
  (gameOver) => {
    if (!gameOver || getQuantity(VOUCHER_KEYS.GAME_REVIVE) > 0) {
      return
    }
    persistLadderJumpRecord()
  },
)
</script>

<template>
  <!-- 游戏页面主容器 -->
  <section class="ladder-game-page">
    <!-- 游戏舞台区域 -->
    <main class="ladder-stage" aria-label="万题天梯跳游戏区域" tabindex="0">
      <!-- 游戏世界渲染组件 -->
      <LadderJumpWorld
        :asset-base="assetBase"
        :world-style="worldStyle"
        :scene-layers="sceneLayers"
        :question-cards="questionCards"
        :feedback="feedback"
        :active-platforms="activePlatforms"
        :selected-platform="selectedPlatform"
        :option-letter="optionLetter"
        :coins="coins"
        :travel-coins="travelCoins"
        :current-question-key="currentQuestionKey"
        :answered-question-ids="answeredQuestionIds"
        :confirm-offset="confirmOffset"
        :cars="cars"
        :damage-flash="damageFlash"
        :player-style="playerStyle"
        :player-sprite="playerSprite"
      />

      <!-- 返回按钮 -->
      <button type="button" class="ladder-back-button ladder-floating-back" @click="emit('back')">Back</button>

      <!-- 题库选择面板 -->
      <LadderQuestionBankPanel
        :ref="setQuestionBankPanelRef"
        :question-bank-loading="questionBankLoading"
        :question-bank-button-title="questionBankButtonTitle"
        :question-bank-button-subtitle="questionBankButtonSubtitle"
        :question-dropdown-open="questionDropdownOpen"
        :selected-question-bank-code="selectedQuestionBankCode"
        :question-banks="questionBanks"
        :question-bank-summary="questionBankSummary"
        @toggle="toggleQuestionDropdown"
        @select="selectQuestionBank"
      />

      <!-- 游戏状态HUD -->
      <LadderHud
        :score="score"
        :combo="combo"
        :game-time-text="gameTimeText"
        :heart-text="heartText"
        @pause="pauseGame"
      />

      <!-- 游戏覆盖层（暂停/结束界面） -->
      <LadderGameOverlay
        :is-paused="isPaused"
        :is-game-over="isGameOver"
        :overlay-title="overlayTitle"
        :score="score"
        :overlay-subtitle="overlaySubtitle"
        :overlay-stats="overlayStats"
        :revive-available="getQuantity(VOUCHER_KEYS.GAME_REVIVE) > 0"
        :revive-count="getQuantity(VOUCHER_KEYS.GAME_REVIVE)"
        @resume="resumeGame"
        @revive="handleReviveGame"
        @restart="handleRestartGame"
        @finish="handleFinishGame"
      />

      <!-- 券操作错误提示 -->
      <p v-if="voucherErrorMessage" class="ladder-voucher-error">{{ voucherErrorMessage }}</p>
    </main>
  </section>
</template>
