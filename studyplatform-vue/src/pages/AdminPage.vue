<script setup>
// 管理员后台页面，提供用户管理、课程管理、卡券管理、题库管理和评论管理功能
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete as AdminDeleteIcon, EditPen as AdminEditIcon, Lock as AdminLockIcon } from '@element-plus/icons-vue'
import { getStoredAuthUser } from '../api/auth'
import {
  checkAdminOjProblem,
  createAdminQuestion,
  createAdminOjProblem,
  deleteAdminCourseCategory,
  deleteAdminCourse,
  deleteAdminOjCategory,
  deleteAdminOjProblem,
  deleteAdminQuestion,
  deleteAdminQuestionBankSet,
  deleteAdminReview,
  deleteAdminReviewReply,
  deleteAdminUser,
  deleteAdminVoucher,
  fetchAdminCourses,
  fetchAdminCourseCategories,
  fetchAdminOjCategories,
  fetchAdminOjProblem,
  fetchAdminOjProblems,
  fetchAdminQuestionBankSets,
  fetchAdminQuestions,
  fetchAdminReviews,
  fetchAdminUsers,
  fetchAdminVouchers,
  saveAdminCourseCategory,
  saveAdminOjCategory,
  saveAdminCourse,
  saveAdminQuestionBankSet,
  saveAdminVoucher,
  replyAdminReview,
  updateAdminOjProblem,
  updateAdminQuestion,
  updateAdminUser,
} from '../api/admin'
import {
  algorithmCategoryOptions,
  difficultyOptions,
  formatAlgorithmCategory,
  formatDifficulty,
  statementLanguageOptions,
} from '../oj/catalog'

// 当前认证用户
const authUser = ref(getStoredAuthUser())
// 判断是否为管理员
const isAdmin = computed(() => authUser.value?.email === 'admin@admin.com' && authUser.value?.roleType === 'admin')
// 当前激活的标签页
const activeTab = ref('users')
// 加载状态
const loading = ref(false)
// 操作消息提示
const message = ref('')

// 数据列表
const users = ref([])
const courses = ref([])
const reviews = ref([])
const vouchers = ref([])
const questionSets = ref([])
const questions = ref([])
const ojProblems = ref([])
const courseCategories = ref([])
const ojCategories = ref(algorithmCategoryOptions.map((item) => ({ name: item.value })))
const selectedResourceType = ref('online-open-courses')
const selectedSetCode = ref('')
const questionBankMode = ref('course')
const newCourseCategory = ref('')
const newOjCategory = ref('')
const userSearch = ref('')
const courseSearch = ref('')
const voucherSearch = ref('')
const questionBankSearch = ref('')
const selectedQuestionSetFilter = ref('')
const selectedQuestionFilter = ref('')
const reviewUserSearch = ref('')
const reviewContentSearch = ref('')
const ojCheckResult = ref(null)
const editorOjCheckResult = ref(null)
const activeEditor = ref('')
const editingUser = ref(null)
const reviewReplies = reactive({})
const confirmDialog = reactive({
  open: false,
  title: '',
  message: '',
  confirmText: '确认',
  pending: false,
  onConfirm: null,
})

// 表单数据
const courseForm = reactive(emptyCourseForm())
const voucherForm = reactive(emptyVoucherForm())
const setForm = reactive(emptySetForm())
const questionForm = reactive(emptyQuestionForm())
const editorQuestionForm = reactive(emptyQuestionForm())
const ojForm = reactive(emptyOjForm())
const editorOjForm = reactive(emptyOjForm())
const userForm = reactive(emptyUserForm())

const choiceLabels = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']
const isChoiceQuestion = computed(() => isChoiceQuestionForm(questionForm))
const isEditorChoiceQuestion = computed(() => isChoiceQuestionForm(editorQuestionForm))
const canCheckOjCases = computed(() => canCheckOjForm(ojForm))
const canCheckEditorOjCases = computed(() => canCheckOjForm(editorOjForm))
const filteredUsers = computed(() => filterByRank(users.value, userSearch.value, [
  (user) => user.id,
  (user) => user.email,
  (user) => user.username,
  (user) => user.name,
]))
const filteredCourses = computed(() => filterByRank(courses.value, courseSearch.value, [
  (course) => course.name,
  (course) => course.id,
]))
const filteredVouchers = computed(() => filterByRank(vouchers.value, voucherSearch.value, [
  (voucher) => voucher.voucherKey,
]))
const matchedQuestionSets = computed(() => filterByRank(questionSets.value, questionBankSearch.value, [
  (set) => set.title,
  (set) => set.code,
  (set) => set.categoryName,
]))
const matchedQuestions = computed(() => filterByRank(questions.value, questionBankSearch.value, [
  (question) => question.stem,
  (question) => question.answer,
  (question) => question.type,
]))
const filteredQuestionSets = computed(() => {
  if (selectedQuestionSetFilter.value) {
    return questionSets.value.filter((set) => set.code === selectedQuestionSetFilter.value)
  }
  return matchedQuestionSets.value
})
const filteredQuestions = computed(() => {
  if (selectedQuestionFilter.value) {
    return questions.value.filter((question) => String(question.id) === String(selectedQuestionFilter.value))
  }
  return matchedQuestions.value
})
const filteredOjProblems = computed(() => filterByRank(ojProblems.value, questionBankSearch.value, [
  (problem) => problem.title,
  (problem) => problem.slug,
  (problem) => problem.category,
  (problem) => problem.tags,
]))
const courseReviews = computed(() => reviews.value.filter((review) => normalizeText(review.reviewType || 'course') === 'course'))
const textbookReviews = computed(() => reviews.value.filter((review) => normalizeText(review.reviewType) === 'textbook'))
const filteredCourseReviews = computed(() => filterReviews(courseReviews.value))
const filteredTextbookReviews = computed(() => filterReviews(textbookReviews.value))
const ojDifficultyValues = difficultyOptions.map((item) => item.value)
const ojMetaCategories = computed(() => [
  ...ojCategories.value.filter((item) => !ojDifficultyValues.includes(item.name) && !statementLanguageOptions.some((language) => language.value === item.name)),
  ...difficultyOptions.map((item) => ({ name: item.value })),
  ...statementLanguageOptions.map((item) => ({ name: item.value })),
])

// 标签页配置
const tabs = [
  { key: 'users', label: '用户管理' },
  { key: 'courses', label: '课程管理' },
  { key: 'vouchers', label: '卡券管理' },
  { key: 'question-bank', label: '题库管理' },
  { key: 'reviews', label: '评论管理' },
]

// 资源类型配置
const resourceTypes = [
  { value: 'online-open-courses', label: '在线开放课程' },
  { value: 'general-courses', label: '通识课程' },
  { value: 'micro-major-courses', label: '微专业课程' },
]

// 通用任务执行函数，统一处理加载状态和错误提示
function normalizeText(value) {
  return String(value ?? '').trim().toLowerCase()
}

function includesText(value, keyword) {
  return normalizeText(value).includes(keyword)
}

function filterByRank(items, keyword, accessors) {
  const term = normalizeText(keyword)
  if (!term) return items
  return items
    .map((item) => ({
      item,
      rank: accessors.findIndex((accessor) => includesText(accessor(item), term)),
    }))
    .filter((entry) => entry.rank >= 0)
    .sort((left, right) => left.rank - right.rank)
    .map((entry) => entry.item)
}

function filterReviews(items) {
  const userTerm = normalizeText(reviewUserSearch.value)
  const contentTerm = normalizeText(reviewContentSearch.value)
  return items.filter((review) => {
    const userMatched = !userTerm || [
      review.userName,
      review.userId,
      review.id,
      review.userEmail,
    ].some((value) => includesText(value, userTerm))
    const contentMatched = !contentTerm || includesText(review.content, contentTerm) || includesText(review.replyContent, contentTerm)
    return userMatched && contentMatched
  })
}

function selectedOjCategories(form) {
  return uniqueOjTags([form.category, form.difficulty, ...parseOjTags(form.tags)])
}

function updateOjCategories(form, event) {
  const values = Array.from(event.target.selectedOptions).map((option) => option.value)
  writeOjTags(form, values)
  const algorithmCategory = values.find((value) => !ojDifficultyValues.includes(value) && !statementLanguageOptions.some((item) => item.value === value))
  const difficultyCategory = values.find((value) => ojDifficultyValues.includes(value))
  form.category = algorithmCategory || ''
  form.difficulty = difficultyCategory || form.difficulty || 'EASY'
}

async function handleQuestionSetFilterChange() {
  if (!selectedQuestionSetFilter.value) return
  selectedSetCode.value = selectedQuestionSetFilter.value
  selectedQuestionFilter.value = ''
  await loadQuestions()
}

function reviewKey(review) {
  return `${review.reviewType || 'course'}-${review.id}`
}

function reviewTarget(review) {
  return review.targetId || review.courseId || review.textbookId || ''
}

function reviewTypeLabel(type) {
  return normalizeText(type) === 'textbook' ? '教材' : '课程'
}

function roleLabel(role) {
  const normalized = normalizeText(role)
  if (normalized === 'teacher') return '教师'
  if (normalized === 'admin') return '管理员'
  return ''
}

async function runTask(task, successText = '操作已完成') {
  loading.value = true
  message.value = ''
  try {
    await task()
    message.value = successText
  } catch (error) {
    message.value = error?.message || '操作失败'
  } finally {
    loading.value = false
  }
}

// 加载用户列表
async function loadUsers() {
  users.value = await fetchAdminUsers()
}

// 加载课程列表
async function loadCourses() {
  const [courseData, categoryData] = await Promise.all([
    fetchAdminCourses(selectedResourceType.value),
    fetchAdminCourseCategories(selectedResourceType.value),
  ])
  courses.value = courseData
  courseCategories.value = categoryData
}

// 加载评论列表
async function loadReviews() {
  reviews.value = await fetchAdminReviews()
}

// 加载卡券列表
async function loadVouchers() {
  vouchers.value = await fetchAdminVouchers()
}

// 加载题库列表
async function loadQuestionSets() {
  questionSets.value = await fetchAdminQuestionBankSets()
  if (!selectedSetCode.value && questionSets.value.length > 0) {
    selectedSetCode.value = questionSets.value[0].code
  }
  if (selectedSetCode.value) {
    await loadQuestions()
  }
}

