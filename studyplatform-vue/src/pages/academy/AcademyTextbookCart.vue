﻿<script setup>
/**
 * 鏁欐潗璐墿杞︾粨绠楅〉闈㈢粍浠? * 灞曠ず璐墿杞﹀晢鍝佸垪琛紝鏀寔鏁伴噺璋冩暣銆佸晢鍝佸垹闄ゃ€佷紭鎯犲埜浣跨敤鍜岃鍗曟彁浜? */
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import {
  createAcademyTextbookOrder,
  createAcademyTextbookPayment,
  fetchAcademyTextbookCart,
  fetchAcademyTextbookPaymentStatus,
  getAcademyTextbookPaymentCashierUrl,
  getAcademyTextbookPaymentQrUrl,
  refreshAcademyTextbookPaymentStatus,
  removeAcademyTextbookCartItem,
  updateAcademyTextbookCartItem,
} from '../../api/academy'
import { resolveResourceUrl } from '../../api/request'
import { fetchUserVouchers } from '../../api/vouchers'

// 璺敱瀹炰緥
const router = useRouter()
// 璐墿杞﹀晢鍝佸垪琛?
const cartItems = ref([])
// 閫変腑鐨勫晢鍝両D鍒楄〃
const selectedIds = ref([])
// 鍔犺浇鐘舵€?
const loading = ref(false)
// 鎻愪氦璁㈠崟涓姸鎬?
const submitting = ref(false)
// 鍙嶉鎻愮ず鍙鎬?
const feedbackVisible = ref(false)
// 鍙嶉鎻愮ず娑堟伅
const feedbackMessage = ref('')
// 寰呮敮浠樿鍗?
const pendingOrder = ref(null)
// 鏀粯寮圭獥鍙鎬?
const paymentVisible = ref(false)
// 鏀粯涓姸鎬?
const paying = ref(false)
// 鐢ㄦ埛鍗″埜鍒楄〃
const paymentProvider = ref('WECHAT')
const paymentSession = ref(null)
const paymentStatus = ref('PENDING')
const paymentError = ref('')
const paymentMode = ref('NATIVE')
const vouchers = ref([])
// 鏄惁浣跨敤鏁欐潗浼樻儬鍒?
const useTextbookVoucher = ref(false)
const selectedVoucherKey = ref('')
// 浼樻儬鍒告槸鍚﹀凡琚敤鎴锋搷浣滆繃
const voucherTouched = ref(false)
// 鍙嶉鎻愮ず瀹氭椂鍣?
let feedbackTimer = null

/**
 * 鑾峰彇閫変腑鐨勫晢鍝佸垪琛? * @returns {Array} 閫変腑鐨勫晢鍝佹暟缁? */
let paymentPollingTimer = null

const selectedItems = computed(() => cartItems.value.filter((item) => selectedIds.value.includes(item.id)))

/**
 * 璁＄畻閫変腑鍟嗗搧鐨勬€绘暟閲? * @returns {number} 鎬绘暟閲? */
const selectedCount = computed(() => selectedItems.value.reduce((total, item) => total + Number(item.quantity || 1), 0))

/**
 * 璁＄畻閫変腑鍟嗗搧鐨勫皬璁￠噾棰? * @returns {number} 灏忚閲戦
 */
const subtotal = computed(() => selectedItems.value.reduce((total, item) => total + itemTotal(item), 0))

/**
 * 璁＄畻婊″噺浼樻儬閲戦
 * @returns {number} 婊″噺閲戦
 */
const discount = computed(() => subtotal.value >= 150 ? 18 : subtotal.value >= 80 ? 8 : 0)

/**
 * 鑾峰彇鏁欐潗浼樻儬鍒镐俊鎭? * @returns {Object|null} 浼樻儬鍒稿璞? */
const availableTextbookVouchers = computed(() => vouchers.value
  .filter((item) => Number(item.quantity ?? 0) > 0)
  .filter((item) => item.voucherType === 'DISCOUNT')
  .map((item) => ({
    ...item,
    discountValue: calculateVoucherDiscount(item, subtotal.value),
  }))
  .filter((item) => item.discountValue > 0))
const selectedTextbookVoucher = computed(() => (
  availableTextbookVouchers.value.find((item) => item.voucherKey === selectedVoucherKey.value)
  || availableTextbookVouchers.value[0]
  || null
))

