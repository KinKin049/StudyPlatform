/**
 * HTTP 请求封装模块，提供统一的请求处理、错误处理和认证机制
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

/**
 * 解析资源 URL，将相对路径转换为完整的 API 地址
 * @param {string} path - 资源路径
 * @returns {string} 完整的资源 URL
 */
export function resolveResourceUrl(path) {
  if (!path || /^https?:\/\//i.test(path)) {
    return path || ''
  }

  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${API_BASE_URL}${normalizedPath}`
}

/**
 * 发起 HTTP 请求
 * @param {string} path - 请求路径
 * @param {Object} options - 请求配置选项
 * @param {Object} [options.body] - 请求体数据
 * @param {Object} [options.headers] - 请求头信息
 * @param {string} [options.method] - 请求方法
 * @returns {Promise<any>} 响应数据
 */
export async function request(path, options = {}) {
  const isFormData = typeof FormData !== 'undefined' && options.body instanceof FormData
  const authUserId = readStoredAuthUserId()
  const headers = {
    ...(options.body && !isFormData ? { 'Content-Type': 'application/json' } : {}),
    ...(authUserId ? { 'X-Auth-User-Id': authUserId } : {}),
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

/**
 * 从本地存储读取已认证用户 ID
 * @returns {string} 用户 ID
 */
function readStoredAuthUserId() {
  if (typeof localStorage === 'undefined') {
    return ''
  }
  try {
    const raw = localStorage.getItem('study-platform-auth-user')
    const user = raw ? JSON.parse(raw) : null
    return user?.id ? String(user.id) : ''
  } catch {
    return ''
  }
}

/**
 * 解析错误响应消息
 * @param {Response} response - HTTP 响应对象
 * @returns {string} 错误消息
 */
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
