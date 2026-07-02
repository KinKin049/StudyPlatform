<script setup>
import { ref } from 'vue'
import PumpIndicatorPanel from './PumpIndicatorPanel.vue'
import ReservoirDynamicsPanel from './ReservoirDynamicsPanel.vue'
import WaterfloodPanel from './WaterfloodPanel.vue'
import WellLogPanel from './WellLogPanel.vue'

/**
 * 石油气仿真总页面。
 * 统一承载测井解释与采油生产四类前端仿真模块。
 */

const activeTab = ref('wellLog')
</script>

<template>
  <main class="production-page petroleum-page">
    <header class="production-header">
      <div>
        <p class="production-kicker">Petroleum & Gas Simulation</p>
        <h1>石油气仿真平台</h1>
      </div>
      <a class="production-home-link" href="/">返回首页</a>
    </header>

    <el-tabs v-model="activeTab" class="production-tabs petroleum-tabs" type="border-card">
      <el-tab-pane label="测井曲线仿真" name="wellLog">
        <WellLogPanel v-if="activeTab === 'wellLog'" />
      </el-tab-pane>
      <el-tab-pane label="抽油机展示功图" name="pump">
        <PumpIndicatorPanel v-if="activeTab === 'pump'" />
      </el-tab-pane>
      <el-tab-pane label="油藏动态" name="reservoir">
        <ReservoirDynamicsPanel v-if="activeTab === 'reservoir'" />
      </el-tab-pane>
      <el-tab-pane label="注水开发" name="waterflood">
        <WaterfloodPanel v-if="activeTab === 'waterflood'" />
      </el-tab-pane>
    </el-tabs>
  </main>
</template>
