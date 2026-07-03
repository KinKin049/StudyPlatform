import { request } from './request'

export const fetchAcademyHome = () => request('/api/academy/home')

export const fetchAcademyCourses = (resource) => request(`/api/academy/${resource}`)

export const fetchAcademyCourse = (resource, id) =>
  request(`/api/academy/${resource}/${encodeURIComponent(id)}`)

export const enrollAcademyCourse = (resource, id, payload = {}) =>
  request(`/api/academy/${resource}/${encodeURIComponent(id)}/enroll`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })

export const fetchAcademyCourseReviews = (resource, id) =>
  request(`/api/academy/${resource}/${encodeURIComponent(id)}/reviews`)

export const createAcademyCourseReview = (resource, id, payload) =>
  request(`/api/academy/${resource}/${encodeURIComponent(id)}/reviews`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })

export const fetchAcademyCategories = (resource) =>
  request(`/api/academy/${resource}/categories`)

