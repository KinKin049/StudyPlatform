<script setup>
/**
 * 用户新手引导页面组件
 * 新用户注册后完成身份选择、详细信息填写和宠物选择三个步骤的引导流程
 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchAcademyCategories } from '../api/academy'
import { getStoredAuthUser, saveAuthOnboarding, storeAuthUser } from '../api/auth'
import { AI_PET_SHOP_ITEMS } from '../data/aiPetShop'

const router = useRouter()

/**
 * 当前登录用户信息
 */
const user = ref(getStoredAuthUser())

/**
 * 当前引导步骤（role: 选择身份, details: 填写详情, pet: 选择宠物）
 */
const step = ref('role')

/**
 * 步骤切换方向（forward: 前进, back: 后退）
 */
const slideDirection = ref('forward')

/**
 * 用户选择的身份（student/teacher）
 */
const selectedRole = ref('')

/**
 * 用户选择的宠物key
 */
const selectedPet = ref('')

/**
 * 学生表单数据
 */
const studentForm = ref({
  learningGoal: '',
  interests: [],
})

/**
 * 教师表单数据
 */
const teacherForm = ref({
  school: '',
  teacherName: '',
})

/**
 * 当前展示的兴趣选项列表
 */
const interestOptions = ref(['计算机', '经济管理', '心理学', '外语', '文学历史', '艺术设计'])

/**
 * 兴趣选项池（从后端获取或使用默认值）
 */
const interestPool = ref([...interestOptions.value])

/**
 * 表单提交状态
 */
const submitting = ref(false)

/**
 * 错误提示信息
 */
const errorMessage = ref('')

/**
 * 宠物选项列表
 */
const pets = AI_PET_SHOP_ITEMS.slice(0, 3).map((pet) => ({
  key: pet.key,
  name: pet.name,
  note: pet.tag,
  image: pet.preview || pet.image,
}))

/**
 * 当前步骤索引（从0开始）
 */
const currentStepIndex = computed(() => ['role', 'details', 'pet'].indexOf(step.value))

/**
 * 当前步骤标题
 */
const cardTitle = computed(() => {
  if (step.value === 'role') return '选择你的身份'
  if (step.value === 'details') return selectedRole.value === 'teacher' ? '完善教师信息' : '设置学习方向'
  return '选择宠物'
})

/**
 * 当前步骤副标题说明
 */
const cardSubtitle = computed(() => {
  if (step.value === 'role') return '请选择学生或教师，后续页面会根据身份切换。'
  if (step.value === 'details') return selectedRole.value === 'teacher' ? '填写学校和教师姓名。' : '填写目标，并选择你感兴趣的课程方向。'
  return '选择一个 AI 学习伙伴，之后也可以在金币兑换中心切换。'
})

/**
 * 步骤切换动画名称
 */
const transitionName = computed(() => (slideDirection.value === 'forward' ? 'auth-slide' : 'auth-slide-back'))

/**
 * 组件挂载时初始化
 * 校验用户登录状态，获取课程分类作为兴趣选项
 */
onMounted(async () => {
  if (!user.value?.id) {
    router.replace('/register')
    return
  }
  try {
    const categories = await fetchAcademyCategories('online-open-courses')
    const names = categories.map((item) => item.name).filter(Boolean)
    if (names.length) {
      interestPool.value = uniqueValues(names)
      refreshInterestOptions()
    }
  } catch {
    interestPool.value = uniqueValues(interestOptions.value)
    refreshInterestOptions()
  }
})

/**
 * 选择用户身份并进入下一步
 */
function chooseRole(role) {
  selectedRole.value = role
  goToStep('details')
}

/**
 * 切换兴趣选项的选中状态
 */
function toggleInterest(interest) {
  const exists = studentForm.value.interests.includes(interest)
  if (exists) {
    studentForm.value.interests = studentForm.value.interests.filter((item) => item !== interest)
  } else {
    studentForm.value.interests = [...studentForm.value.interests, interest]
  }
}

/**
 * 刷新兴趣选项列表
 * 从兴趣池中随机选取最多8个不同的选项
 */
function refreshInterestOptions() {
  const pool = uniqueValues(interestPool.value)
  if (!pool.length) {
    return
  }
  const current = new Set(interestOptions.value)
  const freshOptions = shuffle(pool.filter((item) => !current.has(item)))
  const fallbackOptions = shuffle(pool)
  interestOptions.value = uniqueValues([...freshOptions, ...fallbackOptions]).slice(0, Math.min(8, pool.length))
}

/**
 * 校验详情表单并进入宠物选择步骤
 */
function continueToPets() {
  errorMessage.value = ''
  if (selectedRole.value === 'student' && !studentForm.value.learningGoal.trim()) {
    errorMessage.value = '请填写目标'
    return
  }
  if (selectedRole.value === 'teacher' && (!teacherForm.value.school.trim() || !teacherForm.value.teacherName.trim())) {
    errorMessage.value = '请填写所属学校和教师姓名'
    return
  }
  goToStep('pet')
}

/**
 * 完成新手引导并保存用户信息
 * 将用户选择的身份、详情和宠物信息提交到后端
 */
