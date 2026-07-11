﻿<script setup>
/**
 * 鏁欐潗璇︽儏椤甸潰缁勪欢
 * 灞曠ず鏁欐潗璇︾粏淇℃伅锛屾敮鎸佸姞鍏ヨ喘鐗╄溅銆佺珛鍗宠喘涔般€佽瘎璁虹瓑鍔熻兘
 */
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import {
  addAcademyTextbookCartItem,
  createAcademyTextbookOrder,
  createAcademyTextbookPayment,
  createAcademyTextbookReview,
  fetchAcademyTextbook,
  fetchAcademyTextbookCart,
  fetchAcademyTextbookPaymentStatus,
  getAcademyTextbookPaymentCashierUrl,
  getAcademyTextbookPaymentQrUrl,
  refreshAcademyTextbookPaymentStatus,
  removeAcademyTextbookCartItem,
} from '../../api/academy'
import { resolveResourceUrl } from '../../api/request'
import { fetchUserVouchers } from '../../api/vouchers'

// 缁勪欢灞炴€у畾涔?
const props = defineProps({
  textbookId: {
    type: String,
    required: true,
  },
})

// 璺敱瀹炰緥
const router = useRouter()
// 鏁欐潗璇︽儏鏁版嵁
const textbook = ref(null)
// 鍔犺浇鐘舵€?
const loading = ref(false)
// 閿欒鎻愮ず淇℃伅
const error = ref('')
// 璐拱鏁伴噺
const quantity = ref(1)
// 鍙嶉鎻愮ず鍙鎬?
const feedbackVisible = ref(false)
// 鍙嶉鎻愮ず娑堟伅
const feedbackMessage = ref('')
// 璐墿杞﹀晢鍝佸垪琛?
const cartItems = ref([])
// 绠€浠嬪睍寮€鐘舵€?
const overviewExpanded = ref(false)
// 鐩綍灞曞紑鐘舵€?
const catalogExpanded = ref(false)
// 寰呮敮浠樿鍗?
const pendingOrder = ref(null)
// 鏀粯寮圭獥鍙鎬?
const paymentVisible = ref(false)
// 鏀粯涓姸鎬?
const paying = ref(false)
const paymentProvider = ref('WECHAT')
const paymentSession = ref(null)
const paymentStatus = ref('PENDING')
const paymentError = ref('')
const paymentMode = ref('NATIVE')
// 鐢ㄦ埛鍗″埜鍒楄〃
const vouchers = ref([])
// 鏄惁浣跨敤鏁欐潗浼樻儬鍒?
const useTextbookVoucher = ref(false)
const selectedVoucherKey = ref('')
// 浼樻儬鍒告槸鍚﹀凡琚敤鎴锋搷浣滆繃
const voucherTouched = ref(false)
// 璇勮鎻愪氦涓姸鎬?
const reviewSubmitting = ref(false)
// 璇勮琛ㄥ崟鏁版嵁
const reviewForm = ref({
  userName: '默认用户',
  rating: 5,
  content: '',
})
// 鍙嶉鎻愮ず瀹氭椂鍣?
let feedbackTimer = null
let paymentPollingTimer = null

/**
 * 瑙ｆ瀽鏁欐潗灏侀潰鍥剧墖URL
 * @returns {string} 灏侀潰鍥剧墖URL
 */
const cover = computed(() => resolveResourceUrl(textbook.value?.cover || textbook.value?.coverUrl))

/**
 * 鑾峰彇鏁欐潗鐩綍鍒楄〃
 * @returns {Array} 鐩綍鏁扮粍
 */
const catalog = computed(() => textbook.value?.catalog?.length ? textbook.value.catalog : ['暂无目录信息'])

/**
 * 鑾峰彇鏁欐潗璇勮鍒楄〃
 * @returns {Array} 璇勮鏁扮粍
 */
const comments = computed(() => textbook.value?.comments?.length ? textbook.value.comments : [])

/**
 * 鑾峰彇鍘熶环鏄剧ず鏂囨湰
 * @returns {string} 鍘熶环鏂囨湰
 */
