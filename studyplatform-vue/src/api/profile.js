import { request } from './request'

export const fetchProfileOverview = () => request('/api/profile/overview')

export const fetchProfileUser = () => request('/api/profile/user')

export const updateProfileUser = (payload) =>
  request('/api/profile/user', {
    method: 'PATCH',
    body: JSON.stringify(payload),
  })

export const uploadProfileAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request('/api/profile/avatar', {
    method: 'POST',
    body: formData,
  })
}

export const recordProfileLearningEvent = (payload) =>
  request('/api/profile/events', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