// 加载题目列表
async function loadQuestions() {
  if (!selectedSetCode.value) {
    questions.value = []
    return
  }
  questions.value = await fetchAdminQuestions(selectedSetCode.value)
}

async function loadOjProblems() {
  const [problemData, categoryData] = await Promise.all([
    fetchAdminOjProblems(),
    fetchAdminOjCategories(),
  ])
  ojProblems.value = problemData
  ojCategories.value = categoryData.length > 0 ? categoryData : algorithmCategoryOptions.map((item) => ({ name: item.value }))
  ensureOjFormCategory(ojForm)
  ensureOjFormCategory(editorOjForm)
}

function ensureOjFormCategory(form) {
  const validCategories = ojCategories.value.map((item) => item.name)
  const tags = parseOjTags(form.tags).filter((tag) => validCategories.includes(tag))
  if (!form.category && tags.length > 0) {
    form.category = tags[0]
  }
  if ((!form.category || !validCategories.includes(form.category)) && ojCategories.value.length > 0) {
    form.category = ojCategories.value[0].name
  }
  syncOjCategory(form)
}

function parseOjTags(tags) {
  return String(tags || '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function uniqueOjTags(values) {
  return Array.from(new Set(values.filter(Boolean)))
}

function writeOjTags(form, values) {
  form.tags = uniqueOjTags(values).join(',')
}

function syncOjCategory(form) {
  if (!form.category) return
  const tags = parseOjTags(form.tags)
  if (!tags.includes(form.category)) {
    writeOjTags(form, [form.category, ...tags])
  }
}

function normalizedOjTagsForPayload(form) {
  const tags = parseOjTags(form.tags)
  return uniqueOjTags([form.category, form.difficulty, ...tags]).join(',')
}

function formatOjCategoryName(value) {
  const difficulty = difficultyOptions.find((item) => item.value === value)
  if (difficulty) return `${difficulty.label} ${difficulty.labelEn}`
  const language = statementLanguageOptions.find((item) => item.value === value)
  if (language) return `${language.label} ${language.labelEn}`
  return formatAlgorithmCategory(value || '')
}

// 编辑用户
function editUser(user) {
  editingUser.value = user
  Object.assign(userForm, {
    id: user.id,
    username: user.username || '',
    email: user.email || '',
    roleType: user.roleType || 'student',
    teacherName: user.teacherName || '',
    school: user.school || '',
    coinTotal: Number(user.coinTotal ?? 0),
    password: '',
    dataNote: user.dataNote || '',
  })
  activeEditor.value = 'user'
}

// 取消编辑用户
function cancelUserEdit() {
  closeEditor()
}

// 保存用户信息
async function saveUser() {
  if (editingUser.value?.roleType === 'admin' && !userForm.password.trim()) {
    message.value = '管理员账号只能修改密码，请填写新密码'
    return
  }
  await runTask(async () => {
    await updateAdminUser(userForm.id, { ...userForm })
    await loadUsers()
    closeEditor()
  }, '用户信息已保存')
}

// 删除用户
async function removeUser(user) {
  openConfirm({
    title: '删除用户',
    message: `确认删除用户 ${user.username}？`,
    confirmText: '删除',
    onConfirm: async () => {
      await runTask(async () => {
        await deleteAdminUser(user.id)
        await loadUsers()
      }, '用户已删除')
    },
  })
}

// 填充课程表单
function fillCourse(course) {
  Object.assign(courseForm, {
    resourceType: selectedResourceType.value,
    id: course.id || '',
    name: course.name || '',
    teacher: course.teacher || '',
    category: course.category || '',
    school: course.school || '',
    coverUrl: '',
    coverFilePath: course.coverFilePath || '',
    startTime: course.startTime || '',
    participants: course.participants || 0,
    comment: course.comment || '',
    description: course.description || '',
    semesterPlan: course.semesterPlan || '',
    overview: course.overview || '',
    videoFilePath: course.videoFilePath || '',
    link: course.link || '',
    certificationLabel: course.certificationLabel || (course.certified ? '已认证' : ''),
  })
  activeEditor.value = 'course'
}

// 重置课程表单
function resetCourseForm() {
  Object.assign(courseForm, emptyCourseForm(), { resourceType: selectedResourceType.value })
}

// 提交课程表单
async function submitCourse() {
  await runTask(async () => {
    await saveAdminCourse({
      ...courseForm,
      resourceType: selectedResourceType.value,
      certified: Boolean(courseForm.certificationLabel?.trim()),
    })
    resetCourseForm()
    await loadCourses()
    closeEditor()
  }, '课程已保存')
}

// 删除课程
async function removeCourse(course) {
  openConfirm({
    title: '删除课程',
    message: `确认删除课程 ${course.name}？`,
    confirmText: '删除',
    onConfirm: async () => {
      await runTask(async () => {
        await deleteAdminCourse(selectedResourceType.value, course.id)
        await loadCourses()
      }, '课程已删除')
    },
  })
}

async function addCourseCategory() {
  const name = newCourseCategory.value.trim()
  if (!name) {
    message.value = '分类名称不能为空'
    return
  }
  await runTask(async () => {
    courseCategories.value = await saveAdminCourseCategory({
      resourceType: selectedResourceType.value,
      name,
    })
    newCourseCategory.value = ''
  }, '分类已保存')
}

async function removeCourseCategory(category) {
  openConfirm({
    title: '删除分类',
    message: `确认删除分类 ${category.name}？已使用该分类的课程不会被删除。`,
    confirmText: '删除',
    onConfirm: async () => {
      await runTask(async () => {
        courseCategories.value = await deleteAdminCourseCategory(selectedResourceType.value, category.name)
        if (courseForm.category === category.name) {
          courseForm.category = ''
        }
      }, '分类已删除')
    },
  })
}

async function addOjCategory() {
  const name = newOjCategory.value.trim()
  if (!name) {
    message.value = '请填写OJ分类名称'
    return
  }
  await runTask(async () => {
    ojCategories.value = await saveAdminOjCategory({ name })
    newOjCategory.value = ''
    ensureOjFormCategory(ojForm)
    ensureOjFormCategory(editorOjForm)
  }, 'OJ分类已添加')
}

async function removeOjCategory(category) {
  openConfirm({
    title: '删除OJ分类',
    message: '确认删除OJ分类 ' + category.name + '？已使用该分类的题目不会被删除。',
    confirmText: '删除',
    onConfirm: async () => {
      await runTask(async () => {
        ojCategories.value = await deleteAdminOjCategory(category.name)
        if (ojForm.category === category.name) {
          ojForm.category = ''
          ensureOjFormCategory(ojForm)
        }
        if (editorOjForm.category === category.name) {
          editorOjForm.category = ''
          ensureOjFormCategory(editorOjForm)
        }
      }, 'OJ分类已删除')
    },
  })
}

// 填充题库表单
function fillSet(set) {
  Object.assign(setForm, {
    categoryCode: set.categoryCode || '',
    categoryName: set.categoryName || '',
    categoryDescription: '',
    code: set.code || '',
    title: set.title || '',
    subtitle: set.subtitle || '',
    description: set.description || '',
    coverUrl: '',
    coverFilePath: set.coverFilePath || '',
    difficultyLabel: set.difficultyLabel || '',
    statusLabel: set.statusLabel || '',
    sourceName: set.sourceName || '',
    sourceUrl: set.sourceUrl || '',
    sourceRefsText: (set.sourceRefs || []).join('\n'),
    routePath: set.routePath || `/academy/question-bank/courses/${set.code}`,
    sortOrder: 0,
  })
  selectedSetCode.value = set.code
  activeEditor.value = 'set'
  loadQuestions()
}

// 重置题库表单
function resetSetForm() {
  Object.assign(setForm, emptySetForm())
}

// 提交题库表单
async function submitSet() {
  await runTask(async () => {
    await saveAdminQuestionBankSet({
      ...setForm,
      sourceRefs: splitLines(setForm.sourceRefsText),
    })
    selectedSetCode.value = setForm.code
    resetSetForm()
    await loadQuestionSets()
    closeEditor()
  }, '题库已保存')
}

// 删除题库
async function removeSet(set) {
  openConfirm({
    title: '删除题库',
    message: `确认删除题库 ${set.title}？题目会一起删除。`,
    confirmText: '删除',
    onConfirm: async () => {
      await runTask(async () => {
        await deleteAdminQuestionBankSet(set.code)
        selectedSetCode.value = ''
        await loadQuestionSets()
      }, '题库已删除')
    },
  })
}

// 填充题目表单
function fillQuestion(question) {
  Object.assign(editorQuestionForm, questionToForm(question))
  activeEditor.value = 'question'
}

// 重置题目表单
function resetQuestionForm() {
  Object.assign(questionForm, emptyQuestionForm(), { setCode: selectedSetCode.value })
}

function handleQuestionTypeChange() {
  normalizeQuestionOptions(questionForm)
}

function handleEditorQuestionTypeChange() {
  normalizeQuestionOptions(editorQuestionForm)
}

function normalizeQuestionOptions(form) {
  if (isChoiceQuestionForm(form) && form.options.length === 0) {
    form.options = ['']
  }
  if (!isChoiceQuestionForm(form)) {
    form.options = []
  }
}

function addChoiceOption(form = questionForm) {
  if (form.options.length < choiceLabels.length) {
    form.options.push('')
  }
}

function removeChoiceOption(index, form = questionForm) {
  if (form.options.length <= 1) {
    form.options = ['']
    return
  }
  form.options.splice(index, 1)
}

async function submitQuestion() {
  await runTask(async () => {
    await createAdminQuestion(questionPayload(questionForm, selectedSetCode.value))
    resetQuestionForm()
    await loadQuestions()
  }, '题目已保存')
}

async function submitEditorQuestion() {
  await runTask(async () => {
    await updateAdminQuestion(editorQuestionForm.id, questionPayload(editorQuestionForm, editorQuestionForm.setCode || selectedSetCode.value))
    await loadQuestions()
    closeEditor()
  }, '题目已保存')
}

// 删除题目
async function removeQuestion(question) {
  openConfirm({
    title: '删除题目',
    message: '确认删除这道题？',
    confirmText: '删除',
    onConfirm: async () => {
      await runTask(async () => {
        await deleteAdminQuestion(question.id)
        await loadQuestions()
      }, '题目已删除')
    },
  })
}

// 删除评论
async function removeReview(review) {
  openConfirm({
    title: '删除评论',
    message: '确认删除这条评论？',
    confirmText: '删除',
    onConfirm: async () => {
      await runTask(async () => {
        await deleteAdminReview(review.reviewType || 'course', review.id)
        await loadReviews()
      }, '评论已删除')
    },
  })
}

async function submitReviewReply(review) {
  const key = reviewKey(review)
  const content = String(reviewReplies[key] || '').trim()
  if (!content) {
    message.value = '请先填写回复内容'
    return
  }
  await runTask(async () => {
    await replyAdminReview(review.reviewType || 'course', review.id, { content })
    reviewReplies[key] = ''
    await loadReviews()
  }, '评论回复已保存')
}

async function removeReviewReply(review) {
  openConfirm({
    title: '删除回复',
    message: '确认删除这条评论下的管理员回复？',
    confirmText: '删除回复',
    onConfirm: async () => {
      await runTask(async () => {
        await deleteAdminReviewReply(review.reviewType || 'course', review.id)
        await loadReviews()
      }, '评论回复已删除')
    },
  })
}

// 填充卡券表单
async function fillOjProblem(problem) {
  const detail = await fetchAdminOjProblem(problem.id)
  Object.assign(editorOjForm, ojDetailToForm(detail))
  ensureOjFormCategory(editorOjForm)
  editorOjCheckResult.value = null
  activeEditor.value = 'oj'
}

function resetOjForm() {
  Object.assign(ojForm, emptyOjForm())
  ensureOjFormCategory(ojForm)
  ojCheckResult.value = null
}

function addOjCase(form = ojForm, resultRef = ojCheckResult) {
  form.testCases.push(emptyOjCase(form.testCases.length + 1))
  resultRef.value = null
}

function removeOjCase(index, form = ojForm, resultRef = ojCheckResult) {
  openConfirm({
    title: 'Delete test case',
    message: `Delete test case ${index + 1}?`,
    confirmText: 'Delete',
    onConfirm: async () => {
      if (form.testCases.length <= 1) {
        form.testCases = [emptyOjCase()]
      } else {
        form.testCases.splice(index, 1)
      }
      resultRef.value = null
      message.value = '测试点已删除'
    },
  })
}

function ojPayload(form = ojForm) {
  const tags = normalizedOjTagsForPayload(form)
  return {
    id: form.id,
    title: form.title,
    slug: form.slug,
    category: form.category || parseOjTags(tags)[0] || '',
    description: form.description,
    inputDescription: form.inputDescription,
    outputDescription: form.outputDescription,
    standardCode: String(form.standardCode || ''),
    difficulty: form.difficulty,
    timeLimitMs: form.timeLimitMs,
    memoryLimitKb: form.memoryLimitKb,
    tags,
    status: form.status,
    testCases: form.testCases.map((item, index) => ({
      id: item.id || null,
      inputData: item.inputData,
      expectedOutput: item.expectedOutput,
      sample: Boolean(item.sample),
      weight: Number(item.weight || 1),
      sortOrder: index + 1,
    })),
  }
}

function formatActualOutput(caseResult) {
  const output = caseResult?.actualOutput
  if (output === null || output === undefined) {
    return '实际输出：<沙箱未返回 actualOutput 字段>'
  }
  if (String(output).length === 0) {
    return '实际输出：<程序输出为空>'
  }
  return `实际输出：\n${output}`
}

async function checkOjCases() {
  await checkOjCasesFor(ojForm, ojCheckResult)
}

async function checkEditorOjCases() {
  await checkOjCasesFor(editorOjForm, editorOjCheckResult)
}

async function checkOjCasesFor(form, resultRef) {
  if (!form.standardCode?.trim()) {
    message.value = '请先填写标准代码后再校验测试点'
    return
  }
  await runTask(async () => {
    resultRef.value = await checkAdminOjProblem(ojPayload(form))
    let filledCount = 0
    resultRef.value?.cases?.forEach((item, index) => {
      if (!form.testCases[index]?.expectedOutput?.trim() && item.actualOutput !== null && item.actualOutput !== undefined) {
        form.testCases[index].expectedOutput = item.actualOutput
        filledCount += 1
      }
    })
    if (filledCount > 0) {
      resultRef.value = {
        ...resultRef.value,
        message: (resultRef.value?.message || '校验完成') + '，已自动填充 ' + filledCount + ' 个输出样例',
      }
    }
  }, 'OJ测试点已校验，请查看下方结果')
}

async function submitOjProblem() {
  await runTask(async () => {
    await createAdminOjProblem(ojPayload(ojForm))
    resetOjForm()
    await loadOjProblems()
  }, 'OJ题目已保存')
}

async function submitEditorOjProblem() {
  await runTask(async () => {
    const saved = await updateAdminOjProblem(editorOjForm.id, ojPayload(editorOjForm))
    Object.assign(editorOjForm, ojDetailToForm(saved))
    await loadOjProblems()
    closeEditor()
  }, 'OJ题目已保存')
}

async function removeOjProblem(problem) {
  openConfirm({
    title: '删除OJ题目',
    message: `确认删除 OJ 题目 ${problem.title}？`,
    confirmText: '删除',
    onConfirm: async () => {
      await runTask(async () => {
        await deleteAdminOjProblem(problem.id)
        if (editorOjForm.id === problem.id) {
          Object.assign(editorOjForm, emptyOjForm())
          editorOjCheckResult.value = null
          closeEditor()
        }
        await loadOjProblems()
      }, 'OJ题目已删除')
    },
  })
}

function fillVoucher(voucher) {
  Object.assign(voucherForm, {
    voucherKey: voucher.voucherKey || '',
    voucherType: voucher.voucherType || 'DISCOUNT',
    name: voucher.name || '',
    description: voucher.description || '',
    price: voucher.price || 0,
    stockQuantity: voucher.stockQuantity ?? 0,
    unlimitedStock: Boolean(voucher.unlimitedStock),
    discountType: voucher.discountType || 'AMOUNT',
    thresholdAmount: voucher.thresholdAmount ?? 0,
    discountAmount: voucher.discountAmount ?? 0,
    discountRate: voucher.discountRate ?? 0.9,
    maxDiscountAmount: voucher.maxDiscountAmount ?? 0,
    validFrom: toInputDateTime(voucher.validFrom),
    validUntil: toInputDateTime(voucher.validUntil),
    enabled: Boolean(voucher.enabled),
    sortOrder: voucher.sortOrder || 0,
  })
  activeEditor.value = 'voucher'
}

// 重置卡券表单
function resetVoucherForm() {
  Object.assign(voucherForm, emptyVoucherForm())
}

// 提交卡券表单
async function submitVoucher() {
  await runTask(async () => {
    const isGameItem = voucherForm.voucherType === 'GAME_ITEM'
    await saveAdminVoucher({
      ...voucherForm,
      discountType: isGameItem ? 'NONE' : voucherForm.discountType,
      validFrom: voucherForm.validFrom || null,
      validUntil: voucherForm.validUntil || null,
      stockQuantity: voucherForm.unlimitedStock ? null : voucherForm.stockQuantity,
      discountRate: !isGameItem && voucherForm.discountType === 'PERCENT' ? voucherForm.discountRate : null,
      discountAmount: !isGameItem && voucherForm.discountType === 'AMOUNT' ? voucherForm.discountAmount : null,
    })
    resetVoucherForm()
    await loadVouchers()
    closeEditor()
  }, '卡券已上架')
}

// 删除卡券
async function removeVoucher(voucher) {
  openConfirm({
    title: '下架卡券',
    message: `确认下架 ${voucher.name}？`,
    confirmText: '下架',
    onConfirm: async () => {
      await runTask(async () => {
        await deleteAdminVoucher(voucher.voucherKey)
        await loadVouchers()
      }, '卡券已下架')
    },
  })
}

function openConfirm({ title, message: confirmMessage, confirmText = '确认', onConfirm }) {
  Object.assign(confirmDialog, {
    open: true,
    title,
    message: confirmMessage,
    confirmText,
    pending: false,
    onConfirm,
  })
}

function closeConfirm() {
  if (confirmDialog.pending) return
  Object.assign(confirmDialog, {
    open: false,
    title: '',
    message: '',
    confirmText: '确认',
    pending: false,
    onConfirm: null,
  })
}

async function handleConfirm() {
  if (!confirmDialog.onConfirm) return
  confirmDialog.pending = true
  try {
    await confirmDialog.onConfirm()
    confirmDialog.pending = false
    closeConfirm()
  } finally {
    confirmDialog.pending = false
  }
}

function closeEditor() {
  activeEditor.value = ''
  editingUser.value = null
}

function requestCloseEditor() {
  if (!activeEditor.value) return
  openConfirm({
    title: '关闭编辑',
    message: '确认关闭当前编辑弹窗？未保存的修改会丢失。',
    confirmText: '关闭',
    onConfirm: async () => {
      closeEditor()
    },
  })
}

// 将多行文本分割为数组
function splitLines(value) {
  return String(value || '')
    .split('\n')
    .map((item) => item.trim())
    .filter(Boolean)
}

function isChoiceQuestionForm(form) {
  return ['single', 'multiple'].includes(form.type)
}

function canCheckOjForm(form) {
  return Boolean(form.standardCode?.trim()) && !loading.value
}

function questionToForm(question) {
  const options = question.options || []
  return {
    id: question.id || null,
    setCode: selectedSetCode.value,
    type: question.type || 'single',
    stem: question.stem || '',
    options: options.length ? [...options] : [''],
    optionsText: options.join('\n'),
    answer: question.answer || '',
    explanation: question.explanation || '',
    difficultyLabel: question.difficultyLabel || '',
    sourceUrl: question.sourceUrl || '',
    sortOrder: question.sortOrder || 0,
  }
}

function questionPayload(form, setCode) {
  return {
    setCode,
    type: form.type,
    stem: form.stem,
    options: isChoiceQuestionForm(form) ? form.options.map((item) => item.trim()).filter(Boolean) : splitLines(form.optionsText),
    answer: form.answer,
    explanation: form.explanation,
    difficultyLabel: form.difficultyLabel,
    sourceUrl: form.sourceUrl,
    sortOrder: form.sortOrder,
  }
}

function ojDetailToForm(detail) {
  const form = {
    ...emptyOjForm(),
    id: detail.id || null,
    title: detail.title || '',
    slug: detail.slug || '',
    category: detail.category || '',
    description: detail.description || '',
    inputDescription: detail.inputDescription || '',
    outputDescription: detail.outputDescription || '',
    standardCode: detail.standardCode || '',
    difficulty: detail.difficulty || 'EASY',
    timeLimitMs: detail.timeLimitMs || 1000,
    memoryLimitKb: detail.memoryLimitKb || 262144,
    tags: detail.tags || '',
    status: detail.status || 'DRAFT',
    testCases: (detail.testCases || []).map(normalizeOjCase),
  }
  if (form.testCases.length === 0) {
    form.testCases.push(emptyOjCase())
  }
  return form
}

// 创建空的课程表单
function emptyCourseForm() {
  return {
    resourceType: selectedResourceType.value,
    id: '',
    name: '',
    teacher: '',
    category: '',
    school: '',
    coverUrl: '',
    coverFilePath: '',
    startTime: '',
    participants: 0,
    comment: '',
    description: '',
    semesterPlan: '',
    overview: '',
    videoFilePath: '',
    link: '',
    certified: false,
    certificationLabel: '',
  }
}

function emptyUserForm() {
  return {
    id: null,
    username: '',
    email: '',
    roleType: 'student',
    teacherName: '',
    school: '',
    coinTotal: 0,
    password: '',
    dataNote: '',
  }
}

// 创建空的题库表单
function emptySetForm() {
  return {
    categoryCode: '',
    categoryName: '',
    categoryDescription: '',
    code: '',
    title: '',
    subtitle: '',
    description: '',
    coverUrl: '',
    coverFilePath: '',
    difficultyLabel: '',
    statusLabel: '',
    sourceName: '',
    sourceUrl: '',
    sourceRefsText: '',
    routePath: '',
    sortOrder: 0,
  }
}

// 创建空的题目表单
function emptyQuestionForm() {
  return {
    id: null,
    setCode: '',
    type: 'single',
    stem: '',
    options: [''],
    optionsText: '',
    answer: '',
    explanation: '',
    difficultyLabel: '',
    sourceUrl: '',
    sortOrder: 0,
  }
}

// 创建空的卡券表单
function emptyOjCase(sortOrder = 1) {
  return {
    id: null,
    inputData: '',
    expectedOutput: '',
    sample: sortOrder === 1,
    weight: 1,
    sortOrder,
  }
}

function normalizeOjCase(item, index = 0) {
  return {
    id: item.id || null,
    inputData: item.inputData || '',
    expectedOutput: item.expectedOutput || '',
    sample: Boolean(item.sample),
    weight: item.weight || 1,
    sortOrder: item.sortOrder || index + 1,
  }
}

function emptyOjForm() {
  return {
    id: null,
    title: '',
    slug: '',
    category: '',
    description: '',
    inputDescription: '',
    outputDescription: '',
    standardCode: '',
    difficulty: 'EASY',
    timeLimitMs: 1000,
    memoryLimitKb: 262144,
    tags: '',
    status: 'DRAFT',
    testCases: [emptyOjCase()],
  }
}

function emptyVoucherForm() {
  return {
    voucherKey: '',
    voucherType: 'DISCOUNT',
    name: '',
    description: '',
    price: 0,
    stockQuantity: 100,
    unlimitedStock: true,
    discountType: 'AMOUNT',
    thresholdAmount: 0,
    discountAmount: 0,
    discountRate: 0.9,
    maxDiscountAmount: 0,
    validFrom: '',
    validUntil: '',
    enabled: true,
    sortOrder: 0,
  }
}

// 将日期时间转换为输入框格式
function toInputDateTime(value) {
  return value ? String(value).slice(0, 16) : ''
}

// 格式化卡券摘要信息
function formatVoucherSummary(voucher) {
  const stock = voucher.unlimitedStock ? '不限量' : `${voucher.stockQuantity ?? 0} 张`
  const enabled = voucher.enabled ? '上架中' : '已下架'
  return `${voucher.voucherType === 'GAME_ITEM' ? '游戏券' : '优惠券'} · 库存 ${stock} · ${enabled}`
}

// 页面挂载时加载所有管理员数据
onMounted(async () => {
  if (!isAdmin.value) return
  await runTask(async () => {
    await Promise.all([loadUsers(), loadCourses(), loadQuestionSets(), loadOjProblems(), loadReviews(), loadVouchers()])
  }, '管理员数据已加载')
})
</script>

<template>
  <!-- 管理员后台主布局 -->
  <main class="admin-page">
    <section class="admin-shell">
      <!-- 管理员后台头部 -->
      <header class="admin-header">
        <div>
          <p>admin console</p>
          <h1>管理员后台</h1>
        </div>
        <span>{{ authUser?.email || '未登录' }}</span>
      </header>

      <!-- 权限验证提示 -->
      <div v-if="!isAdmin" class="admin-empty">
        <h2>需要管理员权限</h2>
        <p>请使用 admin@admin.com 登录。</p>
      </div>

      <!-- 管理员操作区域 -->
      <template v-else>
        <!-- 标签页导航 -->
        <nav class="admin-tabs">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            type="button"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </nav>

          <!-- 操作消息提示 -->
          <p v-if="message" class="admin-message">{{ message }}</p>

          <!-- 用户管理面板 -->
          <section v-if="activeTab === 'users'" class="admin-panel">
            <h2>用户管理</h2>
            <div class="admin-search-row">
              <input v-model="userSearch" placeholder="搜索用户序号、邮箱或名称" />
            </div>
            <div class="admin-table">
              <article v-for="user in filteredUsers" :key="user.id" class="admin-row">
                <strong>{{ user.username }}</strong>
                <span>{{ user.email }}</span>
                <span>{{ user.roleType === 'teacher' ? '教师' : user.roleType === 'admin' ? '管理员' : '学生' }}</span>
                <span>金币 {{ Number(user.coinTotal ?? 0).toLocaleString('zh-CN') }}</span>
                <span>{{ user.school || '未填写学校' }}</span>
                <div class="admin-actions">
                  <button type="button" class="admin-action-edit" @click="editUser(user)">
                    {{ user.roleType === 'admin' ? '改密' : '编辑' }}
                    <AdminLockIcon v-if="user.roleType === 'admin'" class="admin-button-icon" />
                    <AdminEditIcon v-else class="admin-button-icon" />
                  </button>
                  <button
                    type="button"
                    class="admin-action-delete"
                    :disabled="user.roleType === 'admin'"
                    @click="removeUser(user)"
                  >
                    删除
                    <AdminDeleteIcon class="admin-button-icon" />
                  </button>
                </div>
            </article>
          </div>
          </section>

          <!-- 课程管理面板 -->
          <section v-if="activeTab === 'courses'" class="admin-panel">
            <div class="admin-panel-head">
              <h2>课程管理</h2>
            <select v-model="selectedResourceType" @change="loadCourses(); resetCourseForm()">
              <option v-for="item in resourceTypes" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
            </div>

            <section class="admin-category-manager">
              <div class="admin-category-head">
                <strong>课程分类管理</strong>
                <form class="admin-category-form" @submit.prevent="addCourseCategory">
                  <input v-model="newCourseCategory" placeholder="新增分类名称" />
                  <button type="submit" :disabled="loading">添加分类</button>
                </form>
              </div>
              <div class="admin-category-tags">
                <span v-for="category in courseCategories" :key="category.name">
                  {{ category.name }}
                  <button type="button" aria-label="删除分类" @click="removeCourseCategory(category)">×</button>
                </span>
              </div>
            </section>

            <!-- 课程表单 -->
            <form class="admin-form" @submit.prevent="submitCourse">
              <input v-model="courseForm.id" placeholder="课程编号" />
            <input v-model="courseForm.name" placeholder="课程名称" />
            <input v-model="courseForm.teacher" placeholder="教师" />
            <select v-model="courseForm.category">
              <option value="">请选择分类</option>
              <option v-for="category in courseCategories" :key="category.name" :value="category.name">
                {{ category.name }}
              </option>
            </select>
            <input v-model="courseForm.school" placeholder="学校" />
            <input v-model="courseForm.startTime" placeholder="开课时间" />
            <label class="admin-field">
              <span>参与人数</span>
              <input v-model.number="courseForm.participants" type="number" min="0" placeholder="例如 1200" />
            </label>
            <input v-model="courseForm.certificationLabel" placeholder="认证标签，清空即删除" />
            <textarea v-model="courseForm.comment" placeholder="课程概述"></textarea>
            <textarea v-model="courseForm.description" placeholder="课程详情"></textarea>
            <label class="admin-field">
              <span>封面本地路径</span>
              <input v-model="courseForm.coverFilePath" placeholder="storage/... 或 teacher_courses/..." />
            </label>
            <input v-model="courseForm.videoFilePath" placeholder="视频本地路径" />
            <input v-model="courseForm.link" placeholder="链接" />
            <div class="admin-actions">
              <button type="submit" :disabled="loading">保存课程</button>
              <button type="button" @click="resetCourseForm">清空</button>
            </div>
          </form>

            <div class="admin-search-row">
              <input v-model="courseSearch" placeholder="搜索课程名称或编号" />
            </div>
            <!-- 课程列表 -->
            <div class="admin-card-grid">
              <article v-for="course in filteredCourses" :key="course.id" class="admin-card">
                <strong>{{ course.name }}</strong>
              <span>{{ course.id }} · {{ course.category }}</span>
              <em>{{ course.certificationLabel || '无认证标签' }}</em>
              <div class="admin-actions">
                <button type="button" class="admin-action-edit" @click="fillCourse(course)">
                  编辑
                  <AdminEditIcon class="admin-button-icon" />
                </button>
                <button type="button" class="admin-action-delete" @click="removeCourse(course)">
                  删除
                  <AdminDeleteIcon class="admin-button-icon" />
                </button>
              </div>
            </article>
          </div>
          </section>

          <!-- 卡券管理面板 -->
          <section v-if="activeTab === 'vouchers'" class="admin-panel">
            <h2>卡券管理</h2>

            <!-- 卡券表单 -->
            <form class="admin-form admin-voucher-form" @submit.prevent="submitVoucher">
            <input v-model="voucherForm.voucherKey" placeholder="卡券编号，例如 coupon-textbook-80-15" />
            <select v-model="voucherForm.voucherType">
              <option value="DISCOUNT">优惠券</option>
              <option value="GAME_ITEM">游戏券</option>
            </select>
            <input v-model="voucherForm.name" placeholder="卡券名称" />
            <label class="admin-field">
              <span>兑换金币价格</span>
              <input v-model.number="voucherForm.price" type="number" min="0" placeholder="例如 200" />
            </label>
            <textarea v-model="voucherForm.description" placeholder="卡券说明"></textarea>
            <label class="admin-check"><input v-model="voucherForm.enabled" type="checkbox" /> 上架启用</label>
            <label class="admin-check"><input v-model="voucherForm.unlimitedStock" type="checkbox" /> 无限库存</label>
            <label class="admin-field">
              <span>库存数量</span>
              <input
                v-model.number="voucherForm.stockQuantity"
                type="number"
                min="0"
                placeholder="例如 100"
                :disabled="voucherForm.unlimitedStock"
              />
            </label>

            <select v-model="voucherForm.discountType" :disabled="voucherForm.voucherType === 'GAME_ITEM'">
              <option value="NONE">无折扣</option>
              <option value="AMOUNT">满减</option>
              <option value="PERCENT">折扣</option>
            </select>
            <label class="admin-field">
              <span>使用门槛金额</span>
              <input v-model.number="voucherForm.thresholdAmount" type="number" min="0" step="0.01" placeholder="例如 80" />
            </label>
            <label v-if="voucherForm.discountType === 'AMOUNT'" class="admin-field">
              <span>减免金额</span>
              <input
                v-model.number="voucherForm.discountAmount"
                type="number"
                min="0"
                step="0.01"
                placeholder="例如 15"
              />
            </label>
            <label v-else class="admin-field">
              <span>折扣力度</span>
              <input
                v-model.number="voucherForm.discountRate"
                type="number"
                min="0"
                max="1"
                step="0.01"
                placeholder="0.9 表示 9 折"
              />
            </label>
            <label class="admin-field">
              <span>减免上限</span>
              <input v-model.number="voucherForm.maxDiscountAmount" type="number" min="0" step="0.01" placeholder="0 表示不限" />
            </label>
            <input v-model="voucherForm.validFrom" type="datetime-local" placeholder="有效期开始" />
            <input v-model="voucherForm.validUntil" type="datetime-local" placeholder="有效期结束" />
            <label class="admin-field">
              <span>排序</span>
              <input v-model.number="voucherForm.sortOrder" type="number" placeholder="数字越小越靠前" />
            </label>

            <div class="admin-actions">
              <button type="submit" :disabled="loading">保存卡券</button>
              <button type="button" @click="resetVoucherForm">清空</button>
            </div>
          </form>

            <div class="admin-search-row">
              <input v-model="voucherSearch" placeholder="按卡券编号搜索" />
            </div>
            <!-- 卡券列表 -->
            <div class="admin-card-grid">
              <article v-for="voucher in filteredVouchers" :key="voucher.voucherKey" class="admin-card">
                <strong>{{ voucher.name }}</strong>
              <span>{{ voucher.voucherKey }}</span>
              <span>{{ formatVoucherSummary(voucher) }}</span>
              <span v-if="voucher.voucherType === 'DISCOUNT'">
                门槛 {{ voucher.thresholdAmount ?? 0 }} ·
                {{ voucher.discountType === 'PERCENT' ? `${Number(voucher.discountRate || 0) * 10} 折` : `减 ${voucher.discountAmount ?? 0}` }}
                · 上限 {{ voucher.maxDiscountAmount ?? '不限' }}
              </span>
              <span>有效期 {{ toInputDateTime(voucher.validFrom) || '现在' }} - {{ toInputDateTime(voucher.validUntil) || '长期' }}</span>
              <div class="admin-actions">
                <button type="button" class="admin-action-edit" @click="fillVoucher(voucher)">
                  编辑
                  <AdminEditIcon class="admin-button-icon" />
                </button>
                <button type="button" class="admin-action-delete" @click="removeVoucher(voucher)">
                  下架
                  <AdminDeleteIcon class="admin-button-icon" />
                </button>
              </div>
            </article>
          </div>
          </section>

          <!-- 题库管理面板 -->
          <section v-if="activeTab === 'question-bank'" class="admin-panel">
            <h2>题库管理</h2>
            <div class="admin-mode-switch">
              <button type="button" :class="{ active: questionBankMode === 'course' }" @click="questionBankMode = 'course'">课程题库</button>
              <button type="button" :class="{ active: questionBankMode === 'oj' }" @click="questionBankMode = 'oj'">OJ题库</button>
            </div>
            <div class="admin-search-row admin-question-search">
              <input v-model="questionBankSearch" placeholder="搜索题库或题目" />
              <select v-model="selectedQuestionSetFilter" @change="handleQuestionSetFilterChange">
                <option value="">匹配题库</option>
                <option v-for="set in matchedQuestionSets" :key="set.code" :value="set.code">{{ set.title }}</option>
              </select>
              <select v-model="selectedQuestionFilter">
                <option value="">匹配题目</option>
                <option v-for="question in matchedQuestions" :key="question.id" :value="String(question.id)">{{ question.stem }}</option>
              </select>
            </div>

            <section v-if="questionBankMode === 'oj'" class="admin-category-manager">
              <div class="admin-category-head">
                <strong>在线OJ分类同步</strong>
                <span>算法、难度、题面语言与实验平台在线OJ保持一致</span>
                <form class="admin-category-form" @submit.prevent="addOjCategory">
                  <input v-model="newOjCategory" placeholder="新增OJ分类名称" />
                  <button type="submit" :disabled="loading">添加分类</button>
                </form>
              </div>
              <div class="admin-category-tags">
                <span v-for="category in ojCategories" :key="category.name">
                  {{ formatOjCategoryName(category.name) }}
                  <button type="button" aria-label="删除OJ分类" @click="removeOjCategory(category)">×</button>
                </span>
              </div>
              <div class="admin-category-tags">
                <span v-for="item in difficultyOptions" :key="item.value">{{ item.label }} {{ item.labelEn }}</span>
              </div>
              <div class="admin-category-tags">
                <span v-for="item in statementLanguageOptions" :key="item.value">{{ item.label }} {{ item.labelEn }}</span>
              </div>
            </section>

            <div v-if="questionBankMode === 'oj'" class="admin-split admin-oj-layout">
              <form class="admin-form admin-oj-form" @submit.prevent="submitOjProblem">
                <input v-model="ojForm.title" placeholder="题目标题" />
                <input v-model="ojForm.slug" placeholder="题目标识，例如 two-sum" />
                <label class="admin-field admin-field-wide">
                  <span>OJ分类（可多选）</span>
                  <select
                    multiple
                    class="admin-multi-select"
                    :value="selectedOjCategories(ojForm)"
                    @change="updateOjCategories(ojForm, $event)"
                  >
                    <option v-for="category in ojMetaCategories" :key="category.name" :value="category.name">{{ formatOjCategoryName(category.name) }}</option>
                  </select>
                </label>
                <textarea v-model="ojForm.description" placeholder="题目描述"></textarea>
                <textarea v-model="ojForm.inputDescription" placeholder="输入描述"></textarea>
                <textarea v-model="ojForm.outputDescription" placeholder="输出描述"></textarea>
                <textarea v-model="ojForm.standardCode" class="admin-code-input" placeholder="标准代码（C++）"></textarea>
                <div class="admin-form-row">
                  <select v-model="ojForm.status">
                    <option value="DRAFT">草稿</option>
                    <option value="PUBLISHED">发布</option>
                    <option value="ARCHIVED">归档</option>
                  </select>
                  <span class="admin-oj-derived-difficulty">难度：{{ formatDifficulty(ojForm.difficulty) }}</span>
                </div>
                <div class="admin-form-row">
                  <input v-model.number="ojForm.timeLimitMs" type="number" min="100" placeholder="时间限制ms" />
                  <input v-model.number="ojForm.memoryLimitKb" type="number" min="1024" placeholder="内存限制KB" />
                </div>
                <div class="admin-oj-cases">
                  <article v-for="(testCase, index) in ojForm.testCases" :key="index" class="admin-oj-case">
                    <header>
                      <strong>测试点 {{ index + 1 }}</strong>
                      <label><input v-model="testCase.sample" type="checkbox" /> 示例</label>
                      <button type="button" aria-label="删除测试点" @click="removeOjCase(index)">×</button>
                    </header>
                    <textarea v-model="testCase.inputData" placeholder="测试点输入"></textarea>
                    <textarea v-model="testCase.expectedOutput" placeholder="测试点输出（可留空，由标准代码生成）"></textarea>
                    <input v-model.number="testCase.weight" type="number" min="1" placeholder="权重" />
                    <div v-if="ojCheckResult?.cases?.[index]" class="admin-case-result">
                      <p :class="{ danger: !ojCheckResult.cases[index].matched }">
                        {{ ojCheckResult.cases[index].message }}
                      </p>
                      <pre>{{ formatActualOutput(ojCheckResult.cases[index]) }}</pre>
                    </div>
                  </article>
                  <button type="button" class="admin-add-option" @click="addOjCase()">+</button>
                </div>
                <div class="admin-actions">
                  <button type="button" :disabled="!canCheckOjCases" @click="checkOjCases">校验测试点</button>
                  <button type="submit" :disabled="loading">保存OJ题目</button>
                  <button type="button" @click="resetOjForm">清空</button>
                </div>
                <div v-if="ojCheckResult" :class="['admin-check-result', { danger: !ojCheckResult.passed }]">
                  <strong>{{ ojCheckResult.passed ? '校验通过' : '校验存在提示' }}</strong>
                  <span>{{ ojCheckResult.message }}</span>
                </div>
              </form>
              <div class="admin-card-grid">
                <article v-for="problem in filteredOjProblems" :key="problem.id" class="admin-card">
                  <strong>{{ problem.title }}</strong>
                  <span>{{ problem.slug }} · {{ formatOjCategoryName(problem.category) || '未分类' }} · {{ formatDifficulty(problem.difficulty) }} · {{ problem.status }}</span>
                  <div class="admin-actions">
                    <button type="button" class="admin-action-edit" @click="fillOjProblem(problem)">
                      编辑
                      <AdminEditIcon class="admin-button-icon" />
                    </button>
                    <button type="button" class="admin-action-delete" @click="removeOjProblem(problem)">
                      删除
                      <AdminDeleteIcon class="admin-button-icon" />
                    </button>
                  </div>
                </article>
              </div>
            </div>

            <div v-if="questionBankMode === 'course'" class="admin-split">
              <!-- 题库表单与列表 -->
              <div>
                <form class="admin-form" @submit.prevent="submitSet">
                <input v-model="setForm.categoryCode" placeholder="分类编号" />
                <input v-model="setForm.categoryName" placeholder="分类名称" />
                <input v-model="setForm.code" placeholder="题库编号" />
                <input v-model="setForm.title" placeholder="题库名称" />
                <input v-model="setForm.subtitle" placeholder="副标题" />
                <input v-model="setForm.difficultyLabel" placeholder="难度" />
                <input v-model="setForm.statusLabel" placeholder="状态" />
                <textarea v-model="setForm.description" placeholder="题库描述"></textarea>
                <textarea v-model="setForm.sourceRefsText" placeholder="来源链接，每行一个"></textarea>
                <div class="admin-actions">
                  <button type="submit" :disabled="loading">保存题库</button>
                  <button type="button" @click="resetSetForm">清空</button>
                </div>
              </form>

                <!-- 题库列表 -->
                <div class="admin-list">
                  <button
                    v-for="set in filteredQuestionSets"
                    :key="set.code"
                    type="button"
                    :class="{ active: selectedSetCode === set.code }"
                    @click="fillSet(set)"
                  >
                    {{ set.title }} · {{ set.questionCount }}
                  </button>
                </div>
              </div>

              <!-- 题目表单与列表 -->
              <div>
                <form class="admin-form" @submit.prevent="submitQuestion">
                <select v-model="selectedSetCode" @change="loadQuestions(); resetQuestionForm()">
                  <option v-for="set in questionSets" :key="set.code" :value="set.code">{{ set.title }}</option>
                </select>
                <select v-model="questionForm.type" @change="handleQuestionTypeChange">
                  <option value="single">单选题</option>
                  <option value="multiple">多选题</option>
                  <option value="short">主观题</option>
                  <option value="vocabulary">词汇题</option>
                </select>
                <textarea v-model="questionForm.stem" placeholder="题干"></textarea>
                <div v-if="isChoiceQuestion" class="admin-choice-options">
                  <label v-for="(_, index) in questionForm.options" :key="index" class="admin-choice-row">
                    <span>{{ choiceLabels[index] }}.</span>
                    <input v-model="questionForm.options[index]" :placeholder="`${choiceLabels[index]}.待输入选项`" />
                    <button type="button" aria-label="删除选项" @click="removeChoiceOption(index)">×</button>
                  </label>
                  <button v-if="questionForm.options.length < choiceLabels.length" type="button" class="admin-add-option" @click="addChoiceOption">+</button>
                </div>
                <textarea v-else v-model="questionForm.optionsText" placeholder="选项或补充内容，每行一个"></textarea>
                <input v-model="questionForm.answer" placeholder="答案" />
                <textarea v-model="questionForm.explanation" placeholder="解析"></textarea>
                <input v-model="questionForm.difficultyLabel" placeholder="难度" />
                <div class="admin-actions">
                  <button type="submit" :disabled="loading">保存题目</button>
                  <button type="button" @click="resetQuestionForm">清空</button>
                </div>
              </form>

                <!-- 题目列表 -->
                <div class="admin-card-grid">
                  <article v-for="question in filteredQuestions" :key="question.id" class="admin-card">
                    <strong>{{ question.stem }}</strong>
                  <span>{{ question.type }} · {{ question.answer }}</span>
                  <div class="admin-actions">
                    <button type="button" class="admin-action-edit" @click="fillQuestion(question)">
                      编辑
                      <AdminEditIcon class="admin-button-icon" />
                    </button>
                    <button type="button" class="admin-action-delete" @click="removeQuestion(question)">
                      删除
                      <AdminDeleteIcon class="admin-button-icon" />
                    </button>
                  </div>
                </article>
              </div>
            </div>
          </div>
          </section>

          <!-- 评论管理面板 -->
          <section v-if="activeTab === 'reviews'" class="admin-panel">
            <h2>评论管理</h2>
            <div class="admin-search-row admin-review-search">
              <input v-model="reviewUserSearch" placeholder="搜索用户名、序号或邮箱" />
              <input v-model="reviewContentSearch" placeholder="搜索评论内容" />
            </div>

            <section class="admin-review-section">
              <h3>课程评论管理</h3>
              <div class="admin-card-grid">
                <article v-for="review in filteredCourseReviews" :key="reviewKey(review)" class="admin-card admin-review-card">
                  <strong>
                    <span class="admin-review-author">
                      {{ review.userName || '匿名用户' }}
                      <span v-if="roleLabel(review.userRoleType)" class="admin-role-badge">{{ roleLabel(review.userRoleType) }}</span>
                    </span>
                    <small>#{{ review.userId || review.id }}</small>
                    <template v-if="!review.parentReviewId">· {{ review.rating }} 星</template>
                  </strong>
                  <span>{{ reviewTypeLabel(review.reviewType) }} / {{ reviewTarget(review) }}</span>
                  <p v-if="!review.parentReviewId">{{ review.content }}</p>
                  <div v-if="review.parentReviewId" class="admin-review-reply">
                    <strong>
                      <span class="admin-review-author">
                        {{ review.userName || '匿名用户' }}
                        <span v-if="roleLabel(review.userRoleType)" class="admin-role-badge">{{ roleLabel(review.userRoleType) }}</span>
                      </span>
                      回复 @{{ review.parentUserName || '用户' }}
                    </strong>
                    <p>{{ review.content }}</p>
                  </div>
                  <textarea v-model="reviewReplies[reviewKey(review)]" placeholder="回复这条评论"></textarea>
                  <div class="admin-actions">
                    <button type="button" class="admin-action-edit" @click="submitReviewReply(review)">
                      回复
                      <AdminEditIcon class="admin-button-icon" />
                    </button>
                    <button type="button" class="admin-action-delete" @click="removeReview(review)">
                      删除评论
                      <AdminDeleteIcon class="admin-button-icon" />
                    </button>
                  </div>
                </article>
              </div>
            </section>

            <section class="admin-review-section">
              <h3>教材评论管理</h3>
              <div class="admin-card-grid">
                <article v-for="review in filteredTextbookReviews" :key="reviewKey(review)" class="admin-card admin-review-card">
                  <strong>
                    <span class="admin-review-author">
                      {{ review.userName || '匿名用户' }}
                      <span v-if="roleLabel(review.userRoleType)" class="admin-role-badge">{{ roleLabel(review.userRoleType) }}</span>
                    </span>
                    <small>#{{ review.userId || review.id }}</small>
                    · {{ review.rating }} 星
                  </strong>
                  <span>{{ reviewTypeLabel(review.reviewType) }} / {{ reviewTarget(review) }}</span>
                  <p>{{ review.content }}</p>
                  <div v-if="review.replyContent" class="admin-review-reply">
                    <strong>
                      回复：{{ review.replyUserName || review.replyUserId || '管理员' }}
                      <span v-if="roleLabel(review.replyUserRoleType)" class="admin-role-badge">{{ roleLabel(review.replyUserRoleType) }}</span>
                    </strong>
                    <p>{{ review.replyContent }}</p>
                    <button type="button" class="admin-reply-remove" @click="removeReviewReply(review)">删除回复</button>
                  </div>
                  <textarea v-model="reviewReplies[reviewKey(review)]" placeholder="回复这条评论"></textarea>
                  <div class="admin-actions">
                    <button type="button" class="admin-action-edit" @click="submitReviewReply(review)">
                      {{ review.replyContent ? '修改回复' : '回复' }}
                      <AdminEditIcon class="admin-button-icon" />
                    </button>
                    <button type="button" class="admin-action-delete" @click="removeReview(review)">
                      删除评论
                      <AdminDeleteIcon class="admin-button-icon" />
                    </button>
                  </div>
                </article>
              </div>
            </section>
          </section>
      </template>
    </section>

    <div
      v-if="activeEditor"
      class="admin-modal-backdrop"
      role="presentation"
      @click.self="requestCloseEditor"
    >
      <section class="admin-modal-dialog" role="dialog" aria-modal="true">
        <header class="admin-modal-head">
          <h2>{{ activeEditor === 'user' ? (editingUser?.roleType === 'admin' ? '修改管理员密码' : '编辑用户') : activeEditor === 'course' ? '编辑课程' : activeEditor === 'voucher' ? '编辑卡券' : activeEditor === 'set' ? '编辑题库' : activeEditor === 'oj' ? '编辑OJ题目' : '编辑题目' }}</h2>
          <button type="button" aria-label="关闭弹窗" @click="requestCloseEditor">×</button>
        </header>

        <form v-if="activeEditor === 'user'" class="admin-form admin-modal-form" @submit.prevent="saveUser">
          <template v-if="editingUser?.roleType === 'admin'">
            <label class="admin-field admin-field-wide">
              <span>管理员邮箱</span>
              <input v-model="userForm.email" disabled />
            </label>
            <label class="admin-field admin-field-wide">
              <span>新密码</span>
              <input v-model="userForm.password" type="password" required placeholder="请输入新密码" />
            </label>
          </template>
          <template v-else>
            <input v-model="userForm.username" placeholder="用户名" />
            <input v-model="userForm.email" placeholder="邮箱" />
            <select v-model="userForm.roleType">
              <option value="student">学生</option>
              <option value="teacher">教师</option>
            </select>
            <input v-model="userForm.teacherName" placeholder="教师姓名" />
            <input v-model="userForm.school" placeholder="学校" />
            <label class="admin-field">
              <span>目标金币总数</span>
              <input v-model.number="userForm.coinTotal" type="number" min="0" placeholder="例如 1000" />
            </label>
            <input v-model="userForm.password" type="password" placeholder="新密码，留空不改" />
            <input v-model="userForm.dataNote" placeholder="数据信息备注" />
          </template>
          <div class="admin-actions admin-modal-actions">
            <button type="submit" :disabled="loading">保存</button>
            <button type="button" @click="requestCloseEditor">取消</button>
          </div>
        </form>

        <form v-else-if="activeEditor === 'course'" class="admin-form admin-modal-form" @submit.prevent="submitCourse">
          <input v-model="courseForm.id" placeholder="课程编号" />
          <input v-model="courseForm.name" placeholder="课程名称" />
          <input v-model="courseForm.teacher" placeholder="教师" />
          <select v-model="courseForm.category">
            <option value="">请选择分类</option>
            <option v-for="category in courseCategories" :key="category.name" :value="category.name">{{ category.name }}</option>
          </select>
          <input v-model="courseForm.school" placeholder="学校" />
          <input v-model="courseForm.startTime" placeholder="开课时间" />
          <label class="admin-field">
            <span>参与人数</span>
            <input v-model.number="courseForm.participants" type="number" min="0" />
          </label>
          <input v-model="courseForm.certificationLabel" placeholder="认证标签，清空即删除" />
          <textarea v-model="courseForm.comment" placeholder="课程概述"></textarea>
          <textarea v-model="courseForm.description" placeholder="课程详情"></textarea>
          <input v-model="courseForm.coverFilePath" placeholder="封面本地路径（storage/... 或 teacher_courses/...）" />
          <input v-model="courseForm.videoFilePath" placeholder="视频本地路径" />
          <input v-model="courseForm.link" placeholder="链接" />
          <div class="admin-actions admin-modal-actions">
            <button type="submit" :disabled="loading">保存课程</button>
            <button type="button" @click="requestCloseEditor">取消</button>
          </div>
        </form>

        <form v-else-if="activeEditor === 'voucher'" class="admin-form admin-modal-form" @submit.prevent="submitVoucher">
          <input v-model="voucherForm.voucherKey" placeholder="卡券编号" />
          <select v-model="voucherForm.voucherType">
            <option value="DISCOUNT">优惠券</option>
            <option value="GAME_ITEM">游戏券</option>
          </select>
          <input v-model="voucherForm.name" placeholder="卡券名称" />
          <input v-model.number="voucherForm.price" type="number" min="0" placeholder="兑换金币价格" />
          <textarea v-model="voucherForm.description" placeholder="卡券说明"></textarea>
          <label class="admin-check"><input v-model="voucherForm.enabled" type="checkbox" /> 上架启用</label>
          <label class="admin-check"><input v-model="voucherForm.unlimitedStock" type="checkbox" /> 无限库存</label>
          <input v-model.number="voucherForm.stockQuantity" type="number" min="0" :disabled="voucherForm.unlimitedStock" placeholder="库存数量" />
          <select v-model="voucherForm.discountType" :disabled="voucherForm.voucherType === 'GAME_ITEM'">
            <option value="NONE">无折扣</option>
            <option value="AMOUNT">满减</option>
            <option value="PERCENT">折扣</option>
          </select>
          <input v-model.number="voucherForm.thresholdAmount" type="number" min="0" step="0.01" placeholder="使用门槛金额" />
          <input v-model.number="voucherForm.discountAmount" type="number" min="0" step="0.01" placeholder="减免金额" />
          <input v-model.number="voucherForm.discountRate" type="number" min="0" max="1" step="0.01" placeholder="0.9 表示 9 折" />
          <input v-model.number="voucherForm.maxDiscountAmount" type="number" min="0" step="0.01" placeholder="减免上限" />
          <input v-model="voucherForm.validFrom" type="datetime-local" />
          <input v-model="voucherForm.validUntil" type="datetime-local" />
          <input v-model.number="voucherForm.sortOrder" type="number" placeholder="排序" />
          <div class="admin-actions admin-modal-actions">
            <button type="submit" :disabled="loading">保存卡券</button>
            <button type="button" @click="requestCloseEditor">取消</button>
          </div>
        </form>

        <form v-else-if="activeEditor === 'set'" class="admin-form admin-modal-form" @submit.prevent="submitSet">
          <input v-model="setForm.categoryCode" placeholder="分类编号" />
          <input v-model="setForm.categoryName" placeholder="分类名称" />
          <input v-model="setForm.code" placeholder="题库编号" />
          <input v-model="setForm.title" placeholder="题库名称" />
          <input v-model="setForm.subtitle" placeholder="副标题" />
          <input v-model="setForm.difficultyLabel" placeholder="难度" />
          <input v-model="setForm.statusLabel" placeholder="状态" />
          <textarea v-model="setForm.description" placeholder="题库描述"></textarea>
          <textarea v-model="setForm.sourceRefsText" placeholder="来源链接，每行一个"></textarea>
          <div class="admin-actions admin-modal-actions">
            <button type="submit" :disabled="loading">保存题库</button>
            <button type="button" @click="requestCloseEditor">取消</button>
          </div>
        </form>

        <form v-else-if="activeEditor === 'question'" class="admin-form admin-modal-form" @submit.prevent="submitEditorQuestion">
          <select v-model="selectedSetCode">
            <option v-for="set in questionSets" :key="set.code" :value="set.code">{{ set.title }}</option>
          </select>
          <select v-model="editorQuestionForm.type" @change="handleEditorQuestionTypeChange">
            <option value="single">单选题</option>
            <option value="multiple">多选题</option>
            <option value="short">主观题</option>
            <option value="vocabulary">词汇题</option>
          </select>
          <textarea v-model="editorQuestionForm.stem" placeholder="题干"></textarea>
          <div v-if="isEditorChoiceQuestion" class="admin-choice-options">
            <label v-for="(_, index) in editorQuestionForm.options" :key="index" class="admin-choice-row">
              <span>{{ choiceLabels[index] }}.</span>
              <input v-model="editorQuestionForm.options[index]" :placeholder="`${choiceLabels[index]}.待输入选项`" />
              <button type="button" aria-label="删除选项" @click="removeChoiceOption(index, editorQuestionForm)">×</button>
            </label>
            <button v-if="editorQuestionForm.options.length < choiceLabels.length" type="button" class="admin-add-option" @click="addChoiceOption(editorQuestionForm)">+</button>
          </div>
          <textarea v-else v-model="editorQuestionForm.optionsText" placeholder="选项或补充内容，每行一个"></textarea>
          <input v-model="editorQuestionForm.answer" placeholder="答案" />
          <textarea v-model="editorQuestionForm.explanation" placeholder="解析"></textarea>
          <input v-model="editorQuestionForm.difficultyLabel" placeholder="难度" />
          <div class="admin-actions admin-modal-actions">
            <button type="submit" :disabled="loading">保存题目</button>
            <button type="button" @click="requestCloseEditor">取消</button>
          </div>
        </form>

        <form v-else-if="activeEditor === 'oj'" class="admin-form admin-modal-form admin-oj-form" @submit.prevent="submitEditorOjProblem">
          <input v-model="editorOjForm.title" placeholder="题目标题" />
          <input v-model="editorOjForm.slug" placeholder="题目标识，例如 two-sum" />
          <label class="admin-field admin-field-wide">
            <span>OJ分类（可多选）</span>
            <select
              multiple
              class="admin-multi-select"
              :value="selectedOjCategories(editorOjForm)"
              @change="updateOjCategories(editorOjForm, $event)"
            >
              <option v-for="category in ojMetaCategories" :key="category.name" :value="category.name">{{ formatOjCategoryName(category.name) }}</option>
            </select>
          </label>
          <textarea v-model="editorOjForm.description" placeholder="题目描述"></textarea>
          <textarea v-model="editorOjForm.inputDescription" placeholder="输入描述"></textarea>
          <textarea v-model="editorOjForm.outputDescription" placeholder="输出描述"></textarea>
          <textarea v-model="editorOjForm.standardCode" class="admin-code-input" placeholder="标准代码（C++）"></textarea>
          <div class="admin-form-row">
            <select v-model="editorOjForm.status">
              <option value="DRAFT">草稿</option>
              <option value="PUBLISHED">发布</option>
              <option value="ARCHIVED">归档</option>
            </select>
            <span class="admin-oj-derived-difficulty">难度：{{ formatDifficulty(editorOjForm.difficulty) }}</span>
          </div>
          <div class="admin-form-row">
            <input v-model.number="editorOjForm.timeLimitMs" type="number" min="100" placeholder="时间限制ms" />
            <input v-model.number="editorOjForm.memoryLimitKb" type="number" min="1024" placeholder="内存限制KB" />
          </div>
          <div class="admin-oj-cases admin-modal-cases">
            <article v-for="(testCase, index) in editorOjForm.testCases" :key="index" class="admin-oj-case">
              <header>
                <strong>测试点 {{ index + 1 }}</strong>
                <button type="button" aria-label="删除测试点" @click="removeOjCase(index, editorOjForm, editorOjCheckResult)">×</button>
              </header>
              <textarea v-model="testCase.inputData" placeholder="测试点输入"></textarea>
              <textarea v-model="testCase.expectedOutput" placeholder="测试点输出（可留空，由标准代码生成）"></textarea>
              <input v-model.number="testCase.weight" type="number" min="1" placeholder="权重" />
              <div v-if="editorOjCheckResult?.cases?.[index]" class="admin-case-result">
                <p :class="{ danger: !editorOjCheckResult.cases[index].matched }">
                  {{ editorOjCheckResult.cases[index].message }}
                </p>
                <pre>{{ formatActualOutput(editorOjCheckResult.cases[index]) }}</pre>
              </div>
            </article>
            <button type="button" class="admin-add-option" @click="addOjCase(editorOjForm, editorOjCheckResult)">+</button>
          </div>
          <div v-if="editorOjCheckResult" :class="['admin-check-result', { danger: !editorOjCheckResult.passed }]">
            <strong>{{ editorOjCheckResult.passed ? '校验通过' : '校验存在提示' }}</strong>
            <span>{{ editorOjCheckResult.message }}</span>
          </div>
          <div class="admin-actions admin-modal-actions">
            <button type="button" :disabled="!canCheckEditorOjCases" @click="checkEditorOjCases">校验测试点</button>
            <button type="submit" :disabled="loading">保存OJ题目</button>
            <button type="button" @click="requestCloseEditor">取消</button>
          </div>
        </form>
      </section>
    </div>

    <div
      v-if="confirmDialog.open"
      class="admin-modal-backdrop"
      role="presentation"
      @click.self="closeConfirm"
    >
      <section class="admin-confirm-dialog" role="dialog" aria-modal="true">
        <h2>{{ confirmDialog.title }}</h2>
        <p>{{ confirmDialog.message }}</p>
        <div class="admin-actions admin-modal-actions">
          <button type="button" class="admin-action-delete" :disabled="confirmDialog.pending" @click="handleConfirm">
            {{ confirmDialog.pending ? '处理中' : confirmDialog.confirmText }}
            <AdminDeleteIcon class="admin-button-icon" />
          </button>
          <button type="button" :disabled="confirmDialog.pending" @click="closeConfirm">取消</button>
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped>
.admin-page {
  min-height: 100vh;
  padding: 104px 28px 48px;
  background: #f4f7fb;
  color: #1f2937;
}

.admin-shell {
  width: min(1280px, 100%);
  margin: 0 auto;
}

.admin-header,
.admin-panel {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.08);
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px;
}

