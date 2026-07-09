<script setup>
/**
 * 金币兑换中心页面组件
 * 提供学习金币兑换卡券、宠物形象和游戏道具的功能
 */
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchProfileOverview } from '../api/profile'
import { exchangeVoucher, fetchUserVouchers, fetchVoucherItems } from '../api/vouchers'
import { AI_PET_SHOP_ITEMS, PET_SELECTION_EVENT, PET_STORAGE_KEYS } from '../data/aiPetShop'

// 用户概览信息
const overview = ref(null)
// 用户已拥有的卡券列表
const vouchers = ref([])
// 可兑换的卡券商品列表
const voucherItems = ref([])
// 加载状态
const loading = ref(true)
// 当前正在兑换的卡券标识
const exchangingKey = ref('')
// 兑换成功提示信息
const exchangeMessage = ref('')
// 兑换失败错误信息
const exchangeError = ref('')
const ownedPetKeys = ref([])
const activePetKey = ref('')
const localPetSpent = ref(0)

// 金币获取规则列表
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

/**
 * 计算展示的兑换区域数据
 * @returns {Array} 展示区域数组
 */
const petShopItems = computed(() =>
  AI_PET_SHOP_ITEMS.map((pet) => ({
    ...pet,
    kind: 'pet',
    owned: ownedPetKeys.value.includes(pet.key),
    active: activePetKey.value === pet.key,
    status: activePetKey.value === pet.key ? '使用中' : ownedPetKeys.value.includes(pet.key) ? '切换' : '兑换',
  })),
)

/**
 * 计算展示的兑换区域数据
 * @returns {Array} 展示区域数组
 */
const displaySections = computed(() => [
  {
    title: 'AI 陪伴宠物形象',
    subtitle: '兑换后可立即切换全局 AI 陪伴宠物形象，选择会保存在本地。',
    items: petShopItems.value,
  },
  {
    title: '优惠券与学习权益',
    subtitle: '展示当前管理员上架的优惠券，按库存和有效期开放兑换。',
    items: voucherItems.value.filter((item) => item.voucherType === 'DISCOUNT').map(toShopItem),
  },
  {
    title: '游戏道具展示',
    subtitle: '游戏券兑换后会进入我的卡券，并可在游戏中使用。',
    items: voucherItems.value.filter((item) => item.voucherType === 'GAME_ITEM').map(toShopItem),
  },
])

/**
 * 获取用户金币总数
 * @returns {number} 金币总数
 */
const coinTotal = computed(() => Number(overview.value?.coinTotal ?? 0))
const availableCoinTotal = computed(() => Math.max(0, coinTotal.value - localPetSpent.value))

/**
 * 计算用户已拥有的卡券数量映射
 * @returns {Map} 卡券标识到数量的映射
 */
const voucherQuantityMap = computed(() => {
  const map = new Map()
  vouchers.value.forEach((item) => {
    map.set(item.voucherKey, Number(item.quantity ?? 0))
  })
  return map
})

/**
 * 加载用户概览和卡券数据
 */
async function loadOverview() {
  loading.value = true
  try {
    const [nextOverview, nextVouchers, nextVoucherItems] = await Promise.all([
      fetchProfileOverview(),
      fetchUserVouchers().catch(() => []),
      fetchVoucherItems().catch(() => []),
    ])
    overview.value = nextOverview
    vouchers.value = nextVouchers
    voucherItems.value = nextVoucherItems
  } finally {
    loading.value = false
  }
}

function loadPetState() {
  try {
    const savedOwned = JSON.parse(window.localStorage.getItem(PET_STORAGE_KEYS.owned) || '[]')
    ownedPetKeys.value = Array.isArray(savedOwned) ? savedOwned.filter((key) => typeof key === 'string') : []
  } catch {
    ownedPetKeys.value = []
  }
  activePetKey.value = window.localStorage.getItem(PET_STORAGE_KEYS.active) || ''
  localPetSpent.value = Number(window.localStorage.getItem(PET_STORAGE_KEYS.spent) || 0) || 0
}

function savePetState() {
  window.localStorage.setItem(PET_STORAGE_KEYS.owned, JSON.stringify(ownedPetKeys.value))
  window.localStorage.setItem(PET_STORAGE_KEYS.active, activePetKey.value)
  window.localStorage.setItem(PET_STORAGE_KEYS.spent, String(localPetSpent.value))
}

function notifyPetSelectionChanged(pet) {
  window.dispatchEvent(new CustomEvent(PET_SELECTION_EVENT, {
    detail: {
      key: pet.key,
      name: pet.name,
    },
  }))
}

