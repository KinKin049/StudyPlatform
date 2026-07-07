<script setup>
/**
 * 教材详情页面组件
 * 展示教材详细信息，支持加入购物车、立即购买、评论等功能
 */
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import {
  addAcademyTextbookCartItem,
  createAcademyTextbookOrder,
  createAcademyTextbookReview,
  fetchAcademyTextbook,
  fetchAcademyTextbookCart,
  payAcademyTextbookOrder,
  removeAcademyTextbookCartItem,
} from '../../api/academy'
import { resolveResourceUrl } from '../../api/request'
import { fetchUserVouchers, VOUCHER_KEYS } from '../../api/vouchers'

// 组件属性定义
const props = defineProps({
  textbookId: {
    type: String,
    required: true,
  },
})

// 路由实例
const router = useRouter()
// 教材详情数据
const textbook = ref(null)
// 加载状态
const loading = ref(false)
// 错误提示信息
const error = ref('')
// 购买数量
const quantity = ref(1)
// 反馈提示可见性
const feedbackVisible = ref(false)
// 反馈提示消息
const feedbackMessage = ref('')
// 购物车商品列表
const cartItems = ref([])
// 简介展开状态
const overviewExpanded = ref(false)
// 目录展开状态
const catalogExpanded = ref(false)
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
// 评论提交中状态
const reviewSubmitting = ref(false)
// 评论表单数据
const reviewForm = ref({
  userName: '默认用户',
  rating: 5,
  content: '',
})
// 反馈提示定时器
let feedbackTimer = null

/**
 * 解析教材封面图片URL
 * @returns {string} 封面图片URL
 */
const cover = computed(() => resolveResourceUrl(textbook.value?.cover || textbook.value?.coverUrl))

/**
 * 获取教材目录列表
 * @returns {Array} 目录数组
 */
const catalog = computed(() => textbook.value?.catalog?.length ? textbook.value.catalog : ['暂无目录信息'])

/**
 * 获取教材评论列表
 * @returns {Array} 评论数组
 */
const comments = computed(() => textbook.value?.comments?.length ? textbook.value.comments : [])

/**
 * 获取原价显示文本
 * @returns {string} 原价文本
 */
const originalPrice = computed(() => formatPrice(textbook.value?.originalPrice))

/**
 * 获取折扣价显示文本
 * @returns {string} 折扣价文本
 */
const discountPrice = computed(() => formatPrice(textbook.value?.discountPrice))

/**
 * 计算购买小计金额
 * @returns {number} 小计金额
 */
const buySubtotal = computed(() => {
  const unitPrice = Number(textbook.value?.discountPrice ?? 0)
  return (Number.isFinite(unitPrice) ? unitPrice : 0) * Number(quantity.value || 1)
})

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
  useTextbookVoucher.value && textbookVoucherAvailable.value && buySubtotal.value >= 80 ? 15 : 0
))

/**
 * 计算实际应付金额
 * @returns {number} 应付金额
 */
const buyPayable = computed(() => Math.max(buySubtotal.value - textbookVoucherDiscount.value, 0))

/**
 * 获取简介段落列表
 * @returns {Array} 段落数组
 */
const overviewParagraphs = computed(() => {
  const overview = textbook.value?.overview || textbook.value?.description || '暂无简介'
  return overview.split(/\n+/).map((line) => line.trim()).filter(Boolean)
})

/**
 * 计算简介文本总长度
 * @returns {number} 文本长度
 */
const overviewTextLength = computed(() => overviewParagraphs.value.join('').length)

/**
 * 判断简介是否需要展开
 * @returns {boolean} 是否需要展开
 */
const isOverviewLong = computed(() => overviewParagraphs.value.length > 2 || overviewTextLength.value > 260)

/**
 * 计算目录文本总长度
 * @returns {number} 文本长度
 */
const catalogTextLength = computed(() => catalog.value.join('').length)

/**
 * 判断目录是否需要展开
 * @returns {boolean} 是否需要展开
 */