.admin-header p,
.admin-header h1 {
  margin: 0;
}

.admin-header p {
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
  text-transform: uppercase;
}

.admin-header h1 {
  margin-top: 6px;
  font-size: 30px;
}

.admin-empty,
.admin-message {
  margin-top: 16px;
  padding: 18px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.82);
}

.admin-tabs {
  display: flex;
  gap: 10px;
  margin: 18px 0;
}

.admin-tabs button,
.admin-actions button,
.admin-list button {
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 12px;
  background: #ffffff;
  color: #1f2937;
  font-weight: 800;
  cursor: pointer;
}

.admin-tabs button {
  padding: 12px 18px;
}

.admin-tabs button.active,
.admin-list button.active {
  border-color: rgba(37, 99, 235, 0.3);
  background: #dbeafe;
}

.admin-panel {
  display: grid;
  gap: 16px;
  padding: 22px;
}

.admin-panel h2 {
  margin: 0;
}

.admin-panel-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.admin-form,
.admin-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.admin-form textarea {
  min-height: 82px;
  grid-column: span 2;
}

.admin-form input,
.admin-form select,
.admin-form textarea,
.admin-row input,
.admin-row select,
.admin-panel-head select,
.admin-search-row input,
.admin-search-row select,
.admin-review-card textarea {
  width: 100%;
  min-height: 38px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 10px;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.94);
  color: #1f2937;
}

