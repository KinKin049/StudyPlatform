import { request } from './request'

export const fetchLadderJumpQuestionBanks = () =>
  request('/api/games/ladder-jump/question-banks')

export const saveLadderJumpRecord = (payload) =>
  request('/api/games/ladder-jump/records', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

export const saveTypeWarriorRecord = (payload) =>
  request('/api/games/type-warrior/records', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
