import { request } from './request'

const AUTH_USER_KEY = 'study-platform-auth-user'

export const registerAuthUser = (payload) =>
  request('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

export const loginAuthUser = (payload) =>
  request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

export const saveAuthOnboarding = (payload) =>
  request('/api/auth/onboarding', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

export function getStoredAuthUser() {
  try {
    const raw = localStorage.getItem(AUTH_USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function storeAuthUser(user) {
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user))
  window.dispatchEvent(new CustomEvent('study-platform:auth-updated', { detail: user }))
}

export function clearStoredAuthUser() {
  localStorage.removeItem(AUTH_USER_KEY)
  window.dispatchEvent(new CustomEvent('study-platform:auth-updated'))
}
