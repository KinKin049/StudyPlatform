<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { downloadTextReport } from './api'

/**
 * 测井曲线仿真面板。
 * 前端完成岩性判别、曲线计算（GR、AC、RT）、层段合并和图表渲染。
 * 支持孔隙度和含油饱和度参数调节，自动识别泥岩、干层、水层和油气层。
 */

// 声波时差计算常量：骨架声波时差 μs/m
const AC_MATRIX = 180
// 声波时差计算常量：流体声波时差 μs/m
const AC_FLUID = 600
// Archie公式系数：胶结指数相关系数
const ARCHIE_A = 1
// Archie公式系数：胶结指数
const ARCHIE_M = 2
// Archie公式系数：饱和度指数
const ARCHIE_N = 2
// 地层水电阻率 Ω·m
const WATER_RESISTIVITY = 0.12
// 最大模拟深度 m
const MAX_DEPTH = 2000
// 深度采样间隔 m
const DEPTH_STEP = 20

// 图表容器引用
const chartWrapRef = ref(null)
// 图表DOM引用
const chartRef = ref(null)
// ECharts图表实例
const chartInstance = ref(null)
// 图表尺寸变化监听器
const chartResizeObserver = ref(null)
// 孔隙度百分比（用户输入参数）
const porosityPercent = ref(20)
// 含油饱和度百分比（用户输入参数）
const oilSaturationPercent = ref(60)
// 报告抽屉显示状态
const reportVisible = ref(false)

// 深度数组，从0到MAX_DEPTH，步长为DEPTH_STEP
const depthArray = Array.from({ length: MAX_DEPTH / DEPTH_STEP + 1 }, (_, index) => index * DEPTH_STEP)

// 自然伽马（GR）曲线基础数据，根据深度区间生成周期性变化的GR值
const grBase = depthArray.map((depth) => {
  if (depth < 260) return 95 + Math.sin(depth / 38) * 8
  if (depth < 620) return 42 + Math.sin(depth / 44) * 7
  if (depth < 880) return 88 + Math.sin(depth / 35) * 9
  if (depth < 1260) return 36 + Math.sin(depth / 50) * 8
  if (depth < 1500) return 92 + Math.sin(depth / 42) * 8
  if (depth < 1840) return 48 + Math.sin(depth / 45) * 9
  return 86 + Math.sin(depth / 40) * 7
})

// 岩性分层区间，基于GR值判断泥岩/砂岩并合并相邻相同层段
const lithologyIntervals = computed(() =>
  mergeIntervals(
    depthArray.map((depth, index) => ({
      depth,
      type: grBase[index] > 80 ? '泥岩' : '砂岩',
    })),
  ),
)

// 自然伽马（GR）曲线数据，格式为[GR值, 深度]
const grCurve = computed(() => depthArray.map((depth, index) => [Number(grBase[index].toFixed(2)), depth]))

// 声波时差（AC）曲线数据，根据Wyllie公式计算，考虑岩性偏移和层理纹理
const acCurve = computed(() => {
  const porosity = porosityPercent.value / 100
  const acValue = porosity * (AC_FLUID - AC_MATRIX) + AC_MATRIX
  return depthArray.map((depth, index) => {
    const lithologyOffset = grBase[index] > 80 ? -10 : 10
    const layerTexture = Math.sin(depth / 80) * 4
    return [Number((acValue + lithologyOffset + layerTexture).toFixed(2)), depth]
  })
})

// 电阻率（RT）曲线数据，根据Archie公式计算，考虑岩性因子和层理纹理
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

// 电阻率轴最大值，取10的整数次幂且不小于1000
const rtAxisMax = computed(() => {
  const maxValue = Math.max(...rtCurve.value.map(([value]) => value))
  return Math.max(1000, Math.pow(10, Math.ceil(Math.log10(maxValue))))
})

// 解释分层结果，根据GR、孔隙度、含油饱和度综合判别并添加评价结论
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

// 标记区域数据，用于在图表中高亮显示各层段
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
 * 层位分类函数，根据GR值、孔隙度和含油饱和度综合判别层位类型
 * @param {number} grValue - 自然伽马值
 * @param {number} porosity - 孔隙度百分比
 * @param {number} oilSaturation - 含油饱和度百分比
 * @returns {string} 层位类型：泥岩、干层、水层、油气层
 */
function classifyLayer(grValue, porosity, oilSaturation) {
  if (grValue > 80) return '泥岩'
  if (porosity < 10) return '干层'
  if (oilSaturation < 20) return '水层'
  if (oilSaturation >= 50) return '油气层'
  return '干层'
}

/**
 * 合并相邻相同类型的层段区间
 * @param {Array} points - 各深度点的层位类型数组
 * @returns {Array} 合并后的层段区间数组
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
 * 获取层位类型对应的颜色
 * @param {string} type - 层位类型
 * @returns {string} 颜色值
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
 * 获取层位类型对应的评价结论
 * @param {string} type - 层位类型
 * @returns {string} 评价结论
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

/**
 * 获取图表标题样式
 * @returns {Object} 标题样式配置
 */
function titleStyle() {
  return {
    color: '#233f4d',
    fontSize: 14,
    fontWeight: 800,
  }
}

/**
 * 创建核心坐标轴配置
 * @param {number} gridIndex - 网格索引
 * @param {string} type - 轴类型：value/log
 * @param {number} min - 最小值
 * @param {number} max - 最大值
 * @param {string} axisColor - 轴颜色
 * @param {boolean} showLabel - 是否显示标签
 * @returns {Object} 坐标轴配置对象
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
 * 创建曲线系列配置
 * @param {string} name - 系列名称
 * @param {Array} data - 曲线数据
 * @param {number} axisIndex - 轴索引
 * @param {string} color - 曲线颜色
 * @returns {Object} 系列配置对象
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
 * 岩心柱状图渲染函数，用于ECharts自定义系列
 * @param {Object} interval - 层段区间信息
 * @returns {Function} ECharts自定义渲染函数
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
 * 创建ECharts图表配置项
 * @returns {Object} ECharts配置对象
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
 * 渲染图表，初始化或更新ECharts实例
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

/**
 * 调整图表尺寸
 */
