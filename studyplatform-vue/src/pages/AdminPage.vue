<script setup>
// 管理员后台页面，提供用户管理、课程管理、卡券管理、题库管理和评论管理功能
import { computed, onMounted, reactive, ref } from 'vue'
import { getStoredAuthUser } from '../api/auth'
import {
  createAdminQuestion,
  deleteAdminCourse,
  deleteAdminQuestion,
  deleteAdminQuestionBankSet,
  deleteAdminReview,
  deleteAdminUser,
  deleteAdminVoucher,
  fetchAdminCourses,
  fetchAdminQuestionBankSets,
  fetchAdminQuestions,
  fetchAdminReviews,
  fetchAdminUsers,
  fetchAdminVouchers,
  saveAdminCourse,
  saveAdminQuestionBankSet,
  saveAdminVoucher,
  updateAdminQuestion,
  updateAdminUser,
} from '../api/admin'

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
const selectedResourceType = ref('online-open-courses')
const selectedSetCode = ref('')

// 表单数据
const courseForm = reactive(emptyCourseForm())
const voucherForm = reactive(emptyVoucherForm())
const setForm = reactive(emptySetForm())
const questionForm = reactive(emptyQuestionForm())

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
  courses.value = await fetchAdminCourses(selectedResourceType.value)
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

// 编辑用户
function editUser(user) {
  users.value = users.value.map((item) => ({
    ...item,
    editing: item.id === user.id,
    draft: item.id === user.id ? { ...user, coinTotal: Number(user.coinTotal ?? 0), password: '' } : item.draft,
  }))
}

// 取消编辑用户
function cancelUserEdit(user) {
  user.editing = false
}

// 保存用户信息
async function saveUser(user) {
  await runTask(async () => {
    await updateAdminUser(user.id, user.draft)
    await loadUsers()
  }, '用户信息已保存')
}

// 删除用户
async function removeUser(user) {
  if (!window.confirm(`确认删除用户 ${user.username}？`)) return
  await runTask(async () => {
    await deleteAdminUser(user.id)
    await loadUsers()
  }, '用户已删除')
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
    coverUrl: course.coverUrl || '',
    coverFilePath: course.coverFilePath || '',
    startTime: course.startTime || '',
    participants: course.participants || 0,
    comment: course.comment || '',
    description: course.description || '',
    semesterPlan: course.semesterPlan || '',
    overview: course.overview || '',
    videoFilePath: course.videoFilePath || '',
    link: course.link || '',
    certified: Boolean(course.certified),
  })
}

// 重置课程表单
function resetCourseForm() {
  Object.assign(courseForm, emptyCourseForm(), { resourceType: selectedResourceType.value })
}

// 提交课程表单
async function submitCourse() {
  await runTask(async () => {
    await saveAdminCourse({ ...courseForm, resourceType: selectedResourceType.value })
    resetCourseForm()
    await loadCourses()
  }, '课程已保存')
}

// 删除课程
async function removeCourse(course) {
  if (!window.confirm(`确认删除课程 ${course.name}？`)) return
  await runTask(async () => {
    await deleteAdminCourse(selectedResourceType.value, course.id)
    await loadCourses()
  }, '课程已删除')
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
    coverUrl: set.coverUrl || '',
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
  }, '题库已保存')
}

// 删除题库
async function removeSet(set) {
  if (!window.confirm(`确认删除题库 ${set.title}？题目会一起删除。`)) return
  await runTask(async () => {
    await deleteAdminQuestionBankSet(set.code)
    selectedSetCode.value = ''
    await loadQuestionSets()
  }, '题库已删除')
}

// 填充题目表单
function fillQuestion(question) {
  Object.assign(questionForm, {
    id: question.id || null,
    setCode: selectedSetCode.value,
    type: question.type || 'single',
    stem: question.stem || '',
    optionsText: (question.options || []).join('\n'),
    answer: question.answer || '',
    explanation: question.explanation || '',
    difficultyLabel: question.difficultyLabel || '',
    sourceUrl: question.sourceUrl || '',
    sortOrder: 0,
  })
}

