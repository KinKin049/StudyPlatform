<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import LadderJumpGame from './LadderJumpGame.vue'

const route = useRoute()
const router = useRouter()

const games = [
  {
    id: 'type-warrior',
    name: 'type warrior',
    nameEn: 'Typing Combat',
    status: '游戏尚未实现',
    background: '#1f7a8c',
    foreground: '#f7fbfc',
    description: '预留打字闯关、键盘反应和英文输入训练类小游戏入口。',
  },
  {
    id: 'ladder-jump',
    name: '万题天梯跳',
    nameEn: 'Question Ladder Jump',
    status: '游戏尚未实现',
    background: '#d96c4f',
    foreground: '#fff8f4',
    description: '预留题库闯关、跳跃答题和积分晋级类小游戏入口。',
  },
]

const hoveredGame = ref(null)
const transitionPhase = ref('idle')
const transitionGame = ref(games[0])
const transitionKind = ref('team')
let coverTimer = 0
let revealTimer = 0

const currentGame = computed(() => games.find((game) => game.id === route.params.gameId) || null)
const isDetailPage = computed(() => Boolean(currentGame.value))
const isTransitioning = computed(() => transitionPhase.value !== 'idle')
const transitionWords = computed(() => `正在进入 ${transitionGame.value.name}`.split(' '))
const archiveCoverDuration = 1680
const archiveRevealDuration = 980
const splitStyle = computed(() => {
  if (hoveredGame.value === 'type-warrior') {
    return { '--split-top': '68%', '--split-bottom': '58%' }
  }
  if (hoveredGame.value === 'ladder-jump') {
    return { '--split-top': '44%', '--split-bottom': '32%' }
  }
  return { '--split-top': '56%', '--split-bottom': '44%' }
})

function openGame(game) {
  if (isTransitioning.value) return

  window.clearTimeout(coverTimer)
  window.clearTimeout(revealTimer)
  transitionGame.value = game
  transitionKind.value = game.id === 'type-warrior' ? 'archive' : 'team'
  transitionPhase.value = 'cover'

  const coverDuration = transitionKind.value === 'archive' ? archiveCoverDuration : 940
  const revealDuration = transitionKind.value === 'archive' ? archiveRevealDuration : 820

  coverTimer = window.setTimeout(() => {
    router.push(`/games/${game.id}`)
    transitionPhase.value = 'reveal'
  }, coverDuration)

  revealTimer = window.setTimeout(() => {
    transitionPhase.value = 'idle'
  }, coverDuration + revealDuration)
}

function returnToSelector() {
  hoveredGame.value = null
  router.push('/games')
}

onBeforeUnmount(() => {
  window.clearTimeout(coverTimer)
  window.clearTimeout(revealTimer)
})
</script>

