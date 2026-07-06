<script setup>
import { useLadderJumpGame } from './composables/useLadderJumpGame'

const emit = defineEmits(['back'])

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
} = useLadderJumpGame()
</script>

<template>
  <section class="ladder-game-page">
    <main class="ladder-stage" aria-label="万题天梯跳游戏区域" tabindex="0">
      <div class="ladder-world" :style="worldStyle">
        <div
          v-for="layer in sceneLayers"
          :key="layer.key"
          :class="layer.className"
        ></div>

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
          <template v-if="platform.questionId">
            <span>{{ optionLetter(platform.index) }}</span>
            <strong>{{ platform.option }}</strong>
          </template>
        </div>

        <div
          v-for="coin in coins"
          v-show="!coin.collected"
          :key="coin.id"
          class="ladder-coin"
          :style="{ left: `${coin.x}px`, top: `${coin.y}px` }"
        >
          金
        </div>

        <div
          v-for="coin in travelCoins"
          v-show="!coin.collected"
          :key="coin.id"
          class="ladder-coin is-travel-coin"
          :style="{ left: `${coin.x}px`, top: `${coin.y}px` }"
        >
          金
        </div>

        <div
          v-if="selectedPlatform && selectedPlatform.questionId === currentQuestionKey && !answeredQuestionIds.includes(currentQuestionKey)"
          class="ladder-confirm-line"
          :style="{ left: `${selectedPlatform.x + confirmOffset}px`, top: `${selectedPlatform.y - 106}px`, height: '152px' }"
        >
          <span>确认线</span>
        </div>

        <div class="ladder-ground"></div>

        <img
          v-for="car in cars"
          :key="car.id"
          class="ladder-car"
          :src="`${assetBase}/cars/${car.file}`"
          alt=""
          :style="{ left: `${car.x}px`, bottom: `${car.bottom}px`, transform: `scaleX(${car.direction})` }"
        />

        <div class="ladder-player" :class="{ 'is-damaged': damageFlash }" :style="playerStyle">
          <img :src="playerSprite" alt="玩家角色" />
        </div>
      </div>

      <button type="button" class="ladder-back-button ladder-floating-back" @click="emit('back')">Back</button>

      <section ref="questionBankPanelRef" class="ladder-bank-panel" aria-label="题库选择">
        <span class="ladder-bank-panel__label">题库</span>
        <button
          type="button"
          class="ladder-bank-panel__trigger"
          :disabled="questionBankLoading"
          @click="toggleQuestionDropdown"
        >
          <span class="ladder-bank-panel__trigger-title">{{ questionBankButtonTitle }}</span>
          <span class="ladder-bank-panel__trigger-meta">{{ questionBankButtonSubtitle }}</span>
        </button>

        <div v-if="questionDropdownOpen" class="ladder-bank-panel__menu">
          <button
            type="button"
            class="ladder-bank-panel__option"
            :class="{ 'is-active': !selectedQuestionBankCode }"
            @click="selectQuestionBank('')"
          >
            <span>全部单选题库</span>
            <small>随机混合题池</small>
          </button>
          <button
            v-for="bank in questionBanks"
            :key="bank.code"
            type="button"
            class="ladder-bank-panel__option"
            :class="{ 'is-active': bank.code === selectedQuestionBankCode }"
            @click="selectQuestionBank(bank.code)"
          >
            <span>{{ bank.title }}</span>
            <small>{{ bank.categoryName }} · {{ bank.questionCount }} 题</small>
          </button>
        </div>

        <p class="ladder-bank-panel__summary">{{ questionBankSummary }}</p>
      </section>

      <div class="ladder-stats ladder-floating-stats">
        <span>金币 {{ score }}</span>
        <span>Combo {{ combo }}</span>
        <span>Time {{ gameTimeText }}</span>
        <span class="ladder-hearts">{{ heartText }}</span>
        <button type="button" class="ladder-pause-button" @click="pauseGame">暂停</button>
      </div>

      <aside class="ladder-control-hint">
        <span>A/D 或方向键左右移动</span>
        <span>W / 空格 / 上方向键三级跳</span>
        <span>S / 下方向键下落</span>
        <span>Esc 暂停</span>
      </aside>

      <div v-if="isPaused || isGameOver" class="ladder-game-over">
        <p>{{ overlayTitle }}</p>
        <h2>{{ score }} 金币</h2>
        <span class="ladder-overlay-subtitle">{{ overlaySubtitle }}</span>

        <section class="ladder-overlay-stats" aria-label="本局统计">
          <article v-for="item in overlayStats" :key="item.label">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </section>

        <div class="ladder-overlay-actions">
          <button v-if="isPaused" type="button" @click="resumeGame">继续游戏</button>
          <button type="button" @click="restartGame">重新开始</button>
          <button v-if="isPaused" type="button" class="is-danger" @click="finishGame('本局结束')">立即结束</button>
        </div>
      </div>
    </main>
  </section>
</template>
