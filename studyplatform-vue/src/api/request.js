const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const AUTH_USER_KEY = 'study-platform-auth-user'

export function resolveResourceUrl(path) {
  if (!path || /^https?:\/\//i.test(path)) {
    return path || ''
  }

  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${API_BASE_URL}${normalizedPath}`
}

export async function request(path, options = {}) {
  const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData
  const authToken = readStoredAuthToken()
  const headers = {
    ...(options.body && !isFormData ? { 'Content-Type': 'application/json' } : {}),
    ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
    ...(options.headers || {}),
  }

  const response = await fetch(resolveResourceUrl(path), {
    ...options,
    headers,
  })

  if (!response.ok) {
    if (response.status === 401 || response.status === 403) {
      clearStoredAuthUser()
      throw new Error('登录状态已过期，请重新登录')
    }
    throw new Error(await resolveErrorMessage(response))
  }

  if (response.status === 204) {
    return null
  }

  return response.json()
}

function readStoredAuthToken() {
  if (typeof localStorage === 'undefined') {
    return ''
  }
  try {
    const raw = localStorage.getItem(AUTH_USER_KEY)
    const user = raw ? JSON.parse(raw) : null
    return typeof user?.token === 'string' ? user.token : ''
  } catch {
    return ''
  }
}

function clearStoredAuthUser() {
  if (typeof localStorage === 'undefined') {
    return
  }
  localStorage.removeItem(AUTH_USER_KEY)
  window.dispatchEvent(new CustomEvent('study-platform:auth-updated'))
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