// 重置题目表单
function resetQuestionForm() {
  Object.assign(questionForm, emptyQuestionForm(), { setCode: selectedSetCode.value })
}

// 提交题目表单
async function submitQuestion() {
  await runTask(async () => {
    const payload = {
      setCode: selectedSetCode.value,
      type: questionForm.type,
      stem: questionForm.stem,
      options: splitLines(questionForm.optionsText),
      answer: questionForm.answer,
      explanation: questionForm.explanation,
      difficultyLabel: questionForm.difficultyLabel,
      sourceUrl: questionForm.sourceUrl,
      sortOrder: questionForm.sortOrder,
    }
    if (questionForm.id) {
      await updateAdminQuestion(questionForm.id, payload)
    } else {
      await createAdminQuestion(payload)
    }
    resetQuestionForm()
    await loadQuestions()
  }, '题目已保存')
}

// 删除题目
async function removeQuestion(question) {
  if (!window.confirm('确认删除这道题？')) return
  await runTask(async () => {
    await deleteAdminQuestion(question.id)
    await loadQuestions()
  }, '题目已删除')
}

// 删除评论
async function removeReview(review) {
  if (!window.confirm('确认删除这条评论？')) return
  await runTask(async () => {
    await deleteAdminReview(review.id)
    await loadReviews()
  }, '评论已删除')
}

// 填充卡券表单
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
  }, '卡券已上架')
}

// 删除卡券
async function removeVoucher(voucher) {
  if (!window.confirm(`确认下架 ${voucher.name}？`)) return
  await runTask(async () => {
    await deleteAdminVoucher(voucher.voucherKey)
    await loadVouchers()
  }, '卡券已下架')
}

// 将多行文本分割为数组
function splitLines(value) {
  return String(value || '')
    .split('\n')
    .map((item) => item.trim())
    .filter(Boolean)
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
    optionsText: '',
    answer: '',
    explanation: '',
    difficultyLabel: '',
    sourceUrl: '',
    sortOrder: 0,
  }
}