function handlePetAction(item) {
  if (item.active) {
    exchangeMessage.value = `${item.name} 已经是当前陪伴宠物`
    exchangeError.value = ''
    return
  }

  if (!item.owned && availableCoinTotal.value < item.price) {
    exchangeMessage.value = ''
    exchangeError.value = `金币不足，还差 ${Math.max(0, item.price - availableCoinTotal.value).toLocaleString('zh-CN')} 金币`
    return
  }

  if (!item.owned) {
    ownedPetKeys.value = [...new Set([...ownedPetKeys.value, item.key])]
    localPetSpent.value += Number(item.price) || 0
    exchangeMessage.value = `已兑换 ${item.name}，并自动切换为当前宠物`
  } else {
    exchangeMessage.value = `已切换为 ${item.name}`
  }

  activePetKey.value = item.key
  exchangeError.value = ''
  savePetState()
  notifyPetSelectionChanged(item)
}

/**
 * 获取用户已拥有的指定卡券数量
 * @param {Object} item 卡券商品对象
 * @returns {number} 拥有数量
 */
function getOwnedQuantity(item) {
  return item.voucherKey ? voucherQuantityMap.value.get(item.voucherKey) || 0 : 0
}

function getActionDisabled(item) {
  if (item.kind === 'pet') {
    return loading.value || item.active || (!item.owned && availableCoinTotal.value < item.price)
  }
  return loading.value || exchangingKey.value === item.voucherKey || coinTotal.value < item.price || isVoucherSoldOut(item)
}

function getActionText(item) {
  if (item.kind === 'pet') {
    if (item.active) return '使用中'
    if (item.owned) return '切换'
    return availableCoinTotal.value < item.price ? '金币不足' : '兑换'
  }
  if (exchangingKey.value === item.voucherKey) return '兑换中...'
  if (isVoucherSoldOut(item)) return '已兑完'
  return coinTotal.value < item.price ? '金币不足' : item.status
}

function handleShopAction(item) {
  if (item.kind === 'pet') {
    handlePetAction(item)
    return
  }
  handleExchange(item)
}

/**
 * 执行卡券兑换操作
 * @param {Object} item 卡券商品对象
 */
async function handleExchange(item) {
  if (!item.voucherKey || exchangingKey.value) {
    return
  }
  exchangingKey.value = item.voucherKey
  exchangeMessage.value = ''
  exchangeError.value = ''
  try {
    vouchers.value = await exchangeVoucher(item.voucherKey)
    const [nextOverview, nextVoucherItems] = await Promise.all([
      fetchProfileOverview(),
      fetchVoucherItems().catch(() => voucherItems.value),
    ])
    overview.value = nextOverview
    voucherItems.value = nextVoucherItems
    exchangeMessage.value = `已兑换 ${item.name}`
  } catch (error) {
    exchangeError.value = error?.message || '兑换失败'
  } finally {
    exchangingKey.value = ''
  }
}

/**
 * 将卡券商品转换为商店展示格式
 * @param {Object} item 卡券商品对象
 * @returns {Object} 商店展示格式对象
 */
function toShopItem(item) {
  return {
    ...item,
    tag: item.voucherType === 'GAME_ITEM' ? '游戏券' : '优惠券',
    status: '兑换',
  }
}

/**
 * 格式化卡券库存信息
 * @param {Object} item 卡券商品对象
 * @returns {string} 库存显示文本
 */
function formatVoucherStock(item) {
  return item.unlimitedStock ? '库存：不限量' : `库存：${Number(item.stockQuantity ?? 0)} 张`
}

/**
 * 判断卡券是否已售罄
 * @param {Object} item 卡券商品对象
 * @returns {boolean} 是否已售罄
 */
function isVoucherSoldOut(item) {
  return item.voucherKey && !item.unlimitedStock && Number(item.stockQuantity ?? 0) <= 0
}

/**
 * 格式化卡券使用规则
 * @param {Object} item 卡券商品对象
 * @returns {string} 使用规则显示文本
 */
