const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

const requestJson = async (path) => {
  const response = await fetch(`${API_BASE_URL}${path}`)

  if (!response.ok) {
    throw new Error(`接口请求失败：${response.status}`)
  }

  return response.json()
}

export const fetchAcademyCourses = (resource) => requestJson(`/api/academy/${resource}`)

export const fetchAcademyCategories = (resource) =>
  requestJson(`/api/academy/${resource}/categories`)