// 创建空的卡券表单
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
    await Promise.all([loadUsers(), loadCourses(), loadQuestionSets(), loadReviews(), loadVouchers()])
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
            <div class="admin-table">
              <article v-for="user in users" :key="user.id" class="admin-row">
                <!-- 用户编辑表单 -->
                <template v-if="user.editing">
                  <input v-model="user.draft.username" placeholder="用户名" />
                <input v-model="user.draft.email" placeholder="邮箱" />
                <select v-model="user.draft.roleType">
                  <option value="student">学生</option>
                  <option value="teacher">教师</option>
                </select>
                <input v-model="user.draft.teacherName" placeholder="教师姓名" />
                <input v-model="user.draft.school" placeholder="学校" />
                <label class="admin-field">
                  <span>目标金币总数</span>
                  <input v-model.number="user.draft.coinTotal" type="number" min="0" placeholder="例如 1000" />
                </label>
                <input v-model="user.draft.password" type="password" placeholder="新密码，留空不改" />
                <input v-model="user.draft.dataNote" placeholder="数据信息备注" />
                <div class="admin-actions">
                  <button type="button" :disabled="loading" @click="saveUser(user)">保存</button>
                  <button type="button" @click="cancelUserEdit(user)">取消</button>
                  </div>
                </template>
                <!-- 用户信息展示 -->
                <template v-else>
                  <strong>{{ user.username }}</strong>
                <span>{{ user.email }}</span>
                <span>{{ user.roleType === 'teacher' ? '教师' : user.roleType === 'admin' ? '管理员' : '学生' }}</span>
                <span>金币 {{ Number(user.coinTotal ?? 0).toLocaleString('zh-CN') }}</span>
                <span>{{ user.school || '未填写学校' }}</span>
                <div class="admin-actions">
                  <button type="button" @click="editUser(user)" :disabled="user.roleType === 'admin'">编辑</button>
                  <button type="button" @click="removeUser(user)" :disabled="user.roleType === 'admin'">删除</button>
                </div>
              </template>
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

            <!-- 课程表单 -->
            <form class="admin-form" @submit.prevent="submitCourse">
              <input v-model="courseForm.id" placeholder="课程编号" />
            <input v-model="courseForm.name" placeholder="课程名称" />
            <input v-model="courseForm.teacher" placeholder="教师" />
            <input v-model="courseForm.category" placeholder="分类" />
            <input v-model="courseForm.school" placeholder="学校" />
            <input v-model="courseForm.startTime" placeholder="开课时间" />
            <label class="admin-field">
              <span>参与人数</span>
              <input v-model.number="courseForm.participants" type="number" min="0" placeholder="例如 1200" />
            </label>
            <label class="admin-check"><input v-model="courseForm.certified" type="checkbox" /> 认证课程</label>
            <textarea v-model="courseForm.comment" placeholder="课程概述"></textarea>
            <textarea v-model="courseForm.description" placeholder="课程详情"></textarea>
            <input v-model="courseForm.coverUrl" placeholder="封面 URL" />
            <input v-model="courseForm.coverFilePath" placeholder="封面文件路径" />
            <input v-model="courseForm.videoFilePath" placeholder="视频文件路径" />
            <input v-model="courseForm.link" placeholder="链接" />
            <div class="admin-actions">
              <button type="submit" :disabled="loading">保存课程</button>
              <button type="button" @click="resetCourseForm">清空</button>
            </div>
          </form>

            <!-- 课程列表 -->
            <div class="admin-card-grid">
              <article v-for="course in courses" :key="course.id" class="admin-card">
                <strong>{{ course.name }}</strong>
              <span>{{ course.id }} · {{ course.category }}</span>
              <em>{{ course.certified ? '已认证' : '未认证' }}</em>
              <div class="admin-actions">
                <button type="button" @click="fillCourse(course)">编辑</button>
                <button type="button" @click="removeCourse(course)">删除</button>
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

            <!-- 卡券列表 -->
            <div class="admin-card-grid">
              <article v-for="voucher in vouchers" :key="voucher.voucherKey" class="admin-card">
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
                <button type="button" @click="fillVoucher(voucher)">编辑</button>
                <button type="button" @click="removeVoucher(voucher)">下架</button>
              </div>
            </article>
          </div>
          </section>

          <!-- 题库管理面板 -->
          <section v-if="activeTab === 'question-bank'" class="admin-panel">
            <h2>题库管理</h2>
            <div class="admin-split">
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
                    v-for="set in questionSets"
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
                <select v-model="questionForm.type">
                  <option value="single">单选题</option>
                  <option value="multiple">多选题</option>
                  <option value="short">主观题</option>
                  <option value="vocabulary">词汇题</option>
                </select>
                <textarea v-model="questionForm.stem" placeholder="题干"></textarea>
                <textarea v-model="questionForm.optionsText" placeholder="选项，每行一个"></textarea>
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
                  <article v-for="question in questions" :key="question.id" class="admin-card">
                    <strong>{{ question.stem }}</strong>
                  <span>{{ question.type }} · {{ question.answer }}</span>
                  <div class="admin-actions">
                    <button type="button" @click="fillQuestion(question)">编辑</button>
                    <button type="button" @click="removeQuestion(question)">删除</button>
                  </div>
                </article>
              </div>
            </div>
          </div>
          </section>

          <!-- 评论管理面板 -->
          <section v-if="activeTab === 'reviews'" class="admin-panel">
            <h2>评论管理</h2>
            <div class="admin-card-grid">
              <article v-for="review in reviews" :key="review.id" class="admin-card">
              <strong>{{ review.userName }} · {{ review.rating }} 星</strong>
              <span>{{ review.resourceType }} / {{ review.courseId }}</span>
              <p>{{ review.content }}</p>
              <div class="admin-actions">
                <button type="button" @click="removeReview(review)">删除评论</button>
              </div>
            </article>
          </div>
        </section>
      </template>
    </section>
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
.admin-panel-head select {
  width: 100%;
  min-height: 38px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 10px;
  padding: 8px 10px;
  background: rgba(255, 255, 255, 0.94);
  color: #1f2937;
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
  min-height: 34px;
  padding: 0 12px;
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
}
</style>
