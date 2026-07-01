<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'

/**
 * 测井曲线仿真页面。
 * 页面内完成岩性判别、曲线计算、层位合并和 ECharts 实时渲染；
 * 后端接口仅用于模板与报告记录持久化，不参与任何仿真计算。
 */

// 岩石物理固定参数，与题目要求保持一致。
const AC_MATRIX = 180
const AC_FLUID = 600
const ARCHIE_A = 1
const ARCHIE_M = 2
const ARCHIE_N = 2
const WATER_RESISTIVITY = 0.12

// 默认剖面范围与采样间隔，四道曲线共享同一深度轴。
const MAX_DEPTH = 2000
const DEPTH_STEP = 20

// 后端接口基础地址，可通过 Vite 环境变量覆盖。
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// 图表 DOM、ECharts 实例和尺寸监听器引用。
const chartWrapRef = ref(null)
const chartRef = ref(null)
const chartInstance = ref(null)
const chartResizeObserver = ref(null)

// 页面交互状态，滑块变化会直接触发曲线重算和图表重绘。
const porosityPercent = ref(20)
const oilSaturationPercent = ref(60)
const reportVisible = ref(false)
const savingReport = ref(false)

// 内置默认深度序列，单位 m。
const depthArray = Array.from({ length: MAX_DEPTH / DEPTH_STEP + 1 }, (_, index) => index * DEPTH_STEP)

// 内置基础GR数据。GR只反映岩性，不受孔隙度和含油饱和度滑块影响。
const grBase = depthArray.map((depth) => {
  if (depth < 260) return 95 + Math.sin(depth / 38) * 8
  if (depth < 620) return 42 + Math.sin(depth / 44) * 7
  if (depth < 880) return 88 + Math.sin(depth / 35) * 9
  if (depth < 1260) return 36 + Math.sin(depth / 50) * 8
  if (depth < 1500) return 92 + Math.sin(depth / 42) * 8
  if (depth < 1840) return 48 + Math.sin(depth / 45) * 9
  return 86 + Math.sin(depth / 40) * 7
})

// 根据 GR 阈值生成岩性柱状图层段，连续相同岩性会合并为一个绘制块。
const lithologyIntervals = computed(() =>
  mergeIntervals(
    depthArray.map((depth, index) => ({
      depth,
      type: grBase[index] > 80 ? '泥岩' : '砂岩',
    })),
  ),
)

// GR 曲线只由内置基础数据生成，不随滑块变化。
const grCurve = computed(() => depthArray.map((depth, index) => [Number(grBase[index].toFixed(2)), depth]))

// 声波时差曲线由威利公式反算，叠加轻微岩性纹理便于默认状态下观察层间差异。
const acCurve = computed(() => {
  const porosity = porosityPercent.value / 100
  const acValue = porosity * (AC_FLUID - AC_MATRIX) + AC_MATRIX
  return depthArray.map((depth, index) => {
    const lithologyOffset = grBase[index] > 80 ? -10 : 10
    const layerTexture = Math.sin(depth / 80) * 4
    return [Number((acValue + lithologyOffset + layerTexture).toFixed(2)), depth]
  })
})

// 电阻率曲线由简化阿尔奇公式反算，使用对数轴显示行业常见的跨度变化。
const rtCurve = computed(() => {
  const porosity = Math.max(porosityPercent.value / 100, 0.001)
  const waterSaturation = Math.max(1 - oilSaturationPercent.value / 100, 0.001)
  const rtValue =
    (ARCHIE_A * WATER_RESISTIVITY) /
    (Math.pow(porosity, ARCHIE_M) * Math.pow(waterSaturation, ARCHIE_N))

  return depthArray.map((depth, index) => {
    const lithologyFactor = grBase[index] > 80 ? 0.45 : 1.35
    const layerTexture = 1 + Math.sin(depth / 90) * 0.08
    return [Number((rtValue * lithologyFactor * layerTexture).toFixed(3)), depth]
  })
})

// 对数电阻率轴上限按数据自动扩展，避免高含油饱和度时曲线越界。
const rtAxisMax = computed(() => {
  const maxValue = Math.max(...rtCurve.value.map(([value]) => value))
  return Math.max(1000, Math.pow(10, Math.ceil(Math.log10(maxValue))))
})

// 逐深度点判别层位，并合并连续同类型层段供图表和报告复用。
const interpretedLayers = computed(() => {
  const classifiedPoints = depthArray.map((depth, index) => ({
    depth,
    type: classifyLayer(grBase[index], porosityPercent.value, oilSaturationPercent.value),
  }))

  return mergeIntervals(classifiedPoints).map((layer, index) => ({
    ...layer,
    index: index + 1,
    conclusion: layerConclusion(layer.type),
  }))
})

