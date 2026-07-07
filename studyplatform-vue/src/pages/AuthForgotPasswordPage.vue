<script setup>
/**
 * 密码找回页面组件
 * 提供通过邮箱验证码重置密码的功能，包含发送验证码和提交重置密码两个核心流程
 */
import { onBeforeUnmount, ref } from 'vue'
import { confirmPasswordReset, sendPasswordResetCode } from '../api/auth'

/**
 * 密码重置表单数据
 */
const form = ref({
  email: '',
  password: '',
  confirmPassword: '',
  code: '',
})

/**
 * 成功提示信息
 */
const message = ref('')

/**
 * 错误提示信息
 */
const errorMessage = ref('')

/**
 * 验证码发送状态
 */
const sendingCode = ref(false)

/**
 * 密码重置提交状态
 */
const resetting = ref(false)

/**
 * 发送验证码后的倒计时（秒）
 */
const countdown = ref(0)

/**
 * 邮箱格式校验正则
 */
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/**
 * 倒计时定时器引用
 */
let countdownTimer = null

/**
 * 启动60秒倒计时
 * 防止用户频繁发送验证码
 */
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

/**
 * 发送密码重置验证码
 * 校验邮箱格式后调用接口发送验证码，并启动倒计时
 */
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

/**
 * 提交密码重置请求
 * 校验邮箱、密码长度、密码一致性和验证码后调用重置接口
 */
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

/**
 * 组件卸载前清理倒计时定时器
 */
onBeforeUnmount(() => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
  }
})
</script>

<template>
  <main class="auth-page">
    <!-- 密码找回卡片区域 -->
    <section class="auth-card auth-card-compact">
      <p class="auth-kicker">Password Recovery</p>
      <h1>找回密码</h1>

      <!-- 密码重置表单 -->
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
        <!-- 验证码输入区域（含发送按钮） -->
        <label>
          验证码
          <div class="auth-code-row">
            <input v-model="form.code" type="text" inputmode="numeric" maxlength="12" required />
            <button type="button" :disabled="sendingCode || countdown > 0" @click="sendCode">
              {{ countdown > 0 ? `${countdown}s` : sendingCode ? '发送中...' : '发送验证码' }}
            </button>
          </div>
        </label>
        <!-- 错误提示 -->
        <p v-if="errorMessage" class="auth-error">{{ errorMessage }}</p>
        <!-- 成功提示 -->
        <p v-if="message" class="auth-success">{{ message }}</p>
        <button type="submit" :disabled="resetting">{{ resetting ? '重置中...' : '确认找回' }}</button>
      </form>

      <!-- 跳转链接区域 -->
      <p class="auth-switch">想起密码了？<RouterLink to="/login">返回登录</RouterLink></p>
    </section>
  </main>
</template>
