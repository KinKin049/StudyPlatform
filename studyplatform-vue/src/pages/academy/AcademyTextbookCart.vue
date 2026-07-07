<script setup>
/**
 * 教材购物车结算页面组件
 * 展示购物车商品列表，支持数量调整、商品删除、优惠券使用和订单提交
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  createAcademyTextbookOrder,
  fetchAcademyTextbookCart,
  payAcademyTextbookOrder,
  removeAcademyTextbookCartItem,
  updateAcademyTextbookCartItem,
} from '../../api/academy'
import { resolveResourceUrl } from '../../api/request'
import { fetchUserVouchers, VOUCHER_KEYS } from '../../api/vouchers'

// 路由实例
const router = useRouter()
// 购物车商品列表
const cartItems = ref([])
// 选中的商品ID列表
const selectedIds = ref([])
// 加载状态
const loading = ref(false)
// 提交订单中状态
const submitting = ref(false)
// 反馈提示可见性
const feedbackVisible = ref(false)
// 反馈提示消息
const feedbackMessage = ref('')
// 待支付订单
const pendingOrder = ref(null)
// 支付弹窗可见性
const paymentVisible = ref(false)
// 支付中状态
const paying = ref(false)
// 用户卡券列表
const vouchers = ref([])
// 是否使用教材优惠券
const useTextbookVoucher = ref(true)
// 优惠券是否已被用户操作过
const voucherTouched = ref(false)
// 反馈提示定时器
let feedbackTimer = null

/**
 * 获取选中的商品列表
 * @returns {Array} 选中的商品数组
 */
const selectedItems = computed(() => cartItems.value.filter((item) => selectedIds.value.includes(item.id)))

/**
 * 计算选中商品的总数量
 * @returns {number} 总数量
 */
const selectedCount = computed(() => selectedItems.value.reduce((total, item) => total + Number(item.quantity || 1), 0))

/**
 * 计算选中商品的小计金额
 * @returns {number} 小计金额
 */
const subtotal = computed(() => selectedItems.value.reduce((total, item) => total + itemTotal(item), 0))

/**
 * 计算满减优惠金额
 * @returns {number} 满减金额
 */
const discount = computed(() => subtotal.value >= 150 ? 18 : subtotal.value >= 80 ? 8 : 0)

/**
 * 获取教材优惠券信息
 * @returns {Object|null} 优惠券对象
 */
const textbookVoucher = computed(() => vouchers.value.find((item) => item.voucherKey === VOUCHER_KEYS.TEXTBOOK_80_15))

/**
 * 判断教材优惠券是否可用
 * @returns {boolean} 是否可用
 */
const textbookVoucherAvailable = computed(() => Number(textbookVoucher.value?.quantity ?? 0) > 0)

/**
 * 计算教材优惠券抵扣金额
 * @returns {number} 抵扣金额
 */
const textbookVoucherDiscount = computed(() => (
  useTextbookVoucher.value && textbookVoucherAvailable.value && subtotal.value >= 80 ? 15 : 0
))

/**
 * 计算总优惠金额（满减 + 优惠券）
 * @returns {number} 总优惠金额
 */
const totalDiscount = computed(() => discount.value + textbookVoucherDiscount.value)

/**
 * 计算配送费
 * @returns {number} 配送费金额
 */
const freight = computed(() => (subtotal.value > 0 && subtotal.value < 99 ? 6 : 0))

/**
 * 计算实际应付金额
 * @returns {number} 应付金额
 */
const payable = computed(() => Math.max(subtotal.value - totalDiscount.value + freight.value, 0))

/**
 * 判断是否全选
 * @returns {boolean} 是否全选
 */
const allSelected = computed(() => cartItems.value.length > 0 && selectedIds.value.length === cartItems.value.length)

/**
 * 加载购物车和卡券数据
 */
const loadCart = async () => {
  loading.value = true
  try {
    const [nextCartItems, nextVouchers] = await Promise.all([
      fetchAcademyTextbookCart(1),
      fetchUserVouchers().catch(() => []),
    ])
    cartItems.value = nextCartItems
    vouchers.value = nextVouchers
    selectedIds.value = cartItems.value.map((item) => item.id)
    if (!voucherTouched.value) {
      useTextbookVoucher.value = Number(nextVouchers.find((item) => item.voucherKey === VOUCHER_KEYS.TEXTBOOK_80_15)?.quantity ?? 0) > 0
    }
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '购物车加载失败')
  } finally {
    loading.value = false
  }
}