/**
 * 鍒ゆ柇鏁欐潗浼樻儬鍒告槸鍚﹀彲鐢? * @returns {boolean} 鏄惁鍙敤
 */
const canUseTextbookVoucher = computed(() => availableTextbookVouchers.value.length > 0)

/**
 * 璁＄畻鏁欐潗浼樻儬鍒告姷鎵ｉ噾棰? * @returns {number} 鎶垫墸閲戦
 */
const textbookVoucherDiscount = computed(() => (
  useTextbookVoucher.value && selectedTextbookVoucher.value ? selectedTextbookVoucher.value.discountValue : 0
))

/**
 * 璁＄畻鎬讳紭鎯犻噾棰濓紙婊″噺 + 浼樻儬鍒革級
 * @returns {number} 鎬讳紭鎯犻噾棰? */
const totalDiscount = computed(() => discount.value + textbookVoucherDiscount.value)

/**
 * 璁＄畻閰嶉€佽垂
 * @returns {number} 閰嶉€佽垂閲戦
 */
const freight = computed(() => (subtotal.value > 0 && subtotal.value < 99 ? 6 : 0))

/**
 * 璁＄畻瀹為檯搴斾粯閲戦
 * @returns {number} 搴斾粯閲戦
 */
const payable = computed(() => Math.max(subtotal.value - totalDiscount.value + freight.value, 0))
const paymentQrUrl = computed(() => (
  paymentMode.value === 'NATIVE' && paymentSession.value?.sessionId ? getAcademyTextbookPaymentQrUrl(paymentSession.value.sessionId) : ''
))
const paymentCashierUrl = computed(() => (
  paymentSession.value?.sessionId ? getAcademyTextbookPaymentCashierUrl(paymentSession.value.sessionId) : ''
))
const paymentProviderLabel = computed(() => {
  if (paymentMode.value === 'PAGE') return '支付宝网页收银台'
  return paymentProvider.value === 'ALIPAY' ? '支付宝' : '微信'
})
const pendingOrderDiscount = computed(() => Number(pendingOrder.value?.discountAmount || 0))
const pendingOrderVoucherName = computed(() => (
  pendingOrder.value?.voucherName
  || pendingOrder.value?.voucherKey
  || selectedTextbookVoucher.value?.name
  || ''
))

/**
 * 鍒ゆ柇鏄惁鍏ㄩ€? * @returns {boolean} 鏄惁鍏ㄩ€? */
const allSelected = computed(() => cartItems.value.length > 0 && selectedIds.value.length === cartItems.value.length)

/**
 * 鍔犺浇璐墿杞﹀拰鍗″埜鏁版嵁
 */
const loadCart = async () => {
  loading.value = true
  try {
    const [nextCartItems, nextVouchers] = await Promise.all([
      fetchAcademyTextbookCart(),
      fetchUserVouchers().catch(() => []),
    ])
    cartItems.value = nextCartItems
    vouchers.value = nextVouchers
    selectedIds.value = cartItems.value.map((item) => item.id)
    if (!voucherTouched.value) {
      useTextbookVoucher.value = canUseTextbookVoucher.value
      selectedVoucherKey.value = availableTextbookVouchers.value[0]?.voucherKey || ''
    }
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '购物车加载失败')
  } finally {
    loading.value = false
  }
}

/**
 * 鍒囨崲鍏ㄩ€夌姸鎬? */
const toggleAll = () => {
  selectedIds.value = allSelected.value ? [] : cartItems.value.map((item) => item.id)
}

/**
 * 淇敼鍟嗗搧鏁伴噺
 * @param {Object} item 璐墿杞﹀晢鍝侀」
 * @param {number} delta 鏁伴噺鍙樺寲鍊硷紙+1鎴?1锛? */
const changeQuantity = async (item, delta) => {
  const nextQuantity = Math.min(Math.max(Number(item.quantity || 1) + delta, 1), 99)
  if (nextQuantity === Number(item.quantity)) return
  try {
    cartItems.value = await updateAcademyTextbookCartItem(item.id, {
      quantity: nextQuantity,
    })
    selectedIds.value = selectedIds.value.filter((id) => cartItems.value.some((cartItem) => cartItem.id === id))
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '数量更新失败')
  }
}

