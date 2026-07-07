<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

/**
 * 测井仿真页面兼容入口。
 * 旧路由重定向到统一的石油气仿真平台页面。
 */

// 路由实例
const router = useRouter()

// 组件挂载时跳转到石油气仿真平台
onMounted(() => {
  router.replace('/lab/petroleum')
})
</script>

<template>
  <main class="lab-page" aria-label="正在跳转">
    <!-- 跳转提示页面 -->
    <section class="lab-hero">
      <p class="lab-kicker">Petroleum Simulation</p>
      <h1>正在进入石油气仿真平台</h1>
    </section>
  </main>
</template>
