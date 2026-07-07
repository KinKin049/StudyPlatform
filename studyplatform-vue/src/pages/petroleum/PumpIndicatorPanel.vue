<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { downloadTextReport } from './api'

/**
 * 游梁式抽油机动画与示功图面板。
 * 使用二维SVG表现游梁式抽油机的曲柄、连杆、游梁、驴头和悬绳器联动过程。
 * 右侧示功图根据杆柱静载荷、液柱静载荷和惯性动载荷生成，支持正常、供液不足、气体影响和漏失等典型工况。
 */

// 泵挂深度 m
const PUMP_DEPTH = 1500

// 动画播放状态
const isPumpPlaying = ref(true)
// 曲柄相位角（弧度）
const pumpPhase = ref(0)
// 冲次 次/min（用户输入参数）
const pumpStrokeTimes = ref(6)
// 冲程 m（用户输入参数）
const pumpStroke = ref(3)
// 泵径 mm（用户输入参数）
const pumpDiameter = ref(44)
// 当前工况：normal/under_supply/gas/leakage
const pumpCondition = ref('normal')
// 报告抽屉显示状态
const reportVisible = ref(false)
// 示功图图表DOM引用
const indicatorChartRef = ref(null)
// ECharts图表实例
const chartInstance = ref(null)
// 图表尺寸变化监听器
const chartResizeObserver = ref(null)
// 动画帧ID
let animationFrameId = 0
// 上一帧时间戳
let lastFrameTime = 0

// 工况选项列表
const workConditionOptions = [
  { label: '正常', value: 'normal' },
  { label: '供液不足', value: 'under_supply' },
  { label: '气体影响', value: 'gas' },
  { label: '漏失', value: 'leakage' },
]

// 泵筒横截面积 m²，根据泵径计算
const pumpArea = computed(() => {
  const diameterMeter = pumpDiameter.value / 1000
  return Math.PI * diameterMeter * diameterMeter / 4
})

// 载荷计算结果：杆柱静载荷、液柱静载荷、最大/最小静载荷、惯性动载荷
const pumpLoadRange = computed(() => {
  const rodLoad = 2.7 * PUMP_DEPTH * pumpArea.value
  const liquidLoad = 0.85 * PUMP_DEPTH * pumpArea.value
  const staticMax = rodLoad + liquidLoad
  const staticMin = rodLoad
  const inertiaLoad = 0.035 * pumpStroke.value * pumpStrokeTimes.value * pumpStrokeTimes.value
  return { rodLoad, liquidLoad, staticMax, staticMin, inertiaLoad }
})

