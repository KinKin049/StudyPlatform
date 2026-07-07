/**
 * 学院列表组合式函数
 * 提供课程列表的加载、筛选、分页等功能
 */
import { computed, onMounted, ref, watch } from 'vue'
import { fetchAcademyCategories, fetchAcademyCourses } from '../api/academy'

/**
 * 创建学院列表管理对象
 * @param {string} resource - 资源类型标识
 * @param {string[]} searchFields - 搜索字段数组
 * @param {Object} options - 配置选项
 * @param {number} options.pageSize - 每页条数
 * @returns {Object} 列表管理对象
 */
export function useAcademyList(resource, searchFields, options = {}) {
  /** 当前选中的分类 */
  const selectedCategory = ref('全部')
  /** 搜索关键词 */
  const keyword = ref('')
  /** 列表数据 */
  const items = ref([])
  /** 远程分类数据 */
  const remoteCategories = ref([])
  /** 加载状态 */
  const loading = ref(true)
  /** 错误信息 */
  const error = ref('')
  /** 当前页码 */
  const currentPage = ref(1)
  /** 每页条数 */
  const pageSize = ref(options.pageSize || 12)

  /**
   * 分类列表
   * 包含'全部'选项和远程获取的分类
   */
  const categories = computed(() => [
    '全部',
    ...remoteCategories.value.map((category) => category.name),
  ])

  /**
   * 筛选后的列表数据
   * 根据选中分类和关键词进行过滤
   */
  const filteredItems = computed(() => {
    const normalizedKeyword = keyword.value.trim().toLowerCase()

    return items.value.filter((item) => {
      const matchesCategory =
        selectedCategory.value === '全部' || item.category === selectedCategory.value
      const matchesKeyword =
        !normalizedKeyword ||
        searchFields.some((field) =>
          String(item[field] || '')
            .toLowerCase()
            .includes(normalizedKeyword),
        )

      return matchesCategory && matchesKeyword
    })
  })

  /** 筛选后的总条数 */
  const totalItems = computed(() => filteredItems.value.length)

  /**
   * 当前页的列表数据
   * 根据页码和每页条数进行分页
   */
  const pagedItems = computed(() => {
    const start = (currentPage.value - 1) * pageSize.value
    return filteredItems.value.slice(start, start + pageSize.value)
  })

  /**
   * 监听分类、关键词、每页条数变化
   * 变化时重置页码为第一页
   */
  watch([selectedCategory, keyword, pageSize], () => {
    currentPage.value = 1
  })

  /**
   * 监听总条数变化
   * 当总条数减少导致当前页超出范围时，自动调整页码
   */
  watch(totalItems, (total) => {
    const maxPage = Math.max(1, Math.ceil(total / pageSize.value))
    if (currentPage.value > maxPage) {
      currentPage.value = maxPage
    }
  })

  /**
   * 加载列表数据
   * 并行请求分类和课程数据
   */
  const loadItems = async () => {
    loading.value = true
    error.value = ''

    try {
      const [categoryData, itemData] = await Promise.all([
        fetchAcademyCategories(resource),
        fetchAcademyCourses(resource),
      ])
      remoteCategories.value = categoryData
      items.value = itemData
    } catch (err) {
      error.value = err instanceof Error ? err.message : '接口请求失败'
      remoteCategories.value = []
      items.value = []
    } finally {
      loading.value = false
    }
  }

  /** 组件挂载时自动加载数据 */
  onMounted(loadItems)

  return {
    selectedCategory,
    keyword,
    categories,
    filteredItems,
    pagedItems,
    currentPage,
    pageSize,
    totalItems,
    loading,
    error,
    loadItems,
  }
}