async function finishOnboarding() {
  if (!selectedPet.value) {
    errorMessage.value = '请选择宠物'
    return
  }
  submitting.value = true
  errorMessage.value = ''
  try {
    const nextUser = await saveAuthOnboarding({
      userId: user.value.id,
      roleType: selectedRole.value,
      learningGoal: selectedRole.value === 'student' ? studentForm.value.learningGoal.trim() : '',
      interests: selectedRole.value === 'student' ? studentForm.value.interests : [],
      school: selectedRole.value === 'teacher' ? teacherForm.value.school.trim() : '',
      teacherName: selectedRole.value === 'teacher' ? teacherForm.value.teacherName.trim() : '',
      petKey: selectedPet.value,
    })
    storeAuthUser(nextUser)
    router.push('/')
  } catch (error) {
    errorMessage.value = error.message || '保存失败'
  } finally {
    submitting.value = false
  }
}

/**
 * 跳转到指定步骤并设置动画方向
 */
function goToStep(nextStep) {
  const steps = ['role', 'details', 'pet']
  slideDirection.value = steps.indexOf(nextStep) > currentStepIndex.value ? 'forward' : 'back'
  step.value = nextStep
}

/**
 * 数组随机打乱排序
 */
function shuffle(values) {
  return [...values].sort(() => Math.random() - 0.5)
}

/**
 * 数组去重并过滤空值
 */
function uniqueValues(values) {
  return [...new Set(values.map((value) => String(value || '').trim()).filter(Boolean))]
}
</script>

<template>
  <main class="auth-page auth-onboarding-page">
    <!-- 新手引导卡片区域 -->
    <section class="auth-card auth-onboarding-card">
      <!-- 步骤头部信息 -->
      <div class="auth-step-head">
        <p class="auth-kicker">Step {{ currentStepIndex + 1 }} / 3</p>
        <h1>{{ cardTitle }}</h1>
        <p class="auth-subtitle">{{ cardSubtitle }}</p>
      </div>

      <!-- 步骤切换动画区域 -->
      <Transition :name="transitionName" mode="out-in">
        <!-- 步骤一：选择身份 -->
        <section v-if="step === 'role'" key="role" class="auth-step-panel">
          <button class="auth-choice-card" type="button" @click="chooseRole('student')">
            <strong>学生</strong>
            <span>设置学习目标和兴趣方向</span>
          </button>
          <button class="auth-choice-card" type="button" @click="chooseRole('teacher')">
            <strong>教师</strong>
            <span>填写学校与教师身份信息</span>
          </button>
        </section>

        <!-- 步骤二：填写详情（学生/教师表单） -->
        <section v-else-if="step === 'details'" key="details" class="auth-step-panel">
          <template v-if="selectedRole === 'student'">
            <label class="auth-field">
              目标
              <input v-model="studentForm.learningGoal" type="text" placeholder="例如：通过六级 / 提升算法能力" />
            </label>
            <label class="auth-field">
              兴趣
              <input
                :value="studentForm.interests.join('、')"
                type="text"
                placeholder="从下方课程分类中选择"
                readonly
              />
            </label>
            <!-- 兴趣选项工具栏 -->
            <div class="auth-interest-toolbar">
              <span>从课程分类中选择兴趣方向</span>
              <button type="button" @click="refreshInterestOptions">换一批</button>
            </div>
            <!-- 兴趣选项标签列表 -->
            <div class="auth-chip-list">
              <button
                v-for="interest in interestOptions"
                :key="interest"
                type="button"
                :class="{ 'is-selected': studentForm.interests.includes(interest) }"
                @click="toggleInterest(interest)"
              >
                {{ interest }}
              </button>
            </div>
          </template>
          <template v-else>
            <label class="auth-field">
              所属学校
              <input v-model="teacherForm.school" type="text" placeholder="请输入学校名称" />
            </label>
            <label class="auth-field">
              教师姓名
              <input v-model="teacherForm.teacherName" type="text" placeholder="请输入教师姓名" />
            </label>
          </template>
          <!-- 错误提示 -->
          <p v-if="errorMessage" class="auth-error">{{ errorMessage }}</p>
          <!-- 步骤操作按钮 -->
          <div class="auth-step-actions">
            <button type="button" class="auth-secondary-button" @click="goToStep('role')">上一步</button>
            <button type="button" @click="continueToPets">继续</button>
          </div>
        </section>

        <!-- 步骤三：选择宠物 -->
        <section v-else key="pet" class="auth-step-panel">
          <div class="auth-pet-grid">
            <button
              v-for="pet in pets"
              :key="pet.key"
              type="button"
              class="auth-pet-card"
              :class="{ 'is-selected': selectedPet === pet.key }"
              @click="selectedPet = pet.key"
            >
              <img :src="pet.image" :alt="pet.name" />
              <strong>{{ pet.name }}</strong>
              <span>{{ pet.note }}</span>
            </button>
          </div>
          <!-- 错误提示 -->
          <p v-if="errorMessage" class="auth-error">{{ errorMessage }}</p>
          <!-- 步骤操作按钮 -->
          <div class="auth-step-actions">
            <button type="button" class="auth-secondary-button" @click="goToStep('details')">上一步</button>
            <button type="button" :disabled="submitting" @click="finishOnboarding">
              {{ submitting ? '保存中' : '进入平台' }}
            </button>
          </div>
        </section>
      </Transition>
    </section>
  </main>
</template>
