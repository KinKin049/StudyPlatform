import { request } from './request'

export const chatWithAiPet = (payload) =>
  request('/api/ai-pet/chat', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