function formatVoucherRule(item) {
  if (item.voucherType === 'GAME_ITEM') {
    return '游戏内使用'
  }
  const threshold = Number(item.thresholdAmount ?? 0)
  const maxDiscount = Number(item.maxDiscountAmount ?? 0)
  const thresholdText = threshold > 0 ? `满 ${threshold.toFixed(0)} 元可用` : '无门槛'
  let discountText = '优惠券'
  if (item.discountType === 'AMOUNT' && Number(item.discountAmount ?? 0) > 0) {
    discountText = `减 ${Number(item.discountAmount).toFixed(0)} 元`
  } else if (item.discountType === 'PERCENT' && Number(item.discountRate ?? 0) > 0) {
    discountText = `${(Number(item.discountRate) * 10).toFixed(1).replace(/\.0$/, '')} 折`
  }
  const maxText = maxDiscount > 0 ? `，最高减 ${maxDiscount.toFixed(0)} 元` : ''
  return `${thresholdText}，${discountText}${maxText}`
}

/**
 * 格式化卡券有效期信息
 * @param {Object} item 卡券商品对象
 * @returns {string} 有效期显示文本
 */
function formatVoucherValidity(item) {
  if (!item.validFrom && !item.validUntil) {
    return '长期有效'
  }
  const start = item.validFrom ? formatDate(item.validFrom) : '现在'
  const end = item.validUntil ? formatDate(item.validUntil) : '长期'
  return `${start} 至 ${end}`
}

/**
 * 格式化日期时间
 * @param {string} value 日期时间字符串
 * @returns {string} 格式化后的日期时间
 */
function formatDate(value) {
  return String(value || '').replace('T', ' ').slice(0, 16)
}

// 页面挂载时加载数据
onMounted(() => {
  loadPetState()
  loadOverview()
})
</script>

<template>
  <!-- 金币兑换中心主页面 -->
  <main class="exchange-page">
    <!-- 页面头部区域 -->
    <section class="exchange-hero">
      <div>
        <p class="exchange-kicker">金币兑换中心</p>
        <h1>把学习、实验和游戏中的努力换成可见奖励</h1>
        <p class="exchange-summary">
          金币来自在线课程、题库练习、可视化学习、实验平台在线时长，以及两个学习游戏的结算数据。
        </p>
      </div>
      <!-- 用户金币余额卡片 -->
      <aside class="exchange-balance">
        <span>当前可用金币</span>
        <strong>{{ loading ? '...' : availableCoinTotal.toLocaleString('zh-CN') }}</strong>
        <p>宠物解锁会保存在本地，卡券兑换由后端扣除金币。</p>
        <small v-if="localPetSpent">宠物已使用 {{ localPetSpent.toLocaleString('zh-CN') }} 金币</small>
        <RouterLink to="/exchange/vouchers" class="exchange-voucher-link">我的卡券</RouterLink>
      </aside>
    </section>

    <!-- 兑换提示信息 -->
    <p v-if="exchangeMessage" class="exchange-message">{{ exchangeMessage }}</p>
    <p v-if="exchangeError" class="exchange-message is-error">{{ exchangeError }}</p>

    <!-- 金币获取规则区域 -->
    <section class="exchange-rule-grid" aria-label="金币获取方式">
      <article v-for="rule in earnRules" :key="rule.title" :class="['exchange-rule-card', `is-${rule.tone}`]">
        <span>{{ rule.title }}</span>
        <strong>{{ rule.value }}</strong>
        <p>{{ rule.detail }}</p>
      </article>
    </section>

    <!-- 兑换商品区域列表 -->
    <section v-for="section in displaySections" :key="section.title" class="exchange-section">
      <!-- 区域标题 -->
      <header class="exchange-section-header">
        <div>
          <p>兑换展示</p>
          <h2>{{ section.title }}</h2>
        </div>
        <span>{{ section.subtitle }}</span>
      </header>
      <!-- 商品卡片网格 -->
      <div
        class="exchange-shop-grid"
        :class="{ 'is-pet-grid': section.items.some((item) => item.kind === 'pet') }"
      >
        <article
          v-for="item in section.items"
          :key="item.key || item.voucherKey || item.name"
          class="exchange-shop-card"
          :class="{ 'is-pet': item.kind === 'pet', 'is-active-pet': item.active }"
        >
          <div v-if="item.kind === 'pet'" class="exchange-pet-preview" aria-hidden="true">
            <img :src="item.preview || item.image" :alt="item.name" />
          </div>
          <div v-else class="exchange-shop-icon" aria-hidden="true">{{ item.name.slice(0, 1) }}</div>
          <div class="exchange-shop-info">
            <span>{{ item.tag }}</span>
            <h3>{{ item.name }}</h3>
            <p v-if="item.kind === 'pet'" class="exchange-pet-description">{{ item.description }}</p>
          </div>
          <!-- 底部操作区域 -->
          <footer>
            <strong>{{ item.price.toLocaleString('zh-CN') }} 金币</strong>
            <button
              type="button"
              :disabled="getActionDisabled(item)"
              @click="handleShopAction(item)"
            >
              {{ getActionText(item) }}
            </button>
          </footer>
          <div v-if="item.kind === 'pet'" class="exchange-pet-meta">
            <small>{{ item.owned ? '已拥有' : '未拥有' }}</small>
            <small>{{ item.active ? '当前正在陪伴你' : item.owned ? '可随时切换' : '兑换后立即解锁' }}</small>
          </div>
          <!-- 卡券详细信息 -->
          <div v-if="item.voucherKey" class="exchange-voucher-meta">
            <small>{{ formatVoucherStock(item) }}</small>
            <small>{{ formatVoucherRule(item) }}</small>
            <small>{{ formatVoucherValidity(item) }}</small>
            <small>已拥有 {{ getOwnedQuantity(item) }} 张</small>
          </div>
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

