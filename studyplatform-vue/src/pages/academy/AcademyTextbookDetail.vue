<script setup>
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

const props = defineProps({
  textbookId: {
    type: String,
    required: true,
  },
})

const router = useRouter()
const textbook = ref(null)
const loading = ref(false)
const error = ref('')
const quantity = ref(1)
const feedbackVisible = ref(false)
const feedbackMessage = ref('')
const cartItems = ref([])
const overviewExpanded = ref(false)
const catalogExpanded = ref(false)
const pendingOrder = ref(null)
const paymentVisible = ref(false)
const paying = ref(false)
const reviewSubmitting = ref(false)
const reviewForm = ref({
  userName: '默认用户',
  rating: 5,
  content: '',
})
let feedbackTimer = null

const cover = computed(() => resolveResourceUrl(textbook.value?.cover || textbook.value?.coverUrl))
const catalog = computed(() => textbook.value?.catalog?.length ? textbook.value.catalog : ['暂无目录信息'])
const comments = computed(() => textbook.value?.comments?.length ? textbook.value.comments : [])
const originalPrice = computed(() => formatPrice(textbook.value?.originalPrice))
const discountPrice = computed(() => formatPrice(textbook.value?.discountPrice))
const overviewParagraphs = computed(() => {
  const overview = textbook.value?.overview || textbook.value?.description || '暂无简介'
  return overview.split(/\n+/).map((line) => line.trim()).filter(Boolean)
})
const overviewTextLength = computed(() => overviewParagraphs.value.join('').length)
const isOverviewLong = computed(() => overviewParagraphs.value.length > 2 || overviewTextLength.value > 260)
const catalogTextLength = computed(() => catalog.value.join('').length)
const isCatalogLong = computed(() => catalog.value.length > 12 || catalogTextLength.value > 360)
const hasPurchased = computed(() => Boolean(textbook.value?.purchased))
const cartTotal = computed(() => cartItems.value.reduce((total, item) => {
  const unitPrice = Number(item.unitPrice)
  const quantityValue = Number(item.quantity)
  return total + (Number.isFinite(unitPrice) ? unitPrice : 0) * (Number.isFinite(quantityValue) ? quantityValue : 1)
}, 0))

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

const loadCart = async () => {
  try {
    cartItems.value = await fetchAcademyTextbookCart(1)
  } catch {
    cartItems.value = []
  }
}

const showFeedback = (message) => {
  feedbackMessage.value = message
  feedbackVisible.value = true
  window.clearTimeout(feedbackTimer)
  feedbackTimer = window.setTimeout(() => {
    feedbackVisible.value = false
  }, 1800)
}

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

const removeCartItem = async (item) => {
  try {
    cartItems.value = await removeAcademyTextbookCartItem(item.id, 1)
    showFeedback('已从购物车移除')
  } catch (err) {
    showFeedback(err instanceof Error ? err.message : '移除购物车失败')
  }
}

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
    })
    pendingOrder.value = order
    paymentVisible.value = true
  } catch (err) {
    showFeedback(err instanceof Error ? err.message : '创建订单失败')
  }
}

const confirmPayment = async () => {
  if (!pendingOrder.value?.orderNo) return
  paying.value = true
  try {
    const result = await payAcademyTextbookOrder(pendingOrder.value.orderNo, 1)
    paymentVisible.value = false
    pendingOrder.value = null
    await loadTextbook()
    showFeedback(result?.message || '支付成功')
  } catch (err) {
    showFeedback(err instanceof Error ? err.message : '支付失败')
  } finally {
    paying.value = false
  }
}

const cancelPayment = () => {
  paymentVisible.value = false
  pendingOrder.value = null
}

const goCheckout = () => {
  router.push('/academy/textbook-cart')
}

const toggleOverview = () => {
  overviewExpanded.value = !overviewExpanded.value
}

const toggleCatalog = () => {
  catalogExpanded.value = !catalogExpanded.value
}

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

const useCoverFallback = (event) => {
  if (textbook.value?.coverUrl && event.target.src !== textbook.value.coverUrl) {
    event.target.src = textbook.value.coverUrl
  }
}

function formatPrice(value) {
  const numberValue = Number(value)
  if (!Number.isFinite(numberValue)) {
    return '暂无'
  }
  return `￥${numberValue.toFixed(2)}`
}

watch(() => props.textbookId, () => {
  overviewExpanded.value = false
  catalogExpanded.value = false
  loadTextbook()
  loadCart()
}, { immediate: true })

onBeforeUnmount(() => {
  window.clearTimeout(feedbackTimer)
})
</script>

<template>
  <main class="online-course-main textbook-detail-main">
    <Transition name="academy-assignment-feedback">
      <div v-if="feedbackVisible" class="academy-assignment-feedback-toast" role="status">
        {{ feedbackMessage }}
      </div>
    </Transition>

    <Transition name="academy-assignment-feedback">
      <div v-if="paymentVisible" class="textbook-payment-mask">
        <section class="textbook-payment-dialog" role="dialog" aria-modal="true" aria-label="确认支付">
          <span>模拟支付</span>
          <h2>确认购买教材</h2>
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
      返回教材列表
    </button>

    <div v-if="loading" class="academy-state">正在加载教材详情...</div>
    <div v-else-if="error" class="academy-state academy-state-error">
      <span>{{ error }}</span>
      <button type="button" @click="loadTextbook">重试</button>
    </div>

    <template v-else-if="textbook">
      <section class="textbook-shop-hero" aria-labelledby="textbook-detail-title">
        <div class="textbook-shop-cover">
          <img :src="cover" :alt="textbook.name" @error="useCoverFallback" />
        </div>

        <div class="textbook-shop-info">
          <p class="textbook-shop-category">{{ textbook.category }} · {{ textbook.publisher }}</p>
          <h1 id="textbook-detail-title">{{ textbook.name }}</h1>
          <p class="textbook-shop-recommendation">{{ textbook.description || textbook.recommendation }}</p>

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

          <div class="textbook-shop-price">
            <span>{{ discountPrice }}</span>
            <del>{{ originalPrice }}</del>
          </div>

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

          <a v-if="textbook.link" class="textbook-shop-source" :href="textbook.link" target="_blank" rel="noreferrer">
            查看原始教材页
          </a>
        </div>
      </section>

      <section class="textbook-shop-content">
        <article class="textbook-shop-overview">
          <h2>概览</h2>
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

        <article>
          <h2>评论</h2>
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
          <p v-else class="textbook-review-locked">购买教材后即可发表评论。</p>
          <div v-if="comments.length" class="textbook-shop-comments">
            <div v-for="comment in comments" :key="`${comment.user}-${comment.content}`">
              <strong>{{ comment.user }}</strong>
              <span>{{ '★'.repeat(comment.rating || 5) }}</span>
              <p>{{ comment.content }}</p>
            </div>
          </div>
          <p v-else>暂无评论</p>
        </article>
      </section>

      <section class="textbook-cart-panel" aria-label="教材购物车">
        <div>
          <h2>购物车</h2>
          <p>共 {{ cartItems.length }} 本教材 · 合计 {{ formatPrice(cartTotal) }}</p>
          <button type="button" class="textbook-cart-checkout-link" @click="goCheckout">去结算</button>
        </div>

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

        <p v-else class="textbook-cart-empty">购物车还是空的，先挑一本教材吧。</p>
      </section>
    </template>
  </main>
</template>
