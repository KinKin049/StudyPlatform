/**
 * 用户卡券组合式函数
 * 提供卡券列表的加载、查询、使用等功能
 */
import { computed, onMounted, ref } from 'vue'
import { fetchUserVouchers, useVoucher } from '../api/vouchers'

/**
 * 创建用户卡券管理对象
 * @returns {Object} 卡券管理对象
 */
export function useUserVouchers() {
  /** 卡券列表 */
  const vouchers = ref([])
  /** 加载状态 */
  const loading = ref(false)
  /** 错误信息 */
  const errorMessage = ref('')

  /**
   * 卡券数量映射
   * 以卡券标识为键，数量为值的 Map 对象
   */
  const quantityMap = computed(() => {
    const map = new Map()
    vouchers.value.forEach((item) => {
      map.set(item.voucherKey, Number(item.quantity ?? 0))
    })
    return map
  })

  /**
   * 刷新卡券列表
   * 请求最新的用户卡券数据
   */
  async function refreshVouchers() {
    loading.value = true
    errorMessage.value = ''
    try {
      vouchers.value = await fetchUserVouchers()
    } catch (error) {
      errorMessage.value = error?.message || '卡券加载失败'
      vouchers.value = []
    } finally {
      loading.value = false
    }
  }

  /**
   * 获取指定卡券的数量
   * @param {string} voucherKey - 卡券标识
   * @returns {number} 卡券数量
   */
  function getQuantity(voucherKey) {
    return quantityMap.value.get(voucherKey) || 0
  }

  /**
   * 使用卡券
   * 调用使用接口并更新卡券列表
   * @param {string} voucherKey - 卡券标识
   * @returns {boolean} 是否使用成功
   */
  async function consumeVoucher(voucherKey) {
    errorMessage.value = ''
    try {
      vouchers.value = await useVoucher(voucherKey)
      return true
    } catch (error) {
      errorMessage.value = error?.message || '卡券使用失败'
      await refreshVouchers()
      return false
    }
  }

  /** 组件挂载时自动加载卡券 */
  onMounted(refreshVouchers)

  return {
    vouchers,
    loading,
    errorMessage,
    refreshVouchers,
    getQuantity,
    consumeVoucher,
  }
}