/**
 * 切换全选状态
 */
const toggleAll = () => {
  selectedIds.value = allSelected.value ? [] : cartItems.value.map((item) => item.id)
}

/**
 * 修改商品数量
 * @param {Object} item 购物车商品项
 * @param {number} delta 数量变化值（+1或-1）
 */
const changeQuantity = async (item, delta) => {
  const nextQuantity = Math.min(Math.max(Number(item.quantity || 1) + delta, 1), 99)
  if (nextQuantity === Number(item.quantity)) return
  try {
    cartItems.value = await updateAcademyTextbookCartItem(item.id, {
      userId: 1,
      quantity: nextQuantity,
    })
    selectedIds.value = selectedIds.value.filter((id) => cartItems.value.some((cartItem) => cartItem.id === id))
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '数量更新失败')
  }
}

/**
 * 删除购物车商品
 * @param {Object} item 购物车商品项
 */
const removeItem = async (item) => {
  try {
    cartItems.value = await removeAcademyTextbookCartItem(item.id, 1)
    selectedIds.value = selectedIds.value.filter((id) => id !== item.id)
    showFeedback('已移除教材')
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '移除失败')
  }
}

/**
 * 提交订单
 */
const submitOrder = async () => {
  if (!selectedIds.value.length) {
    showFeedback('请先选择要结算的教材')
    return
  }
  submitting.value = true
  try {
    const order = await createAcademyTextbookOrder({
      userId: 1,
      cartItemIds: selectedIds.value,
      useVoucher: useTextbookVoucher.value && textbookVoucherAvailable.value,
      voucherKey: VOUCHER_KEYS.TEXTBOOK_80_15,
    })
    pendingOrder.value = order
    paymentVisible.value = true
    await loadCart()
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '结算失败')
  } finally {
    submitting.value = false
  }
}

/**
 * 确认支付订单
 */
const confirmPayment = async () => {
  if (!pendingOrder.value?.orderNo) return
  paying.value = true
  try {
    const result = await payAcademyTextbookOrder(pendingOrder.value.orderNo, 1)
    paymentVisible.value = false
    pendingOrder.value = null
    vouchers.value = await fetchUserVouchers().catch(() => [])
    showFeedback(result?.message || '支付成功')
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '支付失败')
  } finally {
    paying.value = false
  }
}

/**
 * 取消支付
 */
const cancelPayment = () => {
  paymentVisible.value = false
  pendingOrder.value = null
}

/**
 * 显示反馈提示消息
 * @param {string} message 提示消息
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
 * 计算单个商品的金额
 * @param {Object} item 购物车商品项
 * @returns {number} 商品金额
 */
const itemTotal = (item) => {
  const unitPrice = Number(item.unitPrice)
  const quantity = Number(item.quantity || 1)
  return (Number.isFinite(unitPrice) ? unitPrice : 0) * (Number.isFinite(quantity) ? quantity : 1)
}

/**
 * 格式化价格显示文本
 * @param {*} value 价格值
 * @returns {string} 格式化后的价格文本
 */
const formatPrice = (value) => `￥${Number(value || 0).toFixed(2)}`

// 页面挂载时加载购物车数据
onMounted(loadCart)

// 组件卸载前清理定时器
onBeforeUnmount(() => {
  window.clearTimeout(feedbackTimer)
})
</script>