// 抽油机几何参数计算，包括曲柄、连杆、游梁、驴头各部件的位置坐标和路径
const pumpGeometry = computed(() => {
  const phase = pumpPhase.value
  const amplitude = 12 + pumpStroke.value * 5
  const crankCenter = { x: 152, y: 248 }
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
  const rearBeam = {
    x: beamPivot.x - Math.cos(beamAngle) * 170,
    y: beamPivot.y - Math.sin(beamAngle) * 170,
  }
  const horseHead = {
    x: beamPivot.x + Math.cos(beamAngle) * 205,
    y: beamPivot.y + Math.sin(beamAngle) * 205,
  }
  const polishedRodY = 222 + Math.sin(phase) * amplitude
  const counterWeight = {
    x: crankCenter.x + Math.cos(phase + Math.PI) * (crankRadius + 10),
    y: crankCenter.y + Math.sin(phase + Math.PI) * (crankRadius + 10),
  }
  const headOuterTop = { x: horseHead.x - 10, y: horseHead.y - 54 }
  const headOuterBottom = { x: horseHead.x - 10, y: horseHead.y + 54 }
  const headOuterControl = { x: horseHead.x + 54, y: horseHead.y }
  const headInnerTop = { x: horseHead.x - 34, y: horseHead.y - 38 }
  const headInnerBottom = { x: horseHead.x - 34, y: horseHead.y + 38 }
  const headInnerControl = { x: horseHead.x + 20, y: horseHead.y }
  const cableX = horseHead.x + 5
  const cableTopY = horseHead.y + 43
  return {
    crankCenter,
    crankPin,
    beamPivot,
    leftBeam,
    rearBeam,
    horseHead,
    polishedRodY,
    counterWeight,
    beamBodyPath: `M ${rearBeam.x} ${rearBeam.y - 9} L ${horseHead.x - 10} ${horseHead.y - 13} L ${horseHead.x - 6} ${horseHead.y + 13} L ${rearBeam.x + 4} ${rearBeam.y + 9} Z`,
    headShellPath: `M ${headOuterTop.x} ${headOuterTop.y} Q ${headOuterControl.x} ${headOuterControl.y} ${headOuterBottom.x} ${headOuterBottom.y} L ${headInnerBottom.x} ${headInnerBottom.y} Q ${headInnerControl.x} ${headInnerControl.y} ${headInnerTop.x} ${headInnerTop.y} Z`,
    headInnerPath: `M ${headInnerTop.x} ${headInnerTop.y} Q ${headInnerControl.x} ${headInnerControl.y} ${headInnerBottom.x} ${headInnerBottom.y}`,
    headTopRibPath: `M ${headInnerTop.x} ${headInnerTop.y} L ${headOuterTop.x} ${headOuterTop.y}`,
    headBottomRibPath: `M ${headInnerBottom.x} ${headInnerBottom.y} L ${headOuterBottom.x} ${headOuterBottom.y}`,
    cableX,
    cableTopY,
  }
})