.admin-search-row {
  display: grid;
  grid-template-columns: minmax(240px, 1fr);
  gap: 10px;
}

.admin-question-search,
.admin-review-search {
  grid-template-columns: minmax(260px, 1fr) minmax(180px, 0.55fr) minmax(180px, 0.55fr);
}

.admin-review-search {
  grid-template-columns: repeat(2, minmax(220px, 1fr));
}

.admin-field {
  display: grid;
  gap: 6px;
  color: #64748b;
  font-size: 12px;
  font-weight: 900;
}

.admin-field span {
  line-height: 1;
}

.admin-field input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.admin-multi-select {
  min-height: 180px;
  width: 100%;
  resize: vertical;
}

.admin-oj-form .admin-field-wide {
  grid-column: 1 / -1;
}

.admin-oj-derived-difficulty {
  display: inline-flex;
  align-items: center;
  min-height: 38px;
  padding: 0 12px;
  border: 1px solid rgba(37, 99, 235, 0.18);
  border-radius: 10px;
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 900;
}

.admin-check {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 800;
}

.admin-check input {
  width: auto;
}

.admin-table,
.admin-list,
.admin-card-grid {
  display: grid;
  gap: 10px;
}

.admin-row,
.admin-card {
  align-items: center;
  padding: 14px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.9);
}

