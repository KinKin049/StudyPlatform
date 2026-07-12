/**
 * 游戏模块，提供天梯跳跃、打字战士等游戏相关 API
 */

import { request } from './request'

/**
 * 获取天梯跳跃题库列表
 * @returns {Promise<any>} 题库列表
 */
export const fetchLadderJumpQuestionBanks = () =>
  request('/api/games/ladder-jump/question-banks')

/**
 * 获取天梯跳跃题目列表
 * @param {string} setCode - 题库编码，可为空表示全部题库
 * @returns {Promise<any>} 题目列表
 */
export const fetchLadderJumpQuestions = (setCode = '') => {
  const normalizedSetCode = String(setCode || '').trim()
  const params = new URLSearchParams()
  if (normalizedSetCode) {
    params.set('setCode', normalizedSetCode)
  }
  const query = params.toString()
  return request(query ? `/api/games/ladder-jump/questions?${query}` : '/api/games/ladder-jump/questions')
}

/**
 * 保存天梯跳跃游戏记录
 * @param {Object} payload - 游戏记录信息
 * @returns {Promise<any>} 保存结果
 */
export const saveLadderJumpRecord = (payload) =>
  request('/api/games/ladder-jump/records', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

/**
 * 保存打字战士游戏记录
 * @param {Object} payload - 游戏记录信息
 * @returns {Promise<any>} 保存结果
 */
export const saveTypeWarriorRecord = (payload) =>
  request('/api/games/type-warrior/records', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