// 示功图数据点，上冲程从下到上，下冲程从上到下，形成闭合曲线
const indicatorData = computed(() => {
  const points = []
  const count = 80
  const { staticMax, staticMin, inertiaLoad } = pumpLoadRange.value

  // 上冲程：悬点载荷从最大到最小
  for (let index = 0; index <= count; index += 1) {
    const ratio = index / count
    const displacementValue = ratio * pumpStroke.value
    let load = staticMax + inertiaLoad * (0.25 + ratio)

    if (pumpCondition.value === 'under_supply' && ratio > 0.55) load -= inertiaLoad * 1.8 * (ratio - 0.55)
    if (pumpCondition.value === 'gas') load -= inertiaLoad * 0.55 * Math.sin(ratio * Math.PI)
    if (pumpCondition.value === 'leakage') load -= inertiaLoad * 0.35

    points.push([Number(displacementValue.toFixed(2)), Number(load.toFixed(2))])
  }

  // 下冲程：悬点载荷从最小到最大
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

// 报告表格数据行
const pumpReportRows = computed(() => [
  { name: '杆柱静载荷 Wr', value: `${pumpLoadRange.value.rodLoad.toFixed(2)} kN` },
  { name: '液柱静载荷 Wl', value: `${pumpLoadRange.value.liquidLoad.toFixed(2)} kN` },
  { name: '最大静载荷 Wmax', value: `${pumpLoadRange.value.staticMax.toFixed(2)} kN` },
  { name: '最小静载荷 Wmin', value: `${pumpLoadRange.value.staticMin.toFixed(2)} kN` },
  { name: '惯性动载荷', value: `${pumpLoadRange.value.inertiaLoad.toFixed(2)} kN` },
])

// 报告解释结论，根据当前工况、冲次和冲程生成综合评价
const pumpReportConclusion = computed(() => {
  const conditionText = workConditionOptions.find((item) => item.value === pumpCondition.value)?.label || '正常'
  const speedLevel = pumpStrokeTimes.value >= 9 ? '冲次较高，惯性载荷影响明显' : '冲次处于常规范围，载荷变化较平稳'
  const strokeLevel = pumpStroke.value >= 4.5 ? '冲程较长，悬点位移范围较大' : '冲程适中，适合观察常规抽汲过程'
  return `当前工况为${conditionText}。${speedLevel}；${strokeLevel}。示功图形态可用于判断泵况、供液状态和杆柱受力变化。`
})

/**
 * 渲染示功图图表，展示悬点位移与载荷的关系曲线
 */
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

/**
 * 调整图表尺寸
 */
function resizeChart() {
  chartInstance.value?.resize()
}

/**
 * 调度图表渲染，使用requestAnimationFrame优化性能
 */
function scheduleRenderChart() {
  window.requestAnimationFrame(() => {
    if (!chartInstance.value) {
      renderIndicatorChart()
      return
    }
    resizeChart()
  })
}

/**
 * 抽油机动画帧更新函数，根据冲次计算曲柄旋转速度
 * @param {number} timestamp - 帧时间戳
 */
function animatePump(timestamp) {
  if (!lastFrameTime) lastFrameTime = timestamp
  const deltaSeconds = (timestamp - lastFrameTime) / 1000
  lastFrameTime = timestamp
  if (isPumpPlaying.value) {
    pumpPhase.value += (pumpStrokeTimes.value / 60) * Math.PI * 2 * deltaSeconds
  }
  animationFrameId = window.requestAnimationFrame(animatePump)
}

/**
 * 下载抽油机展示功图解释报告到本地
 */
function downloadPumpReport() {
  const rows = pumpReportRows.value.map((row) => `${row.name}：${row.value}`).join('\n')
  const conditionText = workConditionOptions.find((item) => item.value === pumpCondition.value)?.label || '正常'
  const content = [
    '抽油机展示功图解释报告',
    '',
    '一、当前仿真参数',
    `冲次：${pumpStrokeTimes.value} 次/min`,
    `冲程：${pumpStroke.value} m`,
    `泵径：${pumpDiameter.value} mm`,
    `工况：${conditionText}`,
    '',
    '二、载荷计算结果',
    rows,
    '',
    '三、解释结论',
    pumpReportConclusion.value,
  ].join('\n')

  downloadTextReport(`抽油机展示功图解释报告_${Date.now()}.txt`, content)
  ElMessage.success('报告已下载到本地')
}

// 监听抽油机参数变化，重新计算载荷并渲染示功图
watch([pumpStrokeTimes, pumpStroke, pumpDiameter, pumpCondition], () => {
  renderIndicatorChart()
})

// 组件挂载时初始化图表、尺寸监听和动画
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

// 组件卸载时清理动画和资源
onBeforeUnmount(() => {
  window.cancelAnimationFrame(animationFrameId)
  window.removeEventListener('resize', resizeChart)
  chartResizeObserver.value?.disconnect()
  chartInstance.value?.dispose()
})
</script>

<template>
  <section class="production-layout">
    <!-- 左侧控制面板 -->
    <aside class="production-control">
      <!-- 抽油机参数配置卡片 -->
      <el-card shadow="never">
        <template #header>抽油机参数</template>
        <!-- 冲次滑块 -->
        <div class="control-item">
          <div class="control-label"><span>冲次</span><strong>{{ pumpStrokeTimes }} 次/min</strong></div>
          <el-slider v-model="pumpStrokeTimes" :min="1" :max="12" :step="0.5" />
        </div>
        <!-- 冲程滑块 -->
        <div class="control-item">
          <div class="control-label"><span>冲程</span><strong>{{ pumpStroke }} m</strong></div>
          <el-slider v-model="pumpStroke" :min="1" :max="6" :step="0.2" />
        </div>
        <!-- 泵径滑块 -->
        <div class="control-item">
          <div class="control-label"><span>泵径</span><strong>{{ pumpDiameter }} mm</strong></div>
          <el-slider v-model="pumpDiameter" :min="28" :max="83" :step="1" />
        </div>
        <!-- 工况选择器 -->
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
        <!-- 动画控制按钮 -->
        <el-button type="primary" class="full-control" @click="isPumpPlaying = !isPumpPlaying">
          {{ isPumpPlaying ? '暂停动画' : '播放动画' }}
        </el-button>
        <!-- 生成报告按钮 -->
        <el-button class="full-control secondary-action" @click="reportVisible = true">生成解释报告</el-button>
      </el-card>

      <!-- 仿真说明卡片 -->
      <el-card shadow="never" class="simulation-intro-card">
        <template #header>仿真说明</template>
        <p>
          该模块用二维 SVG 表现游梁式抽油机的曲柄、连杆、游梁、驴头和悬绳器联动过程。
          冲次控制曲柄角速度，冲程控制驴头摆动幅度和悬点位移范围。
        </p>
        <p>
          右侧展示功图根据杆柱静载荷、液柱静载荷和惯性动载荷生成，用于观察正常、供液不足、气体影响和漏失等典型工况差异。
        </p>
      </el-card>
    </aside>

    <!-- 右侧可视化展示区域 -->
    <section class="production-visual pump-visual pump-wide-visual">
      <!-- 抽油机2D动画卡片 -->
      <el-card shadow="never">
        <template #header>游梁式抽油机 2D 动画</template>
        <svg class="pump-svg" viewBox="0 0 640 390" role="img" aria-label="游梁式抽油机二维动画">
          <!-- SVG定义区域：渐变 -->
          <defs>
            <linearGradient id="pumpGroundGradient" x1="0" x2="1">
              <stop offset="0%" stop-color="#e8f4f1" />
              <stop offset="100%" stop-color="#f8fbfc" />
            </linearGradient>
            <linearGradient id="pumpSteelGradient" x1="0" x2="1">
              <stop offset="0%" stop-color="#203747" />
              <stop offset="100%" stop-color="#526a75" />
            </linearGradient>
          </defs>
          <!-- 背景元素：天空、底座、地面 -->
          <rect x="0" y="0" width="640" height="390" class="pump-sky" />
          <rect x="30" y="315" width="570" height="24" rx="4" class="pump-base" />
          <rect x="0" y="339" width="640" height="51" fill="url(#pumpGroundGradient)" />
          <path d="M58 354 C118 340 174 350 230 342 S342 354 412 342 S530 338 604 352" class="pump-ground-line" />
          <!-- 塔架 -->
          <path d="M244 315 L305 145 L372 315 Z" class="pump-tower" />
          <path d="M270 315 L305 145 L340 315" class="pump-tower-brace" />
          <path d="M252 255 L354 255 M265 218 L342 218 M280 181 L328 181" class="pump-tower-rung" />
          <!-- 电机和变速箱 -->
          <rect x="92" y="256" width="116" height="58" rx="10" class="pump-gearbox" />
          <circle cx="114" cy="285" r="18" class="pump-motor-wheel" />
          <circle cx="196" cy="285" r="22" class="pump-motor-wheel" />
          <path d="M114 267 C132 242 178 242 196 263" class="pump-belt" />
          <rect x="54" y="274" width="52" height="30" rx="6" class="pump-motor" />
          <!-- 游梁和驴头 -->
          <circle :cx="pumpGeometry.beamPivot.x" :cy="pumpGeometry.beamPivot.y" r="8" class="pump-joint" />
          <path :d="pumpGeometry.beamBodyPath" class="pump-beam-body" />
          <line :x1="pumpGeometry.leftBeam.x" :y1="pumpGeometry.leftBeam.y" :x2="pumpGeometry.horseHead.x" :y2="pumpGeometry.horseHead.y" class="pump-beam" />
          <circle :cx="pumpGeometry.rearBeam.x" :cy="pumpGeometry.rearBeam.y" r="12" class="pump-tail-bearing" />
          <path :d="pumpGeometry.headShellPath" class="pump-head-shell" />
          <path :d="pumpGeometry.headInnerPath" class="pump-head-inner" />
          <path :d="pumpGeometry.headTopRibPath" class="pump-head-rib" />
          <path :d="pumpGeometry.headBottomRibPath" class="pump-head-rib" />
          <!-- 曲柄和连杆 -->
          <line :x1="pumpGeometry.crankCenter.x" :y1="pumpGeometry.crankCenter.y" :x2="pumpGeometry.crankPin.x" :y2="pumpGeometry.crankPin.y" class="pump-crank" />
          <line :x1="pumpGeometry.crankPin.x" :y1="pumpGeometry.crankPin.y" :x2="pumpGeometry.leftBeam.x" :y2="pumpGeometry.leftBeam.y" class="pump-rod" />
          <circle :cx="pumpGeometry.counterWeight.x" :cy="pumpGeometry.counterWeight.y" r="15" class="pump-counterweight" />
          <path :d="`M ${pumpGeometry.crankCenter.x - 38} ${pumpGeometry.crankCenter.y} A 38 38 0 1 1 ${pumpGeometry.crankCenter.x + 38} ${pumpGeometry.crankCenter.y}`" class="pump-wheel-track" />
          <circle :cx="pumpGeometry.crankCenter.x" :cy="pumpGeometry.crankCenter.y" r="38" class="pump-wheel" />
          <circle :cx="pumpGeometry.crankCenter.x" :cy="pumpGeometry.crankCenter.y" r="9" class="pump-joint" />
          <circle :cx="pumpGeometry.crankPin.x" :cy="pumpGeometry.crankPin.y" r="6" class="pump-pin" />
          <!-- 悬绳器和井口 -->
          <line :x1="pumpGeometry.cableX" :y1="pumpGeometry.cableTopY" :x2="pumpGeometry.cableX" :y2="pumpGeometry.polishedRodY" class="pump-cable" />
          <rect :x="pumpGeometry.cableX - 13" :y="pumpGeometry.polishedRodY" width="26" height="44" rx="4" class="pump-carrier" />
          <line :x1="pumpGeometry.cableX" :y1="pumpGeometry.polishedRodY + 44" :x2="pumpGeometry.cableX" y2="346" class="pump-well-line" />
          <rect :x="pumpGeometry.cableX - 34" y="313" width="68" height="10" rx="3" class="pump-wellhead" />
          <rect :x="pumpGeometry.cableX - 18" y="323" width="36" height="32" rx="4" class="pump-wellhead-body" />
          <path :d="`M ${pumpGeometry.cableX - 36} 333 H ${pumpGeometry.cableX - 82} M ${pumpGeometry.cableX + 36} 333 H ${pumpGeometry.cableX + 82}`" class="pump-flowline" />
          <!-- 标注文字 -->
          <text x="54" y="366" class="pump-label">Motor</text>
          <text x="112" y="246" class="pump-label">Gearbox & crank</text>
          <text x="360" y="128" class="pump-label">Walking beam</text>
          <text :x="pumpGeometry.cableX - 28" y="374" class="pump-label">Wellhead</text>
        </svg>
      </el-card>

      <!-- 示功图图表卡片 -->
      <el-card shadow="never" class="pump-chart-card">
        <template #header>实时图形展示</template>
        <div ref="indicatorChartRef" class="production-chart pump-indicator-chart"></div>
      </el-card>
    </section>
  </section>

  <!-- 抽油机展示功图解释报告抽屉 -->
  <el-drawer v-model="reportVisible" title="抽油机展示功图解释报告" size="44%">
    <!-- 当前仿真参数 -->
    <section class="report-section">
      <h3>当前仿真参数</h3>
      <p>冲次：{{ pumpStrokeTimes }} 次/min</p>
      <p>冲程：{{ pumpStroke }} m</p>
      <p>泵径：{{ pumpDiameter }} mm</p>
      <p>工况：{{ workConditionOptions.find((item) => item.value === pumpCondition)?.label }}</p>
    </section>

    <!-- 载荷计算结果表格 -->
    <section class="report-section">
      <h3>载荷计算结果</h3>
      <el-table :data="pumpReportRows" border>
        <el-table-column prop="name" label="指标" width="160" />
        <el-table-column prop="value" label="结果" />
      </el-table>
    </section>

    <!-- 解释结论 -->
    <section class="report-section">
      <h3>解释结论</h3>
      <p>{{ pumpReportConclusion }}</p>
    </section>

    <!-- 下载报告按钮 -->
    <el-button type="primary" @click="downloadPumpReport">下载报告到本地</el-button>
  </el-drawer>
</template>
