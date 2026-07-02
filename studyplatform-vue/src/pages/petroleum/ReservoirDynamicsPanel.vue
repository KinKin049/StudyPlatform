<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { postSimulationRecord } from './api'

/**
 * 油藏动态仿真面板。
 */

const formationPressure = ref(25)
const permeability = ref(100)
const waterSaturation = ref(30)
const viscosity = ref(10)
const reservoirChartRef = ref(null)
const chartInstance = ref(null)

const reservoirProduction = computed(() => {
  const coefficient = 0.035
  const dailyLiquid = Math.max(
    0,
    coefficient * permeability.value * (formationPressure.value - 8) / viscosity.value,
  )
  const waterFraction = waterSaturation.value / 100
  const dailyOil = dailyLiquid * (1 - waterFraction)
  const dailyWater = dailyLiquid * waterFraction

  return {
    dailyLiquid: Number(dailyLiquid.toFixed(2)),
    dailyOil: Number(dailyOil.toFixed(2)),
    dailyWater: Number(dailyWater.toFixed(2)),
  }
})

function renderReservoirChart() {
  if (!reservoirChartRef.value) return
  if (!chartInstance.value) chartInstance.value = echarts.init(reservoirChartRef.value)
  chartInstance.value.setOption({
    animation: false,
    grid: { top: 34, right: 22, bottom: 36, left: 56 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['日产油', '日产水'] },
    yAxis: { type: 'value', name: '产量 t/d' },
    series: [
      {
        type: 'bar',
        data: [
          { value: reservoirProduction.value.dailyOil, itemStyle: { color: '#f2b94b' } },
          { value: reservoirProduction.value.dailyWater, itemStyle: { color: '#4aa6d8' } },
        ],
        barWidth: 52,
      },
    ],
  }, true)
}

function resizeChart() {
  chartInstance.value?.resize()
}

async function saveReservoirRecord() {
  try {
    await postSimulationRecord('/api/production/reservoir/save', {
      userId: null,
      formationPressure: formationPressure.value,
      permeability: permeability.value,
      waterSaturation: waterSaturation.value,
      viscosity: viscosity.value,
      dailyOil: reservoirProduction.value.dailyOil,
      dailyWater: reservoirProduction.value.dailyWater,
    })
    ElMessage.success('油藏动态记录已保存')
  } catch (error) {
    console.error('保存油藏动态记录失败', error)
    ElMessage.warning('后端未连接，当前仅完成前端仿真')
  }
}

watch([formationPressure, permeability, waterSaturation, viscosity], renderReservoirChart)

onMounted(async () => {
  await nextTick()
  renderReservoirChart()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chartInstance.value?.dispose()
})
</script>

<template>
  <section class="production-layout">
    <aside class="production-control">
      <el-card shadow="never">
        <template #header>油藏参数</template>
        <div class="control-item">
          <div class="control-label"><span>地层压力</span><strong>{{ formationPressure }} MPa</strong></div>
          <el-slider v-model="formationPressure" :min="10" :max="40" :step="0.5" />
        </div>
        <div class="control-item">
          <div class="control-label"><span>渗透率</span><strong>{{ permeability }} mD</strong></div>
          <el-slider v-model="permeability" :min="1" :max="1000" :step="1" />
        </div>
        <div class="control-item">
          <div class="control-label"><span>含水饱和度</span><strong>{{ waterSaturation }}%</strong></div>
          <el-slider v-model="waterSaturation" :min="0" :max="100" :step="1" />
        </div>
        <div class="control-item">
          <div class="control-label"><span>原油粘度</span><strong>{{ viscosity }} mPa·s</strong></div>
          <el-slider v-model="viscosity" :min="1" :max="50" :step="0.5" />
        </div>
        <el-button class="full-control secondary-action" @click="saveReservoirRecord">保存记录</el-button>
      </el-card>
    </aside>

    <section class="production-visual">
      <div class="metric-grid">
        <el-card shadow="never"><span>日产油</span><strong>{{ reservoirProduction.dailyOil }} t/d</strong></el-card>
        <el-card shadow="never"><span>日产水</span><strong>{{ reservoirProduction.dailyWater }} t/d</strong></el-card>
        <el-card shadow="never"><span>日产液</span><strong>{{ reservoirProduction.dailyLiquid }} t/d</strong></el-card>
      </div>
      <el-card shadow="never" class="chart-card-fill">
        <template #header>日产油 / 日产水占比</template>
        <div ref="reservoirChartRef" class="production-chart large-chart"></div>
      </el-card>
    </section>
  </section>
</template>