const isCatalogLong = computed(() => catalog.value.length > 12 || catalogTextLength.value > 360)

/**
 * 判断用户是否已购买该教材
 * @returns {boolean} 是否已购买
 */
const hasPurchased = computed(() => Boolean(textbook.value?.purchased))

/**
 * 计算购物车总金额
 * @returns {number} 购物车总金额
 */
const cartTotal = computed(() => cartItems.value.reduce((total, item) => {
  const unitPrice = Number(item.unitPrice)
  const quantityValue = Number(item.quantity)
  return total + (Number.isFinite(unitPrice) ? unitPrice : 0) * (Number.isFinite(quantityValue) ? quantityValue : 1)
}, 0))

/**
 * 加载教材详情数据
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
 * 加载购物车和卡券数据
 */
const loadCart = async () => {
  try {
    const [nextCartItems, nextVouchers] = await Promise.all([
      fetchAcademyTextbookCart(1),
      fetchUserVouchers().catch(() => []),
    ])
    cartItems.value = nextCartItems
    vouchers.value = nextVouchers
    if (!voucherTouched.value) {
      useTextbookVoucher.value = Number(nextVouchers.find((item) => item.voucherKey === VOUCHER_KEYS.TEXTBOOK_80_15)?.quantity ?? 0) > 0
    }
  } catch {
    cartItems.value = []
  }
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
 * 将教材加入购物车
 */
const addToCart = async () => {
  try {
    await addAcademyTextbookCartItem({
      textbookId: props.textbookId,
      quantity: quantity.value,
      userId: 1,
    })
    await loadCart()
    showFeedback('已加入购物车')
  } catch (err) {
    showFeedback(err instanceof Error ? err.message : '加入购物车失败')
  }
}

/**
 * 从购物车移除指定商品
 * @param {Object} item 购物车商品项
 */
const removeCartItem = async (item) => {
  try {
    cartItems.value = await removeAcademyTextbookCartItem(item.id, 1)
    showFeedback('已从购物车移除')
  } catch (err) {
    showFeedback(err instanceof Error ? err.message : '移除购物车失败')
  }
}

/**
 * 立即购买教材
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
      userId: 1,
      useVoucher: useTextbookVoucher.value && textbookVoucherAvailable.value,
      voucherKey: VOUCHER_KEYS.TEXTBOOK_80_15,
    })
    pendingOrder.value = order
    paymentVisible.value = true
  } catch (err) {
    showFeedback(err instanceof Error ? err.message : '创建订单失败')
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
    await loadTextbook()
    showFeedback(result?.message || '支付成功')
  } catch (err) {
    showFeedback(err instanceof Error ? err.message : '支付失败')
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
 * 跳转到购物车结算页面
 */
const goCheckout = () => {
  router.push('/academy/textbook-cart')
}

/**
 * 切换简介展开状态
 */
const toggleOverview = () => {
  overviewExpanded.value = !overviewExpanded.value
}

/**
 * 切换目录展开状态
 */
const toggleCatalog = () => {
  catalogExpanded.value = !catalogExpanded.value
}

/**
 * 提交评论
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
 * 封面加载失败时的降级处理
 * @param {Event} event 错误事件
 */
const useCoverFallback = (event) => {
  if (textbook.value?.coverUrl && event.target.src !== textbook.value.coverUrl) {
    event.target.src = textbook.value.coverUrl
  }
}

/**
 * 格式化价格显示文本
 * @param {*} value 价格值
 * @returns {string} 格式化后的价格文本
 */
function formatPrice(value) {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) {
    return '暂无'
  }
  return `￥${numberValue.toFixed(2)}`
}

// 监听教材ID变化，重新加载数据
watch(() => props.textbookId, () => {
  overviewExpanded.value = false
  catalogExpanded.value = false
  loadTextbook()
  loadCart()
}, { immediate: true })

// 组件卸载前清理定时器
onBeforeUnmount(() => {
  window.clearTimeout(feedbackTimer)
})
</script>

