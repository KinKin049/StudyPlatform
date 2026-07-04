const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

export function resolveResourceUrl(path) {
  if (!path || /^https?:\/\//i.test(path)) {
    return path || ''
  }

  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${API_BASE_URL}${normalizedPath}`
}

export async function request(path, options = {}) {
  const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData
  const headers = {
    ...(options.body && !isFormData ? { 'Content-Type': 'application/json' } : {}),
    ...(options.headers || {}),
  }

  const response = await fetch(resolveResourceUrl(path), {
    ...options,
    headers,
  })

  if (!response.ok) {
    throw new Error(await resolveErrorMessage(response))
  }

  if (response.status === 204) {
    return null
  }

  return response.json()
}

async function resolveErrorMessage(response) {
  const fallback = `接口请求失败：${response.status}`

  try {
    const text = await response.text()
    if (!text) {
      return fallback
    }

    try {
      const data = JSON.parse(text)
      return data.message || data.error || text
    } catch {
      return text
    }
  } catch {
    return fallback
  }
}
