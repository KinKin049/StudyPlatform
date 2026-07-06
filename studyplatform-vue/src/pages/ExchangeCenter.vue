<script setup>
import { computed, onMounted, ref } from 'vue'
import { fetchProfileOverview } from '../api/profile'

const overview = ref(null)
const loading = ref(true)

const earnRules = [
  {
    title: '在线学习',
    value: '课程时长 + 题目得分',
    detail: '观看课程按学习时长获得金币，答对题目额外获得金币。',
    tone: 'cyan',
  },
  {
    title: '题库与 OJ',
    value: '答题正确 + 练习时长',
    detail: '题库、错题本、收藏题目和 OJ 平台都会记录学习贡献。',
    tone: 'blue',
  },
  {
    title: '可视化学习',
    value: '在线时长兑换',
    detail: '算法结构、函数图像、空间模型页面按在线学习时长累计。',
    tone: 'violet',
  },
  {
    title: '实验平台',
    value: '在线时长兑换',
    detail: '油气仿真、在线编程等实验学习按在线时长累计金币。',
    tone: 'amber',
  },
  {
    title: '游戏学习',
    value: '游戏内直接获得',
    detail: '万题天梯跳按游戏金币结算，Type Warrior 按得分折算金币。',
    tone: 'rose',
  },
]

const sections = [
  {
    title: 'AI 陪伴宠物形象',
    subtitle: '暂时只展示样式，后续可接入真实宠物解锁。',
    items: [
      { name: '学习猫', tag: '专注陪伴', price: 1200, status: '样式展示' },
      { name: '代码机器人', tag: 'OJ 练习', price: 1800, status: '暂未开放' },
      { name: '油气工程小助手', tag: '实验平台', price: 2200, status: '暂未开放' },
      { name: '单词精灵', tag: '英语记忆', price: 1600, status: '暂未开放' },
    ],
  },
  {
    title: '优惠券与学习权益',
    subtitle: '展示满减券与折扣券，后续可接入课程资料、教材购买、答疑服务等真实抵扣场景。',
    items: [
      { name: '满 30 元减 5 元优惠券', tag: '课程资料', price: 300, status: '样式展示' },
      { name: '满 80 元减 15 元优惠券', tag: '教材购买', price: 700, status: '样式展示' },
      { name: '满 150 元减 35 元优惠券', tag: '学习礼包', price: 1200, status: '样式展示' },
      { name: '课程资料 9 折券', tag: '折扣优惠', price: 500, status: '样式展示' },
      { name: '教材购买 8.5 折券', tag: '折扣优惠', price: 900, status: '样式展示' },
      { name: '答疑服务 8 折券', tag: '学习权益', price: 1500, status: '样式展示' },
    ],
  },
  {
    title: '游戏道具展示',
    subtitle: '只展示兑换入口，暂不改变游戏内数值。',
    items: [
      { name: 'Type Warrior 技能刷新券', tag: '打字生存', price: 260, status: '暂未开放' },
      { name: '天梯跳复活券', tag: '万题天梯跳', price: 360, status: '暂未开放' },
      { name: 'AI 宠物高级对话次数', tag: '陪伴互动', price: 420, status: '暂未开放' },
    ],
  },
]

const coinTotal = computed(() => Number(overview.value?.coinTotal ?? 0))

async function loadOverview() {
  loading.value = true
  try {
    overview.value = await fetchProfileOverview()
  } finally {
    loading.value = false
  }
}

onMounted(loadOverview)
</script>

<template>
  <main class="exchange-page">
    <section class="exchange-hero">
      <div>
        <p class="exchange-kicker">金币兑换中心</p>
        <h1>把学习、实验和游戏中的努力换成可见奖励</h1>
        <p class="exchange-summary">
          金币来自在线课程、题库练习、可视化学习、实验平台在线时长，以及两个学习游戏的结算数据。
        </p>
      </div>
      <aside class="exchange-balance">
        <span>当前金币</span>
        <strong>{{ loading ? '...' : coinTotal.toLocaleString('zh-CN') }}</strong>
        <p>暂未开启真实扣费兑换</p>
      </aside>
    </section>

    <section class="exchange-rule-grid" aria-label="金币获取方式">
      <article v-for="rule in earnRules" :key="rule.title" :class="['exchange-rule-card', `is-${rule.tone}`]">
        <span>{{ rule.title }}</span>
        <strong>{{ rule.value }}</strong>
        <p>{{ rule.detail }}</p>
      </article>
    </section>

    <section v-for="section in sections" :key="section.title" class="exchange-section">
      <header class="exchange-section-header">
        <div>
          <p>兑换展示</p>
          <h2>{{ section.title }}</h2>
        </div>
        <span>{{ section.subtitle }}</span>
      </header>
      <div class="exchange-shop-grid">
        <article v-for="item in section.items" :key="item.name" class="exchange-shop-card">
          <div class="exchange-shop-icon" aria-hidden="true">{{ item.name.slice(0, 1) }}</div>
          <div>
            <span>{{ item.tag }}</span>
            <h3>{{ item.name }}</h3>
          </div>
          <footer>
            <strong>{{ item.price.toLocaleString('zh-CN') }} 金币</strong>
            <button type="button" disabled>{{ item.status }}</button>
          </footer>
        </article>
      </div>
    </section>
  </main>
