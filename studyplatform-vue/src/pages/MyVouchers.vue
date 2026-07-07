<script setup>
/**
 * 我的卡券页面组件
 * 展示用户已兑换的优惠券和游戏券
 */
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchUserVouchers } from '../api/vouchers'

// 用户卡券列表
const vouchers = ref([])
// 加载状态
const loading = ref(true)
// 错误提示信息
const errorMessage = ref('')

/**
 * 获取优惠券列表（过滤类型为 DISCOUNT 的卡券）
 * @returns {Array} 优惠券数组
 */
const discountVouchers = computed(() => vouchers.value.filter((item) => item.voucherType === 'DISCOUNT'))

/**
 * 获取游戏券列表（过滤类型为 GAME_ITEM 的卡券）
 * @returns {Array} 游戏券数组
 */
const gameVouchers = computed(() => vouchers.value.filter((item) => item.voucherType === 'GAME_ITEM'))

/**
 * 加载用户卡券数据
 */
async function loadVouchers() {
  loading.value = true
  errorMessage.value = ''
  try {
    vouchers.value = await fetchUserVouchers()
  } catch (error) {
    errorMessage.value = error?.message || '卡券加载失败'
  } finally {
    loading.value = false
  }
}

// 页面挂载时加载卡券数据
onMounted(loadVouchers)

/**
 * 格式化卡券使用规则
 * @param {Object} item 卡券对象
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
 * @param {Object} item 卡券对象
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
</script>

<template>
  <!-- 我的卡券主页面 -->
  <main class="voucher-page">
    <!-- 页面头部区域 -->
    <section class="voucher-hero">
      <div>
        <p>我的卡券</p>
        <h1>优惠券与游戏券</h1>
        <span>这里显示当前账号已经兑换的优惠券、技能刷新券和复活券。</span>
      </div>
      <RouterLink to="/exchange" class="voucher-back-link">返回兑换中心</RouterLink>
    </section>

    <!-- 错误提示 -->
    <p v-if="errorMessage" class="voucher-error">{{ errorMessage }}</p>
    <!-- 加载中提示 -->
    <p v-else-if="loading" class="voucher-empty">正在加载卡券...</p>
    <!-- 空状态提示 -->
    <p v-else-if="vouchers.length === 0" class="voucher-empty">暂无卡券，可先前往兑换中心兑换。</p>

    <!-- 游戏券区域 -->
    <section v-if="gameVouchers.length" class="voucher-section">
      <header>
        <p>Game Tickets</p>
        <h2>游戏券</h2>
      </header>
      <div class="voucher-grid">
        <article v-for="item in gameVouchers" :key="item.voucherKey" class="voucher-card is-game">
          <span>{{ item.name }}</span>
          <strong>x{{ item.quantity }}</strong>
          <p>{{ item.description }}</p>
          <p>{{ formatVoucherValidity(item) }}</p>
        </article>
      </div>
    </section>

    <!-- 优惠券区域 -->
    <section v-if="discountVouchers.length" class="voucher-section">
      <header>
        <p>Coupons</p>
        <h2>优惠券</h2>
      </header>
      <div class="voucher-grid">
        <article v-for="item in discountVouchers" :key="item.voucherKey" class="voucher-card">
          <span>{{ item.name }}</span>
          <strong>x{{ item.quantity }}</strong>
          <p>{{ item.description }}</p>
          <p>{{ formatVoucherRule(item) }}</p>
          <p>{{ formatVoucherValidity(item) }}</p>
        </article>
      </div>
    </section>
  </main>
</template>

<style scoped>
.voucher-page {
  min-height: 100vh;
  padding: 112px clamp(20px, 5vw, 72px) 56px;
  background: linear-gradient(135deg, #eef8f6 0%, #f8fbff 48%, #fff6df 100%);
  color: #172033;
}

.voucher-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  align-items: end;
  max-width: 1120px;
  margin: 0 auto 28px;
}

.voucher-hero p,
.voucher-section header p {
  margin: 0 0 10px;
  color: #0f766e;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.voucher-hero h1 {
  margin: 0;
  font-size: clamp(34px, 5vw, 58px);
  letter-spacing: 0;
}

.voucher-hero span {
  display: block;
  max-width: 620px;
  margin-top: 14px;
  color: #64748b;
  line-height: 1.8;
}

.voucher-back-link {
  flex: 0 0 auto;
  border-radius: 999px;
  padding: 12px 18px;
  color: #0f766e;
  background: rgba(20, 184, 166, 0.14);
  font-weight: 900;
  text-decoration: none;
}

.voucher-empty,
.voucher-error {
  max-width: 1120px;
  margin: 0 auto;
  border-radius: 14px;
  padding: 18px;
  background: rgba(255, 255, 255, 0.78);
  color: #64748b;
}

.voucher-error {
  color: #b91c1c;
}

.voucher-section {
  max-width: 1120px;
  margin: 30px auto 0;
}

.voucher-section h2 {
  margin: 0;
  font-size: 28px;
}

.voucher-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.voucher-card {
  min-height: 170px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 18px;
  padding: 22px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(255, 248, 225, 0.84)),
    radial-gradient(circle at 92% 12%, rgba(245, 158, 11, 0.24), transparent 30%);
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.1);
}

.voucher-card.is-game {
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(236, 253, 245, 0.84)),
    radial-gradient(circle at 92% 12%, rgba(20, 184, 166, 0.24), transparent 30%);
}

.voucher-card span {
  color: #0f172a;
  font-size: 19px;
  font-weight: 900;
}

.voucher-card strong {
  display: block;
  margin-top: 18px;
  color: #b45309;
  font-size: 38px;
  line-height: 1;
}

.voucher-card p {
  margin: 16px 0 0;
  color: #64748b;
  line-height: 1.7;
}

@media (max-width: 760px) {
  .voucher-page {
    padding-top: 88px;
  }

  .voucher-hero {
    display: block;
  }

  .voucher-back-link {
    display: inline-flex;
    margin-top: 18px;
  }
}
</style>
