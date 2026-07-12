/**
 * 用户认证模块，提供注册、登录、密码重置等认证相关 API
 */

import { request } from './request'

const AUTH_USER_KEY = 'study-platform-auth-user'

/**
 * 注册新用户
 * @param {Object} payload - 注册信息
 * @returns {Promise<any>} 注册结果
 */
export const registerAuthUser = (payload) =>
  request('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

/**
 * 用户登录
 * @param {Object} payload - 登录信息
 * @returns {Promise<any>} 登录结果
 */
export const loginAuthUser = (payload) =>
  request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

/**
 * 保存用户引导信息
 * @param {Object} payload - 引导信息
 * @returns {Promise<any>} 保存结果
 */
export const saveAuthOnboarding = (payload) =>
  request('/api/auth/onboarding', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

export const updateAuthPet = (payload) =>
  request('/api/auth/pet', {
    method: 'PUT',
    body: JSON.stringify(payload),
  })

/**
 * 发送密码重置验证码
 * @param {Object} payload - 请求参数
 * @returns {Promise<any>} 发送结果
 */
export const sendPasswordResetCode = (payload) =>
  request('/api/auth/password-reset/code', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

/**
 * 确认密码重置
 * @param {Object} payload - 重置信息
 * @returns {Promise<any>} 重置结果
 */
export const confirmPasswordReset = (payload) =>
  request('/api/auth/password-reset/confirm', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

/**
 * 获取本地存储的认证用户信息
 * @returns {Object|null} 用户信息对象或 null
 */
export function getStoredAuthUser() {
  try {
    const raw = localStorage.getItem(AUTH_USER_KEY)
    const user = raw ? JSON.parse(raw) : null
    if (!user?.id || typeof user?.token !== 'string' || !user.token) {
      localStorage.removeItem(AUTH_USER_KEY)
      return null
    }
    return user
  } catch {
    localStorage.removeItem(AUTH_USER_KEY)
    return null
  }
}

/**
 * 存储认证用户信息到本地存储
 * @param {Object} user - 用户信息对象
 */
export function storeAuthUser(user) {
  localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user))
  window.dispatchEvent(new CustomEvent('study-platform:auth-updated', { detail: user }))
}

/**
 * 清除本地存储的认证用户信息
 */
export function clearStoredAuthUser() {
  localStorage.removeItem(AUTH_USER_KEY)
  window.dispatchEvent(new CustomEvent('study-platform:auth-updated'))
}
