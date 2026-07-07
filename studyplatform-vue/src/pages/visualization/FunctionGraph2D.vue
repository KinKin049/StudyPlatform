<script setup>
/**
 * 函数图像实验室页面
 * 支持输入函数表达式实时绘制二维曲线，提供参数控制和预设函数
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import * as echarts from 'echarts'
import { useLearningTimeTracker } from '../../composables/useLearningTimeTracker'

useLearningTimeTracker({
  moduleType: 'visualization',
  targetCode: 'function-2d',
  targetTitle: '二维函数图像实验室',
})

/** 函数表达式输入 */
const expression = ref('sin(x) + 0.2 * x')
/** x轴最小值 */
const xMin = ref(-10)
/** x轴最大值 */
const xMax = ref(10)
/** 采样点数 */
const samples = ref(400)
/** 错误提示信息 */
const error = ref('')
/** 图表容器引用 */
const chartRef = ref(null)
/** ECharts 实例 */
let chart = null

/** 支持的函数名称列表 */
const functionNames = ['sin', 'cos', 'tan', 'sqrt', 'abs', 'log', 'exp', 'pow', 'floor', 'ceil', 'round']

/**
 * 编译函数表达式
 * 将用户输入的表达式转换为可执行的 JavaScript 函数
 * 支持数学常量 pi、e 和常用数学函数
 */
const compiledFunction = computed(() => {
  error.value = ''
  const normalized = expression.value
    .replace(/\^/g, '**')
    .replace(/\bpi\b/gi, 'Math.PI')
    .replace(/\be\b/g, 'Math.E')
    .replace(/\b(sin|cos|tan|sqrt|abs|log|exp|floor|ceil|round)\b/g, 'Math.$1')
    .replace(/\bpow\b/g, 'Math.pow')

  if (!/^[\d\s+\-*/%().,xMathPIEabcdefghijklmnopqrstuvwxyz_]*$/i.test(normalized)) {
    error.value = '表达式包含不支持的字符'
    return null
  }

  try {
    return new Function('x', `"use strict"; return ${normalized}`)
  } catch {
    error.value = '表达式无法解析'
    return null
  }
})

/** 预设函数表达式列表 */
const presets = [
  'sin(x)',
  'cos(x)',
  'x^2 - 4',
  'sqrt(abs(x))',
  'exp(-0.1*x*x) * sin(3*x)',
  'log(abs(x)+1)',
]

/**
 * 构建图表数据点
 * 根据编译后的函数、x轴范围和采样点数生成数据数组
 */
function buildData() {
  const fn = compiledFunction.value
  if (!fn) return []

  const min = Number(xMin.value)
  const max = Number(xMax.value)
  const count = Math.max(60, Math.min(Number(samples.value), 1200))
  const step = (max - min) / count

  if (!Number.isFinite(min) || !Number.isFinite(max) || min >= max) {
    error.value = '请输入有效的 x 范围'
    return []
  }

  const data = []
  for (let i = 0; i <= count; i += 1) {
    const x = min + step * i
    const y = fn(x)
    data.push([Number(x.toFixed(4)), Number.isFinite(y) ? Number(y.toFixed(6)) : null])
  }
  return data
}

/**
 * 渲染图表
 * 初始化或更新 ECharts 图表实例，设置配置项和数据
 */
function renderChart() {
  if (!chartRef.value) return
  if (!chart) chart = echarts.init(chartRef.value)
  const data = buildData()
  chart.setOption({
    animation: false,
    grid: { top: 42, right: 28, bottom: 48, left: 58 },
    tooltip: { trigger: 'axis' },
    dataZoom: [{ type: 'inside' }, { type: 'slider', height: 22, bottom: 12 }],
    xAxis: { type: 'value', name: 'x', splitLine: { lineStyle: { color: '#e8f0f4' } } },
    yAxis: { type: 'value', name: 'y', splitLine: { lineStyle: { color: '#e8f0f4' } } },
    series: [
      {
        name: `y = ${expression.value}`,
        type: 'line',
        showSymbol: false,
        connectNulls: false,
        lineStyle: { width: 3, color: '#16a085' },
        data,
      },
    ],
  })
}

/**
 * 调整图表尺寸
 * 响应窗口大小变化
 */
function resizeChart() {
  chart?.resize()
}

/**
 * 组件挂载时初始化图表
 */
onMounted(async () => {
  await nextTick()
  renderChart()
  window.addEventListener('resize', resizeChart)
})

/**
 * 组件卸载时清理资源
 */
onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
})

/**
 * 监听参数变化，实时更新图表
 */
watch([expression, xMin, xMax, samples], renderChart)
</script>

<template>
  <main class="visual-page function-lab-page">
    <!-- 面包屑导航 -->
    <nav class="algorithm-viewer-breadcrumb visual-page-breadcrumb" aria-label="当前位置">
      <RouterLink to="/visualization">可视化</RouterLink>
      <span>&gt;</span>
      <strong>函数图像实验室</strong>
    </nav>

    <!-- 页面标题区域 -->
    <section class="visual-hero compact">
      <p class="visual-kicker">2D Math</p>
      <h1>函数图像实验室</h1>
      <p>输入关于 x 的函数表达式，实时绘制二维曲线。支持 {{ functionNames.join(' / ') }} 等常用函数。</p>
    </section>

    <!-- 主布局区域 -->
    <section class="function-lab-layout">
      <!-- 参数控制面板 -->
      <aside class="function-control-panel">
        <label>
          <span>函数表达式</span>
          <input v-model="expression" type="text" />
        </label>
        <!-- x轴范围设置 -->
        <div class="function-range-grid">
          <label>
            <span>x 最小值</span>
            <input v-model.number="xMin" type="number" />
          </label>
          <label>
            <span>x 最大值</span>
            <input v-model.number="xMax" type="number" />
          </label>
        </div>
        <label>
          <span>采样点数</span>
          <input v-model.number="samples" type="number" min="60" max="1200" />
        </label>
        <!-- 预设函数按钮 -->
        <div class="function-presets">
          <button v-for="item in presets" :key="item" type="button" @click="expression = item">
            {{ item }}
          </button>
        </div>
        <!-- 错误提示 -->
        <p v-if="error" class="function-error">{{ error }}</p>
      </aside>

      <!-- 图表显示区域 -->
      <section class="function-chart-panel">
        <div ref="chartRef" class="function-chart"></div>
      </section>
    </section>
  </main>
</template>