const originalPrice = computed(() => formatPrice(textbook.value?.originalPrice))

/**
 * 鑾峰彇鎶樻墸浠锋樉绀烘枃鏈?
 * @returns {string} 鎶樻墸浠锋枃鏈?
 */
const discountPrice = computed(() => formatPrice(textbook.value?.discountPrice))

/**
 * 璁＄畻璐拱灏忚閲戦
 * @returns {number} 灏忚閲戦
 */
const buySubtotal = computed(() => {
  const unitPrice = Number(textbook.value?.discountPrice ?? 0)
  return (Number.isFinite(unitPrice) ? unitPrice : 0) * Number(quantity.value || 1)
})

/**
 * 鑾峰彇鏁欐潗浼樻儬鍒镐俊鎭?
 * @returns {Object|null} 浼樻儬鍒稿璞?
 */
const availableTextbookVouchers = computed(() => vouchers.value
  .filter((item) => Number(item.quantity ?? 0) > 0)
  .filter((item) => item.voucherType === 'DISCOUNT')
  .map((item) => ({
    ...item,
    discountValue: calculateVoucherDiscount(item, buySubtotal.value),
  }))
  .filter((item) => item.discountValue > 0))
const selectedTextbookVoucher = computed(() => (
  availableTextbookVouchers.value.find((item) => item.voucherKey === selectedVoucherKey.value)
  || availableTextbookVouchers.value[0]
  || null
))

/**
 * 鍒ゆ柇鏁欐潗浼樻儬鍒告槸鍚﹀彲鐢?
 * @returns {boolean} 鏄惁鍙敤
 */
const canUseTextbookVoucher = computed(() => availableTextbookVouchers.value.length > 0)

/**
 * 璁＄畻鏁欐潗浼樻儬鍒告姷鎵ｉ噾棰?
 * @returns {number} 鎶垫墸閲戦
 */
const textbookVoucherDiscount = computed(() => (
  useTextbookVoucher.value && selectedTextbookVoucher.value ? selectedTextbookVoucher.value.discountValue : 0
))

/**
 * 璁＄畻瀹為檯搴斾粯閲戦
 * @returns {number} 搴斾粯閲戦
 */
const buyPayable = computed(() => Math.max(buySubtotal.value - textbookVoucherDiscount.value, 0))
const purchaseDisplayPrice = computed(() => (
  textbookVoucherDiscount.value > 0 ? formatPrice(buyPayable.value) : discountPrice.value
))
const purchaseReferencePrice = computed(() => (
  textbookVoucherDiscount.value > 0 ? formatPrice(buySubtotal.value) : ''
))
const purchasePriceLabel = computed(() => (
  textbookVoucherDiscount.value > 0 ? '用券后预计支付' : '当前价格'
))
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
 * 鑾峰彇绠€浠嬫钀藉垪琛?
 * @returns {Array} 娈佃惤鏁扮粍
 */
const overviewParagraphs = computed(() => {
  const overview = textbook.value?.overview || textbook.value?.description || '暂无简介'
  return overview.split(/\n+/).map((line) => line.trim()).filter(Boolean)
})

/**
 * 璁＄畻绠€浠嬫枃鏈€婚暱搴?
 * @returns {number} 鏂囨湰闀垮害
 */
const overviewTextLength = computed(() => overviewParagraphs.value.join('').length)

/**
 * 鍒ゆ柇绠€浠嬫槸鍚﹂渶瑕佸睍寮€
 * @returns {boolean} 鏄惁闇€瑕佸睍寮€
 */
const isOverviewLong = computed(() => overviewParagraphs.value.length > 2 || overviewTextLength.value > 260)

/**
 * 璁＄畻鐩綍鏂囨湰鎬婚暱搴?
 * @returns {number} 鏂囨湰闀垮害
 */
const catalogTextLength = computed(() => catalog.value.join('').length)

/**
 * 鍒ゆ柇鐩綍鏄惁闇€瑕佸睍寮€
 * @returns {boolean} 鏄惁闇€瑕佸睍寮€
 */