.admin-row {
  grid-template-columns: 1fr 1.4fr 0.8fr 0.8fr 1fr auto;
}

.admin-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-actions button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 34px;
  padding: 0 12px;
}

.admin-actions .admin-action-edit {
  border-color: rgba(37, 99, 235, 0.24);
  background: #60a5fa;
  color: #ffffff;
}

.admin-actions .admin-action-delete {
  border-color: rgba(248, 113, 113, 0.32);
  background: #f87171;
  color: #ffffff;
}

.admin-mode-switch {
  display: inline-flex;
  gap: 6px;
  width: fit-content;
  padding: 4px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 10px;
  background: #eef2f7;
}

.admin-mode-switch button {
  min-height: 34px;
  border: 0;
  border-radius: 8px;
  padding: 0 14px;
  background: transparent;
  color: #475569;
  font-weight: 800;
}

.admin-mode-switch button.active {
  background: #ffffff;
  color: #2563eb;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.12);
}

.admin-choice-options,
.admin-oj-cases {
  display: grid;
  gap: 10px;
  grid-column: span 2;
}

.admin-choice-row {
  display: grid;
  grid-template-columns: 28px 1fr 34px;
  gap: 8px;
  align-items: center;
}

.admin-choice-row span {
  font-weight: 900;
  color: #475569;
}

