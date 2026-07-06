<script setup>
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

const router = useRouter()
const cartItems = ref([])
const selectedIds = ref([])
const loading = ref(false)
const submitting = ref(false)
const feedbackVisible = ref(false)
const feedbackMessage = ref('')
const pendingOrder = ref(null)
const paymentVisible = ref(false)
const paying = ref(false)
let feedbackTimer = null

const selectedItems = computed(() => cartItems.value.filter((item) => selectedIds.value.includes(item.id)))
const selectedCount = computed(() => selectedItems.value.reduce((total, item) => total + Number(item.quantity || 1), 0))
const subtotal = computed(() => selectedItems.value.reduce((total, item) => total + itemTotal(item), 0))
const discount = computed(() => subtotal.value >= 150 ? 18 : subtotal.value >= 80 ? 8 : 0)
const freight = computed(() => (subtotal.value > 0 && subtotal.value < 99 ? 6 : 0))
const payable = computed(() => Math.max(subtotal.value - discount.value + freight.value, 0))
const allSelected = computed(() => cartItems.value.length > 0 && selectedIds.value.length === cartItems.value.length)

const loadCart = async () => {
  loading.value = true
  try {
    cartItems.value = await fetchAcademyTextbookCart(1)
    selectedIds.value = cartItems.value.map((item) => item.id)
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '购物车加载失败')
  } finally {
    loading.value = false
  }
}

const toggleAll = () => {
  selectedIds.value = allSelected.value ? [] : cartItems.value.map((item) => item.id)
}

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

const removeItem = async (item) => {
  try {
    cartItems.value = await removeAcademyTextbookCartItem(item.id, 1)
    selectedIds.value = selectedIds.value.filter((id) => id !== item.id)
    showFeedback('已移除教材')
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '移除失败')
  }
}

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

const confirmPayment = async () => {
  if (!pendingOrder.value?.orderNo) return
  paying.value = true
  try {
    const result = await payAcademyTextbookOrder(pendingOrder.value.orderNo, 1)
    paymentVisible.value = false
    pendingOrder.value = null
    showFeedback(result?.message || '支付成功')
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '支付失败')
  } finally {
    paying.value = false
  }
}

const cancelPayment = () => {
  paymentVisible.value = false
  pendingOrder.value = null
}

const showFeedback = (message) => {
  feedbackMessage.value = message
  feedbackVisible.value = true
  window.clearTimeout(feedbackTimer)
  feedbackTimer = window.setTimeout(() => {
    feedbackVisible.value = false
  }, 1800)
}

const itemTotal = (item) => {
  const unitPrice = Number(item.unitPrice)
  const quantity = Number(item.quantity || 1)
  return (Number.isFinite(unitPrice) ? unitPrice : 0) * (Number.isFinite(quantity) ? quantity : 1)
}

const formatPrice = (value) => `￥${Number(value || 0).toFixed(2)}`

onMounted(loadCart)

onBeforeUnmount(() => {
  window.clearTimeout(feedbackTimer)
})
</script>

<template>
  <main class="online-course-main textbook-cart-main">
    <Transition name="academy-assignment-feedback">
      <div v-if="feedbackVisible" class="academy-assignment-feedback-toast" role="status">
        {{ feedbackMessage }}
      </div>
    </Transition>

    <Transition name="academy-assignment-feedback">
      <div v-if="paymentVisible" class="textbook-payment-mask">
        <section class="textbook-payment-dialog" role="dialog" aria-modal="true" aria-label="确认支付">
          <span>模拟支付</span>
          <h2>确认支付订单</h2>
          <p>订单号：{{ pendingOrder?.orderNo }}</p>
          <strong>{{ formatPrice(pendingOrder?.totalAmount) }}</strong>
          <div>
            <button type="button" @click="cancelPayment">稍后支付</button>
            <button type="button" class="is-primary" :disabled="paying" @click="confirmPayment">
              {{ paying ? '支付中...' : '确认支付' }}
            </button>
          </div>
        </section>
      </div>
    </Transition>

    <button type="button" class="textbook-detail-back" @click="router.push('/academy/textbooks')">
      继续挑选教材
    </button>

    <section class="textbook-cart-checkout-hero">
      <div>
        <span>Textbook Cart</span>
        <h1>教材购物车</h1>
        <p>核对教材、配送信息和优惠后即可提交订单。</p>
      </div>
      <div class="textbook-cart-checkout-stat">
        <strong>{{ selectedCount }}</strong>
        <span>本待结算</span>
      </div>
    </section>

    <div v-if="loading" class="academy-state">正在加载购物车...</div>
    <section v-else class="textbook-cart-checkout-layout">
      <div class="textbook-cart-checkout-list">
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

        <div v-if="cartItems.length" class="textbook-cart-checkout-items">
          <article v-for="item in cartItems" :key="item.id" class="textbook-cart-checkout-item">
            <label class="textbook-cart-checkout-select">
              <input v-model="selectedIds" type="checkbox" :value="item.id" />
            </label>
            <button type="button" class="textbook-cart-checkout-book" @click="router.push(`/academy/textbooks/${item.textbookId}`)">
              <img :src="resolveResourceUrl(item.cover || item.coverUrl)" :alt="item.name" />
              <span>
                <strong>{{ item.name }}</strong>
                <em>{{ item.publisher }} · {{ item.editor || '暂无主编' }}</em>
              </span>
            </button>
            <span class="textbook-cart-checkout-price">{{ formatPrice(item.unitPrice) }}</span>
            <div class="textbook-cart-checkout-quantity">
              <button type="button" @click="changeQuantity(item, -1)">−</button>
              <span>{{ item.quantity }}</span>
              <button type="button" @click="changeQuantity(item, 1)">+</button>
            </div>
            <strong class="textbook-cart-checkout-total">{{ formatPrice(itemTotal(item)) }}</strong>
            <button type="button" class="textbook-cart-checkout-remove" @click="removeItem(item)">删除</button>
          </article>
        </div>

        <div v-else class="textbook-cart-checkout-empty">
          <strong>购物车还是空的</strong>
          <p>去教材列表挑一本适合当前课程的教材吧。</p>
          <button type="button" @click="router.push('/academy/textbooks')">去逛逛</button>
        </div>
      </div>

      <aside class="textbook-cart-checkout-side">
        <section>
          <h2>收货信息</h2>
          <p>默认地址：校内驿站 A 区 03 柜</p>
          <p>联系人：默认用户 · 138****2026</p>
          <button type="button">更换地址</button>
        </section>

        <section>
          <h2>优惠与配送</h2>
          <div><span>满减优惠</span><strong>-{{ formatPrice(discount).slice(1) }}</strong></div>
          <div><span>配送费</span><strong>{{ freight === 0 ? '包邮' : formatPrice(freight) }}</strong></div>
          <p>满 99 元包邮，满 80 减 8，满 150 减 18。</p>
        </section>

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
