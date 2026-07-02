import { request } from '../api/request'

export function listProblems() {
  return request('/api/oj/problems?status=PUBLISHED')
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

