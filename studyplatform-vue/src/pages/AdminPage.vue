<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getStoredAuthUser } from '../api/auth'
import {
  createAdminQuestion,
  deleteAdminCourse,
  deleteAdminQuestion,
  deleteAdminQuestionBankSet,
  deleteAdminReview,
  deleteAdminUser,
  fetchAdminCourses,
  fetchAdminQuestionBankSets,
  fetchAdminQuestions,
  fetchAdminReviews,
  fetchAdminUsers,
  saveAdminCourse,
  saveAdminQuestionBankSet,
  updateAdminQuestion,
  updateAdminUser,
} from '../api/admin'

const authUser = ref(getStoredAuthUser())
const isAdmin = computed(() => authUser.value?.email === 'admin@admin.com' && authUser.value?.roleType === 'admin')
const activeTab = ref('users')
const loading = ref(false)
const message = ref('')

const users = ref([])
const courses = ref([])
const reviews = ref([])
const questionSets = ref([])
const questions = ref([])
const selectedResourceType = ref('online-open-courses')
const selectedSetCode = ref('')

const courseForm = reactive(emptyCourseForm())
const setForm = reactive(emptySetForm())
const questionForm = reactive(emptyQuestionForm())

const tabs = [
  { key: 'users', label: '用户管理' },
  { key: 'courses', label: '课程管理' },
  { key: 'question-bank', label: '题库管理' },
  { key: 'reviews', label: '评论管理' },
]

const resourceTypes = [
  { value: 'online-open-courses', label: '在线开放课程' },
  { value: 'general-courses', label: '通识课程' },
  { value: 'micro-major-courses', label: '微专业课程' },
]

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

async function loadUsers() {
  users.value = await fetchAdminUsers()
}

async function loadCourses() {
  courses.value = await fetchAdminCourses(selectedResourceType.value)
}

async function loadReviews() {
  reviews.value = await fetchAdminReviews()
}

async function loadQuestionSets() {
  questionSets.value = await fetchAdminQuestionBankSets()
  if (!selectedSetCode.value && questionSets.value.length > 0) {
    selectedSetCode.value = questionSets.value[0].code
  }
  if (selectedSetCode.value) {
    await loadQuestions()
  }
}

async function loadQuestions() {
  if (!selectedSetCode.value) {
    questions.value = []
    return
  }
  questions.value = await fetchAdminQuestions(selectedSetCode.value)
}

function editUser(user) {
  users.value = users.value.map((item) => ({
    ...item,
    editing: item.id === user.id,
    draft: item.id === user.id ? { ...user, password: '' } : item.draft,
  }))
}

function cancelUserEdit(user) {
  user.editing = false
}

async function saveUser(user) {
  await runTask(async () => {
    await updateAdminUser(user.id, user.draft)
    await loadUsers()
  }, '用户信息已保存')
}

async function removeUser(user) {
  if (!window.confirm(`确认删除用户 ${user.username}？`)) return
  await runTask(async () => {
    await deleteAdminUser(user.id)
    await loadUsers()
  }, '用户已删除')
}

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

function resetCourseForm() {
  Object.assign(courseForm, emptyCourseForm(), { resourceType: selectedResourceType.value })
}

async function submitCourse() {
  await runTask(async () => {
    await saveAdminCourse({ ...courseForm, resourceType: selectedResourceType.value })
    resetCourseForm()
    await loadCourses()
  }, '课程已保存')
}

async function removeCourse(course) {
  if (!window.confirm(`确认删除课程 ${course.name}？`)) return
  await runTask(async () => {
    await deleteAdminCourse(selectedResourceType.value, course.id)
    await loadCourses()
  }, '课程已删除')
}

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

function resetSetForm() {
  Object.assign(setForm, emptySetForm())
}

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

async function removeSet(set) {
  if (!window.confirm(`确认删除题库 ${set.title}？题目会一起删除。`)) return
  await runTask(async () => {
    await deleteAdminQuestionBankSet(set.code)
    selectedSetCode.value = ''
    await loadQuestionSets()
  }, '题库已删除')
}

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

function resetQuestionForm() {
  Object.assign(questionForm, emptyQuestionForm(), { setCode: selectedSetCode.value })
}

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

