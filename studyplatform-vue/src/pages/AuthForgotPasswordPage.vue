<script setup>
import { ref } from 'vue'

const form = ref({
  email: '',
  password: '',
  confirmPassword: '',
  code: '',
})
const message = ref('')
const errorMessage = ref('')
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

function submitResetPassword() {
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

  message.value = '找回密码模块已创建，验证码与重置功能暂未接入。'
}
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
          <input v-model="form.code" type="text" inputmode="numeric" maxlength="12" required />
        </label>
        <p v-if="errorMessage" class="auth-error">{{ errorMessage }}</p>
        <p v-if="message" class="auth-success">{{ message }}</p>
        <button type="submit">确认找回</button>
      </form>

      <p class="auth-switch">想起密码了？<RouterLink to="/login">返回登录</RouterLink></p>
    </section>
  </main>
</template>