// ECharts markArea 背景色保持不透明，避免影响岩性块和曲线辨识。
const markAreaData = computed(() =>
  interpretedLayers.value.map((layer) => [
    {
      yAxis: layer.topDepth,
      itemStyle: {
        color: layerColor(layer.type),
        opacity: 1,
      },
      label: { show: false },
    },
    { yAxis: layer.bottomDepth },
  ]),
)

/**
 * 按 GR、孔隙度和含油饱和度识别层位类型。
 */
function classifyLayer(grValue, porosity, oilSaturation) {
  if (grValue > 80) return '泥岩'
  if (porosity < 10) return '干层'
  if (oilSaturation < 20) return '水层'
  if (oilSaturation >= 50) return '油气层'
  return '干层'
}

/**
 * 将逐点判别结果合并为连续层段，减少图表绘制对象和报告行数。
 */
function mergeIntervals(points) {
  const intervals = []
  let current = null

  points.forEach((point, index) => {
    const nextDepth = points[index + 1]?.depth ?? Math.min(point.depth + DEPTH_STEP, MAX_DEPTH)

    if (!current || current.type !== point.type) {
      if (current) intervals.push(current)
      current = {
        type: point.type,
        topDepth: point.depth,
        bottomDepth: nextDepth,
      }
      return
    }

    current.bottomDepth = nextDepth
  })

  if (current) intervals.push(current)
  return intervals
}

/**
 * 返回层位可视化颜色，图表标注与右侧层位文本共用同一套色值。
 */
function layerColor(type) {
  const colorMap = {
    泥岩: '#3f454f',
    干层: '#c9d1d9',
    水层: '#8fd3ff',
    油气层: '#f2b94b',
  }
  return colorMap[type] || '#d8dee8'
}

/**
 * 返回层位解释结论，用于生成报告表格。
 */
function layerConclusion(type) {
  const conclusionMap = {
    泥岩: 'GR高于80API，解释为泥质层，非优质储层。',
    干层: 'GR较低但孔隙度不足10%，储集能力弱，解释为干层。',
    水层: '孔隙度达标但含油饱和度低于20%，解释为水层。',
    油气层: '低GR、孔隙度达标且含油饱和度高，建议重点评价。',
  }
  return conclusionMap[type] || '需结合更多资料复核。'
}

// 标题样式统一封装，保证四道标题视觉一致。
function titleStyle() {
  return {
    color: '#233f4d',
    fontSize: 14,
    fontWeight: 800,
  }
}

/**
 * 创建单道 X 轴配置。
 * 岩心道隐藏刻度标签，GR/RT/AC 曲线道显示行业单位刻度。
 */
function coreAxis(gridIndex, type, min, max, axisColor, showLabel) {
  return {
    type,
    gridIndex,
    min,
    max,
    axisLine: { show: true, lineStyle: { color: axisColor } },
    axisTick: { show: showLabel },
    axisLabel: { show: showLabel, color: axisColor },
    splitLine: { show: true, lineStyle: { color: '#edf2f7' } },
  }
}

/**
 * 创建测井曲线序列配置。
 */
function curveSeries(name, data, axisIndex, color) {
  return {
    name,
    type: 'line',
    xAxisIndex: axisIndex,
    yAxisIndex: axisIndex,
    data,
    showSymbol: false,
    lineStyle: { color, width: 2 },
  }
}

/**
 * 绘制岩心柱状图的单个岩性矩形块。
 */
function renderLithologyBlock(interval) {
  return (_params, api) => {
    const start = api.coord([0, interval.topDepth])
    const end = api.coord([1, interval.bottomDepth])

    return {
      type: 'rect',
      shape: {
        x: start[0],
        y: start[1],
        width: end[0] - start[0],
        height: Math.max(1, end[1] - start[1]),
      },
      style: {
        fill: interval.type === '砂岩' ? '#f7e2a4' : '#3f454f',
        stroke: '#ffffff',
        lineWidth: 1,
        opacity: 1,
      },
    }
  }
}

/**
 * 组装完整 ECharts 配置。
 * 四个 grid 横向排列并共享 0-2000m 反向深度轴，实现测井剖面对齐。
 */
