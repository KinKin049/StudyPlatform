/**
 * 用户个人资料模块，提供用户信息、头像上传、学习记录等相关 API
 */

import { request } from './request'

/**
 * 获取用户学习概况
 * @returns {Promise<any>} 学习概况数据
 */
export const fetchProfileOverview = () => request('/api/profile/overview')

/**
 * 获取用户个人信息
 * @returns {Promise<any>} 用户信息
 */
export const fetchProfileUser = () => request('/api/profile/user')

/**
 * 更新用户个人信息
 * @param {Object} payload - 更新信息
 * @returns {Promise<any>} 更新结果
 */
export const updateProfileUser = (payload) =>
  request('/api/profile/user', {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })

/**
 * 上传用户头像
 * @param {File} file - 头像文件
 * @returns {Promise<any>} 上传结果
 */
export const uploadProfileAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request('/api/profile/avatar', {
    method: 'POST',
    body: formData,
  })
}

/**
 * 记录学习事件
 * @param {Object} payload - 事件信息
 * @returns {Promise<any>} 记录结果
 */
export const recordProfileLearningEvent = (payload) =>
  request('/api/profile/events', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

/**
 * 记录学习时长
 * @param {Object} payload - 学习时长信息
 * @param {Object} [options={}] - 请求选项
 * @param {boolean} [options.keepalive] - 是否启用 keepalive
 * @returns {Promise<any>} 记录结果
 */
export const recordProfileLearningTime = (payload, options = {}) =>
  request('/api/profile/learning-time', {
    method: 'POST',
    body: JSON.stringify(payload),
    keepalive: Boolean(options.keepalive),
  })
