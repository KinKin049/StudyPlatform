<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { postSimulationRecord } from './api'

/**
 * 游梁式抽油机动画与示功图面板。
 */

const PUMP_DEPTH = 1500

const isPumpPlaying = ref(true)
const pumpPhase = ref(0)
const pumpStrokeTimes = ref(6)
const pumpStroke = ref(3)
const pumpDiameter = ref(44)
const pumpCondition = ref('normal')
const indicatorChartRef = ref(null)
const chartInstance = ref(null)
const chartResizeObserver = ref(null)
let animationFrameId = 0
let lastFrameTime = 0

const workConditionOptions = [
  { label: '正常', value: 'normal' },
  { label: '供液不足', value: 'under_supply' },
  { label: '气体影响', value: 'gas' },
  { label: '漏失', value: 'leakage' },
]

const pumpArea = computed(() => {
  const diameterMeter = pumpDiameter.value / 1000
  return Math.PI * diameterMeter * diameterMeter / 4
})

const pumpLoadRange = computed(() => {
  const rodLoad = 2.7 * PUMP_DEPTH * pumpArea.value
  const liquidLoad = 0.85 * PUMP_DEPTH * pumpArea.value
  const staticMax = rodLoad + liquidLoad
  const staticMin = rodLoad
  const inertiaLoad = 0.035 * pumpStroke.value * pumpStrokeTimes.value * pumpStrokeTimes.value
  return { rodLoad, liquidLoad, staticMax, staticMin, inertiaLoad }
})

const pumpGeometry = computed(() => {
  const phase = pumpPhase.value
  const amplitude = 12 + pumpStroke.value * 5
  const crankCenter = { x: 155, y: 250 }
  const crankRadius = 34
  const crankPin = {
    x: crankCenter.x + Math.cos(phase) * crankRadius,
    y: crankCenter.y + Math.sin(phase) * crankRadius,
  }
  const beamPivot = { x: 305, y: 145 }
  const beamAngle = Math.sin(phase) * (0.08 + pumpStroke.value * 0.015)
  const leftBeam = {
    x: beamPivot.x - Math.cos(beamAngle) * 145,
    y: beamPivot.y - Math.sin(beamAngle) * 145,
  }
  const horseHead = {
    x: beamPivot.x + Math.cos(beamAngle) * 205,
    y: beamPivot.y + Math.sin(beamAngle) * 205,
  }
  const polishedRodY = 222 + Math.sin(phase) * amplitude
  return {
    crankCenter,
    crankPin,
    beamPivot,
    leftBeam,
    horseHead,
    polishedRodY,
    headArcPath: `M ${horseHead.x - 12} ${horseHead.y - 42} Q ${horseHead.x + 34} ${horseHead.y} ${horseHead.x - 8} ${horseHead.y + 44}`,
  }
})

const indicatorData = computed(() => {
  const points = []
  const count = 80
  const { staticMax, staticMin, inertiaLoad } = pumpLoadRange.value

  for (let index = 0; index <= count; index += 1) {
    const ratio = index / count
    const displacementValue = ratio * pumpStroke.value
    let load = staticMax + inertiaLoad * (0.25 + ratio)

    if (pumpCondition.value === 'under_supply' && ratio > 0.55) load -= inertiaLoad * 1.8 * (ratio - 0.55)
    if (pumpCondition.value === 'gas') load -= inertiaLoad * 0.55 * Math.sin(ratio * Math.PI)
    if (pumpCondition.value === 'leakage') load -= inertiaLoad * 0.35

    points.push([Number(displacementValue.toFixed(2)), Number(load.toFixed(2))])
  }

  for (let index = count; index >= 0; index -= 1) {
    const ratio = index / count
    const displacementValue = ratio * pumpStroke.value
    let load = staticMin - inertiaLoad * (0.15 + (1 - ratio) * 0.75)

    if (pumpCondition.value === 'under_supply') load -= inertiaLoad * 0.2 * Math.sin(ratio * Math.PI)
    if (pumpCondition.value === 'gas') load += inertiaLoad * 0.35 * Math.sin(ratio * Math.PI * 2)
    if (pumpCondition.value === 'leakage') load += inertiaLoad * 0.45

    points.push([Number(displacementValue.toFixed(2)), Number(load.toFixed(2))])
  }

  points.push(points[0])
  return points
})

