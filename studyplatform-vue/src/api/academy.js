import { request } from './request'

export const fetchAcademyCourses = (resource) => request(`/api/academy/${resource}`)

export const fetchAcademyCategories = (resource) =>
  request(`/api/academy/${resource}/categories`)

