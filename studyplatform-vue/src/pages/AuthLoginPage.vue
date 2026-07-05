<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { loginAuthUser, storeAuthUser } from '../api/auth'

const router = useRouter()
const form = ref({
  account: '',
  password: '',
})
const submitting = ref(false)
const errorMessage = ref('')
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

async function submitLogin() {
  const email = form.value.account.trim()
  if (!emailPattern.test(email) || !form.value.password) {
    errorMessage.value = '请输入邮箱和密码'
    return
  }
  submitting.value = true
  errorMessage.value = ''
  try {
    const user = await loginAuthUser({
      account: email,
      password: form.value.password,
    })
    storeAuthUser(user)
    router.push(user.onboardingCompleted ? '/' : '/onboarding')
  } catch (error) {
    errorMessage.value = error.message || '登录失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <section class="auth-card auth-card-compact">
      <p class="auth-kicker">StudyPlatform</p>
      <h1>登录</h1>
      <p class="auth-subtitle">继续进入课程、实验与游戏模块。</p>

      <form class="auth-form" @submit.prevent="submitLogin">
        <label>
          邮箱
          <input v-model="form.account" type="email" autocomplete="email" />
        </label>
        <label>
          密码
          <input v-model="form.password" type="password" autocomplete="current-password" />
        </label>
        <p v-if="errorMessage" class="auth-error">{{ errorMessage }}</p>
        <button type="submit" :disabled="submitting">{{ submitting ? '登录中...' : '登录' }}</button>
      </form>

      <p class="auth-switch">忘记密码？<RouterLink to="/forgot-password">找回密码</RouterLink></p>
      <p class="auth-switch">还没有账号？<RouterLink to="/register">立即注册</RouterLink></p>
    </section>
  </main>
</template>