function createChartOption() {
  const axisColor = '#526a75'
  const grid = [
    { left: '3%', width: '18%', top: 42, bottom: 38, containLabel: true },
    { left: '25%', width: '18%', top: 42, bottom: 38, containLabel: true },
    { left: '47%', width: '18%', top: 42, bottom: 38, containLabel: true },
    { left: '69%', width: '18%', top: 42, bottom: 38, containLabel: true },
  ]

  return {
    animation: false,
    backgroundColor: '#ffffff',
    title: [
      { text: '岩心', left: '11%', top: 8, textAlign: 'center', textStyle: titleStyle() },
      { text: 'GR API', left: '33%', top: 8, textAlign: 'center', textStyle: titleStyle() },
      { text: 'RT Ω·m', left: '55%', top: 8, textAlign: 'center', textStyle: titleStyle() },
      { text: 'AC μs/m', left: '77%', top: 8, textAlign: 'center', textStyle: titleStyle() },
    ],
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      valueFormatter: (value) => Number(value).toFixed(2),
    },
    grid,
    xAxis: [
      coreAxis(0, 'value', 0, 1, axisColor, false),
      coreAxis(1, 'value', 0, 150, axisColor, true),
      coreAxis(2, 'log', 0.1, rtAxisMax.value, axisColor, true),
      coreAxis(3, 'value', 150, 360, axisColor, true),
    ],
    yAxis: [0, 1, 2, 3].map((gridIndex) => ({
      type: 'value',
      gridIndex,
      inverse: true,
      min: 0,
      max: MAX_DEPTH,
      name: gridIndex === 0 ? '深度 m' : '',
      nameLocation: 'middle',
      nameGap: 48,
      axisLine: { show: true, lineStyle: { color: axisColor } },
      axisTick: { show: true },
      axisLabel: { color: axisColor },
      splitLine: { show: true, lineStyle: { color: '#edf2f7' } },
    })),
    series: [
      ...lithologyIntervals.value.map((interval) => ({
        name: interval.type,
        type: 'custom',
        xAxisIndex: 0,
        yAxisIndex: 0,
        renderItem: renderLithologyBlock(interval),
        data: [[0, interval.topDepth, interval.bottomDepth]],
        silent: true,
      })),
      {
        ...curveSeries('GR', grCurve.value, 1, '#2f7ed8'),
        markArea: {
          silent: true,
          data: markAreaData.value,
        },
      },
      {
        ...curveSeries('RT', rtCurve.value, 2, '#c86b2d'),
        markArea: {
          silent: true,
          data: markAreaData.value,
        },
      },
      {
        ...curveSeries('AC', acCurve.value, 3, '#178f86'),
        markArea: {
          silent: true,
          data: markAreaData.value,
        },
      },
    ],
  }
}

/**
 * 渲染或更新图表。
 * 当容器尺寸尚未稳定时延后到下一帧，避免 ECharts 初始化到 0 高度容器。
 */
function renderChart() {
  if (!chartRef.value) return
  const { width, height } = chartRef.value.getBoundingClientRect()
  if (width < 20 || height < 20) {
    scheduleResizeChart()
    return
  }
  if (!chartInstance.value) {
    chartInstance.value = echarts.init(chartRef.value)
  }
  chartInstance.value.setOption(createChartOption(), true)
}

// 浏览器窗口或分割面板尺寸变化时同步调整 ECharts 画布。
function resizeChart() {
  chartInstance.value?.resize()
}

// 将尺寸更新推迟到下一帧，等待 Element Plus 布局完成。
function scheduleResizeChart() {
  window.requestAnimationFrame(() => {
    if (!chartInstance.value) {
      renderChart()
      return
    }
    resizeChart()
  })
}

// 构造解释报告数据，保存接口直接接收该对象的 JSON 字符串。
function buildReportPayload() {
  return {
    porosity: porosityPercent.value,
    oilSaturation: oilSaturationPercent.value,
    layers: interpretedLayers.value,
  }
}

// 打开报告抽屉，报告内容由当前响应式计算结果即时生成。
function openReport() {
  reportVisible.value = true
}

/**
 * 保存报告到后端。
 * 保存失败时不影响前端报告生成，便于离线演示仿真功能。
 */
async function saveReport() {
  const reportPayload = buildReportPayload()
  savingReport.value = true

  try {
    // 后端持久化接口调用位置：仿真计算仍全部在前端完成，只提交参数与解释报告JSON。
    const response = await fetch(`${API_BASE}/api/well-log/record/save`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: null,
        porosity: porosityPercent.value,
        oilSaturation: oilSaturationPercent.value,
        reportJson: JSON.stringify(reportPayload),
      }),
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    ElMessage.success('报告已保存')
  } catch (error) {
    console.error('保存测井解释报告失败', error)
    ElMessage.warning('当前未连接后端，报告已在前端生成')
  } finally {
    savingReport.value = false
  }
}

