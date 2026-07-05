import { request } from './request'

export const fetchAdminUsers = () => request('/api/admin/users')

export const updateAdminUser = (userId, payload) =>
  request(`/api/admin/users/${userId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })

export const deleteAdminUser = (userId) =>
  request(`/api/admin/users/${userId}`, {
    method: 'DELETE',
  })

export const fetchAdminCourses = (resourceType) =>
  request(`/api/admin/courses?resourceType=${encodeURIComponent(resourceType)}`)

export const saveAdminCourse = (payload) =>
  request('/api/admin/courses', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

export const deleteAdminCourse = (resourceType, courseId) =>
  request(`/api/admin/courses/${encodeURIComponent(resourceType)}/${encodeURIComponent(courseId)}`, {
    method: 'DELETE',
  })

export const fetchAdminReviews = () => request('/api/admin/reviews')

export const deleteAdminReview = (reviewId) =>
  request(`/api/admin/reviews/${reviewId}`, {
    method: 'DELETE',
  })

export const fetchAdminQuestionBankSets = () => request('/api/admin/question-bank/sets')

export const saveAdminQuestionBankSet = (payload) =>
  request('/api/admin/question-bank/sets', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

export const deleteAdminQuestionBankSet = (setCode) =>
  request(`/api/admin/question-bank/sets/${encodeURIComponent(setCode)}`, {
    method: 'DELETE',
  })

export const fetchAdminQuestions = (setCode) =>
  request(`/api/admin/question-bank/questions?setCode=${encodeURIComponent(setCode)}`)

export const createAdminQuestion = (payload) =>
  request('/api/admin/question-bank/questions', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

export const updateAdminQuestion = (questionId, payload) =>
  request(`/api/admin/question-bank/questions/${questionId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })

export const deleteAdminQuestion = (questionId) =>
  request(`/api/admin/question-bank/questions/${questionId}`, {
    method: 'DELETE',
  })