</template>

<style scoped>
.exchange-page {
  min-height: 100vh;
  padding: 112px clamp(20px, 5vw, 72px) 56px;
  background:
    radial-gradient(circle at 10% 12%, rgba(250, 204, 21, 0.2), transparent 28%),
    radial-gradient(circle at 86% 8%, rgba(45, 212, 191, 0.18), transparent 26%),
    linear-gradient(135deg, #eef8f6 0%, #f8fbff 46%, #fff6df 100%);
  color: #172033;
}

.exchange-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(240px, 320px);
  gap: 24px;
  align-items: stretch;
  max-width: 1180px;
  margin: 0 auto 26px;
}

.exchange-kicker,
.exchange-section-header p {
  margin: 0 0 10px;
  color: #0f766e;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.exchange-hero h1 {
  max-width: 760px;
  margin: 0;
  font-size: clamp(34px, 5vw, 64px);
  line-height: 1.02;
  letter-spacing: 0;
}

.exchange-summary {
  max-width: 720px;
  margin: 18px 0 0;
  color: #475569;
  font-size: 17px;
  line-height: 1.8;
}

.exchange-balance,
.exchange-rule-card,
.exchange-shop-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.74);
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.1);
  backdrop-filter: blur(18px);
}

.exchange-balance {
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-radius: 22px;
  padding: 30px;
}

.exchange-balance span,
.exchange-shop-card span {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.exchange-balance strong {
  margin-top: 10px;
  color: #b45309;
  font-size: 46px;
  line-height: 1;
}

.exchange-balance p {
  margin: 12px 0 0;
  color: #64748b;
}

.exchange-rule-grid,
.exchange-shop-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 16px;
  max-width: 1180px;
  margin: 0 auto;
}

.exchange-rule-card {
  min-height: 168px;
  border-radius: 18px;
  padding: 22px;
}

.exchange-rule-card span {
  color: #64748b;
  font-size: 13px;
  font-weight: 800;
}

.exchange-rule-card strong {
  display: block;
  margin-top: 12px;
  font-size: 22px;
}

.exchange-rule-card p {
  margin: 12px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.exchange-rule-card.is-cyan {
  border-color: rgba(20, 184, 166, 0.28);
}

.exchange-rule-card.is-blue {
  border-color: rgba(59, 130, 246, 0.26);
}

.exchange-rule-card.is-violet {
  border-color: rgba(139, 92, 246, 0.26);
}

.exchange-rule-card.is-amber {
  border-color: rgba(245, 158, 11, 0.28);
}

.exchange-rule-card.is-rose {
  border-color: rgba(244, 63, 94, 0.24);
}

.exchange-section {
  max-width: 1180px;
  margin: 34px auto 0;
}

.exchange-section-header {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: end;
  margin-bottom: 16px;
}

.exchange-section-header h2 {
  margin: 0;
  font-size: 28px;
}

.exchange-section-header > span {
  max-width: 420px;
  color: #64748b;
  line-height: 1.7;
  text-align: right;
}

.exchange-shop-card {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  gap: 16px;
  border-radius: 18px;
  padding: 18px;
}

.exchange-shop-icon {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  border-radius: 18px;
  background: linear-gradient(135deg, #14b8a6, #f59e0b);
  color: white;
  font-size: 24px;
  font-weight: 900;
}

.exchange-shop-card h3 {
  margin: 6px 0 0;
  font-size: 18px;
}

.exchange-shop-card footer {
  grid-column: 1 / -1;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-top: 8px;
}

.exchange-shop-card footer strong {
  color: #b45309;
}

.exchange-shop-card button {
  border: 0;
  border-radius: 999px;
  padding: 9px 14px;
  color: #475569;
  background: rgba(15, 23, 42, 0.08);
  font-weight: 800;
}

@media (max-width: 760px) {
  .exchange-page {
    padding-top: 88px;
  }

  .exchange-hero {
    grid-template-columns: 1fr;
  }

  .exchange-section-header {
    display: block;
  }

  .exchange-section-header > span {
    display: block;
    margin-top: 8px;
    text-align: left;
  }
}
</style>