const isCatalogLong = computed(() => catalog.value.length > 12 || catalogTextLength.value > 360)

/**
 * 鍒ゆ柇鐢ㄦ埛鏄惁宸茶喘涔拌鏁欐潗
 * @returns {boolean} 鏄惁宸茶喘涔?
 */
const hasPurchased = computed(() => Boolean(textbook.value?.purchased))

/**
 * 璁＄畻璐墿杞︽€婚噾棰?
 * @returns {number} 璐墿杞︽€婚噾棰?
 */
const cartTotal = computed(() => cartItems.value.reduce((total, item) => {
  const unitPrice = Number(item.unitPrice)
  const quantityValue = Number(item.quantity)
  return total + (Number.isFinite(unitPrice) ? unitPrice : 0) * (Number.isFinite(quantityValue) ? quantityValue : 1)
}, 0))

/**
 * 鍔犺浇鏁欐潗璇︽儏鏁版嵁
 */
const loadTextbook = async () => {
  loading.value = true
  error.value = ''
  try {
    textbook.value = await fetchAcademyTextbook(props.textbookId)
  } catch (err) {
    error.value = err instanceof Error ? err.message : '教材详情加载失败'
  } finally {
    loading.value = false
  }
}

/**
 * 鍔犺浇璐墿杞﹀拰鍗″埜鏁版嵁
 */
