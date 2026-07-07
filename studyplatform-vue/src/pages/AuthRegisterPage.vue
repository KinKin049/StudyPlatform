<script setup>
/**
 * 用户注册页面组件
 * 提供用户账号注册功能，包含用户名、邮箱、密码等信息的校验与提交
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { registerAuthUser, storeAuthUser } from '../api/auth'

const router = useRouter()

/**
 * 注册表单数据
 */
const form = ref({
  username: '',
  email: '',
  password: '',
  confirmPassword: '',
  agreementAccepted: false,
})

/**
 * 表单提交状态
 */
const submitting = ref(false)

/**
 * 错误提示信息
 */
const errorMessage = ref('')

/**
 * 用户名格式校验正则
 * 支持中文、字母、数字、下划线和短横线，长度至少2位
 */
const usernamePattern = /^[A-Za-z0-9_\u4e00-\u9fa5-]{2,64}$/

/**
 * 邮箱格式校验正则
 */
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/**
 * 提交注册请求
 * 依次校验用户名、邮箱、密码、用户协议和密码一致性，调用注册接口
 */
async function submitRegister() {
  const username = form.value.username.trim()
  const email = form.value.email.trim()

  if (!usernamePattern.test(username)) {
    errorMessage.value = '用户名只能包含中文、字母、数字、下划线或短横线，长度至少 2 位'
    return
  }
  if (!emailPattern.test(email)) {
    errorMessage.value = '请输入正确的邮箱地址'
    return
  }
  if (form.value.password.length < 6 || form.value.password.length > 72) {
    errorMessage.value = '密码长度需要在 6 到 72 个字符之间'
    return
  }
  if (!form.value.agreementAccepted) {
    errorMessage.value = '请先同意用户协议'
    return
  }
  if (form.value.password !== form.value.confirmPassword) {
    errorMessage.value = '两次输入的密码不一致'
    return
  }
  submitting.value = true
  errorMessage.value = ''
  try {
    const user = await registerAuthUser({
      username,
      email,
      password: form.value.password,
      confirmPassword: form.value.confirmPassword,
      agreementAccepted: form.value.agreementAccepted,
    })
    storeAuthUser(user)
    router.push('/onboarding')
  } catch (error) {
    errorMessage.value = error.message || '注册失败'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <main class="auth-page">
    <!-- 注册卡片区域 -->
    <section class="auth-card auth-register-card">
      <p class="auth-kicker">Create Account</p>
      <h1>注册账号</h1>
      <p class="auth-subtitle">账号会写入数据库，注册成功后自动登录。</p>

      <!-- 注册表单 -->
      <form class="auth-form" @submit.prevent="submitRegister">
        <label>
          用户名
          <input v-model="form.username" type="text" autocomplete="username" maxlength="64" required />
        </label>
        <label>
          邮箱
          <input v-model="form.email" type="email" autocomplete="email" maxlength="128" required />
        </label>
        <label>
          密码
          <input v-model="form.password" type="password" autocomplete="new-password" minlength="6" maxlength="72" required />
        </label>
        <label>
          确认密码
          <input v-model="form.confirmPassword" type="password" autocomplete="new-password" required />
        </label>
        <!-- 用户协议勾选 -->
        <label class="auth-agreement">
          <input v-model="form.agreementAccepted" type="checkbox" required />
          <span>我已阅读并同意用户协议</span>
        </label>
        <!-- 错误提示 -->
        <p v-if="errorMessage" class="auth-error">{{ errorMessage }}</p>
        <button type="submit" :disabled="submitting">{{ submitting ? '注册中' : '注册并继续' }}</button>
      </form>

      <!-- 跳转链接区域 -->
      <p class="auth-switch">已有账号？<RouterLink to="/login">去登录</RouterLink></p>
    </section>
  </main>
</template>