async function removeQuestion(question) {
  if (!window.confirm('确认删除这道题？')) return
  await runTask(async () => {
    await deleteAdminQuestion(question.id)
    await loadQuestions()
  }, '题目已删除')
}

async function removeReview(review) {
  if (!window.confirm('确认删除这条评论？')) return
  await runTask(async () => {
    await deleteAdminReview(review.id)
    await loadReviews()
  }, '评论已删除')
}

function splitLines(value) {
  return String(value || '')
    .split('\n')
    .map((item) => item.trim())
    .filter(Boolean)
}

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

onMounted(async () => {
  if (!isAdmin.value) return
  await runTask(async () => {
    await Promise.all([loadUsers(), loadCourses(), loadQuestionSets(), loadReviews()])
  }, '管理员数据已加载')
})
</script>

<template>
  <main class="admin-page">
    <section class="admin-shell">
      <header class="admin-header">
        <div>
          <p>admin console</p>
          <h1>管理员后台</h1>
        </div>
        <span>{{ authUser?.email || '未登录' }}</span>
      </header>

      <div v-if="!isAdmin" class="admin-empty">
        <h2>需要管理员权限</h2>
        <p>请使用 admin@admin.com 登录。</p>
      </div>

      <template v-else>
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

        <p v-if="message" class="admin-message">{{ message }}</p>

        <section v-if="activeTab === 'users'" class="admin-panel">
          <h2>用户管理</h2>
          <div class="admin-table">
            <article v-for="user in users" :key="user.id" class="admin-row">
              <template v-if="user.editing">
                <input v-model="user.draft.username" placeholder="用户名" />
                <input v-model="user.draft.email" placeholder="邮箱" />
                <select v-model="user.draft.roleType">
                  <option value="student">学生</option>
                  <option value="teacher">教师</option>
                </select>
                <input v-model="user.draft.teacherName" placeholder="教师姓名" />
                <input v-model="user.draft.school" placeholder="学校" />
                <input v-model.number="user.draft.coinAdjustment" type="number" placeholder="金币调整" />
                <input v-model="user.draft.password" type="password" placeholder="新密码，留空不改" />
                <input v-model="user.draft.dataNote" placeholder="数据信息备注" />
                <div class="admin-actions">
                  <button type="button" :disabled="loading" @click="saveUser(user)">保存</button>
                  <button type="button" @click="cancelUserEdit(user)">取消</button>
                </div>
              </template>
              <template v-else>
                <strong>{{ user.username }}</strong>
                <span>{{ user.email }}</span>
                <span>{{ user.roleType === 'teacher' ? '教师' : user.roleType === 'admin' ? '管理员' : '学生' }}</span>
                <span>金币调整 {{ user.coinAdjustment }}</span>
                <span>{{ user.school || '未填写学校' }}</span>
                <div class="admin-actions">
                  <button type="button" @click="editUser(user)" :disabled="user.roleType === 'admin'">编辑</button>
                  <button type="button" @click="removeUser(user)" :disabled="user.roleType === 'admin'">删除</button>
                </div>
              </template>
            </article>
          </div>
        </section>

        <section v-if="activeTab === 'courses'" class="admin-panel">
          <div class="admin-panel-head">
            <h2>课程管理</h2>
            <select v-model="selectedResourceType" @change="loadCourses(); resetCourseForm()">
              <option v-for="item in resourceTypes" :key="item.value" :value="item.value">{{ item.label }}</option>
            </select>
          </div>

          <form class="admin-form" @submit.prevent="submitCourse">
            <input v-model="courseForm.id" placeholder="课程编号" />
            <input v-model="courseForm.name" placeholder="课程名称" />
            <input v-model="courseForm.teacher" placeholder="教师" />
            <input v-model="courseForm.category" placeholder="分类" />
            <input v-model="courseForm.school" placeholder="学校" />
            <input v-model="courseForm.startTime" placeholder="开课时间" />
            <input v-model.number="courseForm.participants" type="number" placeholder="参与人数" />
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

        <section v-if="activeTab === 'question-bank'" class="admin-panel">
          <h2>题库管理</h2>
          <div class="admin-split">
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