<template>
  <!-- 教材详情主页面 -->
  <main class="online-course-main textbook-detail-main">
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
          <h2>确认购买教材</h2>
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
      返回教材列表
    </button>

    <!-- 加载状态 -->
    <div v-if="loading" class="academy-state">正在加载教材详情...</div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="academy-state academy-state-error">
      <span>{{ error }}</span>
      <button type="button" @click="loadTextbook">重试</button>
    </div>

    <!-- 教材详情内容 -->
    <template v-else-if="textbook">
      <!-- 教材购买区域 -->
      <section class="textbook-shop-hero" aria-labelledby="textbook-detail-title">
        <!-- 教材封面 -->
        <div class="textbook-shop-cover">
          <img :src="cover" :alt="textbook.name" @error="useCoverFallback" />
        </div>

        <!-- 教材信息 -->
        <div class="textbook-shop-info">
          <p class="textbook-shop-category">{{ textbook.category }} · {{ textbook.publisher }}</p>
          <h1 id="textbook-detail-title">{{ textbook.name }}</h1>
          <p class="textbook-shop-recommendation">{{ textbook.description || textbook.recommendation }}</p>

          <!-- 教材元信息 -->
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

          <!-- 价格信息 -->
          <div class="textbook-shop-price">
            <span>{{ discountPrice }}</span>
            <del>{{ originalPrice }}</del>
          </div>

          <!-- 优惠券选择 -->
          <label class="textbook-voucher-toggle textbook-detail-voucher-toggle">
            <input
              v-model="useTextbookVoucher"
              type="checkbox"
              :disabled="!textbookVoucherAvailable || buySubtotal < 80"
              @change="voucherTouched = true"
            />
            <span>使用教材优惠券</span>
            <strong>
              {{ textbookVoucherAvailable ? `满 80 减 15 · 剩余 ${textbookVoucher.quantity} 张` : '暂无可用券' }}
            </strong>
          </label>

          <!-- 用券后金额 -->
          <p v-if="textbookVoucherDiscount > 0" class="textbook-detail-payable">
            用券后预计支付 {{ formatPrice(buyPayable) }}
          </p>

          <!-- 购买操作 -->
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

          <!-- 原始教材页链接 -->
          <a v-if="textbook.link" class="textbook-shop-source" :href="textbook.link" target="_blank" rel="noreferrer">
            查看原始教材页
          </a>
        </div>
      </section>

      <!-- 教材内容区域 -->
      <section class="textbook-shop-content">
        <!-- 概览区域 -->
        <article class="textbook-shop-overview">
          <h2>概览</h2>
          <!-- 简介 -->
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
          <!-- 目录 -->
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

        <!-- 评论区域 -->
        <article>
          <h2>评论</h2>
          <!-- 评论表单 -->
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
          <!-- 未购买提示 -->
          <p v-else class="textbook-review-locked">购买教材后即可发表评论。</p>
          <!-- 评论列表 -->
          <div v-if="comments.length" class="textbook-shop-comments">
            <div v-for="comment in comments" :key="`${comment.user}-${comment.content}`">
              <strong>{{ comment.user }}</strong>
              <span>{{ '★'.repeat(comment.rating || 5) }}</span>
              <p>{{ comment.content }}</p>
            </div>
          </div>
          <!-- 无评论提示 -->
          <p v-else>暂无评论</p>
        </article>
      </section>

      <!-- 购物车面板 -->
      <section class="textbook-cart-panel" aria-label="教材购物车">
        <div>
          <h2>购物车</h2>
          <p>共 {{ cartItems.length }} 本教材 · 合计 {{ formatPrice(cartTotal) }}</p>
          <button type="button" class="textbook-cart-checkout-link" @click="goCheckout">去结算</button>
        </div>

        <!-- 购物车列表 -->
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

        <!-- 购物车空状态 -->
        <p v-else class="textbook-cart-empty">购物车还是空的，先挑一本教材吧。</p>
      </section>
    </template>
  </main>
</template>
