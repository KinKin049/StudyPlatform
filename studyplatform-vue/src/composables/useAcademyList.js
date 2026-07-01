import { computed, onMounted, ref } from 'vue'
import { fetchAcademyCategories, fetchAcademyCourses } from '../api/academy'

export function useAcademyList(resource, searchFields) {
  const selectedCategory = ref('全部')
  const keyword = ref('')
  const items = ref([])
  const remoteCategories = ref([])
  const loading = ref(true)
  const error = ref('')

  const categories = computed(() => [
    '全部',
    ...remoteCategories.value.map((category) => category.name),
  ])

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

  onMounted(loadItems)

  return {
    selectedCategory,
    keyword,
    categories,
    filteredItems,
    loading,
    error,
    loadItems,
  }
}
