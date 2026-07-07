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