function resizeChart() {
  chartInstance.value?.resize()
}

/**
 * 调度图表尺寸调整，使用requestAnimationFrame优化性能
 */
function scheduleResizeChart() {
  window.requestAnimationFrame(() => {
    if (!chartInstance.value) {
      renderChart()
      return
    }
    resizeChart()
  })
}

/**
 * 构建报告数据载荷
 * @returns {Object} 报告数据对象
 */
function buildReportPayload() {
  return {
    porosity: porosityPercent.value,
    oilSaturation: oilSaturationPercent.value,
    layers: interpretedLayers.value,
  }
}

/**
 * 下载测井解释报告到本地
 */
function downloadWellLogReport() {
  const layerRows = interpretedLayers.value
    .map((layer) => `${layer.index}. ${layer.topDepth}-${layer.bottomDepth} m，${layer.type}：${layer.conclusion}`)
    .join('\n')
  const content = [
    '测井解释报告',
    '',
    '一、当前仿真参数',
    `孔隙度 φ：${porosityPercent.value}%`,
    `含油饱和度 So：${oilSaturationPercent.value}%`,
    '',
    '二、分层结果',
    layerRows,
  ].join('\n')

  downloadTextReport(`测井解释报告_${Date.now()}.txt`, content)
  ElMessage.success('报告已下载到本地')
}

// 监听孔隙度和含油饱和度变化，重新渲染图表
watch([porosityPercent, oilSaturationPercent], () => {
  renderChart()
})

// 组件挂载时初始化图表和尺寸监听
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

// 组件卸载时清理资源
onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chartResizeObserver.value?.disconnect()
  chartInstance.value?.dispose()
})
</script>

<template>
  <section class="well-log-content petroleum-embedded-content">
    <!-- 左右分栏布局容器 -->
    <el-splitter class="well-log-layout" @resize="resizeChart" @resize-end="resizeChart">
      <!-- 左侧控制面板 -->
      <el-splitter-panel size="20%" min="260px" class="well-log-control">
        <div class="well-log-panel well-log-control-panel">
          <!-- 仿真参数配置卡片 -->
          <el-card shadow="never">
            <template #header>
              <span>仿真参数</span>
            </template>

            <!-- 孔隙度滑块 -->
            <div class="control-item">
              <div class="control-label">
                <span>孔隙度 φ</span>
                <strong>{{ porosityPercent }}%</strong>
              </div>
              <el-slider v-model="porosityPercent" :min="0" :max="35" :step="0.5" />
            </div>

            <!-- 含油饱和度滑块 -->
            <div class="control-item">
              <div class="control-label">
                <span>含油饱和度 So</span>
                <strong>{{ oilSaturationPercent }}%</strong>
              </div>
              <el-slider v-model="oilSaturationPercent" :min="0" :max="100" :step="1" />
            </div>

            <!-- 生成报告按钮 -->
            <el-button type="primary" class="report-button" @click="reportVisible = true">
              生成解释报告
            </el-button>
          </el-card>

          <!-- 计算模型说明卡片 -->
          <el-card shadow="never" class="formula-card">
            <template #header>
              <span>计算模型</span>
            </template>
            <p>AC = φ × (600 - 180) + 180</p>
            <p>RT = (1 × 0.12) / (φ² × (1 - So)²)</p>
            <p>GR为岩性基础曲线，仅用于岩性判别。</p>
          </el-card>

          <!-- 仿真说明卡片 -->
          <el-card shadow="never" class="simulation-intro-card">
            <template #header>
              <span>仿真说明</span>
            </template>
            <p>
              测井仿真模块将岩心柱状图、GR、RT 和 AC 四道曲线按同一深度轴联动显示，用于观察储层段和泥岩段的响应差异。
            </p>
            <p>
              孔隙度主要影响声波时差，含油饱和度主要影响电阻率；系统根据 GR、孔隙度和含油饱和度自动识别泥岩、干层、水层和油气层。
            </p>
          </el-card>
        </div>
      </el-splitter-panel>

      <!-- 右侧图表展示区域 -->
      <el-splitter-panel size="80%" min="620px" class="well-log-main">
        <div class="well-log-panel well-log-main-panel">
          <!-- 测井曲线图表卡片 -->
          <el-card shadow="never" class="well-log-chart-card">
            <template #header>
              <div class="chart-header">
                <span>岩心 + 测井曲线联动剖面</span>
                <small>深度 0-2000m，自上而下递增</small>
              </div>
            </template>

            <!-- 图表容器 -->
            <div ref="chartWrapRef" class="chart-wrap">
              <div ref="chartRef" class="well-log-chart"></div>
              <!-- 层位标注侧边栏 -->
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

  <!-- 测井解释报告抽屉 -->
  <el-drawer v-model="reportVisible" title="测井解释报告" size="46%">
    <!-- 当前仿真参数 -->
    <section class="report-section">
      <h3>当前仿真参数</h3>
      <p>孔隙度 φ：{{ porosityPercent }}%</p>
      <p>含油饱和度 So：{{ oilSaturationPercent }}%</p>
    </section>

    <!-- 分层结果表格 -->
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

    <!-- 下载报告按钮 -->
    <el-button type="primary" @click="downloadWellLogReport">
      下载报告到本地
    </el-button>
  </el-drawer>
</template>