.admin-choice-row button,
.admin-oj-case header button {
  min-height: 34px;
  border-color: rgba(248, 113, 113, 0.32);
  background: #fff1f2;
  color: #dc2626;
}

.admin-add-option {
  justify-self: center;
  width: 36px;
  min-height: 36px;
  border-radius: 999px;
  border-color: rgba(37, 99, 235, 0.24);
  background: #dbeafe;
  color: #2563eb;
  font-size: 20px;
  font-weight: 900;
}

.admin-form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  grid-column: span 2;
}

.admin-code-input {
  min-height: 180px;
  font-family: ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace;
}

.admin-oj-layout {
  align-items: start;
}

.admin-oj-form {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.admin-oj-case {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 12px;
  background: #f8fafc;
}

.admin-oj-case header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.admin-oj-case header label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 800;
  color: #64748b;
}

.admin-oj-case p {
  margin: 0;
  color: #059669;
  font-weight: 800;
}

.admin-oj-case p.danger {
  color: #dc2626;
}

.admin-case-result {
  display: grid;
  gap: 6px;
}

.admin-case-result pre {
  max-height: 160px;
  overflow: auto;
  margin: 0;
  padding: 8px 10px;
  border-radius: 8px;
  background: #111827;
  color: #f8fafc;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.admin-check-result {
  display: grid;
  gap: 4px;
  grid-column: 1 / -1;
  padding: 10px 12px;
  border: 1px solid rgba(16, 185, 129, 0.24);
  border-radius: 10px;
  background: #ecfdf5;
  color: #047857;
}

