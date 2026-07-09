/**
 * 学习平台模块，提供课程、作业、考试、题库等学习相关 API
 */

import { request } from './request'

/**
 * 获取学习平台首页数据
 * @returns {Promise<any>} 首页数据
 */
export const fetchAcademyHome = () => request('/api/academy/home')

/**
 * 获取课程列表
 * @param {string} resource - 资源类型
 * @returns {Promise<any>} 课程列表
 */
export const fetchAcademyCourses = (resource) => request(`/api/academy/${resource}`)

/**
 * 获取课程详情
 * @param {string} resource - 资源类型
 * @param {string|number} id - 课程 ID
 * @returns {Promise<any>} 课程详情
 */
export const fetchAcademyCourse = (resource, id) =>
  request(`/api/academy/${resource}/${encodeURIComponent(id)}`)

/**
 * 获取教材详情
 * @param {string|number} id - 教材 ID
 * @param {string|number} [userId=1] - 用户 ID
 * @returns {Promise<any>} 教材详情
 */
export const fetchAcademyTextbook = (id, userId = 1) =>
  request(`/api/academy/textbooks/${encodeURIComponent(id)}?userId=${encodeURIComponent(userId)}`)

/**
 * 获取教材购物车
 * @param {string|number} [userId=1] - 用户 ID
 * @returns {Promise<any>} 购物车数据
 */
export const fetchAcademyTextbookCart = (userId = 1) =>
  request(`/api/academy/textbook-cart?userId=${encodeURIComponent(userId)}`)

/**
 * 添加教材到购物车
 * @param {Object} payload - 添加信息
 * @returns {Promise<any>} 添加结果
 */