// 双滑块变化时实时刷新曲线和层位识别结果。
watch([porosityPercent, oilSaturationPercent], () => {
  renderChart()
})

// 页面挂载后初始化图表，并监听外层容器尺寸变化。
onMounted(async () => {
  await nextTick()
  window.requestAnimationFrame(() => {
    renderChart()
  })
  chartResizeObserver.value = new ResizeObserver(() => {
    scheduleResizeChart()
  })
  if (chartWrapRef.value) {
    chartResizeObserver.value.observe(chartWrapRef.value)
  }
  window.addEventListener('resize', resizeChart)
})

// 页面销毁时释放监听器和 ECharts 实例，避免重复进入页面时残留资源。
onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chartResizeObserver.value?.disconnect()
  chartInstance.value?.dispose()
})
</script>

<template>
  <main class="well-log-page">
    <header class="well-log-header">
      <div>
        <p class="well-log-kicker">Petroleum Simulation</p>
        <h1>测井曲线仿真平台</h1>
      </div>
      <a class="well-log-home-link" href="/">返回首页</a>
    </header>

    <section class="well-log-content">
      <el-splitter class="well-log-layout" @resize="resizeChart" @resize-end="resizeChart">
        <el-splitter-panel size="20%" min="260px" class="well-log-control">
          <div class="well-log-panel well-log-control-panel">
            <el-card shadow="never">
              <template #header>
                <span>仿真参数</span>
              </template>

              <div class="control-item">
                <div class="control-label">
                  <span>孔隙度 φ</span>
                  <strong>{{ porosityPercent }}%</strong>
                </div>
                <el-slider v-model="porosityPercent" :min="0" :max="35" :step="0.5" />
              </div>

              <div class="control-item">
                <div class="control-label">
                  <span>含油饱和度 So</span>
                  <strong>{{ oilSaturationPercent }}%</strong>
                </div>
                <el-slider v-model="oilSaturationPercent" :min="0" :max="100" :step="1" />
              </div>

              <el-button type="primary" class="report-button" @click="openReport">
                生成解释报告
              </el-button>
            </el-card>

            <el-card shadow="never" class="formula-card">
              <template #header>
                <span>计算模型</span>
              </template>
              <p>AC = φ × (600 - 180) + 180</p>
              <p>RT = (1 × 0.12) / (φ² × (1 - So)²)</p>
              <p>GR为岩性基础曲线，仅用于岩性判别。</p>
            </el-card>
          </div>
        </el-splitter-panel>

        <el-splitter-panel size="80%" min="620px" class="well-log-main">
          <div class="well-log-panel well-log-main-panel">
            <el-card shadow="never" class="well-log-chart-card">
              <template #header>
                <div class="chart-header">
                  <span>岩心 + 测井曲线联动剖面</span>
                  <small>深度 0-2000m，自上而下递增</small>
                </div>
              </template>

              <div ref="chartWrapRef" class="chart-wrap">
                <div ref="chartRef" class="well-log-chart"></div>
                <aside class="layer-labels" aria-label="层位标注">
                  <div
                    v-for="layer in interpretedLayers"
                    :key="`${layer.index}-${layer.topDepth}`"
                    class="layer-label"
                    :style="{
                      top: `${(layer.topDepth / MAX_DEPTH) * 100}%`,
                      height: `${((layer.bottomDepth - layer.topDepth) / MAX_DEPTH) * 100}%`,
                      backgroundColor: layerColor(layer.type),
                    }"
                  >
                    <span>{{ layer.type }}</span>
                  </div>
                </aside>
              </div>
            </el-card>
          </div>
        </el-splitter-panel>
      </el-splitter>
    </section>

    <el-drawer v-model="reportVisible" title="测井解释报告" size="46%">
      <section class="report-section">
        <h3>当前仿真参数</h3>
        <p>孔隙度 φ：{{ porosityPercent }}%</p>
        <p>含油饱和度 So：{{ oilSaturationPercent }}%</p>
      </section>

      <section class="report-section">
        <h3>分层结果</h3>
        <el-table :data="interpretedLayers" border>
          <el-table-column prop="index" label="层号" width="72" />
          <el-table-column prop="topDepth" label="顶深 m" width="92" />
          <el-table-column prop="bottomDepth" label="底深 m" width="92" />
          <el-table-column prop="type" label="层位类型" width="100" />
          <el-table-column prop="conclusion" label="评价结论" />
        </el-table>
      </section>

      <el-button type="primary" :loading="savingReport" @click="saveReport">
        保存报告
      </el-button>
    </el-drawer>
  </main>
</template>