.exchange-balance small {
  margin-top: 8px;
  color: #b45309;
  font-weight: 900;
}

.exchange-voucher-link {
  width: fit-content;
  margin-top: 18px;
  border-radius: 999px;
  padding: 10px 16px;
  color: #0f766e;
  background: rgba(20, 184, 166, 0.14);
  font-weight: 900;
  text-decoration: none;
}

.exchange-message {
  max-width: 1180px;
  margin: 0 auto 16px;
  border-radius: 14px;
  padding: 12px 16px;
  color: #0f766e;
  background: rgba(20, 184, 166, 0.12);
  font-weight: 800;
}

.exchange-message.is-error {
  color: #b91c1c;
  background: rgba(239, 68, 68, 0.12);
}

.exchange-rule-grid,
.exchange-shop-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 16px;
  max-width: 1180px;
  margin: 0 auto;
}

.exchange-shop-grid.is-pet-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
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

.exchange-shop-card.is-pet {
  display: flex;
  min-width: 0;
  min-height: 430px;
  flex-direction: column;
  grid-template-columns: 1fr;
  overflow: hidden;
  border-color: rgba(139, 92, 246, 0.2);
  padding: 22px;
  background:
    radial-gradient(circle at 20% 18%, rgba(255, 183, 213, 0.2), transparent 34%),
    radial-gradient(circle at 90% 4%, rgba(125, 244, 229, 0.2), transparent 30%),
    rgba(255, 255, 255, 0.82);
}

.exchange-shop-card.is-active-pet {
  border-color: rgba(20, 184, 166, 0.5);
  box-shadow:
    0 24px 70px rgba(20, 184, 166, 0.16),
    inset 0 0 0 1px rgba(20, 184, 166, 0.18);
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

.exchange-pet-preview {
  position: relative;
  display: grid;
  min-height: 188px;
  place-items: center;
  border-radius: 18px;
  background:
    linear-gradient(135deg, rgba(255, 247, 251, 0.95), rgba(235, 255, 251, 0.92)),
    #ffffff;
  box-shadow: inset 0 0 0 1px rgba(15, 23, 42, 0.06);
}

.exchange-pet-preview img {
  width: 156px;
  height: 156px;
  object-fit: contain;
  image-rendering: pixelated;
  filter: drop-shadow(0 12px 10px rgba(15, 23, 42, 0.16));
}

.exchange-shop-info {
  min-width: 0;
}

.exchange-shop-card.is-pet .exchange-shop-info {
  display: grid;
  gap: 7px;
}

.exchange-shop-card h3 {
  margin: 6px 0 0;
  font-size: 18px;
}

.exchange-pet-description {
  min-height: 44px;
  margin: 0;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  line-height: 1.65;
}

.exchange-shop-card.is-pet footer {
  margin-top: auto;
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
  color: #0f766e;
  background: rgba(20, 184, 166, 0.14);
  font-weight: 800;
}

.exchange-shop-card button:disabled {
  color: #475569;
  background: rgba(15, 23, 42, 0.08);
  cursor: not-allowed;
}

.exchange-voucher-meta {
  grid-column: 1 / -1;
  display: grid;
  gap: 4px;
}

.exchange-pet-meta {
  grid-column: 1 / -1;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.exchange-pet-meta small {
  border-radius: 999px;
  padding: 6px 10px;
  color: #0f766e;
  background: rgba(20, 184, 166, 0.12);
  font-size: 12px;
  font-weight: 900;
}

.exchange-voucher-meta small {
  color: #64748b;
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

@media (max-width: 980px) {
  .exchange-shop-grid.is-pet-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .exchange-shop-grid.is-pet-grid {
    grid-template-columns: 1fr;
  }
}
</style>
