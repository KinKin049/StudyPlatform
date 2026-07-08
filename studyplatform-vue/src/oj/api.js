import { request } from '../api/request'

export function listProblems(keyword = '', filters = {}) {
  const params = new URLSearchParams({ status: 'PUBLISHED' })
  if (keyword.trim()) {
    params.set('keyword', keyword.trim())
  }
  if (filters.tags?.length) {
    params.set('tags', filters.tags.join(','))
  }
  if (filters.difficulties?.length) {
    params.set('difficulties', filters.difficulties.join(','))
  }
  if (filters.languages?.length) {
    params.set('languages', filters.languages.join(','))
  }
  return request(`/api/oj/problems?${params.toString()}`)
}

export function listCategories() {
  return request('/api/oj/problems/categories')
}

export function getProblem(id) {
  return request(`/api/oj/problems/${id}`)
}

export function createSubmission(payload) {
  return request('/api/oj/submissions', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export function getSubmission(id) {
  return request(`/api/oj/submissions/${id}`)
}

export function listSubmissionCases(id) {
  return request(`/api/oj/submissions/${id}/cases`)
}

