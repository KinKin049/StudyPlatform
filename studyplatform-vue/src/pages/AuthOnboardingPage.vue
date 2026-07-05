<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchAcademyCategories } from '../api/academy'
import { getStoredAuthUser, saveAuthOnboarding, storeAuthUser } from '../api/auth'

const router = useRouter()
const user = ref(getStoredAuthUser())
const step = ref('role')
const slideDirection = ref('forward')
const selectedRole = ref('')
const selectedPet = ref('')
const studentForm = ref({
  learningGoal: '',
  interests: [],
})
const teacherForm = ref({
  school: '',
  teacherName: '',
})
const interestOptions = ref(['计算机', '经济管理', '心理学', '外语', '文学历史', '艺术设计'])
const interestPool = ref([...interestOptions.value])
const submitting = ref(false)
const errorMessage = ref('')

const pets = [
  { key: 'spark', name: '星火', note: '偏学习陪伴' },
  { key: 'byte', name: '比特', note: '偏编程训练' },
  { key: 'terra', name: '塔拉', note: '偏实验探索' },
]

const currentStepIndex = computed(() => ['role', 'details', 'pet'].indexOf(step.value))
const cardTitle = computed(() => {
  if (step.value === 'role') return '选择你的身份'
  if (step.value === 'details') return selectedRole.value === 'teacher' ? '完善教师信息' : '设置学习方向'
  return '选择宠物'
})
const cardSubtitle = computed(() => {
  if (step.value === 'role') return '请选择学生或教师，后续页面会根据身份切换。'
  if (step.value === 'details') return selectedRole.value === 'teacher' ? '填写学校和教师姓名。' : '填写目标，并选择你感兴趣的课程方向。'
  return '宠物功能暂未实现，这里先提供三个占位选择。'
})
const transitionName = computed(() => (slideDirection.value === 'forward' ? 'auth-slide' : 'auth-slide-back'))

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

function chooseRole(role) {
  selectedRole.value = role
  goToStep('details')
}

function toggleInterest(interest) {
  const exists = studentForm.value.interests.includes(interest)
  if (exists) {
    studentForm.value.interests = studentForm.value.interests.filter((item) => item !== interest)
  } else {
    studentForm.value.interests = [...studentForm.value.interests, interest]
  }
}

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

function goToStep(nextStep) {
  const steps = ['role', 'details', 'pet']
  slideDirection.value = steps.indexOf(nextStep) > currentStepIndex.value ? 'forward' : 'back'
  step.value = nextStep
}

function shuffle(values) {
  return [...values].sort(() => Math.random() - 0.5)
}

function uniqueValues(values) {
  return [...new Set(values.map((value) => String(value || '').trim()).filter(Boolean))]
}
</script>

<template>
  <main class="auth-page auth-onboarding-page">
    <section class="auth-card auth-onboarding-card">
      <div class="auth-step-head">
        <p class="auth-kicker">Step {{ currentStepIndex + 1 }} / 3</p>
        <h1>{{ cardTitle }}</h1>
        <p class="auth-subtitle">{{ cardSubtitle }}</p>
      </div>

      <Transition :name="transitionName" mode="out-in">
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
            <div class="auth-interest-toolbar">
              <span>从课程分类中选择兴趣方向</span>
              <button type="button" @click="refreshInterestOptions">换一批</button>
            </div>
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
          <p v-if="errorMessage" class="auth-error">{{ errorMessage }}</p>
          <div class="auth-step-actions">
            <button type="button" class="auth-secondary-button" @click="goToStep('role')">上一步</button>
            <button type="button" @click="continueToPets">继续</button>
          </div>
        </section>

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
              <i></i>
              <strong>{{ pet.name }}</strong>
              <span>{{ pet.note }}</span>
            </button>
          </div>
          <p v-if="errorMessage" class="auth-error">{{ errorMessage }}</p>
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
