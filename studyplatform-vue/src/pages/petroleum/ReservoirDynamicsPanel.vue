<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { downloadTextReport } from './api'

/**
 * 油藏动态仿真面板。
 * 用于观察地层压力、渗透率、含水饱和度和原油粘度对产能的综合影响。
 * 采用简化产能公式，将渗流能力、生产压差和粘度阻力统一折算为日产液，再按含水饱和度拆分油水产量。
 */

// 地层压力 MPa（用户输入参数）
const formationPressure = ref(25)
// 渗透率 mD（用户输入参数）
const permeability = ref(100)
// 含水饱和度 %（用户输入参数）
const waterSaturation = ref(30)
// 原油粘度 mPa·s（用户输入参数）
const viscosity = ref(10)
// 报告抽屉显示状态
const reportVisible = ref(false)
// 图表DOM引用
const reservoirChartRef = ref(null)
// ECharts图表实例
const chartInstance = ref(null)

// 根据油藏参数计算产能，采用简化产能公式：日产液 = 系数 × 渗透率 × 生产压差 / 粘度
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

// 报告表格数据行
const reservoirReportRows = computed(() => [
  { name: '日产油', value: `${reservoirProduction.value.dailyOil} t/d` },
  { name: '日产水', value: `${reservoirProduction.value.dailyWater} t/d` },
  { name: '日产液', value: `${reservoirProduction.value.dailyLiquid} t/d` },
  { name: '含水占比', value: `${waterSaturation.value}%` },
])

// 报告解释结论，根据各参数取值范围生成综合评价
const reservoirReportConclusion = computed(() => {
  const pressureText = formationPressure.value >= 28 ? '地层压力较高，具备较好的驱动能量' : '地层压力偏中低，产液能力受压差限制'
  const permeabilityText = permeability.value >= 300 ? '渗透率较高，流体渗流条件较好' : '渗透率偏低或中等，流动能力受储层物性约束'
  const waterText = waterSaturation.value >= 60 ? '含水饱和度较高，日产水占比较大，需要关注控水措施' : '含水饱和度处于可控范围，日产油占比相对较高'
  const viscosityText = viscosity.value >= 25 ? '原油粘度较高，对产能有明显抑制' : '原油粘度较低或中等，对产能抑制较弱'
  return `${pressureText}；${permeabilityText}；${waterText}；${viscosityText}。`
})

/**
 * 渲染油藏动态柱状图，展示日产油和日产水的占比
 */
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

/**
 * 调整图表尺寸
 */
function resizeChart() {
  chartInstance.value?.resize()
}

/**
 * 下载油藏动态解释报告到本地
 */
function downloadReservoirReport() {
  const rows = reservoirReportRows.value.map((row) => `${row.name}：${row.value}`).join('\n')
  const content = [
    '油藏动态解释报告',
    '',
    '一、当前仿真参数',
    `地层压力：${formationPressure.value} MPa`,
    `渗透率：${permeability.value} mD`,
    `含水饱和度：${waterSaturation.value}%`,
    `原油粘度：${viscosity.value} mPa·s`,
    '',
    '二、产能计算结果',
    rows,
    '',
    '三、解释结论',
    reservoirReportConclusion.value,
  ].join('\n')

  downloadTextReport(`油藏动态解释报告_${Date.now()}.txt`, content)
  ElMessage.success('报告已下载到本地')
}

// 监听所有油藏参数变化，重新计算产能并渲染图表
watch([formationPressure, permeability, waterSaturation, viscosity], renderReservoirChart)

// 组件挂载时初始化图表和尺寸监听
onMounted(async () => {
  await nextTick()
  renderReservoirChart()
  window.addEventListener('resize', resizeChart)
})

// 组件卸载时清理资源
onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chartInstance.value?.dispose()
})
</script>

