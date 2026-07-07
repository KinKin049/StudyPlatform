/**
 * AI 宠物模块，提供与 AI 宠物聊天交互相关 API
 */

import { request } from './request'

/**
 * 与 AI 宠物聊天
 * @param {Object} payload - 聊天消息信息
 * @returns {Promise<any>} 聊天回复
 */
export const chatWithAiPet = (payload) =>
  request('/api/ai-pet/chat', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
