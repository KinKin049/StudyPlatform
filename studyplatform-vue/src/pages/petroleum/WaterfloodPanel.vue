<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { downloadTextReport } from './api'

/**
 * 注水开发仿真面板。
 */

const injectionRate = ref(100)
const waterfloodDay = ref(180)
const waterfloodPlaying = ref(false)
const waterfloodTimer = ref(null)
const reportVisible = ref(false)
const waterfloodChartRef = ref(null)
const chartInstance = ref(null)
const chartResizeObserver = ref(null)

const waterfloodKeyDays = computed(() => ({
  effectDay: Math.round(Math.max(25, Math.min(90, 90 - injectionRate.value * 0.18))),
  breakthroughDay: Math.round(Math.max(120, Math.min(260, 230 - injectionRate.value * 0.35))),
}))

const waterfloodFullCurve = computed(() => {
  const { effectDay, breakthroughDay } = waterfloodKeyDays.value
  const peakOil = 22 + injectionRate.value * 0.13
  const initialOil = 12
  const initialWaterCut = 0.22

  return Array.from({ length: 366 }, (_, day) => {
    let dailyOil
    let waterCut

    if (day <= effectDay) {
      const ratio = day / effectDay
      dailyOil = initialOil + (peakOil - initialOil) * ratio * 0.78
      waterCut = initialWaterCut + ratio * 0.02
    } else if (day <= breakthroughDay) {
      const ratio = (day - effectDay) / (breakthroughDay - effectDay)
      dailyOil = peakOil - Math.sin(ratio * Math.PI) * 1.2
      waterCut = 0.24 + ratio * 0.05
    } else {
      const ratio = (day - breakthroughDay) / (365 - breakthroughDay)
      const declineRate = 0.48 + injectionRate.value / 900
      dailyOil = Math.max(4, peakOil * (1 - ratio * declineRate))
      waterCut = Math.min(0.92, 0.32 + ratio * (0.48 + injectionRate.value / 800))
    }

    const dailyLiquid = dailyOil / Math.max(0.08, 1 - waterCut)
    const dailyWater = dailyLiquid - dailyOil

    return {
      day,
      dailyOil: Number(dailyOil.toFixed(2)),
      dailyWater: Number(dailyWater.toFixed(2)),
      waterCut: Number((waterCut * 100).toFixed(2)),
    }
  })
})

const waterfloodVisibleCurve = computed(() =>
  waterfloodFullCurve.value.filter((point) => point.day <= waterfloodDay.value),
)

const waterfloodSummary = computed(() => {
  const peakOil = Math.max(...waterfloodFullCurve.value.map((point) => point.dailyOil))
  return {
    ...waterfloodKeyDays.value,
    peakOil: Number(peakOil.toFixed(2)),
  }
})

const currentWaterfloodPoint = computed(() =>
  waterfloodFullCurve.value.find((point) => point.day === waterfloodDay.value) || waterfloodFullCurve.value[0],
)

const waterfloodStage = computed(() => {
  if (waterfloodDay.value <= waterfloodSummary.value.effectDay) return '见效期'
  if (waterfloodDay.value <= waterfloodSummary.value.breakthroughDay) return '稳产期'
  return '水淹期'
})

const waterfloodReportRows = computed(() => [
  { name: '见效时间', value: `${waterfloodSummary.value.effectDay} d` },
  { name: '见水时间', value: `${waterfloodSummary.value.breakthroughDay} d` },
  { name: '峰值日产油', value: `${waterfloodSummary.value.peakOil} t/d` },
  { name: '当前日产油', value: `${currentWaterfloodPoint.value.dailyOil} t/d` },
  { name: '当前日产水', value: `${currentWaterfloodPoint.value.dailyWater} t/d` },
  { name: '当前含水率', value: `${currentWaterfloodPoint.value.waterCut}%` },
])

const waterfloodReportConclusion = computed(() => {
  if (waterfloodStage.value === '见效期') {
    return '当前处于注水见效期，地层能量逐步恢复，日产油随注水推进呈上升趋势，含水率整体较稳定。'
  }
  if (waterfloodStage.value === '稳产期') {
    return '当前处于稳产期，日产油接近峰值并保持相对稳定，应重点关注注采平衡和压力保持水平。'
  }
  return '当前处于水淹期，注入水已突破并导致含水率快速升高，日产油递减、日产水增加，应考虑调剖堵水或优化注采井网。'
})