/**
 * 鍒犻櫎璐墿杞﹀晢鍝? * @param {Object} item 璐墿杞﹀晢鍝侀」
 */
const removeItem = async (item) => {
  try {
    cartItems.value = await removeAcademyTextbookCartItem(item.id)
    selectedIds.value = selectedIds.value.filter((id) => id !== item.id)
    showFeedback('已移除教材')
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '移除失败')
  }
}

/**
 * 鎻愪氦璁㈠崟
 */
const submitOrder = async () => {
  if (!selectedIds.value.length) {
    showFeedback('请先选择要结算的教材')
    return
  }
  submitting.value = true
  try {
    const order = await createAcademyTextbookOrder({
      cartItemIds: selectedIds.value,
      useVoucher: useTextbookVoucher.value && Boolean(selectedTextbookVoucher.value),
      voucherKey: useTextbookVoucher.value ? selectedTextbookVoucher.value?.voucherKey : '',
    })
    pendingOrder.value = order
    paymentVisible.value = true
    await startPaymentSession('WECHAT')
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '结算失败')
  } finally {
    submitting.value = false
  }
}

/**
 * 纭鏀粯璁㈠崟
 */
const confirmPayment = async () => {
  if (!paymentSession.value?.sessionId) return
  paying.value = true
  try {
    const result = await refreshAcademyTextbookPaymentStatus(paymentSession.value.sessionId)
    handlePaymentStatus(result)
    vouchers.value = await fetchUserVouchers().catch(() => [])
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '支付失败')
  } finally {
    paying.value = false
  }
}

/**
 * 鍙栨秷鏀粯
 */
const cancelPayment = () => {
  stopPaymentPolling()
  paymentVisible.value = false
  pendingOrder.value = null
  paymentSession.value = null
  paymentStatus.value = 'PENDING'
  paymentError.value = ''
  paymentMode.value = 'NATIVE'
}

const startPaymentSession = async (provider = paymentProvider.value, mode = 'NATIVE') => {
  if (!pendingOrder.value?.orderNo) return
  stopPaymentPolling()
  paying.value = true
  paymentError.value = ''
  paymentSession.value = null
  paymentStatus.value = 'PENDING'
  try {
    paymentProvider.value = provider
    paymentMode.value = mode
    paymentSession.value = await createAcademyTextbookPayment(pendingOrder.value.orderNo, {
      provider,
      paymentMode: mode,
    })
    paymentStatus.value = paymentSession.value.status || 'PENDING'
    if (mode === 'PAGE' && paymentCashierUrl.value) {
      window.open(paymentCashierUrl.value, '_blank', 'noopener,noreferrer')
    }
    startPaymentPolling()
  } catch (error) {
    paymentError.value = error instanceof Error ? error.message : '支付会话生成失败'
    showFeedback(paymentError.value)
  } finally {
    paying.value = false
  }
}

const startPaymentPolling = () => {
  stopPaymentPolling()
  paymentPollingTimer = window.setInterval(checkPaymentStatus, 1800)
}

const stopPaymentPolling = () => {
  window.clearInterval(paymentPollingTimer)
  paymentPollingTimer = null
}

const checkPaymentStatus = async () => {
  if (!paymentSession.value?.sessionId) return
  try {
    const result = await fetchAcademyTextbookPaymentStatus(paymentSession.value.sessionId)
    handlePaymentStatus(result)
  } catch (error) {
    stopPaymentPolling()
    showFeedback(error instanceof Error ? error.message : '支付状态查询失败')
  }
}

const handlePaymentStatus = (result) => {
  paymentStatus.value = result?.status || 'PENDING'
  if (result?.paid) {
    stopPaymentPolling()
    paymentVisible.value = false
    pendingOrder.value = null
    paymentSession.value = null
    paymentError.value = ''
    showFeedback(result?.message || '支付成功')
    loadCart()
  } else if (result?.status === 'EXPIRED') {
    stopPaymentPolling()
    showFeedback(result?.message || '支付会话已过期')
  }
}

/**
 * 鏄剧ず鍙嶉鎻愮ず娑堟伅
 * @param {string} message 鎻愮ず娑堟伅
 */
const showFeedback = (message) => {
  feedbackMessage.value = message
  feedbackVisible.value = true
  window.clearTimeout(feedbackTimer)
  feedbackTimer = window.setTimeout(() => {
    feedbackVisible.value = false
  }, 1800)
}