<template>
  <section class="production-layout">
    <!-- 左侧控制面板 -->
    <aside class="production-control">
      <!-- 油藏参数配置卡片 -->
      <el-card shadow="never">
        <template #header>油藏参数</template>
        <!-- 地层压力滑块 -->
        <div class="control-item">
          <div class="control-label"><span>地层压力</span><strong>{{ formationPressure }} MPa</strong></div>
          <el-slider v-model="formationPressure" :min="10" :max="40" :step="0.5" />
        </div>
        <!-- 渗透率滑块 -->
        <div class="control-item">
          <div class="control-label"><span>渗透率</span><strong>{{ permeability }} mD</strong></div>
          <el-slider v-model="permeability" :min="1" :max="1000" :step="1" />
        </div>
        <!-- 含水饱和度滑块 -->
        <div class="control-item">
          <div class="control-label"><span>含水饱和度</span><strong>{{ waterSaturation }}%</strong></div>
          <el-slider v-model="waterSaturation" :min="0" :max="100" :step="1" />
        </div>
        <!-- 原油粘度滑块 -->
        <div class="control-item">
          <div class="control-label"><span>原油粘度</span><strong>{{ viscosity }} mPa·s</strong></div>
          <el-slider v-model="viscosity" :min="1" :max="50" :step="0.5" />
        </div>
        <!-- 生成报告按钮 -->
        <el-button type="primary" class="full-control" @click="reportVisible = true">生成解释报告</el-button>
      </el-card>

      <!-- 仿真说明卡片 -->
      <el-card shadow="never" class="simulation-intro-card">
        <template #header>仿真说明</template>
        <p>
          油藏动态模块用于观察地层压力、渗透率、含水饱和度和原油粘度对产能的综合影响。
          参数变化后，日产油、日产水和日产液会立即重新计算。
        </p>
        <p>
          模型采用简化产能公式，将渗流能力、生产压差和粘度阻力统一折算为日产液，再按含水饱和度拆分油水产量。
        </p>
      </el-card>
    </aside>

    <!-- 右侧可视化展示区域 -->
    <section class="production-visual">
      <!-- 产能指标网格 -->
      <div class="metric-grid">
        <el-card shadow="never"><span>日产油</span><strong>{{ reservoirProduction.dailyOil }} t/d</strong></el-card>
        <el-card shadow="never"><span>日产水</span><strong>{{ reservoirProduction.dailyWater }} t/d</strong></el-card>
        <el-card shadow="never"><span>日产液</span><strong>{{ reservoirProduction.dailyLiquid }} t/d</strong></el-card>
      </div>
      <!-- 柱状图卡片 -->
      <el-card shadow="never" class="chart-card-fill">
        <template #header>日产油 / 日产水占比</template>
        <div ref="reservoirChartRef" class="production-chart large-chart"></div>
      </el-card>
    </section>
  </section>

  <!-- 油藏动态解释报告抽屉 -->
  <el-drawer v-model="reportVisible" title="油藏动态解释报告" size="44%">
    <!-- 当前仿真参数 -->
    <section class="report-section">
      <h3>当前仿真参数</h3>
      <p>地层压力：{{ formationPressure }} MPa</p>
      <p>渗透率：{{ permeability }} mD</p>
      <p>含水饱和度：{{ waterSaturation }}%</p>
      <p>原油粘度：{{ viscosity }} mPa·s</p>
    </section>

    <!-- 产能计算结果表格 -->
    <section class="report-section">
      <h3>产能计算结果</h3>
      <el-table :data="reservoirReportRows" border>
        <el-table-column prop="name" label="指标" width="130" />
        <el-table-column prop="value" label="结果" />
      </el-table>
    </section>

    <!-- 解释结论 -->
    <section class="report-section">
      <h3>解释结论</h3>
      <p>{{ reservoirReportConclusion }}</p>
    </section>

    <!-- 下载报告按钮 -->
    <el-button type="primary" @click="downloadReservoirReport">下载报告到本地</el-button>
  </el-drawer>
</template>
