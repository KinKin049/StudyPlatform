import { request } from './request'

export const fetchAcademyHome = () => request('/api/academy/home')

export const fetchAcademyCourses = (resource) => request(`/api/academy/${resource}`)

export const fetchAcademyCourse = (resource, id) =>
  request(`/api/academy/${resource}/${encodeURIComponent(id)}`)

export const fetchMyAcademyCourses = (userId = 1) =>
  request(`/api/academy/my-courses?userId=${encodeURIComponent(userId)}`)

export const fetchAcademyAssignments = (userId = 1) =>
  request(`/api/academy/assignments?userId=${encodeURIComponent(userId)}`)

export const fetchAcademyAssignment = (assignmentId, userId = 1) =>
  request(`/api/academy/assignments/${encodeURIComponent(assignmentId)}?userId=${encodeURIComponent(userId)}`)

export const saveAcademyAssignmentDraft = (assignmentId, answers, userId = 1) =>
  request(`/api/academy/assignments/${encodeURIComponent(assignmentId)}/draft`, {
    method: 'POST',
    body: JSON.stringify({ userId, answers }),
  })

export const submitAcademyAssignment = (assignmentId, answers, userId = 1) =>
  request(`/api/academy/assignments/${encodeURIComponent(assignmentId)}/submit`, {
    method: 'POST',
    body: JSON.stringify({ userId, answers }),
  })

export const enrollAcademyCourse = (resource, id, payload = {}) =>
  request(`/api/academy/${resource}/${encodeURIComponent(id)}/enroll`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })

export const unenrollAcademyCourse = (resource, id, userId = 1) =>
  request(`/api/academy/${resource}/${encodeURIComponent(id)}/enroll?userId=${encodeURIComponent(userId)}`, {
    method: 'DELETE',
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

export const fetchQuestionBankSubjects = () => request('/api/academy/question-bank/subjects')

export const fetchQuestionBankProblems = ({ subject, keyword, difficulty, page = 0, size = 12 } = {}) => {
  const params = new URLSearchParams()
  if (subject) params.set('subject', subject)
  if (keyword) params.set('keyword', keyword)
  if (difficulty !== undefined && difficulty !== null && difficulty !== '') params.set('difficulty', difficulty)
  params.set('page', page)
  params.set('size', size)
  return request(`/api/academy/question-bank/problems?${params.toString()}`)
}

export const fetchQuestionBankProblem = (id) =>
  request(`/api/academy/question-bank/problems/${encodeURIComponent(id)}`)

export const fetchQuestionBankCourseCatalog = () =>
  request('/api/academy/question-bank/course-catalog')

export const fetchQuestionBankCourse = (code, { page = 0, size = 30, keyword = '' } = {}) => {
  const params = new URLSearchParams()
  params.set('page', page)
  params.set('size', size)
  if (keyword) params.set('keyword', keyword)
  return request(`/api/academy/question-bank/courses/${encodeURIComponent(code)}?${params.toString()}`)
}

export const fetchQuestionBankMistakeSummary = () =>
  request('/api/academy/question-bank/mistakes/summary')

export const fetchQuestionBankMistakes = ({ setCode = '', status = 'active', keyword = '', page = 0, size = 20 } = {}) => {
  const params = new URLSearchParams()
  if (setCode) params.set('setCode', setCode)
  if (status) params.set('status', status)
  if (keyword) params.set('keyword', keyword)
  params.set('page', page)
  params.set('size', size)
  return request(`/api/academy/question-bank/mistakes?${params.toString()}`)
}

export const recordQuestionBankAnswer = (payload) =>
  request('/api/academy/question-bank/mistakes/answers', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

export const fetchQuestionBankFavoriteSummary = () =>
  request('/api/academy/question-bank/favorites/summary')

export const fetchQuestionBankFavorites = ({ setCode = '', keyword = '', page = 0, size = 20 } = {}) => {
  const params = new URLSearchParams()
  if (setCode) params.set('setCode', setCode)
  if (keyword) params.set('keyword', keyword)
  params.set('page', page)
  params.set('size', size)
  return request(`/api/academy/question-bank/favorites?${params.toString()}`)
}

export const addQuestionBankFavorite = (questionId) =>
  request('/api/academy/question-bank/favorites', {
    method: 'POST',
    body: JSON.stringify({ questionId }),
  })

export const removeQuestionBankFavorite = (questionId) =>
  request(`/api/academy/question-bank/favorites/${encodeURIComponent(questionId)}`, {
    method: 'DELETE',
  })

export const fetchTypeWarriorWordPool = () =>
  request('/api/academy/question-bank/type-warrior/words')

export const importLuoguQuestionBank = ({ pages = 1, limit = 20 } = {}) =>
  request(`/api/academy/question-bank/import/luogu?pages=${pages}&limit=${limit}`, {
    method: 'POST',
  })