<template>
  <!-- 教材购物车主页面 -->
  <main class="online-course-main textbook-cart-main">
    <!-- 反馈提示消息 -->
    <Transition name="academy-assignment-feedback">
      <div v-if="feedbackVisible" class="academy-assignment-feedback-toast" role="status">
        {{ feedbackMessage }}
      </div>
    </Transition>

    <!-- 支付弹窗 -->
    <Transition name="academy-assignment-feedback">
      <div v-if="paymentVisible" class="textbook-payment-mask">
        <section class="textbook-payment-dialog" role="dialog" aria-modal="true" aria-label="确认支付">
          <span>模拟支付</span>
          <h2>确认支付订单</h2>
          <p>订单号：{{ pendingOrder?.orderNo }}</p>
          <strong>{{ formatPrice(pendingOrder?.totalAmount) }}</strong>
          <p v-if="pendingOrder?.discountAmount > 0">
            已使用 {{ pendingOrder?.voucherName }}，优惠 {{ formatPrice(pendingOrder?.discountAmount) }}
          </p>
          <div>
            <button type="button" @click="cancelPayment">稍后支付</button>
            <button type="button" class="is-primary" :disabled="paying" @click="confirmPayment">
              {{ paying ? '支付中...' : '确认支付' }}
            </button>
          </div>
        </section>
      </div>
    </Transition>

    <!-- 返回教材列表按钮 -->
    <button type="button" class="textbook-detail-back" @click="router.push('/academy/textbooks')">
      继续挑选教材
    </button>

    <!-- 页面标题区域 -->
    <section class="textbook-cart-checkout-hero">
      <div>
        <span>Textbook Cart</span>
        <h1>教材购物车</h1>
        <p>核对教材、配送信息和优惠后即可提交订单。</p>
      </div>
      <!-- 待结算数量 -->
      <div class="textbook-cart-checkout-stat">
        <strong>{{ selectedCount }}</strong>
        <span>本待结算</span>
      </div>
    </section>

    <!-- 加载状态 -->
    <div v-if="loading" class="academy-state">正在加载购物车...</div>

    <!-- 结算布局 -->
    <section v-else class="textbook-cart-checkout-layout">
      <!-- 购物车商品列表 -->
      <div class="textbook-cart-checkout-list">
        <!-- 列表表头 -->
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

        <!-- 商品列表 -->
        <div v-if="cartItems.length" class="textbook-cart-checkout-items">
          <article v-for="item in cartItems" :key="item.id" class="textbook-cart-checkout-item">
            <!-- 选择框 -->
            <label class="textbook-cart-checkout-select">
              <input v-model="selectedIds" type="checkbox" :value="item.id" />
            </label>
            <!-- 商品信息 -->
            <button type="button" class="textbook-cart-checkout-book" @click="router.push(`/academy/textbooks/${item.textbookId}`)">
              <img :src="resolveResourceUrl(item.cover || item.coverUrl)" :alt="item.name" />
              <span>
                <strong>{{ item.name }}</strong>
                <em>{{ item.publisher }} · {{ item.editor || '暂无主编' }}</em>
              </span>
            </button>
            <!-- 单价 -->
            <span class="textbook-cart-checkout-price">{{ formatPrice(item.unitPrice) }}</span>
            <!-- 数量调整 -->
            <div class="textbook-cart-checkout-quantity">
              <button type="button" @click="changeQuantity(item, -1)">−</button>
              <span>{{ item.quantity }}</span>
              <button type="button" @click="changeQuantity(item, 1)">+</button>
            </div>
            <!-- 小计 -->
            <strong class="textbook-cart-checkout-total">{{ formatPrice(itemTotal(item)) }}</strong>
            <!-- 删除按钮 -->
            <button type="button" class="textbook-cart-checkout-remove" @click="removeItem(item)">删除</button>
          </article>
        </div>

        <!-- 空购物车状态 -->
        <div v-else class="textbook-cart-checkout-empty">
          <strong>购物车还是空的</strong>
          <p>去教材列表挑一本适合当前课程的教材吧。</p>
          <button type="button" @click="router.push('/academy/textbooks')">去逛逛</button>
        </div>
      </div>

      <!-- 右侧结算面板 -->
      <aside class="textbook-cart-checkout-side">
        <!-- 收货信息 -->
        <section>
          <h2>收货信息</h2>
          <p>默认地址：校内驿站 A 区 03 柜</p>
          <p>联系人：默认用户 · 138****2026</p>
          <button type="button">更换地址</button>
        </section>

        <!-- 优惠与配送 -->
        <section>
          <h2>优惠与配送</h2>
          <div><span>满减优惠</span><strong>-{{ formatPrice(discount).slice(1) }}</strong></div>
          <!-- 优惠券选择 -->
          <label class="textbook-voucher-toggle">
            <input
              v-model="useTextbookVoucher"
              type="checkbox"
              :disabled="!textbookVoucherAvailable || subtotal < 80"
              @change="voucherTouched = true"
            />
            <span>使用教材优惠券</span>
            <strong>
              {{ textbookVoucherAvailable ? `满 80 减 15 · 剩余 ${textbookVoucher.quantity} 张` : '暂无可用券' }}
            </strong>
          </label>
          <div><span>优惠券抵扣</span><strong>-{{ formatPrice(textbookVoucherDiscount).slice(1) }}</strong></div>
          <div><span>配送费</span><strong>{{ freight === 0 ? '包邮' : formatPrice(freight) }}</strong></div>
          <p>满 99 元包邮，平台满减与教材优惠券可同时使用；优惠券默认使用，也可以取消勾选。</p>
        </section>

        <!-- 订单结算 -->
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