export const addAcademyTextbookCartItem = (payload) =>
  request('/api/academy/textbook-cart', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

/**
 * 从购物车移除教材
 * @param {string|number} itemId - 购物车项 ID
 * @param {string|number} [userId=1] - 用户 ID
 * @returns {Promise<any>} 移除结果
 */
export const removeAcademyTextbookCartItem = (itemId, userId = 1) =>
  request(`/api/academy/textbook-cart/${encodeURIComponent(itemId)}?userId=${encodeURIComponent(userId)}`, {
    method: 'DELETE',
  })

/**
 * 更新购物车教材数量
 * @param {string|number} itemId - 购物车项 ID
 * @param {Object} payload - 更新信息
 * @returns {Promise<any>} 更新结果
 */
export const updateAcademyTextbookCartItem = (itemId, payload) =>
  request(`/api/academy/textbook-cart/${encodeURIComponent(itemId)}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })

/**
 * 创建教材订单
 * @param {Object} payload - 订单信息
 * @returns {Promise<any>} 订单结果
 */
export const createAcademyTextbookOrder = (payload) =>
  request('/api/academy/textbook-orders', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

/**
 * 支付教材订单
 * @param {string} orderNo - 订单编号
 * @param {string|number} [userId=1] - 用户 ID
 * @returns {Promise<any>} 支付结果
 */
export const payAcademyTextbookOrder = (orderNo, userId = 1) =>
  request(`/api/academy/textbook-orders/${encodeURIComponent(orderNo)}/pay?userId=${encodeURIComponent(userId)}`, {
    method: 'POST',
  })

/**
 * 创建教材评价
 * @param {string|number} textbookId - 教材 ID
 * @param {Object} payload - 评价信息
 * @returns {Promise<any>} 创建结果
 */
export const createAcademyTextbookReview = (textbookId, payload) =>
  request(`/api/academy/textbooks/${encodeURIComponent(textbookId)}/reviews`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })

/**
 * 发布在线公开课
 * @param {Object} payload - 课程信息
 * @returns {Promise<any>} 发布结果
 */
export const publishOnlineOpenCourse = (payload) =>
  request('/api/academy/online-open-courses', {
    method: 'POST',
    body: payload,
  })

/**
 * 获取我发布的在线公开课列表
 * @returns {Promise<any>} 课程列表
 */
export const fetchMyPublishedOnlineOpenCourses = () =>
  request('/api/academy/online-open-courses/teacher/mine')

/**
 * 删除已发布的在线公开课
 * @param {string|number} id - 课程 ID
 * @returns {Promise<any>} 删除结果
 */
export const deletePublishedOnlineOpenCourse = (id) =>
  request(`/api/academy/online-open-courses/${encodeURIComponent(id)}`, {
    method: 'DELETE',
  })

/**
 * 获取我的课程列表
 * @param {string|number} [userId=1] - 用户 ID
 * @returns {Promise<any>} 课程列表
 */
export const fetchMyAcademyCourses = (userId = 1) =>
  request(`/api/academy/my-courses?userId=${encodeURIComponent(userId)}`)

/**
 * 获取作业列表
 * @param {string|number} [userId=1] - 用户 ID
 * @returns {Promise<any>} 作业列表
 */
export const fetchAcademyAssignments = (userId = 1) =>
  request(`/api/academy/assignments?userId=${encodeURIComponent(userId)}`)

/**
 * 获取作业详情
 * @param {string|number} assignmentId - 作业 ID
 * @param {string|number} [userId=1] - 用户 ID
 * @returns {Promise<any>} 作业详情
 */
export const fetchAcademyAssignment = (assignmentId, userId = 1) =>
  request(`/api/academy/assignments/${encodeURIComponent(assignmentId)}?userId=${encodeURIComponent(userId)}`)

/**
 * 保存作业草稿
 * @param {string|number} assignmentId - 作业 ID
 * @param {Object} answers - 答案数据
 * @param {string|number} [userId=1] - 用户 ID
 * @returns {Promise<any>} 保存结果
 */
export const saveAcademyAssignmentDraft = (assignmentId, answers, userId = 1) =>
  request(`/api/academy/assignments/${encodeURIComponent(assignmentId)}/draft`, {
    method: 'POST',
    body: JSON.stringify({ userId, answers }),
  })

/**
 * 提交作业
 * @param {string|number} assignmentId - 作业 ID
 * @param {Object} answers - 答案数据
 * @param {string|number} [userId=1] - 用户 ID
 * @returns {Promise<any>} 提交结果
 */
export const submitAcademyAssignment = (assignmentId, answers, userId = 1) =>
  request(`/api/academy/assignments/${encodeURIComponent(assignmentId)}/submit`, {
    method: 'POST',
    body: JSON.stringify({ userId, answers }),
  })

/**
 * 获取考试列表
 * @param {string|number} [userId=1] - 用户 ID
 * @returns {Promise<any>} 考试列表
 */
export const fetchAcademyExams = (userId = 1) =>
  request(`/api/academy/exams?userId=${encodeURIComponent(userId)}`)

/**
 * 获取考试详情
 * @param {string|number} examId - 考试 ID
 * @param {string|number} [userId=1] - 用户 ID
 * @returns {Promise<any>} 考试详情
 */
export const fetchAcademyExam = (examId, userId = 1) =>
  request(`/api/academy/exams/${encodeURIComponent(examId)}?userId=${encodeURIComponent(userId)}`)

/**
 * 开始考试
 * @param {string|number} examId - 考试 ID
 * @param {string|number} [userId=1] - 用户 ID
 * @returns {Promise<any>} 开始结果
 */
export const startAcademyExam = (examId, userId = 1) =>
  request(`/api/academy/exams/${encodeURIComponent(examId)}/start`, {
    method: 'POST',
    body: JSON.stringify({ userId, answers: {} }),
  })

/**
 * 保存考试草稿
 * @param {string|number} examId - 考试 ID
 * @param {Object} answers - 答案数据
 * @param {string|number} [userId=1] - 用户 ID
 * @returns {Promise<any>} 保存结果
 */
export const saveAcademyExamDraft = (examId, answers, userId = 1) =>
  request(`/api/academy/exams/${encodeURIComponent(examId)}/draft`, {
    method: 'POST',
    body: JSON.stringify({ userId, answers }),
  })

/**
 * 提交考试
 * @param {string|number} examId - 考试 ID
 * @param {Object} answers - 答案数据
 * @param {string|number} [userId=1] - 用户 ID
 * @returns {Promise<any>} 提交结果
 */
export const submitAcademyExam = (examId, answers, userId = 1) =>
  request(`/api/academy/exams/${encodeURIComponent(examId)}/submit`, {
    method: 'POST',
    body: JSON.stringify({ userId, answers }),
  })

/**
 * 报名课程
 * @param {string} resource - 资源类型
 * @param {string|number} id - 课程 ID
 * @param {Object} [payload={}] - 报名信息
 * @returns {Promise<any>} 报名结果
 */
export const enrollAcademyCourse = (resource, id, payload = {}) =>
  request(`/api/academy/${resource}/${encodeURIComponent(id)}/enroll`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })

/**
 * 取消课程报名
 * @param {string} resource - 资源类型
 * @param {string|number} id - 课程 ID
 * @param {string|number} [userId=1] - 用户 ID
 * @returns {Promise<any>} 取消结果
 */
export const unenrollAcademyCourse = (resource, id, userId = 1) =>
  request(`/api/academy/${resource}/${encodeURIComponent(id)}/enroll?userId=${encodeURIComponent(userId)}`, {
    method: 'DELETE',
  })

/**
 * 获取课程评价列表
 * @param {string} resource - 资源类型
 * @param {string|number} id - 课程 ID
 * @returns {Promise<any>} 评价列表
 */
export const fetchAcademyCourseReviews = (resource, id) =>
  request(`/api/academy/${resource}/${encodeURIComponent(id)}/reviews`)

/**
 * 创建课程评价
 * @param {string} resource - 资源类型
 * @param {string|number} id - 课程 ID
 * @param {Object} payload - 评价信息
 * @returns {Promise<any>} 创建结果
 */
export const createAcademyCourseReview = (resource, id, payload) =>
  request(`/api/academy/${resource}/${encodeURIComponent(id)}/reviews`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })

export const replyAcademyCourseReview = (reviewId, payload) =>
  request(`/api/academy/reviews/${reviewId}/reply`, {
    method: 'POST',
    body: JSON.stringify(payload),
  })

/**
 * 获取课程分类
 * @param {string} resource - 资源类型
 * @returns {Promise<any>} 分类列表
 */
export const fetchAcademyCategories = (resource) =>
  request(`/api/academy/${resource}/categories`)

/**
 * 获取题库科目列表
 * @returns {Promise<any>} 科目列表
 */
export const fetchQuestionBankSubjects = () => request('/api/academy/question-bank/subjects')

/**
 * 获取题库题目列表
 * @param {Object} [params={}] - 查询参数
 * @param {string} [params.subject] - 科目
 * @param {string} [params.keyword] - 关键词
 * @param {string} [params.difficulty] - 难度
 * @param {number} [params.page=0] - 页码
 * @param {number} [params.size=12] - 每页数量
 * @returns {Promise<any>} 题目列表
 */
export const fetchQuestionBankProblems = ({ subject, keyword, difficulty, page = 0, size = 12 } = {}) => {
  const params = new URLSearchParams()
  if (subject) params.set('subject', subject)
  if (keyword) params.set('keyword', keyword)
  if (difficulty !== undefined && difficulty !== null && difficulty !== '') params.set('difficulty', difficulty)
  params.set('page', page)
  params.set('size', size)
  return request(`/api/academy/question-bank/problems?${params.toString()}`)
}

/**
 * 获取题库题目详情
 * @param {string|number} id - 题目 ID
 * @returns {Promise<any>} 题目详情
 */
export const fetchQuestionBankProblem = (id) =>
  request(`/api/academy/question-bank/problems/${encodeURIComponent(id)}`)

/**
 * 获取题库课程目录
 * @returns {Promise<any>} 课程目录
 */
export const fetchQuestionBankCourseCatalog = () =>
  request('/api/academy/question-bank/course-catalog')

/**
 * 获取题库课程题目列表
 * @param {string} code - 课程代码
 * @param {Object} [params={}] - 查询参数
 * @param {number} [params.page=0] - 页码
 * @param {number} [params.size=30] - 每页数量
 * @param {string} [params.keyword=''] - 关键词
 * @returns {Promise<any>} 题目列表
 */
export const fetchQuestionBankCourse = (code, { page = 0, size = 30, keyword = '' } = {}) => {
  const params = new URLSearchParams()
  params.set('page', page)
  params.set('size', size)
  if (keyword) params.set('keyword', keyword)
  return request(`/api/academy/question-bank/courses/${encodeURIComponent(code)}?${params.toString()}`)
}

/**
 * 获取错题统计摘要
 * @returns {Promise<any>} 错题统计
 */
export const fetchQuestionBankMistakeSummary = () =>
  request('/api/academy/question-bank/mistakes/summary')

/**
 * 获取错题列表
 * @param {Object} [params={}] - 查询参数
 * @param {string} [params.setCode=''] - 题库代码
 * @param {string} [params.status='active'] - 状态
 * @param {string} [params.keyword=''] - 关键词
 * @param {number} [params.page=0] - 页码
 * @param {number} [params.size=20] - 每页数量
 * @returns {Promise<any>} 错题列表
 */
export const fetchQuestionBankMistakes = ({ setCode = '', status = 'active', keyword = '', page = 0, size = 20 } = {}) => {
  const params = new URLSearchParams()
  if (setCode) params.set('setCode', setCode)
  if (status) params.set('status', status)
  if (keyword) params.set('keyword', keyword)
  params.set('page', page)
  params.set('size', size)
  return request(`/api/academy/question-bank/mistakes?${params.toString()}`)
}

/**
 * 记录题库答题结果
 * @param {Object} payload - 答题信息
 * @returns {Promise<any>} 记录结果
 */
export const recordQuestionBankAnswer = (payload) =>
  request('/api/academy/question-bank/mistakes/answers', {
    method: 'POST',
    body: JSON.stringify(payload),
  })

/**
 * 获取收藏题目统计摘要
 * @returns {Promise<any>} 收藏统计
 */
export const fetchQuestionBankFavoriteSummary = () =>
  request('/api/academy/question-bank/favorites/summary')

/**
 * 获取收藏题目列表
 * @param {Object} [params={}] - 查询参数
 * @param {string} [params.setCode=''] - 题库代码
 * @param {string} [params.keyword=''] - 关键词
 * @param {number} [params.page=0] - 页码
 * @param {number} [params.size=20] - 每页数量
 * @returns {Promise<any>} 收藏列表
 */
export const fetchQuestionBankFavorites = ({ setCode = '', keyword = '', page = 0, size = 20 } = {}) => {
  const params = new URLSearchParams()
  if (setCode) params.set('setCode', setCode)
  if (keyword) params.set('keyword', keyword)
  params.set('page', page)
  params.set('size', size)
  return request(`/api/academy/question-bank/favorites?${params.toString()}`)
}

/**
 * 添加题目到收藏
 * @param {string|number} questionId - 题目 ID
 * @returns {Promise<any>} 添加结果
 */
export const addQuestionBankFavorite = (questionId) =>
  request('/api/academy/question-bank/favorites', {
    method: 'POST',
    body: JSON.stringify({ questionId }),
  })

/**
 * 从收藏移除题目
 * @param {string|number} questionId - 题目 ID
 * @returns {Promise<any>} 移除结果
 */
export const removeQuestionBankFavorite = (questionId) =>
  request(`/api/academy/question-bank/favorites/${encodeURIComponent(questionId)}`, {
    method: 'DELETE',
  })

/**
 * 获取打字战士词库
 * @returns {Promise<any>} 词库数据
 */
export const fetchTypeWarriorWordPool = () =>
  request('/api/academy/question-bank/type-warrior/words')

/**
 * 导入洛谷题库
 * @param {Object} [params={}] - 导入参数
 * @param {number} [params.pages=1] - 页数
 * @param {number} [params.limit=20] - 每页数量
 * @returns {Promise<any>} 导入结果
 */
export const importLuoguQuestionBank = ({ pages = 1, limit = 20 } = {}) =>
  request(`/api/academy/question-bank/import/luogu?pages=${pages}&limit=${limit}`, {
    method: 'POST',
  })
