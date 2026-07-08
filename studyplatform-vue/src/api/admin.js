/**
 * 管理员模块，提供用户管理、课程管理、题库管理等后台管理相关 API
 */

import { request } from './request'

/**
 * 获取用户列表
 * @returns {Promise<any>} 用户列表
 */
export const fetchAdminUsers = () => request('/api/admin/users')

/**
 * 更新用户信息
 * @param {string|number} userId - 用户 ID
 * @param {Object} payload - 更新信息
 * @returns {Promise<any>} 更新结果
 */
export const updateAdminUser = (userId, payload) =>
  request(`/api/admin/users/${userId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })

/**
 * 删除用户
 * @param {string|number} userId - 用户 ID
 * @returns {Promise<any>} 删除结果
 */
export const deleteAdminUser = (userId) =>
  request(`/api/admin/users/${userId}`, {
    method: 'DELETE',
  })

/**
 * 获取课程列表
 * @param {string} resourceType - 资源类型
 * @returns {Promise<any>} 课程列表
 */
export const fetchAdminCourses = (resourceType) =>
  request(`/api/admin/courses?resourceType=${encodeURIComponent(resourceType)}`)

/**
 * 获取管理员维护的课程分类
 * @param {string} resourceType - 资源类型
 * @returns {Promise<any>} 分类列表
 */
export const fetchAdminCourseCategories = (resourceType) =>
  request(`/api/admin/course-categories?resourceType=${encodeURIComponent(resourceType)}`)

/**
 * 保存课程分类
 * @param {Object} payload - 分类信息
 * @returns {Promise<any>} 分类列表
 */
export const saveAdminCourseCategory = (payload) =>
  request('/api/admin/course-categories', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

/**
 * 删除课程分类
 * @param {string} resourceType - 资源类型
 * @param {string} name - 分类名称
 * @returns {Promise<any>} 分类列表
 */
export const deleteAdminCourseCategory = (resourceType, name) =>
  request(`/api/admin/course-categories/${encodeURIComponent(resourceType)}/${encodeURIComponent(name)}`, {
    method: 'DELETE',
  })

/**
 * 保存课程信息
 * @param {Object} payload - 课程信息
 * @returns {Promise<any>} 保存结果
 */
export const saveAdminCourse = (payload) =>
  request('/api/admin/courses', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

/**
 * 删除课程
 * @param {string} resourceType - 资源类型
 * @param {string|number} courseId - 课程 ID
 * @returns {Promise<any>} 删除结果
 */
export const deleteAdminCourse = (resourceType, courseId) =>
  request(`/api/admin/courses/${encodeURIComponent(resourceType)}/${encodeURIComponent(courseId)}`, {
    method: 'DELETE',
  })

/**
 * 获取评价列表
 * @returns {Promise<any>} 评价列表
 */
export const fetchAdminReviews = () => request('/api/admin/reviews')

/**
 * 删除评价
 * @param {string|number} reviewId - 评价 ID
 * @returns {Promise<any>} 删除结果
 */
export const deleteAdminReview = (reviewType, reviewId) =>
  request(`/api/admin/reviews/${encodeURIComponent(reviewType)}/${reviewId}`, {
    method: 'DELETE',
  })

export const replyAdminReview = (reviewType, reviewId, payload) =>
  request(
    reviewType === 'course'
      ? `/api/academy/reviews/${reviewId}/reply`
      : `/api/admin/reviews/${encodeURIComponent(reviewType)}/${reviewId}/reply`,
    {
    method: 'POST',
    body: JSON.stringify(payload),
    },
  )

export const deleteAdminReviewReply = (reviewType, reviewId) =>
  request(`/api/admin/reviews/${encodeURIComponent(reviewType)}/${reviewId}/reply`, {
    method: 'DELETE',
  })

/**
 * 获取题库集合列表
 * @returns {Promise<any>} 题库集合列表
 */
export const fetchAdminQuestionBankSets = () => request('/api/admin/question-bank/sets')

/**
 * 保存题库集合
 * @param {Object} payload - 题库集合信息
 * @returns {Promise<any>} 保存结果
 */
export const saveAdminQuestionBankSet = (payload) =>
  request('/api/admin/question-bank/sets', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

/**
 * 删除题库集合
 * @param {string} setCode - 题库代码
 * @returns {Promise<any>} 删除结果
 */
export const deleteAdminQuestionBankSet = (setCode) =>
  request(`/api/admin/question-bank/sets/${encodeURIComponent(setCode)}`, {
    method: 'DELETE',
  })

/**
 * 获取题目列表
 * @param {string} setCode - 题库代码
 * @returns {Promise<any>} 题目列表
 */
export const fetchAdminQuestions = (setCode) =>
  request(`/api/admin/question-bank/questions?setCode=${encodeURIComponent(setCode)}`)

/**
 * 创建题目
 * @param {Object} payload - 题目信息
 * @returns {Promise<any>} 创建结果
 */
export const createAdminQuestion = (payload) =>
  request('/api/admin/question-bank/questions', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

/**
 * 更新题目
 * @param {string|number} questionId - 题目 ID
 * @param {Object} payload - 更新信息
 * @returns {Promise<any>} 更新结果
 */
export const updateAdminQuestion = (questionId, payload) =>
  request(`/api/admin/question-bank/questions/${questionId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })

/**
 * 删除题目
 * @param {string|number} questionId - 题目 ID
 * @returns {Promise<any>} 删除结果
 */
export const deleteAdminQuestion = (questionId) =>
  request(`/api/admin/question-bank/questions/${questionId}`, {
    method: 'DELETE',
  })

/**
 * 获取优惠券列表
 * @returns {Promise<any>} 优惠券列表
 */
export const fetchAdminOjProblems = () => request('/api/admin/oj/problems')

export const fetchAdminOjCategories = () => request('/api/admin/oj/categories')

export const saveAdminOjCategory = (payload) =>
  request('/api/admin/oj/categories', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

export const deleteAdminOjCategory = (name) =>
  request(`/api/admin/oj/categories/${encodeURIComponent(name)}`, {
    method: 'DELETE',
  })

export const fetchAdminOjProblem = (problemId) =>
  request(`/api/admin/oj/problems/${problemId}?_=${Date.now()}`)

export const createAdminOjProblem = (payload) =>
  request('/api/admin/oj/problems', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

export const updateAdminOjProblem = (problemId, payload) =>
  request(`/api/admin/oj/problems/${problemId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })

export const deleteAdminOjProblem = (problemId) =>
  request(`/api/admin/oj/problems/${problemId}`, {
    method: 'DELETE',
  })

export const checkAdminOjProblem = (payload) =>
  request('/api/admin/oj/problems/check', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

export const fetchAdminVouchers = () => request('/api/admin/vouchers')

/**
 * 保存优惠券
 * @param {Object} payload - 优惠券信息
 * @returns {Promise<any>} 保存结果
 */
export const saveAdminVoucher = (payload) =>
  request('/api/admin/vouchers', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

/**
 * 删除优惠券
 * @param {string} voucherKey - 优惠券编码
 * @returns {Promise<any>} 删除结果
 */
export const deleteAdminVoucher = (voucherKey) =>
  request(`/api/admin/vouchers/${encodeURIComponent(voucherKey)}`, {
    method: 'DELETE',
  })
