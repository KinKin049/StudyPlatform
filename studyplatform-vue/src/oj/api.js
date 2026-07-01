const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

// Small API wrapper used only by the standalone OJ page.
async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
    ...options,
  })

  if (!response.ok) {
    const message = await response.text()
    throw new Error(message || `Request failed: ${response.status}`)
  }

  if (response.status === 204) {
    return null
  }

  return response.json()
}

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