.admin-check-result.danger {
  border-color: rgba(248, 113, 113, 0.32);
  background: #fff1f2;
  color: #dc2626;
}

.admin-check-result strong {
  font-size: 14px;
}

.admin-check-result span {
  font-size: 13px;
  font-weight: 700;
}

.admin-modal-cases {
  grid-column: 1 / -1;
}

.admin-button-icon {
  width: 15px;
  height: 15px;
  flex: 0 0 auto;
}

.admin-actions button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.admin-card-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.admin-card {
  display: grid;
  gap: 8px;
}

.admin-card strong,
.admin-card span,
.admin-card em,
.admin-card p {
  margin: 0;
}

.admin-review-section {
  display: grid;
  gap: 10px;
}

.admin-review-section h3 {
  margin: 0;
  color: #334155;
  font-size: 18px;
}

.admin-review-card {
  align-items: stretch;
}

.admin-review-author {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.admin-review-card small {
  color: #64748b;
  font-size: 12px;
}

.admin-review-card textarea {
  min-height: 72px;
  resize: vertical;
  background: #ffffff;
  color: #0f172a;
}

.admin-review-card textarea:focus {
  background: #ffffff;
  color: #0f172a;
}

.admin-review-reply {
  display: grid;
  gap: 6px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #eef6ff;
  color: #1e3a8a;
}

.admin-review-reply p {
  color: #1e3a8a;
}

.admin-review-reply-target {
  color: #64748b;
  font-size: 13px;
}

.admin-reply-remove {
  justify-self: start;
  width: fit-content;
  min-height: 30px;
  border: 1px solid rgba(248, 113, 113, 0.28);
  border-radius: 8px;
  background: #fff1f2;
  color: #dc2626;
  cursor: pointer;
  font-weight: 900;
  padding: 0 10px;
}

.admin-role-badge {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  padding: 0 7px;
  border-radius: 999px;
  background: #e0f2fe;
  color: #0369a1;
  font-size: 12px;
  font-weight: 900;
}

.admin-card span,
.admin-card em {
  color: #64748b;
  font-size: 13px;
}

.admin-split {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(420px, 1.1fr);
  gap: 18px;
}

.admin-list button {
  min-height: 38px;
  text-align: left;
  padding: 0 12px;
}

.admin-category-manager {
  display: grid;
  gap: 12px;
  padding: 14px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.9);
}