/**
 * 璁＄畻鍗曚釜鍟嗗搧鐨勯噾棰? * @param {Object} item 璐墿杞﹀晢鍝侀」
 * @returns {number} 鍟嗗搧閲戦
 */
const itemTotal = (item) => {
  const unitPrice = Number(item.unitPrice)
  const quantity = Number(item.quantity || 1)
  return (Number.isFinite(unitPrice) ? unitPrice : 0) * (Number.isFinite(quantity) ? quantity : 1)
}

function calculateVoucherDiscount(voucher, amount) {
  const original = Math.max(Number(amount || 0), 0)
  const threshold = Number(voucher?.thresholdAmount ?? 0)
  if (!Number.isFinite(original) || original < threshold) return 0
  let discountValue = 0
  if (voucher?.discountType === 'AMOUNT') {
    discountValue = Number(voucher.discountAmount ?? 0)
  } else if (voucher?.discountType === 'PERCENT') {
    discountValue = original * (1 - Number(voucher.discountRate ?? 1))
  }
  const maxDiscount = Number(voucher?.maxDiscountAmount ?? 0)
  if (Number.isFinite(maxDiscount) && maxDiscount > 0) {
    discountValue = Math.min(discountValue, maxDiscount)
  }
  return Math.min(Math.max(discountValue, 0), original)
}

function formatVoucherRule(voucher) {
  const threshold = Number(voucher?.thresholdAmount ?? 0)
  const prefix = threshold > 0 ? `满 ${threshold.toFixed(0)} 元` : '无门槛'
  if (voucher?.discountType === 'AMOUNT') {
    return `${prefix} 减 ${Number(voucher.discountAmount ?? 0).toFixed(0)}`
  }
  if (voucher?.discountType === 'PERCENT') {
    return `${prefix} ${Math.round(Number(voucher.discountRate ?? 1) * 10)} 折`
  }
  return prefix
}

function toggleTextbookVoucher() {
  voucherTouched.value = true
  if (!useTextbookVoucher.value) {
    selectedVoucherKey.value = ''
    return
  }
  selectedVoucherKey.value = selectedTextbookVoucher.value?.voucherKey || ''
}

function selectTextbookVoucher(voucherKey) {
  voucherTouched.value = true
  selectedVoucherKey.value = voucherKey
  useTextbookVoucher.value = Boolean(voucherKey)
}

/**
 * 鏍煎紡鍖栦环鏍兼樉绀烘枃鏈? * @param {*} value 浠锋牸鍊? * @returns {string} 鏍煎紡鍖栧悗鐨勪环鏍兼枃鏈? */
const formatPrice = (value) => `￥${Number(value || 0).toFixed(2)}`

// 椤甸潰鎸傝浇鏃跺姞杞借喘鐗╄溅鏁版嵁
onMounted(loadCart)

watch(canUseTextbookVoucher, (canUse) => {
  if (!canUse) {
    useTextbookVoucher.value = false
    selectedVoucherKey.value = ''
  } else if (!voucherTouched.value) {
    useTextbookVoucher.value = true
    selectedVoucherKey.value = availableTextbookVouchers.value[0]?.voucherKey || ''
  }
})

// 缁勪欢鍗歌浇鍓嶆竻鐞嗗畾鏃跺櫒
onBeforeUnmount(() => {
  window.clearTimeout(feedbackTimer)
  stopPaymentPolling()
})
</script>