const loadCart = async () => {
  try {
    const [nextCartItems, nextVouchers] = await Promise.all([
      fetchAcademyTextbookCart(),
      fetchUserVouchers().catch(() => []),
    ])
    cartItems.value = nextCartItems
    vouchers.value = nextVouchers
    if (!voucherTouched.value) {
      useTextbookVoucher.value = canUseTextbookVoucher.value
      selectedVoucherKey.value = availableTextbookVouchers.value[0]?.voucherKey || ''
    }
  } catch {
    cartItems.value = []
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
 * 灏嗘暀鏉愬姞鍏ヨ喘鐗╄溅
 */
const addToCart = async () => {
  try {
    await addAcademyTextbookCartItem({
      textbookId: props.textbookId,
      quantity: quantity.value,
    })
    await loadCart()
    showFeedback('已加入购物车')
  } catch (err) {
    showFeedback(err instanceof Error ? err.message : '加入购物车失败')
  }
}

/**
 * 浠庤喘鐗╄溅绉婚櫎鎸囧畾鍟嗗搧
 * @param {Object} item 璐墿杞﹀晢鍝侀」
 */
const removeCartItem = async (item) => {
  try {
    cartItems.value = await removeAcademyTextbookCartItem(item.id)
    showFeedback('已从购物车移除')
  } catch (err) {
    showFeedback(err instanceof Error ? err.message : '移除购物车失败')
  }
}

/**
 * 绔嬪嵆璐拱鏁欐潗
 */
const buyNow = async () => {
  if (hasPurchased.value) {
    showFeedback('你已经购买过这本教材')
    return
  }
  try {
    const order = await createAcademyTextbookOrder({
      textbookId: props.textbookId,
      quantity: quantity.value,
      useVoucher: useTextbookVoucher.value && Boolean(selectedTextbookVoucher.value),
      voucherKey: useTextbookVoucher.value ? selectedTextbookVoucher.value?.voucherKey : '',
    })
    pendingOrder.value = order
    paymentVisible.value = true
    await startPaymentSession('WECHAT')
  } catch (err) {
    showFeedback(err instanceof Error ? err.message : '创建订单失败')
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
    await loadTextbook()
  } catch (err) {
    showFeedback(err instanceof Error ? err.message : '支付失败')
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
  } catch (err) {
    paymentError.value = err instanceof Error ? err.message : '支付会话生成失败'
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
  } catch (err) {
    stopPaymentPolling()
    showFeedback(err instanceof Error ? err.message : '支付状态查询失败')
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
    loadTextbook()
    loadCart()
  } else if (result?.status === 'EXPIRED') {
    stopPaymentPolling()
    showFeedback(result?.message || '支付会话已过期')
  }
}

/**
 * 璺宠浆鍒拌喘鐗╄溅缁撶畻椤甸潰
 */
const goCheckout = () => {
  router.push('/academy/textbook-cart')
}

/**
 * 鍒囨崲绠€浠嬪睍寮€鐘舵€?
 */
const toggleOverview = () => {
  overviewExpanded.value = !overviewExpanded.value
}

/**
 * 鍒囨崲鐩綍灞曞紑鐘舵€?
 */
const toggleCatalog = () => {
  catalogExpanded.value = !catalogExpanded.value
}

/**
 * 鎻愪氦璇勮
 */
const submitReview = async () => {
  if (!hasPurchased.value) {
    showFeedback('购买教材后才能评论')
    return
  }
  if (!reviewForm.value.content.trim()) {
    showFeedback('请先填写评论内容')
    return
  }
  reviewSubmitting.value = true
  try {
    await createAcademyTextbookReview(props.textbookId, {
      userId: 1,
      userName: reviewForm.value.userName,
      rating: reviewForm.value.rating,
      content: reviewForm.value.content.trim(),
    })
    reviewForm.value.content = ''
    await loadTextbook()
    showFeedback('评论发布成功')
  } catch (err) {
    showFeedback(err instanceof Error ? err.message : '评论发布失败')
  } finally {
    reviewSubmitting.value = false
  }
}

/**
 * 灏侀潰鍔犺浇澶辫触鏃剁殑闄嶇骇澶勭悊
 * @param {Event} event 閿欒浜嬩欢
 */
const useCoverFallback = (event) => {
  if (textbook.value?.coverUrl && event.target.src !== textbook.value.coverUrl) {
    event.target.src = textbook.value.coverUrl
  }
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
 * 鏍煎紡鍖栦环鏍兼樉绀烘枃鏈?
 * @param {*} value 浠锋牸鍊?
 * @returns {string} 鏍煎紡鍖栧悗鐨勪环鏍兼枃鏈?
 */
function formatPrice(value) {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) {
    return '暂无'
  }
  return `￥${numberValue.toFixed(2)}`
}

// 鐩戝惉鏁欐潗ID鍙樺寲锛岄噸鏂板姞杞芥暟鎹?
watch(canUseTextbookVoucher, (canUse) => {
  if (!canUse) {
    useTextbookVoucher.value = false
    selectedVoucherKey.value = ''
  } else if (!voucherTouched.value) {
    useTextbookVoucher.value = true
    selectedVoucherKey.value = availableTextbookVouchers.value[0]?.voucherKey || ''
  }
})

watch(() => props.textbookId, () => {
  overviewExpanded.value = false
  catalogExpanded.value = false
  loadTextbook()
  loadCart()
}, { immediate: true })

// 缁勪欢鍗歌浇鍓嶆竻鐞嗗畾鏃跺櫒
onBeforeUnmount(() => {
  window.clearTimeout(feedbackTimer)
  stopPaymentPolling()
})
</script>

<template>
  <!-- 鏁欐潗璇︽儏涓婚〉闈?-->
  <main class="online-course-main textbook-detail-main">
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
      返回教材列表
    </button>

    <!-- 鍔犺浇鐘舵€?-->
    <div v-if="loading" class="academy-state">正在加载教材详情...</div>

    <!-- 閿欒鐘舵€?-->
    <div v-else-if="error" class="academy-state academy-state-error">
      <span>{{ error }}</span>
      <button type="button" @click="loadTextbook">重试</button>
    </div>

    <!-- 鏁欐潗璇︽儏鍐呭 -->
    <template v-else-if="textbook">
      <!-- 鏁欐潗璐拱鍖哄煙 -->
      <section class="textbook-shop-hero" aria-labelledby="textbook-detail-title">
        <!-- 鏁欐潗灏侀潰 -->
        <div class="textbook-shop-cover">
          <img :src="cover" :alt="textbook.name" @error="useCoverFallback" />
        </div>

        <!-- 鏁欐潗淇℃伅 -->
        <div class="textbook-shop-info">
          <p class="textbook-shop-category">{{ textbook.category }} · {{ textbook.publisher }}</p>
          <h1 id="textbook-detail-title">{{ textbook.name }}</h1>
          <p class="textbook-shop-recommendation">{{ textbook.description || textbook.recommendation }}</p>

          <!-- 鏁欐潗鍏冧俊鎭?-->
          <dl class="textbook-shop-meta">
            <div>
              <dt>主编</dt>
              <dd>{{ textbook.editor || '暂无' }}</dd>
            </div>
            <div>
              <dt>ISBN</dt>
              <dd>{{ textbook.isbn || '暂无' }}</dd>
            </div>
            <div>
              <dt>出版单位</dt>
              <dd>{{ textbook.publisher || '暂无' }}</dd>
            </div>
            <div>
              <dt>出版时间</dt>
              <dd>{{ textbook.publishDate || '暂无' }}</dd>
            </div>
            <div>
              <dt>已阅读</dt>
              <dd>{{ textbook.readerCount || 0 }} 人</dd>
            </div>
          </dl>

          <!-- 浠锋牸淇℃伅 -->
          <div class="textbook-shop-price" :class="{ 'is-voucher-price': textbookVoucherDiscount > 0 }">
            <small>{{ purchasePriceLabel }}</small>
            <span>{{ purchaseDisplayPrice }}</span>
            <del v-if="purchaseReferencePrice">{{ purchaseReferencePrice }}</del>
          </div>

          <!-- 浼樻儬鍒搁€夋嫨 -->
          <template v-if="canUseTextbookVoucher">
            <label class="textbook-voucher-toggle textbook-detail-voucher-toggle">
              <input
                v-model="useTextbookVoucher"
                type="checkbox"
                @change="toggleTextbookVoucher"
              />
              <span>使用优惠券</span>
              <strong>{{ selectedTextbookVoucher ? `${selectedTextbookVoucher.name} · 剩余 ${selectedTextbookVoucher.quantity} 张` : '未选择' }}</strong>
            </label>
            <div v-if="useTextbookVoucher" class="textbook-voucher-options textbook-detail-voucher-options">
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
          </template>

          <!-- 璐拱鎿嶄綔 -->
          <div class="textbook-shop-actions">
            <label>
              数量
              <input v-model.number="quantity" type="number" min="1" max="99" />
            </label>
            <button type="button" class="is-primary" :disabled="hasPurchased" @click="buyNow">
              {{ hasPurchased ? '已购买' : '立即购买' }}
            </button>
            <button type="button" @click="addToCart">加入购物车</button>
          </div>

          <!-- 鍘熷鏁欐潗椤甸摼鎺?-->
          <a v-if="textbook.link" class="textbook-shop-source" :href="textbook.link" target="_blank" rel="noreferrer">
            查看原始教材页
          </a>
        </div>
      </section>

      <!-- 鏁欐潗鍐呭鍖哄煙 -->
      <section class="textbook-shop-content">
        <!-- 姒傝鍖哄煙 -->
        <article class="textbook-shop-overview">
          <h2>概览</h2>
          <!-- 绠€浠?-->
          <section>
            <h3>简介</h3>
            <div
              class="textbook-shop-overview-text"
              :class="{ 'is-collapsed': isOverviewLong && !overviewExpanded }"
            >
              <p v-for="paragraph in overviewParagraphs" :key="paragraph">{{ paragraph }}</p>
            </div>
            <button
              v-if="isOverviewLong"
              type="button"
              class="textbook-shop-overview-toggle"
              :class="{ 'is-expanded': overviewExpanded }"
              @click="toggleOverview"
            >
              <el-icon><ArrowDown /></el-icon>
              <span>{{ overviewExpanded ? '点击收起' : '点击展开' }}</span>
            </button>
          </section>
          <!-- 鐩綍 -->
          <section>
            <h3>目录</h3>
            <div
              class="textbook-shop-catalog-wrap"
              :class="{ 'is-collapsed': isCatalogLong && !catalogExpanded }"
            >
              <ol class="textbook-shop-catalog">
                <li v-for="item in catalog" :key="item">{{ item }}</li>
              </ol>
            </div>
            <button
              v-if="isCatalogLong"
              type="button"
              class="textbook-shop-overview-toggle"
              :class="{ 'is-expanded': catalogExpanded }"
              @click="toggleCatalog"
            >
              <el-icon><ArrowDown /></el-icon>
              <span>{{ catalogExpanded ? '点击收起' : '点击展开' }}</span>
            </button>
          </section>
        </article>

        <!-- 璇勮鍖哄煙 -->
        <article>
          <h2>评论</h2>
          <!-- 璇勮琛ㄥ崟 -->
          <form v-if="hasPurchased" class="textbook-review-form" @submit.prevent="submitReview">
            <div>
              <label>
                昵称
                <input v-model.trim="reviewForm.userName" type="text" maxlength="40" />
              </label>
              <label>
                评分
                <select v-model.number="reviewForm.rating">
                  <option :value="5">5 星</option>
                  <option :value="4">4 星</option>
                  <option :value="3">3 星</option>
                  <option :value="2">2 星</option>
                  <option :value="1">1 星</option>
                </select>
              </label>
            </div>
            <textarea
              v-model="reviewForm.content"
              maxlength="800"
              placeholder="写下你对这本教材的阅读体验吧"
            />
            <button type="submit" :disabled="reviewSubmitting">
              {{ reviewSubmitting ? '发布中...' : '发布评论' }}
            </button>
          </form>
          <!-- 鏈喘涔版彁绀?-->
          <p v-else class="textbook-review-locked">购买教材后即可发表评论。</p>
          <!-- 璇勮鍒楄〃 -->
          <div v-if="comments.length" class="textbook-shop-comments">
            <div v-for="comment in comments" :key="`${comment.user}-${comment.content}`">
              <strong>{{ comment.user }}</strong>
              <span>{{ '★'.repeat(comment.rating || 5) }}</span>
              <p>{{ comment.content }}</p>
              <div v-if="comment.replyContent" class="textbook-comment-reply">
                <strong>
                  {{ comment.replyUserName || '管理员' }} 回复
                  <span v-if="comment.replyUserRoleType === 'admin'">管理员</span>
                  <span v-else-if="comment.replyUserRoleType === 'teacher'">教师</span>
                </strong>
                <p>{{ comment.replyContent }}</p>
              </div>
            </div>
          </div>
          <!-- 鏃犺瘎璁烘彁绀?-->
          <p v-else>暂无评论</p>
        </article>
      </section>

      <!-- 璐墿杞﹂潰鏉?-->
      <section class="textbook-cart-panel" aria-label="教材购物车">
        <div>
          <h2>购物车</h2>
          <p>共 {{ cartItems.length }} 本教材 · 合计 {{ formatPrice(cartTotal) }}</p>
          <button type="button" class="textbook-cart-checkout-link" @click="goCheckout">去结算</button>
        </div>

        <!-- 璐墿杞﹀垪琛?-->
        <div v-if="cartItems.length" class="textbook-cart-list">
          <div v-for="item in cartItems" :key="item.id">
            <img :src="resolveResourceUrl(item.cover || item.coverUrl)" :alt="item.name" />
            <div>
              <strong>{{ item.name }}</strong>
              <span>{{ item.publisher }} · x{{ item.quantity }}</span>
            </div>
            <em>{{ formatPrice(Number(item.unitPrice) * Number(item.quantity || 1)) }}</em>
            <button type="button" @click="removeCartItem(item)">移除</button>
          </div>
        </div>

        <!-- 璐墿杞︾┖鐘舵€?-->
        <p v-else class="textbook-cart-empty">购物车还是空的，先挑一本教材吧。</p>
      </section>
    </template>
  </main>
</template>

<style scoped>
.textbook-comment-reply {
  display: grid;
  gap: 6px;
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #eef6ff;
  color: #1e3a8a;
}

.textbook-comment-reply strong {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.textbook-comment-reply span {
  padding: 2px 8px;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 12px;
}

.textbook-comment-reply p {
  margin: 0;
}
</style>

