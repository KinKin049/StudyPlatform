<script setup>
import { onBeforeUnmount, ref } from 'vue'
import { confirmPasswordReset, sendPasswordResetCode } from '../api/auth'

const form = ref({
  email: '',
  password: '',
  confirmPassword: '',
  code: '',
})
const message = ref('')
const errorMessage = ref('')
const sendingCode = ref(false)
const resetting = ref(false)
const countdown = ref(0)
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
let countdownTimer = null

function startCountdown() {
  countdown.value = 60
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
  countdownTimer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

async function sendCode() {
  const email = form.value.email.trim()
  errorMessage.value = ''
  message.value = ''

  if (!emailPattern.test(email)) {
    errorMessage.value = '请输入正确的邮箱'
    return
  }

  sendingCode.value = true
  try {
    const response = await sendPasswordResetCode({ email })
    message.value = response.message || '如果邮箱存在，验证码已发送'
    startCountdown()
  } catch (error) {
    errorMessage.value = error.message || '验证码发送失败'
  } finally {
    sendingCode.value = false
  }
}

async function submitResetPassword() {
  const email = form.value.email.trim()
  errorMessage.value = ''
  message.value = ''

  if (!emailPattern.test(email)) {
    errorMessage.value = '请输入正确的邮箱'
    return
  }
  if (form.value.password.length < 6 || form.value.password.length > 72) {
    errorMessage.value = '新密码长度需要在 6 到 72 个字符之间'
    return
  }
  if (form.value.password !== form.value.confirmPassword) {
    errorMessage.value = '两次输入的新密码不一致'
    return
  }
  if (!form.value.code.trim()) {
    errorMessage.value = '请输入验证码'
    return
  }

  resetting.value = true
  try {
    const response = await confirmPasswordReset({
      email,
      password: form.value.password,
      confirmPassword: form.value.confirmPassword,
      code: form.value.code.trim(),
    })
    message.value = response.message || '密码已重置，请返回登录'
    form.value.password = ''
    form.value.confirmPassword = ''
    form.value.code = ''
  } catch (error) {
    errorMessage.value = error.message || '密码重置失败'
  } finally {
    resetting.value = false
  }
}

onBeforeUnmount(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<template>
  <main class="auth-page">
    <section class="auth-card auth-card-compact">
      <p class="auth-kicker">Password Recovery</p>
      <h1>找回密码</h1>

      <form class="auth-form" @submit.prevent="submitResetPassword">
        <label>
          邮箱
          <input v-model="form.email" type="email" autocomplete="email" required />
        </label>
        <label>
          新密码
          <input v-model="form.password" type="password" autocomplete="new-password" minlength="6" maxlength="72" required />
        </label>
        <label>
          确认密码
          <input v-model="form.confirmPassword" type="password" autocomplete="new-password" required />
        </label>
        <label>
          验证码
          <div class="auth-code-row">
            <input v-model="form.code" type="text" inputmode="numeric" maxlength="12" required />
            <button type="button" :disabled="sendingCode || countdown > 0" @click="sendCode">
              {{ countdown > 0 ? `${countdown}s` : sendingCode ? '发送中...' : '发送验证码' }}
            </button>
          </div>
        </label>
        <p v-if="errorMessage" class="auth-error">{{ errorMessage }}</p>
        <p v-if="message" class="auth-success">{{ message }}</p>
        <button type="submit" :disabled="resetting">{{ resetting ? '重置中...' : '确认找回' }}</button>
      </form>

      <p class="auth-switch">想起密码了？<RouterLink to="/login">返回登录</RouterLink></p>
    </section>
  </main>
</template>
