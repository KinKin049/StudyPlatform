<script setup>
/**
 * 用户登录页面组件
 * 提供邮箱和密码登录功能，登录成功后根据用户是否完成新手引导跳转至对应页面
 */
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { loginAuthUser, storeAuthUser } from '../api/auth'

const router = useRouter()

/**
 * 登录表单数据
 */
const form = ref({
  account: '',
  password: '',
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
 * 邮箱格式校验正则
 */
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/**
 * 提交登录请求
 * 校验邮箱格式和密码，调用登录接口，成功后存储用户信息并跳转
 */
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
    <!-- 登录卡片区域 -->
    <section class="auth-card auth-card-compact">
      <p class="auth-kicker">StudyPlatform</p>
      <h1>登录</h1>
      <p class="auth-subtitle">继续进入课程、实验与游戏模块。</p>

      <!-- 登录表单 -->
      <form class="auth-form" @submit.prevent="submitLogin">
        <label>
          邮箱
          <input v-model="form.account" type="email" autocomplete="email" />
        </label>
        <label>
          密码
          <input v-model="form.password" type="password" autocomplete="current-password" />
        </label>
        <!-- 错误提示 -->
        <p v-if="errorMessage" class="auth-error">{{ errorMessage }}</p>
        <button type="submit" :disabled="submitting">{{ submitting ? '登录中...' : '登录' }}</button>
      </form>

      <!-- 跳转链接区域 -->
      <p class="auth-switch">忘记密码？<RouterLink to="/forgot-password">找回密码</RouterLink></p>
      <p class="auth-switch">还没有账号？<RouterLink to="/register">立即注册</RouterLink></p>
    </section>
  </main>
</template>