<template>
  <!-- 鏁欐潗璐墿杞︿富椤甸潰 -->
  <main class="online-course-main textbook-cart-main">
    <!-- 鍙嶉鎻愮ず娑堟伅 -->
    <Transition name="academy-assignment-feedback">
      <div v-if="feedbackVisible" class="academy-assignment-feedback-toast" role="status">
        {{ feedbackMessage }}
      </div>
    </Transition>

    <!-- 鏀粯寮圭獥 -->
    <Transition name="academy-assignment-feedback">
      <div v-if="paymentVisible" class="textbook-payment-mask">
        <section class="textbook-payment-dialog" role="dialog" aria-modal="true" aria-label="确认支付">
          <span>{{ paymentProviderLabel }}</span>
          <h2>等待付款确认</h2>
          <p>订单号：{{ pendingOrder?.orderNo }}</p>
          <strong>{{ formatPrice(pendingOrder?.totalAmount) }}</strong>
          <p v-if="pendingOrderDiscount > 0">
            已使用 {{ pendingOrderVoucherName }}，优惠 {{ formatPrice(pendingOrderDiscount) }}
          </p>
          <p v-else>本次支付未使用优惠券</p>
          <div class="textbook-payment-tabs">
            <button type="button" :class="{ 'is-active': paymentProvider === 'WECHAT' && paymentMode === 'NATIVE' }" :disabled="paying" @click="startPaymentSession('WECHAT', 'NATIVE')">微信扫码</button>
            <button type="button" :class="{ 'is-active': paymentProvider === 'ALIPAY' && paymentMode === 'NATIVE' }" :disabled="paying" @click="startPaymentSession('ALIPAY', 'NATIVE')">支付宝扫码</button>
            <button type="button" :class="{ 'is-active': paymentProvider === 'ALIPAY' && paymentMode === 'PAGE' }" :disabled="paying" @click="startPaymentSession('ALIPAY', 'PAGE')">支付宝网页收银台</button>
          </div>
          <div class="textbook-payment-qr" aria-label="付款二维码">
            <img v-if="paymentQrUrl" :src="paymentQrUrl" :alt="`${paymentProviderLabel}付款二维码`" />
            <span v-else-if="paymentMode === 'PAGE' && paymentSession">已打开支付宝沙箱网页收银台，付款后请返回本页刷新状态</span>
            <span v-else-if="paymentError" class="is-error">{{ paymentError }}</span>
            <span v-else>{{ paying ? '支付会话生成中...' : '请选择支付方式' }}</span>
          </div>
          <p class="textbook-payment-status">
            {{ paymentError || (paymentStatus === 'EXPIRED' ? '支付会话已过期，请重新生成' : `请使用${paymentProviderLabel}完成支付，付款成功后系统会自动确认`) }}
          </p>
          <div class="textbook-payment-actions">
            <button type="button" @click="cancelPayment">稍后支付</button>
            <button type="button" class="is-primary" :disabled="paying" @click="confirmPayment">
              {{ paying ? '查询中...' : '刷新支付状态' }}
            </button>
          </div>
        </section>
      </div>
    </Transition>

    <!-- 杩斿洖鏁欐潗鍒楄〃鎸夐挳 -->
    <button type="button" class="textbook-detail-back" @click="router.push('/academy/textbooks')">
      继续挑选教材
    </button>

    <!-- 椤甸潰鏍囬鍖哄煙 -->
    <section class="textbook-cart-checkout-hero">
      <div>
        <span>Textbook Cart</span>
        <h1>教材购物车</h1>
        <p>核对教材、配送信息和优惠后即可提交订单。</p>
      </div>
      <!-- 寰呯粨绠楁暟閲?-->
      <div class="textbook-cart-checkout-stat">
        <strong>{{ selectedCount }}</strong>
        <span>本待结算</span>
      </div>
    </section>

    <!-- 鍔犺浇鐘舵€?-->
    <div v-if="loading" class="academy-state">正在加载购物车...</div>

    <!-- 缁撶畻甯冨眬 -->
    <section v-else class="textbook-cart-checkout-layout">
      <!-- 璐墿杞﹀晢鍝佸垪琛?-->
      <div class="textbook-cart-checkout-list">
        <!-- 鍒楄〃琛ㄥご -->
        <div class="textbook-cart-checkout-toolbar">
          <label>
            <input type="checkbox" :checked="allSelected" @change="toggleAll" />
            全选
          </label>
          <span>商品信息</span>
          <span>单价</span>
          <span>数量</span>
          <span>小计</span>
        </div>

        <!-- 鍟嗗搧鍒楄〃 -->
        <div v-if="cartItems.length" class="textbook-cart-checkout-items">
          <article v-for="item in cartItems" :key="item.id" class="textbook-cart-checkout-item">
            <!-- 閫夋嫨妗?-->
            <label class="textbook-cart-checkout-select">
              <input v-model="selectedIds" type="checkbox" :value="item.id" />
            </label>
            <!-- 鍟嗗搧淇℃伅 -->
            <button type="button" class="textbook-cart-checkout-book" @click="router.push(`/academy/textbooks/${item.textbookId}`)">
              <img :src="resolveResourceUrl(item.cover || item.coverUrl)" :alt="item.name" />
              <span>
                <strong>{{ item.name }}</strong>
                <em>{{ item.publisher }} · {{ item.editor || '暂无主编' }}</em>
              </span>
            </button>
            <!-- 鍗曚环 -->
            <span class="textbook-cart-checkout-price">{{ formatPrice(item.unitPrice) }}</span>
            <!-- 鏁伴噺璋冩暣 -->
            <div class="textbook-cart-checkout-quantity">
              <button type="button" @click="changeQuantity(item, -1)">-</button>
              <span>{{ item.quantity }}</span>
              <button type="button" @click="changeQuantity(item, 1)">+</button>
            </div>
            <!-- 灏忚 -->
            <strong class="textbook-cart-checkout-total">{{ formatPrice(itemTotal(item)) }}</strong>
            <!-- 鍒犻櫎鎸夐挳 -->
            <button type="button" class="textbook-cart-checkout-remove" @click="removeItem(item)">删除</button>
          </article>
        </div>

        <!-- 绌鸿喘鐗╄溅鐘舵€?-->
        <div v-else class="textbook-cart-checkout-empty">
          <strong>购物车还是空的</strong>
          <p>去教材列表挑一本适合当前课程的教材吧。</p>
          <button type="button" @click="router.push('/academy/textbooks')">去逛逛</button>
        </div>
      </div>

      <!-- 鍙充晶缁撶畻闈㈡澘 -->
      <aside class="textbook-cart-checkout-side">
        <!-- 鏀惰揣淇℃伅 -->
        <section>
          <h2>收货信息</h2>
          <p>默认地址：校内驿站 A 区 03 柜</p>
          <p>联系人：默认用户 · 138****2026</p>
          <button type="button">更换地址</button>
        </section>

        <!-- 浼樻儬涓庨厤閫?-->
        <section>
          <h2>优惠与配送</h2>
          <div><span>满减优惠</span><strong>-{{ formatPrice(discount).slice(1) }}</strong></div>
          <!-- 浼樻儬鍒搁€夋嫨 -->
          <template v-if="canUseTextbookVoucher">
            <label class="textbook-voucher-toggle">
              <input
                v-model="useTextbookVoucher"
                type="checkbox"
                @change="toggleTextbookVoucher"
              />
              <span>使用优惠券</span>
              <strong>{{ selectedTextbookVoucher ? `${selectedTextbookVoucher.name} · 剩余 ${selectedTextbookVoucher.quantity} 张` : '未选择' }}</strong>
            </label>
            <div v-if="useTextbookVoucher" class="textbook-voucher-options">
              <button
                v-for="voucher in availableTextbookVouchers"
                :key="voucher.voucherKey"
                type="button"
                :class="{ 'is-active': selectedVoucherKey === voucher.voucherKey }"
                @click="selectTextbookVoucher(voucher.voucherKey)"
              >
                <span>{{ voucher.name }}</span>
                <strong>{{ formatVoucherRule(voucher) }} · 省 {{ formatPrice(voucher.discountValue) }} · {{ voucher.quantity }} 张</strong>
              </button>
            </div>
            <div><span>优惠券抵扣</span><strong>-{{ formatPrice(textbookVoucherDiscount).slice(1) }}</strong></div>
          </template>
          <div><span>配送费</span><strong>{{ freight === 0 ? '包邮' : formatPrice(freight) }}</strong></div>
          <p>满 99 元包邮，平台满减与教材优惠券可同时使用；优惠券可自行勾选或取消。</p>
        </section>

        <!-- 璁㈠崟缁撶畻 -->
        <section class="textbook-cart-checkout-summary">
          <h2>订单结算</h2>
          <div><span>商品合计</span><strong>{{ formatPrice(subtotal) }}</strong></div>
          <div><span>已选数量</span><strong>{{ selectedCount }} 本</strong></div>
          <div class="is-payable"><span>应付金额</span><strong>{{ formatPrice(payable) }}</strong></div>
          <button type="button" :disabled="submitting || !selectedIds.length" @click="submitOrder">
            {{ submitting ? '正在提交...' : '去结算' }}
          </button>
        </section>
      </aside>
    </section>
  </main>
</template>