function renderWaterfloodChart() {
  if (!waterfloodChartRef.value) return
  const { width, height } = waterfloodChartRef.value.getBoundingClientRect()
  if (width < 360 || height < 160) {
    scheduleRenderChart()
    return
  }
  if (!chartInstance.value) chartInstance.value = echarts.init(waterfloodChartRef.value)
  const curve = waterfloodVisibleCurve.value
  chartInstance.value.setOption({
    animation: false,
    grid: { top: 42, right: 46, bottom: 72, left: 54 },
    legend: { top: 4 },
    tooltip: { trigger: 'axis' },
    dataZoom: [
      { type: 'inside', xAxisIndex: 0, filterMode: 'none' },
      { type: 'slider', xAxisIndex: 0, height: 22, bottom: 18, filterMode: 'none' },
    ],
    xAxis: {
      type: 'value',
      name: '开发天数 d',
      min: 0,
      max: 365,
      interval: 30,
      axisLabel: { formatter: '{value} d' },
    },
    yAxis: [
      { type: 'value', name: '产量 t/d' },
      { type: 'value', name: '含水率 %', min: 0, max: 100 },
    ],
    series: [
      {
        name: '日产油',
        type: 'line',
        data: curve.map((point) => [point.day, point.dailyOil]),
        showSymbol: false,
        lineStyle: { color: '#f2b94b', width: 2.5 },
        markLine: {
          symbol: 'none',
          data: [
            { name: '见效时间', xAxis: waterfloodSummary.value.effectDay },
            { name: '见水时间', xAxis: waterfloodSummary.value.breakthroughDay },
          ],
        },
      },
      {
        name: '日产水',
        type: 'line',
        data: curve.map((point) => [point.day, point.dailyWater]),
        showSymbol: false,
        lineStyle: { color: '#4aa6d8', width: 2.5 },
      },
      {
        name: '含水率',
        type: 'line',
        yAxisIndex: 1,
        data: curve.map((point) => [point.day, point.waterCut]),
        showSymbol: false,
        lineStyle: { color: '#526a75', width: 2.5 },
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
      renderWaterfloodChart()
      return
    }
    resizeChart()
    renderWaterfloodChart()
  })
}

function toggleWaterfloodPlayback() {
  waterfloodPlaying.value = !waterfloodPlaying.value
  if (!waterfloodPlaying.value) {
    window.clearInterval(waterfloodTimer.value)
    waterfloodTimer.value = null
    return
  }

  waterfloodTimer.value = window.setInterval(() => {
    waterfloodDay.value = waterfloodDay.value >= 365 ? 0 : waterfloodDay.value + 1
  }, 80)
}

function downloadWaterfloodReport() {
  const rows = waterfloodReportRows.value.map((row) => `${row.name}：${row.value}`).join('\n')
  const content = [
    '注水开发解释报告',
    '',
    '一、当前仿真参数',
    `日配注量：${injectionRate.value} m³/d`,
    `模拟时间：${waterfloodDay.value} d`,
    `当前阶段：${waterfloodStage.value}`,
    '',
    '二、开发指标',
    rows,
    '',
    '三、解释结论',
    waterfloodReportConclusion.value,
  ].join('\n')

  downloadTextReport(`注水开发解释报告_${Date.now()}.txt`, content)
  ElMessage.success('报告已下载到本地')
}

watch([injectionRate, waterfloodDay], renderWaterfloodChart)

onMounted(async () => {
  await nextTick()
  window.requestAnimationFrame(() => {
    renderWaterfloodChart()
  })
  chartResizeObserver.value = new ResizeObserver(() => {
    scheduleRenderChart()
  })
  if (waterfloodChartRef.value) {
    chartResizeObserver.value.observe(waterfloodChartRef.value)
  }
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.clearInterval(waterfloodTimer.value)
  window.removeEventListener('resize', resizeChart)
  chartResizeObserver.value?.disconnect()
  chartInstance.value?.dispose()
})
</script>

<template>
  <section class="production-layout">
    <aside class="production-control">
      <el-card shadow="never">
        <template #header>注水参数</template>
        <div class="control-item">
          <div class="control-label"><span>日配注量</span><strong>{{ injectionRate }} m³/d</strong></div>
          <el-slider v-model="injectionRate" :min="0" :max="300" :step="10" />
        </div>
        <div class="control-item">
          <div class="control-label"><span>模拟时间</span><strong>{{ waterfloodDay }} d</strong></div>
          <el-slider v-model="waterfloodDay" :min="0" :max="365" :step="1" />
        </div>
        <div class="summary-list">
          <p>见效时间：{{ waterfloodSummary.effectDay }} d</p>
          <p>见水时间：{{ waterfloodSummary.breakthroughDay }} d</p>
          <p>峰值日产油：{{ waterfloodSummary.peakOil }} t/d</p>
        </div>
        <el-button type="primary" class="full-control" @click="toggleWaterfloodPlayback">
          {{ waterfloodPlaying ? '暂停播放' : '自动播放' }}
        </el-button>
        <el-button class="full-control secondary-action" @click="reportVisible = true">生成解释报告</el-button>
      </el-card>

      <el-card shadow="never" class="simulation-intro-card">
        <template #header>仿真说明</template>
        <p>
          注水开发模块模拟注水井向油藏补充能量后的生产响应，时间轴覆盖见效、稳产和水淹三个阶段。
          配注量越大，见效越快，但水突破也会提前。
        </p>
        <p>
          曲线中日产油、日产水和含水率共享开发天数横轴，标注线用于定位见效时间和见水时间，便于分析注采制度变化。
        </p>
      </el-card>
    </aside>

    <section class="production-visual waterflood-visual">
      <el-card shadow="never" class="chart-card-fill waterflood-chart-card">
        <template #header>注水见效、稳产与水淹过程</template>
        <div ref="waterfloodChartRef" class="production-chart waterflood-chart"></div>
      </el-card>
    </section>
  </section>

  <el-drawer v-model="reportVisible" title="注水开发解释报告" size="44%">
    <section class="report-section">
      <h3>当前仿真参数</h3>
      <p>日配注量：{{ injectionRate }} m³/d</p>
      <p>模拟时间：{{ waterfloodDay }} d</p>
      <p>当前阶段：{{ waterfloodStage }}</p>
    </section>

    <section class="report-section">
      <h3>开发指标</h3>
      <el-table :data="waterfloodReportRows" border>
        <el-table-column prop="name" label="指标" width="130" />
        <el-table-column prop="value" label="结果" />
      </el-table>
    </section>

    <section class="report-section">
      <h3>解释结论</h3>
      <p>{{ waterfloodReportConclusion }}</p>
    </section>

    <el-button type="primary" @click="downloadWaterfloodReport">下载报告到本地</el-button>
  </el-drawer>
</template>
