<script setup>
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useLearningTimeTracker } from '../../composables/useLearningTimeTracker'
import PumpIndicatorPanel from './PumpIndicatorPanel.vue'
import ReservoirDynamicsPanel from './ReservoirDynamicsPanel.vue'
import WaterfloodPanel from './WaterfloodPanel.vue'
import WellLogPanel from './WellLogPanel.vue'

/**
 * 石油气仿真总页面。
 * 统一承载测井解释与采油生产四类前端仿真模块，包括测井曲线、抽油机示功图、油藏动态和注水开发。
 */

// 当前激活的仿真模块标签页
const activeTab = ref('wellLog')

// 学习时间追踪器，记录用户在各仿真模块的学习时长
useLearningTimeTracker({
  moduleType: 'petroleum',
  targetCode: () => `petroleum:${activeTab.value}`,
  targetTitle: '油气仿真平台',
})
</script>

<template>
  <main class="production-page petroleum-page">
    <!-- 页面顶部标题栏 -->
    <header class="production-header">
      <div>
        <p class="production-kicker">Petroleum & Gas Simulation</p>
        <h1>石油气仿真平台</h1>
      </div>
      <RouterLink class="production-home-link" to="/lab">返回实验平台</RouterLink>
    </header>

    <!-- 仿真模块切换标签页 -->
    <el-tabs v-model="activeTab" class="production-tabs petroleum-tabs" type="border-card">
      <!-- 测井曲线仿真模块 -->
      <el-tab-pane label="测井曲线仿真" name="wellLog">
        <WellLogPanel v-if="activeTab === 'wellLog'" />
      </el-tab-pane>
      <!-- 抽油机展示功图模块 -->
      <el-tab-pane label="抽油机展示功图" name="pump">
        <PumpIndicatorPanel v-if="activeTab === 'pump'" />
      </el-tab-pane>
      <!-- 油藏动态仿真模块 -->
      <el-tab-pane label="油藏动态" name="reservoir">
        <ReservoirDynamicsPanel v-if="activeTab === 'reservoir'" />
      </el-tab-pane>
      <!-- 注水开发仿真模块 -->
      <el-tab-pane label="注水开发" name="waterflood">
        <WaterfloodPanel v-if="activeTab === 'waterflood'" />
      </el-tab-pane>
    </el-tabs>
  </main>
</template>