function renderIndicatorChart() {
  if (!indicatorChartRef.value) return
  const { width, height } = indicatorChartRef.value.getBoundingClientRect()
  if (width < 320 || height < 120) {
    scheduleRenderChart()
    return
  }
  if (!chartInstance.value) chartInstance.value = echarts.init(indicatorChartRef.value)
  const loads = indicatorData.value.map(([, load]) => load)
  chartInstance.value.setOption({
    animation: false,
    grid: { top: 34, right: 18, bottom: 46, left: 54 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value', name: '悬点位移 m', min: 0, max: pumpStroke.value },
    yAxis: {
      type: 'value',
      name: '悬点载荷 kN',
      min: Math.floor(Math.min(...loads) - 2),
      max: Math.ceil(Math.max(...loads) + 2),
    },
    series: [
      {
        name: '示功图',
        type: 'line',
        data: indicatorData.value,
        showSymbol: false,
        lineStyle: { width: 2.5, color: '#178f86' },
        areaStyle: { color: 'rgba(23, 143, 134, 0.12)' },
      },
    ],
  }, true)
}

function resizeChart() {
  chartInstance.value?.resize()
}

function scheduleRenderChart() {
  window.requestAnimationFrame(() => {
    if (!chartInstance.value) {
      renderIndicatorChart()
      return
    }
    resizeChart()
  })
}

function animatePump(timestamp) {
  if (!lastFrameTime) lastFrameTime = timestamp
  const deltaSeconds = (timestamp - lastFrameTime) / 1000
  lastFrameTime = timestamp
  if (isPumpPlaying.value) {
    pumpPhase.value += (pumpStrokeTimes.value / 60) * Math.PI * 2 * deltaSeconds
  }
  animationFrameId = window.requestAnimationFrame(animatePump)
}

async function savePumpRecord() {
  try {
    await postSimulationRecord('/api/production/pump/save', {
      userId: null,
      stroke: pumpStroke.value,
      strokeTimes: pumpStrokeTimes.value,
      pumpDiameter: pumpDiameter.value,
      workCondition: pumpCondition.value,
      indicatorChartData: JSON.stringify(indicatorData.value),
    })
    ElMessage.success('抽油机仿真记录已保存')
  } catch (error) {
    console.error('保存抽油机仿真记录失败', error)
    ElMessage.warning('后端未连接，当前仅完成前端仿真')
  }
}

watch([pumpStrokeTimes, pumpStroke, pumpDiameter, pumpCondition], () => {
  renderIndicatorChart()
})

onMounted(async () => {
  await nextTick()
  window.requestAnimationFrame(() => {
    renderIndicatorChart()
  })
  chartResizeObserver.value = new ResizeObserver(() => {
    scheduleRenderChart()
  })
  if (indicatorChartRef.value) {
    chartResizeObserver.value.observe(indicatorChartRef.value)
  }
  animationFrameId = window.requestAnimationFrame(animatePump)
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.cancelAnimationFrame(animationFrameId)
  window.removeEventListener('resize', resizeChart)
  chartResizeObserver.value?.disconnect()
  chartInstance.value?.dispose()
})
</script>

<template>
  <section class="production-layout">
    <aside class="production-control">
      <el-card shadow="never">
        <template #header>抽油机参数</template>
        <div class="control-item">
          <div class="control-label"><span>冲次</span><strong>{{ pumpStrokeTimes }} 次/min</strong></div>
          <el-slider v-model="pumpStrokeTimes" :min="1" :max="12" :step="0.5" />
        </div>
        <div class="control-item">
          <div class="control-label"><span>冲程</span><strong>{{ pumpStroke }} m</strong></div>
          <el-slider v-model="pumpStroke" :min="1" :max="6" :step="0.2" />
        </div>
        <div class="control-item">
          <div class="control-label"><span>泵径</span><strong>{{ pumpDiameter }} mm</strong></div>
          <el-slider v-model="pumpDiameter" :min="28" :max="83" :step="1" />
        </div>
        <div class="control-item">
          <div class="control-label"><span>工况</span></div>
          <el-select v-model="pumpCondition" class="full-control">
            <el-option
              v-for="item in workConditionOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </div>
        <el-button type="primary" class="full-control" @click="isPumpPlaying = !isPumpPlaying">
          {{ isPumpPlaying ? '暂停动画' : '播放动画' }}
        </el-button>
        <el-button class="full-control secondary-action" @click="savePumpRecord">保存记录</el-button>
      </el-card>
    </aside>

    <section class="production-visual pump-visual pump-wide-visual">
      <el-card shadow="never">
        <template #header>游梁式抽油机 2D 动画</template>
        <svg class="pump-svg" viewBox="0 0 560 340" role="img" aria-label="游梁式抽油机二维动画">
          <rect x="42" y="294" width="470" height="18" rx="3" class="pump-base" />
          <path d="M250 292 L305 145 L358 292 Z" class="pump-tower" />
          <circle :cx="pumpGeometry.beamPivot.x" :cy="pumpGeometry.beamPivot.y" r="8" class="pump-joint" />
          <line :x1="pumpGeometry.leftBeam.x" :y1="pumpGeometry.leftBeam.y" :x2="pumpGeometry.horseHead.x" :y2="pumpGeometry.horseHead.y" class="pump-beam" />
          <path :d="pumpGeometry.headArcPath" class="pump-head" />
          <line :x1="pumpGeometry.crankCenter.x" :y1="pumpGeometry.crankCenter.y" :x2="pumpGeometry.crankPin.x" :y2="pumpGeometry.crankPin.y" class="pump-crank" />
          <line :x1="pumpGeometry.crankPin.x" :y1="pumpGeometry.crankPin.y" :x2="pumpGeometry.leftBeam.x" :y2="pumpGeometry.leftBeam.y" class="pump-rod" />
          <circle :cx="pumpGeometry.crankCenter.x" :cy="pumpGeometry.crankCenter.y" r="38" class="pump-wheel" />
          <circle :cx="pumpGeometry.crankPin.x" :cy="pumpGeometry.crankPin.y" r="6" class="pump-pin" />
          <line :x1="pumpGeometry.horseHead.x" :y1="pumpGeometry.horseHead.y + 40" :x2="pumpGeometry.horseHead.x" :y2="pumpGeometry.polishedRodY" class="pump-cable" />
          <rect :x="pumpGeometry.horseHead.x - 13" :y="pumpGeometry.polishedRodY" width="26" height="44" rx="4" class="pump-carrier" />
          <line :x1="pumpGeometry.horseHead.x" :y1="pumpGeometry.polishedRodY + 44" :x2="pumpGeometry.horseHead.x" y2="318" class="pump-well-line" />
        </svg>
      </el-card>

      <el-card shadow="never" class="pump-chart-card">
        <template #header>实时图形展示</template>
        <div ref="indicatorChartRef" class="production-chart pump-indicator-chart"></div>
      </el-card>
    </section>
  </section>
</template>