.admin-category-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.admin-category-head span {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.admin-category-form {
  display: flex;
  gap: 8px;
}

.admin-category-form input {
  min-height: 36px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 10px;
  padding: 8px 10px;
}

.admin-category-form button,
.admin-category-tags button {
  border: 0;
  cursor: pointer;
}

.admin-category-form button {
  border-radius: 10px;
  background: #16a34a;
  color: #ffffff;
  font-weight: 800;
  padding: 0 12px;
}

.admin-category-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-category-tags span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 30px;
  padding: 0 8px 0 10px;
  border-radius: 999px;
  background: #e0f2fe;
  color: #075985;
  font-size: 13px;
  font-weight: 800;
}

.admin-category-tags button {
  width: 20px;
  height: 20px;
  border-radius: 999px;
  background: rgba(2, 132, 199, 0.16);
  color: #075985;
  line-height: 1;
}

.admin-modal-backdrop {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.42);
  backdrop-filter: blur(6px);
}

.admin-modal-dialog,
.admin-confirm-dialog {
  width: min(860px, 100%);
  max-height: min(82vh, 760px);
  overflow: auto;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 24px 72px rgba(15, 23, 42, 0.24);
  animation: admin-dialog-in 0.2s ease-out both;
}

.admin-modal-dialog {
  padding: 20px;
}

.admin-confirm-dialog {
  width: min(420px, 100%);
  padding: 22px;
}

.admin-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.admin-modal-head h2,
.admin-confirm-dialog h2,
.admin-confirm-dialog p {
  margin: 0;
}

.admin-modal-head button {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 10px;
  background: #f1f5f9;
  color: #334155;
  cursor: pointer;
  font-size: 20px;
}

.admin-modal-form {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.admin-modal-form textarea,
.admin-field-wide {
  grid-column: 1 / -1;
}

.admin-modal-actions {
  grid-column: 1 / -1;
  justify-content: flex-end;
}

.admin-confirm-dialog {
  display: grid;
  gap: 14px;
}

@keyframes admin-dialog-in {
  from {
    opacity: 0;
    transform: translateY(12px) scale(0.98);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@media (width <= 900px) {
  .admin-page {
    padding: 88px 14px 32px;
  }

  .admin-form,
  .admin-row,
  .admin-card-grid,
  .admin-split {
    grid-template-columns: 1fr;
  }

  .admin-form textarea {
    grid-column: auto;
  }

  .admin-category-head,
  .admin-category-form,
  .admin-modal-form {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