<template>
  <main class="game-page" aria-label="游戏模块">
    <section v-if="!isDetailPage" class="game-split-stage" :style="splitStyle">
      <button
        class="game-split-panel game-split-left"
        type="button"
        :disabled="isTransitioning"
        :style="{ backgroundColor: games[0].background, color: games[0].foreground }"
        @mouseenter="hoveredGame = games[0].id"
        @mouseleave="hoveredGame = null"
        @focus="hoveredGame = games[0].id"
        @blur="hoveredGame = null"
        @click="openGame(games[0])"
      >
        <span class="game-panel-number">01</span>
        <span class="game-panel-content">
          <strong>{{ games[0].name }}</strong>
          <em>{{ games[0].nameEn }}</em>
          <small>{{ games[0].description }}</small>
          <span class="game-panel-action">进入游戏</span>
        </span>
      </button>

      <button
        class="game-split-panel game-split-right"
        type="button"
        :disabled="isTransitioning"
        :style="{ backgroundColor: games[1].background, color: games[1].foreground }"
        @mouseenter="hoveredGame = games[1].id"
        @mouseleave="hoveredGame = null"
        @focus="hoveredGame = games[1].id"
        @blur="hoveredGame = null"
        @click="openGame(games[1])"
      >
        <span class="game-panel-number">02</span>
        <span class="game-panel-content">
          <strong>{{ games[1].name }}</strong>
          <em>{{ games[1].nameEn }}</em>
          <small>{{ games[1].description }}</small>
          <span class="game-panel-action">进入游戏</span>
        </span>
      </button>

      <div class="game-split-line" aria-hidden="true"></div>
    </section>

    <section v-else-if="currentGame.id === 'ladder-jump'" class="game-ladder-detail-stage">
      <LadderJumpGame @back="returnToSelector" />
    </section>

    <section
      v-else
      class="game-detail-stage"
      :style="{
        backgroundColor: currentGame.background,
        color: currentGame.foreground,
      }"
    >
      <header class="game-detail-header">
        <div>
          <p class="game-kicker">Game Placeholder</p>
          <h1>{{ currentGame.name }}</h1>
        </div>
        <button type="button" @click="returnToSelector">返回游戏模块</button>
      </header>

      <section class="game-detail-content">
        <p>{{ currentGame.nameEn }}</p>
        <h2>{{ currentGame.status }}</h2>
        <span>{{ currentGame.description }}</span>
      </section>
    </section>

    <div
      class="team-transition-overlay"
      :class="{
        'is-covering': transitionKind === 'team' && transitionPhase === 'cover',
        'is-revealing': transitionKind === 'team' && transitionPhase === 'reveal',
      }"
      aria-hidden="true"
    >
      <h2>
        <span
          v-for="(word, index) in transitionWords"
          :key="`${word}-${index}`"
          class="team-transition-word"
          :style="{ transitionDelay: `${index * 70}ms` }"
        >
          {{ word }}
        </span>
      </h2>
    </div>

    <div
      class="archive-transition-overlay"
      :class="{
        'is-covering': transitionKind === 'archive' && transitionPhase === 'cover',
        'is-revealing': transitionKind === 'archive' && transitionPhase === 'reveal',
      }"
      aria-hidden="true"
    >
      <div class="archive-transition-canvas">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          viewBox="-360 -360 760 760"
          preserveAspectRatio="xMidYMid slice"
        >
          <path
            pathLength="100"
            d="m0 0c3.36 0.06 6.6-2.82 7.07-7.07 0.59-4.19-1.79-9.6-7.07-12.93-5.18-3.4-13.27-4.43-21.21-1.21-7.92 3.09-15.48 10.7-18.79 21.21-3.42 10.46-2.3 23.68 4.64 35.36 6.83 11.64 19.57 21.35 35.36 24.64 15.69 3.43 34.14 0.2 49.5-10.5 15.38-10.52 27.21-28.47 30.5-49.5 3.43-20.92-1.92-44.6-16.36-63.64-14.2-19.1-37.37-33.1-63.64-36.36-26.16-3.45-55.05 4.06-77.78 22.22-22.82 17.9-38.98 46.25-42.22 77.78-3.48 31.43 6.2 65.49 28.08 91.92 21.58 26.55 55.16 44.87 91.92 48.08 36.66 3.5 75.94-8.33 106.07-33.93 30.27-25.28 50.74-64.07 53.93-106.07 3.53-41.9-10.46-86.4-39.79-120.21-28.97-33.99-72.97-56.62-120.21-59.79-47.13-3.56-96.85 12.6-134.35 45.65-37.71 32.65-62.51 81.88-65.65 134.35-3.55 52.4 14.72 107.29 51.51 148.49 36.35 41.41 90.75 68.41 148.49 71.51 57.63 3.58 117.75-16.86 162.63-57.37 45.14-40.03 74.29-99.66 77.37-162.63 3.62-62.86-19.02-128.21-63.22-176.78-43.73-48.85-108.57-80.17-176.78-83.22-68.13-3.64-138.65 21.15-190.92 69.08-52.57 47.43-86.06 117.45-89.08 190.92-3.66 73.36 23.29 149.1 74.94 205.06 51.12 56.3 126.35 91.94 205.06 94.94 78.6 3.69 159.56-25.42 219.2-80.8 60.03-54.8 97.82-135.26 100.8-219.2 3.72-83.84-27.56-170.01-86.65-233.35-58.51-63.77-144.14-103.69-233.35-106.65-89.07-3.71-180.46 29.68-247.49 92.51-67.51 62.18-109.54 153.08-112.51 247.49-3.74 94.33 31.82 190.91 98.37 261.63 65.88 71.23 161.96 115.43 261.63 118.37 99.57 3.76 201.36-33.95 275.77-104.23 74.96-69.57 121.31-170.86 124.23-275.77"
            fill="none"
            stroke="currentColor"
            stroke-linecap="round"
            stroke-linejoin="round"
            vector-effect="non-scaling-stroke"
          />
        </svg>
      </div>
    </div>
  </main>
</template>
