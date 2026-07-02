<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

onMounted(() => {
  router.replace('/lab/petroleum')
})
</script>

<template>
  <main class="lab-page" aria-label="正在跳转">
    <section class="lab-hero">
      <p class="lab-kicker">Petroleum Simulation</p>
      <h1>正在进入石油气仿真平台</h1>
    </section>
  </main>
</template>
